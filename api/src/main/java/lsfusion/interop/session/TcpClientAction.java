package lsfusion.interop.session;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;

public class TcpClientAction implements ClientAction {
    public byte[] fileBytes;
    public String host;
    public Integer port;
    public Integer timeout;

    public TcpClientAction(byte[] fileBytes, String host, Integer port, Integer timeout) {
        this.fileBytes = fileBytes;
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public byte[] dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return ExternalUtils.sendTCP(fileBytes, host, port, timeout);
    }
}