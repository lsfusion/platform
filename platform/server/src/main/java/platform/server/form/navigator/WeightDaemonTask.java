package platform.server.form.navigator;

import jssc.SerialPort;
import jssc.SerialPortException;
import platform.interop.event.EventBus;
import platform.interop.event.IDaemonTask;

import java.io.Serializable;
import java.math.BigInteger;

public class WeightDaemonTask implements IDaemonTask, Serializable {
    public static final String SCALES_SID = "SCALES";
    private transient EventBus eventBus;
    SerialPort serialPort;
    int prev;
    int period;
    int delay;

    public WeightDaemonTask(int com, int period, int delay) {
        serialPort = new SerialPort("COM" + com);
        this.period = period;
        this.delay = delay;
    }

    @Override
    public void run() {
        try {
            serialPort.openPort();
            serialPort.setParams(4800, 8, 1, 0);
            byte[] msg = {0x4A};
            serialPort.writeBytes(msg);
            byte[] buffer = serialPort.readBytes(5);
            if (isWeighted(buffer)) {
                int newValue = getWeight(buffer);
                if ((newValue > 10) && (Math.abs(prev - newValue) > 10)) {
                    eventBus.enterValue(newValue / 1000.0, SCALES_SID);
                    //System.out.println(newValue);
                }
                prev = newValue;
            }
            serialPort.closePort();
        } catch (SerialPortException ex) {
            System.out.println(ex);
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
