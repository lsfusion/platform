package lsfusion.server.logics.action.session.change.increment;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.TableProps;
import lsfusion.server.logics.action.session.table.PropertyChangeTableUsage;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class IncrementTableProps extends IncrementProps {

    public final TableProps tableProps = new TableProps();

    @Override
    public String toString() {
        return tableProps.toString();
    }

    public IncrementTableProps() {
    }
    
    public boolean isEmpty() {
        return getProperties().isEmpty();
    }

    public ImSet<Property> getProperties() {
        return tableProps.getProperties();
    }

    public boolean contains(Property property) {
        return tableProps.contains(property);
    }

    public <P extends PropertyInterface> PropertyChangeTableUsage<P> getTable(Property<P> property) {
        return tableProps.getTable(property);
    }

    public <P extends PropertyInterface> PropertyChange<P> getPropertyChange(Property<P> property) {
        return tableProps.getPropertyChange(property);
    }

    public <P extends PropertyInterface> void add(Property<P> property, PropertyChangeTableUsage<P> changeTable) throws SQLException, SQLHandledException {
        assert !tableProps.contains(property);
        tableProps.add(property, changeTable);

        eventChange(property, true);
    }

    public <P extends PropertyInterface> void remove(Property<P> property, SQLSession sql, OperationOwner owner) throws SQLException, SQLHandledException {
        assert tableProps.contains(property);
        tableProps.remove(property, sql, owner);

        eventChange(property, true);
    }

    public void clear(SQLSession session, OperationOwner owner) throws SQLException, SQLHandledException {
        eventChanges(tableProps.getProperties());

        tableProps.clear(session, owner);
    }

    public long getMaxCount(Property property) {
        PropertyChangeTableUsage table = tableProps.getTable(property);
        if(table != null)
            return table.getCount();
        return 0;
    }

    @Override
    public String out() {
        return "\nincrementprops : " + BaseUtils.tab(tableProps.out());
    }
}
