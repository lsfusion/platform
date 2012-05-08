package platform.server.logics.property.actions.flow;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public class ChangeActionProperty<P extends PropertyInterface, W extends PropertyInterface, I extends PropertyInterface> extends WriteActionProperty<P, W, I> {

    private PropertyInterfaceImplement<I> writeFrom;

    public ChangeActionProperty(String sID,
                                String caption,
                                Collection<I> innerInterfaces,
                                List<I> mapInterfaces, PropertyMapImplement<W, I> where, PropertyMapImplement<P, I> writeTo,
                                PropertyInterfaceImplement<I> writeFrom) {
        super(sID, caption, innerInterfaces, mapInterfaces, writeTo, where, Collections.singletonList(writeFrom));

        this.writeFrom = writeFrom;

        finalizeInit();
    }

    protected void write(ExecutionContext context, Map<P, DataObject> toValues, Map<P, KeyExpr> toKeys, Where changeWhere, Map<I, PropertyObjectInterfaceInstance> innerObjects, Map<I, Expr> innerExprs) throws SQLException {

        Expr writeExpr = writeFrom.mapExpr(innerExprs, context.getModifier());

        if(!isWhereFull())
            changeWhere = changeWhere.and(writeExpr.getWhere().or(
                    writeTo.property.getExpr(PropertyChange.getMapExprs(toKeys, toValues), context.getModifier()).getWhere()));

        PropertyChange<P> change = new PropertyChange<P>(toValues, toKeys, writeExpr, changeWhere);
        context.addActions(context.getEnv().execute(writeTo.property, change, nullInnerJoin(writeTo.mapping, innerObjects)));
    }

    public Set<Property> getUsedProps() {
        Set<Property> result = new HashSet<Property>();
        writeFrom.mapFillDepends(result);
        where.mapFillDepends(result);
        return result;
    }

    protected Collection<Property> getWriteProps() {
        return new HashSet<Property>(writeTo.property.getDataChanges());
    }
}
