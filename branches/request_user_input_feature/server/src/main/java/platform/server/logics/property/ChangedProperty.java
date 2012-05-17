package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;

import java.util.*;

public class ChangedProperty<T extends PropertyInterface> extends SimpleIncrementProperty<T> {

    private final CalcProperty<T> property;
    private final IncrementType type;

    public ChangedProperty(CalcProperty<T> property, IncrementType type) {
        super("CHANGED_" + type + "_" + property.getSID(), property.caption + " (" + type + ")", (List<T>)property.interfaces);
        this.property = property;
        this.type = type;

        property.getOld();// чтобы зарегить old
    }

    @Override
    protected void fillDepends(Set<CalcProperty> depends, boolean events) {
        depends.add(property);
        depends.add(property.getOld());
    }

    protected Expr calculateExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        WhereBuilder changedIncrementWhere = new WhereBuilder();
        property.getIncrementExpr(joinImplement, changedIncrementWhere, propClasses, propChanges, type);
        if(changedWhere!=null) changedWhere.add(changedIncrementWhere.toWhere());
        return ValueExpr.get(changedIncrementWhere.toWhere());
    }

    @Override
    public Set<ChangedProperty> getChangedDepends() {
        return Collections.<ChangedProperty>singleton(this);
    }

    // для resolve'а следствий в частности
    protected PropertyChange<T> getFullChange(Modifier modifier) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        Expr expr = property.getExpr(mapKeys, modifier);
        Where where;
        switch(type) {
            case SET:
                where = expr.getWhere();
                break;
            case DROP:
                where = expr.getWhere().not();
                break;
            default:
                throw new RuntimeException();
        }
        return new PropertyChange<T>(mapKeys, ValueExpr.get(where), Where.TRUE);
    }

    public Map<T, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        return property.getInterfaceCommonClasses(commonValue);
    }
}
