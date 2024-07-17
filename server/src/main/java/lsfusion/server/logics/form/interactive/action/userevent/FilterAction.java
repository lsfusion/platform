package lsfusion.server.logics.form.interactive.action.userevent;

import com.google.common.base.Throwables;
import lsfusion.interop.action.FilterClientAction;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.isRedundantString;

public class FilterAction extends UserEventAction {
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
        List<JSONObject> objectList = readJSON(context, formInstance.BL.userEventsLM.filters);

        List<FilterClientAction.FilterItem> filters = new ArrayList<>();
        if (objectList != null) {
            for (JSONObject jsonObject : objectList) {
                FilterClientAction.FilterItem filterItem;
                String propertyString = jsonObject.optString(PROPERTY_KEY);
                if (!isRedundantString(propertyString)) {
                    PropertyDrawInstance<?> propertyDraw = formInstance.getPropertyDraw(propertyString);
                    if (propertyDraw != null) {
                        // make sure group object is the same
                        GroupObjectEntity propertyGO = propertyDraw.toDraw.entity;
                        if (propertyGO == groupObject) {
                            filterItem = new FilterClientAction.FilterItem(propertyDraw.getID());

                            String compareString = jsonObject.optString(COMPARE_KEY);
                            if (!isRedundantString(compareString)) {
                                Compare compare = Compare.get(compareString);
                                if (compare != null) {
                                    filterItem.compare = compare.serialize();
                                }
                            }
                            filterItem.negation = jsonObject.optBoolean(NEGATION_KEY);
                            // value may be String (when stored via ReadFiltersAction), may be any other Object
                            try {
                                filterItem.value = FormChanges.serializeConvertFileValue(jsonObject.opt(VALUE_KEY), context.getRemoteContext());
                            } catch (IOException e) {
                                throw Throwables.propagate(e);
                            }
                            filterItem.junction = !jsonObject.optBoolean(OR_KEY);

                            filters.add(filterItem);
                        }
                    }
                }
            }
        }
        FilterClientAction filterClientAction = new FilterClientAction(groupObject.getID(), filters);
        context.delayUserInteraction(filterClientAction);
    }
}
