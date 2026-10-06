package org.example.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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

    private final LauncherUpdater launcherUpdater = new LauncherUpdater();
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Getter
    private volatile Status status = Status.IDLE;

    @Getter
    private volatile String latestVersion = "";

    @Getter
    private volatile Path installedLauncher;

    public LauncherUpdateCheckService() {
    }

    /** Идемпотентный запуск фоновой проверки. */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        Thread worker = new Thread(this::runCheck, "launcher-update-check");
        worker.setDaemon(true);
        worker.start();
    }

    /** Принудительная повторная проверка. */
    public void refreshAsync() {
        Thread worker = new Thread(this::runCheck, "launcher-update-check-refresh");
        worker.setDaemon(true);
        worker.start();
    }

    private void runCheck() {
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
