package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterGroupInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.Map;

public class ReadFilterGroupsAction extends SystemExplicitAction {
    private final Integer filterGroup;
    private final LP<?> toProperty;

    public ReadFilterGroupsAction(Integer filterGroup, LP<?> toProperty) {
        this.filterGroup = filterGroup;
        this.toProperty = toProperty;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);

        Integer index = 0;
        for(Map.Entry<RegularFilterGroupInstance, RegularFilterInstance> entry : formInstance.regularFilterValues.entrySet()) {
            RegularFilterGroupInstance regularFilterGroup = entry.getKey();
            if(regularFilterGroup.getID() == filterGroup) {
                index = regularFilterGroup.filters.indexOf(entry.getValue()) + 1;
            }
        }

        LP<?> targetProperty = toProperty;
        if (targetProperty == null) {
            targetProperty = context.getBL().userEventsLM.filterGroups;
        }
        targetProperty.change(index, context.getSession());
    }
}
