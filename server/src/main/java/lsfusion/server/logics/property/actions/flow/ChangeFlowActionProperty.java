package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.derived.DerivedProperty;

public abstract class ChangeFlowActionProperty extends KeepContextActionProperty {

    protected ChangeFlowActionProperty(String caption) {
        super(caption, 0);
    }

    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createTrue();
    }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.EMPTY();
    }

}
