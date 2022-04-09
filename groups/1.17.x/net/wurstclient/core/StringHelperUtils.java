package net.wurstclient.core;

import java.util.regex.Pattern;

public class StringHelperUtils {
    private static final Pattern NAME_FORMATTING = Pattern.compile("(?i)\\u00A7[A-FK-OR0-9]");
    public static String stripTextFormat(String name){
        return NAME_FORMATTING.matcher(name).replaceAll("");
    }

}
