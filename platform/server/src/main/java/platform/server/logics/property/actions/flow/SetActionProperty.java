package platform.server.logics.property.actions.flow;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.where.Where;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.*;

public class SetActionProperty<P extends PropertyInterface, I extends PropertyInterface> extends ExtendContextActionProperty<I> {

    private PropertyMapImplement<P, I> writeTo;
    private PropertyInterfaceImplement<I> writeFrom;

    public SetActionProperty(String sID,
                             String caption,
                             Collection<I> innerInterfaces,
                             List<I> mapInterfaces, PropertyMapImplement<P, I> writeTo,
                             PropertyInterfaceImplement<I> writeFrom) {
        super(sID, caption, innerInterfaces, mapInterfaces, toList(writeTo, writeFrom));

        this.writeTo = writeTo;
        this.writeFrom = writeFrom;
    }

    @Override
    public FlowResult flowExecute(ExecutionContext context) throws SQLException {
        Map<I, KeyExpr> allKeys = KeyExpr.getMapKeys(innerInterfaces);

        Map<P, KeyExpr> toKeys = join(writeTo.mapping, allKeys);
        Map<P, DataObject> toValues = innerJoin(writeTo.mapping, crossJoin(mapInterfaces, context.getKeys()));
        Map<P, PropertyObjectInterfaceInstance> toObjects = innerJoin(writeTo.mapping, crossJoin(mapInterfaces, context.getObjectInstances()));

        Map<I, Expr> fromKeys = new HashMap<I, Expr>(allKeys);
        for (Map.Entry<ClassPropertyInterface, I> entry : mapInterfaces.entrySet()) {
            fromKeys.put(entry.getValue(), context.getKeyValue(entry.getKey()).getExpr());
        }

        Expr writeExpr = writeFrom.mapExpr(fromKeys, context.getModifier());
        Where changeWhere = CompareWhere.compareValues(filterKeys(toKeys, toValues.keySet()), toValues);
        changeWhere = changeWhere.and(
                writeExpr.getWhere().or(
                        writeTo.property.getExpr(toKeys, context.getModifier()).getWhere()
                )
        );

        PropertyChange<P> change = new PropertyChange<P>(toKeys, writeExpr, changeWhere);
        context.addActions(
                context.getSession().execute(writeTo.property, change, context.getModifier(), context.getRemoteForm(), toObjects)
        );
        return FlowResult.FINISH;
    }
}
