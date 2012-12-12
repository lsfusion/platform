package platform.server.logics;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.PropertyEditType;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.classes.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.DisconnectActionProperty;
import platform.server.session.DataSession;
import platform.server.session.PropertyChange;

import javax.swing.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import static platform.server.logics.ServerResourceBundle.getString;

public class SystemEventsLogicsModule<T extends BusinessLogics<T>> extends LogicsModule {
    Logger logger;
    T BL;

    public T getBL(){
        return BL;
    }

    public AbstractCustomClass exception;
    public ConcreteCustomClass clientException;
    public ConcreteCustomClass serverException;
    public ConcreteCustomClass launch;
    public ConcreteCustomClass connection;
    public StaticCustomClass connectionStatus;
    
    
    public LCP computerConnection;
    public LCP userConnection;
    public LCP userLoginConnection;
    public LCP<PropertyInterface> connectionStatusConnection;
    public LCP connectTimeConnection;
    public LCP disconnectTimeConnection;
    public LAP disconnectConnection;

    public LCP computerLaunch;
    public LCP hostnameLaunch;
    public LCP timeLaunch;
    public LCP revisionLaunch;

    public LCP messageException;
    public LCP dateException;
    public LCP erTraceException;
    public LCP typeException;
    public LCP clientClientException;
    public LCP loginClientException;
    
    public SystemEventsLogicsModule(T BL, BaseLogicsModule baseLM, Logger logger) {
        super("SystemEvents", "SystemEvents");
        setBaseLogicsModule(baseLM);
        this.BL = BL;
        this.logger = logger;
    }
    @Override
    public void initModuleDependencies() {
        setRequiredModules(Arrays.asList("System", "Reflection"));
    }

    @Override
    public void initModule() {
    }

    @Override
    public void initClasses() {
        initBaseClassAliases();
        exception = addAbstractClass("exception", getString("logics.exception"), baseLM.baseClass);
        clientException = addConcreteClass("clientException", getString("logics.exception.client"), exception);
        serverException = addConcreteClass("serverException", getString("logics.exception.server"), exception);
        launch = addConcreteClass("launch", getString("logics.launch"), baseLM.baseClass);
        connection = addConcreteClass("connection", getString("logics.connection"), baseLM.baseClass);
        connectionStatus = addStaticClass("connectionStatus", getString("logics.connection.status"),
                new String[]{"connectedConnection", "disconnectedConnection"},
                new String[]{getString("logics.connection.connected"), getString("logics.connection.disconnected")});
    }

    @Override
    public void initGroups() {
         initBaseGroupAliases();
    }

    @Override
    public void initTables() {
        addTable("connection", connection);
        addTable("exception", exception);
        addTable("launch", launch);
    }

    @Override
    public void initProperties() {
        // -------------------- Логирование сервера ----------------- //
        // SystemLogLogicsModule

        // Сессии
        LCP userSession = addDProp("userSession", getString("logics.session.user"), BL.LM.user, BL.LM.session);
        userSession.setEventChangeNew(BL.LM.currentUser, is(BL.LM.session), 1);
        addJProp(baseGroup, getString("logics.session.user"), BL.LM.name, userSession, 1);
        LCP dateSession = addDProp(baseGroup, "dateSession", getString("logics.session.date"), DateTimeClass.instance, BL.LM.session);
        dateSession.setEventChangeNew(BL.LM.currentDateTime, is(BL.LM.session), 1);

        // Подключения к серверу
        computerConnection = addDProp("computerConnection", getString("logics.computer"), BL.LM.computer, connection);
        addJProp(baseGroup, getString("logics.computer"), BL.LM.hostname, computerConnection, 1);
        userConnection = addDProp("userConnection", getString("logics.user"), BL.LM.customUser, connection);
        userLoginConnection = addJProp(baseGroup, getString("logics.user"), BL.LM.userLogin, userConnection, 1);
        connectionStatusConnection = addDProp("connectionStatusConnection", getString("logics.connection.status"), connectionStatus, connection);
        addJProp(baseGroup, getString("logics.connection.status"), BL.LM.name, connectionStatusConnection, 1);

        connectTimeConnection = addDProp(baseGroup, "connectTimeConnection", getString("logics.connection.connect.time"), DateTimeClass.instance, connection);
        disconnectTimeConnection = addDProp(baseGroup, "disconnectTimeConnection", getString("logics.connection.disconnect.time"), DateTimeClass.instance, connection);
        disconnectTimeConnection.setEventChangePrevSet(BL.LM.currentDateTime,
                addJProp(BL.LM.equals2, connectionStatusConnection, 1, addCProp(connectionStatus, "disconnectedConnection")), 1);
        disconnectConnection = addProperty(null, new LAP(new DisconnectActionProperty(BL, connection)));
        addIfAProp(baseGroup, getString("logics.connection.disconnect"), true, disconnectTimeConnection, 1, disconnectConnection, 1);

        // Логирование старта сервера
        computerLaunch = addDProp("computerLaunch", getString("logics.computer"), BL.LM.computer, launch);
        hostnameLaunch = addJProp(baseGroup, getString("logics.computer"), BL.LM.hostname, computerLaunch, 1);
        timeLaunch = addDProp(baseGroup, "launchConnectTime", getString("logics.launch.time"), DateTimeClass.instance, launch);
        revisionLaunch = addDProp(baseGroup, "revisionLaunch", getString("logics.launch.revision"), StringClass.get(10), launch);

        // Ошибки выполнения
        messageException = addDProp(baseGroup, "messageException", getString("logics.exception.message"), BL.reflectionLM.propertyCaptionValueClass, exception);
        dateException = addDProp(baseGroup, "dateException", getString("logics.exception.date"), DateTimeClass.instance, exception);
        erTraceException = addDProp(baseGroup, "erTraceException", getString("logics.exception.ertrace"), TextClass.instance, exception);
        erTraceException.setPreferredWidth(500);
        typeException =  addDProp(baseGroup, "typeException", getString("logics.exception.type"), BL.reflectionLM.propertyCaptionValueClass, exception);
        clientClientException = addDProp(baseGroup, "clientClientException", getString("logics.exception.client.client"), BL.reflectionLM.loginValueClass, clientException);
        loginClientException = addDProp(baseGroup, "loginClientException", getString("logics.exception.client.login"), BL.reflectionLM.loginValueClass, clientException);

        initNavigators();
    }

