package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.nvl;

public class FilterPropertyAction extends SystemExplicitAction {
    private final PropertyDrawEntity property;
    private final LP<?> fromProperty;

    public FilterPropertyAction(PropertyDrawEntity property, LP<?> fromProperty) {
        this.property = property;
        this.fromProperty = fromProperty;
    }
    
    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);

        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("property", property.getSID());
        jsonObject.put("value", nvl(fromProperty, formInstance.BL.userEventsLM.filtersProperty).read(context));
        jsonArray.put(jsonObject);
        formInstance.BL.userEventsLM.filters.change(jsonArray.toString(), context.getSession());

        new FilterAction(property.getToDraw(formInstance.entity), formInstance.BL.userEventsLM.filters).executeInternal(context);
    }
}