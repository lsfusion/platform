package platform.server.context;

import platform.base.col.interfaces.immutable.ImMap;
import platform.interop.action.ClientAction;
import platform.server.Settings;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.DialogInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.logics.*;
import platform.server.logics.SecurityManager;
import platform.server.logics.property.ExecutionContext;
import platform.server.remote.RemoteDialog;
import platform.server.remote.RemoteForm;
import platform.server.session.DataSession;

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

    public static FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive) throws SQLException {
        return get().createFormInstance(formEntity, mapObjects, session, isModal, sessionScope, checkOnOk, showDrop, interactive);
    }

    public static RemoteForm createRemoteForm(FormInstance formInstance) {
        return get().createRemoteForm(formInstance);
    }

    public static RemoteDialog createRemoteDialog(DialogInstance dialogInstance) {
        return get().createRemoteDialog(dialogInstance);
    }

    public static ObjectValue requestUserObject(ExecutionContext.RequestDialog requestDialog) throws SQLException {
        return get().requestUserObject(requestDialog);
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
