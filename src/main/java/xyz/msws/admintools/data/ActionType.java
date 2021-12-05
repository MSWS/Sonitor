package xyz.msws.admintools.data;

public enum ActionType {
    DAMAGE("damaged %s for %s"), KILL("killed %s"), BUTTON("pressed button %s"), WARDEN("took warden"), VENTS("broke vents"),
    DROP_WEAPON("dropped a(n) %s"), NADE("threw a(n) %s"), WARDEN_DEATH("died as warden"), PASS("passed warden"), FIRE("was fired"),
    RESKIN("reskinned %2$s's %1$s"), GHOST_RESPAWN("respawned as ghost");

    private String sum;

    ActionType(String summary) {
        this.sum = summary;
    }

    public String getSummary() {
        return sum;
    }

    public String getSummary(String... objects) {
        return String.format(sum, (Object[]) objects);
    }
}
