import org.apache.log4j.Logger;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.commands.GetDeviceInfoCommand;
import org.hid4java.HidDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Disabled
public class GetDeviceInfoCommandTest {

    @Mock
    private HidDevice mockDevice;

    @Mock
    private CommandParameters mockParameters;

    private GetDeviceInfoCommand command;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new GetDeviceInfoCommand();

        // Mock device.open() to return true (since it returns boolean)
        Mockito.when(mockDevice.open()).thenReturn(true);

        // Mock device.close() to do nothing (void method)
        doNothing().when(mockDevice).close();

        // Mock device.write() to simulate successful writes (return positive value, e.g., 64)
        Mockito.when(mockDevice.write(ArgumentMatchers.any(byte[].class), anyInt(), anyByte())).thenReturn(64);

        // Mock device.read() to always fill the buffer with 64 zeros and return 64 bytes read
        doAnswer(invocation -> {
            byte[] buffer = invocation.getArgument(0);
            Arrays.fill(buffer, (byte) 0);
            return 64; // Simulate reading 64 bytes, all zeros
        }).when(mockDevice).read(ArgumentMatchers.any(byte[].class), anyInt());
    }

    @Test
    public void testExecuteWithAllZeroResponses() throws Exception {
        // Call the execute method with the mocked device and parameters
        byte[] result = command.execute(mockDevice, mockParameters);

        // You can add assertions here based on expected behavior.
        // For example, since all responses are zeros, the assembled result might be a specific value.
        // Inspect or assert on 'result' as needed.
        // e.g., assertArrayEquals(expectedBytes, result);
        // But based on the code, assembled will likely be an array of zeros or derived from zeros.
    }
}