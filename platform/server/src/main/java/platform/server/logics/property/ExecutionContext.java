package platform.server.logics.property;

import jasperapi.ReportGenerator;
import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.action.ClientAction;
import platform.interop.form.ReportGenerationData;
import platform.server.Context;
import platform.server.auth.SecurityPolicy;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.data.QueryEnvironment;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.*;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.session.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

public class ExecutionContext<P extends PropertyInterface> {
    private final ImMap<P, DataObject> keys;

    private final ObjectValue pushedUserInput;
    private final DataObject pushedAddObject;

    private final ExecutionEnvironment env;
    private final FormEnvironment<P> form;

    public ExecutionContext(ImMap<P, DataObject> keys, ObjectValue pushedUserInput, DataObject pushedAddObject, ExecutionEnvironment env, FormEnvironment<P> form) {
        this.keys = keys;
        this.pushedUserInput = pushedUserInput;
        this.pushedAddObject = pushedAddObject;
        this.env = env;
        this.form = form;
    }

    public ExecutionEnvironment getEnv() {
        return env;
    }

    public ImMap<P, DataObject> getKeys() {
        return keys;
    }

    public DataObject getKeyValue(P key) {
        return keys.get(key);
    }

    public Object getKeyObject(P key) {
        return keys.get(key).object;
    }

    public DataObject getSingleKeyValue() {
        return keys.singleValue();
    }

    public Object getSingleKeyObject() {
        return getSingleKeyValue().object;
    }

    public int getKeyCount() {
        return keys.size();
    }

    public DataSession getSession() {
        return env.getSession();
    }

    public void delayUserInterfaction(ClientAction action) {
        Context.context.get().delayUserInteraction(action);
    }

    public FormInstance<?> getFormInstance() {
        return env.getFormInstance();
    }

    BusinessLogics<?> BL;
    public BusinessLogics<?> getBL() {
        if(BL==null) {
            BL = Context.context.get().getBL();
        }
        return BL;
    }

    public GroupObjectInstance getGroupObjectInstance() {
        PropertyDrawInstance drawInstance = form.getDrawInstance();
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

    public DataObject addObject(ConcreteCustomClass cls) throws SQLException {
        return getSession().addObject(cls, pushedAddObject);
    }

    public <T extends PropertyInterface> SinglePropertyTableUsage<T> addObjects(ConcreteCustomClass cls, PropertySet<T> set) throws SQLException {
        return getSession().addObjects(cls, set);
    }

    public DataObject addFormObject(ObjectEntity object, ConcreteCustomClass cls) throws SQLException {
        FormInstance<?> form = getFormInstance();
        return form.addFormObject((CustomObjectInstance) form.instanceFactory.getInstance(object), cls, pushedAddObject);
    }

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject object, ConcreteObjectClass changeClass) throws SQLException {
        getEnv().changeClass(objectInstance, object, changeClass);
    }

    public void changeClass(ClassChange change) throws SQLException {
        getSession().changeClass(change);
    }

    public boolean checkApply(BusinessLogics BL) throws SQLException {
        return getSession().check(BL, getFormInstance());
    }

    public boolean apply(BusinessLogics BL) throws SQLException {
        return getEnv().apply(BL);
    }

    public void cancel() throws SQLException {
        getEnv().cancel();
    }

    public void emitExceptionIfNotInFormSession() {
        if (getFormInstance()==null) {
            throw new IllegalStateException("Property should only be used in form's session!");
        }
    }

    public ExecutionContext<P> override(ExecutionEnvironment newEnv) {
        return new ExecutionContext<P>(keys, pushedUserInput, pushedAddObject, newEnv, form);
    }

    public <T extends PropertyInterface> ExecutionContext<T> override(ImMap<T, DataObject> keys, ImMap<T, ? extends CalcPropertyInterfaceImplement<P>> mapInterfaces) {
        return override(keys, form!=null ? form.mapJoin(mapInterfaces) : null, pushedUserInput);
    }

