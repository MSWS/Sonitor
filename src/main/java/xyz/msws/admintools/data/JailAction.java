package xyz.msws.admintools.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a line in Jailbreak Logs
 * <p>
 * Compares by time
 */
public class JailAction implements Comparable<JailAction> {
    private JailActionType type;
    private String player, target;
    private JailRole playerRole, targetRole;
    private String[] other;
    private String line;

    private int playerRoleStart = Integer.MAX_VALUE, playerRoleEnd, targetRoleStart = -1, targetRoleEnd = -1;
    private int playerStart, playerEnd;
    private int targetStart, targetEnd;

    private long time;

    public JailAction(String line) {
        this.line = line;
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

    public JailRole getPlayerRole() {
        return playerRole;
    }

    public JailRole getTargetRole() {
        return targetRole;
    }

    public JailActionType getType() {
        return type;
    }

    public String[] getOther() {
        return other;
    }

    private JailActionType findActionType() {
        if (line.endsWith("is now warden")) {
            return JailActionType.WARDEN;
        } else if (line.endsWith("broke a vent or wall")) {
            return JailActionType.VENTS;
        } else if (line.contains(") pressed button '")) {
            return JailActionType.BUTTON;
        } else if (line.contains("threw a") && (line.endsWith("smoke") || line.endsWith("grenade") || line.endsWith("flash") || line.endsWith("decoy") || line.endsWith("molotov"))) {
            return JailActionType.NADE;
        } else if (line.contains("hurt") && line.contains("with") && line.contains("damage (")) {
            return JailActionType.DAMAGE;
        } else if (line.contains("has died and is no longer warden")) {
            return JailActionType.WARDEN_DEATH;
        } else if (line.contains("killed ")) {
            return JailActionType.KILL;
        } else if (line.contains("dropped the weapon")) {
            return JailActionType.DROP_WEAPON;
        } else if (line.endsWith("has been fired by an admin")) {
            return JailActionType.FIRE;
        } else if (line.endsWith("has passed warden")) {
            return JailActionType.PASS;
        } else if (line.contains("reskinned weapon_")) {
            return JailActionType.RESKIN;
        } else if (line.contains("was respawned for touching")) {
            return JailActionType.GHOST_RESPAWN;
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
                index = line.indexOf("(" + role.toString().charAt(0) + role.toString().substring(1).toLowerCase() + ")") + 1;
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

        if (type != JailActionType.DAMAGE && type != JailActionType.KILL)
            return;

        for (JailRole role : JailRole.values()) {
//            int index = line.toUpperCase().lastIndexOf("(" + role.toString() + ")") + 1;
            int index = role == JailRole.WARDEN ? line.toUpperCase().lastIndexOf(role.toString()) : (line.toUpperCase().lastIndexOf("(" + role + ")") + 1);

            if (index <= 0 || index == playerRoleStart)
                continue;
            targetRoleStart = index;
            targetRoleEnd = targetRoleStart + role.toString().length();
            targetRole = JailRole.valueOf(line.substring(targetRoleStart, targetRoleEnd).toUpperCase());
            break;
        }

        String damageLine = line.substring(playerRoleEnd + (world ? 1 : 2), line.indexOf(" ", playerRoleEnd + (world ? 1 : 2)));

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
        switch (type) {
            case KILL:
                return new String[]{targetRole.getIcon()};
            case DAMAGE:
                return new String[]{line.substring(line.lastIndexOf("with ") + "with ".length(), line.lastIndexOf("damage") - 1), line.substring(line.lastIndexOf("(") + 1, line.length() - 1)};
            case BUTTON:
                String name = line.substring(line.substring(0, line.length() - 1).lastIndexOf(line.contains("pressed button 'Unknown'") ? "(" : "'", line.length() - 2) + 1, line.length() - 1);
                Button b = ButtonDatabase.getInstance().getButton(name);
                return new String[]{b.getName(), b.getAlias()};
            case DROP_WEAPON:
                return new String[]{line.substring(line.lastIndexOf(" ") + 1, line.length() - 1)};
            case NADE:
                return new String[]{line.substring(line.lastIndexOf(" ") + 1)};
            case RESKIN:
                String weapon = line.substring(line.indexOf("reskinned weapon_") + "reskinned weapon_".length(), line.indexOf(" ", line.lastIndexOf("weapon_")));
                if (line.endsWith("(not previously owned)")) {
                    return new String[]{weapon, "their own"};
                }
                return new String[]{weapon, line.substring(line.indexOf("previous owner: ") + "previous owner: ".length(), line.length() - 1)};
            default:
                return new String[]{};
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

    @Override
    public int compareTo(JailAction o) {
        return (int) (this.getTime() - o.getTime());
    }
}
