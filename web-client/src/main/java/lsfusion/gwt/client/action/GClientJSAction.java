package lsfusion.gwt.client.action;

import java.util.ArrayList;
import java.util.List;

public class GClientJSAction extends GExecuteAction {

    public List<String> externalResources;
    public ArrayList<Object> keys;

    @SuppressWarnings("UnusedDeclaration")
    public GClientJSAction() {}

    public GClientJSAction(List<String> externalResources, ArrayList<Object> keys) {
        this.externalResources = externalResources;
        this.keys = keys;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
