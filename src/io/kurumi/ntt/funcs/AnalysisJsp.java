package io.kurumi.ntt.funcs;

import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.utils.T;
import io.kurumi.ntt.twitter.TApi;
import io.kurumi.ntt.twitter.TAuth;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import java.util.LinkedList;
import io.kurumi.ntt.twitter.archive.UserArchive;
import io.kurumi.ntt.db.BotDB;

public class AnalysisJsp extends Fragment {

    public static AnalysisJsp INSTANCE = new AnalysisJsp();
    
    @Override
    public boolean onMsg(UserData user,Msg msg) {

        if ("jsp".equals(msg.command())) {

            analysis(user,msg);

            return true;

        }

        return false;

    }

    LinkedList<Long> fos;
    LinkedList<Long> frs;

    void analysis(UserData user,Msg msg) {

        if (T.checkUserNonAuth(user,msg)) return;

        Twitter api = TAuth.get(user.id).createApi();

        try {

            if (fos == null) fos = TApi.getAllFoIDs(api,917716145121009664L);
            if (frs == null) frs = TApi.getAllFrIDs(api,917716145121009664L);

            LinkedList<Long> ifr = TApi.getAllFrIDs(api,api.getId());

            LinkedList<Long> fj = new LinkedList<Long>(ifr);
            fj.retainAll(fos);

            LinkedList<Long> jf = new LinkedList<Long>(ifr);
            jf.retainAll(frs);

            LinkedList<Long> hg = new LinkedList<Long>(fj);
            hg.retainAll(jf);
            
            fj.removeAll(hg);
            
            jf.removeAll(hg);

            StringBuilder result = new StringBuilder();

            result.append("与互相关注 :");

            for (Long id : hg) {

                if (!BotDB.userExists(id)) {

                    BotDB.saveUser(api.showUser(id));

                }
                
                result.append("\n").append(BotDB.getUser(id).urlHtml());

            }

            result.append("\n-----------------------------\n单向关注的 :");

            for (Long id : fj) {

                if (!BotDB.userExists(id)) {

                    BotDB.saveUser(api.showUser(id));

                }

                result.append("\n").append(BotDB.getUser(id).urlHtml());

            }
            
            result.append("\n-----------------------------\n被关注的 :");

            for (Long id : jf) {

                if (!BotDB.userExists(id)) {

                    BotDB.saveUser(api.showUser(id));

                }

                result.append("\n").append(BotDB.getUser(id).urlHtml());

            }
            
            msg.send(result.toString()).html().exec();

            
        } catch (TwitterException e) {}

    }

}
