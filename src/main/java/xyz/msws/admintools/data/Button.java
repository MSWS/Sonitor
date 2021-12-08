package xyz.msws.admintools.data;

/**
 * Represents a button that we know about
 */
public class Button {
    private String name, alias = "";
    private boolean safe = false;

    public Button(String line) {
        String[] split = line.split(":");
        name = split[0];
        safe = !split[1].isBlank() && Boolean.parseBoolean(split[1]);

        alias = split.length == 3 ? split[2] : "";
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

    /**
     * Checks if the button is safe to ignore in terms of harming players
     *
     * @return if the button doesn't not harm players
     */
    public boolean isSafe() {
        return safe;
    }

    /**
     * Gets the alias attached to the button, or an empty string if none
     *
     * @return the alias or an empty string
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Gets the button name / identification
     *
     * @return buttion id
     */
    public String getName() {
        return name;
    }
}
