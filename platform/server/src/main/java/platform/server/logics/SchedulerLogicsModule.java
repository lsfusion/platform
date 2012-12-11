package platform.server.logics;

import org.apache.log4j.Logger;
import platform.interop.ClassViewType;
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
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.actions.GenerateLoginPasswordActionProperty;
import platform.server.logics.property.group.AbstractGroup;

import javax.swing.*;
import java.awt.event.KeyEvent;

import static platform.server.logics.ServerResourceBundle.getString;

public class SchedulerLogicsModule<T extends BusinessLogics<T>> extends LogicsModule {
    Logger logger;
    T BL;

    public T getBL(){
        return BL;
    }

    public ConcreteCustomClass scheduledTask;
    public ConcreteCustomClass scheduledTaskLog;
    public ConcreteCustomClass scheduledClientTaskLog;
    
    public LCP nameScheduledTask;
    public LCP runAtStartScheduledTask;
    public LCP startDateScheduledTask;
    public LCP periodScheduledTask;
    public LCP activeScheduledTask;
    public LCP inScheduledTaskProperty;
    public LCP activeScheduledTaskProperty;
    public LCP orderScheduledTaskProperty;
    public LCP propertyScheduledTaskLog;
    public LCP resultScheduledTaskLog;
    public LCP dateStartScheduledTaskLog;
    public LCP dateFinishScheduledTaskLog;
    public LCP scheduledTaskScheduledTaskLog;
    public LCP currentScheduledTaskLogScheduledTask;
    public LCP messageScheduledClientTaskLog;
    public LCP scheduledTaskLogScheduledClientTaskLog;

    public SchedulerLogicsModule(T BL, BaseLogicsModule baseLM, Logger logger) {
        super("Scheduler", "Scheduler");
        setBaseLogicsModule(baseLM);
        this.BL = BL;
        this.logger = logger;
    }
    @Override
    public void initModuleDependencies() {
    }

    @Override
    public void initModule() {
    }

    @Override
    public void initClasses() {
        scheduledTask = addConcreteClass("scheduledTask", getString("logics.scheduled.task"), baseLM.baseClass);
        scheduledTaskLog = addConcreteClass("scheduledTaskLog", getString("logics.scheduled.task.log"), baseLM.baseClass);
        scheduledClientTaskLog = addConcreteClass("scheduledClientTaskLog", getString("logics.scheduled.task.log.client"), baseLM.baseClass);

    }

    @Override
    public void initGroups() {
    }

    @Override
    public void initTables() {
        addTable("scheduledTask", scheduledTask);
        addTable("scheduledTaskLog", scheduledTaskLog);
        addTable("scheduledClientTaskLog", scheduledClientTaskLog);
        addTable("scheduledTaskProperty", scheduledTask, BL.reflectionLM.property);
        addTable("scheduledTaskScheduledTaskLog", scheduledTask, scheduledTaskLog);
    }

    @Override
    public void initProperties() {

        // SchedulerLogicsModule
        // ----- Планировщик ----------- //
        nameScheduledTask = addDProp(baseGroup, "nameScheduledTask", getString("logics.scheduled.task.name"), StringClass.get(100), scheduledTask);
        runAtStartScheduledTask = addDProp(baseGroup, "runAtStartScheduledTask", getString("logics.scheduled.task.run.at.start"), LogicalClass.instance, scheduledTask);
        startDateScheduledTask = addDProp(baseGroup, "startDateScheduledTask", getString("logics.scheduled.task.start.date"), DateTimeClass.instance, scheduledTask);
        periodScheduledTask = addDProp(baseGroup, "periodScheduledTask", getString("logics.scheduled.task.period"), IntegerClass.instance, scheduledTask);
        activeScheduledTask = addDProp(baseGroup, "activeScheduledTask", getString("logics.scheduled.task.active"), LogicalClass.instance, scheduledTask);
        inScheduledTaskProperty = addDProp(baseGroup, "inScheduledTaskProperty", getString("logics.scheduled.task.enable"), LogicalClass.instance, scheduledTask, BL.reflectionLM.property);
        activeScheduledTaskProperty = addDProp(baseGroup, "activeScheduledTaskProperty", getString("logics.scheduled.task.active"), LogicalClass.instance, scheduledTask, BL.reflectionLM.property);
        orderScheduledTaskProperty = addDProp(baseGroup, "orderScheduledTaskProperty", getString("logics.scheduled.task.order"), IntegerClass.instance, scheduledTask, BL.reflectionLM.property);

        resultScheduledTaskLog = addDProp(baseGroup, "resultScheduledTaskLog", getString("logics.scheduled.task.result"), StringClass.get(200), scheduledTaskLog);
        propertyScheduledTaskLog = addDProp(baseGroup, "propertyScheduledTaskLog", getString("logics.scheduled.task.property"), StringClass.get(200), scheduledTaskLog);
        dateStartScheduledTaskLog = addDProp(baseGroup, "dateStartScheduledTaskLog", getString("logics.scheduled.task.date.start"), DateTimeClass.instance, scheduledTaskLog);
        dateFinishScheduledTaskLog = addDProp(baseGroup, "dateFinishScheduledTaskLog", getString("logics.scheduled.task.date.finish"), DateTimeClass.instance, scheduledTaskLog);
        scheduledTaskScheduledTaskLog = addDProp(baseGroup, "scheduledTaskScheduledTaskLog", getString("logics.scheduled.task"), scheduledTask, scheduledTaskLog);
        currentScheduledTaskLogScheduledTask = addDProp(baseGroup, "currentScheduledTaskLogScheduledTask", getString("logics.scheduled.task.log.current"), IntegerClass.instance, scheduledTask);
        scheduledTaskLogScheduledClientTaskLog = addDProp(baseGroup, "scheduledTaskLogScheduledClientTaskLog", getString("logics.scheduled.task.log"), scheduledTaskLog, scheduledClientTaskLog);
        messageScheduledClientTaskLog = addDProp(baseGroup, "messageScheduledClientTaskLog", getString("logics.scheduled.task.log.message"), StringClass.get(200), scheduledClientTaskLog);

        initNavigators();
    }

