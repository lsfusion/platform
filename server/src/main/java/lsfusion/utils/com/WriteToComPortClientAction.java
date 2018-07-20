package lsfusion.utils.com;

import jssc.SerialPort;
import jssc.SerialPortException;
import lsfusion.base.BaseUtils;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;


public class WriteToComPortClientAction implements ClientAction {

    byte[] file;
    Integer baudRate;
    Integer comPort;

    public WriteToComPortClientAction(byte[] file, Integer baudRate, Integer comPort) {
        this.file = file;
        this.baudRate = baudRate;
        this.comPort = comPort;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        try {
            SerialPort serialPort = new SerialPort("COM" + comPort);
            serialPort.openPort();
            serialPort.setParams(baudRate, 8, 1, 0);
            serialPort.writeBytes(BaseUtils.getFile(file));
            serialPort.closePort();
        } catch (SerialPortException e) {
            return e.getMessage();
        }
        return null;
    }
}
