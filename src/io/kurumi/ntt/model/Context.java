package io.kurumi.ntt.model;

import cn.hutool.core.util.NumberUtil;
import com.pengrad.telegrambot.model.Chat;
import io.kurumi.ntt.fragment.Fragment;

public class Context {

    public Fragment fragment;
    public long targetChatId = -1;
    private Chat chat;

    public Context(Fragment fragment, Chat chat) {

        this.fragment = fragment;
        this.chat = chat;


    }

    public Chat chat() {
        return chat;
    }

    public Long chatId() {

        return targetChatId == -1 ? chat.id() : targetChatId;

    }

    public int chatIdInt() {

        return NumberUtil.parseInt(chatId().toString().substring(4));

    }


    public boolean isPrivate() {
        return chat.type() == Chat.Type.Private;
    }

    public boolean isGroup() {
        return chat.type() == Chat.Type.group || chat.type() == Chat.Type.supergroup;
    }

    public boolean isPublicGroup() {
        return chat.username() != null;
    }

    public boolean isSuperGroup() {
        return chat.type() == Chat.Type.supergroup;
    }

    public boolean isChannel() {
        return chat.type() == Chat.Type.channel;
    }


}


