package xyz.msws.admintools.data.ttt;

import xyz.msws.admintools.data.DataStructs.Role;

public enum TTTRole implements Role {
    INNOCENT("I"), TRAITOR("T"), DETECTIVE("D"), SPECTATOR("S"), WORLD("World"), GHOST("Ghost"),
    UNKNOWN("Unknown");

    private final String icon;

    TTTRole(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isCT() {
        return this == DETECTIVE;
    }

    public boolean isT() {
        return this == TRAITOR || this == INNOCENT;
    }

    public boolean isAlive() {
        return isCT() || isT();
    }
}
