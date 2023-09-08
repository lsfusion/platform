package lsfusion.server.logics.form.interactive.event;

import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.form.interactive.action.activity.FilterAction;
import lsfusion.server.logics.form.interactive.action.activity.UserActivityAction;
import lsfusion.server.logics.form.interactive.instance.filter.CompareFilterInstance;
import lsfusion.server.logics.form.interactive.instance.filter.CompareInstance;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.filter.PropertyFilterInstance;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterEvent extends UserActivityEvent<FilterInstance> {
    public FilterEvent(String groupObject) {
        super(groupObject);
    }

    public FilterEvent(String groupObject, LP<?> toProperty) {
        super(groupObject, toProperty);
    }

    @Override
    public List<JSONObject> createJSON(List<FilterInstance> filters) {
        List<JSONObject> objects = new ArrayList<>();
        for (FilterInstance filter : filters) {
            Map<String, Object> filterMap = new HashMap<>();
            if (filter instanceof PropertyFilterInstance) {
                filterMap.put(UserActivityAction.PROPERTY_KEY, ((PropertyFilterInstance<?>) filter).propertyDraw.getSID());
            }
            if (filter instanceof CompareFilterInstance) {
                filterMap.put(FilterAction.COMPARE_KEY, ((CompareFilterInstance<?>) filter).compare.name());
                filterMap.put(FilterAction.NEGATION_KEY, ((CompareFilterInstance<?>) filter).negate);
                CompareInstance value = ((CompareFilterInstance<?>) filter).value;
                if (value instanceof ObjectValue) {
                    filterMap.put(FilterAction.VALUE_KEY, ((ObjectValue<?>) value).getValue());
                }
            }
            if (!filter.junction) {
                filterMap.put(FilterAction.OR_KEY, true);
            }
            JSONObject jsonObject = new JSONObject(filterMap);
            objects.add(jsonObject);
        }
        return objects;
    }

    @Override
    public LP<?> getDefaultToProperty(BusinessLogics BL) {
        return BL.LM.filters;
    }
}
