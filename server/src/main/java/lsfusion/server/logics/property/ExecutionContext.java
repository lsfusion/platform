package lsfusion.server.logics.property;

import jasperapi.ReportGenerator;
import lsfusion.base.FunctionSet;
import lsfusion.base.Pair;
import lsfusion.base.Processor;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.ServerLoggers;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.ConcreteObjectClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.*;
import lsfusion.server.logics.*;
import lsfusion.server.logics.SecurityManager;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.actions.FormEnvironment;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.session.*;
import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPdfExporter;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ScheduledExecutorService;

public class ExecutionContext<P extends PropertyInterface> implements UpdateCurrentClasses, UserInteraction, SessionCreator {
    private ImMap<P, ? extends ObjectValue> keys;
    private Stack<UpdateCurrentClasses> updateClasses;
    public void pushUpdate(UpdateCurrentClasses push) {
        if(updateClasses == null)
            updateClasses = new Stack<UpdateCurrentClasses>();
        updateClasses.push(push);
    }
    public void popUpdate() {
        updateClasses.pop();
    }

    private Stack<UpdateInputListener> updateInputListeners;
    public void pushUpdateInput(UpdateInputListener push) {
        if (updateInputListeners == null) {
            updateInputListeners = new Stack<UpdateInputListener>();
        }
        updateInputListeners.push(push);
    }
    public void popUpdateInput() {
        updateInputListeners.pop();
    }

    private final ObjectValue pushedUserInput;
    private final DataObject pushedAddObject;

    private final ExecutionEnvironment env;
        
    private final ExecutionContext stack;

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
    public ExecutionContext(ImMap<P, ? extends ObjectValue> keys, ExecutionEnvironment env) {
        this(keys, null, null, env, null, null);
    }
    
    public ExecutionContext(ImMap<P, ? extends ObjectValue> keys, ObjectValue pushedUserInput, DataObject pushedAddObject, ExecutionEnvironment env, FormEnvironment<P> form, ExecutionContext stack) {
        this.keys = keys;
        this.pushedUserInput = pushedUserInput;
        this.pushedAddObject = pushedAddObject;
        this.env = env;
        this.form = form;
        this.stack = stack;
    }
    
