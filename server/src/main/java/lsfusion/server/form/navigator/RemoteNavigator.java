package lsfusion.server.form.navigator;

// навигатор работает с абстрактной BL

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.interop.LocalePreferences;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.form.ServerResponse;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.server.EnvStackRunnable;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.auth.User;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.context.SyncType;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.instance.GroupObjectInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.form.instance.listener.RemoteFormListener;
import lsfusion.server.logics.*;
import lsfusion.server.logics.SecurityManager;
import lsfusion.server.logics.property.*;
import lsfusion.server.remote.ContextAwarePendingRemoteObject;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.remote.RemoteLoggerAspect;
import lsfusion.server.remote.RemotePausableInvocation;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;
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
import java.text.SimpleDateFormat;
import java.util.*;

import static lsfusion.base.BaseUtils.nvl;

// приходится везде BusinessLogics Generics'ом гонять потому как при инстанцировании формы нужен конкретный класс

public class RemoteNavigator<T extends BusinessLogics<T>> extends ContextAwarePendingRemoteObject implements RemoteNavigatorInterface, FocusListener<T>, CustomClassListener, RemoteFormListener {
    protected final static Logger logger = ServerLoggers.systemLogger;

    private static NotificationsMap notificationsMap = new NotificationsMap();

    private final SQLSession sql;

    final LogicsInstance logicsInstance;
    private final NavigatorsManager navigatorManager;
    private final BusinessLogics businessLogics;
    private final SecurityManager securityManager;
    private final DBManager dbManager;

    // просто закэшируем, чтобы быстрее было
    SecurityPolicy securityPolicy;

    private DataObject user;

    private LocalePreferences userLocalePreferences;
    
    private DataObject computer;

    private ObjectValue currentForm;

    private DataObject connection;

    private int updateTime;

    private String remoteAddress;

    private final WeakIdentityHashSet<DataSession> sessions = new WeakIdentityHashSet<>();

    private final boolean isFullClient;

    private ClientCallBackController client;

    private RemotePausableInvocation currentInvocation = null;
    
    private final WeakIdentityHashSet<RemoteForm> forms = new WeakIdentityHashSet<>();

    private static final List<Pair<DataObject, String>> recentlyOpenForms = Collections.synchronizedList(new ArrayList<Pair<DataObject, String>>());

    private String formID = null;

    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать
    public RemoteNavigator(LogicsInstance logicsInstance, boolean isFullClient, NavigatorInfo navigatorInfo, User currentUser, int port, ExecutionStack stack) throws RemoteException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, SQLHandledException {
        super(port);

        this.logicsInstance = logicsInstance;
        this.navigatorManager = logicsInstance.getNavigatorsManager();
        this.businessLogics = logicsInstance.getBusinessLogics();
        this.securityManager = logicsInstance.getSecurityManager();
        this.dbManager = logicsInstance.getDbManager();

        this.isFullClient = isFullClient;

        setContext(new RemoteNavigatorContext(this));
        this.classCache = new ClassCache();

        this.securityPolicy = currentUser.getSecurityPolicy();
        this.transactionTimeout = currentUser.getTimeout();

        try(DataSession session = dbManager.createSession()) {
            this.user = currentUser.getDataObject(businessLogics.authenticationLM.customUser, session);
        }
        this.computer = new DataObject(navigatorInfo.computer, businessLogics.authenticationLM.computer);
        this.currentForm = NullValue.instance;

        this.remoteAddress = navigatorInfo.remoteAddress;
        this.sql = dbManager.createSQL(new WeakSQLSessionUserProvider(this));

        ServerLoggers.remoteLifeLog("NAVIGATOR OPEN : " + this);

        this.client = new ClientCallBackController(port, toString(), new ClientCallBackController.UsageTracker() {
            @Override
            public void used() {
                updateLastUsedTime();
            }
        });

        finalizeInit(stack, SyncType.NOSYNC);

        navigatorManager.navigatorCreated(stack, this, navigatorInfo);
        
        loadLocalePreferences();
    }

