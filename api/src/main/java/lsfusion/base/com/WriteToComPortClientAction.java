package lsfusion.base.com;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

public class WriteToComPortClientAction implements ClientAction {
    public RawFileData file;
    public String comPort;
    public int baudRate;
    public boolean useJssc;

    public WriteToComPortClientAction(RawFileData file, String comPort, int baudRate, boolean useJssc) {
        this.file = file;
        this.baudRate = baudRate;
        this.comPort = comPort;
        this.useJssc = useJssc;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        if(useJssc) {
            return SerialPortHandler.writeBytes(comPort, baudRate, file.getBytes());
        } else {
            return SerialPortHandler2.writeBytes(comPort, baudRate, file.getBytes());
        }
    }
}