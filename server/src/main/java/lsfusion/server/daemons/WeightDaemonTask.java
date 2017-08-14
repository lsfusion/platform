package lsfusion.server.daemons;

import com.google.common.base.Throwables;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import lsfusion.interop.event.AbstractDaemonTask;

import java.io.Serializable;

public class WeightDaemonTask extends AbstractDaemonTask implements Serializable, SerialPortEventListener {
    public static final String SCALES_SID = "SCALES";

    private int com;
    SerialPort serialPort;

    public WeightDaemonTask(int com) {
        this.com = com;
    }

    @Override
    public void start() {
        try {
            serialPort = new SerialPort("COM" + com);
            boolean opened = serialPort.openPort();
            if (!opened) {
                throw new RuntimeException("Не удалось открыть порт COM" + com + ". Попробуйте закрыть все другие приложения, использующие этот порт и перезапустить клиент.");
            }
            serialPort.setParams(4800, 8, 1, 0);
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR | SerialPort.MASK_CTS | SerialPort.MASK_DSR);//Set mask
            serialPort.addEventListener(this, SerialPort.MASK_RXCHAR | SerialPort.MASK_CTS | SerialPort.MASK_DSR);//Add SerialPortEventListener
        } catch (SerialPortException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public void stop() {
        try {
            if(serialPort != null) {
                serialPort.removeEventListener();
                serialPort.closePort();
                serialPort = null;
            }
        } catch (SerialPortException e) {
            logger.error("Error releasing scanner: ", e);
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR()) {
            try {
                Thread.sleep(50);
                byte[] portBytes = serialPort.readBytes();
                if (portBytes != null && portBytes[portBytes.length - 5] == (byte) 0x55 && portBytes[portBytes.length - 4] == (byte) 0xAA) {
                    boolean negate = portBytes[portBytes.length - 1] != 0x00;
                    int weightByte1 = portBytes[portBytes.length - 2];
                    if(weightByte1 < 0)
                        weightByte1 += 256;
                    int weightByte2 = portBytes[portBytes.length - 3];
                    if(weightByte2 < 0)
                        weightByte2 += 256;
                    double weight = ((double)((negate ? -1 : 1) * (weightByte1 * 256 + weightByte2))) / 1000;
                    if(weight >= 0.01) //игнорируем веса до 10г
                        eventBus.fireValueChanged(SCALES_SID, weight);
                }
            } catch (SerialPortException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
