import org.example.device.protErstevakMtp4d.ERSTEVAK_MTP4D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ConvertTest {

    @Test
    public void ERSTEVAK_MTP4D() {

        ERSTEVAK_MTP4D device = new ERSTEVAK_MTP4D();
        StringBuilder sb = new StringBuilder("001M^");
        sb.append("\n");
        device.setCmdToSend(sb.toString());

        device.setReceived("001M100023D\r");
        device.parseData();
        assertTrue(device.hasAnswer());
        assertTrue(device.hasValue());
        assertEquals(1000.2,device.getValues().getValues()[0]);
        String devAnswer = device.getAnswer();
        String exceptedValue = "1000.2  unit  ";
        if(! exceptedValue.equals(devAnswer)){
            System.out.println(devAnswer);
            System.out.println(exceptedValue);
        }
        assertTrue(exceptedValue.equals(devAnswer));


        device.setReceived("001M495820Z\r");
        device.parseData();
        assertTrue(device.hasAnswer());
        assertTrue(device.hasValue());
        assertEquals(4.9582,device.getValues().getValues()[0]);
        devAnswer = device.getAnswer();
        exceptedValue = "4.9582  unit  ";
        if(! exceptedValue.equals(devAnswer)){
            System.out.println(devAnswer);
            System.out.println(exceptedValue);
        }
        assertTrue(exceptedValue.equals(devAnswer));

        device.setReceived("001M495820Z\r\n");
        device.parseData();
        assertTrue(device.hasAnswer());
        assertTrue(device.hasValue());
        assertEquals(4.9582,device.getValues().getValues()[0]);
        devAnswer = device.getAnswer();
        exceptedValue = "4.9582  unit  ";
        if(! exceptedValue.equals(devAnswer)){
            System.out.println(devAnswer);
            System.out.println(exceptedValue);
        }
        assertTrue(exceptedValue.equals(devAnswer));


        device.setReceived("001M495820Z\r");
        device.parseData();
        assertTrue(device.hasAnswer());
        assertTrue(device.hasValue());
        assertEquals(4.9582,device.getValues().getValues()[0]);
        devAnswer = device.getAnswer();
        exceptedValue = "4.9582  unit  ";
        if(! exceptedValue.equals(devAnswer)){
            System.out.println(devAnswer);
            System.out.println(exceptedValue);
        }
        assertTrue(exceptedValue.equals(devAnswer));

        device.setReceived("123");
        device.parseData();
        assertTrue(device.hasAnswer());
        assertFalse(device.hasValue());
        assertEquals("123", device.getAnswer());



    }

}

