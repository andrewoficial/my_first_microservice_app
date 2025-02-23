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
    private static final ConcurrentHashMap<String, Integer> deviceTabPairs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, String> tabDevicePairs = new ConcurrentHashMap<>();
    private static final int MAX_ANSWERS_PER_TAB = 10_000;

    public static void registerDeviceTabPair(String ident, Integer tabN) {
        tabDevicePairs.put(tabN, ident); // Добавлено
        log.info("Регистрирую связку устройства с вкладкой. Device id: " + ident + " and tab num: " + tabN);
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
        if (answer.getAnswerReceivedTime().isBefore(answer.getRequestSendTime())) {
            log.error("Отклонено сохранение ответа ["+clientId+"] - ответ получен до отправки запроса ({} < {})");
            return;
        }

        // Проверки строковых данных
        if (answer.getAnswerReceivedString() == null || answer.getAnswerReceivedString().trim().isEmpty()) {
            log.error("Отклонено сохранение ответа ["+clientId+"] - пустая строка ответа");
            return;
        }



        // Все проверки пройдены
        answersByTab.computeIfAbsent(clientId, k -> new ConcurrentLinkedQueue<>()).add(answer);
        log.info("Ответ ["+clientId+"] успешно сохранен (всего: "+answersByTab.get(clientId).size()+")");

        // Очистка старых записей
        if (answersByTab.get(clientId).size() > MAX_ANSWERS_PER_TAB) {
            DeviceAnswer removed = answersByTab.get(clientId).poll();
            log.debug("Удален устаревший ответ ["+ clientId +"]: "+ removed.getRequestSendTime() );
        }
    }
    public static TabAnswerPart getAnswersQueForTab(Integer lastPosition, Integer clientId, boolean showCommands) {
        StringBuilder sb = new StringBuilder();

        List<DeviceAnswer> tabAnswers = List.copyOf(answersByTab.getOrDefault(clientId, new ConcurrentLinkedQueue<>()));
        int size = tabAnswers.size();

        if (lastPosition >= size) {
            return new TabAnswerPart("", size);
        }

        for (int i = lastPosition; i < size; i++) {
            appendAnswer(sb, tabAnswers.get(i), showCommands);
        }

        return new TabAnswerPart(sb.toString(), size);
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
}