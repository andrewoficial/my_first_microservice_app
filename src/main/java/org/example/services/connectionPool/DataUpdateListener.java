package org.example.services.connectionPool;

import com.fasterxml.jackson.databind.JsonNode;

public interface DataUpdateListener {
    void onDataUpdated(String clientId, String action, String data, String cmd, JsonNode payload);
}