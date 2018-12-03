package com.icytown.course.lftp.network;

import com.icytown.course.lftp.util.Console;
import javafx.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;

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
                        File file = new File(folderPath, parameters[2]);
                        Packet ack = new Packet(packet.getId(), true);
                        if (!file.exists()) {
                            ack.setData("not_found".getBytes());
                            Console.err(url + " want to download '" + file.getAbsolutePath() + "', but not found.");
                        } else {
                            Pair<DatagramSocket, Integer> pair = SocketPool.getSocketAndPort(url);
                            if (pair == null) {
                                ack.setData("failed".getBytes());
                                Console.err(url + " want to download '" + file.getAbsolutePath() + "', but alloc socket failed.");
                            } else {
                                ack.setData(("ok," + pair.getValue()).getBytes());
                                Console.out(url + " want to download '" + file.getAbsolutePath() + "', allowed.");
                                if (pair.getKey() != null) {
                                    new Thread(new FileSender(pair.getKey(), rawPacket.getAddress(), Integer.parseInt(parameters[1]), file.getPath())).start();
                                }
                            }
                        }
                        byte[] ackData = ack.getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, rawPacket.getAddress(), rawPacket.getPort());
                        socket.send(ackPacket);
                    } else if (parameters[0].equals("Send")) {
                        File file = new File(folderPath, parameters[2]);
                        Packet ack = new Packet(packet.getId(), true);
                        if (file.exists()) {
                            ack.setData("exist".getBytes());
                            Console.err(url + " want to send file as '" + file.getAbsolutePath() + "', but another file exists.");
                        } else {
                            Pair<DatagramSocket, Integer> pair = SocketPool.getSocketAndPort(url);
                            if (pair == null) {
                                ack.setData("failed".getBytes());
                                Console.err(url + " want to send file as '" + file.getAbsolutePath() + "', but alloc socket failed.");
                            } else {
                                ack.setData(("ok," + pair.getValue()).getBytes());
                                Console.out(url + " want to send file as '" + file.getAbsolutePath() + "', allowed.");
                                if (pair.getKey() != null) {
                                    new Thread(new FileReceiver(pair.getKey(), file.getPath())).start();
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
