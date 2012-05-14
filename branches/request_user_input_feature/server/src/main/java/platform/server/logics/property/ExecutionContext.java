package platform.server.logics.property;

import jasperapi.ReportGenerator;
import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.interop.form.RemoteFormInterface;
import platform.server.Context;
import platform.server.auth.SecurityPolicy;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.DataClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.*;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.session.DataSession;
import platform.server.session.ExecutionEnvironment;
import platform.server.session.Modifier;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.join;

public class ExecutionContext {
    private final Map<ClassPropertyInterface, DataObject> keys;
    private final ObjectValue value;
    private final List<ClientAction> actions;
    private final boolean groupLast; // обозначает, что изменение последнее, чтобы форма начинала определять, что изменилось

    private final ExecutionEnvironment env;
    private final FormEnvironment form;

    public ExecutionContext(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, ExecutionEnvironment env, List<ClientAction> actions, FormEnvironment form, boolean groupLast) {
        this.keys = keys;
        this.value = value;
        this.env = env;
        this.actions = actions;
        this.form = form;
        this.groupLast = groupLast;
    }

    public ExecutionEnvironment getEnv() {
        return env;
    }

    public Map<ClassPropertyInterface, DataObject> getKeys() {
        return keys;
    }

    public DataObject getKeyValue(ClassPropertyInterface key) {
        return keys.get(key);
    }

    public Object getKeyObject(ClassPropertyInterface key) {
        return keys.get(key).object;
    }

    public DataObject getSingleKeyValue() {
        return BaseUtils.singleValue(keys);
    }

    public Object getSingleKeyObject() {
        return getSingleKeyValue().object;
    }

    public int getKeyCount() {
        return keys.size();
    }

    public ObjectValue getValue() {
        return value;
    }

    public Object getValueObject() {
        return value.getValue();
    }

    public DataSession getSession() {
        return env.getSession();
    }

    public List<ClientAction> getActions() {
        return actions;
    }

    public void addAction(ClientAction action) {
        actions.add(action);
    }

    public void addActions(List<ClientAction> actions) {
        this.actions.addAll(actions);
    }

    public FormInstance<?> getFormInstance() {
        return env.getFormInstance();
    }

    public GroupObjectInstance getGroupObjectInstance() {
        PropertyDrawInstance drawInstance = form.getDrawInstance();
        if(drawInstance==null)
            return null;
        return drawInstance.toDraw;
    }
    public Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> getObjectInstances() {
        return form!=null ? form.getMapObjects() : null;
    }

    public ObjectInstance getObjectInstance(ObjectEntity object) {
        return getFormInstance().instanceFactory.getInstance(object);
    }

    public PropertyObjectInterfaceInstance getObjectInstance(ClassPropertyInterface cls) {
        return getObjectInstances().get(cls);
    }

    public PropertyObjectInterfaceInstance getSingleObjectInstance() {
        Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects = getObjectInstances();
        return mapObjects != null ? BaseUtils.singleValue(mapObjects) : null;
    }

    public boolean isGroupLast() {
        return groupLast;
    }

    public Modifier getModifier() {
        return getEnv().getModifier();
    }

    public DataObject addObject(ConcreteCustomClass cls) throws SQLException {
        return getEnv().addObject(cls);
    }

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject object, int clsID) throws SQLException {
        getEnv().changeClass(objectInstance, object, getSession().baseClass.findConcreteClassID(clsID < 0 ? null : clsID), isGroupLast());
    }

    public void apply(BusinessLogics BL) throws SQLException {
        getEnv().apply(BL, actions);
    }

    public void cancel() throws SQLException {
        getEnv().cancel();
    }

    public void emitExceptionIfNotInFormSession() {
        if (getFormInstance()==null) {
            throw new IllegalStateException("Property should only be used in form's session!");
        }
    }

    public ExecutionContext override(ExecutionEnvironment newEnv) {
        return new ExecutionContext(keys, value, newEnv, new ArrayList<ClientAction>(), form, groupLast);
    }

    public ExecutionContext override(Map<ClassPropertyInterface, DataObject> keys, Map<ClassPropertyInterface, ? extends PropertyInterfaceImplement<ClassPropertyInterface>> mapInterfaces) {
        return override(keys, form!=null ? form.map(mapInterfaces) : null, value);
    }

    public ExecutionContext map(Map<ClassPropertyInterface, ClassPropertyInterface> mapping, ObjectValue value) {
        return override(join(mapping, keys), form!=null ? form.map(mapping) : null, value);
    }

    public ExecutionContext override(Map<ClassPropertyInterface, DataObject> keys, FormEnvironment form, ObjectValue value) {
        return new ExecutionContext(keys, value, env, actions, form, groupLast);
    }

    // зеркалирование Context, чтобы если что можно было бы не юзать ThreadLocal
    public FormInstance createFormInstance(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects, DataSession session, boolean newSession, boolean interactive)  throws SQLException {
        return Context.context.get().createFormInstance(formEntity, mapObjects, session, newSession, interactive);
    }
    public RemoteForm createRemoteForm(FormInstance formInstance, boolean checkOnOk) {
        return Context.context.get().createRemoteForm(formInstance, checkOnOk);
    }
    public RemoteForm createReportForm(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects) throws SQLException { //
        return createRemoteForm(createFormInstance(formEntity, mapObjects, getSession(), false, false), false);
    }

    public interface RequestDialog {
        DialogInstance createDialog() throws SQLException;
    }

    public Object requestUserInteraction(ClientAction action) {
        return Context.context.get().requestUserInteraction(action);
    }

    // для подмены ввода и обеспечания WYSIWYG механизмов
    public ObjectValue getLastUserInput() {
        return null;
//        throw new RuntimeException("not implemented yet");
    }
    public void pushUserInput(ObjectValue value) {
        // todo;
    }
    public void popUserInput(ObjectValue value) {
        // todo:
    }

    // чтение пользователя
    public ObjectValue requestUserObject(RequestDialog dialog) throws SQLException { // null если canceled
        return Context.context.get().requestUserObject(dialog);
    }

    public ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        return Context.context.get().requestUserData(dataClass, oldValue);
    }

    public File generateFileFromForm(BusinessLogics BL, FormEntity formEntity, ObjectEntity objectEntity, DataObject dataObject) throws SQLException {

        RemoteFormInterface remoteForm = createReportForm(formEntity, Collections.singletonMap(objectEntity, dataObject));
        try {
            ReportGenerator report = new ReportGenerator(remoteForm, BL.getTimeZone());
            JasperPrint print = report.createReport(false, false, new HashMap(), null);
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

    public FormEnvironment getForm() {
        return form;
    }

    public SecurityPolicy getSecurityPolicy() {
        return getFormInstance().securityPolicy;
    }
}
