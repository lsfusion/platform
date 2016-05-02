package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.DateConverter;
import lsfusion.base.ExceptionUtils;
import lsfusion.interop.exceptions.FatalHandledRemoteException;
import lsfusion.interop.exceptions.HandledRemoteException;
import lsfusion.interop.exceptions.NonFatalHandledRemoteException;
import lsfusion.interop.exceptions.RemoteServerException;
import lsfusion.server.classes.AbstractCustomClass;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;
import lsfusion.server.stack.ExecutionStackAspect;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Calendar;

public class SystemEventsLogicsModule extends ScriptingLogicsModule {

    private final AuthenticationLogicsModule authenticationLM;

    public AbstractCustomClass exception;
    public ConcreteCustomClass clientException;
    public ConcreteCustomClass remoteServerException;
    public ConcreteCustomClass fatalHandledRemoteException;
    public ConcreteCustomClass nonFatalHandledRemoteException;
    public ConcreteCustomClass unhandledRemoteException;
    public ConcreteCustomClass serverException;
    public ConcreteCustomClass launch;
    public ConcreteCustomClass connection;
    public ConcreteCustomClass connectionStatus;
    public ConcreteCustomClass session;

    public LCP computerConnection;
    public LCP remoteAddressConnection;
    public LCP userConnection;
    public LCP userLoginConnection;
    public LCP osVersionConnection;
    public LCP processorConnection;
    public LCP architectureConnection;
    public LCP coresConnection;
    public LCP physicalMemoryConnection;
    public LCP totalMemoryConnection;
    public LCP maximumMemoryConnection;
    public LCP freeMemoryConnection;
    public LCP javaVersionConnection;
    public LCP screenSizeConnection;
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
    public LCP lsfTraceException;
    public LCP typeException;
    public LCP clientClientException;
    public LCP loginClientException;

    private LCP reqIdHandledException;
    private LCP countNonFatalHandledException;
    private LCP abandonedNonFatalHandledException;

    public LCP connectionFormCount;

    public LCP currentSession;
    public LCP connectionSession;
    public LCP navigatorElementSession;
    public LCP quantityAddedClassesSession;
    public LCP quantityRemovedClassesSession;
    public LCP quantityChangedClassesSession;
    public LCP changesSession;

    public LCP pingComputerDateTimeFromDateTimeTo;
    public LCP minTotalMemoryComputerDateTimeFromDateTimeTo;
    public LCP maxTotalMemoryComputerDateTimeFromDateTimeTo;
    public LCP minUsedMemoryComputerDateTimeFromDateTimeTo;;
    public LCP maxUsedMemoryComputerDateTimeFromDateTimeTo;

    public LAP onClientStarted;

    public SystemEventsLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(SystemEventsLogicsModule.class.getResourceAsStream("/lsfusion/system/SystemEvents.lsf"), "/lsfusion/system/SystemEvents.lsf", baseLM, BL);
        setBaseLogicsModule(baseLM);
        this.authenticationLM = BL.authenticationLM;
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();

        clientException = (ConcreteCustomClass) findClass("ClientException");
        remoteServerException = (ConcreteCustomClass) findClass("RemoteServerException");
        fatalHandledRemoteException = (ConcreteCustomClass) findClass("FatalHandledException");
        nonFatalHandledRemoteException = (ConcreteCustomClass) findClass("NonFatalHandledException");
        unhandledRemoteException = (ConcreteCustomClass) findClass("UnhandledException");
        serverException = (ConcreteCustomClass) findClass("ServerException");
        launch = (ConcreteCustomClass) findClass("Launch");
        connection = (ConcreteCustomClass) findClass("Connection");
        connectionStatus = (ConcreteCustomClass) findClass("ConnectionStatus");
        session = (ConcreteCustomClass) findClass("Session");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        // Подключения к серверу
        computerConnection = findProperty("computer[Connection]");
        remoteAddressConnection = findProperty("remoteAddress[Connection]");
        userConnection = findProperty("user[Connection]");
        userLoginConnection = findProperty("userLogin[Connection]");
        osVersionConnection = findProperty("osVersion[Connection]");
        processorConnection = findProperty("processor[Connection]");
        architectureConnection = findProperty("architecture[Connection]");
        coresConnection = findProperty("cores[Connection]");
        physicalMemoryConnection = findProperty("physicalMemory[Connection]");
        totalMemoryConnection = findProperty("totalMemory[Connection]");
        maximumMemoryConnection = findProperty("maximumMemory[Connection]");
        freeMemoryConnection = findProperty("freeMemory[Connection]");
        javaVersionConnection = findProperty("javaVersion[Connection]");
        screenSizeConnection = findProperty("screenSize[Connection]");
        connectionStatusConnection = (LCP<PropertyInterface>) findProperty("connectionStatus[Connection]");

