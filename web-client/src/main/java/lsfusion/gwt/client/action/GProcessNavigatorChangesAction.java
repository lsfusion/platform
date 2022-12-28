package lsfusion.gwt.client.action;

import lsfusion.gwt.client.GNavigatorChangesDTO;

public class GProcessNavigatorChangesAction extends GExecuteAction {
    public GNavigatorChangesDTO navigatorChanges;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GProcessNavigatorChangesAction() {}

    public GProcessNavigatorChangesAction(GNavigatorChangesDTO navigatorChanges) {
        this.navigatorChanges = navigatorChanges;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}