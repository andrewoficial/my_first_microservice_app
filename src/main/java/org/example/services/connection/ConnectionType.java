package org.example.services.connection;

import java.util.Arrays;

/**
 * Type of device connection for the main window left panel.
 * Not nested in GUI — used by services, state, and forms.
 * Only {@link #isSelectable()} types appear in the connection-type combo.
 */
public enum ConnectionType {
    COM("COM", true),
    HID("HID", true),
    WEBSOCKET("WebSocket", true),
    BLE("BLE", false);

    private final String displayName;
    private final boolean selectable;

    ConnectionType(String displayName, boolean selectable) {
        this.displayName = displayName;
        this.selectable = selectable;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Shown in MainWindow jcbConnectionType when true. */
    public boolean isSelectable() {
        return selectable;
    }

    public static ConnectionType[] selectableValues() {
        return Arrays.stream(values())
                .filter(ConnectionType::isSelectable)
                .toArray(ConnectionType[]::new);
    }

    @Override
    public String toString() {
        return displayName;
    }

    /** Parse config / UI storage; unknown → COM. */
    public static ConnectionType fromStoredName(String name) {
        if (name == null || name.isBlank()) {
            return COM;
        }
        String n = name.trim();
        for (ConnectionType t : values()) {
            if (t.name().equalsIgnoreCase(n) || t.displayName.equalsIgnoreCase(n)) {
                return t;
            }
        }
        return COM;
    }
}
