package lsfusion.server.logics.property.actions;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.PropertyInterface;

public abstract class BaseActionProperty<P extends PropertyInterface> extends ActionProperty<P> {

    protected BaseActionProperty(String sID, String caption, ImOrderSet<P> interfaces) {
        super(sID, caption, interfaces);
    }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.EMPTY();
    }
}
