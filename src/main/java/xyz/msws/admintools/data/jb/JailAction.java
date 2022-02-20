package xyz.msws.admintools.data.jb;

import java.util.ArrayList;
import java.util.List;

import xyz.msws.admintools.data.Action;
import xyz.msws.admintools.data.Button;
import xyz.msws.admintools.data.ButtonDatabase;
import xyz.msws.admintools.data.DataStructs.ActionType;
import xyz.msws.admintools.data.DataStructs.GenericActionType;
import xyz.msws.admintools.data.DataStructs.Role;

/**
 * Represents a line in Jailbreak Logs
 * <p>
 * Compares by time
 */
public class JailAction extends Action {
    public JailAction(String line) {
        super(line);
        this.type = findActionType();

        calculateIndices();

        this.player = findPlayer();
        this.target = findTarget();
        this.other = findOther();

        this.time = findTime();
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

    private ActionType findActionType() {
        if (line.endsWith("is now warden")) {
            return JailActionType.WARDEN;
        } else if (line.endsWith("broke a vent or wall")) {
            return JailActionType.VENTS;
        } else if (line.contains(") pressed button '")) {
            return JailActionType.BUTTON;
        } else if (line.contains("threw a") && (line.endsWith("smoke") || line.endsWith("grenade")
                || line.endsWith("flash") || line.endsWith("decoy") || line.endsWith("molotov"))) {
            return GenericActionType.NADE;
        } else if (line.contains("hurt") && line.contains("with") && line.contains("damage (")) {
            return GenericActionType.DAMAGE;
        } else if (line.contains("has died and is no longer warden")) {
            return JailActionType.WARDEN_DEATH;
        } else if (line.contains("killed ")) {
            return GenericActionType.KILL;
        } else if (line.contains("dropped the weapon")) {
            return JailActionType.DROP_WEAPON;
        } else if (line.endsWith("has been fired by an admin")) {
            return JailActionType.FIRE;
        } else if (line.endsWith("has passed warden")) {
            return JailActionType.PASS;
        } else if (line.contains("reskinned weapon_")) {
            return JailActionType.RESKIN;
        } else if (line.contains("was respawned for touching")) {
            return GenericActionType.GHOST_RESPAWN;
        } else if (line.endsWith("has disconnected, passing warden")) {
            return JailActionType.PASS;
        } else if (line.contains("broke '")) {
            return JailActionType.VENTS;
        }

        throw new IllegalArgumentException("Invalid line: " + line);
    }

    private void calculateIndices() {
        for (JailRole role : JailRole.values()) {
            int index = line.toUpperCase().indexOf("(" + role.toString() + ")");
            if (index != -1) {
                index++;
            } else if (role == JailRole.WORLD) {
                index = line.toUpperCase().indexOf((role.toString()));
            } else {
                index = line.indexOf("(" + role.toString().charAt(0) + role.toString().substring(1).toLowerCase() + ")")
                        + 1;
            }

            if (index <= 0 || index > playerRoleStart)
                continue;
            if (role == JailRole.WORLD) {
                playerRoleStart = index;
                playerRoleEnd = playerRoleStart + "The World".length();
                playerRole = JailRole.WORLD;
            } else {
                playerRoleStart = index;
                playerRoleEnd = playerRoleStart + role.toString().length();
                playerRole = JailRole.valueOf(line.substring(playerRoleStart, playerRoleEnd).toUpperCase());
            }
        }
        if (playerRole == null) {
            System.out.println("Unknown line: " + line);
            return;
        }
        boolean world = playerRole == JailRole.WORLD;

        playerStart = 8;
        playerEnd = playerRoleStart - (world ? -5 : 2);

        if (type != GenericActionType.DAMAGE && type != GenericActionType.KILL)
            return;

        for (JailRole role : JailRole.values()) {
            // int index = line.toUpperCase().lastIndexOf("(" + role.toString() + ")") + 1;
            int index = role == JailRole.WARDEN ? line.toUpperCase().lastIndexOf(role.toString())
                    : (line.toUpperCase().lastIndexOf("(" + role + ")") + 1);

            if (index <= 0 || index == playerRoleStart)
                continue;
            targetRoleStart = index;
            targetRoleEnd = targetRoleStart + role.toString().length();
            targetRole = JailRole.valueOf(line.substring(targetRoleStart, targetRoleEnd).toUpperCase());
            break;
        }

        String damageLine = line.substring(playerRoleEnd + (world ? 1 : 2),
                line.indexOf(" ", playerRoleEnd + (world ? 1 : 2)));

        targetStart = playerRoleEnd + (world ? 2 : 3) + damageLine.length();
        targetEnd = targetRoleStart - 2;
    }

    private String findPlayer() {
        return line.substring(playerStart, playerEnd);
    }

    private String findTarget() {
        if (targetStart == -1 || targetEnd == -1)
            return null;
        return line.substring(targetStart, targetEnd);
    }

    private String[] findOther() {
        if (type instanceof GenericActionType gType) {
            switch (gType) {
                case KILL:
                    return new String[] { targetRole.getIcon() };
                case DAMAGE:
                    return new String[] {
                            line.substring(line.lastIndexOf("with ") + "with ".length(),
                                    line.lastIndexOf("damage") - 1),
                            line.substring(line.lastIndexOf("(") + 1, line.length() - 1) };
                case NADE:
                    return new String[] { line.substring(line.lastIndexOf(" ") + 1) };
                default:
                    return new String[] {};
            }
        }
        if (!(type instanceof JailActionType jbType))
            return new String[] { "Invalid Type" };
        switch (jbType) {
            case BUTTON:
                String name = line.substring(
                        line.substring(0, line.length() - 1).lastIndexOf(
                                line.contains("pressed button 'Unknown'") ? "(" : "'", line.length() - 2) + 1,
                        line.length() - 1);
                Button b = ButtonDatabase.getInstance().getButton(name);
                return new String[] { b.getName(), b.getAlias() };
            case DROP_WEAPON:
                return new String[] { line.substring(line.lastIndexOf(" ") + 1, line.length() - 1) };
            case RESKIN:
                String weapon = line.substring(line.indexOf("reskinned weapon_") + "reskinned weapon_".length(),
                        line.indexOf(" ", line.lastIndexOf("weapon_")));
                if (line.endsWith("(not previously owned)")) {
                    return new String[] { weapon, "their own" };
                }
                return new String[] { weapon, line
                        .substring(line.indexOf("previous owner: ") + "previous owner: ".length(), line.length() - 1) };
            default:
                return new String[] {};
        }
    }

    private long findTime() {
        int minutes = Integer.parseInt(line.substring(1, 3));
        int seconds = Integer.parseInt(line.substring(4, 6));
        return minutes * 60L + seconds;
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
}
