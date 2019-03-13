package lsfusion.server.physics.dev.integration.external.to.equ.com;

import jssc.SerialPort;
import jssc.SerialPortEvent;

public interface SerialPortEventListener {

    void serialEvent(SerialPortEvent event, SerialPort serialPort);
}