package xyz.msws.admintools.data;

import xyz.msws.admintools.utils.Convert;

public class SID {
    private long community;
    private String id, thirtyTwo;

    public SID(String id) {
        this.id = id.startsWith("U:") ? Convert.sourceSteamToSteam(id) : id;
        this.community = Convert.steamToCommunity(id);
    }

    public SID(long com) {
        this.id = Convert.communityToSteam(com);
        this.community = com;
    }

    public long getCommunity() {
        return community;
    }

    public String getId() {
        return id;
    }
}