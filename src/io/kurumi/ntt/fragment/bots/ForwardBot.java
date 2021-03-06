package io.kurumi.ntt.fragment.bots;

import cn.hutool.core.util.ArrayUtil;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;
import io.kurumi.ntt.Env;
import io.kurumi.ntt.Launcher;
import io.kurumi.ntt.db.PointData;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.model.request.Send;
import io.kurumi.ntt.utils.Html;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForwardBot extends UserBotFragment {

    final String POINT_REPLY = "r";

    public Long lastReceivedFrom;
    public String welcomeMessage;
    public Set<Long> blockList;

    @Override
    public void reload() {

        super.reload();

        welcomeMessage = getParam("msg");

        List<Long> bl = getParam("block");

        if (bl != null) {

            blockList = new HashSet<Long>(bl);

            blockList.remove(userId);

        } else {

            blockList = new HashSet<>();

            setParam("block", blockList);

        }

    }

    @Override
    public int checkMsg(UserData user, Msg msg) {

        return PROCESS_SYNC_REJ;

    }

    @Override
    public void onFunction(UserData user, Msg msg, String function, String[] params) {

        super.onFunction(user, msg, function, params);

        if (!msg.isPrivate() || "cancel".equals(msg.command())) return;

        if ("start".equals(function)) {

            msg.send(welcomeMessage).exec();

        }

        checkMsg(user, msg);

    }

    @Override
    public void onPayload(UserData user, Msg msg, String payload, String[] params) {

        if (userId.equals(user.id) && "reply".equals(payload)) {

            UserData target = UserData.get(Long.parseLong(params[0]));

            if (target == null) {

                msg.send("找不到目标...").failedWith();

                return;

            }

            msg.send("回复 " + target.userName() + " : \n直接发送信息即可 (非文本，表情，文件 会直接转发) : ", "使用 /cancel 退出").html().exec();

            setPrivatePointData(user, POINT_REPLY, target.id);

        } else if (userId.equals(user.id) && "del".equals(payload)) {

            try {

                long target = Long.parseLong(params[0]);
                int messageId = Integer.parseInt(params[1]);

                BaseResponse resp = bot().execute(new DeleteMessage(target, messageId));

                if (resp.isOk()) {

                    msg.send("已删除").failedWith();

                } else {

                    msg.send("删除失败 这条发送的信息还在吗 ？").failedWith();

                }

            } catch (NumberFormatException e) {

                msg.send("这个删除已经点过了 :)").failedWith();

            }

        } else if (userId.equals(user.id) && "block".equals(payload)) {

            UserData target = UserData.get(Long.parseLong(params[0]));

            if (target == null) {

                msg.send("找不到目标...").failedWith();

                return;

            }

            if (target.id.equals(userId)) {

                msg.send("你不能屏蔽你自己...").failedWith();

                return;

            }

            if (blockList.contains(target.id.longValue())) {

                msg.send("已经屏蔽过了 " + target.userName() + " ~ [ " + Html.a("解除屏蔽", "https://t.me/" + me.username() + "?start=unblok" + PAYLOAD_SPLIT + target.id) + " ] ~").html().exec();

            } else {

                blockList.add(target.id);
                msg.send("已屏蔽 " + target.userName() + " ~ [ " + Html.a("解除屏蔽", "https://t.me/" + me.username() + "?start=unblock" + PAYLOAD_SPLIT + target.id) + " ] ~").html().exec();


            }

        } else if (userId.equals(user.id) && "unblock".equals(payload)) {

            UserData target = UserData.get(Long.parseLong(params[0]));

            if (target == null) {

                msg.send("找不到目标...").failedWith();

                return;

            }

            if (blockList.contains(target.id.longValue())) {

                blockList.remove(target.id.longValue());

                msg.send("已解除屏蔽 " + target.userName() + " ~ [ " + Html.a("屏蔽", "https://t.me/" + me.username() + "?start=block" + PAYLOAD_SPLIT + target.id) + " " + Html.a("发送消息", "https://t.me/" + me.username() + "?start=reply" + PAYLOAD_SPLIT + user.id) + " ]").html().exec();

            } else {

                msg.send("没有屏蔽 " + target.userName() + " ~ [ " + Html.a("屏蔽", "https://t.me/" + me.username() + "?start=block" + PAYLOAD_SPLIT + target.id) + " " + Html.a("发送消息", "https://t.me/" + me.username() + "?start=reply" + PAYLOAD_SPLIT + user.id) + " ]").html().exec();


            }

        } else if (msg.isStartPayload()) {

            onFunction(user, msg, msg.command(), msg.fixedParams());

        } else {

            checkMsg(user, msg);

        }


    }

    @Override
    public void onMsg(UserData user, Msg msg) {

        if (!msg.isPrivate()) {

            msg.exit();

            return;

        }

        if (userId.equals(user.id) || !blockList.contains(user.id.longValue())) {

            if (lastReceivedFrom == null || !lastReceivedFrom.equals(user.id)) {

                new Send(this, userId, "来自 " + user.userName() + " : [ " + Html.a("回复", "https://t.me/" + me.username() + "?start=reply" + PAYLOAD_SPLIT + user.id) + " " + Html.a("屏蔽", "https://t.me/" + me.username() + "?start=block" + PAYLOAD_SPLIT + user.id) + " ]").html().exec();

                lastReceivedFrom = user.id;

            }

            msg.forwardTo(userId);

            if (msg.isStartPayload()) {

                new Send(this, userId, "内容 : " + msg.text()).exec();

            }

        }

    }

    @Override
    public void onPoint(UserData user, Msg msg, String point, PointData data) {

        if (msg.isCommand()) {

            if ("cancel".equals(msg.command())) {

                super.onPoint(user, msg, point, data);

            } else if (msg.isStartPayload()) {

                final String payload = msg.payload()[0];
                final String[] params = msg.payload().length > 1 ? ArrayUtil.sub(msg.payload(), 1, msg.payload().length) : new String[0];

                onPayload(user, msg, payload, params);

            }

            return;

        }

        long target = (long) data.data;

        if (POINT_REPLY.equals(point)) {

            Message message = msg.message();

            int sended = -1;

            if (message.document() != null) {

                SendDocument send = new SendDocument(target, message.document().fileId());

                send.fileName(message.document().fileName());

                send.caption(message.text());

                SendResponse resp = bot().execute(send);

                if (!resp.isOk()) {

                    msg.send("发送失败 (˚☐˚! )/\n-----------------------", resp.description()).exec();

                } else {

                    sended = resp.message().messageId();

                }

            } else if (message.sticker() != null) {

                SendSticker send = new SendSticker(target, message.sticker().fileId());

                SendResponse resp = bot().execute(send);

                if (!resp.isOk()) {

                    msg.send("发送失败 (˚☐˚! )/\n-----------------------", resp.description()).exec();

                } else {

                    sended = resp.message().messageId();

                }

            } else if (msg.hasText()) {

                SendMessage send = new SendMessage(target, msg.text());

                SendResponse resp = bot().execute(send);

                if (!resp.isOk()) {

                    msg.send("发送失败 (˚☐˚! )/\n-----------------------", resp.description()).exec();

                } else {

                    sended = resp.message().messageId();

                }

            } else {

                ForwardMessage forward = new ForwardMessage(target, msg.chatId(), msg.messageId());

                SendResponse resp = bot().execute(forward);

                if (!resp.isOk()) {

                    msg.send("发送失败 (˚☐˚! )/\n-----------------------", resp.description()).exec();

                } else {

                    sended = resp.message().messageId();

                }

            }

            if (sended != -1) {

                msg.reply("发送成功 [ " + Html.a("删除", "https://t.me/" + me.username() + "?start=del" + PAYLOAD_SPLIT + target + PAYLOAD_SPLIT + sended) + " ]\n退出回复使用 /cancel ").html().exec();

            }


        }

    }

}
