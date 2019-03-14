package lsfusion.server.logics.navigator;

// навигатор работает с абстрактной BL

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.weak.WeakIdentityHashMap;
import lsfusion.base.col.heavy.weak.WeakIdentityHashSet;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.interop.navigator.ClientSettings;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.exception.AuthenticationException;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.server.EnvStackRunnable;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.SystemProperties;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.language.linear.LA;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.admin.authentication.policy.SecurityPolicy;
import lsfusion.server.logics.classes.ConcreteCustomClass;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.classes.DateTimeClass;
import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.interactive.listener.RemoteFormListener;
import lsfusion.server.logics.*;
import lsfusion.server.remote.*;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.ExecutionEnvironment;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static lsfusion.base.BaseUtils.nvl;

// приходится везде BusinessLogics Generics'ом гонять потому как при инстанцировании формы нужен конкретный класс

public class RemoteNavigator extends RemoteConnection implements RemoteNavigatorInterface, FocusListener, CustomClassListener, RemoteFormListener {
    protected final static Logger logger = ServerLoggers.systemLogger;

    private static NotificationsMap notificationsMap = new NotificationsMap();

    private final NavigatorsManager navigatorManager;

    private String currentForm;

    private DataObject connection;

    public SecurityPolicy securityPolicy;

    private ClientCallBackController client;

    private RemotePausableInvocation currentInvocation = null;
    
    private final WeakIdentityHashSet<RemoteForm> forms = new WeakIdentityHashSet<>();

    private static final List<Pair<DataObject, String>> recentlyOpenForms = Collections.synchronizedList(new ArrayList<Pair<DataObject, String>>());

    private String formID = null;

    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать
    public RemoteNavigator(int port, LogicsInstance logicsInstance, AuthenticationToken token, NavigatorInfo navigatorInfo, ExecutionStack stack) throws RemoteException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, SQLHandledException {
        super(port, "navigator", stack);

        setContext(new RemoteNavigatorContext(this));
        initContext(logicsInstance, token, navigatorInfo.session, stack);
        
        ServerLoggers.remoteLifeLog("NAVIGATOR OPEN : " + this);

        this.classCache = new ClassCache();

        this.client = new ClientCallBackController(port, toString(), new ClientCallBackController.UsageTracker() {
            @Override
            public void used() {
                updateLastUsedTime();
            }
        });

        createPausablesExecutor();

        this.navigatorManager = logicsInstance.getNavigatorsManager();
        navigatorManager.navigatorCreated(stack, this, navigatorInfo);
    }

    @Override
    protected void initUserContext(String hostName, String remoteAddress, String clientLanguage, String clientCountry, ExecutionStack stack, DataSession session) throws SQLException, SQLHandledException {
        super.initUserContext(hostName, remoteAddress, clientLanguage, clientCountry, stack, session);
        
        localePreferences = readLocalePreferences(session, user, businessLogics, clientLanguage, clientCountry, stack);
        securityPolicy = logicsInstance.getSecurityManager().readSecurityPolicy(session, user);
    }

    private LocalePreferences readLocalePreferences(DataSession session, DataObject user, BusinessLogics businessLogics, String clientLanguage, String clientCountry, ExecutionStack stack) throws SQLException, SQLHandledException {
        return new LocalePreferences(locale,
                (String) businessLogics.authenticationLM.timeZone.read(session, user),
                (Integer) businessLogics.authenticationLM.twoDigitYearStart.read(session, user));
    }

    public void delayUserInteraction(ClientAction action) {
        currentInvocation.delayUserInteraction(action);
    }

    public Object[] requestUserInteraction(final ClientAction... actions) {
        return currentInvocation.pauseForUserInteraction(actions);
    }

