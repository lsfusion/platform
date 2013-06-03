package lsfusion.server.logics;

import org.antlr.runtime.RecognitionException;
import lsfusion.base.DateConverter;
import lsfusion.server.classes.AbstractCustomClass;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.DisconnectActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class SystemEventsLogicsModule extends ScriptingLogicsModule {

    private final AuthenticationLogicsModule authenticationLM;

    public AbstractCustomClass exception;
    public ConcreteCustomClass clientException;
    public ConcreteCustomClass serverException;
    public ConcreteCustomClass launch;
    public ConcreteCustomClass connection;
    public ConcreteCustomClass connectionStatus;
    public ConcreteCustomClass session;

    public LCP computerConnection;
    public LCP remoteAddressConnection;
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
        super(SystemEventsLogicsModule.class.getResourceAsStream("/scripts/system/SystemEvents.lsf"), baseLM, BL);
        setBaseLogicsModule(baseLM);
        this.authenticationLM = BL.authenticationLM;
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();

        clientException = (ConcreteCustomClass) getClassByName("ClientException");
        serverException = (ConcreteCustomClass) getClassByName("ServerException");
        launch = (ConcreteCustomClass) getClassByName("Launch");
        connection = (ConcreteCustomClass) getClassByName("Connection");
        connectionStatus = (ConcreteCustomClass) getClassByName("ConnectionStatus");
        session = (ConcreteCustomClass) getClassByName("Session");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        // Подключения к серверу
        computerConnection = getLCPByName("computerConnection");
        remoteAddressConnection = getLCPByName("remoteAddressConnection");
        userConnection = getLCPByName("userConnection");
        userLoginConnection = getLCPByName("userLoginConnection");
        connectionStatusConnection = (LCP<PropertyInterface>) getLCPByName("connectionStatusConnection");

        connectTimeConnection = getLCPByName("connectTimeConnection");
        disconnectConnection = addProperty(null, new LAP(new DisconnectActionProperty(connection)));
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
//        baseLM.objectClassName.makeLoggable(this, true);
    }

    public void logException(BusinessLogics bl, String message, String errorType, String erTrace, DataObject user, String clientName, boolean client) throws SQLException {
        DataSession session = createSession();
        DataObject exceptionObject;
        if (client) {
            exceptionObject = session.addObject(clientException);
            clientClientException.change(clientName, session, exceptionObject);
            String userLogin = (String) authenticationLM.loginCustomUser.read(session, user);
            loginClientException.change(userLogin, session, exceptionObject);
        } else {
            exceptionObject = session.addObject(serverException);
        }
        messageException.change(message, session, exceptionObject);
        typeException.change(errorType, session, exceptionObject);
        erTraceException.change(erTrace, session, exceptionObject);
        dateException.change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, exceptionObject);

        session.apply(bl);
        session.close();
    }
}
