package org.example.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.utilites.ProgramUpdater;
import org.example.utilites.properties.MyProperties;
import org.example.utilites.update.SourceChangelog;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Тихий фоновый сервис проверки обновлений.
 * <p>
 * Запускается один раз при старте программы ({@link #start()}) в отдельном демон-потоке
 * и никогда не трогает интерфейс сам. Если проверка не удалась (нет сети, битый ответ,
 * неизвестный формат версии) — состояние просто переходит в {@link Status#FAILED},
 * а UI, опрашивающий {@link #hasUpdate()}, не получает никаких изменений.
 * <p>
 * Представление (меню «Справка») опрашивает {@link #getStatus()} / {@link #hasUpdate()}
 * и по наличию обновления само добавляет или убирает красную точку.
 */
@Slf4j
@Service
public class UpdateCheckService {

    public enum Status {
        /** Проверка ещё не запускалась или результат неизвестен. */
        IDLE,
        /** Проверка выполняется прямо сейчас. */
        CHECKING,
        /** Проверка успешна, новых версий нет. */
        UP_TO_DATE,
        /** Проверка успешна, найдена более новая версия. */
        UPDATE_AVAILABLE,
        /** Проверка не удалась — интерфейс менять не нужно. */
        FAILED
    }

    private final MyProperties properties;
    private final ProgramUpdater programUpdater = new ProgramUpdater();
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Getter
    private volatile Status status = Status.IDLE;

    /** Самая новая найденная версия (пусто, если обновлений нет). */
    @Getter
    private volatile String latestVersion = "";

    @Getter
    private volatile List<SourceChangelog> changelogs = Collections.emptyList();

    public UpdateCheckService(MyProperties properties) {
        this.properties = properties;
    }

    /**
     * Идемпотентный запуск фоновой проверки. Повторные вызовы игнорируются,
     * пока первая проверка не завершилась.
     */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        Thread worker = new Thread(this::runCheck, "update-check");
        worker.setDaemon(true);
        worker.start();
    }

    /** Принудительная повторная проверка (например, после действий пользователя). */
    public void refreshAsync() {
        Thread worker = new Thread(this::runCheck, "update-check-refresh");
        worker.setDaemon(true);
        worker.start();
    }

    private void runCheck() {
        status = Status.CHECKING;
        try {
            String currentVersion = properties != null ? properties.getVersion() : null;
            if (currentVersion == null || currentVersion.isBlank()) {
                status = Status.FAILED;
                return;
            }

            List<SourceChangelog> result = programUpdater.getChangelogsSince(currentVersion);
            changelogs = result != null ? result : Collections.emptyList();

            boolean anyNewer = false;
            String bestVersion = "";
            for (SourceChangelog cl : changelogs) {
                if (cl != null && cl.hasNewer()) {
                    anyNewer = true;
                    bestVersion = cl.getLatestVersion();
                    break;
                }
            }

            latestVersion = anyNewer ? bestVersion : "";
            status = anyNewer ? Status.UPDATE_AVAILABLE : Status.UP_TO_DATE;
            if (anyNewer) {
                log.info("Найдено обновление программы: {}", bestVersion);
            } else {
                log.debug("Обновлений программы не найдено");
            }
        } catch (Throwable ex) {
            log.debug("Тихая проверка обновлений не удалась: {}", ex.toString());
            latestVersion = "";
            changelogs = Collections.emptyList();
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
