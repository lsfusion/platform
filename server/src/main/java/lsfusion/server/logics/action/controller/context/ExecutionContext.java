package lsfusion.server.logics.action.controller.context;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.lambda.Processor;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.form.ModalityWindowFormType;
import lsfusion.interop.form.ShowFormType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.base.controller.remote.RmiManager;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.controller.stack.SameThreadExecutionStack;
import lsfusion.server.logics.action.data.PropertyOrderSet;
import lsfusion.server.logics.action.implement.ActionValueImplement;
import lsfusion.server.logics.action.interactive.UserInteraction;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.classes.change.ClassChange;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClasses;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.action.session.controller.init.SessionCreator;
import lsfusion.server.logics.action.session.table.SinglePropertyTableUsage;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.controller.manager.RestartManager;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.action.async.*;
import lsfusion.server.logics.form.interactive.action.input.InputContext;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputResult;
import lsfusion.server.logics.form.interactive.action.input.RequestResult;
import lsfusion.server.logics.form.interactive.instance.FormEnvironment;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.navigator.controller.manager.NavigatorsManager;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ScheduledExecutorService;

public class ExecutionContext<P extends PropertyInterface> implements UserInteraction, SessionCreator {
    private ImMap<P, ? extends ObjectValue> keys;
    private Stack<UpdateCurrentClasses> updateClasses;
    public void pushUpdate(UpdateCurrentClasses push) {
        if(updateClasses == null)
            updateClasses = new Stack<>();
        updateClasses.push(push);
    }
    public void popUpdate() {
        updateClasses.pop();
    }

    public final ExecutionStack stack;

    private class ContextStack extends SameThreadExecutionStack {

        public ContextStack(ExecutionStack upStack) {
            super(upStack);
        }

        @Override
        protected DataSession getSession() {
            return env.getSession();
        }

        public ImMap<String, String> getAllParamsWithClassesInStack() {
            ImMap<String, String> result = MapFact.EMPTY();

            if(paramsToFQN != null) {
                result = paramsToFQN.addExcl(result);
            }

            if(newDebugStack)
                return result;

            return result.addExcl(super.getAllParamsWithClassesInStack());
        }

        public ImMap<String, ObjectValue> getAllParamsWithValuesInStack() {
            ImMap<String, ObjectValue> result = MapFact.EMPTY();

            if(paramsToInterfaces != null) {
                result = paramsToInterfaces.mapValues(ExecutionContext.this::getKeyValue).addExcl(result);
            }

            if(newDebugStack)
                return result;

            return result.addExcl(super.getAllParamsWithValuesInStack());
        }

        public ImSet<Pair<LP, List<ResolveClassSet>>> getAllLocalsInStack() {
            ImSet<Pair<LP, List<ResolveClassSet>>> result = SetFact.EMPTY();

            if(locals != null)
                result = result.addExcl(locals);

            if(newDebugStack)
                return result;

            return result.addExcl(super.getAllLocalsInStack());
        }

        public boolean hasNewDebugStack() {
            if(newDebugStack)
                return true;
            return super.hasNewDebugStack();
        }

        public Processor<ImMap<String, ObjectValue>> getWatcher() {
            if(watcher != null)
                return watcher;
            return super.getWatcher();
        }

        public void updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
            final ImMap<P, ? extends ObjectValue> prevKeys = keys;
            keys = session.updateCurrentClasses(keys);
            session.addRollbackInfo(() -> keys = prevKeys);

            if(updateClasses!=null) {
                for(UpdateCurrentClasses update : updateClasses)
                    update.updateCurrentClasses(session);
            }
            super.updateCurrentClasses(session);
        }
    }

    private final PushAsyncResult pushedAsyncResult; // чисто для асинхронного добавления объектов

    public final boolean hasMoreSessionUsages;
    
    private final ExecutionEnvironment env;

    private final ScheduledExecutorService executorService;

    private final FormEnvironment<P> form;

    // debug info 
    private ImRevMap<String, P> paramsToInterfaces;
    private ImMap<String, String> paramsToFQN;
    private ImSet<Pair<LP, List<ResolveClassSet>>> locals;
    private boolean newDebugStack;
    private Processor<ImMap<String, ObjectValue>> watcher;
    
