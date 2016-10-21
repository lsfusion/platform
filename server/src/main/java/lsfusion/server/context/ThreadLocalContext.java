package lsfusion.server.context;

import lsfusion.base.ConcurrentWeakHashMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.WrapperContext;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.lifecycle.EventServer;
import lsfusion.server.lifecycle.MonitorServer;
import lsfusion.server.logics.*;
import lsfusion.server.logics.SecurityManager;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.DialogRequest;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.remote.ContextAwarePendingRemoteObject;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.remote.RmiServer;
import lsfusion.server.session.DataSession;
import lsfusion.server.stack.ExecutionStackItem;
import lsfusion.server.stack.ProgressStackItem;
import org.apache.log4j.MDC;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ThreadLocalContext {
    private static final ThreadLocal<Context> context = new ThreadLocal<>();
    public static ConcurrentWeakHashMap<Thread, LogInfo> logInfoMap = new ConcurrentWeakHashMap<>();
    public static Context get() { // временно, потом надо private сделать
        return context.get();
    }
    public static void assureContext(Context context) { // временно, должно уйти
        assure(context, null);
    }

    private static void set(Context c) {
        context.set(c);
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
            }
        }
    }

    public static Integer getCurrentUser() {
        return get().getCurrentUser();
    }

    public static LogicsInstance getLogicsInstance() {
        return get().getLogicsInstance();
    }

    public static BusinessLogics getBusinessLogics() {
        return getLogicsInstance().getBusinessLogics();
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

    public static RMIManager getRmiManager() {
        return getLogicsInstance().getRmiManager();
    }

    public static Settings getSettings() {
        return getLogicsInstance().getSettings();
    }

    public static FormInstance getFormInstance() {
        return get().getFormInstance();
    }

    public static FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, ExecutionStack stack, DataSession session, boolean isModal, boolean isAdd, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters, PropertyDrawEntity initFilterProperty, ImSet<PullChangeProperty> pullProps, boolean readonly) throws SQLException, SQLHandledException {
        return get().createFormInstance(formEntity, mapObjects, session, isModal, isAdd, sessionScope, stack, checkOnOk, showDrop, interactive, contextFilters, initFilterProperty, pullProps, readonly);
    }

    public static RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
        return get().createRemoteForm(formInstance, stack);
    }

    public static ObjectValue requestUserObject(DialogRequest dialogRequest, ExecutionStack stack) throws SQLException, SQLHandledException {
        return get().requestUserObject(dialogRequest, stack);
    }

    public static ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        return get().requestUserData(dataClass, oldValue);
    }

    public static DataObject getConnection() {
        return get().getConnection();
    }

    public static ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete) {
        return get().requestUserClass(baseClass, defaultValue, concrete);
    }

    public static String getLogMessage() {
        return get().getLogMessage();
    }

    public static void delayUserInteraction(ClientAction action) {
        get().delayUserInteraction(action);
    }

    public static Object requestUserInteraction(ClientAction action) {
        return get().requestUserInteraction(action);
    }

    public static boolean canBeProcessed() {
        return get().canBeProcessed();
    }

    public static Object[] requestUserInteraction(ClientAction... actions) {
        return get().requestUserInteraction(actions);
    }

    public static String getActionMessage() {
        return get() != null ? get().getActionMessage() : "";
    }

    public static List<Object> getActionMessageList() {
        return get() != null ? get().getActionMessageList() : new ArrayList<>();
    }

    public static Thread getLastThread() {
        return get() != null ? get().getLastThread() : null;
    }

    public static ProgressStackItem pushProgressMessage(String message, Integer progress, Integer total) {
        ProgressStackItem progressStackItem = new ProgressStackItem(message, progress, total);
        pushActionMessage(progressStackItem);
        return progressStackItem;
    }

    public static void pushActionMessage(ExecutionStackItem stackItem) {
        if (get() != null) {
            get().pushActionMessage(stackItem);
        }
    }

    public static void popActionMessage(ExecutionStackItem stackItem) {
        if(get() != null && stackItem != null)
            get().popActionMessage(stackItem);
    }

    // есть пока всего одна ветка с assertTop (кроме wrapContext) - rmicontextobject, да и то не до конца понятно в каких стеках

    private static final ThreadLocal<NewThreadExecutionStack> stack = new ThreadLocal<>();
    private static void assure(Context context, NewThreadExecutionStack stack) {
        // nullable stack временно в assure
        Context prevContext = ThreadLocalContext.get();
        if(prevContext == null || !prevContext.equals(context)) {
            ServerLoggers.assertLog(false, "DIFFERENT CONTEXT, PREV : " + prevContext + ", SET : " + context);
            ThreadLocalContext.set(context);
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
    private static Context aspectBefore(Context context, boolean assertTop, NewThreadExecutionStack stack, SyncType type) { // type - не null, значит новый поток с заданным типом синхронизации
        Context prevContext = ThreadLocalContext.get();
        assert !(assertTop && prevContext != null);
        ThreadLocalContext.set(context);

        ThreadLocalContext.stack.set(stack);

        return prevContext;
    }
    private static void aspectAfter(Context prevContext, boolean assertTop) {
        ThreadLocalContext.set(prevContext);
    }

    private static void assureEvent(Context context, NewThreadExecutionStack stack) {
        assure(context, stack);
    }
    private static Context aspectBeforeEvent(Context context, boolean assertTop, NewThreadExecutionStack stack, SyncType type) {
        return aspectBefore(context, assertTop, stack, type); // здесь stack не вызова, а "общий" стек, поэтому оборачивать его по сути не надо
    }
    private static void aspectAfterEvent(Context prevContext, boolean assertTop) {
        aspectAfter(prevContext, assertTop);
    }
    private static void assureEvent(LogicsInstance instance, NewThreadExecutionStack stack) {
        assure(instance.getContext(), stack);
    }
    private static void aspectBeforeEvent(LogicsInstance instance, NewThreadExecutionStack stack, SyncType mirror) {
        aspectBeforeEvent(instance.getContext(), true, stack, mirror);
    }
    private static void aspectAfterEvent() {
        aspectAfterEvent(null, true);
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
    public static Context aspectBeforeRmi(ContextAwarePendingRemoteObject object, boolean assertTop) { // rmi + unreferenced и несколько мелочей
        return aspectBeforeRmi(object, assertTop, null);
    }
    public static Context aspectBeforeRmi(ContextAwarePendingRemoteObject object, boolean assertTop, SyncType mirror) { // rmi + unreferenced и несколько мелочей
        return aspectBeforeEvent(object.getContext(), assertTop, rmiStack(object), mirror);
    }
    public static void aspectAfterRmi(Context prevContext, boolean assertTop) {
        aspectAfterEvent(prevContext, assertTop);
    }
    private static NewThreadExecutionStack rmiStack(RmiServer remoteServer) {
        return eventStack(remoteServer);
    }
    public static void assureRmi(RmiServer remoteServer) { // Андрей уверяет что не всегда устанавливается
        assureEvent(remoteServer.getLogicsInstance(), rmiStack(remoteServer));
    }
    public static void aspectBeforeRmi(RmiServer remoteServer) {
        aspectBeforeRmi(remoteServer, null);
    }
    public static void aspectBeforeRmi(RmiServer remoteServer, SyncType type) {
        aspectBeforeEvent(remoteServer.getLogicsInstance(), rmiStack(remoteServer), type);
    }
    public static void aspectAfterRmi() {
        aspectAfterEvent();
    }

    // MONITOR вызовы, когда вызов осуществляется "чужим" потоком (чтение из socket'а, servlet'ом и т.п.)
    private static NewThreadExecutionStack monitorStack(MonitorServer monitor) {
        return eventStack(monitor);
    }
    public static void assureMonitor(MonitorServer monitor) {
        assureEvent(monitor.getLogicsInstance(), monitorStack(monitor));
    }
    public static void aspectBeforeMonitor(MonitorServer monitor) {
        aspectBeforeMonitor(monitor, null);
    }
    public static void aspectBeforeMonitor(MonitorServer monitor, SyncType type) {
        aspectBeforeEvent(monitor.getLogicsInstance(), monitorStack(monitor), type);
    }
    public static void aspectAfterMonitor() {
        aspectAfterEvent();
    }

    // СТАРТ СЕРВЕРА

    private final static TopExecutionStack lifecycleStack = new TopExecutionStack("init");
    public static void assureLifecycle(LogicsInstance logicsInstance) { // nullable
        assureEvent(logicsInstance, lifecycleStack);
    }
    public static void aspectBeforeLifecycle(LogicsInstance context) {
        aspectBeforeLifecycle(context, null);
    }
    public static void aspectBeforeLifecycle(LogicsInstance context, SyncType mirror) {
        aspectBeforeEvent(context, lifecycleStack, mirror);
    }
    public static void aspectAfterLifecycle() {
        aspectAfterEvent();
    }

    // вызов из другого контекста в стеке
    public static Context assureContext(ExecutionContext<PropertyInterface> context) {
        return get();
//        assure(get(), getStack());
    }
    public static void aspectBeforeContext(Context exContext, ExecutionContext<PropertyInterface> context, SyncType type) { // проблема в том, что ExecutionContext не умеет возвращать свой контекст и его приходится передавать в явну.
        aspectBefore(exContext, true, SyncExecutionStack.newThread(context.stack, "new-thread", type), type);
    }
    public static void aspectAfterContext() {
        aspectAfter(null, true);
    }

    public static Context wrapContext(WrapperContext context) {
        Context prevContext = get();
        context.setContext(prevContext);
        aspectBefore(context, false, getStack(), SyncType.SYNC);
        return prevContext;
    }

    public static void unwrapContext(Context prevContext) {
        aspectAfter(prevContext, false);
    }
    
    public static String localize(LocalizedString s) {
        return s == null ? null : get().localize(s);    
    }
    
    public static String localize(String s) {
        return s == null ? null : get().localize(LocalizedString.create(s));
    } 

    public static String localize(LocalizedString s, Locale locale) {
        return s == null ? null : get().localize(s, locale);
    }
}
