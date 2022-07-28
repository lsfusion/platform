package lsfusion.server.logics.navigator.controller.context;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.form.ShowFormType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.base.controller.remote.ui.RemoteUIContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.action.async.InputList;
import lsfusion.server.logics.form.interactive.action.input.InputContext;
import lsfusion.server.logics.form.interactive.action.input.InputResult;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.interactive.listener.RemoteFormListener;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.navigator.controller.remote.RemoteNavigator;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnectionContext;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.admin.log.LogInfo;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Stack;

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
            public void aspectDelayUserInteraction(ClientAction action, String message) {
                RemoteNavigatorContext.this.aspectDelayUserInteraction(action, message);
            }

            @Override
            public Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
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
            public Long getCurrentConnection() {
                return RemoteNavigatorContext.this.getCurrentConnection();
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

            @Override
            protected boolean isExternal() {
                return getForm.get() != null;
            }

            @Override
            protected void requestFormUserInteraction(RemoteForm remoteForm, ShowFormType showFormType, boolean forbidDuplicate, String formId, ExecutionStack stack) throws SQLException, SQLHandledException {
                Stack<Result<RemoteForm>> getForms = getForm.get();
                if(getForms != null)
                    getForms.peek().set(remoteForm);
                else
                    super.requestFormUserInteraction(remoteForm, showFormType, forbidDuplicate, formId, stack);
            }
        };
    }

    private ThreadLocal<Stack<Result<RemoteForm>>> getForm = new ThreadLocal<>();

    public void pushGetForm() {
        Stack<Result<RemoteForm>> getForms = getForm.get();
        if(getForms == null) {
            getForms = new Stack<>();
            getForm.set(getForms);
        }
        getForms.push(new Result<>());
    }

    public RemoteForm popGetForm() {
        Stack<Result<RemoteForm>> getForms = getForm.get();
        Result<RemoteForm> result = getForms.pop();
        if(getForms.isEmpty())
            getForm.remove();
        return result.result;
    }

    @Override
    protected RemoteConnection getConnectionObject() {
        return navigator;
    }

    public void aspectDelayUserInteraction(ClientAction action, String message) {
        navigator.delayUserInteraction(action);
    }

    @Override
    public Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
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
    public void requestFormUserInteraction(FormInstance formInstance, ShowFormType showFormType, boolean forbidDuplicate, String formId, ExecutionStack stack) throws SQLException, SQLHandledException {
        uiContext.requestFormUserInteraction(formInstance, showFormType, forbidDuplicate, formId, stack);
    }

    @Override
    public InputContext lockInputContext() {
        return uiContext.lockInputContext();
    }

    @Override
    public void unlockInputContext() {
        uiContext.unlockInputContext();
    }

    public InputResult inputUserData(DataClass dataClass, Object oldValue, boolean hasOldValue, InputContext inputContext, String customChangeFunction, InputList inputList) {
        return uiContext.inputUserData(dataClass, oldValue, hasOldValue, inputContext, customChangeFunction, inputList);
    }

    public ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete) {
        return uiContext.requestUserClass(baseClass, defaultValue, concrete);
    }

    public FormInstance createFormInstance(FormEntity formEntity, ImSet<ObjectEntity> inputObjects, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, WindowFormType type, ImSet<ContextFilterInstance> contextFilters, boolean readonly, String formId) throws SQLException, SQLHandledException {
        return uiContext.createFormInstance(formEntity, inputObjects, mapObjects, session, isModal, noCancel, manageSession, stack, checkOnOk, showDrop, interactive, type, contextFilters, readonly, formId);
    }

    public RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
        return uiContext.createRemoteForm(formInstance, stack);
    }

}
