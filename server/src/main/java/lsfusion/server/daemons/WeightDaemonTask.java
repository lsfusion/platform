package lsfusion.server.daemons;

import jssc.SerialPort;
import jssc.SerialPortException;
import lsfusion.interop.event.RepeatableDaemonTask;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

public class WeightDaemonTask extends RepeatableDaemonTask implements Serializable {

    public static final String SCALES_SID = "SCALES";

    SerialPort serialPort;
    int prev;
    int speed;

    public WeightDaemonTask(int com, int speed, int period, int delay) {
        super(delay, period, "weight-daemon-task");
        serialPort = new SerialPort("COM" + com);
        this.speed = speed;
    }

    @Override
    protected void tick() {
        try {
            serialPort.openPort();
            serialPort.setParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
            byte[] msg = {0x4A};
            serialPort.writeBytes(msg);
            byte[] buffer = serialPort.readBytes(5);
            System.out.println(Arrays.toString(buffer));
            if (isWeighted(buffer)) {
                int newValue = getWeight(buffer);
                System.out.println(newValue);
                if ((newValue > 10) && (Math.abs(prev - newValue) > 10)) {
                    double value = newValue / 1000.0;
                    eventBus.fireValueChanged(SCALES_SID, value);
                    //System.out.println(newValue);
                }
                prev = newValue;
            }
            serialPort.closePort();
        } catch (SerialPortException ex) {
            throw new RuntimeException(ex);
        }
    }

    static public int getWeight(byte[] msg) {
        byte[] weight = {msg[4], msg[3], msg[2]};
        BigInteger w = new BigInteger(weight);
        return w.intValue();
    }

    static public boolean isWeighted(byte[] msg) {
        return msg[0] < 0;
    }
}
