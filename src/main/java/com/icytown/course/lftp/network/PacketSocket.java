package com.icytown.course.lftp.network;

import com.icytown.course.lftp.util.Console;

import java.io.IOException;
import java.net.*;

public class PacketSocket {

    private static boolean flag = false;

    public static byte[] send(DatagramSocket socket, String ip, int port, long timeOut, byte[] body, String msg) {
        try {
            Packet result = null;
            Packet packet = new Packet(0);
            packet.setData(body);
            byte[] data = packet.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, new InetSocketAddress(ip, port));
            new Thread(new TimeOutTask(socket, datagramPacket)).start();
            byte[] recData = new byte[1400];
            DatagramPacket recDatagramPacket = new DatagramPacket(recData, recData.length);
            Console.out(msg);
            socket.send(datagramPacket);
            while (!flag) {
                socket.receive(recDatagramPacket);
                Packet recPacket = Packet.fromBytes(recDatagramPacket.getData());
                if (recPacket != null && recPacket.getId() == 0 && recPacket.isAck()) {
                    synchronized (PacketSocket.class) {
                        flag = true;
                    }
                    result = recPacket;
                    break;
                }
            }
            Console.out("Send request successfully.");
            if (result == null) {
                return null;
            } else {
                return result.getData();
            }
        } catch (SocketException e) {
            Console.err("Send failed, can not create socket.");
        } catch (UnknownHostException e) {
            Console.err("Send failed, unknown host.");
        } catch (IOException e) {
            Console.err("Send failed.");
        }
        return null;
    }

    public static class TimeOutTask implements Runnable {

        private long time;
        private DatagramSocket socket;
        private DatagramPacket packet;

        public TimeOutTask(DatagramSocket socket, DatagramPacket packet) {
            this.socket = socket;
            this.packet = packet;
        }

        public void updateTime() {
            synchronized (TimeOutTask.this) {
                time = System.currentTimeMillis();
            }
        }

        public long getTime() {
            synchronized (TimeOutTask.this) {
                return time;
            }
        }

        @Override
        public void run() {
            updateTime();
            int count = 0;
            while (!flag) {
                if (System.currentTimeMillis() - getTime() > 300) {
                    try {
                        count++;
                        if (count % 3 == 0) {
                            Console.err("Time out, send request to " + packet.getAddress().getHostName() + ":" + packet.getPort() + " again.");
                        }
                        if (count == 20) {
                            Console.err("Time out, please checkout your network.");
                            System.exit(-2);
                        }
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    updateTime();
                }
            }
        }
    }
}
