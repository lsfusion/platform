package lsfusion.server.logics.form.struct.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetExValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class PropertyObjectEntity<P extends PropertyInterface> extends ActionOrPropertyObjectEntity<P, Property<P>> implements OrderEntity<PropertyObjectInstance<P>> {

    public PropertyObjectEntity() {
        //нужен для десериализации
    }

    public PropertyObjectEntity(Property<P> property, ImRevMap<P, ObjectEntity> mapping) {
        super(property, mapping, null, null);
    }

    public PropertyObjectEntity(Property<P> property, ImRevMap<P, ObjectEntity> mapping, String creationScript, String creationPath) {
        super(property, mapping, creationScript, creationPath);
    }

    @Override
    public PropertyObjectInstance<P> getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public PropertyObjectInstance<P> getRemappedInstance(final ObjectEntity oldObject, final ObjectInstance newObject, final InstanceFactory instanceFactory) {
        ImMap<P, PropertyObjectInterfaceInstance> nmapping = mapping.mapValues(new GetValue<PropertyObjectInterfaceInstance, ObjectEntity>() {
            public PropertyObjectInterfaceInstance getMapValue(ObjectEntity value) {
                return value.getRemappedInstance(oldObject, newObject, instanceFactory);
            }});
        return new PropertyObjectInstance<>(property, nmapping);
    }

    @Override
    public Type getType() {
        return property.getType();
    }

    public GroupObjectEntity getApplyObject(FormEntity formEntity, ImSet<GroupObjectEntity> excludeGroupObjects) {
        return formEntity.getApplyObject(getObjectInstances(), excludeGroupObjects);
    }

    public Expr getExpr(final ImMap<ObjectEntity, ? extends Expr> mapExprs, final Modifier modifier) throws SQLException, SQLHandledException {
        return property.getExpr(mapping.join(mapExprs), modifier);
    }
    public Expr getEntityExpr(final ImMap<ObjectEntity, ? extends Expr> mapExprs, final Modifier modifier) throws SQLException, SQLHandledException {
        return getExpr(mapExprs, modifier); 
    }

    public Object read(ExecutionEnvironment env, final ImMap<ObjectEntity, ? extends ObjectValue> mapObjects) throws SQLException, SQLHandledException {
        ImMap<P, ObjectValue> joinImplement = mapping.mapValuesEx(new GetExValue<ObjectValue, ObjectEntity, SQLException, SQLHandledException>() {
            public ObjectValue getMapValue(ObjectEntity value) throws SQLException, SQLHandledException {
                return value.getObjectValue(mapObjects);
            }
        });
        return property.read(env, joinImplement);
    }
}
