package lsfusion.server.logics.form.struct.property;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
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
import lsfusion.server.logics.form.interactive.action.input.InputValueList;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;

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
    public <X extends PropertyInterface> PropertyObjectEntity<?> getProperty(PropertyObjectEntity<X> readOnly, boolean isNative) {
        return this;
    }

    public <T extends PropertyInterface> PropertyMapImplement<?, T> getImplement(ImRevMap<ObjectEntity, T> mapObjects) {
        return new PropertyMapImplement<>(property, mapping.join(mapObjects));
    }

    public static class Select {

        public final PropertyObjectEntity<?> property;

        public final int length;
        public final int count;
        public final boolean actual;

        public enum Type {
            MULTI,
            NOTNULL,
            NULL
        }

        public final Type type;
        public final boolean html;

        public Select(PropertyObjectEntity<?> property, int length, int count, boolean actual, Type type, boolean html) {
            this.property = property;
            this.length = length;
            this.count = count;
            this.actual = actual;
            this.type = type;
            this.html = html;
        }
    }
    public Select getSelectProperty(FormInstanceContext context, boolean forceSelect, Boolean forceFilterSelected) { // false - filter selected,
        Property.Select<P> select = property.getSelectProperty(ListFact.EMPTY(), forceSelect);
        if(select != null) {
            Pair<Integer, Integer> stats = select.stat;
            boolean actualStats = false;
            if(select.values != null && context.dbManager != null) {
                stats = getActualSelectStats(context, select);
                actualStats = true;
            }
            PropertyMapImplement<?, P> selectProperty = select.property.get(forceFilterSelected != null ? forceFilterSelected : stats.second > Settings.get().getMaxInterfaceStatForValueDropdown());
            if(selectProperty == null)
                return null;
            boolean multi = select.multi;
            return new Select(selectProperty.mapEntityObjects(mapping), stats.first, stats.second, actualStats, multi ? Select.Type.MULTI : (select.notNull ? Select.Type.NOTNULL : Select.Type.NULL), select.html);
        }
        return null;
    }

    private static <P extends PropertyInterface> Pair<Integer, Integer> getActualSelectStats(FormInstanceContext context, Property.Select<P> select) {
        Pair<Integer, Integer> actualStats = new Pair<>(0, 0);
        for(InputValueList value : select.values) {
            Pair<Integer, Integer> readValues = context.getValues(value);
            if(actualStats.second < readValues.second)
                actualStats = readValues;
        }
        return actualStats;
    }

    public boolean isValueUnique(GroupObjectEntity grid, Property.ValueUniqueType uniqueType) {
        // remapping all objects except ones in the grid
        ImMap<P, StaticParamNullableExpr> fixedExprs = mapping.filterFnValuesRev(value -> grid != null && !grid.getObjects().contains(value)).mapRevValues(ObjectEntity::getParamExpr);
        return property.isValueUnique(fixedExprs, uniqueType); // false because all sticky columns look bad
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

    public boolean hasNoGridReadOnly(ImSet<ObjectEntity> gridObjects) {
        return property.hasNoGridReadOnly(mapping.filterValuesRev(gridObjects).keys());
    }
}
