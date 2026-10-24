package lsfusion.server.physics.admin.monitor.action;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.RemoteLoggerAspect;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Iterator;

import static lsfusion.base.DateConverter.sqlTimestampToLocalDateTime;

public class CancelSQLProcessAction extends InternalAction {
    private final ClassPropertyInterface integerInterface;

    public CancelSQLProcessAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        integerInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataObject currentObject = context.getDataKeyValue(integerInterface);
            Integer processId = (Integer) findProperty("idSQLProcess[STRING[10]]").read(context, currentObject);
            Thread thread = SQLSession.getJavaSQLSession(processId);
            if (thread == null) {
                // nothing runs on the connection, but its thread can be between statements (e.g. sleeping between apply attempts
                // after update conflicts), so it is interrupted as Cancel java-process does - only while it is still in the call
                // that ran the connection's last statement (a pool thread may be in another call by now), and never this action's
                // own thread; "s" marks a process without a java thread
                String idThread = (String) findProperty("idThreadProcess[STRING[10]]").read(context, currentObject);
                LocalDateTime dateTimeCall = (LocalDateTime) findProperty("dateTimeCallProcess[STRING[10]]").read(context, currentObject);
                LocalDateTime dateTimeSQL = (LocalDateTime) findProperty("dateTimeSQLProcess[STRING[10]]").read(context, currentObject);
                if (idThread != null && !idThread.startsWith("s") && dateTimeCall != null && dateTimeSQL != null && !dateTimeCall.isAfter(dateTimeSQL)) {
                    long threadId = Long.parseLong(idThread);
                    Timestamp currentCall = RemoteLoggerAspect.getDateTimeCall(threadId);
                    if (threadId != Thread.currentThread().getId() && currentCall != null && sqlTimestampToLocalDateTime(currentCall).equals(dateTimeCall))
                        thread = ThreadUtils.getThreadById(threadId);
                }
            }
            if (!ThreadUtils.interruptThread(context.getDbManager(), thread))
                SQLSession.cancelExecutingStatement(context.getSession().sql, processId);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
