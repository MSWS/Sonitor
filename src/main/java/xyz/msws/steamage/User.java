package xyz.msws.steamage;

public class User implements Comparable<User> {
    

    private String name, steamId;
    private int userId;
    private long steamCom, date = -1;


    public User(int userid, String name, long steam) {
        this.userId = userid;
        this.name = name;
        this.steamCom = steam;
        this.steamId = Convert.communityToSteam(steam);
    }

    public User(int userid, String name, String steam) {
        this(userid, name, Convert.steamToCommunity(steam));
        this.steamId = steam;
    }

    public User(String userid, String name, String steam) {
        this(Integer.parseInt(userid), name, steam);
    }

    public long getCommunityID() {
        return steamCom;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUserId() {
        return userId;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getDate() {
        return date;
    }

    public String getSteamId() {
        return steamId;
    }

    @Override
    public int compareTo(User o) {
        return Long.compare(date, o.getDate());
    }
}
