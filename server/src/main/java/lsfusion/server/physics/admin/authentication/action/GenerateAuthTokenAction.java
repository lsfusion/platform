package lsfusion.server.physics.admin.authentication.action;

import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class GenerateAuthTokenAction extends InternalAction {

    private final AuthenticationLogicsModule authLM;

    private final ClassPropertyInterface userLoginInterface;

    public GenerateAuthTokenAction(AuthenticationLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        userLoginInterface = i.next();

        authLM = LM;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        AuthenticationToken token = context.getSecurityManager().generateToken((String) context.getKeyObject(userLoginInterface));
        authLM.resultAuthToken.change(token.string, context);
    }
}
