package lsfusion.server.session;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;

public class IncrementTableProps extends IncrementProps {

    public final TableProps tableProps = new TableProps();

    public IncrementTableProps() {
    }
    
    public boolean isEmpty() {
        return getProperties().isEmpty();
    }

    public <P extends PropertyInterface> IncrementTableProps(CalcProperty<P> property, SinglePropertyTableUsage<P> table) {
        add(property, table);
    }

    public ImSet<CalcProperty> getProperties() {
        return tableProps.getProperties();
    }

    public boolean contains(CalcProperty property) {
        return tableProps.contains(property);
    }

    public <P extends PropertyInterface> SinglePropertyTableUsage<P> getTable(CalcProperty<P> property) {
        return tableProps.getTable(property);
    }

    public <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property) {
        return tableProps.getPropertyChange(property);
    }

    public <P extends PropertyInterface> void add(CalcProperty<P> property, SinglePropertyTableUsage<P> changeTable) {
        assert !tableProps.contains(property);
        tableProps.add(property, changeTable);

        eventChange(property, true);
    }

    public void clear(SQLSession session) throws SQLException {
        eventChanges(tableProps.getProperties());

        tableProps.clear(session);
    }
}
