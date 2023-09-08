package lsfusion.server.logics.form.interactive.action.activity;

import lsfusion.interop.action.OrderClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;

import static lsfusion.base.BaseUtils.isRedundantString;

public class OrderAction extends UserActivityAction {
    public static final String ORDER_KEY = "order";
    public static final String ASC_VALUE = "asc";
    public static final String DESC_VALUE = "desc";
    
    public OrderAction(GroupObjectEntity groupObject, LP<?> fromProperty) {
        super(groupObject, fromProperty);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        List<JSONObject> objectList = readJSON(context, formInstance.BL.LM.orders);
        if (objectList != null) {
            LinkedHashMap<Integer, Boolean> orders = new LinkedHashMap<>();
            
            for (JSONObject jsonObject : objectList) {
                String propertyString = jsonObject.optString(PROPERTY_KEY);
                if (!isRedundantString(propertyString)) {
                    PropertyDrawInstance<?> propertyDraw = formInstance.getPropertyDraw(propertyString);
                    if (propertyDraw != null) {
                        boolean asc = true;
                        String orderString = jsonObject.optString(ORDER_KEY);
                        if (!isRedundantString(orderString) && orderString.equalsIgnoreCase(DESC_VALUE)) {
                            asc = false;
                        }

                        orders.put(propertyDraw.getID(), asc);
                    }
                }
            }
            
            OrderClientAction orderClientAction = new OrderClientAction(groupObject.getID(), orders);
            context.delayUserInteraction(orderClientAction);
        }
    }
}
