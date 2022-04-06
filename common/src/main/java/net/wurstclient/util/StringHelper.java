package net.wurstclient.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class StringHelper {
    private static final Pattern FORMATTING_CODE = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
    private static final Pattern LINE_BREAK = Pattern.compile("\\r\\n|\\v");
    private static final Pattern ENDS_WITH_LINE_BREAK = Pattern.compile("(?:\\r\\n|\\v)$");

    public StringHelper() {
    }

    public static String formatTicks(int ticks) {
        int i = ticks / 20;
        int j = i / 60;
        i %= 60;
        return i < 10 ? j + ":0" + i : j + ":" + i;
    }

    public static String stripTextFormat(String text) {
        return FORMATTING_CODE.matcher(text).replaceAll("");
    }

    public static boolean isEmpty(@Nullable String text) {
        return StringUtils.isEmpty(text);
    }

    public static String truncate(String text, int maxLength, boolean addEllipsis) {
        if (text.length() <= maxLength) {
            return text;
        } else if (addEllipsis && maxLength > 3) {
            String var10000 = text.substring(0, maxLength - 3);
            return var10000 + "...";
        } else {
            return text.substring(0, maxLength);
        }
    }

    public static int countLines(String text) {
        if (text.isEmpty()) {
            return 0;
        } else {
            Matcher matcher = LINE_BREAK.matcher(text);

            int i;
            for (i = 1; matcher.find(); ++i) {
            }

            return i;
        }
    }

    public static boolean endsWithLineBreak(String text) {

        return ENDS_WITH_LINE_BREAK.matcher(text).find();
    }
}
