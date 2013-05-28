package platform.server.logics.property.actions;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.PropertyInterface;

public abstract class BaseActionProperty<P extends PropertyInterface> extends ActionProperty<P> {

    protected BaseActionProperty(String sID, String caption, ImOrderSet<P> interfaces) {
        super(sID, caption, interfaces);
    }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.EMPTY();
    }
}
