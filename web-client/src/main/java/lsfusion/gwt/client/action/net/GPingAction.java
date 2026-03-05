package lsfusion.gwt.client.action.net;

import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GActionDispatcher;

public class GPingAction implements GAction {
    public String host;

    public GPingAction() {}

    public GPingAction(String host) {
        this.host = host;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}