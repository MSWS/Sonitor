package xyz.msws.admintools.data;

import xyz.msws.admintools.utils.FileUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ButtonDatabase {

    private Map<String, Button> buttons = new HashMap<>();

    public ButtonDatabase() {
    }

    private static ButtonDatabase db;

    public static ButtonDatabase getInstance() {
        return db;
    }

    public ButtonDatabase(File file) {
        db = this;
        try {
            if (file.createNewFile()) {
                try (InputStream io = getClass().getClassLoader().getResourceAsStream("buttons.txt")) {
                    if (io == null)
                        throw new NullPointerException("buttons.txt not found");
                    FileWriter write = new FileWriter(file);
                    System.out.println("wrote " + file.getAbsolutePath());
                    try (InputStreamReader reader = new InputStreamReader(io); BufferedReader br = new BufferedReader(reader)) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            write.write(line + "\n");
                            System.out.println("Write " + line);
                        }
                    }
                    write.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String line : FileUtils.readFile(file).split("\n")) {
            if (line.split(":").length != 3 || (line.split(":").length == 2) && line.endsWith(":"))
                continue;
            Button b = new Button(line);
            buttons.put(b.getName(), b);
        }
    }

    public Button getButton(String name) {
        buttons.putIfAbsent(name, new Button(name, false, ""));
        return buttons.get(name);
    }

    private Button register(String name, boolean damage) {
        return new Button(name, damage);
    }

    private Button register(String name, String alias) {
        return new Button(name, alias);
    }


}
