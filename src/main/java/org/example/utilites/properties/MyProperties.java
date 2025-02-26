package org.example.utilites.properties;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.example.device.ProtocolsList;
import org.example.gui.MainLeftPanelState;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.AnswerStorage;

import java.util.*;
import java.util.function.BiConsumer;

import org.example.services.comPort.BaudRatesList;
import org.example.services.comPort.DataBitsList;
import org.example.services.comPort.ParityList;
import org.example.services.comPort.StopBitsList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
/**
 * The class responsible for getting the settings from the file
 * (what not to place in the code and on the git-hub)
 *
 * <p>Author: Andrew Kantser</p>
 * <p>Date: 2023-07-01</p>
 *
 * <p>Refactored: 2025-01-05</p>
 */

@Component
@ConfigurationProperties(prefix = "app")
public class MyProperties {
    private static MyProperties INSTANCE;
    private static Logger log = Logger.getLogger(MyProperties.class);

    @Value("${spring.profiles.active:dunno}") // Берем профиль или "dunno", если не установлен
    private String activeProfile;


    @Getter
    private int tabCounter;

    @Setter
    @Getter
    @Value("${app.version}")
    private String version;

    @Setter
    @Getter
    @Value("${app.title}")
    private String title;


    @Value("${fromSpringBooleanMarker}")
    private boolean fromSpring = false;

    @Setter
    @Getter
    private String drv = "";

    @Setter
    @Getter
    private String url = "";

    @Setter
    @Getter
    private String pwd = "";

    @Setter
    @Getter
    private String usr = "";


    @Getter
    private String prt = "";



    private String [] clientAssociationMarkers = new String[2];//Получается в результате атомизации полученных сообщений (например ответ содержит информацию о двух независимых параметрах)
    private Integer [] clientAssociationID = new Integer[2];//Он же Гоша, он же Гоги, он же Жора, он же TabDev, tabN, номер вкладки
    private Integer [] tabNumber = new Integer[2];//Массив ( в той же последоватльности что и все массивы MainLeftPane) номеров вкладок

    @Getter
    private String[] ports = new String[2];

    private String logLevel;


    @Getter
    private boolean needSyncSavingAnswer = true;

    @Getter
    private int syncSavingAnswerTimerLimitMS = 1000; //1 sec

    @Getter
    private int syncSavingAnswerWindowMS = 100000; //100 sec

    // CSV Logging
    @Setter
    @Getter
    private boolean csvLogState = true;

    @Setter
    @Getter
    private String csvLogSeparator = ";";

    @Setter
    @Getter
    private boolean csvLogOutputASCII = false;

    @Setter
    @Getter
    private boolean csvLogInputASCII = false;

    @Setter
    @Getter
    private boolean csvLogInputParsed = false;

    // TXT Logging
    @Setter
    @Getter
    private boolean dbgLogState = false;

    @Setter
    @Getter
    private String dbgLogSeparator = "\t";

    @Setter
    @Getter
    private boolean dbgLogOutputASCII = false;

    @Setter
    @Getter
    private boolean dbgLogOutputHEX = false;

    @Setter
    @Getter
    private boolean dbgLogInputASCII = false;

    @Setter
    @Getter
    private boolean dbgLogInputHEX = false;

    @Setter
    @Getter
    private boolean dbgLogInputParsed = false;



    @Getter
    private MainLeftPanelStateCollection leftPanelStateCollection;




    private final MyPropertiesFileHandler fileHandler = new MyPropertiesFileHandler();
    private MyPropertiesSettingsLoader settingsLoader;

    private java.util.Properties properties;


    // Приватный конструктор для Singleton
    private MyProperties() {
        log.info("Вызван приватный конструктор");
        initializeProperties();
    }

    // Конструктор для Spring
    public MyProperties(boolean fromSpringContext, MainLeftPanelStateCollection mainLeftPanelStateCollection) {
        log.info("Вызван публичный конструтор с параметром fromSpringContext " + fromSpringContext);
        if (fromSpringContext) {
            synchronized (MyProperties.class) {
                if (INSTANCE == null) {
                    leftPanelStateCollection = mainLeftPanelStateCollection;
                    log.info("Задаю инстанс при запуске спринга");
                    initializeProperties();
                    INSTANCE = this;
                    log.info("Количество вкладок " + INSTANCE.getTabCounter());
                    log.info("activeProfile при запуске спринга" + this.activeProfile);

                }else{
                    log.warn("Инстанс уже существует (динамический перезапуск?) ");
                    log.info("Количество вкладок " + INSTANCE.getTabCounter());
                    log.info("activeProfile (динамический перезапуск?)" + this.activeProfile);
                    leftPanelStateCollection = mainLeftPanelStateCollection;
                }
            }
        } else {
            throw new IllegalStateException("Используйте метод getInstance() вне контекста Spring.");
        }
    }