    public void logClientException(String title, String hostname, Throwable t) {
        boolean web = false;
        if (hostname == null) { // считается, что Web
            web = true;
            hostname = ThreadLocalContext.get().getLogInfo().hostnameComputer + " - web";
        }
        
        try {
            businessLogics.systemEventsLM.logException(businessLogics, getStack(), t, this.user, hostname, true, web);
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void setCurrentForm(String formID) throws RemoteException {
        this.formID = formID;
    }

    public String getCurrentForm() {
        return formID;
    }

    @Aspect
    public static class RemoteNavigatorUsageAspect {
        @Around(RemoteContextAspect.allRemoteCalls)
        public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, RemoteNavigator target) throws Throwable {
            target.updateLastUsedTime();
            return thisJoinPoint.proceed();
        }
    }

    private volatile long lastUsedTime;

    public void updateLastUsedTime() {
        //забиваем на синхронизацию, потому что для времени использования совсем неактуально
        //пусть потоки меняют как хотят
        lastUsedTime = System.currentTimeMillis();
    }

    public long getLastUsedTime() {
        return lastUsedTime;
    }

    public Long getConnectionId() {
        return connection != null ? (Long) connection.object : null;
    }

    private static class WeakFormController implements FormController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteNavigator> weakThis;

        private WeakFormController(RemoteNavigator navigator) {
            this.weakThis = new WeakReference<>(navigator);
        }

        @Override
        public void changeCurrentForm(String form) {
            RemoteNavigator remoteNavigator = weakThis.get();
            if(remoteNavigator !=null)
                remoteNavigator.currentForm = form;
        }

        public String getCurrentForm() {
            RemoteNavigator remoteNavigator = weakThis.get();
            return remoteNavigator == null ? null : remoteNavigator.currentForm;
        }
    }

    private static class WeakChangesUserProvider implements ChangesController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<ChangesSync> weakThis;

        private WeakChangesUserProvider(ChangesSync dbManager) {
            this.weakThis = new WeakReference<>(dbManager);
        }

        public void regChange(ImSet<Property> changes, DataSession session) {
            ChangesSync changesSync = weakThis.get();
            if(changesSync != null)
                changesSync.regChange(changes, session);
        }

        public ImSet<Property> update(DataSession session, FormInstance form) {
            ChangesSync changesSync = weakThis.get();
            if(changesSync != null)
                return changesSync.update(session, form);
            return SetFact.EMPTY();
        }

        public void registerForm(FormInstance form) {
            ChangesSync changesSync = weakThis.get();
            if(changesSync != null)
                changesSync.registerForm(form);
        }

