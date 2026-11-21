package lsfusion.server.physics.admin.authentication.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class ReadLogin2FAAction extends InternalAction {

    private final AuthenticationLogicsModule authLM;

    private final ClassPropertyInterface tokenInterface;

    public ReadLogin2FAAction(AuthenticationLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        tokenInterface = interfaces.single();

        authLM = LM;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        authLM.login2FA.change(context.getSecurityManager().parse2FAToken((String) context.getKeyObject(tokenInterface)), context);
    }
}
