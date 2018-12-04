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

    public static void progress(long value, float nowSpeed, float filelength) {
        synchronized (Console.class) {
            int nowType = 0, timeType = 0;
            float time = filelength * (100 - value) / 100 / nowSpeed;
            if (time > 60) {
                timeType++;
                time /= 60;
            }
            if (time > 60) {
                timeType++;
                time /= 60;
            }
            if (time > 24) {
                timeType++;
                time /= 24;
            }
            while (nowSpeed >= 512) {
                nowSpeed /= 1024;
                nowType++;
            }
            StringBuilder msg = new StringBuilder("[>");
            for (int i = 1; i <= 99; i+= 2) {
                if (i <= value) {
                    msg.append("=");
                } else {
                    msg.append(" ");
                }
            }
            msg.append("<] ");
            System.out.print(String.format(msg + "%.2f%s in %.1f %s    \r", nowSpeed, speedTypes[nowType], time, timeTypes[timeType]));
        }
    }

    public static void progressFinish(float time, long totalLength) {
        synchronized (Console.class) {
            int timeType = 0, totalType = 0;
            float totalSpeed = ((float) totalLength) / time;
            if (time > 60) {
                timeType++;
                time /= 60;
            }
            if (time > 60) {
                timeType++;
                time /= 60;
            }
            if (time > 24) {
                timeType++;
                time /= 24;
            }
            while (totalSpeed >= 512) {
                totalSpeed /= 1024;
                totalType++;
            }
            System.out.println(String.format("\nFinish in %.0f %s, with total speed %.2f%s", time, timeTypes[timeType], totalSpeed, speedTypes[totalType]));
        }
    }

    private final static String[] speedTypes = {
            "KiB/s",
            "MiB/s",
            "GiB/s"
    };

    private final static String[] timeTypes = {
            "seconds",
            "minutes",
            "hours",
            "days"
    };
}
