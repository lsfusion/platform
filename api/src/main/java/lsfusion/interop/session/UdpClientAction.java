package lsfusion.interop.session;

import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;

import java.io.IOException;

public class UdpClientAction extends ExecuteClientAction {
    public byte[] fileBytes;
    public String host;
    public Integer port;

    public UdpClientAction(byte[] fileBytes, String host, Integer port) {
        this.fileBytes = fileBytes;
        this.host = host;
        this.port = port;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        ExternalUtils.sendUDP(fileBytes, host, port);
    }
}