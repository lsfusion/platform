package lsfusion.gwt.client.action;

import java.util.ArrayList;

public class GClientWebAction implements GAction {

    public String resource;
    public String resourceName;
    public ArrayList<Object> values;
    public ArrayList<Object> types;
    public boolean isFile;
    public boolean syncType;

    @SuppressWarnings("UnusedDeclaration")
    public GClientWebAction() {}

    public GClientWebAction(String resource, String resourceName, ArrayList<Object> values, ArrayList<Object> types, boolean isFile, boolean syncType) {
        this.resource = resource;
        this.resourceName = resourceName;
        this.values = values;
        this.types = types;
        this.isFile = isFile;
        this.syncType = syncType;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
