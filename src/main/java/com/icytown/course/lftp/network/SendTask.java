package com.icytown.course.lftp.network;

import com.icytown.course.lftp.util.Console;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SendTask implements Runnable {

    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private String filepath;

    public SendTask(DatagramSocket socket, InetAddress address, int port, String filepath) {
        this.socket = socket;
        this.address = address;
        this.port = port;
        this.filepath = filepath;
    }

    @Override
    public void run() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(filepath));
            FileSender fileSender = new FileSender(fis, socket, address, port);
            fileSender.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
