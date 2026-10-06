package org.example.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.utilites.properties.MyProperties;
import org.example.utilites.update.LauncherUpdater;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Тихий фоновый сервис проверки обновлений лаунчера.
 * <p>
 * Аналог {@link UpdateCheckService}, но для {@code launcher.exe} из отдельного
 * GitHub-репозитория. Никогда не трогает интерфейс сам: на любой ошибке состояние
 * переходит в {@link Status#FAILED}, а представление, опрашивающее {@link #hasUpdate()},
 * просто не показывает индикатор.
 */
@Slf4j
@Service
public class LauncherUpdateCheckService {

    public enum Status {
        IDLE,
        CHECKING,
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        FAILED
    }

    /** Минимальный интервал между успешными проверками — 1 час. */
    private static final long MIN_CHECK_INTERVAL_MS = 60L * 60L * 1000L;

    private final MyProperties properties;
    private final LauncherUpdater launcherUpdater = new LauncherUpdater();
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Getter
    private volatile Status status = Status.IDLE;

    @Getter
    private volatile String latestVersion = "";

    @Getter
    private volatile Path installedLauncher;

    public LauncherUpdateCheckService(MyProperties properties) {
        this.properties = properties;
    }

    /** Идемпотентный запуск фоновой проверки. */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        Thread worker = new Thread(() -> runCheck(false), "launcher-update-check");
        worker.setDaemon(true);
        worker.start();
    }

    /** Принудительная повторная проверка. */
    public void refreshAsync() {
        Thread worker = new Thread(() -> runCheck(true), "launcher-update-check-refresh");
        worker.setDaemon(true);
        worker.start();
    }

    private void runCheck(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && properties != null) {
            long last = properties.getLastLauncherUpdateCheckEpochMs();
            if (last > 0 && now - last < MIN_CHECK_INTERVAL_MS) {
                long leftMin = (MIN_CHECK_INTERVAL_MS - (now - last)) / 60000L;
                log.debug("Проверка обновлений лаунчера пропущена (следующая через ~{} мин)", leftMin);
                // Не оставляем IDLE, иначе UI-индикатор будет вечно ждать результата.
                status = Status.UP_TO_DATE;
                return;
            }
        }

        status = Status.CHECKING;
        try {
            LauncherUpdater.CheckResult result = launcherUpdater.checkForUpdate();
            installedLauncher = result.localLauncher;

            if (result.failed) {
                latestVersion = "";
                status = Status.FAILED;
                log.debug("Тихая проверка лаунчера не удалась: {}", result.error);
                return;
            }

            latestVersion = result.latest != null ? result.latest.version : "";
            // Успех = был ответ. Запоминаем время, чтобы не долбить сервер чаще раза в час.
            if (properties != null) {
                properties.setLastLauncherUpdateCheckEpochMs(System.currentTimeMillis());
            }
            if (result.updateAvailable) {
                status = Status.UPDATE_AVAILABLE;
                log.info("Найдено обновление лаунчера: {}", latestVersion);
            } else {
                status = Status.UP_TO_DATE;
                log.debug("Обновлений лаунчера не найдено");
            }
        } catch (Throwable ex) {
            log.debug("Тихая проверка обновлений лаунчера не удалась: {}", ex.toString());
            latestVersion = "";
            status = Status.FAILED;
        }
    }

    /** {@code true} только когда проверка завершилась успешно и есть новая версия. */
    public boolean hasUpdate() {
        return status == Status.UPDATE_AVAILABLE;
    }

    /** Проверка ещё идёт или её результат неизвестен. */
    public boolean isPending() {
        return status == Status.IDLE || status == Status.CHECKING;
    }
}
