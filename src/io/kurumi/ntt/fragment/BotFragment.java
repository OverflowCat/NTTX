package io.kurumi.ntt.fragment;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.qrcode.QrCodeException;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.log.StaticLog;
import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramException;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SetWebhook;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.GetMeResponse;
import io.kurumi.ntt.Launcher;
import io.kurumi.ntt.db.PointData;
import io.kurumi.ntt.db.PointStore;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.admin.Firewall;
import io.kurumi.ntt.fragment.extra.ShowFile;
import io.kurumi.ntt.fragment.mods.ModuleEnv;
import io.kurumi.ntt.fragment.twitter.TAuth;
import io.kurumi.ntt.fragment.twitter.archive.UserArchive;
import io.kurumi.ntt.fragment.twitter.ext.StatusGetter;
import io.kurumi.ntt.i18n.LocalString;
import io.kurumi.ntt.model.Callback;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.model.Query;
import io.kurumi.ntt.model.request.Send;
import io.kurumi.ntt.utils.Html;
import io.kurumi.ntt.utils.NTT;
import okhttp3.OkHttpClient;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BotFragment extends Fragment implements UpdatesListener, ExceptionHandler {

    public static Timer mainTimer = new Timer();
    public static Timer trackTimer = new Timer();

    public static ExecutorService asyncPool = Executors.newCachedThreadPool();
    public static ExecutorService waitPool = Executors.newFixedThreadPool(3);

    public User me;
    private TelegramBot bot;
    public LinkedList<Fragment> fragments = new LinkedList<>();
    private String token;
    private PointStore point;

    public List<Long> localAdmins = new ArrayList<>();

    @Override
    public TelegramBot bot() {

        return bot;
    }

    public void reload() {

        fragments.clear();

        addFragment(this);

        addFragment(new Firewall());

    }

    public HashMap<String, Fragment> functions = new HashMap<>();

    public HashMap<String, Fragment> adminFunctions = new HashMap<>();

    public HashMap<String, Fragment> payloads = new HashMap<>();

    public HashMap<String, Fragment> adminPayloads = new HashMap<>();

    public HashMap<String, Fragment> points = new HashMap<>();
    public HashMap<String, Fragment> callbacks = new HashMap<>();


    @Override
    public void init(BotFragment origin) {

        super.init(origin);

        registerFunction("cancel");

        registerPoint(POINT_REQUEST_TWITTER);

    }


    @Override
    public void onFunction(UserData user, Msg msg, String function, String[] params) {

        if ("cancel".equals(function)) {

            msg.send(LocalString.get(user).CANCEL).removeKeyboard().failedWith(2333);

            return;

        }

    }

    @Override
    public int checkPoint(UserData user, Msg msg, String point, PointData data) {

        return PROCESS_SYNC;

    }

    @Override
    public void onPoint(final UserData user, Msg msg, String point, PointData data) {

        data.with(msg);

        if ("cancel".equals(msg.command())) {

            if (data.type == 1) {

                clearPrivatePoint(user).onCancel(user, msg);

                msg.send(LocalString.get(user).CANCEL).removeKeyboard().failed(1000);

            } else {

                clearGroupPoint(user).onCancel(user, msg);

                msg.send(LocalString.get(user).CANCEL).removeKeyboard().failed(1000);

            }

            return;

        } else if (POINT_REQUEST_TWITTER.equals(point)) {

            final TwitterRequest request = (TwitterRequest) data;

            if (!msg.hasText() || !msg.text().startsWith("@")) {

                msg.send("请选择 Twitter 账号 (˚☐˚! )/").withCancel().exec(data);

                return;

            }

            String screenName = msg.text().substring(1);

            final TAuth account = TAuth.getById(UserArchive.get(screenName).id);

            if (account == null) {

                clearPrivatePoint(user);

                return;

            }

            clearPrivatePoint(user);

            msg.send("选择了 : " + account.archive().urlHtml() + " (❁´▽`❁)").html().failed(2 * 1000);

            if (request.payload) {

                final String payload = request.originMsg.payload()[0];

                final String[] params = request.originMsg.payload().length > 1 ? ArrayUtil.sub(request.originMsg.payload(), 1, request.originMsg.payload().length) : new String[0];

                int checked = request.fragment.checkTwitterPayload(user, request.originMsg, payload, params, account);

                if (checked == PROCESS_ASYNC) {

                    execute(msg.update, new Runnable() {

                        @Override
                        public void run() {

                            request.fragment.onTwitterPayload(user, request.originMsg, payload, params, account);

                        }

                    });

                } else {

                    request.fragment.onTwitterPayload(user, request.originMsg, payload, params, account);

                }


            } else {

                int checked = request.fragment.checkTwitterFunction(user, request.originMsg, request.originMsg.command(), request.originMsg.fixedParams(), account);

                if (checked == PROCESS_ASYNC) {

                    execute(msg.update, new Runnable() {

                        @Override
                        public void run() {

                            request.fragment.onTwitterFunction(user, request.originMsg, request.originMsg.command(), request.originMsg.fixedParams(), account);

                        }

                    });

                } else {

                    request.fragment.onTwitterFunction(user, request.originMsg, request.originMsg.command(), request.originMsg.fixedParams(), account);

                }

            }

        }

    }

    public void addFragment(Fragment fragment) {

        fragment.init(this);

        fragments.add(fragment);

    }

    public abstract String botName();

    @Override
    public int process(List<Update> updates) {

        for (final Update update : updates) {

            try {

                processAsync(update);

            } catch (Exception e) {

                StaticLog.get(getClass()).error(e, "出错");

            }


        }

        return CONFIRMED_UPDATES_ALL;

    }

    @Override
    public PointStore point() {

        if (point != null) return point;

        synchronized (this) {

            if (point != null) return point;

            point = PointStore.getInstance(this);

            return point;

        }

    }

    public void processAsync(final Update update) {

        final UserData user;

        long targetId = -1;

        if (update.message() != null) {

            if (update.message().chat().type() != Chat.Type.Private) {

                targetId = update.message().chat().id();

            }

            user = UserData.get(update.message().from());

        } else if (update.editedMessage() != null) {

            update.lock.send(null);

            return;

			/*

			 user = UserData.get(update.editedMessage().from());

			 if (update.editedMessage().chat().type() != Chat.Type.Private) {

			 targetId = update.editedMessage().chat().id();

			 }

			 */

        } else if (update.channelPost() != null) {

            user = update.channelPost().from() != null ? UserData.get(update.channelPost().from()) : null;

            targetId = update.channelPost().chat().id();

        } else if (update.editedChannelPost() != null) {

            user = update.editedChannelPost().from() != null ? UserData.get(update.editedChannelPost().from()) : null;

            targetId = update.editedChannelPost().chat().id();

        } else if (update.callbackQuery() != null) {

            user = UserData.get(update.callbackQuery().from());

        } else if (update.inlineQuery() != null) {

            user = UserData.get(update.inlineQuery().from());

        } else user = null;

        for (Fragment f : fragments) if (f.update() && f.onUpdate(user, update)) return;

        if (update.message() != null) {

            final Msg msg = new Msg(this, update.message());

            msg.update = update;

            if (msg.replyTo() != null) msg.replyTo().update = update;

            final PointData privatePoint = point().getPrivate(user.id);
            final PointData groupPoint = point().getGroup(user.id);

            // StaticLog.debug("{} : {}",user.name(),msg.text());

            if (msg.isGroup() && groupPoint != null) {

                final Fragment function = points.containsKey(groupPoint.point) ? points.get(groupPoint.point) : this;

                int checked = function.checkPoint(user, msg, groupPoint.point, groupPoint);

                if (checked == PROCESS_REJECT) return;

                if (checked == PROCESS_ASYNC) {

                    execute(update, new Runnable() {

                        @Override
                        public void run() {

                            function.onPoint(user, msg, groupPoint.point, groupPoint);

                        }

                    });

                } else {

                    function.onPoint(user, msg, groupPoint.point, groupPoint);

                }

            } else if (msg.isPrivate() && privatePoint != null) {

                // if (NTT.checkDropped(user,msg)) return;

                final Fragment function = !points.containsKey(privatePoint.point) || "cancel".equals(msg.command()) ? this : points.get(privatePoint.point);

                int checked = function.checkPoint(user, msg, privatePoint.point, privatePoint);

                if (checked == PROCESS_REJECT) return;

                if (checked == PROCESS_ASYNC) {

                    execute(update, new Runnable() {

                        @Override
                        public void run() {

                            function.onPoint(user, msg, privatePoint.point, privatePoint);

                        }

                    });

                } else {

                    function.onPoint(user, msg, privatePoint.point, privatePoint);

                }

            } else {

                if (msg.isCommand()) {

                    if (NTT.checkDropped(user, msg)) return;

                    if (msg.isStartPayload()) {

                        final String payload = msg.payload()[0];
                        final String[] params = msg.payload().length > 1 ? ArrayUtil.sub(msg.payload(), 1, msg.payload().length) : new String[0];

                        if (isMainInstance() && (Launcher.TD.payloads.containsKey(payload) || Launcher.TD.adminPayloads.containsKey(payload)))
                            return;

                        if (payloads.containsKey(payload)) {

                            final Fragment function = payloads.get(payload);

                            int checked = function.checkPayload(user, msg, payload, params);

                            if (checked == PROCESS_REJECT) return;

                            if (checked == PROCESS_ASYNC) {

                                execute(update, new Runnable() {

                                    @Override
                                    public void run() {

                                        function.onPayload(user, msg, payload, params);

                                    }

                                });

                            } else {

                                function.onPayload(user, msg, payload, params);


                            }

                        } else if ((user.admin() || localAdmins.contains(user.id)) && adminPayloads.containsKey(payload)) {

                            final Fragment function = adminPayloads.get(payload);

                            int checked = function.checkPayload(user, msg, payload, params);

                            if (checked == PROCESS_REJECT) return;

                            if (checked == PROCESS_ASYNC) {

                                execute(update, new Runnable() {

                                    @Override
                                    public void run() {

                                        function.onPayload(user, msg, payload, params);

                                    }

                                });

                            } else {

                                function.onPayload(user, msg, payload, params);

                            }


                        } else {

                            int checked = checkPayload(user, msg, payload, params);

                            if (checked == PROCESS_REJECT) return;

                            if (checked == PROCESS_ASYNC) {

                                execute(update, new Runnable() {

                                    @Override
                                    public void run() {

                                        onPayload(user, msg, payload, params);

                                    }

                                });

                            } else {

                                onPayload(user, msg, payload, params);

                            }

                        }


                    } else {

                        if ((user.admin() || localAdmins.contains(user.id)) && adminFunctions.containsKey(msg.command())) {

                            final Fragment function = adminFunctions.get(msg.command());

                            int checked = function.checkFunction(user, msg, msg.command(), msg.fixedParams());

                            if (checked == PROCESS_REJECT) return;

                            if (checked == PROCESS_ASYNC) {

                                execute(update, new Runnable() {

                                    @Override
                                    public void run() {

                                        function.onFunction(user, msg, msg.command(), msg.fixedParams());

                                    }

                                });

                            } else {

                                function.onFunction(user, msg, msg.command(), msg.fixedParams());

                            }

                        } else {

                            if (isMainInstance() && (Launcher.TD.functions.containsKey(msg.command()) || Launcher.TD.adminFunctions.containsKey(msg.command()))) return;

                            final Fragment function;

                            if (user.admin() && isLauncher()) {

                                ModuleEnv env = ModuleEnv.get(user.id);

                                if (env != null && env.functionIndex.containsKey(msg.command())) {

                                    function = env;

                                } else {

                                    function = functions.containsKey(msg.command()) ? functions.get(msg.command()) : this;

                                }

                            } else {

                                function = functions.containsKey(msg.command()) ? functions.get(msg.command()) : this;

                            }

                            int checked = function.checkFunction(user, msg, msg.command(), msg.fixedParams());

                            if (checked == PROCESS_REJECT) return;

                            if (isLauncher() && !isMainInstance()) {

                                msg.send("警告！这里是旧式实例，已经无法控制，请尽快切换到 @" + Launcher.INSTANCE.me.username() + " :(").async();

                            }

                            if (function != this && function.checkFunctionContext(user, msg, msg.command(), msg.fixedParams()) == FUNCTION_GROUP && !msg.isGroup()) {

                                msg.send(LocalString.get(user).COMMAND_GROUP_ONLY).async();

                                return;

                            }

                            if (function != this && function.checkFunctionContext(user, msg, msg.command(), msg.fixedParams()) == FUNCTION_PRIVATE && !msg.isPrivate()) {

                                execute(update, new Runnable() {

                                    @Override
                                    public void run() {

                                        msg.send(LocalString.get(user).COMMAND_PRIVATE_ONLY).failedWith();


                                    }

                                });


                            } else if (checked == PROCESS_ASYNC) {

                                execute(update, new Runnable() {

                                    @Override
                                    public void run() {

                                        function.onFunction(user, msg, msg.command(), msg.fixedParams());

                                    }

                                });

                            } else {

                                function.onFunction(user, msg, msg.command(), msg.fixedParams());

                            }

                        }
                    }

                } else {

                    for (final Fragment f : fragments) {

                        int checked = f.checkMsg(user, msg);

                        if (checked == PROCESS_ASYNC || checked == PROCESS_ASYNC_REJ || checked == PROCESS_ASYNC_CONTINUE) {

                            execute(update, new Runnable() {

                                @Override
                                public void run() {

                                    f.onMsg(user, msg);

                                }

                            });

                            if (checked == PROCESS_ASYNC_REJ) {

                                return;

                            }

                            if (checked == PROCESS_ASYNC_CONTINUE) {

                                continue;

                            }

                        } else if (checked == PROCESS_REJECT) {

                            return;

                        } else if (checked == PROCESS_CONTINUE) {

                            continue;

                        } else {

                            f.onMsg(user, msg);

                            if (checked == PROCESS_SYNC_REJ) {

                                return;

                            }

                            if (checked == PROCESS_SYNC_CONTINUE) {

                                continue;

                            }

                        }

                    }

                    onFinalMsg(user, msg);

                    update.lock.send(null);

                }

            }

        } else if (update.channelPost() != null) {

            final Msg msg = new Msg(this, update.channelPost());

            msg.update = update;

            if (msg.replyTo() != null) msg.replyTo().update = update;

            for (final Fragment f : fragments) {

                int checked = f.checkChanPost(user, msg);

                if (checked == PROCESS_REJECT) return;
                if (checked == PROCESS_CONTINUE) continue;

                if (checked == PROCESS_ASYNC || checked == PROCESS_ASYNC_REJ || checked == PROCESS_ASYNC_CONTINUE) {

                    execute(update, new Runnable() {

                        @Override
                        public void run() {

                            f.onChanPost(user, msg);

                        }

                    });

                    if (checked == PROCESS_ASYNC_REJ) {

                        return;

                    }

                    if (checked == PROCESS_ASYNC_CONTINUE) {

                        continue;

                    }


                } else {

                    f.onChanPost(user, msg);

                    if (checked == PROCESS_SYNC_REJ) {

                        return;

                    }

                    if (checked == PROCESS_SYNC_CONTINUE) {

                        continue;

                    }

                }

            }

        } else if (update.callbackQuery() != null) {

            final Callback callback = new Callback(this, update.callbackQuery());

            if (NTT.checkDropped(user, callback)) return;

            final String point = callback.params.length == 0 ? "" : callback.params[0];
            final String[] params = callback.params.length > 1 ? ArrayUtil.sub(callback.params, 1, callback.params.length) : new String[0];

            final Fragment function = callbacks.containsKey(point) ? callbacks.get(point) : this;

            int checked = function.checkCallback(user, callback, point, params);

            if (checked == PROCESS_REJECT) return;

            if (checked == PROCESS_ASYNC) {

                execute(update, new Runnable() {

                    @Override
                    public void run() {

                        function.onCallback(user, callback, point, params);


                    }

                });

            } else {

                function.onCallback(user, callback, point, params);

            }

        } else if (update.inlineQuery() != null) {

            Query query = new Query(this, update.inlineQuery());

            query.update = update;

            for (Fragment f : fragments) {

                if (f.query()) f.onQuery(user, query);

            }

        } else if (update.poll() != null) {

            for (Fragment f : fragments) {

                if (f.poll()) f.onPollUpdate(update.poll());

            }

        }

    }

    @Override
    public void onCallback(UserData user, Callback callback, String point, String[] params) {

        if ("null".equals(point)) callback.confirm();
        else callback.alert("Error Callback Point : " + point);

    }

    final String split = "------------------------\n";

    public void onFinalMsg(UserData user, final Msg msg) {

        if (!msg.isPrivate()) return;

        StringBuilder str = new StringBuilder();

        Message message = msg.message();

        str.append("MessageId : " + message.messageId()).append("\n");

        if (message.forwardFrom() != null) {

            str.append("FromUser : ").append(UserData.get(message.forwardFrom()).userName()).append("\n");
            str.append("UserId : ").append(message.forwardFrom().id()).append("\n");

        }

        if (message.forwardFromChat() != null) {

            if (message.forwardFromChat().type() == Chat.Type.channel) {

                str.append("From Cahnnel : ").append(message.forwardFromChat().username() == null ? message.forwardFromChat().title() : Html.a(message.forwardFromChat().username(), "https://t.me/" + message.forwardFromChat().username())).append("\n");

                str.append("Channel Id : ").append(message.forwardFromChat().id());

            } else if (message.forwardFromChat().type() == Chat.Type.group || message.forwardFromChat().type() == Chat.Type.supergroup) {

                str.append("Form Group : ").append(message.forwardFromChat().username() == null ? message.forwardFromChat().title() : Html.a(message.forwardFromChat().username(), "https://t.me/" + message.forwardFromChat().username())).append("\n");

            } else {

                if (message.forwardFrom() == null) {

                    str.append("From : ").append(message.forwardSenderName()).append(" (隐藏来源)\n");

                }

            }

        }

        if (message.forwardSenderName() != null) {

            str.append("Sender Name : ").append(message.forwardSenderName());

        }

        if (message.sticker() != null) {

            str.append(split);

            str.append("Sticker ID : ").append(Html.code(message.sticker().fileId())).append("\n");

            str.append("Sticker Emoji : ").append(Html.code(message.sticker().emoji())).append("\n");

            str.append("Share Link : ").append(ShowFile.createPayload(this, msg.sticker().fileId())).append("\n");

            if (message.sticker().setName() != null) {

                str.append("Sticker Set : ").append("https://t.me/addstickers/" + message.sticker().setName()).append("\n");

            }

            msg.sendUpdatingPhoto();

            bot().execute(new SendPhoto(msg.chatId(), getFile(msg.message().sticker().fileId())).caption(str.toString()).parseMode(ParseMode.HTML).replyMarkup(new ReplyKeyboardRemove()).replyToMessageId(msg.messageId()));

            return;

        }

        if (msg.doc() != null) {

            str.append("File Name : ").append(Html.code(msg.doc().fileName())).append("\n");
            str.append("File ID : ").append(Html.code(msg.doc().fileId())).append("\n");
            //str.append("Share Link : ").append(ShowFile.createPayload(this,msg.doc().fileId())).append("\n");

        }

        if (msg.message().photo() != null) {

            str.append("File ID : ").append(Html.code(msg.maxSize().fileId())).append("\n");

        }

        Long statusId = NTT.parseStatusId(msg.text());

        if (statusId != -1) {

            msg.setFunctionAndParam("status", statusId.toString());

            getInstance(StatusGetter.class).onFunction(user, msg, null, msg.fixedParams());

            return;

        }

        msg.send("{}\n{}", LocalString.get(user).UNPROCESSED, str.toString()).replyTo(msg).html().removeKeyboard().async();

        if (msg.message().photo() != null) {

            execute(msg.update, new Runnable() {

                @Override
                public void run() {

                    try {

                        String result = QrCodeUtil.decode(msg.photo());

                        if (!StrUtil.isBlank(result)) {

                            msg.send("二维码解析结果 :\n{}", Html.code(result)).html().async();

                        }

                    } catch (QrCodeException ex) {
                    }

                }
            });

        }

    }

    public boolean isLongPulling() {

        return false;

    }

    public abstract String getToken();

    public boolean silentStart() throws Exception {

        reload();

        token = getToken();

        bot = new TelegramBot.Builder(token).build();

        GetMeResponse resp = bot.execute(new GetMe());

        if (resp.errorCode() == 401) {

            throw new Exception(resp.errorCode() + " : " + resp.description());

        } else if (!resp.isOk()) return false;

        me = resp.user();

        realStart();

        return true;

    }

    public void init() {

        reload();

        token = getToken();

        OkHttpClient.Builder okhttpClient = new OkHttpClient.Builder();

        okhttpClient.networkInterceptors().clear();

        bot = new TelegramBot.Builder(token).okHttpClient(okhttpClient.build()).build();

    }

    public void start() throws Exception {

        init();

        me = bot.execute(new GetMe()).user();

        realStart();

    }

    public void realStart() {

        // bot.execute(new DeleteWebhook());

        if (isLongPulling()) {

            bot.setUpdatesListener(this, this);

        } else {

			/*

			 GetUpdatesResponse update = bot.execute(new GetUpdates());

			 if (update.isOk()) {

			 process(update.updates());

			 }

			 */

            final String url = "https://" + BotServer.INSTANCE.domain + "/" + token;

            BotServer.fragments.put(token, this);

            BaseResponse resp = bot.execute(new SetWebhook().url(url));

            if (!resp.isOk()) {

                BotServer.fragments.remove(token);

            }

            if (me != null) BotServer.idIndex.put(me.id(), this);

        }

    }

    public void stop() {

        for (Fragment f : fragments) f.onStop();

        for (Long id : new HashMap<>(point().privatePoints).keySet()) {

            UserData user = UserData.get(id);

            clearPrivatePoint(user);

            new Send(this, id, LocalString.get(UserData.get(id)).FORCE_CANCEL).removeKeyboard().exec();

        }

        if (!isLongPulling()) {

            // bot.execute(new DeleteWebhook());

        } else {

            bot.removeGetUpdatesListener();

        }

    }

    @Override
    public void onException(TelegramException e) {

        StaticLog.warn(e.getMessage());

    }


}
