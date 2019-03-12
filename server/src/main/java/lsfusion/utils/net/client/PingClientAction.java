package lsfusion.utils.net.client;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;
import java.net.InetAddress;


public class PingClientAction implements ClientAction {

    String host;

    public PingClientAction(String host) {
        this.host = host;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        try {
            return InetAddress.getByName(host).isReachable(5000) ? null : "Host is not reachable";
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}