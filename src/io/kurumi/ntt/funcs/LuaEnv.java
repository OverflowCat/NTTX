package io.kurumi.ntt.funcs;

import cn.hutool.core.util.*;
import io.kurumi.ntt.*;
import io.kurumi.ntt.db.*;
import io.kurumi.ntt.fragment.*;
import io.kurumi.ntt.model.*;
import io.kurumi.ntt.utils.*;
import java.io.*;
import java.util.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

public class LuaEnv extends Fragment {

	public static LuaEnv INSTANCE = new LuaEnv();

	public LuaTable env;
	public LuaTable functions;

	public Globals lua; { reset(); }

	void reset() {

		lua = JsePlatform.standardGlobals();

		env = lua.get("_G").checktable();

		functions = new LuaTable();

		env.set("this",new JavaInstance(Launcher.INSTANCE));

		env.set("functions",functions);

		new BindLib().install();

		lua.loadfile("init.lua").call();

	}

	@Override
	public boolean onMsg(UserData user,Msg msg) {

		if (!msg.isCommand()) return false;

		LuaValue func = functions.get(msg.commandName());

		System.out.println(func);

		if (func.isfunction()) {

			try {

				Varargs result = func.invoke(LuaValue.varargsOf(new JavaInstance(user),new JavaInstance(msg)));

				StringBuilder reply = new StringBuilder();

				for (int index = 0;index < result.narg();index ++) {

					reply.append(result.arg(index + 1));

				}

				msg.send(reply.toString()).exec();


			} catch (Throwable err) {

				ByteArrayOutputStream out = new ByteArrayOutputStream();

				err.printStackTrace(new PrintWriter(out,true));

				msg.send(StrUtil.str(out.toByteArray(),CharsetUtil.CHARSET_UTF_8)).exec();	

				

			}


			return true;

		}

		return false;

	}

	@Override
	public boolean onPrivMsg(UserData user,Msg msg) {
		
		if (!Env.FOUNDER.equals(user.userName)) return false;

		if (msg.isCommand()) {

			switch (msg.commandName()) {

				case "reset" : reset();break;
				case "reload" : reload(user,msg);break;

				default : return false;

			}

			return true;

		} else {

			if (msg.text() != null) {

				try {

					LuaValue result = lua.load(msg.text()).call();

					if (!result.isnil()) {

						msg.send(result.toString()).exec();

					}

				} catch (Throwable err) {

					ByteArrayOutputStream out = new ByteArrayOutputStream();

					err.printStackTrace(new PrintWriter(out,true));

					msg.send(StrUtil.str(out.toByteArray(),CharsetUtil.CHARSET_UTF_8)).exec();	

				}

			}

			return true;

		}

	}

	void reload(UserData user,Msg msg) {

		long start = System.currentTimeMillis();

		reset();

		long end = System.currentTimeMillis();

		msg.send("reloaded seccessful","time : " + (end - start) + "ms").exec();

	}

	class BindLib {

		void install() {

			env.set("bind",new bind());

			LuaTable mt = new LuaTable();

			mt.set("__index",new __index());

			env.setmetatable(mt);

		}

		LinkedHashSet<String> packages = new LinkedHashSet<>(); {
			
			packages.add("java.lang");
			packages.add("java.io");
			packages.add("java.util");
			
			packages.add("com.pengrad.telegrambot.request");
			
			packages.add("io.kurumi.ntt");
			packages.add("io.kurumi.ntt.db");
			packages.add("io.kurumi.ntt.twitter");
			packages.add("io.kurumi.ntt.utils");
			packages.add("io.kurumi.ntt.model");
			packages.add("io.kurumi.ntt.model.request");
			
			packages.add("cn.hutool.core.io");
			packages.add("cn.hutool.core.codec");
			packages.add("cn.hutool.core.util");
			packages.add("cn.hutool.extra.qrcode");
			packages.add("cn.hutool.http");
			packages.add("cn.hutool.json");
			
		}
		
		HashMap<String,Class<?>> loaded = new HashMap<>();

