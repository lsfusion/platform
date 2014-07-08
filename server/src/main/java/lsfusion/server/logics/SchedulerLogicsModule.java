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

    public LCP canonicalNamePropertyScheduledTaskDetail;
    
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

        scheduledTask = (ConcreteCustomClass) getClass("ScheduledTask");
        scheduledTaskLog = (ConcreteCustomClass) getClass("ScheduledTaskLog");
        scheduledClientTaskLog = (ConcreteCustomClass) getClass("ScheduledClientTaskLog");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        runAtStartScheduledTask = findLCPByCompoundOldName("runAtStartScheduledTask");
        startDateScheduledTask = findLCPByCompoundOldName("startDateScheduledTask");
        periodScheduledTask = findLCPByCompoundOldName("periodScheduledTask");
        schedulerStartTypeScheduledTask = findLCPByCompoundOldName("schedulerStartTypeScheduledTask");
        activeScheduledTask = findLCPByCompoundOldName("activeScheduledTask");
        activeScheduledTaskDetail = findLCPByCompoundOldName("activeScheduledTaskDetail");
        orderScheduledTaskDetail = findLCPByCompoundOldName("orderScheduledTaskDetail");
        scheduledTaskScheduledTaskDetail = findLCPByCompoundOldName("scheduledTaskScheduledTaskDetail");

        canonicalNamePropertyScheduledTaskDetail = findLCPByCompoundOldName("canonicalNamePropertyScheduledTaskDetail");
        
        resultScheduledTaskLog = findLCPByCompoundOldName("resultScheduledTaskLog");
        propertyScheduledTaskLog = findLCPByCompoundOldName("propertyScheduledTaskLog");
        dateScheduledTaskLog = findLCPByCompoundOldName("dateScheduledTaskLog");
        scheduledTaskScheduledTaskLog = findLCPByCompoundOldName("scheduledTaskScheduledTaskLog");
        scheduledTaskLogScheduledClientTaskLog = findLCPByCompoundOldName("scheduledTaskLogScheduledClientTaskLog");
        messageScheduledClientTaskLog = findLCPByCompoundOldName("messageScheduledClientTaskLog");
    }
}
