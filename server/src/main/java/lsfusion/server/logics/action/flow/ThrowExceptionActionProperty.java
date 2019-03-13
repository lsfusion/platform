package lsfusion.server.logics.action.flow;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.sql.SQLException;

public class ThrowExceptionActionProperty extends ScriptingActionProperty {

    public ThrowExceptionActionProperty(BaseLogicsModule lm, ValueClass... classes) {
        super(lm, classes);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String message = ((String) context.getSingleKeyObject());
        throw new LSFException(message);
    }
}