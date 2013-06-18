package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLSession;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.HSet;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.interop.event.IDaemonTask;
import lsfusion.interop.form.screen.ExternalScreen;
import lsfusion.interop.form.screen.ExternalScreenParameters;
import lsfusion.server.ServerLoggers;
import lsfusion.server.SystemProperties;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.LogFormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.FormActionProperty;
import lsfusion.server.logics.property.actions.SessionEnvEvent;
import lsfusion.server.logics.property.actions.SystemEvent;
import lsfusion.server.logics.property.actions.flow.ChangeFlowType;
import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.session.DataSession;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.isRedundantString;
import static lsfusion.base.BaseUtils.systemLogger;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public abstract class BusinessLogics<T extends BusinessLogics<T>> extends LifecycleAdapter implements InitializingBean {
    protected final static Logger logger = ServerLoggers.systemLogger;
    protected final static Logger debuglogger = Logger.getLogger(BusinessLogics.class);

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

    protected LogicsInstance logicsInstance;

    private Boolean dialogUndecorated = false;

    private String overridingModulesList;

    //чтобы можно было использовать один инстанс логики с несколькими инстансами, при этом инициализировать только один раз
    private final AtomicBoolean initialized = new AtomicBoolean();

    public BusinessLogics() {
        super(LOGICS_ORDER);
    }

    public void setOverridingModulesList(String overridingModulesList) {
        this.overridingModulesList = overridingModulesList;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        if (initialized.compareAndSet(false, true)) {
            logger.info("Initializing BusinessLogics");
            try {
                createModules();
                initModules();

                if (!SystemProperties.isDebug) {
                    prereadCaches();
                }
                initExternalScreens();

            } catch (ScriptParsingException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Error initializing BusinessLogics: ", e);
            }
        }
    }

    public LogicsModule getModule(String name) {
        return nameToModule.get(name);
    }

    public Boolean isDialogUndecorated() {
        return dialogUndecorated;
    }

    public void setDialogUndecorated(Boolean dialogUndecorated) {
        this.dialogUndecorated = dialogUndecorated;
    }

    public ConcreteClass getDataClass(Object object, Type type) {
        try {
            DataSession session = getDbManager().createSession();
            ConcreteClass result = type.getDataClass(object, session.sql, LM.baseClass.getUpSet(), LM.baseClass);
            session.close();

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<IDaemonTask> getDaemonTasks(int compId) {
        return new ArrayList<IDaemonTask>();
    }

    protected void initExternalScreens() {
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

    public Property getProperty(String sid) {
        return LM.rootGroup.getProperty(sid);
    }

    protected <T extends LogicsModule> T addModule(T module) {
        logicModules.add(module);
        return module;
    }

    protected void createModules() throws IOException {
        LM = addModule(new BaseLogicsModule(this));
        serviceLM = addModule(new ServiceLogicsModule(this, LM));
        reflectionLM = addModule(new ReflectionLogicsModule(this, LM));
        contactLM = addModule(new ContactLogicsModule(this, LM));
        authenticationLM = addModule(new AuthenticationLogicsModule(this, LM));
        securityLM = addModule(new SecurityLogicsModule(this, LM));
        systemEventsLM = addModule(new SystemEventsLogicsModule(this, LM));
        emailLM = addModule(new EmailLogicsModule(this, LM));
        schedulerLM = addModule(new SchedulerLogicsModule(this, LM));
        timeLM = addModule(new TimeLogicsModule(this, LM));
    }

    protected void addModulesFromResource(List<String> paths, List<String> excludedPaths) throws IOException {

        List<String> excludedLSF = new ArrayList<String>();

        if (excludedPaths != null) {
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
        return addModule(new ScriptingLogicsModule(is, LM, this));
    }

    private void fillNameToModules() {
        for (LogicsModule module : logicModules) {
            if (nameToModule.containsKey(module.getName())) {
                throw new RuntimeException(String.format("[error]:\tmodule '%s' has already been added", module.getName()));
            }
            nameToModule.put(module.getName(), module);
        }
    }

    private Map<String, List<String>> buildModuleGraph() {
        Map<String, List<String>> graph = new HashMap<String, List<String>>();
        for (LogicsModule module : logicModules) {
            graph.put(module.getName(), new ArrayList<String>());
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

    private void checkCycles(String cur, LinkedList<String> way, Set<String> used, Map<String, List<String>> graph) {
        way.add(cur);
        used.add(cur);
        for (String next : graph.get(cur)) {
            if (!used.contains(next)) {
                checkCycles(next, way, used, graph);
            } else if (way.contains(next)) {
                String errMsg = next;
                do {
                    errMsg = errMsg + " <- " + way.peekLast();
                } while (!way.pollLast().equals(next));
                throw new RuntimeException("[error]:\tthere is a circular dependency: " + errMsg);
            }
        }
        way.removeLast();
    }

    private void checkCycles(Map<String, List<String>> graph) {
        Set<String> used = new HashSet<String>();
        for (Map.Entry<String, List<String>> vertex : graph.entrySet()) {
            if (!used.contains(vertex.getKey())) {
                checkCycles(vertex.getKey(), new LinkedList<String>(), used, graph);
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

    private List<LogicsModule> orderModules() {
        fillNameToModules();
        Map<String, List<String>> graph = buildModuleGraph();
//        outDotFile();
        checkCycles(graph);

        Map<String, Integer> degree = new HashMap<String, Integer>();
        for (LogicsModule module : logicModules) {
            degree.put(module.getName(), module.getRequiredModules().size());
        }

        Set<LogicsModule> usedModules = new LinkedHashSet<LogicsModule>();
        for (int i = 0; i < logicModules.size(); ++i) {
            for (LogicsModule module : logicModules) {
                if (degree.get(module.getName()) == 0 && !usedModules.contains(module)) {
                    for (String nextModule : graph.get(module.getName())) {
                        degree.put(nextModule, degree.get(nextModule) - 1);
                    }
                    usedModules.add(module);
                    break;
                }
            }
        }
        return new ArrayList<LogicsModule>(usedModules);
    }

    private void overrideModulesList(String startModulesList) {

        Set<LogicsModule> was = new HashSet<LogicsModule>();
        Queue<LogicsModule> queue = new LinkedList<LogicsModule>();

        fillNameToModules();

        for (String moduleName : startModulesList.split(",\\s*")) {
            LogicsModule startModule = nameToModule.get(moduleName);
            assert startModule != null;
            queue.add(startModule);
            was.add(startModule);
        }

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

    private void initModules() {
        Exception initException = null;
        try {
            for (LogicsModule module : logicModules) {
                module.initModuleDependencies();
            }

            if (!isRedundantString(overridingModulesList)) {
                overrideModulesList(overridingModulesList);
            }

            logger.info("Initializing modules.");
            List<LogicsModule> orderedModules = orderModules();

            for (LogicsModule module : orderedModules) {
                String namespace = module.getNamespace();
                if (!namespaceToModules.containsKey(namespace)) {
                    namespaceToModules.put(namespace, new ArrayList<LogicsModule>());
                }
                namespaceToModules.get(namespace).add(module);
            }

            for (LogicsModule module : orderedModules) {
                module.initModule();
            }

            logger.info("Initializing property groups.");
            for (LogicsModule module : orderedModules) {
                module.initGroups();
            }

            logger.info("Initializing classes.");
            for (LogicsModule module : orderedModules) {
                module.initClasses();
            }

            LM.baseClass.initObjectClass();
            LM.storeCustomClass(LM.baseClass.objectClass);

            logger.info("Initializing tables.");
            for (LogicsModule module : orderedModules) {
                module.initTables();
            }

            logger.info("Initializing class tables.");
            initClassDataProps();

            logger.info("Initializing properties.");
            int i = 1;
            for (LogicsModule module : orderedModules) {
                logger.info(String.format("Initializing properties for module #%d of %d: %s", i++, orderedModules.size(), module.getName()));
                module.initProperties();
            }

            logger.info("Finalizing abstracts.");
            finishAbstract();

            logger.info("Finalizing actions.");
            finishActions();

            logger.info("Setup loggables.");
            finishLogInit();

            if (!SystemProperties.isDebug) {
                logger.info("Setup drill-down.");
                setupDrillDown();

                logger.info("Setup property policy.");
                setupPropertyPolicyForms();
            }

            logger.info("Showing dependencies.");
            showDependencies();

            logger.info("Building check property list.");
            getPropertyList(true);
            logger.info("Building property list.");
            ImOrderSet<Property> propertyList = getPropertyList();
            logger.info("Finalizing property list.");
            for(Property property : propertyList) {
                property.finalizeAroundInit();
            }

            logger.info("Initializing class forms.");
            LM.initClassForms();

//            Set idSet = new HashSet<String>();
//            for (Property property : getOrderProperties()) {
//                assert idSet.add(property.getObjectSID()) : "Same sid " + property.getObjectSID();
//            }
            logger.info("Initializing indices.");
            for (LogicsModule module : orderedModules) {
                module.initIndexes();
            }

        } catch (RecognitionException e) {
            initException = new ScriptParsingException(e.getMessage());
        } catch (Exception e) {
            initException = e;
        }

        String syntaxErrors = "";
        for (LogicsModule module : logicModules) {
            syntaxErrors += module.getErrorsDescription();
        }

        if (!syntaxErrors.isEmpty()) {
            if (initException != null) {
                syntaxErrors = syntaxErrors + initException.getMessage();
            }
            throw new ScriptParsingException(syntaxErrors);
        } else if (initException != null) {
            Throwables.propagate(initException);
        }
    }

    private void initClassDataProps() {
        ImMap<ImplementTable, ImSet<ConcreteCustomClass>> groupTables = getConcreteCustomClasses().group(new BaseUtils.Group<ImplementTable, ConcreteCustomClass>() {
            public ImplementTable group(ConcreteCustomClass customClass) {
                return LM.tableFactory.getMapTable(MapFact.singleton("key", (ValueClass) customClass)).table;
            }
        });

        for(int i=0,size=groupTables.size();i<size;i++) {
            ImplementTable table = groupTables.getKey(i);
            ImSet<ConcreteCustomClass> set = groupTables.getValue(i);

            ObjectValueClassSet classSet = OrObjectClassSet.fromSetConcreteChildren(set);
            ClassDataProperty dataProperty = new ClassDataProperty(table.name+"_class", classSet.toString(), classSet);
            LM.addProperty(null, new LCP<ClassPropertyInterface>(dataProperty));
            dataProperty.markStored(LM.tableFactory, table);

            for(ConcreteCustomClass customClass : set)
                customClass.dataProperty = dataProperty;
        }
    }

    private void finishAbstract() {
        for (Property property : getOrderProperties())
            if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract())
                property.finalizeInit();

        for (Property property : getOrderProperties())
            if(property instanceof CaseUnionProperty) {
                ((CaseUnionProperty)property).checkAbstract();
            }
    }

    private void finishActions() { // потому как могут использовать abstract
        for (Property property : getOrderProperties())
            if(property instanceof ActionProperty) {
                if(property instanceof ListCaseActionProperty && ((ListCaseActionProperty)property).isAbstract())
                    property.finalizeInit();

                if(!((ActionProperty)property).getEvents().isEmpty()) { // вырежем Action'ы без Event'ов, они нигде не используются, а дают много компонент связности
                    ImMap<CalcProperty, Boolean> change = ((ActionProperty<?>) property).getChangeExtProps();
                    for(int i=0,size=change.size();i<size;i++) // вообще говоря DataProperty и IsClassProperty
                        change.getKey(i).addActionChangeProp(new Pair<Property<?>, LinkType>(property, change.getValue(i) ? LinkType.RECCHANGE : LinkType.DEPEND));
                }
            }
    }

    private void finishLogInit() {
        // с одной стороны нужно отрисовать на форме логирования все свойства из recognizeGroup, с другой - LogFormEntity с Action'ом должен уже существовать
        // поэтому makeLoggable делаем сразу, а LogFormEntity при желании заполняем здесь
        for (Property property : getOrderProperties()) {
            if (property.loggable && property.logFormProperty.property instanceof FormActionProperty) {
                FormActionProperty formActionProperty = (FormActionProperty) property.logFormProperty.property;
                if (formActionProperty.form instanceof LogFormEntity) {
                    LogFormEntity logForm = (LogFormEntity) formActionProperty.form;
                    if (logForm.lazyInit) {
                        logForm.initProperties();
                    }
                }

                //добавляем в контекстное меню пункт для показа формы
                String actionSID = formActionProperty.getSID();
                property.setContextMenuAction(actionSID, formActionProperty.caption);
                property.setEditAction(actionSID, formActionProperty.getImplement(property.getOrderInterfaces()));
            }
        }
    }

    private void setupDrillDown() {
        for (Property property : getOrderProperties()) {
            if (property.supportsDrillDown()) {
                DrillDownFormEntity drillDownFormEntity = property.createDrillDownForm(this);
                if (drillDownFormEntity != null) {
                    String drillDownActionSID = LM.isGeneratedSID(property.getSID()) ? LM.genSID() : "drillDownAction_" + property.getSID();
                    LAP<?> drillDownFormProperty =
                            LM.addMFAProp(LM.drillDownGroup, drillDownActionSID, getString("logics.property.drilldown.action"), drillDownFormEntity, drillDownFormEntity.paramObjects, property.drillDownInNewSession());

                    ActionProperty formProperty = drillDownFormProperty.property;
                    property.setContextMenuAction(formProperty.getSID(), formProperty.caption);
                    property.setEditAction(formProperty.getSID(), formProperty.getImplement(property.getOrderInterfaces()));
                }
            }
        }
    }

    private void setupPropertyPolicyForms() {
        FormEntity policyFormEntity = securityLM.propertyPolicyForm;
        ObjectEntity propertyObj = policyFormEntity.getObject("p");
        LAP<?> setupPolicyFormProperty = LM.addMFAProp(null, "sys", policyFormEntity, new ObjectEntity[]{propertyObj}, true);
        LAP<?> setupPolicyForPropBySID = LM.addJoinAProp(setupPolicyFormProperty, reflectionLM.propertySID, 1);

        for (Property property : getOrderProperties()) {
            String propertySID = property.getSID();
            String setupPolicyActionSID = "propertyPolicySetup_" + propertySID;
            if (!LM.isGeneratedSID(propertySID) &&
                    //todo: убрать проверку, когда пофиксится баг с поиском дубликатов только в зависимостях, а не во всей логике
                    LM.getLPBySID(setupPolicyActionSID) == null) {
                LAP<?> setupPolicyLAP = LM.addJoinAProp(LM.propertyPolicyGroup, setupPolicyActionSID, getString("logics.property.propertypolicy.action"),
                                                        setupPolicyForPropBySID, LM.addCProp(StringClass.get(propertySID.length()), propertySID));

                ActionProperty setupPolicyAction = setupPolicyLAP.property;
                property.setContextMenuAction(setupPolicyAction.getSID(), setupPolicyAction.caption);
                property.setEditAction(setupPolicyAction.getSID(), setupPolicyAction.getImplement());
            }
        }
    }

    private void prereadCaches() {
        getAppliedProperties(true);
        getAppliedProperties(false);
        getMapSingleApplyDepends();
        for (Property property : getPropertyList())
            property.prereadCaches();
    }

    protected void initAuthentication(SecurityManager securityManager) throws SQLException {
        securityManager.setupDefaultAdminUser();
    }

    public ImOrderSet<Property> getOrderProperties() {
        return LM.rootGroup.getProperties();
    }

    public ImSet<Property> getProperties() {
        return getOrderProperties().getSet();
    }

    public List<AbstractGroup> getParentGroups() {
        return LM.rootGroup.getParentGroups();
    }

    public ImOrderSet<Property> getPropertyList() {
        return getPropertyList(false);
    }

    // находит свойство входящее в "верхнюю" сильносвязную компоненту
    private static HSet<Link> goDown(Property<?> property, MAddMap<Property, HSet<Link>> linksMap, List<Property> order, HSet<Link> removedLinks, boolean include, HSet<Property> component) {
        HSet<Link> linksIn = linksMap.get(property);
        if (linksIn == null) { // уже были, linksMap - одновременно используется и как пометки, и как список, и как обратный обход
            linksIn = new HSet<Link>();
            linksMap.add(property, linksIn);

            ImSet<Link> links = property.getLinks();
            for (int i = 0,size = links.size(); i < size; i++) {
                Link link = links.get(i);
                if (!removedLinks.contains(link) && component.contains(link.to) == include)
                    goDown(link.to, linksMap, order, removedLinks, include, component).add(link);
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
    private static HSet<Property> buildList(HSet<Property> props, HSet<Property> exclude, HSet<Link> removedLinks, MOrderExclSet<Property> mResult) {
        HSet<Property> proceeded;

        List<Property> order = new ArrayList<Property>();
        MAddMap<Property, HSet<Link>> linksMap = MapFact.mAddOverrideMap();
        for (int i = 0; i < props.size; i++) {
            Property property = props.get(i);
            if (linksMap.get(property) == null) // проверка что не было
                goDown(property, linksMap, order, removedLinks, exclude == null, exclude != null ? exclude : props);
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
                        buildList(innerComponent, null, removedLinks, mCycle);
                        ImOrderSet<Property> cycle = mCycle.immutableOrder();

                        String print = "";
                        for (Property property : cycle)
                            print = (print.length() == 0 ? "" : print + " -> ") + property.toString();
                        throw new RuntimeException(getString("message.cycle.detected") + " : " + print + " -> " + minLink.result.to);
                    }
                    buildList(innerComponent, null, removedLinks, mResult);
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

        for (Link link : property.getLinks()) {
            path.push(link);
            if (link.type.getNum() <= desiredType.getNum() && findDependency(link.to, with, proceeded, path, desiredType))
                return true;
            path.pop();
        }

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

    private void showDependencies() {
        String show = "";
        for (Property property : getOrderProperties())
            if (property.showDep != null)
                show += findDependency(property, property.showDep, LinkType.DEPEND);
        if (!show.isEmpty()) {
            systemLogger.debug("Dependencies: " + show);
        }
    }

    @IdentityStrongLazy // глобальное очень сложное вычисление
    public ImOrderSet<Property> getPropertyList(boolean onlyCheck) {
        // жестковато тут конечно написано, но пока не сильно времени жрет

        // сначала бежим по Action'ам с cancel'ами
        HSet<Property> cancelActions = new HSet<Property>();
        HSet<Property> rest = new HSet<Property>();
        for (Property property : getOrderProperties())
            if (property instanceof ActionProperty && ((ActionProperty) property).hasFlow(ChangeFlowType.CANCEL))
                cancelActions.add(property);
            else
                rest.add(property);

        MOrderExclSet<Property> mCancelResult = SetFact.mOrderExclSet();
        HSet<Property> proceeded = buildList(cancelActions, new HSet<Property>(), new HSet<Link>(), mCancelResult);
        ImOrderSet<Property> cancelResult = mCancelResult.immutableOrder();
        if (onlyCheck)
            return cancelResult.reverseOrder();

        // потом бежим по всем остальным, за исключением proceeded
        MOrderExclSet<Property> mRestResult = SetFact.mOrderExclSet();
        HSet<Property> removed = new HSet<Property>();
        removed.addAll(rest.remove(proceeded));
        buildList(removed, proceeded, new HSet<Link>(), mRestResult); // потом этот cast уберем
        ImOrderSet<Property> restResult = mRestResult.immutableOrder();

        // затем по всем кроме proceeded на прошлом шаге
        assert cancelResult.getSet().disjoint(restResult.getSet());
        return cancelResult.reverseOrder().addOrderExcl(restResult.reverseOrder());
    }

    List<AggregateProperty> getAggregateStoredProperties() {
        List<AggregateProperty> result = new ArrayList<AggregateProperty>();
        for (Property property : getStoredProperties())
            if (property instanceof AggregateProperty)
                result.add((AggregateProperty) property);
        return result;
    }

    @IdentityLazy
    public ImOrderSet<CalcProperty> getSingleApplyStoredProperties() {
        return BaseUtils.immutableCast(getPropertyList().filterOrder(new SFunctionSet<Property>() {
            public boolean contains(Property property) {
                return property instanceof CalcProperty && ((CalcProperty) property).isSingleApplyStored();
            }}));
    }

    @IdentityLazy
    public ImOrderSet<CalcProperty> getStoredProperties() {
        return BaseUtils.immutableCast(getPropertyList().filterOrder(new SFunctionSet<Property>() {
            public boolean contains(Property property) {
                return property instanceof CalcProperty && ((CalcProperty) property).isStored();
            }}));
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
    public ImOrderMap<OldProperty, SessionEnvEvent> getApplyEventDependProps() {
        MOrderMap<OldProperty, SessionEnvEvent> mResult = MapFact.mOrderMap(SessionEnvEvent.<OldProperty>mergeSessionEnv());
        for (Property<?> property : getPropertyList()) {
            SessionEnvEvent sessionEnv;
            if (property instanceof ActionProperty && (sessionEnv = ((ActionProperty) property).getSessionEnv(SystemEvent.APPLY))!=null)
                mResult.addAll(property.getOldDepends().toMap(sessionEnv).toOrderMap());
            if (property instanceof DataProperty && ((DataProperty) property).event != null)
                mResult.addAll(((DataProperty) property).event.getOldDepends().toMap(SessionEnvEvent.ALWAYS).toOrderMap());
        }
        ImOrderMap<OldProperty, SessionEnvEvent> result = mResult.immutableOrder();

        assert result.keys().filterFn(new SFunctionSet<OldProperty>() {
            public boolean contains(OldProperty element) {
                return !element.scope.onlyDB();
            }}).isEmpty();

        return result;
    }

    @IdentityLazy
    public ImOrderMap<Object, SessionEnvEvent> getAppliedProperties(boolean onlyCheck) {
        // здесь нужно вернуть список stored или тех кто
        ImOrderSet<Property> list = getPropertyList(onlyCheck);
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

    private void fillSingleApplyDependFrom(CalcProperty<?> fill, CalcProperty<?> applied, SessionEnvEvent appliedSet, Map<CalcProperty, MMap<CalcProperty, SessionEnvEvent>> mapDepends) {
        if (!fill.equals(applied) && fill.isSingleApplyStored())
            mapDepends.get(fill).add(applied, appliedSet);
        else
            for (CalcProperty depend : fill.getDepends(false)) // derived'ы отдельно отрабатываются
                fillSingleApplyDependFrom(depend, applied, appliedSet, mapDepends);
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

    // assert что key property is stored, а value property is stored или instanceof OldProperty
    @IdentityStrongLazy // глобальное очень сложное вычисление
    private ImMap<CalcProperty, ImOrderMap<CalcProperty, SessionEnvEvent>> getMapSingleApplyDepends() {
        Map<CalcProperty, MMap<CalcProperty, SessionEnvEvent>> mapDepends = new HashMap<CalcProperty, MMap<CalcProperty, SessionEnvEvent>>();
        for (CalcProperty property : getSingleApplyStoredProperties()) {
            mapDepends.put(property, MapFact.mMap(SessionEnvEvent.<CalcProperty>mergeSessionEnv()));
            fillSingleApplyDependFrom(property, property, SessionEnvEvent.ALWAYS, mapDepends);
        }
        ImOrderMap<OldProperty, SessionEnvEvent> eventDependProps = getApplyEventDependProps();
        for (int i=0,size=eventDependProps.size();i<size;i++) {
            OldProperty old = eventDependProps.getKey(i);
            fillSingleApplyDependFrom(old.property, old, eventDependProps.getValue(i), mapDepends);
        }

        ImRevMap<Property, Integer> indexMap = getPropertyList().mapOrderRevValues(new GetIndex<Integer>() {
            public Integer getMapValue(int i) {
                return i;
            }
        });
        MExclMap<CalcProperty, ImOrderMap<CalcProperty, SessionEnvEvent>> mOrderedMapDepends = MapFact.mExclMap(mapDepends.size());
        for (Map.Entry<CalcProperty, MMap<CalcProperty, SessionEnvEvent>> mapDepend : mapDepends.entrySet()) {
            ImMap<CalcProperty, SessionEnvEvent> depends = mapDepend.getValue().immutable();
            ImOrderSet<CalcProperty> dependList = indexMap.filterInclRev(depends.keys()).reverse().sort().valuesList().toOrderExclSet();
            assert dependList.size() == depends.size();
            mOrderedMapDepends.exclAdd(mapDepend.getKey(), dependList.mapOrderMap(depends));
        }
        return mOrderedMapDepends.immutable();
    }

    // определяет для stored свойства зависимые от него stored свойства, а также свойства которым необходимо хранить изменения с начала транзакции (constraints и derived'ы)
    public ImOrderMap<CalcProperty, SessionEnvEvent> getSingleApplyDependFrom(CalcProperty property) {
        assert property.isSingleApplyStored();
        return getMapSingleApplyDepends().get(property);
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

    public void recalculateStats(DataSession session) throws SQLException {
        for (ImplementTable dataTable : LM.tableFactory.getImplementTables()) {
            dataTable.calculateStat(this.reflectionLM, session);
        }
    }

    public void recalculateFollows(DataSession session) throws SQLException {
        for (Property property : getPropertyList())
            if (property instanceof ActionProperty) {
                ActionProperty<?> action = (ActionProperty) property;
                if (action.getSessionEnv(SystemEvent.APPLY)==SessionEnvEvent.ALWAYS && action.resolve)
                    session.resolve(action);
            }
    }

    public String checkClasses(DataSession session) throws SQLException {
        String message = session.checkClasses();
        for (Property property : getPropertyList())
            if (property instanceof StoredDataProperty)
                message += session.checkClasses((StoredDataProperty)property);
        return message;
    }

    public void recalculateClasses(SQLSession session) throws SQLException {
        for (Property property : getPropertyList())
            if (property instanceof StoredDataProperty)
                ((StoredDataProperty)property).recalculateClasses(session, LM.baseClass);;
    }

    protected LP getLP(String sID) {
        return LM.getLP(sID);
    }

    public LCP getLCP(String sID) {
        return (LCP) LM.getLP(sID);
    }

    protected LAP getLAP(String sID) {
        return (LAP) LM.getLP(sID);
    }

    private void outputCalcPropertyClasses() {
        for (LP lp : LM.lproperties) {
            debuglogger.debug(lp.property.getSID() + " : " + lp.property.caption + " - " + lp.getClassWhere(ClassType.ASIS));
        }
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

    Collection<FormEntity> getFormEntities(){
        Collection<FormEntity> result = new ArrayList<FormEntity>();
        for(LogicsModule logicsModule : logicModules) {
            for(NavigatorElement entry : logicsModule.moduleNavigators.values())
                if(entry instanceof FormEntity)
                    result.add((FormEntity) entry);
        }
        return result;
    }

    Collection<NavigatorElement> getNavigatorElements(){
        Collection<NavigatorElement> result = new ArrayList<NavigatorElement>();
        for(LogicsModule logicsModule : logicModules) {
            result.addAll(logicsModule.moduleNavigators.values());
        }
        return result;
    }

    public FormEntity getFormEntity(String formSID){
        for(LogicsModule logicsModule : logicModules) {
            for(NavigatorElement entry : logicsModule.moduleNavigators.values())
                if((formSID.equals(entry.getSID()))&& (entry instanceof FormEntity))
                    return (FormEntity) entry;
        }
        return null;
    }

    // Набор методов для поиска модуля, в котором находится элемент системы
    private LogicsModule getModuleContainingObject(String namespaceName, String name, LogicsModule.ModuleFinder finder) {
        List<LogicsModule> modules = namespaceToModules.get(namespaceName);
        if(modules==null)
            return null;
        for (LogicsModule module : modules) {
            if (finder.resolveInModule(module, name) != null) {
                return module;
            }
        }
        return null;
    }

    public LogicsModule getModuleContainingLP(String namespaceName, String name) {
        return getModuleContainingObject(namespaceName, name, new LogicsModule.LPNameModuleFinder());
    }

    public LogicsModule getModuleContainingGroup(String namespaceName, String name) {
        return getModuleContainingObject(namespaceName, name, new LogicsModule.GroupNameModuleFinder());
    }

    public LogicsModule getModuleContainingClass(String namespaceName, String name) {
        return getModuleContainingObject(namespaceName, name, new LogicsModule.ClassNameModuleFinder());
    }

    public LogicsModule getModuleContainingTable(String namespaceName, String name) {
        return getModuleContainingObject(namespaceName, name, new LogicsModule.TableNameModuleFinder());
    }

    public LogicsModule getModuleContainingWindow(String namespaceName, String name) {
        return getModuleContainingObject(namespaceName, name, new LogicsModule.WindowNameModuleFinder());
    }

    public LogicsModule getModuleContainingNavigatorElement(String namespaceName, String name) {
        return getModuleContainingObject(namespaceName, name, new LogicsModule.NavigatorElementNameModuleFinder());
    }

    public LogicsModule getModuleContainingMetaCode(String namespaceName, String name, int paramCnt) {
        return getModuleContainingObject(namespaceName, name, new LogicsModule.MetaCodeNameModuleFinder(paramCnt));
    }

    protected DBManager getDbManager() {
        return ThreadLocalContext.getDbManager();
    }
}
