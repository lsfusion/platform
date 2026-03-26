package lsfusion.server.physics.dev.integration.external.to;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class SetAutoCommitAction extends InternalAction {
    private final ClassPropertyInterface connectionStringInterface;
    private final ClassPropertyInterface autoCommitInterface;

    public SetAutoCommitAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> interfaces = getOrderInterfaces().iterator();
        connectionStringInterface = interfaces.next();
        autoCommitInterface = interfaces.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        String connectionString = (String) context.getKeyValue(connectionStringInterface).getValue();
        boolean autoCommit = context.getKeyValue(autoCommitInterface).getValue() != null;
        if (connectionString != null) {
            context.getSQLConnection(context, connectionString).setAutoCommit(autoCommit);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
