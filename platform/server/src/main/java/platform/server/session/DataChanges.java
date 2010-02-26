package platform.server.session;

import platform.server.logics.property.DataProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.PropertyChange;
import platform.server.caches.MapValues;
import platform.server.caches.HashValues;
import platform.server.caches.MapValuesIterable;
import platform.server.data.expr.ValueExpr;
import platform.base.QuickMap;

import java.sql.SQLException;
import java.util.*;

public class DataChanges extends QuickMap<DataProperty, PropertyChange<ClassPropertyInterface>> implements MapValues<DataChanges> {

    public DataChanges() {
    }

    public DataChanges(DataProperty property, PropertyChange<ClassPropertyInterface> change) {
        super(property, change);
    }

    public DataChanges(DataChanges changes1, DataChanges changes2) {
        super(changes1);
        addAll(changes2);
    }

    protected PropertyChange<ClassPropertyInterface> addValue(PropertyChange<ClassPropertyInterface> prevValue, PropertyChange<ClassPropertyInterface> newValue) {
        return prevValue.add(newValue);
    }

    protected boolean containsAll(PropertyChange<ClassPropertyInterface> who, PropertyChange<ClassPropertyInterface> what) {
        throw new RuntimeException("not supported yet");
    }

    public void change(DataSession session) throws SQLException {
        for(int i=0;i<size;i++)
            for(Map.Entry<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>> row : getValue(i).getQuery("value").executeClasses(session, session.baseClass).entrySet())
                session.changeProperty(getKey(i), row.getKey(), row.getValue().get("value"), false);
    }

    public boolean hasChanges() {
        for(int i=0;i<size;i++)
            if(!getValue(i).where.isFalse())
                return true;
        return false;
    }

    public Collection<DataProperty> getProperties() {
        Collection<DataProperty> result = new ArrayList<DataProperty>();
        for(int i=0;i<size;i++)
            if(!getValue(i).where.isFalse())
                result.add(getKey(i));
        return result;
    }

    public int hashValues(HashValues hashValues) {
        return MapValuesIterable.hash(this,hashValues);
    }

    public Set<ValueExpr> getValues() {
        Set<ValueExpr> result = new HashSet<ValueExpr>();
        for(int i=0;i<size;i++)
            result.addAll(getValue(i).getValues());
        return result;
    }

    public DataChanges translate(Map<ValueExpr, ValueExpr> mapValues) {
        DataChanges result = new DataChanges();
        for(int i=0;i<size;i++)
            result.add(getKey(i),getValue(i).translate(mapValues));
        return result;
    }
}
