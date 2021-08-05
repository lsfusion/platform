package lsfusion.server.logics.action.session.change.modifier;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.logics.action.data.PrereadRows;
import lsfusion.server.logics.action.session.change.ModifyChange;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.function.Function;

public abstract class DataSessionModifier extends SessionModifier {

    public DataSessionModifier(String debugInfo) {
        super(debugInfo);
    }

    protected abstract <P extends PropertyInterface> PropertyChange<P> getPropertyChange(Property<P> property);
    
    protected abstract ImSet<Property> getChangedProps();

    public OperationOwner getOpOwner() {
        return getQueryEnv().getOpOwner();
    }

    protected <P extends PropertyInterface> ModifyChange<P> calculateModifyChange(Property<P> property, PrereadRows<P> preread, FunctionSet<Property> overrided) {
        PropertyChange<P> propertyChange = getPropertyChange(property);
        if(propertyChange == null && !preread.isEmpty())
            propertyChange = property.getNoChange();

        if(propertyChange!=null)
            return new ModifyChange<>(propertyChange, preread, false);
        return null;
    }

    @Override
    public String out() {
        return super.out() + "\nchanged : " + getChangedProps().mapValues((Function<Property, PropertyChange>) this::getPropertyChange);
    }

    public ImSet<Property> calculateProperties() {
        return getChangedProps();
    }
}