    public ExecutionContext<P> override() { // для дебаггера
        return new ExecutionContext<P>(keys, pushedUserInput, pushedAddObject, env, form, this);
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

    public void setWatcher(Processor<ImMap<String, ObjectValue>> watcher) {
        this.watcher = watcher;
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
            result = paramsToInterfaces.mapValues(new GetValue<ObjectValue, P>() {
                public ObjectValue getMapValue(P value) {
                    return getKeyValue(value);
                }
            }).addExcl(result);
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

    public ScheduledExecutorService getExecutorService() {
        return ThreadLocalContext.getExecutorService();
    }

    public FormInstance<?> getFormInstance() {
        return env.getFormInstance();
    }

    public FormEnvironment<P> getForm() {
        return form;
    }

    public SecurityPolicy getSecurityPolicy() {
        return getFormInstance().securityPolicy;
    }

    //todo: закэшировать, если скорость доступа к ThreadLocal станет критичной
    //todo: сейчас по идее не актуально, т.к. большая часть времени в ActionProperty уходит на основную работу
    public LogicsInstance getLogicsInstance() {
        return ThreadLocalContext.getLogicsInstance();
    }

    public BusinessLogics<?> getBL() {
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

    public RMIManager getRmiManager() {
        return getLogicsInstance().getRmiManager();
    }

    public DataSession createSession() throws SQLException {
        return getSession().createSession();
//        return getDbManager().createSession();
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

    public ObjectInstance getObjectInstance(ObjectEntity object) {
        return getFormInstance().instanceFactory.getInstance(object);
    }

    public PropertyObjectInterfaceInstance getObjectInstance(P cls) {
        ImMap<P, PropertyObjectInterfaceInstance> objectInstances = getObjectInstances();
        return objectInstances != null ? objectInstances.get(cls) : null;
    }

    public PropertyObjectInterfaceInstance getSingleObjectInstance() {
        ImMap<P, PropertyObjectInterfaceInstance> mapObjects = getObjectInstances();
        return mapObjects != null && mapObjects.size() == 1 ? mapObjects.singleValue() : null;
    }

    public Modifier getModifier() {
        return getEnv().getModifier();
    }

    public DataObject addObject(ConcreteCustomClass cls) throws SQLException, SQLHandledException {
        return getSession().addObject(cls, pushedAddObject);
    }

    public <T extends PropertyInterface> SinglePropertyTableUsage<T> addObjects(ConcreteCustomClass cls, PropertySet<T> set) throws SQLException, SQLHandledException {
        return getSession().addObjects(cls, set);
    }

    public DataObject addFormObject(ObjectEntity object, ConcreteCustomClass cls) throws SQLException, SQLHandledException {
        FormInstance<?> form = getFormInstance();
        return form.addFormObject((CustomObjectInstance) form.instanceFactory.getInstance(object), cls, pushedAddObject);
    }

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject object, ConcreteObjectClass changeClass) throws SQLException, SQLHandledException {
        getEnv().changeClass(objectInstance, object, changeClass);
    }

    public void changeClass(ClassChange change) throws SQLException, SQLHandledException {
        getSession().changeClass(change);
    }

    public boolean checkApply(BusinessLogics BL) throws SQLException, SQLHandledException {
        return getSession().check(BL, getFormInstance(), this);
    }

    public boolean checkApply() throws SQLException, SQLHandledException {
        return checkApply(getBL());
    }

    public boolean apply() throws SQLException, SQLHandledException {
        return apply(null);
    }

    public boolean apply(ActionPropertyValueImplement applyAction) throws SQLException, SQLHandledException {
        return apply(applyAction, SetFact.<SessionDataProperty>EMPTY());
    }
    
    public boolean apply(ActionPropertyValueImplement applyAction, FunctionSet<SessionDataProperty> keepProperties) throws SQLException, SQLHandledException {
        return getEnv().apply(getBL(), this, this, applyAction, keepProperties, getFormInstance());
    }

    public void cancel() throws SQLException, SQLHandledException {
        getEnv().cancel();
    }

    public void emitExceptionIfNotInFormSession() {
        if (getFormInstance()==null) {
            throw new IllegalStateException("Property should only be used in form's session!");
        }
    }

    public ExecutionContext<P> override(ExecutionEnvironment newEnv) {
        return new ExecutionContext<P>(keys, pushedUserInput, pushedAddObject, newEnv, form, this);
    }

    public <T extends PropertyInterface> ExecutionContext<T> override(ImMap<T, ? extends ObjectValue> keys, ImMap<T, ? extends CalcPropertyInterfaceImplement<P>> mapInterfaces) {
        return override(keys, form!=null ? form.mapJoin(mapInterfaces) : null, pushedUserInput);
    }

    public <T extends PropertyInterface> ExecutionContext<T> map(ImRevMap<T, P> mapping) {
        return override(mapping.join(keys), form!=null ? form.map(mapping) : null, pushedUserInput);
    }

    public ExecutionContext<P> override(ImMap<P, ? extends ObjectValue> keys) {
        return override(keys, form, pushedUserInput);
    }

    public <T extends PropertyInterface> ExecutionContext<T> override(ImMap<T, ? extends ObjectValue> keys, FormEnvironment<T> form) {
        return override(keys, form, pushedUserInput);
    }

    @Override
    public void update(DataSession session) throws SQLException, SQLHandledException {
        keys = session.updateCurrentClasses(keys);

        final ImMap<P, ? extends ObjectValue> prevKeys = keys;
        session.addRollbackInfo(new Runnable() {
            public void run() {
                keys = prevKeys;
            }});
        
        if(updateClasses!=null) {
            for(UpdateCurrentClasses update : updateClasses)
                update.update(session);
        }
        if(stack != null && env == stack.env)
            stack.update(session);
    }
    
    

    public <T extends PropertyInterface> ExecutionContext<T> override(ImMap<T, ? extends ObjectValue> keys, FormEnvironment<T> form, ObjectValue pushedUserInput) {
        return new ExecutionContext<T>(keys, pushedUserInput, pushedAddObject, env, form, this);
    }

    public QueryEnvironment getQueryEnv() {
        return env.getQueryEnv();
    }

    public void delayUserInteraction(ClientAction action) {
        ThreadLocalContext.delayUserInteraction(action);
    }

    private void assertNotUserInteractionInTransaction() {
        ServerLoggers.assertLog(!getSession().isInTransaction(), "USER INTERACTION IN TRANSACTION");
    }
    public Object requestUserInteraction(ClientAction action) {
        assertNotUserInteractionInTransaction();
        return ThreadLocalContext.requestUserInteraction(action);
    }

    public ExecutionContext<P> pushUserInput(ObjectValue overridenUserInput) {
        return override(keys, form, overridenUserInput);
    }

    public ObjectValue getPushedUserInput() {
        return pushedUserInput;
    }

    // чтение пользователя
    public ObjectValue requestUserObject(DialogRequest dialog) throws SQLException, SQLHandledException { // null если canceled
        assertNotUserInteractionInTransaction();
        ObjectValue userInput = pushedUserInput != null ? pushedUserInput : ThreadLocalContext.requestUserObject(dialog);
        setLastUserInput(userInput);
        return userInput;
    }

    public ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        assertNotUserInteractionInTransaction();
        ObjectValue userInput = pushedUserInput != null ? pushedUserInput : ThreadLocalContext.requestUserData(dataClass, oldValue);
        setLastUserInput(userInput);
        return userInput;
    }

    public ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete) {
        assertNotUserInteractionInTransaction();
        ObjectValue userInput = pushedUserInput != null ? pushedUserInput : ThreadLocalContext.requestUserClass(baseClass, defaultValue, concrete);
        setLastUserInput(userInput);
        return userInput;
    }

    public void setLastUserInput(ObjectValue userInput) {
        updateLastUserInput(userInput);
    }

    public void updateLastUserInput(final ObjectValue userInput) {
        if (updateInputListeners != null) {
            for (UpdateInputListener inputListener : updateInputListeners) {
                inputListener.userInputUpdated(userInput);
            }
        }

        if(stack != null && env == stack.env)
            stack.updateLastUserInput(userInput);
    }

    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters) throws SQLException, SQLHandledException {
        return createFormInstance(formEntity, mapObjects, session, isModal, sessionScope, checkOnOk, showDrop, interactive, contextFilters, null, null);
    }

