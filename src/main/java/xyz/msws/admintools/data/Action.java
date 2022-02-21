package xyz.msws.admintools.data;

import java.util.ArrayList;
import java.util.List;

import xyz.msws.admintools.data.DataStructs.ActionType;
import xyz.msws.admintools.data.DataStructs.Role;
import xyz.msws.admintools.data.jb.JailRole;

/**
 * Represents a line in Jailbreak Logs
 * <p>
 * Compares by time
 */
public abstract class Action implements Comparable<Action> {
    protected ActionType type;
    protected String player, target;
    protected Role playerRole, targetRole;
    protected String[] other;
    protected String line;

    protected int playerRoleStart = Integer.MAX_VALUE, playerRoleEnd, targetRoleStart = -1, targetRoleEnd = -1;
    protected int playerStart, playerEnd;
    protected int targetStart, targetEnd;

    protected long time;

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
        if (target == null || target.isEmpty()) {
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
    public int compareTo(Action o) {
        return (int) (this.getTime() - o.getTime());
    }
}