		void topLevelBind(String className,String bindAs) {
			
			if (className.endsWith(".*")) {

				packages.add(StrUtil.subBefore(className,".*",true));

				return;

			}

			try {

				JavaClass binded = JavaClass.forClass(Class.forName(className));

				if (bindAs == null) {

					bindAs = ((Class<?>)binded.m_instance).getSimpleName();

				}

				env.set(bindAs,binded);

			} catch (ClassNotFoundException e) {

				throw new LuaError("\n\n没有那样的Java类 : " + className);

			}

		}

		class bind extends OneArgFunction {

			@Override
			public LuaValue call(LuaValue arg) {

				if (arg.isstring()) {

					topLevelBind(arg.checkjstring(),null);

				} else if (arg.istable()) {

					LuaValue[] keys = arg.checktable().keys();

					for (LuaValue key : keys) {

						if (key.isnumber()) {

							topLevelBind(arg.get(key.checkint()).checkjstring(),null);

						} else {

							String className = arg.get(key.checkint()).checkjstring();

							if (className.endsWith(".*")) {

								throw new LuaError("\n\n导入Java包时指定别名是无意义的 (");

							}

							topLevelBind(className,key.checkjstring());

						}

					}

				} else {

					throw new LuaError("\n\n无效的导入内容 : 需要字符串或表 , 而不是" + arg.typename() + " , 你会用 bind 吗？");

				}

				return NIL;

			}


		}

		JavaClass matchClass(String simpleName) {

			for (String imported : packages) {

				try {

					return JavaClass.forClass(Class.forName(imported + "." + simpleName));

				} catch (ClassNotFoundException e) {}

			}

			return null;

		}

		class __index extends TwoArgFunction {

			@Override
			public LuaValue call(LuaValue T,LuaValue simpleName) {

				if (loaded.containsKey(simpleName)) {

					JavaClass clazz = JavaClass.forClass(loaded.get(simpleName));

					T.set(simpleName,clazz);

					return clazz;

				}

				JavaClass matched = matchClass(simpleName.checkjstring());

				if (matched != null) {

					T.set(simpleName,matched);

					return matched;

				}

				return NIL;

			}

		}

	}

	class LuaFragment extends Fragment {
		
		LuaTable fragment;

		boolean call(String function,Object... args) {

			if (fragment.get(function).isfunction()) {

				Varargs result = fragment.get(function).checkfunction().invoke(JavaArray.parseArray(args));

				if (result.arg1().isboolean()) {

					return result.arg1().checkboolean();

				}

			}
			
			return false;

		}

		LuaFragment(LuaTable fragment) {

			this.fragment = fragment;

		}

		public boolean onMsg(UserData user,Msg msg) {
			return call("onMsg",user,msg);
		}

		public boolean onPoiMsg(UserData user,Msg msg,CData point) {
			return call("onPoiMsg",user,msg,point);
		}

		public boolean onPrivMsg(UserData user,Msg msg) {
			return call("onPoiMsg",user,msg);
		}

		public boolean onPoiPrivMsg(UserData user,Msg msg,CData point) {
			return call("onPoiPrivMsg",user,msg,point);
		}

		public boolean onGroupMsg(UserData user,Msg msg,boolean superGroup) {
			return call("onGroupMsg",user,msg,superGroup);
		}

		public boolean onPoiGroupMsg(UserData user,Msg msg,CData point,boolean superGroup) {
			return call("onPoiGroupMsg",user,msg,superGroup);
		}

		public boolean onChanPost(UserData user,Msg msg) {
			return call("onChanPost",user,msg);
		}
		
		public boolean onCallback(UserData user,Callback callback) {
			return call("onCallback",user,callback);
		}

		public boolean onPoiCallback(UserData user,Callback callback,CData point) {
			return call("onPoiCallback",user,callback,point);
		}

		public boolean onQuery(UserData user,Query inlineQuery) {
			return call("onQuery",user,inlineQuery);
			
		}


	}

}
