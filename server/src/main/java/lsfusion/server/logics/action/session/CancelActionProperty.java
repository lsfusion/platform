package lsfusion.server.logics.action.session;

import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.SessionDataProperty;
import lsfusion.server.logics.action.SystemExplicitActionProperty;

import java.sql.SQLException;

public class CancelActionProperty extends SystemExplicitActionProperty {

    private final FunctionSet<SessionDataProperty> keepSessionProps;
    public CancelActionProperty(LocalizedString caption, FunctionSet<SessionDataProperty> keepSessionProps) {
        super(caption);
        this.keepSessionProps = keepSessionProps;
    }

    @Override
    protected boolean isSync() {
        return true;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.CANCEL)
            return true;
        if (type == ChangeFlowType.READONLYCHANGE)
            return true;
        return super.hasFlow(type);
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.cancel(keepSessionProps);
    }

}
