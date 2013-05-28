package platform.server.logics.authentication;

import platform.base.BaseUtils;
import platform.interop.remote.UserInfo;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.logics.AuthenticationLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.Iterator;

public class CalculateBase64HashActionProperty extends ScriptingActionProperty {
    AuthenticationLogicsModule LM;
    private final ClassPropertyInterface algorithmInterface;
    private final ClassPropertyInterface passwordInterface;

    public CalculateBase64HashActionProperty(AuthenticationLogicsModule LM) {
        super(LM, new ValueClass[]{StringClass.get(10), StringClass.get(30)});
        this.LM = LM;
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        algorithmInterface = i.next();
        passwordInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        String algorithm = String.valueOf(context.getDataKeyValue(algorithmInterface).getValue());
        String password = String.valueOf(context.getDataKeyValue(passwordInterface).getValue()).trim();

        LM.calculatedHash.change(BaseUtils.calculateBase64Hash(algorithm, password, UserInfo.salt), context.getSession());
    }
}