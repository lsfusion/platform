package lsfusion.server.form.entity;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetExValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.CalcPropertyObjectInstance;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.ExecutionEnvironment;
import lsfusion.server.session.Modifier;

import java.sql.SQLException;

public class CalcPropertyObjectEntity<P extends PropertyInterface> extends PropertyObjectEntity<P, CalcProperty<P>> implements OrderEntity<CalcPropertyObjectInstance<P>> {

    public CalcPropertyObjectEntity() {
        //нужен для десериализации
    }

    public CalcPropertyObjectEntity(CalcProperty<P> property, ImRevMap<P, ObjectEntity> mapping) {
        super(property, mapping, null, null);
    }

    public CalcPropertyObjectEntity(CalcProperty<P> property, ImRevMap<P, ObjectEntity> mapping, String creationScript, String creationPath) {
        super(property, mapping, creationScript, creationPath);
    }

    @Override
    public CalcPropertyObjectInstance<P> getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public CalcPropertyObjectInstance<P> getRemappedInstance(final ObjectEntity oldObject, final ObjectInstance newObject, final InstanceFactory instanceFactory) {
        ImMap<P, PropertyObjectInterfaceInstance> nmapping = mapping.mapValues(new GetValue<PropertyObjectInterfaceInstance, ObjectEntity>() {
            public PropertyObjectInterfaceInstance getMapValue(ObjectEntity value) {
                return value.getRemappedInstance(oldObject, newObject, instanceFactory);
            }});
        return new CalcPropertyObjectInstance<>(property, nmapping);
    }

    @Override
    public CalcPropertyObjectEntity<?> getDrawProperty() {
        return this;
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
