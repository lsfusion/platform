package lsfusion.server.remote;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.GUIPreferences;
import lsfusion.interop.LocalePreferences;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.VMOptions;
import lsfusion.interop.event.IDaemonTask;
import lsfusion.interop.exceptions.RemoteMessageException;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenParameters;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.SystemProperties;
import lsfusion.server.auth.User;
import lsfusion.server.classes.*;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.lifecycle.LifecycleListener;
import lsfusion.server.logics.*;
import lsfusion.server.logics.SecurityManager;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.DataSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lsfusion.server.context.ThreadLocalContext.localize;

public class RemoteLogics<T extends BusinessLogics> extends ContextAwarePendingRemoteObject implements RemoteLogicsInterface, InitializingBean, LifecycleListener {
    protected final static Logger logger = ServerLoggers.remoteLogger;

    protected T businessLogics;
    protected BaseLogicsModule baseLM;

    protected NavigatorsManager navigatorsManager;

    protected RestartManager restartManager;
    
    protected lsfusion.server.logics.SecurityManager securityManager;

    protected DBManager dbManager;

    private VMOptions clientVMOptions;

    private String displayName;
    private String name;
    
    private Integer twoDigitYearStart;
    private String userTimeZone;
    private String userCountry;
    private String userLanguage;
    
