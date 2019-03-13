package lsfusion.server.logics;

import lsfusion.server.logics.classes.ConcreteCustomClass;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.language.ScriptingLogicsModule;
import org.antlr.runtime.RecognitionException;

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

    public LCP canonicalNameActionScheduledTaskDetail;
    
    public LCP resultScheduledTaskLog;
    public LCP exceptionOccurredScheduledTaskLog;
    public LCP propertyScheduledTaskLog;
    public LCP dateScheduledTaskLog;
    public LCP scheduledTaskScheduledTaskLog;
    public LCP scheduledTaskLogScheduledClientTaskLog;
    public LCP messageScheduledClientTaskLog;
    public LCP lsfStackScheduledClientTaskLog;
    public LCP failedScheduledClientTaskLog;
    public LCP dateScheduledClientTaskLog;

    public LCP scriptText;
    public LAP evalScript;
    
    public LAP copyAction;

    public SchedulerLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(SchedulerLogicsModule.class.getResourceAsStream("/system/Scheduler.lsf"), "/system/Scheduler.lsf", baseLM, BL);
    }

    @Override
    public void initMetaAndClasses() throws RecognitionException {
        super.initMetaAndClasses();

        scheduledTask = (ConcreteCustomClass) findClass("ScheduledTask");
        scheduledTaskLog = (ConcreteCustomClass) findClass("ScheduledTaskLog");
        scheduledClientTaskLog = (ConcreteCustomClass) findClass("ScheduledClientTaskLog");
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        super.initMainLogic();

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

        canonicalNameActionScheduledTaskDetail = findProperty("canonicalNameAction[ScheduledTaskDetail]");

        resultScheduledTaskLog = findProperty("result[ScheduledTaskLog]");
        exceptionOccurredScheduledTaskLog = findProperty("exceptionOccurred[ScheduledTaskLog]");
        propertyScheduledTaskLog = findProperty("property[ScheduledTaskLog]");
        dateScheduledTaskLog = findProperty("date[ScheduledTaskLog]");
        scheduledTaskScheduledTaskLog = findProperty("scheduledTask[ScheduledTaskLog]");
        scheduledTaskLogScheduledClientTaskLog = findProperty("scheduledTaskLog[ScheduledClientTaskLog]");
        messageScheduledClientTaskLog = findProperty("message[ScheduledClientTaskLog]");
        lsfStackScheduledClientTaskLog = findProperty("lsfStack[ScheduledClientTaskLog]");
        failedScheduledClientTaskLog = findProperty("failed[ScheduledClientTaskLog]");
        dateScheduledClientTaskLog = findProperty("date[ScheduledClientTaskLog]");

        scriptText = findProperty("scriptText[]");
        evalScript = findAction("evalScript[]");

        copyAction = findAction("copyAction[]");
    }
}
