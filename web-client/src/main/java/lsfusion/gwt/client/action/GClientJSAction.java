package lsfusion.gwt.client.action;

import java.util.ArrayList;
import java.util.List;

public class GClientJSAction extends GExecuteAction {

    public List<String> externalResources;
    public ArrayList<Object> values;
    public ArrayList<Object> types;

    @SuppressWarnings("UnusedDeclaration")
    public GClientJSAction() {}

    public GClientJSAction(List<String> externalResources, ArrayList<Object> values, ArrayList<Object> types) {
        this.externalResources = externalResources;
        this.values = values;
        this.types = types;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
