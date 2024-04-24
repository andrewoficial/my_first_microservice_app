package org.example.services;

import org.apache.log4j.Logger;
import org.example.gui.ChartWindow;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;

public class AnswerStorage {
    private static final Logger log = Logger.getLogger(ChartWindow.class);
    static StringBuilder sbAnswer = new StringBuilder();
    static DateTimeFormatter dtfAnswer = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static ArrayList<DeviceAnswer> AN = new ArrayList<>();

    public static void addAnswer(DeviceAnswer answer){
        if(AnswerStorage.AN.size() > 10000){
            //Push to cache
            AnswerStorage.AN.clear();
        }
        AnswerStorage.AN.add(answer);
        log.trace("Получено новое значение ответа от прибора со вкладки " + answer.getTabNumber());
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

