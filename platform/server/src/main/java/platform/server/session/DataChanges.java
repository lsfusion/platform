package platform.server.session;

import platform.server.logics.property.DataProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.*;

public class DataChanges extends AbstractPropertyChanges<ClassPropertyInterface,DataProperty,DataChanges> {

    protected DataChanges createThis() {
        return new DataChanges(); 
    }

    public DataChanges() {
    }

    public DataChanges(DataProperty property, PropertyChange<ClassPropertyInterface> change) {
        super(property, change);
    }

    public DataChanges(DataChanges changes1, DataChanges changes2) {
        super(changes1, changes2);
    }

    public DataChanges add(DataChanges add) {
        return new DataChanges(this, add);
    }

    public void change(DataSession session) throws SQLException {
        for(int i=0;i<size;i++)
            for(Map.Entry<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>> row : getValue(i).getQuery("value").executeClasses(session, session.baseClass).entrySet())
                session.changeProperty(getKey(i), row.getKey(), row.getValue().get("value"), false);
    }
}
