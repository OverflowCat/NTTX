package io.kurumi.ntt.model.request;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.StaticLog;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import io.kurumi.ntt.Launcher;
import io.kurumi.ntt.db.PointData;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.twitter.status.MessagePoint;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.utils.NTT;

public class Send extends AbstractSend<Send> {

    private SendMessage request;

    public Send(Fragment fragment, String chatId, String msg, Object... params) {

        this(null, fragment, chatId, msg, params);

    }

    public Send(Fragment fragment, long chatId, String msg, Object... params) {

        this(null, fragment, chatId, msg, params);

    }

    public Send(String chatId, String msg, Object... params) {

        this(null, Launcher.INSTANCE, chatId, msg, params);

    }

    public Send(long chatId, String msg, Object... params) {

        this(null, Launcher.INSTANCE, chatId, msg, params);

    }


    private Send(Void v, Fragment fragment, Object chatId, String msg, Object... params) {

        super(fragment);

        request = new SendMessage(chatId, StrUtil.format(msg, params));

        this.fragment = fragment;

        request.disableWebPagePreview(true);

    }

    @Override
    public Send enableLinkPreview() {

        request.disableWebPagePreview(false);

        return this;

    }


    public Send disableNotification() {

        request.disableNotification(true);

        return this;

    }

    public Send replyToMessageId(int replyToMessageId) {

        request.replyToMessageId(replyToMessageId);

        return this;

    }

    public Send replyTo(Msg msg) {

        if (msg == null) return this;

        replyToMessageId(msg.messageId());

        return this;

    }

    @Override
    public Send markdown() {

        request.parseMode(ParseMode.Markdown);

        return this;

    }

    @Override
    public Send html() {

        request.parseMode(ParseMode.HTML);

        return this;

    }


    public Send removeKeyboard() {

        request.replyMarkup(new ReplyKeyboardRemove());

        return this;

    }

    public Send keyboardVertical(final Object... buttons) {

        Keyboard keyboard = new Keyboard();

        for (Object button : buttons) keyboard.newButtonLine(button.toString());

        keyboard(keyboard);

        return this;

    }

    public Send keyboardHorizontal(final Object... buttons) {

        Keyboard keyboard = new Keyboard();

        KeyboradButtonLine line = keyboard.newButtonLine();

        for (Object button : buttons) line.newButton(button.toString());

        keyboard(keyboard);

        return this;

    }


    public Send keyboard(Keyboard keyboard) {

        request.replyMarkup(keyboard.markup());

        return this;

    }

    @Override
    public Send buttons(ButtonMarkup markup) {

        request.replyMarkup(markup.markup());

        return this;

    }

    public void publicFailed() {

        if (origin.isPrivate()) {

            exec();

        } else {

            failedWith();

        }

    }

    public void failed() {

        failed(5000);

    }

    public void failed(final long delay) {

        if (origin == null) return;

        BotFragment.waitPool.execute(new Runnable() {

            @Override
            public void run() {

                SendResponse resp = exec();

                if (resp.isOk()) {

                    NTT.tryDelete(delay, new Msg(fragment, resp.message()));

                }

            }

        });


    }

    public void failedWith() {

        failedWith(5000);

    }

    public void failedWith(final long delay) {

        SendResponse resp = exec();

        if (resp.isOk()) {

            NTT.tryDelete(delay, new Msg(fragment, resp.message()), origin);

        }

    }

    public void cancel() {

        cancel(5000);

    }

    public void cancel(final long delay) {

        if (origin == null) return;

        SendResponse resp = exec();

        if (resp.isOk()) {

            NTT.tryDelete(delay, new Msg(fragment, resp.message()));

        }

    }

    public Send withCancel() {

        request.setText(request.getText() + "\n\n使用 /cancel 取消当前正在进行的操作 ~");

        return this;

    }

    public Msg send() {

        SendResponse resp = exec();

        if (!resp.isOk()) return null;

        return new Msg(fragment, resp.message());

    }

    public SendResponse point(int type, long targetId) {

        SendResponse resp = exec();

        if (resp != null && resp.isOk() && resp.message().chat().type() == Chat.Type.Private) {

            MessagePoint.set(resp.message().messageId(), type, targetId);

        }

        return resp;

    }


    public SendMessage request() {

        return request;

    }

    public Msg exec(PointData toAdd) {

        Msg resp = send();

        if (resp != null) toAdd.with(resp);

        return resp;

    }

    private boolean noLog = false;

    public Send noLog() {

        noLog = true;

        return this;

    }

    private boolean async = false;

    public void async() {

        this.async = true;

        exec();

    }

    @Override
    public SendResponse exec() {

        if (fragment == null) return null;
        if (request.getText() == null) return null;

        char[] arr = request.getText().toCharArray();

        while (arr.length > 4096) {

            Character[] chars = (Character[]) ArrayUtil.sub(ArrayUtil.wrap((Object) arr), 0, 4096);

            int index = chars.length;

            boolean sdd = false;

            for (Character c : ArrayUtil.reverse(chars)) {

                index--;

                if (c.equals('\n')) {

                    char[] send = new char[index];

                    ArrayUtil.copy(arr, send, index);

                    fork(String.valueOf(send)).exec();

                    char[] subed = new char[arr.length - index];

                    ArrayUtil.copy(arr, index, subed, 0, subed.length);

                    request.setText(String.valueOf(subed));

                    sdd = true;

                    break;

                }

            }

            if (!sdd) {

                // 没有换行的情况

                char[] send = new char[4096];

                ArrayUtil.copy(arr, send, 4096);

                fork(String.valueOf(send)).exec();

                char[] subed = new char[arr.length - 4096];

                ArrayUtil.copy(arr, 4095, subed, 0, subed.length);

                request.setText(String.valueOf(subed));

            }

            arr = request.getText().toCharArray();

        }

        try {

            if (async) {

                fragment.executeAsync(origin == null ? null : origin.update, request);

                return null;

            }

            SendResponse resp = fragment.execute(request);

            if (resp != null && !resp.isOk()) {

                if (resp.errorCode() == 403 && !(request.chatId instanceof String) && ((long) request.chatId > 0)) {

                    UserData user = UserData.get((long) request.chatId);

                    if (user != null) {

                        user.contactable = false;

                        UserData.userDataIndex.put(user.id, user);

                        UserData.data.setById(user.id, user);

                    }

                }

                if (resp != null) {

                    StaticLog.debug("发送出错 : \n\n{}\n\n{} {}", request.getText(), resp.errorCode(), resp.description());

                }

            }

            if (resp != null) return resp;


        } catch (Exception ex) {
        }


        return new SendResponse("Telegram 服务器超时");


    }

    public Send fork(Long chatId) {

        Send send = new Send(null, fragment, chatId, request.getText());

        if (request.mode != null) {

            send.request.parseMode(request.mode);
        }

        send.request.disableWebPagePreview(request.disableWebPagePreview);

        return send;

    }

    public Send fork(String msg) {

        Send send = new Send(null, fragment, request.chatId, msg);

        if (request.mode != null) {

            send.request.parseMode(request.mode);
        }

        send.request.disableWebPagePreview(request.disableWebPagePreview);

        return send;

    }

}