//    public String actionName;
//    public int showStack() {
//        int level = 0;
//        if(stack != null) {
//            if(newDebugStack)
//                System.out.println("new debug");
//            level = stack.showStack();
//        }
//        if(actionName != null)
//            System.out.println((level++) + actionName + (newDebugStack ? " NEWSTACK" : ""));
//        return level;
//    } 
//
    public ExecutionContext(ImMap<P, ? extends ObjectValue> keys, ExecutionEnvironment env, ExecutionStack stack) {
        this(keys, null, env, null, null, stack, false);
    }

    public ExecutionContext(ImMap<P, ? extends ObjectValue> keys, PushAsyncResult pushedAsyncResult, ExecutionEnvironment env, ScheduledExecutorService executorService, FormEnvironment<P> form, ExecutionStack stack, boolean hasMoreSessionUsages) {
        this.keys = keys;
        this.pushedAsyncResult = pushedAsyncResult;
        this.env = env;
        this.executorService = executorService;
        this.form = form;
        this.stack = new ContextStack(stack);
        this.hasMoreSessionUsages = hasMoreSessionUsages;
    }
    
    public ExecutionContext<P> override() { // для дебаггера
        return new ExecutionContext<>(keys, pushedAsyncResult, env, executorService, form, stack, hasMoreSessionUsages);
    }
    
    public ExecutionContext<P> override(boolean hasMoreSessionUsages) {
        return new ExecutionContext<>(keys, pushedAsyncResult, env, executorService, form, stack, hasMoreSessionUsages);
    }

    public void setParamsToInterfaces(ImRevMap<String, P> paramsToInterfaces) {
        this.paramsToInterfaces = paramsToInterfaces;
    }

    public void setLocals(ImSet<Pair<LP, List<ResolveClassSet>>> locals) {
        this.locals = locals;
    }
    
    public void setNewDebugStack(boolean newDebugStack) {
        this.newDebugStack = newDebugStack;        
    }

    public boolean isPrevEventScope() { // если не в объявлении действия и не в локальном событии
        return getSession().isInSessionEvent() && !stack.hasNewDebugStack();
    }

    public void setWatcher(Processor<ImMap<String, ObjectValue>> watcher) {
        this.watcher = watcher;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public ImRevMap<String, P> getParamsToInterfaces() {
        return paramsToInterfaces;
    }

    public ImMap<String, String> getAllParamsWithClassesInStack() {
        ImMap<String, String> result = MapFact.EMPTY();
        
        if(paramsToFQN != null) {
            result = paramsToFQN.addExcl(result); // потому 
        }

        if(!newDebugStack && stack != null)
            result = result.addExcl(stack.getAllParamsWithClassesInStack());

        return result;
    }

    public ImMap<String, ObjectValue> getAllParamsWithValuesInStack() {
        ImMap<String, ObjectValue> result = MapFact.EMPTY();

        if(paramsToInterfaces != null) {
            result = paramsToInterfaces.mapValues(this::getKeyValue).addExcl(result);
        }

        if(!newDebugStack && stack != null)
            result = result.addExcl(stack.getAllParamsWithValuesInStack());

        return result;
    }

    public ImSet<Pair<LP, List<ResolveClassSet>>> getAllLocalsInStack() {
        ImSet<Pair<LP, List<ResolveClassSet>>> result = SetFact.EMPTY();
        
        if(locals != null)
            result = result.addExcl(locals);

        if(!newDebugStack && stack != null)
            result = result.addExcl(stack.getAllLocalsInStack());
        
        return result;
    }
    
    public Processor<ImMap<String, ObjectValue>> getWatcher() {
        if(watcher != null)
            return watcher;
        if(stack != null)
            return stack.getWatcher();
        return null;
    }

    public void setParamsToFQN(ImMap<String, String> paramsToFQN) {
        this.paramsToFQN = paramsToFQN;
    }

    public ExecutionEnvironment getEnv() {
        return env;
    }

    public ImMap<P, ? extends ObjectValue> getKeys() {
        return keys;
    }

    public ImMap<P, DataObject> getDataKeys() { // предполагается что вызывается из действий у которых !allowNulls
        return DataObject.assertDataObjects(getKeys());
    }

    public ObjectValue getKeyValue(P key) {
        return getKeys().get(key);
    }

    public DataObject getDataKeyValue(P key) {
        return getDataKeys().get(key);
    }

    public Object getKeyObject(P key) {
        return getKeyValue(key).getValue();
    }

    public ObjectValue getSingleKeyValue() {
        return getKeys().singleValue();
    }

    public DataObject getSingleDataKeyValue() {
        return getDataKeys().singleValue();
    }

    public Object getSingleKeyObject() {
        return getSingleKeyValue().getValue();
    }

    public int getKeyCount() {
        return getKeys().size();
    }

    public DataSession getSession() {
        return env.getSession();
    }

    public void delayUserInterfaction(ClientAction action) {
        ThreadLocalContext.delayUserInteraction(action);
    }

    public ExecutionEnvironment getSessionEventFormEnv() {
        return getFormInstance(true, false);
    }
    
    // подразумевают вызов только из Top Action'ов (FORM.NEW, form*, DefaultChange)
    public FormInstance getFormFlowInstance() {
        return getFormFlowInstance(true, true);
    }
    public FormInstance getFormFlowInstance(boolean assertExists, boolean sameSession) {
        return getFormInstance(sameSession, assertExists);
    }
    
    // использование формы, чисто для того чтобы передать дальше 
    public FormInstance getFormAspectInstance() {
        return getFormInstance(false, false);
    }

    public FormInstance getFormInstance(boolean sameSession, boolean assertExists) {
        FormInstance formInstance = form != null ? form.getInstance() : null;
        FormInstance formExecEnv = env.getFormInstance();
        
        if(formExecEnv != null) { // пока дублирующие механизмы, в будущем надо рефакторить
            // formInstance == null так как в события не всегда formEnv проталкивается
            ServerLoggers.assertLog(formInstance == null || formExecEnv == formInstance, "FORMS SHOULD BE EQUAL : ENV - " + formExecEnv + ", FORM - " + formInstance);
            return formExecEnv;
        }
        
        if(formInstance != null) {
            if (formInstance.getSession() == env) {
                ServerLoggers.assertLog(false, "FORM EXECUTION ENVIRONMENT DROPPED");
            } else {
                if (sameSession)
                    formInstance = null;
            }
        }
        
        if(assertExists && formInstance == null)
            ServerLoggers.assertLog(false, "FORM ALWAYS SHOULD EXIST");
        return formInstance;
    }

    //todo: закэшировать, если скорость доступа к ThreadLocal станет критичной
    //todo: сейчас по идее не актуально, т.к. большая часть времени в ActionProperty уходит на основную работу
    public LogicsInstance getLogicsInstance() {
        return ThreadLocalContext.getLogicsInstance();
    }
    
    private CustomClassListener getClassListener() {
        return ThreadLocalContext.getClassListener();
    }

    public BusinessLogics getBL() {
        return getLogicsInstance().getBusinessLogics();
    }

    public DBManager getDbManager() {
        return getLogicsInstance().getDbManager();
    }

    public NavigatorsManager getNavigatorsManager() {
        return getLogicsInstance().getNavigatorsManager();
    }

    public RestartManager getRestartManager() {
        return getLogicsInstance().getRestartManager();
    }

    public SecurityManager getSecurityManager() {
        return getLogicsInstance().getSecurityManager();
    }

    public RmiManager getRmiManager() {
        return getLogicsInstance().getRmiManager();
    }

    public static class NewSession<P extends PropertyInterface> extends ExecutionContext<P> implements AutoCloseable {

        public NewSession(ImMap<P, ? extends ObjectValue> keys, PushAsyncResult pushedAsyncResult, DataSession session, ScheduledExecutorService executorService, FormEnvironment<P> form, ExecutionStack stack) {
            super(keys, pushedAsyncResult, session, executorService, form, stack, false);
        }

        @Override
        public void close() throws SQLException {
            ((DataSession)getEnv()).close();
        }
    }
    public NewSession<P> newSession() throws SQLException { // the same as override, bu 
        return newSession(getSession().sql);
    }
    public NewSession<P> newSession(SQLSession sql) throws SQLException { // the same as override, bu 
        return new NewSession<>(keys, pushedAsyncResult, getSession().createSession(sql), executorService, form, stack);
    }

    public GroupObjectInstance getChangingPropertyToDraw() {
        PropertyDrawInstance drawInstance = form.getChangingDrawInstance();
        if(drawInstance==null)
            return null;
        return drawInstance.toDraw;
    }

    public ImMap<P, PropertyObjectInterfaceInstance> getObjectInstances() {
        return form!=null ? form.getMapObjects() : null;
    }

    public PropertyObjectInterfaceInstance getSingleObjectInstance() {
        ImMap<P, PropertyObjectInterfaceInstance> mapObjects = getObjectInstances();
        return mapObjects != null && mapObjects.size() == 1 ? mapObjects.singleValue() : null;
    }

    public Modifier getModifier() {
        return getEnv().getModifier();
    }

    private DataObject getPushedAddObject() {
        return pushedAsyncResult instanceof PushAsyncAdd ? ((PushAsyncAdd) pushedAsyncResult).value : null; 
    }

    public boolean isPushedConfirmedClose() {
        return pushedAsyncResult instanceof PushAsyncClose;
    }
    
    public DataObject addObject(ConcreteCustomClass cls) throws SQLException, SQLHandledException {
        return getSession().addObject(cls, getPushedAddObject());
    }

    public DataObject addObject(ConcreteCustomClass cls, boolean autoSet) throws SQLException, SQLHandledException {
        if(autoSet)
            return addObjectAutoSet(cls);
        else
            return addObject(cls);
    }

    public DataObject addObjectAutoSet(ConcreteCustomClass cls) throws SQLException, SQLHandledException {
        return getSession().addObjectAutoSet(cls, getPushedAddObject(), getBL(), getClassListener());
    }

    public <T extends PropertyInterface> SinglePropertyTableUsage<T> addObjects(String debugInfo, ConcreteCustomClass cls, PropertyOrderSet<T> set) throws SQLException, SQLHandledException {
        return getSession().addObjects(debugInfo, cls, set);
    }

    public DataObject formAddObject(ObjectEntity object, ConcreteCustomClass cls) throws SQLException, SQLHandledException {
        FormInstance form = getFormFlowInstance();
        return form.addFormObject((CustomObjectInstance) form.instanceFactory.getInstance(object), cls, getPushedAddObject(), stack);
    }

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject object, ConcreteObjectClass changeClass) throws SQLException, SQLHandledException {
        getEnv().changeClass(objectInstance, object, changeClass);
    }

    public void changeClass(ClassChange change) throws SQLException, SQLHandledException {
        getSession().changeClass(change);
    }

    public boolean checkApply(BusinessLogics BL) throws SQLException, SQLHandledException {
        return getSession().check(BL, getSessionEventFormEnv(), stack, this);
    }

    public boolean checkApply() throws SQLException, SQLHandledException {
        return checkApply(getBL());
    }

    // action calls
    public String applyMessage() throws SQLException, SQLHandledException {
        return getEnv().applyMessage(getBL(), stack, this, getSessionEventFormEnv());
    }

    // action calls
    public boolean apply() throws SQLException, SQLHandledException {
        return apply(SetFact.EMPTYORDER());
    }

    // action calls
    public void applyException() throws SQLException, SQLHandledException {
        getEnv().applyException(getBL(), stack, this, getSessionEventFormEnv());
    }

    // action calls

    // action calls

    // action calls
    public boolean apply(ImOrderSet<ActionValueImplement> applyActions) throws SQLException, SQLHandledException {
        return getEnv().apply(getBL(), stack, this, applyActions, getSessionEventFormEnv());
    }

    // action calls
    public boolean apply(ImOrderSet<ActionValueImplement> applyActions, FunctionSet<SessionDataProperty> keepProperties, Result<String> applyMessage) throws SQLException, SQLHandledException {
        return getEnv().apply(getBL(), stack, this, applyActions, keepProperties, getSessionEventFormEnv(), applyMessage);
    }

    public void cancel(FunctionSet<SessionDataProperty> keep) throws SQLException, SQLHandledException {
        getEnv().cancel(stack, keep);
    }

    public ExecutionContext<P> override(ScheduledExecutorService newExecutorService) {
        return new ExecutionContext<>(keys, pushedAsyncResult, env, newExecutorService, form, stack, hasMoreSessionUsages);
    }

    public ExecutionContext<P> override(ExecutionEnvironment newEnv, ExecutionStack stack) {
        return new ExecutionContext<>(keys, pushedAsyncResult, newEnv, executorService, new FormEnvironment<>(null, null, newEnv.getFormInstance()), stack, hasMoreSessionUsages);
    }

    public ExecutionContext<P> override(ExecutionStack stack) {
        return new ExecutionContext<>(keys, pushedAsyncResult, env, executorService, form, stack, hasMoreSessionUsages);
    }

    public <T extends PropertyInterface> ExecutionContext<T> override(ImMap<T, ? extends ObjectValue> keys, ImMap<T, ? extends PropertyInterfaceImplement<P>> mapInterfaces) {
        return override(keys, form!=null ? form.mapJoin(mapInterfaces) : null);
    }

    public <T extends PropertyInterface> ExecutionContext<T> map(ImRevMap<T, P> mapping) {
        return override(mapping.join(keys), form!=null ? form.map(mapping) : null);
    }

    public ExecutionContext<P> override(ImMap<P, ? extends ObjectValue> keys) {
        return override(keys, form);
    }

    public ExecutionContext<P> override(ImMap<P, ? extends ObjectValue> keys, boolean hasMoreSessionUsages) {
        return new ExecutionContext<>(keys, pushedAsyncResult, env, executorService, form, stack, hasMoreSessionUsages);
    }

    public <T extends PropertyInterface> ExecutionContext<T> override(ImMap<T, ? extends ObjectValue> keys, FormEnvironment<T> form) {
        return new ExecutionContext<>(keys, pushedAsyncResult, env, executorService, form, stack, hasMoreSessionUsages);
    }

    public QueryEnvironment getQueryEnv() {
        return env.getQueryEnv();
    }

    public void executeSessionEvents() throws SQLException, SQLHandledException {
        getSession().executeSessionEvents(getSessionEventFormEnv(), stack);
    }

    public void delayUserInteraction(ClientAction action) {
        ThreadLocalContext.delayUserInteraction(action);
    }

    private void assertNotUserInteractionInTransaction() {
        ServerLoggers.assertLog(!getSession().isInTransaction() || ThreadLocalContext.canBeProcessed(), "USER INTERACTION IN TRANSACTION");
    }
    public Object requestUserInteraction(ClientAction action) {
        assertNotUserInteractionInTransaction();
        return ThreadLocalContext.requestUserInteraction(action);
    }

    public void requestFormUserInteraction(FormInstance remoteForm, ShowFormType showFormType, boolean forbidDuplicate, String formId, ExecutionStack stack) throws SQLException, SQLHandledException {
        assertNotUserInteractionInTransaction();
        ThreadLocalContext.requestFormUserInteraction(remoteForm, showFormType, forbidDuplicate, formId, stack);
    }

    public void writeRequested(ImList<RequestResult> requestResults) throws SQLException, SQLHandledException { // have to be used with getRequestChangeExtProps
        getBL().LM.writeRequested(requestResults, getEnv());
    }

    public void dropRequestCanceled() throws SQLException, SQLHandledException {
        getBL().LM.dropRequestCanceled(getEnv());
    }

    public <R> R pushRequest(SQLCallable<R> callable) throws SQLException, SQLHandledException {
        return getBL().LM.pushRequest(getEnv(), callable);
    }

    public <R> R popRequest(SQLCallable<R> callable) throws SQLException, SQLHandledException {
        return getBL().LM.popRequest(getEnv(), callable);
    }

    // cannot use because of backward compatibility
