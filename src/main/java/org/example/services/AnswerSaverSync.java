package org.example.services;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class AnswerSaverSync {
    private final ConcurrentMap<Integer, DeviceAnswer> lastAnswers = new ConcurrentHashMap<>();
    private final MyProperties properties;
    private final AnswerStorage answerStorage;
    //private final long MAX_ALLOWED_DIFFERENCE = properties.getSyncSavingAnswerTimerLimitMS(); // Максимальный допустимый разброс
    private long MAX_ALLOWED_DIFFERENCE = 1200; // Максимальный допустимый разброс
    private long SYNC_WINDOW = 950000; // Длительность окна синхронизации


    private long currentSyncStart = System.currentTimeMillis(); // Начало текущего окна
    private final Set<Integer> ignoredThreads = ConcurrentHashMap.newKeySet(); // Потоки, которые временно игнорируются

    @Autowired
    public AnswerSaverSync(MyProperties properties, AnswerStorage answerStorage) {
        this.properties = properties;
        this.answerStorage = answerStorage;
    }
    public synchronized boolean shouldSaveAnswer(DeviceAnswer answer, int threadId) {
        //log.info("Начинаю принятие решения для threadId: " + threadId);
        SYNC_WINDOW = properties.getSyncSavingAnswerWindowMS();
        MAX_ALLOWED_DIFFERENCE = properties.getSyncSavingAnswerTimerLimitMS();
        long now = System.currentTimeMillis();

        // Проверка начала нового окна синхронизации
        if (now - currentSyncStart > SYNC_WINDOW) {
            log.info("Новое окно синхронизации начато. Сброс старых данных.");
            currentSyncStart = now;
            lastAnswers.clear();
            ignoredThreads.clear();
        }

        /*
        // Проверка, игнорируется ли поток
        if (ignoredThreads.contains(threadId)) {
            log.info("Поток " + threadId + " в списке игнорируемых. Ответ не будет сохранен.");
            return false;
        }
         */

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
            answerStorage.addAnswer(answer); // Хранилище ответов
            //log.info("Ответ сохранен для threadId: " + threadId);
        } else {
            //log.info("Ответ от threadId " + threadId + " отклонен.");
        }
    }

    // FIXME: doLog блокирует все опрашивающие потоки из-за synchronized + файловый I/O внутри монитора.
    //  ПЛАН РЕФАКТОРИНГА:
    //  1. Создать отдельный поток-слушатель (BlockingQueue<DeviceAnswer>), накапливающий данные для записи.
    //  2. Синхронизация ТОЛЬКО по передаче сообщения в логер (queue.put()).
    //  3. Логер работает в отдельном потоке, читает из очереди и пишет в файл.
    //  4. Тротлинг на 100ms — не писать в файл чаще раза в 100ms, чтобы файловую систему не убивать.
    public synchronized void doLog(DeviceAnswer answer, int threadId, PoolLogger poolLogger, DeviceLogger devLogger) {
        //log.info("Было принято к рассмотрению логирование ответа от threadId: " + threadId);
        if (shouldSaveAnswer(answer, threadId)) {
            log.info("Ответ принят для логирования от threadId: " + threadId);
            PoolLogger.getInstance().writeLine(answer);
            if (devLogger != null) {
                devLogger.writeLine(answer);
            }
        } else {
            //log.info("Ответ от threadId " + threadId + " отклонен для логирования.");
        }
    }
}
