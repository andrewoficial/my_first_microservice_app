# Elephant Monitor - Roadmap архитектурных улучшений

## Текущее состояние

- Spring Boot 4.0.7 для DI
- Swing GUI + WebUI (REST контроллеры)
- H2 in-memory база (для будущего серверного режима)
- `AnswerStorage` — `@Service` Spring-бин с экземплярными методами
- REST-контроллеры для серверного режима (`srv-online`, `srv-offline`)
- Профиль `gui-only` по умолчанию (`config/configAccess.properties` → `startupProfile`)
- В `gui-only` JPA/Hikari/Security/WebMvc **не поднимаются** (`GuiOnlyEnvironmentPostProcessor`)
- Узкий component scan: `services` + `utilites.properties` + `web`; GUI-бины через `@Import` (не сканируем `device`/`gui` целиком)
- Холодный старт gui-only (замер 2026-07-16): **Started Main ~2.8 с**, process ~3.7 с (было ~6.9 / ~9.1)
- Transport: `services.transport.serial.*` (ComPort), `services.transport.hid.*` (HidPort, HidCommunicator); MainWindow COM/HID CardLayout + HID scan/filter

---

## Выполнено

### Этап 1: Хранение данных для графиков ✅

- `GraphDataRepository` — per (tabId, command) хранение через `ConcurrentHashMap<Integer, ConcurrentHashMap<String, GraphHistory>>`
- Данные ВСЕХ команд сохраняются; отображается только стабильная команда
- `GraphPoint` с кешированным `Millisecond` (ленивая инициализация)
- `GraphHistory.forEachPoint()` — снапшот под локом, итерация вне лока
- `ChartWindow` — `cachedSeriesRefs` для мгновенного поиска `TimeSeries`, `detectStabilityChanges()` сброс `lastProcessedTime`, `applySeriesVisibility()` скрытие нестабильных серий
- `AnswerLoader.getLastAnswerForTab()` — один проход итератором вместо копирования всей 20K очереди
- `CommandBuffer` в `AnswerStorage` — 3-конsecutive буфер, first-seen-stable правило
- `isCommandStable()` / `getStableCommand()` API

### Этап 2: Сервисный слой для GUI и WebUI ✅

- `ConnectionSettingsService` — управление настройками подключения (baud, parity, stop bits, data bits, protocol, device name)
- `PortLifecycleService` — открытие/закрытие/проверка портов, возвращает результаты (`PortOpenResult`, `PortStatus`)
- `PollingService` — управление опросом (`startPolling`, `updatePoolDelay`, `toggleLog`, `updateCommand`, `sendOnce`)
- `TabService` — жизненный цикл вкладок (`addTab`, `removeTab`, `rebuildTabMappings`, `getTabData`, `getClientIdByTab`)
- `PortManager.java` удалён (заменён `PortLifecycleService`)
- `MainWindow` делегирует все операции сервисам (~150 строк вынесено)
- `TabManager` делегирует бизнес-логику `TabService`

### Контроллеры переписаны на сервисы ✅

- `StateMeasureController` — использует `TabService.getTabData()` и `PollingService.sendOnce()` вместо прямых вызовов `AnswerStorage`
- `TerminalController` — использует `TabService.getStateSize()` вместо `leftPanelStateCollection`
- Все контроллеры на `@RequiredArgsConstructor` вместо `@Autowired` field injection
- Прямые обращения к `AnswerStorage` из контроллерей устранены

### Оптимизации горячего пути ✅

- `ComDataCollector` — `LockSupport.parkNanos(1ms)` вместо `Thread.sleep(20)`, переиспользуемые буферы, `StringBuilder` вместо `byte[]` конкатенации, sleep в главном цикле (30% CPU → 0%)
- `AnswerStorage` — `queueOffset.merge()` атомарный, iterator-based `getAnswersQueForTab()` без копирования
- `ChartWindow` — eliminated allocations в render loop
- `InterruptedException` — корректное восстановление флага прерывания

---

## Осталось

### Этап 3: AnswerStorage → Spring-бин ✅

