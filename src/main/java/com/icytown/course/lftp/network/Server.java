package com.icytown.course.lftp.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server {

    private DatagramSocket socket;
    private int sequence;

    public Server(int port) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            socket = null;
        }
    }

    public boolean canServe() {
        return socket != null;
    }

    public boolean serve() {
        while (true) {
            byte[] data = new byte[1400];
            DatagramPacket rawPacket = new DatagramPacket(data, data.length);
            try {
                socket.receive(rawPacket);
                Packet packet = Packet.fromBytes(rawPacket.getData());
                if (packet != null) {
                    System.out.println(new String(packet.getData()));
                    byte[] ackData = new Packet(packet.getId(), true).getBytes();
                    DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, rawPacket.getAddress(), rawPacket.getPort());
                    socket.send(ackPacket);
                }
            } catch (IOException e) {
                return false;
            }
        }
    }
}
