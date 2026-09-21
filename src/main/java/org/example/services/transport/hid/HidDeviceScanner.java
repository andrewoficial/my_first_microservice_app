package org.example.services.transport.hid;

import lombok.extern.slf4j.Slf4j;
import org.example.utilites.Constants;
import org.example.utilites.properties.MyProperties;
import org.hid4java.HidDevice;
import org.hid4java.HidServices;
import org.hid4java.HidServicesSpecification;
import org.hid4java.ScanMode;
import org.hid4java.jna.HidApi;
import org.hid4java.jna.HidDeviceInfoStructure;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Централизованный доступ к HID-подсистеме hid4java.
 * <p>
 * Заменяет {@code HidManager.getHidServices()} (у которого дефолтная спецификация включает
 * фоновый скан всех HID-устройств каждые 500 мс и авто-закрытие устройств). Здесь:
 * <ul>
 *   <li>мастер-выключатель {@code hid.enabled} (по умолчанию {@code true});</li>
 *   <li>явная {@link HidServicesSpecification} без автостарта/автоскана;</li>
 *   <li>по умолчанию фильтрованное перечисление только целевых PID
 *       ({@code showAllHidDevices=false}) — чужие мыши/клавиатуры не открываются;</li>
 *   <li>режим {@code showAllHidDevices=true} — старый полный скан, но лениво (один раз).</li>
 * </ul>
 * <p>
 * ВАЖНО: сканер ничего не закрывает ({@code stop()} не вызывается), поэтому живые каналы
 * приборов не рвутся при обновлении списка.
 */
@Slf4j
public final class HidDeviceScanner {

    private static final String SETTING_HID_ENABLED = "hid.enabled";
    private static final String SETTING_SHOW_ALL = "showAllHidDevices";

    /**
     * Источник настроек (вынесен интерфейсом для тестируемости без Spring/MyProperties).
     */
    public interface Settings {
        boolean isHidEnabled();

        boolean isShowAllHidDevices();
    }

    private static final Settings DEFAULT_SETTINGS = new MyPropertiesHidSettings();

    private static volatile HidDeviceScanner instance;

    private static final Constructor<HidDevice> HID_DEVICE_CONSTRUCTOR = resolveHidDeviceConstructor();
    private static final Field HID_DEVICE_MANAGER_FIELD = resolveHidDeviceManagerField();

    private final Settings settings;
    private final HidServicesSpecification spec;
    private final HidServices hidServices;
    private boolean managedScanStarted;

    HidDeviceScanner(Settings settings) {
        this.settings = settings;

        HidServicesSpecification specification = new HidServicesSpecification();
        specification.setAutoStart(false);
        specification.setAutoShutdown(false);
        specification.setAutoDataRead(false);
        specification.setScanMode(ScanMode.NO_SCAN);
        this.spec = specification;

        if (settings.isHidEnabled()) {
            HidServices services = null;
            try {
                services = new HidServices(specification);
            } catch (RuntimeException e) {
                log.error("Не удалось инициализировать HID-подсистему: {}", e.getMessage());
            }
            this.hidServices = services;
        } else {
            log.info("HID-подсистема отключена ({} = false)", SETTING_HID_ENABLED);
            this.hidServices = null;
        }
    }

    public static HidDeviceScanner getInstance() {
        HidDeviceScanner local = instance;
        if (local == null) {
            synchronized (HidDeviceScanner.class) {
                local = instance;
                if (local == null) {
                    local = new HidDeviceScanner(DEFAULT_SETTINGS);
                    instance = local;
                }
            }
        }
        return local;
    }

    /**
     * @return {@code true}, если HID-подсистема загружена (hid.enabled и hidapi инициализировались).
     */
    public boolean isEnabled() {
        return hidServices != null;
    }

