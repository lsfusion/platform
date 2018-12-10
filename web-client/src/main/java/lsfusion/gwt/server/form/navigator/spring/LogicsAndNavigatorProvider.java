package lsfusion.gwt.server.form.navigator.spring;

import lsfusion.gwt.server.form.form.spring.FormProvider;
import lsfusion.gwt.shared.form.view.GForm;
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
