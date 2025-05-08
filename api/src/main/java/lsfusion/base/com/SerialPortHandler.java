package lsfusion.base.com;

import com.google.common.base.Throwables;
import jssc.SerialPort;
import jssc.SerialPortException;
import lsfusion.interop.action.ClientActionDispatcher;

import java.util.HashMap;
import java.util.Map;

import static lsfusion.base.BaseUtils.systemLogger;

public class SerialPortHandler {

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
                        systemLogger.error("Error releasing scanner: ", e);
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
                throw new RuntimeException("Can't open port " + comPort + ". Try to close all other applications using this port and restart the client.");
            }
        }
        return serialPort;
    }
}