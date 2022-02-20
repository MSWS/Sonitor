package xyz.msws.admintools.data;

import java.util.ArrayList;
import java.util.List;

import xyz.msws.admintools.data.DataStructs.ActionType;
import xyz.msws.admintools.data.DataStructs.Role;

/**
 * Represents a line in Jailbreak Logs
 * <p>
 * Compares by time
 */
public abstract class Action implements Comparable<JailAction> {
    ActionType type;
    String player, target;
    Role playerRole, targetRole;
    String[] other;
    String line;

    int playerRoleStart = Integer.MAX_VALUE, playerRoleEnd, targetRoleStart = -1, targetRoleEnd = -1;
    int playerStart, playerEnd;
    int targetStart, targetEnd;

    long time;

    public Action(String line) {
        this.line = line;
    }

    public String getPlayer() {
        return player;
    }

    public String getTarget() {
        return target;
    }

    public Role getPlayerRole() {
        return playerRole;
    }

    public Role getTargetRole() {
        return targetRole;
    }

    public ActionType getType() {
        return type;
    }

    public String[] getOther() {
        return other;
    }

    public String getLine() {
        return line;
    }

    public String simplify() {
        String[] opts;
        if (target.isEmpty()) {
            opts = other;
        } else {
            List<String> s = new ArrayList<>();
            s.add(target);
            s.addAll(List.of(other));
            opts = s.toArray(new String[0]);
        }
        if (playerRole == JailRole.WORLD)
            return player + " " + type.getSummary(opts);

        return player + " (" + getPlayerRole().getIcon() + ") " + type.getSummary(opts);
    }

    public long getTime() {
        return time;
    }

    public String getTimeString() {
        return String.format("%02d:%02d", (int) Math.floor((float) time / 60), time % 60);
    }

    @Override
    public int compareTo(JailAction o) {
        return (int) (this.getTime() - o.getTime());
    }
}
