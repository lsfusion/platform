package lsfusion.gwt.client.action.file;

import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GActionDispatcher;

public class GReadAction implements GAction {
    public String sourcePath;
    public boolean isDynamicFormatFileClass;

    @SuppressWarnings("UnusedDeclaration")
    public GReadAction() {}

    public GReadAction(String sourcePath, boolean isDynamicFormatFileClass) {
        this.sourcePath = sourcePath;
        this.isDynamicFormatFileClass = isDynamicFormatFileClass;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
