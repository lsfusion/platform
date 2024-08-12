package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.interop.action.FilterGroupClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.nvl;

public class FilterGroupAction extends SystemExplicitAction {
    private final Integer filterGroup;
    private final LP<?> fromProperty;

    public FilterGroupAction(Integer filterGroup, LP<?> fromProperty) {
        this.filterGroup = filterGroup;
        this.fromProperty = fromProperty;
    }
    
    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        Integer index = nvl((Integer) nvl(fromProperty, formInstance.BL.userEventsLM.filterGroups).read(context), 0);
        context.requestUserInteraction(new FilterGroupClientAction(filterGroup, index));
    }
}
