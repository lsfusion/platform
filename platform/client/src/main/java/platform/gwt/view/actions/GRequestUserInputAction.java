package platform.gwt.view.actions;

import platform.gwt.view.changes.dto.ObjectDTO;
import platform.gwt.view.classes.GType;

public class GRequestUserInputAction implements GAction {
    public GType readType;
    public ObjectDTO oldValue;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GRequestUserInputAction() {}

    public GRequestUserInputAction(GType readType, ObjectDTO oldValue) {
        this.readType = readType;
        this.oldValue = oldValue;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}
