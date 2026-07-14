package org.example.services.connectionPool;


import tools.jackson.databind.JsonNode;

public interface DataUpdateListener {
    void onDataUpdated(String clientId, String action, String data, String cmd, JsonNode payload);
}