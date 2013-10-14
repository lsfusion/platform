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
        super(SchedulerLogicsModule.class.getResourceAsStream("/scripts/system/Scheduler.lsf"), baseLM, BL);
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

        runAtStartScheduledTask = getLCPByName("runAtStartScheduledTask");
        startDateScheduledTask = getLCPByName("startDateScheduledTask");
        periodScheduledTask = getLCPByName("periodScheduledTask");
        schedulerStartTypeScheduledTask = getLCPByName("schedulerStartTypeScheduledTask");
        activeScheduledTask = getLCPByName("activeScheduledTask");
        activeScheduledTaskDetail = getLCPByName("activeScheduledTaskDetail");
        orderScheduledTaskDetail = getLCPByName("orderScheduledTaskDetail");
        scheduledTaskScheduledTaskDetail = getLCPByName("scheduledTaskScheduledTaskDetail");

        SIDPropertyScheduledTaskDetail = getLCPByName("SIDPropertyScheduledTaskDetail");
        
        resultScheduledTaskLog = getLCPByName("resultScheduledTaskLog");
        propertyScheduledTaskLog = getLCPByName("propertyScheduledTaskLog");
        dateScheduledTaskLog = getLCPByName("dateScheduledTaskLog");
        scheduledTaskScheduledTaskLog = getLCPByName("scheduledTaskScheduledTaskLog");
        scheduledTaskLogScheduledClientTaskLog = getLCPByName("scheduledTaskLogScheduledClientTaskLog");
        messageScheduledClientTaskLog = getLCPByName("messageScheduledClientTaskLog");
    }
}
