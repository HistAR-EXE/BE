package com.histar.be.rag.service;

import java.util.Locale;

public final class VectorUtils {

    private VectorUtils() {}

    /** pgvector text literal, e.g. {@code [0.1,0.2,0.3]}; bind it with {@code CAST(:vec AS vector)}. */
    public static String toLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 10 + 2).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.ROOT, "%.7f", vector[i]));
        }
        return sb.append(']').toString();
    }
}
