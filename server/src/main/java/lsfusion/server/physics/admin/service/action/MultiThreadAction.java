package lsfusion.server.physics.admin.service.action;

import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.base.task.TaskRunner;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.controller.init.GroupPropertiesSingleTask;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public abstract class MultiThreadAction extends InternalAction {
    private ClassPropertyInterface threadCountInterface;
    private ClassPropertyInterface propertyTimeoutInterface;

    public MultiThreadAction(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        threadCountInterface = i.next();
        propertyTimeoutInterface = i.next();
    }


    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        TaskRunner taskRunner = new TaskRunner(context.getBL());
        GroupPropertiesSingleTask task = createTask();
        task.setBL(context.getBL());
        boolean errorOccurred = false;
        try {
            ObjectValue threadCount = context.getKeyValue(threadCountInterface);
            ObjectValue propertyTimeout = context.getKeyValue(propertyTimeoutInterface);
            taskRunner.runTask(task, ServerLoggers.serviceLogger, threadCount == null ? null : (Integer) threadCount.getValue(),
                    propertyTimeout == null ? null : (Integer) propertyTimeout.getValue(), context);
        } catch (InterruptedException e) {
            errorOccurred = true;
            
            task.logTimeoutTasks();
            
            taskRunner.shutdownNow();            
            taskRunner.interruptThreadPoolProcesses(context);       
            
            ThreadUtils.interruptThread(context, Thread.currentThread());
        } finally {
            Messages messages = getMessages(task, errorOccurred);
            context.message(messages.message, messages.header);
        }
    }

    protected abstract GroupPropertiesSingleTask createTask();

    protected abstract String getCaptionError();

    protected static class Messages {
        public final String message;
        public final String header;

        public Messages(String message, String header) {
            this.message = message;
            this.header = header;
        }
    }
    protected abstract Messages getMessages(GroupPropertiesSingleTask task, boolean errorOccurred);
}