    public boolean isFullClient() {
        return isFullClient;
    }

    public boolean changeCurrentUser(DataObject user) throws SQLException, SQLHandledException {
        Object newRole = securityManager.getUserMainRole(user);
        Object currentRole = securityManager.getUserMainRole(this.user);
        if (BaseUtils.nullEquals(newRole, currentRole)) {
            setUser(user);
            Result<Integer> timeout = new Result<>();
            this.securityPolicy = getUserSecurityPolicy(timeout);
            this.transactionTimeout = timeout.result;
            updateEnvironmentProperty((CalcProperty) businessLogics.authenticationLM.currentUser.property, user);
            return true;
        }
        return false;
    }

    public synchronized void updateEnvironmentProperty(CalcProperty property, ObjectValue value) throws SQLException {
        if(isClosed())
            return;

        for (DataSession session : sessions)
            session.updateProperties(SetFact.singleton(property), true); // редко используется поэтому все равно
    }

    public SecurityPolicy getUserSecurityPolicy(Result<Integer> timeout) {
        try {
            try(DataSession session = createSession()) {
                User user = securityManager.readUserWithSecurityPolicy(getUserLogin(), session);
                timeout.set(user.getTimeout());
                return user.getSecurityPolicy();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserLogin() {
        try {
            try (DataSession session = createSession()) {
                return (String) businessLogics.authenticationLM.loginCustomUser.read(session, user);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String getLogMessage() {
        return currentInvocation.getLogMessage();
    }

    @IdentityLazy
    public LogInfo getLogInfo() {
        try {
            if(isClosed())
                return LogInfo.system;

            try (DataSession session = createSession()) {
                String userName = (String) businessLogics.authenticationLM.currentUserName.read(session);
                String computerName = (String) businessLogics.authenticationLM.hostnameCurrentComputer.read(session);
                return new LogInfo(userName, computerName, remoteAddress);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void delayUserInteraction(ClientAction action) {
        currentInvocation.delayUserInteraction(action);
    }

    Object[] requestUserInteraction(final ClientAction... actions) {
        return currentInvocation.pauseForUserInteraction(actions);
    }

    public void logClientException(String title, String hostname, Throwable t) {
        String time = new SimpleDateFormat().format(Calendar.getInstance().getTime());
        
        boolean web = false;
        if (hostname == null) { // считается, что Web
            web = true;
            hostname = ThreadLocalContext.get().getLogInfo().hostnameComputer + " - web";
        }
        
        logger.error(title + " at '" + time + "' from '" + hostname + "': ", t);
        try {
            businessLogics.systemEventsLM.logException(businessLogics, getStack(), t, this.user, hostname, true, web);
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public boolean isForbidDuplicateForms() {
        try {
            boolean forbidDuplicateForms;
            try (DataSession session = createSession()) {
                QueryBuilder<Object, String> query = new QueryBuilder<>(MapFact.<Object, KeyExpr>EMPTYREV());
                query.addProperty("forbidDuplicateForms", businessLogics.securityLM.forbidDuplicateFormsCurrentUser.getExpr());
                forbidDuplicateForms = query.execute(session).singleValue().get("forbidDuplicateForms") != null;
            }
            return forbidDuplicateForms;
        } catch (Exception e) {
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

    public byte[] getCurrentUserInfoByteArray() {
        try {
            String userName;
            try (DataSession session = createSession()) {
                QueryBuilder<Object, String> query = new QueryBuilder<>(MapFact.<Object, KeyExpr>EMPTYREV());
                query.addProperty("name", businessLogics.authenticationLM.currentUserName.getExpr());
                userName = BaseUtils.nvl((String) query.execute(session).singleValue().get("name"), "(без имени)").trim();
            }

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            dataStream.writeUTF(userName);
            return outStream.toByteArray();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void setUser(DataObject user) {
        this.user = user;
        loadLocalePreferences();
    }

    @Aspect
    private static class RemoteNavigatorUsageAspect {
        @Around("execution(* lsfusion.interop.navigator.RemoteNavigatorInterface.*(..)) && target(remoteNavigator)")
        public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, RemoteNavigator remoteNavigator) throws Throwable {
            remoteNavigator.updateLastUsedTime();
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

    private static class WeakUserController implements UserController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteNavigator> weakThis;

        private WeakUserController(RemoteNavigator navigator) {
            this.weakThis = new WeakReference<>(navigator);
        }

        public boolean changeCurrentUser(DataObject user) throws SQLException, SQLHandledException {
            RemoteNavigator remoteNavigator = weakThis.get();
            return remoteNavigator != null && remoteNavigator.changeCurrentUser(user);
        }

        public ObjectValue getCurrentUser() {
            RemoteNavigator remoteNavigator = weakThis.get();
            return remoteNavigator == null ? NullValue.instance : remoteNavigator.user;
        }
    }

    private static class WeakLocaleController implements LocaleController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteNavigator> weakThis;

        private WeakLocaleController(RemoteNavigator navigator) {
            this.weakThis = new WeakReference<>(navigator);
        }

        @Override
        public Locale getLocale() {
            RemoteNavigator remoteNavigator = weakThis.get();
            if(remoteNavigator != null)
                return remoteNavigator.getLocale();
            return Locale.getDefault();
        }
    }

    private static class WeakComputerController implements ComputerController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteNavigator> weakThis;

        private WeakComputerController(RemoteNavigator navigator) {
            this.weakThis = new WeakReference<>(navigator);
        }

        public ObjectValue getCurrentComputer() {
            RemoteNavigator remoteNavigator = weakThis.get();
            return remoteNavigator == null ? NullValue.instance : remoteNavigator.computer;
        }

        public boolean isFullClient() {
            RemoteNavigator remoteNavigator = weakThis.get();
            return remoteNavigator != null && remoteNavigator.isFullClient();
        }
    }

    private static class WeakFormController implements FormController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteNavigator> weakThis;

        private WeakFormController(RemoteNavigator navigator) {
            this.weakThis = new WeakReference<>(navigator);
        }

        @Override
        public void changeCurrentForm(ObjectValue form) {
            RemoteNavigator remoteNavigator = weakThis.get();
            if(remoteNavigator !=null)
                remoteNavigator.currentForm = form;
        }

        public ObjectValue getCurrentForm() {
            RemoteNavigator remoteNavigator = weakThis.get();
            return remoteNavigator == null ? NullValue.instance : remoteNavigator.currentForm;
        }
    }

    private static class WeakConnectionController implements ConnectionController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteNavigator> weakThis;

        private WeakConnectionController(RemoteNavigator navigator) {
            this.weakThis = new WeakReference<>(navigator);
        }

        public ObjectValue getCurrentConnection() {
            RemoteNavigator remoteNavigator = weakThis.get();
            return remoteNavigator == null  || remoteNavigator.connection == null ? NullValue.instance : remoteNavigator.connection;
        }
    }


    private static class WeakTimeoutController implements TimeoutController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteNavigator> weakThis;

        private WeakTimeoutController(RemoteNavigator navigator) {
            this.weakThis = new WeakReference<>(navigator);
        }

        public int getTransactionTimeout() {
            RemoteNavigator remoteNavigator = weakThis.get();
            return remoteNavigator == null ? 0 : remoteNavigator.getTransactionTimeout();
        }
    }

    private static class WeakSQLSessionUserProvider implements SQLSessionUserProvider { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteNavigator> weakThis;

        private WeakSQLSessionUserProvider(RemoteNavigator dbManager) {
            this.weakThis = new WeakReference<>(dbManager);
        }

        @Override
        public Integer getCurrentUser() {
            final RemoteNavigator wThis = weakThis.get();
            if(wThis == null) // используется в мониторе процессов
                return null;
            return (Integer) wThis.user.object;
        }

        public LogInfo getLogInfo() {
            final RemoteNavigator wThis = weakThis.get();
            if(wThis == null) // используется в мониторе процессов
                return null;
            return wThis.getLogInfo();
        }

        @Override
        public Integer getCurrentComputer() {
            final RemoteNavigator wThis = weakThis.get();
            if(wThis == null)
                return null;
            return (Integer) wThis.computer.object;
        }
    }

    private static class WeakChangesUserProvider implements ChangesController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<ChangesSync> weakThis;

        private WeakChangesUserProvider(ChangesSync dbManager) {
            this.weakThis = new WeakReference<>(dbManager);
        }

        public void regChange(ImSet<CalcProperty> changes, DataSession session) {
            ChangesSync changesSync = weakThis.get();
            if(changesSync != null)
                changesSync.regChange(changes, session);
        }

        public ImSet<CalcProperty> update(DataSession session, FormInstance form) {
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

    private int transactionTimeout;
    public int getTransactionTimeout() {
        return transactionTimeout;
    }
    
    private DataSession createSession() throws SQLException {
        DataSession session = dbManager.createSession(sql, new WeakUserController(this), new WeakComputerController(this), new WeakFormController(this), new WeakConnectionController(this), new WeakTimeoutController(this), new WeakChangesUserProvider(changesSync), new WeakLocaleController(this), null);
        sessions.add(session);
        return session;
    }

    @Override
    public boolean isConfigurationAccessAllowed() throws RemoteException {
        return securityPolicy.configurator != null && securityPolicy.configurator;
    }

    public boolean isBusyDialog() throws RemoteException {
        boolean useBusyDialog = false;
        try (DataSession session = createSession()) {
            useBusyDialog = businessLogics.authenticationLM.useBusyDialog.read(session) != null;
        } catch (SQLException | SQLHandledException ignored) {
        }
        return useBusyDialog;
    }

    private void loadLocalePreferences() {
        String language = null;
        String country = null;
        String timeZone = null;
        Integer twoDigitYearStart = null;
        boolean useClientLocale = false;
        try (DataSession session = createSession()) {
            language = (String) businessLogics.authenticationLM.languageCustomUser.read(session, user);
            if (language == null) {
                language = logicsInstance.getSettings().getLanguage();
                country = logicsInstance.getSettings().getCountry();
            } else {
                country = (String) businessLogics.authenticationLM.countryCustomUser.read(session, user);
            }
            timeZone = (String) businessLogics.authenticationLM.timeZoneCustomUser.read(session, user);
            twoDigitYearStart = (Integer) businessLogics.authenticationLM.twoDigitYearStartCustomUser.read(session, user);
            useClientLocale = businessLogics.authenticationLM.useClientLocaleCustomUser.read(session, user) != null;
        } catch (SQLException | SQLHandledException ignored) {
        }
        this.userLocalePreferences = new LocalePreferences(language, country, timeZone, twoDigitYearStart, useClientLocale);
    }
    
    public LocalePreferences getLocalePreferences() throws RemoteException {
        return userLocalePreferences;
    }

    public LocalePreferences getLocalLocalePreferences() {
        return userLocalePreferences;
    }

    public Locale getLocale() {
        LocalePreferences pref = getLocalLocalePreferences();
        if (pref != null && pref.useClientLocale && pref.language != null) {
            return new Locale(pref.language, pref.country == null ? "" : pref.country);
        }
        return Locale.getDefault();
    }

    public void gainedFocus(FormInstance<T> form) {
        //todo: не нужно, так что позже можно удалить
    }

    public static void updateOpenFormCount(BusinessLogics businessLogics, ExecutionStack stack) {
        try {

            try (DataSession session = ThreadLocalContext.getDbManager().createSession()) {
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

                    Integer formId = (Integer) businessLogics.reflectionLM.navigatorElementCanonicalName.read(
                            session,
                            new DataObject(canonicalName, businessLogics.reflectionLM.navigatorElementCanonicalNameClass));

                    if (formId == null) {
                        continue;
                    }

                    DataObject formObject = new DataObject(formId, businessLogics.reflectionLM.navigatorElement);

                    int count = 1 + nvl((Integer) businessLogics.systemEventsLM.connectionFormCount.read(session, connection, formObject), 0);
                    businessLogics.systemEventsLM.connectionFormCount.change(count, session, connection, formObject);
                }
                session.apply(businessLogics, stack);
            }
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public static void updateUserLastActivity(BusinessLogics businessLogics, ExecutionStack stack) {
        try {

            Map<Integer, Long> userActivityMap;
            userActivityMap = new HashMap<>(RemoteLoggerAspect.userActivityMap);
            RemoteLoggerAspect.userActivityMap.clear();

            try (DataSession session = ThreadLocalContext.getDbManager().createSession()) {

                for (Map.Entry<Integer, Long> userActivity : userActivityMap.entrySet()) {
                    DataObject customUserObject = new DataObject(userActivity.getKey(), businessLogics.authenticationLM.customUser);
                    businessLogics.authenticationLM.lastActivityCustomUser.change(new Timestamp(userActivity.getValue()), session, customUserObject);

                }
                String result = session.applyMessage(businessLogics, stack);
                if(result != null)
                    logger.error("UpdateUserLastActivity error: " + result);
            }
        } catch (Exception e) {
            logger.error("UpdateUserLastActivity error: ", e);
        }
    }

    public static void updatePingInfo(BusinessLogics businessLogics, ExecutionStack stack) {
        try {
            Map<Integer, Map<Long, List<Long>>> pingInfoMap = new HashMap<>(RemoteLoggerAspect.pingInfoMap);
            RemoteLoggerAspect.pingInfoMap.clear();
            try (DataSession session = ThreadLocalContext.getDbManager().createSession()) {
                for (Map.Entry<Integer, Map<Long, List<Long>>> entry : pingInfoMap.entrySet()) {
                    DataObject computerObject = new DataObject(entry.getKey(), businessLogics.authenticationLM.computer);
                    for (Map.Entry<Long, List<Long>> pingEntry : entry.getValue().entrySet()) {
                        DataObject dateFrom = new DataObject(new Timestamp(pingEntry.getKey()), DateTimeClass.instance);
                        DataObject dateTo = new DataObject(new Timestamp(pingEntry.getValue().get(0)), DateTimeClass.instance);
                        businessLogics.systemEventsLM.pingComputerDateTimeFromDateTimeTo.change(pingEntry.getValue().get(1).intValue(), session, computerObject, dateFrom, dateTo);
                        if(pingEntry.getValue().size() >= 6) {
                            businessLogics.systemEventsLM.minTotalMemoryComputerDateTimeFromDateTimeTo.change(pingEntry.getValue().get(2).intValue(), session, computerObject, dateFrom, dateTo);
                            businessLogics.systemEventsLM.maxTotalMemoryComputerDateTimeFromDateTimeTo.change(pingEntry.getValue().get(3).intValue(), session, computerObject, dateFrom, dateTo);
                            businessLogics.systemEventsLM.minUsedMemoryComputerDateTimeFromDateTimeTo.change(pingEntry.getValue().get(4).intValue(), session, computerObject, dateFrom, dateTo);
                            businessLogics.systemEventsLM.maxUsedMemoryComputerDateTimeFromDateTimeTo.change(pingEntry.getValue().get(5).intValue(), session, computerObject, dateFrom, dateTo);
                        }
                    }
                }
                String result = session.applyMessage(businessLogics, stack);
                if(result != null)
                    logger.error("UpdatePingInfo error: " + result);
            }
        } catch (Exception e) {
            logger.error("UpdatePingInfo error: ", e);
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Integer getObject(CustomClass cls) {
        return getCacheObject(cls);
    }

    public void objectChanged(ConcreteCustomClass cls, int objectID) {
        addCacheObject(cls, objectID);
    }

    public RemoteFormInterface createForm(String formSID, Map<String, String> initialObjects, boolean isModal, boolean interactive) {
        RemoteForm form = (RemoteForm) createForm(getFormEntity(formSID), isModal, interactive);
        if(initialObjects != null) {
            for (String objectSID : initialObjects.keySet()) {
                GroupObjectInstance groupObject = null;
                ObjectInstance object = null;
                for (GroupObjectInstance group : (ImOrderSet<GroupObjectInstance>) form.form.getOrderGroups()) {
                    for (ObjectInstance obj : group.objects) {
                        if (obj.getsID().equals(objectSID)) {
                            object = obj;
                            groupObject = group;
                            break;
                        }
                    }
                }
                if (object != null) {
                    groupObject.addSeek(object, new DataObject(Integer.decode(initialObjects.get(objectSID)), object.getCurrentClass()), true);
                }
            }
        }
        return form;
    }

    private FormEntity<T> getFormEntity(String formSID) {
        FormEntity<T> formEntity = (FormEntity<T>) businessLogics.getFormEntityBySID(formSID);

        if (formEntity == null) {
            throw new RuntimeException(ThreadLocalContext.localize("{form.navigator.form.with.id.not.found}") + " : " + formSID);
        }

        if (!securityPolicy.navigator.checkPermission(formEntity)) {
            return null;
        }

        return formEntity;
    }

    private RemoteFormInterface createForm(FormEntity<T> formEntity, boolean isModal, boolean interactive) {
        //todo: вернуть, когда/если починиться механизм восстановления сессии
//        try {
//            RemoteForm remoteForm = invalidatedForms.remove(formEntity);
//            if (remoteForm == null) {
//                remoteForm = context.createRemoteForm(
//                        context.createFormInstance(formEntity, MapFact.<ObjectEntity, DataObject>EMPTY(), createSession(), isModal, FormSessionScope.NEWSESSION, false, false, interactive)
//                );
//            }
//            return remoteForm;
//        } catch (Exception e) {
//            throw Throwables.propagate(e);
//        }
        try {
            ExecutionStack stack = getStack();
            try(DataSession session = createSession()) {
                return context.createRemoteForm(
                        context.createFormInstance(formEntity, MapFact.<ObjectEntity, DataObject>EMPTY(), session,
                                isModal, false, true, stack, false, false, interactive, null,
                                null, null, false),
                        stack);
            }
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    private ClassCache classCache;

    Integer getCacheObject(CustomClass cls) {
        return classCache.getObject(cls);
    }

    public void addCacheObject(ConcreteCustomClass cls, int value) {
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

    public void setUpdateTime(int updateTime) {
        this.updateTime = updateTime;
    }

    public int getUpdateTime() {
        return updateTime;
    }

    public String getRemoteAddress(){
        return remoteAddress;
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
    public DefaultFormsType showDefaultForms() throws RemoteException {
        return securityManager.showDefaultForms(user);
    }

    @Override
    public List<String> getDefaultForms() throws RemoteException {
        return securityManager.getDefaultForms(user);
    }

    @Override
    public byte[] getNavigatorTree() throws RemoteException {
        
        ImOrderMap<NavigatorElement<T>, List<String>> elements = businessLogics.LM.root.getChildrenMap(securityPolicy);

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
        } catch (IOException e) {
            Throwables.propagate(e);
        }

        return outStream.toByteArray();
    }

    @Override
    public byte[] getCommonWindows() throws RemoteException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            businessLogics.LM.windows.log.serialize(dataStream);
            businessLogics.LM.windows.status.serialize(dataStream);
            businessLogics.LM.windows.forms.serialize(dataStream);
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
                        session.apply(businessLogics, stack);
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
                    session.apply(businessLogics, stack);
                }
                EnvStackRunnable notification = notificationsMap.getNotification(idNotification);
                notification.run(env, stack);
            } catch (SQLException | SQLHandledException e) {
                ServerLoggers.systemLogger.error("DeliveredNotificationAction failed: ", e);
            }
        }
    }

    private void runAction(DataSession session, String actionSID, boolean isNavigatorAction, ExecutionStack stack) throws SQLException, SQLHandledException {
        final ActionProperty property;
        if (isNavigatorAction) {
            final NavigatorElement element = businessLogics.LM.root.getNavigatorElementBySID(actionSID);

            if (!(element instanceof NavigatorAction)) {
                throw new RuntimeException(ThreadLocalContext.localize("{form.navigator.action.not.found}"));
            }

            if (!securityPolicy.navigator.checkPermission(element)) {
                throw new RuntimeException(ThreadLocalContext.localize("{form.navigator.not.enough.permissions}"));
            }

            property = ((NavigatorAction) element).getProperty();
        } else {
            property = (ActionProperty) businessLogics.findProperty(actionSID).property;
        }
        property.execute(MapFact.<ClassPropertyInterface, DataObject>EMPTY(), session, stack, null);
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

    public synchronized void close() throws RemoteException {
        deactivateAndCloseLater(true); // после вызова close, предполагается что новых запросов уже идти не может, а старые закроются
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

        private final Map<CalcProperty, LastChanges> changes = MapFact.mAddRemoveMap();
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
                for(Iterator<Map.Entry<CalcProperty,LastChanges>> it = changes.entrySet().iterator();it.hasNext();) {
                    Map.Entry<CalcProperty, LastChanges> entry = it.next();
                    if(entry.getValue().timeStamp <= minPrevUpdatedStamp) // isChanged никак не будет
                        it.remove();
                }
            }
        }

        public synchronized void regChange(ImSet<CalcProperty> updateChanges, DataSession session) {
            if(!Settings.get().getUseUserChangesSync())
                return;

            currentStamp++;

            for(CalcProperty change : updateChanges) {
                LastChanges last = changes.get(change);
                if(last == null) {
                    last = new LastChanges();
                    changes.put(change, last);
                }
                last.regChange(currentStamp, session);
            }
        }

        public synchronized ImSet<CalcProperty> update(DataSession session, FormInstance form) {
            if(!Settings.get().getUseUserChangesSync())
                return SetFact.EMPTY();

            assert session == form.session;

            Long lPrevStamp = formStamps.get(form);
            assert lPrevStamp != null;
            if(lPrevStamp == null) // just in case
                return SetFact.EMPTY();
            long prevStamp = lPrevStamp;

            if(prevStamp == currentStamp) // если не было никаких изменений
                return SetFact.EMPTY();

            MExclSet<CalcProperty> mProps = SetFact.mExclSet();
            for(Map.Entry<CalcProperty, LastChanges> change : changes.entrySet()) {
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
    public String toString() {
        return "RemoteNavigator[clientAddress: " + remoteAddress + "," + user + "," + System.identityHashCode(this) + "," + sql + "]";
    }

    @Override
    public String getSID() {
        return "navigator";
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

        super.onClose();

        navigatorManager.navigatorClosed(this, getStack(), getConnection());

        try {
            ThreadLocalContext.assureRmi(RemoteNavigator.this);
            sql.close();
        } catch (Throwable t) {
            ServerLoggers.sqlSuppLog(t);
        }
    }

    @Override
    protected boolean isUnreferencedSyncedClient() { // если ушли все ссылки считаем синхронизированным, так как клиент уже ни к чему обращаться не может
        return true;
    }
}