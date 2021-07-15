package lsfusion.server.logics.form.struct.property;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.Property;
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

    public ContextFilterInstance<P> getRemappedInstance(final ObjectEntity oldObject, final ObjectEntity newObject, InstanceFactory instanceFactory) {
        P oldObjectInterface = mapping.reverse().get(oldObject);
        return new ContextFilterInstance<>(property, 
                            mapping.removeValues(oldObject).mapValues((ObjectEntity object) -> object.getInstance(instanceFactory).getObjectValue()), 
                            oldObjectInterface != null ? MapFact.singletonRev(oldObjectInterface, newObject) : MapFact.EMPTYREV());
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
}
