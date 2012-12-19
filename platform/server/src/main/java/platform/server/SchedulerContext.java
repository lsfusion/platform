package platform.server;

import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ConcreteCustomClass;
import platform.server.logics.DataObject;
import platform.server.logics.Scheduler;

import java.sql.SQLException;

public class SchedulerContext extends WrapperContext {

    private Scheduler scheduler;

    public SchedulerContext(Scheduler scheduler) {
        this.wrappedContext = scheduler.BL;
        this.scheduler = scheduler;
    }

    @Override
    public void delayUserInteraction(ClientAction action) {
        if (action instanceof MessageClientAction) {
            try {
                DataObject scheduledClientTaskLogObject = scheduler.currentLogSession.addObject((ConcreteCustomClass) getBL().schedulerLM.scheduledClientTaskLog);
                getBL().schedulerLM.scheduledTaskLogScheduledClientTaskLog.change(scheduler.currentScheduledTaskLogObject.getValue(), scheduler.currentLogSession, scheduledClientTaskLogObject);
                getBL().schedulerLM.messageScheduledClientTaskLog.change(((MessageClientAction) action).message, scheduler.currentLogSession, scheduledClientTaskLogObject);
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else
            wrappedContext.delayUserInteraction(action);
    }
}
