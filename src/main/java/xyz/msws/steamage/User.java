package xyz.msws.steamage;

public class User implements Comparable<User> {

    private String serverName, steamId;
    private int userId;
    private long steamCom, date = -1;
    private boolean isEstimate = false;

    public User(int userid, String serverName, long steam) {
        this.userId = userid;
        this.serverName = serverName;
        this.steamCom = steam;
        this.steamId = Convert.communityToSteam(steam);
    }

    public User(int userid, String serverName, String steam) {
        this(userid, serverName, Convert.steamToCommunity(steam));
        this.steamId = steam;
    }

    public User(String userid, String serverName, String steam) {
        this(Integer.parseInt(userid), serverName, steam);
    }

    public long getCommunityID() {
        return steamCom;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getUserId() {
        return userId;
    }

    public void setDate(long date) {
        setDate(date, false);
    }

    public void setDate(long date, boolean estimate) {
        this.date = date;
        this.isEstimate = estimate;
    }

    public long getDate() {
        return date;
    }

    public void setEstimate(boolean est) {
        this.isEstimate = est;
    }

    public boolean isEstimate() {
        return isEstimate;
    }

    public String getSteamId() {
        return steamId;
    }

    @Override
    public int compareTo(User o) {
        return Long.compare(date, o.getDate());
    }
}
