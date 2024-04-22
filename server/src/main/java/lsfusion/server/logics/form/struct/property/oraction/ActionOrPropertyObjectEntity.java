package lsfusion.server.logics.form.struct.property.oraction;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.form.interactive.action.input.InputValueList;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;

public abstract class ActionOrPropertyObjectEntity<P extends PropertyInterface, T extends ActionOrProperty<P>> extends TwinImmutableObject {

    public T property;
    public ImRevMap<P, ObjectEntity> mapping;

    protected ActionOrPropertyObjectEntity() {
        //нужен для десериализации
        creationScript = null;
        creationPath = null;
        path = null;
    }

    public String toString() {
        return property.toString();
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return property.equals(((ActionOrPropertyObjectEntity) o).property) && mapping.equals(((ActionOrPropertyObjectEntity) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }

    public ActionOrPropertyObjectEntity(T property, ImRevMap<P, ObjectEntity> mapping, String creationScript, String creationPath, String path) {
        this.property = property;
        this.mapping = mapping;
        this.creationScript = creationScript==null ? null : creationScript.substring(0, Math.min(10000, creationScript.length()));
        this.creationPath = creationPath;
        this.path = path;
        assert !mapping.containsNull();
        assert property.interfaces.equals(mapping.keys());
    }

    public ImSet<ObjectEntity> getObjectInstances() {
        return mapping.valuesSet();
    }

    protected final String creationScript;
    protected final String creationPath;
    protected final String path;

    public String getCreationScript() {
        return creationScript;
    }

    public String getCreationPath() {
        return creationPath;
    }

    public String getPath() {
        return path;
    }

    public static <I extends PropertyInterface, T extends ActionOrProperty<I>> ActionOrPropertyObjectEntity<I, ?> create(T property, ImRevMap<I, ObjectEntity> map, String creationScript, String creationPath, String path) {
        if(property instanceof Property)
            return new PropertyObjectEntity<>((Property<I>) property, map, creationScript, creationPath, path);
        else
            return new ActionObjectEntity<>((Action<I>) property, map, creationScript, creationPath, path);
    }

    protected static <P extends PropertyInterface> PropertyObjectEntity.Select getSelectProperty(FormInstanceContext context, Boolean forceFilterSelected, Property.Select<P> select, ImRevMap<P, ObjectEntity> mapping) {
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
            return new PropertyObjectEntity.Select(selectProperty.mapEntityObjects(mapping), stats.first, stats.second, actualStats, multi ? PropertyObjectEntity.Select.Type.MULTI : (select.notNull ? PropertyObjectEntity.Select.Type.NOTNULL : PropertyObjectEntity.Select.Type.NULL), select.html);
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
}
