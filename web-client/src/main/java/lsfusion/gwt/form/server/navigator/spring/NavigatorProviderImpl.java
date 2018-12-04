package lsfusion.gwt.form.server.navigator.spring;

import com.google.gwt.core.client.GWT;
import lsfusion.client.logics.ClientForm;
import lsfusion.client.logics.ClientFormChanges;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.gwt.base.server.spring.InvalidateListener;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.server.convert.ClientComponentToGwtConverter;
import lsfusion.gwt.form.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.gwt.form.server.form.spring.FormProvider;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.server.logics.spring.LogicsProvider;
import lsfusion.gwt.form.server.logics.spring.LogicsProviderImpl;
import lsfusion.gwt.form.shared.view.*;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

import static lsfusion.gwt.form.server.convert.StaticConverters.convertFont;

// session scoped - one for one browser (! not tab)
public class NavigatorProviderImpl implements NavigatorProvider, DisposableBean, InvalidateListener {

    private LogicsProvider blProvider;

    private volatile RemoteNavigatorInterface navigator;
    private final Object navigatorLock = new Object();

    public NavigatorProviderImpl(LogicsProvider blProvider) {
        this.blProvider = blProvider;
        blProvider.addInvalidateListener(this);
    }

    public String servSID = GwtSharedUtils.randomString(25);

    private ClientCallBackInterface clientCallBack;

    public final Set<String> openTabs = new HashSet<String>();

    public GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, String tabSID, FormProvider formProvider) throws IOException {
        FormClientAction.methodNames = FormClientAction.methodNames; // чтобы не потерять
        ClientForm clientForm = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(immutableMethods != null ? (byte[])immutableMethods[2] : remoteForm.getRichDesignByteArray())));

        GForm gForm = new ClientComponentToGwtConverter().convertOrCast(clientForm);

        gForm.sID = formSID;
        gForm.canonicalName = canonicalName;

        if (firstChanges != null) {
            gForm.initialFormChanges = ClientFormChangesToGwtConverter.getInstance().convertOrCast(
                    new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(firstChanges)), clientForm),
                    -1,
                    blProvider
            );
        }

        FormUserPreferences formUP = immutableMethods != null ? (FormUserPreferences)immutableMethods[0] : remoteForm.getUserPreferences();

        if (formUP != null) {
            gForm.userPreferences = new GFormUserPreferences(convertUserPreferences(gForm, formUP.getGroupObjectGeneralPreferencesList()),
                                                            convertUserPreferences(gForm, formUP.getGroupObjectUserPreferencesList()));
        }

        gForm.sessionID = formProvider.addFormSessionObject(new FormSessionObject(clientForm, remoteForm, tabSID));
        return gForm;
    }

    private static List<GGroupObjectUserPreferences> convertUserPreferences(GForm gForm, List<GroupObjectUserPreferences> groupObjectUserPreferences) {
        ArrayList<GGroupObjectUserPreferences> gGroupObjectUPList = new ArrayList<>();
        for (GroupObjectUserPreferences groupObjectUP : groupObjectUserPreferences) {
            HashMap<String, GColumnUserPreferences> gColumnUPMap = new HashMap<>();
            for (Map.Entry<String, ColumnUserPreferences> entry : groupObjectUP.getColumnUserPreferences().entrySet()) {
                ColumnUserPreferences columnUP = entry.getValue();
                gColumnUPMap.put(entry.getKey(), new GColumnUserPreferences(columnUP.userHide, columnUP.userCaption, columnUP.userPattern, columnUP.userWidth, columnUP.userOrder, columnUP.userSort, columnUP.userAscendingSort));
            }
            GFont userFont = convertFont(groupObjectUP.fontInfo);
            GGroupObject groupObj = gForm.getGroupObject(groupObjectUP.groupObjectSID);
            if (groupObj != null && groupObj.grid.font != null && groupObj.grid.font.size != 0) {
                if (userFont.size == 0) {
                    userFont.size = groupObj.grid.font.size;
                }
                userFont.family = groupObj.grid.font.family;
            } else {
                if (userFont.size == 0) {
                    userFont.size = GFont.DEFAULT_FONT_SIZE;
                }
                userFont.family = GFont.DEFAULT_FONT_FAMILY;
            }
            gGroupObjectUPList.add(new GGroupObjectUserPreferences(gColumnUPMap, groupObjectUP.groupObjectSID, userFont, groupObjectUP.pageSize, groupObjectUP.headerHeight, groupObjectUP.hasUserPreferences));
            gForm.addFont(userFont); // добавляем к используемым шрифтам с целью подготовить FontMetrics
        }
        return gGroupObjectUPList;
    }

    @Override
    public void tabOpened(String tabSID) {
        synchronized (openTabs) {
            openTabs.add(tabSID);
        }
    }

    @Override
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

    public void onInvalidate() {
        // пока не null'им навигатор, а то потом он собирается через unreferenced и убивает все свои формы
//        invalidate();
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
        blProvider.removeInvalidateListener(this);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        GWT.log("Destroying navigator for user " + (auth == null ? "UNKNOWN" : auth.getName()) + "...", new Exception());

        RemoteNavigatorInterface navigator = getNavigator();
        if (navigator != null) {
            navigator.close();
        }
    }
}
