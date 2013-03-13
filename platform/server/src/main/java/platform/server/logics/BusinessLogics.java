package platform.server.logics;

import com.google.common.base.Throwables;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import platform.base.*;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.implementations.HSet;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MOrderExclSet;
import platform.base.col.interfaces.mutable.MOrderSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.add.MAddMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.interop.event.IDaemonTask;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.server.SystemProperties;
import platform.server.caches.IdentityLazy;
import platform.server.caches.IdentityStrongLazy;
import platform.server.classes.ConcreteClass;
import platform.server.context.ThreadLocalContext;
import platform.server.data.type.Type;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.LogFormEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.lifecycle.LifecycleAdapter;
import platform.server.lifecycle.LifecycleEvent;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.FormActionProperty;
import platform.server.logics.property.actions.SystemEvent;
import platform.server.logics.property.actions.flow.ChangeFlowType;
import platform.server.logics.property.actions.flow.ListActionProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.logics.table.DataTable;
import platform.server.logics.table.ImplementTable;
import platform.server.session.DataSession;

import java.io.*;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static platform.base.BaseUtils.isRedundantString;
import static platform.server.logics.ServerResourceBundle.getString;

public abstract class BusinessLogics<T extends BusinessLogics<T>> extends LifecycleAdapter implements InitializingBean {
    protected final static Logger logger = Logger.getLogger(BusinessLogics.class);

    private List<LogicsModule> logicModules = new ArrayList<LogicsModule>();

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

    protected LogicsInstance logicsInstance;

