package xyz.msws.admintools.utils;

public class MSG {
    public static String plural(String str, int amo) {
        return amo == 1 ? str : (str + "s");
    }
}
