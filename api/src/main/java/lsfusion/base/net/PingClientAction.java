package lsfusion.base.net;

import lsfusion.base.FileUtils;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;


public class PingClientAction implements ClientAction {
    public final String host;

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