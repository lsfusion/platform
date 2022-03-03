package lsfusion.gwt.client.action;

import java.util.ArrayList;

public class GClientJSAction extends GExecuteAction {

    public String resource;
    public String resourceName;
    public ArrayList<Object> values;
    public ArrayList<Object> types;
    public boolean isFile;

    @SuppressWarnings("UnusedDeclaration")
    public GClientJSAction() {}

    public GClientJSAction(String resource, String resourceName, ArrayList<Object> values, ArrayList<Object> types, boolean isFile) {
        this.resource = resource;
        this.resourceName = resourceName;
        this.values = values;
        this.types = types;
        this.isFile = isFile;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
