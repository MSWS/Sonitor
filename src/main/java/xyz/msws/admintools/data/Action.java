package xyz.msws.admintools.data;

import java.util.ArrayList;
import java.util.List;

public class Action implements Comparable<Action> {
    private ActionType type;
    private String player, target = null;
    private Role playerRole, targetRole;
    private String[] other;
    private String line;

    private int playerRoleStart = Integer.MAX_VALUE, playerRoleEnd, targetRoleStart = -1, targetRoleEnd = -1;
    private int playerStart, playerEnd;
    private int targetStart, targetEnd;

    private long time;

    public Action(String line) {
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
            return ActionType.WARDEN;
        } else if (line.endsWith("broke a vent or wall")) {
            return ActionType.VENTS;
        } else if (line.contains(") pressed button '")) {
            return ActionType.BUTTON;
        } else if (line.contains("threw a") && (line.endsWith("smoke") || line.endsWith("grenade") || line.endsWith("flash") || line.endsWith("decoy") || line.endsWith("molotov"))) {
            return ActionType.NADE;
        } else if (line.contains("hurt") && line.contains("with") && line.contains("damage (")) {
            return ActionType.DAMAGE;
        } else if (line.contains("has died and is no longer warden")) {
            return ActionType.WARDEN_DEATH;
        } else if (line.contains("killed ")) {
            return ActionType.KILL;
        } else if (line.contains("dropped the weapon")) {
            return ActionType.DROP_WEAPON;
        } else if (line.endsWith("has been fired by an admin")) {
            return ActionType.FIRE;
        } else if (line.endsWith("has passed warden")) {
            return ActionType.PASS;
        } else if (line.contains("reskinned weapon_")) {
            return ActionType.RESKIN;
        } else if (line.endsWith("was respawned for touching a worldspawn.")) {
            return ActionType.GHOST_RESPAWN;
        }

        throw new IllegalArgumentException("Invalid line: " + line);
    }

    private void calculateIndices() {
        for (Role role : Role.values()) {
            int index = line.toUpperCase().indexOf("(" + role.toString() + ")");
            if (index != -1) {
                index++;
            } else if (role == Role.WORLD) {
                index = line.toUpperCase().indexOf((role.toString()));
            } else {
                index = line.indexOf("(" + role.toString().charAt(0) + role.toString().substring(1).toLowerCase() + ")") + 1;
            }

            if (index <= 0 || index > playerRoleStart)
                continue;
            if (role == Role.WORLD) {
                playerRoleStart = index;
                playerRoleEnd = playerRoleStart + "The World".length();
                playerRole = Role.WORLD;
            } else {
                playerRoleStart = index;
                playerRoleEnd = playerRoleStart + role.toString().length();
                playerRole = Role.valueOf(line.substring(playerRoleStart, playerRoleEnd).toUpperCase());
            }
        }
        boolean world = playerRole == Role.WORLD;

        playerStart = 8;
        System.out.printf("role %s, start %d, end %d\n", playerRole.toString(), playerRoleStart, playerRoleEnd);
        playerEnd = playerRoleStart - (world ? -5 : 2);

        if (type != ActionType.DAMAGE && type != ActionType.KILL)
            return;

        for (Role role : Role.values()) {
//            int index = line.toUpperCase().lastIndexOf("(" + role.toString() + ")") + 1;
            int index = role == Role.WARDEN ? line.toUpperCase().lastIndexOf(role.toString()) : (line.toUpperCase().lastIndexOf("(" + role + ")") + 1);

            if (index <= 0 || index == playerRoleStart)
                continue;
            targetRoleStart = index;
            targetRoleEnd = targetRoleStart + role.toString().length();
            targetRole = Role.valueOf(line.substring(targetRoleStart, targetRoleEnd).toUpperCase());
            break;
        }

        String damageLine = line.substring(playerRoleEnd + (world ? 1 : 2), line.indexOf(" ", playerRoleEnd + (world ? 1 : 2)));

        targetStart = playerRoleEnd + (world ? 2 : 3) + damageLine.length();
        targetEnd = targetRoleStart - 2;
    }

    private String findPlayer() {
        System.out.println("line: " + line);
        return line.substring(playerStart, playerEnd);
    }

    private String findTarget() {
        if (targetStart == -1 || targetEnd == -1)
            return null;
        return line.substring(targetStart, targetEnd);
    }

    private Role findPlayerRole() {
        return Role.valueOf(line.substring(playerRoleStart + 1, playerRoleEnd - 1).toUpperCase());
    }

    private Role findTargetRole() {
        if (targetRoleStart == -1 || targetRoleEnd == -1)
            return null;
        return Role.valueOf(line.substring(targetRoleStart + 1, targetRoleEnd - 1).toUpperCase());
    }

    private String[] findOther() {
        return switch (type) {
            case DAMAGE -> new String[]{line.substring(line.lastIndexOf("with ") + "with ".length(), line.lastIndexOf("damage") - 1), line.substring(line.lastIndexOf("(") + 1, line.length() - 1)};
            case BUTTON -> new String[]{line.substring(line.lastIndexOf("button ") + "button ".length())};
            case DROP_WEAPON -> new String[]{line.substring(line.lastIndexOf(" ") + 1, line.length() - 1)};
            case NADE -> new String[]{line.substring(line.lastIndexOf(" ") + 1)};
            case RESKIN -> new String[]{line.substring(line.indexOf("reskinned weapon_") + "reskinned weapon_".length(), line.indexOf(" (previous owner:")), line.substring(line.indexOf("previous owner: ") + "previous owner: ".length(), line.length() - 1)};
            default -> new String[]{};
        };
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
        if (playerRole == Role.WORLD)
            return player + " " + type.getSummary(opts);

        return player + " (" + getPlayerRole().getIcon() + ") " + type.getSummary(opts);
    }

    public long getTime() {
        return time;
    }

    @Override
    public int compareTo(Action o) {
        return (int) (this.getTime() - o.getTime());
    }
}
