package lsfusion.server.logics.action.flow;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.derived.DerivedProperty;

public abstract class ChangeFlowAction extends KeepContextAction {

    protected ChangeFlowAction(LocalizedString caption) {
        super(caption, 0);
    }

    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return DerivedProperty.createTrue();
    }

    public ImSet<Action> getDependActions() {
        return SetFact.EMPTY();
    }

}
