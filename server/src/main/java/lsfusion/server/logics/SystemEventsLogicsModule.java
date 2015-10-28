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
        computerConnection = findProperty("computerConnection");
        remoteAddressConnection = findProperty("remoteAddressConnection");
        userConnection = findProperty("userConnection");
        userLoginConnection = findProperty("userLoginConnection");
        osVersionConnection = findProperty("osVersionConnection");
        processorConnection = findProperty("processorConnection");
        architectureConnection = findProperty("architectureConnection");
        coresConnection = findProperty("coresConnection");
        physicalMemoryConnection = findProperty("physicalMemoryConnection");
        totalMemoryConnection = findProperty("totalMemoryConnection");
        maximumMemoryConnection = findProperty("maximumMemoryConnection");
        freeMemoryConnection = findProperty("freeMemoryConnection");
        javaVersionConnection = findProperty("javaVersionConnection");
        screenSizeConnection = findProperty("screenSizeConnection");
        connectionStatusConnection = (LCP<PropertyInterface>) findProperty("connectionStatusConnection");

        connectTimeConnection = findProperty("connectTimeConnection");
        disconnectConnection = findAction("disconnectConnection");
        //addIfAProp(baseGroup, "Отключить", true, findProperty("disconnectTimeConnection"), 1, disconnectConnection, 1);

        // Логирование старта сервера
        computerLaunch = findProperty("computerLaunch");
        timeLaunch = findProperty("timeLaunch");
        revisionLaunch = findProperty("revisionLaunch");

        // Ошибки выполнения
        messageException = findProperty("messageException");
        dateException = findProperty("dateException");
        erTraceException = findProperty("erTraceException");
        lsfTraceException = findProperty("lsfStackTraceException");
        typeException =  findProperty("typeException");
        clientClientException = findProperty("clientClientException");
        loginClientException = findProperty("loginClientException");
        reqIdHandledException = findProperty("reqIdHandledException");
        countNonFatalHandledException = findProperty("countNonFatalHandledException");
        abandonedNonFatalHandledException = findProperty("abandonedNonFatalHandledException");

        // Открытые формы во время подключения
        connectionFormCount = findProperty("connectionFormCount");

        // Сессия
        currentSession = findProperty("currentSession");
        connectionSession = findProperty("connectionSession");
        navigatorElementSession = findProperty("navigatorElementSession");
        quantityAddedClassesSession = findProperty("quantityAddedClassesSession");
        quantityRemovedClassesSession = findProperty("quantityRemovedClassesSession");
        quantityChangedClassesSession = findProperty("quantityChangedClassesSession");
        changesSession = findProperty("changesSession");
//        baseLM.objectClassName.makeLoggable(this, true);

        pingComputerDateTimeFromDateTimeTo = findProperty("pingComputerDateTimeFromDateTimeTo");
        minTotalMemoryComputerDateTimeFromDateTimeTo = findProperty("minTotalMemoryComputerDateTimeFromDateTimeTo");
        maxTotalMemoryComputerDateTimeFromDateTimeTo = findProperty("maxTotalMemoryComputerDateTimeFromDateTimeTo");
        minUsedMemoryComputerDateTimeFromDateTimeTo = findProperty("minUsedMemoryComputerDateTimeFromDateTimeTo");
        maxUsedMemoryComputerDateTimeFromDateTimeTo = findProperty("maxUsedMemoryComputerDateTimeFromDateTimeTo");
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
