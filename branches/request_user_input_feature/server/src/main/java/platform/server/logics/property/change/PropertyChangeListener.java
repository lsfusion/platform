package platform.server.logics.property.change;

import platform.base.QuickSet;
import platform.interop.Compare;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.MapDataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static platform.base.BaseUtils.*;

public abstract class PropertyChangeListener<P extends PropertyInterface> {
    protected final Property<P> property;

    //assert, что здесь мэппинг на интерфейсы property + valueInterface
    protected final PropertyImplement<ClassPropertyInterface, PropertyInterface> listenerImplement;

    protected final PropertyInterface valueInterface;

    public PropertyChangeListener(Property<P> property, PropertyImplement<ClassPropertyInterface, P> listenerImplement) {
        this(property, null, (PropertyImplement<ClassPropertyInterface, PropertyInterface>) listenerImplement);
    }

    public PropertyChangeListener(Property<P> property, PropertyInterface valueInterface, PropertyImplement<ClassPropertyInterface, PropertyInterface> listenerImplement) {
        this.property = property;
        this.valueInterface = valueInterface;
        this.listenerImplement = listenerImplement;
    }

    public QuickSet<Property> getUsedDataChanges(StructChanges propChanges) {
        return new QuickSet<Property>(listenerImplement.property.getUsedDataChanges(propChanges));
    }

    public MapDataChanges<P> getDataChanges(PropertyChange<P> change, PropertyChanges propChanges, Where changedWhere) {
        if (valueInterface == null) {
            return getValueIndependentChanges(change, propChanges, changedWhere);
        }

        KeyExpr valueKey = new KeyExpr(valueInterface.toString());
        Map<PropertyInterface, KeyExpr> allMapKeys = merge(change.getMapKeys(), singletonMap(valueInterface, valueKey));
        Where listenerChangedWhere = change.where.or(changedWhere).and(valueKey.compare(change.expr, Compare.EQUALS));

        Map<ClassPropertyInterface, KeyExpr> listenerChangeKeys = rightJoin(listenerImplement.mapping, allMapKeys);
        Map<ClassPropertyInterface, DataObject> listenerChangeValues = rightJoin(listenerImplement.mapping, new HashMap<PropertyInterface, DataObject>(change.getMapValues()));

        Expr listenerChangeExpr = getValueExpr();

        MapDataChanges<ClassPropertyInterface> listenerChanges =
                listenerImplement.property.getDataChanges(new PropertyChange(listenerChangeValues, listenerChangeKeys, listenerChangeExpr, listenerChangedWhere), propChanges, null);

        return listenerChanges.map(filterValues(listenerImplement.mapping, property.interfaces));
    }

    private MapDataChanges<P> getValueIndependentChanges(PropertyChange<P> change, PropertyChanges propChanges, Where changedWhere) {
        Map<ClassPropertyInterface, P> propertyMapping = (Map<ClassPropertyInterface, P>) listenerImplement.mapping;
        return listenerImplement.property.getDataChanges(
                new PropertyChange<P>(change, getValueExpr(), changedWhere).map(propertyMapping),
                propChanges
        ).map(propertyMapping);
    }

    protected abstract Expr getValueExpr();
}
