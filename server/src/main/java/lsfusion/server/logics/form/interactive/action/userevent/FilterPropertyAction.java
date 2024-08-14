package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

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

        GroupObjectEntity toDraw = property.getToDraw(formInstance.entity);
        String groupObject = formInstance.entity.getSID() + "." + toDraw.getSID();
        String value = (String) nvl(fromProperty, formInstance.BL.userEventsLM.filtersProperty).read(context);
        context.getBL().userEventsLM.filterPropertyAction.execute(context, new DataObject(groupObject), new DataObject(property.getSID()), ObjectValue.getValue(value, StringClass.instance));
    }
}