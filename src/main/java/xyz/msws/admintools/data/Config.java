package xyz.msws.admintools.data;

import java.util.EnumSet;

public abstract class Config {
    protected long rate = 5000;
    protected long timeout = 5000;
    protected boolean cache = true, persistGuesses = true, persistKnowns = true, warnNameChanges = true;
    protected String header = "";
    protected String outputPath = null, apiKey = null, clonePath = null;
    protected EnumSet<ActionType> actions = EnumSet.of(ActionType.KILL, ActionType.WARDEN, ActionType.WARDEN_DEATH, ActionType.FIRE, ActionType.PASS, ActionType.RESKIN);
    protected int gunTimeout = 10, buttonTimeout = 5, nadeTimeout = 10;
    protected boolean showEarlyVents = true, showEarlyKills = true;

    public EnumSet<ActionType> getActions() {
        return actions;
    }

    public int getGunTimeout() {
        return gunTimeout;
    }

    public int getButtonTimeout() {
        return buttonTimeout;
    }

    public int getNadeTimeout() {
        return nadeTimeout;
    }

    public boolean showEarlyVents() {
        return showEarlyVents;
    }

    public boolean showEarlyKills() {
        return showEarlyKills;
    }


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
