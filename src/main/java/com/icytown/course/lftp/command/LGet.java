package com.icytown.course.lftp.command;

import com.icytown.course.lftp.network.LFTPException;
import com.icytown.course.lftp.network.LGetClient;
import com.icytown.course.lftp.network.PacketSocket;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

@Command(name = "lget", aliases = {"g"}, mixinStandardHelpOptions = true, description = "Download a file from server.")
public class LGet implements Runnable {

    @Parameters(index = "0", paramLabel = "server_url", description = "Server's url or ip.")
    private String url;

    @Parameters(index = "1", paramLabel = "file_name", description = "Filename which need to download.")
    private String filename;

    @Override
    public void run() {
        int port = 2333, index = url.indexOf(':');
        if (index != -1) {
            try {
                port = Integer.parseInt(url.substring(index + 1));
            } catch (NumberFormatException e) {
                System.err.println("Server url '" + url + "' invalid.");
                return;
            }
            url = url.substring(0, index);
        }

        /*
        PacketSocket.sendNowAsync(url, port, filename, packet -> {
            System.out.println("Send packet successfully.");
        });
        */

        try{
            LGetClient lGetClient = new LGetClient(url, port, filename);
            lGetClient.run();
        } catch (UnknownHostException e) {
            System.err.println("Send failed, unknown host.");
        } catch (SocketException e) {
            System.err.println("Send failed, can not create socket.");
        } catch (IOException e) {
            System.err.println("Transfer or write failed.");
        } catch (LFTPException e) {
            System.err.println(e.getMessage());
        }
    }
}
