package lsfusion.erp.utils.com;

import jssc.SerialPort;
import jssc.SerialPortException;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;
import java.nio.charset.Charset;


public class WriteToComPortClientAction implements ClientAction {

    String text;
    String charset;
    Integer baudRate;
    Integer comPort;

    public WriteToComPortClientAction(String text, String charset, Integer baudRate, Integer comPort) {
        this.text = text;
        this.charset = charset;
        this.baudRate = baudRate;
        this.comPort = comPort;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        try {
            SerialPort serialPort = new SerialPort("COM" + comPort);
            serialPort.openPort();
            serialPort.setParams(baudRate, 8, 1, 0);
            serialPort.writeBytes(text.getBytes(Charset.forName(charset)));
            serialPort.closePort();
        } catch (SerialPortException e) {
            return e.getMessage();
        }
        return null;
    }
}
