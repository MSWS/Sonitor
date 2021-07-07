package xyz.msws.steamage;

import com.lukaspradel.steamapi.core.exception.SteamApiException;
import com.lukaspradel.steamapi.data.json.playersummaries.GetPlayerSummaries;
import com.lukaspradel.steamapi.data.json.playersummaries.Player;
import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient;
import com.lukaspradel.steamapi.webapi.request.SteamWebApiRequest;
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Monitor {
    private final Map<Long, Long> userCache = new HashMap<>();
    private File master = new File(System.getProperty("user.dir"));
    private File cacheFile = new File(master, "cache.txt");
    private File settings = new File(master, "settings.txt");
    private File output, clone;
    private SteamWebApiClient client;

    private String outputPath = null, apiKey = null, clonePath = null;

    private long rate = 5000;
    private boolean cache = true, persist = true;
    private String header = "";

    private List<User> users = new ArrayList<>(), unknown = new ArrayList<>();

    public void run() {
        if (!setupFiles())
            return;

        while (true) {
            String lines = readFile(output);
            for (String line : lines.split("\n")) {
                parse(line);
            }

            try (FileWriter writer = new FileWriter(output)) {
                writer.write("");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (clone != null) {
                try {
                    clone.createNewFile();
                    FileWriter writer = new FileWriter(clone, true);
                    writer.write(lines);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(rate);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean setupFiles() {
        try {
            settings.createNewFile();
            cacheFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String sets = readFile(settings);

        if (sets.isEmpty()) {
            try (FileWriter write = new FileWriter(settings)) {
                write.write("This is where the output file from CS:GO is located\n");
                write.write("directory=C:\\Program Files (x86)\\Steam\\steamapps\\common\\Counter-Strike Global Offensive\\csgo\\output.log\n");
                write.write("\n");
                write.write("This is your steam API Key, open the link if you do not know where to get it\n");
                write.write("steamkey=https://steamcommunity.com/dev/apikey\n");
                write.write("\n");
                write.write("If true, we will cache the results during runtime, reducing API calls (RECOMMENDED TRUE)\n");
                write.write("cache=true\n");
                write.write("\n");
                write.write("If true, we will save the results over multiple executions, reducing API calls (RECOMMENDED TRUE)\n");
                write.write("persist=true\n");
                write.write("\n");
                write.write("If you want to save logs to another file, specify the path here\n");
                write.write("clonePath=\n");
                write.write("\n");
                write.write("Rate determines how often the output file is scanned, lower = less latency, but may consume more resources\n");
                write.write("Numbers are in milliseconds, 1000 ms = 1 second\n");
                write.write("rate=5000\n");
                write.write("\n");
                write.write("The header is printed after each status output in console, useful for distinguishing outputs\n");
                write.write("header={}{}{}{}=================================");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sets = readFile(settings);
        for (String line : sets.split("\n")) {
            if (line.startsWith("directory=")) {
                outputPath = line.substring("directory=".length());
            } else if (line.startsWith("steamkey=")) {
                apiKey = line.substring("steamkey=".length());
            } else if (line.startsWith("cache=")) {
                cache = Boolean.parseBoolean(line.substring("cache=".length()));
            } else if (line.startsWith("persist=")) {
                persist = Boolean.parseBoolean(line.substring("persist=".length()));
            } else if (line.startsWith("clonePath=")) {
                clonePath = line.substring("clonePath=".length());
            } else if (line.startsWith("rate=")) {
                rate = Long.parseLong(line.substring("rate=".length()));
            } else if (line.startsWith("header="))
                header = line.substring("header=".length());
        }

        client = new SteamWebApiClient.SteamWebApiClientBuilder(apiKey).build();

        if (persist) {
            String[] lines = Objects.requireNonNull(readFile(cacheFile)).split("\n");
            for (String line : lines) {
                if (line == null || !line.contains(":"))
                    continue;
                userCache.put(Long.parseLong(line.split(":")[0]), Long.parseLong(line.split(":")[1]));
            }
        }

        if (outputPath == null) {
            System.out.println("No path has been specified in the settings.txt");
            return false;
        }

        output = new File(outputPath);

        if (!output.exists()) {
            output = new File("C:\\Program Files (x86)\\Steam\\steamapps\\common\\Counter-Strike Global Offensive\\csgo", outputPath.isEmpty() ? "output.log" : outputPath);
            if (!output.exists()) {
                System.out.println(output.getAbsolutePath() + " was not found!");
                System.out.println("Please make sure you have con_logfile enabled");
                System.out.println("If you have, make sure both the file NAME and EXTENSION");
                System.out.println("are correct. If they are, make sure to specify it in");
                System.out.println("the settings.txt file located at " + settings.getAbsolutePath());
                return false;
            }
        }

        if (clonePath != null && !clonePath.isEmpty())
            clone = new File(clonePath);
        return true;
    }

    private String readFile(File f) {
        if (f == null || !f.exists())
            return null;
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
            return new String(in.readAllBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void parse(String line) {
        if (!line.startsWith("#"))
            return;
        if (line.contains("# userid name uniqueid connected ping loss state rate")) {
            users = new ArrayList<>();
            unknown = new ArrayList<>();
            return;
        }
        if (line.contains("#end")) {
            int offset = 0;
            while (!unknown.isEmpty()) {
                GetPlayerSummaries response = getUserSummaries(unknown, offset);
                List<Player> players = response.getResponse().getPlayers();
                processPlayers(players);
                removeKnownNames();
                offset++;
            }

            if (persist)
                try (FileWriter writer = new FileWriter(cacheFile)) {
                    for (Map.Entry<Long, Long> entry : userCache.entrySet()) {
                        writer.write(String.format("%d:%d\n", entry.getKey(), entry.getValue()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            Collections.sort(users);
            if (header != null)
                Arrays.stream(header.split("\\{}")).forEach(System.out::println);
            for (User user : users)
                System.out.printf("#%d %s: %s\n", user.getUserId(), user.getName(), Convert.timeToStr(System.currentTimeMillis() - user.getDate()));
            return;
        }

        if (!line.contains("STEAM_"))
            return;
        line = line.replaceAll("\\s{2,}", " ");
        String[] parts = line.split(" ");
        if (parts.length < 6)
            return;
        String id = parts[parts.length - 6];
        if (!id.startsWith("STEAM_"))
            return;
        String name = line.substring(line.indexOf("\"") + 1, line.indexOf("\"", line.indexOf("\"") + 1));
        User user = new User(parts[1], name, id);
        if (userCache.containsKey(user.getCommunityID()) && cache) {
            user.setDate(userCache.get(user.getCommunityID()));
            users.add(user);
        } else {
            unknown.add(user);
        }
    }

    private void processPlayers(List<Player> players) {
        for (Player p : players) {
            if (p.getTimecreated() == null)
                continue;
            User user = null;
            long currentDiff = Long.MAX_VALUE;
            for (User u : unknown) {
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
            if (currentDiff > 0)
                user.setName("*" + user.getName());
            else
                userCache.put(user.getCommunityID(), p.getTimecreated().longValue());
            user.setDate(p.getTimecreated() * 1000L);
            users.add(user);
        }
    }

    private void removeKnownNames() {
        Iterator<User> it = unknown.iterator();
        while (it.hasNext()) {
            User u = it.next();
            if (u.getDate() == -1)
                continue;
            userCache.put(u.getCommunityID(), u.getDate());
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


}
