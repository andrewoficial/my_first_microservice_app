package org.example.gui.mgstest.transport.commands;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.CradleCommunicationHelper;
import org.example.gui.mgstest.transport.HidCommandName;
import org.hid4java.HidDevice;

import java.util.ArrayList;

public class GetAllCoefficients implements DeviceCommand {
    private final CradleCommunicationHelper communicator = new CradleCommunicationHelper();
    private final Logger log = Logger.getLogger(GetAllCoefficients.class);

    @Override
    public void execute(HidDevice device, CommandParameters parameters, MgsExecutionListener progress) throws Exception {
        setStatusExecution(device, progress, "Opening device", 5);
        device.open();

        byte [] exceptedAns;
        setStatusExecution(device, progress, "doSettingsBytes", 10);
        communicator.doSettingsBytes(device);

        setStatusExecution(device, progress, "cradleSwitchOn", 15);
        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device); //NOTE: 0x07, 0x00, 0x00, 0x00, (byte)0xDC DC Похоже на запрос последней команды

        setStatusExecution(device, progress, "resetZeroOffset", 20);
        // 01 04 07 02 21 00 00 00
        communicator.resetZeroOffset(device);

        setStatusExecution(device, progress, "writeMagikInFirstOffset", 25);
        // 01 04 07 02 21 01 03 11 d1 01
        communicator.writeMagikInFirstOffset(device);

        setStatusExecution(device, progress, "writeMagikInSecondOffset", 30);
        // 01 04 07 02 21 02 0D 54 02 65
        communicator.writeMagikInSecondOffset(device);

        setStatusExecution(device, progress, "writeCountInThirdOffset", 35);
        // 01 04 07 02 21 03 6E 00 00 00
        communicator.writeCountInThirdOffset(device, 0x00);

        setStatusExecution(device, progress, "write command byte at addr 04", 40);
        // 01 04 07 02 21 04 00 05 00 01
        exceptedAns = new byte[]{0x07, (byte)0x80, 0x04, 0x00, 0x78, (byte)0xF0, 0x00};
        ArrayList <byte []> answers = new ArrayList<>();
        answers.add(exceptedAns);
        communicator.waitForResponse(device,
                () -> communicator.cradleWriteBlock(device, (byte) 0x04, new byte[]{0x00, 0x05, 0x00, 0x01}),  // Команда 0x05 GetAllCoefByte,
                answers,"GetAllCoeff",5, 3, 200, 300);
        //07 80 04 00 78 F0 00

        setStatusExecution(device, progress, "writeMagikInFifthOffset", 45);
        //01 04 07 02 21 05 01 00 00 FE
        communicator.writeMagikInFifthOffset(device);

        setStatusExecution(device, progress, "writeZeroInSixthOffset", 50);
        // 01 04 07 02 21 06 00 00 00 00
        communicator.writeZeroInSixthOffset(device);

        setStatusExecution(device, progress, "writeCountInThirdOffset", 55);
        // 01 04 07 02 21 03 6E 06
        communicator.writeCountInThirdOffset(device, 0x06);

        setStatusExecution(device, progress, "cradleActivateTransmit", 60);
        // 01 04 07 02 21 00 E1 40 FF 01
        communicator.cradleActivateTransmit(device);

        setStatusExecution(device, progress, "cradleSwitchOff", 62);
        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);

        setStatusExecution(device, progress, "cradleSwitchOn", 67);
        // 01 02 02 01 0D
        communicator.cradleSwitchOn(device); //NOTE: 0x07, 0x00, 0x00, 0x00, (byte)0xDC DC Похоже на запрос последней команды
        //07 00 00 00 00

        setStatusExecution(device, progress, "run read device answer", 70);
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
        byte[] offsets = new byte[]{0x00, 0x08, 0x10, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40, 0x48};
        byte[] payloads = communicator.assembleCget(device, offsets, (byte) 0x07);
        //Ответ зависит от состояния прибора

        setStatusExecution(device, progress, "cradleSwitchOff", 95);
        // 01 02 02 00 00
        communicator.cradleSwitchOff(device);

        setStatusExecution(device, progress, "done", 100);
        progress.onExecutionFinished(device, 100, payloads, this.getName());
    }

    private void setStatusExecution(HidDevice device, MgsExecutionListener progress, String comment, int percent){
        log.info("Do [" + getDescription() + "]... ["+comment+"]:" + percent);
        progress.onProgressUpdate(device, percent, "Do [" + getDescription() + "]... ["+comment+"]");
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
