package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.interop.action.OrderClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;

import static lsfusion.base.BaseUtils.isRedundantString;

public class OrderAction extends UserEventAction {
    public static final String DESC_KEY = "desc";
    
    public OrderAction(GroupObjectEntity groupObject, ValueClass... valueClasses) {
        super(groupObject, valueClasses);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        List<JSONObject> objectList = readJSON(context);
        LinkedHashMap<Integer, Boolean> orders = new LinkedHashMap<>();
        if (objectList != null) {
            for (JSONObject jsonObject : objectList) {
                String propertyString = jsonObject.optString(PROPERTY_KEY);
                if (!isRedundantString(propertyString)) {
                    PropertyDrawInstance<?> propertyDraw = formInstance.getPropertyDraw(propertyString);
                    if (propertyDraw != null) {
                        // make sure group object is the same
                        GroupObjectEntity propertyGO = propertyDraw.toDraw.entity;
                        if (propertyGO == groupObject) {
                            boolean desc = jsonObject.optBoolean(DESC_KEY, false);
                            orders.put(propertyDraw.getID(), !desc); // as true is for "asc" on client
                        }
                    }
                }
            }
        }
        
        OrderClientAction orderClientAction = new OrderClientAction(groupObject.getID(), orders);
        context.delayUserInteraction(orderClientAction);
    }
}