**Цель**: `AnswerStorage` живёт в контейнере Spring, а не в статике.

**Решение**: преобразован в `@Service`.

- `AnswerStorage.java` — `@Service`, все методы и поля — экземплярные, внедряется через конструктор `GraphDataRepository`
- `GraphDataRepository.java` — `@Service` (singleton-паттерн заменён на Spring-бин)
- `SpringContextHolder.java` — `@Component` implementing `ApplicationContextAware` для доступа из не-Spring кода
- `AnyPoolService.java` — `AnswerStorage` и `AnswerSaverSync` через конструктор (исправлена двойная инстанциализация `AnswerSaverSync`)
- `ComDataCollector.java` — `AnswerStorage` через конструктор
- `MainWindow.java` — `AnswerStorage` через конструктор
- `TabService.java` — `AnswerStorage` через `@RequiredArgsConstructor`
- `AnswerSaverSync.java` — `AnswerStorage` через конструктор
- `MyProperties.java` — `AnswerStorage` через конструктор
- `MyPropertiesSpringCreatingConfiguration.java` — передаёт `AnswerStorage` в `MyProperties`
- `Main.java` — передаёт `AnswerStorage` в `MainWindow`
- Non-Spring GUI классы (`JmenuFile`, `TabMarkersSettings`, `RuleManagmentDialog`, `ChartWindow`, `AnswerLoader`, `WebSocketWindow`) — `AnswerStorage` через конструктор или `SpringContextHolder`
- Protocol parsers (`Mipex2CommandRegistry`, `ArdFeeBrdMeterCommandRegistry`, `Igm10AsciiCommandRegistry`, `Dvk4rdCommandRegistry`, `FULUParser`, `FULSParser`) — `SpringContextHolder.getBean(AnswerStorage.class)` с null-check для тестов
- `GPS_Loger` — удалён неиспользуемый import

### Этап 4: Ускорение запуска (частично ✅)

**Цель**: сократить запуск с ~6.8с до ~1с.

**Проблема**: JPA/Hibernate/Hikari/H2 грузились ~5.4с (79% времени) даже в `gui-only`, где нет ни одного JPA-репозитория.

**Было (до exclude, gui-only):**
```
+0.000s  JVM старт
+1.2s    Spring Boot стартует
+1.5s    Hibernate Validator           ← 0.3с
+2.5s    Spring Data JPA scan          ← 1.0с (0 репозиториев!)
+3.5s    HikariCP pool                 ← 0.5с
+4.0s    Hibernate ORM core            ← 0.7с
+4.8s    JPA EntityManagerFactory      ← 1.5с  ← всё лишнее
+6.0s    MyProperties + tabs           ← 0.6с
+6.8s    Started Main
```

**Стало (EPP + узкий scan, gui-only, замер 2026-07-16):**

```
+0.000s  JVM / process start
+0.00s   readStartupProfile + EPP marker
+1.14s   Starting Main                    (было ~3.4с до Starting)
+1.00s   → MyProperties (scan+бины)       (было ~3.8с)
+0.01s   MyProperties file I/O + вкладки  (не 0.6с — миф развеян логом)
+0.87s   остаток контекста → Started Main
———      Started Main in 2.764s (process 3.74s)   ← было 6.9s / 9.1s
+0.57s   EDT: Nimbus + 2 вкладки + протоколы
```

Маркер EPP: `[GuiOnlyEnvironmentPostProcessor] gui-only: excluded …`  
JPA/Hikari/EntityManagerFactory в логе **нет**.

**Итог по вкладу в ускорение (порядок величины, gui-only cold start):**

| Шаг | Что сделали | Эффект |
|-----|-------------|--------|
| 4.1 | Профиль до Spring (`.profiles` + `configAccess`) | Нужен для всего остального |
| 4.2 | EPP exclude JPA/DataSource/Validation/WebMvc/Security (Boot 4 FQCN) | Убрал Hibernate/Hikari из старта; один только exclude **не** дал «магические 1с» — classpath и scan всё ещё тяжёлые |
| **4.2b** | **Узкий `@SpringBootApplication(scanBasePackages=…)` + `@Import` двух GUI-бинов** | **Главный выигрыш по wall-time:** ~6.9с → ~2.8с Spring; process ~9с → ~3.7с. Не сканируем ~180 `gui` + ~88 `device` class-файлов |

