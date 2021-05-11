package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.filter.NotNullFilterInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class ContextFilterInstance<P extends PropertyInterface> implements FilterEntityInstance {

    private final Property<P> property;

    private final ImMap<P, ? extends ObjectValue> mapValues; // external context
    private final ImRevMap<P, ObjectEntity> mapObjects; // objects
    
    public ContextFilterInstance(Property<P> property, ImMap<P, ? extends ObjectValue> mapValues, ImRevMap<P, ObjectEntity> mapObjects) {
        this.property = property;
        this.mapValues = mapValues;
        this.mapObjects = mapObjects;
    }

    public FilterInstance getInstance(InstanceFactory factory) {
        return new NotNullFilterInstance<>(
                new PropertyObjectInstance<>(property, 
                        MapFact.<P, PropertyObjectInterfaceInstance>addExcl(mapValues, mapObjects.mapValues(entity -> factory.getInstance(entity)))));
    }

    public ImSet<ObjectEntity> getObjects() {
        return mapObjects.valuesSet();
    }

    public Where getWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, Modifier modifier) throws SQLException, SQLHandledException {
        return property.getExpr(ObjectValue.getMapExprs(mapValues).addExcl(mapObjects.join(mapKeys)), modifier).getWhere();
    }
}
