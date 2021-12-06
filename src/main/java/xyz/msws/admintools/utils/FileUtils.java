package xyz.msws.admintools.utils;

import xyz.msws.admintools.Main;

import java.io.*;

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

    public static void saveResource(String name) {
        File file = new File(name);
        try {
            if (file.createNewFile() || file.exists()) {
                try (InputStream io = Main.class.getClassLoader().getResourceAsStream(name)) {
                    if (io == null)
                        throw new NullPointerException(name + " not found");
                    FileWriter write = new FileWriter(file);
                    try (InputStreamReader reader = new InputStreamReader(io); BufferedReader br = new BufferedReader(reader)) {
                        String line;
                        while ((line = br.readLine()) != null)
                            write.write(line + "\n");
                    }
                    write.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
