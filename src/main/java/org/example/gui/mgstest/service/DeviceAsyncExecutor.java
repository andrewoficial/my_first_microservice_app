package org.example.gui.mgstest.service;
import org.apache.log4j.Logger;
import org.example.gui.mgstest.repository.DeviceState;
import org.example.gui.mgstest.repository.DeviceStateRepository;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.HidCommandName;
import org.hid4java.HidDevice;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class DeviceAsyncExecutor {
    private static final Logger log = Logger.getLogger(DeviceAsyncExecutor.class);
    
    private final ExecutorService executorService;
    private final DeviceStateRepository stateRepository;
    private final Map<HidDevice, Future<?>> runningTasks;
    private final List<MgsExecutionListener> listeners;
    
    public DeviceAsyncExecutor(DeviceStateRepository stateRepository) {
        this.stateRepository = stateRepository;
        this.executorService = Executors.newCachedThreadPool();
        this.runningTasks = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }
    
    public <T> byte[] executeCommand(DeviceCommand command, CommandParameters parameters, HidDevice device) {

        
        // Проверяем, не выполняется ли уже команда для этого устройства
        if (runningTasks.containsKey(device)) {
            notifyExecutionListeners(device, "Device is already executing a command", true);
        }
        
        // Обновляем состояние устройства
        DeviceState state = getOrCreateDeviceState(device);
        state.setIsBusy(true);
        state.setProgressPercent(0);
        state.setProgressMessage("Starting operation...");
        state.setCurrentOperation(command.getDescription());
        
        // Создаем MgsExecutionListener для этой команды
        MgsExecutionListener mgsExecutionListener = createProgressUpdater();
        
        // Запускаем задачу
        Future<?> future = executorService.submit(() -> {
            try {
                log.info("Starting async execution of: " + command.getDescription() + " for device: " + device.getId());
                
                command.execute(device, parameters, mgsExecutionListener);
                
                // Завершение успешно
                SwingUtilities.invokeLater(() -> {
                    state.setIsBusy(false);
                    state.setProgressPercent(100);
                    state.setProgressMessage("Completed successfully");
                    notifyExecutionListeners(device, "Command completed successfully", false);
                    //return result;
                });

                log.info("Completed async execution of: " + command.getDescription() + " for device: " + device.getId());
                
            } catch (Exception e) {
                log.error("Error during async execution of " + command.getDescription() + " for device: " + device.getId(), e);
                
                SwingUtilities.invokeLater(() -> {
                    state.setIsBusy(false);
                    state.setProgressMessage("Error: " + e.getMessage());
                    notifyExecutionListeners(device, "Error: " + e.getMessage(), true);
                    //return null;
                });
            } finally {
                runningTasks.remove(device);
            }
        });
        
        runningTasks.put(device, future);
        notifyExecutionListeners(device, "Command started: " + command.getDescription(), false);
        return null;
    }
    
    public void cancelCommand(HidDevice deviceId) {
        Future<?> future = runningTasks.get(deviceId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            runningTasks.remove(deviceId);
            
            DeviceState state = stateRepository.get(deviceId);
            if (state != null) {
                state.setIsBusy(false);
                state.setProgressMessage("Operation cancelled");
            }
            
            notifyExecutionListeners(deviceId, "Command cancelled", true);
        }
    }
    
    public boolean isDeviceBusy(HidDevice deviceId) {
        Future<?> future = runningTasks.get(deviceId);
        return future != null && !future.isDone();
    }
    
    public void addListener(MgsExecutionListener listener) {
        listeners.add(listener);
    }
    

    private MgsExecutionListener createProgressUpdater() {
        return new MgsExecutionListener() {

            @Override
            public void onExecutionEvent(HidDevice deviceId, String answer, boolean isError) {
                notifyExecutionListeners(deviceId, answer, isError);
            }

            @Override
            public void onProgressUpdate(HidDevice deviceId, int progress, String message) {
                notifyProgressListeners(deviceId, progress, message);
            }

            @Override
            public void onExecutionFinished(HidDevice deviceId, int progress, byte[] answer, HidCommandName commandName) {
                notifyFinishedListeners(deviceId, progress, answer, commandName);
            }
        };


    }
    
    private DeviceState getOrCreateDeviceState(HidDevice deviceId) {
        DeviceState state = stateRepository.get(deviceId);
        if (state == null) {
            state = new DeviceState();
            stateRepository.put(deviceId, state);
        }
        return state;
    }
    
    private void notifyExecutionListeners(HidDevice deviceId, String answer, boolean isError) {
        for (MgsExecutionListener listener : listeners) {
            listener.onExecutionEvent(deviceId, answer, isError);
        }
    }
    
    private void notifyProgressListeners(HidDevice deviceId, int progress, String message) {
        for (MgsExecutionListener listener : listeners) {
            listener.onProgressUpdate(deviceId, progress, message);
        }
    }

    private void notifyFinishedListeners(HidDevice deviceId, int progress,  byte[] answer, HidCommandName commandName) {
        for (MgsExecutionListener listener : listeners) {
            listener.onExecutionFinished(deviceId, progress, answer, commandName);
        }
    }
    
    public void shutdown() {
        executorService.shutdownNow();
        runningTasks.clear();
    }
}