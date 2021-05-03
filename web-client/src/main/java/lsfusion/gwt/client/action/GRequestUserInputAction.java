package lsfusion.gwt.client.action;

import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.property.async.GInputList;

import java.io.Serializable;

public class GRequestUserInputAction implements GAction {
    public GType readType;
    public Serializable oldValue;
    public boolean hasOldValue;

    public GInputList inputList;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GRequestUserInputAction() {}

    public GRequestUserInputAction(GType readType, Object oldValue, boolean hasOldValue, GInputList inputList) {
        this.readType = readType;
        this.oldValue = (Serializable) oldValue;
        this.hasOldValue = hasOldValue;
        this.inputList = inputList;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
