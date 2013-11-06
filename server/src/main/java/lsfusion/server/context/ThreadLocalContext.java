package lsfusion.server.context;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.server.Settings;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.logics.*;
import lsfusion.server.logics.SecurityManager;
import lsfusion.server.logics.property.DialogRequest;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class ThreadLocalContext {
    private static final ThreadLocal<Context> context = new ThreadLocal<Context>();

    public static Context get() {
        return context.get();
    }

    public static void set(Context c) {
        context.set(c);
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

    public static FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters, PropertyDrawEntity initFilterProperty, ImSet<PullChangeProperty> pullProps) throws SQLException {
        return get().createFormInstance(formEntity, mapObjects, session, isModal, sessionScope, checkOnOk, showDrop, interactive, contextFilters, initFilterProperty, pullProps);
    }

    public static RemoteForm createRemoteForm(FormInstance formInstance) {
        return get().createRemoteForm(formInstance);
    }

    public static ObjectValue requestUserObject(DialogRequest dialogRequest) throws SQLException {
        return get().requestUserObject(dialogRequest);
    }

    public static ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        return get().requestUserData(dataClass, oldValue);
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

    public static Object[] requestUserInteraction(ClientAction... actions) {
        return get().requestUserInteraction(actions);
    }

    public static void setActionMessage(String message) {
        if (get() != null) {
            get().setActionMessage(message);
        }
    }

    public static String getActionMessage() {
        return get() != null ? get().getActionMessage() : "";
    }

    public static void pushActionMessage(String segment) {
        if (get() != null) {
            get().pushActionMessage(segment);
        }
    }

    public static String popActionMessage() {
        return get() != null ? get().popActionMessage() : "";
    }
}
