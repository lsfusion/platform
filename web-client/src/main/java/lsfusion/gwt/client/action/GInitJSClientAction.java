package lsfusion.gwt.client.action;

import java.util.List;

public class GInitJSClientAction extends GExecuteAction {

    public List<String> externalResources;

    @SuppressWarnings("UnusedDeclaration")
    public GInitJSClientAction() {}

    public GInitJSClientAction(List<String> externalResources) {
        this.externalResources = externalResources;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
