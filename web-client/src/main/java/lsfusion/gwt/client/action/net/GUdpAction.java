package lsfusion.gwt.client.action.net;

import lsfusion.gwt.client.action.GActionDispatcher;
import lsfusion.gwt.client.action.GExecuteAction;

public class GUdpAction extends GExecuteAction {
    public String fileBytes;
    public String host;
    public Integer port;

    public GUdpAction() {}

    public GUdpAction(String fileBytes, String host, Integer port) {
        this.fileBytes = fileBytes;
        this.host = host;
        this.port = port;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}