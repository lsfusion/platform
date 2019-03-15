package lsfusion.server.physics.dev.integration.external.to.equ.com.client;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.server.physics.dev.integration.external.to.equ.com.SerialPortHandler;

import java.io.IOException;

public class WriteToComPortClientAction implements ClientAction {

    public RawFileData file;
    public int baudRate;
    public int comPort;

    public WriteToComPortClientAction(RawFileData file, int baudRate, int comPort) {
        this.file = file;
        this.baudRate = baudRate;
        this.comPort = comPort;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return SerialPortHandler.writeBytes("COM" + comPort, baudRate, file.getBytes());
    }
}