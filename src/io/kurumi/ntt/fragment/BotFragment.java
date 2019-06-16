package io.kurumi.ntt.fragment;

import com.pengrad.telegrambot.*;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.*;
import io.kurumi.ntt.*;
import io.kurumi.ntt.db.*;
import io.kurumi.ntt.fragment.abs.*;
import io.kurumi.ntt.fragment.abs.request.*;
import io.kurumi.ntt.utils.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import okhttp3.*;
import io.kurumi.ntt.fragment.Fragment.*;

public abstract class BotFragment extends Fragment implements UpdatesListener {

	static ExecutorService asyncPool = Executors.newFixedThreadPool(5);
	static LinkedBlockingQueue<UserAndUpdate> queue = new LinkedBlockingQueue<>();
	static LinkedList<ProcessThread> threads = new LinkedList<>();

    public User me;
    private TelegramBot bot;
    private LinkedList<Fragment> fragments = new LinkedList<>();
    private String token;
    private PointStore point;

	public static void startThreads(int count) {

		for (int index = 0;index < count;index ++) {

			threads.add(new ProcessThread() {{ start(); }});

		}

	}

	public static void stopThreads() {

		for (ProcessThread thread : threads) {

			thread.stopped.set(true);

			thread.interrupt();

		}

	}

	class UserAndUpdate {

		long chatId;

		UserData user;

		Update update;

		BotFragment.Processed process() {

			for (final Fragment fragmnet : fragments) {

				Fragment.Processed processed =  fragmnet.onAsyncUpdate(user, update);

				if (processed != null) return processed;

			}

			return null;

		}

	}

	public BotFragment() {

        origin = this;

	}


    @Override
    public TelegramBot bot() {

        return bot;
    }

    public void reload() {

		fragments.clear();

		addFragment(this);

    }

    public void addFragment(Fragment fragment) {

        fragment.origin = this;
        fragments.add(fragment);


    }

    /*

     public boolean isLongPulling() {

     return false;

     }

     */

    public void remFragment(Fragment fragment) {

        fragments.remove(fragment);


    }

    public abstract String botName();

    @Override
    public int process(List<Update> updates) {

        for (final Update update : updates) {

            try {

                processAsync(update);

            } catch (Exception e) {

                BotLog.error("更新出错", e);

                Launcher.INSTANCE.uncaughtException(Thread.currentThread(), e);

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

    @Override
    public boolean onMsg(UserData user, Msg msg) {

        if ("cancel".equals(msg.command())) {

            msg.send("你要取消什么？ >_<").exec();

            return true;

        }

        return false;

    }

    @Override
    public boolean onPointedMsg(UserData user, Msg msg) {

        if ("cancel".equals(msg.command())) {

            clearPoint(user);

            msg.send("取消成功 ~").removeKeyboard().exec();

            return true;

        }

        return false;

    }

    public void processAsync(final Update update) {

        final UserData user;

		long targetId = -1;

        if (update.message() != null) {

            user = UserData.get(update.message().from());

			if (update.message().chat().type() != Chat.Type.Private) {

				targetId = update.message().chat().id();

			}

        } else if (update.editedMessage() != null) {

			user = UserData.get(update.editedMessage().from());

			if (update.editedMessage().chat().type() != Chat.Type.Private) {

				targetId = update.editedMessage().chat().id();

			}

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

		UserAndUpdate uau = new UserAndUpdate();

		uau.chatId = targetId;
		uau.user = user;
		uau.update = update;

		queue.add(uau);


    }

	static class ProcessThread extends Thread {

		static LinkedList<Long> processing = new LinkedList<>();

		AtomicBoolean stopped = new AtomicBoolean(false);

		@Override
		public void run() {

			while (!stopped.get()) {

				UserAndUpdate uau;

				try {

					uau = queue.take();

				} catch (InterruptedException e) {

					continue;

				}

				if (uau.user == null && uau.chatId == -1) {

					try {

						Fragment.Processed processed = uau.process();

						if (processed != null) asyncPool.execute(processed);

					} catch (Exception e) {

						new Send(Env.GROUP, "处理中出错 " + uau.update.toString(), BotLog.parseError(e)).exec();

						if (uau.user != null && !uau.user.developer()) {

							new Send(uau.user.id, "处理出错，已提交报告，可以到官方群组 @NTTDiscuss  继续了解").exec();

						}

					}

					continue;

				}

				synchronized (processing) {

					if ((uau.chatId != -1 && processing.contains(uau.chatId)) || (uau.user != null && processing.contains(uau.user.id))) {

						queue.add(uau);

						continue;

					} else {

						if (uau.chatId != -1) {

							processing.add(uau.chatId);

						}

						if (uau.user != null) {

							processing.add(uau.user.id);

						}

					}

				}


				try {

					Fragment.Processed processed = uau.process();

					if (processed != null) asyncPool.execute(processed);

				} catch (Exception e) {

					new Send(Env.GROUP, "处理中出错 " + uau.update.toString(), BotLog.parseError(e)).exec();

					if (uau.user != null && !uau.user.developer()) {

						new Send(uau.user.id, "处理出错，已提交报告，可以到官方群组 @NTTDiscuss  继续了解").exec();

					}

				}

				synchronized (processing) {

					if (uau.chatId != -1) {

						processing.remove(uau.chatId);

					}

					if (uau.user != null) {

						processing.remove(uau.user.id);

					}
				}

			}

		}

	}

	public boolean isLongPulling() {

		return false;

	}

	public String getToken() {

		return Env.get("token." + botName());

	}

	public void setToken(String botToken) {

		Env.set("token." + botName(), token);

	}

	public boolean silentStart() {

		reload();
		
		token = getToken();

		bot = new TelegramBot.Builder(token).build();

		GetMeResponse resp = bot.execute(new GetMe());

		if (resp == null || !resp.isOk()) return false;

		me = resp.user();

		realStart();

		return true;

	}

	public void start() {

		reload();
		
		token = getToken();

		if (token == null || !Env.verifyToken(token)) {

			token = Env.inputToken(botName());

		}

		setToken(token);

		OkHttpClient.Builder okhttpClient = new OkHttpClient.Builder();

		okhttpClient.networkInterceptors().clear();

		bot = new TelegramBot.Builder(token)
			.okHttpClient(okhttpClient.build()).build();

		me = bot.execute(new GetMe()).user();

		realStart();

	}

	public void realStart() {

		bot.execute(new DeleteWebhook());

		if (isLongPulling()) {

			bot.setUpdatesListener(this, new GetUpdates());

		} else {

			/*

			 GetUpdatesResponse update = bot.execute(new GetUpdates());

			 if (update.isOk()) {

			 process(update.updates());

			 }

			 */

			String url = "https://" + BotServer.INSTANCE.domain + "/" + token;

			BotServer.fragments.put(token, this);

			BaseResponse resp = bot.execute(new SetWebhook().url(url));

			BotLog.debug("SET WebHook for " + botName() + " : " + url);

			if (!resp.isOk()) {

				BotLog.debug("Failed... : " + resp.description());

				BotServer.fragments.remove(token);

			}


		}

	}

	public void stop() {

		if (!isLongPulling()) {

			bot.execute(new DeleteWebhook());

		} else {

			bot.removeGetUpdatesListener();

		}

		BotLog.info(botName() + " 已停止 :)");

	}


}
