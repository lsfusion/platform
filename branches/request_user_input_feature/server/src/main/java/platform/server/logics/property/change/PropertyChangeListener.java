package platform.server.logics.property.change;

import platform.base.QuickSet;
import platform.interop.Compare;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.session.*;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static platform.base.BaseUtils.*;

public abstract class PropertyChangeListener<P extends PropertyInterface> {
    protected final CalcProperty<P> property;

    //assert, что здесь мэппинг на интерфейсы property + valueInterface
    protected final CalcPropertyMapImplement<ClassPropertyInterface, PropertyInterface> listenerImplement;

    protected final PropertyInterface valueInterface;

    public PropertyChangeListener(CalcProperty<P> property, CalcPropertyImplement<ClassPropertyInterface, P> listenerImplement) {
        this(property, null, (CalcPropertyMapImplement<ClassPropertyInterface, PropertyInterface>) listenerImplement);
    }

    public PropertyChangeListener(CalcProperty<P> property, PropertyInterface valueInterface, CalcPropertyMapImplement<ClassPropertyInterface, PropertyInterface> listenerImplement) {
        this.property = property;
        this.valueInterface = valueInterface;
        this.listenerImplement = listenerImplement;
    }

    public QuickSet<CalcProperty> getUsedDataChanges(StructChanges propChanges) {
        return new QuickSet<CalcProperty>(listenerImplement.property.getUsedDataChanges(propChanges));
    }

    public DataChanges getDataChanges(PropertyChange<P> change, PropertyChanges propChanges, Where changedWhere) {
        if (valueInterface == null) {
            return getValueIndependentChanges(change, propChanges, changedWhere);
        }

        KeyExpr valueKey = new KeyExpr(valueInterface.toString());
        Map<PropertyInterface, KeyExpr> allMapKeys = merge(change.getMapKeys(), singletonMap(valueInterface, valueKey));
        Where listenerChangedWhere = change.where.or(changedWhere).and(valueKey.compare(change.expr, Compare.EQUALS));

        Map<ClassPropertyInterface, KeyExpr> listenerChangeKeys = rightJoin(listenerImplement.mapping, allMapKeys);
        Map<ClassPropertyInterface, DataObject> listenerChangeValues = rightJoin(listenerImplement.mapping, new HashMap<PropertyInterface, DataObject>(change.getMapValues()));

        Expr listenerChangeExpr = getValueExpr();

        DataChanges listenerChanges = ((CalcProperty<ClassPropertyInterface>)listenerImplement.property).
                getDataChanges(new PropertyChange(listenerChangeValues, listenerChangeKeys, listenerChangeExpr, listenerChangedWhere), propChanges);

        return listenerChanges;
    }

    private DataChanges getValueIndependentChanges(PropertyChange<P> change, PropertyChanges propChanges, Where changedWhere) {
        Map<ClassPropertyInterface, P> propertyMapping = (Map<ClassPropertyInterface, P>) listenerImplement.mapping;
        return ((CalcPropertyMapImplement<ClassPropertyInterface, P>)listenerImplement).mapDataChanges(
                new PropertyChange<P>(change, getValueExpr(), changedWhere), null, 
                propChanges
        );
    }

    protected abstract Expr getValueExpr();
}
