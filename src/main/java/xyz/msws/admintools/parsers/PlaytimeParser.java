package xyz.msws.admintools.parsers;

import com.lukaspradel.steamapi.core.exception.SteamApiException;
import com.lukaspradel.steamapi.data.json.playersummaries.GetPlayerSummaries;
import com.lukaspradel.steamapi.data.json.playersummaries.Player;
import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient;
import com.lukaspradel.steamapi.webapi.request.SteamWebApiRequest;
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory;
import xyz.msws.admintools.Monitor;
import xyz.msws.admintools.data.User;
import xyz.msws.admintools.utils.Convert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static xyz.msws.admintools.utils.FileUtils.readFile;

/**
 * Parsers CSGO/CSS status and prints account ages
 */
public class PlaytimeParser extends Parser {
    private List<User> users = new ArrayList<>(), unknown = new ArrayList<>();
    long lastStatus = -1;
    private final SteamWebApiClient client;
    private final File master = new File(System.getProperty("user.dir"));
    private final File cacheFile = new File(master, "cache.txt");
    private final Map<String, String> nameCache = new HashMap<>();
    private final Map<Long, Long> userCache = new HashMap<>();
    private final Map<String, User> newCache = new HashMap<>();
    private String webId = null;

    public PlaytimeParser(Monitor monitor) {
        super(monitor);
        client = new SteamWebApiClient.SteamWebApiClientBuilder(config.getApiKey()).build();
        try {
            cacheFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (config.persistKnowns()) {
            String[] lines = Objects.requireNonNull(readFile(cacheFile)).split("\n");
            for (String line : lines) {
                if (line == null || !line.contains(":"))
                    continue;
                userCache.put(Long.parseLong(line.split(":")[0]), Long.parseLong(line.split(":")[1]));
            }
        }
    }

    public void parse(String line) {
        if (config.getWebId() == null && line.startsWith("hostname:")) {
            checkWebID(line);
            return;
        }
        if (line.contains("# userid name uniqueid connected ping loss state rate") || line.contains("# userid name                uniqueid            connected ping loss state")) {
            reset();
            return;
        }
        if (line.contains("#end") || (lastStatus != -1 && System.currentTimeMillis() - lastStatus > config.getTimeout() && config.getTimeout() != -1)) {
            lastStatus = -1;
            // Sometimes CS doesn't print the #end at the end
            int offset = 0;
            while (!unknown.isEmpty()) {
                GetPlayerSummaries response = getUserSummaries(unknown, offset);
                if (response == null) {
                    System.out.println("Unable to query Steam API, is your API Key invalid?");
                    return;
                }
                List<Player> players = response.getResponse().getPlayers();
                processPlayers(players);
                removeKnownNames(unknown.iterator());
                offset++;
            }

            if (config.persistKnowns())
                try (FileWriter writer = new FileWriter(cacheFile)) {
                    for (Map.Entry<Long, Long> entry : userCache.entrySet())
                        writer.write(String.format("%d:%d\n", entry.getKey(), entry.getValue()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            Collections.sort(users);
            if (config.getHeader() != null)
                Arrays.stream(config.getHeader().split("\\\\n")).forEach(System.out::println);
            List<String> changes = new ArrayList<>();
            int longestName = 0, longestAge = 1, longestPlaytime = 1;
            for (User user : users) {
                String ls = user.getServerName();
                if (ls.length() + 1 > longestName)
                    longestName = ls.length() + 1;
                ls = Convert.timeToStr(System.currentTimeMillis() - user.getDate());
                if (ls.length() + 1 > longestAge)
                    longestAge = ls.length() + 1;
                if (webId == null || user.getPlaytime(webId) <= 0)
                    continue;
                ls = Convert.timeToStr(user.getPlaytime(webId));
                if (ls.length() + 1 > longestPlaytime)
                    longestPlaytime = ls.length() + 1;
            }
            System.out.println(" ".repeat(longestName + longestAge - 10) + " Account Age | Server Playtime | Game Time");
            for (User user : users) {
                String age = Convert.timeToStr(System.currentTimeMillis() - user.getDate());
                String playtime = webId != null ? Convert.timeToStr(user.getPlaytime(webId)) : "";
                String account = user.getAccountAge() > 0 ? Convert.timeToStr(user.getAccountAge()) : "";

                System.out.printf("#%-3d %-" + longestName + "s %" + longestAge + "s | %-" + longestPlaytime + "s | %-15s\n",
                        user.getUserId(), user.getServerName(), age, playtime, account);

                if (config.warnNameChanges()) {
                    String currentName = user.getServerName();
                    nameCache.putIfAbsent(user.getSteamId(), currentName);
                    String oldName = nameCache.get(user.getSteamId());
                    if (!user.getServerName().equals(oldName)) {
                        changes.add(String.format("#%d %s changed their username to %s\n", user.getUserId(), oldName, currentName));
                        nameCache.put(user.getSteamId(), currentName);
                    }
                }
            }
            if (changes.isEmpty())
                return;
            System.out.println();
            System.out.println(String.join("\n", changes));
            return;
        }
        if (!line.startsWith("#"))
            return;
        boolean source = line.contains("[U:") && !line.contains("STEAM_");
        if (!source && !line.contains("STEAM_"))
            return;
        line = line.replaceAll("\\s{2,}", " ");
        String[] parts = line.split(" ");
        if (parts.length < 6)
            return;
        String id = parts[parts.length - 6];

        if (!id.startsWith("STEAM_")) {
            id = parts[parts.length - 5];
            if (!id.startsWith("[U:")) {
                System.out.println("Expected STEAM_ at " + id + ". Has the status format changed?");
                return;
            }
            id = Convert.sourceSteamToSteam(id.substring(1, id.length() - 1));
        }
        String name = line.substring(line.indexOf("\"") + 1, line.indexOf("\"", line.indexOf("\"") + 1));
        newCache.putIfAbsent(id, new User(parts[1], name, id));

        User user = newCache.get(id);
        user.setServerName(name);
        user.setUserId(Integer.parseInt(parts[1]));
        user.getStats(client, config);

        if (config.doCache() && userCache.containsKey(user.getCommunityID())) {
            long time = userCache.get(user.getCommunityID());
            user.setDate(Math.abs(time), time < 0);
            users.add(user);
        } else {
            unknown.add(user);
        }
    }

    private void reset() {
        users = new ArrayList<>();
        unknown = new ArrayList<>();
        lastStatus = System.currentTimeMillis();
    }

    private void checkWebID(String line) {
        String old = webId;
        switch (line) {
            case "hostname:     =(eGO)= MINIGAMES | AWP+ | 102 TICK | !WS !KNIFE | EdgeGame" -> webId = "csgo";
            case "hostname:     =(eGO)= TTT | TROUBLE IN TERRORIST TOWN | EdgeGamers.com" -> webId = "csgo2";
            case "hostname:     =(eGO)= | JAILBREAK | GANGS+ | EdgeGamers.com" -> webId = "csgo3";
            case "hostname:     Hydra Sabers Beta | TDM | Jedi vs Sith" -> webId = "csgo4";
            case "hostname:     =(eGO)= BEGINNER SURF 24/7 | 85 TICK | !WS !KNIFE | EdgeGam" -> webId = "csgo5";
            case "hostname:     =(eGO)= EASY BHOP 24/7 | 102 TICK | !WS !KNIFE | EdgeGamers" -> webId = "csgo6";
            case "hostname:     =(eGO)= 2FORT | US | FAST RESPAWN | EdgeGamers.com" -> webId = "tf";
            default -> {
                System.out.println("Unknown server: " + line);
                webId = null;
            }
        }
        if (old == null || !old.equals(webId))
            System.out.println("Auto-detected server: " + line.substring("hostname:     ".length()) + " using " + webId);
    }

    private void removeKnownNames(Iterator<User> it) {
        while (it.hasNext()) {
            User u = it.next();
            if (u.getDate() == -1)
                continue;
            it.remove();
        }
    }

    private GetPlayerSummaries getUserSummaries(List<User> users, int offset) {
        return getSummaries(users.stream().map(s -> (s.getCommunityID() + offset) + "").collect(Collectors.toList()));
    }

    private GetPlayerSummaries getSummaries(List<String> users) {
        System.out.printf("Getting %d new users...\n", users.size());
        SteamWebApiRequest request = SteamWebApiRequestFactory.createGetPlayerSummariesRequest(users);
        try {
            return client.processRequest(request);
        } catch (SteamApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void processPlayers(List<Player> players) {
        for (Player p : players) {
            if (p.getTimecreated() == null)
                continue;

            User user = null;
            long currentDiff = Long.MAX_VALUE;
            for (User u : unknown) {
                // In this case, communityID should be close to Steamid
                // The Player's steamid is the one that we know the creation date of
                // The user's communityid will vary slightly, the less diff, the more accurate the time is
                long diff = Math.abs(u.getCommunityID() - Long.parseLong(p.getSteamid()));
                if (diff >= currentDiff)
                    continue;
                user = u;
                currentDiff = diff;
                if (diff == 0)
                    break;
            }
            if (user == null) {
                System.out.printf("ERROR: Unable to get steam age of %s", p.getPersonaname());
                continue;
            }

            if (currentDiff == 0 /* Not a guess */ || config.persistGuesses())
                userCache.put(user.getCommunityID(), (currentDiff == 0 ? p.getTimecreated().longValue() : -p.getTimecreated().longValue()) * 1000L);

            user.setDate(p.getTimecreated() * 1000L, currentDiff != 0);
            users.add(user);
        }
    }
}
