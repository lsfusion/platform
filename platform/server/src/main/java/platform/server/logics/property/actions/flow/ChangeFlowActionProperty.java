package platform.server.logics.property.actions.flow;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.derived.DerivedProperty;

import java.util.HashSet;

public abstract class ChangeFlowActionProperty extends KeepContextActionProperty {

    protected ChangeFlowActionProperty(String sID, String caption) {
        super(sID, caption, 0);
    }

    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createTrue();
    }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.EMPTY();
    }

}
