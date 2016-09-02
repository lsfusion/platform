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
import lsfusion.interop.exceptions.LogMessageLogicsException;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenParameters;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.SystemProperties;
import lsfusion.server.caches.CacheStats;
import lsfusion.server.caches.CacheStats.CacheType;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.classes.sets.ResolveOrObjectClassSet;
import lsfusion.server.context.EExecutionStackRunnable;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.daemons.DiscountCardDaemonTask;
import lsfusion.server.daemons.ScannerDaemonTask;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.MapCacheAspect;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.LogFormEntity;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.form.navigator.RemoteNavigator;
import lsfusion.server.form.window.AbstractWindow;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.NFLazy;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.FormActionProperty;
import lsfusion.server.logics.property.actions.SessionEnvEvent;
import lsfusion.server.logics.property.actions.SystemEvent;
import lsfusion.server.logics.property.cases.AbstractCase;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.scripted.MetaCodeFragment;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.logics.table.MapKeysTable;
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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.isRedundantString;
import static lsfusion.base.BaseUtils.nullToZero;
import static lsfusion.base.BaseUtils.serviceLogger;
import static lsfusion.server.logics.LogicsModule.*;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public abstract class BusinessLogics<T extends BusinessLogics<T>> extends LifecycleAdapter implements InitializingBean {
    protected final static Logger logger = ServerLoggers.systemLogger;
    protected final static Logger sqlLogger = ServerLoggers.sqlLogger;
    protected final static Logger startLogger = ServerLoggers.startLogger;
    protected final static Logger debuglogger = Logger.getLogger(BusinessLogics.class);
    protected final static Logger lruLogger = ServerLoggers.lruLogger;

    public static final List<String> defaultExcludedScriptPaths = Collections.singletonList("lsfusion/system");
    public static final List<String> defaultIncludedScriptPaths = Collections.singletonList("");

    public static final String[] systemModulesNames = {"System", "Authentication", "Email", "EvalScript", "Reflection", "Contact", 
                                                       "Scheduler", "Security", "Service", "SystemEvents", "Time"};
    
    private List<LogicsModule> logicModules = new ArrayList<>();
    private Map<String, List<LogicsModule>> namespaceToModules = new HashMap<>();

    private final Map<String, LogicsModule> nameToModule = new HashMap<>();

    private final List<ExternalScreen> externalScreens = new ArrayList<>();

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
            startLogger.info("Initializing BusinessLogics");
            try {
                getDbManager().ensureLogLevel();
                
                new TaskRunner(this).runTask(initTask, startLogger);
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException("Error initializing BusinessLogics: ", e);
            }
        }
    }

    public LRUWSASVSMap<Object, Method, Object, Object> startLruCache = new LRUWSASVSMap<>(LRUUtil.G2);
    public void cleanCaches() {
        startLruCache = null;
        MapCacheAspect.cleanClassCaches();

        startLogger.info("Obsolete caches were successfully cleaned");
    }
    
    public ScriptingLogicsModule getModule(String name) {
        return (ScriptingLogicsModule) getSysModule(name);
    }
    
    public LogicsModule getSysModule(String name) {
        return nameToModule.get(name);
    }

    public ConcreteClass getDataClass(Object object, Type type) {
        try {
            try (DataSession session = getDbManager().createSession()) {
                return type.getDataClass(object, session.sql, LM.baseClass.getUpSet(), LM.baseClass, OperationOwner.unknown);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<IDaemonTask> getDaemonTasks(int compId) {
        ArrayList<IDaemonTask> daemons = new ArrayList<>();

        Integer scannerComPort;
        Boolean scannerSingleRead;
        boolean useDiscountCardReader;

        try {
            try(DataSession session = getDbManager().createSession()) {
                DataObject computerObject = new DataObject(compId, authenticationLM.computer);
                scannerComPort = (Integer) authenticationLM.scannerComPortComputer.read(session, computerObject);
                scannerSingleRead = (Boolean) authenticationLM.scannerSingleReadComputer.read(session, computerObject);
                useDiscountCardReader = authenticationLM.useDiscountCardReaderComputer.read(session, computerObject) != null;
            }
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
        if(useDiscountCardReader)
            daemons.add(new DiscountCardDaemonTask());
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

    protected <M extends LogicsModule> M addModule(M module) {
        logicModules.add(module);
        return module;
    }

    public void createModules() throws IOException {
        LM = addModule(new BaseLogicsModule(this, getDBNamingPolicy()));
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

    private PropertyDBNamePolicy getDBNamingPolicy() {
        PropertyDBNamePolicy dbNamingPolicy = null;
        try {
            String policyName = getDbManager().getDbNamingPolicy();
            if (policyName != null && !policyName.isEmpty()) {
                Integer maxIdLength = getDbManager().getDbMaxIdLength();
                Class cls = Class.forName(policyName);
                dbNamingPolicy = (PropertyDBNamePolicy) cls.getConstructors()[0].newInstance(maxIdLength);
            }
        } catch (InvocationTargetException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            logger.error("Failed to get DBNamingPolicy, used default", e);
        }
        return dbNamingPolicy == null ? new DefaultDBNamePolicy(63) : dbNamingPolicy;
    }

    protected void addModulesFromResource(List<String> paths, List<String> excludedPaths) throws IOException {
        if (excludedPaths == null || excludedPaths.isEmpty()) {
            excludedPaths = defaultExcludedScriptPaths;
        } else {
            excludedPaths = new ArrayList<>(excludedPaths);
            excludedPaths.addAll(defaultExcludedScriptPaths);
        }

        List<String> excludedLSF = new ArrayList<>();

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
        Map<String, Set<String>> graph = new HashMap<>();
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
        Set<String> used = new HashSet<>();
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
            FileWriter fStream = new FileWriter("D:/lsf/modules.dot");
            BufferedWriter out = new BufferedWriter(fStream);
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

        Map<String, Integer> degree = new HashMap<>();
        for (LogicsModule module : logicModules) {
            degree.put(module.getName(), 0);
        }

        for (Map.Entry<String, Set<String>> e : graph.entrySet()) {
            for (String nextModule : e.getValue()) {
                degree.put(nextModule, degree.get(nextModule) + 1);
            }
        }

        Set<LogicsModule> usedModules = new LinkedHashSet<>();
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
        return new ArrayList<>(usedModules);
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

    private LogicsModule getModuleWithCheck(String moduleName) {
        LogicsModule module = nameToModule.get(moduleName);
        if (module == null) {
            throw new RuntimeException(String.format("Module %s not found.", moduleName));
        }
        return module;
    }
    
    private void overrideModulesList(String startModuleName) {
        Set<LogicsModule> was = new HashSet<>();
        Queue<LogicsModule> queue = new LinkedList<>();

        fillNameToModules();
        
        // Всегда добавляем сначала системные модули 
        for (String systemModuleName : systemModulesNames) {
            was.add(getModuleWithCheck(systemModuleName));    
        }
        
        LogicsModule startModule = getModuleWithCheck(startModuleName);
        queue.add(startModule);
        was.add(startModule);

        while (!queue.isEmpty()) {
            LogicsModule current = queue.poll();

            for (String nextModuleName : current.getRequiredModules()) {
                LogicsModule nextModule = getModuleWithCheck(nextModuleName);
                if (!was.contains(nextModule)) {
                    was.add(nextModule);
                    queue.add(nextModule);
                }
            }
        }

        logicModules = new ArrayList<>(was);
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

        Map<LogicsModule, ImSet<LogicsModule>> recRequiredModules = new HashMap<>();
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

    public void initFullSingleTables() {
        for(ImplementTable table : LM.tableFactory.getImplementTables()) {
            if(table.markedFull && !table.isFull())  // для второго условия все и делается, чтобы не создавать лишние св-ва
                LM.markFull(table, table.mapFields.singleValue());
        }
    }

    private boolean needIndex(ObjectValueClassSet classSet) {
        ImSet<ConcreteCustomClass> set = classSet.getSetConcreteChildren();
        if(set.size() > 1) { // оптимизация
            int count = classSet.getCount();
            if(count >= Settings.get().getMinClassDataIndexCount()) {
                Stat totStat = new Stat(count);
                for (ConcreteCustomClass customClass : set)
                    if (customClass.stat != null && new Stat(customClass.getCount()).less(totStat))
                        return true;
            }
        }
        return false;
    }

    public void initClassDataProps() {
        ImMap<ImplementTable, ImSet<ConcreteCustomClass>> groupTables = getConcreteCustomClasses().group(new BaseUtils.Group<ImplementTable, ConcreteCustomClass>() {
            public ImplementTable group(ConcreteCustomClass customClass) {
                return LM.tableFactory.getClassMapTable(MapFact.singleton("key", (ValueClass) customClass)).table;
            }
        });

        for(int i=0,size=groupTables.size();i<size;i++) {
            ImplementTable table = groupTables.getKey(i);
            ImSet<ConcreteCustomClass> set = groupTables.getValue(i);

            ObjectValueClassSet classSet = OrObjectClassSet.fromSetConcreteChildren(set);

            CustomClass tableClass = (CustomClass) table.mapFields.singleValue();
            // помечаем full tables
            assert tableClass.getUpSet().containsAll(classSet, false); // должны быть все классы по определению, исходя из логики раскладывания классов по таблицам
            boolean isFull = classSet.containsAll(tableClass.getUpSet(), false);
            if(isFull) // важно чтобы getInterfaceClasses дал тот же tableClass
                classSet = tableClass.getUpSet();

            ClassDataProperty dataProperty = new ClassDataProperty(classSet.toString(), classSet);
            LCP<ClassPropertyInterface> lp = new LCP<>(dataProperty);
            LM.addProperty(null, new LCP<>(dataProperty));
            LM.makePropertyPublic(lp, PropertyCanonicalNameUtils.classDataPropPrefix + table.getName(), Collections.<ResolveClassSet>singletonList(ResolveOrObjectClassSet.fromSetConcreteChildren(set)));
            // именно такая реализация, а не implementTable, из-за того что getInterfaceClasses может попасть не в "класс таблицы", а мимо и тогда нарушится assertion что должен попасть в ту же таблицу, это в принципе проблема getInterfaceClasses
            dataProperty.markStored(LM.tableFactory, new MapKeysTable<>(table, MapFact.singletonRev(dataProperty.interfaces.single(), table.keys.single())));

            // помечаем dataProperty
            for(ConcreteCustomClass customClass : set)
                customClass.dataProperty = dataProperty;
            if(isFull) // неважно implicit или нет
                table.setFullField(dataProperty);
        }
    }

    public void initClassDataIndices() {
        for(ObjectClassField classField : LM.baseClass.getUpObjectClassFields().keyIt()) {
            ClassDataProperty classProperty = classField.getProperty();
            if(needIndex(classProperty.set))
                LM.addIndex(classProperty);
        }
    }

    public void initReflectionEvents() {

        initStats();
        //временное решение
        
        try {
            SQLSession sql = getDbManager().getThreadLocalSql();
            
            startLogger.info("Setting user logging for properties");
            try {
                setUserLoggableProperties(sql);
            } catch (Exception e) {
                // пробуем еще раз но с reparse'ом, для явной типизации
                useReparse = true;

                ImSet<String> props = SetFact.toSet("canonicalName", "stats", "userLoggable", "notNullQuantity");
                reparse.set(props.mapKeyValues(new GetValue<String, String>() {
                    public String getMapValue(String value) {
                        return "Reflection_" + value + "_Property";
                    }}, new GetValue<String, String>() {
                    public String getMapValue(String value) {
                        return "Reflection_" + value + "Property_Property";
                    }}));

                setUserLoggableProperties(sql);

                reparse.set(null);
                useReparse = false;
            }

            startLogger.info("Setting user not null constraints for properties");
            setNotNullProperties(sql);

            startLogger.info("Setting user notifications for property changes");
            setupPropertyNotifications(sql);
            
        } catch (Exception ignored) {
            ignored = ignored;
        }

    }

    public Integer readCurrentUser() {
        try {
            return (Integer) authenticationLM.currentUser.read(getDbManager().createSession());
        } catch (Exception e) {
            return null;
        }
    }

    // временный хак для перехода на явную типизацию
    public static boolean useReparse = false;
    public static final ThreadLocal<ImMap<String, String>> reparse = new ThreadLocal<>();

    private void setUserLoggableProperties(SQLSession sql) throws SQLException, IllegalAccessException, InstantiationException, ClassNotFoundException, SQLHandledException {
        Map<String, String> changes = getDbManager().getPropertyCNChanges(sql);
        
        Integer maxStatsProperty = null;
        try {
            maxStatsProperty = (Integer) reflectionLM.maxStatsProperty.read(sql, Property.defaultModifier, DataSession.emptyEnv(OperationOwner.unknown));
        } catch (Exception ignored) {
        }

        LCP<PropertyInterface> isProperty = LM.is(reflectionLM.property);
        ImRevMap<PropertyInterface, KeyExpr> keys = isProperty.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<>(keys);
        query.addProperty("CNProperty", reflectionLM.canonicalNameProperty.getExpr(key));
        query.addProperty("overStatsProperty", reflectionLM.overStatsProperty.getExpr(key));
        query.and(reflectionLM.userLoggableProperty.getExpr(key).getWhere());
        ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = query.execute(sql, OperationOwner.unknown);

        for (ImMap<Object, Object> values : result.valueIt()) {
            String canonicalName = values.get("CNProperty").toString().trim();
            if (changes.containsKey(canonicalName)) {
                canonicalName = changes.get(canonicalName);
            }
            LP lp = findProperty(canonicalName);
            Integer statsProperty = (Integer) values.get("overStatsProperty");
            statsProperty = statsProperty == null && lp != null ? getStatsProperty(lp.property) : statsProperty;
            if((statsProperty == null || maxStatsProperty == null || statsProperty < maxStatsProperty) && lp instanceof LCP) {
                LM.makeUserLoggable(systemEventsLM, (LCP) lp);
            }
        }
    }

    public Integer getStatsProperty (Property property) {
        Integer statsProperty = null;
        if (property instanceof AggregateProperty) {
            StatKeys classStats = ((AggregateProperty) property).getInterfaceClassStats();
            if (classStats != null && classStats.rows != null)
                statsProperty = classStats.rows.getCount();
        }
        return statsProperty;
    }

    private void setNotNullProperties(SQLSession sql) throws SQLException, IllegalAccessException, InstantiationException, ClassNotFoundException, SQLHandledException {
        
        LCP isProperty = LM.is(reflectionLM.property);
        ImRevMap<Object, KeyExpr> keys = isProperty.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
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
        QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
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

            QueryBuilder<String, String> query2 = new QueryBuilder<>(newKeys2);
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


    public void initStats() {
        try {
            SQLSession sql = getDbManager().getThreadLocalSql();

            updateStats(sql);
        } catch (Exception ignored) {
        }

        initClassStat();
    }

    public void initClassStat() { // по аналогии с initStats
        try {
            SQLSession sql = getDbManager().getThreadLocalSql();

            updateClassStat(sql, true);
        } catch (Exception ignored) {
            ignored = ignored;
        }
    }

    public void updateClassStat(SQLSession session, boolean useSIDs) throws SQLException, SQLHandledException {

        Map<Integer, Integer> customObjectClassMap = new HashMap<>();
        Map<String, Integer> customSIDObjectClassMap = new HashMap<>();

        KeyExpr customObjectClassExpr = new KeyExpr("customObjectClass");
        ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object)"innerInvoice", customObjectClassExpr);

        QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
        query.addProperty("statCustomObjectClass", LM.statCustomObjectClass.getExpr(customObjectClassExpr));
        if(useSIDs) {
            query.addProperty("staticName", LM.staticName.getExpr(customObjectClassExpr));
        }

        query.and(LM.statCustomObjectClass.getExpr(customObjectClassExpr).getWhere());

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session, OperationOwner.unknown);

        for (int i=0,size=result.size();i<size;i++) {
            Integer statCustomObjectClass = (Integer) result.getValue(i).get("statCustomObjectClass");
            if(useSIDs) {
                String sID = (String)result.getValue(i).get("staticName");
                if(sID != null)
                    customSIDObjectClassMap.put(sID.trim(), statCustomObjectClass);
            } else {
                customObjectClassMap.put((Integer) result.getKey(i).getValue(0), statCustomObjectClass);
            }
        }

        for(CustomClass customClass : LM.baseClass.getAllClasses()) {
            if(customClass instanceof ConcreteCustomClass) {
                Integer stat;
                if(useSIDs) {
                    assert customClass.ID == null;
                    stat = customSIDObjectClassMap.get(customClass.getSID());
                } else
                    stat = customObjectClassMap.get(customClass.ID);
                ((ConcreteCustomClass) customClass).stat = stat == null ? 1 : stat;
            }
        }
    }

    public ImMap<String, Integer> updateStats(SQLSession sql) throws SQLException, SQLHandledException {
        ImMap<String, Integer> result = updateStats(sql, true); // чтобы сами таблицы статистики получили статистику
        return SystemProperties.doNotCalculateStats ? result : updateStats(sql, false);
    }

    public ImMap<String, Integer> updateStats(SQLSession sql, boolean statDefault) throws SQLException, SQLHandledException {
        ImMap<String, Integer> tableStats;
        ImMap<String, Integer> keyStats;
        ImMap<String, Pair<Integer, Integer>> propStats;
        if(statDefault) {
            tableStats = MapFact.EMPTY();
            keyStats = MapFact.EMPTY();
            propStats = MapFact.EMPTY();
        } else {
            tableStats = readStatsFromDB(sql, reflectionLM.tableSID, reflectionLM.rowsTable, null);
            keyStats = readStatsFromDB(sql, reflectionLM.tableKeySID, reflectionLM.quantityTableKey, null);
            propStats = readStatsFromDB(sql, reflectionLM.tableColumnLongSID, reflectionLM.quantityTableColumn, reflectionLM.notNullQuantityTableColumn);
        }

        for (ImplementTable dataTable : LM.tableFactory.getImplementTables()) {
            dataTable.updateStat(tableStats, keyStats, propStats, statDefault, null);
        }
        return tableStats;
    }

    public <V> ImMap<String, V> readStatsFromDB(SQLSession sql, LCP sIDProp, LCP statsProp, final LCP notNullProp) throws SQLException, SQLHandledException {
        QueryBuilder<String, String> query = new QueryBuilder<>(SetFact.toSet("key"));
        Expr sidToObject = sIDProp.getExpr(query.getMapExprs().singleValue());
        query.and(sidToObject.getWhere());
        query.addProperty("property", statsProp.getExpr(sidToObject));
        if(notNullProp!=null)
            query.addProperty("notNull", notNullProp.getExpr(sidToObject));
        return query.execute(sql, OperationOwner.unknown).getMap().mapKeyValues(new GetValue<String, ImMap<String, Object>>() {
            public String getMapValue(ImMap<String, Object> key) {
                return ((String) key.singleValue()).trim();
            }}, new GetValue<V, ImMap<String, Object>>() {
            public V getMapValue(ImMap<String, Object> value) {
                if(notNullProp!=null) {
                    return (V) new Pair<>((Integer) value.get("property"), (Integer) value.get("notNull"));
                } else
                    return (V)value.singleValue();
            }});
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
            property.setEditAction(property.getSID(), formActionProperty.getImplement(property.getReflectionOrderInterfaces()));
            formActionProperty.checkReadOnly = false;
        }
    }

    @NFLazy
    public void setupPropertyPolicyForms(LAP<?> setupPolicyForPropByCN, Property property) {
        if (property.isNamed()) {
            String propertyCN = property.getCanonicalName();
            
            // todo [dale]: тут есть потенциальное пересечение канонических имен, так как приходится разделять эти свойства только по имени
            // и имя приходится создавать из канонического имени базового свойства, заменив спецсимволы на подчеркивания
            String setupPolicyActionName = PropertyCanonicalNameUtils.policyPropPrefix + PropertyCanonicalNameUtils.makeSafeName(propertyCN); 
            LAP<?> setupPolicyLAP = LM.addJoinAProp(LM.propertyPolicyGroup, getString("logics.property.propertypolicy.action"),
                    setupPolicyForPropByCN, LM.addCProp(StringClass.get(propertyCN.length()), propertyCN));
            
            ActionProperty setupPolicyAction = setupPolicyLAP.property;
            LM.makePropertyPublic(setupPolicyLAP, setupPolicyActionName, new ArrayList<ResolveClassSet>());
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
        List<LP<?, ?>> namedProperties = new ArrayList<>();
        for (LogicsModule module : logicModules) {
            namedProperties.addAll(module.getNamedProperties());
        }
        return namedProperties;
    }
    
    // todo [dale]: Временно сделал public для переименования log-свойств
    public static class NamedDecl {
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
        Map<String, List<NamedDecl>> result = new HashMap<>();
        for (Map.Entry<String, List<LogicsModule>> namespaceToModule : namespaceToModules.entrySet()) {
            String namespace = namespaceToModule.getKey();
            for(LogicsModule module : namespaceToModule.getValue()) {
                for(Map.Entry<String, List<LP<?, ?>>> namedModuleProperty : module.getNamedModuleProperties().entrySet()) {
                    String propertyName = namedModuleProperty.getKey();
                    List<NamedDecl> resultProps = result.get(propertyName);
                    if(resultProps == null) {
                        resultProps = new ArrayList<>();
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
        return getPropertyListWithGraph(ApplyFilter.NO).first;
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
    private static HSet<Link> buildOrder(Property<?> property, MAddMap<Property, HSet<Link>> linksMap, List<Property> order, ImSet<Link> removedLinks, boolean include, ImSet<Property> component, boolean events, boolean recursive, boolean checkNotRecursive) {
        HSet<Link> linksIn = linksMap.get(property);
        if (linksIn == null) { // уже были, linksMap - одновременно используется и как пометки, и как список, и как обратный обход
            assert !(recursive && checkNotRecursive);
            linksIn = new HSet<>();
            linksMap.add(property, linksIn);

            ImSet<Link> links = property.getLinks(events);
            for (int i = 0,size = links.size(); i < size; i++) {
                Link link = links.get(i);
                if (!removedLinks.contains(link) && component.contains(link.to) == include)
                    buildOrder(link.to, linksMap, order, removedLinks, include, component, events, true, checkNotRecursive).add(link);
            }
            if(order != null)
                order.add(property);
        }
        return linksIn;
    }

    private static class PropComparator implements Comparator<Property> {

        private final boolean strictCompare;

        public PropComparator(boolean strictCompare) {
            this.strictCompare = strictCompare;
        }

        public int compare(Property o1, Property o2) {

            String c1 = o1.getCanonicalName();
            String c2 = o2.getCanonicalName();
            if(c1 == null && c2 == null) {
                return ActionProperty.compareChangeExtProps(o1, o2, strictCompare);
            }

            if(c1 == null)
                return 1;

            if(c2 == null)
                return -1;

            assert !(c1.equals(c2) && !BaseUtils.hashEquals(o1,o2) && !(o1 instanceof SessionDataProperty) && !(o2 instanceof SessionDataProperty));
            return c1.compareTo(c2);
        }
    }

    private final static Comparator<Property> strictComparator = new PropComparator(true);
    private final static Comparator<Property> comparator = new PropComparator(false);


    // ищем компоненту (нужно для детерминированности, иначе можно было бы с findMinCycle совместить)
    private static void findComponent(Property<?> property, MAddMap<Property, HSet<Link>> linksMap, HSet<Property> proceeded, HSet<Property> component, Result<Property> minProperty) {
        if (component.add(property))
            return;

        if(minProperty.result == null || strictComparator.compare(minProperty.result, property) > 0)
            minProperty.set(property);

        HSet<Link> linksIn = linksMap.get(property);
        for (int i = 0; i < linksIn.size; i++) {
            Link link = linksIn.get(i);
            if (!proceeded.contains(link.from)) { // если не в верхней компоненте
                findComponent(link.from, linksMap, proceeded, component, minProperty);
            }
        }
    }

    private static int compareCycles(List<Link> cycle1, List<Link> cycle2) {
        assert cycle1.size() == cycle2.size();
        for(int i=0,size=cycle1.size();i<size;i++) {
            Link link1 = cycle1.get(i);
            Link link2 = cycle2.get(i);

            int cmp = Integer.compare(link1.type.getNum(), link2.type.getNum());
            if(cmp != 0)
                return cmp;
            cmp = comparator.compare(link1.from, link2.from);
            if(cmp != 0)
                return cmp;
        }

        return strictComparator.compare(cycle1.get(0).from, cycle2.get(0).from);
    }

    private static List<Link> findMinCycle(Property<?> property, MAddMap<Property, HSet<Link>> linksMap, HSet<Property> component) {
        // поиск в ширину
        HSet<Property> inQueue = new HSet<>();
        Link[] queue = new Link[component.size()];
        Integer[] from = new Integer[component.size()];
        int left = -1; int right = 0;
        int sright = right;
        List<Link> minCycle = null;

        while(true) {
            Property current = left >= 0 ? queue[left].from : property;
            HSet<Link> linksIn = linksMap.get(current);
            for (int i = 0; i < linksIn.size; i++) {
                Link link = linksIn.get(i);

                if(BaseUtils.hashEquals(link.from, property)) { // нашли цикл
                    List<Link> cycle = new ArrayList<>();
                    cycle.add(link);

                    int ifrom = left;
                    while(ifrom != -1) {
                        cycle.add(queue[ifrom]);
                        ifrom = from[ifrom];
                    }

                    if(minCycle == null || compareCycles(minCycle, cycle) > 0) // для детерменированности
                        minCycle = cycle;
                }
                if (component.contains(link.from) && !inQueue.add(link.from)) { // если не в очереди
                    queue[right] = link;
                    from[right++] = left;
                }
            }
            left++;
            if(left == sright) { // новая длина пути
                if(minCycle != null)
                    return minCycle;
                sright = right;
            }
//            if(left == right)
//                break;
        }
    }

    private static Link getMinLink(List<Link> result) {

        // одновременно ведем минимум, путь от начала и link с максимальной длинной
        int firstMinIndex = 0;
        int lastMinIndex = 0;
        int bestMinIndex = 0;
        int maxPath = 0;
        for(int i=1;i<result.size();i++) {
            Link link = result.get(i);

            Link minLink = result.get(lastMinIndex);
            int num = link.type.getNum();
            int minNum = minLink.type.getNum();
            if (num > minNum) {
                firstMinIndex = lastMinIndex = bestMinIndex = i;
                maxPath = 0;
            } else if (num == minNum) { // выбираем с меньшей длиной пути
                int path = i - lastMinIndex;
                if(path > maxPath) { // тут тоже надо детерминировать, когда равны ? (хотя если сверху выставляем минверщину, то не надо)
                    maxPath = path;
                    bestMinIndex = i;
                }
                lastMinIndex = i;
            }
        }

        int roundPath = result.size() - lastMinIndex + firstMinIndex;
        if(roundPath > maxPath) { // замыкаем круг
            bestMinIndex = lastMinIndex;
        }

        return result.get(bestMinIndex);
    }

    // upComponent нужен так как изначально неизвестны все элементы
    private static HSet<Property> buildList(HSet<Property> props, HSet<Property> exclude, HSet<Link> removedLinks, MOrderExclSet<Property> mResult, boolean events) {
        HSet<Property> proceeded;

        List<Property> order = new ArrayList<>();
        MAddMap<Property, HSet<Link>> linksMap = MapFact.mAddOverrideMap();
        for (int i = 0; i < props.size; i++) {
            Property property = props.get(i);
            if (linksMap.get(property) == null) // проверка что не было
                buildOrder(property, linksMap, order, removedLinks, exclude == null, exclude != null ? exclude : props, events, false, false);
        }

        Result<Property> minProperty = new Result<>();
        proceeded = new HSet<>();
        for (int i = 0; i < order.size(); i++) { // тут нужн
            Property orderProperty = order.get(order.size() - 1 - i);
            if (!proceeded.contains(orderProperty)) {
                minProperty.set(null);
                HSet<Property> innerComponent = new HSet<>();
                findComponent(orderProperty, linksMap, proceeded, innerComponent, minProperty);
                assert innerComponent.size > 0;
                if (innerComponent.size == 1) // если цикла нет все ОК
                    mResult.exclAdd(innerComponent.single());
                else { // нашли цикл
                    // assert что minProperty один из ActionProperty.getChangeExtProps
                    List<Link> minCycle = findMinCycle(minProperty.result, linksMap, innerComponent);
                    assert BaseUtils.hashEquals(minCycle.get(0).from, minProperty.result) && BaseUtils.hashEquals(minCycle.get(minCycle.size()-1).to, minProperty.result);

                    Link minLink = getMinLink(minCycle);
                    removedLinks.exclAdd(minLink);

//                    printCycle("test", minLink, innerComponent, minCycle);
                    if (minLink.type.equals(LinkType.DEPEND)) { // нашли сильный цикл
                        MOrderExclSet<Property> mCycle = SetFact.mOrderExclSet();
                        buildList(innerComponent, null, removedLinks, mCycle, events);
                        ImOrderSet<Property> cycle = mCycle.immutableOrder();

                        String print = "";
                        for (Property property : cycle)
                            print = (print.length() == 0 ? "" : print + " -> ") + property.toString();
                        throw new RuntimeException(getString("message.cycle.detected") + " : " + print + " -> " + minLink.to);
                    }
                    buildList(innerComponent, null, removedLinks, mResult, events);
                }
                proceeded.exclAddAll(innerComponent);
            }
        }

        return proceeded;
    }

    private static void printCycle(String property, Link minLink, HSet<Property> innerComponent, List<Link> minCycle) {

        int showCycle = 0;

        for(Property prop : innerComponent) {
            if(prop.toString().contains(property))
                showCycle = 1;
        }

        for(Link link : minCycle) {
            if(link.from.toString().contains(property))
                showCycle = 2;
        }

        if(showCycle > 0) {
            String result = "";
            for(Link link : minCycle) {
                result += " " + link.from;
            }
            System.out.println(showCycle + " LEN " + minCycle.size() + " COMP " + innerComponent.size() + " MIN " + minLink.from + " " + result);
        }
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

        Stack<Link> forward = new Stack<>();
        if (findDependency(property1, property2, new HSet<Property>(), forward, desiredType))
            result += outDependency("FORWARD (" + forward.size() + ")", property1, forward) + '\n';

        Stack<Link> backward = new Stack<>();
        if (findDependency(property2, property1, new HSet<Property>(), backward, desiredType))
            result += outDependency("BACKWARD (" + backward.size() + ")", property2, backward) + '\n';

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
            logger.debug("Dependencies: " + show);
        }

        if(found)
            dropActionChangeProps();
    }

    @IdentityLazy
    public Graph<ActionProperty> getRecalculateFollowsGraph() {
        return BaseUtils.<Graph<ActionProperty>>immutableCast(getPropertyGraph().filterGraph(new SFunctionSet<Property>() {
            public boolean contains(Property element) {
                return element instanceof ActionProperty && ((ActionProperty) element).hasResolve();
            }
        }));
    }

    @IdentityLazy
    public Graph<AggregateProperty> getAggregateStoredGraph() {
        return BaseUtils.<Graph<AggregateProperty>>immutableCast(getPropertyGraph().filterGraph(new SFunctionSet<Property>() {
            public boolean contains(Property element) {
                return element instanceof AggregateProperty && ((AggregateProperty) element).isStored();
            }
        }));
    }

    public Graph<AggregateProperty> getRecalculateAggregateStoredGraph() {
        QueryBuilder<String, Object> query = new QueryBuilder<>(SetFact.singleton("key"));

        ImSet<String> skipProperties = SetFact.EMPTY();
        try (final DataSession dataSession = getDbManager().createSession()) {

            Expr expr = reflectionLM.notRecalculateSID.getExpr(query.getMapExprs().singleValue());
            query.and(expr.getWhere());
            skipProperties = query.execute(dataSession).keys().mapSetValues(new GetValue<String, ImMap<String,Object>>() {
                @Override
                public String getMapValue(ImMap<String, Object> value) {
                    return (String)value.singleValue();
                }
            });

        } catch (SQLException | SQLHandledException e) {
            serviceLogger.info(e.getMessage());
        }


        final ImSet<String> fSkipProperties = skipProperties;
        return getAggregateStoredGraph().filterGraph(new SFunctionSet<AggregateProperty>() {
            public boolean contains(AggregateProperty element) {
                return !fSkipProperties.contains(element.getDBName());
            }
        });
    }

    public Graph<Property> getPropertyGraph() {
        return getPropertyListWithGraph(ApplyFilter.NO).second;
    }

    @IdentityStrongLazy // глобальное очень сложное вычисление
    public Pair<ImOrderSet<Property>, Graph<Property>> getPropertyListWithGraph(ApplyFilter filter) {
        // жестковато тут конечно написано, но пока не сильно времени жрет

        fillActionChangeProps();

        // сначала бежим по Action'ам с cancel'ами
        HSet<Property> cancelActions = new HSet<>();
        HSet<Property> rest = new HSet<>();
        for (Property property : getOrderProperties())
            if(filter.contains(property)) {
                if (ApplyFilter.isCheck(property))
                    cancelActions.add(property);
                else
                    rest.add(property);
            }
        boolean events = filter != ApplyFilter.ONLY_DATA;

        MOrderExclSet<Property> mCancelResult = SetFact.mOrderExclSet();
        HSet<Link> firstRemoved = new HSet<>();
        HSet<Property> proceeded = buildList(cancelActions, new HSet<Property>(), firstRemoved, mCancelResult, events);
        ImOrderSet<Property> cancelResult = mCancelResult.immutableOrder();

        // потом бежим по всем остальным, за исключением proceeded
        MOrderExclSet<Property> mRestResult = SetFact.mOrderExclSet();
        HSet<Property> removed = new HSet<>();
        removed.addAll(rest.remove(proceeded));
        HSet<Link> secondRemoved = new HSet<>();
        buildList(removed, proceeded, secondRemoved, mRestResult, events); // потом этот cast уберем
        ImOrderSet<Property> restResult = mRestResult.immutableOrder();

        // затем по всем кроме proceeded на прошлом шаге
        assert cancelResult.getSet().disjoint(restResult.getSet());
        ImOrderSet<Property> result = cancelResult.reverseOrder().addOrderExcl(restResult.reverseOrder());

        Graph<Property> graph = null;
        if(filter == ApplyFilter.NO) {
            graph = buildGraph(result, firstRemoved.addExcl(secondRemoved));
        }

        for(Property property : result) {
            property.dropLinks();
            if(property instanceof CalcProperty)
                ((CalcProperty)property).dropActionChangeProps();
        }
        return new Pair<>(result, graph);
    }

    private static Graph<Property> buildGraph(ImOrderSet<Property> props, ImSet<Link> removedLinks) {
        MAddMap<Property, HSet<Link>> linksMap = MapFact.mAddOverrideMap();
        for (int i = 0, size = props.size(); i < size; i++) {
            Property property = props.get(i);
            if (linksMap.get(property) == null) // проверка что не было
                buildOrder(property, linksMap, null, removedLinks, true, props.getSet(), true, false, true);
        }

        MExclMap<Property, ImSet<Property>> mEdgesIn = MapFact.mExclMap(linksMap.size());
        for(int i=0,size=linksMap.size();i<size;i++) {
            final Property property = linksMap.getKey(i);
            HSet<Link> links = linksMap.getValue(i);
            mEdgesIn.exclAdd(property, links.mapSetValues(new GetValue<Property, Link>() {
                public Property getMapValue(Link value) {
                    assert BaseUtils.hashEquals(value.to, property);
                    return value.from;
                }
            }));
        }
        return new Graph<>(mEdgesIn.immutable());
    }

    public AggregateProperty getAggregateStoredProperty(String propertyCanonicalName) {
        for (Property property : getStoredProperties()) {
            if (property instanceof AggregateProperty && propertyCanonicalName.equals(property.getCanonicalName()))
                return (AggregateProperty) property;
        }
        return null;
    }

    // используется не в task'ах
    public List<CalcProperty> getAggregateStoredProperties(boolean ignoreCheck) {
        List<CalcProperty> result = new ArrayList<>();
        try (final DataSession dataSession = getDbManager().createSession()) {
            for (Property property : getStoredProperties())
                if (property instanceof AggregateProperty) {
                    boolean recalculate = ignoreCheck || reflectionLM.notRecalculateTableColumn.read(dataSession, reflectionLM.tableColumnSID.readClasses(dataSession, new DataObject(property.getDBName()))) == null;
                    if(recalculate)
                        result.add((AggregateProperty) property);
                }
        } catch (SQLException | SQLHandledException e) {
            serviceLogger.info(e.getMessage());
        }
        return result;
    }

    public ImOrderSet<CalcProperty> getStoredDataProperties() {
        try (final DataSession dataSession = getDbManager().createSession()) {
            return BaseUtils.immutableCast(getStoredProperties().filterOrder(new SFunctionSet<CalcProperty>() {
                public boolean contains(CalcProperty property) {
                    boolean recalculate = true;
                    try {
                        recalculate = reflectionLM.notRecalculateTableColumn.read(dataSession, reflectionLM.tableColumnSID.readClasses(dataSession, new DataObject(property.getDBName()))) == null;
                    } catch (SQLException | SQLHandledException e) {
                        serviceLogger.error(e.getMessage());
                    }
                    return recalculate && (property instanceof StoredDataProperty || property instanceof ClassDataProperty);
                }
            }));
        } catch (SQLException e) {
            serviceLogger.info(e.getMessage());
        }
        return SetFact.EMPTYORDER();
    }

    @IdentityLazy
    public ImOrderSet<CalcProperty> getStoredProperties() {
        return BaseUtils.immutableCast(getPropertyList().filterOrder(new SFunctionSet<Property>() {
            public boolean contains(Property property) {
                return property instanceof CalcProperty && ((CalcProperty) property).isStored();
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
        ImOrderSet<Property> list = getPropertyListWithGraph(increment).first;
        MOrderExclMap<Object, SessionEnvEvent> mResult = MapFact.mOrderExclMapMax(list.size());
        for (Property property : list) {
            if (property instanceof CalcProperty && ((CalcProperty) property).isStored())
                mResult.exclAdd(property, SessionEnvEvent.ALWAYS);
            SessionEnvEvent sessionEnv;
            if (property instanceof ActionProperty && (sessionEnv = ((ActionProperty) property).getSessionEnv(SystemEvent.APPLY))!=null)
                mResult.exclAdd(property, sessionEnv);
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
            if(property instanceof ActionPropertyValueImplement || property instanceof ActionProperty) {
                ActionProperty actionProperty = property instanceof ActionPropertyValueImplement ? ((ActionPropertyValueImplement) property).property : (ActionProperty)property;
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
        List<CalcProperty> result = new ArrayList<>();
        for (Property property : getPropertyList()) {
            if (property instanceof CalcProperty && ((CalcProperty) property).checkChange != CalcProperty.CheckType.CHECK_NO) {
                result.add((CalcProperty) property);
            }
        }
        return result;
    }

    public List<CalcProperty> getCheckConstrainedProperties(CalcProperty<?> changingProp) {
        List<CalcProperty> result = new ArrayList<>();
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
        int count = 0;
        ImSet<ImplementTable> tables = LM.tableFactory.getImplementTables();
        for (ImplementTable dataTable : tables) {
            count++;
            long start = System.currentTimeMillis();
            serviceLogger.info(String.format("Recalculate Stats %s of %s: %sms", count, tables.size(), String.valueOf(dataTable)));
            dataTable.calculateStat(this.reflectionLM, session);
            long time = System.currentTimeMillis() - start;
            serviceLogger.info(String.format("Recalculate Stats: %s, %sms", String.valueOf(dataTable), time));
        }
        recalculateClassStat(session);
    }

    public void overCalculateStats(DataSession session, Integer maxQuantityOverCalculate) throws SQLException, SQLHandledException {
        int count = 0;
        MSet<Integer> propertiesSet = getOverCalculatePropertiesSet(session, maxQuantityOverCalculate);
        ImSet<ImplementTable> tables = LM.tableFactory.getImplementTables();
        for (ImplementTable dataTable : tables) {
            count++;
            long start = System.currentTimeMillis();
            if(dataTable.overCalculateStat(this.reflectionLM, session, propertiesSet,
                    new ProgressBar("Recalculate Stats", count, tables.size(), String.format("Table: %s (%s of %s)", dataTable, count, tables.size())))) {
                long time = System.currentTimeMillis() - start;
                serviceLogger.info(String.format("Recalculate Stats: %s, %sms", String.valueOf(dataTable), time));
            }
        }
    }

    public MSet<Integer> getOverCalculatePropertiesSet(DataSession session, Integer maxQuantity) throws SQLException, SQLHandledException {
        KeyExpr propertyExpr = new KeyExpr("Property");
        ImRevMap<Object, KeyExpr> propertyKeys = MapFact.singletonRev((Object) "Property", propertyExpr);

        QueryBuilder<Object, Object> propertyQuery = new QueryBuilder<>(propertyKeys);
        propertyQuery.and(reflectionLM.canonicalNameProperty.getExpr(propertyExpr).getWhere());
        if(maxQuantity == null)
            propertyQuery.and(reflectionLM.notNullQuantityProperty.getExpr(propertyExpr).getWhere().not()); //null quantityProperty
        else
            propertyQuery.and(reflectionLM.notNullQuantityProperty.getExpr(propertyExpr).getWhere().not().or( //null quantityProperty
                    reflectionLM.notNullQuantityProperty.getExpr(propertyExpr).compare(new DataObject(maxQuantity).getExpr(), Compare.LESS_EQUALS))); //less or equals then maxQuantity

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> propertyResult = propertyQuery.execute(session);

        MSet<Integer> resultSet = SetFact.mSet();
        for (int i = 0, size = propertyResult.size(); i < size; i++) {
            resultSet.add((Integer) propertyResult.getKey(i).get("Property"));
        }
        return resultSet;
    }

    public void recalculateClassStat(DataSession session) throws SQLException, SQLHandledException {
        for (ObjectValueClassSet tableClasses : LM.baseClass.getUpObjectClassFields().valueIt()) {
            long start = System.currentTimeMillis();
            serviceLogger.info(String.format("Recalculate Stats: %s", String.valueOf(tableClasses)));
            QueryBuilder<Integer, Integer> classes = new QueryBuilder<>(SetFact.singleton(0));

            KeyExpr countKeyExpr = new KeyExpr("count");
            Expr countExpr = GroupExpr.create(MapFact.singleton(0, countKeyExpr.classExpr(LM.baseClass)),
                    new ValueExpr(1, IntegerClass.instance), countKeyExpr.isClass(tableClasses), GroupType.SUM, classes.getMapExprs());

            classes.addProperty(0, countExpr);
            classes.and(countExpr.getWhere());

            ImOrderMap<ImMap<Integer, Object>, ImMap<Integer, Object>> classStats = classes.execute(session);
            ImSet<ConcreteCustomClass> concreteChilds = tableClasses.getSetConcreteChildren();
            for (int i = 0, size = concreteChilds.size(); i < size; i++) {
                ConcreteCustomClass customClass = concreteChilds.get(i);
                ImMap<Integer, Object> classStat = classStats.get(MapFact.singleton(0, (Object) customClass.ID));
                LM.statCustomObjectClass.change(classStat == null ? 1 : (Integer) classStat.singleValue(), session, customClass.getClassObject());
            }
            long time = System.currentTimeMillis() - start;
            serviceLogger.info(String.format("Recalculate Stats: %s, %sms", String.valueOf(tableClasses), time));
        }
    }

    public String recalculateFollows(SessionCreator creator, boolean isolatedTransaction, final ExecutionStack stack) throws SQLException, SQLHandledException {
        final List<String> messageList = new ArrayList<>();
        final long maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        for (Property property : getPropertyList())
            if (property instanceof ActionProperty) {
                final ActionProperty<?> action = (ActionProperty) property;
                if (action.hasResolve()) {
                    long start = System.currentTimeMillis();
                    try {
                        DBManager.runData(creator, isolatedTransaction, new DBManager.RunServiceData() {
                            public void run(SessionCreator session) throws SQLException, SQLHandledException {
                                ((DataSession) session).resolve(action, stack);
                            }
                        });
                    } catch (LogMessageLogicsException e) { // suppress'им так как понятная ошибка
                        serviceLogger.info(e.getMessage());
                    }
                    long time = System.currentTimeMillis() - start;
                    String message = String.format("Recalculate Follows: %s, %sms", property.getSID(), time);
                    serviceLogger.info(message);
                    if (time > maxRecalculateTime)
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
        for(ImplementTable implementTable : LM.tableFactory.getImplementTables()) {
            message += DataSession.checkTableClasses(implementTable, session, LM.baseClass, false); // так как снизу есть проверка классов
        }
        for (CalcProperty property : getStoredDataProperties())
            message += DataSession.checkClasses(property, session, LM.baseClass);
        return message;
    }

    public void checkIndices(SQLSession session) throws SQLException, SQLHandledException {
        session.startTransaction(DBManager.START_TIL, OperationOwner.unknown);
        try {
            for (Map.Entry<Table, Map<List<Field>, Boolean>> mapIndex : getDbManager().getIndicesMap().entrySet()) {
                Table table = mapIndex.getKey();
                for (Map.Entry<List<Field>, Boolean> index : mapIndex.getValue().entrySet()) {
                    ImOrderSet<Field> fields = SetFact.fromJavaOrderSet(index.getKey());
                    if (!getDbManager().getThreadLocalSql().checkIndex(table, table.keys, fields, index.getValue()))
                        session.addIndex(table, table.keys, fields, index.getValue(), sqlLogger);
                    session.addConstraint(table);
                }
                session.checkExtraIndices(getDbManager().getThreadLocalSql(), table, table.keys, sqlLogger);
            }
            session.commitTransaction();
        } catch (Exception e) {
            session.rollbackTransaction();
            throw e;
        }
    }

    public void recalculateExclusiveness(final SQLSession session, boolean isolatedTransactions) throws SQLException, SQLHandledException {
        DBManager.run(session, isolatedTransactions, new DBManager.RunService() {
            public void run(final SQLSession sql) throws SQLException, SQLHandledException {
                DataSession.runExclusiveness(new DataSession.RunExclusiveness() {
                    public void run(Query<String, String> query) throws SQLException, SQLHandledException {
                        SingleKeyTableUsage<String> table = new SingleKeyTableUsage<>(ObjectType.instance, SetFact.toOrderExclSet("sum", "agg"), new Type.Getter<String>() {
                            public Type getType(String key) {
                                return key.equals("sum") ? ValueExpr.COUNTCLASS : StringClass.getv(false, ExtInt.UNLIMITED);
                            }
                        });

                        table.writeRows(sql, query, LM.baseClass, DataSession.emptyEnv(OperationOwner.unknown), SessionTable.nonead);
                        
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

        final List<String> messageList = new ArrayList<>();
        final long maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        for (final ImplementTable implementTable : LM.tableFactory.getImplementTables()) {
            DBManager.run(session, isolatedTransactions, new DBManager.RunService() {
                public void run(SQLSession sql) throws SQLException, SQLHandledException {
                    long start = System.currentTimeMillis();
                    DataSession.recalculateTableClasses(implementTable, sql, LM.baseClass);
                    long time = System.currentTimeMillis() - start;
                    String message = String.format("Recalculate Table Classes: %s, %sms", implementTable.toString(), time);
                    serviceLogger.info(message);
                    if (time > maxRecalculateTime)
                        messageList.add(message);
                }
            });
        }

        for (final CalcProperty property : getStoredDataProperties())
            DBManager.run(session, isolatedTransactions, new DBManager.RunService() {
                public void run(SQLSession sql) throws SQLException, SQLHandledException {
                    long start = System.currentTimeMillis();
                    property.recalculateClasses(sql, LM.baseClass);
                    long time = System.currentTimeMillis() - start;
                    String message = String.format("Recalculate Class: %s, %sms", property.getSID(), time);
                    serviceLogger.info(message);
                    if (time > maxRecalculateTime)
                        messageList.add(message);
                }
            });
        return formatMessageList(messageList);
    }

    public LP findSafeProperty(String canonicalName) {
        LP lp = null;
        try {
            lp = findProperty(canonicalName);
        } catch (Exception e) {
        }
        return lp;
    }

    public LP[] findProperties(String... names) {
        LP[] result = new LP[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = findProperty(names[i]);
        }
        return result;
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
            classSets = getResolveList(classes);
        }
        return findProperty(namespace, name, classSets);
    }

    private LP findProperty(String namespace, String name, List<ResolveClassSet> classes) {
        assert namespaceToModules.get(namespace) != null;
        NamespacePropertyFinder finder = new NamespacePropertyFinder(new EqualLPModuleFinder(false), namespaceToModules.get(namespace));
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
            NamespaceElementFinder<R, P> finder = new NamespaceElementFinder<>(moduleFinder, namespaceToModules.get(namespaceName));
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
        ImOrderSet<CalcProperty> storedProperties = getStoredProperties();
        for (Map.Entry<ImplementTable, Collection<CalcProperty>> groupTable : BaseUtils.group(new BaseUtils.Group<ImplementTable, CalcProperty>() {
            public ImplementTable group(CalcProperty key) {
                return key.mapTable.table;
            }
        }, storedProperties).entrySet()) {
            result += groupTable.getKey().outputKeys() + '\n';
            for (CalcProperty property : groupTable.getValue())
                result += '\t' + property.outputStored(false) + '\n';
        }
        result += '\n' + getString("logics.info.by.properties") + '\n' + '\n';
        for (CalcProperty property : storedProperties)
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

    // в том числе и приватные
    public ImSet<NavigatorElement> getAllNavigatorElements() {
        MExclSet<NavigatorElement> mResult = SetFact.mExclSet();
        for(LogicsModule logicsModule : logicModules) {
            for(NavigatorElement entry : logicsModule.getAllModuleNavigators())
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
    private <C, P> LogicsModule getModuleContainingObject(String namespaceName, String name, P param, ModuleFinder<C, P> finder) {
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

    public DBManager getDbManager() {
        return ThreadLocalContext.getDbManager();
    }
    
    public String getDataBaseName() {
        return getDbManager().getDataBaseName();
    }

    private void updateThreadAllocatedBytesMap() {
        if(!Settings.get().isReadAllocatedBytes())
            return;

        final long period = Settings.get().getThreadAllocatedMemoryPeriod();
        final long maxAllocatedBytes = Settings.get().getMaxThreadAllocatedBytes();
        final int cacheMissesStatsLimit = Settings.get().getCacheMissesStatsLimit();

        ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
        if (tBean instanceof com.sun.management.ThreadMXBean && ((com.sun.management.ThreadMXBean) tBean).isThreadAllocatedMemorySupported()) {
            long time = System.currentTimeMillis();
            long bytesSum = 0;
            long totalBytesSum = 0;
            SQLSession.updateThreadAllocatedBytesMap();
            Map<Long, Thread> threadMap = ThreadUtils.getThreadMap();

            ConcurrentHashMap<Long, HashMap<CacheType, Long>> hitStats = MapFact.getGlobalConcurrentHashMap(CacheStats.getCacheHitStats());
            ConcurrentHashMap<Long, HashMap<CacheType, Long>> missedStats = MapFact.getGlobalConcurrentHashMap(CacheStats.getCacheMissedStats());
            CacheStats.resetStats();

            long totalHit = 0;
            long totalMissed = 0;
            HashMap<CacheType, Long> totalHitMap = new HashMap<>();
            HashMap<CacheType, Long> totalMissedMap = new HashMap<>();
            long exceededMisses = 0;
            long exceededMissesHits = 0;
            HashMap<CacheType, Long> exceededHitMap = new HashMap<>();
            HashMap<CacheType, Long> exceededMissedMap = new HashMap<>();

            boolean logTotal = false;

            for (Map.Entry<Long, Long> bEntry : SQLSession.threadAllocatedBytesBMap.entrySet()) {
                Long id = bEntry.getKey();
                if (id != null) {
                    Long bBytes = bEntry.getValue();
                    Long aBytes = SQLSession.threadAllocatedBytesAMap.get(bEntry.getKey());

                    Long deltaBytes = bBytes != null && aBytes != null ? (bBytes - aBytes) : 0;
                    totalBytesSum += deltaBytes;

                    long userMissed = 0;
                    long userHit = 0;

                    HashMap<CacheType, Long> userHitMap = hitStats.get(id) != null ? hitStats.get(id) : new HashMap<CacheType, Long>();
                    HashMap<CacheType, Long> userMissedMap = missedStats.get(id) != null ? missedStats.get(id) : new HashMap<CacheType, Long>();
                    for (CacheType cacheType : CacheType.values()) {
                        Long hit = nullToZero(userHitMap.get(cacheType));
                        Long missed = nullToZero(userMissedMap.get(cacheType));
                        userHit += hit;
                        userMissed += missed;
                    }
                    totalHit += userHit;
                    totalMissed += userMissed;
                    sumMap(totalHitMap, userHitMap);
                    sumMap(totalMissedMap, userMissedMap);

                    if (deltaBytes > maxAllocatedBytes || userMissed > cacheMissesStatsLimit) {
                        logTotal = true;

                        bytesSum += deltaBytes;

                        exceededMisses += userMissed;
                        exceededMissesHits += userHit;
                        sumMap(exceededHitMap, userHitMap);
                        sumMap(exceededMissedMap, userMissedMap);

                        Thread thread = threadMap.get(id);
                        LogInfo logInfo = thread == null ? null : ThreadLocalContext.logInfoMap.get(thread);
                        String computer = logInfo == null ? null : logInfo.hostnameComputer;
                        String user = logInfo == null ? null : logInfo.userName;

                        String userMessage;
                        if (user == null) {
                            userMessage = String.format("PID %s: %s", bEntry.getKey(), humanReadableByteCount(deltaBytes));
                        } else {
                            userMessage = String.format("PID %s, %s, Comp. %s, User %s", bEntry.getKey(),
                                    humanReadableByteCount(deltaBytes), computer == null ? "unknown" : computer, user);
                        }
                        userMessage += String.format(", missed-hit: All: %s-%s, %s", userMissed, userHit, getStringMap(userHitMap, userMissedMap));

                        ServerLoggers.allocatedBytesLogger.info(userMessage);
                    }
                }
            }
            if (logTotal) {
                ServerLoggers.allocatedBytesLogger.info(String.format("Exceeded: sum: %s, \t\t\tmissed-hit: All: %s-%s, %s",
                        humanReadableByteCount(bytesSum), exceededMisses, exceededMissesHits, getStringMap(exceededHitMap, exceededMissedMap)));
                ServerLoggers.allocatedBytesLogger.info(String.format("Total: sum: %s, elapsed %sms, missed-hit: All: %s-%s, %s",
                        humanReadableByteCount(totalBytesSum), System.currentTimeMillis() - time, totalMissed, totalHit, getStringMap(totalHitMap, totalMissedMap)));
            }
        }
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private String getStringMap(HashMap<CacheType, Long> hitStats, HashMap<CacheType, Long> missedStats) {
        String result = "";
        for (int i = 0; i < CacheType.values().length; i++) {
            CacheType type = CacheType.values()[i];
            result += type + ": " + nullToZero(missedStats.get(type)) + "-" + nullToZero(hitStats.get(type));
            if (i < CacheType.values().length - 1) {
                result += "; ";
            }
        }
        return result;
    }

    private void sumMap(HashMap<CacheType, Long> target, HashMap<CacheType, Long> source) {
        for (CacheType type : CacheType.values()) {
            target.put(type, nullToZero(target.get(type)) + nullToZero(source.get(type)));
        }
    }

    public List<Scheduler.SchedulerTask> getSystemTasks(Scheduler scheduler) {
        if(SystemProperties.isDebug) // чтобы не мешать при включенных breakPoint'ах
            return new ArrayList<>();

        List<Scheduler.SchedulerTask> result = new ArrayList<>();
        result.add(getOpenFormCountUpdateTask(scheduler));
        result.add(getUserLastActivityUpdateTask(scheduler));
        result.add(getInitPingInfoUpdateTask(scheduler));
        result.add(getAllocatedBytesUpdateTask(scheduler));
        result.add(getCleanTempTablesTask(scheduler));
        result.add(getRestartConnectionsTask(scheduler));
        return result;
    }

    private Scheduler.SchedulerTask getOpenFormCountUpdateTask(Scheduler scheduler) {
        return scheduler.createSystemTask(new EExecutionStackRunnable() {
            public void run(ExecutionStack stack) throws Exception {
                RemoteNavigator.updateOpenFormCount(BusinessLogics.this, stack);
            }
        }, false, Settings.get().getUpdateFormCountPeriod(), false, "Open Form Count");
    }

    private Scheduler.SchedulerTask getUserLastActivityUpdateTask(Scheduler scheduler) {
        return scheduler.createSystemTask(new EExecutionStackRunnable() {
            public void run(ExecutionStack stack) throws Exception {
                RemoteNavigator.updateUserLastActivity(BusinessLogics.this, stack);
            }
        }, false, Settings.get().getUpdateUserLastActivity(), false, "User Last Activity");
    }

    private Scheduler.SchedulerTask getInitPingInfoUpdateTask(Scheduler scheduler) {
        return scheduler.createSystemTask(new EExecutionStackRunnable() {
            public void run(ExecutionStack stack) throws Exception {
                RemoteNavigator.updatePingInfo(BusinessLogics.this, stack);
            }
        }, false, Settings.get().getUpdatePingInfo(), false, "Ping Info");
    }

    private Scheduler.SchedulerTask getCleanTempTablesTask(Scheduler scheduler) {
        return scheduler.createSystemTask(new EExecutionStackRunnable() {
            public void run(ExecutionStack stack) throws Exception {
                SQLSession.cleanTemporaryTables();
            }
        }, false, Settings.get().getTempTablesTimeThreshold(), false, "Drop Temp Tables");
    }

    private Scheduler.SchedulerTask getRestartConnectionsTask(Scheduler scheduler) {
        final Result<Double> prevStart = new Result<>(0.0);
        return scheduler.createSystemTask(new EExecutionStackRunnable() {
            public void run(ExecutionStack stack) throws Exception {
                SQLSession.restartConnections(prevStart);
            }
        }, false, Settings.get().getPeriodRestartConnections(), false, "Connection restart");
    }

    private Scheduler.SchedulerTask getAllocatedBytesUpdateTask(Scheduler scheduler) {
        return scheduler.createSystemTask(new EExecutionStackRunnable() {
            public void run(ExecutionStack stack) throws Exception {
                updateThreadAllocatedBytesMap();
            }
        }, false, Settings.get().getThreadAllocatedMemoryPeriod() / 2, false, "Allocated Bytes");
    }
}
