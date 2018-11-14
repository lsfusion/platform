package lsfusion.interop.action;

import lsfusion.base.RawFileData;

import java.io.IOException;

public class WriteToComPortClientAction implements ClientAction {

    public RawFileData file;
    public int baudRate;
    public int comPort;
    public boolean daemon;

    public WriteToComPortClientAction(RawFileData file, int baudRate, int comPort, boolean daemon) {
        this.file = file;
        this.baudRate = baudRate;
        this.comPort = comPort;
        this.daemon = daemon;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}