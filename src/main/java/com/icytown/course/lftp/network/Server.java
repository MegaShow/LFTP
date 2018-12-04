package com.icytown.course.lftp.network;

import com.icytown.course.lftp.util.Console;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.Map;

public class Server {

    private DatagramSocket socket;

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

    public boolean serve(String folderPath) {
        byte[] data = new byte[1400];
        DatagramPacket rawPacket = new DatagramPacket(data, data.length);
        while (true) {
            try {
                socket.receive(rawPacket);
                Packet packet = Packet.fromBytes(rawPacket.getData());
                if (packet != null) {
                    String url = rawPacket.getAddress().getHostName() + ":" + rawPacket.getPort();
                    Console.out("Recieve request from " + url);
                    String[] parameters = new String(packet.getData()).split(",");
                    if (parameters[0].equals("Get")) {
                        File file = new File(folderPath, parameters[1]);
                        Packet ack = new Packet(packet.getId(), true);
                        if (!file.exists()) {
                            ack.setData("not_found".getBytes());
                            Console.err(url + " want to download '" + file.getAbsolutePath() + "', but not found.");
                        } else {
                            Map.Entry<DatagramSocket, Integer> pair = SocketPool.getSocketAndPort(url);
                            if (pair == null) {
                                ack.setData("failed".getBytes());
                                Console.err(url + " want to download '" + file.getAbsolutePath() + "', but alloc socket failed.");
                            } else {
                                long filelength = (file.length() + 1023) / 1024 + 1;
                                ack.setData(("ok," + pair.getValue() + "," + filelength).getBytes());
                                Console.out(url + " want to download '" + file.getAbsolutePath() + "', allowed.");
                                if (pair.getKey() != null) {
                                    new Thread(() -> {
                                        byte[] bytes = new byte[1024];
                                        DatagramPacket readyPacket = new DatagramPacket(bytes, bytes.length);
                                        try {
                                            while (true) {
                                                pair.getKey().receive(readyPacket);
                                                Packet packet1 = Packet.fromBytes(readyPacket.getData());
                                                if (packet1 != null && new String(packet1.getData()).equals("Ready")) {
                                                    Console.out(url + " ready to download.");
                                                    new Thread(new FileSender(pair.getKey(), readyPacket.getAddress(), readyPacket.getPort(), url, file.getPath(), filelength, true)).start();
                                                    break;
                                                }
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }).start();
                                }
                            }
                        }
                        byte[] ackData = ack.getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, rawPacket.getAddress(), rawPacket.getPort());
                        socket.send(ackPacket);
                    } else if (parameters[0].equals("Send")) {
                        File file = new File(folderPath, parameters[1]);
                        Packet ack = new Packet(packet.getId(), true);
                        if (file.exists()) {
                            ack.setData("exist".getBytes());
                            Console.err(url + " want to send file as '" + file.getAbsolutePath() + "', but another file exists.");
                        } else {
                            Map.Entry<DatagramSocket, Integer> pair = SocketPool.getSocketAndPort(url);
                            if (pair == null) {
                                ack.setData("failed".getBytes());
                                Console.err(url + " want to send file as '" + file.getAbsolutePath() + "', but alloc socket failed.");
                            } else {
                                ack.setData(("ok," + pair.getValue()).getBytes());
                                Console.out(url + " want to send file as '" + file.getAbsolutePath() + "', allowed.");
                                if (pair.getKey() != null) {
                                    new Thread(new FileReceiver(pair.getKey(), url, file.getPath(), Long.parseLong(parameters[2]), true)).start();
                                }
                            }
                        }
                        byte[] ackData = ack.getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, rawPacket.getAddress(), rawPacket.getPort());
                        socket.send(ackPacket);
                    }
                }
            } catch (FileNotFoundException e) {
                Console.err("File doesn't exist on server.");
            } catch (IOException e) {
                Console.err("Server failed to transfer or write.");
//            } catch (LFTPException e) {
//                Console.err.println(e.getMessage());
            }
        }
    }
}
