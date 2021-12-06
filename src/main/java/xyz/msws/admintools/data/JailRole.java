package xyz.msws.admintools.data;

/**
 * Enum to identify a player's role on Jailbreak
 */
public enum JailRole {
    WARDEN("W"), GUARD("G"), PRISONER("P"), REBEL("R"), SPECTATOR("S"), WORLD("World"), GHOST("Ghost");

    private final String icon;

    JailRole(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isCT() {
        return this == GUARD || this == WARDEN;
    }

    public boolean isT() {
        return this == PRISONER || this == REBEL;
    }

    public boolean isAlive() {
        return isCT() || isT();
    }
}
