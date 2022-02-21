package xyz.msws.admintools.data.jb;

import xyz.msws.admintools.data.DataStructs.ActionType;

/**
 * Represents an action that a player can make on Jailbreak
 */
public enum JailActionType implements ActionType {
    BUTTON("pressed %s (%s)"), WARDEN("took warden"),
    VENTS("broke vents"),
    DROP_WEAPON("dropped a(n) %s"), WARDEN_DEATH("died as warden"), PASS("passed warden"),
    FIRE("was fired"),
    RESKIN("reskinned %2$s's %1$s");

    private final String sum;

    JailActionType(String summary) {
        this.sum = summary;
    }

    public String getSummary(String... objects) {
        return String.format(sum, (Object[]) objects);
    }
}
