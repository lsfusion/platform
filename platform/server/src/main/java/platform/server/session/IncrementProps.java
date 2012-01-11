package platform.server.session;

import platform.server.data.SQLSession;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class IncrementProps extends BaseMutableModifier {

    public IncrementProps() {
    }

    public <P extends PropertyInterface> IncrementProps(Property<P> property, SinglePropertyTableUsage<P> table) {
        add(property, table);
    }

    private Map<Property, SinglePropertyTableUsage<PropertyInterface>> tables = new HashMap<Property, SinglePropertyTableUsage<PropertyInterface>>();

    public Collection<Property> getProperties() {
        return tables.keySet();
    }

    public <P extends PropertyInterface> SinglePropertyTableUsage<P> getTable(Property<P> property) {
        return (SinglePropertyTableUsage<P>)tables.get(property);
    }

    protected boolean isFinal() {
        return true;
    }

    protected <P extends PropertyInterface> PropertyChange<P> getPropertyChange(Property<P> property) {
        SinglePropertyTableUsage<P> table = getTable(property);
        if(table!=null)
            return SinglePropertyTableUsage.getChange(table);
        return null;
    }

    public <P extends PropertyInterface> void add(Property<P> property, SinglePropertyTableUsage<P> changeTable) {
        tables.put(property, (SinglePropertyTableUsage<PropertyInterface>) changeTable);

        addChange(property);
    }

    public void remove(Property property, SQLSession session) throws SQLException {
        SinglePropertyTableUsage<PropertyInterface> table = tables.remove(property);
        if(table!=null) {
            table.drop(session);
            addChange(property);
        }
    }

    public void cleanIncrementTables(SQLSession session) throws SQLException {
        for (Map.Entry<Property, SinglePropertyTableUsage<PropertyInterface>> addTable : tables.entrySet()) {
            addTable.getValue().drop(session);
            addChange(addTable.getKey());
        }
        tables = new HashMap<Property, SinglePropertyTableUsage<PropertyInterface>>();
    }

    protected Collection<Property> calculateProperties() {
        return tables.keySet();
    }
}
