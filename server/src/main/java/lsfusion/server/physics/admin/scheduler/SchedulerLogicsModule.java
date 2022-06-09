package lsfusion.server.physics.admin.scheduler;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;

public class SchedulerLogicsModule extends ScriptingLogicsModule {

    public ConcreteCustomClass userScheduledTask;
    public ConcreteCustomClass scheduledTaskLog;
    public ConcreteCustomClass scheduledClientTaskLog;

    public LP nameScheduledTask;
    public LP runAtStartScheduledTask;
    public LP startDateScheduledTask;
    public LP timeFromScheduledTask;
    public LP timeToScheduledTask;
    public LP periodScheduledTask;
    public LP schedulerStartTypeScheduledTask;
    public LP activeScheduledTask;
    public LP daysOfWeekScheduledTask;
    public LP daysOfMonthScheduledTask;
    public LP ignoreExceptionsScheduledTaskDetail;
    public LP activeScheduledTaskDetail;
    public LP orderScheduledTaskDetail;
    public LP scriptScheduledTaskDetail;
    public LP timeoutScheduledTaskDetail;
    public LP parameterScheduledTaskDetail;
    public LP scheduledTaskScheduledTaskDetail;

    public LP canonicalNameActionScheduledTaskDetail;
    
    public LP resultScheduledTaskLog;
    public LP exceptionOccurredScheduledTaskLog;
    public LP propertyScheduledTaskLog;
    public LP dateScheduledTaskLog;
    public LP scheduledTaskScheduledTaskLog;
    public LP scheduledTaskLogScheduledClientTaskLog;
    public LP messageScheduledClientTaskLog;
    public LP lsfStackScheduledClientTaskLog;
    public LP failedScheduledClientTaskLog;
    public LP dateScheduledClientTaskLog;

    public LP scriptText;
    public LA evalScript;

    public SchedulerLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(baseLM, BL, "/system/Scheduler.lsf");
    }

    @Override
    public void initMetaAndClasses() throws RecognitionException {
        super.initMetaAndClasses();

        userScheduledTask = (ConcreteCustomClass) findClass("UserScheduledTask");
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
    }
}
