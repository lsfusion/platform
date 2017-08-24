package lsfusion.server.daemons;

import com.google.common.base.Throwables;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import lsfusion.interop.event.AbstractDaemonTask;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ScannerDaemonTask extends AbstractDaemonTask implements Serializable, SerialPortEventListener {
    public static final String SCANNER_SID = "SCANNER";

    private int com;
    private final boolean singleRead;
    private Map<String, SerialPort> serialPortMap = new HashMap<>();
    private Map<String, String> barcodeMap = new HashMap<>();


    public ScannerDaemonTask(int com, boolean singleRead) {
        this.com = com;
        this.singleRead = singleRead;
    }

    @Override
    public void start() {
        while (com > 100) {
            connect(com % 100);
            com = com / 100;
        }
        connect(com);
    }

    private void connect(int currentCom) {
        try {
            String portName = "COM" + currentCom;
            SerialPort serialPort = new SerialPort(portName);
            boolean opened = serialPort.openPort();
            if (!opened) {
                throw new RuntimeException("Не удалось открыть порт COM" + currentCom + ". Попробуйте закрыть все другие приложения, использующие этот порт и перезапустить клиент.");
            }
            serialPort.setParams(9600, 8, 1, 0);
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR | SerialPort.MASK_CTS | SerialPort.MASK_DSR);//Set mask
            serialPort.addEventListener(this, SerialPort.MASK_RXCHAR | SerialPort.MASK_CTS | SerialPort.MASK_DSR);//Add SerialPortEventListener
            serialPortMap.put(portName, serialPort);
            barcodeMap.put(portName, "");
        } catch (SerialPortException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void stop() {
        Throwable t = null;
        for (SerialPort serialPort : serialPortMap.values()) {
            try {
                serialPort.removeEventListener();
                serialPort.closePort();
                serialPort = null;
            } catch (SerialPortException e) {
                logger.error("Error releasing scanner: ", e);
                t = e;
            }
        }
        if (t != null)
            throw Throwables.propagate(t);
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        SerialPort serialPort = serialPortMap.get(event.getPortName());
        if (serialPort != null) {
            if (event.isRXCHAR()) {
                String barcode = barcodeMap.get(event.getPortName());
                if (singleRead) {
                    try {
                        byte[] portBytes;

                        Thread.sleep(50);
                        portBytes = serialPort.readBytes();

                        if (portBytes != null) {
                            barcode = "";
                            for (byte portByte : portBytes) {
                                if (((char) portByte) != '\n' && ((char) portByte) != '\r')
                                    barcode += (char) portByte;
                            }
                            if (!barcode.isEmpty())
                                eventBus.fireValueChanged(SCANNER_SID, barcode.trim());
                        }
                    } catch (SerialPortException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                } else if (event.isRXCHAR() && event.getEventValue() > 0) {
                    try {
                        char ch = (char) serialPort.readBytes(1)[0];
                        if (ch >= '0' && ch <= '9')
                            barcode += ch;
                        if (event.getEventValue() == 1) {
                            eventBus.fireValueChanged(SCANNER_SID, barcode);
                            barcode = "";
                        }
                    } catch (SerialPortException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                barcodeMap.put(event.getPortName(), barcode);
            }
        } else
            logger.error("Ignored Event for non existing port " + event.getPortName());
    }
}
