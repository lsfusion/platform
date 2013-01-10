package platform.server.logics;

import org.antlr.runtime.RecognitionException;
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
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.CurrentSessionDataProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.DisconnectActionProperty;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import platform.server.session.PropertyChange;

import javax.swing.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Arrays;

import static platform.server.logics.ServerResourceBundle.getString;

public class SystemEventsLogicsModule  extends ScriptingLogicsModule {

    BusinessLogics BL;
    BaseLogicsModule baseLM;

    public AbstractCustomClass exception;
    public ConcreteCustomClass clientException;
    public ConcreteCustomClass serverException;
    public ConcreteCustomClass launch;
    public ConcreteCustomClass connection;
    public StaticCustomClass connectionStatus;
    public ConcreteCustomClass session;

    public LCP computerConnection;
    public LCP userConnection;
    public LCP userLoginConnection;
    public LCP<PropertyInterface> connectionStatusConnection;
    public LCP connectTimeConnection;
    public LCP disconnectTimeConnection;
    public LAP disconnectConnection;

    public LCP computerLaunch;
    public LCP timeLaunch;
    public LCP revisionLaunch;

    public LCP messageException;
    public LCP dateException;
    public LCP erTraceException;
    public LCP typeException;
    public LCP clientClientException;
    public LCP loginClientException;

    public LCP connectionFormCount;

    public LCP currentSession;
    public LCP connectionSession;
    public LCP navigatorElementSession;
    public LCP quantityAddedClassesSession;
    public LCP quantityRemovedClassesSession;
    public LCP quantityChangedClassesSession;
    public LCP changesSession;

    public SystemEventsLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(SystemEventsLogicsModule.class.getResourceAsStream("/scripts/SystemEvents.lsf"), baseLM, BL);
        setBaseLogicsModule(baseLM);
        this.BL = BL;
        this.baseLM = baseLM;
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();

        clientException = (ConcreteCustomClass) getClassByName("clientException");
        serverException = (ConcreteCustomClass) getClassByName("serverException");
        launch = (ConcreteCustomClass) getClassByName("launch");
        connection = (ConcreteCustomClass) getClassByName("connection");
        connectionStatus = (StaticCustomClass) getClassByName("connectionStatus");
        session = (ConcreteCustomClass) getClassByName("session");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        // Подключения к серверу
        computerConnection = getLCPByName("computerConnection");
        userConnection = getLCPByName("userConnection");
        userLoginConnection = getLCPByName("userLoginConnection");
        connectionStatusConnection = (LCP<PropertyInterface>) getLCPByName("connectionStatusConnection");

        connectTimeConnection = getLCPByName("connectTimeConnection");
        disconnectConnection = addProperty(null, new LAP(new DisconnectActionProperty(BL, connection)));
        addIfAProp(baseGroup, getString("logics.connection.disconnect"), true, getLCPByName("disconnectTimeConnection"), 1, disconnectConnection, 1);

        // Логирование старта сервера
        computerLaunch = getLCPByName("computerLaunch");
        timeLaunch = getLCPByName("timeLaunch");
        revisionLaunch = getLCPByName("revisionLaunch");

        // Ошибки выполнения
        messageException = getLCPByName("messageException");
        dateException = getLCPByName("dateException");
        erTraceException = getLCPByName("erTraceException");
        typeException =  getLCPByName("typeException");
        clientClientException = getLCPByName("clientClientException");
        loginClientException = getLCPByName("loginClientException");

        // Открытые формы во время подключения
        connectionFormCount = getLCPByName("connectionFormCount");

        // Сессия
        currentSession = getLCPByName("currentSession");
        connectionSession = getLCPByName("connectionSession");
        navigatorElementSession = getLCPByName("navigatorElementSession");
        quantityAddedClassesSession = getLCPByName("quantityAddedClassesSession");
        quantityRemovedClassesSession = getLCPByName("quantityRemovedClassesSession");
        quantityChangedClassesSession = getLCPByName("quantityChangedClassesSession");
        changesSession = getLCPByName("changesSession");
        baseLM.objectClassName.makeLoggable(baseLM, true);
    }

    protected void resetConnectionStatus() {
        try {
            DataSession session = BL.createSession();

            PropertyChange statusChanges = new PropertyChange(connectionStatus.getDataObject("disconnectedConnection"),
                    connectionStatusConnection.property.interfaces.single(), connectionStatus.getDataObject("connectedConnection"));

            session.change((CalcProperty) connectionStatusConnection.property, statusChanges);

            session.apply(this.BL);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
