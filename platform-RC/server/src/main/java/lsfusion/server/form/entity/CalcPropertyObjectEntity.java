package lsfusion.server.form.entity;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetExValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.CalcPropertyObjectInstance;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.ExecutionEnvironment;
import lsfusion.server.session.Modifier;

import java.sql.SQLException;

public class CalcPropertyObjectEntity<P extends PropertyInterface> extends PropertyObjectEntity<P, CalcProperty<P>> implements OrderEntity<CalcPropertyObjectInstance<P>> {

    public CalcPropertyObjectEntity() {
        //нужен для десериализации
    }

    public CalcPropertyObjectEntity(CalcProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceEntity> mapping) {
        super(property, (ImMap<P,PropertyObjectInterfaceEntity>) mapping, null, null);
    }

    public CalcPropertyObjectEntity(CalcProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceEntity> mapping, String creationScript, String creationPath) {
        super(property, (ImMap<P,PropertyObjectInterfaceEntity>) mapping, creationScript, creationPath);
    }

    @Override
    public CalcPropertyObjectInstance<P> getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public CalcPropertyObjectEntity<P> getRemappedEntity(final ObjectEntity oldObject, final ObjectEntity newObject, final InstanceFactory instanceFactory) {
        ImMap<P, PropertyObjectInterfaceEntity> nmapping = mapping.mapValues(new GetValue<PropertyObjectInterfaceEntity, PropertyObjectInterfaceEntity>() {
            public PropertyObjectInterfaceEntity getMapValue(PropertyObjectInterfaceEntity value) {
                return value.getRemappedEntity(oldObject, newObject, instanceFactory);
            }});
        return new CalcPropertyObjectEntity<>(property, nmapping, creationScript, creationPath);
    }

    @Override
    public CalcPropertyObjectEntity<?> getDrawProperty() {
        return this;
    }

    @Override
    public Type getType() {
        return property.getType();
    }

    @Override
    public Expr getExpr(final ImMap<ObjectEntity, ? extends Expr> mapExprs, final Modifier modifier, final ImMap<ObjectEntity, ObjectValue> mapObjects) throws SQLException, SQLHandledException {
        ImMap<P, Expr> joinImplement = mapping.mapValuesEx(new GetExValue<Expr, PropertyObjectInterfaceEntity, SQLException, SQLHandledException>() {
            public Expr getMapValue(PropertyObjectInterfaceEntity value) throws SQLException, SQLHandledException {
                return value.getExpr(mapExprs, modifier, mapObjects);
            }
        });
        return property.getExpr(joinImplement, modifier);
    }

    public Object read(ExecutionEnvironment env, final ImMap<ObjectEntity, ObjectValue> mapObjects) throws SQLException, SQLHandledException {
        ImMap<P, ObjectValue> joinImplement = mapping.mapValuesEx(new GetExValue<ObjectValue, PropertyObjectInterfaceEntity, SQLException, SQLHandledException>() {
            public ObjectValue getMapValue(PropertyObjectInterfaceEntity value) throws SQLException, SQLHandledException {
                return value.getObjectValue(mapObjects);
            }
        });
        return property.read(env, joinImplement);
    }
}
