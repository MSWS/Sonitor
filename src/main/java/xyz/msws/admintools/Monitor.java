package xyz.msws.admintools;

import xyz.msws.admintools.data.Config;
import xyz.msws.admintools.data.FileConfig;
import xyz.msws.admintools.parsers.JBParser;
import xyz.msws.admintools.parsers.Parser;
import xyz.msws.admintools.parsers.PlaytimeParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static xyz.msws.admintools.utils.FileUtils.readFile;

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

    private boolean setupFiles() {
        try {
            settings.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        config = new FileConfig(settings);

        if (config.getOutputPath() == null) {
            System.out.println("No path has been specified in the settings.txt");
            return false;
        }

        output = new File(config.getOutputPath());

        if (!output.exists()) {
            output = new File("C:\\Program Files (x86)\\Steam\\steamapps\\common\\Counter-Strike Global Offensive\\csgo", config.getOutputPath().isEmpty() ? "output.log" : config.getOutputPath());
            if (!output.exists()) {
                System.out.println(output.getAbsolutePath() + " was not found!");
                System.out.println("Please make sure you have con_logfile enabled");
                System.out.println("If you have, make sure both the file NAME and EXTENSION");
                System.out.println("are correct. If they are, make sure to specify it in");
                System.out.println("the settings.txt file located at " + settings.getAbsolutePath());
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
