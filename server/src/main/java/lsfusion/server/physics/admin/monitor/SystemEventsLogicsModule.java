package lsfusion.server.physics.admin.monitor;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.interop.base.exception.*;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.user.AbstractCustomClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.admin.authentication.property.CurrentConnectionProperty;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;

public class SystemEventsLogicsModule extends ScriptingLogicsModule {

    private final AuthenticationLogicsModule authenticationLM;

    public AbstractCustomClass exception;
    public ConcreteCustomClass clientException;
    public ConcreteCustomClass webClientException;
    public ConcreteCustomClass remoteServerException;
    public ConcreteCustomClass fatalHandledRemoteException;
    public ConcreteCustomClass nonFatalHandledRemoteException;
    public ConcreteCustomClass unhandledRemoteException;
    public ConcreteCustomClass serverException;
    public ConcreteCustomClass launch;
    public ConcreteCustomClass connection;
    public ConcreteCustomClass clientType;
    public ConcreteCustomClass connectionStatus;
    public ConcreteCustomClass session;

    public LA<?> onStarted;

    public LP useBootstrap;

    public LP computerConnection;
    public LP remoteAddressConnection;
    public LP webHostConnection;
    public LP webPortConnection;
    public LP contextPathConnection;
    public LP userConnection;
    public LP userLoginConnection;
    public LP osVersionConnection;
    public LP processorConnection;
    public LP architectureConnection;
    public LP coresConnection;
    public LP physicalMemoryConnection;
    public LP totalMemoryConnection;
    public LP maximumMemoryConnection;
    public LP freeMemoryConnection;
    public LP javaVersionConnection;
    public LP is64JavaConnection;
    public LP screenSizeConnection;
    public LP clientTypeConnection;
    public LP<PropertyInterface> connectionStatusConnection;
    public LP connectTimeConnection;
    public LP launchConnection;
    public LP lastActivity;

    public LP currentConnection;

    public LP currentLaunch;

    public LP messageException;
    public LP dateException;
    public LP erTraceException;
    public LP lsfTraceException;
    public LP typeException;
    public LP clientClientException;
    public LP loginClientException;

    private LP reqIdHandledException;
    private LP countNonFatalHandledException;
    private LP abandonedNonFatalHandledException;

    public LP connectionFormCount;

    public LP<?> currentSession;
    public LP<?> isCurrentSession;
    public LP connectionSession;
    public LP formSession;
    public LP quantityAddedClassesSession;
    public LP quantityRemovedClassesSession;
    public LP quantityChangedClassesSession;
    public LP changesSession;

    public LP pingComputerDateTimeFromDateTimeTo;
    public LP minTotalMemoryComputerDateTimeFromDateTimeTo;
    public LP maxTotalMemoryComputerDateTimeFromDateTimeTo;
    public LP minUsedMemoryComputerDateTimeFromDateTimeTo;
    public LP maxUsedMemoryComputerDateTimeFromDateTimeTo;

