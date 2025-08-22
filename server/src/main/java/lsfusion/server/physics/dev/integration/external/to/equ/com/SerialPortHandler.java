package lsfusion.server.physics.dev.integration.external.to.equ.com;

import com.google.common.base.Throwables;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ICleanListener;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

//todo: Replace all usages to lsfusion.base.com.SerialPortHandler (available since 6.1)

public class SerialPortHandler {

    protected static final Logger logger = ServerLoggers.systemLogger;
    private static Map<String, SerialPort> serialPortMap = new HashMap<>();

    public static String writeBytes(String comPort, Integer baudRate, byte[] bytes) {
        String error = null;
        try {
            SerialPort serialPort = serialPortMap.get(comPort);
            if (serialPort != null) {
                serialPort.writeBytes(bytes);
            } else {
                serialPort = new SerialPort(comPort);
                try {
                    serialPort.openPort();
                    serialPort.setParams(baudRate, 8, 1, 0);
                    serialPort.writeBytes(bytes);
                } finally {
                    serialPort.closePort();
                }
            }
        } catch (SerialPortException e) {
            error = e.getMessage();
        }
        return error;
    }

    public static void addSerialPort(ClientActionDispatcher dispatcher, String comPort, Integer baudRate, final SerialPortEventListener serialPortEventListener, int mask) throws SerialPortException {

        if(serialPortMap.isEmpty()) {
            dispatcher.addCleanListener(() -> {

                Throwable t = null;
                for(SerialPort serialPort : serialPortMap.values()) {
                    try {
                        serialPort.removeEventListener();
                        serialPort.closePort();
                    } catch (SerialPortException e) {
                        logger.error("Error releasing scanner: ", e);
                        t = e;
                    }
                }
                serialPortMap.clear();
                if (t != null)
                    throw Throwables.propagate(t);
            });
        }

        final SerialPort serialPort = getSerialPort(comPort);

        serialPort.setParams(baudRate, 8, 1, 0);
        serialPort.setEventsMask(serialPort.getEventsMask() | mask);//Set mask
        serialPort.addEventListener(serialPortEvent -> serialPortEventListener.serialEvent(serialPortEvent, serialPort), mask);


        serialPortMap.put(comPort, serialPort);
    }

    private static SerialPort getSerialPort(String comPort) throws SerialPortException {
        SerialPort serialPort = serialPortMap.get(comPort);
        if(serialPort == null) {
            serialPort = new SerialPort(comPort);
            boolean opened = serialPort.openPort();
            if (!opened) {
                throw new RuntimeException("Не удалось открыть порт " + comPort + ". Попробуйте закрыть все другие приложения, использующие этот порт и перезапустить клиент.");
            }
        }
        return serialPort;
    }
}