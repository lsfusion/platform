package lsfusion.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Random;

public class GeneratePasswordActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface lengthInterface;
    private final ClassPropertyInterface useAtLeastOneDigitInterface;
    private final ClassPropertyInterface useBothRegistersInterface;

    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public GeneratePasswordActionProperty(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        lengthInterface = i.next();
        useAtLeastOneDigitInterface = i.next();
        useBothRegistersInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
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