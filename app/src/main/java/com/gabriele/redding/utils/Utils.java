package com.gabriele.redding.utils;

import android.content.Context;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    final static Pattern pattern = Pattern.compile("\\b((((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" +
            "(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov" +
            "|mil|biz|info|mobi|name|aero|jobs|museum" +
            "|travel|[a-z]{2}))(:[\\d]{1,5})?" +
            "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" +
            "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
            "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" +
            "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
            "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" +
            "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?)\\b");

    public static float pixelsToSp(Context context, float px) {
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        return px/scaledDensity;
    }

    public static int spToPixels(Context context, int sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                context.getResources().getDisplayMetrics());
    }

    public static List<LinkPosition> findLinks(String text) {
        List<LinkPosition> positions = new ArrayList<>();
        final Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            positions.add(new LinkPosition(matcher.start(), matcher.end(), matcher.group()));
        }

        return positions;
    }


    public static class LinkPosition {
        private int start;
        private int end;
        private String url;

        public LinkPosition(int start, int end, String url) {
            this.start = start;
            this.end = end;
            this.url = url;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public String getUrl() {
            return url;
        }
    }
}
