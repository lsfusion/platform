package lsfusion.gwt.client.action.net;

import lsfusion.gwt.client.action.GActionDispatcher;
import lsfusion.gwt.client.action.GExecuteAction;

public class GWriteToSocketAction extends GExecuteAction {
    public String text;
    public String charset;
    public String ip;
    public Integer port;

    public GWriteToSocketAction() {}

    public GWriteToSocketAction(String text, String charset, String ip, Integer port) {
        this.text = text;
        this.charset = charset;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}