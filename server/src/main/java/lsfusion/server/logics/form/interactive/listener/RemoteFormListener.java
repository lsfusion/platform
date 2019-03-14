package lsfusion.server.logics.form.interactive.listener;

import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.logics.action.ExecutionEnvironment;
import lsfusion.server.remote.RemoteForm;

import java.rmi.RemoteException;

public interface RemoteFormListener {
    void formCreated(RemoteForm form);
    void formClosed(RemoteForm form);
    void executeNotificationAction(ExecutionEnvironment env, ExecutionStack stack, Integer idNotification) throws RemoteException;
}
