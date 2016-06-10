package lsfusion.server.logics.service;

import lsfusion.base.MultiCauseException;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.ThreadUtils;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.logics.tasks.TaskRunner;

import java.sql.SQLException;
import java.util.Iterator;

public abstract class MultiThreadActionProperty extends ScriptingActionProperty {
    private ClassPropertyInterface threadCountInterface;
    private ClassPropertyInterface propertyTimeoutInterface;

    public MultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        threadCountInterface = i.next();
        propertyTimeoutInterface = i.next();
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        TaskRunner taskRunner = new TaskRunner(context.getBL());
        GroupPropertiesSingleTask task = createTask();
        boolean errorOccurred = false;
        try {
            ObjectValue threadCount = context.getKeyValue(threadCountInterface);
            ObjectValue propertyTimeout = context.getKeyValue(propertyTimeoutInterface);
            task.init(context);
            taskRunner.runTask(task, ServerLoggers.serviceLogger, threadCount == null ? null : (Integer) threadCount.getValue(),
                    propertyTimeout == null ? null : (Integer) propertyTimeout.getValue());
        } catch (InterruptedException | MultiCauseException e) {
            errorOccurred = true;
            task.logTimeoutTasks();
            taskRunner.shutdownNow();
            ServerLoggers.serviceLogger.error(getCaptionError(), e);
            if(e instanceof MultiCauseException) {
                for(Throwable t : ((MultiCauseException) e).getCauses())
                    ServerLoggers.serviceLogger.error(getCaptionError(), t);
            }
            context.delayUserInterfaction(new MessageClientAction(e.getMessage(), getCaptionError()));
            ThreadUtils.interruptThread(context, Thread.currentThread());
            taskRunner.interruptThreadPoolProcesses(context);
        } finally {
            context.delayUserInterfaction(createMessageClientAction(task, errorOccurred));
        }
    }

    protected abstract GroupPropertiesSingleTask createTask();

    protected abstract String getCaptionError();

    protected abstract MessageClientAction createMessageClientAction(GroupPropertiesSingleTask task, boolean errorOccurred);
}