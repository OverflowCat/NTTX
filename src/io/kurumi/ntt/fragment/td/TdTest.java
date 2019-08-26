package io.kurumi.ntt.fragment.td;

import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.td.client.TdBot;
import io.kurumi.ntt.td.TdApi;
import io.kurumi.ntt.td.TdApi.*;
import io.kurumi.ntt.td.client.TdException;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;

public class TdTest extends Fragment {

	@Override
	public void init(BotFragment origin) {

		super.init(origin);

		registerAdminFunction("test_get_members");

	}

	@Override
	public int checkFunctionContext(UserData user,Msg msg,String function,String[] params) {

		return FUNCTION_PUBLIC;

	}

	TdBot bot;

	@Override
	public void onFunction(UserData user,Msg msg,String function,String[] params) {

		if (bot == null) {

			bot = new TdBot(origin.getToken());

			bot.start();

		}

		try {

			TdApi.ChatMembers members = bot.execute(new TdApi.GetSupergroupMembers(msg.chatIdInt(),new TdApi.SupergroupMembersFilterRecent(),0,200));

			String message = "所有用户 : \n";

			for (ChatMember memberId : members.members) {

				TdApi.User member = bot.execute(new GetUser(memberId.userId));

				message += "\n<a href=\"" + memberId.userId + "\">" + member.firstName;
				
				if (!StrUtil.isBlank(member.lastName)) {
					
					message += " " + member.lastName;
					
				}
				
				message += "</a>";

			}

			msg.send(message).html().async();

		} catch (TdException e) {

			msg.send(e.getError().message).async();

		}

		//bot.destroy();

		// 


	}

}