    public SystemEventsLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(baseLM, BL, "/system/SystemEvents.lsf");
        this.authenticationLM = BL.authenticationLM;
    }

    @Override
    public void initMetaAndClasses() throws RecognitionException {
        super.initMetaAndClasses();

        clientException = (ConcreteCustomClass) findClass("ClientException");
        webClientException = (ConcreteCustomClass) findClass("WebClientException");
        remoteServerException = (ConcreteCustomClass) findClass("RemoteServerException");
        fatalHandledRemoteException = (ConcreteCustomClass) findClass("FatalHandledException");
        nonFatalHandledRemoteException = (ConcreteCustomClass) findClass("NonFatalHandledException");
        unhandledRemoteException = (ConcreteCustomClass) findClass("UnhandledException");
        serverException = (ConcreteCustomClass) findClass("ServerException");
        launch = (ConcreteCustomClass) findClass("Launch");
        connection = (ConcreteCustomClass) findClass("Connection");
        clientType = (ConcreteCustomClass) findClass("ClientType");
        connectionStatus = (ConcreteCustomClass) findClass("ConnectionStatus");
        session = (ConcreteCustomClass) findClass("Session");
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        currentConnection = addProperty(null, new LP<>(new CurrentConnectionProperty(connection)));
        makePropertyPublic(currentConnection, "currentConnection", new ArrayList<>());

        super.initMainLogic();

        onStarted = findAction("onStarted[]");

        useBootstrap = findProperty("useBootstrap[]");

        // Подключения к серверу
        computerConnection = findProperty("computer[Connection]");
        remoteAddressConnection = findProperty("remoteAddress[Connection]");
        webHostConnection = findProperty("webHost[Connection]");
        webPortConnection = findProperty("webPort[Connection]");
        contextPathConnection = findProperty("contextPath[Connection]");
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
        is64JavaConnection = findProperty("is64Java[Connection]");
        screenSizeConnection = findProperty("screenSize[Connection]");
        clientTypeConnection = findProperty("clientType[Connection]");
        connectionStatusConnection = (LP<PropertyInterface>) findProperty("connectionStatus[Connection]");
        lastActivity = findProperty("lastActivity[Connection]");

        connectTimeConnection = findProperty("connectTime[Connection]");
        launchConnection = findProperty("launch[Connection]");

        currentLaunch = findProperty("currentLaunch[]");

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
        connectionFormCount = findProperty("connectionFormCount[Connection,Form]");

        // Сессия
        currentSession = findProperty("currentSession[]");
        isCurrentSession = findProperty("isCurrentSession[?]");
        connectionSession = findProperty("connection[Session]");
        formSession = findProperty("form[Session]");
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
    }

    public void logException(BusinessLogics bl, ExecutionStack stack, Throwable t, DataObject user, String clientName, boolean client, boolean web) throws SQLException, SQLHandledException {
        assert t.getCause() == null;

        String message = replaceNonUTFCharacters(t.getMessage());
        String errorType = t.getClass().getName();

        Pair<String, String> exStacks = RemoteInternalException.getExStacks(t);
        String javaStack = replaceNonUTFCharacters(exStacks.first);
        String lsfStack = exStacks.second;

        String time = new SimpleDateFormat().format(Calendar.getInstance().getTime());
        logger.error( message + " at '" + time + "' from '" + clientName + "': " + '\n' + ExceptionUtils.getExStackTrace(javaStack, lsfStack));

        try (DataSession session = ThreadLocalContext.createSession()) {
            DataObject exceptionObject;
            if (client) {
                if (t instanceof RemoteServerException) {
                    exceptionObject = session.addObject(remoteServerException);
                } else if (t instanceof RemoteException) {
                    exceptionObject = session.addObject(unhandledRemoteException);
                } else if (t instanceof RemoteClientException) {
                    RemoteClientException handled = (RemoteClientException) t;

                    if (t instanceof FatalRemoteClientException)
                        exceptionObject = session.addObject(fatalHandledRemoteException);
                    else {
                        exceptionObject = session.addObject(nonFatalHandledRemoteException);

                        NonFatalRemoteClientException nonFatal = (NonFatalRemoteClientException) t;
                        countNonFatalHandledException.change(nonFatal.count, session, exceptionObject);
                        abandonedNonFatalHandledException.change(nonFatal.abandoned, session, exceptionObject);
                    }

                    reqIdHandledException.change(handled.reqId, session, exceptionObject);
                } else if (web) {
                    exceptionObject = session.addObject(webClientException);
                } else {
                    exceptionObject = session.addObject(clientException);    
                }
                clientClientException.change(clientName, session, exceptionObject);
                if(user != null) {
                    String userLogin = (String) authenticationLM.loginCustomUser.read(session, user);
                    loginClientException.change(userLogin, session, exceptionObject);
                }
            } else {
                exceptionObject = session.addObject(serverException);
            }
            messageException.change(message, session, exceptionObject);
            typeException.change(errorType, session, exceptionObject);
            erTraceException.change(javaStack, session, exceptionObject);
            lsfTraceException.change(lsfStack, session, exceptionObject);
            dateException.change(LocalDateTime.now(), session, exceptionObject);

            session.applyException(bl, stack);
        }
    }

    private String replaceNonUTFCharacters(String value) {
        return value == null ? null : value.replace('\u0000', '?');
    }
}
