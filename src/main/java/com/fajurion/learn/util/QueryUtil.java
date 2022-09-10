package com.fajurion.learn.util;

import java.util.List;

public class QueryUtil {

    public static String buildIDString(List<Integer> integers) {
        StringBuilder builder = new StringBuilder();

        for(int i : integers) {
            builder.append(",").append(i);
        }

        return builder.substring(1);
    }

}
