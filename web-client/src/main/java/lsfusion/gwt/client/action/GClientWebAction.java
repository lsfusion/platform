package lsfusion.gwt.client.action;

import lsfusion.gwt.client.classes.GType;

import java.util.ArrayList;

public class GClientWebAction implements GAction {

    public String resource;
    public String resourceName;
    public String originalResourceName;
    public ArrayList<Object> values;
    public ArrayList<Object> types;
    public GType returnType;
    public boolean isFile;
    public boolean syncType;
    public String fontFamily;

    public transient Object execResult;

    @SuppressWarnings("UnusedDeclaration")
    public GClientWebAction() {}

    public GClientWebAction(String resource, String resourceName, String originalResourceName, ArrayList<Object> values, ArrayList<Object> types, GType returnType, boolean isFile, boolean syncType, String fontFamily) {
        this.resource = resource;
        this.resourceName = resourceName;
        this.originalResourceName = originalResourceName;
        this.values = values;
        this.types = types;
        this.returnType = returnType;
        this.isFile = isFile;
        this.syncType = syncType;
        this.fontFamily = fontFamily;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
