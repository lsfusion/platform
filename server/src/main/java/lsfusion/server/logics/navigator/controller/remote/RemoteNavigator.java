package lsfusion.server.logics.navigator.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.weak.WeakIdentityHashMap;
import lsfusion.base.col.heavy.weak.WeakIdentityHashSet;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.base.view.ColorTheme;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.form.object.table.grid.user.design.ColorPreferences;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.navigator.ClientSettings;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.server.base.controller.remote.context.RemoteContextAspect;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.EnvStackRunnable;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.interactive.listener.RemoteFormListener;
import lsfusion.server.logics.navigator.NavigatorAction;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.controller.context.RemoteNavigatorContext;
import lsfusion.server.logics.navigator.controller.env.ChangesController;
import lsfusion.server.logics.navigator.controller.env.ClassCache;
import lsfusion.server.logics.navigator.controller.env.FormController;
import lsfusion.server.logics.navigator.controller.manager.NavigatorsManager;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.admin.log.RemoteLoggerAspect;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;

import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.server.logics.classes.data.time.DateTimeConverter.sqlTimestampToLocalDateTime;

// it would be better if there was NavigatorInstance (just like FormInstance and LogicsInstance), but for now will leave it this way
public class RemoteNavigator extends RemoteConnection implements RemoteNavigatorInterface, FocusListener, CustomClassListener, RemoteFormListener {
    protected final static Logger logger = ServerLoggers.systemLogger;

    private static NotificationsMap notificationsMap = new NotificationsMap();

    private final NavigatorsManager navigatorManager;

    private String currentForm;

    private DataObject connection;

    public SecurityPolicy securityPolicy;

    private ClientCallBackController client;
    
    private final WeakIdentityHashSet<RemoteForm> forms = new WeakIdentityHashSet<>();

    private static final List<Pair<DataObject, String>> recentlyOpenForms = Collections.synchronizedList(new ArrayList<>());

    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать
    public RemoteNavigator(int port, LogicsInstance logicsInstance, AuthenticationToken token, NavigatorInfo navigatorInfo, ExecutionStack stack) throws RemoteException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, SQLHandledException {
        super(port, "navigator", stack);

        setContext(new RemoteNavigatorContext(this));
        initContext(logicsInstance, token, navigatorInfo.session, stack);

        changesSync = new ChangesSync(dbManager);
        
        ServerLoggers.remoteLifeLog("NAVIGATOR OPEN : " + this);

        this.classCache = new ClassCache();

        this.client = new ClientCallBackController(port, toString(), this::updateLastUsedTime);

        createPausablesExecutor();

        this.navigatorManager = logicsInstance.getNavigatorsManager();
        navigatorManager.navigatorCreated(stack, this, navigatorInfo);
    }

    @Override
    protected void initUserContext(String hostName, String remoteAddress, String clientLanguage, String clientCountry, String clientDateFormat, String clientTimeFormat, ExecutionStack stack, DataSession session) throws SQLException, SQLHandledException {
        super.initUserContext(hostName, remoteAddress, clientLanguage, clientCountry, clientDateFormat, clientTimeFormat, stack, session);
        
        localePreferences = readLocalePreferences(session, user, businessLogics, clientLanguage, clientCountry, stack);
        securityPolicy = logicsInstance.getSecurityManager().getSecurityPolicy(session, user);
    }

    private LocalePreferences readLocalePreferences(DataSession session, DataObject user, BusinessLogics businessLogics, String clientLanguage, String clientCountry, ExecutionStack stack) throws SQLException, SQLHandledException {
        return new LocalePreferences(locale,
                (String) businessLogics.authenticationLM.timeZone.read(session, user),
                (Integer) businessLogics.authenticationLM.twoDigitYearStart.read(session, user),
                (String) businessLogics.authenticationLM.dateFormat.read(session, user),
                (String) businessLogics.authenticationLM.timeFormat.read(session, user));
    }

