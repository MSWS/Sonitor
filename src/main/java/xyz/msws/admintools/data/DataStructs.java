package xyz.msws.admintools.data;

public class DataStructs {
    public static interface ActionType {
        String getSummary(String... opts);
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
