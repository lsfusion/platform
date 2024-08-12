package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.interop.action.ReadFilterGroupClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class ReadFiltersGroupAction extends SystemExplicitAction {
    private final Integer filterGroup;
    private final LP<?> toProperty;

    public ReadFiltersGroupAction(Integer filterGroup, LP<?> toProperty) {
        this.filterGroup = filterGroup;
        this.toProperty = toProperty;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        Integer index = (Integer) context.requestUserInteraction(new ReadFilterGroupClientAction(filterGroup));

        LP<?> targetProperty = toProperty;
        if (targetProperty == null) {
            targetProperty = context.getBL().userEventsLM.filtersGroup;
        }
        targetProperty.change(index, context.getSession());
    }
}
