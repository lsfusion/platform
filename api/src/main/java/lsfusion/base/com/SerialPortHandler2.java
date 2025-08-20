package lsfusion.base.com;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.google.common.base.Throwables;
import jssc.SerialPortException;
import lsfusion.interop.action.ClientActionDispatcher;

import java.util.HashMap;
import java.util.Map;

import static lsfusion.base.BaseUtils.systemLogger;

public class SerialPortHandler2 {
    private static Map<String, SerialPort> serialPortMap = new HashMap<>();

    public static String writeBytes(String comPort, Integer baudRate, byte[] bytes) {
        try {
            SerialPort serialPort = serialPortMap.get(comPort);
            if (serialPort != null) {
                serialPort.writeBytes(bytes, bytes.length);
            } else {
                serialPort = SerialPort.getCommPort(comPort);
                try {
                    serialPort.openPort();
                    serialPort.setBaudRate(baudRate);
                    serialPort.writeBytes(bytes, bytes.length);
                } finally {
                    serialPort.closePort();
                }
            }
        } catch (Exception e) {
            return e.getMessage();
        }

        return null;
    }

    public static void addSerialPort(ClientActionDispatcher dispatcher, String comPort, Integer baudRate, final SerialPortEventListener2 serialPortEventListener, int mask) throws SerialPortException {
        if (serialPortMap.isEmpty()) {
            dispatcher.addCleanListener(() -> {

                Throwable t = null;
                for (SerialPort serialPort : serialPortMap.values()) {
                    try {
                        serialPort.removeDataListener();
                        serialPort.closePort();
                    } catch (Exception e) {
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

        serialPort.setBaudRate(baudRate);
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return mask;
            }

            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {
                serialPortEventListener.serialEvent(serialPortEvent, serialPort);
            }
        });


        serialPortMap.put(comPort, serialPort);
    }

    private static SerialPort getSerialPort(String comPort) {
        SerialPort serialPort = serialPortMap.get(comPort);
        if (serialPort == null) {
            serialPort = SerialPort.getCommPort(comPort);
            boolean opened = serialPort.openPort();
            if (!opened) {
                throw new RuntimeException("Can't open port " + comPort + ". Try to close all other applications using this port and restart the client.");
            }
        }
        return serialPort;
    }

}