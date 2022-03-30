package net.wurstclient.util;

import java.util.ArrayList;
import java.util.Arrays;

public class SearchType {
    public static final String _ITEMID = "Item Id";
    public static final String _NAME = "Item name";
    public static final String _DISABLED = "Disabled";
    private ArrayList<String> list = new ArrayList<String>(Arrays.asList(_DISABLED, _ITEMID, _NAME));
    private String current = list.get(0);

    public SearchType() {

    }

    public String getCurrent() {
        return this.current;
    }

    public String getNext() {
        int currentIndex = list.indexOf(this.current);
        int listSize = list.size() - 1;
        int increment = currentIndex != listSize ? 1 : -listSize;
        this.current = list.get(currentIndex + increment);
        return this.current;
    }
}