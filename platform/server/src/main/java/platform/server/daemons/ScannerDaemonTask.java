package platform.server.daemons;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import platform.interop.event.EventBus;
import platform.interop.event.IDaemonTask;

import java.io.Serializable;

public class ScannerDaemonTask implements IDaemonTask, Serializable, SerialPortEventListener {
    public static final String SCANNER_SID = "SCANNER";

    private transient EventBus eventBus;

    SerialPort serialPort;
    boolean singleRead = false;

    public ScannerDaemonTask(int com, boolean singleRead) {
        serialPort = new SerialPort("COM" + com);
        this.singleRead = singleRead;
    }

    @Override
    public void run() {

        try {
            serialPort.openPort();
            serialPort.setParams(9600, 8, 1, 0);
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR);//Set mask
            serialPort.addEventListener(this);//Add SerialPortEventListener
            barcode = "";
        } catch (SerialPortException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public int getDelay() {
        return 0;
    }

    @Override
    public int getPeriod() {
        return 2000000000;
    }

    @Override
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    private transient String barcode = "";

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR()) {
            if (singleRead) {
                try {
                    byte[] portBytes = serialPort.readBytes(event.getEventValue());
                    barcode = "";
                    for (byte portByte : portBytes) {
                        barcode += (char) portByte;
                    }
                    if (!barcode.isEmpty())
                        eventBus.fireValueChanged(SCANNER_SID, barcode);
                } catch (SerialPortException ex) {
                    throw new RuntimeException(ex);
                }
            } else
                if (event.isRXCHAR() && event.getEventValue() > 0) {
                    try {
                        char ch = (char)serialPort.readBytes(1)[0];
                        if (ch >= '0' && ch <= '9')
                            barcode += ch;
                        if (event.getEventValue() == 1) {
                            eventBus.fireValueChanged(SCANNER_SID, barcode);
                            barcode = "";
                        }
                    }
                    catch (SerialPortException ex) {
                        throw new RuntimeException(ex);
                    }
                }
        }
    }
}