    // Singleton вне Spring
    public static MyProperties getInstance() {
        if (INSTANCE == null) {
            synchronized (MyProperties.class) {
                if (INSTANCE == null) {
                    return null;
//                    log.info("Создаю инстанс при getInstance");
//                    MyProperties myProperties = new MyProperties();
//                    myProperties.initializeProperties();
//                    INSTANCE = myProperties;
//                    log.info("Количество вкладок " + INSTANCE.getTabCounter());
                }
            }
        }
        return INSTANCE;
    }

    @PreDestroy
    public void destroy() {
        log.info("Очистка Singleton при закрытии контекста");
        INSTANCE = null;
    }
    private void initializeProperties() {
        log.info("Инициализирую класс MyProperties");
        if (fileHandler.getSettingFile() == null) {
            log.error("Ошибка при работе с файлом настроек ");
            System.exit(1);
        }
        properties = fileHandler.loadPropertiesFromFile();
        settingsLoader = new MyPropertiesSettingsLoader(fileHandler, properties);

        logLevel = settingsLoader.getString("logLevel", "WARN");
        updateLogLevel(logLevel);
        needSyncSavingAnswer = settingsLoader.getBoolean("needSyncSavingAnswer", false);
        tabCounter = settingsLoader.getInt("tabCounter", 1);
        syncSavingAnswerTimerLimitMS = settingsLoader.getInt("syncSavingAnswerTimerLimitMS", 1000);
        syncSavingAnswerWindowMS = settingsLoader.getInt("syncSavingAnswerWindowMS", 100000);
        usr = settingsLoader.getString("usr", "user");
        pwd = settingsLoader.getString("pwd", "1234");
        prt = settingsLoader.getString("prt", "8075");
        url = settingsLoader.getString("url", "127.0.0.1");
        drv = settingsLoader.getString("drv", "jdbc:postgresql://");
        ports = settingsLoader.getStringArray("ports", "", tabCounter);
        clientAssociationMarkers = settingsLoader.getStringArray("clientAssociationMarkers", "", tabCounter);
        clientAssociationID = settingsLoader.getIntegerArray("clientAssociationID", 0, tabCounter);
        tabNumber = settingsLoader.getIntegerArray("tabNumber", 0, tabCounter);
        updatePairsState();

        // Load CSV settings
        csvLogState = settingsLoader.getBoolean("csvLogState", true);
        csvLogSeparator = settingsLoader.getString("csvLogSeparator", ";");
        csvLogOutputASCII = settingsLoader.getBoolean("csvLogOutputASCII", false);
        csvLogInputASCII = settingsLoader.getBoolean("csvLogInputASCII", true);
        csvLogInputParsed = settingsLoader.getBoolean("csvLogInputParsed", false);

        // Load TXT settings
        dbgLogState = settingsLoader.getBoolean("dbgLogState", false);
        dbgLogSeparator = settingsLoader.getString("dbgLogSeparator", "\t");
        dbgLogOutputASCII = settingsLoader.getBoolean("dbgLogOutputASCII", false);
        dbgLogOutputHEX = settingsLoader.getBoolean("dbgLogOutputHEX", false);
        dbgLogInputASCII = settingsLoader.getBoolean("dbgLogInputASCII", false);
        dbgLogInputHEX = settingsLoader.getBoolean("dbgLogInputHEX", false);
        dbgLogInputParsed = settingsLoader.getBoolean("dbgLogInputParsed", false);

        // Load Clients States
        updateLeftPanelStateCollectionClass();
    }


    public void setNeedSyncSavingAnswer(boolean state){
        this.needSyncSavingAnswer = state;
        settingsLoader.setBoolean("needSyncSavingAnswer", state);
    }



    public void setSyncSavingAnswerTimerLimitMS(int value){
        if(value < 10 || value > 999999){
            log.warn("Передано значение меньше 10 и  больше 999999");
            return;
        }
        this.syncSavingAnswerTimerLimitMS = value;
        settingsLoader.setInt("syncSavingAnswerTimerLimitMS", value);
    }

