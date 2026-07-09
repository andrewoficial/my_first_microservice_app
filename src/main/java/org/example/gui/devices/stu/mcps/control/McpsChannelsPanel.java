package org.example.gui.devices.stu.mcps.control;

import org.example.gui.devices.stu.mcps.AsyncLogger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class McpsChannelsPanel extends JPanel implements ChannelPulseCoordinator {

    private static final int CHANNEL_COUNT = 8;
    private static final int POLL_INTERVAL_MS = 800;

    private final McpsCommunicationService service;
    private final AsyncLogger logger;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private final List<channelSettings> blocks = new ArrayList<>();
    private volatile int minCommandIntervalMs = 37;
    private volatile boolean anyPulseActive = false;
    private volatile boolean backgroundPollingEnabled = true;
    private ScheduledFuture<?> pollFuture;

    public McpsChannelsPanel(McpsCommunicationService service, AsyncLogger logger, int minCommandIntervalMs) {
        this.service = service;
        this.logger = logger;
        this.minCommandIntervalMs = minCommandIntervalMs;
        setLayout(new GridLayout(2, 4, 8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int i = 1; i <= CHANNEL_COUNT; i++) {
            channelSettings block = new channelSettings(i, service, logger, this, minCommandIntervalMs);
            blocks.add(block);
            add(block.getRootPanel());
        }

        service.addResponseListener(this::handleResponse);
        startPolling();
    }

    private void startPolling() {
        pollFuture = scheduler.scheduleAtFixedRate(() -> {
            if (backgroundPollingEnabled && service.isConnected()) {
                service.readAllOutputs();
            }
        }, 500, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void setBackgroundPollingEnabled(boolean enabled) {
        this.backgroundPollingEnabled = enabled;
    }

    private void handleResponse(String response) {
        SwingUtilities.invokeLater(() -> {
            if (response.startsWith("@RAOU ")) {
                String bits = response.substring(6).trim();
                for (int ch = 1; ch <= CHANNEL_COUNT && ch <= bits.length(); ch++) {
                    boolean on = bits.charAt(ch - 1) == '1';
                    blocks.get(ch - 1).updateActualState(on);
                }
            } else if (response.startsWith("@RA") && response.length() > 5) {
                try {
                    int ch = Integer.parseInt(response.substring(3, 5));
                    if (ch >= 1 && ch <= CHANNEL_COUNT) {
                        boolean on = response.trim().endsWith("1");
                        blocks.get(ch - 1).updateActualState(on);
                    }
                } catch (Exception ignored) {}
            } else if (response.startsWith("@WR") && response.contains("OK")) {
                try {
                    int ch = Integer.parseInt(response.substring(3, 5));
                    if (ch >= 1 && ch <= CHANNEL_COUNT) {
                        scheduleAutoRead(ch);
                    }
                } catch (Exception ignored) {}
            } else if (response.contains("OK")) {
                logger.debug("Команда подтверждена: " + response);
            }
        });
    }

    private void scheduleAutoRead(int channel) {
        scheduler.schedule(() -> {
            if (service.isConnected()) {
                service.readOutput(channel);
            }
        }, minCommandIntervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setAllControlsEnabled(boolean enabled) {
        for (channelSettings b : blocks) {
            b.setControlsEnabled(enabled);
        }
    }

    public void setMinCommandIntervalMs(int ms) {
        this.minCommandIntervalMs = ms;
        for (channelSettings b : blocks) {
            b.setMinCommandIntervalMs(ms);
        }
    }

    public void setGlobalPulseActive(boolean active) {
        this.anyPulseActive = active;
        setAllControlsEnabled(!active);
    }

    public void shutdown() {
        if (pollFuture != null) pollFuture.cancel(true);
        scheduler.shutdownNow();
        for (channelSettings b : blocks) {
            b.stopPulse();
        }
    }

    @Override
    public boolean isAnyPulseActive() {
        return anyPulseActive;
    }

    @Override
    public void onPulseStateChanged(boolean active) {
        this.anyPulseActive = active;
    }
}
