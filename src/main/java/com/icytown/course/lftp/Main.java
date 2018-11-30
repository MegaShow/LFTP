package com.icytown.course.lftp;

import com.icytown.course.lftp.command.Root;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new Root());
        commandLine.parseWithHandler(new CommandLine.RunLast(), args);
    }
}
