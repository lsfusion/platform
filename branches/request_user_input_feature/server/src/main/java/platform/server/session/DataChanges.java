package platform.server.session;

import platform.base.BaseUtils;
import platform.base.QuickMap;
import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractValuesContext;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.UserProperty;

import java.sql.SQLException;
import java.util.*;

// вообще должен содержать только DataProperty и ActionProperty но так как мн-вого наследования нету приходится извращаться
public class DataChanges extends AbstractValuesContext<DataChanges> {

    protected static class Changes extends QuickMap<UserProperty, PropertyChange<ClassPropertyInterface>> {

        private Changes() {
        }

        public Changes(UserProperty key, PropertyChange<ClassPropertyInterface> value) {
            super(key, value);
        }

        private Changes(Changes changes, boolean pack) {
            for(int i=0;i<changes.size;i++)
                add(changes.getKey(i), changes.getValue(i).pack());
        }

        private Changes(QuickMap<? extends UserProperty, ? extends PropertyChange<ClassPropertyInterface>> set) {
            super(set);
        }

        protected PropertyChange<ClassPropertyInterface> addValue(UserProperty key, PropertyChange<ClassPropertyInterface> prevValue, PropertyChange<ClassPropertyInterface> newValue) {
            return prevValue.add(newValue);
        }

        public Changes translate(MapValuesTranslate mapValues) {
            Changes result = new Changes();
            for(int i=0;i<size;i++)
                result.add(getKey(i),getValue(i).translateValues(mapValues));
            return result;
        }

        protected boolean containsAll(PropertyChange<ClassPropertyInterface> who, PropertyChange<ClassPropertyInterface> what) {
            throw new RuntimeException("not supported");
        }
    }
    private final Changes changes;

    public DataChanges() {
        changes = new Changes();
    }

    public DataChanges(UserProperty property, PropertyChange<ClassPropertyInterface> change) {
        if(change.isEmpty()) // в общем-то почти никогда не срабатывает, на всякий случай
            changes = new Changes();
        else
            changes = new Changes(property, change);
    }

    private DataChanges(DataChanges changes1, DataChanges changes2) {
        changes = new Changes(changes1.changes);
        changes.addAll(changes2.changes);
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
        return new PropertyChanges(changes);
    }
    public PropertyChanges add(PropertyChanges add) {
        return getPropertyChanges().add(add);
    }

    public Collection<UserProperty> getProperties() {
        return changes.keys();
    }

    public Map<UserProperty, Map<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>> read(SQLSession session, QueryEnvironment env, BaseClass baseClass) throws SQLException {
        Map<UserProperty, Map<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>> result = new HashMap<UserProperty, Map<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>>();
        for(int i=0;i<changes.size;i++)
            result.put(changes.getKey(i), changes.getValue(i).executeClasses(session, env, baseClass));
        return result;
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public PropertyChange<ClassPropertyInterface> get(UserProperty property) {
        return changes.get(property);
    }

    public int hash(HashValues hash) {
        return MapValuesIterable.hash(changes, hash);
    }

    public QuickSet<Value> getValues() {
        return MapValuesIterable.getContextValues(changes);
    }

    private DataChanges(DataChanges propChanges, MapValuesTranslate mapValues) {
        changes = propChanges.changes.translate(mapValues);
    }
    public DataChanges translate(MapValuesTranslate mapValues) {
        return new DataChanges(this, mapValues);
    }

    public boolean twins(TwinImmutableInterface o) {
        return changes.equals(((DataChanges)o).changes);
    }

    private DataChanges(DataChanges dataChanges, boolean pack) {
        changes = new Changes(dataChanges.changes, pack);
    }
    @Override
    public DataChanges calculatePack() {
        return new DataChanges(this, true);
    }

    protected long calculateComplexity(boolean outer) {
        long result = 0;
        for(int i=0;i<changes.size;i++)
            result += changes.getValue(i).getComplexity(outer);
        return result;
    }
}
