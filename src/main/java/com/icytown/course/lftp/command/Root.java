package com.icytown.course.lftp.command;

import picocli.CommandLine.Command;

@Command(
        name = "lftp",
        mixinStandardHelpOptions = true,
        description = "A large file transfer tool.",
        version = "Version 1.0.0",
        subcommands = {Serve.class, LGet.class, LSend.class}
)
public class Root implements Runnable {

    @Override
    public void run() {
        System.out.println("LFTP, a large file transfer tool, with vesion 1.0.0");
    }
}
