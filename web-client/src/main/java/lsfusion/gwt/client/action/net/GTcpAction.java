package lsfusion.gwt.client.action.net;

import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GActionDispatcher;

public class GTcpAction implements GAction {
    public String fileBytes;
    public String host;
    public Integer port;
    public Integer timeout;

    public GTcpAction() {}

    public GTcpAction(String fileBytes, String host, Integer port, Integer timeout) {
        this.fileBytes = fileBytes;
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public String dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}