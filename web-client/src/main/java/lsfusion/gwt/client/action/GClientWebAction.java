package lsfusion.gwt.client.action;

import lsfusion.gwt.client.classes.GType;

import java.io.Serializable;
import java.util.ArrayList;

public class GClientWebAction implements GAction {

    public String resource;
    public String resourceName;
    public String originalResourceName;
    public ArrayList<Serializable> values;
    public ArrayList<Object> types;
    public GType returnType;
    public boolean isFile;
    public boolean syncType;
    public boolean remove;

    public transient Object execResult;

    @SuppressWarnings("UnusedDeclaration")
    public GClientWebAction() {}

    public GClientWebAction(String resource, String resourceName, String originalResourceName, ArrayList<Serializable> values,
                            ArrayList<Object> types, GType returnType, boolean isFile, boolean syncType, boolean remove) {
        this.resource = resource;
        this.resourceName = resourceName;
        this.originalResourceName = originalResourceName;
        this.values = values;
        this.types = types;
        this.returnType = returnType;
        this.isFile = isFile;
        this.syncType = syncType;
        this.remove = remove;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
