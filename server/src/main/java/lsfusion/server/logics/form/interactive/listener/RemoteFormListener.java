package lsfusion.server.logics.form.interactive.listener;

import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;

import java.rmi.RemoteException;

public interface RemoteFormListener {
    void formCreated(RemoteForm form);
    void formClosed(RemoteForm form);
    void executeNotificationAction(ExecutionEnvironment env, ExecutionStack stack, Integer idNotification) throws RemoteException;
}
