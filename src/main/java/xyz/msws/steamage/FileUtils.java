package xyz.msws.steamage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtils {
    public static String readFile(File f) {
        if (f == null || !f.exists())
            return null;
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
            return new String(in.readAllBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
