package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public abstract class WriteActionProperty<P extends PropertyInterface, W extends PropertyInterface, I extends PropertyInterface> extends ExtendContextActionProperty<I> {

    protected final PropertyMapImplement<P, I> writeTo;
    protected final PropertyMapImplement<W, I> where;
    
    public WriteActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces, PropertyMapImplement<P, I> writeTo, PropertyMapImplement<W, I> where, List<PropertyInterfaceImplement<I>> used) {
        super(sID, caption, innerInterfaces, mapInterfaces, addList(writeTo, addList(where, used)));

        this.writeTo = writeTo;
        this.where = where;
    }

    @Override
    public FlowResult flowExecute(ExecutionContext context) throws SQLException {
        Map<I, KeyExpr> allKeys = KeyExpr.getMapKeys(innerInterfaces);
        Map<I, DataObject> innerValues = crossJoin(mapInterfaces, context.getKeys());

        Map<P, KeyExpr> toKeys = new HashMap<P, KeyExpr>(); Map<P, DataObject> toValues = new HashMap<P, DataObject>();
        for(Map.Entry<P, I> map : writeTo.mapping.entrySet()) {
            DataObject innerValue = innerValues.get(map.getValue());
            if(innerValue!=null)
                toValues.put(map.getKey(), innerValue);
            else
                toKeys.put(map.getKey(), allKeys.get(map.getValue()));
        }

        Map<I, Expr> innerExprs = BaseUtils.override(allKeys, DataObject.getMapExprs(innerValues)); // чисто оптимизационная вещь
        write(context, toValues, toKeys, where.mapExpr(innerExprs, context.getModifier()).getWhere(),
                rightNullCrossJoin(mapInterfaces, context.getObjectInstances()), innerExprs); // для extend'утого контекста проверяем все ли ключи

        return FlowResult.FINISH;
    }

    @IdentityLazy
    protected boolean isWhereFull() {
        return where.mapIsFull(BaseUtils.filterNot(innerInterfaces, mapInterfaces.values()));
    }

    protected abstract void write(ExecutionContext context, Map<P, DataObject> toValues, Map<P, KeyExpr> toKeys, Where changeWhere, Map<I, PropertyObjectInterfaceInstance> innerObjects, Map<I, Expr> innerExprs) throws SQLException;

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
