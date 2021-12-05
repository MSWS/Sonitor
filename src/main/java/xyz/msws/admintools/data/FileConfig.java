package xyz.msws.admintools.data;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static xyz.msws.admintools.utils.FileUtils.readFile;

public class FileConfig extends Config {
    public FileConfig(File file) {
        String sets = readFile(file);
        if (sets.isEmpty()) {
            try (FileWriter write = new FileWriter(file)) {
                write.write("This is where the output file from CS:GO is located\n");
                write.write("directory=C:\\Program Files (x86)\\Steam\\steamapps\\common\\Counter-Strike Global Offensive\\csgo\\output.log\n");
                write.write("\n");
                write.write("This is your steam API Key, open the link if you do not know where to get it\n");
                write.write("steamkey=https://steamcommunity.com/dev/apikey\n");
                write.write("\n");
                write.write("If true, we will cache the results during runtime, reducing API calls (RECOMMENDED TRUE)\n");
                write.write("cache=true\n");
                write.write("\n");
                write.write("If true, we will save accurate ages over multiple executions, reducing API calls (RECOMMENDED TRUE)\n");
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
                write.write("header=\\n\\n\\n=================================\n");
                write.write("\n");
                write.write("Some profiles have their visibility set to private, this application guesstimates based off other accounts that were made immediately before/after\n");
                write.write("If false, we will try to fetch, if true, we will save the estimated guess\n");
                write.write("persistGuessses=true\n");
                write.write("\n");
                write.write("Source sometimes fails to print the #end of a status command. If this happens,\n");
                write.write("we will assume the status has ended after X milliseconds (-1 to disable)\n");
                write.write("statusTimeout=5000\n");
                write.write("\n");
                write.write("Some people may break a rule and quickly disconnect. This checks if the same\n");
                write.write("steam account joins with a name that is different than the one they had previously\n");
                write.write("warnNameChages=true\n");
                write.write("\n");
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
                cache = Boolean.parseBoolean(getValue(line, "cache="));
            } else if (line.startsWith("persist=")) {
                persistKnowns = Boolean.parseBoolean(getValue(line, "persist="));
            } else if (line.startsWith("clonePath=")) {
                clonePath = getValue(line, "clonePath=");
            } else if (line.startsWith("rate=")) {
                rate = Long.parseLong(getValue(line, "rate="));
            } else if (line.startsWith("header=")) {
                header = getValue(line, "header=");
            } else if (line.startsWith("persistGuesses=")) {
                persistGuesses = Boolean.parseBoolean(getValue(line, "persistGuesses="));
            } else if (line.startsWith("warnNameChanges=")) {
                warnNameChanges = Boolean.parseBoolean(getValue(line, "warnNameChanges="));
            } else if (line.startsWith("statusTimeout=")) {
                timeout = Long.parseLong(getValue(line, "statusTimeout="));
            }
        }
    }

    private String getValue(String value, String key) {
        return value.substring(key.length());
    }
}
