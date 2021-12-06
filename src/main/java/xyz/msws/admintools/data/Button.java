package xyz.msws.admintools.data;

public class Button {
    private String name = null, alias = "";
    private boolean safe = false;

    public Button(String line) {
        String[] split = line.split(":");
        if (split.length != 3)
            throw new IllegalArgumentException("Invalid button format: " + line);
        name = split[0];
        safe = !split[1].isBlank() && Boolean.parseBoolean(split[1]);
        alias = split[2];
    }

    public Button(String name, boolean safe) {
        this.name = name;
        this.safe = safe;
    }

    public Button(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    public Button(String name, boolean safe, String alias) {
        this(name, safe);
        this.alias = alias;
    }

    public boolean isSafe() {
        return safe;
    }

    public String getAlias() {
        return alias;
    }

    public String getName() {
        return name;
    }
}
