package lsfusion.server.daemons;

import com.google.common.base.Throwables;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import lsfusion.interop.event.AbstractDaemonTask;

import java.io.Serializable;

public class ScannerDaemonTask extends AbstractDaemonTask implements Serializable, SerialPortEventListener {
    public static final String SCANNER_SID = "SCANNER";

    private final int com;
    private final boolean singleRead;
    private final Integer bytesCount;
    private transient SerialPort serialPort;
    private transient String barcode = "";

    public ScannerDaemonTask(int com, boolean singleRead) {
        this(com, singleRead, null);
    }

    public ScannerDaemonTask(int com, boolean singleRead, Integer bytesCount) {
        this.com = com;
        this.singleRead = singleRead;
        this.bytesCount = bytesCount;
    }

    @Override
    public void start() {
        try {
            serialPort = new SerialPort("COM" + com);
            boolean opened = serialPort.openPort();
            if(!opened) {
                throw new RuntimeException("Не удалось открыть порт COM" + com + ". Попробуйте закрыть все другие приложения, использующие этот порт и перезапустить клиент.");
            }
            serialPort.setParams(9600, 8, 1, 0);
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR | SerialPort.MASK_CTS | SerialPort.MASK_DSR);//Set mask
            serialPort.addEventListener(this, SerialPort.MASK_RXCHAR | SerialPort.MASK_CTS | SerialPort.MASK_DSR);//Add SerialPortEventListener
            barcode = "";
        } catch (SerialPortException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void stop() {
        try {
            serialPort.removeEventListener();
            serialPort.closePort();
            serialPort = null;
        } catch (SerialPortException e) {
            logger.error("Error releasing scanner: ", e);
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR()) {
            if (singleRead) {
                try {
                    byte[] portBytes;
                    if (bytesCount != null) {
                        Thread.sleep(50);
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
                            if (((char)portByte) != '\n' && ((char)portByte) != '\r')
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