    public <T extends PropertyInterface> ExecutionContext<T> map(ImRevMap<T, P> mapping) {
        return override(mapping.join(keys), form!=null ? form.map(mapping) : null, pushedUserInput);
    }

    public ExecutionContext<P> override(ImMap<P, DataObject> keys) {
        return override(keys, form, pushedUserInput);
    }

    public <T extends PropertyInterface> ExecutionContext<T> override(ImMap<T, DataObject> keys, FormEnvironment<T> form) {
        return override(keys, form, pushedUserInput);
    }

    public <T extends PropertyInterface> ExecutionContext<T> override(ImMap<T, DataObject> keys, FormEnvironment<T> form, ObjectValue pushedUserInput) {
        return new ExecutionContext<T>(keys, pushedUserInput, pushedAddObject, env, form);
    }

    // зеркалирование Context, чтобы если что можно было бы не юзать ThreadLocal
    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, DataObject> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive)  throws SQLException {
        return Context.context.get().createFormInstance(formEntity, mapObjects, session, isModal, sessionScope, checkOnOk, showDrop, interactive);
    }
    public RemoteForm createRemoteForm(FormInstance formInstance) {
        return Context.context.get().createRemoteForm(formInstance);
    }
    public RemoteForm createReportForm(FormEntity formEntity, ImMap<ObjectEntity, DataObject> mapObjects) throws SQLException {
        return createRemoteForm(createFormInstance(formEntity, mapObjects, getSession(), false, FormSessionScope.OLDSESSION, false, false, false));
    }

    public QueryEnvironment getQueryEnv() {
        return env.getQueryEnv();
    }

    public interface RequestDialog {
        DialogInstance createDialog() throws SQLException;
    }

    // предполагается, что например при помощи delayUserInteraction пользователь получит обновление клиента, и можно не делать remoteChanges
    public void delayRemoteChanges() {
        Context.context.get().delayRemoteChanges();
    }

    public void delayUserInteraction(ClientAction action) {
        Context.context.get().delayUserInteraction(action);
    }

    public Object requestUserInteraction(ClientAction action) {
        return Context.context.get().requestUserInteraction(action);
    }

    public ExecutionContext<P> pushUserInput(ObjectValue overridenUserInput) {
        return override(keys, form, overridenUserInput);
    }

    public ObjectValue getPushedUserInput() {
        return pushedUserInput;
    }

    // чтение пользователя
    public ObjectValue requestUserObject(RequestDialog dialog) throws SQLException { // null если canceled
        ObjectValue userInput = pushedUserInput != null ? pushedUserInput : Context.context.get().requestUserObject(dialog);
        env.setLastUserInput(userInput);
        return userInput;
    }

    public ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        ObjectValue userInput = pushedUserInput != null ? pushedUserInput : Context.context.get().requestUserData(dataClass, oldValue);
        env.setLastUserInput(userInput);
        return userInput;
    }

    public ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete) {
        ObjectValue userInput = pushedUserInput != null ? pushedUserInput : Context.context.get().requestUserClass(baseClass, defaultValue, concrete);
        env.setLastUserInput(userInput);
        return userInput;
    }

    // для подмены ввода и обеспечания WYSIWYG механизмов
    public ObjectValue getLastUserInput() {
        return env.getLastUserInput();
    }
    public boolean getWasUserInput() {
        return env.getWasUserInput();
    }

    public File generateFileFromForm(BusinessLogics BL, FormEntity formEntity, ObjectEntity objectEntity, DataObject dataObject) throws SQLException {

        RemoteForm remoteForm = createReportForm(formEntity, MapFact.singleton(objectEntity, dataObject));
        try {
            ReportGenerationData generationData = remoteForm.reportManager.getReportData();
            ReportGenerator report = new ReportGenerator(generationData, BL.getTimeZone());
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

    public FormEnvironment<P> getForm() {
        return form;
    }

    public SecurityPolicy getSecurityPolicy() {
        return getFormInstance().securityPolicy;
    }
}
