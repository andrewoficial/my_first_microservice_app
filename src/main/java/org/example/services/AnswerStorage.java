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
        if (answer == null || answer.getTabNumber() == null || answer.getTabNumber() < 0) {
            log.info("Отклонено сохранение ответа [" + answer + "]");
            return;
        }

        answersByTab.computeIfAbsent(answer.getTabNumber(), k -> new ConcurrentLinkedQueue<>())
                .add(answer);

        log.debug("Новое значение ответа с идентификатором " + answer.getTabNumber());
        // Удаляем старые ответы, если превышен лимит
        if (answersByTab.get(answer.getTabNumber()).size() > MAX_ANSWERS_PER_TAB) {
            answersByTab.get(answer.getTabNumber()).poll();
        }
    }

    public static TabAnswerPart getAnswersQueForTab(Integer lastPosition, Integer tabNumber, boolean showCommands) {
        StringBuilder sb = new StringBuilder();

        List<DeviceAnswer> tabAnswers = List.copyOf(answersByTab.getOrDefault(tabNumber, new ConcurrentLinkedQueue<>()));
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