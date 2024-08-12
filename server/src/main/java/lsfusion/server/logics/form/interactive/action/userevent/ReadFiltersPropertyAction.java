package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.interop.action.ReadFilterPropertyClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class ReadFiltersPropertyAction extends SystemExplicitAction {
    private final Integer property;
    private final LP<?> toProperty;

    public ReadFiltersPropertyAction(Integer property, LP<?> toProperty) {
        this.property = property;
        this.toProperty = toProperty;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String value = (String) context.requestUserInteraction(new ReadFilterPropertyClientAction(property));

        LP<?> targetProperty = toProperty;
        if (targetProperty == null) {
            targetProperty = context.getBL().userEventsLM.filtersProperty;
        }
        targetProperty.change(value, context.getSession());
    }
}
