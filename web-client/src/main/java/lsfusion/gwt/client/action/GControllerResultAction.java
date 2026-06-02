package lsfusion.gwt.client.action;

import lsfusion.gwt.client.classes.GType;

import java.io.Serializable;

// a form-controller exec/eval/change RESULT -> resolves the JS callback (value == null means no/undefined value)
public class GControllerResultAction extends GControllerCallbackAction {
    public GType type;
    public Serializable value;

    @SuppressWarnings("UnusedDeclaration")
    public GControllerResultAction() {}

    public GControllerResultAction(long callbackId, GType type, Serializable value) {
        super(callbackId);
        this.type = type;
        this.value = value;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
