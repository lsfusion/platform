package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.PropertyColumn;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadOrdersAction extends ReadUserEventsAction<ImOrderMap<PropertyColumn, Boolean>> {
    public ReadOrdersAction(GroupObjectEntity groupObject, LP<?> toProperty) {
        super(groupObject, toProperty);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        store(context, formInstance.instanceFactory.getExInstance(groupObject).getUserOrders());
    }

    @Override
    public List<JSONObject> createJSON(ImOrderMap<PropertyColumn, Boolean> orders) {
        List<JSONObject> objects = new ArrayList<>();
        for (PropertyColumn column : orders.keyOrderSet()) {
            Boolean order = orders.get(column);
            if (order != null) {
                Map<String, Object> orderMap = new HashMap<>();

                orderMap.put(UserEventAction.PROPERTY_KEY, column.property.getSID());
                orderMap.put(OrderAction.DESC_KEY, order); // true is for desc on server

                JSONObject json = new JSONObject(orderMap);

                objects.add(json);
            }
        }

        return objects;
    }

    @Override
    public LP<?> getDefaultToProperty(BusinessLogics BL) {
        return BL.userEventsLM.orders;
    }
}