    private String clientHideMenu;

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.businessLogics = (T) businessLogics;
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        setContext(logicsInstance.getContext());
    }

    public void setNavigatorsManager(NavigatorsManager navigatorsManager) {
        this.navigatorsManager = navigatorsManager;
    }

    public void setRestartManager(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    public void setClientVMOptions(VMOptions clientVMOptions) {
        this.clientVMOptions = clientVMOptions;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void setTwoDigitYearStart(Integer twoDigitYearStart) {
        this.twoDigitYearStart = twoDigitYearStart;
    }
    
    public void setUserTimeZone(String userTimeZone) {
        this.userTimeZone = userTimeZone;
    }
    
    public void setUserCountry(String userCountry) {
        this.userCountry = userCountry;
    }
    
    public void setUserLanguage(String userLanguage) {
        this.userLanguage = userLanguage;
    }
    
    public void setClientHideMenu(String clientHideMenu) {
        this.clientHideMenu = clientHideMenu;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(navigatorsManager, "navigatorsManager must be specified");
        Assert.notNull(restartManager, "restartManager must be specified");
        Assert.notNull(securityManager, "securityManager must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
        Assert.notNull(clientVMOptions, "clientVMOptions must be specified");
        //assert logicsInstance by checking the context
        Assert.notNull(getContext(), "logicsInstance must be specified");

        if (name == null) {
            name = businessLogics.getClass().getSimpleName();
        }
    }

    @Override
    public int getOrder() {
        return BLLOADER_ORDER - 1;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (LifecycleEvent.INIT.equals(event.getType())) {
            this.baseLM = businessLogics.LM;
        }
    }

    public RemoteNavigatorInterface createNavigator(boolean isFullClient, NavigatorInfo navigatorInfo, boolean reuseSession) {
        if (restartManager.isPendingRestart() && (navigatorInfo.login == null || !navigatorInfo.login.equals("admin"))) {
            return null;
        }

        return navigatorsManager.createNavigator(getStack(), isFullClient, navigatorInfo, reuseSession);
    }

    protected DataSession createSession() throws SQLException {
        return dbManager.createSession();
    }

    public Integer getComputer(String strHostName) {
        return dbManager.getComputer(strHostName, getStack());
    }

    @Override
    public ArrayList<IDaemonTask> getDaemonTasks(int compId) throws RemoteException {
        return businessLogics.getDaemonTasks(compId);
    }

    public ExternalScreen getExternalScreen(int screenID) {
        return businessLogics.getExternalScreen(screenID);
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        return businessLogics.getExternalScreenParameters(screenID, computerId);
    }

    public void ping() throws RemoteException {
        //for filterIncl-alive
    }

    @Override
    public Integer getApiVersion() throws RemoteException {
        return 19;
    }

    public GUIPreferences getGUIPreferences() throws RemoteException {
        return new GUIPreferences(name, displayName, null, null, Boolean.parseBoolean(clientHideMenu));
    }
    
    public LocalePreferences getDefaultLocalePreferences() throws RemoteException {
        return new LocalePreferences(userLanguage, userCountry, userTimeZone, twoDigitYearStart, false);
    }

    public int generateNewID() throws RemoteException {
        return BaseLogicsModule.generateStaticNewID();
    }

    public void sendPingInfo(Integer computerId, Map<Long, List<Long>> pingInfoMap) {
        Map<Long, List<Long>> pingInfoEntry = RemoteLoggerAspect.pingInfoMap.get(computerId);
        pingInfoEntry = pingInfoEntry != null ? pingInfoEntry : MapFact.<Long, List<Long>>getGlobalConcurrentHashMap();
        pingInfoEntry.putAll(pingInfoMap);
        RemoteLoggerAspect.pingInfoMap.put(computerId, pingInfoEntry);
    }

    @Override
    public List<String> authenticateUser(String userName, String password) throws RemoteException {
        DataSession session;
        try {
            session = dbManager.createSession();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        try {
            User user = securityManager.authenticateUser(session, userName, password, getStack());
            if (user != null) {
                return securityManager.getUserRolesNames(userName, getExtraUserRoleNames(userName));    
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @Override
    public VMOptions getClientVMOptions() throws RemoteException {
        return clientVMOptions;
    }

    @Override
    public int generateID() throws RemoteException {
        return dbManager.generateID();
    }

    protected List<String> getExtraUserRoleNames(String username) {
        return new ArrayList<>();
    }

    protected Integer getUserByEmail(DataSession session, String email) throws SQLException, SQLHandledException {
        return (Integer) businessLogics.contactLM.contactEmail.read(session, new DataObject(email));
    }

    @Override
    public void remindPassword(String email, String localeLanguage) throws RemoteException {
        assert email != null;
        //todo: в будущем нужно поменять на проставление локали в Context
//            ServerResourceBundle.load(localeLanguage);
        try {
            try (DataSession session = createSession()) {
                Integer userId = getUserByEmail(session, email);
                if (userId == null) {
                    throw new RuntimeException(localize("{mail.user.not.found}") + ": " + email);
                }

                businessLogics.emailLM.emailUserPassUser.execute(session, getStack(), new DataObject(userId, businessLogics.authenticationLM.customUser));
            }
        } catch (Exception e) {
            logger.error("Error reminding password: ", e);
            throw new RemoteMessageException(localize("{mail.error.sending.password.remind}"), e);
        }
    }

    @Override
    public boolean checkPropertyViewPermission(String userName, String propertySID) {
        return securityManager.checkPropertyViewPermission(userName, propertySID);
    }

    @Override
    public boolean checkPropertyChangePermission(String userName, String propertySID) throws RemoteException {
        return securityManager.checkPropertyChangePermission(userName, propertySID);
    }

    @Override
    public boolean checkDefaultViewPermission(String propertySid) throws RemoteException {
        return securityManager.checkDefaultViewPermission(propertySid);
    }

    public boolean checkFormExportPermission(String canonicalName) throws RemoteException {
        return securityManager.checkFormExportPermission(canonicalName);
    }

    public boolean isSingleInstance() throws RemoteException {
        return Settings.get().isSingleInstance();
    }

    public boolean isBusyDialog() throws RemoteException {
        return Settings.get().isBusyDialog() || SystemProperties.isDebug;
    }

    @Override
    public byte[] readFile(String canonicalName, String... params) throws RemoteException {
        LCP<PropertyInterface> property = (LCP) businessLogics.findProperty(canonicalName);
        if (property != null) {
            if (!(property.property.getType() instanceof FileClass)) {
                throw new RuntimeException("Property type is distinct from FileClass");
            }
            ImOrderSet<PropertyInterface> interfaces = property.listInterfaces;
            DataObject[] objects = new DataObject[interfaces.size()];
            byte[] fileBytes;
            try {
                DataSession session = createSession();
                ImMap<PropertyInterface, ValueClass> interfaceClasses = property.property.getInterfaceClasses(ClassType.filePolicy);
                for (int i = 0; i < interfaces.size(); i++) {
                    objects[i] = session.getDataObject(interfaceClasses.get(interfaces.get(i)), Integer.decode(params[i]));
                }
                fileBytes = (byte[]) property.read(session, objects);

                if (fileBytes != null && !(property.property.getType() instanceof DynamicFormatFileClass)) {
                    fileBytes = BaseUtils.mergeFileAndExtension(fileBytes, ((StaticFormatFileClass) property.property.getType()).getOpenExtension(fileBytes).getBytes());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return fileBytes;
        } else {
            throw new RuntimeException("Property was not found");
        }
    }

    @Override
    public void runAction(String canonicalName, String... params) throws RemoteException {
        LAP property = (LAP) businessLogics.findProperty(canonicalName);
        if (property != null) {
            try {
                DataSession session = createSession();
                ImOrderSet<PropertyInterface> interfaces = property.listInterfaces;
                ImMap<PropertyInterface, ValueClass> interfaceClasses = property.property.getInterfaceClasses(ClassType.editPolicy);

                DataObject[] objects = new DataObject[interfaces.size()];
                for (int i = 0; i < interfaces.size(); i++) {
                    Object objectValue = null;
                    ValueClass valueClass = interfaceClasses.get(interfaces.get(i));
                    if (valueClass instanceof CustomClass) {
                        objectValue = Integer.parseInt(params[i]);
                    } else if (valueClass instanceof DataClass) {
                        objectValue = ((DataClass) valueClass).parseString(params[i]);
                    }
                    objects[i] = session.getDataObject(valueClass, objectValue);
                }
                ExecutionStack stack = getStack();
                property.execute(session, stack, objects);
                session.apply(businessLogics, stack);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Action was not found");
        }
    }

    @Override
    public String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException {
        return securityManager.addUser(username, email, password, firstName, lastName, localeLanguage, getStack());
    }
    
    public Integer getCurrentUser() {
        return dbManager.getSystemUserObject();
    }

    @Override
    protected boolean isEnabledUnreferenced() { // иначе когда отключаются все клиенты логика закрывается
        return false;
    }

    @Override
    public String getSID() {
        return "logics";
    }

    @Override
    protected boolean isUnreferencedSyncedClient() { // если ушли все ссылки считаем синхронизированным, так как клиент уже ни к чему обращаться не может
        return true;
    }
}

