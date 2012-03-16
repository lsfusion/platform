package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.innerjoins.KeyEqual;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.util.*;

public class DerivedChange<D extends PropertyInterface, C extends PropertyInterface> {

    private final Property<C> property; // что меняем
    private final PropertyImplement<D, PropertyInterfaceImplement<C>> value;
    private final Collection<PropertyMapImplement<?,C>> where;
    private final Collection<PropertyMapImplement<?,C>> onChange;
    private final boolean valueChanged;
    private final boolean forceChanged;

    public DerivedChange(Property<C> property, PropertyImplement<D, PropertyInterfaceImplement<C>> value, Collection<PropertyMapImplement<?,C>> where, Collection<PropertyMapImplement<?,C>> onChange, boolean valueChanged, boolean forceChanged) {
        this.property = property;
        this.value = value;
        this.where = where;
        this.onChange = onChange;
        this.valueChanged = valueChanged;
        this.forceChanged = forceChanged;
    }

    private Set<Property> getDepends(boolean newDepends) {
        Set<Property> used = new HashSet<Property>();
        if(valueChanged==newDepends) {
            used.add(value.property);
            FunctionProperty.fillDepends(used,where);
        }
        FunctionProperty.fillDepends(used, BaseUtils.merge(value.mapping.values(), onChange));
        return used;
    }

    public Set<Property> getNewDepends() {
        return getDepends(true);
    }

    public Set<Property> getPrevDepends() {
        return getDepends(false);
    }

    public boolean hasUsedDataChanges(PropertyChanges propChanges, PropertyChanges prevChanges) {
        return hasUsedDataChanges(propChanges, true) || hasUsedDataChanges(prevChanges, false);
    }

    private PropertyChange<C> getDerivedChange(PropertyChanges newChanges, PropertyChanges prevChanges) {

        Map<C,KeyExpr> mapKeys = property.getMapKeys();

        WhereBuilder onChangeWhere = new WhereBuilder();
        Map<D, Expr> implementExprs = new HashMap<D, Expr>();
        for(Map.Entry<D,PropertyInterfaceImplement<C>> interfaceImplement : value.mapping.entrySet())
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapIncrementExpr(mapKeys, newChanges, prevChanges, onChangeWhere, IncrementType.CHANGE));

        Where andWhere = Where.TRUE; // докинем дополнительные условия
        for(PropertyMapImplement<?,C> propChange : where) // наплевать на изменения
            andWhere = andWhere.and(propChange.mapExpr(mapKeys, valueChanged ? newChanges : prevChanges).getWhere());
        for(PropertyMapImplement<?,C> propChange : onChange)
            andWhere = andWhere.and(propChange.mapIncrementExpr(mapKeys, newChanges, prevChanges, onChangeWhere, forceChanged ? IncrementType.CHANGESET : IncrementType.CHANGE).getWhere());

        andWhere = andWhere.and(onChangeWhere.toWhere());

        return new PropertyChange<C>(mapKeys, getDerivedChangeExpr(implementExprs, newChanges, prevChanges, andWhere), andWhere);
    }
    
    private Expr getDerivedChangeExpr(Map<D, Expr> implementExprs, PropertyChanges newChanges, PropertyChanges prevChanges, Where andWhere) {
        KeyEquals keyEquals = andWhere.getKeyEquals(); // оптимизация
        if(keyEquals.size==0)
            return Expr.NULL;
        KeyEqual keyEqual;
        if(keyEquals.size == 1 && !(keyEqual=keyEquals.getKey(0)).isEmpty())
            implementExprs = keyEqual.getTranslator().translate(implementExprs);
        return value.property.getExpr(implementExprs, valueChanged ? newChanges : prevChanges);
    }

    public DataChanges getDataChanges(Modifier modifier) {
        return getDataChanges(modifier, Property.defaultModifier);
    }

    public DataChanges getDataChanges(Modifier modifier, Modifier prevModifier) {
        return getDataChanges(modifier.getPropertyChanges(), prevModifier.getPropertyChanges());
    }

    public boolean hasUsedDataChanges(PropertyChanges propChanges, boolean newDepends) {
        StructChanges struct = propChanges.getStruct();
        return struct.hasChanges(getUsedDataChanges(struct, newDepends));
    }

    public QuickSet<Property> getUsedDerivedChange(StructChanges changes, boolean newDepends) {
        return changes.getUsedChanges(newDepends?getNewDepends():getPrevDepends());
    }

    public QuickSet<Property> getUsedDataChanges(StructChanges changes, boolean newModifier) {
        return QuickSet.add(property.getUsedDataChanges(changes),getUsedDerivedChange(changes, newModifier));
    }
    
    public DataChanges getDataChanges(PropertyChanges changes) {
        return getDataChanges(changes, PropertyChanges.EMPTY);
    }

    public DataChanges getDataChanges(PropertyChanges newChanges, PropertyChanges prevChanges) {
        return property.getDataChanges(getDerivedChange(newChanges, prevChanges), newChanges, null).changes;
    }
}