    private void initNavigators() {
        addFormEntity(new ScheduledTaskFormEntity(BL.LM.configuration, "scheduledTask"));
    }
    
    @Override
    public void initIndexes() {
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    public class ScheduledTaskFormEntity extends FormEntity {

        private ObjectEntity objScheduledTask;
        private ObjectEntity objProperty;
        private ObjectEntity objScheduledTaskLog;
        private ObjectEntity objScheduledClientTaskLog;

        public ScheduledTaskFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.scheduled.task.tasks"));

            objScheduledTask = addSingleGroupObject(scheduledTask, getString("logics.scheduled.task"));
            objProperty = addSingleGroupObject(BL.reflectionLM.property, getString("logics.property.properties"));
            objScheduledTaskLog = addSingleGroupObject(BL.schedulerLM.scheduledTaskLog, getString("logics.scheduled.task.log"));
            objScheduledClientTaskLog = addSingleGroupObject(BL.schedulerLM.scheduledClientTaskLog, getString("logics.scheduled.task.log.client"));

            addPropertyDraw(objScheduledTask, objProperty, inScheduledTaskProperty, activeScheduledTaskProperty, orderScheduledTaskProperty);
            addPropertyDraw(objScheduledTask, activeScheduledTask, nameScheduledTask, startDateScheduledTask, periodScheduledTask, runAtStartScheduledTask);
            addObjectActions(this, objScheduledTask);
            addPropertyDraw(objProperty, BL.reflectionLM.captionProperty, BL.reflectionLM.SIDProperty, BL.reflectionLM.classProperty, BL.reflectionLM.returnProperty);
            addPropertyDraw(objScheduledTaskLog, propertyScheduledTaskLog, resultScheduledTaskLog, dateStartScheduledTaskLog, dateFinishScheduledTaskLog);
            addPropertyDraw(objScheduledClientTaskLog, messageScheduledClientTaskLog);
            setEditType(BL.reflectionLM.captionProperty, PropertyEditType.READONLY);
            setEditType(BL.reflectionLM.SIDProperty, PropertyEditType.READONLY);
            setEditType(BL.reflectionLM.classProperty, PropertyEditType.READONLY);
            setEditType(BL.reflectionLM.returnProperty, PropertyEditType.READONLY);
            setEditType(objScheduledTaskLog, PropertyEditType.READONLY);
            setEditType(objScheduledClientTaskLog, PropertyEditType.READONLY);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(scheduledTaskScheduledTaskLog, objScheduledTaskLog), Compare.EQUALS, objScheduledTask));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(scheduledTaskLogScheduledClientTaskLog, objScheduledClientTaskLog), Compare.EQUALS, objScheduledTaskLog));
            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(
                    new RegularFilterEntity(genID(),
                            new NotNullFilterEntity(addPropertyObject(inScheduledTaskProperty, objScheduledTask, objProperty)),
                            getString("logics.only.checked"),
                            KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)
                    ), true);
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView specContainer = design.createContainer();
            ContainerView bottomContainer = design.createContainer();
            bottomContainer.add(design.getGroupObjectContainer(objProperty.groupTo));

            ContainerView logContainer = design.createContainer("Лог");
            logContainer.add(design.getGroupObjectContainer(objScheduledTaskLog.groupTo));
            logContainer.add(design.getGroupObjectContainer(objScheduledClientTaskLog.groupTo));

            bottomContainer.add(logContainer);
            bottomContainer.type = ContainerType.TABBED_PANE;

            specContainer.add(design.getGroupObjectContainer(objScheduledTask.groupTo));
            specContainer.add(bottomContainer);
            specContainer.type = ContainerType.SPLIT_PANE_VERTICAL;

            design.getMainContainer().add(0, specContainer);
            return design;
        }
    }
}
