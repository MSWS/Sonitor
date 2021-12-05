package xyz.msws.admintools.data;

public enum Role {
    WARDEN("W"), GUARD("G"), PRISONER("P"), REBEL("R"), SPECTATOR("S"), WORLD("World"), GHOST("Ghost");

    private final String icon;

    Role(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }
}
