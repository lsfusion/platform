package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.filter.UserFilterInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadFiltersAction extends ReadUserEventsAction<ImList<UserFilterInstance>> {
    public ReadFiltersAction(GroupObjectEntity groupObject, LP<?> toProperty) {
        super(groupObject, toProperty);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        ImList<UserFilterInstance> userFilters = formInstance.instanceFactory.getExInstance(groupObject).getUserFilters();
        store(context, userFilters);
    }

    @Override
    public List<JSONObject> createJSON(ImList<UserFilterInstance> filters) {
        List<JSONObject> objects = new ArrayList<>();
        for (UserFilterInstance filter : filters) {
            Map<String, Object> filterMap = new HashMap<>();
            filterMap.put(UserEventAction.PROPERTY_KEY, filter.column.property.getSID());
            filterMap.put(FilterAction.COMPARE_KEY, filter.compare.toString());
            filterMap.put(FilterAction.NEGATION_KEY, filter.negation);
            // storing String because filter JSON may be imported into filters form
            // and no cast to String is being done during import
            Object value = filter.value.getValue();
            if (value != null)
                filterMap.put(FilterAction.VALUE_KEY, value.toString());
            if (!filter.junction) {
                filterMap.put(FilterAction.OR_KEY, true);
            }
            objects.add(new JSONObject(filterMap));
        }
        return objects;
    }

    @Override
    public LP<?> getDefaultToProperty(BusinessLogics BL) {
        return BL.userEventsLM.filters;
    }
}
