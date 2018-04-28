package lsfusion.server.session;

import lsfusion.base.FunctionSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.PropertyInterface;

public abstract class DataSessionModifier extends SessionModifier {

    public DataSessionModifier(String debugInfo) {
        super(debugInfo);
    }

    protected abstract <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property);
    
    protected abstract ImSet<CalcProperty> getChangedProps();

    public OperationOwner getOpOwner() {
        return getQueryEnv().getOpOwner();
    }

    protected <P extends PropertyInterface> ModifyChange<P> calculateModifyChange(CalcProperty<P> property, PrereadRows<P> preread, FunctionSet<CalcProperty> overrided) {
        PropertyChange<P> propertyChange = getPropertyChange(property);
        if(propertyChange!=null)
            return new ModifyChange<>(propertyChange, false);
        if(!preread.isEmpty())
            return new ModifyChange<>(property.getNoChange(), preread, false);
        return null;
    }

    @Override
    public String out() {
        return super.out() + "\nchanged : " + getChangedProps().mapValues(new GetValue<PropertyChange, CalcProperty>() {
            @Override
            public PropertyChange getMapValue(CalcProperty value) {
                return getPropertyChange(value);
            }
        });
    }

    public ImSet<CalcProperty> calculateProperties() {
        return getChangedProps();
    }
}
