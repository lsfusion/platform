package platform.server.session;

import platform.server.logics.property.DataProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.PropertyChange;
import platform.base.QuickMap;

import java.sql.SQLException;
import java.util.*;

public class DataChanges extends AbstractPropertyChanges<ClassPropertyInterface,DataProperty,DataChanges> {

    protected DataChanges createThis() {
        return new DataChanges(); 
    }

    public class Actions extends QuickMap<ActionProperty, PropertyChange<ClassPropertyInterface>> {

        public Actions() {
        }

        public Actions(ActionProperty key, PropertyChange<ClassPropertyInterface> value) {
            super(key, value);
        }

        public Actions(QuickMap<? extends ActionProperty, ? extends PropertyChange<ClassPropertyInterface>> set) {
            super(set);
        }

        protected PropertyChange<ClassPropertyInterface> addValue(PropertyChange<ClassPropertyInterface> prevValue, PropertyChange<ClassPropertyInterface> newValue) {
            return prevValue.add(newValue);
        }

        protected boolean containsAll(PropertyChange<ClassPropertyInterface> who, PropertyChange<ClassPropertyInterface> what) {
            throw new RuntimeException("not supported yet");
        }

        public void execute(DataSession session) throws SQLException {
            for(int i=0;i<size;i++)
                for(Map<ClassPropertyInterface, DataObject> row : getValue(i).getQuery("value").executeClasses(session, session.baseClass).keySet())
                    getKey(i).execute(row);
        }
    }

    public final Actions actions;

    public DataChanges() {
        actions = new Actions();
    }

    public DataChanges(DataProperty property, PropertyChange<ClassPropertyInterface> change) {
        super(property, change);
        actions = new Actions();
    }

    public DataChanges(ActionProperty action, PropertyChange<ClassPropertyInterface> change) {
        actions = new Actions(action, change);
    }

    public DataChanges(DataChanges changes1, DataChanges changes2) {
        super(changes1, changes2);
        actions = new Actions(changes1.actions);
        actions.addAll(changes2.actions);
    }

    public DataChanges add(DataChanges add) {
        return new DataChanges(this, add);
    }

    public void execute(DataSession session) throws SQLException {
        for(int i=0;i<size;i++)
            for(Map.Entry<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>> row : getValue(i).getQuery("value").executeClasses(session, session.baseClass).entrySet())
                session.changeProperty(getKey(i), row.getKey(), row.getValue().get("value"), false);
        actions.execute(session);
    }
}
