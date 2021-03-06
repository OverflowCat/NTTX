package io.kurumi.ntt.fragment.group;

import com.pengrad.telegrambot.model.MessageEntity;
import io.kurumi.ntt.db.GroupData;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.model.Msg;

public class GroupActions extends Fragment {

    @Override
    public void init(BotFragment origin) {

        super.init(origin);

        registerFunction("d", "rest", "kick", "ban");

    }

    @Override
    public int checkFunctionContext(UserData user, Msg msg, String function, String[] params) {

        return FUNCTION_GROUP;

    }

    @Override
    public void onFunction(UserData user, Msg msg, String function, String[] params) {

        if (!GroupAdmin.fastAdminCheck(this, GroupData.get(this, msg.chat()), user.id, false)) {

            msg.reply("permission denied.").failedWith(500);

            return;

        }

        if ("d".equals(function)) {

            if (msg.replyTo() == null) {

                return;

            }

            msg.delete();
            msg.replyTo().delete();

            return;

        }

        long targetId = -1;

        if (msg.replyTo() != null) {

            msg.delete();

            msg.replyTo().delete();

            targetId = msg.replyTo().from().id;

        } else {

            for (MessageEntity entry : msg.message().entities()) {

                if (entry.type() == MessageEntity.Type.text_mention) {

                    targetId = UserData.get(entry.user()).id;

                }

            }

            if (targetId == -1) {

                msg.invalidParams("用户引用 / 对消息回复").async();

                return;

            }

        }

        msg.delete();

        if ("rest".equals(function)) {

            msg.restrict();

        } else if ("kick".equals(function)) {

            msg.kick(targetId, false);

        } else {

            msg.kick(targetId, true);

        }

    }

}
