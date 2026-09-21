package org.example.services.transport.hid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Безопасная ветка без нативного hidapi: проверяем мастер-выключатель {@code hid.enabled}.
 * Режимы с включённым стеком проверяются на реальном компьютере.
 */
class HidDeviceScannerTest {

    @Test
    void disabledScannerIsNotEnabledAndReturnsEmpty() {
        HidDeviceScanner.Settings settings = mock(HidDeviceScanner.Settings.class);
        when(settings.isHidEnabled()).thenReturn(false);

        HidDeviceScanner scanner = new HidDeviceScanner(settings);

        assertFalse(scanner.isEnabled());
        assertTrue(scanner.scanAllHidDevices().isEmpty());
        verify(settings, never()).isShowAllHidDevices();
    }
}
