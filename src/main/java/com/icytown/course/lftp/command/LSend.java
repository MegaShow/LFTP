package com.icytown.course.lftp.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "lsend", aliases = {"s"}, mixinStandardHelpOptions = true, description = "Upload a file to server.")
public class LSend implements Runnable {

    @Parameters(index = "0", paramLabel = "server_url", description = "Server's url or ip.")
    private String url;

    @Parameters(index = "1", paramLabel = "file_name", description = "Filename which need to upload.")
    private String filename;

    @Override
    public void run() {
        System.out.println("Call LSend");
        System.out.println("Url: " + url);
        System.out.println("Filename: " + filename);
    }
}
