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

    public LCP nameScheduledTask;
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

        nameScheduledTask = findProperty("name[ScheduledTask]");
        runAtStartScheduledTask = findProperty("runAtStart[ScheduledTask]");
        startDateScheduledTask = findProperty("startDate[ScheduledTask]");
        timeFromScheduledTask = findProperty("timeFrom[ScheduledTask]");
        timeToScheduledTask = findProperty("timeTo[ScheduledTask]");
        periodScheduledTask = findProperty("period[ScheduledTask]");
        schedulerStartTypeScheduledTask = findProperty("schedulerStartType[ScheduledTask]");
        activeScheduledTask = findProperty("active[ScheduledTask]");
        daysOfWeekScheduledTask = findProperty("daysOfWeek[ScheduledTask]");
        daysOfMonthScheduledTask = findProperty("daysOfMonth[ScheduledTask]");
        ignoreExceptionsScheduledTaskDetail = findProperty("ignoreExceptions[ScheduledTaskDetail]");
        activeScheduledTaskDetail = findProperty("active[ScheduledTaskDetail]");
        orderScheduledTaskDetail = findProperty("order[ScheduledTaskDetail]");
        scriptScheduledTaskDetail = findProperty("script[ScheduledTaskDetail]");
        timeoutScheduledTaskDetail = findProperty("timeout[ScheduledTaskDetail]");
        parameterScheduledTaskDetail = findProperty("parameter[ScheduledTaskDetail]");
        scheduledTaskScheduledTaskDetail = findProperty("scheduledTask[ScheduledTaskDetail]");

        canonicalNamePropertyScheduledTaskDetail = findProperty("canonicalNameProperty[ScheduledTaskDetail]");

        resultScheduledTaskLog = findProperty("result[ScheduledTaskLog]");
        exceptionOccurredScheduledTaskLog = findProperty("exceptionOccurred[ScheduledTaskLog]");
        propertyScheduledTaskLog = findProperty("property[ScheduledTaskLog]");
        dateScheduledTaskLog = findProperty("date[ScheduledTaskLog]");
        scheduledTaskScheduledTaskLog = findProperty("scheduledTask[ScheduledTaskLog]");
        scheduledTaskLogScheduledClientTaskLog = findProperty("scheduledTaskLog[ScheduledClientTaskLog]");
        messageScheduledClientTaskLog = findProperty("message[ScheduledClientTaskLog]");
        dateScheduledClientTaskLog = findProperty("date[ScheduledClientTaskLog]");

        scriptText = findProperty("scriptText[]");
        evalScript = findAction("evalScript[]");
    }
}
