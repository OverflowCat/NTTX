package io.kurumi.ntt.funcs.abs;

import io.kurumi.ntt.fragment.*;
import io.kurumi.ntt.db.*;
import io.kurumi.ntt.model.*;
import cn.hutool.core.util.*;
import io.kurumi.ntt.db.PointStore.*;
import java.util.*;
import io.kurumi.ntt.utils.*;

public abstract class Function extends Fragment {

    public abstract void functions(LinkedList<String> names);

    public int target() {

        return All;

    }

    public void points(LinkedList<String> points) {
    }

    public abstract void onFunction(UserData user,Msg msg,String function,String[] params);

    public void onPoint(UserData user,Msg msg,PointStore.Point point) {}
    public void onCallback(UserData user,Callback callback,String point,String[] params) {}

    public static final String[] None = new String[0];

    public static final int All = 1;
    public static final int Private = 2;
    public static final int Group = 3;

    private LinkedList<String> functions = new LinkedList<String>() {{ functions(this); }};
    private LinkedList<String> points = new LinkedList<String>() {{ points(this); }};

    @Override
    public boolean onMsg(UserData user,Msg msg) {

        if (!msg.isCommand()) return false;

        if (!functions.contains(msg.command())) return false;

        if (target() == Group && !msg.isGroup())  {

            msg.send("请在群组使用 (˚☐˚! )/").exec();

            return true;

        }

        if (target() == Private && !msg.isPrivate())  {

			if (!user.contactable()) {

				msg.send("请使用私聊 (˚☐˚! )/").publicFailed();

				return true;

			} else {

				msg.send("这是一个仅私聊可用的命令，咱已经在私聊回复了你。","如果BOT有删除信息权限,命令和此回复将被自动删除。:)").exec();

				msg.targetChatId = user.id;

			}

        }

        msg.sendTyping();

        onFunction(user,msg,msg.command(),msg.params());

        return true;

    }


    @Override
    public boolean onPointedMsg(UserData user,Msg msg) {

        PointStore.Point point = point().get(user);

        switch (target()) {

			case Group : if (msg.isPrivate()) return false;break;
			case Private : if (msg.isGroup()) return false;break;

        }

        for (String used : points) {

            if (used.equals(point.point)) {

                onPoint(user,msg,point);

                return true;

            }

        }

        return false;

    }

    @Override
    public boolean onCallback(UserData user,Callback callback) {

        for (String used : points) {

            if (used.equals(callback.params[0])) {

                onCallback(user,callback,callback.params[0],ArrayUtil.remove(callback.params,0));

                return true;

            }

        }

        return false;
    }

}
