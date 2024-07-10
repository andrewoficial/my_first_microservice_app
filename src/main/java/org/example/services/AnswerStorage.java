package org.example.services;

import org.apache.log4j.Logger;
import org.example.gui.ChartWindow;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AnswerStorage {
    private static final Logger log = Logger.getLogger(AnswerStorage.class);
    static StringBuilder sbAnswer = new StringBuilder();
    static DateTimeFormatter dtfAnswer = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static ArrayList<DeviceAnswer> AN = new ArrayList<>();
    public static HashMap <String, Integer> deviceTabPairs = new HashMap<>();
    public static void registerDeviceTabPair(String ident, Integer tabN){
        AnswerStorage.deviceTabPairs.put(ident, tabN);
        System.out.println("IDN:" + ident + " bounded with tab num: " + tabN);
        System.out.println("Now contain:" + AnswerStorage.deviceTabPairs.size());
    }
    public static Integer getTabByIdent(String ident){
        return (Integer) AnswerStorage.deviceTabPairs.get(ident);
    }

    public static String getIdentByTab(Integer tab){
        for (Map.Entry<String, Integer> stringIntegerEntry : deviceTabPairs.entrySet()) {
            if(Objects.equals(stringIntegerEntry.getValue(), tab)){
                return stringIntegerEntry.getKey();
            }
        }
        System.out.println("Попытка получить несуществующую связку Вкладка/Прибор по признаку вкладки");
        return null;
    }
    public static void addAnswer(DeviceAnswer answer){
        if(AnswerStorage.AN.size() > 10000){
            //Push to cache
            AnswerStorage.AN.clear();
        }
        AnswerStorage.AN.add(answer);
        //System.out.println("Add answer");
        log.info("Новое значение ответа со вкладки " + answer.getTabNumber() + " протокол " + answer.getDeviceType().getClass().getSimpleName() + " строка  :" + answer.getAnswerReceivedString());

    }

    public static TabAnswerPart getAnswersQueForTab(Integer lastPosition, Integer tabNumber, boolean showCommands){
        sbAnswer.delete(0, sbAnswer.length());
        if(lastPosition > AN.size()){
            return new TabAnswerPart("-- \n", 0);
        }
        int i = lastPosition;
        for ( ; i < AN.size(); i++) {
            if(Objects.equals(AN.get(i).getTabNumber(), tabNumber)){
                if(showCommands){
                    sbAnswer.append(dtfAnswer.format(AN.get(i).getRequestSendTime()));
                    sbAnswer.append(":\t");
                    sbAnswer.append(AN.get(i).getRequestSendString());
                    sbAnswer.append("\n");
                }
                sbAnswer.append(dtfAnswer.format(AN.get(i).getAnswerReceivedTime()));

                if(AN.get(i).getAnswerReceivedString() != null){
                    sbAnswer.append(":\t");
                    sbAnswer.append(AN.get(i).getAnswerReceivedString());
                }
                sbAnswer.append("\n");
            }
        }
        return new TabAnswerPart(sbAnswer.toString(), i);
    }
    public static String getAnswersForTab(Integer tabNumber, boolean showCommands){

        sbAnswer.delete(0, sbAnswer.length());

        for (DeviceAnswer deviceAnswer : AN) {
            if(Objects.equals(deviceAnswer.getTabNumber(), tabNumber)){
                if(showCommands){
                    sbAnswer.append(dtfAnswer.format(deviceAnswer.getRequestSendTime()));
                    sbAnswer.append(":\t");
                    sbAnswer.append(deviceAnswer.getRequestSendString());
                    sbAnswer.append("\n");
                }
                sbAnswer.append(dtfAnswer.format(deviceAnswer.getAnswerReceivedTime()));

                if(deviceAnswer.getAnswerReceivedString() != null){
                    sbAnswer.append(":\t");
                    sbAnswer.append(deviceAnswer.getAnswerReceivedString());
                }
                sbAnswer.append("\n");
            }

        }
        return sbAnswer.toString();
    }


}

