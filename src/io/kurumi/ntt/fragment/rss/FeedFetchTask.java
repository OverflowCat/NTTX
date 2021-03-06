package io.kurumi.ntt.fragment.rss;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.pengrad.telegrambot.request.GetChat;
import com.pengrad.telegrambot.response.GetChatResponse;
import com.pengrad.telegrambot.response.SendResponse;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import io.kurumi.ntt.Launcher;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.bots.UserBotFragment;
import io.kurumi.ntt.model.request.Send;
import io.kurumi.ntt.utils.Html;

import java.io.*;
import java.util.*;

public class FeedFetchTask extends TimerTask {

    public static Timer rssTimer = new Timer();

    public static FeedFetchTask INSTANCE = new FeedFetchTask();

    public static void start() {

        rssTimer.schedule(INSTANCE, new Date(), 5 * 60 * 1000);

        for (RssSub.ChannelRss info : RssSub.channel.getAll()) {

            BotFragment bot = Launcher.INSTANCE;

            if (info.fromBot != null) {

                if (!UserBotFragment.bots.containsKey(info.fromBot)) {

                    info.fromBot = null;

                } else {

                    bot = UserBotFragment.bots.get(info.fromBot);

                }

            }

            GetChatResponse resp = bot.execute(new GetChat(info.id));

            if (resp.errorCode() == 403) {

                RssSub.channel.deleteById(info.id);

            }

        }

    }

    public static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64;) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157";

    int step = 0;

    static boolean first = true;

    @Override
    public void run() {

        Set<String> sites = new HashSet<>();
        Set<String> errors = new HashSet<>();

        for (RssSub.ChannelRss info : RssSub.channel.getAll()) {

            if (info.delay == null) info.delay = 40L;

            if (first || info.last == null || (System.currentTimeMillis() - info.last) > info.delay * 60 * 1000) {

                info.last = System.currentTimeMillis();

                RssSub.channel.setById(info.id, info);

            } else {

                continue;

            }

            sites.addAll(info.subscriptions);

            if (!first && info.error != null) {

                for (Map.Entry<String, RssSub.ChannelRss.FeedError> error : info.error.entrySet()) {

                    if (System.currentTimeMillis() - error.getValue().startAt > 6 * 60 * 60 * 1000) {

                        errors.add(error.getKey());

                    }

                }

            }

        }

        if (step < 9) {

            step++;

            // sites.removeAll(errors);

            if (step != 2) {


            }

        } else {

            step = 0;

        }

        // BotLog.error("FETCHING : \n\n" + ArrayUtil.join(sites.toArray(),"\n"));

        next:
        for (String url : sites) {

            try {

                SyndFeedInput input = new SyndFeedInput();

                HttpResponse resp;

                try {

                    resp = HttpUtil.createGet(URLUtil.encode(url)).header(Header.USER_AGENT, "NTT Feed Fetcher ( https://github.com/HiedaNaKan/NTTools)").execute();

                } catch (HttpException ex) {

                    fetchError(url, ex);

                    continue;

                }

                if (!resp.isOk()) {

                    StringBuilder error = new StringBuilder();

                    error.append("HTTP ERROR ").append(resp.getStatus());

                    String content = resp.body();

                    if (!StrUtil.isBlank(content)) {

                        error.append(" : \n\n");

                        error.append(content);

                    }

                    fetchError(url, new Exception(error.toString()));

                    continue;

                }

                SyndFeed feed = input.build(new StringReader(resp.body()));

                RssSub.RssInfo info = RssSub.info.getById(url);

                //BotLog.debug("拉取 " + feed.getTitle());

                for (RssSub.ChannelRss channel : RssSub.channel.findByField("subscriptions", url)) {

                    if (channel.error != null) {

                        if (channel.error.remove(url) != null) {

                            if (channel.error.isEmpty()) channel.error = null;

                            RssSub.channel.setById(channel.id, channel);

                        }

                    }

                }

                if (info == null) {

                    info = new RssSub.RssInfo();

                    info.id = url;
                    info.title = feed.getTitle();
                    info.link = feed.getLink();
                    info.last = generateSign(feed.getEntries().get(0));

                    RssSub.info.setById(info.id, info);

                    continue next;

                }


                LinkedList<SyndEntry> posts = new LinkedList<>();

                for (SyndEntry entry : feed.getEntries()) {

                    if (generateSign(entry).equals(info.last)) {

                        break;

                    }

                    posts.add(entry);

                }

                info.title = feed.getTitle();
                info.link = feed.getLink();
                info.last = generateSign(feed.getEntries().get(0));

                RssSub.info.setById(info.id, info);

                if (posts.isEmpty()) {

                    continue next;

                }

                Collections.reverse(posts);

                for (RssSub.ChannelRss channel : RssSub.channel.findByField("subscriptions", info.id)) {

                    for (SyndEntry entry : posts) {

                        Fragment sender = Launcher.INSTANCE;

                        if (Launcher.INSTANCE.me.id().equals(channel.fromBot)) {

                            channel.fromBot = null;

                            RssSub.channel.setById(channel.id, channel);

                        }

                        if (channel.fromBot != null && UserBotFragment.bots.containsKey(channel.fromBot)) {

                            sender = UserBotFragment.bots.get(channel.fromBot);

                        }

                        Send request = new Send(sender, channel.id, FeedHtmlFormater.format(channel, feed, entry));

                        if (channel.format > 8 || channel.preview) {

                            request.enableLinkPreview();

                        }

                        SendResponse result = request.html().exec();

                        if (result != null && !result.isOk() && result.description().contains("not found")) {

                            if (channel.fromBot != null) {

                                channel.fromBot = null;

                                RssSub.channel.setById(channel.id, channel);

                            }

                        }

                        ThreadUtil.sleep(1000);

                    }

                }

            } catch (FeedException e) {

                fetchError(url, e);

            } catch (IllegalArgumentException e) {
            }


        }

        first = false;


    }

    void fetchError(String url, Exception e) {

        RssSub.RssInfo info = RssSub.info.getById(url);

        for (RssSub.ChannelRss channel : RssSub.channel.findByField("subscriptions", info.id)) {

            if (channel.error == null) {

                channel.error = new HashMap<>();

            }

            RssSub.ChannelRss.FeedError error;

            if (channel.error.containsKey(url)) {

                error = channel.error.get(url);

            } else {

                error = new RssSub.ChannelRss.FeedError();

                error.startAt = System.currentTimeMillis();

                error.errorMsg = e.getMessage();

            }

            if (System.currentTimeMillis() - error.startAt > 3 * 24 * 60 * 60 * 1000) {

                channel.subscriptions.remove(url);
                channel.error.remove(url);

                if (channel.error.isEmpty()) channel.error = null;

                new Send(channel.id, Html.b(info.title) + " 连续三天拉取错误，已取消订阅 (" + error.errorMsg).async();

            }

        }


    }

    static String generateSign(SyndEntry entry) {

        StringBuilder str = new StringBuilder();

        if (entry.getPublishedDate() != null) {

            str.append(entry.getPublishedDate().getTime());

        }

        str.append(entry.getTitle());

        if (!StrUtil.isBlank(entry.getLink())) {

            str.append(entry.getLink());

        }

        return DigestUtil.md5Hex(str.toString());

    }

}
