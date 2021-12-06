package xyz.msws.admintools.data;

import xyz.msws.admintools.utils.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ButtonDatabase {

    private Map<String, Button> buttons = new HashMap<>();

    private static ButtonDatabase db;

    public static ButtonDatabase getInstance() {
        return db;
    }

    public ButtonDatabase(File file) {
        db = this;
        FileUtils.saveResource("buttons.txt");
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
