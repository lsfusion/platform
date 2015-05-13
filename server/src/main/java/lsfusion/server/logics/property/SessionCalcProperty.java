package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.Settings;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.logics.property.infer.Inferred;

public abstract class SessionCalcProperty<T extends PropertyInterface> extends SimpleIncrementProperty<T> {

    public final CalcProperty<T> property;

    public SessionCalcProperty(String caption, CalcProperty<T> property) {
        super(caption, property.getOrderInterfaces());
        this.property = property;
    }

    public abstract OldProperty<T> getOldProperty();

    public abstract ChangedProperty<T> getChangedProperty();

    @Override
    public ImSet<OldProperty> getParseOldDepends() {
        return SetFact.singleton((OldProperty)getOldProperty());
    }

    @Override
    public ImSet<SessionCalcProperty> getSessionCalcDepends(boolean events) {
        return SetFact.<SessionCalcProperty>singleton(this);
    }
}
