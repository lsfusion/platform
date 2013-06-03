package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;

import java.sql.SQLException;

public class ActionPropertyValueImplement<T extends PropertyInterface> extends ActionPropertyImplement<T, ObjectValue> {

    public ActionPropertyValueImplement(ActionProperty<T> action) {
        super(action);
    }

    public ActionPropertyValueImplement(ActionProperty<T> action, ImMap<T, ? extends ObjectValue> mapping) {
        super(action, (ImMap<T, ObjectValue>)mapping);
    }

    public void execute(ExecutionEnvironment session) throws SQLException {
        property.execute(mapping, session, null);
    }
    
    public ActionPropertyValueImplement<T> updateCurrentClasses(DataSession session) throws SQLException {
        return new ActionPropertyValueImplement<T>(property, session.updateCurrentClasses(mapping));
    }
}
