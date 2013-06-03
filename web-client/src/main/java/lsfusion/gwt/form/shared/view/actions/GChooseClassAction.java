package lsfusion.gwt.form.shared.view.actions;

import lsfusion.gwt.form.shared.view.classes.GObjectClass;

import java.io.IOException;

public class GChooseClassAction implements GAction {
    public boolean concreate;
    public GObjectClass baseClass;
    public GObjectClass defaultClass;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GChooseClassAction() {}

    public GChooseClassAction(boolean concreate, GObjectClass baseClass, GObjectClass defaultClass) {
        this.concreate = concreate;
        this.baseClass = baseClass;
        this.defaultClass = defaultClass;
    }

    public final Object dispatch(GActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}
