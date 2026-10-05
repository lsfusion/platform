package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static lsfusion.base.BaseUtils.isRedundantString;

public abstract class UserEventAction extends SystemExplicitAction {
    public static final String PROPERTY_KEY = "property";
    
    protected final GroupObjectEntity groupObject;

    private final ClassPropertyInterface fromInterface;

    public UserEventAction(GroupObjectEntity groupObject, ValueClass... valueClasses) {
        super(valueClasses);
        this.groupObject = groupObject;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();
        this.fromInterface = orderInterfaces.get(0);
    }
    
    protected List<JSONObject> readJSON(ExecutionContext<ClassPropertyInterface> context) {
        ObjectValue jsonObjectValue = context.getKeyValue(fromInterface);
        Object json = InternalAction.readJSON(jsonObjectValue, (DataClass)fromInterface.interfaceClass);
        if (json instanceof JSONObject) {
            return Collections.singletonList((JSONObject) json);
        } else if (json instanceof JSONArray) {
            List<JSONObject> list = new ArrayList<>();
            for (Object object : (JSONArray) json) {
                if (object instanceof JSONObject) {
                    list.add((JSONObject) object);
                }
            }
            return list;
        }
        return null;
    }

    // the property an item names, if it is one of the group's: one in columns is not, as an item names no column
    protected static PropertyDrawInstance<?> getPropertyDraw(FormInstance form, GroupObjectInstance group, JSONObject item) {
        String propertyString = item.optString(PROPERTY_KEY);
        if (!isRedundantString(propertyString)) {
            PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(propertyString);
            if (propertyDraw != null && propertyDraw.toDraw == group && propertyDraw.getColumnGroupObjects().isEmpty())
                return propertyDraw;
        }
        return null;
    }
}
