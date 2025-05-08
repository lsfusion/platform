package lsfusion.gwt.client.action.file;

import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GActionDispatcher;

public class GDeleteFileAction implements GAction {
    public String source;

    @SuppressWarnings("UnusedDeclaration")
    public GDeleteFileAction() {}

    public GDeleteFileAction(String source) {
        this.source = source;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}