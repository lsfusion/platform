package lsfusion.gwt.client.action;

import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;

import java.io.Serializable;

public class GRequestUserInputAction implements GAction {
    public GType readType;
    public Serializable oldValue;
    public boolean hasOldValue; // assert !hasOldValue => oldValue = null

    public String customChangeFunction;

    public GInputList inputList;
    public GInputListAction[] inputListActions;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GRequestUserInputAction() {}

    public GRequestUserInputAction(GType readType, Object oldValue, boolean hasOldValue, String customChangeFunction, GInputList inputList, GInputListAction[] inputListActions) {
        this.readType = readType;
        this.oldValue = (Serializable) oldValue;
        this.hasOldValue = hasOldValue;
        this.customChangeFunction = customChangeFunction;
        this.inputList = inputList;
        this.inputListActions = inputListActions;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
