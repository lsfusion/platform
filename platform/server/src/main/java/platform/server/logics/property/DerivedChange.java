package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.util.*;

public class DerivedChange<D extends PropertyInterface, C extends PropertyInterface> {

    public boolean notDeterministic() {
        return value.property.notDeterministic();
    }

    private final Property<C> property; // что меняем
    private final PropertyImplement<D, PropertyInterfaceImplement<C>> value;
    private final Collection<PropertyMapImplement<?,C>> onChange;
    private final boolean valueChanged;
    private final boolean forceChanged;

    public DerivedChange(Property<C> property, PropertyImplement<D, PropertyInterfaceImplement<C>> value, Collection<PropertyMapImplement<?,C>> onChange, boolean valueChanged, boolean forceChanged) {
        this.property = property;
        this.value = value;
        this.onChange = onChange;
        this.valueChanged = valueChanged;
        this.forceChanged = forceChanged;
    }

    protected void fillDepends(Set<Property> depends) {
        if(valueChanged) depends.add(value.property);
        FunctionProperty.fillDepends(depends, BaseUtils.merge(value.mapping.values(), onChange));
    }

    public Set<Property> getDepends() {
        Set<Property> used = new HashSet<Property>();
        fillDepends(used);
        return used;
    }

    public boolean hasDerivedChange(Modifier modifier, Modifier prevModifier) {
        return hasDerivedChange(modifier.getPropertyChanges()) || hasDerivedChange(prevModifier.getPropertyChanges());
    }

    public boolean hasDerivedChange(PropertyChanges propChanges) {
        StructChanges struct = propChanges.getStruct();
        return struct.hasChanges(getUsedDerivedChange(struct));
    }

    public QuickSet<Property> getUsedDerivedChange(StructChanges changes) {
        return changes.getUsedChanges(getDepends());
    }

    private PropertyChange<C> getDerivedChange(PropertyChanges newChanges, PropertyChanges prevChanges) {

        Map<C,KeyExpr> mapKeys = property.getMapKeys();

        WhereBuilder onChangeWhere = new WhereBuilder();
        Map<D, Expr> implementExprs = new HashMap<D, Expr>();
        for(Map.Entry<D,PropertyInterfaceImplement<C>> interfaceImplement : value.mapping.entrySet())
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapIncrementExpr(mapKeys, newChanges, prevChanges, onChangeWhere, IncrementType.CHANGE));

        Where andWhere = Where.TRUE; // докинем дополнительные условия
        for(PropertyMapImplement<?,C> propChange : onChange)
            andWhere = andWhere.and(propChange.mapIncrementExpr(mapKeys, newChanges, prevChanges, onChangeWhere, forceChanged ? IncrementType.SET : IncrementType.CHANGE).getWhere());

        andWhere = andWhere.and(onChangeWhere.toWhere()); // если не делать нижней проверки могут пойти сложные не нужные getExpr
        return new PropertyChange<C>(mapKeys, value.property.getExpr(implementExprs, valueChanged && !andWhere.isFalse() ? newChanges : prevChanges), andWhere);
    }

    public DataChanges getDataChanges(Modifier modifier) {
        return getDataChanges(modifier, Property.defaultModifier);
    }

    public DataChanges getDataChanges(Modifier modifier, Modifier prevModifier) {
        return getDataChanges(modifier.getPropertyChanges(), prevModifier.getPropertyChanges());
    }

    public boolean hasUsedDataChanges(PropertyChanges propChanges) {
        StructChanges struct = propChanges.getStruct();
        return struct.hasChanges(getUsedDataChanges(struct));
    }

    public QuickSet<Property> getUsedDataChanges(StructChanges changes) {
        return QuickSet.add(property.getUsedDataChanges(changes),getUsedDerivedChange(changes));
    }
    
    public DataChanges getDataChanges(PropertyChanges changes) {
        return getDataChanges(changes, PropertyChanges.EMPTY);
    }

    public DataChanges getDataChanges(PropertyChanges newChanges, PropertyChanges prevChanges) {
        return property.getDataChanges(getDerivedChange(newChanges, prevChanges), newChanges, null).changes;
    }
}
