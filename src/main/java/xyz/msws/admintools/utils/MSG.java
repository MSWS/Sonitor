package xyz.msws.admintools.utils;

/**
 * String manipulations
 */
public class MSG {
    /**
     * Appends s if amo != 1
     *
     * @param str String to append to
     * @param amo amo
     * @return pluralized str
     */
    public static String plural(String str, int amo) {
        return amo == 1 ? str : (str + "s");
    }
}
