package io.kurumi.ntt.fragment.bots;

import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.rss.RssSub;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.utils.Html;

public class RssBot extends UserBotFragment {

    @Override
    public void reload() {

        super.reload();

        addFragment(new RssSub());

        registerFunction("/start");

    }

    private static String link = "https://manual.kurumi.io/rss";

    @Override
    public void onFunction(UserData user, Msg msg, String function, String[] params) {

        super.onFunction(user, msg, function, params);

        if ("start".equals(function)) {

            msg.send("欢迎使用Rss订阅机器人 , 点此 " + Html.a("查看文档", link) + " ~").html().async();

        } else if (function.startsWith("rss_")) {

            msg.send("在自定义机器人中 " + Html.b("没有 rss_ 前缀") + "！你有认真看 " + Html.a("文档", link) + " 吗？").html().async();

        } else {

            msg.send(Html.b("没有这个命令") + " : /" + function + "\n\n为什么不看看 " + Html.a("文档", link) + " 呢？").html().async();

        }


    }

    @Override
    public void onFinalMsg(UserData user, Msg msg) {

        if (!msg.isPrivate()) return;

        msg.send("(？你在干啥 快看看 " + Html.a("文档", link)).html().async();

    }


}
