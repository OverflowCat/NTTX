package io.kurumi.ntt.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

public class Cndic {

    String __VIEWSTATE;

    String __EVENTVALIDATION;

    String __VIEWSTATEGENERATOR;

    public void reset() {

        String html = HttpUtil.get("http://www.cndic.com/manchu.aspx");

        __VIEWSTATE = StrUtil.subBetween(html, "__VIEWSTATE\" value=\"", "\"");

        __EVENTVALIDATION = StrUtil.subBetween(html, "__EVENTVALIDATION\" value=\"", "\"");

        __VIEWSTATEGENERATOR = StrUtil.subBetween(html, "__VIEWSTATEGENERATOR\" value=\"", "\"");

    }

    public String cn_ma(String str, boolean noReset, boolean unTrans) {

        if (__VIEWSTATE == null) reset();

        if (unTrans) {

            str = str.replace("᠈", "，").replace("᠉", "。");

            str = str.replace("？", "？");

            str = str.replace("᠄", "：");


        }

        HttpResponse resp = HttpUtil
                .createPost("http://www.cndic.com/manchu.aspx")
                .form("__VIEWSTATE", __VIEWSTATE)
                .form("__EVENTVALIDATION", __EVENTVALIDATION)
                .form("__VIEWSTATEGENERATOR", __VIEWSTATEGENERATOR)
                .form("tbLeft", str)
                .form("selType", unTrans ? "ma_cn" : "cn_ma")
                .form("selWordText", "selText")
                .form("btnTranslate", "+翻+译+")
                .form("tbRight", "Dit+ding+").execute();

        String result = resp.body();

        result = StrUtil.subAfter(result, "<textarea", true);

        result = StrUtil.subBetween(result, ">", "<").trim();

        result = result.trim();

        if (!noReset && result.isEmpty()) {

            reset();

            return cn_ma(str, true, unTrans);

        }

        if (result.isEmpty() || result.contains("您输入的内容未能翻译出来")) {

            result = null;

        } else if (!unTrans) {

            result = result.replace("，", "᠈").replace("。", "᠉");

            result = result.replace("?", "？");

            result = result.replace("，", "᠈").replace(",", "᠈");

            result = result.replace("：", "᠄").replace(":", "᠄");

            // result = result.replaceAll("\\.*","᠁");

        }

        return result;

    }

}
