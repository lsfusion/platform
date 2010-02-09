package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.session.*;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;

import java.util.*;

public class DerivedChange<D extends PropertyInterface, C extends PropertyInterface> {

    Property<C> property;
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

        // запишем в DataProperty
        for(DataProperty dataProperty : property.getDataChanges())
            dataProperty.derivedChange = this;
    }

    public <U extends Changes<U>> U getUsedChanges(Modifier<U> modifier) {

        U result = modifier.newChanges();
        Set<Property> used = new HashSet<Property>();
        FunctionProperty.fillDepends(used,BaseUtils.merge(value.mapping.values(),onChange));
        if(valueChanged) used.add(value.property);
        result.add(Property.getUsedChanges(used, modifier));
        result.add(property.getUsedDataChanges(modifier));
        return result;
    }

    public DataChanges getDataChanges(TableModifier<? extends TableChanges> modifier) {
        Map<C,KeyExpr> mapKeys = property.getMapKeys();

        WhereBuilder onChangeWhere = new WhereBuilder();
        Map<D, Expr> implementExprs = new HashMap<D, Expr>();
        for(Map.Entry<D,PropertyInterfaceImplement<C>> interfaceImplement : value.mapping.entrySet())
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapExpr(mapKeys, modifier, onChangeWhere));

        Where andWhere = Where.TRUE; // докинем дополнительные условия
        for(PropertyMapImplement<?,C> propChange : onChange)
            andWhere = andWhere.and(propChange.mapExpr(mapKeys, modifier, onChangeWhere).getWhere());

        return property.getDataChanges(new PropertyChange<C>(mapKeys, valueChanged ? value.property.getExpr(implementExprs, modifier, null): value.property.getExpr(implementExprs),
                andWhere.and(onChangeWhere.toWhere())), null, modifier);
    }
}
