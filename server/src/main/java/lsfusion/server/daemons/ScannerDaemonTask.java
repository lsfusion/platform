package lsfusion.server.daemons;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import lsfusion.interop.event.EventBus;
import lsfusion.interop.event.IDaemonTask;

import java.io.Serializable;

public class ScannerDaemonTask implements IDaemonTask, Serializable, SerialPortEventListener {
    public static final String SCANNER_SID = "SCANNER";

    private transient EventBus eventBus;

    int com;
    boolean singleRead = false;
    Integer bytesCount; 

    public ScannerDaemonTask(int com, boolean singleRead) {
        this(com, singleRead, null);
    }

    public ScannerDaemonTask(int com, boolean singleRead, Integer bytesCount) {
        this.com = com;
        this.singleRead = singleRead;
        this.bytesCount = bytesCount;
    }

    transient SerialPort serialPort;

    @Override
    public void run() {

        try {
            serialPort = new SerialPort("COM" + com);
            serialPort.openPort();
            serialPort.setParams(9600, 8, 1, 0);
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR | SerialPort.MASK_CTS | SerialPort.MASK_DSR);//Set mask
            serialPort.addEventListener(this, SerialPort.MASK_RXCHAR | SerialPort.MASK_CTS | SerialPort.MASK_DSR);//Add SerialPortEventListener
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
                    byte[] portBytes;
                    if (bytesCount != null) {
                        Thread.sleep(20);
                        if (bytesCount == -1) {
                            portBytes = serialPort.readBytes();
                        } else {
                            portBytes = serialPort.readBytes(this.bytesCount);
                        }
                    } else {
                        portBytes = serialPort.readBytes(event.getEventValue());
                    }
                    
                    if (portBytes != null) {
                        barcode = "";
                        for (byte portByte : portBytes) {
                            barcode += (char) portByte;
                        }
                        if (!barcode.isEmpty())
                            eventBus.fireValueChanged(SCANNER_SID, barcode.trim());
                    }
                } catch (SerialPortException ex) {
                    throw new RuntimeException(ex);
                } catch (InterruptedException ex) {
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
