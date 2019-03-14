package lsfusion.server.base.remote.ui;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ChooseClassClientAction;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.action.RequestUserInputClientAction;
import lsfusion.interop.form.ModalityType;
import lsfusion.interop.form.user.UserInputResult;
import lsfusion.server.Settings;
import lsfusion.server.base.context.AbstractContext;
import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.NullValue;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.classes.DataClass;
import lsfusion.server.logics.form.interactive.FormCloseType;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.dialogedit.DialogRequest;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.interactive.listener.RemoteFormListener;
import lsfusion.server.logics.form.interactive.instance.remote.RemoteForm;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilter;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.derived.PullChangeProperty;
import lsfusion.server.physics.admin.authentication.policy.SecurityPolicy;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.serializeObject;
import static lsfusion.server.data.type.TypeSerializer.serializeType;

public abstract class RemoteUIContext extends AbstractContext {

    @Override
    public void requestFormUserInteraction(FormInstance formInstance, ModalityType modalityType, boolean forbidDuplicate, ExecutionStack stack) throws SQLException, SQLHandledException {
        FormEntity formEntity = formInstance.entity;
        RemoteForm remoteForm = createRemoteForm(formInstance, stack);
        FormClientAction action = new FormClientAction(formEntity.getCanonicalName(), formEntity.getSID(), forbidDuplicate, remoteForm, remoteForm.getImmutableMethods(), Settings.get().isDisableFirstChangesOptimization() ? null : remoteForm.getFormChangesByteArray(stack), modalityType);
        if(modalityType.isModal()) {
            requestUserInteraction(action);
            formInstance.syncLikelyOnClose(true, stack);
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

    public ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        try {
            UserInputResult result = (UserInputResult) requestUserInteraction(new RequestUserInputClientAction(serializeType(dataClass), serializeObject(oldValue)));
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

    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<ContextFilter> contextFilters, ImSet<PullChangeProperty> pullProps, boolean readonly) throws SQLException, SQLHandledException {
        return new FormInstance(formEntity, getLogicsInstance(),
                session,
                getSecurityPolicy(), getFocusListener(), getClassListener(),
                mapObjects, stack, isModal,
                noCancel, manageSession,
                checkOnOk, showDrop, interactive, contextFilters, pullProps, readonly, getLocale());
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
