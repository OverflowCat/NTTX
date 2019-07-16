package io.kurumi.ntt.fragment.bots;

import io.kurumi.ntt.db.Data;
import io.kurumi.ntt.fragment.bots.UserBot;

import java.util.Map;
import java.util.HashMap;

import io.kurumi.ntt.fragment.BotServer;

public class UserBot {

    public static Data<UserBot> data = new Data<UserBot>("UserCustomBot",UserBot.class);

		public Long id;
    public String userName;
    public Long user;
    public String token;
    public int type;
    public Map<String, Object> params;

    public static void startAll() {

        for (UserBot bot : data.collection.find()) {

            bot.startBot();

        }

    }

    public void startBot() {

        if (!BotServer.fragments.containsKey(token)) {

            if (type == 0) {

                ForwardBot client = new ForwardBot();

                client.botId = id;
                client.silentStart();

            } else if (type == 1) {

                GroupBot client = new GroupBot();

                client.botId = id;
                client.silentStart();

            }

        }

    }

    public void stopBot() {

        if (BotServer.fragments.containsKey(token)) {

            BotServer.fragments.remove(token).stop();

        }

    }

    public void reloadBot() {

        if (BotServer.fragments.containsKey(token)) {

            BotServer.fragments.get(token).reload();

        }

    }

    public String information() {

        StringBuilder information = new StringBuilder();

        if (type == 0) {

            String welcomeMsg = (String) params.get("msg");

            information.append("欢迎语 : > ").append(welcomeMsg).append(" <");

        }

        return information.toString();

    }

    public String typeName() {

        switch (type) {

            case 0:
                return "私聊";

            case 1:
                return "群组管理";

            default:
                return null;

        }

    }

}
