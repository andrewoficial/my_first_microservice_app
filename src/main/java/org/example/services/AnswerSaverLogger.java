package org.example.services;

import org.apache.log4j.Logger;
import org.example.services.comPool.ComDataCollector;
import org.example.services.loggers.DeviceLogger;
import org.example.services.loggers.PoolLogger;
import org.example.utilites.MyProperties;

import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AnswerSaverLogger {
    private final ConcurrentMap<Integer, DeviceAnswer> lastAnswers = new ConcurrentHashMap<>();
    private final MyProperties properties = new MyProperties();
    private final long MAX_ALLOWED_DIFFERENCE = properties.getSyncSavingAnswerTimerLimitMS();
    private static final Logger log = Logger.getLogger(AnswerSaverLogger.class);
    public synchronized boolean shouldSaveAnswer(DeviceAnswer answer, int threadId) {

        log.info("Начинаю принятие решения...");
        lastAnswers.put(threadId, answer);
        log.info("Обновил значение коллекции lastAnswers для threadId " + threadId);
        long latestTime = answer.getAnswerReceivedTime().toEpochSecond(ZoneOffset.UTC);
        for (DeviceAnswer otherAnswer : lastAnswers.values()) {
            if (Math.abs(latestTime - otherAnswer.getAnswerReceivedTime().toEpochSecond(ZoneOffset.UTC)) > MAX_ALLOWED_DIFFERENCE) {
                log.info(" Будет отклонено, потому что  " + Math.abs(latestTime - otherAnswer.getAnswerReceivedTime().toEpochSecond(ZoneOffset.UTC)) + " но предел отклонения " + MAX_ALLOWED_DIFFERENCE);
                return false;
            }
        }
        return true;
    }

    public synchronized void addAnswer(DeviceAnswer answer, int threadId) {
        if (shouldSaveAnswer(answer, threadId)) {
            AnswerStorage.addAnswer(answer); // Статическое хранилище
        }
    }

    public synchronized void doLog(DeviceAnswer answer, int threadId, PoolLogger poolLogger, DeviceLogger devLogger) {
        log.info("Было принято к рассмотрению логирование ответа от вкладки (клиента)" + threadId);
        if (shouldSaveAnswer(answer, threadId)) {
            log.info("Предложение принято от " + threadId);
            PoolLogger.writeLine(answer);
            if(devLogger != null) {
                devLogger.writeLine(answer);
            }
        }
    }
}
