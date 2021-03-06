package io.kurumi.ntt.maven;

import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.HttpUtil;
import io.kurumi.ntt.Env;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.utils.BotLog;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class MvnDownloader extends Fragment {

    @Override
    public void init(BotFragment origin) {

        super.init(origin);

        registerFunction("mvn");

    }

    @Override
    public int checkFunction(UserData user, Msg msg, String function, String[] params) {

        return PROCESS_ASYNC;

    }

    @Override
    public void onFunction(UserData user, Msg msg, String function, String[] params) {

        if (params.length < 2) {

            msg.invalidParams("groupId", "artifactId", "version").async();

            return;

        }

        String version = params.length == 2 ? "+" : params[2];

        MvnResolver resolver = new MvnResolver();

        MvnArtifact result;

        StringBuilder log = new StringBuilder();

        try {

            result = resolver.resolve(params[0], params[1], version, null, log, null);

        } catch (Exception e) {

            msg.send("解析失败 :\n\n{}", BotLog.parseError(e)).async();

            return;

        }

        msg.send(log.toString()).async();

        File zipFile = new File(Env.CACHE_DIR, "maven_marge/" + result.fileNameZip());

        if (zipFile.isFile()) {

            msg.sendUpdatingFile();

            msg.sendFile(zipFile);

            return;

        }

        Set<MvnArtifact> all = result.marge();

        Msg status = msg.send("解析完成 数量 : " + all.size()).send();

        LinkedList<File> cache = new LinkedList<>();

        Iterator<MvnArtifact> iter = all.iterator();

        for (int index = 0; index < all.size(); index++) {

            MvnArtifact art = iter.next();

            File localFile = new File(Env.CACHE_DIR, "maven/" + art.fileName());

            if (!localFile.isFile()) {

                status.edit("正在下载 : " + art.fileName() + " ( " + (index + 1) + " / " + all.size() + " )").exec();

                HttpUtil.downloadFile(art.path(), localFile);

            }

            cache.add(localFile);

        }

        if (cache.size() == 1) {

            status.delete();

            msg.sendUpdatingFile();

            msg.sendFile(cache.get(0));

            return;

        }

        status.edit("正在打包...").exec();

        ZipUtil.zip(zipFile, false, cache.toArray(new File[cache.size()]));

        msg.delete();

        msg.sendUpdatingFile();

        msg.sendFile(zipFile);

    }

}
