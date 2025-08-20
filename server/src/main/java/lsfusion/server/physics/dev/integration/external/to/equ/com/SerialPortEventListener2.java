package lsfusion.server.physics.dev.integration.external.to.equ.com;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;

//todo: Replace all usages to lsfusion.base.com.SerialPortEventListener2 (available since 6.1)

public interface SerialPortEventListener2 {
    void serialEvent(SerialPortEvent event, SerialPort serialPort);
}