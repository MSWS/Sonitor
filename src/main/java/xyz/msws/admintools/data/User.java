package xyz.msws.admintools.data;

import com.lukaspradel.steamapi.core.exception.SteamApiException;
import com.lukaspradel.steamapi.data.json.playerstats.GetUserStatsForGame;
import com.lukaspradel.steamapi.data.json.playerstats.Playerstats;
import com.lukaspradel.steamapi.data.json.playerstats.Stat;
import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient;
import com.lukaspradel.steamapi.webapi.request.SteamWebApiRequest;
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import xyz.msws.admintools.utils.Convert;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents any user on any server
 * <p>
 * Compares by creation date
 */
public class User implements Comparable<User> {
    private String serverName, steamId;
    private int userId;
    private long steamCom, date = -1, accountAge = -1;
    //    private long playtime = -1;
    private Map<String, Long> playtime = new HashMap<>();
    private boolean isEstimate = false;
    private Playerstats stats;

    // prestigegaming.gameme.com/r/playerinfo/csgo3/STEAM_0:1:186661284

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

    public void fetchGameME(String server) {
        try {
            Document doc = Jsoup.connect("http://prestigegaming.gameme.com/r/playerinfo/" + server + "/" + steamId).get();
            Element time = doc.select("td").get(22);
            playtime.put(server, Convert.gameMETime(time.ownText()));
        } catch (IOException e) {
            e.printStackTrace();
//            playtime = 0;
            playtime.put(server, 0L);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Invalid playerinfo format for " + steamId);
            playtime.put(server, 0L);
        }
    }

    public long getPlaytime(String server) {
        if (!playtime.containsKey(server))
            fetchGameME(server);
        return playtime.getOrDefault(server, -1L);
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

    public void setUserId(int id) {
        this.userId = id;
    }

    public void getStats(SteamWebApiClient client, Config config) {
        if (!config.requestGametimes())
            return;
        if (stats == null || !config.cacheGametimes()) {
            SteamWebApiRequest gametime = SteamWebApiRequestFactory.createGetUserStatsForGameRequest(config.getAppId(), steamCom + "");
            GetUserStatsForGame result;
            try {
                result = client.processRequest(gametime);
                this.stats = result.getPlayerstats();
            } catch (SteamApiException ignored) {
            }
        }
    }

    public long getAccountAge() {
        if (stats == null)
            return -1;
        if (accountAge != -1)
            return accountAge;
        Stat stat = stats.getStats().stream().filter(s -> s.getName().equals("total_time_played")).findFirst().orElse(null);
        if (stat == null)
            return -1;
        accountAge = (long) stat.getValue() * 1000;

        return accountAge; // Time is in seconds
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
