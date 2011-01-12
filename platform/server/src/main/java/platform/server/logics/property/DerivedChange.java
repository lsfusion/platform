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

    private final Property<C> property; // что меняем
    private final PropertyImplement<PropertyInterfaceImplement<C>,D> value;
    private final Collection<PropertyMapImplement<?,C>> onChange;
    private final boolean valueChanged;
    private final boolean forceChanged;

    protected void fillDepends(Set<Property> depends) {
        if(valueChanged) depends.add(value.property);
        FunctionProperty.fillDepends(depends,BaseUtils.merge(value.mapping.values(),onChange));
    }
    
    public DerivedChange(Property<C> property, PropertyImplement<PropertyInterfaceImplement<C>,D> value, Collection<PropertyMapImplement<?,C>> onChange, boolean valueChanged, boolean forceChanged) {
        this.property = property;
        this.value = value;
        this.onChange = onChange;
        this.valueChanged = valueChanged;
        this.forceChanged = forceChanged;
    }

    public <U extends Changes<U>> U getUsedChanges(Modifier<U> modifier) {

        Set<Property> used = new HashSet<Property>();
        fillDepends(used);
        return modifier.getUsedChanges(used).add(property.getUsedDataChanges(modifier));
    }

    public DataChanges getDataChanges(Modifier<? extends Changes> modifier) {
        Map<C,KeyExpr> mapKeys = property.getMapKeys();

        WhereBuilder onChangeWhere = new WhereBuilder();
        Map<D, Expr> implementExprs = new HashMap<D, Expr>();
        for(Map.Entry<D,PropertyInterfaceImplement<C>> interfaceImplement : value.mapping.entrySet())
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapExpr(mapKeys, modifier, onChangeWhere));

        Where andWhere = Where.TRUE; // докинем дополнительные условия
        for(PropertyMapImplement<?,C> propChange : onChange) {
            if(forceChanged) { // при forceChanged проверяем что раньше null был
                andWhere = andWhere.and(propChange.mapExpr(mapKeys, modifier).getWhere());
                onChangeWhere.add(propChange.mapExpr(mapKeys).getWhere().not());
            } else
                andWhere = andWhere.and(propChange.mapExpr(mapKeys, modifier, onChangeWhere).getWhere());
        }

        Where derivedWhere = andWhere.and(onChangeWhere.toWhere()); // если не делать нижней проверки могут пойти сложные не нужные getExpr
        return property.getDataChanges(new PropertyChange<C>(mapKeys, valueChanged && !derivedWhere.isFalse() ? value.property.getExpr(implementExprs, modifier): value.property.getExpr(implementExprs), derivedWhere), null, modifier).changes;
    }
}
