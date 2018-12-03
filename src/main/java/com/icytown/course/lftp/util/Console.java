package com.icytown.course.lftp.util;

import java.util.Locale;

public class Console {

    public static void out(String msg) {
        synchronized (Console.class) {
            System.out.println(msg);
        }
    }

    public static void err(String msg) {
        synchronized (Console.class) {
            System.err.println(msg);
        }
    }

    public static void progress(long value, float nowSpeed, float totalSpeed) {
        int nowType = 0, totalType = 0;
        while (nowSpeed >= 512) {
            nowSpeed /= 1024;
            nowType++;
        }
        while (totalSpeed >= 512) {
            totalSpeed /= 1024;
            totalType++;
        }
        synchronized (Console.class) {
            System.err.print(String.format(Locale.getDefault(), "Progress: %d,  Speed: %.2f%s,  Total Speed: %.2f%s\r",
                    value, nowSpeed, speedTypes[nowType], totalSpeed, speedTypes[totalType]));
        }
    }

    private final static String[] speedTypes = {
            "KiB/s",
            "MiB/s",
            "GiB/s"
    };
}
