package lsfusion.server.daemons;

import jssc.SerialPort;
import jssc.SerialPortException;
import lsfusion.interop.event.EventBus;
import lsfusion.interop.event.IDaemonTask;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

public class WeightDaemonTask implements IDaemonTask, Serializable {
    public static final String SCALES_SID = "SCALES";
    private transient EventBus eventBus;
    SerialPort serialPort;
    int prev;
    int speed;
    int period;
    int delay;

    public WeightDaemonTask(int com, int speed, int period, int delay) {
        serialPort = new SerialPort("COM" + com);
        this.speed = speed;
        this.period = period;
        this.delay = delay;
    }

    @Override
    public void run() {
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

    public void setEventBus(EventBus eventBus){
        this.eventBus = eventBus;
    }

    @Override
    public int getDelay() {
        return delay;
    }

    @Override
    public int getPeriod() {
        return period;
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
