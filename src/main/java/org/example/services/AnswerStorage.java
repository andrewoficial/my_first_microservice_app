package org.example.services;

import org.apache.log4j.Logger;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class AnswerStorage {
    private static final Logger log = Logger.getLogger(AnswerStorage.class);
    static StringBuilder sbAnswer = new StringBuilder();
    public static DateTimeFormatter dtfAnswer = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static HashMap<Integer, ArrayList<DeviceAnswer>> answersByTab = new HashMap<>();
    public static HashMap<String, Integer> deviceTabPairs = new HashMap<>();

    public static void registerDeviceTabPair(String ident, Integer tabN) {
        AnswerStorage.deviceTabPairs.put(ident, tabN);
        System.out.println("IDN:" + ident + " bounded with tab num: " + tabN);
        System.out.println("Now contain:" + AnswerStorage.deviceTabPairs.size());
    }



    public static Integer getTabByIdent(String ident) {
        return AnswerStorage.deviceTabPairs.get(ident);
    }

    public static String getIdentByTab(Integer tab) {
        for (Map.Entry<String, Integer> entry : deviceTabPairs.entrySet()) {
            if (Objects.equals(entry.getValue(), tab)) {
                return entry.getKey();
            }
        }
        System.out.println("Попытка получить несуществующую связку Вкладка/Прибор по признаку вкладки");
        return null;
    }

    public static void addAnswer(DeviceAnswer answer) {
        answersByTab.putIfAbsent(answer.getTabNumber(), new ArrayList<>());
        ArrayList<DeviceAnswer> tabAnswers = answersByTab.get(answer.getTabNumber());
        if (tabAnswers.size() > 10000) {
            // Push to cache
            tabAnswers.clear();
        }
        tabAnswers.add(answer);
        log.info("Новое значение ответа со вкладки " + answer.getTabNumber() + " протокол " + answer.getDeviceType().getClass().getSimpleName() + " строка  :" + answer.getAnswerReceivedString());
    }

    public static TabAnswerPart getAnswersQueForTab(Integer lastPosition, Integer tabNumber, boolean showCommands) {
        sbAnswer.setLength(0); // Clear the StringBuilder
        ArrayList<DeviceAnswer> tabAnswers = answersByTab.getOrDefault(tabNumber, new ArrayList<>());
        if (lastPosition >= tabAnswers.size()) {
            //return null;
            return new TabAnswerPart("", tabAnswers.size());
        }
        for (int i = lastPosition; i < tabAnswers.size(); i++) {
            DeviceAnswer answer = tabAnswers.get(i);
            appendAnswer(answer, showCommands);
        }
        return new TabAnswerPart(sbAnswer.toString(), tabAnswers.size());
    }

    public static String getAnswersForTab(Integer tabNumber, boolean showCommands) {
        sbAnswer.setLength(0); // Clear the StringBuilder
        ArrayList<DeviceAnswer> tabAnswers = answersByTab.getOrDefault(tabNumber, new ArrayList<>());
        for (DeviceAnswer answer : tabAnswers) {
            appendAnswer(answer, showCommands);
        }
        return sbAnswer.toString();
    }

    private static void appendAnswer(DeviceAnswer answer, boolean showCommands) {
        if (showCommands) {
            sbAnswer.append(dtfAnswer.format(answer.getRequestSendTime()));
            sbAnswer.append(":\t");
            sbAnswer.append(answer.getRequestSendString());
            sbAnswer.append("\n");
        }
        sbAnswer.append(dtfAnswer.format(answer.getAnswerReceivedTime()));
        if (answer.getAnswerReceivedString() != null) {
            sbAnswer.append(":\t");
            sbAnswer.append(answer.getAnswerReceivedString());
        }
        sbAnswer.append("\n");
    }

    // New methods for getting data for graphs
    public static List<DeviceAnswer> getAnswersForGraph(Integer tabNumber) {
        return new ArrayList<>(answersByTab.getOrDefault(tabNumber, new ArrayList<>()));
    }

    public static List<DeviceAnswer> getRecentAnswersForGraph(Integer tabNumber, int range) {
        ArrayList<DeviceAnswer> tabAnswers = answersByTab.getOrDefault(tabNumber, new ArrayList<>());
        int size = tabAnswers.size();
        if (range >= size) {
            return new ArrayList<>(tabAnswers);
        }
        return new ArrayList<>(tabAnswers.subList(size - range, size));
    }
}