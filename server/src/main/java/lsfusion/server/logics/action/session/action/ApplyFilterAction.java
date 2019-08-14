package lsfusion.server.logics.action.session.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.ApplyFilter;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class ApplyFilterAction extends InternalAction {

    private final ApplyFilter type;

    public ApplyFilterAction(BaseLogicsModule lm, ApplyFilter type) {
        super(lm);
        this.type = type;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        context.getSession().setApplyFilter(type);
    }
}
