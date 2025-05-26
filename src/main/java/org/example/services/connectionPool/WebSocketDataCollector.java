package org.example.services.connectionPool;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

import org.apache.log4j.Logger;
import org.example.device.ProtocolsList;
import org.example.device.SomeDevice;
import org.example.device.protVega.VEGA_WAN;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.AnswerStorage;
import org.example.services.DeviceAnswer;
import org.example.services.loggers.DeviceLogger;
import org.example.services.loggers.PoolLogger;
import org.example.utilites.properties.MyProperties;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;


import javax.swing.*;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.example.utilites.MyUtilities.createDeviceByProtocol;

public class WebSocketDataCollector implements Runnable{
    private final static Logger log = Logger.getLogger(WebSocketDataCollector.class); // Объект логера
    private final MyProperties myProperties = MyProperties.getInstance(); //Объект с параметрами для того, что бы определять тип логирования
    @Getter
    private volatile boolean alive = true; // Признак того, что поток жив (после введения Event всегда истина и сбрасывается в shutdown, раньше сбрасывался если нету клиентов у потока)
    @Getter
    private final AtomicInteger currentDirection = new AtomicInteger();
    @Getter
    public final AtomicBoolean webSocketDataCollectorBusy = new AtomicBoolean(false);// Признак того, что текущее соединение по ком-порту занято
    private SomeDevice device = null;//Объект устройства, содержащий особенности обработки команд. Задается на основе выбранного протоколаё

    @Getter
    private String token; //ToDo переделать в коллекцию токенов

    private String url;
    @Getter
    private WebSocketClient client;//Объект, обслуживающий соединение. Библиотека springframework. web. socket. client.
    @Getter
    private WebSocketSession session;//Объект, обслуживающий соединение с авторизацией. Библиотека springframework. web. socket. client.
//    @Setter
//    private ArrayList<WebSocketListener> listeners = new ArrayList<>();//Объект EventListener, обрабатывающий входящие без запроса сообщения. Библиотека fazecast.
    @Getter
    private long poolDelay = 2000;//Задает переодичность отправки запросов в мс.
    private long millisPrev = System.currentTimeMillis() - (poolDelay * 100);//Обслуживание таймера в миллисекундах (основная переменная poolDelay).
    private final ConcurrentHashMap<Integer, WebSocketDataCollector.ClientData> clientsMap = new ConcurrentHashMap<>();
    private volatile DeviceAnswer deviceAnswer;


    private volatile boolean responseRequested = false;
    private volatile long requestTimestamp = 0;
    private final long RESPONSE_TIMEOUT_MS = 3000;  // Таймаут ожидания ответа
    private int clientId;
    private DataUpdateListener listener;


    public WebSocketDataCollector(VEGA_WAN protocol, String url, int poolDelay, boolean needLog, Integer clientId, DataUpdateListener handleDataUpdate) throws ConnectException  {
        super();
        this.listener = handleDataUpdate;
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode message = objectMapper.createObjectNode();
        message.put("clientId", "sys");
        message.put("action", "creating_run");
        message.put("data", "start creating obj poo srv");

        handleIncomingData(message);



        if(protocol == null || clientId  == null ){
            log.error("Один из параметров конструктора передан как null");
            return;
        }
        device = protocol;
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        client = new StandardWebSocketClient();

        String defUrl = "ws://127.0.0.1:8002";
        if (url == null || url.isEmpty()) {
            this.url = defUrl;
        }else{
            this.url = url;
        }
        this.clientId = clientId;
        //log.warn("getBaudRateValue " + state.getBaudRateValue(clientId) + "getDataBitsValue " +  state.getDataBitsValue(clientId)+ "getStopBitsValue " + state.getStopBitsValue(clientId)+ "getParityBitsValue " + state.getParityBitsValue(clientId));

        try {
            openSession();
            if(session.isOpen()){
                log.info("Подключение установлено");
            }else{
                log.info("Подключение установить не удалось");
            }

        } catch (Exception e) {
            log.info("Ошибка при подключении к сокету" + e.getMessage());
            //throw new ConnectException("Адрес: " + url + ". Код ошибки: " + e.getMessage());
        }

        this.clientId = clientId;
        this.poolDelay = poolDelay;
        clientsMap.put(clientId, new WebSocketDataCollector.ClientData(clientId, needLog, true, "", "", null));

    }
    private void messageToGui(String a, String b, String c, JsonNode rootNode){

        handleIncomingData(rootNode);

    }

