package org.example.services;

import lombok.extern.slf4j.Slf4j;
import org.example.utilites.MyUtilities;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class AnswerStorage {
    public static final ConcurrentHashMap<Integer, Integer> queueOffset = new ConcurrentHashMap<>();

    private static final ThreadLocal<StringBuilder> sbAnswer = ThreadLocal.withInitial(StringBuilder::new);
    private static final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<DeviceAnswer>> answersByTab = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> deviceTabPairs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, String> tabDevicePairs = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<Integer> clientsList = new ConcurrentLinkedQueue<>();
    private static final int MAX_ANSWERS_PER_TAB = 20_000;

    // Command stability tracking
    private static final int STABLE_THRESHOLD = 3;
    private static final ConcurrentHashMap<Integer, CommandBuffer> commandBuffers = new ConcurrentHashMap<>();

    private static class CommandBuffer {
        private final ConcurrentLinkedDeque<String> buffer = new ConcurrentLinkedDeque<>();
        private volatile String stableCommand = null;

        synchronized void recordCommand(String command) {
            buffer.addLast(command);
            while (buffer.size() > STABLE_THRESHOLD) {
                buffer.removeFirst();
            }
            updateStability();
        }

        private void updateStability() {
            if (buffer.isEmpty()) {
                stableCommand = null;
                return;
            }
            if (buffer.size() == 1) {
                String prev = stableCommand;
                stableCommand = buffer.peekFirst();
                if (prev == null) {
                    log.info("Command STABILIZED (first seen): '{}' for buffer: {}", stableCommand, buffer);
                }
                return;
            }
            String first = buffer.peekFirst();
            boolean allSame = true;
            for (String cmd : buffer) {
                if (!cmd.equals(first)) {
                    allSame = false;
                    break;
                }
            }
            String prev = stableCommand;
            stableCommand = allSame ? first : null;
            if (prev == null && stableCommand != null) {
                log.info("Command STABILIZED: '{}' for buffer: {}", stableCommand, buffer);
            } else if (prev != null && stableCommand == null) {
                log.info("Command UNSTABLE (was '{}'), buffer: {}", prev, buffer);
            } else if (prev != null && !prev.equals(stableCommand)) {
                log.info("Command SWITCHED: '{}' → '{}', buffer: {}", prev, stableCommand, buffer);
            }
        }

        String getStableCommand() {
            return stableCommand;
        }
    }

    public static void registerDeviceTabPair(String ident, Integer clientId) {
        tabDevicePairs.put(clientId, ident);

        log.info("Регистрирую связку устройства с вкладкой. Device id: " + ident + " and clientId: " + clientId);
        log.info("После регистрации размер хранилища: " + tabDevicePairs.size());
    }

    public static ConcurrentLinkedQueue<Integer> getClientsList(){
        return clientsList;
    }

    public static void addInTabsList(Integer clientId){
        if(clientId == null){
            log.warn("Попытка добавить клиента с null id ");
            return;
        }
        clientsList.add(clientId);
    }

    public static void removeInTabsList(Integer clientId){
        if(clientId == null){
            log.warn("Попытка удалить клиента с null id ");
            return;
        }
        if( ! clientsList.contains(clientId)){
            log.warn("Попытка удалить клиента котрого нет ");
            return;
        }
        clientsList.remove(clientId);
    }

    public static boolean isInTabsList(Integer clientId) {
        if(clientId == null){
            log.warn("Попытка удалить клиента с null id ");
            return false;
        }
        return clientsList.contains(clientId);
    }
    public static int getAnswersQueueForClient(int clientId){
        ConcurrentLinkedQueue<DeviceAnswer> queue = answersByTab.get(clientId);
        return queue != null ? queue.size() : 0;
    }

    public static List<Integer> getListOfTabsInStorage(){
        return new ArrayList<>(answersByTab.keySet());
    }

    public static Set<Integer> getTabsInStorage(){
        return answersByTab.keySet();
    }

    public static ConcurrentHashMap<Integer, String> getDeviceTabPair() {
        return tabDevicePairs;
    }

    public static Integer getTabByIdent(String ident) {
        if(deviceTabPairs.containsKey(ident)){
            return deviceTabPairs.get(ident);
        }else{
            return 0;
        }

    }

    public static String getIdentByTab(Integer tab) {
        return tabDevicePairs.getOrDefault(tab, null);
    }

    public static boolean isCommandStable(Integer tabId) {
        CommandBuffer cb = commandBuffers.get(tabId);
        return cb != null && cb.getStableCommand() != null;
    }

    public static String getStableCommand(Integer tabId) {
        CommandBuffer cb = commandBuffers.get(tabId);
        return cb != null ? cb.getStableCommand() : null;
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
            log.error(String.format("Отклонено сохранение для клиента [%d] - пустая строка ответа ", clientId));
            return;
        }

        // Record command and check stability
        String command = answer.getRequestSendString();
        if (command != null) {
            command = command.trim();
        }
        if (command != null && !command.isEmpty()) {
            commandBuffers.computeIfAbsent(clientId, k -> new CommandBuffer())
                    .recordCommand(command);
        }

        // Добавление ответа
        ConcurrentLinkedQueue<DeviceAnswer> queue = answersByTab.computeIfAbsent(clientId, k -> new ConcurrentLinkedQueue<>());
        queue.add(answer);

        // Store in GraphDataRepository — always store, regardless of stability
        if (command != null && !command.isEmpty()) {
            GraphDataRepository.getInstance().addData(clientId, command, answer);
        }


        if( answer.getFieldCount() > 0 &&  answer.getAnswerReceivedValues() != null && answer.getAnswerReceivedValues().getValues() != null && answer.getAnswerReceivedValues().getValues().length >= 1 &&
                answer.getAnswerReceivedValues().getUnits() != null && answer.getAnswerReceivedValues().getUnits().length >= 1){
        }else{
        }


        if(answersByTab != null && answersByTab.containsKey(clientId) && answersByTab.get(clientId) != null) {
            // Очистка старых записей
            if (answersByTab.get(clientId).size() > MAX_ANSWERS_PER_TAB) {
                log.info("Для клиента" + clientId + " найдено записей больше допустимого (" + MAX_ANSWERS_PER_TAB + ") инициирована очистка");
                queueOffset.putIfAbsent(clientId, 0);
                queue = answersByTab.get(clientId);

                while (queue.size() > MAX_ANSWERS_PER_TAB) {
                    DeviceAnswer removed = queue.poll();
                    if (removed != null) {
                        queueOffset.put(clientId, (queueOffset.get(clientId) + 1));
                        log.info("Удален устаревший ответ [" + clientId + "]: " + removed.getRequestSendTime() + " сдвиг окна хранилища установлен на " + queueOffset.get(clientId));
                    } else {
                        log.warn("Ошибка очистки ответов [" + clientId + "]: очередь оказалась пустой");
                        break;
                    }
                }
            }
        }


    }
    public static TabAnswerPart getAnswersQueForTab(Integer lastPosition, Integer clientId, boolean showCommands) {
        ConcurrentLinkedQueue<DeviceAnswer> queue = answersByTab.getOrDefault(clientId, new ConcurrentLinkedQueue<>());
        int lastPositionBeforeCorrection = lastPosition;
        List<DeviceAnswer> tabAnswers = new ArrayList<>(queue);
        int size = tabAnswers.size();
        int queueOffsetInt = queueOffset.getOrDefault(clientId, 0);

        if (lastPosition < queueOffsetInt) {
            lastPosition = queueOffsetInt;
        }

        int realPosition = lastPosition - queueOffsetInt;

        if (realPosition >= size || realPosition < 0) {
            return new TabAnswerPart("", queueOffsetInt + size);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = realPosition; i < size; i++) {
            appendAnswer(sb, tabAnswers.get(i), showCommands);
        }

        return new TabAnswerPart(sb.toString(), queueOffsetInt + size);
    }

    public static TabAnswerPart getAnswersQueForWeb(Integer lastPosition, Integer clientId, boolean showCommands) {
        ConcurrentLinkedQueue<DeviceAnswer> queue = answersByTab.getOrDefault(clientId, new ConcurrentLinkedQueue<>());
        List<DeviceAnswer> tabAnswers = new ArrayList<>(queue);
        int size = tabAnswers.size();

        StringBuilder sb = new StringBuilder();
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
        if (sb == null) {
            log.error("Передан null StringBuilder в appendAnswer");
            sb = new StringBuilder();
        }

        if (answer == null) {
            log.error("Передан NULL answer в склейку ответов для вывода в GUI");
            return;
        }

        if (showCommands) {
            LocalDateTime requestTime = answer.getRequestSendTime();
            String requestString = answer.getRequestSendString();
            if (requestTime == null) {
                requestTime = LocalDateTime.now();
                log.warn("В объекте answer null параметр requestSendTime, использовано текущее время");
            }
            if (requestString == null) {
                requestString = "NaN command";
                log.warn("В объекте answer null параметр requestSendString, использовано значение по умолчанию");
            }
            sb.append(MyUtilities.CUSTOM_FORMATTER.format(requestTime))
                    .append(" :\t")
                    .append(requestString)
                    .append("\n");
        }

        LocalDateTime answerTime = answer.getAnswerReceivedTime();
        String answerString = answer.getAnswerReceivedString();
        if (answerTime == null) {
            answerTime = LocalDateTime.now();
            log.warn("В объекте answer null параметр answerReceivedTime, использовано текущее время");
        }
        if (answerString == null || answerString.isEmpty()) {
            answerString = "NaN answer";
            log.warn("В объекте answer null или пустой параметр answerReceivedString, использовано значение по умолчанию");
        }
        sb.append(MyUtilities.CUSTOM_FORMATTER.format(answerTime))
                .append(":\t")
                .append(answerString)
                .append("\n");
    }

    public static List<DeviceAnswer> getAnswersForGraph(Integer tabNumber) {
        ConcurrentLinkedQueue<DeviceAnswer> answers = answersByTab.getOrDefault(tabNumber, null);
        if(answers != null) {
            return List.copyOf(answers);
        }else{
            log.error("Для клиента " + tabNumber + " возвращен пустой массив ответов");
            return new ArrayList<>();
        }
    }

    public static List<DeviceAnswer> getRecentAnswersForGraph(Integer tabNumber, int range) {

        List<DeviceAnswer> snapshot = List.copyOf(answersByTab.getOrDefault(tabNumber, new ConcurrentLinkedQueue<>()));

        int size = snapshot.size();
        if (range >= size) {
            return snapshot;
        }

        return snapshot.subList(size - range, size);
    }

    public static void removeAnswersForTab(int tabNum) {
        tabDevicePairs.remove(tabNum);
    }

    public static int getQueueOffsetForClient(int clientId) {
        if(queueOffset == null || queueOffset.get(clientId) == null){
            return 0;
        }
        return queueOffset.get(clientId);
    }
}
