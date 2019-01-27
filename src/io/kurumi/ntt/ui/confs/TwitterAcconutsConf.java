package io.kurumi.ntt.ui.confs;

import cn.hutool.json.*;
import io.kurumi.ntt.*;
import io.kurumi.ntt.bots.*;
import io.kurumi.ntt.twitter.*;
import io.kurumi.ntt.ui.*;
import io.kurumi.ntt.ui.request.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class TwitterAcconutsConf extends BaseConf<List<TwiAccount>> {

    public TwitterAcconutsConf(UserBot bot,String name,String key) {
        super(bot,name,key);
    }

    @Override
    public List<TwiAccount> get() {
        
        LinkedList<TwiAccount> accounts = new LinkedList<>();
        
        JSONArray accountIdArray = bot.data.getJSONArray("twitter_accounts");
        
        if (accountIdArray == null) return accounts;
        
        for (Long accountId : accountIdArray.toList(Long.class)) {
            
            TwiAccount account = bot.owner.find(accountId);
            
            if (account != null) {
                
                accounts.add(account);
                
            }

        }
        
        return accounts;
    }

    @Override
    public void set(List<TwiAccount> value) {
        
        JSONArray accountIdArray = new JSONArray();
        
        for (TwiAccount account : value) {
            
            accountIdArray.add(account.accountId);
            
        }
        
        bot.data.put(key,accountIdArray);
        
    }
    
    public static final String INDEX_MAIN = "main";
    public static final String INDEX_SWITCH = "switch";

    @Override
    public void applySetting(AbsSendMsg msg) {
        
        List<TwiAccount> accounts = get();

        String actionMsg = "无";
        
        if (!accounts.isEmpty()) {
            
            actionMsg = accounts.get(0).screenName;
            
            
            if (accounts.size() > 1) {
                
                actionMsg = actionMsg + " ...";
            }
            
        }
        
        DataObject chooseAccountsQuery = createQuery();
        
        chooseAccountsQuery.setindex(INDEX_MAIN);
        
        msg.singleLineButton(name + " : " + actionMsg,chooseAccountsQuery);
        
    }
    
    @Override
    public AbsResuest onCallback(DataObject obj,AtomicBoolean refresh) {
        
        switch(obj.getIndex()) {
            
            case INDEX_MAIN : return chooseAccount(obj,refresh);
            case INDEX_SWITCH : return switchAccount(obj,refresh);
            
        }
        
        return null;
    }

    public AbsResuest chooseAccount(DataObject obj,AtomicBoolean refresh) {
        
        final List<TwiAccount> accounts = get();
        
        final LinkedList<TwiAccount> all = bot.owner.twitterAccounts;

        return new EditMsg(obj.msg(), name) {{
            
            singleLineButton("<< 返回设置",createBackQuery());
            
            for (TwiAccount account : all) {
                
                DataObject switchQuery = createQuery();
                
                switchQuery.setindex(INDEX_SWITCH);
                
                switchQuery.setUser(account);
                
                if (accounts.contains(account)) {
                    
                    switchQuery.put("target",false);
                    singleLineButton("关闭 " + account.name,switchQuery);
                    
                } else {
                    
                    switchQuery.put("target",true);
                    singleLineButton("开启 " + account.name,switchQuery);
                    
                    
                } 
                
            }
            
        }};
        
    }
    
    public AbsResuest switchAccount(DataObject obj,AtomicBoolean refresh) {
        
        TwiAccount account = obj.getUser(bot.owner);
        
        List<TwiAccount> list = get();

        if (obj.getBool("target")) {
            
            list.add(account);
            
        } else {
            
            list.remove(account);
            
        }
        
        return chooseAccount(obj,refresh);
        
    }
        

}
