package lsfusion.gwt.client.action.file;

import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GActionDispatcher;

public class GListFilesAction implements GAction {
    public String source;
    public boolean recursive;

    @SuppressWarnings("UnusedDeclaration")
    public GListFilesAction() {}

    public GListFilesAction(String source, boolean recursive) {
        this.source = source;
        this.recursive = recursive;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}