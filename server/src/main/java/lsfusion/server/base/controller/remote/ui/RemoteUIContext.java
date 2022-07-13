package lsfusion.server.base.controller.remote.ui;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ChooseClassClientAction;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.action.RequestUserInputClientAction;
import lsfusion.interop.form.ShowFormType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.interop.form.property.cell.UserInputResult;
import lsfusion.server.base.controller.context.AbstractContext;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.action.async.InputList;
import lsfusion.server.logics.form.interactive.action.async.AsyncSerializer;
import lsfusion.server.logics.form.interactive.action.input.InputContext;
import lsfusion.server.logics.form.interactive.action.input.InputResult;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.interactive.listener.RemoteFormListener;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

import static lsfusion.base.BaseUtils.serializeObject;
import static lsfusion.server.data.type.TypeSerializer.serializeType;

public abstract class RemoteUIContext extends AbstractContext {

    @Override
    public void requestFormUserInteraction(FormInstance formInstance, ShowFormType showFormType, boolean forbidDuplicate, ExecutionStack stack) throws SQLException, SQLHandledException {
        requestFormUserInteraction(createRemoteForm(formInstance, stack), showFormType, forbidDuplicate, stack);
    }

    protected void requestFormUserInteraction(RemoteForm remoteForm, ShowFormType showFormType, boolean forbidDuplicate, ExecutionStack stack) throws SQLException, SQLHandledException {
        FormClientAction action = new FormClientAction(remoteForm.getCanonicalName(), remoteForm.getSID(), forbidDuplicate, remoteForm, remoteForm.getImmutableMethods(), Settings.get().isDisableFirstChangesOptimization() ? null : remoteForm.getFormChangesByteArray(stack), showFormType);
        if(showFormType.isModal()) {
            requestUserInteraction(action);
            remoteForm.form.syncLikelyOnClose(true, stack);
        } else
            delayUserInteraction(action);
    }

    private InputContext inputContext;
    private final ReentrantLock inputContextLock = new ReentrantLock();
    private Thread inputContextLockThread;

    @Override
    public InputContext lockInputContext() {
        inputContextLock.lock();
        this.inputContextLockThread = Thread.currentThread();
        return this.inputContext;
    }

    public void unlockInputContext() {
        this.inputContextLockThread = null;
        inputContextLock.unlock();
    }

    public InputResult inputUserData(DataClass dataClass, Object oldValue, boolean hasOldValue, InputContext inputContext, String customChangeFunction, InputList inputList) {
        this.inputContext = inputContext; // we don't have to lock here since thread-safety will be ok anyway
        try {
            UserInputResult result = (UserInputResult) requestUserInteraction(new RequestUserInputClientAction(serializeType(dataClass), serializeObject(oldValue), hasOldValue, customChangeFunction, inputContext != null ? AsyncSerializer.serializeInputList(inputList) : null));
            if (result.isCanceled()) {
                return null;
            }
            return InputResult.get(result, dataClass);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            boolean locked = inputContextLock.tryLock();
            if(!locked) {
                // canceling locking thread and lock after all
                Thread interruptThread = this.inputContextLockThread;
                if(interruptThread != null) { // it can be already unlocked but it doesn't matter, since the input is already finished
                    try {
                        ThreadUtils.interruptThread(this, interruptThread);
                    } catch (Throwable t) {
                        ServerLoggers.sqlSuppLog(t);
                    }
                }

                inputContextLock.lock();
            }
            try {
                this.inputContext = null;
            } finally {
                inputContextLock.unlock();
            }
        }
    }

    public abstract FocusListener getFocusListener();
    protected abstract SecurityPolicy getSecurityPolicy();
    protected boolean isExternal() {
        return false;
    }

    public FormInstance createFormInstance(FormEntity formEntity, ImSet<ObjectEntity> inputObjects, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, WindowFormType type, ImSet<ContextFilterInstance> contextFilters, boolean readonly) throws SQLException, SQLHandledException {
        return new FormInstance(formEntity, getLogicsInstance(), inputObjects,
                session,
                getSecurityPolicy(), getFocusListener(), getClassListener(),
                mapObjects, stack, isModal,
                noCancel, manageSession,
                checkOnOk, showDrop, interactive, type, isExternal(), contextFilters, readonly, getLocale());
    }

    protected abstract int getExportPort();
    protected abstract RemoteFormListener getFormListener();

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
        try {
            return new RemoteForm<>(formInstance, getExportPort(), getFormListener(), stack);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            baseClass.serialize(dataStream);
            defaultValue.serialize(dataStream);
            Long result = (Long) requestUserInteraction(new ChooseClassClientAction(outStream.toByteArray(), concrete));
            if (result == null) {
                return null;
            }
            return new DataObject(result, baseClass.getBaseClass().objectClass);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

}
