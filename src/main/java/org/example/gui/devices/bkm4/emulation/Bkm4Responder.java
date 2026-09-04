package org.example.gui.devices.bkm4.emulation;

import java.util.Locale;

/**
 * ASCII-отклик протокола БКМ-4 (см. {@code bkm4.md}).
 * <p>
 * Принимает команду вида {@code &A?}, {@code &A1}, {@code &S1500}, {@code &F?}, {@code &G1}
 * и возвращает ответ {@code @...} БЕЗ символа CR (CR добавляет транспорт).
 * Неизвестная/ошибочная команда → {@code @ERROR}.
 */
public class Bkm4Responder {

    private final Bkm4Emulator emulator;

    public Bkm4Responder(Bkm4Emulator emulator) {
        this.emulator = emulator;
    }

    public synchronized String processCommand(String raw) {
        if (raw == null) {
            return "@ERROR";
        }
        String cmd = raw.trim();
        if (cmd.isEmpty()) {
            return "@ERROR";
        }
        if (cmd.charAt(0) != '&') {
            return "@ERROR";
        }
        String body = cmd.substring(1);

        // &A? / &A0 / &A1 — режим работы
        if (body.startsWith("A")) {
            return handleMode(body.substring(1));
        }
        // &V? / &V0..&V4 — газовый клапан
        if (body.startsWith("V")) {
            return handleValve(body.substring(1));
        }
        // &S? / &Sxxxx — уставка расхода
        if (body.startsWith("S")) {
            return handleSetpoint(body.substring(1));
        }
        // &F? — фактический расход
        if (body.startsWith("F")) {
            return handleFlow(body.substring(1));
        }
        // &G? / &G0 / &G1 — генерация
        if (body.startsWith("G")) {
            return handleGeneration(body.substring(1));
        }
        return "@ERROR";
    }

    private String handleMode(String arg) {
        if (arg.startsWith("?")) {
            return "@A" + emulator.getMode();
        }
        try {
            int v = Integer.parseInt(arg);
            if (v == 0 || v == 1) {
                emulator.setMode(v);
                return "@A" + emulator.getMode();
            }
        } catch (NumberFormatException ignored) {
            // fall through to ERROR
        }
        return "@ERROR";
    }

    private String handleValve(String arg) {
        if (arg.startsWith("?")) {
            return "@V" + emulator.getValve();
        }
        try {
            int v = Integer.parseInt(arg);
            if (v >= 0 && v <= 4) {
                emulator.setValve(v);
                return "@V" + emulator.getValve();
            }
        } catch (NumberFormatException ignored) {
            // fall through to ERROR
        }
        return "@ERROR";
    }

    private String handleSetpoint(String arg) {
        if (arg.startsWith("?")) {
            return "@S" + Math.round(emulator.getSetpointMlMin());
        }
        try {
            int v = Integer.parseInt(arg);
            if (v >= 0 && v <= 3000) {
                emulator.setSetpointMlMin(v);
                return "@S" + Math.round(emulator.getSetpointMlMin());
            }
        } catch (NumberFormatException ignored) {
            // fall through to ERROR
        }
        return "@ERROR";
    }

    private String handleFlow(String arg) {
        if (arg.startsWith("?")) {
            return "@F" + formatFlow(emulator.getCurrentFlowMlMin());
        }
        return "@ERROR";
    }

    private String handleGeneration(String arg) {
        if (arg.startsWith("?")) {
            return "@G" + emulator.getGeneration();
        }
        try {
            int v = Integer.parseInt(arg);
            if (v == 0 || v == 1) {
                emulator.setGeneration(v);
                return "@G" + emulator.getGeneration();
            }
        } catch (NumberFormatException ignored) {
            // fall through to ERROR
        }
        return "@ERROR";
    }

    private static String formatFlow(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}