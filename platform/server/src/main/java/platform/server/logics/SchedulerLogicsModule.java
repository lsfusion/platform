package platform.server.logics;

import org.antlr.runtime.RecognitionException;
import platform.server.classes.ConcreteCustomClass;
import platform.server.logics.linear.LCP;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

public class SchedulerLogicsModule extends ScriptingLogicsModule {

    public ConcreteCustomClass scheduledTask;
    public ConcreteCustomClass scheduledTaskLog;
    public ConcreteCustomClass scheduledClientTaskLog;

    public LCP runAtStartScheduledTask;
    public LCP startDateScheduledTask;
    public LCP periodScheduledTask;
    public LCP activeScheduledTask;
    public LCP inScheduledTaskProperty;
    public LCP activeScheduledTaskProperty;
    public LCP orderScheduledTaskProperty;

    public LCP resultScheduledTaskLog;
    public LCP propertyScheduledTaskLog;
    public LCP dateStartScheduledTaskLog;
    public LCP dateFinishScheduledTaskLog;
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
        activeScheduledTask = getLCPByName("activeScheduledTask");
        inScheduledTaskProperty = getLCPByName("inScheduledTaskProperty");
        activeScheduledTaskProperty = getLCPByName("activeScheduledTaskProperty");
        orderScheduledTaskProperty = getLCPByName("orderScheduledTaskProperty");

        resultScheduledTaskLog = getLCPByName("resultScheduledTaskLog");
        propertyScheduledTaskLog = getLCPByName("propertyScheduledTaskLog");
        dateStartScheduledTaskLog = getLCPByName("dateStartScheduledTaskLog");
        dateFinishScheduledTaskLog = getLCPByName("dateFinishScheduledTaskLog");
        scheduledTaskScheduledTaskLog = getLCPByName("scheduledTaskScheduledTaskLog");
        scheduledTaskLogScheduledClientTaskLog = getLCPByName("scheduledTaskLogScheduledClientTaskLog");
        messageScheduledClientTaskLog = getLCPByName("messageScheduledClientTaskLog");
    }
}
