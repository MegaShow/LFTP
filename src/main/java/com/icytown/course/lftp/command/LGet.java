package com.icytown.course.lftp.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "lget", aliases = {"g"}, mixinStandardHelpOptions = true, description = "Download a file from server.")
public class LGet implements Runnable {

    @Parameters(index = "0", paramLabel = "server_url", description = "Server's url or ip.")
    private String url;

    @Parameters(index = "1", paramLabel = "file_name", description = "Filename which need to download.")
    private String filename;

    @Override
    public void run() {
        System.out.println("Call LGet");
        System.out.println("Url: " + url);
        System.out.println("Filename: " + filename);
    }
}
