package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.Compare;
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
        FunctionProperty.fillDepends(depends,BaseUtils.merge(value.mapping.values(),onChange));
    }

    public Set<Property> getDepends() {
        Set<Property> used = new HashSet<Property>();
        fillDepends(used);
        return used;
    }

    public <U extends Changes<U>> boolean hasDerivedChange(Modifier<U> modifier) {
        return getDerivedUsedChange(modifier).hasChanges();
    }

    public <U extends Changes<U>> U getDerivedUsedChange(Modifier<U> modifier) {
        return modifier.getApplyUsedChanges(getDepends());
    }

    public Expr getDerivedChange(Map<C, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier, WhereBuilder changeWhere) {
        WhereBuilder onChangeWhere = new WhereBuilder();
        Map<D, Expr> implementExprs = new HashMap<D, Expr>();
        for(Map.Entry<D,PropertyInterfaceImplement<C>> interfaceImplement : value.mapping.entrySet()) {
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapExpr(mapKeys, modifier, onChangeWhere));
            interfaceImplement.getValue().mapExpr(mapKeys, modifier.getApplyStart(), onChangeWhere);
        }

        Where andWhere = Where.TRUE; // докинем дополнительные условия
        for(PropertyMapImplement<?,C> propChange : onChange) {
            WhereBuilder onValueChangeWhere = new WhereBuilder();
            Expr newExpr = propChange.mapExpr(mapKeys, modifier, onValueChangeWhere);
            Expr prevExpr = propChange.mapExpr(mapKeys, modifier.getApplyStart(), onValueChangeWhere);

            Where forceWhere;
            if(forceChanged) // при forceChanged проверяем что раньше null был
                forceWhere = prevExpr.getWhere();
            else
                forceWhere = newExpr.compare(prevExpr, Compare.EQUALS);

            onChangeWhere.add(onValueChangeWhere.toWhere().and(forceWhere.not()));
            andWhere = andWhere.and(newExpr.getWhere());
        }

        changeWhere.add(andWhere.and(onChangeWhere.toWhere())); // если не делать нижней проверки могут пойти сложные не нужные getExpr
        return valueChanged && !changeWhere.toWhere().isFalse() ? value.property.getExpr(implementExprs, modifier): value.property.getExpr(implementExprs, modifier.getApplyStart());
    }

    public <U extends Changes<U>> U getUsedChanges(Modifier<U> modifier) {
        return getDerivedUsedChange(modifier).add(property.getUsedDataChanges(modifier));
    }

    public DataChanges getDataChanges(Modifier<? extends Changes> modifier) {
        Map<C,KeyExpr> mapKeys = property.getMapKeys();

        WhereBuilder onChangeWhere = new WhereBuilder();
        Expr expr = getDerivedChange(mapKeys, modifier, onChangeWhere);
        return property.getDataChanges(new PropertyChange<C>(mapKeys, expr, onChangeWhere.toWhere()), null, modifier).changes;
    }
}
