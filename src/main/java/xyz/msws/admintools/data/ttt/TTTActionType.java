package xyz.msws.admintools.data.ttt;

import xyz.msws.admintools.data.DataStructs.ActionType;

public enum TTTActionType implements ActionType {
    IDENTIFY("found %s's body"), DNA("DNA'd %s's body"),
    SHOP("bought %s"), T_SECRET("activated %s"), TAZE("tazed %s");

    private final String sum;

    TTTActionType(String summary) {
        this.sum = summary;
    }

    @Override
    public String getSummary(String... opts) {
        return String.format(sum, (Object[]) opts);
    }

}
