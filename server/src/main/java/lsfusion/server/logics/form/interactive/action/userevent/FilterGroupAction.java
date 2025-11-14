package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.action.FilterGroupClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.nvl;

public class FilterGroupAction extends SystemExplicitAction {
    private final String filterGroup;

    private final ClassPropertyInterface fromInterface;

    public FilterGroupAction(String filterGroup, ValueClass... valueClasses) {
        super(valueClasses);
        this.filterGroup = filterGroup;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();
        this.fromInterface = orderInterfaces.get(0);
    }
    
    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Integer index = nvl((Integer) context.getKeyObject(fromInterface), 0);
        RegularFilterGroupEntity filterGroupEntity = context.getFormInstance(true, true).entity.getRegularFilterGroup(filterGroup);
        if(filterGroupEntity != null) {
            context.requestUserInteraction(new FilterGroupClientAction(filterGroupEntity.getID(), index));
        }
    }
}
