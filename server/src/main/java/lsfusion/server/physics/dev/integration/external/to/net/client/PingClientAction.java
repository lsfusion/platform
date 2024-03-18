package lsfusion.server.physics.dev.integration.external.to.net.client;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.server.physics.dev.integration.external.to.file.FileUtils;

import java.io.IOException;
import java.net.InetAddress;


public class PingClientAction implements ClientAction {

    String host;

    public PingClientAction(String host) {
        this.host = host;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) {
        try {
            return FileUtils.ping(host);
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}