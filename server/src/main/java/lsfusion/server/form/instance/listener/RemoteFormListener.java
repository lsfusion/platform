package lsfusion.server.form.instance.listener;

import lsfusion.server.remote.RemoteForm;
import lsfusion.server.session.ExecutionEnvironment;

import java.rmi.RemoteException;

public interface RemoteFormListener {
    void formCreated(RemoteForm form);
    void formDestroyed(RemoteForm form);
    void executeNotificationAction(ExecutionEnvironment env, Integer idNotification) throws RemoteException;
}
