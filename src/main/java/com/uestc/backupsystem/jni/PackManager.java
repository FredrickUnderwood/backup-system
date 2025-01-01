package com.uestc.backupsystem.jni;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PackManager {
    static {
        String packManagerLibPath = null;
        try {
            packManagerLibPath = new ClassPathResource("dll/pack_manager.dll").getFile().getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.load(packManagerLibPath);
    }

    public native void pack(String inputName, String outputName);
    public native void unpack(String inputName, String outputPath);
}
