package lsfusion.server.form.entity;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.GroupObjectInstance;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.Instantiable;
import lsfusion.server.form.instance.OrderInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.session.Modifier;

import java.sql.SQLException;
import java.util.Set;

public interface OrderEntity<T extends OrderInstance> extends Instantiable<T> {
    void fillObjects(Set<ObjectEntity> objects);

    /**
     * Возвращает OrderEntity, которая заменяет все старые ObjectEntities, на их текущие значения, взятые из instanceFactory,
     * кроме oldObject, который заменяется на newObject.
     *
     * По сути фиксирует текущие значения всех ObjectEntities, кроме oldObject.
     */
    OrderEntity<T> getRemappedEntity(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory);

    Type getType();
    
    GroupObjectEntity getApplyObject(ImOrderSet<GroupObjectEntity> groups);

    Expr getExpr(ImMap<ObjectEntity, ? extends Expr> mapExprs, Modifier modifier, ImMap<ObjectEntity, ObjectValue> mapObjects) throws SQLException, SQLHandledException;
}
