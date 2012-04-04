package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.where.Where;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public abstract class WriteActionProperty<P extends PropertyInterface, I extends PropertyInterface> extends ExtendContextActionProperty<I> {

    protected final PropertyMapImplement<P, I> writeTo;
    
    public WriteActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces, PropertyMapImplement<P,I> writeTo, List<PropertyInterfaceImplement<I>> used) {
        super(sID, caption, innerInterfaces, mapInterfaces, BaseUtils.addList(writeTo, used));

        this.writeTo = writeTo;
    }

    @Override
    public FlowResult flowExecute(ExecutionContext context) throws SQLException {
        Map<I, KeyExpr> allKeys = KeyExpr.getMapKeys(innerInterfaces);
        Map<I, DataObject> innerValues = crossJoin(mapInterfaces, context.getKeys());
        Map<I, PropertyObjectInterfaceInstance> innerObjects = null;
        if(context.isInFormSession())
            innerObjects = rightCrossJoin(mapInterfaces, context.getObjectInstances());

        Map<P, KeyExpr> toKeys = join(writeTo.mapping, allKeys);
        Map<P, DataObject> toValues = rightJoin(writeTo.mapping, innerValues);
        Where changeWhere = CompareWhere.compareValues(filterKeys(toKeys, toValues.keySet()), toValues);

        Map<I, Expr> fromKeys = BaseUtils.override(allKeys, DataObject.getMapExprs(innerValues)); // чисто оптимизационная вещь
        write(context, toKeys, changeWhere, innerObjects, fromKeys);

        return FlowResult.FINISH;
    }

    protected abstract void write(ExecutionContext context, Map<P, KeyExpr> allKeys, Where changeWhere, Map<I, PropertyObjectInterfaceInstance> innerObjects, Map<I, Expr> fromKeys) throws SQLException;

    protected abstract Collection<Property> getWriteProps();

    public Set<Property> getChangeProps() {
        Set<Property> result = new HashSet<Property>();
        for(Property<?> property : getWriteProps()) {
            assert !(property instanceof ActionProperty); // предполагается что exec'ом должен вызываться
            if(property instanceof ObjectClassProperty)
                result.addAll(((ObjectClassProperty) property).getChangeProps());
            else
                result.add(property);
        }
        return result;
    }
}
