package lsfusion.server.logics.form.interactive.event;

import lsfusion.base.Pair;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.form.interactive.action.activity.OrderAction;
import lsfusion.server.logics.form.interactive.action.activity.UserActivityAction;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderEvent extends UserActivityEvent<Pair<PropertyDrawInstance, Boolean>> {
    public OrderEvent(String groupObject) {
        super(groupObject);
    }

    public OrderEvent(String groupObject, LP toProperty) {
        super(groupObject, toProperty);
    }

    @Override
    public List<JSONObject> createJSON(List<Pair<PropertyDrawInstance, Boolean>> orders) {
        List<JSONObject> objects = new ArrayList<>();
        for (Pair<PropertyDrawInstance, Boolean> orderItem : orders) {
            if (orderItem.second != null) {
                Map<String, Object> orderMap = new HashMap<>();

                orderMap.put(UserActivityAction.PROPERTY_KEY, orderItem.first.getSID());
                orderMap.put(OrderAction.ORDER_KEY, orderItem.second ? OrderAction.ASC_VALUE : OrderAction.DESC_VALUE);

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
