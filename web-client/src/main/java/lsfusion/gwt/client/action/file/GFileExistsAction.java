package lsfusion.gwt.client.action.file;

import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GActionDispatcher;

public class GFileExistsAction implements GAction {
    public String source;

    @SuppressWarnings("UnusedDeclaration")
    public GFileExistsAction() {}

    public GFileExistsAction(String source) {
        this.source = source;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}