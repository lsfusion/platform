package lsfusion.server.logics.classes.data.utils.time;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Iterator;

import static lsfusion.server.logics.classes.data.time.DateTimeConverter.localDateTimeToSqlTimestamp;

public class ToMillisecondsAction extends InternalAction {
    private final ClassPropertyInterface timestampInterface;

    public ToMillisecondsAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        timestampInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        LocalDateTime timestamp = (LocalDateTime) context.getKeyValue(timestampInterface).getValue();
        try {
            findProperty("resultMilliseconds[]").change(timestamp != null ? localDateTimeToSqlTimestamp(timestamp).getTime() : null, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}