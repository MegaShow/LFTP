package com.icytown.course.lftp.util;

public class Console {

    public static void out(String msg) {
        synchronized (Console.class) {
            java.lang.System.out.println(msg);
        }
    }

    public static void err(String msg) {
        synchronized (Console.class) {
            java.lang.System.err.println(msg);
        }
    }
}
