package lsfusion.gwt.form.shared.view.actions;

import lsfusion.gwt.form.shared.view.classes.GType;

import java.io.Serializable;

public class GRequestUserInputAction implements GAction {
    public GType readType;
    public Serializable oldValue;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GRequestUserInputAction() {}

    public GRequestUserInputAction(GType readType, Object oldValue) {
        this.readType = readType;
        this.oldValue = (Serializable) oldValue;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}
