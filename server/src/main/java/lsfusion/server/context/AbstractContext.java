package lsfusion.server.context;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.ChooseClassClientAction;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.action.RequestUserInputClientAction;
import lsfusion.interop.form.UserInputResult;
import lsfusion.server.Settings;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ManageSessionType;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.DialogRequest;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.session.DataSession;
import lsfusion.server.stack.ExecutionStackAspect;
import lsfusion.server.stack.ExecutionStackItem;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static lsfusion.base.BaseUtils.serializeObject;
import static lsfusion.server.data.type.TypeSerializer.serializeType;

public abstract class AbstractContext implements Context {

    public abstract LogicsInstance getLogicsInstance();

    @Override
    public FormInstance getFormInstance() {
        return null;
    }

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

    @Override
    public void pushLogMessage() {
    }

    @Override
    public String popLogMessage() {
        throw new UnsupportedOperationException("popLogMessage is not supported");
    }

    @Override
    public void delayUserInteraction(ClientAction action) {
        throw new UnsupportedOperationException("delayUserInteraction is not supported");
    }

    @Override
    public Object requestUserInteraction(ClientAction action) {
        return requestUserInteraction(new ClientAction[]{action})[0];
    }

    @Override
    public boolean canBeProcessed() {
        return false;
    }

    @Override
    public Object[] requestUserInteraction(ClientAction... actions) {
        throw new UnsupportedOperationException("requestUserInteraction is not supported");
    }

    public abstract SecurityPolicy getSecurityPolicy();

    public abstract FocusListener getFocusListener();

    public abstract CustomClassListener getClassListener();

    public abstract PropertyObjectInterfaceInstance getComputer(ExecutionStack stack);

    public abstract Long getCurrentUser();

    public abstract Long getCurrentUserRole();

    public abstract DataObject getConnection();

    @Override
    public String localize(LocalizedString s) {
        return localize(s, getLocale());
    }

    public String localize(LocalizedString s, Locale locale) {
        return s.getString(locale, getLogicsInstance().getBusinessLogics().getLocalizer());
    }

    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters, ImSet<PullChangeProperty> pullProps, boolean readonly) throws SQLException, SQLHandledException {
        return new FormInstance(formEntity, getLogicsInstance(),
                session,
                getSecurityPolicy(), getFocusListener(), getClassListener(),
                getComputer(stack), getConnection(), mapObjects, stack, isModal,
                noCancel, manageSession,
                checkOnOk, showDrop, interactive, contextFilters, pullProps, readonly, getLocale());
    }

    public RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
        throw new UnsupportedOperationException("createRemoteForm is not supported");
    }
}
