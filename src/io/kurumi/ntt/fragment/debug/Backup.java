package io.kurumi.ntt.fragment.debug;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.ZipUtil;
import io.kurumi.ntt.Env;
import io.kurumi.ntt.Launcher;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.abs.Msg;

import java.io.File;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import io.kurumi.ntt.fragment.BotFragment;

public class Backup extends Fragment {

    public static Backup INSTANCE = new Backup();
    static Timer timer;

    public static void start() {

        stop();

        Date next = new Date();

        next.setHours(next.getHours() + 1);

        next.setMinutes(0);
        next.setSeconds(0);

        timer = new Timer("NTT Data Backup Task");
        timer.scheduleAtFixedRate(AutoBackupTask.INSTANCE,next,1 * 60 * 60 * 1000);

    }

    public static void stop() {

        if (timer != null) {

            timer.cancel();

            timer = null;

        }

    }

    static void backup(long chatId) {

        try {

            RuntimeUtil.exec(
				"mongodump",
				"-h",Env.getOrDefault("db_address","127.0.0.1") + ":" + Env.getOrDefault("db_port","27017"),
				"-d","NTTools",
				"-o",Env.DATA_DIR.getPath() + "/db"
            ).waitFor();

        } catch (InterruptedException e) {
        }

		File dest = new File(Env.CACHE_DIR,"data.zip");

		FileUtil.del(dest);

        File zip = ZipUtil.zip(Env.DATA_DIR.getPath(),dest.getPath());

        FileUtil.del(Env.DATA_DIR + "/db");

        Launcher.INSTANCE.sendFile(chatId,zip);


    }

	@Override
	public void init(BotFragment origin) {
		
		super.init(origin);

		registerFunction("backup");

	}

	@Override
	public int checkFunction(UserData user,Msg msg,String function,String[] params) {
		
		return PROCESS_THREAD;
		
	}

	@Override
	public void onFunction(UserData user,Msg msg,String function,String[] params) {

		if (!user.developer()) {

			msg.send("Permission Denied").exec();

		}

        backup(msg.chatId());

    }

    public static class AutoBackupTask extends TimerTask {

        public static AutoBackupTask INSTANCE = new AutoBackupTask();

        @Override
        public void run() {

            backup(Env.GROUP);

        }


    }

}
