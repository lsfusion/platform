package lsfusion.server.logics;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.ReflectionUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.implementations.HSet;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSASVSMap;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.log.DebugInfoWriter;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.connection.TFormats;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.ResourceUtils;
import lsfusion.server.base.caches.CacheStats;
import lsfusion.server.base.caches.CacheStats.CacheType;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.controller.lifecycle.LifecycleAdapter;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.base.exception.ApplyCanceledException;
import lsfusion.server.base.task.PublicTask;
import lsfusion.server.base.task.TaskRunner;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.metacode.MetaCodeFragment;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.ApplyFilter;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.Correlation;
import lsfusion.server.logics.action.session.change.StructChanges;
import lsfusion.server.logics.action.session.changed.ChangedProperty;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.action.session.controller.init.SessionCreator;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.utils.time.TimeLogicsModule;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.event.*;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.group.AbstractNode;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.controller.remote.RemoteNavigator;
import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.caches.MapCacheAspect;
import lsfusion.server.logics.property.cases.AbstractCase;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.classes.user.ClassDataProperty;
import lsfusion.server.logics.property.classes.user.ObjectClassProperty;
import lsfusion.server.logics.property.data.AbstractDataProperty;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.activity.UserEventsLogicsModule;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.admin.authentication.security.SecurityLogicsModule;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.LogInfo;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.monitor.StatusMessage;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.admin.scheduler.SchedulerLogicsModule;
import lsfusion.server.physics.admin.scheduler.controller.manager.Scheduler;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.DefaultLocalizer;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.i18n.ResourceBundleGenerator;
import lsfusion.server.physics.dev.i18n.ReversedI18NDictionary;
import lsfusion.server.physics.dev.icon.IconLogicsModule;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;
import lsfusion.server.physics.dev.id.name.DBNamingPolicy;
import lsfusion.server.physics.dev.id.name.DuplicateElementsChecker;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameUtils;
import lsfusion.server.physics.dev.id.resolve.*;
import lsfusion.server.physics.dev.integration.external.to.file.ClearCacheWatcher;
import lsfusion.server.physics.dev.integration.external.to.file.SynchronizeSourcesWatcher;
import lsfusion.server.physics.dev.integration.external.to.mail.EmailLogicsModule;
import lsfusion.server.physics.dev.module.ModuleList;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static lsfusion.base.BaseUtils.*;
import static lsfusion.server.physics.admin.log.ServerLoggers.runWithStartLog;
import static lsfusion.server.physics.admin.log.ServerLoggers.startLog;
import static lsfusion.server.physics.dev.id.resolve.BusinessLogicsResolvingUtils.findElementByCanonicalName;
import static lsfusion.server.physics.dev.id.resolve.BusinessLogicsResolvingUtils.findElementByCompoundName;

public abstract class BusinessLogics extends LifecycleAdapter implements InitializingBean, SessionEvents {
    protected final static Logger logger = ServerLoggers.systemLogger;
    protected final static Logger allocatedBytesLogger = ServerLoggers.allocatedBytesLogger;

    public static final String systemScriptsPath = "/system/*.lsf";

    private ModuleList modules = new ModuleList();
    
    private Map<String, List<LogicsModule>> namespaceToModules = new HashMap<>();

    private final Map<Long, Integer> excessAllocatedBytesMap = new HashMap<>();

    public BaseLogicsModule LM;
    public ServiceLogicsModule serviceLM;
    public ReflectionLogicsModule reflectionLM;
    public AuthenticationLogicsModule authenticationLM;
    public SecurityLogicsModule securityLM;
    public SystemEventsLogicsModule systemEventsLM;
    public EmailLogicsModule emailLM;
    public IconLogicsModule iconLM;
    public SchedulerLogicsModule schedulerLM;
    public TimeLogicsModule timeLM;
    public UtilsLogicsModule utilsLM;
    public UserEventsLogicsModule userEventsLM;

    public String topModule;

    public String theme;
    public String size;
    public String navbar;
    
    public Boolean navigatorPinMode;

    public String logicsCaption;

    private String orderDependencies;

    private String pdfEncoding;

    private String lsfStrLiteralsLanguage;
    private String lsfStrLiteralsCountry;

    private String searchLanguage;

    private String setTimezone;
    private String setLanguage;
    private String setCountry;

    private Integer twoDigitYearStart;
    private String dateFormat;
    private String timeFormat;

    public TFormats tFormats;

    private LocalizedString.Localizer localizer;
    
    private PublicTask initTask;

    //чтобы можно было использовать один инстанс логики с несколькими инстансами, при этом инициализировать только один раз
    private final AtomicBoolean initialized = new AtomicBoolean();

    public BusinessLogics() {
        super(LOGICS_ORDER);
    }

    public LA<?> evaluateRun(String script, boolean action) {
        return LM.evaluateRun(script, null, action).first;
    }

