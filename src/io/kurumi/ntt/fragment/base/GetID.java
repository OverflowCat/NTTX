package io.kurumi.ntt.fragment.base;

import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.abs.Msg;
import java.util.LinkedList;
import io.kurumi.ntt.fragment.BotFragment;

public class GetID extends Fragment {

	@Override
	public void init(BotFragment origin) {

		super.init(origin);

		registerFunction("id");

	}

	@Override
	public void onFunction(UserData user,Msg msg,String function,String[] params) {

		if (msg.isReply()) {

			msg.send(msg.replyTo().from().id.toString()).publicFailed();

		} else {

			msg.send(msg.chatId().toString()).publicFailed();

		}

    }

	@Override
	public int checkChanPost(UserData user,Msg msg) {

		return "id".equals(msg.command()) ? 2 : 0;

	}

	@Override
	public void onChanPost(UserData user,Msg msg) {

		msg.send(msg.chatId().toString()).publicFailed();

	}



}
