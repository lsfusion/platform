package platform.server.logics.property.actions.flow;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.where.Where;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.property.*;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public class ChangeActionProperty<P extends PropertyInterface, I extends PropertyInterface> extends WriteActionProperty<P, I> {

    private PropertyInterfaceImplement<I> writeFrom;

    public ChangeActionProperty(String sID,
                                String caption,
                                Collection<I> innerInterfaces,
                                List<I> mapInterfaces, PropertyMapImplement<P, I> writeTo,
                                PropertyInterfaceImplement<I> writeFrom) {
        super(sID, caption, innerInterfaces, mapInterfaces, writeTo, Collections.singletonList(writeFrom));

        this.writeFrom = writeFrom;

        finalizeInit();
    }

    protected void write(ExecutionContext context, Map<P, KeyExpr> toKeys, Where changeWhere, Map<I, PropertyObjectInterfaceInstance> innerObjects, Map<I, Expr> fromKeys) throws SQLException {
        Map<P, PropertyObjectInterfaceInstance> toObjects = null;
        if(context.isInFormSession())
            toObjects = innerJoin(writeTo.mapping, innerObjects);

        Expr writeExpr = writeFrom.mapExpr(fromKeys, context.getModifier());
        changeWhere = changeWhere.and(
                writeExpr.getWhere().or(
                        writeTo.property.getExpr(toKeys, context.getModifier()).getWhere()
                )
        );

        PropertyChange<P> change = new PropertyChange<P>(toKeys, writeExpr, changeWhere);
        context.addActions(
                context.getSession().execute(writeTo.property, change, context.getModifier(), context.getRemoteForm(), toObjects)
        );
    }

    public Set<Property> getUsedProps() {
        Set<Property> result = new HashSet<Property>();
        writeFrom.mapFillDepends(result);
        return result;
    }

    protected Collection<Property> getWriteProps() {
        return new HashSet<Property>(writeTo.property.getDataChanges());
    }
}
