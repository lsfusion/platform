package lsfusion.gwt.form.server.navigator.spring;

import com.google.gwt.core.client.GWT;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.SystemUtils;
import lsfusion.client.logics.ClientForm;
import lsfusion.client.logics.ClientFormChanges;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.gwt.base.server.spring.InvalidateListener;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.server.convert.ClientComponentToGwtConverter;
import lsfusion.gwt.form.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.gwt.form.server.form.spring.FormProviderImpl;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.server.logics.spring.LogicsProvider;
import lsfusion.gwt.form.shared.view.*;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
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

    public static GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, String tabSID, String formID, FormProviderImpl formProvider) throws IOException {
        FormClientAction.methodNames = FormClientAction.methodNames; // чтобы не потерять
        ClientForm clientForm = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(immutableMethods != null ? (byte[])immutableMethods[2] : remoteForm.getRichDesignByteArray())));

        GForm gForm = new ClientComponentToGwtConverter().convertOrCast(clientForm);

        gForm.sID = formSID;
        gForm.canonicalName = canonicalName;
        gForm.sessionID = formID;

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

        FormSessionObject formSessionObject = new FormSessionObject(clientForm, remoteForm, tabSID);

        formProvider.addFormSessionObject(formID, formSessionObject);
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
    public ClientCallBackInterface getClientCallBack() throws RemoteException {
        if(clientCallBack == null)
            clientCallBack = getNavigator().getClientCallBack();
        return clientCallBack;
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

    @Override
    public RemoteNavigatorInterface getNavigator() throws RemoteException {
        //double-check locking
        if (navigator == null) {
            synchronized (navigatorLock) {
                if (navigator == null) {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth == null) {
                        auth = new TestingAuthenticationToken("admin", "fusion");
                        //throw new IllegalStateException("Пользователь должен быть аутентифицирован, чтобы использовать навигатор.");
                    }

                    String username = auth.getName();
                    String password = (String) auth.getCredentials();
                    String osVersion = System.getProperty("os.name");
                    String processor = System.getenv("PROCESSOR_IDENTIFIER");

                    String architecture = System.getProperty("os.arch");
                    if (osVersion.startsWith("Windows")) {
                        String arch = System.getenv("PROCESSOR_ARCHITECTURE");
                        String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
                        architecture = arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64") ? "x64" : "x32";
                    }

                    Integer cores = Runtime.getRuntime().availableProcessors();
                    com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean)
                            java.lang.management.ManagementFactory.getOperatingSystemMXBean();
                    Integer physicalMemory = (int) (os.getTotalPhysicalMemorySize() / 1048576);
                    Integer totalMemory = (int) (Runtime.getRuntime().totalMemory() / 1048576);
                    Integer maximumMemory = (int) (Runtime.getRuntime().maxMemory() / 1048576);
                    Integer freeMemory = (int) (Runtime.getRuntime().freeMemory() / 1048576);
                    String javaVersion = SystemUtils.getJavaVersion() + " " + System.getProperty("sun.arch.data.model") + " bit";

                    String language = Locale.getDefault().getLanguage();
                    String country = Locale.getDefault().getCountry(); 
                    
                    try {
                        RemoteLogicsInterface bl = blProvider.getLogics();
/*                        RemoteNavigatorInterface unsynced = bl.createNavigator(true, new NavigatorInfo(username, password,
                                bl.getComputer(SystemUtils.getLocalHostName()), ((WebAuthenticationDetails) auth.getDetails()).getRemoteAddress(),
                                osVersion, processor, architecture, cores, physicalMemory, totalMemory, maximumMemory, freeMemory,
                                javaVersion, null, language, country), true);*/
                        RemoteNavigatorInterface unsynced = bl.createNavigator(true, new NavigatorInfo(username, password,
                                bl.getComputer(SystemUtils.getLocalHostName()), "127.0.0.1", osVersion, processor, architecture,
                                cores, physicalMemory, totalMemory, maximumMemory, freeMemory, javaVersion, null, language, country), true);
                        navigator = unsynced; // ReflectionUtils.makeSynchronized(RemoteNavigatorInterface.class, unsynced) - в десктопе не синхронизировалось, непонятно зачем здесь синхронизировать
                    } catch (RemoteException e) {
                        blProvider.invalidate();
                        throw e;
                    }
                }
            }
        }

        return navigator;
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