    /**
     * Обновляет список HID-устройств, не закрывая уже открытые каналы.
     *
     * @return список устройств (пустой, если HID отключён)
     */
    public synchronized List<HidDevice> scanAllHidDevices() {
        if (!isEnabled()) {
            return List.of();
        }
        if (settings.isShowAllHidDevices()) {
            log.debug("HID: полное перечисление ({} = true)", SETTING_SHOW_ALL);
            ensureManagedScanStarted();
            hidServices.scan();
            return hidServices.getAttachedHidDevices();
        }
        return enumerateTargetHidDevices();
    }

    /**
     * Полный перечисляющий скан hidapi (0,0) запускается ровно один раз, при первом обращении
     * к режиму showAll. Дальше используется только {@link HidServices#scan()}.
     */
    private void ensureManagedScanStarted() {
        if (managedScanStarted) {
            return;
        }
        managedScanStarted = true;
        hidServices.start();
    }

    /**
     * Фильтрованное перечисление: по одному нативному вызову на целевой PID.
     * {@code vendorId=0} — wildcard. hidapi при несовпадающем фильтре не извлекает строки
     * (manufacturer/product/serial) у чужих устройств.
     */
    private List<HidDevice> enumerateTargetHidDevices() {
        List<HidDevice> result = new ArrayList<>();
        enumerateByVidPid(0, Constants.HidCommunication.MULTIGASSENSE_TARGET_PRODUCT_ID, result);
        enumerateByVidPid(0, Constants.HidCommunication.MIKROSENSE_TARGET_PRODUCT_ID, result);
        return result;
    }

    private void enumerateByVidPid(int vendorId, int productId, List<HidDevice> result) {
        HidDeviceInfoStructure root = HidApi.enumerateDevices(vendorId, productId);
        if (root == null) {
            return;
        }
        try {
            HidDeviceInfoStructure info = root;
            do {
                result.add(newHidDevice(info));
                info = info.next();
            } while (info != null);
        } finally {
            HidApi.freeEnumeration(root);
        }
    }

    /**
     * Оборачивает нативный {@link HidDeviceInfoStructure} в {@link HidDevice} через рефлексию.
     * В hid4java конструктор {@code HidDevice} публичный, но принимает package-private
     * {@code HidDeviceManager}, а инстанс менеджера лежит в приватном поле
     * {@code HidServices.hidDeviceManager}.
     */
    private HidDevice newHidDevice(HidDeviceInfoStructure info) {
        try {
            Object manager = HID_DEVICE_MANAGER_FIELD.get(hidServices);
            return HID_DEVICE_CONSTRUCTOR.newInstance(info, manager, spec);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Не удалось создать обёртку HidDevice для нативного перечисления", e);
        }
    }

    private static Constructor<HidDevice> resolveHidDeviceConstructor() {
        try {
            Class<?> managerClass = HidServices.class.getDeclaredField("hidDeviceManager").getType();
            return HidDevice.class.getConstructor(
                    HidDeviceInfoStructure.class, managerClass, HidServicesSpecification.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Несовместимая версия hid4java: ожидается 0.8.0 "
                            + "(HidDevice(HidDeviceInfoStructure, HidDeviceManager, HidServicesSpecification))", e);
        }
    }

    private static Field resolveHidDeviceManagerField() {
        try {
            Field field = HidServices.class.getDeclaredField("hidDeviceManager");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Несовместимая версия hid4java: не найдено поле HidServices.hidDeviceManager", e);
        }
    }

    /**
     * Настройки из {@link MyProperties}; при отсутствии инстанса — безопасные умолчания.
     */
    private static final class MyPropertiesHidSettings implements Settings {

        @Override
        public boolean isHidEnabled() {
            MyProperties properties = MyProperties.getInstance();
            if (properties == null) {
                log.debug("MyProperties недоступен, {} = true по умолчанию", SETTING_HID_ENABLED);
                return true;
            }
            return properties.isHidEnabled();
        }

        @Override
        public boolean isShowAllHidDevices() {
            MyProperties properties = MyProperties.getInstance();
            return properties != null && properties.isShowAllHidDevices();
        }
    }
}
