package lsfusion.server.logics.action.session.action;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class CancelAction extends SystemExplicitAction {

    private final FunctionSet<SessionDataProperty> keepSessionProps;
    public CancelAction(LocalizedString caption, FunctionSet<SessionDataProperty> keepSessionProps) {
        super(caption);
        this.keepSessionProps = keepSessionProps;
    }

    @Override
    protected boolean isSync() {
        return true;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if(type == ChangeFlowType.CANCEL)
            return true;
        if (type.isManageSession())
            return true;
        return super.hasFlow(type, recursiveAbstracts);
    }

    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.cancel(keepSessionProps);
    }

}