    public void setSyncSavingAnswerTimerLimitMS(String value){
        if(value == null || value.isEmpty()){
            return;
        }
        int val = 0;
        try{
            val = Integer.parseInt(value);
        }catch (NumberFormatException exp){
            log.warn("Передано неверное значение аргумента setSyncSavingAnswerTimerLimitMS" + exp.getMessage());
        }
        setSyncSavingAnswerTimerLimitMS(val);
    }

    public void setSyncSavingAnswerWindowMS(int value){
        if(value < 100 || value > 99999999){
            log.warn("Передано значение меньше 100 и  больше 99999999");
            return;
        }
        this.syncSavingAnswerTimerLimitMS = value;
        settingsLoader.setInt("syncSavingAnswerWindowMS", value);
    }

    public void setSyncSavingAnswerWindowMS(String value){
        if(value == null || value.isEmpty()){
            return;
        }
        int val = 0;
        try{
            val = Integer.parseInt(value);
        }catch (NumberFormatException exp){
            log.warn("Передано неверное значение аргумента setSyncSavingAnswerWindowMS" + exp.getMessage());
        }
        setSyncSavingAnswerWindowMS(val);
    }

    public void setIdentAndTabBounding(HashMap<String, Integer> pairs) {
        StringBuilder sbTabs = new StringBuilder();
        StringBuilder sbIdents = new StringBuilder();
        for (Map.Entry<String, Integer> stringIntegerEntry : pairs.entrySet()) {
            sbTabs.append(stringIntegerEntry.getValue()).append(", ");
            sbIdents.append(stringIntegerEntry.getKey()).append(", ");
        }
        properties.setProperty("tabNumbersIdents", sbTabs.toString());
        properties.setProperty("idents", sbIdents.toString());
        fileHandler.updateFileFromProperties(properties);
        log.debug("Обновлено значение ассоциаций идентификаторов и вкладок... ");
    }


    public void setPortForTab(String portName, int tabNumber) {
        if (portName == null || portName.isEmpty()) {
            log.warn("В метод сохранения ком-портов передано null название");
            return;
        }

        if (tabNumber < 0 || tabNumber > 999) {
            log.warn("В метод сохранения ком-портов передано недопустимое значение номера вкладки");
            return;
        }

        if (ports == null) {
            ports = new String[Math.max(tabNumber + 1, 2)];
        }

        if (ports.length <= tabNumber) {
            ports = Arrays.copyOf(ports, tabNumber + 1);
            log.info("Произведено выравнивание длины сохраняемого массива к количеству вкладок");
        }

        for (int i = 0; i < ports.length; i++) {
            if (ports[i] == null || ports[i].isEmpty()) {
                ports[i] = "notOpen";
            }
        }

        ports[tabNumber] = portName;
        log.info("Для вкладки " + tabNumber + ". Обновлен ком порт на " + portName);

        settingsLoader.setStringArray("ports", ports, true);
    }

    public void setLogLevel(org.apache.log4j.Level level) {
        this.logLevel = String.valueOf(level);
        properties.setProperty("logLevel", String.valueOf(level));
        fileHandler.updateFileFromProperties(properties);
        log.debug("Обновлено значение последнего logLevel: " + logLevel);
    }

    public void setTabCounter(int counter) {
        this.tabCounter = counter;
        properties.setProperty("tabCounter", String.valueOf(counter));
        fileHandler.updateFileFromProperties(properties);
        log.debug("Обновлено значение последнего tabCounter: " + logLevel);
    }


    public void setCsvLogState (boolean state) {
        settingsLoader.setBoolean("csvLogState", state);
        this.csvLogState = state;
    }

    public void setCsvLogSeparator(String separator) {
        if (separator == null || separator.isEmpty()) {
            log.warn("в setCsvLogSeparator не передан параметр разделителя");
            return;
        }
        this.csvLogSeparator = separator;
        settingsLoader.setString("csvLogSeparator", separator);
    }

    public void setPrt(String prt){
        if (prt == null || prt.isEmpty()) {
            log.warn("в prt не передан параметр разделителя");
            return;
        }
        this.prt = prt;
        settingsLoader.setString("prt", prt);
    }

    public void setCsvLogOutputASCII (boolean state) {
        settingsLoader.setBoolean("csvLogOutputASCII", state);
        this.csvLogOutputASCII = state;
    }

    public void setCsvLogInputASCII (boolean state) {
        settingsLoader.setBoolean("csvLogInputASCII", state);
        this.csvLogInputASCII = state;
    }

    public void setCsvLogInputParsed (boolean state) {
        settingsLoader.setBoolean("csvLogInputParsed", state);
        this.csvLogInputParsed = state;
    }

