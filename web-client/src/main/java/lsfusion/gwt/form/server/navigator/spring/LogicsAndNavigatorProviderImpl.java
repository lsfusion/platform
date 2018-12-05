package lsfusion.gwt.form.server.navigator.spring;

import com.google.gwt.core.client.GWT;
import lsfusion.client.logics.ClientForm;
import lsfusion.client.logics.ClientFormChanges;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.server.convert.ClientComponentToGwtConverter;
import lsfusion.gwt.form.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.gwt.form.server.form.spring.FormProvider;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.shared.view.*;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

import static lsfusion.gwt.form.server.convert.StaticConverters.convertFont;

// session scoped - one for one browser (! not tab)
public class LogicsAndNavigatorProviderImpl implements LogicsAndNavigatorProvider, DisposableBean {

    private final Object navigatorLock = new Object();

    public String servSID = GwtSharedUtils.randomString(25);

    private ClientCallBackInterface clientCallBack;

    public final Set<String> openTabs = new HashSet<String>();

    public void tabOpened(String tabSID) {
        synchronized (openTabs) {
            openTabs.add(tabSID);
        }
    }

    public synchronized boolean tabClosed(String tabSID) {
        synchronized (openTabs) {
            openTabs.remove(tabSID);
            return openTabs.isEmpty();
        }
    }

    @Override
    public String getSessionInfo() {
        synchronized (openTabs) {
            return "SESSION " + servSID + " CURRENT OPENED TABS " + openTabs;
        }
    }

    public void invalidate() {
        synchronized (openTabs) {
            assert openTabs.isEmpty();
        }
        synchronized (navigatorLock) {
            navigator = null;
        }
        clientCallBack = null;
    }

    @Override
    public void destroy() throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        GWT.log("Destroying navigator for user " + (auth == null ? "UNKNOWN" : auth.getName()) + "...", new Exception());

        RemoteNavigatorInterface navigator = getNavigator();
        if (navigator != null) {
            navigator.close();
        }
    }
}
