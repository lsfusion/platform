package platform.server.session;

import platform.server.data.SQLSession;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class IncrementProps extends BaseMutableModifier {

    public IncrementProps() {
    }

    public <P extends PropertyInterface> IncrementProps(CalcProperty<P> property, SinglePropertyTableUsage<P> table) {
        add(property, table);
    }

    public <P extends PropertyInterface> IncrementProps(Map<? extends CalcProperty, ? extends SinglePropertyTableUsage> tables) {
        for(Map.Entry<? extends CalcProperty, ? extends SinglePropertyTableUsage> table : tables.entrySet())
            add(table.getKey(), table.getValue());
    }

    private Map<CalcProperty, SinglePropertyTableUsage<PropertyInterface>> tables = new HashMap<CalcProperty, SinglePropertyTableUsage<PropertyInterface>>();

    public Collection<CalcProperty> getProperties() {
        return tables.keySet();
    }

    public <P extends PropertyInterface> SinglePropertyTableUsage<P> getTable(CalcProperty<P> property) {
        return (SinglePropertyTableUsage<P>)tables.get(property);
    }

    protected boolean isFinal() {
        return true;
    }

    protected <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property) {
        SinglePropertyTableUsage<P> table = getTable(property);
        if(table!=null)
            return SinglePropertyTableUsage.getChange(table);
        return null;
    }

    public <P extends PropertyInterface> void add(CalcProperty<P> property, SinglePropertyTableUsage<P> changeTable) {
        tables.put(property, (SinglePropertyTableUsage<PropertyInterface>) changeTable);

        addChange(property);
    }

    public void remove(CalcProperty property, SQLSession session) throws SQLException {
        SinglePropertyTableUsage<PropertyInterface> table = tables.remove(property);
        if(table!=null) {
            table.drop(session);
            addChange(property);
        }
    }

    public void cleanIncrementTables(SQLSession session) throws SQLException {
        for (Map.Entry<CalcProperty, SinglePropertyTableUsage<PropertyInterface>> addTable : tables.entrySet()) {
            addTable.getValue().drop(session);
            addChange(addTable.getKey());
        }
        tables = new HashMap<CalcProperty, SinglePropertyTableUsage<PropertyInterface>>();
    }

    protected Collection<CalcProperty> calculateProperties() {
        return tables.keySet();
    }
}
