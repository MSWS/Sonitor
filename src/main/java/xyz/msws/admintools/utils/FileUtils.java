package xyz.msws.admintools.utils;

import xyz.msws.admintools.Main;

import java.io.*;
import java.util.StringJoiner;

public class FileUtils {
    public static String readFile(File f) {
        if (f == null || !f.exists())
            return "";
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
            return new String(in.readAllBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void saveResource(String name) {
        File file = new File(name);
        try {
            file.createNewFile();
            try (InputStream io = Main.class.getClassLoader().getResourceAsStream(name)) {
                if (io == null)
                    throw new NullPointerException(name + " not found");
                StringJoiner join = new StringJoiner("\n");
                try (InputStreamReader reader = new InputStreamReader(io); BufferedReader br = new BufferedReader(reader)) {
                    String line;
                    while ((line = br.readLine()) != null)
                        join.add(line);
                }
                writeFile(file, join.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeFile(File file, String text) {
        try (FileWriter write = new FileWriter(file)) {
            write.write(text);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
