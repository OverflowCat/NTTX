package io.kurumi.ntt.fragment.admin;

import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.twitter.TAuth;
import io.kurumi.ntt.fragment.twitter.tasks.MargedNoticeTask;
import io.kurumi.ntt.model.Msg;

public class NoticePuhlish extends Fragment {

    @Override
    public void init(BotFragment origin) {

        super.init(origin);

        registerAdminFunction("notice_publish");

    }

    @Override
    public void onFunction(UserData user, Msg msg, String function, String[] params) {

        for (TAuth account : TAuth.data.getAllByField("fo_marge", true)) {

            //if (account.fo == null || account.fo_marge ) continue;

            MargedNoticeTask.doNotice(account);

        }

    }

}
