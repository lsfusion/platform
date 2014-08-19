package lsfusion.server.logics.property;

import jasperapi.ReportGenerator;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.ConcreteObjectClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
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
import java.util.Map;
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

    private final ObjectValue pushedUserInput;
    private final DataObject pushedAddObject;

    private final ExecutionEnvironment env;
    private final ExecutionContext stack;
    private Map<String, P> paramsToInterfaces;
    private Map<String, String> paramsToFQN;
    private final FormEnvironment<P> form;

    public ExecutionContext(ImMap<P, ? extends ObjectValue> keys, ObjectValue pushedUserInput, DataObject pushedAddObject, ExecutionEnvironment env, FormEnvironment<P> form, ExecutionContext stack) {
        this.keys = keys;
        this.pushedUserInput = pushedUserInput;
        this.pushedAddObject = pushedAddObject;
        this.env = env;
        this.form = form;
        this.stack = stack;
    }

    public void setParamsToInterfaces(Map<String, P> paramsToInterfaces) {
        this.paramsToInterfaces = paramsToInterfaces;
    }

    public ObjectValue getParamValue(String param) {
        P val = paramsToInterfaces.get(param);
        if (val != null) {
            return getKeyValue(val);
        }
        
        if (stack != null) {
            return stack.getParamValue(param);
        }
        
        return null;
    }
    
    public String[][] getAllParamsWithClassesInStack() {
        Map<String, String> paramsToClass = new HashMap<String, String>();
        
        ExecutionContext<?> current = this;
        while (current != null) {
            for (Map.Entry<String, String> e : current.paramsToFQN.entrySet()) {
                if (!paramsToClass.containsKey(e.getKey())) {
                    paramsToClass.put(e.getKey(), e.getValue());
                }
            }
            current = current.stack;
        }
        
        String[][] res = new String[2][paramsToClass.size()];
        int i = 0;
        for (Map.Entry<String, String> e : paramsToClass.entrySet()) {
            res[0][i] = e.getKey();
            res[1][i++] = e.getValue();
        }
        
        return res;
    }

    public void setParamsToFQN(Map<String, String> paramsToFQN) {
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
    
    public boolean apply(ActionPropertyValueImplement applyAction, ImSet<SessionDataProperty> keepProperties) throws SQLException, SQLHandledException {
        return getEnv().apply(getBL(), this, this, applyAction, keepProperties);
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
        return new ExecutionContext<P>(keys, pushedUserInput, pushedAddObject, newEnv, form, null);
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
        if(stack != null)
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

    public Object requestUserInteraction(ClientAction action) {
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
        ObjectValue userInput = pushedUserInput != null ? pushedUserInput : ThreadLocalContext.requestUserObject(dialog);
        env.setLastUserInput(userInput);
        return userInput;
    }

    public ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        ObjectValue userInput = pushedUserInput != null ? pushedUserInput : ThreadLocalContext.requestUserData(dataClass, oldValue);
        env.setLastUserInput(userInput);
        return userInput;
    }

    public ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete) {
        ObjectValue userInput = pushedUserInput != null ? pushedUserInput : ThreadLocalContext.requestUserClass(baseClass, defaultValue, concrete);
        env.setLastUserInput(userInput);
        return userInput;
    }

    public void setLastUserInput(ObjectValue userInput) {
        env.setLastUserInput(userInput);
    }

    // для подмены ввода и обеспечания WYSIWYG механизмов
    public ObjectValue getLastUserInput() {
        return env.getLastUserInput();
    }
    public boolean getWasUserInput() {
        return env.getWasUserInput();
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
