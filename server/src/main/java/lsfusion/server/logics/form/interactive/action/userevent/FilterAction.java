package lsfusion.server.logics.form.interactive.action.userevent;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.filter.UserFilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.PropertyColumn;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;

import static lsfusion.base.BaseUtils.nvl;

public class FilterAction extends UserEventAction {
    public static final String COMPARE_KEY = "compare";
    public static final String NEGATION_KEY = "negation";
    public static final String VALUE_KEY = "value";
    public static final String OR_KEY = "or";
    
    public FilterAction(GroupObjectEntity groupObject, ValueClass... valueClasses) {
        super(groupObject, valueClasses);
    }
    
    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        List<JSONObject> objectList = readJSON(context);

        GroupObjectInstance groupObjectInstance = formInstance.instanceFactory.getExInstance(groupObject);
        if(groupObjectInstance != null) {
            MList<UserFilterInstance> mFilters = ListFact.mList();
            if (objectList != null) {
                for (JSONObject jsonObject : objectList) {
                    PropertyDrawInstance<?> propertyDraw = getPropertyDraw(formInstance, groupObjectInstance, jsonObject);
                    if (propertyDraw != null) {
                        // no comparison is the one the filter panel starts a condition on the property with
                        Compare compare = nvl(Compare.get(jsonObject.optString(COMPARE_KEY)),
                                nvl(propertyDraw.entity.view.getDefaultCompare(formInstance.context), Compare.EQUALS));
                        if (compare == Compare.INARRAY) // a condition of a filter compares with a value
                            throw new RuntimeException("FILTER: " + compare + " is no comparison of a filter condition");
                        // value may be String (when stored via ReadFiltersAction), parsed then as a string - a number
                        // does not parse from a JSON string -, may be any other Object
                        Object jsonValue = jsonObject.opt(VALUE_KEY);
                        Type type = propertyDraw.entity.getStaticType();
                        Object value;
                        try {
                            value = jsonValue instanceof String ? type.parseString((String) jsonValue) : type.parseJSON(jsonValue);
                        } catch (ParseException e) {
                            throw Throwables.propagate(e);
                        }
                        mFilters.add(new UserFilterInstance(new PropertyColumn(propertyDraw, MapFact.EMPTY()), jsonObject.optBoolean(NEGATION_KEY), compare,
                                formInstance.session.getObjectValue(propertyDraw.getFilterProperty().getFilterValueClass(compare), value),
                                !jsonObject.optBoolean(OR_KEY)));
                    }
                }
            }
            groupObjectInstance.setUserFilters(mFilters.immutableList());
        }
    }
}
