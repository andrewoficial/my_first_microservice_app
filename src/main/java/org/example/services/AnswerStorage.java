package org.example.services;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;

public class AnswerStorage {
    public static ArrayList<DeviceAnswer> AN = new ArrayList<>();

    public static void addAnswer(DeviceAnswer answer){
        if(AnswerStorage.AN.size() > 10000){
            //Push to cache
            AnswerStorage.AN.clear();
        }
        AnswerStorage.AN.add(answer);
        System.out.println("Save " + answer.getTabNumber());
    }

    public static String getAnswersForTab(Integer tabNumber, boolean showCommands){
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        for (DeviceAnswer deviceAnswer : AN) {
            if(Objects.equals(deviceAnswer.getTabNumber(), tabNumber)){
                if(showCommands){
                    sb.append(dtf.format(deviceAnswer.getRequestSendTime()));
                    sb.append(":\t");
                    sb.append(deviceAnswer.getRequestSendString());
                    sb.append("\n");
                }
                sb.append(dtf.format(deviceAnswer.getAnswerReceivedTime()));

                if(deviceAnswer.getAnswerReceivedString() != null){
                    sb.append(":\t");
                    sb.append(deviceAnswer.getAnswerReceivedString());
                }
                sb.append("\n");
            }

        }
        return sb.toString();
    }
}
