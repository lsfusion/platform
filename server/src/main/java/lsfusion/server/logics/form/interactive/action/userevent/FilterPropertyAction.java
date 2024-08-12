package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.interop.action.FilterPropertyClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.nvl;

public class FilterPropertyAction extends SystemExplicitAction {
    private final Integer property;
    private final LP<?> fromProperty;

    public FilterPropertyAction(Integer property, LP<?> fromProperty) {
        this.property = property;
        this.fromProperty = fromProperty;
    }
    
    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        String value = (String) nvl(fromProperty, formInstance.BL.userEventsLM.filtersProperty).read(context);
        context.requestUserInteraction(new FilterPropertyClientAction(property, value));
    }
}