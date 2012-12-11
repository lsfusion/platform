package platform.server.logics;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.PropertyEditType;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.classes.*;
import platform.server.data.Union;
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

import static platform.server.logics.ServerResourceBundle.getString;

/**
 * User: DAle
 * Date: 16.05.11
 * Time: 17:52
 */

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
    
    
    public LCP connectionComputer;
    public LCP connectionUser;
    public LCP userNameConnection;
    public LCP<PropertyInterface> connectionCurrentStatus;
    public LCP connectionConnectTime;
    public LCP connectionDisconnectTime;
    public LAP disconnectConnection;

    public LCP launchComputer;
    public LCP computerNameLaunch;
    public LCP launchTime;
    public LCP launchRevision;

    public LCP messageException;
    public LCP dateException;
    public LCP erTraceException;
    public LCP typeException;
    public LCP clientClientException;
    public LCP loginClientException;
    
    public SystemEventsLogicsModule(T BL, BaseLogicsModule baseLM, Logger logger) {
        super("SystemEvents", "SystemEvents");
        setBaseLogicsModule(baseLM);
        //setSystemEventsLogicsModule(BL.systemEventsLM);
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
        LCP sessionUser = addDProp("sessionUser", getString("logics.session.user"), BL.LM.user, BL.LM.session);
        sessionUser.setEventChangeNew(BL.LM.currentUser, is(BL.LM.session), 1);
        addJProp(baseGroup, getString("logics.session.user"), BL.LM.name, sessionUser, 1);
        LCP sessionDate = addDProp(baseGroup, "sessionDate", getString("logics.session.date"), DateTimeClass.instance, BL.LM.session);
        sessionDate.setEventChangeNew(BL.LM.currentDateTime, is(BL.LM.session), 1);

        // Подключения к серверу
        connectionComputer = addDProp("connectionComputer", getString("logics.computer"), BL.LM.computer, connection);
        addJProp(baseGroup, getString("logics.computer"), BL.LM.hostname, connectionComputer, 1);
        connectionUser = addDProp("connectionUser", getString("logics.user"), BL.LM.customUser, connection);
        userNameConnection = addJProp(baseGroup, getString("logics.user"), BL.LM.userLogin, connectionUser, 1);
        connectionCurrentStatus = addDProp("connectionCurrentStatus", getString("logics.connection.status"), connectionStatus, connection);
        addJProp(baseGroup, getString("logics.connection.status"), BL.LM.name, connectionCurrentStatus, 1);

        connectionConnectTime = addDProp(baseGroup, "connectionConnectTime", getString("logics.connection.connect.time"), DateTimeClass.instance, connection);
        connectionDisconnectTime = addDProp(baseGroup, "connectionDisconnectTime", getString("logics.connection.disconnect.time"), DateTimeClass.instance, connection);
        connectionDisconnectTime.setEventChangePrevSet(BL.LM.currentDateTime,
                addJProp(BL.LM.equals2, connectionCurrentStatus, 1, addCProp(connectionStatus, "disconnectedConnection")), 1);
        disconnectConnection = addProperty(null, new LAP(new DisconnectActionProperty(BL, connection)));
        addIfAProp(baseGroup, getString("logics.connection.disconnect"), true, connectionDisconnectTime, 1, disconnectConnection, 1);

        // Логирование старта сервера
        launchComputer = addDProp("launchComputer", getString("logics.computer"), BL.LM.computer, launch);
        computerNameLaunch = addJProp(baseGroup, getString("logics.computer"), BL.LM.hostname, launchComputer, 1);
        launchTime = addDProp(baseGroup, "launchConnectTime", getString("logics.launch.time"), DateTimeClass.instance, launch);
        launchRevision = addDProp(baseGroup, "launchRevision", getString("logics.launch.revision"), StringClass.get(10), launch);

        // Ошибки выполнения
        messageException = addDProp(baseGroup, "messageException", getString("logics.exception.message"), BL.LM.propertyCaptionValueClass, exception);
        dateException = addDProp(baseGroup, "dateException", getString("logics.exception.date"), DateTimeClass.instance, exception);
        erTraceException = addDProp(baseGroup, "erTraceException", getString("logics.exception.ertrace"), TextClass.instance, exception);
        erTraceException.setPreferredWidth(500);
        typeException =  addDProp(baseGroup, "typeException", getString("logics.exception.type"), BL.LM.propertyCaptionValueClass, exception);
        clientClientException = addDProp(baseGroup, "clientClientException", getString("logics.exception.client.client"), BL.LM.loginValueClass, clientException);
        loginClientException = addDProp(baseGroup, "loginClientException", getString("logics.exception.client.login"), BL.LM.loginValueClass, clientException);

        

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
                    new CompareFilterEntity(addPropertyObject(connectionCurrentStatus, objConnection), Compare.EQUALS, addPropertyObject(addCProp(connectionStatus, "connectedConnection"))),
                    getString("logics.connection.active.connections"),
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroup);
        }
    }

    protected void resetConnectionStatus() {
        try {
            DataSession session = BL.createSession();

            PropertyChange statusChanges = new PropertyChange(connectionStatus.getDataObject("disconnectedConnection"),
                    BaseUtils.single(connectionCurrentStatus.property.interfaces), connectionStatus.getDataObject("connectedConnection"));

            session.change((CalcProperty) connectionCurrentStatus.property, statusChanges);

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

            ObjectEntity objLaunch = addSingleGroupObject(launch, baseGroup, true);
            setEditType(PropertyEditType.READONLY);
        }
    }
    
}
