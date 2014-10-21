package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.HSet;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.lru.LRULogger;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSASVSMap;
import lsfusion.interop.Compare;
import lsfusion.interop.event.IDaemonTask;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenParameters;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.classes.sets.ResolveOrObjectClassSet;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.daemons.ScannerDaemonTask;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.MapCacheAspect;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.LogFormEntity;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.form.window.AbstractWindow;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.FormActionProperty;
import lsfusion.server.logics.property.actions.SessionEnvEvent;
import lsfusion.server.logics.property.actions.SystemEvent;
import lsfusion.server.logics.property.cases.AbstractCase;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.scripted.MetaCodeFragment;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.logics.tasks.PublicTask;
import lsfusion.server.logics.tasks.TaskRunner;
import lsfusion.server.mail.NotificationActionProperty;
import lsfusion.server.session.ApplyFilter;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.SessionCreator;
import lsfusion.server.session.SingleKeyTableUsage;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.isRedundantString;
import static lsfusion.base.BaseUtils.systemLogger;
import static lsfusion.server.logics.LogicsModule.*;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public abstract class BusinessLogics<T extends BusinessLogics<T>> extends LifecycleAdapter implements InitializingBean {
    protected final static Logger logger = ServerLoggers.systemLogger;
    protected final static Logger debuglogger = Logger.getLogger(BusinessLogics.class);
    protected final static Logger lruLogger = ServerLoggers.lruLogger;

    public static final List<String> defaultExcludedScriptPaths = Arrays.asList("lsfusion/system");
    public static final List<String> defaultIncludedScriptPaths = Arrays.asList("");

    private List<LogicsModule> logicModules = new ArrayList<LogicsModule>();
    private Map<String, List<LogicsModule>> namespaceToModules = new HashMap<String, List<LogicsModule>>();

    private final Map<String, LogicsModule> nameToModule = new HashMap<String, LogicsModule>();

    private final List<ExternalScreen> externalScreens = new ArrayList<ExternalScreen>();

    public BaseLogicsModule<T> LM;
    public ServiceLogicsModule serviceLM;
    public ReflectionLogicsModule reflectionLM;
    public ContactLogicsModule contactLM;
    public AuthenticationLogicsModule authenticationLM;
    public SecurityLogicsModule securityLM;
    public SystemEventsLogicsModule systemEventsLM;
    public EmailLogicsModule emailLM;
    public SchedulerLogicsModule schedulerLM;
    public TimeLogicsModule timeLM;
    public ScriptingLogicsModule evalScriptLM;

    protected LogicsInstance logicsInstance;
    
    private String topModule;

    private String orderDependencies;

    private PublicTask initTask;

    //чтобы можно было использовать один инстанс логики с несколькими инстансами, при этом инициализировать только один раз
    private final AtomicBoolean initialized = new AtomicBoolean();

    public BusinessLogics() {
        super(LOGICS_ORDER);
    }

    public void setTopModule(String topModule) {
        this.topModule = topModule;
    }

    public void setOrderDependencies(String orderDependencies) {
        this.orderDependencies = orderDependencies;
    }

    public void setInitTask(PublicTask initTask) {
        this.initTask = initTask;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(initTask, "initTask must be specified");
        
        LRUUtil.initLRUTuner(new LRULogger() {
            @Override
            public void log(String log) {
                lruLogger.info(log);
            }
        });
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        if (initialized.compareAndSet(false, true)) {
            logger.info("Initializing BusinessLogics");
            try {
                getDbManager().ensureLogLevel();
                
                TaskRunner.runTask(initTask, logger);
            } catch (ScriptParsingException e) {
                throw e;
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException("Error initializing BusinessLogics: ", e);
            }
        }
    }

    public LRUWSASVSMap<Object, Method, Object, Object> startLruCache = new LRUWSASVSMap<Object, Method, Object, Object>(LRUUtil.G2);
    public void cleanCaches() {
        startLruCache = null;
        MapCacheAspect.cleanClassCaches();

        logger.info("Obsolete caches were successfully cleaned");
    }
    
    public ScriptingLogicsModule getModule(String name) {
        return (ScriptingLogicsModule) getSysModule(name);
    }
    
    public LogicsModule getSysModule(String name) {
        return nameToModule.get(name);
    }

    public ConcreteClass getDataClass(Object object, Type type) {
        try {
            DataSession session = getDbManager().createSession();
            ConcreteClass result = type.getDataClass(object, session.sql, LM.baseClass.getUpSet(), LM.baseClass, OperationOwner.unknown);
            session.close();

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<IDaemonTask> getDaemonTasks(int compId) {
        ArrayList<IDaemonTask> daemons = new ArrayList<IDaemonTask>();

        Integer scannerComPort;
        Boolean scannerSingleRead;
        try {
            DataSession session = getDbManager().createSession();
            scannerComPort = (Integer) authenticationLM.scannerComPortComputer.read(session, new DataObject(compId, authenticationLM.computer));
            scannerSingleRead = (Boolean) authenticationLM.scannerSingleReadComputer.read(session, new DataObject(compId, authenticationLM.computer));
            session.close();
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
        if (scannerComPort != null) {
            IDaemonTask task = new ScannerDaemonTask(scannerComPort, ((Boolean)true).equals(scannerSingleRead));
            daemons.add(task);
        }
        return daemons;
    }

    protected void addExternalScreen(ExternalScreen screen) {
        externalScreens.add(screen);
    }

    public ExternalScreen getExternalScreen(int screenID) {
        for (ExternalScreen screen : externalScreens) {
            if (screen.getID() == screenID) {
                return screen;
            }
        }
        return null;
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        return null;
    }

    protected <T extends LogicsModule> T addModule(T module) {
        logicModules.add(module);
        return module;
    }

    public void createModules() throws IOException {
        LM = addModule(new BaseLogicsModule(this, new DefaultDBNamePolicy(63)));
        serviceLM = addModule(new ServiceLogicsModule(this, LM));
        reflectionLM = addModule(new ReflectionLogicsModule(this, LM));
        contactLM = addModule(new ContactLogicsModule(this, LM));
        authenticationLM = addModule(new AuthenticationLogicsModule(this, LM));
        securityLM = addModule(new SecurityLogicsModule(this, LM));
        systemEventsLM = addModule(new SystemEventsLogicsModule(this, LM));
        emailLM = addModule(new EmailLogicsModule(this, LM));
        schedulerLM = addModule(new SchedulerLogicsModule(this, LM));
        timeLM = addModule(new TimeLogicsModule(this, LM));
        evalScriptLM = addModule(new ScriptingLogicsModule(
               SchedulerLogicsModule.class.getResourceAsStream("/lsfusion/system/EvalScript.lsf"), 
               "/lsfusion/system/EvalScript.lsf", LM, this));
    }

    protected void addModulesFromResource(List<String> paths, List<String> excludedPaths) throws IOException {
        if (excludedPaths == null || excludedPaths.isEmpty()) {
            excludedPaths = defaultExcludedScriptPaths;
        } else {
            excludedPaths = new ArrayList<String>(excludedPaths);
            excludedPaths.addAll(defaultExcludedScriptPaths);
        }

        List<String> excludedLSF = new ArrayList<String>();

        for (String filePath : excludedPaths) {
            if (filePath.contains("*")) {
                filePath += filePath.endsWith(".lsf") ? "" : ".lsf";
                Pattern pattern = Pattern.compile(filePath.replace("*", ".*"));
                Collection<String> list = ResourceList.getResources(pattern);
                for (String name : list) {
                    excludedLSF.add(name);
                }
            } else if (filePath.endsWith(".lsf")) {
                excludedLSF.add(filePath);
            } else {
                Pattern pattern = Pattern.compile(filePath + ".*\\.lsf");
                Collection<String> list = ResourceList.getResources(pattern);
                for (String name : list) {
                    excludedLSF.add(name);
                }
            }
        }

        for (String filePath : paths) {
            if (filePath.contains("*")) {
                filePath += filePath.endsWith(".lsf") ? "" : ".lsf";
                Pattern pattern = Pattern.compile(filePath.replace("*", ".*"));
                Collection<String> list = ResourceList.getResources(pattern);
                for (String name : list) {
                    if (!excludedLSF.contains(name)) {
                        addModulesFromResource(name);
                    }
                }
            } else if (filePath.endsWith(".lsf")) {
                if (!excludedLSF.contains(filePath)) {
                    addModulesFromResource(filePath);
                }
            } else {
                Pattern pattern = Pattern.compile(filePath + ".*\\.lsf");
                Collection<String> list = ResourceList.getResources(pattern);
                for (String name : list) {
                    if (!excludedLSF.contains(name)) {
                        addModulesFromResource(name);
                    }
                }
            }
        }
    }

    protected void addModulesFromResource(String... paths) throws IOException {
        for (String path : paths) {
            addModuleFromResource(path);
        }
    }

    protected ScriptingLogicsModule addModuleFromResource(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream("/" + path);
        if (is == null)
            throw new RuntimeException(String.format("[error]:\tmodule '%s' cannot be found", path));
        return addModule(new ScriptingLogicsModule(is, path, LM, this));
    }

    private void fillNameToModules() {
        for (LogicsModule module : logicModules) {
            if (nameToModule.containsKey(module.getName())) {
                throw new RuntimeException(String.format("[error]:\tmodule '%s' has already been added", module.getName()));
            }
            nameToModule.put(module.getName(), module);
        }
    }

    public Map<String, Set<String>> buildModuleGraph() {
        Map<String, Set<String>> graph = new HashMap<String, Set<String>>();
        for (LogicsModule module : logicModules) {
            graph.put(module.getName(), new HashSet<String>());
        }

        for (LogicsModule module : logicModules) {
            for (String reqModule : module.getRequiredModules()) {
                if (graph.get(reqModule) == null) {
                    throw new RuntimeException(String.format("[error]:\t%s:\trequired module '%s' was not found", module.getName(), reqModule));
                }
                graph.get(reqModule).add(module.getName());
            }
        }
        return graph;
    }

    private String checkCycles(String cur, LinkedList<String> way, Set<String> used, Map<String, Set<String>> graph) {
        way.add(cur);
        used.add(cur);
        for (String next : graph.get(cur)) {
            if (!used.contains(next)) {
                String foundCycle = checkCycles(next, way, used, graph);
                if (foundCycle != null) {
                    return foundCycle;
                }
            } else if (way.contains(next)) {
                String foundCycle = next;
                do {
                    foundCycle = foundCycle + " <- " + way.peekLast();
                } while (!way.pollLast().equals(next));
                return foundCycle;
            }
        }

        way.removeLast();
        return null;
    }

    private void checkCycles(Map<String, Set<String>> graph, String errorMessage) {
        Set<String> used = new HashSet<String>();
        for (String vertex : graph.keySet()) {
            if (!used.contains(vertex)) {
                String foundCycle = checkCycles(vertex, new LinkedList<String>(), used, graph);
                if (foundCycle != null) {
                    throw new RuntimeException("[error]:\t" + errorMessage + ": " + foundCycle);
                }
            }
        }
    }

    // Формирует .dot файл для построения графа иерархии модулей с помощью graphviz
    private void outDotFile() {
        try {
            FileWriter fstream = new FileWriter("D:/lsf/modules.dot");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("digraph Modules {\n");
            out.write("\tsize=\"6,4\"; ratio = fill;\n");
            out.write("\tnode [shape=box, fontsize=60, style=filled];\n");
            out.write("\tedge [arrowsize=2];\n");
            for (LogicsModule module : logicModules) {
                for (String name : module.getRequiredModules()) {
                    out.write("\t" + name + " -> " + module.getName() + ";\n");
                }
            }
            out.write("}\n");
            out.close();
        } catch (Exception e) {
        }
    }

    private List<LogicsModule> orderModules(String orderDependencies, Map<LogicsModule, ImSet<LogicsModule>> recRequiredModules) {
//        outDotFile();

        //для обеспечения детерменированности сортируем модули по имени
        Collections.sort(logicModules, new Comparator<LogicsModule>() {
            @Override
            public int compare(LogicsModule m1, LogicsModule m2) {
                return m1.getName().compareTo(m2.getName());
            }
        });

        fillNameToModules();

        Map<String, Set<String>> graph = buildModuleGraph();

        checkCycles(graph, "there is a circular dependency between requred modules");

        if (!isRedundantString(orderDependencies)) {
            addOrderDependencies(orderDependencies, graph);
            checkCycles(graph, "there is a circular dependency introduced by order dependencies");
        }

        Map<String, Integer> degree = new HashMap<String, Integer>();
        for (LogicsModule module : logicModules) {
            degree.put(module.getName(), 0);
        }

        for (Map.Entry<String, Set<String>> e : graph.entrySet()) {
            for (String nextModule : e.getValue()) {
                degree.put(nextModule, degree.get(nextModule) + 1);
            }
        }

        Set<LogicsModule> usedModules = new LinkedHashSet<LogicsModule>();
        for (int i = 0; i < logicModules.size(); ++i) {
            for (LogicsModule module : logicModules) {
                String moduleName = module.getName();
                if (degree.get(moduleName) == 0 && !usedModules.contains(module)) {
                    for (String nextModule : graph.get(moduleName)) {
                        degree.put(nextModule, degree.get(nextModule) - 1);
                    }
                    usedModules.add(module);
                    
                    MSet<LogicsModule> mRecDep = SetFact.mSet();
                    mRecDep.add(module);
                    for(String depend : module.getRequiredModules())
                        mRecDep.addAll(recRequiredModules.get(nameToModule.get(depend)));
                    recRequiredModules.put(module, mRecDep.immutable());    
                    
                    break;
                }
            }
        }
        return new ArrayList<LogicsModule>(usedModules);
    }

    private void addOrderDependencies(String orderDependencies, Map<String, Set<String>> graph) {
        for (String dependencyList : orderDependencies.split(";\\s*")) {
            String dependencies[] = dependencyList.split(",\\s*");
            for (int i = 0; i < dependencies.length; ++i) {
                String moduleName2 = dependencies[i];
                if (graph.get(moduleName2) == null) {
                    throw new RuntimeException(String.format("[error]:\torder dependencies' module '%s' was not found", moduleName2));
                }

                if (i > 0) {
                    String moduleName1 = dependencies[i - 1];
                    graph.get(moduleName1).add(moduleName2);
                }
            }
        }
    }

    private void overrideModulesList(String startModuleName) {

        Set<LogicsModule> was = new HashSet<LogicsModule>();
        Queue<LogicsModule> queue = new LinkedList<LogicsModule>();

        fillNameToModules();

        LogicsModule startModule = nameToModule.get(startModuleName);
        if(startModule == null)
            logger.error(String.format("Module %s not found.", startModuleName));
        assert startModule != null;
        queue.add(startModule);
        was.add(startModule);

        while (!queue.isEmpty()) {
            LogicsModule current = queue.poll();

            for (String nextModuleName : current.getRequiredModules()) {
                LogicsModule nextModule = nameToModule.get(nextModuleName);
                if (!was.contains(nextModule)) {
                    was.add(nextModule);
                    queue.add(nextModule);
                }
            }
        }

        logicModules = new ArrayList<LogicsModule>(was);
        nameToModule.clear();
    }

    public void initObjectClass() {
        LM.baseClass.initObjectClass(LM.getVersion(), LM.transformNameToSID("CustomObjectClass"));
        LM.storeCustomClass(LM.baseClass.objectClass);
    }

    private List<LogicsModule> orderedModules;
    public List<LogicsModule> getOrderedModules() {
        return orderedModules;
    }

    public void initModuleOrders() {
        if (!isRedundantString(topModule)) {
            overrideModulesList(topModule);
        }

        Map<LogicsModule, ImSet<LogicsModule>> recRequiredModules = new HashMap<LogicsModule, ImSet<LogicsModule>>();
        orderedModules = orderModules(orderDependencies, recRequiredModules);

        int moduleNumber = 0;
        for (LogicsModule module : orderedModules) {
            String namespace = module.getNamespace();
            if (!namespaceToModules.containsKey(namespace)) {
                namespaceToModules.put(namespace, new ArrayList<LogicsModule>());
            }
            namespaceToModules.get(namespace).add(module);
            
            module.visible = recRequiredModules.get(module).mapSetValues(new GetValue<Version, LogicsModule>() {
                public Version getMapValue(LogicsModule value) {
                    return value.getVersion();
                }});
            module.order = (moduleNumber++);
        }
    }

    public void initClassDataProps() {
        ImMap<ImplementTable, ImSet<ConcreteCustomClass>> groupTables = getConcreteCustomClasses().group(new BaseUtils.Group<ImplementTable, ConcreteCustomClass>() {
            public ImplementTable group(ConcreteCustomClass customClass) {
                return LM.tableFactory.getMapTable(MapFact.singleton("key", (ValueClass) customClass)).table;
            }
        });

        for(int i=0,size=groupTables.size();i<size;i++) {
            ImplementTable table = groupTables.getKey(i);
            ImSet<ConcreteCustomClass> set = groupTables.getValue(i);

            ObjectValueClassSet classSet = OrObjectClassSet.fromSetConcreteChildren(set);
            ClassDataProperty dataProperty = new ClassDataProperty(classSet.toString(), classSet);
            LCP<ClassPropertyInterface> lp = new LCP<ClassPropertyInterface>(dataProperty);
            LM.addProperty(null, new LCP<ClassPropertyInterface>(dataProperty));
            LM.makePropertyPublic(lp, PropertyCanonicalNameUtils.classDataPropPrefix + table.getName(), Collections.<ResolveClassSet>singletonList(ResolveOrObjectClassSet.fromSetConcreteChildren(set)));
            dataProperty.markStored(LM.tableFactory, table);

            for(ConcreteCustomClass customClass : set)
                customClass.dataProperty = dataProperty;
        }
    }

    public void initReflectionEvents() {

        //временное решение
        
        try {
            SQLSession sql = getDbManager().getThreadLocalSql();            
            
            systemLogger.info("Setting user logging for properties");
            setUserLoggableProperties(sql);

            systemLogger.info("Setting user not null constraints for properties");
            setNotNullProperties(sql);
            
            systemLogger.info("Setting user notifications for property changes");
            setupPropertyNotifications(sql);
            
        } catch (Exception ignored) {
        }

    }

    private void setUserLoggableProperties(SQLSession sql) throws SQLException, IllegalAccessException, InstantiationException, ClassNotFoundException, SQLHandledException {
        
        LCP<PropertyInterface> isProperty = LM.is(reflectionLM.property);
        ImRevMap<PropertyInterface, KeyExpr> keys = isProperty.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
        query.addProperty("CNProperty", reflectionLM.canonicalNameProperty.getExpr(key));
        query.and(reflectionLM.userLoggableProperty.getExpr(key).getWhere());
        ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = query.execute(sql, OperationOwner.unknown);

        for (ImMap<Object, Object> values : result.valueIt()) {
            LM.makeUserLoggable(systemEventsLM, (LCP) findProperty(values.get("CNProperty").toString().trim()));
        }
    }

    private void setNotNullProperties(SQLSession sql) throws SQLException, IllegalAccessException, InstantiationException, ClassNotFoundException, SQLHandledException {
        
        LCP isProperty = LM.is(reflectionLM.property);
        ImRevMap<Object, KeyExpr> keys = isProperty.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);
        query.addProperty("CNProperty", reflectionLM.canonicalNameProperty.getExpr(key));
        query.and(reflectionLM.isSetNotNullProperty.getExpr(key).getWhere());
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(sql, OperationOwner.unknown);

        for (ImMap<Object, Object> values : result.valueIt()) {
            LCP<?> prop = (LCP) findProperty(values.get("CNProperty").toString().trim());
            prop.property.reflectionNotNull = true;
            LM.setNotNull(prop, ListFact.<PropertyFollowsDebug>EMPTY());
        }
    }

    private void setupPropertyNotifications(SQLSession sql) throws SQLException, IllegalAccessException, InstantiationException, ClassNotFoundException, SQLHandledException {

        LCP isNotification = LM.is(emailLM.notification);
        ImRevMap<Object, KeyExpr> keys = isNotification.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);
        query.addProperty("isDerivedChange", emailLM.isEventNotification.getExpr(key));
        query.addProperty("subject", emailLM.subjectNotification.getExpr(key));
        query.addProperty("text", emailLM.textNotification.getExpr(key));
        query.addProperty("emailFrom", emailLM.emailFromNotification.getExpr(key));
        query.addProperty("emailTo", emailLM.emailToNotification.getExpr(key));
        query.addProperty("emailToCC", emailLM.emailToCCNotification.getExpr(key));
        query.addProperty("emailToBC", emailLM.emailToBCNotification.getExpr(key));
        query.and(isNotification.getExpr(key).getWhere());
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(sql, OperationOwner.unknown);

        for (int i=0,size=result.size();i<size;i++) {
            DataObject notificationObject = new DataObject(result.getKey(i).getValue(0), emailLM.notification);
            KeyExpr propertyExpr2 = new KeyExpr("property");
            KeyExpr notificationExpr2 = new KeyExpr("notification");
            ImRevMap<String, KeyExpr> newKeys2 = MapFact.toRevMap("property", propertyExpr2, "notification", notificationExpr2);

            QueryBuilder<String, String> query2 = new QueryBuilder<String, String>(newKeys2);
            query2.addProperty("CNProperty", reflectionLM.canonicalNameProperty.getExpr(propertyExpr2));
            query2.and(emailLM.inNotificationProperty.getExpr(notificationExpr2, propertyExpr2).getWhere());
            query2.and(notificationExpr2.compare(notificationObject, Compare.EQUALS));
            ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> result2 = query2.execute(sql, OperationOwner.unknown);
            List<LCP> listInNotificationProperty = new ArrayList();
            for (int j=0,size2=result2.size();j<size2;j++) {
                listInNotificationProperty.add((LCP) findProperty(result2.getValue(i).get("CNProperty").toString().trim()));
            }
            ImMap<Object, Object> rowValue = result.getValue(i);

            for (LCP prop : listInNotificationProperty) {
                boolean isDerivedChange = rowValue.get("isDerivedChange") == null ? false : true;
                String subject = rowValue.get("subject") == null ? "" : rowValue.get("subject").toString().trim();
                String text = rowValue.get("text") == null ? "" : rowValue.get("text").toString().trim();
                String emailFrom = rowValue.get("emailFrom") == null ? "" : rowValue.get("emailFrom").toString().trim();
                String emailTo = rowValue.get("emailTo") == null ? "" : rowValue.get("emailTo").toString().trim();
                String emailToCC = rowValue.get("emailToCC") == null ? "" : rowValue.get("emailToCC").toString().trim();
                String emailToBC = rowValue.get("emailToBC") == null ? "" : rowValue.get("emailToBC").toString().trim();
                LAP emailNotificationProperty = LM.addProperty(LM.actionGroup, new LAP(new NotificationActionProperty("emailNotificationProperty", prop, subject, text, emailFrom, emailTo, emailToCC, emailToBC, emailLM)));

                Integer[] params = new Integer[prop.listInterfaces.size()];
                for (int j = 0; j < prop.listInterfaces.size(); j++)
                    params[j] = j + 1;
                if (isDerivedChange)
                    emailNotificationProperty.setEventAction(LM, prop, params);
                else
                    emailNotificationProperty.setEventSetAction(LM, prop, params);
            }
        }
    }
    
    private void finishLogInit() {
        // с одной стороны нужно отрисовать на форме логирования все свойства из recognizeGroup, с другой - LogFormEntity с Action'ом должен уже существовать
        // поэтому makeLoggable делаем сразу, а LogFormEntity при желании заполняем здесь
        for (Property property : getOrderProperties()) {
            finishLogInit(property);
        }
    }

    public void finishLogInit(Property property) {
        if (property.loggable && property.logFormProperty.property instanceof FormActionProperty) {
            FormActionProperty formActionProperty = (FormActionProperty) property.logFormProperty.property;
            if (formActionProperty.form instanceof LogFormEntity) {
                LogFormEntity logForm = (LogFormEntity) formActionProperty.form;
                if (logForm.lazyInit) {
                    logForm.initProperties();
                }
            }

            //добавляем в контекстное меню пункт для показа формы
            property.setContextMenuAction(property.getSID(), formActionProperty.caption);
            property.setEditAction(property.getSID(), formActionProperty.getImplement(property.getOrderInterfaces()));
        }
    }

    public void setupPropertyPolicyForms(LAP<?> setupPolicyForPropByCN, Property property) {
        if (property.isNamed()) {
            String propertyCN = property.getCanonicalName();
            PropertyCanonicalNameParser parser = new PropertyCanonicalNameParser(this, propertyCN);
            String setupPolicyActionName = PropertyCanonicalNameUtils.policyPropPrefix + property.getName(); 
            LAP<?> setupPolicyLAP = LM.addJoinAProp(LM.propertyPolicyGroup, getString("logics.property.propertypolicy.action"),
                    setupPolicyForPropByCN, LM.addCProp(StringClass.get(propertyCN.length()), propertyCN));
            
            ActionProperty setupPolicyAction = setupPolicyLAP.property;
            try {
                LM.makePropertyPublic(setupPolicyLAP, setupPolicyActionName, parser.getSignature());
            } catch (PropertyCanonicalNameParser.ParseException e) {
                Throwables.propagate(e);
            }
            setupPolicyAction.checkReadOnly = false;
            property.setContextMenuAction(setupPolicyAction.getSID(), setupPolicyAction.caption);
            property.setEditAction(setupPolicyAction.getSID(), setupPolicyAction.getImplement());
        }
    }

    public void prereadCaches() {
        getAppliedProperties(ApplyFilter.ONLYCHECK);
        getAppliedProperties(ApplyFilter.NO);
        getOrderMapSingleApplyDepends(ApplyFilter.NO);
    }

    protected void initAuthentication(SecurityManager securityManager) throws SQLException, SQLHandledException {
        securityManager.setupDefaultAdminUser();
    }

    public ImOrderSet<Property> getOrderProperties() {
        return LM.rootGroup.getProperties();
    }

    public ImSet<Property> getProperties() {
        return getOrderProperties().getSet();
    }

    public List<LP<?, ?>> getNamedProperties() {
        List<LP<?, ?>> namedProperties = new ArrayList<LP<?, ?>>();
        for (LogicsModule module : logicModules) {
            namedProperties.addAll(module.getNamedProperties());
        }
        return namedProperties;
    }
    
    private static class NamedDecl {
        public final LP prop;
        public final String namespace;
        public final boolean defaultNamespace;
        public final List<ResolveClassSet> signature;
        public final Version version;

        public NamedDecl(LP prop, String namespace, boolean defaultNamespace, List<ResolveClassSet> signature, Version version) {
            this.prop = prop;
            this.namespace = namespace;
            this.defaultNamespace = defaultNamespace;
            this.signature = signature;
            this.version = version;
        }
    }

    public Map<String, List<NamedDecl>> getNamedModuleProperties() {
        Map<String, List<NamedDecl>> result = new HashMap<String, List<NamedDecl>>();
        for (Map.Entry<String, List<LogicsModule>> namespaceToModule : namespaceToModules.entrySet()) {
            String namespace = namespaceToModule.getKey();
            for(LogicsModule module : namespaceToModule.getValue()) {
                for(Map.Entry<String, List<LP<?, ?>>> namedModuleProperty : module.getNamedModuleProperties().entrySet()) {
                    String propertyName = namedModuleProperty.getKey();
                    List<NamedDecl> resultProps = result.get(propertyName);
                    if(resultProps == null) {
                        resultProps = new ArrayList<NamedDecl>();
                        result.put(propertyName, resultProps);
                    }
                    for(LP prop : namedModuleProperty.getValue()) {
                        resultProps.add(new NamedDecl(prop, namespace, module.isDefaultNamespace(), module.getParamClasses(prop), module.getVersion()));
                    }
                }
            }
        }
        return result;
    }
    
    public <A extends PropertyInterface, I extends PropertyInterface> void fillImplicitCases() {
//        ImMap<ImOrderSet<String>, ImSet<String>> mp = getCustomClasses().group(new BaseUtils.Group<String, CustomClass>() {
//            @Override
//            public String group(CustomClass key) {
//                String name = key.getCanonicalName();
//                return name.substring(name.lastIndexOf(".") + 1);
//            }
//        }).filterFnValues(new SFunctionSet<ImSet<CustomClass>>() {
//            @Override
//            public boolean contains(ImSet<CustomClass> element) {
//                return element.size() > 1;
//            }
//        }).mapValues(new GetValue<ImOrderSet<String>, ImSet<CustomClass>>() {
//            @Override
//            public ImOrderSet<String> getMapValue(ImSet<CustomClass> value) {
//                return value.mapSetValues(new GetValue<String, CustomClass>() {
//                    @Override
//                    public String getMapValue(CustomClass value) {
//                        String name = value.getCanonicalName();
//                        return name.substring(0, name.indexOf("."));
//                    }
//                }).sort();
//            }
//        }).groupValues();
//        System.out.println(mp);
//
//        ImMap<ImOrderSet<String>, ImSet<String>> fm = getFormEntities().group(new BaseUtils.Group<String, FormEntity>() {
//            @Override
//            public String group(FormEntity key) {
//                String name = key.getCanonicalName();
//                return name.substring(name.lastIndexOf(".") + 1);
//            }
//        }).filterFnValues(new SFunctionSet<ImSet<FormEntity>>() {
//            @Override
//            public boolean contains(ImSet<FormEntity> element) {
//                return element.size() > 1;
//            }
//        }).mapValues(new GetValue<ImOrderSet<String>, ImSet<FormEntity>>() {
//            @Override
//            public ImOrderSet<String> getMapValue(ImSet<FormEntity> value) {
//                return value.mapSetValues(new GetValue<String, FormEntity>() {
//                    @Override
//                    public String getMapValue(FormEntity value) {
//                        String name = value.getCanonicalName();
//                        return name.substring(0, name.indexOf("."));
//                    }
//                }).sort();
//            }
//        }).groupValues();
//        System.out.println(fm);
        if(!disableImplicitCases) {
            Map<String, List<NamedDecl>> namedProps = getNamedModuleProperties();
            for (List<NamedDecl> props : namedProps.values()) {
                // бежим по всем парам смотрим подходят друг другу или нет
                for (NamedDecl absDecl : props) {
                    String absNamespace = absDecl.namespace;
                    if (AbstractCase.preFillImplicitCases(absDecl.prop)) {
                        for (NamedDecl impDecl : props)
                            AbstractCase.fillImplicitCases(absDecl.prop, impDecl.prop, absDecl.signature, impDecl.signature, absNamespace.equals(impDecl.namespace), impDecl.version);
                    }
                }
            }
        }
    }
    
    public static final boolean disableImplicitCases = true;
    
    public List<AbstractGroup> getParentGroups() {
        return LM.rootGroup.getParentGroups();
    }

    public ImOrderSet<Property> getPropertyList() {
        return getPropertyList(ApplyFilter.NO);
    }

    private void fillActionChangeProps() { // используется только для getLinks, соответственно построения лексикографики и поиска зависимостей
        for (Property property : getOrderProperties()) {
            if (property instanceof ActionProperty && !((ActionProperty) property).getEvents().isEmpty()) { // вырежем Action'ы без Event'ов, они нигде не используются, а дают много компонент связности
                ImMap<CalcProperty, Boolean> change = ((ActionProperty<?>) property).getChangeExtProps();
                for (int i = 0, size = change.size(); i < size; i++) // вообще говоря DataProperty и IsClassProperty
                    change.getKey(i).addActionChangeProp(new Pair<Property<?>, LinkType>(property, change.getValue(i) ? LinkType.RECCHANGE : LinkType.DEPEND));
            }
        }
    }

    private void dropActionChangeProps() { // для экономии памяти - симметричное удаление ссылок
        for (Property property : getOrderProperties()) {
            if (property instanceof ActionProperty && !((ActionProperty) property).getEvents().isEmpty()) {
                ImMap<CalcProperty, Boolean> change = ((ActionProperty<?>) property).getChangeExtProps();
                for (int i = 0, size = change.size(); i < size; i++)
                    change.getKey(i).dropActionChangeProps();
            }
        }
    }

    // находит свойство входящее в "верхнюю" сильносвязную компоненту
    private static HSet<Link> goDown(Property<?> property, MAddMap<Property, HSet<Link>> linksMap, List<Property> order, HSet<Link> removedLinks, boolean include, HSet<Property> component, boolean calcEvents) {
        HSet<Link> linksIn = linksMap.get(property);
        if (linksIn == null) { // уже были, linksMap - одновременно используется и как пометки, и как список, и как обратный обход
            linksIn = new HSet<Link>();
            linksMap.add(property, linksIn);

            ImSet<Link> links = property.getLinks(calcEvents);
            for (int i = 0,size = links.size(); i < size; i++) {
                Link link = links.get(i);
                if (!removedLinks.contains(link) && component.contains(link.to) == include)
                    goDown(link.to, linksMap, order, removedLinks, include, component, calcEvents).add(link);
            }
            order.add(property);
        }
        return linksIn;
    }

    // бежим вниз отсекая выбирая ребро с минимальным приоритетом из этой компоненты
    private static void goUp(Property<?> property, MAddMap<Property, HSet<Link>> linksMap, HSet<Property> proceeded, Result<Link> minLink, HSet<Property> component) {
        if (component.add(property))
            return;

        HSet<Link> linksIn = linksMap.get(property);
        for (int i = 0; i < linksIn.size; i++) {
            Link link = linksIn.get(i);
            if (!proceeded.contains(link.from)) { // если не в верхней компоненте
                goUp(link.from, linksMap, proceeded, minLink, component);
                if (minLink.result == null || link.type.getNum() > minLink.result.type.getNum()) // сразу же ищем минимум из ребер
                    minLink.set(link);
            }
        }
    }

    // upComponent нужен так как изначально неизвестны все элементы
    private static HSet<Property> buildList(HSet<Property> props, HSet<Property> exclude, HSet<Link> removedLinks, MOrderExclSet<Property> mResult, boolean calcEvents) {
        HSet<Property> proceeded;

        List<Property> order = new ArrayList<Property>();
        MAddMap<Property, HSet<Link>> linksMap = MapFact.mAddOverrideMap();
        for (int i = 0; i < props.size; i++) {
            Property property = props.get(i);
            if (linksMap.get(property) == null) // проверка что не было
                goDown(property, linksMap, order, removedLinks, exclude == null, exclude != null ? exclude : props, calcEvents);
        }

        Result<Link> minLink = new Result<Link>();
        proceeded = new HSet<Property>();
        for (int i = 0; i < order.size(); i++) { // тут нужн
            Property orderProperty = order.get(order.size() - 1 - i);
            if (!proceeded.contains(orderProperty)) {
                minLink.set(null);
                HSet<Property> innerComponent = new HSet<Property>();
                goUp(orderProperty, linksMap, proceeded, minLink, innerComponent);
                assert innerComponent.size > 0;
                if (innerComponent.size == 1) // если цикла нет все ОК
                    mResult.exclAdd(innerComponent.single());
                else { // нашли цикл
                    removedLinks.exclAdd(minLink.result);

                    if (minLink.result.type.equals(LinkType.DEPEND)) { // нашли сильный цикл
                        MOrderExclSet<Property> mCycle = SetFact.mOrderExclSet();
                        buildList(innerComponent, null, removedLinks, mCycle, calcEvents);
                        ImOrderSet<Property> cycle = mCycle.immutableOrder();

                        String print = "";
                        for (Property property : cycle)
                            print = (print.length() == 0 ? "" : print + " -> ") + property.toString();
                        throw new RuntimeException(getString("message.cycle.detected") + " : " + print + " -> " + minLink.result.to);
                    }
                    buildList(innerComponent, null, removedLinks, mResult, calcEvents);
                }
                proceeded.exclAddAll(innerComponent);
            }
        }

        return proceeded;
    }

    private static boolean findDependency(Property<?> property, Property<?> with, HSet<Property> proceeded, Stack<Link> path, LinkType desiredType) {
        if (property.equals(with))
            return true;

        if (proceeded.add(property))
            return false;

        for (Link link : property.getLinks(true)) {
            path.push(link);
            if (link.type.getNum() <= desiredType.getNum() && findDependency(link.to, with, proceeded, path, desiredType))
                return true;
            path.pop();
        }
        property.dropLinks();

        return false;
    }

    private static String outDependency(String direction, Property property, Stack<Link> path) {
        String result = direction + " : " + property;
        for (Link link : path)
            result += " " + link.type + " " + link.to;
        return result;
    }

    private static String findDependency(Property<?> property1, Property<?> property2, LinkType desiredType) {
        String result = "";

        Stack<Link> forward = new Stack<Link>();
        if (findDependency(property1, property2, new HSet<Property>(), forward, desiredType))
            result += outDependency("FORWARD", property1, forward) + '\n';

        Stack<Link> backward = new Stack<Link>();
        if (findDependency(property2, property1, new HSet<Property>(), backward, desiredType))
            result += outDependency("BACKWARD", property2, backward) + '\n';

        if (result.isEmpty())
            result += "NO DEPENDENCY " + property1 + " " + property2 + '\n';

        return result;
    }

    public void showDependencies() {
        String show = "";

        boolean found = false; // оптимизация, так как showDep не так часто используется

        for (Property property : getOrderProperties())
            if (property.showDep != null) {
                if(!found) {
                    fillActionChangeProps();
                    found = true;
                }
                show += findDependency(property, property.showDep, LinkType.USEDACTION);
            }
        if (!show.isEmpty()) {
            systemLogger.debug("Dependencies: " + show);
        }

        if(found)
            dropActionChangeProps();
    }

    @IdentityStrongLazy // глобальное очень сложное вычисление
    public ImOrderSet<Property> getPropertyList(ApplyFilter filter) {
        // жестковато тут конечно написано, но пока не сильно времени жрет

        fillActionChangeProps();

        // сначала бежим по Action'ам с cancel'ами
        HSet<Property> cancelActions = new HSet<Property>();
        HSet<Property> rest = new HSet<Property>();
        for (Property property : getOrderProperties())
            if(filter.contains(property)) {
                if (ApplyFilter.isCheck(property))
                    cancelActions.add(property);
                else
                    rest.add(property);
            }
        boolean calcEvents = filter != ApplyFilter.ONLY_DATA;

        MOrderExclSet<Property> mCancelResult = SetFact.mOrderExclSet();
        HSet<Property> proceeded = buildList(cancelActions, new HSet<Property>(), new HSet<Link>(), mCancelResult, calcEvents);
        ImOrderSet<Property> cancelResult = mCancelResult.immutableOrder();

        // потом бежим по всем остальным, за исключением proceeded
        MOrderExclSet<Property> mRestResult = SetFact.mOrderExclSet();
        HSet<Property> removed = new HSet<Property>();
        removed.addAll(rest.remove(proceeded));
        buildList(removed, proceeded, new HSet<Link>(), mRestResult, calcEvents); // потом этот cast уберем
        ImOrderSet<Property> restResult = mRestResult.immutableOrder();

        // затем по всем кроме proceeded на прошлом шаге
        assert cancelResult.getSet().disjoint(restResult.getSet());
        ImOrderSet<Property> result = cancelResult.reverseOrder().addOrderExcl(restResult.reverseOrder());

        for(Property property : result) {
            property.dropLinks();
            if(property instanceof CalcProperty)
                ((CalcProperty)property).dropActionChangeProps();
        }
        return result;
    }

    List<AggregateProperty> getAggregateStoredProperties() {
        List<AggregateProperty> result = new ArrayList<AggregateProperty>();
        for (Property property : getStoredProperties())
            if (property instanceof AggregateProperty)
                result.add((AggregateProperty) property);
        return result;
    }

    @IdentityLazy
    public ImOrderSet<CalcProperty> getStoredProperties() {
        return BaseUtils.immutableCast(getPropertyList().filterOrder(new SFunctionSet<Property>() {
            public boolean contains(Property property) {
                return property instanceof CalcProperty && ((CalcProperty) property).isStored();
            }
        }));
    }

    @IdentityLazy
    public ImOrderSet<CalcProperty> getStoredDataProperties() {
        return BaseUtils.immutableCast(getPropertyList().filterOrder(new SFunctionSet<Property>() {
            public boolean contains(Property property) {
                return property instanceof StoredDataProperty || property instanceof ClassDataProperty;
            }
        }));
    }

    public ImSet<CustomClass> getCustomClasses() {
        return LM.baseClass.getAllClasses();
    }

    public ImSet<ConcreteCustomClass> getConcreteCustomClasses() {
        return BaseUtils.immutableCast(getCustomClasses().filterFn(new SFunctionSet<CustomClass>() {
            public boolean contains(CustomClass property) {
                return property instanceof ConcreteCustomClass;
            }
        }));
    }

    @IdentityLazy
    public ImOrderMap<ActionProperty, SessionEnvEvent> getSessionEvents() {
        ImOrderSet<Property> list = getPropertyList();
        MOrderExclMap<ActionProperty, SessionEnvEvent> mResult = MapFact.mOrderExclMapMax(list.size());
        SessionEnvEvent sessionEnv;
        for (Property property : list)
            if (property instanceof ActionProperty && (sessionEnv = ((ActionProperty) property).getSessionEnv(SystemEvent.SESSION))!=null)
                mResult.exclAdd((ActionProperty) property, sessionEnv);
        return mResult.immutableOrder();
    }

    @IdentityLazy
    public ImSet<CalcProperty> getDataChangeEvents() {
        ImOrderSet<Property> propertyList = getPropertyList();
        MSet<CalcProperty> mResult = SetFact.mSetMax(propertyList.size());
        for (int i=0,size=propertyList.size();i<size;i++) {
            Property property = propertyList.get(i);
            if (property instanceof DataProperty && ((DataProperty) property).event != null)
                mResult.add((((DataProperty) property).event).getWhere());
        }
        return mResult.immutable();
    }

    @IdentityLazy
    public ImOrderMap<Object, SessionEnvEvent> getAppliedProperties(ApplyFilter increment) {
        // здесь нужно вернуть список stored или тех кто
        ImOrderSet<Property> list = getPropertyList(increment);
        MOrderExclMap<Object, SessionEnvEvent> mResult = MapFact.mOrderExclMapMax(list.size());
        for (Property property : list) {
            if (property instanceof CalcProperty && ((CalcProperty) property).isStored())
                mResult.exclAdd(property, SessionEnvEvent.ALWAYS);
            SessionEnvEvent sessionEnv;
            if (property instanceof ActionProperty && (sessionEnv = ((ActionProperty) property).getSessionEnv(SystemEvent.APPLY))!=null)
                mResult.exclAdd(new ActionPropertyValueImplement((ActionProperty) property), sessionEnv);
        }
        return mResult.immutableOrder();
    }

    public ImOrderSet<Object> getAppliedProperties(DataSession session) {
        return session.filterOrderEnv(getAppliedProperties(session.applyFilter));
    }

    private void fillSingleApplyDependFrom(CalcProperty<?> fill, CalcProperty<?> applied, SessionEnvEvent appliedSet, MExclMap<CalcProperty, MMap<CalcProperty, SessionEnvEvent>> mapDepends, boolean canBeOutOfDepends) {
        if (!fill.equals(applied) && fill.isSingleApplyStored()) {
            MMap<CalcProperty, SessionEnvEvent> fillDepends = mapDepends.get(fill);
            if(!canBeOutOfDepends || fillDepends!=null)
                fillDepends.add(applied, appliedSet);
        } else
            for (CalcProperty depend : fill.getDepends(false)) // derived'ы отдельно отрабатываются
                fillSingleApplyDependFrom(depend, applied, appliedSet, mapDepends, canBeOutOfDepends);
    }

    private ImMap<CalcProperty, ImMap<CalcProperty, SessionEnvEvent>> getMapSingleApplyDepends(ApplyFilter increment) {

        ImOrderMap<Object, SessionEnvEvent> appliedProps = getAppliedProperties(increment);

        // нам нужны будут сами persistent свойства + prev'ы у action'ов
        MOrderExclSet<CalcProperty> mSingleAppliedStored = SetFact.mOrderExclSet();
        MMap<OldProperty, SessionEnvEvent> mSingleAppliedOld = MapFact.mMap(SessionEnvEvent.<OldProperty>mergeSessionEnv());
        for(int i=0,size=appliedProps.size();i<size;i++) {
            Object property = appliedProps.getKey(i);
            if(property instanceof ActionPropertyValueImplement) {
                ActionProperty actionProperty = ((ActionPropertyValueImplement) property).property;
                SessionEnvEvent sessionEnv;
                if ((sessionEnv = actionProperty.getSessionEnv(SystemEvent.APPLY))!=null)
                    mSingleAppliedOld.addAll(actionProperty.getOldDepends().toMap(sessionEnv));
            } else {
                if (property instanceof DataProperty && ((DataProperty) property).event != null)
                    mSingleAppliedOld.addAll(((DataProperty) property).event.getOldDepends().toMap(SessionEnvEvent.ALWAYS));
                if(((CalcProperty)property).isEnabledSingleApply())
                    mSingleAppliedStored.exclAdd((CalcProperty) property);
            }
        }
        ImOrderSet<CalcProperty> singleAppliedStored = mSingleAppliedStored.immutableOrder();
        ImMap<OldProperty, SessionEnvEvent> singleAppliedOld = mSingleAppliedOld.immutable();

        MExclMap<CalcProperty, MMap<CalcProperty, SessionEnvEvent>> mMapDepends = MapFact.mExclMap();
        for (CalcProperty property : singleAppliedStored) {
            mMapDepends.exclAdd(property, MapFact.mMap(SessionEnvEvent.<CalcProperty>mergeSessionEnv()));
            fillSingleApplyDependFrom(property, property, SessionEnvEvent.ALWAYS, mMapDepends, false);
        }

        assert singleAppliedOld.keys().filterFn(new SFunctionSet<OldProperty>() {
            public boolean contains(OldProperty element) {
                return !element.scope.onlyDB();
            }}).isEmpty();

        for (int i=0,size= singleAppliedOld.size();i<size;i++) {
            OldProperty old = singleAppliedOld.getKey(i);
            fillSingleApplyDependFrom(old.property, old, singleAppliedOld.getValue(i), mMapDepends, increment != ApplyFilter.NO);
        }

        return mMapDepends.immutable().mapValues(new GetValue<ImMap<CalcProperty, SessionEnvEvent>, MMap<CalcProperty, SessionEnvEvent>>() {
            public ImMap<CalcProperty, SessionEnvEvent> getMapValue(MMap<CalcProperty, SessionEnvEvent> value) {
                return value.immutable();
            }});
    }

    private void fillSingleApplyDependFrom(ImMap<CalcProperty, SessionEnvEvent> singleApplied, MExclMap<CalcProperty, MMap<CalcProperty, SessionEnvEvent>> mMapDepends) {
        for (int i=0,size=singleApplied.size();i<size;i++) {
            CalcProperty property = singleApplied.getKey(i);
            fillSingleApplyDependFrom(property, property, singleApplied.getValue(i), mMapDepends, false);
        }
    }

    @IdentityLazy
    private ImMap<CalcProperty, ImOrderMap<CalcProperty, SessionEnvEvent>> getOrderMapSingleApplyDepends(ApplyFilter filter) {
        final ImRevMap<Property, Integer> indexMap = getPropertyList().mapOrderRevValues(new GetIndex<Integer>() {
            public Integer getMapValue(int i) {
                return i;
            }
        });
        return getMapSingleApplyDepends(filter).mapValues(new GetValue<ImOrderMap<CalcProperty, SessionEnvEvent>, ImMap<CalcProperty, SessionEnvEvent>>() {
            public ImOrderMap<CalcProperty, SessionEnvEvent> getMapValue(ImMap<CalcProperty, SessionEnvEvent> depends) {
                ImOrderSet<CalcProperty> dependList = indexMap.filterInclRev(depends.keys()).reverse().sort().valuesList().toOrderExclSet();
                assert dependList.size() == depends.size();
                return dependList.mapOrderMap(depends);
            }
        });
    }

    // определяет для stored свойства зависимые от него stored свойства, а также свойства которым необходимо хранить изменения с начала транзакции (constraints и derived'ы)
    public ImOrderSet<CalcProperty> getSingleApplyDependFrom(CalcProperty property, DataSession session) {
        assert property.isSingleApplyStored();
        return session.filterOrderEnv(getOrderMapSingleApplyDepends(session.applyFilter).get(property));
    }

    @IdentityLazy
    public List<CalcProperty> getCheckConstrainedProperties() {
        List<CalcProperty> result = new ArrayList<CalcProperty>();
        for (Property property : getPropertyList()) {
            if (property instanceof CalcProperty && ((CalcProperty) property).checkChange != CalcProperty.CheckType.CHECK_NO) {
                result.add((CalcProperty) property);
            }
        }
        return result;
    }

    public List<CalcProperty> getCheckConstrainedProperties(CalcProperty<?> changingProp) {
        List<CalcProperty> result = new ArrayList<CalcProperty>();
        for (CalcProperty property : getCheckConstrainedProperties()) {
            if (property.checkChange == CalcProperty.CheckType.CHECK_ALL ||
                    property.checkChange == CalcProperty.CheckType.CHECK_SOME && property.checkProperties.contains(changingProp)) {
                result.add(property);
            }
        }
        return result;
    }

    public List<LogicsModule> getLogicModules() {
        return logicModules;
    }

    public void recalculateStats(DataSession session) throws SQLException, SQLHandledException {
        for (ImplementTable dataTable : LM.tableFactory.getImplementTables()) {
            dataTable.calculateStat(this.reflectionLM, session);
        }
        recalculateClassStat(session);
    }

    public void recalculateClassStat(DataSession session) throws SQLException, SQLHandledException {
        for(ObjectValueClassSet tableClasses : LM.baseClass.getUpTables().valueIt()) {
            QueryBuilder<Integer, Integer> classes = new QueryBuilder<Integer, Integer>(SetFact.singleton(0));

            KeyExpr countKeyExpr = new KeyExpr("count");
            Expr countExpr = GroupExpr.create(MapFact.singleton(0, countKeyExpr.classExpr(LM.baseClass)),
                    new ValueExpr(1, IntegerClass.instance), countKeyExpr.isClass(tableClasses), GroupType.SUM, classes.getMapExprs());

            classes.addProperty(0, countExpr);
            classes.and(countExpr.getWhere());

            ImOrderMap<ImMap<Integer, Object>, ImMap<Integer, Object>> classStats = classes.execute(session);
            ImSet<ConcreteCustomClass> concreteChilds = tableClasses.getSetConcreteChildren();
            for(int i=0,size=concreteChilds.size();i<size;i++) {
                ConcreteCustomClass customClass = concreteChilds.get(i);
                ImMap<Integer, Object> classStat = classStats.get(MapFact.singleton(0, (Object) customClass.ID));
                LM.statCustomObjectClass.change(classStat==null ? 1 : (Integer)classStat.singleValue(), session, customClass.getClassObject());
            }
        }
    }

    public String recalculateFollows(SessionCreator creator, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        final List<String> messageList = new ArrayList<String>();
        final long maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        for (Property property : getPropertyList())
            if (property instanceof ActionProperty) {
                final ActionProperty<?> action = (ActionProperty) property;
                if (action.hasResolve()) {
                    long start = System.currentTimeMillis();
                    DBManager.runData(creator, isolatedTransaction, new DBManager.RunServiceData() {
                        public void run(SessionCreator session) throws SQLException, SQLHandledException {
                            ((DataSession)session).resolve(action);
                        }
                    });
                    long time = System.currentTimeMillis() - start;
                    String message = String.format("Recalculate Follows: %s, %sms", property.getSID(), time);
                    systemLogger.info(message);
                    if(time > maxRecalculateTime)
                        messageList.add(message);                    
                }
            }
        return formatMessageList(messageList);
    }
    
    protected String formatMessageList(List<String> messageList) {
        if(messageList.isEmpty())
            return null;
        else {
            String result = "";
            for (String message : messageList)
                result += message + '\n';
            return result;
        }
    }

    public String checkClasses(SQLSession session) throws SQLException, SQLHandledException {
        String message = DataSession.checkClasses(session, LM.baseClass);
        for (CalcProperty property : getStoredDataProperties()) // getStoredProperties()
            message += DataSession.checkClasses(property, session, LM.baseClass);
        return message;
    }

    public void recalculateExclusiveness(final SQLSession session, boolean isolatedTransactions) throws SQLException, SQLHandledException {
        DBManager.run(session, isolatedTransactions, new DBManager.RunService() {
            public void run(final SQLSession sql) throws SQLException, SQLHandledException {
                DataSession.runExclusiveness(new DataSession.RunExclusiveness() {
                    public void run(Query<String, String> query) throws SQLException, SQLHandledException {
                        SingleKeyTableUsage<String> table = new SingleKeyTableUsage<String>(ObjectType.instance, SetFact.toOrderExclSet("sum", "agg"), new Type.Getter<String>() {
                            public Type getType(String key) {
                                return key.equals("sum") ? ValueExpr.COUNTCLASS : StringClass.getv(false, ExtInt.UNLIMITED);
                            }});

                        table.writeRows(sql, query, LM.baseClass, DataSession.emptyEnv(OperationOwner.unknown));
                        
                        MExclMap<ConcreteCustomClass, MExclSet<String>> mRemoveClasses = MapFact.mExclMap();
                        for(Object distinct : table.readDistinct("agg", sql, OperationOwner.unknown)) { // разновидности agg читаем
                            String classes = (String)distinct;
                            ConcreteCustomClass keepClass = null;
                            for(String singleClass : classes.split(",")) {
                                ConcreteCustomClass customClass = LM.baseClass.findConcreteClassID(Integer.parseInt(singleClass));
                                if(customClass != null) {
                                    if(keepClass == null)
                                        keepClass = customClass;
                                    else {
                                        ConcreteCustomClass removeClass;
                                        if(keepClass.isChild(customClass)) {
                                            removeClass = keepClass;
                                            keepClass = customClass;
                                        } else
                                            removeClass = customClass;
                                        
                                        MExclSet<String> mRemoveStrings = mRemoveClasses.get(removeClass);
                                        if(mRemoveStrings == null) {
                                            mRemoveStrings = SetFact.mExclSet();
                                            mRemoveClasses.exclAdd(removeClass, mRemoveStrings);
                                        }
                                        mRemoveStrings.exclAdd(classes);
                                    }
                                }
                            }
                        }
                        ImMap<ConcreteCustomClass, ImSet<String>> removeClasses = MapFact.immutable(mRemoveClasses);

                        for(int i=0,size=removeClasses.size();i<size;i++) {
                            KeyExpr key = new KeyExpr("key");
                            Expr aggExpr = table.join(key).getExpr("agg");
                            Where where = Where.FALSE;
                            for(String removeString : removeClasses.getValue(i))
                                where = where.or(aggExpr.compare(new DataObject(removeString, StringClass.text), Compare.EQUALS));
                            removeClasses.getKey(i).dataProperty.dropInconsistentClasses(session, LM.baseClass, key, where, OperationOwner.unknown);
                        }                            
                    }
                }, sql, LM.baseClass);
            }});
    }

    public String recalculateClasses(SQLSession session, boolean isolatedTransactions) throws SQLException, SQLHandledException {
        recalculateExclusiveness(session, isolatedTransactions);

        final List<String> messageList = new ArrayList<String>();
        final long maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        for (final CalcProperty property : getStoredDataProperties())
            DBManager.run(session, isolatedTransactions, new DBManager.RunService() {
                public void run(SQLSession sql) throws SQLException, SQLHandledException {
                    long start = System.currentTimeMillis();
                    property.recalculateClasses(sql, LM.baseClass);
                    long time = System.currentTimeMillis() - start;
                    String message = String.format("Recalculate Class: %s, %s", property.getSID(), time);
                    systemLogger.info(message);
                    if(time > maxRecalculateTime)
                        messageList.add(message);
                }});
        return formatMessageList(messageList);
    }

    private void test(String testCase) {
        try {
            PropertyCanonicalNameParser parser = new PropertyCanonicalNameParser(this, testCase);
            List<ResolveClassSet> res = parser.getSignature();
            System.out.print('"' + testCase + "\": " + (res == null ? "null" : res.toString()));
            testCase = testCase.replaceAll(" ", "");
            System.out.println(" -> " + DefaultDBNamePolicy.staticTransformCanonicalNameToDBName(testCase));
        } catch (PropertyCanonicalNameParser.ParseException e) {
            System.out.println('"' + testCase + "\": error (" + e.getMessage() + ")");
        }
    }
    
    public LP findProperty(String canonicalName) {
        PropertyCanonicalNameParser parser = new PropertyCanonicalNameParser(this, canonicalName);
        try {
            String namespaceName = parser.getNamespace();
            String name = parser.getName();
            List<ResolveClassSet> signature = parser.getSignature();
            return findProperty(namespaceName, name, signature);
        } catch (PropertyCanonicalNameParser.ParseException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    public LP findProperty(String namespace, String name, ValueClass... classes) {
        List<ResolveClassSet> classSets = null;
        if (classes.length > 0) {
            classSets = new ArrayList<ResolveClassSet>();
            for (ValueClass cls : classes) {
                classSets.add(cls.getResolveSet());
            }
        }
        return findProperty(namespace, name, classSets);
    }

    private LP findProperty(String namespace, String name, List<ResolveClassSet> classes) {
        assert namespaceToModules.get(namespace) != null;
        NamespacePropertyFinder finder = new NamespacePropertyFinder(new SoftLPModuleFinder(), namespaceToModules.get(namespace));
        List<NamespaceElementFinder.FoundItem<LP<?, ?>>> foundElements = finder.findInNamespace(namespace, name, classes);
        assert foundElements.size() <= 1;
        return foundElements.size() == 0 ? null : foundElements.get(0).value;
    }

    private <R, P> R findElement(String canonicalName, P param, ModuleFinder<R, P> moduleFinder) {
        assert canonicalName != null;
        if (canonicalName.contains(".")) {
            String namespaceName = canonicalName.substring(0, canonicalName.indexOf('.'));
            String className = canonicalName.substring(canonicalName.indexOf('.') + 1);

            assert namespaceToModules.get(namespaceName) != null;
            NamespaceElementFinder<R, P> finder = new NamespaceElementFinder<R, P>(moduleFinder, namespaceToModules.get(namespaceName));
            List<NamespaceElementFinder.FoundItem<R>> resList = finder.findInNamespace(namespaceName, className, param);
            assert resList.size() <= 1; 
            return resList.size() == 0 ? null : resList.get(0).value;
        }
        return null;
    }
    
    public CustomClass findClass(String canonicalName) {
        return findElement(canonicalName, null, new ClassNameModuleFinder());
    }

    public AbstractGroup findGroup(String canonicalName) {
        return findElement(canonicalName, null, new GroupNameModuleFinder());
    }

    public ImplementTable findTable(String canonicalName) {
        return findElement(canonicalName, null, new TableNameModuleFinder());
    }

    public AbstractWindow findWindow(String canonicalName) {
        return findElement(canonicalName, null, new WindowNameModuleFinder());
    }

    public NavigatorElement findNavigatorElement(String canonicalName) {
        return findElement(canonicalName, null, new NavigatorElementNameModuleFinder());
    }

    public MetaCodeFragment findMetaCode(String canonicalName, int paramCnt) {
        return findElement(canonicalName, paramCnt, new MetaCodeNameModuleFinder());
    }

    private void outputPersistent() {
        String result = "";

        result += '\n' + getString("logics.info.by.tables") + '\n' + '\n';
        for (Map.Entry<ImplementTable, Collection<CalcProperty>> groupTable : BaseUtils.group(new BaseUtils.Group<ImplementTable, CalcProperty>() {
            public ImplementTable group(CalcProperty key) {
                return key.mapTable.table;
            }
        }, getStoredProperties()).entrySet()) {
            result += groupTable.getKey().outputKeys() + '\n';
            for (CalcProperty property : groupTable.getValue())
                result += '\t' + property.outputStored(false) + '\n';
        }
        result += '\n' + getString("logics.info.by.properties") + '\n' + '\n';
        for (CalcProperty property : getStoredProperties())
            result += property.outputStored(true) + '\n';
        System.out.println(result);
    }

    public ImSet<FormEntity> getFormEntities(){
        MExclSet<FormEntity> mResult = SetFact.mExclSet();
        for(LogicsModule logicsModule : logicModules) {
            for(NavigatorElement entry : logicsModule.getModuleNavigators())
                if(entry instanceof FormEntity)
                    mResult.exclAdd((FormEntity) entry);
        }
        return mResult.immutable();
    }

    public ImSet<NavigatorElement> getNavigatorElements() {
        MExclSet<NavigatorElement> mResult = SetFact.mExclSet();
        for(LogicsModule logicsModule : logicModules) {
            for(NavigatorElement entry : logicsModule.getModuleNavigators())
                mResult.exclAdd(entry);            
        }
        return mResult.immutable();
    }

    public FormEntity getFormEntityBySID(String formSID){
        for (LogicsModule logicsModule : logicModules) {
            for (NavigatorElement element : logicsModule.getModuleNavigators()) {
                if ((element instanceof FormEntity) && formSID.equals(element.getSID())) {
                    return (FormEntity) element;
                }
            }
        }
        return null;
    }

    // Набор методов для поиска модуля, в котором находится элемент системы
    private <T, P> LogicsModule getModuleContainingObject(String namespaceName, String name, P param, ModuleFinder<T, P> finder) {
        List<LogicsModule> modules = namespaceToModules.get(namespaceName);
        if (modules != null) {
            for (LogicsModule module : modules) {
                if (!finder.resolveInModule(module, name, param).isEmpty()) {
                    return module;
                }
            }
        }
        return null;
    }

    // Здесь ищется точное совпадение по сигнатуре
    public LogicsModule getModuleContainingLP(String namespaceName, String name, List<ResolveClassSet> classes) {
        return getModuleContainingObject(namespaceName, name, classes, new EqualLPModuleFinder(false));
    }

    public LogicsModule getModuleContainingGroup(String namespaceName, String name) {
        return getModuleContainingObject(namespaceName, name, null, new GroupNameModuleFinder());
    }

    public LogicsModule getModuleContainingClass(String namespaceName, String name) {
        return getModuleContainingObject(namespaceName, name, null, new ClassNameModuleFinder());
    }

    public LogicsModule getModuleContainingTable(String namespaceName, String name) {
        return getModuleContainingObject(namespaceName, name, null, new TableNameModuleFinder());
    }

    public LogicsModule getModuleContainingWindow(String namespaceName, String name) {
        return getModuleContainingObject(namespaceName, name, null, new WindowNameModuleFinder());
    }

    public LogicsModule getModuleContainingNavigatorElement(String namespaceName, String name) {
        return getModuleContainingObject(namespaceName, name, null, new NavigatorElementNameModuleFinder());
    }

    public LogicsModule getModuleContainingMetaCode(String namespaceName, String name, int paramCnt) {
        return getModuleContainingObject(namespaceName, name, paramCnt, new MetaCodeNameModuleFinder());
    }

    protected DBManager getDbManager() {
        return ThreadLocalContext.getDbManager();
    }
    
    public String getDataBaseName() {
        return getDbManager().getDataBaseName();
    }
}
