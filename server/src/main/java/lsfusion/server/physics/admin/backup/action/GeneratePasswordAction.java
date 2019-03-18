package lsfusion.server.physics.admin.backup.action;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class GeneratePasswordAction extends InternalAction {
    private final ClassPropertyInterface lengthInterface;
    private final ClassPropertyInterface useAtLeastOneDigitInterface;
    private final ClassPropertyInterface useBothRegistersInterface;

    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public GeneratePasswordAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        lengthInterface = i.next();
        useAtLeastOneDigitInterface = i.next();
        useBothRegistersInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Integer length = (Integer) context.getKeyValue(lengthInterface).getValue();
        boolean useAtLeastOneDigit = context.getKeyValue(useAtLeastOneDigitInterface).getValue() != null;
        boolean useBothRegisters = context.getKeyValue(useBothRegistersInterface).getValue() != null;

        try {
            if (length == null) {
                length = 8;
            }
            if(length < 3) {
                throw new RuntimeException("Minimum required password length is 3");
            } else {
                findProperty("generatedPassword[]").change(BaseUtils.generatePassword(length, useAtLeastOneDigit, useBothRegisters), context);
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}