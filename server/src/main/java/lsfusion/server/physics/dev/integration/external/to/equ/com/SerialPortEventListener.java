package lsfusion.server.physics.dev.integration.external.to.equ.com;

import jssc.SerialPort;
import jssc.SerialPortEvent;

//todo: Replace all usages to lsfusion.base.com.SerialPortEventListener (available since 6.1)

public interface SerialPortEventListener {

    void serialEvent(SerialPortEvent event, SerialPort serialPort);
}