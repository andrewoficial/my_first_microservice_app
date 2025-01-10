package org.example.services;

import org.apache.log4j.Logger;
import org.example.services.loggers.DeviceLogger;
import org.example.services.loggers.PoolLogger;
import org.example.utilites.properties.MyProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
@Service
public class AnswerSaverLogger {
    private final ConcurrentMap<Integer, DeviceAnswer> lastAnswers = new ConcurrentHashMap<>();
    private final MyProperties properties;
    //private final long MAX_ALLOWED_DIFFERENCE = properties.getSyncSavingAnswerTimerLimitMS(); // Максимальный допустимый разброс
    private final long MAX_ALLOWED_DIFFERENCE = 1200; // Максимальный допустимый разброс
    private final long SYNC_WINDOW = 950000; // Длительность окна синхронизации
    private static final Logger log = Logger.getLogger(AnswerSaverLogger.class);

    private long currentSyncStart = System.currentTimeMillis(); // Начало текущего окна
    private final Set<Integer> ignoredThreads = ConcurrentHashMap.newKeySet(); // Потоки, которые временно игнорируются

    @Autowired
    public AnswerSaverLogger(MyProperties properties) {
        this.properties = properties;
    }
    public synchronized boolean shouldSaveAnswer(DeviceAnswer answer, int threadId) {
        //log.info("Начинаю принятие решения для threadId: " + threadId);

        long now = System.currentTimeMillis();

        // Проверка начала нового окна синхронизации
        if (now - currentSyncStart > SYNC_WINDOW) {
            log.info("Новое окно синхронизации начато. Сброс старых данных.");
            currentSyncStart = now;
            lastAnswers.clear();
            ignoredThreads.clear();
        }

        // Проверка, игнорируется ли поток
        if (ignoredThreads.contains(threadId)) {
            log.info("Поток " + threadId + " в списке игнорируемых. Ответ не будет сохранен.");
            return false;
        }

        // Добавление ответа в коллекцию
        lastAnswers.put(threadId, answer);
        //log.info("Обновил значение коллекции lastAnswers для threadId " + threadId);

        long latestTime = answer.getAnswerReceivedTime().toEpochSecond(ZoneOffset.UTC);
        for (Map.Entry<Integer, DeviceAnswer> entry : lastAnswers.entrySet()) {
            if (!entry.getKey().equals(threadId)) {
                long timeDiff = Math.abs(latestTime - entry.getValue().getAnswerReceivedTime().toEpochSecond(ZoneOffset.UTC)) * 1000; // мс
                if (timeDiff > MAX_ALLOWED_DIFFERENCE) {
                    log.info("Ответ отклонен. Разница времени: " + timeDiff + " мс, предел: " + MAX_ALLOWED_DIFFERENCE + " мс.");
                    ignoredThreads.add(threadId);
                    return false;
                }
            }
        }
        return true;
    }

    public synchronized void addAnswer(DeviceAnswer answer, int threadId) {
        if (shouldSaveAnswer(answer, threadId)) {
            AnswerStorage.addAnswer(answer); // Статическое хранилище
            //log.info("Ответ сохранен для threadId: " + threadId);
        } else {
            //log.info("Ответ от threadId " + threadId + " отклонен.");
        }
    }

    public synchronized void doLog(DeviceAnswer answer, int threadId, PoolLogger poolLogger, DeviceLogger devLogger) {
        //log.info("Было принято к рассмотрению логирование ответа от threadId: " + threadId);
        if (shouldSaveAnswer(answer, threadId)) {
            //log.info("Ответ принят для логирования от threadId: " + threadId);
            PoolLogger.writeLine(answer);
            if (devLogger != null) {
                devLogger.writeLine(answer);
            }
        } else {
            //log.info("Ответ от threadId " + threadId + " отклонен для логирования.");
        }
    }
}
