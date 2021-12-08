package xyz.msws.admintools.data;

import xyz.msws.admintools.utils.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A database that holds all of the known buttons
 */
public class ButtonDatabase {

    private Map<String, Button> buttons = new HashMap<>();

    private static ButtonDatabase db; // Singleton instance

    public static ButtonDatabase getInstance() {
        return db;
    }

    public ButtonDatabase(File file) {
        if (db != null)
            throw new IllegalStateException("ButtonDatabase already initialized");
        db = this;
        if (!file.exists())
            FileUtils.saveResource("buttons.txt");
        for (String line : FileUtils.readFile(file).split("\n")) {
            if (line.split(":").length != 3 && (!(line.split(":").length == 2 && line.endsWith(":"))))
                continue;
            Button b = new Button(line);
            buttons.put(b.getName(), b);
        }
    }

    /**
     * Gets the button by id
     *
     * @param name Name/id of the button
     * @return The button, if we know about it returns the given button, otherwise creates a new generic button with a blank alias
     */
    public Button getButton(String name) {
        buttons.putIfAbsent(name, new Button(name, false, ""));
        return buttons.get(name);
    }
}
