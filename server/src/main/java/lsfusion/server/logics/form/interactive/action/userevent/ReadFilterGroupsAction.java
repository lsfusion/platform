package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterGroupInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterInstance;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.nvl;

public class ReadFilterGroupsAction extends SystemExplicitAction {
    private final RegularFilterGroupEntity filterGroup;
    private final LP<?> toProperty;

    public ReadFilterGroupsAction(RegularFilterGroupEntity filterGroup, LP<?> toProperty) {
        this.filterGroup = filterGroup;
        this.toProperty = toProperty;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        RegularFilterGroupInstance regularFilterGroup = formInstance.instanceFactory.getExInstance(filterGroup);

        Integer index = 0;
        for (RegularFilterInstance filter : formInstance.regularFilterValues.values()) {
            int filterIndex = regularFilterGroup.filters.indexOf(filter);
            if (filterIndex >= 0) {
                index = filterIndex + 1;
                break;
            }
        }

        nvl(toProperty, context.getBL().userEventsLM.filterGroups).change(index, context.getSession());
    }
}
