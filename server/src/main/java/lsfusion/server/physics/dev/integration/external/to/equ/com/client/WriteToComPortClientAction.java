package lsfusion.server.physics.dev.integration.external.to.equ.com.client;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.server.physics.dev.integration.external.to.equ.com.SerialPortHandler;
import lsfusion.server.physics.dev.integration.external.to.equ.com.SerialPortHandler2;

public class WriteToComPortClientAction implements ClientAction {

    public RawFileData file;
    public int baudRate;
    public int comPort;
    public boolean useJSerialComm;

    public WriteToComPortClientAction(RawFileData file, int baudRate, int comPort, boolean useJSerialComm) {
        this.file = file;
        this.baudRate = baudRate;
        this.comPort = comPort;
        this.useJSerialComm = useJSerialComm;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        if(useJSerialComm) {
            return SerialPortHandler2.writeBytes("COM" + comPort, baudRate, file.getBytes());
        } else {
            return SerialPortHandler.writeBytes("COM" + comPort, baudRate, file.getBytes());
        }
    }
}