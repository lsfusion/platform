package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterGroupInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterInstance;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.List;

import static lsfusion.base.BaseUtils.nvl;

public class FilterGroupAction extends SystemExplicitAction {
    private final RegularFilterGroupEntity filterGroup;

    private final ClassPropertyInterface fromInterface;

    public FilterGroupAction(RegularFilterGroupEntity filterGroup, ValueClass... valueClasses) {
        super(valueClasses);
        this.filterGroup = filterGroup;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();
        this.fromInterface = orderInterfaces.get(0);
    }
    
    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Integer index = nvl((Integer) context.getKeyObject(fromInterface), 0);
        FormInstance formInstance = context.getFormInstance(true, true);
        RegularFilterGroupInstance filterGroupInstance = formInstance.instanceFactory.getExInstance(filterGroup);
        if(filterGroupInstance != null) {
            List<RegularFilterInstance> filters = filterGroupInstance.filters;
            // filters are numbered from 1, 0 is none - the way FILTERGROUPS reads them, in a NONULL group too, whose
            // check box a user can clear; a number naming no filter of the group changes nothing
            if (index > 0 && index <= filters.size())
                formInstance.setRegularFilter(filterGroupInstance, filters.get(index - 1));
            else if (index == 0)
                formInstance.setRegularFilter(filterGroupInstance, null);
        }
    }
}