        connectTimeConnection = findProperty("connectTime[Connection]");
        disconnectConnection = findAction("disconnect[Connection]");
        //addIfAProp(baseGroup, "Отключить", true, findProperty("disconnectTimeConnection"), 1, disconnectConnection, 1);

        // Логирование старта сервера
        computerLaunch = findProperty("computer[Launch]");
        timeLaunch = findProperty("time[Launch]");
        revisionLaunch = findProperty("revision[Launch]");

        // Ошибки выполнения
        messageException = findProperty("message[Exception]");
        dateException = findProperty("date[Exception]");
        erTraceException = findProperty("erTrace[Exception]");
        lsfTraceException = findProperty("lsfStackTrace[Exception]");
        typeException =  findProperty("type[Exception]");
        clientClientException = findProperty("client[ClientException]");
        loginClientException = findProperty("login[ClientException]");
        reqIdHandledException = findProperty("reqId[HandledException]");
        countNonFatalHandledException = findProperty("count[NonFatalHandledException]");
        abandonedNonFatalHandledException = findProperty("abandoned[NonFatalHandledException]");

        // Открытые формы во время подключения
        connectionFormCount = findProperty("connectionFormCount[Connection,NavigatorElement]");

        // Сессия
        currentSession = findProperty("currentSession[]");
        connectionSession = findProperty("connection[Session]");
        navigatorElementSession = findProperty("navigatorElement[Session]");
        quantityAddedClassesSession = findProperty("quantityAddedClasses[Session]");
        quantityRemovedClassesSession = findProperty("quantityRemovedClasses[Session]");
        quantityChangedClassesSession = findProperty("quantityChangedClasses[Session]");
        changesSession = findProperty("changes[Session]");
//        baseLM.objectClassName.makeLoggable(this, true);

        pingComputerDateTimeFromDateTimeTo = findProperty("pingFromTo[Computer,DATETIME,DATETIME]");
        minTotalMemoryComputerDateTimeFromDateTimeTo = findProperty("minTotalMemoryFromTo[Computer,DATETIME,DATETIME]");
        maxTotalMemoryComputerDateTimeFromDateTimeTo = findProperty("maxTotalMemoryFromTo[Computer,DATETIME,DATETIME]");
        minUsedMemoryComputerDateTimeFromDateTimeTo = findProperty("minUsedMemoryFromTo[Computer,DATETIME,DATETIME]");
        maxUsedMemoryComputerDateTimeFromDateTimeTo = findProperty("maxUsedMemoryFromTo[Computer,DATETIME,DATETIME]");

        onClientStarted = findAction("onClientStarted[]");
    }

    public void logException(BusinessLogics bl, Throwable t, DataObject user, String clientName, boolean client) throws SQLException, SQLHandledException {
        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        String message = Throwables.getRootCause(t).getLocalizedMessage();
        String errorType = t.getClass().getName();
        String erTrace = ExceptionUtils.getStackTraceString(t);
        String lsfStack = ExecutionStackAspect.getExceptionStackString();

        try (DataSession session = createSession()) {
            DataObject exceptionObject;
            if (client) {
                if (t instanceof RemoteServerException) {
                    exceptionObject = session.addObject(remoteServerException);
                } else if (t instanceof RemoteException) {
                    exceptionObject = session.addObject(unhandledRemoteException);
                } else if (t instanceof HandledRemoteException) {
                    HandledRemoteException handled = (HandledRemoteException) t;

                    if (t instanceof FatalHandledRemoteException)
                        exceptionObject = session.addObject(fatalHandledRemoteException);
                    else {
                        exceptionObject = session.addObject(nonFatalHandledRemoteException);

                        NonFatalHandledRemoteException nonFatal = (NonFatalHandledRemoteException) t;
                        countNonFatalHandledException.change(nonFatal.count, session, exceptionObject);
                        abandonedNonFatalHandledException.change(nonFatal.abandoned, session, exceptionObject);
                    }

                    reqIdHandledException.change(handled.reqId, session, exceptionObject);
                } else {
                    exceptionObject = session.addObject(clientException);
                }
                clientClientException.change(clientName, session, exceptionObject);
                String userLogin = (String) authenticationLM.loginCustomUser.read(session, user);
                loginClientException.change(userLogin, session, exceptionObject);
            } else {
                exceptionObject = session.addObject(serverException);
            }
            messageException.change(message, session, exceptionObject);
            typeException.change(errorType, session, exceptionObject);
            erTraceException.change(erTrace, session, exceptionObject);
            lsfTraceException.change(lsfStack, session, exceptionObject);
            dateException.change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, exceptionObject);

            session.apply(bl);
        }
    }
}
