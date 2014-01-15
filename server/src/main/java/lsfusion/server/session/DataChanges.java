package lsfusion.server.session;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.caches.AbstractValuesContext;
import lsfusion.server.caches.MapValuesIterable;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.Value;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.DataProperty;

import java.sql.SQLException;

// вообще должен содержать только DataProperty и ActionProperty но так как мн-вого наследования нету приходится извращаться
public class DataChanges extends AbstractValuesContext<DataChanges> {

    private final ImMap<DataProperty, PropertyChange<ClassPropertyInterface>> changes;

    public final static DataChanges EMPTY = new DataChanges();
    public DataChanges() {
        changes = MapFact.EMPTY();
    }

    public DataChanges(DataProperty property, PropertyChange<ClassPropertyInterface> change) {
        if(change.isEmpty()) // в общем-то почти никогда не срабатывает, на всякий случай
            changes = MapFact.EMPTY();
        else
            changes = MapFact.singleton(property, change);
    }

    private DataChanges(DataChanges changes1, DataChanges changes2) {
        changes = changes1.changes.merge(changes2.changes, PropertyChange.<DataProperty, ClassPropertyInterface>addValue());
    }
    public DataChanges add(DataChanges add) {
        if(isEmpty())
            return add;
        if(add.isEmpty())
            return this;
        if(BaseUtils.hashEquals(this, add))
            return this;
        return new DataChanges(this, add);
    }

    public PropertyChanges getPropertyChanges() {
        return new PropertyChanges(changes, true);
    }
    public PropertyChanges add(PropertyChanges add) {
        return getPropertyChanges().add(add);
    }

    public ImSet<DataProperty> getProperties() {
        return changes.keys();
    }

    public ImMap<DataProperty, ImOrderMap<ImMap<ClassPropertyInterface, DataObject>, ImMap<String, ObjectValue>>> read(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        ImValueMap<DataProperty, ImOrderMap<ImMap<ClassPropertyInterface, DataObject>, ImMap<String, ObjectValue>>> mvResult = changes.mapItValues(); // exception кидается
        for(int i=0,size=changes.size();i<size;i++)
            mvResult.mapValue(i, changes.getValue(i).executeClasses(env));
        return mvResult.immutableValue();
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public PropertyChange<ClassPropertyInterface> get(DataProperty property) {
        return changes.get(property);
    }

    public int hash(HashValues hash) {
        return MapValuesIterable.hash(changes, hash);
    }

    public ImSet<Value> getValues() {
        return MapValuesIterable.getContextValues(changes);
    }

    private DataChanges(DataChanges propChanges, MapValuesTranslate mapValues) {
        changes = mapValues.translateValues(propChanges.changes);
    }
    public DataChanges translate(MapValuesTranslate mapValues) {
        return new DataChanges(this, mapValues);
    }

    public boolean twins(TwinImmutableObject o) {
        return changes.equals(((DataChanges)o).changes);
    }

    private DataChanges(DataChanges dataChanges, boolean pack) {
        changes = dataChanges.changes.mapValues(new GetValue<PropertyChange<ClassPropertyInterface>, PropertyChange<ClassPropertyInterface>>() {
            public PropertyChange<ClassPropertyInterface> getMapValue(PropertyChange<ClassPropertyInterface> value) {
                return value.pack();
            }});
    }
    @Override
    public DataChanges calculatePack() {
        return new DataChanges(this, true);
    }

    protected long calculateComplexity(boolean outer) {
        long result = 0;
        for(int i=0,size=changes.size();i<size;i++)
            result += changes.getValue(i).getComplexity(outer);
        return result;
    }
}
