package lsfusion.server.logics.form.struct.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.action.input.InputOrderEntity;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class PropertyObjectEntity<P extends PropertyInterface> extends ActionOrPropertyObjectEntity<P, Property<P>> implements OrderEntity<PropertyObjectInstance<P>> {

    public PropertyObjectEntity() {
        //нужен для десериализации
    }

    public PropertyObjectEntity(Property<P> property, ImRevMap<P, ObjectEntity> mapping) {
        super(property, mapping, null, null, null);
    }

    public PropertyObjectEntity(Property<P> property, ImRevMap<P, ObjectEntity> mapping, String creationScript, String creationPath, String path) {
        super(property, mapping, creationScript, creationPath, path);
    }

    @Override
    public PropertyObjectInstance<P> getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
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
        ImMap<P, ObjectValue> joinImplement = mapping.mapValuesEx((ThrowingFunction<ObjectEntity, ObjectValue, SQLException, SQLHandledException>) value -> value.getObjectValue(mapObjects));
        return property.read(env, joinImplement);
    }

    @Override
    public <X extends PropertyInterface> PropertyObjectEntity<?> getDrawProperty(PropertyObjectEntity<X> readOnly) {
        return this;
    }

    public <T extends PropertyInterface> PropertyMapImplement<?, T> getImplement(ImRevMap<ObjectEntity, T> mapObjects) {
        return new PropertyMapImplement<>(property, mapping.join(mapObjects));
    }

    public boolean isValueUnique(GroupObjectEntity grid) {
        // remapping all objects except ones in the grid
        return property.isValueFullAndUnique(mapping.filterFnValuesRev(value -> grid != null && !grid.getObjects().contains(value)).mapRevValues(ObjectEntity::getParamExpr), false);
    }

    @Override
    public ImSet<ObjectEntity> getObjects() {
        return mapping.valuesSet();
    }

    @Override
    public <T extends PropertyInterface> InputOrderEntity<?, T> getInputOrderEntity(ObjectEntity object, ImRevMap<ObjectEntity, T> mapObjects) {
        assert mapping.containsValue(object);
        assert !mapObjects.containsKey(object);
        ImRevMap<P, T> mapOrderObjects = mapping.innerJoin(mapObjects);
        // just like in ContextFilterEntity.getInputFilterEntity we will ignore the cases when there are not all objects
        if(mapOrderObjects.size() != mapping.size() - 1)
            return null;

        return new InputOrderEntity<P, T>(property, mapOrderObjects);
    }

    public boolean equalsMap(PropertyObjectEntity<?> mapProp) {
        return property.equals(mapProp.property) && mapping.equals(mapProp.mapping);
    }
}