    public void setDbgLogState(boolean state) {
        settingsLoader.setBoolean("dbgLogState", state);
        this.dbgLogState = state;
    }

    public void setDbgLogSeparator(String separator) {
        if (separator == null || separator.isEmpty()) {
            log.warn("в setDbgLogSeparator не передан параметр разделителя");
            return;
        }
        this.dbgLogSeparator = separator;
        settingsLoader.setString("dbgLogSeparator", separator);
    }

    public void setDbgLogOutputASCII(boolean state) {
        settingsLoader.setBoolean("dbgLogOutputASCII", state);
        this.dbgLogOutputASCII = state;
    }

    public void setDbgLogOutputHEX(boolean state) {
        settingsLoader.setBoolean("dbgLogOutputHEX", state);
        this.dbgLogOutputHEX = state;
    }

    public void setDbgLogInputASCII(boolean state) {
        settingsLoader.setBoolean("dbgLogInputASCII", state);
        this.dbgLogInputASCII = state;
    }

    public void setDbgLogInputHEX(boolean state) {
        settingsLoader.setBoolean("dbgLogInputHEX", state);
        this.dbgLogInputHEX = state;
    }

    public void setDbgLogInputParsed(boolean state) {
        settingsLoader.setBoolean("dbgLogInputParsed", state);
        this.dbgLogInputParsed = state;
    }


    public org.apache.log4j.Level getLogLevel() {
        return Level.toLevel(logLevel);
    }


    public boolean getNeedSyncSavingAnswer() {
        return needSyncSavingAnswer;
    }

    private void updateLogLevel(String logLevel) {
        {
            Logger root = Logger.getRootLogger();
            Enumeration allLoggers = root.getLoggerRepository().getCurrentCategories();
            root.setLevel(Level.toLevel(this.logLevel));
            while (allLoggers.hasMoreElements()) {
                Category tmpLogger = (Category) allLoggers.nextElement();
                tmpLogger.setLevel(Level.toLevel(this.logLevel));
            }
        }
    }

    private void updateLeftPanelStateCollectionClass() {
        if(leftPanelStateCollection == null){
            log.error("Пересоздаю leftPanelStateCollection");
            leftPanelStateCollection = new MainLeftPanelStateCollection();
        }


        // Integer параметры
        Map<String, BiConsumer<Integer, Integer>> intSetters = Map.of(
                "baudRateCode", leftPanelStateCollection::setBaudRate,
                "stopBitsCode", leftPanelStateCollection::setStopBits,
                "protocolCode", leftPanelStateCollection::setProtocol,
                "dataBitsCode", leftPanelStateCollection::setDataBits,
                "parityBitCode", leftPanelStateCollection::setParityBits
        );

        // String параметры
        Map<String, BiConsumer<Integer, String>> stringSetters = Map.of(
                "command", leftPanelStateCollection::setCommandToSend,
                "prefix", leftPanelStateCollection::setPrefixToSend
        );

        // Чтение параметров из настроек
        Map<String, Integer[]> intParams = loadIntegerParams(intSetters.keySet());
        Map<String, String[]> stringParams = loadStringParams(stringSetters.keySet());

        // Обработка параметров
        processParameters(intSetters, intParams);
        processParameters(stringSetters, stringParams);
    }

    private Map<String, Integer[]> loadIntegerParams(Set<String> keys) {
        Map<String, Integer[]> params = new LinkedHashMap<>();
        for (String key : keys) {
            params.put(key, settingsLoader.getIntegerArray(key, 0, tabCounter));
        }
        return params;
    }

    private Map<String, String[]> loadStringParams(Set<String> keys) {
        Map<String, String[]> params = new LinkedHashMap<>();
        for (String key : keys) {
            params.put(key, settingsLoader.getStringArray(key, "", tabCounter));
        }
        return params;
    }

