package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;

public abstract class SessionCalcProperty<T extends PropertyInterface> extends SimpleIncrementProperty<T> {

    public final CalcProperty<T> property;

    public SessionCalcProperty(String sID, String caption, CalcProperty<T> property) {
        super(sID, caption, property.getOrderInterfaces());
        this.property = property;
    }

    public abstract OldProperty<T> getOldProperty();

    @Override
    public boolean noOld() {
        return false;
    }

    @Override
    public ImSet<SessionCalcProperty> getSessionCalcDepends(boolean events) {
        return SetFact.<SessionCalcProperty>singleton(this);
    }
}
