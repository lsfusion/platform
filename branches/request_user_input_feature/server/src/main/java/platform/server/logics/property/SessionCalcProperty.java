package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.data.where.classes.ClassWhere;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SessionCalcProperty<T extends PropertyInterface> extends SimpleIncrementProperty<T> {

    public final CalcProperty<T> property;

    public SessionCalcProperty(String sID, String caption, CalcProperty<T> property) {
        super(sID, caption, (List<T>)property.interfaces);
        this.property = property;
    }

    public abstract OldProperty<T> getOldProperty();

    @Override
    public Set<SessionCalcProperty> getSessionCalcDepends() {
        return Collections.<SessionCalcProperty>singleton(this);
    }

    @Override
    public ClassWhere<Object> getClassValueWhere() {
        return property.getClassValueWhere();
    }

    public Map<T, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        return property.getInterfaceCommonClasses(commonValue);
    }
}
