package io.kurumi.ntt.model.request;

import com.pengrad.telegrambot.response.BaseResponse;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.model.Msg;

public abstract class AbstractSend<T extends AbstractSend> {

    public Msg origin;

    public Fragment fragment;

    public AbstractSend(Fragment fragment) {
        this.fragment = fragment;
    }

    public abstract T buttons(ButtonMarkup markup);

    public abstract T enableLinkPreview();

    public abstract T markdown();

    public abstract T html();

    public abstract void async();

    public abstract BaseResponse exec();

}
