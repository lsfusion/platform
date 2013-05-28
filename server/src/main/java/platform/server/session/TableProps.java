package platform.server.session;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.SQLSession;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.Map;

public class TableProps {

    public TableProps() {
    }

    private Map<CalcProperty, SinglePropertyTableUsage<PropertyInterface>> tables = MapFact.mAddRemoveMap(); // mutable с remove поведение

    public ImSet<CalcProperty> getProperties() {
        return SetFact.fromJavaSet(tables.keySet());
    }

    public boolean contains(CalcProperty property) {
        return tables.containsKey(property);
    }

    public <P extends PropertyInterface> SinglePropertyTableUsage<P> getTable(CalcProperty<P> property) {
        return (SinglePropertyTableUsage<P>)tables.get(property);
    }

    public <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property) {
        SinglePropertyTableUsage<P> table = getTable(property);
        if(table!=null)
            return SinglePropertyTableUsage.getChange(table);
        return null;
    }

    public <P extends PropertyInterface> void add(CalcProperty<P> property, SinglePropertyTableUsage<P> changeTable) {
        tables.put(property, (SinglePropertyTableUsage<PropertyInterface>) changeTable);
    }

    public <P extends PropertyInterface> void remove(CalcProperty<P> property, SQLSession sql) throws SQLException {
        tables.remove(property).drop(sql);
    }

    public void clear(SQLSession session) throws SQLException {
        for (Map.Entry<CalcProperty, SinglePropertyTableUsage<PropertyInterface>> addTable : tables.entrySet())
            addTable.getValue().drop(session);
        tables.clear();
    }
}
