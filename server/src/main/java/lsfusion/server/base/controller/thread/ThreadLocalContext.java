package lsfusion.server.base.controller.thread;

import com.google.common.base.Throwables;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentWeakHashMap;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.form.ModalityType;
import lsfusion.server.base.controller.context.AbstractContext;
import lsfusion.server.base.controller.context.Context;
import lsfusion.server.base.controller.manager.EventServer;
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.remote.RmiManager;
import lsfusion.server.base.controller.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.base.controller.remote.manager.RmiServer;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.controller.stack.NewThreadExecutionStack;
import lsfusion.server.logics.action.controller.stack.SyncExecutionStack;
import lsfusion.server.logics.action.controller.stack.TopExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.controller.manager.RestartManager;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.action.async.InputList;
import lsfusion.server.logics.form.interactive.action.input.InputContext;
import lsfusion.server.logics.form.interactive.action.input.InputResult;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.navigator.controller.manager.NavigatorsManager;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.LogInfo;
import lsfusion.server.physics.admin.log.RemoteLoggerAspect;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class ThreadLocalContext {
    private static final ThreadLocal<Context> context = new ThreadLocal<>();
    private static final ThreadLocal<Settings> settings = new ThreadLocal<>();
    private static final ThreadLocal<Stack<Settings>> prevSettings = new ThreadLocal<>();
    private static ConcurrentHashMap<Long, Settings> roleSettingsMap = new ConcurrentHashMap<>();
    public static ConcurrentWeakHashMap<Thread, LogInfo> logInfoMap = new ConcurrentWeakHashMap<>();
    public static ConcurrentWeakHashMap<Thread, Boolean> activeMap = new ConcurrentWeakHashMap<>();
    public static Context get() { // временно, потом надо private сделать
        return context.get();
    }
    public static void assureContext(Context context) { // временно, должно уйти
        assure(context, null);
    }

    private static void set(Context c) {
        context.set(c);
    }

    private static void setLogInfo(Context c) {
        if (c != null) {
            LogInfo logInfo = c.getLogInfo();
            if (logInfo != null) {
                if (logInfo.userName != null)
                    MDC.put("client", logInfo.userName);
                if (logInfo.hostnameComputer != null)
                    MDC.put("computer", logInfo.hostnameComputer);
                if (logInfo.remoteAddress != null)
                    MDC.put("remoteAddress", logInfo.remoteAddress);
                logInfoMap.put(Thread.currentThread(), logInfo);
                activeMap.put(Thread.currentThread(), true);
            }
        } else
            activeMap.put(Thread.currentThread(), false);
    }

    public static void setSettings() {
        try {
            settings.set(getRoleSettings(getCurrentRole(), true));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static void dropSettings() {
        settings.set(null);
    }

    public static void pushSettings(String nameProperty, String valueProperty) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, CloneNotSupportedException {
        Settings overrideSettings = settings.get().cloneSettings();
        setPropertyValue(overrideSettings, nameProperty, valueProperty);
        if(prevSettings.get() == null)
            prevSettings.set(new Stack());
        prevSettings.get().push(settings.get());
        settings.set(overrideSettings);
    }

    public static void popSettings(String nameProperty) {
        //на данный момент не важно, какое свойство передано в pop, выбирается верхнее по стеку и нет никакой проверки
        settings.set(prevSettings.get().pop());
    }

    public static void setPropertyValue(Settings settings, String nameProperty, String valueProperty) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class type = PropertyUtils.getPropertyType(settings, nameProperty);
        if(type == Boolean.TYPE)
            BeanUtils.setProperty(settings, nameProperty, valueProperty.equals("true"));
        else if(type == Integer.TYPE)
            BeanUtils.setProperty(settings, nameProperty, Integer.valueOf(valueProperty));
        else if(type == Double.TYPE)
            BeanUtils.setProperty(settings, nameProperty, Double.valueOf(valueProperty));
        else if(type == Long.TYPE)
            BeanUtils.setProperty(settings, nameProperty, Long.valueOf(valueProperty));
        else
            BeanUtils.setProperty(settings, nameProperty, trimToEmpty(valueProperty));
    }

    public static Long getCurrentUser() {
        return get().getCurrentUser();
    }

    public static Long getCurrentComputer() {
        return get().getCurrentComputer();
    }

    public static Long getCurrentConnection() {
        return get().getCurrentConnection();
    }

    public static Long getCurrentRole() {
        return get().getCurrentUserRole();
    }

    public static LogicsInstance getLogicsInstance() {
        return get().getLogicsInstance();
    }

    public static CustomClassListener getClassListener() {
        return get().getClassListener();
    }

    public static BusinessLogics getBusinessLogics() {
        return getLogicsInstance().getBusinessLogics();
    }
    public static BaseLogicsModule getBaseLM() {
        return getBusinessLogics().LM;
    }

    public static NavigatorsManager getNavigatorsManager() {
        return getLogicsInstance().getNavigatorsManager();
    }

    public static RestartManager getRestartManager() {
        return getLogicsInstance().getRestartManager();
    }

    public static SecurityManager getSecurityManager() {
        return getLogicsInstance().getSecurityManager();
    }

    public static DBManager getDbManager() {
        return getLogicsInstance().getDbManager();
    }

    public static DataSession createSession() throws SQLException {
        return getDbManager().createSession();
    }

    public static RmiManager getRmiManager() {
        return getLogicsInstance().getRmiManager();
    }

    public static Settings getSettings() {
        return settings.get();
    }

    public static Settings getRoleSettings(Long role, boolean clone) throws CloneNotSupportedException {
        if (role == null) //системный процесс или пользователь без роли
            return getLogicsInstance().getSettings();
        else {
            Settings roleSettings = roleSettingsMap.get(role);
            if (roleSettings == null && clone) {

                //клонируем settings для новой роли
                roleSettings = getLogicsInstance().getSettings().cloneSettings();

                roleSettingsMap.put(role, roleSettings);
            }
            return roleSettings;
        }
    }

    public static FormEntity getCurrentForm() {
        return get().getCurrentForm();
    }

    public static FormInstance createFormInstance(FormEntity formEntity, ImSet<ObjectEntity> inputObjects, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, ExecutionStack stack, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, boolean checkOnOk, boolean showDrop, boolean interactive, boolean isFloat, ImSet<ContextFilterInstance> contextFilters, boolean readonly) throws SQLException, SQLHandledException {
        return get().createFormInstance(formEntity, inputObjects, mapObjects, session, isModal, noCancel, manageSession, stack, checkOnOk, showDrop, interactive, isFloat, contextFilters, readonly);
    }

    public static InputContext lockInputContext() {
        return get().lockInputContext();
    }
    public static void unlockInputContext() {
        get().unlockInputContext();
    }
    public static InputResult inputUserData(DataClass dataClass, Object oldValue, boolean hasOldValue, InputContext inputContext, InputList inputList) {
        return get().inputUserData(dataClass, oldValue, hasOldValue, inputContext, inputList);
    }

    public static ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete) {
        return get().requestUserClass(baseClass, defaultValue, concrete);
    }

    public static void pushLogMessage() {
        get().pushLogMessage();
    }
    public static ImList<AbstractContext.LogMessage> popLogMessage() {
        return get().popLogMessage();
    }

    public static void delayUserInteraction(ClientAction action) {
        get().delayUserInteraction(action);
    }

    public static Object requestUserInteraction(ClientAction action) {
        return get().requestUserInteraction(action);
    }

    public static void requestFormUserInteraction(FormInstance remoteForm, ModalityType modalityType, boolean forbidDuplicate, ExecutionStack stack) throws SQLException, SQLHandledException {
        get().requestFormUserInteraction(remoteForm, modalityType, forbidDuplicate, stack);
    }

    public static boolean canBeProcessed() {
        return get().canBeProcessed();
    }

    public static Object[] requestUserInteraction(ClientAction... actions) {
        return get().requestUserInteraction(actions);
    }

    // есть пока всего одна ветка с assertTop (кроме wrapContext) - rmicontextobject, да и то не до конца понятно в каких стеках

    private static final ThreadLocal<NewThreadExecutionStack> stack = new ThreadLocal<>();
    private static void assure(Context context, NewThreadExecutionStack stack) {
        // nullable stack временно в assure
        Context prevContext = ThreadLocalContext.get();
        if(prevContext == null || !prevContext.equals(context)) {
            ServerLoggers.assertLog(false, "DIFFERENT CONTEXT, PREV : " + prevContext + ", SET : " + context);
            ThreadLocalContext.set(context);
            ThreadLocalContext.setLogInfo(context);
        }

        if(stack != null) {
            NewThreadExecutionStack prevStack = ThreadLocalContext.getStack();
            if(prevStack == null || !prevStack.checkStack(stack)) {
                ServerLoggers.assertLog(false, "DIFFERENT STACK, PREV : " + prevStack + ", SET : " + stack);
                ThreadLocalContext.stack.set(stack);
            }
        }
    }
    public static NewThreadExecutionStack getStack() {
        return stack.get();
    }
    
    private static void checkThread(Context prevContext, boolean assertTop, ThreadInfo threadInfo) {
        ServerLoggers.assertLog(!(assertTop && prevContext != null), "ASSERT TOP EXECUTION");
        if(assertTop && (Thread.currentThread() instanceof ExecutorFactory.ClosableDaemonThreadFactory.ClosableThread) != (threadInfo instanceof ExecutorFactoryThreadInfo))
            ServerLoggers.assertLog(false, "CLOSABLE THREAD != EXECUTOR FACTORY THREAD");
    }

    public static class AspectState {
        public final Context context;
        public final NewThreadExecutionStack stack;

        public AspectState(Context context, NewThreadExecutionStack stack) {
            this.context = context;
            this.stack = stack;
        }
    }
    
    private static AspectState aspectBefore(Context context, boolean assertTop, ThreadInfo threadInfo, NewThreadExecutionStack stack, SyncType type) { // type - не null, значит новый поток с заданным типом синхронизации
        Context prevContext = ThreadLocalContext.get();
        NewThreadExecutionStack prevStack = ThreadLocalContext.getStack();

        ThreadLocalContext.set(context);
        if(prevContext == null) {
            //необходимо выполнить раньше вызова setLogInfo
            ThreadLocalContext.setSettings();
        }
        ThreadLocalContext.setLogInfo(context);
        ThreadLocalContext.stack.set(stack);

        if(prevContext == null) {
            Thread currentThread = Thread.currentThread();
            long pid = currentThread.getId();
            RemoteLoggerAspect.putDateTimeCall(pid, new Timestamp(System.currentTimeMillis()));

            if(threadInfo instanceof EventThreadInfo) { // можно было попытаться старое имя сохранить, но оно по идее может меняться тогда очень странная логика получится
                currentThread.setName(((EventThreadInfo) threadInfo).getEventName() + " - " + currentThread.getId());
            }
        }

        checkThread(prevContext, assertTop, threadInfo);

        return new AspectState(prevContext, prevStack);
    }
    private static void aspectAfter(AspectState prevState, boolean assertTop, ThreadInfo threadInfo) {
        Context prevContext = prevState != null ? prevState.context : null;
        NewThreadExecutionStack prevStack = prevState != null ? prevState.stack : null;

        checkThread(prevContext, assertTop, threadInfo);

        if(prevContext == null) {
            long pid = Thread.currentThread().getId();
            RemoteLoggerAspect.removeDateTimeCall(pid);

            if(threadInfo instanceof EventThreadInfo) {
                // тут можно было бы сбросить имя потока, но пока сохраним (также как и в SQLSession)                
                if (Settings.get().isEnableCloseThreadLocalSqlInNativeThreads()) // закрываем connection, чтобы не мусорить
                    ThreadLocalContext.getLogicsInstance().getDbManager().closeThreadLocalSql();
            }
            ThreadLocalContext.dropSettings();
        }


        ThreadLocalContext.set(prevContext);
        ThreadLocalContext.setLogInfo(prevContext);
        ThreadLocalContext.stack.set(prevStack);
    }

    private static void assureEvent(Context context, NewThreadExecutionStack stack) {
        assure(context, stack);
    }
    private static AspectState aspectBeforeEvent(Context context, boolean assertTop, ThreadInfo threadInfo, NewThreadExecutionStack stack, SyncType type) {
        return aspectBefore(context, assertTop, threadInfo, stack, type); // здесь stack не вызова, а "общий" стек, поэтому оборачивать его по сути не надо
    }
    private static void aspectAfterEvent(AspectState prevState, boolean assertTop, ThreadInfo threadInfo) {
        aspectAfter(prevState, assertTop, threadInfo);
    }
    private static void assureEvent(LogicsInstance instance, NewThreadExecutionStack stack) {
        assure(instance.getContext(), stack);
    }
    private static void aspectBeforeEvent(LogicsInstance instance, NewThreadExecutionStack stack, ThreadInfo threadInfo, SyncType mirror) {
        aspectBeforeEvent(instance, stack, threadInfo, true, mirror);
    }
    private static AspectState aspectBeforeEvent(LogicsInstance instance, NewThreadExecutionStack stack, ThreadInfo threadInfo, boolean assertTop, SyncType mirror) {
        return aspectBeforeEvent(instance.getContext(), assertTop, threadInfo, stack, mirror);
    }
    private static void aspectAfterEvent(ThreadInfo threadInfo) {
        aspectAfterEvent(null, true, threadInfo);
    }
    private static NewThreadExecutionStack eventStack(EventServer eventServer) {
        return eventServer.getTopStack();
    }

    // RMI вызовы
    private static NewThreadExecutionStack rmiStack(ContextAwarePendingRemoteObject context) {
        return context.getRmiStack();
    }
    public static void assureRmi(ContextAwarePendingRemoteObject context) { // не уверен что всегда устанавливается
        assureEvent(context.getContext(), rmiStack(context));
    }
    public static AspectState aspectBeforeRmi(ContextAwarePendingRemoteObject object, boolean assertTop, ThreadInfo threadInfo) { // rmi + unreferenced и несколько мелочей
        return aspectBeforeRmi(object, assertTop, threadInfo, null);
    }
    public static AspectState aspectBeforeRmi(ContextAwarePendingRemoteObject object, boolean assertTop, ThreadInfo threadInfo, SyncType mirror) { // rmi + unreferenced и несколько мелочей
        return aspectBeforeEvent(object.getContext(), assertTop, threadInfo, rmiStack(object), mirror);
    }
    public static void aspectAfterRmi(AspectState prevState, boolean assertTop, ThreadInfo threadInfo) {
        aspectAfterEvent(prevState, assertTop, threadInfo);
    }
    public static void aspectAfterRmi(ThreadInfo threadInfo) {
        aspectAfterEvent(threadInfo);
    }
    private static NewThreadExecutionStack rmiStack(RmiServer remoteServer) {
        return eventStack(remoteServer);
    }
    public static void assureRmi(RmiServer remoteServer) { // Андрей уверяет что не всегда устанавливается
        assureEvent(remoteServer.getLogicsInstance(), rmiStack(remoteServer));
    }
    public static AspectState aspectBeforeRmi(RmiServer remoteServer, boolean assertTop, ThreadInfo threadInfo) {
        return aspectBeforeEvent(remoteServer.getLogicsInstance(), rmiStack(remoteServer), threadInfo, assertTop, null);
    }

    // MONITOR вызовы, когда вызов осуществляется "чужим" потоком (чтение из socket'а, servlet'ом и т.п.)
    private static NewThreadExecutionStack monitorStack(MonitorServer monitor) {
        return eventStack(monitor);
    }
    public static void assureMonitor(MonitorServer monitor) {
        assureEvent(monitor.getLogicsInstance(), monitorStack(monitor));
    }
    public static void aspectBeforeMonitor(MonitorServer monitor, ThreadInfo threadInfo) {
        aspectBeforeMonitor(monitor, threadInfo, null);
    }
    public static void aspectBeforeMonitor(MonitorServer monitor, ThreadInfo threadInfo, SyncType type) {
        aspectBeforeEvent(monitor.getLogicsInstance(), monitorStack(monitor), threadInfo, type);
    }
    public static void aspectAfterMonitor(ThreadInfo threadInfo) {
        aspectAfterEvent(threadInfo);
    }

    public static void aspectBeforeMonitorHTTP(MonitorServer monitor) {
        aspectBeforeMonitor(monitor, EventThreadInfo.HTTP(monitor));
    }
    public static void aspectAfterMonitorHTTP(MonitorServer monitor) {
        aspectAfterMonitor(EventThreadInfo.HTTP(monitor));
    }
    
    // СТАРТ СЕРВЕРА

    private final static TopExecutionStack lifecycleStack = new TopExecutionStack("init");
    public static void assureLifecycle(LogicsInstance logicsInstance) { // nullable
        assureEvent(logicsInstance, lifecycleStack);
    }
    public static void aspectBeforeLifecycle(LogicsInstance context, ThreadInfo threadInfo) {
        aspectBeforeLifecycle(context, threadInfo, null);
    }
    public static void aspectBeforeLifecycle(LogicsInstance context, ThreadInfo threadInfo, SyncType mirror) {
        aspectBeforeEvent(context, lifecycleStack, threadInfo, mirror);
    }
    public static void aspectAfterLifecycle(ThreadInfo threadInfo) {
        aspectAfterEvent(threadInfo);
    }

    // вызов из другого контекста в стеке
    public static Context assureContext(ExecutionContext<PropertyInterface> context) {
        return get();
//        assure(get(), getStack());
    }
    public static void aspectBeforeContext(Context exContext, ExecutionContext<PropertyInterface> context, SyncType type) { // проблема в том, что ExecutionContext не умеет возвращать свой контекст и его приходится передавать в явну.
        aspectBefore(exContext, true, ExecutorFactoryThreadInfo.instance, SyncExecutionStack.newThread(context.stack, "new-thread", type), type);
    }
    public static void aspectAfterContext() {
        aspectAfter(null, true, ExecutorFactoryThreadInfo.instance);
    }

    public static String localize(LocalizedString s) {
        return s == null ? null : safeLocalize(s);    
    }
    
    public static String localize(String s) {
        return s == null ? null : safeLocalize(LocalizedString.create(s));
    } 

    public static String localize(LocalizedString s, Locale locale) {
        return s == null ? null : safeLocalize(s, locale);
    }
    
    private static String safeLocalize(LocalizedString s) {
        Context context = get();
        if(context == null) { // так как может вызываться в toString, а он в свою очередь может вызываться в случайных потоках (например см. ContextAwarePendingRemoteObject.toString), поэтому на всякий случай проверяем 
            ServerLoggers.assertLog(false, "NO CONTEXT WHEN LOCALIZED");
            return s.getSourceString();
        }
        return context.localize(s); 
    }
    private static String safeLocalize(LocalizedString s, Locale locale) {
        Context context = get();
        if(context == null) { // так как может вызываться в toString, а он в свою очередь может вызываться в случайных потоках (например см. ContextAwarePendingRemoteObject.toString), поэтому на всякий случай проверяем
            ServerLoggers.assertLog(false, "NO CONTEXT WHEN LOCALIZED");
            return s.getSourceString();
        }
        return context.localize(s, locale);
    }
}
