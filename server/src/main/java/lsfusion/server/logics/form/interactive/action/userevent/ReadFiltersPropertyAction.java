package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.filter.*;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.nvl;

public class ReadFiltersPropertyAction extends SystemExplicitAction {
    private final PropertyDrawEntity property;
    private final LP<?> toProperty;

    public ReadFiltersPropertyAction(PropertyDrawEntity property, LP<?> toProperty) {
        this.property = property;
        this.toProperty = toProperty;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);

        new ReadFiltersAction(property.getToDraw(formInstance.entity), formInstance.BL.userEventsLM.filters).execute(context);

        String value = null;
        String filters = (String) formInstance.BL.userEventsLM.filters.read(context);
        if(filters != null) {
            JSONArray jsonArray = new JSONArray(filters);
            value = jsonArray.getJSONObject(0).getString("value");
        }
        LP<?> targetProperty = toProperty;
        if (targetProperty == null) {
            targetProperty = context.getBL().userEventsLM.filtersProperty;
        }
        targetProperty.change(value, context.getSession());
    }
}