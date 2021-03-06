package io.kurumi.ntt.fragment.inline;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.response.BaseResponse;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.model.Query;
import io.kurumi.ntt.model.request.ButtonLine;
import io.kurumi.ntt.model.request.ButtonMarkup;

import java.util.Collections;

public class MakeButtons extends Fragment {

    @Override
    public boolean query() {

        return true;

    }

    @Override
    public void onQuery(UserData user, Query inlineQuery) {

        if (StrUtil.isBlank(inlineQuery.text)) {

            return;

        }

        String text = inlineQuery.text;

        boolean html = false;
        boolean md = false;
        boolean buttons = false;

        while (true) {

            if (text.startsWith(" ") || text.startsWith("\n")) {

                text = text.substring(1);

            } else if (text.startsWith("MD")) {

                text = text.substring(2);

                md = true;

            } else if (text.startsWith("HTML")) {

                text = text.substring(4);

                html = true;

            } else if (text.startsWith("BUTTONS")) {

                text = text.substring(7);

                buttons = true;

            } else {

                break;

            }

        }

        if (StrUtil.isBlank(text) || (!html && !buttons && !md)) {

            return;

        }

        ButtonMarkup markup = null;

        if (buttons) {

            for (String line : ArrayUtil.reverse(text.split("\n"))) {

                if (!(line.startsWith("[") && line.endsWith(")"))) break;

                ButtonLine bL = new ButtonLine();

                while (line.contains("[")) {

                    String after = StrUtil.subAfter(line, "[", true);
                    line = StrUtil.subBefore(line, "[", true);

                    String bText = StrUtil.subBefore(after, "]", false);
                    String bUrl = StrUtil.subBetween(after, "(", ")");

                    if (bText == null || bUrl == null) {

                        // invalid format

                        break;

                    }

                    bL.newUrlButton(bText, bUrl);

                }

                if (markup == null) markup = new ButtonMarkup();

                Collections.reverse(bL);

                markup.add(bL);

                text = StrUtil.subBefore(text, "\n", true);

            }

        }

        if (markup != null) {

            Collections.reverse(markup);

        }

        ParseMode parseMode = null;

        if (md) parseMode = ParseMode.Markdown;
        if (html) parseMode = ParseMode.HTML;

        inlineQuery.article("完成 *٩(๑´∀`๑)ง*", text, parseMode, markup);

        BaseResponse resp = execute(inlineQuery.reply());

        if (!resp.isOk()) {

            inlineQuery.article("解析失败", "解析失败 : \n\n" + resp.description(), null, null);

            execute(inlineQuery.reply());

        }

    }

}