    public void logClientException(String hostname, Throwable t) {
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

    private static class WeakChangesUserProvider extends ChangesController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<ChangesSync> weakThis;

        private WeakChangesUserProvider(ChangesSync dbManager) {
            this.weakThis = new WeakReference<>(dbManager);
        }

        @Override
        public DBManager getDbManager() {
            ChangesSync changesSync = weakThis.get();
            if(changesSync != null)
                return changesSync.getDbManager();
            return null;
        }

        public void regLocalChange(ImSet<Property> changes, DataSession session) {
            ChangesSync changesSync = weakThis.get();
            if(changesSync != null)
                changesSync.regLocalChange(changes, session);
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
        boolean showDetailedInfo;
        boolean devMode;
        String projectLSFDir;
        ColorTheme colorTheme;
        ColorPreferences colorPreferences;
        List<String> preDefinedDateRangesNames = new ArrayList<>();

        try (DataSession session = createSession()) {
            currentUserName = nvl((String) businessLogics.authenticationLM.currentUserName.read(session), "(без имени)");
            fontSize = (Integer) businessLogics.authenticationLM.userFontSize.read(session, user);
            useBusyDialog = Settings.get().isBusyDialog() || SystemProperties.inTestMode || businessLogics.serviceLM.useBusyDialog.read(session) != null;
            useRequestTimeout = Settings.get().isUseRequestTimeout() || businessLogics.serviceLM.useRequestTimeout.read(session) != null;
            forbidDuplicateForms = businessLogics.securityLM.forbidDuplicateFormsCustomUser.read(session, user) != null;
            showDetailedInfo = businessLogics.securityLM.showDetailedInfoCustomUser.read(session, user) != null;

            devMode = SystemProperties.inDevMode || businessLogics.serviceLM.devMode.read(session) != null;
            projectLSFDir = SystemProperties.projectLSFDir;

            String colorThemeStaticName = (String) businessLogics.authenticationLM.colorThemeStaticName.read(session, user);
            String colorThemeString = colorThemeStaticName != null ? colorThemeStaticName.substring(colorThemeStaticName.indexOf(".") + 1) : null; 
            colorTheme = BaseUtils.nvl(ColorTheme.get(colorThemeString), ColorTheme.DEFAULT);

            Color selectedRowBackground = (Color) businessLogics.serviceLM.overrideSelectedRowBackgroundColor.read(session);
            Color selectedCellBackground = (Color) businessLogics.serviceLM.overrideSelectedCellBackgroundColor.read(session);
            Color focusedCellBackground = (Color) businessLogics.serviceLM.overrideFocusedCellBackgroundColor.read(session);
            Color focusedCellBorder = (Color) businessLogics.serviceLM.overrideFocusedCellBorderColor.read(session);
            Color tableGridColor = (Color) businessLogics.serviceLM.overrideTableGridColor.read(session);
            colorPreferences = new ColorPreferences(selectedRowBackground, selectedCellBackground,
                    focusedCellBackground, focusedCellBorder, tableGridColor);

            fillRanges((String) businessLogics.authenticationLM.dateTimePickerRanges.read(session, user), preDefinedDateRangesNames);
            fillRanges((String) businessLogics.authenticationLM.intervalPickerRanges.read(session, user), preDefinedDateRangesNames);
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
        return new ClientSettings(localePreferences, currentUserName, fontSize, useBusyDialog, Settings.get().getBusyDialogTimeout(),
                useRequestTimeout, devMode, projectLSFDir, showDetailedInfo, forbidDuplicateForms, Settings.get().isShowNotDefinedStrings(),
                Settings.get().isPivotOnlySelectedColumn(), colorTheme, colorPreferences, preDefinedDateRangesNames.toArray(new String[0]));
    }

    private void fillRanges(String json, List<String> ranges) {
        JSONArray rangesJson = new JSONArray(json);
        for (int i = 0; i < rangesJson.length(); i++) {
            ranges.add(rangesJson.getJSONObject(i).getString("range"));
        }
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

            Map<Long, LocalDateTime> connectionActivityMap = new HashMap<>(RemoteLoggerAspect.connectionActivityMap);
            RemoteLoggerAspect.connectionActivityMap.clear();

            for (Map.Entry<Long, LocalDateTime> connectionActivity : connectionActivityMap.entrySet()) {
                DataObject connection = session.getDataObject(businessLogics.systemEventsLM.connection, connectionActivity.getKey());
                businessLogics.systemEventsLM.lastActivity.change(connectionActivity.getValue(), session, connection);
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
                        DataObject dateFrom = new DataObject(sqlTimestampToLocalDateTime(new Timestamp(pingEntry.getKey())), DateTimeClass.instance);
                        DataObject dateTo = new DataObject(sqlTimestampToLocalDateTime(new Timestamp(pingEntry.getValue().get(0))), DateTimeClass.instance);
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
    public ServerResponse executeNavigatorAction(long requestIndex, long lastReceivedRequestIndex, final String script) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> evaluateRun(script));
    }

    private void evaluateRun(String script) throws SQLException, SQLHandledException {
        try (DataSession session = createSession()) {
            LA runAction = businessLogics.evaluateRun(script, false);
            if (runAction != null) {
                runAction.execute(session, getStack());
            }
        }
    }

    @Override
    public ServerResponse executeNavigatorAction(long requestIndex, long lastReceivedRequestIndex, final String actionSID, final int type) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
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
        });
    }

    @Override
    protected ServerResponse prepareResponse(long requestIndex, List<ClientAction> pendingActions, ExecutionStack stack, boolean forceLocalEvents) {
        return new ServerResponse(requestIndex, pendingActions.toArray(new ClientAction[pendingActions.size()]), false);
    }

    @Override
    public Pair<RemoteFormInterface, String> createFormExternal(String json) {

        JSONObject jsonObject = new JSONObject(json);
        String name = jsonObject.optString("name");
        String script = "";
        if (name.isEmpty()) {
            name = "external";
            script += "FORM " + name + " " + jsonObject.optString("script") + ";" + '\n';
        }
        script += "run() { SHOW " + name + "; }";

        RemoteForm remoteForm;

        RemoteNavigatorContext navigatorContext = (RemoteNavigatorContext) this.context;
        navigatorContext.pushGetForm();
        try {
            evaluateRun(script);
        } catch (Throwable t) {
            navigatorContext.popGetForm();
            throw Throwables.propagate(t);
        } finally {
            remoteForm = navigatorContext.popGetForm();
        }

        JSONObject result = new JSONObject();
        result.put("initial", remoteForm.getFormChangesExternal(getStack()).get("modify"));
        result.put("meta", remoteForm.getMetaExternal().serialize());
        return new Pair<>(remoteForm, result.toString());
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

            if (!securityPolicy.checkNavigatorPermission(element)) {
                throw new RuntimeException(ThreadLocalContext.localize("{form.navigator.not.enough.permissions}"));
            }

            action = new LA(((NavigatorAction) element).getAction());
        } else {
            action = businessLogics.findAction(canonicalName);
        }
        action.execute(session, stack);
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
    public void executeNotificationAction(ExecutionEnvironment env, ExecutionStack stack, Integer idNotification) {
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
    private static class ChangesSync extends ChangesController {

        private DBManager dbManager;

        private final Map<Property, LastChanges> changes = MapFact.mAddRemoveMap();
        private final WeakIdentityHashMap<FormInstance, Long> formStamps = new WeakIdentityHashMap<>();
        private long minPrevUpdatedStamp = 0;
        private long currentStamp = 0;

        public ChangesSync(DBManager dbManager) {
            this.dbManager = dbManager;
        }

        public DBManager getDbManager() {
            return dbManager;
        }

        private void updateLastStamp(long prevStamp) {
            assert !formStamps.isEmpty();
            if(minPrevUpdatedStamp >= prevStamp) {
                minPrevUpdatedStamp = currentStamp; // ищем новый stamp
                for(Pair<FormInstance, Long> entry : formStamps.entryIt())
                    if(entry.second < minPrevUpdatedStamp)
                        minPrevUpdatedStamp = entry.second;

                // удаляем все меньше minStamp
                changes.entrySet().removeIf(entry -> entry.getValue().timeStamp <= minPrevUpdatedStamp);
            }
        }

        public synchronized void regLocalChange(ImSet<Property> updateChanges, DataSession session) {
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

    private final ChangesSync changesSync;

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
    public Object getProfiledObject() {
        return "n";
    }
}