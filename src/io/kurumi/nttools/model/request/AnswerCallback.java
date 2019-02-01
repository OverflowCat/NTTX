package io.kurumi.nttools.model.request;

import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import io.kurumi.nttools.fragments.Fragment;

public class AnswerCallback {
    
    public Fragment fragment;
    public AnswerCallbackQuery answer;
    
    public AnswerCallback(Fragment fragment,String calbackId) {
        
        this.fragment = fragment;
        answer =  new AnswerCallbackQuery(calbackId);
        
    }
    
    public AnswerCallback text(String text) {
        
        answer.text(text);
        
        return this;
        
    }
    
    public AnswerCallback alert(String text) {

        answer.text(text);
        
        answer.showAlert(true);

        return this;

    }
    
    public AnswerCallback url(String url) {

        answer.url(url);

        return this;

    }
    
    public void exec() {
        
        fragment.bot.execute(answer);
        
    }
    
}
