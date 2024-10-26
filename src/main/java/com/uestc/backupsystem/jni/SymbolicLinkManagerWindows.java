package com.uestc.backupsystem.jni;

import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Component
public class SymbolicLinkManagerWindows {

    private static final String CREATE_SYMBOLIC_LINK_BAT = "E:\\BackupSystem\\src\\main\\resources\\bat\\create_symbolic_link.bat";

    static {
        String symbolicLinkManagerWindowsLibPath = "E:\\BackupSystem\\src\\main\\resources\\dll\\symbolic_link_manager_windows.dll";
        System.load(symbolicLinkManagerWindowsLibPath);
    }

    public native boolean isSymbolicLink(String path);
    public native String getSymbolicLinkTarget(String path);

    public boolean createNewSymbolicLink(String target, String destinationPath) {
        File destination = new File(destinationPath);
        destination.delete();
        List<String> commands = new ArrayList<>();
        try {
            commands.add("cmd.exe");
            commands.add("/c");
            commands.add(CREATE_SYMBOLIC_LINK_BAT);
            commands.add(destinationPath);
            commands.add(target);
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to create symbolic link: " + e.getMessage(), e);
        }
    }
}
