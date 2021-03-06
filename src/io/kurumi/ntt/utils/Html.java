package io.kurumi.ntt.utils;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.EscapeUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONObject;
import io.kurumi.ntt.Launcher;
import io.kurumi.ntt.fragment.Fragment;

public class Html {

    public static String unescape(String text) {

        return EscapeUtil.unescapeHtml4(HtmlUtil.unescape(text));

    }

    public static String b(Object text) {

        return "<b>" + HtmlUtil.escape(text == null ? "" : text.toString()) + "</b>";

    }

    public static String i(Object text) {

        return "<i>" + HtmlUtil.escape(text == null ? "" : text.toString()) + "</i>";

    }

    public static String a(String link) {

        if (link == null) return link;

        if (link.startsWith("http")) link = URLUtil.url(link).toString();

        return "<a href=\"" + link + "\">" + HtmlUtil.escape(link) + "</a>";

    }


    public static String a(String text, String href) {

        if (href == null) return text;

        if (href.startsWith("http")) href = URLUtil.url(href).toString();

        return "<a href=\"" + href + "\">" + HtmlUtil.escape(text) + "</a>";

    }

    public static String user(String text, long id) {

        if (text.isEmpty()) {

            text = " ";

        }

        return a(text, "tg://user?id=" + id);

    }

    public static String twitterUser(String text, String id) {

        if (text.isEmpty()) {

            text = " ";

        }

        return a(text, "https://twitter.com/" + id);

    }

    public static String startPayload(String text, Object... payload) {

        return startPayload(Launcher.INSTANCE, text, payload);

    }

    public static String startPayload(Fragment fragment, String text, Object... payload) {

        return a(text, "https://t.me/" + fragment.origin.me.username() + "?start=" + ArrayUtil.join(payload, "_"));

    }

    public static String json(Object code) {

        return code(new JSONObject(code).toStringPretty());

    }


    public static String code(Object code) {

        return "<code>" + HtmlUtil.escape(code == null ? "null" : code.toString()) + "</code>";

    }

}
