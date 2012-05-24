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
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.CustomClass;
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

public class ExecutionContext<P extends PropertyInterface> {
    private final Map<P, DataObject> keys;
    private final ObjectValue pushedUserInput;
    private final List<ClientAction> actions;
    private final boolean groupLast; // обозначает, что изменение последнее, чтобы форма начинала определять, что изменилось

    private final ExecutionEnvironment env;
    private final FormEnvironment<P> form;

    public ExecutionContext(Map<P, DataObject> keys, ObjectValue pushedUserInput, ExecutionEnvironment env, List<ClientAction> actions, FormEnvironment<P> form, boolean groupLast) {
        this.keys = keys;
        this.pushedUserInput = pushedUserInput;
        this.env = env;
        this.actions = actions;
        this.form = form;
        this.groupLast = groupLast;
    }

    public ExecutionEnvironment getEnv() {
        return env;
    }

    public Map<P, DataObject> getKeys() {
        return keys;
    }

    public DataObject getKeyValue(P key) {
        return keys.get(key);
    }

    public Object getKeyObject(P key) {
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
    public Map<P, PropertyObjectInterfaceInstance> getObjectInstances() {
        return form!=null ? form.getMapObjects() : null;
    }

    public ObjectInstance getObjectInstance(ObjectEntity object) {
        return getFormInstance().instanceFactory.getInstance(object);
    }

    public PropertyObjectInterfaceInstance getObjectInstance(P cls) {
        return getObjectInstances().get(cls);
    }

    public PropertyObjectInterfaceInstance getSingleObjectInstance() {
        Map<P, PropertyObjectInterfaceInstance> mapObjects = getObjectInstances();
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

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject object, ConcreteObjectClass changeClass) throws SQLException {
        getEnv().changeClass(objectInstance, object, changeClass, isGroupLast());
    }

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject object, int clsID) throws SQLException {
        changeClass(objectInstance, object, getSession().baseClass.findConcreteClassID(clsID < 0 ? null : clsID));
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

    public ExecutionContext<P> override(ExecutionEnvironment newEnv) {
        return new ExecutionContext<P>(keys, pushedUserInput, newEnv, new ArrayList<ClientAction>(), form, groupLast);
    }

    public <T extends PropertyInterface> ExecutionContext<T> override(Map<T, DataObject> keys, Map<T, ? extends CalcPropertyInterfaceImplement<P>> mapInterfaces) {
        return override(keys, form!=null ? form.mapJoin(mapInterfaces) : null, pushedUserInput);
    }

    public <T extends PropertyInterface> ExecutionContext<T> map(Map<T, P> mapping) {
        return override(join(mapping, keys), form!=null ? form.map(mapping) : null, pushedUserInput);
    }

    public ExecutionContext<P> override(Map<P, DataObject> keys) {
        return override(keys, form, pushedUserInput);
    }

    public <T extends PropertyInterface> ExecutionContext<T> override(Map<T, DataObject> keys, FormEnvironment<T> form, ObjectValue pushedUserInput) {
        return new ExecutionContext<T>(keys, pushedUserInput, env, actions, form, groupLast);
    }

    // зеркалирование Context, чтобы если что можно было бы не юзать ThreadLocal
    public FormInstance createFormInstance(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects, DataSession session, boolean isModal, boolean newSession, boolean checkOnOk, boolean interactive)  throws SQLException {
        return Context.context.get().createFormInstance(formEntity, mapObjects, session, isModal, newSession, checkOnOk, interactive);
    }
    public RemoteForm createRemoteForm(FormInstance formInstance) {
        return Context.context.get().createRemoteForm(formInstance);
    }
    public RemoteForm createReportForm(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects) throws SQLException {
        return createRemoteForm(createFormInstance(formEntity, mapObjects, getSession(), false, false, false, false));
    }

    public interface RequestDialog {
        DialogInstance createDialog() throws SQLException;
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

    public FormEnvironment<P> getForm() {
        return form;
    }

    public SecurityPolicy getSecurityPolicy() {
        return getFormInstance().securityPolicy;
    }
}