        public void unregisterForm(FormInstance form) {
            ChangesSync changesSync = weakThis.get();
            if(changesSync != null)
                changesSync.unregisterForm(form);
        }
    }

    @Override
    protected FormController createFormController() {
        return new WeakFormController(this);
    }

    @Override
    protected ChangesController createChangesController() {
        return new WeakChangesUserProvider(changesSync);
    }

    @Override
    public ClientSettings getClientSettings() {
        String currentUserName;
        Integer fontSize;
        boolean useBusyDialog;
        boolean useRequestTimeout;
        boolean forbidDuplicateForms;

        try (DataSession session = createSession()) {
            currentUserName = nvl((String) businessLogics.authenticationLM.currentUserName.read(session), "(без имени)");
            fontSize = (Integer) businessLogics.authenticationLM.userFontSize.read(session, user);
            useBusyDialog = Settings.get().isBusyDialog() || SystemProperties.inTestMode || businessLogics.authenticationLM.useBusyDialog.read(session) != null;
            useRequestTimeout = Settings.get().isUseRequestTimeout() || businessLogics.authenticationLM.useRequestTimeout.read(session) != null;
            forbidDuplicateForms = businessLogics.securityLM.forbidDuplicateFormsCustomUser.read(session, user) != null;
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
        boolean configurationAccessAllowed = securityPolicy.configurator != null && securityPolicy.configurator;
        return new ClientSettings(localePreferences, currentUserName, fontSize, useBusyDialog, Settings.get().getBusyDialogTimeout(),
                useRequestTimeout, SystemProperties.inDevMode, configurationAccessAllowed, forbidDuplicateForms);
    }

    public void gainedFocus(FormInstance form) {
        //todo: не нужно, так что позже можно удалить
    }

    public static void updateOpenFormCount(BusinessLogics businessLogics, DataSession session, ExecutionStack stack) {
        try {
            List<Pair<DataObject, String>> openForms;
            synchronized (recentlyOpenForms) {
                openForms = new ArrayList<>(recentlyOpenForms);
            }
            recentlyOpenForms.clear();

            for (Pair<DataObject, String> entry : openForms) {
                DataObject connection = entry.first;
                String canonicalName = entry.second;
                if (canonicalName == null) {
                    continue;
                }

                Long formId = (Long) businessLogics.reflectionLM.formByCanonicalName.read(
                        session,
                        new DataObject(canonicalName, businessLogics.reflectionLM.formCanonicalNameClass));

                if (formId == null) {
                    continue;
                }

                DataObject formObject = new DataObject(formId, businessLogics.reflectionLM.form);

                int count = 1 + nvl((Integer) businessLogics.systemEventsLM.connectionFormCount.read(session, connection, formObject), 0);
                businessLogics.systemEventsLM.connectionFormCount.change(count, session, connection, formObject);
            }
            session.applyException(businessLogics, stack);
        } catch (Exception e) {
            logger.error("UpdateOpenFormCount error: ", e);
        }
    }

    public static void updateUserLastActivity(BusinessLogics businessLogics, DataSession session, ExecutionStack stack) {
        try {

            Map<Long, UserActivity> userActivityMap;
            userActivityMap = new HashMap<>(RemoteLoggerAspect.userActivityMap);
            RemoteLoggerAspect.userActivityMap.clear();

            for (Map.Entry<Long, UserActivity> userActivity : userActivityMap.entrySet()) {
                DataObject customUserObject = session.getDataObject(businessLogics.authenticationLM.customUser, userActivity.getKey());
                businessLogics.authenticationLM.lastActivityCustomUser.change(new Timestamp(userActivity.getValue().time), session, customUserObject);
                businessLogics.authenticationLM.lastComputerCustomUser.change(userActivity.getValue().computer, session, customUserObject);

            }
            session.applyException(businessLogics, stack);
        } catch (Exception e) {
            logger.error("UpdateUserLastActivity error: ", e);
        }
    }

    public static void updatePingInfo(BusinessLogics businessLogics, DataSession session, ExecutionStack stack) {
        try {
            Map<String, Map<Long, List<Long>>> pingInfoMap = new HashMap<>(RemoteLoggerAspect.pingInfoMap);
            RemoteLoggerAspect.pingInfoMap.clear();
            
            for (Map.Entry<String, Map<Long, List<Long>>> entry : pingInfoMap.entrySet()) {
                ObjectValue computerValue = businessLogics.authenticationLM.computerHostname.readClasses(session, new DataObject(entry.getKey()));
                if(computerValue instanceof DataObject) {
                    DataObject computerObject = (DataObject)computerValue;
                    for (Map.Entry<Long, List<Long>> pingEntry : entry.getValue().entrySet()) {
                        DataObject dateFrom = new DataObject(new Timestamp(pingEntry.getKey()), DateTimeClass.instance);
                        DataObject dateTo = new DataObject(new Timestamp(pingEntry.getValue().get(0)), DateTimeClass.instance);
                        businessLogics.systemEventsLM.pingComputerDateTimeFromDateTimeTo.change(pingEntry.getValue().get(1).intValue(), session, computerObject, dateFrom, dateTo);
                        if (pingEntry.getValue().size() >= 6) {
                            businessLogics.systemEventsLM.minTotalMemoryComputerDateTimeFromDateTimeTo.change(pingEntry.getValue().get(2).intValue(), session, computerObject, dateFrom, dateTo);
                            businessLogics.systemEventsLM.maxTotalMemoryComputerDateTimeFromDateTimeTo.change(pingEntry.getValue().get(3).intValue(), session, computerObject, dateFrom, dateTo);
                            businessLogics.systemEventsLM.minUsedMemoryComputerDateTimeFromDateTimeTo.change(pingEntry.getValue().get(4).intValue(), session, computerObject, dateFrom, dateTo);
                            businessLogics.systemEventsLM.maxUsedMemoryComputerDateTimeFromDateTimeTo.change(pingEntry.getValue().get(5).intValue(), session, computerObject, dateFrom, dateTo);
                        }
                    }
                }
            }
            session.applyException(businessLogics, stack);
        } catch (Exception e) {
            logger.error("UpdatePingInfo error: ", e);
        }
    }

    @Override
    public Long getObject(CustomClass cls) {
        return getCacheObject(cls);
    }

    public void objectChanged(ConcreteCustomClass cls, long objectID) {
        addCacheObject(cls, objectID);
    }

    private ClassCache classCache;

    private Long getCacheObject(CustomClass cls) {
        return classCache.getObject(cls);
    }

    public void addCacheObject(ConcreteCustomClass cls, long value) {
        classCache.put(cls, value);
    }

    public DataObject getUser() {
        return user;
    }

    public DataObject getComputer() {
        return computer;
    }

    public DataObject getConnection() {
        return connection;
    }

    public void setConnection(DataObject connection) {
        this.connection = connection;
    }

    public synchronized ClientCallBackController getClientCallBack() throws RemoteException {
        return client;
    }

    public synchronized void pushNotification(EnvStackRunnable run) throws RemoteException {
        if(isClosed())
            return;

        client.pushMessage(notificationsMap.putNotification(run));
    }

    @Override
    public byte[] getNavigatorTree() {

        ImOrderMap<NavigatorElement, List<String>> elements = businessLogics.LM.root.getChildrenMap(securityPolicy);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            int elementsCount = elements.size();
            
            dataStream.writeInt(elementsCount);
            for (NavigatorElement element : elements.keyIt()) {
                element.serialize(dataStream);
            }
            
            for (List<String> children : elements.valueIt()) {
                dataStream.writeInt(children.size());
                for (String child : children) {
                    dataStream.writeUTF(child);
                }
            }

            businessLogics.LM.baseWindows.log.serialize(dataStream);
            businessLogics.LM.baseWindows.status.serialize(dataStream);
            businessLogics.LM.baseWindows.forms.serialize(dataStream);

        } catch (IOException e) {
            Throwables.propagate(e);
        }

        return outStream.toByteArray();
    }

    @Override
    public ServerResponse executeNavigatorAction(final String actionSID, final int type) throws RemoteException {
        currentInvocation = new RemotePausableInvocation(pausablesExecutor, this) {
            @Override
            protected ServerResponse callInvocation() throws Throwable {
                ExecutionStack stack = getStack();
                if (type == 2) {
                    //временно, так как иначе все контроллеры идут от верхней сессии, в частности, currentUser получается чужой
                    try(DataSession session = createSession()) {
                        runNotification(session, stack, actionSID);
                    }
                } else {
                    try (DataSession session = createSession()) {
                        runAction(session, actionSID, type == 1, stack);
                    }
                }
                assert !delayedGetRemoteChanges && !delayedHideForm; // тут не должно быть никаких delayRemote или hideForm
                return new ServerResponse(delayedActions.toArray(new ClientAction[delayedActions.size()]), false);
            }
        };

        return currentInvocation.execute();
    }

    private void runNotification(ExecutionEnvironment env, ExecutionStack stack, String actionSID) {
        Integer idNotification;
        try {
            idNotification = Integer.parseInt(actionSID);
        } catch (Exception e) {
            idNotification = null;
        }
        if (idNotification != null) {
            try {
                try(DataSession session = createSession()) {
                    businessLogics.authenticationLM.deliveredNotificationAction.execute(session, stack, user);
                    session.applyException(businessLogics, stack);
                }
                EnvStackRunnable notification = notificationsMap.getNotification(idNotification);
                if(notification != null)
                    notification.run(env, stack);
                else
                    ServerLoggers.assertLog(false, "NOTIFICATION " + idNotification + " SHOULD EXIST"); // возможно может нарушаться при перепосылке запроса на клиенте при проблемах со связью
            } catch (SQLException | SQLHandledException e) {
                ServerLoggers.systemLogger.error("DeliveredNotificationAction failed: ", e);
            }
        }
    }

    private void runAction(DataSession session, String canonicalName, boolean isNavigatorAction, ExecutionStack stack) throws SQLException, SQLHandledException {
        final LA<?> action;
        if (isNavigatorAction) {
            final NavigatorElement element = businessLogics.findNavigatorElement(canonicalName);

            if (!(element instanceof NavigatorAction)) {
                throw new RuntimeException(ThreadLocalContext.localize("{form.navigator.action.not.found}"));
            }

            if (!securityPolicy.navigator.checkPermission(element)) {
                throw new RuntimeException(ThreadLocalContext.localize("{form.navigator.not.enough.permissions}"));
            }

            action = new LA(((NavigatorAction) element).getAction());
        } else {
            action = businessLogics.findAction(canonicalName);
        }
        action.execute(session, stack);
    }

    @Override
    public ServerResponse continueNavigatorAction(Object[] actionResults) throws RemoteException {
        return currentInvocation.resumeAfterUserInteraction(actionResults);
    }

    @Override
    public ServerResponse throwInNavigatorAction(Throwable clientThrowable) throws RemoteException {
        return currentInvocation.resumeWithThrowable(clientThrowable);
    }

    @Override
    public void formCreated(RemoteForm form) {
        DataObject connection = getConnection();
        if (connection != null) {
            recentlyOpenForms.add(new Pair<>(connection, form.getCanonicalName()));
        }
        synchronized (forms) {
            forms.add(form);
        }
    }

    @Override
    public void formClosed(RemoteForm form) {
        synchronized (forms) {
            forms.remove(form);
        }
    }

    @Override
    public void executeNotificationAction(ExecutionEnvironment env, ExecutionStack stack, Integer idNotification) throws RemoteException {
        try {
            runNotification(env, stack, String.valueOf(idNotification));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // обмен изменениями между сессиями в рамках одного подключения
    private static class LastChanges {
        private long timeStamp;
        private WeakReference<DataSession> wLastSession;
        private long lastSessionTimeStamp; // assert < timeStamp

        private void regChange(long newStamp, DataSession newSession) {
            DataSession lastSession;
            if(wLastSession == null || (lastSession = wLastSession.get()) == null || newSession != lastSession) { // другая сессия
                wLastSession = new WeakReference<>(newSession);
                lastSessionTimeStamp = timeStamp;
            }
            timeStamp = newStamp;

            assert lastSessionTimeStamp < timeStamp;
        }

        public boolean isChanged(long prevStamp, DataSession session) {
            DataSession lastSession = wLastSession.get();
            if(lastSession != null && session == lastSession) { // эта сессия
                return lastSessionTimeStamp > prevStamp;
            }
            return timeStamp > prevStamp;
        }

    }
    // обмен изменениями между сессиями в рамках одного подключения
    private static class ChangesSync implements ChangesController {

        private final Map<Property, LastChanges> changes = MapFact.mAddRemoveMap();
        private final WeakIdentityHashMap<FormInstance, Long> formStamps = new WeakIdentityHashMap<>();
        private long minPrevUpdatedStamp = 0;
        private long currentStamp = 0;

        private void updateLastStamp(long prevStamp) {
            assert !formStamps.isEmpty();
            if(minPrevUpdatedStamp >= prevStamp) {
                minPrevUpdatedStamp = currentStamp; // ищем новый stamp
                for(Pair<FormInstance, Long> entry : formStamps.entryIt())
                    if(entry.second < minPrevUpdatedStamp)
                        minPrevUpdatedStamp = entry.second;

                // удаляем все меньше minStamp
                for(Iterator<Map.Entry<Property,LastChanges>> it = changes.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<Property, LastChanges> entry = it.next();
                    if(entry.getValue().timeStamp <= minPrevUpdatedStamp) // isChanged никак не будет
                        it.remove();
                }
            }
        }

        public synchronized void regChange(ImSet<Property> updateChanges, DataSession session) {
            currentStamp++;

            for(Property change : updateChanges) {
                LastChanges last = changes.get(change);
                if(last == null) {
                    last = new LastChanges();
                    changes.put(change, last);
                }
                last.regChange(currentStamp, session);
            }
        }

        public synchronized ImSet<Property> update(DataSession session, FormInstance form) {
            assert session == form.session;

            Long lPrevStamp = formStamps.get(form);
            assert lPrevStamp != null;
            if(lPrevStamp == null) // just in case
                return SetFact.EMPTY();
            long prevStamp = lPrevStamp;

            if(prevStamp == currentStamp) // если не было никаких изменений
                return SetFact.EMPTY();

            MExclSet<Property> mProps = SetFact.mExclSet();
            for(Map.Entry<Property, LastChanges> change : changes.entrySet()) {
                if(change.getValue().isChanged(prevStamp, session))
                    mProps.exclAdd(change.getKey());
            }
            formStamps.put(form, currentStamp);
            updateLastStamp(prevStamp);
            return mProps.immutable();
        }

        public synchronized void registerForm(FormInstance form) {
            formStamps.put(form, currentStamp);
        }

        public synchronized void unregisterForm(FormInstance form) {
            formStamps.remove(form);
        }
    }

    private static class NotificationsMap {
        private Map<Integer, EnvStackRunnable> notificationsMap = new HashMap<>();
        private int counter = 0;

        private synchronized Integer putNotification(EnvStackRunnable value) {
            counter++;
            notificationsMap.put(counter, value);
            return counter;
        }

        private synchronized EnvStackRunnable getNotification(Integer key) {
            return notificationsMap.remove(key);
        }
    }

    private ChangesSync changesSync = new ChangesSync();

    @Override
    protected String notSafeToString() {
        return "RN[clientAddress: " + logInfo.remoteAddress + "," + user + "," + System.identityHashCode(this) + "," + sql + "]";
    }

    public static void checkEnableUI(boolean anonymous) {
        byte enableUI = Settings.get().getEnableUI();
        if(enableUI == 0)
            throw new RuntimeException("Ui is disabled. It can be enabled by using setting enableUI.");

        if(anonymous && enableUI == 1)
            throw new AuthenticationException();
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate(); // сначала очищаем свои процессы, потом процессы форм (так как свои процессы могут создавать формы)

        Set<RemoteForm> proceededForms = new HashSet<>();
        while(true) { // нужны в том числе закрывающиеся формы, чтобы гарантировать, что все формы закроются до закрытия соединения
            Set<RemoteForm> formsSnap = new HashSet<>();
            synchronized (forms) {
                for(RemoteForm form : forms)
                    if(form != null && proceededForms.add(form))
                        formsSnap.add(form);
                if(formsSnap.isEmpty())
                    break; // считаем что когда навигатор закрывается новые создаваться не могут
            }
            for(RemoteForm form : formsSnap)
                form.deactivate();
        }
    }

    @Override
    protected void onClose() {
        while(true) { // нужны в том числе закрывающиеся формы, чтобы гарантировать, что все формы закроются до закрытия соединения
            Set<RemoteForm> formsSnap;
            synchronized (forms) {
                formsSnap = forms.copy();
                if(formsSnap.isEmpty())
                    break; // считаем что когда навигатор закрывается новые создаваться не могут
            }
            for(RemoteForm form : formsSnap)
                if(form != null)
                    form.explicitClose();
        }

        navigatorManager.navigatorClosed(this, getStack(), getConnection());

        super.onClose();
    }

    @Override
    protected boolean isUnreferencedSyncedClient() { // если ушли все ссылки считаем синхронизированным, так как клиент уже ни к чему обращаться не может
        return true;
    }

    @Override
    public Object getProfiledObject() {
        return "n";
    }

    @Override
    public synchronized void close() throws RemoteException { // without this RemoteContextAspect will not be weaved
        super.close();
    }
}