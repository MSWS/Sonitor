package xyz.msws.admintools.data;

import xyz.msws.admintools.data.DataStructs.ActionType;
import xyz.msws.admintools.data.DataStructs.GenericActionType;
import xyz.msws.admintools.data.jb.JailActionType;
import xyz.msws.admintools.data.ttt.TTTActionType;
import xyz.msws.admintools.utils.Convert;
import xyz.msws.admintools.utils.Utils;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static xyz.msws.admintools.utils.FileUtils.readFile;
import static xyz.msws.admintools.utils.FileUtils.saveResource;

/**
 * Flatfile implementation of {@link Config}
 */
public class FileConfig extends Config {
    public FileConfig(File file) {
        String sets = readFile(file);
        if (sets.isEmpty()) {
            saveResource("settings.txt");

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
                    ActionType type = null;
                    try {
                        type = GenericActionType.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e1) {
                        try {
                            type = JailActionType.valueOf(s.toUpperCase());
                        } catch (IllegalArgumentException e2) {
                            try {
                                type = TTTActionType.valueOf(s.toUpperCase());
                            } catch (IllegalArgumentException e3) {
                            }
                        }
                    }
                    if (type == null) {
                        System.out.println("Invalid action type: " + s);
                        continue;
                    }
                    actions.add(type);
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
            } else if (line.startsWith("doJailbreak=")) {
                doJailbreak = getValue(line, "doJailbreak=", Boolean.class);
            } else if (line.startsWith("doTTT=")) {
                doTTT = getValue(line, "doTTT=", Boolean.class);
            } else if (line.startsWith("cacheGametimes=")) {
                cacheGametimes = getValue(line, "cacheGametimes=", Boolean.class);
            } else if (line.startsWith("requestGametimes=")) {
                cacheGametimes = getValue(line, "requestGametimes=", Boolean.class);
            } else if (line.startsWith("appId=")) {
                appId = getValue(line, "appId=", Integer.class);
            } else if (line.startsWith("webId=")) {
                webId = getValue(line, "webId=");
                if (webId.isBlank())
                    webId = null;
            } else if (line.startsWith("playtimeUnit=")) {
                limitPlaytime = getValue(line, "playtimeUnit=", Convert.TimeUnit.class);
                if (limitPlaytime == null)
                    limitPlaytime = Convert.TimeUnit.YEARS;
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
        } else if (clazz.isEnum()) {
            result = Utils.getEnum(getValue(value, key), (Class<? extends Enum>) clazz);
        }

        return clazz.cast(result);
    }
}
