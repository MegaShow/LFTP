package com.icytown.course.lftp.network;

import javafx.util.Pair;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class SocketPool {

    private static Map<String, DatagramSocket> pool = new HashMap<>();

    public static Pair<DatagramSocket, Integer> getSocketAndPort(String url) {
        if (!pool.containsKey(url)) {
            synchronized (SocketPool.class) {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    pool.put(url, socket);
                    return new Pair<>(socket, socket.getLocalPort());
                } catch (SocketException e) {
                    return null;
                }
            }
        } else {
            return new Pair<>(null, pool.get(url).getLocalPort());
        }
    }

    public static void removeSocket(String url) {
        synchronized (SocketPool.class) {
            DatagramSocket socket = pool.remove(url);
            if (socket != null) {
                socket.close();
            }
        }
    }
}
