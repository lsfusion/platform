package lsfusion.server.logics;

import org.antlr.runtime.RecognitionException;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

public class SchedulerLogicsModule extends ScriptingLogicsModule {

    public ConcreteCustomClass scheduledTask;
    public ConcreteCustomClass scheduledTaskLog;
    public ConcreteCustomClass scheduledClientTaskLog;

    public LCP runAtStartScheduledTask;
    public LCP startDateScheduledTask;
    public LCP periodScheduledTask;
    public LCP schedulerStartTypeScheduledTask;
    public LCP activeScheduledTask;
    public LCP activeScheduledTaskDetail;
    public LCP orderScheduledTaskDetail;
    public LCP scheduledTaskScheduledTaskDetail;

    public LCP SIDPropertyScheduledTaskDetail;
    
    public LCP resultScheduledTaskLog;
    public LCP propertyScheduledTaskLog;
    public LCP dateScheduledTaskLog;
    public LCP scheduledTaskScheduledTaskLog;
    public LCP scheduledTaskLogScheduledClientTaskLog;
    public LCP messageScheduledClientTaskLog;

    public SchedulerLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(SchedulerLogicsModule.class.getResourceAsStream("/lsfusion/system/Scheduler.lsf"), "/lsfusion/system/Scheduler.lsf", baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();

        scheduledTask = (ConcreteCustomClass) getClassByName("ScheduledTask");
        scheduledTaskLog = (ConcreteCustomClass) getClassByName("ScheduledTaskLog");
        scheduledClientTaskLog = (ConcreteCustomClass) getClassByName("ScheduledClientTaskLog");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        runAtStartScheduledTask = getLCPByOldName("runAtStartScheduledTask");
        startDateScheduledTask = getLCPByOldName("startDateScheduledTask");
        periodScheduledTask = getLCPByOldName("periodScheduledTask");
        schedulerStartTypeScheduledTask = getLCPByOldName("schedulerStartTypeScheduledTask");
        activeScheduledTask = getLCPByOldName("activeScheduledTask");
        activeScheduledTaskDetail = getLCPByOldName("activeScheduledTaskDetail");
        orderScheduledTaskDetail = getLCPByOldName("orderScheduledTaskDetail");
        scheduledTaskScheduledTaskDetail = getLCPByOldName("scheduledTaskScheduledTaskDetail");

        SIDPropertyScheduledTaskDetail = getLCPByOldName("SIDPropertyScheduledTaskDetail");
        
        resultScheduledTaskLog = getLCPByOldName("resultScheduledTaskLog");
        propertyScheduledTaskLog = getLCPByOldName("propertyScheduledTaskLog");
        dateScheduledTaskLog = getLCPByOldName("dateScheduledTaskLog");
        scheduledTaskScheduledTaskLog = getLCPByOldName("scheduledTaskScheduledTaskLog");
        scheduledTaskLogScheduledClientTaskLog = getLCPByOldName("scheduledTaskLogScheduledClientTaskLog");
        messageScheduledClientTaskLog = getLCPByOldName("messageScheduledClientTaskLog");
    }
}
