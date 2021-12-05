package xyz.msws.admintools.data;

public abstract class Config {
    long rate = 5000;
    long timeout = 5000;
    boolean cache = true, persistGuesses = true, persistKnowns = true, warnNameChanges = true;
    String header = "";
    String outputPath = null, apiKey = null, clonePath = null;

    public long getTimeout() {
        return timeout;
    }

    public long getRate() {
        return rate;
    }

    public boolean doCache() {
        return cache;
    }

    public boolean persistGuesses() {
        return persistGuesses;
    }

    public boolean persistKnowns() {
        return persistKnowns;
    }

    public boolean warnNameChanges() {
        return warnNameChanges;
    }

    public String getHeader() {
        return header;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getClonePath() {
        return clonePath;
    }
}
