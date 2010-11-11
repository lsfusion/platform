package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.DataChanges;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.util.*;

public class DerivedChange<D extends PropertyInterface, C extends PropertyInterface> {

    public boolean notDeterministic() {
        return value.property.notDeterministic();
    }

    Property<C> property; // что меняем
    PropertyImplement<PropertyInterfaceImplement<C>,D> value;
    Collection<PropertyMapImplement<?,C>> onChange = new ArrayList<PropertyMapImplement<?, C>>();
    boolean valueChanged = false;

    protected void fillDepends(Set<Property> depends) {
        if(valueChanged) depends.add(value.property);
    }
    
    public DerivedChange(Property<C> property, PropertyImplement<PropertyInterfaceImplement<C>,D> value, Collection<PropertyMapImplement<?,C>> onChange, boolean valueChanged) {
        this.property = property;
        this.value = value;
        this.onChange = onChange;
        this.valueChanged = valueChanged;
    }

    public <U extends Changes<U>> U getUsedChanges(Modifier<U> modifier) {

        Set<Property> used = new HashSet<Property>();
        FunctionProperty.fillDepends(used,BaseUtils.merge(value.mapping.values(),onChange));
        if(valueChanged) used.add(value.property);
        return modifier.getUsedChanges(used).add(property.getUsedDataChanges(modifier));
    }

    public DataChanges getDataChanges(Modifier<? extends Changes> modifier) {
        Map<C,KeyExpr> mapKeys = property.getMapKeys();

        WhereBuilder onChangeWhere = new WhereBuilder();
        Map<D, Expr> implementExprs = new HashMap<D, Expr>();
        for(Map.Entry<D,PropertyInterfaceImplement<C>> interfaceImplement : value.mapping.entrySet())
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapExpr(mapKeys, modifier, onChangeWhere));

        Where andWhere = Where.TRUE; // докинем дополнительные условия
        for(PropertyMapImplement<?,C> propChange : onChange)
            andWhere = andWhere.and(propChange.mapExpr(mapKeys, modifier, onChangeWhere).getWhere());

        Where derivedWhere = andWhere.and(onChangeWhere.toWhere()); // если не делать нижней проверки могут пойти сложные не нужные getExpr
        return property.getDataChanges(new PropertyChange<C>(mapKeys, valueChanged && !derivedWhere.isFalse() ? value.property.getExpr(implementExprs, modifier, null): value.property.getExpr(implementExprs), derivedWhere), null, modifier).changes;
    }
}
