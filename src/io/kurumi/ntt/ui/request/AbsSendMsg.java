package io.kurumi.ntt.ui.request;

import java.util.*;
import io.kurumi.ntt.ui.model.*;
import io.kurumi.ntt.ui.*;
import io.kurumi.ntt.twitter.*;
import com.pengrad.telegrambot.response.*;

public abstract class AbsSendMsg implements AbsResuest {
    
    public void processObject(DataObject obj) {}
    
    public abstract AbsSendMsg html();
    public abstract AbsSendMsg markdown();
    public abstract AbsSendMsg disableWebPagePreview();
    public abstract AbsSendMsg disableNotification();
    
    public abstract BaseResponse exec();
    
    protected LinkedList<InlineButtonGroup> inlineKeyBoardGroups = new LinkedList<>();
    
    public InlineButtonGroup newInlineButtonGroup() {
        
        InlineButtonGroup markup = new InlineButtonGroup(this);
        
        inlineKeyBoardGroups.add(markup);
        
        return markup;

    }
    
    public AbsSendMsg singleLineOpenUrlButton(String text,String url) {

        newInlineButtonGroup().newOpenUrlButton(text,url);

        return this;

    }
    
    public AbsSendMsg singleLineButton(String text,DataObject obj) {
        
        newInlineButtonGroup().newButton(text,obj);
        
        return this;
        
    }
    
    public AbsSendMsg singleLineButton(String text,String point) {
        
        newInlineButtonGroup().newButton(text,point);

        return this;

    }
    
    public AbsSendMsg singleLineButton(String text,String point,TwiAccount acc) {

        newInlineButtonGroup().newButton(text,point,acc);

        return this;

    }
    
    
}
