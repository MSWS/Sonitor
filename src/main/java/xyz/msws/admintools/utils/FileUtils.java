package xyz.msws.admintools.utils;

import xyz.msws.admintools.Main;

import java.io.*;
import java.util.StringJoiner;

/**
 * Read / Write files / resources
 */
public class FileUtils {
    /**
     * Returns the file contents, or an empty string if the file does not exist or {@link IOException} occured;
     *
     * @param f File to read
     * @return file contents or empty string
     */
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

    /**
     * Saves the resource contained within the jar file to the current directory with the same file name
     *
     * @param name File path
     */
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

    /**
     * Writes the full contents of the file
     *
     * @param file File to write to
     * @param text Contents to write
     */
    public static void writeFile(File file, String text) {
        try (FileWriter write = new FileWriter(file)) {
            write.write(text);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
