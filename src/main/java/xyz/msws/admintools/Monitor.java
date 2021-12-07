package xyz.msws.admintools;

import xyz.msws.admintools.data.Config;
import xyz.msws.admintools.data.FileConfig;
import xyz.msws.admintools.parsers.JBParser;
import xyz.msws.admintools.parsers.Parser;
import xyz.msws.admintools.parsers.PlaytimeParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static xyz.msws.admintools.utils.FileUtils.readFile;
import static xyz.msws.admintools.utils.FileUtils.writeFile;

public class Monitor extends TimerTask {

    // File that this jar is running in
    private final File master = new File(System.getProperty("user.dir"));

    // Settings file for configuration
    private final File settings = new File(master, "settings.txt");
    private File output, clone;
    private Config config;
    private final Timer timer = new Timer();
    private final Set<Parser> parsers = new HashSet<>();

    public Monitor() {
        if (!setupFiles())
            return;
        if (config.doPlaytime())
            parsers.add(new PlaytimeParser(this));
        if (config.doJailbreak())
            parsers.add(new JBParser(this));
        timer.schedule(this, config.getRate(), config.getRate());
    }

    public void run() {
        String lines = readFile(output);
        for (String line : lines.split("\n")) {
            parse(line.trim());
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
    }

    private final List<String> directories = new ArrayList<>();

    private boolean setupFiles() {
        config = new FileConfig(settings);

        if (config.getOutputPath() != null) {
            output = new File(config.getOutputPath());

            directories.add(config.getOutputPath());
            if (output != null) {
                directories.add(output.getAbsolutePath());
                if (output.exists())
                    directories.add(output.getParentFile().getAbsolutePath());
            }
        }

        // Drive Specific
        directories.add(master.getAbsolutePath().charAt(0) + ":\\Program Files (x86)\\Steam\\steamapps\\common\\Counter-Strike Global Offensive\\csgo");
        directories.add(master.getAbsolutePath().charAt(0) + ":\\Program Files\\Steam\\steamapps\\common\\Counter-Strike Global Offensive\\csgo");

        // Default Computer
        directories.add("C:\\Program Files (x86)\\Steam\\steamapps\\common\\Counter-Strike Global Offensive\\csgo");
        directories.add("C:\\Program Files\\Steam\\steamapps\\common\\Counter-Strike Global Offensive\\csgo");

        // Mac
        directories.add("~/Library/Application Support/Steam/steamapps/common/Counter-Strike Global Offensive/csgo");

        // Linux
        directories.add("~/.steam/steam/SteamApps/common/Counter-Strike Global Offensive/csgo");

        for (int i = 'A'; i <= 'Z'; i++) {
            directories.add((char) i + ":\\Program Files (x86)\\Steam\\steamapps\\common\\Counter-Strike Global Offensive\\csgo");
            directories.add((char) i + ":\\Program Files\\Steam\\steamapps\\common\\Counter-Strike Global Offensive\\csgo");
        }

        File parent = null;
        for (String dir : directories) {
            parent = new File(dir);
            if (parent.isFile())
                parent = parent.getParentFile();
            if (parent.exists()) {
                System.out.println("Located CS:GO directory at " + parent.getAbsolutePath());
                break;
            }
        }
        if (!parent.exists()) {
            System.out.println("Could not locate CS:GO directory. Please specify the proper directory in the settings.txt");
            return false;
        }

        File auto = new File(parent, "cfg" + File.separatorChar + "autoexec.cfg");
        if (!auto.exists()) {
            try {
                System.out.println("Automatically generated an autoexec.cfg file at " + auto.getAbsolutePath());
                auto.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        List<String> lines = new ArrayList<>(List.of(readFile(auto).split("\n")));
        int index = -1;

        for (String line : lines) {
            if (line.trim().toLowerCase().startsWith("con_logfile")) {
                index = lines.indexOf(line);
                break;
            }
        }
        if (index == -1) {
            System.out.println("Appending con_logfile output.log to autoexec.cfg");
            lines.add("con_logfile output.log");
            index = lines.size() - 1;
            writeFile(auto, String.join("\n", lines));
        }

        String logFile = config.getOutputPath();
        if (index >= lines.size()) {
            System.out.println("Could not find con_logfile directory in autoexec.cfg");
            System.out.println("Using provided log path: " + logFile);
        } else {
            logFile = lines.get(index).substring(lines.get(index).indexOf(" ") + 1);
        }

        if (!output.exists() || output.isDirectory())
            output = new File(parent, logFile);
        if (!output.exists()) {
            output = new File(logFile);
            if (!output.exists()) {
                System.out.println("Could not locate " + output.getName() + " at " + output.getAbsolutePath());
                System.out.println("If you have not run CS:GO yet, do so and then re-run this application");
                System.out.println("If CS:GO has already been run, ensure the \"con_logfile\" variable is");
                System.out.println("set to " + logFile + " in your console by running:");
                System.out.println("con_logfile " + output.getName());
                return false;
            }
        }

        if (config.getClonePath() != null && !config.getClonePath().isEmpty())
            clone = new File(config.getClonePath());
        return true;
    }

    private void parse(String line) {
        parsers.forEach(p -> p.parse(line));
    }

    public Config getConfig() {
        return config;
    }

}
