package lsfusion.gwt.server.form.navigator.spring;

import com.google.gwt.core.client.GWT;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.SystemUtils;
import lsfusion.gwt.shared.base.GwtSharedUtils;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class LogicsAndNavigatorSessionObject {

    public final RemoteLogicsInterface remoteLogics;
    public final RemoteNavigatorInterface remoteNavigator;

    public LogicsAndNavigatorSessionObject(RemoteLogicsInterface remoteLogics, RemoteNavigatorInterface remoteNavigator) {
        this.remoteLogics = remoteLogics;
        this.remoteNavigator = remoteNavigator;
    }

    public ClientCallBackInterface remoteCallback; // caching
    public ClientCallBackInterface getRemoteCallback() throws RemoteException {
        if(remoteCallback == null)
            remoteCallback = remoteNavigator.getClientCallBack();
        return remoteCallback;
    }


    public String servSID = GwtSharedUtils.randomString(25);
}
