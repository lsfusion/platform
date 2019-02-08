package lsfusion.server.remote;

import com.google.common.base.Throwables;
import lsfusion.base.ApiResourceBundle;
import lsfusion.base.BaseUtils;
import lsfusion.base.ExecResult;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.GUIPreferences;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.VMOptions;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.exceptions.RemoteMessageException;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.PreAuthentication;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.classes.StringClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.lifecycle.LifecycleListener;
import lsfusion.server.logics.SecurityManager;
import lsfusion.server.logics.*;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ActionProperty;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public RemoteNavigatorInterface createNavigator(NavigatorInfo navigatorInfo, boolean reuseSession) {
        if (restartManager.isPendingRestart() && (navigatorInfo.login == null || !navigatorInfo.login.equals("admin")))
            throw new RemoteMessageException(ApiResourceBundle.getString("exceptions.server.is.restarting"));

        return navigatorsManager.createNavigator(getStack(), navigatorInfo, reuseSession);
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

    public void sendPingInfo(String computerName, Map<Long, List<Long>> pingInfoMap) {
        Map<Long, List<Long>> pingInfoEntry = RemoteLoggerAspect.pingInfoMap.get(computerName);
        pingInfoEntry = pingInfoEntry != null ? pingInfoEntry : MapFact.<Long, List<Long>>getGlobalConcurrentHashMap();
        pingInfoEntry.putAll(pingInfoMap);
        RemoteLoggerAspect.pingInfoMap.put(computerName, pingInfoEntry);
    }

    // web spring authentication
    @Override
    public PreAuthentication preAuthenticateUser(String userName, String password, String language, String country) throws RemoteException {
        return securityManager.preAuthenticateUser(userName, password, language, country, getStack());
    }

    @Override
    public VMOptions getClientVMOptions() throws RemoteException {
        return clientVMOptions;
    }

    @Override
    public long generateID() throws RemoteException {
        return dbManager.generateID();
    }

    public boolean isSingleInstance() throws RemoteException {
        return Settings.get().isSingleInstance();
    }

    @Override
    public ExecResult exec(String action, String[] returnNames, Object[] params, String charsetName, String[] headerNames, String[] headerValues) {
        ExecResult result;
        try {
            LAP property = businessLogics.findActionByCompoundName(action);
            if (property != null) {
                result = executeExternal(property, returnNames, params, Charset.forName(charsetName), headerNames, headerValues);
            } else {
                throw new RuntimeException(String.format("Action %s was not found", action));
            }
        } catch (ParseException | SQLHandledException | SQLException | IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public ExecResult eval(boolean action, Object paramScript, String[] returnNames, Object[] params, String charsetName, String[] headerNames, String[] headerValues) {
        ExecResult result;
        if (paramScript != null) {
            try {
                Charset charset = Charset.forName(charsetName);
                String script = StringClass.text.parseHTTP(paramScript, charset);
                if (action) {
                    //оборачиваем в run без параметров
                    script = "run() = {" + script + ";\n};";
                }
                LAP<?> runAction = businessLogics.evaluateRun(script);
                if(runAction != null) {
                    result = executeExternal(runAction, returnNames, params, charset, headerNames, headerValues);
                } else {
                    throw new RuntimeException("Action run[] was not found");
                    
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Eval script was not found");
        }
        return result;
    }

    // system actions that are needed for native clients
    public boolean isClientNativeRESTAction(ActionProperty action) {
        return businessLogics.authenticationLM.syncUsers.property == action;
    }

    private ExecResult executeExternal(LAP<?> property, String[] returnNames, Object[] params, Charset charset, String[] headerNames, String[] headerValues) throws SQLException, ParseException, SQLHandledException, IOException {
        if(!isClientNativeRESTAction(property.property) && !Settings.get().isEnableRESTApi())
            throw new RuntimeException("REST Api is disabled. It can be enabled using setting enableRESTApi.");
        try (DataSession session = dbManager.createSession()) {
            if(property.property.uses(baseLM.headers.property)) // optimization
                ExternalHTTPActionProperty.writeHeaders(session, baseLM.headers, headerNames, headerValues);
            
            property.execute(session, getStack(), ExternalHTTPActionProperty.getParams(session, property, params, charset));

            return readResult(session, returnNames);
        }
    }

    private ExecResult readResult(DataSession session, String[] returnNames) throws SQLException, SQLHandledException, IOException {
        LCP[] returnProps; 
        if (returnNames.length > 0) {
            returnProps = new LCP[returnNames.length];
            for (int i = 0; i < returnNames.length; i++) {
                String returnName = returnNames[i];
                LCP returnProperty = businessLogics.findPropertyByCompoundName(returnName);
                if (returnProperty == null)
                    throw new RuntimeException(String.format("Return property %s was not found", returnName));
                returnProps[i] = returnProperty;
            }
        } else {
            returnProps = new LCP[] {businessLogics.LM.exportFile};
        }

        List<Object> returns = new ArrayList<>();
        for (LCP returnProp : returnProps) 
            returns.addAll(readReturnProperty(session, returnProp));

        ImOrderMap<String, String> headers = ExternalHTTPActionProperty.readHeaders(session, baseLM.headersTo).toOrderMap();
        return new ExecResult(returns.toArray(), headers.keyOrderSet().toArray(new String[headers.size()]), headers.valuesList().toArray(new String[headers.size()]));
    }

    private List<Object> readReturnProperty(DataSession session, LCP<?> returnProperty, ObjectValue... params) throws SQLException, SQLHandledException, IOException {
        Object returnValue = returnProperty.read(session, params);
        Type returnType = returnProperty.property.getType();
        return readReturnProperty(returnValue, returnType);
    }

    private List<Object> readReturnProperty(Object returnValue, Type returnType) throws IOException {
        List<Object> returnList = new ArrayList<>();
//        boolean jdbcSingleRow = false;
//        if (returnType instanceof DynamicFormatFileClass && returnValue != null) {
//            if (((FileData) returnValue).getExtension().equals("jdbc")) {
//                JDBCTable jdbcTable = JDBCTable.deserializeJDBC(((FileData) returnValue).getRawFile());
//                if (jdbcTable.singleRow) {
//                    ImMap<String, Object> row = jdbcTable.set.isEmpty() ? null : jdbcTable.set.get(0);
//                    for (String field : jdbcTable.fields) {
//                        Type fieldType = jdbcTable.fieldTypes.get(field);
//                        if(row == null)
//                            returnList.add(null);
//                        else
//                            returnList.addAll(readReturnProperty(row.get(field), fieldType));
//                    }
//                    jdbcSingleRow = true;
//                }
//            }
//        }
//        if (!jdbcSingleRow)
            returnList.add(returnType.formatHTTP(returnValue, null));
        return returnList;
    }

    @Override
    public String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException {
        return securityManager.addUser(username, email, password, firstName, lastName, localeLanguage, getStack());
    }

    @Deprecated // change to http request with the common mechanism
    @Override
    public Map<String, String> readMemoryLimits() {
        Map<String, String> memoryLimitMap = new HashMap<>();
        try (DataSession session = dbManager.createSession()) {
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