    private Boolean dialogUndecorated = true;

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
            ConcreteClass result = type.getDataClass(object, session.sql, LM.baseClass);
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
    }

    protected void addModulesFromResource(List<String> paths, List<String> excludedPaths) throws IOException {

        List<String> excludedLSF = new ArrayList<String>();

        if (excludedPaths != null) {
            for (String filePath : excludedPaths) {
                if (filePath.endsWith(".lsf")) {
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
            if (filePath.endsWith(".lsf")) {
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

            List<LogicsModule> orderedModules = orderModules();

            for (LogicsModule module : orderedModules) {
                module.initModule();
            }

            for (LogicsModule module : orderedModules) {
                module.initGroups();
            }

            for (LogicsModule module : orderedModules) {
                module.initClasses();
            }

            LM.baseClass.initObjectClass();
            LM.storeCustomClass(LM.baseClass.objectClass);

            for (LogicsModule module : orderedModules) {
                module.initTables();
            }

            for (LogicsModule module : orderedModules) {
                module.initProperties();
            }

            finishAbstract();

            finishActions();

            finishLogInit();

            showDependencies();

            getPropertyList(true);
            for(Property property : getPropertyList())
                property.finalizeAroundInit();

            LM.initClassForms();

            Set idSet = new HashSet<String>();
            for (Property property : getOrderProperties()) {
                //            assert idSet.add(property.getSID()) : "Same sid " + property.getSID();
            }

            for (LogicsModule module : orderedModules) {
                module.initIndexes();
            }
            assert checkProps();

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

    private void finishAbstract() {
        List<CaseUnionProperty> abstractUnions = new ArrayList<CaseUnionProperty>();
        for (Property property : getOrderProperties())
            if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract()) {
                property.finalizeInit();
                abstractUnions.add((CaseUnionProperty) property);
            }

        for (CaseUnionProperty abstractUnion : abstractUnions)
            abstractUnion.checkExclusive();
    }

    private void finishActions() { // потому как могут использовать abstract
        for (Property property : getOrderProperties())
            if(property instanceof ActionProperty) {
                if(property instanceof ListActionProperty && ((ListActionProperty)property).isAbstract())
                    property.finalizeInit();

                ImMap<CalcProperty, Boolean> change = ((ActionProperty<?>) property).getChangeExtProps();
                for(int i=0,size=change.size();i<size;i++) // вообще говоря DataProperty и IsClassProperty
                    change.getKey(i).addActionChangeProp(new Pair<Property<?>, LinkType>(property, change.getValue(i) ? LinkType.RECCHANGE : LinkType.DEPEND));
            }
    }

    private void finishLogInit() {
        // с одной стороны нужно отрисовать на форме логирования все свойства из recognizeGroup, с другой - LogFormEntity с Action'ом должен уже существовать
        // поэтому makeLoggable делаем сразу, а LogFormEntity при желании заполняем здесь
        for (Property property : getOrderProperties()) {
            if (property.loggable && property.logFormProperty.property instanceof FormActionProperty &&
                    ((FormActionProperty) property.logFormProperty.property).form instanceof LogFormEntity) {
                LogFormEntity logForm = (LogFormEntity) ((FormActionProperty) property.logFormProperty.property).form;
                if (logForm.lazyInit)
                    logForm.initProperties();
            }
        }
    }

    private void prereadCaches() {
        getAppliedProperties(true);
        getAppliedProperties(false);
        getMapAppliedDepends();
        for (Property property : getPropertyList()) // сделалем чтобы
            property.prereadCaches();
    }

    public String getFormSerializationPath(String formSID) {
        return "conf/forms/" + formSID;
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
                        throw new RuntimeException(ServerResourceBundle.getString("message.cycle.detected") + " : " + print + " -> " + minLink.result.to);
                    }
                    buildList(innerComponent, null, removedLinks, mResult);
                }
                proceeded.exclAddAll(innerComponent);
            }
        }

        return proceeded;
    }

    private static boolean findDependency(Property<?> property, Property<?> with, HSet<Property> proceeded, Stack<Link> path) {
        if (property.equals(with))
            return true;

        if (proceeded.add(property))
            return false;

        for (Link link : property.getLinks()) {
            path.push(link);
            if (findDependency(link.to, with, proceeded, path))
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

    private static String findDependency(Property<?> property1, Property<?> property2) {
        String result = "";

        Stack<Link> forward = new Stack<Link>();
        if (findDependency(property1, property2, new HSet<Property>(), forward))
            result += outDependency("FORWARD", property1, forward) + '\n';

        Stack<Link> backward = new Stack<Link>();
        if (findDependency(property2, property1, new HSet<Property>(), backward))
            result += outDependency("BACKWARD", property2, backward) + '\n';

        if (result.isEmpty())
            result += "NO DEPENDENCY " + property1 + " " + property2 + '\n';

        return result;
    }

    private void showDependencies() {
        String show = "";
        for (Property property : getOrderProperties())
            if (property instanceof ActionProperty && ((ActionProperty) property).showDep != null)
                show += findDependency(property, ((ActionProperty) property).showDep);
        if (!show.isEmpty()) {
            logger.debug("Dependencies: " + show);
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
    public ImOrderSet<CalcProperty> getStoredProperties() {
        return BaseUtils.immutableCast(getPropertyList().filterOrder(new SFunctionSet<Property>() {
            public boolean contains(Property property) {
                return property instanceof CalcProperty && ((CalcProperty) property).isStored();
            }}));
    }

    @IdentityLazy
    public ImOrderSet<OldProperty> getApplyEventDependProps() {
        MOrderSet<OldProperty> result = SetFact.mOrderSet();
        for (Property<?> property : getPropertyList()) {
            if (property instanceof ActionProperty && ((ActionProperty) property).onEvent(SystemEvent.APPLY))
                result.addAll(property.getOldDepends().toOrderSet());
            if (property instanceof DataProperty && ((DataProperty) property).event != null)
                result.addAll(((DataProperty) property).event.getOldDepends().toOrderSet());
        }
        return result.immutableOrder();
    }

    @IdentityLazy
    public List<Object> getAppliedProperties(boolean onlyCheck) {
        // здесь нужно вернуть список stored или тех кто
        List<Object> result = new ArrayList<Object>();
        for (Property property : getPropertyList(onlyCheck)) {
            if (property instanceof CalcProperty && ((CalcProperty) property).isStored())
                result.add(property);
            if (property instanceof ActionProperty && ((ActionProperty) property).onEvent(SystemEvent.APPLY))
                result.add(new ActionPropertyValueImplement((ActionProperty) property));
        }
        return result;
    }

    @IdentityLazy
    public List<CalcProperty> getDataChangeEvents() {
        List<CalcProperty> result = new ArrayList<CalcProperty>();
        for (Property property : getPropertyList())
            if (property instanceof DataProperty && ((DataProperty) property).event != null)
                result.add((((DataProperty) property).event).getWhere());
        return result;
    }

    private void fillAppliedDependFrom(CalcProperty<?> fill, CalcProperty<?> applied, Map<CalcProperty, MSet<CalcProperty>> mapDepends) {
        if (!fill.equals(applied) && fill.isStored())
            mapDepends.get(fill).add(applied);
        else
            for (CalcProperty depend : fill.getDepends(false)) // derived'ы отдельно отрабатываются
                fillAppliedDependFrom(depend, applied, mapDepends);
    }

    @IdentityLazy
    public List<ActionProperty> getSessionEvents() {
        List<ActionProperty> result = new ArrayList<ActionProperty>();
        for (Property property : getPropertyList())
            if (property instanceof ActionProperty && ((ActionProperty) property).onEvent(SystemEvent.SESSION))
                result.add((ActionProperty) property);
        return result;
    }

    // assert что key property is stored, а value property is stored или instanceof OldProperty
    @IdentityStrongLazy // глобальное очень сложное вычисление
    private ImMap<CalcProperty, ImOrderSet<CalcProperty>> getMapAppliedDepends() {
        Map<CalcProperty, MSet<CalcProperty>> mapDepends = new HashMap<CalcProperty, MSet<CalcProperty>>();
        for (CalcProperty property : getStoredProperties()) {
            mapDepends.put(property, SetFact.<CalcProperty>mSet());
            fillAppliedDependFrom(property, property, mapDepends);
        }
        for (OldProperty old : getApplyEventDependProps())
            fillAppliedDependFrom(old.property, old, mapDepends);

        ImRevMap<Property, Integer> indexMap = getPropertyList().mapOrderRevValues(new GetIndex<Integer>() {
            public Integer getMapValue(int i) {
                return i;
            }
        });
        MExclMap<CalcProperty, ImOrderSet<CalcProperty>> mOrderedMapDepends = MapFact.mExclMap(mapDepends.size());
        for (Map.Entry<CalcProperty, MSet<CalcProperty>> mapDepend : mapDepends.entrySet()) {
            ImSet<CalcProperty> depends = mapDepend.getValue().immutable();
            ImOrderSet<CalcProperty> dependList = indexMap.filterInclRev(depends).reverse().sort().valuesList().toOrderExclSet();
            assert dependList.size() == depends.size();
            mOrderedMapDepends.exclAdd(mapDepend.getKey(), dependList);
        }
        return mOrderedMapDepends.immutable();
    }

    // определяет для stored свойства зависимые от него stored свойства, а также свойства которым необходимо хранить изменения с начала транзакции (constraints и derived'ы)
    public ImOrderSet<CalcProperty> getAppliedDependFrom(CalcProperty property) {
        assert property.isStored();
        return getMapAppliedDepends().get(property);
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
        for (DataTable dataTable : LM.tableFactory.getDataTables(LM.baseClass)) {
            dataTable.calculateStat(this.reflectionLM, session);
        }
    }

    public void recalculateFollows(DataSession session) throws SQLException {
        for (Property property : getPropertyList())
            if (property instanceof ActionProperty) {
                ActionProperty<?> action = (ActionProperty) property;
                if (action.onEvent(SystemEvent.APPLY) && action.resolve)
                    session.resolve(action);
            }
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

    private boolean intersect(LCP[] props) {
        for (int i = 0; i < props.length; i++)
            for (int j = i + 1; j < props.length; j++)
                if (((LCP<?>) props[i]).intersect((LCP<?>) props[j]))
                    return true;
        return false;
    }

    public final static boolean checkClasses = false;
    private boolean checkProps() {
        if (checkClasses)
            for (Property prop : getOrderProperties()) {
                logger.debug("Checking property : " + prop + "...");
                assert prop.check();
            }
        for (LCP[] props : LM.checkCUProps) {
            logger.debug("Checking class properties : " + props + "...");
            assert !intersect(props);
        }
        for (LCP[] props : LM.checkSUProps) {
            logger.debug("Checking union properties : " + props + "...");
//            assert intersect(props);
        }
        return true;
    }

    private void outputPropertyClasses() {
        for (LP lp : LM.lproperties) {
            logger.debug(lp.property.getSID() + " : " + lp.property.caption + " - " + lp.getClassWhere());
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

    public SecurityManager getSecurityManager() {
        return ThreadLocalContext.getSecurityManager();
    }

    public DBManager getDbManager() {
        return ThreadLocalContext.getDbManager();
    }
}
