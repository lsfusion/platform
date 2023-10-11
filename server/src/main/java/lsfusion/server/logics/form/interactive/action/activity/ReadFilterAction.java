package lsfusion.server.logics.form.interactive.action.activity;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.filter.*;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadFilterAction extends ReadUserActivityAction<List<FilterInstance>> {
    public ReadFilterAction(GroupObjectEntity groupObject, LP<?> toProperty) {
        super(groupObject, toProperty);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        List<FilterInstance> userFilters = formInstance.getGroupObjectInstance(groupObject.ID).getUserFilters();
        store(context, userFilters);
    }

    @Override
    public List<JSONObject> createJSON(List<FilterInstance> filters) {
        List<JSONObject> objects = new ArrayList<>();
        for (FilterInstance filter : filters) {
            Map<String, Object> filterMap = new HashMap<>();
            PropertyDrawInstance propertyDraw = null;
            if (filter instanceof PropertyFilterInstance) {
                propertyDraw = ((PropertyFilterInstance<?>) filter).propertyDraw; 
            } else if (filter instanceof NotFilterInstance) {
                NotFilterInstance notFilter = (NotFilterInstance) filter;
                if (notFilter.filter instanceof NotNullFilterInstance) {
                    propertyDraw = ((NotNullFilterInstance<?>) notFilter.filter).propertyDraw;
                }
            }
            if (propertyDraw != null) {
                filterMap.put(UserActivityAction.PROPERTY_KEY, propertyDraw.getSID());
                if (filter instanceof CompareFilterInstance) {
                    filterMap.put(FilterAction.COMPARE_KEY, ((CompareFilterInstance<?>) filter).compare.toString());
                    filterMap.put(FilterAction.NEGATION_KEY, ((CompareFilterInstance<?>) filter).negate);
                    CompareInstance value = ((CompareFilterInstance<?>) filter).value;
                    if (value instanceof ObjectValue) {
                        filterMap.put(FilterAction.VALUE_KEY, ((ObjectValue<?>) value).getValue());
                    }
                } else if (filter instanceof NotNullFilterInstance) {
                    filterMap.put(FilterAction.NEGATION_KEY, true); // according to FilterInstance.deserialize()
                }
                if (!filter.junction) {
                    filterMap.put(FilterAction.OR_KEY, true);
                }
                JSONObject jsonObject = new JSONObject(filterMap);
                objects.add(jsonObject);
            }
        }
        return objects;
    }

    @Override
    public LP<?> getDefaultToProperty(BusinessLogics BL) {
        return BL.LM.filters;
    }
}