Для потомков: **exclude auto-config ≠ не грузить jar и ≠ не сканировать пакеты.** После EPP узким местом стал **component scan всего `org.example`**. Сужение scan дало больший скачок, чем ожидалось от «просто выключить JPA».

---

#### 4.1 Профиль запуска — ✅

1. **`readStartupProfile()`** — читает `startupProfile` из `config/configAccess.properties` через `java.util.Properties` **до** старта Spring
2. **`.profiles(startupProfile)`** на `SpringApplicationBuilder` — единственный надёжный способ задать профиль до создания контекста
3. **Подавление HHH10001005** — `logback-spring.xml` (на случай серверных профилей, где Hibernate ещё жив)

---

#### 4.2 Exclude JPA/авто-конфигураций в `gui-only` — ✅

##### Файлы решения

| Файл | Назначение |
|------|------------|
| `src/main/java/org/example/config/GuiOnlyEnvironmentPostProcessor.java` | При `gui-only` кладёт `spring.autoconfigure.exclude` в Environment через `MapPropertySource.addFirst` |
| `src/main/resources/META-INF/spring.factories` | Регистрация EPP: ключ `org.springframework.boot.EnvironmentPostProcessor` |
| `Main.java` | Только `.profiles(startupProfile)` — без ручного exclude (EPP делает сам, в т.ч. на `restart()`) |

##### Почему сработало

`EnvironmentPostProcessor` выполняется на `ApplicationEnvironmentPreparedEvent` — **после** `.profiles()`, **до** `AutoConfigurationImportSelector`. Свойство уже в Environment, когда Spring решает, какие auto-config классы импортировать.

Регистрация в **Spring Boot 4**:

```
# META-INF/spring.factories  (не .imports!)
org.springframework.boot.EnvironmentPostProcessor=\
org.example.config.GuiOnlyEnvironmentPostProcessor
```

Интерфейс: `org.springframework.boot.EnvironmentPostProcessor` (пакет `env` — deprecated-путь; Boot 4 грузит основной ключ из `spring.factories`).

##### Имена классов — Spring Boot 4 (не Boot 2/3!)

Старые FQCN из документации/SO по Boot 2–3 **не существуют** как auto-config в Boot 4. Exclude по ним **молча ничего не отключает**.

| Было (Boot 2/3, неверно) | Стало (Boot 4, верно) |
|--------------------------|------------------------|
| `…autoconfigure.orm.jpa.HibernateJpaAutoConfiguration` | `org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration` |
| `…autoconfigure.jdbc.DataSourceAutoConfiguration` | `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration` |
| `…autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration` | `org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration` |
| `…autoconfigure.validation.ValidationAutoConfiguration` | `org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration` |
| `…autoconfigure.web.servlet.WebMvcAutoConfiguration` | `org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration` |
| `…autoconfigure.security.servlet.SecurityAutoConfiguration` | `org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration` |

Актуальный список смотреть в JAR-ах:

