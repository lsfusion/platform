package platform.server.session;

import platform.server.logics.property.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.PropertyChange;
import platform.server.view.form.client.RemoteFormView;
import platform.interop.action.ClientAction;

import java.sql.SQLException;
import java.util.*;

// вообще должен содержать только DataProperty и ActionProperty но так как мн-вого наследования нету приходится извращаться
public class DataChanges extends AbstractPropertyChanges<ClassPropertyInterface, UserProperty, DataChanges> {

    protected DataChanges createThis() {
        return new DataChanges(); 
    }

    public DataChanges() {
    }

    public DataChanges(UserProperty property, PropertyChange<ClassPropertyInterface> change) {
        super(property, change);
    }

    public DataChanges(DataChanges changes1, DataChanges changes2) {
        super(changes1, changes2);
    }

    public DataChanges add(DataChanges add) {
        return new DataChanges(this, add);
    }

    public List<ClientAction> execute(DataSession session, RemoteFormView executeForm) throws SQLException {
        List<ClientAction> actions = new ArrayList<ClientAction>();
        for(int i=0;i<size;i++)
            for(Map.Entry<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>> row : getValue(i).getQuery("value").executeClasses(session, session.baseClass).entrySet())
                getKey(i).execute(row.getKey(), row.getValue().get("value"), session, actions, executeForm);
        return actions;
    }
}