    private void openSession() throws ExecutionException, InterruptedException {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        class MyTextWebSocketHandler extends TextWebSocketHandler {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                handleDataAvailableEvent(message);
            }
        }
        log.info("Начинаю подключение" + url);
        session = client.doHandshake(new MyTextWebSocketHandler(), headers, URI.create(url)).get();

    }
    private void handleDataAvailableEvent(TextMessage message) throws Exception {
        // Получаем ответ от сервера
        String payload = message.getPayload();
        log.info("Получен ответ от сервера: " + payload);
        ObjectMapper objectMapper = new ObjectMapper(); // Создаем или используем существующий ObjectMapper

        try {
             // 2. Проверка на пустоту
            if (payload == null || payload.trim().isEmpty()) {
                log.warn("Пустое сообщение");
                return;
            }

            // 3. Парсим JSON
            JsonNode rootNode = objectMapper.readTree(payload);

            // 4. Проверяем, что это JSON-объект
            if (!rootNode.isObject()) {
                log.warn("Сообщение не является JSON-объектом");
                return;
            }


            // 6. Передаем данные в обработчик
            messageToGui(null, null, null, rootNode);

        } catch (IOException e) {
            log.error("Ошибка парсинга JSON: " + e.getMessage());
        } catch (Exception e) {
            log.error("Неожиданная ошибка: " + e.getMessage(), e);
        }



        if (responseRequested && (System.currentTimeMillis() - requestTimestamp) < RESPONSE_TIMEOUT_MS) {
            log.info("Получен ожидаемый ответ");
            saveReceivedByEvent(payload, true);
            responseRequested = false;
            log.info("Завершена обработка ожидаемых данных (ответа)");
        } else {
            log.info("Получены неожиданные данные: " + payload);
            saveReceivedByEvent(payload, false);
            log.info("Завершена обработка неожиданных данных. responseRequested " + responseRequested + " requestTimestamp " + requestTimestamp);
        }

    }

    public int getClientsCount() {
        return clientsMap.size();
    }



    @Override
    public void run() {
        alive = true;
        Thread.currentThread().setName("Thread Pool Tab " + url);
        log.info("Запущен поток опроса " + Thread.currentThread().getName() + " для вкладки веб-сокета " + url);
        millisPrev = System.currentTimeMillis() - (poolDelay * 2); // Инициализация таймера
        while ((!Thread.currentThread().isInterrupted()) && alive) {
            if ( ! clientsMap.isEmpty() && shouldPollBecauseTimer()) {
                millisPrev = System.currentTimeMillis();
                pollCommands();
            } else {
                sleepSafely(Math.min(poolDelay / 5, 100L));
            }
        }
    }

    private boolean shouldPollBecauseTimer() {
        return System.currentTimeMillis() - millisPrev > poolDelay;
    }


    private void pollCommands() {
        millisPrev = System.currentTimeMillis();
        for (WebSocketDataCollector.ClientData client : clientsMap.values()) {
            if(client.needPool){
                //log.info("Во внутренней очереди устройств отправляю для устройства clientId " + client.clientId + " командой " + client.command);
                //sendOnce(client.command, client.clientId, true);
            }
        }
    }

    private void waitForWebSocketDataCollectorBusy(int totalLimit) {
        if(totalLimit < 20) totalLimit = 20;
        int i = totalLimit / 20;

        while (webSocketDataCollectorBusy.get() && i < totalLimit) {
            sleepSafely(20);
            i++;
        }
    }
    private void sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.warn("Произошла ошибка при попытке сна" + e.getMessage());
        }
    }
    public void sendOnce(String cmd,int clientId, boolean internal) {
        log.info(" Начал отправку " + Thread.currentThread().getName() + " для клиента " + clientId);

        if( ! internal){
            if(! containClientId(clientId)){
                log.error(" Попытка отправки с внешнего вызова sendOnce для клиента, которого нет в потоке");
                return;
            }
        }
        currentDirection.set(clientId);

        if(internal){
            waitForWebSocketDataCollectorBusy(40);
        }else {
            waitForWebSocketDataCollectorBusy(60);
        }


        if (webSocketDataCollectorBusy.get()) {
            log.info("Прервал отправку. Слишком долгое ожидание освобождения соединения") ;
            return;
        }

        if (cmd == null || cmd.isEmpty()) {
            log.info("Прервал отправку. Нет текста команды");
            webSocketDataCollectorBusy.set(false);
            return;
        }

        if( ! session.isOpen()){
            log.warn("Сессия закрыта!");
            boolean isReopened = false;
            try{
                session.close();
                openSession();

                isReopened = session.isOpen();
            }catch (Exception e){
                log.warn("Исключение при попытке восстановить сессию" + e.getMessage());
            }
            if( ! isReopened) {
                webSocketDataCollectorBusy.set(false);
                return;
            }
        }


        responseRequested = true;
        requestTimestamp = System.currentTimeMillis();


        device.setCmdToSend(cmd);
        try {

            session.sendMessage(new TextMessage(cmd));
        } catch ( IOException e) {
            log.warn("Ошибка при отправке команды " + e.getMessage() + " url был " + url);
        }




        deviceAnswer = new DeviceAnswer(LocalDateTime.now(),cmd,clientId);
        log.info("Заготовка ответа создана ");
    }

    public void saveReceivedByEvent(String msg, boolean responseRequested) {
        if(msg == null || msg.isEmpty()){
            log.warn("Пустое сообщение при попытке saveReceivedByEvent");
            return;
        }


        saveAndLogSome(deviceAnswer, 99);//ToDO самый простой путь скрыть
    }

    public void setNeedPool (int clientId, boolean needStatePool){
        if(containClientId(clientId)) {
            clientsMap.get(clientId).needPool = needStatePool;
            log.info("Значение флага пороса для клиента " + clientId + " изменено на " + needStatePool);
        }else{
            log.warn("Значение флага пороса для клиента " + clientId + " невозможно изменить клиента нет в этом потоке ");
        }
    }

    public void shutdown(){
        log.info("Поток опроса " + Thread.currentThread().getName() + " будет закрыт");
        clientsMap.clear();
        alive = false;
        if(session != null){
            if(session.isOpen()){
                try {
                    session.close();
                } catch (IOException e) {
                    log.warn("Исключение при закрытии сессии при закрытии потока опроса " + e.getMessage() + " url был " + url);
                }
            }
        }
    }


    private void saveAndLogSome(DeviceAnswer answer, int clientId){
        if(answer == null){
            log.warn("Попытка сохранить null ответ");
            return;
        }

        if(answer.getClientId() < 0){
            log.warn("Попытка сохранить ответ с отрицательным номером вкладки");
            return;
        }

        AnswerStorage.addAnswer(answer);//Answer Storage
        PoolLogger.getInstance().writeLine(answer); //Sum log

        DeviceLogger deviceLogger;
        if(clientsMap.get(clientId).needLog) {
            deviceLogger = clientsMap.get(clientId).logger;
            if(deviceLogger == null){
                log.info("Логгер не был инициализирован ранее");
                clientsMap.get(clientId).logger = new DeviceLogger(clientsMap.get(clientId).clientId);
                deviceLogger = clientsMap.get(clientId).logger;
            }
            assert myProperties != null;
        }
    }

    public void setPoolDelay(int poolDelay) {
        this.poolDelay = poolDelay;
        log.info("Новое значение задержки опроса " + poolDelay);
    }
    public void setTextToSendString(String prf, String cmd, int clientId){
        if(cmd == null || cmd.isEmpty()){
            log.warn("Пустая команда");
            return;
        }
        if(prf == null ){
            log.info("Пустой префикс");
            prf = "";
        }

        if(containClientId(clientId)){
            clientsMap.get(clientId).prefix = prf;
            clientsMap.get(clientId).command = cmd;
            log.info("Значения команды и префикса обновлены в потоке опроса для вкладки устройства " + clientId);
        }else{
            log.info("Клиент с вкладкой " + clientId + " не найден");
        }
    }
    public String getTextToSensByTab(int clientId){
        if(containClientId(clientId)){
            return clientsMap.get(clientId).prefix + clientsMap.get(clientId).command;
        }
        return  null;
    }
    public void setNeedLog(boolean bool, int clientId){
        if(containClientId(clientId)){
            clientsMap.get(clientId).needLog = bool;
        }
        System.out.println("Значение записи в файл изменено на: " + bool + " для клиента " + clientId);
    }
    public boolean isNeedLog(int clientId){
        if(containClientId(clientId)){
            return clientsMap.get(clientId).needLog;
        }else{
            log.warn("Ошибка в поиске флага логирования для клиента " + clientId);
        }
        return false;
    }
    public boolean isNeedPool(int clientId){
        if(containClientId(clientId)){
            return clientsMap.get(clientId).needPool;
        }else{
            log.warn("Ошибка в поиске флага опроса для клиента " + clientId);
        }
        return false;
    }
    public boolean containClientId(int clientId){
        return clientsMap.containsKey(clientId);
    }
    public void addDeviceToService(int clientId, String prf, String cmd, boolean needLog, boolean needPoolFlag) {
        log.info("Добавление клиента в поток " + clientId  + Thread.currentThread().getName());
        DeviceLogger deviceLogger = null;
        if(needLog){
            deviceLogger = new DeviceLogger(clientId);
        }
        clientsMap.put(clientId, new WebSocketDataCollector.ClientData(clientId, needLog, needPoolFlag, prf, cmd, deviceLogger));
    }
    public void removeDeviceFromWebSocketDataCollector(int clientId){ //Когда вкладка закрывается
        log.info("Удаление клиента из потока " + clientId  + Thread.currentThread().getName());
        if(containClientId(clientId))
            clientsMap.remove(clientId);
    }
    public boolean isEmpty(){
        return clientsMap.isEmpty();
    }

    public String closeSession(int clientId) throws ConnectException {
        if(this.session == null){
            throw new ConnectException("Нет сессии порта в потоке опроса");
        }
        String forReturn;
        if(isRootThread(clientId)){
            if (this.session.isOpen()) {
                try {
                    session.close();
                } catch (IOException e) {
                    forReturn = "Соединение не было закрыто. Текст исключения " + e.getMessage() + " адрес " + url;
                    return forReturn;
                }
                forReturn = "Соединение " + session.getUri() + " закрыто ";
            }else{
                forReturn = "Соединение " + session.getUri() + " уже было закрыто ранее ";
            }
        }else{
            throw new ConnectException("Соединение " + session.getUri() + " занято другими вкладками. В управленеии отказано");
        }
        return forReturn;
    }
    public String reopenPort(int clientId, MainLeftPanelStateCollection stateCollection){
        String forReturn;
        if(isRootThread(clientId)){
            if (this.session.isOpen()) {
                try {
                    this.closeSession(clientId);
                } catch (ConnectException e) {
                    throw new RuntimeException(e);
                }
                //comPort.setComPortParameters(stateCollection.getBaudRateValue(clientId), stateCollection.getDataBits(clientId), stateCollection.getStopBits(clientId), stateCollection.getParityBitsValue(clientId), false);
                //comPort.openPort();
                //forReturn = "Порт " + comPort.getSystemPortName() + " переоткрыт ";
            } else {
                //comPort.setComPortParameters(stateCollection.getBaudRateValue(clientId), stateCollection.getDataBits(clientId), stateCollection.getStopBits(clientId), stateCollection.getParityBitsValue(clientId), false);
                //comPort.openPort();
                //forReturn = "Порт " + comPort.getSystemPortName() + " открыт ";
            }
            if (session.isOpen()) {
                //forReturn += "успешно";
            } else {
                //forReturn += "с ошибкой " + comPort.getSystemPortName() + "! Код ошибки: " + comPort.getLastErrorCode();
            }
            return "Не имплементирован метод переотрытия соединения с новыми параметрами";
        }else{
            return "Соединение " + session.getUri() + " занято другими вкладками. В управленеии отказано";
        }
    }
    public boolean isRootThread(Integer clientId) {
        for (WebSocketDataCollector.ClientData client : clientsMap.values()) {
            if (!Objects.equals(client.clientId, clientId) && client.needPool) { //Если найден другой клиент с активным опросом
                log.warn("Найден " + client.clientId + " и " + clientId + " при этом у найденого " + client.needLog);
                return false;
            }
        }
        return true;
    }

    public void setUpdateListener(DataUpdateListener handleDataUpdate) {
        this.listener = handleDataUpdate;
    }

    private void handleIncomingData(JsonNode message) {
        JsonNode jsonNode = message.path(0);

        log.warn("Сообщение отправляется в окно");
        if(message == null){
            log.warn("Null message in handler");
        }

        if(!Objects.equals(message.path("token").asText("token"), "token")){
            token = message.path("token").asText("token");
        }

        if (listener != null) {
            // Вызываем через EDT для потокобезопасности
            SwingUtilities.invokeLater(() ->

                    listener.onDataUpdated(
                            message.path("clientId").asText("clientId"),
                            message.path("action").asText("action"),
                            message.path("data").asText("data"),
                            message.path("cmd").asText("cmd"),
                            message
                    )
            );
        }else{
            log.warn(" Слушатель не найден!");
        }
    }

    private static class ClientData {
        int clientId;
        boolean needLog;
        boolean needPool;
        String prefix;
        String command;
        DeviceLogger logger;

        public ClientData(int clientId, boolean needLog, boolean needPool, String prefix, String command, DeviceLogger logger) {
            this.clientId = clientId;
            this.needLog = needLog;
            this.needPool = needPool;
            this.prefix = prefix;
            this.command = command;
            this.logger = logger;
        }
    }
}


