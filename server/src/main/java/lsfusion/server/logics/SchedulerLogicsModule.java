package lsfusion.server.logics;

import lsfusion.server.logics.linear.LAP;
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
    public LCP timeFromScheduledTask;
    public LCP timeToScheduledTask;
    public LCP periodScheduledTask;
    public LCP schedulerStartTypeScheduledTask;
    public LCP activeScheduledTask;
    public LCP daysOfWeekScheduledTask;
    public LCP daysOfMonthScheduledTask;
    public LCP ignoreExceptionsScheduledTaskDetail;
    public LCP activeScheduledTaskDetail;
    public LCP orderScheduledTaskDetail;
    public LCP scriptScheduledTaskDetail;
    public LCP timeoutScheduledTaskDetail;
    public LCP parameterScheduledTaskDetail;
    public LCP scheduledTaskScheduledTaskDetail;

    public LCP canonicalNamePropertyScheduledTaskDetail;
    
    public LCP resultScheduledTaskLog;
    public LCP exceptionOccurredScheduledTaskLog;
    public LCP propertyScheduledTaskLog;
    public LCP dateScheduledTaskLog;
    public LCP scheduledTaskScheduledTaskLog;
    public LCP scheduledTaskLogScheduledClientTaskLog;
    public LCP messageScheduledClientTaskLog;
    public LCP dateScheduledClientTaskLog;

    public LCP scriptText;
    public LAP evalScript;

    public SchedulerLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(SchedulerLogicsModule.class.getResourceAsStream("/lsfusion/system/Scheduler.lsf"), "/lsfusion/system/Scheduler.lsf", baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();

        scheduledTask = (ConcreteCustomClass) findClass("ScheduledTask");
        scheduledTaskLog = (ConcreteCustomClass) findClass("ScheduledTaskLog");
        scheduledClientTaskLog = (ConcreteCustomClass) findClass("ScheduledClientTaskLog");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        runAtStartScheduledTask = findProperty("runAtStartScheduledTask");
        startDateScheduledTask = findProperty("startDateScheduledTask");
        timeFromScheduledTask = findProperty("timeFromScheduledTask");
        timeToScheduledTask = findProperty("timeToScheduledTask");
        periodScheduledTask = findProperty("periodScheduledTask");
        schedulerStartTypeScheduledTask = findProperty("schedulerStartTypeScheduledTask");
        activeScheduledTask = findProperty("activeScheduledTask");
        daysOfWeekScheduledTask = findProperty("daysOfWeekScheduledTask");
        daysOfMonthScheduledTask = findProperty("daysOfMonthScheduledTask");
        ignoreExceptionsScheduledTaskDetail = findProperty("ignoreExceptionsScheduledTaskDetail");
        activeScheduledTaskDetail = findProperty("activeScheduledTaskDetail");
        orderScheduledTaskDetail = findProperty("orderScheduledTaskDetail");
        scriptScheduledTaskDetail = findProperty("scriptScheduledTaskDetail");
        timeoutScheduledTaskDetail = findProperty("timeoutScheduledTaskDetail");
        parameterScheduledTaskDetail = findProperty("parameterScheduledTaskDetail");
        scheduledTaskScheduledTaskDetail = findProperty("scheduledTaskScheduledTaskDetail");

        canonicalNamePropertyScheduledTaskDetail = findProperty("canonicalNamePropertyScheduledTaskDetail");

        resultScheduledTaskLog = findProperty("resultScheduledTaskLog");
        exceptionOccurredScheduledTaskLog = findProperty("exceptionOccurredScheduledTaskLog");
        propertyScheduledTaskLog = findProperty("propertyScheduledTaskLog");
        dateScheduledTaskLog = findProperty("dateScheduledTaskLog");
        scheduledTaskScheduledTaskLog = findProperty("scheduledTaskScheduledTaskLog");
        scheduledTaskLogScheduledClientTaskLog = findProperty("scheduledTaskLogScheduledClientTaskLog");
        messageScheduledClientTaskLog = findProperty("messageScheduledClientTaskLog");
        dateScheduledClientTaskLog = findProperty("dateScheduledClientTaskLog");

        scriptText = findProperty("scriptText");
        evalScript = findAction("evalScript");
    }
}
