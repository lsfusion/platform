package lsfusion.server.logics.classes.utils.time;

import com.google.common.base.Throwables;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.language.ScriptingErrorLog;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Iterator;

public class ToMillisecondsActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface timestampInterface;

    public ToMillisecondsActionProperty(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        timestampInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Timestamp timestamp = (Timestamp) context.getKeyValue(timestampInterface).getValue();
        try {
            findProperty("resultMilliseconds[]").change(timestamp != null ? timestamp.getTime() : null, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}