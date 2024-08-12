package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;

public abstract class ReadUserEventsAction<T> extends SystemExplicitAction {
    protected final GroupObjectEntity groupObject;
    protected final LP<?> toProperty;

    public ReadUserEventsAction(GroupObjectEntity groupObject, LP<?> toProperty) {
        this.groupObject = groupObject;
        this.toProperty = toProperty;
    }

    public void store(ExecutionContext<ClassPropertyInterface> context, T items) throws SQLException, SQLHandledException {
        List<JSONObject> objects = createJSON(items);
        LP<?> filterEventProperty = toProperty;
        if (filterEventProperty == null) {
            filterEventProperty = getDefaultToProperty(context.getBL());
        }
        Object writeObject = null;
        if (!objects.isEmpty()) {
            // always writing JSON array for proper import into orders/filters form
            writeObject = new JSONArray(objects).toString();
        }
        filterEventProperty.change(writeObject, context.getSession());
    }

    public abstract List<JSONObject> createJSON(T items);
    public abstract LP<?> getDefaultToProperty(BusinessLogics BL);
}
