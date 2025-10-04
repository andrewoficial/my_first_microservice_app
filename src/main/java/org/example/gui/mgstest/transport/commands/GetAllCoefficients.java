package org.example.gui.mgstest.transport.commands;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.CradleCommunicationHelper;
import org.example.gui.mgstest.transport.HidCommandName;
import org.hid4java.HidDevice;

public class GetAllCoefficients implements DeviceCommand {
    private final CradleCommunicationHelper communicator = new CradleCommunicationHelper();
    private final Logger log = Logger.getLogger(GetAllCoefficients.class);

    @Override
    public void execute(HidDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        device.open();
        log.info("Run get all coef (0x05)");
        progress.onProgressUpdate(device, 2, "Opening device...");
        byte [] exceptedAns;
        byte [] answer;

        communicator.doSettingsBytes(device);
//        if(true)
//            return null;
        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device); //NOTE: 0x07, 0x00, 0x00, 0x00, (byte)0xDC DC Похоже на запрос последней команды
        //07 00 00 00 00

        // 01 04 07 02 21 00 00 00
        communicator.resetZeroOffset(device);

        // 01 04 07 02 21 01 03 11 d1 01
        communicator.writeMagikInFirstOffset(device);

        // 01 04 07 02 21 02 0D 54 02 65
        communicator.writeMagikInSecondOffset(device);

        // 01 04 07 02 21 03 6E 00 00 00
        communicator.writeCountInThirdOffset(device, 0x00);

        // 01 04 07 02 21 04 00 05 00 01
        exceptedAns = new byte[]{0x07, (byte)0x80, 0x04, 0x00, 0x78, (byte)0xF0, 0x00};
        answer = communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{0x00, 0x05, 0x00, 0x01}),  // Команда 0x05 GetAllCoefByte,
                exceptedAns,"",10, 70);
        //07 80 04 00 78 F0 00

        //01 04 07 02 21 05 01 00 00 FE
        communicator.writeMagikInFifthOffset(device);

        // 01 04 07 02 21 06 00 00 00 00
        communicator.writeZeroInSixthOffset(device);

        // 01 04 07 02 21 03 6E 06
        communicator.writeCountInThirdOffset(device, 0x06);

        // 01 04 07 02 21 00 E1 40 FF 01
        communicator.cradleActivateTransmit(device);

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);

        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device); //NOTE: 0x07, 0x00, 0x00, 0x00, (byte)0xDC DC Похоже на запрос последней команды
        //07 00 00 00 00

        // 01 04 04 02 23 00 07
        // 01 04 04 02 23 08 07
        // 01 04 04 02 23 10 07
        // 01 04 04 02 23 18 07
        // 01 04 04 02 23 20 07
        // 01 04 04 02 23 28 07
        // 01 04 04 02 23 30 07
        // 01 04 04 02 23 38 07
        // 01 04 04 02 23 40 07
        // 01 04 04 02 23 48 07
        log.info("Начинаю считывание блоков");
        //Добавлено после отладки с 200 до 650
        communicator.safetySleep(650);
        byte[] offsets = new byte[]{0x00, 0x08, 0x10, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40, 0x48};
        byte[] payloads = communicator.assembleCget(device, offsets, (byte) 0x07);
        //Ответ зависит от состояния прибора

        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);
        progress.onProgressUpdate(device, 95, "Ending...");
        progress.onExecutionFinished(device, 95, payloads, this.getName());
    }

    @Override
    public String getDescription() {
        return "Get all coefficients (0x05)";
    }

    @Override
    public HidCommandName getName() {
        return HidCommandName.GET_COEFF;
    }
}
