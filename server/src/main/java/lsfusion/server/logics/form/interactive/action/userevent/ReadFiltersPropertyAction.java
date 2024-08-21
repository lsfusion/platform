package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

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

        GroupObjectEntity toDraw = property.getToDraw(formInstance.entity);
        String groupObject = formInstance.entity.getSID() + "." + toDraw.getSID();
        context.getBL().userEventsLM.filtersPropertyAction.execute(context, new DataObject(groupObject), new DataObject(property.getSID()));

        if(toProperty != null)
            toProperty.change((String) context.getBL().userEventsLM.filtersProperty.read(context), context);
    }
}