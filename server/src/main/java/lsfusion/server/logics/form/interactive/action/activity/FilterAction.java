package lsfusion.server.logics.form.interactive.action.activity;

import lsfusion.interop.action.FilterClientAction;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.isRedundantString;

public class FilterAction extends UserActivityAction {
    public static final String COMPARE_KEY = "compare";
    public static final String NEGATION_KEY = "negation";
    public static final String VALUE_KEY = "value";
    public static final String OR_KEY = "or";
    
    public FilterAction(GroupObjectEntity groupObject, LP<?> fromProperty) {
        super(groupObject, fromProperty);
    }
    
    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        List<JSONObject> objectList = readJSON(context, formInstance.BL.LM.filters);
        
        if (objectList != null) {
            List<FilterClientAction.FilterItem> filters = new ArrayList<>();

            for (JSONObject jsonObject : objectList) {
                FilterClientAction.FilterItem filterItem;
                String propertyString = jsonObject.optString(PROPERTY_KEY);
                if (!isRedundantString(propertyString)) {
                    PropertyDrawInstance<?> propertyDraw = formInstance.getPropertyDraw(propertyString);
                    if (propertyDraw != null) {
                        filterItem = new FilterClientAction.FilterItem(propertyDraw.getID());

                        String compareString = jsonObject.optString(COMPARE_KEY);
                        if (!isRedundantString(compareString)) {
                            filterItem.compare = Compare.valueOf(compareString).serialize();
                        }

                        filterItem.negation = jsonObject.optBoolean(NEGATION_KEY);
                        
                        filterItem.value = jsonObject.opt(VALUE_KEY);
                        
                        filterItem.junction = !jsonObject.optBoolean(OR_KEY);

                        filters.add(filterItem);
                    }
                }
            }
            
            FilterClientAction filterClientAction = new FilterClientAction(groupObject.getID(), filters);
            context.delayUserInteraction(filterClientAction);
        }
    }
}
