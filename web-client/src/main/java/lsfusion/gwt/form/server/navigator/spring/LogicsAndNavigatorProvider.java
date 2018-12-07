package lsfusion.gwt.form.server.navigator.spring;

import lsfusion.gwt.form.server.form.spring.FormProvider;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.remote.ClientCallBackInterface;

import java.io.IOException;
import java.rmi.RemoteException;

public interface LogicsAndNavigatorProvider {

    String createNavigator(RemoteLogicsInterface remoteLogics) throws RemoteException;
    LogicsAndNavigatorSessionObject getLogicsAndNavigatorSessionObject(String sessionID);
    void removeLogicsAndNavigatorSessionObject(String sessionID) throws RemoteException;

    String getSessionInfo();
}