    private <T> void processParameters(Map<String, BiConsumer<Integer, T>> setters, Map<String, T[]> params) {
        for (Map.Entry<String, BiConsumer<Integer, T>> entry : setters.entrySet()) {
            String paramName = entry.getKey();
            BiConsumer<Integer, T> setter = entry.getValue();
            T[] values = params.get(paramName);

            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    if (values[i] != null) {
                        int clientId = leftPanelStateCollection.getClientIdByTabNumber(tabNumber[i]);

                        if (clientId == -1) {
                            // Создаем новый клиент, если не найден
                            clientId = leftPanelStateCollection.getNewRandomId();
                            MainLeftPanelState state = new MainLeftPanelState();
                            state.setClientId(clientId);
                            state.setTabNumber(tabNumber[i]);
                            leftPanelStateCollection.addPairClientIdTabNumber(clientId, tabNumber[i]);
                            leftPanelStateCollection.addOrUpdateIdState(clientId, state);
                        }

                        setter.accept(clientId, values[i]);
                    }
                }
            }
        }
    }



    public void setLastLeftPanel(MainLeftPanelStateCollection leftPanStateInp) {
        if (leftPanStateInp == null) {
            throw new IllegalArgumentException("Переданный объект состояния левой панели не может быть null");
        }

        if (leftPanStateInp.isCollectionEmpty()) {
            throw new IllegalArgumentException("Переданный объект состояния левой панели должен содержать описание хотя бы одной вкладки");
        }

        this.leftPanelStateCollection = leftPanStateInp;

        // Создаем Map для хранения всех параметров
        Map<String, StringBuilder> propertyBuilders = new HashMap<>();
        String[] propertyKeys = {
                "clientId", "tabNumber", "protocol", "protocolCode", "baudRate", "baudRateCode",
                "stopBits", "stopBitsCode", "dataBits", "dataBitsCode",
                "parityBit", "parityBitCode", "command", "prefix"
        };
        for (String key : propertyKeys) {
            propertyBuilders.put(key, new StringBuilder());
        }

        // Заполняем StringBuilder'ы
        for (MainLeftPanelState state : leftPanStateInp.getIdTabStateAsList()) {
            propertyBuilders.get("clientId").append(state.getClientId()).append(", ");
            propertyBuilders.get("tabNumber").append(state.getTabNumber()).append(", ");
            propertyBuilders.get("protocol").append(ProtocolsList.getLikeArray(state.getProtocol())).append(", ");
            propertyBuilders.get("protocolCode").append(state.getProtocol()).append(", ");

            propertyBuilders.get("baudRate").append(BaudRatesList.getNameLikeArray(state.getBaudRate())).append(", ");
            propertyBuilders.get("baudRateCode").append(state.getBaudRate()).append(", ");

            propertyBuilders.get("stopBits").append(StopBitsList.getNameLikeArray(state.getStopBits())).append(", ");
            propertyBuilders.get("stopBitsCode").append(state.getStopBits()).append(", ");

            propertyBuilders.get("dataBits").append(DataBitsList.getNameLikeArray(state.getDataBits())).append(", ");
            propertyBuilders.get("dataBitsCode").append(state.getDataBits()).append(", ");

            propertyBuilders.get("parityBit").append(ParityList.getNameLikeArray(state.getParityBit())).append(", ");
            propertyBuilders.get("parityBitCode").append(state.getParityBit()).append(", ");

            propertyBuilders.get("command").append(state.getCommand()).append(", ");
            propertyBuilders.get("prefix").append(state.getPrefix()).append(", ");
        }

        // Удаляем лишние запятые и пробелы, экранируем специальные символы
        propertyBuilders.forEach((key, sb) -> {
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2); // Убираем последнюю запятую и пробел
            }
            String escapedValue = escapePropertyValue(sb.toString());
            //log.warn(key + " : " + escapedValue);
            properties.setProperty(key, escapedValue);
        });

        fileHandler.updateFileFromProperties(properties);
        log.debug("Обновлено значение класса левой вкладки... ");
    }

    private String escapePropertyValue(String value) {
        return value.replace("\\", "\\\\")  // Экранируем \
                .replace("\n", "\\n")   // Экранируем перевод строки
                .replace("\r", "\\r")   // Экранируем возврат каретки
                .replace("\t", "\\t")   // Экранируем табуляцию
                .replace("=", "\\=")    // Экранируем =
                .replace(":", "\\:");   // Экранируем :
    }


    private void updatePairsState(){
        if (clientAssociationMarkers.length != clientAssociationID.length) {
            log.warn("Количество элементов в idents и tabNumbersIdents не совпадает");
            return;
        }

        // Регистрация связок в AnswerStorage
        for (int i = 0; i < clientAssociationMarkers.length; i++) {
            String ident = clientAssociationMarkers[i];
            Integer tabNumber = clientAssociationID[i];

            if (tabNumber == null) {
                log.warn("Пропущен элемент с индексом " + i + ": идентификатор '" + ident + "' имеет некорректный номер вкладки.");
                continue;
            }

            AnswerStorage.registerDeviceTabPair(ident, tabNumber);
        }

        //log.info("Успешно загружены пары идентификаторов устройств и вкладок: " + AnswerStorage.deviceTabPairs);

    }
}