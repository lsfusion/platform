package lsfusion.server.remote;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.GUIPreferences;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.VMOptions;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.event.IDaemonTask;
import lsfusion.interop.exceptions.RemoteMessageException;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenParameters;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.auth.User;
import lsfusion.server.classes.*;
import lsfusion.server.data.JDBCTable;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.lifecycle.LifecycleListener;
import lsfusion.server.logics.SecurityManager;
import lsfusion.server.logics.*;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.external.ExternalHTTPActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.session.DataSession;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

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
    private String logicsLogo;
    private String name;
    
    private String clientHideMenu;

    public void setBusinessLogics(T businessLogics) {
        this.businessLogics = businessLogics;
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

    public void setLogicsLogo(String logicsLogo) {
        this.logicsLogo = logicsLogo;
    }

    public void setName(String name) {
        this.name = name;
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
        if (restartManager.isPendingRestart() && (navigatorInfo.login == null || !navigatorInfo.login.equals("admin")))
            throw new RemoteMessageException(ApiResourceBundle.getString("exceptions.server.is.restarting"));

        return navigatorsManager.createNavigator(getStack(), isFullClient, navigatorInfo, reuseSession);
    }

    @Override
    public Set<String> syncUsers(Set<String> userNames) throws RemoteException {
        try {
            return businessLogics.authenticationLM.syncUsers(userNames);
        } catch (Exception e) {
            throw new RuntimeException("Error synchronizing user names", e);
        }
    }

    protected DataSession createSession() throws SQLException {
        return dbManager.createSession();
    }

    public Long getComputer(String strHostName) {
        return dbManager.getComputer(strHostName, getStack());
    }

    @Override
    public ArrayList<IDaemonTask> getDaemonTasks(long compId) throws RemoteException {
        return businessLogics.getDaemonTasks(compId);
    }

    public ExternalScreen getExternalScreen(int screenID) {
        return businessLogics.getExternalScreen(screenID);
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, long computerId) throws RemoteException {
        return businessLogics.getExternalScreenParameters(screenID, computerId);
    }

    public void ping() throws RemoteException {
        //for filterIncl-alive
    }

    @Override
    public Integer getApiVersion() {
        return BaseUtils.getApiVersion();
    }

    @Override
    public String getPlatformVersion() {
        return BaseUtils.getPlatformVersion();
    }

    public GUIPreferences getGUIPreferences() throws RemoteException {
        byte[] logicsLogoBytes = null;
        try {
            if (logicsLogo != null && !logicsLogo.isEmpty())
                logicsLogoBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/" + logicsLogo));
        } catch (IOException e) {
            logger.error("Error reading logics logo: ", e);
        }
        return new GUIPreferences(name, displayName, null, logicsLogoBytes, Boolean.parseBoolean(clientHideMenu));
    }

    public void sendPingInfo(Long computerId, Map<Long, List<Long>> pingInfoMap) {
        Map<Long, List<Long>> pingInfoEntry = RemoteLoggerAspect.pingInfoMap.get(computerId);
        pingInfoEntry = pingInfoEntry != null ? pingInfoEntry : MapFact.<Long, List<Long>>getGlobalConcurrentHashMap();
        pingInfoEntry.putAll(pingInfoMap);
        RemoteLoggerAspect.pingInfoMap.put(computerId, pingInfoEntry);
    }

    @Override
    public List<String> authenticateUser(String userName, String password) throws RemoteException {
        User user;
        try(DataSession session = dbManager.createSession()) {
            user = securityManager.authenticateUser(session, userName, password, getStack());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        if (user != null) {
            return securityManager.getUserRolesNames(userName, getExtraUserRoleNames(userName));
        }
        return null;
    }

    @Override
    public VMOptions getClientVMOptions() throws RemoteException {
        return clientVMOptions;
    }

    @Override
    public long generateID() throws RemoteException {
        return dbManager.generateID();
    }

    protected List<String> getExtraUserRoleNames(String username) {
        return new ArrayList<>();
    }

    protected Long getUserByEmail(DataSession session, String email) throws SQLException, SQLHandledException {
        return (Long) businessLogics.authenticationLM.contactEmail.read(session, new DataObject(email));
    }

    @Override
    public void remindPassword(String email, String localeLanguage) throws RemoteException {
        assert email != null;
        //todo: в будущем нужно поменять на проставление локали в Context
//            ServerResourceBundle.load(localeLanguage);
        try {
            try (DataSession session = createSession()) {
                Long userId = getUserByEmail(session, email);
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

    public boolean isSingleInstance() throws RemoteException {
        return Settings.get().isSingleInstance();
    }

    @Override
    public List<Object> exec(String action, String[] returnCanonicalNames, Object[] params, String charsetName) {
        List<Object> returnList;
        try {
            LAP property = businessLogics.findActionByCompoundName(action);
            if (property != null) {
                returnList = executeExternal(property, returnCanonicalNames, params, Charset.forName(charsetName));
            } else {
                throw new RuntimeException(String.format("Action %s was not found", action));
            }
        } catch (ParseException | SQLHandledException | SQLException | IOException e) {
            throw new RuntimeException(e);
        }
        return returnList;
    }

    @Override
    public List<Object> eval(boolean action, Object paramScript, String[] returnCanonicalNames, Object[] params, String charsetName) {
        List<Object> returnList = new ArrayList<>();
        if (paramScript != null) {
            try {
                Charset charset = Charset.forName(charsetName);
                String script = StringClass.text.parseHTTP(paramScript, charset);
                if (action) {
                    //оборачиваем в run без параметров
                    script = "run() = {" + script + ";\n};";
                }
                LAP<?> runAction = businessLogics.evaluateRun(script);
                if (runAction != null)
                    returnList = executeExternal(runAction, returnCanonicalNames, params, charset);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Eval script was not found");
        }
        return returnList;
    }

    public List<Object> read(String property, Object[] params, Charset charset) {
        try {
            LCP lcp = businessLogics.findPropertyByCompoundName(property);
            if (lcp != null) {
                try (DataSession session = createSession()) {
                    return readReturnProperty(session, lcp, ExternalHTTPActionProperty.getParams(session, lcp, params, charset));
                }
            } else
                throw new RuntimeException(String.format("Property %s was not found", property));

        } catch (SQLException | SQLHandledException | ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Object> executeExternal(LAP property, String[] returnCanonicalNames, Object[] params, Charset charset) throws SQLException, ParseException, SQLHandledException, IOException {
        try (DataSession session = createSession()) {
            property.execute(session, getStack(), ExternalHTTPActionProperty.getParams(session, property, params, charset));

            return readReturnProperties(session, returnCanonicalNames);
        }
    }

    private List<Object> readReturnProperties(DataSession session, String[] returnCanonicalNames) throws SQLException, SQLHandledException, IOException {
        LCP[] returnProps; 
        if (returnCanonicalNames.length > 0) {
            returnProps = new LCP[returnCanonicalNames.length];
            for (int i = 0; i < returnCanonicalNames.length; i++) {
                String returnCanonicalName = returnCanonicalNames[i];
                LCP returnProperty = businessLogics.findProperty(returnCanonicalName);
                if (returnProperty == null)
                    throw new RuntimeException(String.format("Return property %s was not found", returnCanonicalName));
                returnProps[i] = returnProperty;
            }
        } else {
            returnProps = new LCP[] {businessLogics.LM.exportFile};
        }

        List<Object> returnList = new ArrayList<>();
        for (LCP returnProperty : returnProps)
            returnList.addAll(readReturnProperty(session, returnProperty));
        return returnList;
    }

    private List<Object> readReturnProperty(DataSession session, LCP<?> returnProperty, ObjectValue... params) throws SQLException, SQLHandledException, IOException {
        Object returnValue = returnProperty.read(session, params);
        Type returnType = returnProperty.property.getType();
        return readReturnProperty(returnValue, returnType);
    }

    private List<Object> readReturnProperty(Object returnValue, Type returnType) throws IOException {
        List<Object> returnList = new ArrayList<>();
        boolean jdbcSingleRow = false;
        if (returnType instanceof DynamicFormatFileClass && returnValue != null) {
            if (((FileData) returnValue).getExtension().equals("jdbc")) {
                JDBCTable jdbcTable = JDBCTable.deserializeJDBC(((FileData) returnValue).getRawFile());
                if (jdbcTable.singleRow) {
                    ImMap<String, Object> row = jdbcTable.set.isEmpty() ? null : jdbcTable.set.get(0);
                    for (String field : jdbcTable.fields) {
                        Type fieldType = jdbcTable.fieldTypes.get(field);
                        if(row == null)
                            returnList.add(null);
                        else
                            returnList.addAll(readReturnProperty(row.get(field), fieldType));
                    }
                    jdbcSingleRow = true;
                }
            }
        }
        if (!jdbcSingleRow)
            returnList.add(returnType.formatHTTP(returnValue, null));
        return returnList;
    }

    @Override
    public String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException {
        return securityManager.addUser(username, email, password, firstName, lastName, localeLanguage, getStack());
    }

    @Override
    public Map<String, String> readMemoryLimits() {
        Map<String, String> memoryLimitMap = new HashMap<>();
        try (DataSession session = createSession()) {
            KeyExpr memoryLimitExpr = new KeyExpr("memoryLimit");
            ImRevMap<Object, KeyExpr> memoryLimitKeys = MapFact.singletonRev((Object) "memoryLimit", memoryLimitExpr);
            QueryBuilder<Object, Object> query = new QueryBuilder<>(memoryLimitKeys);

            String[] names = new String[]{"name", "maxHeapSize", "vmargs"};
            LCP[] properties = businessLogics.securityLM.findProperties("name[MemoryLimit]", "maxHeapSize[MemoryLimit]",
                    "vmargs[MemoryLimit]");
            for (int j = 0; j < properties.length; j++) {
                query.addProperty(names[j], properties[j].getExpr(memoryLimitExpr));
            }
            query.and(businessLogics.securityLM.findProperty("name[MemoryLimit]").getExpr(memoryLimitExpr).getWhere());

            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session);
            for (ImMap<Object, Object> entry : result.values()) {
                String name = (String) entry.get("name");
                String line = "";
                String maxHeapSize = (String) entry.get("maxHeapSize");
                if(maxHeapSize != null)
                    line += "maxHeapSize=" + maxHeapSize;
                String vmargs = (String) entry.get("vmargs");
                if(vmargs != null)
                    line += (line.isEmpty() ? "" : "&") + "vmargs=" + URLEncoder.encode(vmargs, "utf-8");
                memoryLimitMap.put(name, line);
            }
        } catch (ScriptingErrorLog.SemanticErrorException | SQLException | SQLHandledException | UnsupportedEncodingException e) {
            logger.error("Error reading MemoryLimit: ", e);
        }
        return memoryLimitMap;
    }

    public Long getCurrentUser() {
        return dbManager.getSystemUserObject();
    }

    public Long getCurrentComputer() {
        return (Long) dbManager.getServerComputerObject(getStack()).getValue();
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

    @Override
    public List<ReportPath> saveAndGetCustomReportPathList(String formSID, boolean recreate) throws RemoteException {
        try {
            return FormInstance.saveAndGetCustomReportPathList(businessLogics.findForm(formSID), recreate);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Object getProfiledObject() {
        return "l";
    }
}

