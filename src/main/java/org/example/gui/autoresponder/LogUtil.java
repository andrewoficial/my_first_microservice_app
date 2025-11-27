package org.example.gui.autoresponder;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtil {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    public static void logReceived(JTextArea jtaLog, byte[] data) {
        String timestamp = TIME_FORMAT.format(new Date());
        String hex = ByteUtils.bytesToHex(data);
        SwingUtilities.invokeLater(() -> jtaLog.append(timestamp + " < <" + hex + "\n"));
    }

    public static void logSent(JTextArea jtaLog, byte[] data) {
        String timestamp = TIME_FORMAT.format(new Date());
        String hex = ByteUtils.bytesToHex(data);
        SwingUtilities.invokeLater(() -> jtaLog.append(timestamp + " > >" + hex + "\n"));
    }

    public static void logMessage(JTextArea jtaLog, String msg) {
        String timestamp = TIME_FORMAT.format(new Date());
        SwingUtilities.invokeLater(() -> jtaLog.append(timestamp + ": " + msg + "\n"));
    }
}