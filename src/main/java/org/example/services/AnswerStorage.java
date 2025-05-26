package org.example.services;

import org.apache.log4j.Logger;
import org.example.utilites.MyUtilities;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class AnswerStorage {
    public static final ConcurrentHashMap<Integer, Integer> queueOffset = new ConcurrentHashMap<>();

    private static final Logger log = Logger.getLogger(AnswerStorage.class);

    private static final ThreadLocal<StringBuilder> sbAnswer = ThreadLocal.withInitial(StringBuilder::new);
    private static final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<DeviceAnswer>> answersByTab = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> deviceTabPairs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, String> tabDevicePairs = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<Integer> clientsList = new ConcurrentLinkedQueue<>();
    private static final int MAX_ANSWERS_PER_TAB = 20_000;

    public static void registerDeviceTabPair(String ident, Integer clientId) {
        tabDevicePairs.put(clientId, ident); // Добавлено

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

    public static ConcurrentHashMap<Integer, String> getDeviceTabPair() {
        log.info("Вызвано получение коллекции ассоциаций. Размер коллекции " + tabDevicePairs.size());
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

        // Добавление ответа
        ConcurrentLinkedQueue<DeviceAnswer> queue = answersByTab.computeIfAbsent(clientId, k -> new ConcurrentLinkedQueue<>());
        queue.add(answer);



        if( answer.getFieldCount() > 0 &&  answer.getAnswerReceivedValues() != null && answer.getAnswerReceivedValues().getValues() != null && answer.getAnswerReceivedValues().getValues().length >= 1 &&
                answer.getAnswerReceivedValues().getUnits() != null && answer.getAnswerReceivedValues().getUnits().length >= 1){
            log.info("Сообщение от клиента ["+clientId+"] принято к сохранению (всего: "+answersByTab.get(clientId).size()+")" + answer.getAnswerReceivedValues().getValues()[0] + " " +answer.getAnswerReceivedValues().getUnits()[0]);
        }else{
            log.info("Сообщение от клиента ["+clientId+"] принято к сохранению (всего: "+answersByTab.get(clientId).size()+")" + " (в ответе только строка)");
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
                        break; // Выйти из цикла, если очередь неожиданно пуста
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

        log.info("getAnswersQueForTab: clientId=" + clientId + ", lastPositionBeforeCorrection = "+lastPositionBeforeCorrection+", lastPosition=" + lastPosition + ", queueOffsetInt=" + queueOffsetInt);
        // Возвращаем новую позицию как смещение + текущий размер
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

        // Возвращаем новую позицию как смещение + текущий размер
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

        // Блок запроса
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

        // Блок ответа
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