```
jar tf ~/.m2/.../spring-boot-hibernate-4.x.jar | findstr AutoConfiguration
# и META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

##### Профиль-зависимость (важно для `restart()`)

- `gui-only` → EPP добавляет exclude → без JPA/Hikari/Security/WebMvc
- `srv-offline` / `srv-online` → EPP **ничего не трогает** → JPA/web доступны
- `Main.restart(newProfile)` создаёт новый контекст; EPP снова смотрит активный профиль — отдельный Main-class не нужен

##### Как проверить по логу

**Успех:**
- есть `[GuiOnlyEnvironmentPostProcessor] gui-only: excluded …`
- **нет** Hibernate ORM, HikariCP, EntityManagerFactory, «Spring Data JPA»
- `Started Main` заметно раньше прежних ~6.8с

**Провал:** нет строки EPP или JPA/Hikari всё ещё поднимаются (часто: забыли `spring.factories` в jar / неверные FQCN / профиль не `gui-only`).

---

#### 4.2b Узкий component scan — ✅ (второй большой выигрыш)

##### Зачем

`@SpringBootApplication` по умолчанию сканирует **пакет класса Main и все подпакеты** → весь `org.example`.  
`@Profile` / exclude auto-config **не отменяют** чтение `.class` ASM'ом. В `device` и почти во всём `gui` **нет** Spring-стереотипов — scan был чистой потерей времени (~3–4 с «тишины» между `Starting Main` и `MyProperties`).

##### Что в коде (`Main.java`)

```java
@SpringBootApplication(scanBasePackages = {
    "org.example.services",           // @Service / @Component приложения
    "org.example.utilites.properties",// MyProperties + @Configuration
    "org.example.web"                 // контроллеры/security; в gui-only отсекаются @Profile
})
@Import({
    MainLeftPanelStateCollection.class,  // единственные @Component в gui —
    ServerSettingsWindow.class           // не тащим scan 180 файлов ради двух классов
})
```

| Пакет | ~java | Стереотипы Spring | Scan? |
|-------|------:|-------------------|-------|
| `services` | ~35 | да | ✅ base package |
| `utilites.properties` | ~4 | да | ✅ base package |
| `web` | ~16 | да (@Profile srv-*) | ✅ base package (дёшево) |
| `gui` | ~180 | **2** класса | ❌, только `@Import` |
| `device` | ~88 | **0** | ❌ |
| `config` | 1 EPP | не бин | ❌ (EPP через `spring.factories`) |

##### Как проверить

В логе при старте:
`Component scan: org.example.services, utilites.properties, web (+ Import: …)`  
Интервал `Starting Main` → первый лог `MyProperties` должен быть порядка **~1 с**, не ~4 с.

##### Ловушка для потомков

Появился новый `@Service` / `@Component` **вне** этих пакетов (например в `gui.foo` или `device.bar`) → Spring его **не увидит**.  
Варианты: перенести класс в `services` / `utilites.properties` / `web`, **или** добавить пакет в `scanBasePackages`, **или** `@Import` точечно.  
Не возвращать scan на весь `org.example` «на всякий случай» — это снова +секунды.

---

#### 4.2c Чтение лога: «всё по два раза» — не два потока и не двойной Spring

По логу gui-only (после 4.2+4.2b) часто кажется, что стартуют два контекста / два `MyProperties`. **Это не так.** Разбор:

##### 1) Три строки подряд про `MyProperties` — **один** конструктор

```
Вызван публичный конструтор с параметром fromSpringContext true
Задаю инстанс при запуске спринга
Инициализирую класс MyProperties
```

Это последовательные `log.info` **внутри одного** `MyProperties(true, …)` (`MyPropertiesSpringCreatingConfiguration` → `@Bean`).  
Признак **двойного** создания: вторая серия тех же строк **или** `Инстанс уже существует (динамический перезапуск?)`.  
Сейчас: один конструктор, `INSTANCE` один раз. `activeProfile … null` — отдельный баг: `@Value("${spring.profiles.active}")` пустой, когда профиль задан через `.profiles()`, а не property (смотреть `Environment.getActiveProfiles()`).

##### 2) Двойной WARN «Не найдена связка…» — **было** шумом lookup'а (исправлено)

**Было:** на каждую новую вкладку 2× WARN (`getClientIdByTabNumber` miss + `addPair` → тот же miss-check).

**Стало (API `MainLeftPanelStateCollection`):**

| Метод | Поведение |
|-------|-----------|
| `findClientIdByTabNumber` / `findTabNumberByClientId` | Silent probe, `-1` если нет — для ensure/create |
| `getClientIdByTabNumber` / `getTabNumberByClientId` | То же значение, miss только **DEBUG** |
| `containTabNumber` | Silent boolean |
| `addPairClientIdTabNumber` | Проверки через `find*`; **одна INFO** «Создана связка clientId=… ↔ tabNumber=…» |
| `MyProperties.processParameters` | `find*` + INFO «вкладка из файла… (первое поле: …)» |

Также починен баг: `getTabNumberByClientId` возвращал **key (clientId)** вместо **value (tabNumber)** — для проверок `== -1` «случайно работало».

Остаётся: в config `idents` / `tabNumbersIdents` length mismatch — починить properties (не API).

##### 3) `ARD_FEE_BRD_METER` / `Command F` / `Вкладка добавлена` ×2 — **две вкладки**

В логе `Количество вкладок 2` → EDT честно поднимает **две** вкладки → два протокола (эмуляция), два `TabService.addTab`. Это не дубль одного объекта.

##### 4) `[background-preinit] Hibernate Validator` — не JPA и не второй Spring

`BackgroundPreinitializer` Spring Boot, jar validator на classpath. EPP exclude `ValidationAutoConfiguration` **не** отключает preinit. При желании:  
`System.setProperty("spring.backgroundpreinitializer.ignore", "true")` в начале `main`.

##### 5) Два формата времени в логе

`12:12:57.596 [main] INFO org.example.Main -- …` (до полной инициализации logback)  
vs `2026-07-16 12:12:58.740 INFO …` (уже Spring/logback).  
Один процесс, смена appender/формата — не два запуска.

---

#### Хронология попыток (чтобы не наступать на те же грабли)

| # | Что пробовали | Результат | Настоящая причина (после разбора) |
|---|---------------|-----------|-----------------------------------|
| 1 | `spring.profiles.default=gui-only` в `application.properties` | Слабо | Профиль нужен **до** контекста; default ≠ active для наших целей |
| 2 | `System.setProperty("spring.profiles.active", …)` | Слабо | Поздний/низкий приоритет относительно того, как мы резолвим профиль |
| 3 | `System.setProperty("spring.autoconfigure.exclude", …)` + **старые FQCN** | Не сработало | В первую очередь **неверные имена классов Boot 2/3** |
| 4 | `spring.autoconfigure.exclude` в `application-gui-only.properties` | Не сработало | Плюс риск тайминга; надёжнее EPP с `addFirst` |
| 5 | `.properties("spring.profiles.active", …)` на Builder | Не сработало | Для профиля нужен `.profiles()`, не properties |
| 6 | `.properties("spring.autoconfigure.exclude", "a,b,c")` (два аргумента) | Не сработало | **`properties(String...)` ждёт `"key=value"`**, не пару key, value! + старые FQCN |
| 7 | `spring.config.name="gui-only"` | Сломало | Spring ищет `gui-only.properties` вместо `application*.properties` |
| 8 | **`.profiles(startupProfile)` на Builder** | ✅ | Профиль задан до контекста |
| 9 | **`readStartupProfile()` из configAccess.properties** | ✅ | Профиль до Spring, без контейнера |
| 10 | **`GuiOnlyEnvironmentPostProcessor` + Boot 4 FQCN + `spring.factories`** | ✅ | Exclude рано, условно по профилю, правильные имена |
| 11 | **Узкий `scanBasePackages` + `@Import` двух GUI-бинов** | ✅ | Убрал scan `gui`+`device`; **~6.9с → ~2.8с** Started Main |

##### Мифы, которые родились по дороге (и что верно на самом деле)

| Миф | Как есть |
|-----|----------|
| «В Boot 4 `spring.autoconfigure.exclude` удалён / не читается из Environment» | **Ложь.** `AutoConfigurationImportSelector` по-прежнему читает `spring.autoconfigure.exclude` через Binder / Environment |
| «Exclude через Properties принципиально невозможен» | **Ложь.** Нужны: (1) свойство **уже** в Environment к моменту import selector, (2) **актуальные** FQCN Boot 4, (3) корректная передача (`"key=value"` или `Map`, или EPP) |
| «Регистрация EPP — файл `…EnvironmentPostProcessor.imports`» | В **Boot 4** рабочий путь — **`META-INF/spring.factories`** с ключом `org.springframework.boot.EnvironmentPostProcessor` |
| `builder.properties("key", "value")` | **Неверно.** Надо: `properties("key=value")` или `properties(Map.of("key", value))` |
| «После exclude JPA старт обязан стать ~1с» | **Нет.** Classpath+scan+GUI остаются. У нас второй скачок дал **scan**, не exclude |
| «Двойные WARN = два потока / два Spring» | **Нет.** Один `[main]`, lookup+addPair оба логируют miss; 2 вкладки → 2 протокола |
| «MyProperties жрёт 0.6с» | **По актуальному логу ~10–30 мс.** Тяжёлым был scan **до** конструктора MyProperties |

##### Отклонённые альтернативы (и когда они имели бы смысл)

1. **`@SpringBootApplication(exclude = {…})`** — безусловно на все профили; ломает `srv-*` / `restart()`, если один Main-class
2. **Maven profiles / выкинуть `spring-boot-starter-data-jpa` из classpath** — чище по classpath, но нужна пересборка и разные артефакты/команды
3. **`spring.main.lazy-initialization=true`** — откладывает создание бинов, не убирает scan/init JPA-стека полностью; риск сюрпризов в GUI-коде

**Выбор для потомков:** один fat-jar, runtime-переключение `gui-only` ↔ `srv-*` → **EPP + Boot 4 FQCN**. Разные поставки «только GUI» vs «сервер» → можно Maven profile без JPA на classpath.

---

#### 4.3 Дальнейшее ускорение / чистка (бэклог)

После 4.2 + 4.2b **MyProperties больше не цель №1** (см. лог: десятки мс). Имеет смысл:

| Приоритет | Что | Зачем |
|-----------|-----|--------|
| ~~Средний~~ | ~~WARN-шум tab↔client lookup~~ | ✅ `find*` + INFO при создании (см. 4.2c) |
| Средний | Починить `idents`/`tabNumbersIdents` length mismatch в `configAccess.properties` | Меньше WARN, корректные association markers |
| Низкий | `backgroundpreinitializer.ignore=true` | Убрать HV preinit с classpath |
| Низкий | Splash + lazy протоколы на EDT (~0.5–1 с после Started) | Perceived startup |
| Если нужен &lt;2 с process | Maven profile без JPA/Security jar на classpath | Cold classloading |
| Не приоритет | «Ускорить MyProperties binary cache» | Выгоды мало при текущих цифрах |

#### 4.4 Чеклист «почему снова медленно» (для будущих регрессий)

1. В логе есть EPP marker? Нет → `spring.factories` / профиль не gui-only.  
2. Есть Hikari / EntityManagerFactory / Spring Data JPA? → exclude FQCN откатились на Boot 2/3 имена или EPP не грузится.  
3. `Starting Main` → `MyProperties` снова ~3–4 с? → кто-то вернул широкий scan или добавил огромный пакет в `scanBasePackages`.  
4. Двойные WARN связок? → см. 4.2c, не паниковать про «два контекста».  
5. Сравнивать **Started Main** и **process running for** — второе включает JVM+classload до Spring.

### Этап 5: HID inventory + transport foundation ✅

**Цель (достигнутая граница):** HID как тип соединения в MainWindow + список/фильтр устройств (как COM ports).  
**Полный poll/send multi-step HID** — **не** в core: см. [spi-arch.md](spi-arch.md) и этап 8 (плагины). Решение 2026-07-16: остановиться на inventory.

#### Архитектура transport

| Роль | Serial | HID |
|------|--------|-----|
| Inventory / выбор | `transport.serial.ComPort` | `transport.hid.HidPort` ✅ |
| Кадр write/read | jSerialComm | `HidCommunicator` / `Impl` ✅ |
| Демон опроса | `ComDataCollector` ✅ | отложено → plugin session (этап 8) |
| UI тип соединения | — | `ConnectionType` + CardLayout + `hidParamForm` ✅ |

**Фильтр product id** (из Multigassens `DeviceRepository`, не cradle-cmd):

- `MULTIGASSENSE_TARGET_PRODUCT_ID = 53456`, `MIKROSENSE_TARGET_PRODUCT_ID = 22356`
- `HidDeviceEntry` / `HidDeviceTypeFilter` / MainWindow «Обновить» + «Только известные»

**Сделано:** connection type UI, `HidPort` scan/list/filter, HID panel combos, roadmap + spi-arch зафиксированы.

### Этап 6: WebSocket-команды из основного окна

**Цель**: отправка простых команд через WebSocket так же, как через COM, из основного окна MainWindow.

**Текущее состояние**: `WebSocketDataCollector` и `WebSocketWindow` существуют как отдельная утилита. Протокол — JSON over WS (`auth_req/resp`, `get_devices_req/resp`). Интеграции с основным `AnyPoolService` нет.

**Решение**: опереться на протокол MGS MKRS для скорости.
- Добавить тип `WebSocket` в `jcbConnectionType`
- Панель настроек WebSocket (адрес, логин, пароль, кнопка «Подключить»)
- `WebSocketDataCollector` — интегрировать в `AnyPoolService` (или создать `WsDataCollector` по аналогии с `ComDataCollector`)
- Протокол MGS MKRS: команды отправляются как байтовые массивы (как в `CradleCommunicationHelper`), ответы парсятся теми же `DeviceCommandRegistry`
- `AnyPoolService` — методы `createWsDataCollector`, `removeWsDataCollector`
- `ConnectionSettingsService` — хранить WS-настройки (url, login, password) в `MainLeftPanelState`

**Файлы для изменения**:
- `MainWindow.java` — listener на `jcbConnectionType`, переключение панелей
- `AnyPoolService.java` — методы для WebSocket коллекторов
- Новый `WsDataCollector.java` — аналог `ComDataCollector` для WebSocket
- `MainLeftPanelState.java` — добавить WS-поля (url, login, password)
- `ConnectionSettingsService.java` — добавить WS-методы
- GUI-форма — панель настроек WebSocket

### Этап 7: Покрытие тестами и анализ покрытия

**Цель**: разобраться с Run with Coverage, обеспечить покрытие тестами для всех сервисов.

**Проблема**: нет системного понимания какие классы покрыты тестами, какие нет. Существующие тесты — в основном на парсеры протоколов. Сервисы (`ConnectionSettingsService`, `PortLifecycleService`, `PollingService`, `TabService`) без тестов.

**Решение**:
- Настроить JaCoCo (`jacoco-maven-plugin`) для генерации отчётов покрытия
- Запустить `mvn test jacoco:report`, проанализировать `target/site/jacoco/index.html`
- Определить классы с нулевым/минимальным покрытием
- Дописать тесты по приоритету:
  1. `TabService` — жизненный цикл вкладок (добавление/удаление/маппинги)
  2. `AnswerStorage` — CRUD операции с ответами, очистка, queueOffset
  3. `ConnectionSettingsService` — управление настройками подключения
  4. `PortLifecycleService` — открытие/закрытие портов (с моками)
  5. `PollingService` — управление опросом
  6. `AnswerSaverSync` — логика синхронизации ответов
  7. `GraphDataRepository` — хранение и чтение данных графиков
  8. `SpringContextHolder` — fallback null-безопасность

### Этап 8: WebUI — графики данных

**Цель:** браузерный UI показывает те же ряды/историю, что и desktop `ChartWindow` / `GraphDataRepository`.

**Контекст:** данные для графиков уже есть (этап 1); REST/сервисы частично есть; WebUI пока без полноценных графиков.

**Направление:**
- API: отдача точек / серий per tab / command (стабильные команды, как в desktop)
- UI: графики в `srv-offline` / `srv-online` (библиотека на выбор: Chart.js / uPlot / аналог)
- Не дублировать логику хранения — читать из `GraphDataRepository` / `AnswerStorage` через сервисы
- Учитывать профиль: в `gui-only` web нет; графики WebUI только при серверном профиле

---

### Этап 9: Представления — карта, план здания, схема устройства

**Цель:** в меню **«Представления»** (или аналог) подразделы с визуализацией пространства/устройства, не только терминал и chart.

#### 9.1 Карта местности
- Логику ориентировать на **ble-gui** (знакомый референс) — слои, маркеры, привязка устройств
- Поставщики тайлов: **OSM**, **Yandex** (и расширяемый интерфейс provider)
- Режим **карта из картинки** (custom image as basemap / geo-unreferenced plan overlay)

#### 9.2 План здания
- Поддержка **этажей** (переключение этажа, свой слой/картинка/привязки на этаж)
- Маркеры клиентов/точек на плане
- Связь с tab/clientId слона

#### 9.3 Схема устройства
- Переключение **видов**:
  - **Состояния** — статичная/условная схема (статусы, индикаторы)
  - **Процессы** — потоки/переходы (анимации или simplified process view)
- Данные с приборов через существующие ответы / graph / state services

**Зависимости:** WebUI + сервисы состояния; ble-gui как источник UX/логики карты; desktop-only MVP возможен, но цель — и WebUI.

---

### Этап 10: «О программе» — ссылки + сайт проекта

**Цель:** в меню **«О программе»** (Info) — кликабельные ссылки:
- **GitHub** репозитория / org
- **Сайт проекта** (landing)

**Сайт** (отдельная поставка, не обязательно внутри jar):
- Сделать простую страницу проекта (описание Elephant Monitor, ссылки download/docs/GitHub)
- URL зафиксировать в about-диалоге и README

**UI:** `JmenuFile` / info dialog — `Desktop.browse(URI)` или HTML-ссылки в about panel.

---

### Этап 11: Система плагинов для протоколов (отложено)

**Статус:** обсуждено, реализация **не сейчас** (вернуться ~через месяц).  
**Полный дизайн:** [spi-arch.md](spi-arch.md)

**Суть:** host (слон) + SPI jar + внешние `plugins/*.jar` (в т.ч. closed-source myDevice с N× HID R/W за один exchange).  
Не расширять `ProtocolsList` enum / `SomeDevice` под каждый новый прибор.

**Когда браться:** после 8–10 или когда понадобится private HID-протокол вне monorepo. SPI — **после** WebUI/представлений в приоритете roadmap, если нет срочного myDevice.

---

## Порядок выполнения

| Этап | Сложность | Эффект | Риск |
|------|-----------|--------|------|
| ~~1. Data for graphs~~ | ~~Низкая~~ | ~~Высокий~~ | ~~Низкий~~ |
| ~~2. Service layer~~ | ~~Средняя~~ | ~~Высокий~~ | ~~Низкий~~ |
| ~~3. AnswerStorage → Spring~~ | ~~Средняя~~ | ~~Средний~~ | ~~Средний~~ |
| ~~4.1–4.2 Профиль + exclude JPA~~ | ~~Низкая~~ | ~~Высокий~~ | ~~Низкий~~ |
| ~~4.2b Узкий component scan~~ | ~~Низкая~~ | ~~Очень высокий~~ | ~~Низкий~~ |
| 4.3 Чистка WARN / splash / classpath | Низкая | Средний | Низкий |
| ~~5. HID inventory + transport~~ | ~~Средняя~~ | ~~Высокий~~ | ~~Низкий~~ |
| 6. WebSocket-команды (полный wire) | Средняя | Высокий | Низкий |
| 7. Покрытие тестами | Высокая | Высокий | Низкий |
| **8. WebUI — графики** | Средняя | Высокий | Низкий |
| **9. Представления (карта / план / схема)** | Высокая | Очень высокий | Средний |
| **10. О программе + сайт + GitHub** | Низкая | Средний | Низкий |
| **11. Protocol plugins (SPI)** | Высокая | Очень высокий | Средний |

Рекомендуемый порядок **сейчас:** **8 → 9 → 10**, затем **7** / **6** по желанию; **11** — [spi-arch.md](spi-arch.md), позже.  
Этап 5 inventory ✅; per-tab `connectionType` в state/config ✅.
