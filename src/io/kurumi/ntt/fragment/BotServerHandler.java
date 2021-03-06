package io.kurumi.ntt.fragment;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.*;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.log.StaticLog;
import com.pengrad.telegrambot.BotUtils;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.DeleteWebhook;
import io.kurumi.ntt.Env;
import io.kurumi.ntt.Launcher;
import io.kurumi.ntt.fragment.twitter.TAuth;
import io.kurumi.ntt.fragment.twitter.archive.UserArchive;
import io.kurumi.ntt.model.request.Send;
import io.kurumi.ntt.utils.BotLog;
import io.kurumi.ntt.utils.Html;
import io.kurumi.ntt.utils.NTT;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import twitter4j.TwitterException;
import twitter4j.User;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.util.Date;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class BotServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private String tug_domain = "https://get-twi.me/";

    private FullHttpRequest request;

    String index() {

        return FileUtil.readUtf8String(new File(Env.ROOT_DIR, "res/twi-get/index.html"));

    }

    String error(String message) {

        return StrUtil.format(FileUtil.readUtf8String(new File(Env.ROOT_DIR, "res/twi-get/error.html")), message);

    }

    String result(String title, String message) {

        return StrUtil.format(FileUtil.readUtf8String(new File(Env.ROOT_DIR, "res/twi-get/result.html")), title, message);

    }

    public void channelRead1(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        String uri = URLUtil.decode(request.uri().substring(4));

        if (uri.length() < 2 || (!uri.startsWith("?screenName=") && !NumberUtil.isLong(uri))) {

            sendHtml(ctx, index());

            return;

        } else if (uri.startsWith("?screenName=")) {

            String screenName = StrUtil.subAfter(uri, "?screenName=", false);

            if (screenName.contains("&")) screenName = StrUtil.subBefore(screenName, "&", false);

            screenName = NTT.parseScreenName(screenName);

            try {

                User user = TAuth.next().createApi().showUser(screenName);

                UserArchive.save(user);

                sendRedirect(ctx, tug_domain + user.getId());

                return;

            } catch (TwitterException ex) {

                if (UserArchive.contains(screenName)) {

                    sendRedirect(ctx, tug_domain + UserArchive.get(screenName).id);

                } else {

                    sendHtml(ctx, error(NTT.parseTwitterException(ex)));

                }

                return;

            }

        } else {

            UserArchive archive;
            TwitterException err;

            try {

                User user = TAuth.next().createApi().showUser(NumberUtil.parseLong(uri));

                archive = UserArchive.save(user);

                err = null;

            } catch (TwitterException ex) {

                err = ex;

                archive = UserArchive.get(NumberUtil.parseLong(uri));

            }

            String message = "";

            if (archive != null) {

                message += "<img src=\"" + archive.photoUrl + "\"></img><br />";

                message += "<br />" + HtmlUtil.escape(archive.name);

                if (archive.isProtected) message += Html.b(" [ 锁推 ] ");

                if (err != null) {

                    message += " 「 " + Html.b(NTT.parseTwitterException(err)) + " 」";

                }

                message += "<br />";

                if (archive.nameHistory != null && !archive.nameHistory.isEmpty()) {

                    message += "<br />" + Html.b("历史名称") + " : ";

                    for (String name : archive.nameHistory) {

                        message += "<br />" + Html.code(name);

                    }

                    message += "<br />";

                }

                if (!StrUtil.isBlank(archive.bio)) {

                    message += "<br />" + Html.b("BIO") + " : " + HtmlUtil.escape(archive.bio) + "<br />";

                }

                if (archive.followers != null) {

                    message += "<br />" + archive.following + " 正在关注" + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + archive.followers + " 关注者";

                }

                if (archive.statuses != null) {

                    message += "<br />" + archive.statuses + " 条推文" + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + archive.likes + " 个打心<br />";

                }

                if (archive.url != null) {

                    message += "<br />" + Html.b("个人链接") + " : " + Html.a(archive.url);

                }

                if (archive.snHistory != null && !archive.snHistory.isEmpty()) {

                    message += "<br />" + Html.b("历史用户名") + " :";

                    for (String sn : archive.snHistory) {

                        message += Html.b(" @" + sn);

                    }

                }

                if (archive.lang != null) {

                    message += "<br />" + Html.b("语言") + " : " + archive.lang;

                }

                if (!StrUtil.isBlank(archive.location)) {

                    message += "<br />" + Html.b("位置") + " : " + HtmlUtil.escape(archive.location);

                }

                message += "<br />" + Html.b("加入时间") + " : " + DateUtil.formatChineseDate(new Date(archive.createdAt), false);

                if (archive.isDisappeared && archive.disappearedAt != null) {

                    message += "<br />" + Html.b("消失时间") + " : " + DateUtil.formatChineseDate(new Date(archive.disappearedAt), false);

                }

                message += "<br />" + Html.b("用户链接") + " : " + Html.twitterUser("@" + archive.screenName, archive.screenName) + "<br />";
            }

            message += Html.b("永久链接") + " : " + Html.a("长按复制", tug_domain + uri);

            sendHtml(ctx, result(archive == null ? "Twitter User Getway" : archive.name, message));

            return;

        }

    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        this.request = request;

        // StaticLog.debug("收到HTTP请求 : {}",request.uri());

        if (new File("/etc/ntt/safe").isFile()) {

            sendOk(ctx);

            return;

        }

        if (!request.decoderResult().isSuccess()) {

            sendError(ctx, BAD_REQUEST);

            return;

        }

        if (request.uri().startsWith("/tug")) {

            channelRead1(ctx, request);

            return;

        } else if (request.uri().equals("/api")) {

            if (request.getMethod() != POST) {

                sendError(ctx, BAD_REQUEST);

                return;

            }

            JSONObject json;

            try {

                json = new JSONObject(request.content().toString(CharsetUtil.CHARSET_UTF_8));

            } catch (Exception ex) {

                sendError(ctx, BAD_REQUEST);

                return;

            }

            FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(RpcApi.execute(json).toStringPretty(), CharsetUtil.CHARSET_UTF_8));

            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");

            boolean keepAlive = HttpUtil.isKeepAlive(request);

            if (!keepAlive) {

                ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);

            } else {

                resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

                ctx.writeAndFlush(resp);

            }

            return;

        }

        if (Launcher.INSTANCE != null && request.uri().equals("/data/" + Launcher.INSTANCE.getToken())) {

            File dataFile = new File(Env.CACHE_DIR, "data.zip");

            if (!dataFile.isFile()) {

                sendError(ctx, NOT_FOUND);

                return;

            }

            FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, OK);

            MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();

            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypesMap.getContentType(dataFile));
            resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, dataFile.length());

            final boolean keepAlive = HttpUtil.isKeepAlive(request);

            if (!keepAlive) {

                resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

            } else if (request.protocolVersion().equals(HTTP_1_0)) {

                resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

            }

            ctx.write(resp);

            ChannelFuture last = ctx.writeAndFlush(new ChunkedFile(dataFile)).addListener(ChannelFutureListener.CLOSE);

            if (!keepAlive) {

                last.addListener(ChannelFutureListener.CLOSE);

            }

            return;

        } else if (Launcher.INSTANCE != null && request.uri().equals("/upgrade/" + Launcher.INSTANCE.getToken())) {

            try {

                JSONObject json = new JSONObject(request.content().toString(CharsetUtil.CHARSET_UTF_8));

                if (!"refs/heads/master".equals(json.getStr("ref"))) {

                    sendOk(ctx);

                    return;

                }

            } catch (Exception ex) {
            }

            sendOk(ctx);

            new Thread() {

                @Override
                public void run() {

                    new Send(Env.LOG_CHANNEL, "Bot Update Executed : By WebHook").exec();

                    try {

                        String str = RuntimeUtil.execForStr("bash update.sh");

                        new Send(Env.LOG_CHANNEL, str).exec();

                        Launcher.INSTANCE.stop();

                    } catch (Exception e) {

                        new Send(Env.LOG_CHANNEL, BotLog.parseError(e)).exec();

                    }

                    RuntimeUtil.exec("service mongod restart");

                    RuntimeUtil.exec("service ntt restart");

                }


            }.start();

            return;

        } else if (request.getMethod() != POST || request.uri().length() <= 1) {

            sendError(ctx, NOT_FOUND);

            return;

        }

        String botToken = request.uri().substring(1);

        if (!BotServer.fragments.containsKey(botToken)) {

            // StaticLog.debug("未预期的消息 : {}",request.content().toString(CharsetUtil.CHARSET_UTF_8));

            FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(new DeleteWebhook().toWebhookResponse(), CharsetUtil.CHARSET_UTF_8));

            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");

            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);

            return;

        }

        BaseRequest webhookResponse;

        ProcessLock<BaseRequest> lock = new ProcessLock<>();

        Update update = BotUtils.parseUpdate(request.content().toString(CharsetUtil.CHARSET_UTF_8));

        update.lock = lock;

        try {

            BotServer.fragments.get(botToken).processAsync(update);

            webhookResponse = lock.waitFor();

        } catch (Exception ex) {

            StaticLog.error("出错 (同步) \n\n{}\n\n{}", new JSONObject(update.json).toStringPretty(), BotLog.parseError(ex));

            webhookResponse = null;

            //sendError(ctx,INTERNAL_SERVER_ERROR);

        }

        if (webhookResponse == null) {

            sendOk(ctx);

        } else {

            // System.out.println(webhookResponse.toWebhookResponse());

            FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(webhookResponse.toWebhookResponse(), CharsetUtil.CHARSET_UTF_8));

            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");

            boolean keepAlive = HttpUtil.isKeepAlive(request);

            if (!keepAlive) {

                ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);

            } else {

                resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

                ctx.writeAndFlush(resp);

            }

        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        cause.printStackTrace();

        if (ctx.channel().isActive()) {

            sendError(ctx, INTERNAL_SERVER_ERROR);

        }

    }

    void sendOk(ChannelHandlerContext ctx) {

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);

        this.sendAndCleanupConnection(ctx, response);

    }

    void sendOk(ChannelHandlerContext ctx, String content) {

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content, CharsetUtil.CHARSET_UTF_8));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");

        this.sendAndCleanupConnection(ctx, response);

    }

    void sendHtml(ChannelHandlerContext ctx, String content) {

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content, CharsetUtil.CHARSET_UTF_8));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

        this.sendAndCleanupConnection(ctx, response);

    }

    void sendRedirect(ChannelHandlerContext ctx, String newUri) {

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);

        response.headers().set(HttpHeaderNames.LOCATION, newUri);

        this.sendAndCleanupConnection(ctx, response);

    }

    void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status);

        this.sendAndCleanupConnection(ctx, response);


    }

    void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer(content, CharsetUtil.CHARSET_UTF_8));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        this.sendAndCleanupConnection(ctx, response);

    }

    void sendAndCleanupConnection(ChannelHandlerContext ctx, FullHttpResponse response) {

        final FullHttpRequest request = this.request;

        final boolean keepAlive = HttpUtil.isKeepAlive(request);

        HttpUtil.setContentLength(response, response.content().readableBytes());

        if (!keepAlive) {

            // We're going to close the connection as soon as the response is sent,
            // so we should also make it clear for the client.

            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        } else if (request.protocolVersion().equals(HTTP_1_0)) {

            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        }

        ChannelFuture flushPromise = ctx.writeAndFlush(response);

        if (!keepAlive) {

            // Close the connection as soon as the response is sent.

            flushPromise.addListener(ChannelFutureListener.CLOSE);

        }
    }

}
