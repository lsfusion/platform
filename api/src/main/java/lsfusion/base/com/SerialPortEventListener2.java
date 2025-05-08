package lsfusion.base.com;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;

public interface SerialPortEventListener2 {
    void serialEvent(SerialPortEvent event, SerialPort serialPort);
}