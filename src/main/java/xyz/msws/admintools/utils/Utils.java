package xyz.msws.admintools.utils;

public class Utils {
    public static <T extends Enum> T getEnum(String query, Class<T> en) {
        for (T e : en.getEnumConstants()) {
            if (query.equalsIgnoreCase(e.toString()))
                return e;
        }
        for (T e : en.getEnumConstants()) {
            if (e.toString().startsWith(query.toUpperCase()))
                return e;
        }
        for (T e : en.getEnumConstants()) {
            if (e.toString().contains(query.toUpperCase()))
                return e;
        }
        return null;
    }
}
