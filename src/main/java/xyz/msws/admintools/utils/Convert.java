package xyz.msws.admintools.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Converts Steam ID's from one format to another
 */
public class Convert {
    // Convert from STEAM_1:1:45244579 to 76561198050754887

    /**
     * Converts Steam ID to Steam 64
     *
     * @param steam
     * @return
     */
    public static long steamToCommunity(String steam) {
        if (steam == null)
            throw new NullPointerException("Steam ID is null");
        if (!steam.startsWith("STEAM_"))
            throw new IllegalArgumentException("Not a 64 steam ID");
        String[] parts = steam.substring("STEAM_".length()).split(":");
        if (parts.length != 3)
            return -1;
        int x = Integer.parseInt(parts[0]), y = Integer.parseInt(parts[1]), z = Integer.parseInt(parts[2]);
        return z * 2L + 0x0110000100000000L + y;
    }

    // Convert back from 76561198050754887 to STEAM_1:1:45244579

    /**
     * Convert Steam 64 back to Steam ID
     *
     * @param community
     * @return
     */
    public static String communityToSteam(long community) {
        int y = community % 2 == 0 ? 0 : 1;
        int result = (int) (community / 2 - 0x0110000100000000L / 2);
        return String.format("STEAM_0:%d:%d", y, result);
    }

    // Convert from [U:1:90489159] to STEAM_0:1:45244579

    /**
     * Convert Steam 3 to Steam 64
     *
     * @param steam
     * @return
     */
    public static String sourceSteamToSteam(String steam) {
        if (steam == null)
            throw new NullPointerException("Steam ID is null");
        if (!steam.startsWith("U:"))
            throw new IllegalArgumentException("Not a 64 steam ID");
        String[] parts = steam.substring("U:".length()).split(":");
        if (parts.length != 2)
            return null;
        int y = Integer.parseInt(parts[0]), z = Integer.parseInt(parts[1]);
        int type = z % 2 == 0 ? 0 : 1;
        int id = (int) Math.floor((double) (z - type) / 2);
        return "STEAM_0:" + type + ":" + id;
    }

    enum TimeUnit {
        MILLISECONDS(1), SECONDS(1000), MINUTES(1000 * 60), HOURS(1000 * 60 * 60), DAYS(1000 * 60 * 60 * 24), MONTHS(1000L * 60 * 60 * 24 * 30), YEARS(1000L * 60 * 60 * 24 * 30 * 12);
        private final long ms;

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

    // 31 days 04:27:56 hours
    // 0d 00:00h
    // 4d 18:55h
    // 04:28:49
    public static long gameMETime(String input) {
        if (input.endsWith(" hours"))
            input = input.substring(0, input.length() - " hours".length());
        if (input.endsWith("h"))
            input = input.substring(0, input.length() - 1);
        String[] parts = input.split(" ");
        if (parts[0].endsWith("d"))
            parts[0] = parts[0].substring(0, parts[0].length() - 1);
        String[] hours = parts[parts.length - 1].split(":");

        long time = 0;
        try {
            time += Integer.parseInt(hours[0]) * TimeUnit.HOURS.ms;
            time += Integer.parseInt(hours[1]) * TimeUnit.MINUTES.ms;
            if (hours.length == 3)
                time += Integer.parseInt(hours[2]) * TimeUnit.SECONDS.ms;

            if (parts.length == 1)
                return time;
            time += Integer.parseInt(parts[0]) * TimeUnit.DAYS.ms;
        } catch (NumberFormatException e) {
            System.out.println("Unable to parse time of " + input);
            e.printStackTrace();
        }

        return time;
    }

    @Test
    public void testZero() {
        Assert.assertEquals(gameMETime("0d 00:00h"), 0);
    }

    @Test
    public void testDay() {
        Assert.assertEquals(gameMETime("1d 00:00 hours"), TimeUnit.DAYS.toMillis(1));
    }

    @Test
    public void testDayAlt() {
        Assert.assertEquals(gameMETime("1 days 00:00:00h"), TimeUnit.DAYS.toMillis(1));
    }

    @Test
    public void testMonth() {
        Assert.assertEquals(gameMETime("31 days 04:27:56 hours"), 2694476000L);
    }

    @Test
    public void testSimple() {
        Assert.assertEquals(gameMETime("04:28:49"), 16129000);
    }

    @Test(expected = NullPointerException.class)
    public void testNull() {
        steamToCommunity(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmpty() {
        steamToCommunity("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid() {
        steamToCommunity("INVALID");
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
