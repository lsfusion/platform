package lsfusion.gwt.client.action.com;

import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GActionDispatcher;

public class GWriteToComPortAction implements GAction {
    public String file;
    public String comPort;
    public Integer baudRate;

    public GWriteToComPortAction() {}

    public GWriteToComPortAction(String file, String comPort, Integer baudRate) {
        this.file = file;
        this.comPort = comPort;
        this.baudRate = baudRate;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}