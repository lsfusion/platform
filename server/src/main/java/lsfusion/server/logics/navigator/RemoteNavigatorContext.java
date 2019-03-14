package lsfusion.server.logics.navigator;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.ModalityType;
import lsfusion.interop.action.ClientAction;
import lsfusion.server.physics.admin.authentication.policy.SecurityPolicy;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.classes.DataClass;
import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilter;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.interactive.listener.RemoteFormListener;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.logics.form.interactive.dialogedit.DialogRequest;
import lsfusion.server.logics.property.derived.PullChangeProperty;
import lsfusion.server.remote.RemoteConnection;
import lsfusion.server.remote.RemoteConnectionContext;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.remote.RemoteUIContext;
import lsfusion.server.logics.action.session.DataSession;

import java.sql.SQLException;
import java.util.Locale;

// multiple inheritance - RemoteConnection + RemoteUI
public class RemoteNavigatorContext extends RemoteConnectionContext {
    private final RemoteNavigator navigator;
    
    private final RemoteUIContext uiContext; // multiple inheritance
    
    public RemoteNavigatorContext(RemoteNavigator remoteNavigator) {
        navigator = remoteNavigator;
        
        uiContext = new RemoteUIContext() {
            @Override
            protected SecurityPolicy getSecurityPolicy() {
                return navigator.securityPolicy;
            }

            @Override
            protected int getExportPort() {
                return navigator.getExportPort();
            }

            @Override
            protected RemoteFormListener getFormListener() {
                return navigator;
            }

            @Override
            public LogicsInstance getLogicsInstance() {
                return RemoteNavigatorContext.this.getLogicsInstance();
            }

            @Override
            protected void aspectDelayUserInteraction(ClientAction action, String message) {
                RemoteNavigatorContext.this.aspectDelayUserInteraction(action, message);
            }

            @Override
            protected Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
                return RemoteNavigatorContext.this.aspectRequestUserInteraction(actions, messages);
            }

            @Override
            public FocusListener getFocusListener() {
                return RemoteNavigatorContext.this.getFocusListener();
            }

            @Override
            public CustomClassListener getClassListener() {
                return RemoteNavigatorContext.this.getClassListener();
            }

            @Override
            public Long getCurrentComputer() {
                return RemoteNavigatorContext.this.getCurrentComputer();
            }

            @Override
            public Long getCurrentUser() {
                return RemoteNavigatorContext.this.getCurrentUser();
            }

            @Override
            public Long getCurrentUserRole() {
                return RemoteNavigatorContext.this.getCurrentUserRole();
            }

            @Override
            public LogInfo getLogInfo() {
                return RemoteNavigatorContext.this.getLogInfo();
            }

            @Override
            public Locale getLocale() {
                return RemoteNavigatorContext.this.getLocale();
            }
        };
    }

    @Override
    protected RemoteConnection getConnectionObject() {
        return navigator;
    }

    public void aspectDelayUserInteraction(ClientAction action, String message) {
        navigator.delayUserInteraction(action);
    }

    @Override
    protected Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
        return navigator.requestUserInteraction(actions);
    }

    public FocusListener getFocusListener() {
        return navigator;
    }

    public CustomClassListener getClassListener() {
        return navigator;
    }

    // UI interfaces, multiple inheritance
    
    @Override
    public void requestFormUserInteraction(FormInstance formInstance, ModalityType modalityType, boolean forbidDuplicate, ExecutionStack stack) throws SQLException, SQLHandledException {
        uiContext.requestFormUserInteraction(formInstance, modalityType, forbidDuplicate, stack);
    }

    public ObjectValue requestUserObject(DialogRequest dialog, ExecutionStack stack) throws SQLException, SQLHandledException { // null если canceled
        return uiContext.requestUserObject(dialog, stack);
    }

    public ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        return uiContext.requestUserData(dataClass, oldValue);
    }

    public ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete) {
        return uiContext.requestUserClass(baseClass, defaultValue, concrete);
    }

    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<ContextFilter> contextFilters, ImSet<PullChangeProperty> pullProps, boolean readonly) throws SQLException, SQLHandledException {
        return uiContext.createFormInstance(formEntity, mapObjects, session, isModal, noCancel, manageSession, stack, checkOnOk, showDrop, interactive, contextFilters, pullProps, readonly);
    }

    public RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
        return uiContext.createRemoteForm(formInstance, stack);
    }

}
