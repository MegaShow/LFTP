package com.icytown.course.lftp.network;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class SocketPool {

    private static Map<String, DatagramSocket> pool = new HashMap<>();

    private static int port = 23333;

    public static DatagramSocket getSocket(String url) {
        if (!pool.containsKey(url)) {
            try {
                DatagramSocket socket = new DatagramSocket(port);
                port++;
                pool.put(url, socket);
            } catch (SocketException e) {
                return null;
            }
        }
        return pool.get(url);
    }
}
