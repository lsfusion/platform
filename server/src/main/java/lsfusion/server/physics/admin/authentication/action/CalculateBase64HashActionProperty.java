package lsfusion.server.physics.admin.authentication.action;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.admin.authentication.UserInfo;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class CalculateBase64HashActionProperty extends InternalAction {
    AuthenticationLogicsModule LM;
    private final ClassPropertyInterface algorithmInterface;
    private final ClassPropertyInterface passwordInterface;

    public CalculateBase64HashActionProperty(AuthenticationLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        this.LM = LM;
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        algorithmInterface = i.next();
        passwordInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String algorithm = String.valueOf(context.getDataKeyValue(algorithmInterface).getValue());
        String password = String.valueOf(context.getDataKeyValue(passwordInterface).getValue()).trim();

        LM.calculatedHash.change(BaseUtils.calculateBase64Hash(algorithm, password, UserInfo.salt), context.getSession());
    }
}