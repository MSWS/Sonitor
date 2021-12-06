package xyz.msws.admintools.data;

/**
 * Represents an action that a player can make on Jailbreak
 */
public enum JailActionType {
    DAMAGE("damaged %s for %s"), KILL("killed %s"), BUTTON("pressed %s (%s)"), WARDEN("took warden"), VENTS("broke vents"),
    DROP_WEAPON("dropped a(n) %s"), NADE("threw a(n) %s"), WARDEN_DEATH("died as warden"), PASS("passed warden"), FIRE("was fired"),
    RESKIN("reskinned %2$s's %1$s"), GHOST_RESPAWN("respawned as ghost");

    private final String sum;

    JailActionType(String summary) {
        this.sum = summary;
    }

    public String getSummary(String... objects) {
        return String.format(sum, (Object[]) objects);
    }
}
