package xyz.msws.steamage;

import org.junit.Assert;
import org.junit.Test;

public class Convert {
    public static long steamToCommunity(String steam) {
        if (steam == null || !steam.startsWith("STEAM_"))
            return -1;
        String[] parts = steam.substring("STEAM_".length()).split(":");
        if (parts.length != 3)
            return -1;
        int x = Integer.parseInt(parts[0]), y = Integer.parseInt(parts[1]), z = Integer.parseInt(parts[2]);
        return z * 2L + 0x0110000100000000L + y;
    }

    public static String communityToSteam(long community) {
        int y = community % 2 == 0 ? 0 : 1;
        int result = (int) (community / 2 - 0x0110000100000000L / 2);
        return String.format("STEAM_0:%d:%d", y, result);
    }

    enum TimeUnit {
        MILLISECONDS(1), SECONDS(1000), MINUTES(1000 * 60), HOURS(1000 * 60 * 60), DAYS(1000 * 60 * 60 * 24), MONTHS(1000L * 60 * 60 * 24 * 30), YEARS(1000L * 60 * 60 * 24 * 30 * 12);
        private long ms;

        TimeUnit(long ms) {
            this.ms = ms;
        }

        public long toMillis(long duration) {
            return ms * duration;
        }
    }

    public static String timeToStr(long ms) {
        TimeUnit unit = TimeUnit.YEARS;
        for (TimeUnit u : TimeUnit.values()) {
            if (ms < u.toMillis(1))
                break;
            unit = u;
        }

        double p = (double) ms / unit.toMillis(1);

        if (p == 1) // If it's exactly 1 unit, don't have an S at the end of the unit
            return "1 " + unit.toString().toLowerCase().substring(0, unit.toString().length() - 1);

        if (p == (int) p) // If it's exactly a number of units, don't specify decimals
            return (int) p + " " + unit.toString().toLowerCase();

        return String.format("%.2f %s", (double) ms / unit.toMillis(1), unit.toString().toLowerCase());
    }


    @Test
    public void testNull() {
        Assert.assertEquals(steamToCommunity(null), -1);
    }

    @Test
    public void testEmpty() {
        Assert.assertEquals(steamToCommunity(""), -1);
    }

    @Test
    public void testInvalid() {
        Assert.assertEquals(steamToCommunity("INVALID"), -1);
    }

    @Test
    public void testRegular() {
        Assert.assertEquals(steamToCommunity("STEAM_0:1:186661284"), 76561198333588297L);
    }

    @Test
    public void testRegularCommunity() {
        Assert.assertEquals(communityToSteam(76561197970977166L), "STEAM_0:0:5355719");
        Assert.assertEquals(communityToSteam(76561198333588297L), "STEAM_0:1:186661284");

    }
}
