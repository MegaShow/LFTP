package com.icytown.course.lftp.network;

import com.icytown.course.lftp.util.Console;
import javafx.util.Pair;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class PacketSocket {

    private static int sequence = 0;

    public static byte[] send(String ip, int port, long timeOut, byte[] body) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            Packet result = null;
            channel.configureBlocking(false);
            Packet packet = new Packet(sequence);
            sequence++;
            packet.setData(body);
            byte[] data = packet.getBytes();
            ByteBuffer buffer = ByteBuffer.wrap(data);
            SocketAddress address = new InetSocketAddress(ip, port);
            long timeOutTime = 500;
            long lastTime;
            boolean flag = false;
            Console.out("Send request to " + ip + ":" + port + ".");
            while (!flag && timeOutTime < timeOut) {
                channel.send(buffer, address);
                lastTime = System.currentTimeMillis();
                ByteBuffer recBuffer = ByteBuffer.allocate(1400);
                while (System.currentTimeMillis() - lastTime < timeOutTime && System.currentTimeMillis() - lastTime < timeOut) {
                    SocketAddress recAddress = channel.receive(recBuffer);
                    if (recAddress != null) {
                        Packet recPacket = Packet.fromBytes(recBuffer.array());
                        if (recPacket != null && recPacket.getId() == packet.getId() && recPacket.isAck()) {
                            result = recPacket;
                            flag = true;
                            break;
                        }
                    }
                }
                if (!flag) {
                    timeOutTime <<= 1;
                    channel.send(buffer, address);
                    Console.err("Time out, send request to " + ip + ":" + port + " again.");
                }
            }
            if (flag) {
                Console.out("Send request successfully.");
                return result.getData();
            } else {
                Console.err("Time out, please check your network.");
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
}
