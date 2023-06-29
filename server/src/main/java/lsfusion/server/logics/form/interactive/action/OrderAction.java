package lsfusion.server.logics.form.interactive.action;

import lsfusion.interop.action.OrderClientAction;
import lsfusion.interop.form.order.user.Order;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.json.JSONReader;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.LinkedHashMap;

public class OrderAction extends SystemExplicitAction {
    private final GroupObjectEntity groupObject;

    public OrderAction(GroupObjectEntity groupObject) {
        this.groupObject = groupObject;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);

        JSONObject jsonObject;
        Object json = formInstance.BL.LM.orderedProperty.read(context.getEnv());
        if (json instanceof String) {
            Object readJson = JSONReader.readObject((String) json);
            if (readJson instanceof JSONObject) {
                jsonObject = (JSONObject) readJson;
            } else {
                return;
            }
        } else if (json instanceof JSONObject) {
            jsonObject = (JSONObject) json;
        } else {
            return;
        }

        LinkedHashMap<Integer, Byte> orders = new LinkedHashMap<>();

        jsonObject.keys().forEachRemaining(s -> {
            PropertyDrawInstance propertyDraw = formInstance.getPropertyDraw(s);
            if (propertyDraw != null) {
                Order modiType = Order.valueOf(jsonObject.optString(s).toUpperCase());
                orders.put(propertyDraw.getID(), modiType.serialize());
            }
        });

        OrderClientAction orderClientAction = new OrderClientAction(groupObject.getID(), orders);
        context.delayUserInteraction(orderClientAction);
    }
}
