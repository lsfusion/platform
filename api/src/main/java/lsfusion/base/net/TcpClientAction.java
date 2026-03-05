package lsfusion.base.net;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.session.ExternalUtils;

import java.io.IOException;

public class TcpClientAction implements ClientAction {
    public byte[] fileBytes;
    public String host;
    public Integer port;
    public Integer timeout;
    public boolean externalTCPWaitForByteMinusOne;

    public TcpClientAction(byte[] fileBytes, String host, Integer port, Integer timeout, boolean externalTCPWaitForByteMinusOne) {
        this.fileBytes = fileBytes;
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.externalTCPWaitForByteMinusOne = externalTCPWaitForByteMinusOne;
    }

    @Override
    public byte[] dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return ExternalUtils.sendTCP(fileBytes, host, port, timeout, externalTCPWaitForByteMinusOne);
    }
}