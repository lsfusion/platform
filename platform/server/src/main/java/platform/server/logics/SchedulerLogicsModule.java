package platform.server.logics;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.record.formula.functions.T;
import platform.interop.Compare;
import platform.interop.PropertyEditType;
import platform.interop.form.layout.ContainerType;
import platform.server.classes.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.linear.LCP;
import platform.server.logics.scripted.ScriptingLogicsModule;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Arrays;

import static platform.server.logics.ServerResourceBundle.getString;

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
        super(SchedulerLogicsModule.class.getResourceAsStream("/scripts/Scheduler.lsf"), baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();

        scheduledTask = (ConcreteCustomClass) getClassByName("scheduledTask");
        scheduledTaskLog = (ConcreteCustomClass) getClassByName("scheduledTaskLog");
        scheduledClientTaskLog = (ConcreteCustomClass) getClassByName("scheduledClientTaskLog");
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
