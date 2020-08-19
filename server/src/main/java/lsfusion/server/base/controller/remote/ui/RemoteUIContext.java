package lsfusion.server.base.controller.remote.ui;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ChooseClassClientAction;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.action.RequestUserInputClientAction;
import lsfusion.interop.form.ModalityType;
import lsfusion.interop.form.property.cell.UserInputResult;
import lsfusion.server.base.controller.context.AbstractContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.FormCloseType;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;
import lsfusion.server.logics.form.interactive.dialogedit.DialogRequest;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.interactive.listener.RemoteFormListener;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static lsfusion.base.BaseUtils.serializeObject;
import static lsfusion.server.data.type.TypeSerializer.serializeType;

public abstract class RemoteUIContext extends AbstractContext {

    @Override
    public void requestFormUserInteraction(FormInstance formInstance, ModalityType modalityType, boolean forbidDuplicate, ExecutionStack stack) throws SQLException, SQLHandledException {
        requestFormUserInteraction(createRemoteForm(formInstance, stack), modalityType, forbidDuplicate, stack);
    }

    protected void requestFormUserInteraction(RemoteForm remoteForm, ModalityType modalityType, boolean forbidDuplicate, ExecutionStack stack) throws SQLException, SQLHandledException {
        FormClientAction action = new FormClientAction(remoteForm.getCanonicalName(), remoteForm.getSID(), forbidDuplicate, remoteForm, remoteForm.getImmutableMethods(), Settings.get().isDisableFirstChangesOptimization() ? null : remoteForm.getFormChangesByteArray(stack), modalityType);
        if(modalityType.isModal()) {
            requestUserInteraction(action);
            remoteForm.form.syncLikelyOnClose(true, stack);
        } else
            delayUserInteraction(action);
    }

    public ObjectValue requestUserObject(DialogRequest dialog, ExecutionStack stack) throws SQLException, SQLHandledException { // null если canceled
        FormInstance dialogInstance = dialog.createDialog();
        if (dialogInstance == null) {
            return null;
        }

        requestFormUserInteraction(dialogInstance, ModalityType.DIALOG_MODAL, false, stack);

        if (dialogInstance.getFormResult() == FormCloseType.CLOSE) {
            return null;
        }
        return dialogInstance.getFormResult() == FormCloseType.DROP ? NullValue.instance : dialog.getValue();
    }

    public ObjectValue requestUserData(DataClass dataClass, Object oldValue, boolean hasOldValue) {
        try {
            UserInputResult result = (UserInputResult) requestUserInteraction(new RequestUserInputClientAction(serializeType(dataClass), serializeObject(oldValue), hasOldValue));
            if (result.isCanceled()) {
                return null;
            }
            return result.getValue() == null ? NullValue.instance : new DataObject(result.getValue(), dataClass);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public abstract FocusListener getFocusListener();
    protected abstract SecurityPolicy getSecurityPolicy();
    protected boolean isExternal() {
        return false;
    }

    public FormInstance createFormInstance(FormEntity formEntity, List<String> inputObjects, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, boolean isFloat, ImSet<ContextFilterInstance> contextFilters, boolean readonly) throws SQLException, SQLHandledException {
        return new FormInstance(formEntity, getLogicsInstance(), inputObjects,
                session,
                getSecurityPolicy(), getFocusListener(), getClassListener(),
                mapObjects, stack, isModal,
                noCancel, manageSession,
                checkOnOk, showDrop, interactive, isFloat, isExternal(), contextFilters, readonly, getLocale());
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
