package com.bakkenbaeck.token.model.network;

import java.util.List;

public class AppSearch {
    private int offset;
    private String query;
    private int limit;
    private List<App> apps;

    public List<App> getApps() {
        return apps;
    }
}
