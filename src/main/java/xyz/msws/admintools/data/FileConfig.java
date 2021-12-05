package xyz.msws.admintools.data;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static xyz.msws.admintools.utils.FileUtils.readFile;

public class FileConfig extends Config {

    public FileConfig(File file) {
        String sets = readFile(file);
        if (sets.isEmpty()) {

            List<String> lines = new ArrayList<>();
            lines.add("This is where the output file from CS:GO is located");
            lines.add("directory=C:\\Program Files (x86)\\Steam\\steamapps\\common\\Counter-Strike Global Offensive\\csgo\\output.log");
            lines.add("");
            lines.add("If true, the application will check and output account ages");
            lines.add("doPlaytime=true");
            lines.add("");
            lines.add("This is your steam API Key, open the link if you do not know where to get it");
            lines.add("steamkey=https://steamcommunity.com/dev/apikey");
            lines.add("");
            lines.add("If true, we will cache the results during runtime, reducing API calls (RECOMMENDED TRUE)");
            lines.add("cache=true");
            lines.add("");
            lines.add("If true, we will save accurate ages over multiple executions, reducing API calls (RECOMMENDED TRUE)");
            lines.add("persist=true");
            lines.add("");
            lines.add("If you want to save logs to another file, specify the path here");
            lines.add("clonePath=");
            lines.add("");
            lines.add("Rate determines how often the output file is scanned, lower = less latency, but may consume more resources");
            lines.add("Numbers are in milliseconds, 1000 ms = 1 second");
            lines.add("rate=5000");
            lines.add("");
            lines.add("The header is printed after each status output in console, useful for distinguishing outputs");
            lines.add("header=\\n\\n\\n=================================");
            lines.add("");
            lines.add("Some profiles have their visibility set to private, this application guesstimates based off other accounts that were made immediately before/after");
            lines.add("If false, we will try to fetch, if true, we will save the estimated guess");
            lines.add("persistGuessses=true");
            lines.add("");
            lines.add("Source sometimes fails to print the #end of a status command. If this happens,");
            lines.add("we will assume the status has ended after X milliseconds (-1 to disable)");
            lines.add("statusTimeout=5000");
            lines.add("");
            lines.add("Some people may break a rule and quickly disconnect. This checks if the same");
            lines.add("steam account joins with a name that is different than the one they had previously");
            lines.add("warnNameChages=true");
            lines.add("");
            lines.add("Jailbreak Specific Settings");
            lines.add("Log specific summarized actions");
            lines.add("Values: DAMAGE, KILL, BUTTON, WARDEN, VENTS, DROP_WEAPON, NADE, WARDEN_DEATH, PASS, FIRE, RESKIN, GHOST_RESPAWN");
            lines.add("Damage: Whenever a player damages another player");
            lines.add("Kill: Whenever a player kills another player");
            lines.add("Button: Whenever a player pushes a button");
            lines.add("Warden: Whenever a player takes warden");
            lines.add("Vents: Whenever a player breaks vents");
            lines.add("Drop_Weapon: Whenever a player drops a weapon");
            lines.add("Nade: Whenever a player throws a nade");
            lines.add("Warden_Death: Whenever the warden dies");
            lines.add("Pass: Whenever the warden passes");
            lines.add("Fire: Whenever the warden is fired");
            lines.add("Reskin: Whenever a player reskins a weapon");
            lines.add("Ghost_Respawn: Whenever a ghost respawns");
            lines.add("showTypes=KILL,WARDEN,WARDEN_DEATH,FIRE,PASS,RESKIN");
            lines.add("Cooldown for when a CT drops a gun and a T uses the same type of gun (seconds)");
            lines.add("gundropTimeout=10");
            lines.add("Cooldown for when a player pushes a button and a player takes damage from the world (seconds)");
            lines.add("buttonTimeout=5");
            lines.add("Cooldown for when a player throws a nade and a player takes damage from the world (seconds)");
            lines.add("nadeTimeout=10");
            lines.add("Time that prisoners have when a new warden is selected (seconds)");
            lines.add("wardenTimeout=5");
            lines.add("Max time round can go without a warden until it is a freeday (when warden passes/fired) (seconds)");
            lines.add("freeTime=10");
            lines.add("Show if a CT breaks vents before any prisoner does");
            lines.add("showEarlyVents=true");
            lines.add("Show if a CT kills/damages a prisoner when there is no warden, or within 3 seconds of a new warden");
            lines.add("showEarlyKills=true");
            lines.add("Show if a player pushes a button and other players take damage from the world within buttonTimeout");
            lines.add("showGameButtons=true");
            lines.add("Show if a player throws a nade and other players take damage from the world within nadeTimeout");
            lines.add("showNades=true");
            lines.add("Show if a CT drops a gun and a prisoner uses the same gun within gundropTimeout");
            lines.add("showGunPlants=true");

            try (FileWriter write = new FileWriter(file)) {
                write.write(String.join("\n", lines));
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Created settings.txt located at " + file.getAbsolutePath());
            System.out.println("Please follow the guide at https://msws.xyz/age");
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sets = readFile(file);
        for (String line : sets.split("\n")) {
            if (line.startsWith("directory=")) {
                outputPath = getValue(line, "directory=");
            } else if (line.startsWith("steamkey=")) {
                apiKey = getValue(line, "steamkey=");
            } else if (line.startsWith("cache=")) {
                cache = getValue(line, "cache=", Boolean.class);
            } else if (line.startsWith("persist=")) {
                persistKnowns = getValue(line, "persist=", Boolean.class);
            } else if (line.startsWith("clonePath=")) {
                clonePath = getValue(line, "clonePath=");
            } else if (line.startsWith("rate=")) {
                rate = getValue(line, "rate=", Long.class);
            } else if (line.startsWith("header=")) {
                header = getValue(line, "header=");
            } else if (line.startsWith("persistGuesses=")) {
                persistGuesses = getValue(line, "persistGuesses=", Boolean.class);
            } else if (line.startsWith("warnNameChanges=")) {
                warnNameChanges = getValue(line, "warnNameChanges=", Boolean.class);
            } else if (line.startsWith("statusTimeout=")) {
                timeout = getValue(line, "statusTimeout=", Long.class);
            } else if (line.startsWith("showTypes=")) {
                String[] acts = getValue(line, "showTypes=").split(",");
                actions.clear();
                for (String s : acts) {
                    actions.add(ActionType.valueOf(s.toUpperCase()));
                }
            } else if (line.startsWith("gundropTimeout=")) {
                gunTimeout = getValue(line, "gundropTimeout=", Integer.class);
            } else if (line.startsWith("nadeTimeout=")) {
                nadeTimeout = getValue(line, "nadeTimeout=", Integer.class);
            } else if (line.startsWith("showEarlyVents=")) {
                showEarlyVents = getValue(line, "showEarlyVents=", Boolean.class);
            } else if (line.startsWith("showEarlyKills=")) {
                showEarlyKills = getValue(line, "showEarlyKills=", Boolean.class);
            } else if (line.startsWith("showGameButtons=")) {
                showGameButtons = getValue(line, "showGameButtons=", Boolean.class);
            } else if (line.startsWith("showNades=")) {
                showNades = getValue(line, "showNades=", Boolean.class);
            } else if (line.startsWith("showGunPlants=")) {
                showGunPlants = getValue(line, "showGunPlants=", Boolean.class);
            } else if (line.startsWith("doPlaytime=")) {
                doPlaytime = getValue(line, "doPlaytime=", Boolean.class);
            } else if (line.startsWith("wardenTimeout=")) {
                wardenTimeout = getValue(line, "wardenTimeout=", Integer.class);
            } else if (line.startsWith("freeTime=")) {
                freeTime = getValue(line, "freeTime=", Integer.class);
            }
        }
    }

    private String getValue(String value, String key) {
        return value.substring(key.length());
    }

    private <T> T getValue(String value, String key, Class<T> clazz) {
        Object result = null;
        if (clazz == boolean.class || clazz == Boolean.class) {
            result = Boolean.parseBoolean(getValue(value, key));
        } else if (clazz == String.class) {
            result = getValue(value, key);
        } else if (clazz == long.class || clazz == Long.class) {
            result = Long.parseLong(getValue(value, key));
        } else if (clazz == double.class || clazz == Double.class) {
            result = Double.parseDouble(getValue(value, key));
        } else if (clazz == int.class || clazz == Integer.class) {
            result = Integer.parseInt(getValue(value, key));
        }

        return clazz.cast(result);
    }
}
