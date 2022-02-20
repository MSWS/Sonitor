package xyz.msws.admintools.data;

public class DataStructs {
    public static interface ActionType {
        String getSummary(String... opts);
    }

    public static enum GenericActionType implements ActionType {
        DAMAGE("damaged %s for %s"), KILL("killed %s (%s)"), NADE("threw a(n) %s"), GHOST_RESPAWN("respawned as ghost");

        String sum;

        GenericActionType(String summary) {
            this.sum = summary;
        }

        @Override
        public String getSummary(String... opts) {
            return String.format(sum, (Object[]) opts);
        }

    }

    public static interface Role {
        String getIcon();

        boolean isCT();

        boolean isT();

        default boolean isAlive() {
            return isCT() || isT();
        }
    }
}
