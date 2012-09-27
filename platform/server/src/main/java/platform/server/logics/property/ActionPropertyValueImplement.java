package platform.server.logics.property;

import platform.server.logics.DataObject;
import platform.server.session.DataSession;
import platform.server.session.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.Map;

public class ActionPropertyValueImplement<T extends PropertyInterface> extends ActionPropertyImplement<T, DataObject> {

    public ActionPropertyValueImplement(ActionProperty<T> action) {
        super(action);
    }

    public ActionPropertyValueImplement(ActionProperty<T> action, Map<T, DataObject> mapping) {
        super(action, mapping);
    }

    public void execute(ExecutionEnvironment session) throws SQLException {
        property.execute(mapping, session, null);
    }
    
    public ActionPropertyValueImplement<T> updateCurrentClasses(DataSession session) throws SQLException {
        return new ActionPropertyValueImplement<T>(property, session.updateCurrentClasses(mapping));
    }
}
