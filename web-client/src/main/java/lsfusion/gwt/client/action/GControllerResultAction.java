package lsfusion.gwt.client.action;

import lsfusion.gwt.client.classes.GType;

import java.io.Serializable;

// a form-controller exec/eval/change RESULT -> resolves the JS callback: with the one value of a result without
// parameters (null means no/undefined value), with an array of {name, value} of a result with them (it has names)
public class GControllerResultAction extends GControllerCallbackAction {
    public String[] names;
    public GType[] types;
    public Serializable[] values;

    @SuppressWarnings("UnusedDeclaration")
    public GControllerResultAction() {}

    public GControllerResultAction(String[] names, GType[] types, Serializable[] values) {
        this.names = names;
        this.types = types;
        this.values = values;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
