package lsfusion.server.logics.form.interactive.action.activity;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.order.OrderInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadOrdersAction extends ReadUserActivityAction<ImOrderMap<PropertyDrawInstance, Boolean>> {
    public ReadOrdersAction(GroupObjectEntity groupObject, LP<?> toProperty) {
        super(groupObject, toProperty);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        GroupObjectInstance groupObjectInstance = formInstance.getGroupObjectInstance(groupObject.ID);
        ImOrderMap<OrderInstance, Boolean> userOrders = groupObjectInstance.getUserOrders();
        ImMap<OrderInstance, PropertyDrawInstance> propMapping = groupObjectInstance.getUserOrdersPropertyMapping();

        ImOrderMap<PropertyDrawInstance, Boolean> remappedOrders = MapFact.EMPTYORDER();
        for (OrderInstance orderInstance : userOrders.keyOrderSet()) {
            PropertyDrawInstance propertyDraw = propMapping.get(orderInstance);
            if (propertyDraw != null) {
                remappedOrders = remappedOrders.addOrderExcl(propertyDraw, userOrders.get(orderInstance));
            }
        }
        
        store(context, remappedOrders);
    }

    @Override
    public List<JSONObject> createJSON(ImOrderMap<PropertyDrawInstance, Boolean> orders) {
        List<JSONObject> objects = new ArrayList<>();
        for (PropertyDrawInstance propertyDraw : orders.keyOrderSet()) {
            Boolean order = orders.get(propertyDraw);
            if (order != null) {
                Map<String, Object> orderMap = new HashMap<>();

                orderMap.put(UserActivityAction.PROPERTY_KEY, propertyDraw.getSID());
                orderMap.put(OrderAction.ORDER_KEY, order ? OrderAction.DESC_VALUE : OrderAction.ASC_VALUE);

                JSONObject json = new JSONObject(orderMap);

                objects.add(json);
            }
        }

        return objects;
    }

    @Override
    public LP<?> getDefaultToProperty(BusinessLogics BL) {
        return BL.LM.orders;
    }
}
