package com.icytown.course.lftp.network;

import java.io.IOException;
import java.net.*;

public class PacketSocket {

    private static int sequence = 0;

    public static void sendNowAsync(String ip, int port, String msg, Packet.OnCallbackListener onCallbackListener) {
        try {
            DatagramSocket socket = new DatagramSocket();
            Packet packet = new Packet(sequence);
            packet.setData(msg.getBytes());
            packet.setOnCallbackListener(onCallbackListener);
            byte[] data = packet.getBytes();
            byte[] ackData = new byte[1400];
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, InetAddress.getByName(ip), port);
            DatagramPacket ackDatagramPacket = new DatagramPacket(data, data.length);
            sequence++;
            socket.send(datagramPacket);
            while (true) {
                socket.receive(ackDatagramPacket);
                Packet ackPacket = Packet.fromBytes(ackDatagramPacket.getData());
                if (ackPacket != null && ackPacket.getId() == packet.getId() && ackPacket.isAck()) {
                    onCallbackListener.onSuccess(packet);
                    break;
                }
            }
            socket.close();
        } catch (SocketException e) {
            System.err.println("Send failed, can not create socket.");
        } catch (UnknownHostException e) {
            System.err.println("Send failed, unknown host.");
        } catch (IOException e) {
            System.err.println("Send failed.");
        }
    }
}