//    public ObjectValue requestUserData(FileClass dataClass, Object oldValue) {
//        assertNotUserInteractionInTransaction();
//        ObjectValue userInput = pushedUserInput != null ? pushedUserInput : ThreadLocalContext.requestUserData(dataClass, oldValue);
//        setLastUserInput(userInput);
//        return userInput;
//    }


    public boolean isRequestPushed() throws SQLException, SQLHandledException {
        assertNotUserInteractionInTransaction();
        return getBL().LM.isRequestPushed(getEnv());
    }

    public boolean isRequestCanceled() throws SQLException, SQLHandledException {
        assertNotUserInteractionInTransaction();
        return getBL().LM.isRequestCanceled(getEnv());
    }

    @Deprecated
    public ObjectValue requestUser(Type type, SQLCallable<ObjectValue> request) throws SQLException, SQLHandledException {
        assertNotUserInteractionInTransaction();
        return getBL().LM.getRequestedValue(type, getEnv(), request);
    }

    @Deprecated
    public ObjectValue requestUserData(final DataClass dataClass, final Object oldValue) {
        try {
            return requestUser(dataClass, () -> {
                InputResult inputResult = inputUserData(dataClass, oldValue, true, null, null, null);
                return inputResult != null ? inputResult.value : null;
            });
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public <T extends PropertyInterface> InputResult inputUserData(DataClass dataClass, Object oldValue, boolean hasOldValue, InputListEntity<T, P> list, String customChangeFunction, InputList inputList) {
        assertNotUserInteractionInTransaction();
        if(pushedAsyncResult instanceof PushAsyncInput)
            return ((PushAsyncInput) pushedAsyncResult).value;

        InputContext<T> inputContext = null;
        if(list != null)
            inputContext = new InputContext<>(list.map(getKeys()), list.newSession, getSession(), getModifier(), inputList.strict);
        return ThreadLocalContext.inputUserData(dataClass, oldValue, hasOldValue, inputContext, customChangeFunction, inputList);
    }

    @Deprecated
    public ObjectValue requestUserClass(final CustomClass baseClass, final CustomClass defaultValue, final boolean concrete) throws SQLException, SQLHandledException {
        return requestUser(ObjectType.instance, () -> ThreadLocalContext.requestUserClass(baseClass, defaultValue, concrete));
    }

    public FormInstance createFormInstance(FormEntity formEntity, ImSet<ObjectEntity> inputObjects, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, boolean checkOnOk, boolean showDrop, boolean interactive, WindowFormType type, ImSet<ContextFilterInstance> contextFilters, boolean readonly) throws SQLException, SQLHandledException {
        return ThreadLocalContext.createFormInstance(formEntity, inputObjects, mapObjects, stack, session, isModal, noCancel, manageSession, checkOnOk, showDrop, interactive, type, contextFilters, readonly);
    }

    @Deprecated
    public FormInstance createFormInstance(FormEntity formEntity) throws SQLException, SQLHandledException {
        return createFormInstance(formEntity, null, MapFact.<ObjectEntity, DataObject>EMPTY(), getSession(), false, FormEntity.DEFAULT_NOCANCEL, ManageSessionType.AUTO, false, false, false, ModalityWindowFormType.FLOAT, null, false);
    }

    public SQLSyntax getDbSyntax() {
        return getDbManager().getSyntax();
    }
}
