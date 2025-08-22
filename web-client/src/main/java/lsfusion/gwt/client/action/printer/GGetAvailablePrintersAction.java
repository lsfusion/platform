package lsfusion.gwt.client.action.printer;

import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GActionDispatcher;

public class GGetAvailablePrintersAction implements GAction {
    public GGetAvailablePrintersAction() {}

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}