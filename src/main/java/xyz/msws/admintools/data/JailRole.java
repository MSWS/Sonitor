package xyz.msws.admintools.data;

import xyz.msws.admintools.data.DataStructs.Role;

/**
 * Enum to identify a player's role on Jailbreak
 */
public enum JailRole implements Role {
    WARDEN("W"), GUARD("G"), PRISONER("P"), REBEL("R"), SPECTATOR("S"), WORLD("World"), GHOST("Ghost"), ST("ST"),
    UNKNOWN("Unknown");

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
        return this == PRISONER || this == REBEL || this == ST;
    }

    public boolean isAlive() {
        return isCT() || isT();
    }
}
