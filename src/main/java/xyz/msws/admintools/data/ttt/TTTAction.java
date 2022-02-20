package xyz.msws.admintools.data.ttt;

import xyz.msws.admintools.data.Action;
import xyz.msws.admintools.data.DataStructs.ActionType;
import xyz.msws.admintools.data.DataStructs.GenericActionType;

public class TTTAction extends Action {
    public TTTAction(String line) {
        super(line);

        this.type = findActionType();
        calculateIndices();

        this.player = findPlayer();
        this.target = findTarget();
        this.other = findOther();
    }

    private ActionType findActionType() {
        if (line.contains("threw a") && (line.endsWith("smoke") || line.endsWith("grenade")
                || line.endsWith("flash") || line.endsWith("decoy") || line.endsWith("molotov"))) {
            return GenericActionType.NADE;
        } else if (line.contains("killed ")) {
            return this.isBadAction() ? TTTActionType.BAD_KILL : GenericActionType.KILL;
        } else if (line.contains("damaged") && line.contains("for") && line.contains("with")) {
            return this.isBadAction() ? TTTActionType.BAD_DAMAGE : GenericActionType.DAMAGE;
        } else if (line.contains("identified body of")) {
            return TTTActionType.IDENTIFY;
        } else if (line.contains("purchased an item from the shop:")) {
            return TTTActionType.SHOP;
        } else if (line.contains("used traitor secret:")) {
            return TTTActionType.T_SECRET;
        } else if (line.contains("scanned a body, Killer was")) {
            return TTTActionType.DNA;
        } else if (line.contains("was tased by")) {
            return TTTActionType.TAZE;
        }

        throw new IllegalArgumentException("Invalid line: " + line);
    }

    private void calculateIndices() {
        for (TTTRole role : TTTRole.values()) {
            int index = line.toUpperCase().indexOf("(" + role.toString() + ")");
            if (index != -1) {
                index++;
            } else if (role == TTTRole.WORLD) {
                index = line.toUpperCase().indexOf((role.toString()));
            } else {
                index = line.indexOf("(" + role.toString().charAt(0) + role.toString().substring(1).toLowerCase() + ")")
                        + 1;
            }

            if (index <= 0 || index > playerRoleStart)
                continue;
            if (role == TTTRole.WORLD) {
                playerRoleStart = index;
                playerRoleEnd = playerRoleStart + "The World".length();
                playerRole = TTTRole.WORLD;
            } else {
                playerRoleStart = index;
                playerRoleEnd = playerRoleStart + role.toString().length();
                playerRole = TTTRole.valueOf(line.substring(playerRoleStart, playerRoleEnd).toUpperCase());
            }
        }

        if (playerRole == null) {
            System.out.println("Unknown line: " + line);
            return;
        }
        boolean world = playerRole == TTTRole.WORLD;

        playerStart = 12;
        playerEnd = playerRoleStart - (world ? -5 : 2);

        if (type != GenericActionType.DAMAGE && type != GenericActionType.KILL && type != TTTActionType.BAD_DAMAGE
                && type != TTTActionType.BAD_KILL)
            return;

        for (TTTRole role : TTTRole.values()) {
            int index = line.toUpperCase().lastIndexOf("(" + role + ")") + 1;

            if (index <= 0 || index == playerRoleStart)
                continue;
            targetRoleStart = index;
            targetRoleEnd = targetRoleStart + role.toString().length();
            targetRole = TTTRole.valueOf(line.substring(targetRoleStart, targetRoleEnd).toUpperCase());
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
                            targetRole.getIcon(),
                            line.substring(line.lastIndexOf(") for ") + ") for ".length(),
                                    line.lastIndexOf(" damage ") - 1),
                            line.substring(line.lastIndexOf(" ") + 1, line.length() - 1) };
                case NADE:
                    return new String[] { line.substring(line.lastIndexOf(" ") + 1) };
                default:
                    return new String[] {};
            }
        }
        if (!(type instanceof TTTActionType tttType))
            return new String[] { "Invalid Type" };
        switch (tttType) {
            case BAD_KILL:
                return new String[] { targetRole.getIcon() };
            case BAD_DAMAGE:
                return new String[] {
                        targetRole.getIcon(),
                        line.substring(line.lastIndexOf(") for ") + ") for ".length(),
                                line.lastIndexOf(" damage ") - 1),
                        line.substring(line.lastIndexOf(" ") + 1, line.length() - 1) };
            case IDENTIFY:
                return new String[] {
                        line.substring(line.lastIndexOf("identified body of ") + "identified body of ".length(),
                                line.lastIndexOf("(") - 1) };
            case DNA:
                return new String[] { line.substring(
                        line.indexOf("scanned a body, Killer was ") + "scanned a body, Killer was ".length(),
                        line.lastIndexOf("(") - 1) };
            case SHOP:
                return new String[] {
                        line.substring(line.lastIndexOf("shop: ") + "shop: ".length(), line.length() - 1) };
            case T_SECRET:
                return new String[] {
                        line.substring(line.lastIndexOf("traitor secret: ") + "traitor secret: ".length(),
                                line.length() - 1) };
            case TAZE:
                return new String[] { line.substring(line.lastIndexOf("was tased by ") + "was tased by ".length(),
                        line.length() - 1) };
            default:
                return new String[] {};
        }
    }

    public boolean isBadAction() {
        return line.endsWith("BAD ACTION");
    }
}
