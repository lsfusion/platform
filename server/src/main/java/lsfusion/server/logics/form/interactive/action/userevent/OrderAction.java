package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.MOrderMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.PropertyColumn;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;

public class OrderAction extends UserEventAction {
    public static final String DESC_KEY = "desc";
    
    public OrderAction(GroupObjectEntity groupObject, ValueClass... valueClasses) {
        super(groupObject, valueClasses);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        GroupObjectInstance groupObjectInstance = formInstance.instanceFactory.getExInstance(groupObject);
        if(groupObjectInstance != null) {
            List<JSONObject> objectList = readJSON(context);
            MOrderMap<PropertyColumn, Boolean> mOrders = MapFact.mOrderMap(MapFact.override());
            if (objectList != null) {
                for (JSONObject jsonObject : objectList) {
                    PropertyDrawInstance<?> propertyDraw = getPropertyDraw(formInstance, groupObjectInstance, jsonObject);
                    if (propertyDraw != null)
                        mOrders.add(new PropertyColumn(propertyDraw, MapFact.EMPTY()), jsonObject.optBoolean(DESC_KEY, false));
                }
            }
            groupObjectInstance.setUserOrders(mOrders.immutableOrder());
        }
    }
}
