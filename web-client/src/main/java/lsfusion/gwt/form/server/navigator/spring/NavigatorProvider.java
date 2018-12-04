package lsfusion.gwt.form.server.navigator.spring;

import lsfusion.gwt.form.server.form.spring.FormProvider;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;

import java.io.IOException;
import java.rmi.RemoteException;

public interface NavigatorProvider {

    String addNavigatorSessionObject(NavigatorSessionObject navigatorSessionObject);
    NavigatorSessionObject getNavigatorSessionObject(String navigatorID);
    void removeNavigatorSessionObject(String navigatorID);

    void invalidate();
    
    void tabOpened(String tabSID);
    boolean tabClosed(String tabSID);
    String getSessionInfo();
    ClientCallBackInterface getClientCallBack() throws RemoteException; // caching

    GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, String tabSID, FormProvider formProvider) throws IOException;
}