    private void initNavigators() {
        addFormEntity(new ExceptionsFormEntity(BL.LM.systemEvents, "exceptionsForm"));
        addFormEntity(new ConnectionsFormEntity(BL.LM.systemEvents, "connectionsForm"));
        addFormEntity(new LaunchesFormEntity(BL.LM.systemEvents, "launchesForm"));
    }
    
    @Override
    public void initIndexes() {
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    private class ConnectionsFormEntity extends FormEntity {
        protected ConnectionsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.connection.server.connections"));

            ObjectEntity objConnection = addSingleGroupObject(connection, baseGroup, true);
            ObjectEntity objForm = addSingleGroupObject(BL.reflectionLM.navigatorElement, getString("logics.forms.opened.forms"), baseGroup, true);

//            setEditType(baseGroup, PropertyEditType.READONLY);

            addPropertyDraw(objConnection, objForm, baseGroup, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(BL.reflectionLM.connectionFormCount, objConnection, objForm), Compare.GREATER, 0));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(connectionStatusConnection, objConnection), Compare.EQUALS, addPropertyObject(addCProp(connectionStatus, "connectedConnection"))),
                    getString("logics.connection.active.connections"),
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroup);
        }
    }

    protected void resetConnectionStatus() {
        try {
            DataSession session = BL.createSession();

            PropertyChange statusChanges = new PropertyChange(connectionStatus.getDataObject("disconnectedConnection"),
                    BaseUtils.single(connectionStatusConnection.property.interfaces), connectionStatus.getDataObject("connectedConnection"));

            session.change((CalcProperty) connectionStatusConnection.property, statusChanges);

            session.apply(this.BL);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    class ExceptionsFormEntity extends FormEntity {
        ObjectEntity objExceptions;

        protected ExceptionsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.tables.exceptions"));
            objExceptions = addSingleGroupObject(exception, getString("logics.tables.exceptions"), messageException, clientClientException, loginClientException, typeException, dateException);
            addPropertyDraw(erTraceException, objExceptions).forceViewType = ClassViewType.PANEL;
            setEditType(PropertyEditType.READONLY);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView textContainer = design.createContainer();
            textContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            textContainer.add(design.get(getPropertyDraw(erTraceException, objExceptions)));
            textContainer.constraints.fillHorizontal = 1.0;
            textContainer.constraints.fillVertical = 1.0;

            PropertyDrawView textView = design.get(getPropertyDraw(erTraceException, objExceptions));
            textView.constraints.fillHorizontal = 1.0;
            textView.constraints.fillVertical = 0.5;
            textView.preferredSize = new Dimension(-1, 200);
            textView.panelLabelAbove = true;

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objExceptions.groupTo));
            specContainer.add(design.getGroupObjectContainer(objExceptions.groupTo));
            specContainer.add(textContainer);

            return design;
        }

    }

    private class LaunchesFormEntity extends FormEntity {
        protected LaunchesFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.launch.log"));

            ObjectEntity objLaunch = addSingleGroupObject(launch, computerLaunch, hostnameLaunch, timeLaunch, revisionLaunch, launch);
            setEditType(PropertyEditType.READONLY);
        }
    }
    
}