    // зеркалирование Context, чтобы если что можно было бы не юзать ThreadLocal
    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters, PropertyDrawEntity initFilterProperty, ImSet<PullChangeProperty> pullProps) throws SQLException, SQLHandledException {
        return ThreadLocalContext.createFormInstance(formEntity, mapObjects, this, session, isModal, sessionScope, checkOnOk, showDrop, interactive, contextFilters, initFilterProperty, pullProps);
    }

    public RemoteForm createRemoteForm(FormInstance formInstance) {
        return ThreadLocalContext.createRemoteForm(formInstance);
    }

    public RemoteForm createReportForm(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects) throws SQLException, SQLHandledException {
        return createRemoteForm(createFormInstance(formEntity, mapObjects, getSession(), false, FormSessionScope.OLDSESSION, false, false, false, null));
    }

    public File generateFileFromForm(BusinessLogics BL, FormEntity formEntity, ObjectEntity objectEntity, DataObject dataObject) throws SQLException, SQLHandledException {

        RemoteForm remoteForm = createReportForm(formEntity, MapFact.singleton(objectEntity, dataObject));
        try {
            ReportGenerationData generationData = remoteForm.reportManager.getReportData();
            ReportGenerator report = new ReportGenerator(generationData);
            JasperPrint print = report.createReport(false, new HashMap());
            File tempFile = File.createTempFile("lsfReport", ".pdf");

            JRAbstractExporter exporter = new JRPdfExporter();
            exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, tempFile.getAbsolutePath());
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
            exporter.exportReport();

            return tempFile;

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (JRException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
