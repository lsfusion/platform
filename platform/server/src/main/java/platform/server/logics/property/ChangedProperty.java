package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChangedProperty<T extends PropertyInterface> extends SimpleIncrementProperty<T> {

    private final Property<T> property;
    private final IncrementType type;

    public ChangedProperty(Property<T> property, IncrementType type) {
        super("CHANGED_" + type + "_" + property.getSID(), property.caption + " (" + type + ")", (List<T>)property.interfaces);
        this.property = property;
        this.type = type;

        property.getOld();// чтобы зарегить old
    }

    @Override
    protected void fillDepends(Set<Property> depends, boolean derived) {
        depends.add(property);
        depends.add(property.getOld());
    }

    protected Expr calculateExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        WhereBuilder changedIncrementWhere = new WhereBuilder();
        property.getIncrementExpr(joinImplement, changedIncrementWhere, propChanges, type);
        if(changedWhere!=null) changedWhere.add(changedIncrementWhere.toWhere());
        return ValueExpr.get(changedIncrementWhere.toWhere());
    }
}
