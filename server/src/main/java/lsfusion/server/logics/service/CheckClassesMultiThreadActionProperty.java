package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.tasks.TaskRunner;
import lsfusion.server.logics.tasks.impl.CheckClassesTask;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class CheckClassesMultiThreadActionProperty extends ScriptingActionProperty {
    private ClassPropertyInterface threadCountInterface;

    public CheckClassesMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        threadCountInterface = i.next();

    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Integer threadCount = (Integer) context.getKeyValue(threadCountInterface).getValue();
            CheckClassesTask task = new CheckClassesTask();
            task.init(context);
            TaskRunner.runTask(task, ServerLoggers.serviceLogger, threadCount);
            context.delayUserInterfaction(new MessageClientAction(getString("logics.check.was.completed"), getString("logics.checking.data.classes"), true));
        } catch (Exception e) {
            ServerLoggers.serviceLogger.error("Check Classes error", e);
        }
    }
}