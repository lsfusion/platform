package lsfusion.utils.equ.com;

import jssc.SerialPort;
import jssc.SerialPortEvent;

public interface SerialPortEventListener {

    void serialEvent(SerialPortEvent event, SerialPort serialPort);
}