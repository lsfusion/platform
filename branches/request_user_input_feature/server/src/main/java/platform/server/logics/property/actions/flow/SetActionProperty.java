package platform.server.logics.property.actions.flow;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.*;

public class SetActionProperty<P extends PropertyInterface, W extends PropertyInterface, I extends PropertyInterface> extends WriteActionProperty<P, W, I> {

    private boolean notNull;
    private boolean check;

    public SetActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces, PropertyMapImplement<W, I> where, PropertyMapImplement<P, I> writeTo, boolean notNull, boolean check) {
        super(sID, caption, innerInterfaces, mapInterfaces, writeTo, where, new ArrayList<PropertyInterfaceImplement<I>>());

        this.notNull = notNull;

        if(!isWhereFull())
            assert !notNull && check; // потому как все равно and будет
        this.check = check;

        finalizeInit();
    }

    @Override
    protected void write(ExecutionContext context, Map<P, DataObject> toValues, Map<P, KeyExpr> toKeys, Where changeWhere, Map<I, Expr> innerExprs) throws SQLException {
        if(!isWhereFull())
            changeWhere = changeWhere.and(writeTo.property.getExpr(PropertyChange.getMapExprs(toKeys, toValues), context.getModifier()).getWhere());
        writeTo.property.setNotNull(toValues, toKeys, changeWhere, context.getEnv(), notNull, check);
    }

    protected Collection<Property> getWriteProps() {
        return writeTo.property.getSetChangeProps(notNull, false);
    }

    public Set<Property> getUsedProps() {
        Set<Property> result = new HashSet<Property>();
        where.mapFillDepends(result);
        return result;
    }
}
