package lsfusion.server.form.instance.listener;

import lsfusion.server.context.ExecutionStack;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.session.ExecutionEnvironment;

import java.rmi.RemoteException;

public interface RemoteFormListener {
    void formCreated(RemoteForm form);
    void formClosed(RemoteForm form);
    void executeNotificationAction(ExecutionEnvironment env, ExecutionStack stack, Integer idNotification) throws RemoteException;
}
