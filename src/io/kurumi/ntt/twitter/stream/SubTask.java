package io.kurumi.ntt.twitter.stream;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONArray;
import io.kurumi.ntt.db.BotDB;
import io.kurumi.ntt.funcs.StatusUI;
import io.kurumi.ntt.model.request.Send;
import io.kurumi.ntt.twitter.TAuth;
import io.kurumi.ntt.twitter.archive.StatusArchive;
import io.kurumi.ntt.twitter.track.UTTask;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.StatusAdapter;
import twitter4j.StatusDeletionNotice;
import twitter4j.Twitter;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import cn.hutool.json.*;
import io.kurumi.ntt.db.*;
import twitter4j.*;

public class SubTask extends StatusAdapter {

	public static JSONArray enable = SData.getJSONArray("data","stream",true);

	public static void save() {

		SData.setJSONArray("data","stream",enable);

	}

    long userId;
    long tid;
    Twitter api;

    public SubTask(long userId,long tid,Twitter api) {

        this.userId = userId;
        this.tid = tid;
        this.api = api;

    }

   // public static AtomicBoolean needReset = new AtomicBoolean(true);

    static ExecutorService statusProcessPool = Executors.newFixedThreadPool(3);

	/*
	
    static TimerTask resetTask = new TimerTask() {

        @Override
        public void run() {

            if (needReset.getAndSet(false)) {

                resetStream();

            }

        }

    };



    static Timer timer;

    public static void start() {

        stop();

       
	   timer = new Timer("NTT TwitterStream Task");

        Date start = new Date();

        start.setMinutes(start.getMinutes() + 5);

		resetTask.run();
		
		
		
		resetStream();
		
       // timer.schedule(resetTask,start,5 * 60 * 1000);

    }
	
	



    public static void stop() {

        if (timer != null) timer.cancel();
        timer = null;

    }
	
	*/
	
	public static void start() {
		
		resetStream();
		
	}

	public static void start(long userId) {

		stop(userId);

		TwitterStream stream = new TwitterStreamFactory(TAuth.get(userId).createConfig()).getInstance();

		stream.addListener(new SubTask(userId,TAuth.get(userId).accountId,TAuth.get(userId).createApi()));
		
		userStream.put(userId,stream);
		
		stream.filter(new FilterQuery().follow(new long[] { TAuth.get(userId).accountId }));

	}

	public static void stop(Long userId) {

		TwitterStream stream = userStream.remove(userId);

		if (stream != null) stream.cleanUp();

	}
	
	public static void stopAll() {
		
		TwitterStreamImpl.shutdownAll();
		
	}

    static HashMap<Long,TwitterStream> userStream = new HashMap<>();

	/*

	 static HashMap<Long,List<Long>> currentSubs = new HashMap<>();

	 static void resetStream() {

	 HashMap<Long,List<Long>> newSubs = new HashMap<>();

	 synchronized (UTTask.subs) {

	 for (Map.Entry<String,Object> sub : UTTask.subs.entrySet()) {

	 newSubs.put(Long.parseLong(sub.getKey()),((JSONArray)sub.getValue()).toList(Long.class));

	 }

	 }

	 for (Map.Entry<Long,List<Long>> sub : newSubs.entrySet()) {

	 Long userId = sub.getKey();

	 if (!enable.getBool(userId.toString(),false)) continue;

	 if (currentSubs.containsKey(userId)) {

	 if (currentSubs.get(userId).equals(sub.getValue())) {

	 continue;

	 }

	 }

	 if (TAuth.exists(userId)) {

	 TwitterStream stream = new TwitterStreamFactory(TAuth.get(userId).createConfig()).getInstance();

	 stream.addListener(new SubTask(userId,TAuth.get(userId).accountId,TAuth.get(userId).createApi()));

	 TwitterStream removed = userStream.put(userId,stream);
	 if (removed != null) removed.cleanUp();

	 stream.filter(new FilterQuery().follow(ArrayUtil.unWrap(sub.getValue().toArray(new Long[sub.getValue().size()]))));

	 }

	 }

	 currentSubs.clear();
	 currentSubs.putAll(newSubs);

	 }

	 */

	static void resetStream() {

		synchronized (enable) {

			for (Long userId : enable.toList(Long.class)) {

				if (userStream.containsKey(userId)) continue;

				start(userId);
								// stream.filter(new FilterQuery().follow(ArrayUtil.unWrap(sub.getValue().toArray(new Long[sub.getValue().size()]))));

			}

		}

	}

    @Override
    public void onStatus(final Status status) {

        statusProcessPool.execute(new Runnable() {

                @Override
                public void run() {

                    processStatus(status,userId,tid,api);

                }

            });

    }

    @Override
    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

        statusDeletionNotice.getStatusId();
		statusDeletionNotice.getUserId();

    }

    static void processStatus(Status status,Long userId,Long tid,Twitter api) {

        // List<Long> userSub = currentSubs.get(userId);

        if (status.getRetweetedStatus() != null) {

            // 忽略 无关转推 (可能是大量的)

			// return;

        }

        long from = status.getUser().getId();

		//   if (from == tid) return; // 去掉来自自己的推文？

        StatusArchive archive = BotDB.saveStatus(status).loop(api);

        Send send = new Send(userId,archive.toHtml(0)).html();

		if (archive.inReplyToStatusId != -1 || archive.quotedStatusId != -1) {

			send.buttons(StatusUI.INSTANCE.makeShowButton(status.getId()));

		}
		
		send.exec();

    }

}
