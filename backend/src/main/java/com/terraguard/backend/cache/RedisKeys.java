package com.terraguard.backend.cache;

public final class RedisKeys {

    private RedisKeys() {}

    public static final String GLOBAL_SNAPSHOT = "terraguard:snapshot:global";
    public static final String SNAPSHOT_DIRTY  = "terraguard:snapshot:dirty";

    public static String signalTally(String incidentId) {
        return "terraguard:signals:" + incidentId;
    }

    public static String chatMessages(String incidentId) {
        return "terraguard:chat:" + incidentId;
    }
}