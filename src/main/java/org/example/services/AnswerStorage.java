package org.example.services;

import org.apache.log4j.Logger;
import org.example.utilites.MyUtilities;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.*;
import java.util.concurrent.*;

public class AnswerStorage {
    private static final Logger log = Logger.getLogger(AnswerStorage.class);

    private static final ThreadLocal<StringBuilder> sbAnswer = ThreadLocal.withInitial(StringBuilder::new);
    private static final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<DeviceAnswer>> answersByTab = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Integer, Integer> queueOffset = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> deviceTabPairs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, String> tabDevicePairs = new ConcurrentHashMap<>();
    private static final int MAX_ANSWERS_PER_TAB = 20_000;

    public static void registerDeviceTabPair(String ident, Integer tabN) {
        tabDevicePairs.put(tabN, ident); // Добавлено
        log.info("Регистрирую связку устройства с вкладкой. Device id: " + ident + " and tab num: " + tabN);
    }

    public static int getAnswersQueueForClient(int clientId){
        ConcurrentLinkedQueue<DeviceAnswer> queue = answersByTab.get(clientId);
        return queue != null ? queue.size() : 0;
    }

    public static List<Integer> getListOfTabsInStorage(){
        return new ArrayList<>(answersByTab.keySet());
    }

    public static HashMap<String, Integer> getDeviceTabPair() {
        return new HashMap<>(deviceTabPairs);
    }

    public static Integer getTabByIdent(String ident) {
        return deviceTabPairs.get(ident);
    }

    public static String getIdentByTab(Integer tab) {
        return tabDevicePairs.getOrDefault(tab, null);
    }

    public static void addAnswer(DeviceAnswer answer) {
        // Блок базовых проверок
        if (answer == null) {
            log.error("Отклонено сохранение ответа [ объект ответа null ]");
            return;
        }

        Integer clientId = answer.getClientId();
        if (clientId == null) {
            log.error("Отклонено сохранение ответа [ ClientId: null ]");
            return;
        }
        if (clientId < 0) {
            log.error("Отклонено сохранение ответа [ ClientId: "+clientId+" < 0 ]");
            return;
        }

        // Проверки временных меток
        if (answer.getRequestSendTime() == null) {
            log.error("Отклонено сохранение ответа ["+clientId+"] - не указано время отправки");
            return;
        }
        if (answer.getAnswerReceivedTime() == null) {
            log.error("Отклонено сохранение ответа ["+clientId+"] - не указано время получения");
            return;
        }

        // Проверки строковых данных
        if (answer.getAnswerReceivedString() == null || answer.getAnswerReceivedString().isEmpty()) {
            log.error("Отклонено сохранение ответа ["+clientId+"] - пустая строка ответа");
            return;
        }

        // Все проверки пройдены
        answersByTab.computeIfAbsent(clientId, k -> new ConcurrentLinkedQueue<>()).add(answer);
        log.info("Ответ ["+clientId+"] успешно сохранен (всего: "+answersByTab.get(clientId).size()+")");

        if(answersByTab.get(clientId) != null) {
            // Очистка старых записей
            if (answersByTab.get(clientId).size() > MAX_ANSWERS_PER_TAB) {
                queueOffset.putIfAbsent(clientId, 0);
                ConcurrentLinkedQueue<DeviceAnswer> queue = answersByTab.get(clientId);

                while (queue.size() > MAX_ANSWERS_PER_TAB) {
                    DeviceAnswer removed = queue.poll();
                    if (removed != null) {
                        queueOffset.put(clientId, (queueOffset.get(clientId) + 1));
                        log.info("Удален устаревший ответ [" + clientId + "]: " + removed.getRequestSendTime() + " сдвиг окна хранилища установлен на " + queueOffset.get(clientId));
                    } else {
                        log.warn("Ошибка очистки ответов [" + clientId + "]: очередь оказалась пустой");
                        break; // Выйти из цикла, если очередь неожиданно пуста
                    }
                }
            }
        }
    }
    public static TabAnswerPart getAnswersQueForTab(Integer lastPosition, Integer clientId, boolean showCommands) {
        ConcurrentLinkedQueue<DeviceAnswer> queue = answersByTab.getOrDefault(clientId, new ConcurrentLinkedQueue<>());
        List<DeviceAnswer> tabAnswers = new ArrayList<>(queue);
        int size = tabAnswers.size();
        int queueOffsetInt = queueOffset.getOrDefault(clientId, 0);

        // Корректируем lastPosition относительно смещения
        if (lastPosition < queueOffsetInt) {
            lastPosition = queueOffsetInt;
        }

        int realPosition = lastPosition - queueOffsetInt;

        // Если реальная позиция выходит за пределы текущего размера
        if (realPosition >= size || realPosition < 0) {
            return new TabAnswerPart("", queueOffsetInt + size);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = realPosition; i < size; i++) {
            appendAnswer(sb, tabAnswers.get(i), showCommands);
        }

        // Возвращаем новую позицию как смещение + текущий размер
        return new TabAnswerPart(sb.toString(), queueOffsetInt + size);
    }


    public static String getAnswersForTab(Integer tabNumber, boolean showCommands) {
        StringBuilder sb = sbAnswer.get();
        sb.setLength(0);

        List<DeviceAnswer> tabAnswers = List.copyOf(answersByTab.getOrDefault(tabNumber, new ConcurrentLinkedQueue<>()));
        if(tabAnswers != null) {
            for (DeviceAnswer answer : tabAnswers) {
                appendAnswer(sb, answer, showCommands);
            }
        }
        return sb.toString();
    }

    private static void appendAnswer(StringBuilder sb, DeviceAnswer answer, boolean showCommands) {
        if (showCommands) {
            sb.append(MyUtilities.CUSTOM_FORMATTER.format(answer.getRequestSendTime()))
                    .append(" :\t")
                    .append(answer.getRequestSendString())
                    .append("\n");
        }
        sb.append(MyUtilities.CUSTOM_FORMATTER.format(answer.getAnswerReceivedTime()));
        if (answer.getAnswerReceivedString() != null) {
            sb.append(":\t").append(answer.getAnswerReceivedString());
        }
        sb.append("\n");
    }

    public static List<DeviceAnswer> getAnswersForGraph(Integer tabNumber) {
        return new ArrayList<>(answersByTab.getOrDefault(tabNumber, null));
    }

    public static List<DeviceAnswer> getRecentAnswersForGraph(Integer tabNumber, int range) {

        List<DeviceAnswer> snapshot = List.copyOf(answersByTab.getOrDefault(tabNumber, new ConcurrentLinkedQueue<>()));

        int size = snapshot.size();
        if (range >= size) {
            return snapshot;
        }

        return snapshot.subList(size - range, size); // Берём последние `range` элементов
    }

    public static void removeAnswersForTab(int tabNum) {
        tabDevicePairs.remove(tabNum); // Если используется обратная мапа
    }

    public static int getQueueOffsetForClient(int clientId) {
        if(queueOffset == null || queueOffset.get(clientId) == null){
            return 0;
        }
        return queueOffset.get(clientId);
    }
}