    public void setTopModule(String topModule) {
        this.topModule = topModule;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setNavbar(String navbar) {
        this.navbar = navbar;
    }
    
    public void setNavigatorPinMode(Boolean navigatorPinMode) {
        this.navigatorPinMode = navigatorPinMode;
    }

    public void setLogicsCaption(String logicsCaption) {
        this.logicsCaption = logicsCaption;
    }

    public void setOrderDependencies(String orderDependencies) {
        this.orderDependencies = orderDependencies;
    }

    public void setSetTimezone(String setTimezone) {
        this.setTimezone = setTimezone;
    }

    public void setSetLanguage(String setLanguage) {
        this.setLanguage = setLanguage;
    }

    public void setSetCountry(String setCountry) {
        this.setCountry = setCountry;
    }

    public void setInitTask(PublicTask initTask) {
        this.initTask = initTask;
    }

    public void setTwoDigitYearStart(Integer twoDigitYearStart) {
        this.twoDigitYearStart = twoDigitYearStart;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(initTask, "initTask must be specified");
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        if (initialized.compareAndSet(false, true)) {
            startLog("Initializing BusinessLogics");
            try {
                getDbManager().ensureLogLevel();
                
                if(setLanguage != null) {
                    Locale.setDefault(LocalePreferences.getLocale(setLanguage, setCountry));
                }

                if(searchLanguage == null) {
                    searchLanguage = getHeuristicSearchLanguage();
                }

                TimeZone timeZone = setTimezone == null ? null : TimeZone.getTimeZone(setTimezone);
                if (timeZone != null) {
                    TimeZone.setDefault(timeZone);
                }

                tFormats = new TFormats(twoDigitYearStart,
                        dateFormat != null ? dateFormat : BaseUtils.getDatePattern(),
                        timeFormat != null ? timeFormat : BaseUtils.getTimePattern(), timeZone);

                if(pdfEncoding == null) {
                    pdfEncoding = getHeuristicPdfEncoding();
                }
                if(pdfEncoding != null) {
                    JRPropertiesUtil.getInstance(DefaultJasperReportsContext.getInstance()).setProperty("net.sf.jasperreports.default.pdf.encoding", pdfEncoding);
                }

                new TaskRunner(this).runTask(initTask);
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException("Error initializing BusinessLogics: ", e);
            }
        }
    }

    private static Map<String, String> languagesMap = new HashMap<String, String>() {{
        put("fr", "french"); put("de", "german"); put("it", "italian");
        put("ru", "russian"); put("pt", "portuguese"); put("es", "spanish");
    }};
    private String getHeuristicSearchLanguage() {
        return languagesMap.getOrDefault(Locale.getDefault().getLanguage(), "english");
    }

    private static Map<String, String> pdfEncodingsMap = new HashMap<String, String>() {{
        put("pl", "cp1250"); put("cz", "cp1250"); put("sk", "cp1250");put("sl", "cp1250"); put("hu", "cp1250"); put("sq", "cp1250");
        put("be", "cp1251"); put("ru", "cp1251"); put("uk", "cp1251"); put("uz", "1254");
    }};
    private String getHeuristicPdfEncoding() {
        return pdfEncodingsMap.get(Locale.getDefault().getLanguage());
    }

    public LRUWSASVSMap<Object, Method, Object, Object> startLruCache = new LRUWSASVSMap<>(LRUUtil.G2);
    public void cleanCaches() {
        runWithStartLog(() -> {
            startLruCache = null;
            MapCacheAspect.cleanClassCaches();
            AbstractNode.cleanPropCaches();
            cleanPropCaches();
        }, "Cleaning obsolete caches");

    }
    
    public ScriptingLogicsModule getModule(String name) {
        return (ScriptingLogicsModule) getSysModule(name);
    }
    
    public LogicsModule getSysModule(String name) {
        return modules.get(name);
    }

    protected <M extends LogicsModule> M addModule(M module) {
        modules.add(module);
        return module;
    }

    public void createModules() throws IOException {
        LM = addModule(new BaseLogicsModule(this));
        serviceLM = addModule(new ServiceLogicsModule(this, LM));
        reflectionLM = addModule(new ReflectionLogicsModule(this, LM));
        authenticationLM = addModule(new AuthenticationLogicsModule(this, LM));
        securityLM = addModule(new SecurityLogicsModule(this, LM));
        systemEventsLM = addModule(new SystemEventsLogicsModule(this, LM));
        emailLM = addModule(new EmailLogicsModule(this, LM));
        iconLM = addModule(new IconLogicsModule(this, LM));
        schedulerLM = addModule(new SchedulerLogicsModule(this, LM));
        timeLM = addModule(new TimeLogicsModule(this, LM));
        utilsLM = addModule(new UtilsLogicsModule(this, LM));
        userEventsLM = addModule(new UserEventsLogicsModule(this, LM));
    }

    protected void addModulesFromResource(List<String> includePaths, List<String> excludePaths) throws IOException {
        Set<String> includedLSF = findLSFFiles(includePaths);
        Set<String> excludedLSF = findLSFFiles(excludePaths);
        Set<String> systemLSF = findLSFFiles(Collections.singletonList(systemScriptsPath));
        
        includedLSF.removeAll(excludedLSF);
        includedLSF.removeAll(systemLSF);
        
        for (String filePath : includedLSF) {
            addModuleFromResource(filePath);
        }
    }

    private Set<String> findLSFFiles(List<String> paths) {
        List<Pattern> patterns = new ArrayList<>();
        for (String filePath : paths) {
            String pathRegex = convertPathToRegex(prependPathWithSlash(filePath));
            patterns.add(Pattern.compile(pathRegex));
        }

        Collection<String> list = ResourceUtils.getResources(patterns);
            
        Set<String> resFiles = new HashSet<>();
        for (String filename : list) {
            if (isLSFFile(filename)) {
                resFiles.add(filename);
            }
        }
        return resFiles;
    }

    private static String convertPathToRegex(String path) {
        String[] parts = path.split("[*]", -1);
        for (int i = 0; i < parts.length; ++i) {
            parts[i] = Pattern.quote(parts[i]);
        }
        return StringUtils.join(parts, ".*");
    }

    private static String prependPathWithSlash(String path) {
        if (!path.startsWith("/")) {
            return "/" + path;
        } else {
            return path;
        }
    }

    private static boolean isLSFFile(String fileName) {
        return fileName.endsWith(".lsf");
    }
    
    private void addModuleFromResource(String path) throws IOException {
        addModule(new ScriptingLogicsModule(LM, this, path));
    }
    
    public void initObjectClass() {
        LM.baseClass.initObjectClass(LM.getVersion(), CanonicalNameUtils.createCanonicalName(LM.getNamespace(), "CustomObjectClass"));
        LM.storeCustomClass(LM.baseClass.objectClass);
    }

    public void initLocalizer() {
        localizer = new DefaultLocalizer();
    }
    
    public LocalizedString.Localizer getLocalizer() {
        return localizer;
    }

    public void initModuleOrders() {
        modules.fillNameToModules();
        if (!isRedundantString(topModule)) {
            modules.filterWithTopModule(topModule);
        }
        modules.setOrderDependencies(orderDependencies);
        modules.orderModules();

        fillNamespaceToModules();
        fillModulesVisibleAndOrder();
    }

    private void fillNamespaceToModules() {
        for (LogicsModule module : modules.all()) {
            String namespace = module.getNamespace();
            if (!namespaceToModules.containsKey(namespace)) {
                namespaceToModules.put(namespace, new ArrayList<>());
            }
            namespaceToModules.get(namespace).add(module);
        }
    }
    
    private void fillModulesVisibleAndOrder() {
        Map<LogicsModule, ImSet<LogicsModule>> recRequiredModules = new HashMap<>();
        
        for (LogicsModule module : modules.all()) {
            MSet<LogicsModule> mRecDep = SetFact.mSet();
            mRecDep.add(module);
            for (String requiredName : module.getRequiredNames())
                mRecDep.addAll(recRequiredModules.get(modules.get(requiredName)));
            recRequiredModules.put(module, mRecDep.immutable());
        }

        int moduleNumber = 0;
        for (LogicsModule module : modules.all()) {
            module.visible = recRequiredModules.get(module).mapSetValues(LogicsModule::getVersion);
            module.order = (moduleNumber++);
        }
    }
    
    public void initFullSingleTables(DBNamingPolicy namingPolicy) {
        for(ImplementTable table : LM.tableFactory.getImplementTables()) {
            if(table.markedFull && !table.isFull())
                LM.markFull(table, table.getOrderMapFields().valuesList(), namingPolicy);
        }
    }

    private boolean needIndex(ObjectValueClassSet classSet) {
        ImSet<ConcreteCustomClass> set = classSet.getSetConcreteChildren();
        if(set.size() > 1) { // оптимизация
//            int count = classSet.getCount(); // it's dangerous because if updateStats fails for some reason, then server starts dropping large indices 
//            if(count >= Settings.get().getMinClassDataIndexCount()) {
//                Stat totStat = new Stat(count);
//                for (ConcreteCustomClass customClass : set)
//                    if (new Stat(customClass.getCount()).less(totStat))
                        return true;
//            }
        }
        return false;
    }

    public void initClassDataProps(final DBNamingPolicy namingPolicy) {
        ImMap<ImplementTable, ImSet<ConcreteCustomClass>> groupTables = getConcreteCustomClasses().group(customClass ->
                LM.tableFactory.getClassMapTable(MapFact.singletonOrder("key", customClass), namingPolicy).table);

        for(int i=0,size=groupTables.size();i<size;i++)
            LM.markClassData(groupTables.getKey(i), groupTables.getValue(i), namingPolicy);
    }

    // если добавлять CONSTRAINT SETCHANGED не забыть задание в графе запусков перетащить
    public void initClassAggrProps() {
        MOrderExclSet<Property> queue = SetFact.mOrderExclSet();

        for(Property property : getProperties()) {
            if(property.isAggr())
                queue.exclAdd(property);
        }

        MAddExclMap<CustomClass, MSet<Property>> classAggrProps = MapFact.mAddExclMap();
            
        for(int i=0,size=queue.size();i<size;i++) {
            Property<?> property = queue.get(i);
            ImMap<?, ValueClass> interfaceClasses = property.getInterfaceClasses(ClassType.materializeChangePolicy);
            if(interfaceClasses.size() == 1) {
                ValueClass valueClass = interfaceClasses.singleValue();
                if(valueClass instanceof CustomClass) {
                    CustomClass customClass = (CustomClass) valueClass;
                    MSet<Property> mAggrProps = classAggrProps.get(customClass);
                    if(mAggrProps == null) {
                        mAggrProps = SetFact.mSet();
                        classAggrProps.exclAdd(customClass, mAggrProps);
                    }
                    mAggrProps.add(property);
                }
            }
           
            // все implement'ы тоже помечаем как aggr
            for(Property implement : property.getImplements())
                if(!queue.contains(implement)) {
                    queue.exclAdd(implement);
                    size++;
                }
        }

        for(int i=0,size=classAggrProps.size();i<size;i++)
            classAggrProps.getKey(i).aggrProps = classAggrProps.getValue(i).immutable();
    }

    public void initClassDataIndices(DBManager dbManager) {
        for(ObjectClassField classField : LM.baseClass.getUpObjectClassFields().keyIt()) {
            ClassDataProperty classProperty = classField.getProperty();
            if(needIndex(classProperty.set))
                dbManager.addIndex(classProperty);
        }
    }

    public <P extends PropertyInterface> void finishLogInit(Property<P> property) {
        if (property.isLoggable()) {
            ActionMapImplement<?, P> logAction = property.getLogFormAction();

            //добавляем в контекстное меню пункт для показа формы
            property.setContextMenuAction(property.getSID(), logAction.action.caption);
            property.setEventAction(property.getSID(), logAction);
        }
    }

    @NFLazy
    public void setupPropertyPolicyForms(LA<?> setupPolicyForPropByCN, ActionOrProperty property, boolean actions) {
        if (property.isNamed()) {
            String propertyCN = property.getCanonicalName();
            
            // issue #47 Потенциальное совпадение канонических имен различных свойств
            // Приходится разделять эти свойства только по имени, а имя приходится создавать из канонического имени 
            // базового свойства, заменив спецсимволы на подчеркивания
            String setupPolicyActionName = (actions ? PropertyCanonicalNameUtils.policyPropPrefix : PropertyCanonicalNameUtils.policyActionPrefix) + PropertyCanonicalNameUtils.makeSafeName(propertyCN); 
            LA<?> setupPolicyLA = LM.addJoinAProp(LM.propertyPolicyGroup, LocalizedString.create("{logics.property.propertypolicy.action}"),
                    setupPolicyForPropByCN, LM.addCProp(StringClass.get(propertyCN.length()), LocalizedString.create(propertyCN, false)));
            
            Action setupPolicyAction = setupPolicyLA.action;
            LM.makeActionPublic(setupPolicyLA, setupPolicyActionName, new ArrayList<>());
            property.setContextMenuAction(setupPolicyAction.getSID(), setupPolicyAction.caption);
            property.setEventAction(setupPolicyAction.getSID(), setupPolicyAction.getImplement());
        }
    }

    public void prereadCaches() {
        getApplyEvents(ApplyFilter.ONLYCHECK);
        getApplyEvents(ApplyFilter.NO);
        if(Settings.get().isEnableApplySingleStored()) {
            getOrderMapSingleApplyDepends(ApplyFilter.NO, false);
            if(!Settings.get().isDisableCorrelations())
                getOrderMapSingleApplyDepends(ApplyFilter.NO, true);
        }
    }

    public void initAuthentication(SecurityManager securityManager) throws SQLException, SQLHandledException {
        securityManager.initUsers();
        securityManager.initSecret();
    }

    public void finalizeGroups() {
        LM.getRootGroup().finalizeAroundInit();
    }

    public void cleanPropCaches() {
        LM.getRootGroup().cleanAllActionOrPropertiesCaches();
    }

    // the problem is in the changeChildrenToSimple method, when some props are added to that groups during the runtime
    // actually most of the getOrderActionOrProperties calls filter action / props immediately, so the caching can be more sophisticated
    // but right now we just do pretty simple memoization with the manual update
    public ImOrderSet<ActionOrProperty> getOrderActionOrProperties() {
        return LM.getRootGroup().getActionOrProperties();
    }

    public ImOrderSet<Property> getOrderProperties() {
        return BaseUtils.immutableCast(getOrderActionOrProperties().filterOrder(element -> element instanceof Property));
    }

    public ImSet<Property> getProperties() {
        return getOrderProperties().getSet();
    }

    public ImOrderSet<Action> getOrderActions() {
        return BaseUtils.immutableCast(getOrderActionOrProperties().filterOrder(element -> element instanceof Action));
    }

    public Iterable<LP<?>> getNamedProperties() {
        List<Iterable<LP<?>>> namedProperties = new ArrayList<>();
        for (LogicsModule module : modules.all()) {
            namedProperties.add(module.getNamedProperties());
        }
        return Iterables.concat(namedProperties);
    }

    @IdentityLazy
    public ImOrderSet<Property> getAutoSetProperties() {
        MOrderExclSet<Property> mResult = SetFact.mOrderExclSet();
        for (LP<?> lp : getNamedProperties()) {
            if (lp.property.autoset)
                mResult.exclAdd(lp.property);
        }
        return mResult.immutableOrder();                    
    }

    public <P extends PropertyInterface> void resolveAutoSet(DataSession session, ConcreteCustomClass customClass, DataObject dataObject, CustomClassListener classListener) throws SQLException, SQLHandledException {

        for (Property<P> property : getAutoSetProperties()) {
            ValueClass interfaceClass = property.getInterfaceClasses(ClassType.autoSetPolicy).singleValue();
            ValueClass valueClass = property.getValueClass(ClassType.autoSetPolicy);
            if (valueClass instanceof CustomClass && interfaceClass instanceof CustomClass &&
                    customClass.isChild((CustomClass) interfaceClass)) { // в общем то для оптимизации
                Long obj = classListener.getObject((CustomClass) valueClass, null, null);
                if (obj != null)
                    property.change(MapFact.singleton(property.interfaces.single(), dataObject), session, obj);
            }
        }
    }

    public void setPdfEncoding(String pdfEncoding) {
        this.pdfEncoding = pdfEncoding;
    }

    public String getLsfStrLiteralsLanguage() {
        return lsfStrLiteralsLanguage;
    }

    public String getLsfStrLiteralsCountry() {
        return lsfStrLiteralsCountry;
    }

    public void setLsfStrLiteralsLanguage(String lsfStrLiteralsLanguage) {
        this.lsfStrLiteralsLanguage = lsfStrLiteralsLanguage;
    }

    public void setLsfStrLiteralsCountry(String lsfStrLiteralsCountry) {
        this.lsfStrLiteralsCountry = lsfStrLiteralsCountry;
    }

    public String getSearchLanguage() {
        return searchLanguage;
    }

    public void setSearchLanguage(String searchLanguage) {
        this.searchLanguage = searchLanguage;
    }
    
    private ReversedI18NDictionary dictionary;
    
    public void setReversedI18nDictionary(ReversedI18NDictionary dictionary) {
        this.dictionary = dictionary;    
    }

    public ReversedI18NDictionary getReversedI18nDictionary() {
        return dictionary;
    }
    
    public Function<String, String> getIdFromReversedI18NDictionaryMethod() {
        if (lsfStrLiteralsLanguage != null) {
            return this::getIdFromReversedI18NDictionary;    
        } else {
            return null;
        }
    }
    
    private String getIdFromReversedI18NDictionary(String propertyValue) {
        return getReversedI18nDictionary().getValue(propertyValue);
    }

    private ResourceBundleGenerator generator;
    public synchronized ResourceBundleGenerator getResourceBundleGenerator() {
        if (generator == null) {
            String prefix = ((topModule != null && !topModule.isEmpty()) ? topModule : "Project");
            generator = new ResourceBundleGenerator(prefix + "ResourceBundle");
        }
        return generator;                
    }

    public void appendEntryToBundle(String entry) {
        getResourceBundleGenerator().appendEntry(entry);
    }
    
    private static class NamedDecl {
        public final LAP prop;
        public final String namespace;
        public final boolean defaultNamespace;
        public final List<ResolveClassSet> signature;
        public final Version version;

        public NamedDecl(LAP prop, String namespace, boolean defaultNamespace, List<ResolveClassSet> signature, Version version) {
            this.prop = prop;
            this.namespace = namespace;
            this.defaultNamespace = defaultNamespace;
            this.signature = signature;
            this.version = version;
        }
    }

    public Map<String, List<NamedDecl>> getNamedPropertiesWithDeclInfo() {
        Map<String, List<NamedDecl>> result = new HashMap<>();
        for (Map.Entry<String, List<LogicsModule>> namespaceToModule : namespaceToModules.entrySet()) {
            String namespace = namespaceToModule.getKey();
            for (LogicsModule module : namespaceToModule.getValue()) {
                for (LAP<?, ?> property : Iterables.concat(module.getNamedProperties(), module.getNamedActions())) {
                    String propertyName = property.getActionOrProperty().getName();
                    
                    if (result.get(propertyName) == null) {
                        result.put(propertyName, new ArrayList<>());
                    }
                    List<NamedDecl> resultProps = result.get(propertyName);
                    
                    resultProps.add(new NamedDecl(property, namespace, module.isDefaultNamespace(), module.getParamClasses(property), module.getVersion()));
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
//        }).mapValues(new Function<ImOrderSet<String>, ImSet<CustomClass>>() {
//            @Override
//            public ImOrderSet<String> apply(ImSet<CustomClass> value) {
//                return value.mapSetValues(new Function<String, CustomClass>() {
//                    @Override
//                    public String apply(CustomClass value) {
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
//        }).mapValues(new Function<ImOrderSet<String>, ImSet<FormEntity>>() {
//            @Override
//            public ImOrderSet<String> apply(ImSet<FormEntity> value) {
//                return value.mapSetValues(new Function<String, FormEntity>() {
//                    @Override
//                    public String apply(FormEntity value) {
//                        String name = value.getCanonicalName();
//                        return name.substring(0, name.indexOf("."));
//                    }
//                }).sort();
//            }
//        }).groupValues();
//        System.out.println(fm);
        if(!disableImplicitCases) {
            Map<String, List<NamedDecl>> namedProps = getNamedPropertiesWithDeclInfo();
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
    
    public ImList<Group> getChildGroups() {
        return LM.getRootGroup().getChildGroups();
    }

    //for debug
    public void outPropertyList(ApplyFilter filter) {
        for(ActionOrProperty actionOrProperty : getPropertyList(filter)) {
            if(actionOrProperty instanceof Action) {
                if(actionOrProperty.getDebugInfo() != null) {
                    serviceLogger.info(actionOrProperty.getSID() + ": " + actionOrProperty.getDebugInfo() + ", hasCancel: " + ((Action) actionOrProperty).hasFlow(ChangeFlowType.CANCEL));
                }
            }
        }
    }

    public ImOrderSet<ActionOrProperty> getPropertyList(ApplyFilter filter) {
        return getPropertyListWithGraph(filter).first;
    }

    public void fillActionChangeProps(ApplyFilter filter) { // используется только для getLinks, соответственно построения лексикографики и поиска зависимостей
        for (Action action : getOrderActions()) {
            if (filter.containsChange(action)) { // вырежем Action'ы без Event'ов, они нигде не используются, а дают много компонент связности
                ImMap<Property, Boolean> change = ((Action<?>) action).getChangeExtProps();
                for (int i = 0, size = change.size(); i < size; i++) // вообще говоря DataProperty и IsClassProperty
                    change.getKey(i).addActionChangeProp(new Pair<Action<?>, LinkType>((Action<?>) action, change.getValue(i) ? LinkType.RECCHANGE : LinkType.DEPEND));
            }
        }
    }

    public void dropActionChangeProps(ApplyFilter filter) { // для экономии памяти - симметричное удаление ссылок
        for (Action action : getOrderActions()) {
            if (filter.containsChange(action)) {
                ImMap<Property, Boolean> change = ((Action<?>) action).getChangeExtProps();
                for (int i = 0, size = change.size(); i < size; i++)
                    change.getKey(i).dropActionChangeProps();
            }
        }
    }

    public static void prereadSortedLinks(ActionOrProperty<?> property) {
        if(property.links == null) {
            for (Link link : property.getSortedLinks(true)) {
                prereadSortedLinks(link.to);
            }
        }
    }

    // находит свойство входящее в "верхнюю" сильносвязную компоненту
    private static HSet<Link> buildOrder(ActionOrProperty<?> property, MAddMap<ActionOrProperty, HSet<Link>> linksMap, List<ActionOrProperty> order, ImSet<Link> removedLinks, boolean include, ImSet<ActionOrProperty> component, boolean events, boolean recursive, boolean checkNotRecursive) {
        HSet<Link> linksIn = linksMap.get(property);
        if (linksIn == null) { // уже были, linksMap - одновременно используется и как пометки, и как список, и как обратный обход
            assert !(recursive && checkNotRecursive);
            linksIn = new HSet<>();
            linksMap.add(property, linksIn);

            ImOrderSet<Link> links = property.getSortedLinks(events);
            for (Link link : links)
                if (!removedLinks.contains(link) && component.contains(link.to) == include)
                    buildOrder(link.to, linksMap, order, removedLinks, include, component, events, true, checkNotRecursive).add(link);
            if(order != null)
                order.add(property);
        }
        return linksIn;
    }

    public final static Comparator<ActionOrProperty> actionOrPropComparator = (o1, o2) -> {
        if(o1 == o2)
            return 0;

        String c1 = o1.getCanonicalName();
        String c2 = o2.getCanonicalName();
        if(c1 != null || c2 != null) {
            if(c1 == null)
                return 1;

            if(c2 == null)
                return -1;

            assert !(c1.equals(c2) && !BaseUtils.hashEquals(o1,o2) && !(o1 instanceof SessionDataProperty) && !(o2 instanceof SessionDataProperty));
            return c1.compareTo(c2);
        }

        // если p1 не DataProperty
        c1 = o1.getChangeExtSID();
        c2 = o2.getChangeExtSID();

        if(c1 != null || c2 != null) {
            if (c1 == null)
                return 1;

            if (c2 == null)
                return -1;

            int result = c1.compareTo(c2);
            if (result != 0)
                return result;
        }

        c1 = o1.getClass().getName();
        c2 = o2.getClass().getName();

        int result = c1.compareTo(c2);
        if(result != 0)
            return result;

        DebugInfo debugInfo1 = o1.getDebugInfo();
        DebugInfo debugInfo2 = o2.getDebugInfo();

        if((debugInfo1 == null) != (debugInfo2 == null))
            return Boolean.compare(debugInfo1 == null, debugInfo2 == null);

        if(debugInfo1 != null) {
            DebugInfo.DebugPoint point1 = debugInfo1.getPoint();
            DebugInfo.DebugPoint point2 = debugInfo2.getPoint();

            result = point1.moduleName.compareTo(point2.moduleName);
            if(result != 0)
                return result;

            // we'll use global line number to make it independent of the fact if the metacodes are enabled
            result = Integer.compare(point1.globalLine, point2.globalLine);
            if(result != 0)
                return result;

            result = Integer.compare(point1.offset, point2.offset);
            if(result != 0)
                return result;
        }

        c1 = o1.caption != null ? o1.caption.getSourceString() : "";
        c2 = o2.caption != null ? o2.caption.getSourceString() : "";
        result = c1.compareTo(c2);
        if(result != 0)
            return result;

        ImList<Property> depends1 = o1 instanceof Action ? ((Action<?>) o1).getSortedUsedProps() : ((Property<?>) o1).getSortedDepends();
        ImList<Property> depends2 = o2 instanceof Action ? ((Action<?>) o2).getSortedUsedProps() : ((Property<?>) o2).getSortedDepends();
        result = Integer.compare(depends1.size(), depends2.size());
        if(result != 0)
            return result;

        for(int i=0,size=depends1.size();i<size;i++) {
            Property dp1 = depends1.get(i);
            Property dp2 = depends2.get(i);

            result = propComparator().compare(dp1, dp2);
            if(result != 0)
                return result;
        }

        return Integer.compare(o1.hashCode(), o2.hashCode());
    };
    public static Comparator<Property> propComparator() {
        return BaseUtils.immutableCast(actionOrPropComparator);
    }
    public final static Comparator<Link> linkToComparator = (o1, o2) -> {
//        int result = actionOrPropComparator.compare(o1.from, o2.from);
//        if(result != 0)
//            return result;
//
        return compare(o1.type, o1.to, o2.type, o2.to);
//        result = Integer.compare(o1.type.getNum(), o2.type.getNum());
//        if(result != 0)
//            return result;
//
//        return actionOrPropComparator.compare(o1.to, o2.to);
    };


    private static int compare(LinkType aType, ActionOrProperty aProp, LinkType bType, ActionOrProperty bProp) {
        int compare = Integer.compare(aType.getNum(), bType.getNum());
        if(compare != 0) // меньше тот у кого связь слабее (num больше)
            return -compare;
        
        return actionOrPropComparator.compare(aProp, bProp);
    }
    // ищем вершину в компоненту (нужно для детерминированности, иначе можно было бы с findMinCycle совместить) - вершину с самыми слабыми исходящими связями (эвристика, потом возможно надо все же объединить все с findMinCycle и искать минимальный цикл с минимальным вырезаемым типом ребра)
    private static ActionOrProperty<?> findMinProperty(HMap<ActionOrProperty, LinkType> component) {
        ActionOrProperty minProp = null;
        LinkType minLinkType = null; 
        for (int i = 0; i < component.size; i++) {
            ActionOrProperty prop = component.getKey(i);
            LinkType linkType = component.getValue(i);
            if(minProp == null || compare(minLinkType, minProp, linkType, prop) > 0) {
                minProp = prop;
                minLinkType = linkType;
            }
        }
        return minProp;
    }
    
    // ищем компоненту (нужно для детерминированности, иначе можно было бы с findMinCycle совместить)
    private static void findComponent(ActionOrProperty<?> property, LinkType linkType, MAddMap<ActionOrProperty, HSet<Link>> linksMap, HSet<ActionOrProperty> proceeded, HMap<ActionOrProperty, LinkType> component) {
        boolean checked = component.containsKey(property);
        component.add(property, linkType);
        if (checked)
            return;

        HSet<Link> linksIn = linksMap.get(property);
        for (int i = 0; i < linksIn.size; i++) {
            Link link = linksIn.get(i);
            if (!proceeded.contains(link.from)) { // если не в верхней компоненте
                findComponent(link.from, link.type, linksMap, proceeded, component);
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
        }

        for(int i=0,size=cycle1.size();i<size;i++) {
            Link link1 = cycle1.get(i);
            Link link2 = cycle2.get(i);

            int cmp = actionOrPropComparator.compare(link1.from, link2.from);
            if(cmp != 0)
                return cmp;
        }
        return 0;
    }

    private static List<Link> findMinCycle(ActionOrProperty<?> property, MAddMap<ActionOrProperty, HSet<Link>> linksMap, ImSet<ActionOrProperty> component) {
        // поиск в ширину
        HSet<ActionOrProperty> inQueue = new HSet<>();
        Link[] queue = new Link[component.size()];
        Integer[] from = new Integer[component.size()];
        int left = -1; int right = 0;
        int sright = right;
        List<Link> minCycle = null;

        while(true) {
            ActionOrProperty current = left >= 0 ? queue[left].from : property;
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
    private static HSet<ActionOrProperty> buildList(HSet<ActionOrProperty> props, HSet<ActionOrProperty> exclude, HSet<Link> removedLinks, HMap<Link, List<Link>> removedLinkCycles, MOrderExclSet<ActionOrProperty> mResult, boolean events, DebugInfoWriter debugInfoWriter) {
        HSet<ActionOrProperty> proceeded;

        List<ActionOrProperty> order = new ArrayList<>();
        MAddMap<ActionOrProperty, HSet<Link>> linksMap = MapFact.mAddOverrideMap();
        for (int i = 0, size = props.size(); i < size ; i++) {
            ActionOrProperty property = props.get(i);
            if (linksMap.get(property) == null) // проверка что не было
                buildOrder(property, linksMap, order, removedLinks, exclude == null, exclude != null ? exclude : props, events, false, false);
        }

        proceeded = new HSet<>();
        for (int i = 0; i < order.size(); i++) {
            ActionOrProperty orderProperty = order.get(order.size() - 1 - i);
            if (!proceeded.contains(orderProperty)) {
                HMap<ActionOrProperty, LinkType> innerComponentOutTypes = new HMap<>(LinkType.minLinkAdd());                
                findComponent(orderProperty, LinkType.MAX, linksMap, proceeded, innerComponentOutTypes);

                HSet<ActionOrProperty> innerComponent = innerComponentOutTypes.keys();
                        
                assert innerComponent.size() > 0;
                if (innerComponent.size() == 1) { // если цикла нет все ОК
                    ActionOrProperty innerProperty = innerComponent.single();
                    if(debugInfoWriter != null)
                        debugInfoWriter.addLines(innerProperty.toString());
                    mResult.exclAdd(innerProperty);
                } else { // нашли цикл
                    ActionOrProperty minProperty = findMinProperty(innerComponentOutTypes);

                    // assert что minProperty один из ActionProperty.getChangeExtProps
                    List<Link> minCycle = findMinCycle(minProperty, linksMap, innerComponent);
                    assert BaseUtils.hashEquals(minCycle.get(0).from, minProperty) && BaseUtils.hashEquals(minCycle.get(minCycle.size()-1).to, minProperty);

                    Link minLink = getMinLink(minCycle);
                    removedLinks.exclAdd(minLink);
                    if(removedLinkCycles != null)
                        removedLinkCycles.exclAdd(minLink, minCycle);

                    DebugInfoWriter pushDebugInfoWriter = null;
                    if(debugInfoWriter != null) {
                        pushDebugInfoWriter = debugInfoWriter.pushPrefix(minProperty.toString());

                        String result = "";
                        for(Link link : minCycle) {
                            result += " " + link.to;
                        }
                        pushDebugInfoWriter.addLines("REMOVE LINK : " + minLink + " FROM CYCLE : " + result);
                    }

//                    printCycle("Features", minLink, innerComponent, minCycle);
                    if (minLink.type.equals(LinkType.DEPEND)) { // нашли сильный цикл
                        MOrderExclSet<ActionOrProperty> mCycle = SetFact.mOrderExclSet();
                        buildList(innerComponent, null, removedLinks, removedLinkCycles, mCycle, events, pushDebugInfoWriter);
                        ImOrderSet<ActionOrProperty> cycle = mCycle.immutableOrder();

                        String print = "";
                        for (ActionOrProperty property : cycle)
                            print = (print.length() == 0 ? "" : print + " -> ") + property.toString();
                        throw new RuntimeException(ThreadLocalContext.localize("{message.cycle.detected}") + " : " + print + " -> " + minLink.to);
                    }
                    buildList(innerComponent, null, removedLinks, removedLinkCycles, mResult, events, pushDebugInfoWriter);
                }
                proceeded.exclAddAll(innerComponent);
            }
        }

        return proceeded;
    }

    private static void outputLink(StringBuilder result, boolean forward, Link link) {
        LinkType type = link.type;
        if(type == LinkType.DEPEND) {
            result.append(forward ? " ---> " : " <--- ");
            if(link.to instanceof Action)
                result.append("(is changed by action) ");
            else
                result.append("(property uses)");
        } else {
            result.append(forward ? " - -> " : " <- - ").append("(").append(-type.getNum());

            String linkText = "";
            switch (type) {
                case GOAFTERREC:
                    linkText = "action has recursive AFTER";
                    break;
                case EVENTACTION:
                    linkText = "action uses PREV";
                    break;
                case USEDACTION:
                    linkText = "action uses";
                    break;
                case RECCHANGE:
                    linkText = "is changed by action in NEWSESSION";
                    break;
                case RECEVENT:
                    linkText = "action uses PREV in NEWSESSION";
                    break;
                case RECUSED:
                    linkText = "action uses in NEWSESSION";
                    break;
                case REMOVEDCLASSES:
                    linkText = "object is deleted";
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            result.append(",").append(linkText).append(") ");

        }
    }

    private static void outputPath(StringBuilder result, String shift, String prefix, ImList<Link> path, boolean forward, boolean outputFrom, IntFunction<String> extraMark) {
        shift = "\n" + shift;
        result.append(shift).append(prefix).append(":");

        if(forward) {
            if(outputFrom)
                result.append(shift).append("\t").append(path.get(0).from);
            for (int i = 0, size = path.size(); i < size; i++)
                outputLink(result, shift, path.get(i), forward, extraMark.apply(i));
        } else {
            if(outputFrom)
                result.append(shift).append("\t").append(path.get(0).to);
            for (int i = path.size() - 1; i >= 0; i--)
                outputLink(result, shift, path.get(i), forward, extraMark.apply(i));
        }
    }

    private static void outputLink(StringBuilder result, String shift, Link link, boolean forward, String extra) {
        result.append(shift).append("\t");

        outputLink(result, forward, link);

        if(extra != null)
            result.append(" [").append(extra).append("]");

        result.append(shift).append("\t");
        result.append(forward ? link.to : link.from);
    }

    public String buildShowDeps(ImSet<ActionOrProperty> showDeps, ApplyFilter filter) {

        Result<String> rResult = new Result<>();
        calcPropertyListWithGraph(filter, null, (propertyList, removedLinkCycles) -> {
            // we're doing it here, because we need the links to be valid`
            ImMap<ActionOrProperty, ImMap<ActionOrProperty, ImList<Link>>> recShowDeps = showDeps.mapValues((Function<ActionOrProperty, ImMap<ActionOrProperty, ImList<Link>>>) actionOrProperty -> buildShowDeps(actionOrProperty, showDeps));

            StringBuilder result = new StringBuilder("LIST: ");
            ImOrderSet<ActionOrProperty> orderedShowDeps = propertyList.filterOrder(showDeps);
            for(int i=0,size=orderedShowDeps.size();i<size;i++) {
                ActionOrProperty showDep = orderedShowDeps.get(i);
                result.append("\n\t").append(i+1).append(". ").append(showDep);

                for(int j=i;j<size;j++) {
                    ActionOrProperty depProp = orderedShowDeps.get(j);
                    ImList<Link> depPath = recShowDeps.get(depProp).get(showDep);
                    if(depPath != null) {
                        // outputing the property and the path
                        result.append("\n\t\t <- ").append(depProp);

                        ImList<Link> cyclePath;
                        String cycleName;
                        if(j > i) {
                            outputPath(result, "\t\t\t","PATH", depPath, true, false, value -> null);
                            cyclePath = recShowDeps.get(showDep).get(depProp);
                            cycleName = "REVERSE PATH";
                        } else {
                            cyclePath = depPath;
                            cycleName = "CYCLE PATH";
                        }

                        if(cyclePath != null) {
                            ImList<Link> fCyclePath = cyclePath;
                            List<Link> removedLinks = new ArrayList<>();
                            outputPath(result, "\t\t\t", cycleName, cyclePath, true, false, index -> {
                                Link link = fCyclePath.get(index);
                                if(removedLinkCycles.containsKey(link)) {
                                    removedLinks.add(link);
                                    return "REMOVED " + removedLinks.size();
                                }
                                return null;
                            });

                            result.append("\n\t\t\tREMOVED CYCLES:");
                            for (int k = 0; k < removedLinks.size(); k++) {
                                Link removedLink = removedLinks.get(k);
                                ImList<Link> removedLinkCycle = ListFact.fromJavaList(removedLinkCycles.get(removedLink));
                                outputPath(result, "\t\t\t\t", "REMOVED CYCLE " + (k + 1), removedLinkCycle, true, true, index -> {
                                    if(removedLinkCycle.get(index).equals(removedLink))
                                        return "REMOVED";
                                    return null;
                                });
                            }
//                            we don't need such complex mechanism so far, we'll just show the removed links
//                            outputRemovedLinkCycle(result, removedLinkCycles, depPath, cyclePath);
                        }
                    }
                }
            }
            rResult.set(result.toString());
        });

        return rResult.result;
    }

    public ImMap<ActionOrProperty, ImList<Link>> buildShowDeps(ActionOrProperty<?> property, ImSet<ActionOrProperty> showDeps) {
        ImMap<ActionOrProperty, ImList<Link>> result = MapFact.EMPTY();
        for(int i=LinkType.order.length-1;i>=0;i--) {
            LinkType maxLinkType = LinkType.order[i];
            ImMap<ActionOrProperty, ImList<Link>> maxResult = buildShowDeps(property, showDeps, maxLinkType);
            if(maxResult.isEmpty())
                break;
            result = result.override(maxResult);
        }
        return result;
    }
    public ImMap<ActionOrProperty, ImList<Link>> buildShowDeps(ActionOrProperty<?> property, ImSet<ActionOrProperty> showDeps, LinkType maxLinkType) {

        List<ActionOrProperty> queue = new ArrayList<>();
        List<ImList<Link>> pathes = new ArrayList<>();
        MAddSet<ActionOrProperty> nodes = SetFact.mAddSet();

        MExclMap<ActionOrProperty, ImList<Link>> mResult = MapFact.mExclMap();

        int q = -1;
        ImList<Link> currentPath = ListFact.EMPTY();
        while(true) {
            for (Link link : property.getSortedLinks(true))
                if(link.type.getNum() <= maxLinkType.getNum()) {
                    ActionOrProperty linkProp = link.to;
                    if(showDeps.contains(linkProp)) {
                        if(mResult.get(linkProp) == null)
                            mResult.exclAdd(linkProp, currentPath.addList(link));
                    } else {
                        if (!nodes.add(linkProp)) {
                            queue.add(linkProp);
                            pathes.add(currentPath.addList(link));
                        }
                    }
                }

            q++;
            if(q >= queue.size())
                break;

            property = queue.get(q);
            currentPath = pathes.get(q);
        }
        return mResult.immutable();
    }

    @IdentityLazy
    public Graph<Action> getRecalculateFollowsGraph() {
        return BaseUtils.immutableCast(getPropertyGraph().filterGraph(element -> element instanceof Action && ((Action) element).hasResolve()));
    }

    @IdentityLazy
    public ImSet<Action> getRecalculateFollows() {
        return BaseUtils.immutableCast(getPropertyList(ApplyFilter.NO).getSet().filterFn(property -> property instanceof Action && ((Action) property).hasResolve()));
    }

    @IdentityLazy
    public Graph<AggregateProperty> getAggregateStoredGraph() {
        return BaseUtils.immutableCast(getPropertyGraph().filterGraph(element -> element instanceof AggregateProperty && ((AggregateProperty) element).isStored()));
    }

    public Graph<AggregateProperty> getRecalculateAggregateStoredGraph(DataSession session) throws SQLException, SQLHandledException {
        QueryBuilder<String, Object> query = new QueryBuilder<>(SetFact.singleton("key"));

        Expr expr = reflectionLM.disableMaterializationsTableColumnSID.getExpr(query.getMapExprs().singleValue());
        query.and(expr.getWhere());
        ImSet<String> skipProperties = query.execute(session).keys().mapSetValues(value -> (String)value.singleValue());


        final ImSet<String> fSkipProperties = skipProperties;
        return getAggregateStoredGraph().filterGraph(element -> !fSkipProperties.contains(element.getDBName()));
    }

    public Graph<ActionOrProperty> getPropertyGraph() {
        return getPropertyListWithGraph(ApplyFilter.NO).second;
    }

    @IdentityStrongLazy // глобальное очень сложное вычисление
    public Pair<ImOrderSet<ActionOrProperty>, Graph<ActionOrProperty>> getPropertyListWithGraph(ApplyFilter filter) {
        return calcPropertyListWithGraph(filter, null, null);
    }

    public boolean propertyListInitialized;
    // should be synchronized because of actionChangeProps / links, but it is already synchronized with the IdentityStrongLazy
    public Pair<ImOrderSet<ActionOrProperty>, Graph<ActionOrProperty>> calcPropertyListWithGraph(ApplyFilter filter, DebugInfoWriter debugInfoWriter, BiConsumer<ImOrderSet<ActionOrProperty>, HMap<Link, List<Link>>> debugConsumer) {
        assert ImplementTable.updatedStats;
        assert propertyListInitialized;

        fillActionChangeProps(filter);

        // сначала бежим по Action'ам с cancel'ами
        HSet<ActionOrProperty> cancelActions = new HSet<>();
        HSet<ActionOrProperty> rest = new HSet<>();
        for (ActionOrProperty property : getOrderActionOrProperties())
            if(filter.contains(property)) {
                if (ApplyFilter.isCheck(property))
                    cancelActions.add(property);
                else
                    rest.add(property);
            }
        boolean events = filter != ApplyFilter.ONLY_DATA;

        HMap<Link, List<Link>> removedLinkCycles = debugConsumer != null ? new HMap<>(MapFact.exclusive()) : null;

        MOrderExclSet<ActionOrProperty> mCancelResult = SetFact.mOrderExclSet();
        HSet<Link> firstRemoved = new HSet<>();
        HSet<ActionOrProperty> proceeded = buildList(cancelActions, new HSet<>(), firstRemoved, removedLinkCycles, mCancelResult, events, DebugInfoWriter.pushPrefix(debugInfoWriter, "CANCELABLE"));
        ImOrderSet<ActionOrProperty> cancelResult = mCancelResult.immutableOrder();

        // потом бежим по всем остальным, за исключением proceeded
        MOrderExclSet<ActionOrProperty> mRestResult = SetFact.mOrderExclSet();
        HSet<ActionOrProperty> removed = new HSet<>();
        removed.addAll(rest.remove(proceeded));
        HSet<Link> secondRemoved = new HSet<>();
        buildList(removed, proceeded, secondRemoved, removedLinkCycles, mRestResult, events, DebugInfoWriter.pushPrefix(debugInfoWriter, "REST")); // потом этот cast уберем
        ImOrderSet<ActionOrProperty> restResult = mRestResult.immutableOrder();

        // затем по всем кроме proceeded на прошлом шаге
        assert cancelResult.getSet().disjoint(restResult.getSet());
        ImOrderSet<ActionOrProperty> result = cancelResult.reverseOrder().addOrderExcl(restResult.reverseOrder());

        Graph<ActionOrProperty> graph = null;
        if(filter == ApplyFilter.NO) {
            graph = buildGraph(result, firstRemoved.addExcl(secondRemoved));
        }

        if(debugConsumer != null)
            debugConsumer.accept(result, removedLinkCycles);

        for(ActionOrProperty property : result)
            property.dropLinks();

        dropActionChangeProps(filter);

        return new Pair<>(result, graph);
    }

    private static Graph<ActionOrProperty> buildGraph(ImOrderSet<ActionOrProperty> props, ImSet<Link> removedLinks) {
        MAddMap<ActionOrProperty, HSet<Link>> linksMap = MapFact.mAddOverrideMap();
        for (int i = 0, size = props.size(); i < size; i++) {
            ActionOrProperty property = props.get(i);
            if (linksMap.get(property) == null) // проверка что не было
                buildOrder(property, linksMap, null, removedLinks, true, props.getSet(), true, false, true);
        }

        MExclMap<ActionOrProperty, ImSet<ActionOrProperty>> mEdgesIn = MapFact.mExclMap(linksMap.size());
        for(int i=0,size=linksMap.size();i<size;i++) {
            final ActionOrProperty property = linksMap.getKey(i);
            HSet<Link> links = linksMap.getValue(i);
            mEdgesIn.exclAdd(property, links.mapSetValues(value -> {
                assert BaseUtils.hashEquals(value.to, property);
                return value.from;
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

    public List<AggregateProperty> getRecalculateAggregateStoredProperties(DataSession session, boolean ignoreCheck) throws SQLException, SQLHandledException {
        List<AggregateProperty> result = new ArrayList<>();
        for (Property property : getStoredProperties())
            if (property instanceof AggregateProperty) {
                boolean recalculate = ignoreCheck || reflectionLM.disableMaterializationsTableColumn.read(session, reflectionLM.tableColumnSID.readClasses(session, new DataObject(property.getDBName()))) == null;
                if(recalculate)
                    result.add((AggregateProperty) property);
            }
        return result;
    }

    public ImOrderSet<AbstractDataProperty> getStoredDataProperties(final DataSession dataSession) {
        return BaseUtils.immutableCast(getStoredProperties().filterOrder(property -> {
            boolean recalculate;
            try {
                recalculate = reflectionLM.disableClassesTableColumn.read(dataSession, reflectionLM.tableColumnSID.readClasses(dataSession, new DataObject(property.getDBName()))) == null;
            } catch (SQLException | SQLHandledException e) {
                throw Throwables.propagate(e);
            }
            return recalculate && (property instanceof StoredDataProperty || property instanceof ClassDataProperty);
        }));
    }

    @IdentityLazy
    public ImOrderSet<Property> getStoredProperties() {
        return BaseUtils.immutableCast(getOrderProperties().filterOrder(property -> property instanceof Property && ((Property) property).isStored()));
    }

    public ImSet<CustomClass> getCustomClasses() {
        return LM.baseClass.getAllClasses();
    }

    public ImSet<ConcreteCustomClass> getConcreteCustomClasses() {
        return BaseUtils.immutableCast(getCustomClasses().filterFn(property -> property instanceof ConcreteCustomClass));
    }

    public ImMap<OldProperty, SessionEnvEvent> calcSessionEventOldDepends(FunctionSet<? extends Property> properties, ImOrderMap<Action, SessionEnvEvent> sessionEvents) {
        MMap<OldProperty, SessionEnvEvent> mResult = MapFact.mMap(SessionEnvEvent.mergeSessionEnv());
        for (int i = 0, size = sessionEvents.size(); i < size; i++) {
            Action<?> action = sessionEvents.getKey(i);
            SessionEnvEvent sessionEnv = sessionEvents.getValue(i);

            for(OldProperty old : action.getSessionEventOldDepends())
                if(Property.depends(old.property, (FunctionSet<Property>) properties))
                    mResult.add(old, sessionEnv);
        }
        return mResult.immutable();
    }

    @IdentityLazy
    public ImMap<OldProperty, SessionEnvEvent> getCacheSessionEventOldDepends(ImSet<Property> properties) {
        return calcSessionEventOldDepends(properties, getAllSessionEvents());
    }

    public ImMap<OldProperty, SessionEnvEvent> getSessionEventOldDepends(FunctionSet<? extends Property> properties) {
        ImOrderMap<Action, SessionEnvEvent> sessionEvents = getAllSessionEvents();
        if(properties instanceof ImSet && ((ImSet<Property>) properties).size() < (double) sessionEvents.size() * Settings.get().getCacheNextEventActionRatio())
            return getCacheSessionEventOldDepends((ImSet<Property>) properties);
        return calcSessionEventOldDepends(properties, sessionEvents);
    }

    private NextSession calcNextSessionEvent(int i, ImSet<Property> sessionEventChangedOld, ImOrderMap<Action, SessionEnvEvent> sessionEvents) {
        for(int size=sessionEvents.size();i<size;i++) {
            Action<?> action = sessionEvents.getKey(i);
            if(sessionEventChangedOld.intersect(action.getSessionEventOldDepends()))
                return new NextSession(action, sessionEvents.getValue(i), i, new StatusMessage("event", action, i, size));
        }
        return null;
    }

    @IdentityLazy
    private NextSession getCachedNextSessionEvent(int i, ImSet<Property> sessionEventChangedOld) {
        return calcNextSessionEvent(i, sessionEventChangedOld, getAllSessionEvents());
    }

    public NextSession getNextSessionEvent(int i, ImSet<Property> sessionEventChangedOld, ImOrderMap<Action, SessionEnvEvent> sessionEvents) {
        if(sessionEventChangedOld.size() < (double) sessionEvents.size() * Settings.get().getCacheNextEventActionRatio())
            return getCachedNextSessionEvent(i, sessionEventChangedOld);
        return calcNextSessionEvent(i, sessionEventChangedOld, sessionEvents);
    }

    @IdentityLazy
    public ImOrderMap<Action, SessionEnvEvent> getAllSessionEvents() {
        ImOrderSet<ActionOrProperty> list = getPropertyList(ApplyFilter.SESSION);
        MOrderExclMap<Action, SessionEnvEvent> mResult = MapFact.mOrderExclMapMax(list.size());
        SessionEnvEvent sessionEnv;
        for (ActionOrProperty property : list)
            if (property instanceof Action && (sessionEnv = ((Action) property).getSessionEnv(SystemEvent.SESSION))!=null)
                mResult.exclAdd((Action) property, sessionEnv);
        return mResult.immutableOrder();
    }

    // определяет для stored свойства зависимые от него stored свойства, а также свойства которым необходимо хранить изменения с начала транзакции (constraints и derived'ы)
    public ImOrderSet<ApplySingleEvent> getSingleApplyDependFrom(ApplyCalcEvent event, DataSession session, boolean includeCorrelations) {
        Pair<ImMap<ApplyCalcEvent, ImOrderMap<ApplySingleEvent, SessionEnvEvent>>, ImMap<ApplyUpdatePrevEvent, Integer>> orderMapSingleApplyDepends = getOrderMapSingleApplyDepends(session.applyFilter, includeCorrelations);

        ImOrderMap<ApplySingleEvent, SessionEnvEvent> depends = orderMapSingleApplyDepends.first.get(event);
        ImMap<ApplyUpdatePrevEvent, Integer> lastUsedEvents = orderMapSingleApplyDepends.second;

        if(!Settings.get().isRemoveClassesFallback()) {
            depends = depends.filterOrder(element -> {
                if(element instanceof ApplyUpdatePrevEvent) {
                    return lastUsedEvents.get((ApplyUpdatePrevEvent)element) >= session.executingApplyEvent;
                }
                return true;
            });

        }
        return depends.filterOrderValues(sessionEnv -> sessionEnv.contains(session));
    }

    @IdentityLazy
    public ImSet<Property> getDataChangeEvents() {
        ImSet<DataProperty> result = getDataChangeEvents(getOrderActionOrProperties()); // in theory all data properties with event should be in this method
        assert result.remove(getDataChangeEvents(getPropertyList(ApplyFilter.NO))).filterFn(element -> !(element instanceof SessionDataProperty)).isEmpty();
        return result.mapSetValues(property -> property.event.getWhere());
    }

    private static ImSet<DataProperty> getDataChangeEvents(ImOrderSet<ActionOrProperty> propertyList) {
        MSet<DataProperty> mResult = SetFact.mSetMax(propertyList.size());
        for (int i = 0, size = propertyList.size(); i<size; i++) {
            ActionOrProperty property = propertyList.get(i);
            if (property instanceof DataProperty && ((DataProperty) property).event != null)
                mResult.add((DataProperty) property);
        }
        return mResult.immutable();
    }

    @IdentityLazy
    public ImOrderMap<ApplyGlobalEvent, SessionEnvEvent> getApplyEvents(ApplyFilter filter) {
        // здесь нужно вернуть список stored или тех кто
        ImOrderSet<ActionOrProperty> list = getPropertyList(filter);
        MOrderExclMap<ApplyGlobalEvent, SessionEnvEvent> mResult = MapFact.mOrderExclMapMax(list.size());
        for (ActionOrProperty property : list) {
            ApplyGlobalEvent applyEvent = property.getApplyEvent();
            if(applyEvent != null)
                mResult.exclAdd(applyEvent, applyEvent.getSessionEnv());
        }
        return mResult.immutableOrder();
    }

    // optimization similar to Next
    public static class NextSession {
        public final Action action;
        public final SessionEnvEvent sessionEnv;
        public final int index;
        public final StatusMessage statusMessage;

        public NextSession(Action action, SessionEnvEvent sessionEnv, int index, StatusMessage statusMessage) {
            this.action = action;
            this.sessionEnv = sessionEnv;
            this.index = index;
            this.statusMessage = statusMessage;
        }
    }

    private Next calcNextApplyEvent(int i, StructChanges changes, ImOrderMap<ApplyGlobalEvent, SessionEnvEvent> applyEvents) {
        for(int size=applyEvents.size();i<size;i++) {
            ApplyGlobalEvent event = applyEvents.getKey(i);
            if(event.hasChanges(changes))
                return new Next(event, applyEvents.getValue(i), i, new StatusMessage("event", event, i, size));
        }
        return null;
    }

    @IdentityLazy
    private Next getCachedNextApplyEvent(ApplyFilter filter, int i, StructChanges changes) {
        return calcNextApplyEvent(i, changes, getApplyEvents(filter));
    }

    public Next getNextApplyEvent(ApplyFilter filter, int i, StructChanges changes, ImOrderMap<ApplyGlobalEvent, SessionEnvEvent> applyEvents) {
        if(changes.size() < (double) applyEvents.size() * Settings.get().getCacheNextEventActionRatio())
            return getCachedNextApplyEvent(filter, i, changes);
        return calcNextApplyEvent(i, changes, applyEvents);
    }

    private static ImSet<Property> getSingleApplyDepends(Property<?> fill, Result<Boolean> canBeOutOfDepends, ApplySingleEvent event) {
        ImSet<Property> depends = fill.getDepends(false);// вычисляемые события отдельно отрабатываются (собственно обрабатываются как обычные события)

        if (fill instanceof DataProperty) { // отдельно обрабатывается так как в getDepends передается false (так как в локальных событиях удаление это скорее вычисляемое событие, а в глобальных - императивное, приходится делать такой хак)
             assert depends.isEmpty();
             canBeOutOfDepends.set(true); // могут не быть в propertyList
             return ((DataProperty) fill).getSingleApplyDroppedIsClassProps();
        }
        if (fill instanceof IsClassProperty) {
             assert depends.isEmpty();
             canBeOutOfDepends.set(true); // могут не быть в propertyList
             return ((IsClassProperty) fill).getSingleApplyDroppedIsClassProps();
        }
        if (fill instanceof ObjectClassProperty) {
             assert depends.isEmpty();
             canBeOutOfDepends.set(true); // могут не быть в propertyList
             return ((ObjectClassProperty) fill).getSingleApplyDroppedIsClassProps();
        }
        return depends;
    }

    private static void fillSingleApplyDependFrom(Property<?> prop, ApplySingleEvent applied, SessionEnvEvent appliedSet, MExclMap<ApplyCalcEvent, MOrderMap<ApplySingleEvent, SessionEnvEvent>> mapDepends, boolean canBeOutOfDepends) {
        ApplyCalcEvent applyEvent = prop.getApplyEvent();
        if (applyEvent != null && !applyEvent.equals(applied)) {
            MOrderMap<ApplySingleEvent, SessionEnvEvent> fillDepends = mapDepends.get(applyEvent);
            
            boolean propCanBeOutOfDepends = canBeOutOfDepends;
            if(prop instanceof ChangedProperty && (applied instanceof ApplyStoredEvent)) // applied может идти до DROPPED(класс), но в этом и смысл, так как если он идет до то удаление уже прошло и это удаление "фейковое" (не влияет на этот applied)
                propCanBeOutOfDepends = true;
            
            if(!(propCanBeOutOfDepends && fillDepends==null))
                fillDepends.add(applied, appliedSet);
        } else {
            Result<Boolean> rCanBeOutOfDepends = new Result<>(canBeOutOfDepends);
            for (Property depend : getSingleApplyDepends(prop, rCanBeOutOfDepends, applied))
                fillSingleApplyDependFrom(depend, applied, appliedSet, mapDepends, rCanBeOutOfDepends.result);
        }
    }

    @IdentityLazy
    private Pair<ImMap<ApplyCalcEvent, ImOrderMap<ApplySingleEvent, SessionEnvEvent>>, ImMap<ApplyUpdatePrevEvent, Integer>> getOrderMapSingleApplyDepends(ApplyFilter increment, boolean includeCorrelations) {
        assert Settings.get().isEnableApplySingleStored();

        ImOrderMap<ApplyGlobalEvent, SessionEnvEvent> applyEvents = getApplyEvents(increment);

        // нам нужны будут сами persistent свойства + prev'ы у action'ов
        boolean canBeOutOfDepends = increment != ApplyFilter.NO;
        MExclMap<ApplyCalcEvent, MOrderMap<ApplySingleEvent, SessionEnvEvent>> mMapDepends = MapFact.mExclMap();
        MAddMap<ApplyUpdatePrevEvent, SessionEnvEvent> singleAppliedOld = MapFact.mAddMap(SessionEnvEvent.mergeSessionEnv());
        MMap<ApplyUpdatePrevEvent, Integer> mLastUsed = MapFact.mMap(MapFact.override());
        for(int i = 0, size = applyEvents.size(); i<size; i++) {
            ApplyGlobalEvent applyEvent = applyEvents.getKey(i);
            SessionEnvEvent sessionEnv = applyEvents.getValue(i);

            ImSet<OldProperty> oldDepends = applyEvent.getEventOldDepends();
            for(OldProperty oldDepend : oldDepends) {
                ApplyUpdatePrevEvent event = new ApplyUpdatePrevEvent(oldDepend);

                singleAppliedOld.add(event, sessionEnv);
                mLastUsed.add(event, i);
            }
            
            if(applyEvent instanceof ApplyCalcEvent) { // сначала классы и stored обрабатываем
                mMapDepends.exclAdd((ApplyCalcEvent) applyEvent, MapFact.mOrderMap(SessionEnvEvent.mergeSessionEnv()));
                if(applyEvent instanceof ApplyStoredEvent) { // так как бежим в нужном порядке, то и stored будут заполняться в нужном порядке (так как он соответствует порядку depends)
                    ApplyStoredEvent applyStoredEvent = (ApplyStoredEvent) applyEvent;
                    fillSingleApplyDependFrom(applyStoredEvent.property, applyStoredEvent, sessionEnv, mMapDepends, canBeOutOfDepends);
                }
            }
        }
        for (int i=0,size= singleAppliedOld.size();i<size;i++) { // old'ы по идее не важно в каком порядке будут (главное что stored до)
            ApplyUpdatePrevEvent event = singleAppliedOld.getKey(i);
            SessionEnvEvent sessionEnv = singleAppliedOld.getValue(i);

            Property<?> oldProperty = event.property.property;
            fillSingleApplyDependFrom(oldProperty, event, sessionEnv, mMapDepends, canBeOutOfDepends);
            
            if(includeCorrelations)
                for(Correlation<?> corProp : oldProperty.getCorrelations(ClassType.materializeChangePolicy))
                    fillSingleApplyDependFrom(corProp.getProperty(), event, sessionEnv, mMapDepends, true);
        }

        return new Pair<>(mMapDepends.immutable().mapValues(MOrderMap::immutableOrder), mLastUsed.immutable());
    }

    // optimization similar to NextSession
    public static class Next {
        public final ApplyGlobalEvent event;
        public final SessionEnvEvent sessionEnv;
        public final int index;
        public final StatusMessage statusMessage;

        public Next(ApplyGlobalEvent event, SessionEnvEvent sessionEnv, int index, StatusMessage statusMessage) {
            this.event = event;
            this.sessionEnv = sessionEnv;
            this.index = index;
            this.statusMessage = statusMessage;
        }
    }

    @IdentityLazy
    public ImSet<Property> getCheckConstrainedProperties() {
        ImSet<Property> result = getCheckConstrainedProperties(getOrderActionOrProperties()); // in theory all data properties with event should be in this method (because is used only for propertyExpression properties)
//        assert result.equals(getCheckConstrainedProperties(getPropertyList(ApplyFilter.NO))); // can be broken in CONSRAINT LOCAL
        return result;
    }

    private static ImSet<Property> getCheckConstrainedProperties(ImOrderSet<ActionOrProperty> propertyList) {
        return BaseUtils.immutableCast(propertyList.getSet().filterFn(property -> property instanceof Property && ((Property) property).checkChange != Property.CheckType.CHECK_NO));
    }

    public ImSet<Property> getCheckConstrainedProperties(Property<?> changingProp) {
        return BaseUtils.immutableCast(getCheckConstrainedProperties().filterFn(property -> property.checkChange == Property.CheckType.CHECK_ALL ||
                property.checkChange == Property.CheckType.CHECK_SOME && property.checkProperties.contains(changingProp)));
    }

    public List<LogicsModule> getLogicModules() {
        return modules.all();
    }

    public MSet<Long> getOverCalculatePropertiesSet(DataSession session, Integer maxQuantity) throws SQLException, SQLHandledException {
        KeyExpr propertyExpr = new KeyExpr("Property");
        ImRevMap<Object, KeyExpr> propertyKeys = MapFact.singletonRev("Property", propertyExpr);

        QueryBuilder<Object, Object> propertyQuery = new QueryBuilder<>(propertyKeys);
        propertyQuery.and(reflectionLM.canonicalNameProperty.getExpr(propertyExpr).getWhere());
        if(maxQuantity == null)
            propertyQuery.and(reflectionLM.notNullQuantityProperty.getExpr(propertyExpr).getWhere().not()); //null quantityProperty
        else
            propertyQuery.and(reflectionLM.notNullQuantityProperty.getExpr(propertyExpr).getWhere().not().or( //null quantityProperty
                    reflectionLM.notNullQuantityProperty.getExpr(propertyExpr).compare(new DataObject(maxQuantity).getExpr(), Compare.LESS_EQUALS))); //less or equals then maxQuantity

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> propertyResult = propertyQuery.execute(session);

        MSet<Long> resultSet = SetFact.mSet();
        for (int i = 0, size = propertyResult.size(); i < size; i++) {
            resultSet.add((Long) propertyResult.getKey(i).get("Property"));
        }
        return resultSet;
    }

    public String recalculateFollows(SessionCreator creator, boolean isolatedTransaction, final ExecutionStack stack) throws SQLException, SQLHandledException {
        final List<String> messageList = new ArrayList<>();
        final long maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        ImSet<Action> actions = getRecalculateFollows();
        for (int i = 0; i < actions.size(); i++) {
            final Action<?> action = actions.get(i);
            long start = System.currentTimeMillis();
            try {
                DBManager.runData(creator, isolatedTransaction, session -> ((DataSession) session).resolve(action, stack));
            } catch (ApplyCanceledException e) { // suppress'им так как понятная ошибка
                serviceLogger.info(e.getMessage());
            }
            long time = System.currentTimeMillis() - start;
            String message = String.format("Recalculate Follows %s of %s: %s, %sms", i + 1, actions.size(), action.getSID(), time);
            serviceLogger.info(message);
            if (time > maxRecalculateTime)
                messageList.add(message);
        }
        return formatMessageList(messageList);
    }

    public String formatMessageList(List<String> messageList) {
        if(messageList.isEmpty())
            return null;
        else {
            String result = "";
            for (String message : messageList)
                result += message + '\n';
            return result;
        }
    }

    public LAP findSafeProperty(String canonicalName) {
        LP lp = null;
        try {
            lp = findProperty(canonicalName);
        } catch (Exception e) {
        }
        return lp;
    }

    @IdentityLazy
    public LAP<?,?> findPropertyElseAction(String canonicalName) {
        LAP<?,?> property = findProperty(canonicalName);
        if(property == null)
            property = findAction(canonicalName);
        return property;
    }

    @IdentityLazy
    public LP<?> findProperty(String canonicalName) {
        return BusinessLogicsResolvingUtils.findPropertyByCanonicalName(this, canonicalName);
    }

    @IdentityLazy
    public LA<?> findAction(String canonicalName) {
        return BusinessLogicsResolvingUtils.findActionByCanonicalName(this, canonicalName);
    }

    @IdentityLazy
    public LA<?> findActionByCompoundName(String compoundName) {
        return BusinessLogicsResolvingUtils.findLAPByCompoundName(this, compoundName, new ModuleLAFinder());
    }

    @IdentityLazy
    public LP<?> findPropertyByCompoundName(String compoundName) {
        return BusinessLogicsResolvingUtils.findLAPByCompoundName(this, compoundName, new ModuleLPFinder());
    }

    @IdentityLazy
    public CustomClass findClassByCompoundName(String compoundName) {
        return findElementByCompoundName(this, compoundName, null, new ModuleClassFinder());
    }

    @IdentityLazy
    public CustomClass findClass(String canonicalName) {
        return findElementByCanonicalName(this, canonicalName, null, new ModuleClassFinder());
    }

    @IdentityLazy
    public Group findGroup(String canonicalName) {
        return findElementByCanonicalName(this, canonicalName, null, new ModuleGroupFinder());
    }

    @IdentityLazy
    public ImplementTable findTable(String canonicalName) {
        return findElementByCanonicalName(this, canonicalName, null, new ModuleTableFinder());
    }

    @IdentityLazy
    public AbstractWindow findWindow(String canonicalName) {
        return findElementByCanonicalName(this, canonicalName, null, new ModuleWindowFinder());
    }

    @IdentityLazy
    public NavigatorElement findNavigatorElement(String canonicalName) {
        return findElementByCanonicalName(this, canonicalName, null, new ModuleNavigatorElementFinder());
    }

    @IdentityLazy
    public FormEntity findForm(String canonicalName) {
        return findElementByCanonicalName(this, canonicalName, null, new ModuleFormFinder());
    }

    @IdentityLazy
    public MetaCodeFragment findMetaCodeFragment(String canonicalName, int paramCnt) {
        return findElementByCanonicalName(this, canonicalName, paramCnt, new ModuleMetaCodeFragmentFinder());
    }

    public Collection<String> getNamespacesList() {
        return namespaceToModules.keySet();
    }
    
    public List<LogicsModule> getNamespaceModules(String namespace) {
        return namespaceToModules.getOrDefault(namespace, Collections.emptyList());
    }
    
    private void outputPersistent() {
        String result = "";

        result += ThreadLocalContext.localize("\n{logics.info.by.tables}\n\n");
        ImOrderSet<Property> storedProperties = getStoredProperties();
        for (Map.Entry<ImplementTable, Collection<Property>> groupTable : BaseUtils.group(new BaseUtils.Group<ImplementTable, Property>() {
            public ImplementTable group(Property key) {
                return key.mapTable.table;
            }
        }, storedProperties).entrySet()) {
            result += groupTable.getKey().outputKeys() + '\n';
            for (Property property : groupTable.getValue())
                result += '\t' + property.outputStored(false) + '\n';
        }
        result += ThreadLocalContext.localize("\n{logics.info.by.properties}\n\n");
        for (Property property : storedProperties)
            result += property.outputStored(true) + '\n';
        System.out.println(result);
    }

    public ImSet<FormEntity> getFormEntities(){
        MExclSet<FormEntity> mResult = SetFact.mExclSet();
        for(LogicsModule logicsModule : modules.all()) {
            for(FormEntity entry : logicsModule.getNamedForms())
                mResult.exclAdd(entry);
        }
        return mResult.immutable();
    }

    public ImSet<NavigatorElement> getNavigatorElements() {
        MExclSet<NavigatorElement> mResult = SetFact.mExclSet();
        for(LogicsModule logicsModule : modules.all()) {
            for(NavigatorElement entry : logicsModule.getNavigatorElements())
                mResult.exclAdd(entry);            
        }
        return mResult.immutable();
    }

    public ImSet<AbstractWindow> getWindows() {
        MExclSet<AbstractWindow> mResult = SetFact.mExclSet();
        for(LogicsModule logicsModule : modules.all()) {
            for(AbstractWindow entry : logicsModule.getWindows())
                mResult.exclAdd(entry);
        }
        return mResult.immutable();
    }

    public void markFormsForFinalization() {
        for(LogicsModule logicsModule : modules.all())
            logicsModule.markFormsForFinalization();
    }

    public void markPropsForFinalization() {
        for(LogicsModule logicsModule : modules.all())
            logicsModule.markPropsForFinalization();
    }

    public ImSet<FormEntity> getAllForms() {
        MExclSet<FormEntity> mResult = SetFact.mExclSet();
        for(LogicsModule logicsModule : modules.all()) {
            for(FormEntity entry : logicsModule.getAllModuleForms())
                mResult.exclAdd(entry);
        }
        return mResult.immutable();
    }

    public void checkForDuplicateElements() {
        new DuplicateElementsChecker(modules.all()).check();
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

        final long excessAllocatedBytes = Settings.get().getExcessThreadAllocatedBytes();
        final long maxAllocatedBytes = Settings.get().getMaxThreadAllocatedBytes();
        final int cacheMissesStatsLimit = Settings.get().getCacheMissesStatsLimit();

        ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
        Class threadMXBeanClass = ReflectionUtils.classForName("com.sun.management.ThreadMXBean");
        if (threadMXBeanClass != null && threadMXBeanClass.isInstance(tBean) && (boolean) ReflectionUtils.getMethodValue(threadMXBeanClass, tBean, "isThreadAllocatedMemorySupported", new Class[0], new Object[0])) {
            long time = System.currentTimeMillis();
            long bytesSum = 0;
            long totalBytesSum = 0;
            SQLSession.updateThreadAllocatedBytesMap();
            Map<Long, Thread> threadMap = ThreadUtils.getThreadMap();

            HashMap<Long, HashMap<CacheType, Long>> hitStats = new HashMap<>(CacheStats.getCacheHitStats());
            HashMap<Long, HashMap<CacheType, Long>> missedStats = new HashMap<>(CacheStats.getCacheMissedStats());
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
            List<AllocatedInfo> infos = new ArrayList<>();
            Set<Long> excessAllocatedBytesSet = new HashSet<>();

            for (Map.Entry<Long, Long> bEntry : SQLSession.threadAllocatedBytesBMap.entrySet()) {
                Long id = bEntry.getKey();
                if (id != null) {
                    Long bBytes = bEntry.getValue();
                    Long aBytes = SQLSession.threadAllocatedBytesAMap.get(bEntry.getKey());

                    Long deltaBytes = bBytes != null && aBytes != null ? (bBytes - aBytes) : 0;
                    totalBytesSum += deltaBytes;

                    long userMissed = 0;
                    long userHit = 0;

                    HashMap<CacheType, Long> userHitMap = hitStats.get(id) != null ? hitStats.get(id) : new HashMap<>();
                    HashMap<CacheType, Long> userMissedMap = missedStats.get(id) != null ? missedStats.get(id) : new HashMap<>();
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

                    if (deltaBytes > excessAllocatedBytes) {
                        if (!isSystem(threadMap, id) && ThreadUtils.isActiveJavaProcess(ManagementFactory.getThreadMXBean().getThreadInfo(id, Integer.MAX_VALUE))) {
                            excessAllocatedBytesSet.add(id);
                        }
                    }

                    if (deltaBytes > maxAllocatedBytes || userMissed > cacheMissesStatsLimit) {
                        logTotal = true;

                        bytesSum += deltaBytes;

                        exceededMisses += userMissed;
                        exceededMissesHits += userHit;
                        sumMap(exceededHitMap, userHitMap);
                        sumMap(exceededMissedMap, userMissedMap);

                        Thread thread = threadMap.get(id);
                        LogInfo logInfo = thread == null ? null : ThreadLocalContext.getLogInfo(thread);
                        String computer = logInfo == null ? null : logInfo.hostnameComputer;
                        String user = logInfo == null ? null : logInfo.userName;
                        String userRoles = logInfo == null ? null : logInfo.userRoles;
                        String threadName = "";
                        ThreadInfo threadInfo = thread == null ? null : tBean.getThreadInfo(thread.getId());
                        if (threadInfo != null) {
                            threadName = threadInfo.getThreadName();
                        }

                        infos.add(new AllocatedInfo(user, userRoles, computer, threadName, bEntry.getKey(), deltaBytes, userMissed, userHit, userHitMap, userMissedMap));
                    }
                }
            }

            checkExceededAllocatedBytes(threadMap, excessAllocatedBytesSet);

            infos.sort((o1, o2) -> {
                long delta = o1.bytes - o2.bytes;
                return delta > 0 ? 1 : (delta < 0 ? -1 : 0);
            });
            for (AllocatedInfo info : infos) {
                allocatedBytesLogger.info(info);
            }
            
            if (logTotal) {
                allocatedBytesLogger.info(String.format("Exceeded: sum: %s, \t\t\tmissed-hit: All: %s-%s, %s",
                        humanReadableByteCount(bytesSum), exceededMisses, exceededMissesHits, CacheStats.getAbsoluteString(exceededHitMap, exceededMissedMap)));
                allocatedBytesLogger.info(String.format("Total: sum: %s, elapsed %sms, missed-hit: All: %s-%s, %s",
                        humanReadableByteCount(totalBytesSum), System.currentTimeMillis() - time, totalMissed, totalHit, CacheStats.getAbsoluteString(totalHitMap, totalMissedMap)));
            }
        }
    }

    private boolean isSystem(Map<Long, Thread> threadMap, long id) {
        boolean system = ThreadLocalContext.activeMap.get(threadMap.get(id)) == null || !ThreadLocalContext.activeMap.get(threadMap.get(id));
        if (!system) {
            Thread thread = threadMap.get(id);
            LogInfo logInfo = thread == null ? null : ThreadLocalContext.getLogInfo(thread);
            system = logInfo == null || logInfo.allowExcessAllocatedBytes;
        }
        return system;
    }

    private void checkExceededAllocatedBytes(Map<Long, Thread> threadMap, Set<Long> excessAllocatedBytesSet) {

        int accessInterruptCount = Settings.get().getExcessInterruptCount();

        for (Iterator<Map.Entry<Long, Integer>> it = excessAllocatedBytesMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, Integer> entry = it.next();
            Long id = entry.getKey();
            if (excessAllocatedBytesSet.contains(id)) {
                Integer count = entry.getValue();
                excessAllocatedBytesSet.remove(id);
                count = (count == null ? 0 : count) + 1;
                excessAllocatedBytesMap.put(id, count);
                allocatedBytesLogger.info(String.format("Process %s allocated too much bytes, %s cycles", id, count));
                if(count >= accessInterruptCount) {
                    allocatedBytesLogger.info(String.format("Process %s allocated too much bytes for %s cycles, will be interrupted", id, count));
                    try {
                        ThreadUtils.interruptThread(getDbManager(), threadMap.get(id));
                    } catch (SQLException | SQLHandledException e) {
                        allocatedBytesLogger.info(String.format("Failed to interrupt process %s", id));
                    }
                }
            } else
                it.remove();
        }
        for (Long id : excessAllocatedBytesSet)
            excessAllocatedBytesMap.put(id, 1);
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private void sumMap(HashMap<CacheType, Long> target, HashMap<CacheType, Long> source) {
        for (CacheType type : CacheType.values()) {
            target.put(type, nullToZero(target.get(type)) + nullToZero(source.get(type)));
        }
    }

    public List<Scheduler.SchedulerTask> getSystemTasks(Scheduler scheduler, boolean isServer) {
        List<Scheduler.SchedulerTask> result = new ArrayList<>();
        result.add(getChangeCurrentDateTask(scheduler));
        result.add(getChangeDataCurrentDateTimeTask(scheduler));
        result.add(getFlushAsyncValuesCachesTask(scheduler));
        result.add(resetResourcesCacheTasks(scheduler));

        if(!SystemProperties.inDevMode) { // чтобы не мешать при включенных breakPoint'ах
            result.add(getOpenFormCountUpdateTask(scheduler));
            result.add(getUserLastActivityUpdateTask(scheduler));
            result.add(getInitPingInfoUpdateTask(scheduler));
            result.add(getAllocatedBytesUpdateTask(scheduler));
            result.add(getCleanTempTablesTask(scheduler));
            result.add(getFlushPendingTransactionCleanersTask(scheduler));
//            result.add(getBalanceConnectionsTask(scheduler));
            result.add(getRestartConnectionsTask(scheduler));
            result.add(getUpdateSavePointsInfoTask(scheduler));
            result.add(getProcessDumpTask(scheduler));
        } else {
            result.add(getBalanceConnectionsTask(scheduler)); // to test

            result.add(getSynchronizeSourceTask(scheduler));
            result.add(getRecalculateAndUpdateStatsTask(scheduler));
        }
        return result;
    }
    
    private DataSession createSystemTaskSession() throws SQLException {
        return ThreadLocalContext.createSession();
    }

    private Scheduler.SchedulerTask getChangeCurrentDateTask(Scheduler scheduler) {
        return scheduler.createSystemTask(stack -> {
            try (DataSession session = createSystemTaskSession()) {
                session.setNoCancelInTransaction(true);

                LocalDate currentDate = (LocalDate) timeLM.currentDate.read(session);
                LocalDate newDate = LocalDate.now();
                if (currentDate == null || !currentDate.equals(newDate)) {
                    logger.info(String.format("ChangeCurrentDate started: from %s to %s", currentDate, newDate));
                    timeLM.currentDate.change(newDate, session);
                    session.applyException(this, stack);
                    logger.info("ChangeCurrentDate finished");
                }
            } catch (Exception e) {
                logger.error(String.format("ChangeCurrentDate error: %s", e), e);
            }
        }, true, Settings.get().getCheckCurrentDate(), true, "Changing current date");
    }

    private Scheduler.SchedulerTask getChangeDataCurrentDateTimeTask(Scheduler scheduler) {
        return scheduler.createSystemTask(stack -> {
            try (DataSession session = createSystemTaskSession()) {
                session.setNoCancelInTransaction(true);
                LocalDateTime newDataDateTime = LocalDateTime.now();
                Instant newZDataDateTime = Instant.now();
                logger.info("Change current time snapshots to " + newDataDateTime + ", " + newZDataDateTime);
                timeLM.currentDateTimeSnapshot.change(newDataDateTime, session);
                timeLM.currentZDateTimeSnapshot.change(newZDataDateTime, session);
                session.applyException(this, stack);
            } catch (Exception e) {
                logger.error(String.format("ChangeCurrentDateTime error: %s", e), e);
            }
        }, true, Settings.get().getCheckCurrentDataDateTime(), true, "Changing current dateTime");
    }


    private Scheduler.SchedulerTask getOpenFormCountUpdateTask(Scheduler scheduler) {
        return scheduler.createSystemTask(stack -> {
            try(DataSession session = createSystemTaskSession()) {
                RemoteNavigator.updateOpenFormCount(BusinessLogics.this, session, stack);
            }
        }, false, Settings.get().getUpdateFormCountPeriod(), false, "Open Form Count");
    }

    private Scheduler.SchedulerTask getUserLastActivityUpdateTask(Scheduler scheduler) {
        return scheduler.createSystemTask(stack -> {
            try(DataSession session = createSystemTaskSession()) {
                RemoteNavigator.updateUserLastActivity(BusinessLogics.this, session, stack);
            }
        }, false, Settings.get().getUpdateUserLastActivity(), false, "User Last Activity");
    }

    private Scheduler.SchedulerTask getInitPingInfoUpdateTask(Scheduler scheduler) {
        return scheduler.createSystemTask(stack -> {
            try(DataSession session = createSystemTaskSession()) {
                RemoteNavigator.updatePingInfo(BusinessLogics.this, session, stack);
            }
        }, false, Settings.get().getUpdatePingInfo(), false, "Ping Info");
    }

    private Scheduler.SchedulerTask getCleanTempTablesTask(Scheduler scheduler) {
        return scheduler.createSystemTask(stack -> SQLSession.cleanTemporaryTables(), false, Settings.get().getTempTablesTimeThreshold(), false, "Drop Temp Tables");
    }

    private Scheduler.SchedulerTask getFlushPendingTransactionCleanersTask(Scheduler scheduler) {
        return scheduler.createSystemTask(stack -> DataSession.flushPendingTransactionCleaners(), false, Settings.get().getFlushPendingTransactionCleanersThreshold(), false, "Flush Pending Transaction Cleaners");
    }

    private Scheduler.SchedulerTask getFlushAsyncValuesCachesTask(Scheduler scheduler) {
        return scheduler.createSystemTask(stack -> getDbManager().flushChanges(), false, Settings.get().getFlushAsyncValuesCaches(), false, "Flush async values caches");
    }

    private Scheduler.SchedulerTask getBalanceConnectionsTask(Scheduler scheduler) {
        return scheduler.createSystemTask(stack -> SQLSession.balanceConnections(), false, Settings.get().getPeriodBalanceConnections(), false, "Connection balancing");
    }

    private Scheduler.SchedulerTask getRestartConnectionsTask(Scheduler scheduler) {
        final Result<Double> prevStart = new Result<>(0.0);
        return scheduler.createSystemTask(stack -> SQLSession.restartConnections(prevStart), false, Settings.get().getPeriodRestartConnections(), false, "Connection restart");
    }

    private Scheduler.SchedulerTask getUpdateSavePointsInfoTask(Scheduler scheduler) {
        final Result<Long> prevResult = new Result<>(null);
        return scheduler.createSystemTask(stack -> getDbManager().getAdapter().updateSavePointsInfo(prevResult), false, Settings.get().getUpdateSavePointsPeriod(), false, "Update save points thresholds");
    }


    private Scheduler.SchedulerTask getProcessDumpTask(Scheduler scheduler) {
        return scheduler.createSystemTask(stack -> {
            try(DataSession session = createSystemTaskSession()) {
                serviceLM.makeProcessDumpAction.execute(session, stack);
            }
        }, false, Settings.get().getPeriodProcessDump(), false, "Process Dump");
    }

    private Scheduler.SchedulerTask getRecalculateAndUpdateStatsTask(Scheduler scheduler) {
        return scheduler.createSystemTask(stack -> {
            try(DataSession session = createSystemTaskSession()) {
                DBManager dbManager = getDbManager();

                ImSet<ImplementTable> updateTables;
                ImSet<ConcreteCustomClass> updateClasses;
                synchronized (LM.tableFactory.syncLock) {
                    MExclSet<ImplementTable> mUpdateTables = SetFact.mExclSet();
                    MExclSet<ConcreteCustomClass> mUpdateClasses = SetFact.mExclSet();
                    for (ImplementTable table : LM.tableFactory.getImplementTables()) {
                        long changed = table.statChange.get();
                        if (table.getStatKeys().getRows().majorStatChanged(new Stat(changed >= 0 ? changed : -changed), changed >= 0 ? Stat.Mode.ADD : Stat.Mode.REMOVE)) {
                            table.statChange.set(0);

                            table.recalculateStat(reflectionLM, new HashSet<>(), session);
                            mUpdateTables.exclAdd(table);

                            ObjectValueClassSet classSet = table.getClassDataSet();
                            if (classSet != null) {
                                classSet.recalculateClassStat(LM, session);
                                mUpdateClasses.exclAddAll(classSet.getSetConcreteChildren());
                            }
                        }
                    }
                    updateTables = mUpdateTables.immutable();
                    updateClasses = mUpdateClasses.immutable();

                    if(updateTables.isEmpty() && updateClasses.isEmpty()) // optimization
                        return;

                    session.applyException(this, stack);
                }

                dbManager.updateStats(session.sql, updateTables, updateClasses);
            } catch (Throwable t) {
                ServerLoggers.serviceLogger.info("Recalculate and update stats task error: ", t);
            }
        }, false, 1, false, "RecalculateAndUpdateStats");
    }

    private Scheduler.SchedulerTask getSynchronizeSourceTask(Scheduler scheduler) {
        SynchronizeSourcesWatcher sourcesWatcher = new SynchronizeSourcesWatcher();
        return scheduler.createSystemTask(stack -> sourcesWatcher.watch("lsf", ""),
                true, null, false, "Synchronizing resources from sources to build. Only for debug");
    }

    private Scheduler.SchedulerTask getAllocatedBytesUpdateTask(Scheduler scheduler) {
        return scheduler.createSystemTask(stack -> updateThreadAllocatedBytesMap(), false, Settings.get().getThreadAllocatedMemoryPeriod() / 2, false, "Allocated Bytes");
    }

    private Scheduler.SchedulerTask resetResourcesCacheTasks(Scheduler scheduler) {
        ClearCacheWatcher clearCacheWatcher = new ClearCacheWatcher();
        clearCacheWatcher.walkAndRegisterDirectories(Arrays.stream(ResourceUtils.getClassPathElements())
                .filter(element -> !isRedundantString(element) && !element.endsWith("*"))
                .map(element -> Paths.get(element + "/"))
                .filter(Files::isDirectory)
                .collect(Collectors.toList()));

        return scheduler.createSystemTask(stack -> clearCacheWatcher.watch(), true, null, false, "Reset resources cache");
    }

    private class AllocatedInfo {
        private final String user;
        private final String userRoles;
        private final String computer;
        private final String threadName;
        private final Long pid;
        private final Long bytes;
        private final long userMissed;
        private final long userHit;
        private final HashMap<CacheType, Long> userHitMap;
        private final HashMap<CacheType, Long> userMissedMap;

        AllocatedInfo(String user, String userRoles, String computer, String threadName, Long pid, Long bytes, long userMissed, long userHit, HashMap<CacheType, Long> userHitMap, HashMap<CacheType, Long> userMissedMap) {
            this.user = user;
            this.userRoles = userRoles;
            this.computer = computer;
            this.threadName = threadName;
            this.pid = pid;
            this.bytes = bytes;
            this.userMissed = userMissed;
            this.userHit = userHit;
            this.userHitMap = userHitMap;
            this.userMissedMap = userMissedMap;
        }

        @Override
        public String toString() {
            String userMessage = String.format("PID %s: %s, Thread %s", pid, humanReadableByteCount(bytes), threadName);
            if (user != null) {
                userMessage += String.format(", Comp. %s, User %s, Roles %s", computer == null ? "unknown" : computer, user, userRoles);
            }
            userMessage += String.format(", missed-hit: All: %s-%s, %s", userMissed, userHit, CacheStats.getAbsoluteString(userHitMap, userMissedMap));

            return userMessage;
        }
    }
}
