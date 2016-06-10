package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.FunctionSet;
import lsfusion.base.Pair;
import lsfusion.base.SFunctionSet;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.IdentityStartLazy;
import lsfusion.server.classes.ActionClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.debug.*;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.actions.BaseEvent;
import lsfusion.server.logics.property.actions.FormEnvironment;
import lsfusion.server.logics.property.actions.SessionEnvEvent;
import lsfusion.server.logics.property.actions.SystemEvent;
import lsfusion.server.logics.property.actions.edit.GroupChangeActionProperty;
import lsfusion.server.logics.property.actions.flow.ChangeFlowType;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.session.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class ActionProperty<P extends PropertyInterface> extends Property<P> {
    //просто для быстрого доступа
    private static final ActionPropertyDebugger debugger = ActionPropertyDebugger.getInstance();

    private ActionDebugInfo debugInfo;

    private ParamDebugInfo<P> paramInfo; // только для "top-level" action
    
    private ImSet<Pair<LP, List<ResolveClassSet>>> debugLocals;// только для list action
    
    public boolean hasDebugLocals() {
        return debugLocals != null && !debugLocals.isEmpty();
    }
    public void setDebugLocals(ImSet<Pair<LP, List<ResolveClassSet>>> debugLocals) {
        this.debugLocals = debugLocals;
    }

    public ActionProperty(String caption, ImOrderSet<P> interfaces) {
        super(caption, interfaces);
    }

    public final static AddValue<CalcProperty, Boolean> addValue = new SymmAddValue<CalcProperty, Boolean>() {
        public Boolean addValue(CalcProperty key, Boolean prevValue, Boolean newValue) {
            return prevValue && newValue;
        }
    };

    public void setDebugInfo(ActionDebugInfo debugInfo) {
        this.debugInfo = debugInfo;
    }
    
    public ActionDebugInfo getDebugInfo() {
        return debugInfo;
    }

    public void setParamInfo(ParamDebugInfo<P> paramInfo) {
        this.paramInfo = paramInfo;
    }

    public static int compareChangeExtProps(Property p1, Property p2, boolean strictCompare) {
        // если p1 не DataProperty
        String c1 = p1.getChangeExtSID();
        String c2 = p2.getChangeExtSID();

        if(c1 == null && c2 == null) {
            if(strictCompare)
                return Integer.compare(p1.hashCode(), p2.hashCode());
            return 0;
        }

        if(c1 == null)
            return 1;

        if(c2 == null)
            return -1;

        int result = c1.compareTo(c2);
        if(strictCompare && result == 0)
            return Integer.compare(p1.hashCode(), p2.hashCode());

        return result;
    }

    // assert что возвращает только DataProperty, ClassDataProperty, Set(IsClassProperty), Drop(IsClassProperty), ObjectClassProperty, для использования в лексикографике (calculateLinks)
    public ImMap<CalcProperty, Boolean> getChangeExtProps() {
        ActionPropertyMapImplement<?, P> compile = callCompile(false);
        if(compile!=null)
            return compile.property.getChangeExtProps();

        return aspectChangeExtProps();
    }

    // убирает Set и Drop, так как с depends будет использоваться
    public ImSet<CalcProperty> getChangeProps() {
        ImMap<CalcProperty, Boolean> changeExtProps = getChangeExtProps();
        int size = changeExtProps.size();
        MSet<CalcProperty> mResult = SetFact.mSetMax(size);
        for(int i=0;i<size;i++) {
            CalcProperty property = changeExtProps.getKey(i);
            if(property instanceof ChangedProperty)
                mResult.add(((ChangedProperty)property).property);
            else {
                assert property instanceof DataProperty || property instanceof ObjectClassProperty || property instanceof ClassDataProperty;
                mResult.add(property);
            }
        }

        return mResult.immutable();
    }
    // схема с аспектом сделана из-за того что getChangeProps для ChangeClassAction не инвариантен (меняется после компиляции), тоже самое и For с addObject'ом
    @IdentityStartLazy // только компиляция, построение лексикографики
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        MMap<CalcProperty, Boolean> result = MapFact.mMap(addValue);
        for(ActionProperty<?> dependAction : getDependActions())
            result.addAll(dependAction.getChangeExtProps());
        return result.immutable();
    }

    protected void markRecursions(ListCaseActionProperty recursiveAction) {
        for(ActionProperty action : getDependActions())
            action.markRecursions(recursiveAction);
    }

    public ImMap<CalcProperty, Boolean> getUsedExtProps() {
        ActionPropertyMapImplement<?, P> compile = callCompile(false);
        if(compile!=null)
            return compile.property.getUsedExtProps();

        return aspectUsedExtProps();
    }

    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    protected ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        MMap<CalcProperty, Boolean> result = MapFact.mMap(addValue);
        for(ActionProperty<?> dependAction : getDependActions())
            result.addAll(dependAction.getUsedExtProps());
        return result.immutable();
    }

    public ImSet<CalcProperty> getUsedProps() {
        return getUsedExtProps().keys();
    }

    protected static ImMap<CalcProperty, Boolean> getChangeProps(CalcProperty... props) {
        MMap<CalcProperty, Boolean> result = MapFact.mMap(addValue);
        for(CalcProperty element : props)
            result.addAll(element.getChangeProps().toMap(false));
        return result.immutable();
    }
    protected static <T extends PropertyInterface> ImMap<CalcProperty, Boolean> getUsedProps(CalcPropertyInterfaceImplement<T>... props) {
        return getUsedProps(SetFact.<CalcPropertyInterfaceImplement<T>>EMPTY(), props);
    }
    protected static <T extends PropertyInterface> ImMap<CalcProperty, Boolean> getUsedProps(ImCol<? extends CalcPropertyInterfaceImplement<T>> col, CalcPropertyInterfaceImplement<T>... props) {
        MSet<CalcProperty> mResult = SetFact.mSet();
        for(CalcPropertyInterfaceImplement<T> element : col)
            element.mapFillDepends(mResult);
        for(CalcPropertyInterfaceImplement<T> element : props)
            element.mapFillDepends(mResult);
        return mResult.immutable().toMap(false);
    }

    private FunctionSet<CalcProperty> usedProps;
    public FunctionSet<CalcProperty> getDependsUsedProps() {
        if(usedProps==null)
            usedProps = CalcProperty.getDependsFromSet(getUsedProps());
        return usedProps;
    }

    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    public boolean hasFlow(ChangeFlowType type) {
        for(ActionProperty<?> dependAction : getDependActions())
            if(dependAction.hasFlow(type))
                return true;
        return false;
    }

    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    public ImSet<SessionCalcProperty> getSessionCalcDepends(boolean events) {
        MSet<SessionCalcProperty> mResult = SetFact.mSet();
        for(CalcProperty property : getUsedProps())
            mResult.addAll(property.getSessionCalcDepends(events));
        return mResult.immutable();
    }

    public ImSet<OldProperty> getParseOldDepends() {
        MSet<OldProperty> mResult = SetFact.mSet();
        for(CalcProperty property : getUsedProps())
            mResult.addAll(property.getParseOldDepends());
        return mResult.immutable();
    }

    public abstract ImSet<ActionProperty> getDependActions();
    
    @IdentityLazy
    private ImSet<Pair<String, Integer>> getInnerDebugActions() {
        MSet<Pair<String, Integer>> result = SetFact.mSet();
        if (debugInfo != null) {
            result.add(new Pair<>(debugInfo.moduleName, debugInfo.line));
        }
        for (ActionProperty actionProperty : getDependActions()) {
            result.addAll(actionProperty.getInnerDebugActions());
        }
        return result.immutable();
    }

    @IdentityLazy
    private ImSet<Pair<String, Integer>> getChangePropsLocations() {
        MSet<Pair<String, Integer>> result = SetFact.mSet();
        for (CalcProperty property : getChangeProps()) {
            CalcPropertyDebugInfo debugInfo = property.getDebugInfo();
            if (debugInfo != null) {
                result.add(new Pair<>(debugInfo.moduleName, debugInfo.line));
            }
        }
        return result.immutable();
    }

    public boolean isInInterface(ImMap<P, ? extends AndClassSet> interfaceClasses, boolean isAny) {
        return getWhereProperty().mapIsInInterface(interfaceClasses, isAny);
    }

    public ImMap<P, ValueClass> getInterfaceClasses(ClassType type) {
        return getWhereProperty().mapGetInterfaceClasses(type);
    }

    @IdentityLazy
    public CalcPropertyMapImplement<?, P> getWhereProperty() {
        return getWhereProperty(false);
    }
    
    private static final Checker<ValueClass> checker = new Checker<ValueClass>() {
        public boolean checkEquals(ValueClass expl, ValueClass calc) {
            return BaseUtils.hashEquals(expl, calc);
        }
    }; 
            
    private CalcPropertyMapImplement<?, P> calcClassWhereProperty() {
        ImMap<P, ValueClass> inferred = getExplicitCalcInterfaces(interfaces, getExplicitInterfaces(), new Callable<ImMap<P, ValueClass>>() {
            public ImMap<P, ValueClass> call() throws Exception {
                return calcWhereInterfaceClasses();
            }}, "ACTION " + this, checker);
        return IsClassProperty.getMapProperty(inferred);
    }

    private ImMap<P, ValueClass> getExplicitInterfaces() {
        if(explicitClasses == null)
            return null;
        return ExClassSet.fromResolveValue(explicitClasses);
    }

    private ImMap<P, ValueClass> calcWhereInterfaceClasses() {
        return calcWhereProperty().mapInterfaceClasses(ClassType.signaturePolicy);
    }

    public CalcPropertyMapImplement<?, P> getWhereProperty(boolean recursive) {
        ActionWhereType actionWhere = AlgType.actionWhere;
        if(actionWhere != ActionWhereType.CALC && (!recursive || actionWhere == ActionWhereType.CLASS))
            return calcClassWhereProperty();
        else
            return calcWhereProperty();
    }
    
    public abstract CalcPropertyMapImplement<?, P> calcWhereProperty();

    @Override
    protected ImCol<Pair<Property<?>, LinkType>> calculateLinks(boolean events) {
        if(getEvents().isEmpty()) // вырежем Action'ы без Event'ов, они нигде не используются, а дают много компонент связности
            return SetFact.EMPTY();

        MCol<Pair<Property<?>, LinkType>> mResult = ListFact.mCol();
        ImMap<CalcProperty, Boolean> used = getUsedExtProps();
        for(int i=0,size=used.size();i<size;i++) {
            CalcProperty<?> property = used.getKey(i);
            Boolean rec = used.getValue(i);

            // эвристика : усилим связи к session calc, предполагается 
            ImSet<SessionCalcProperty> calcDepends = property.getSessionCalcDepends(events); // в том числе и для событий усилим, хотя может быть определенная избыточность,когда в SessionCalc - другой SessionCalc, но это очень редкие случаи
            for(int j=0,sizeJ=calcDepends.size();j<sizeJ;j++)
                mResult.add(new Pair<Property<?>, LinkType>(calcDepends.get(j), rec ? LinkType.RECEVENT : LinkType.EVENTACTION));

            mResult.add(new Pair<Property<?>, LinkType>(property, rec ? LinkType.RECUSED : LinkType.USEDACTION));
        }

//        раньше зачем-то было, но зачем непонятно
//        mResult.add(new Pair<Property<?>, LinkType>(getWhereProperty().property, hasFlow(ChangeFlowType.NEWSESSION) ? LinkType.RECUSED : LinkType.USEDACTION));

        ImSet<CalcProperty> depend = getStrongUsed();
        for(int i=0,size=depend.size();i<size;i++)
            mResult.add(new Pair<Property<?>, LinkType>(depend.get(i), LinkType.DEPEND));
        return mResult.immutableCol();
    }

    public ImSet<CalcProperty> strongUsed = SetFact.EMPTY();
    public void addStrongUsed(ImSet<CalcProperty> properties) { // чисто для лексикографики
        strongUsed = strongUsed.merge(properties);
    }
    public ImSet<CalcProperty> getStrongUsed() {
        return strongUsed;
    }

    public <V extends PropertyInterface> ActionPropertyMapImplement<P, V> getImplement(ImOrderSet<V> list) {
        return new ActionPropertyMapImplement<>(this, getMapInterfaces(list));
    }

    public Object events = MapFact.mExclMap();
    public void addEvent(BaseEvent event, SessionEnvEvent forms) {
        ((MExclMap<BaseEvent, SessionEnvEvent>)events).exclAdd(event, forms);
    }
    @LongMutable
    public ImMap<BaseEvent, SessionEnvEvent> getEvents() {
        return (ImMap<BaseEvent, SessionEnvEvent>)events;
    }
    public SessionEnvEvent getSessionEnv(BaseEvent event) {
        return getEvents().get(event);
    }

    public boolean singleApply = false;
    public boolean resolve = false;
    public boolean hasResolve() {
        return getSessionEnv(SystemEvent.APPLY)==SessionEnvEvent.ALWAYS && resolve;
    }

    private Object beforeAspects = ListFact.mCol();
    public void addBeforeAspect(ActionPropertyMapImplement<?, P> action) {
        ((MCol<ActionPropertyMapImplement<?, P>>)beforeAspects).add(action);
    }
    @LongMutable
    public ImCol<ActionPropertyMapImplement<?, P>> getBeforeAspects() {
        return (ImCol<ActionPropertyMapImplement<?,P>>)beforeAspects;
    }
    private Object afterAspects = ListFact.mCol();
    public void addAfterAspect(ActionPropertyMapImplement<?, P> action) {
        ((MCol<ActionPropertyMapImplement<?, P>>)afterAspects).add(action);
    }
    @LongMutable
    public ImCol<ActionPropertyMapImplement<?, P>> getAfterAspects() {
        return (ImCol<ActionPropertyMapImplement<?,P>>)afterAspects;
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        beforeAspects = ((MCol<ActionPropertyMapImplement<?, P>>)beforeAspects).immutableCol();
        afterAspects = ((MCol<ActionPropertyMapImplement<?, P>>)afterAspects).immutableCol();
        events = ((MMap<BaseEvent, SessionEnvEvent>)events).immutable();
    }

    @Override
    public String toString() {
        String result = super.toString();
        if(commonDebugInfo != null)
           result += ":" + commonDebugInfo;
        return result;
    }

    public final FlowResult execute(ExecutionContext<P> context) throws SQLException, SQLHandledException {
//        context.actionName = toString();
        if(paramInfo != null) {
            context = context.override();
            context.setParamsToInterfaces(paramInfo.paramsToInterfaces);
            context.setParamsToFQN(paramInfo.paramsToClassFQN);
        }
        if(debugLocals != null) {
            context = context.override();
            context.setLocals(debugLocals);
        }
        if (debugInfo != null) {
            return debugger.delegate(this, context);
        } else {
            return executeImpl(context);
        }
    }

    public FlowResult executeImpl(ExecutionContext<P> context) throws SQLException, SQLHandledException {
        if(Thread.currentThread().isInterrupted())
            return FlowResult.THROWS;

        if(debugInfo != null && debugInfo.delegationType == ActionDelegationType.IN_DELEGATE) {
            context = context.override();
            context.setNewDebugStack(true);
        }

        for(ActionPropertyMapImplement<?, P> aspect : getBeforeAspects()) {
            FlowResult beforeResult = aspect.execute(context);
            if(beforeResult != FlowResult.FINISH)
                return beforeResult;
        }

        ActionPropertyMapImplement<?, P> compile = callCompile(true);
        if (compile != null)
            return compile.execute(context);

        FlowResult result = aspectExecute(context);

        for(ActionPropertyMapImplement<?, P> aspect : getAfterAspects())
            aspect.execute(context);

        return result;
    }

    @Override
    public void prereadCaches() {
        super.prereadCaches();
        callCompile(true);
    }

    protected abstract FlowResult aspectExecute(ExecutionContext<P> context) throws SQLException, SQLHandledException;

    public ActionPropertyMapImplement<P, P> getImplement() {
        return new ActionPropertyMapImplement<>(this, getIdentityInterfaces());
    }

    public void execute(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        assert interfaces.size()==0;
        execute(MapFact.<P, DataObject>EMPTY(), env, null);
    }

    public void execute(ImMap<P, ? extends ObjectValue> keys, ExecutionEnvironment env, FormEnvironment<P> formEnv) throws SQLException, SQLHandledException {
        env.execute(this, keys, formEnv, null, null);
    }

    public ValueClass getValueClass(ClassType classType) {
        return ActionClass.instance;
    }

    @Override
    public ActionPropertyMapImplement<?, P> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        return getImplement();
    }

    // если этот action используется как действие для редактирования свойства, проверять ли это свойство на readOnly
    public boolean checkReadOnly = true;

    /**
     * возвращает тип для "простого" редактирования, когда этот action используется в качестве действия для редактирования </br>
     * assert, что тип будет DataClass, т.к. для остальных такое редактирование невозможно...
     * @param optimistic - если true, то если для некоторых случаев нельзя вывести тип, то эти случае будут игнорироваться
     */
    public Type getSimpleRequestInputType(boolean optimistic) {
        return null;
    }

    // по аналогии с верхним, assert что !hasChildren
    public CustomClass getSimpleAdd() {
        return null;
    }

    public P getSimpleDelete() {
        return null;
    }

    protected ActionPropertyClassImplement<P> createClassImplement(ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> mapping) {
        return new ActionPropertyClassImplement<>(this, classes, mapping);
    }

    @IdentityInstanceLazy
    public ActionPropertyMapImplement<?, P> getGroupChange() {
        ActionPropertyMapImplement<P, P> changeImplement = getImplement();
        ImOrderSet<P> listInterfaces = getOrderInterfaces();

        GroupChangeActionProperty groupChangeActionProperty = new GroupChangeActionProperty("sys", listInterfaces, changeImplement);
        return groupChangeActionProperty.getImplement(listInterfaces);
    }

    @IdentityLazy
    public ImSet<OldProperty> getSessionEventOldDepends() { // assert что OldProperty, при этом у которых Scope соответствующий локальному событию
        // assert что OldProperty, при этом у которых Scope соответствующий локальному событию
        assert getSessionEnv(SystemEvent.SESSION) != null;
        return getOldDepends().filterFn(new SFunctionSet<OldProperty>() {
            public boolean contains(OldProperty element) {
                return element.scope == PrevScope.EVENT;
            }
        });
    }

    @IdentityLazy
    public ImSet<SessionCalcProperty> getGlobalEventSessionCalcDepends() { // assert что OldProperty, при этом у которых Scope соответствующий локальному событию
        assert getSessionEnv(SystemEvent.APPLY) != null;
        return getSessionCalcDepends(false); // предполагается что FOR g, гда для g - есть вычисляемое свойство не будет, так как может привести к событиям на пустых сессиях
    }

    private ActionPropertyMapImplement<?, P> callCompile(boolean forExecution) {
        //не включаем компиляцию экшенов при дебаге
        if (forExecution && debugger.isEnabled() && !forceCompile()) {
            if (debugger.steppingMode || debugger.hasBreakpoint(getInnerDebugActions(), getChangePropsLocations())) {
                return null;
            }
        }
        return compile();
    }
    
    protected boolean forceCompile() {
        return false;
    }

    public ActionPropertyMapImplement<?, P> compile() {
        return null;
    }

    public ImList<ActionPropertyMapImplement<?, P>> getList() {
        return ListFact.<ActionPropertyMapImplement<?, P>>singleton(getImplement());
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<P, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return false;
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> CalcProperty getPushWhere(ImRevMap<P, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        throw new RuntimeException("should not be");
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?,T> pushFor(ImRevMap<P, T> mapping, ImSet<T> context, CalcPropertyMapImplement<PW, T> where, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        throw new RuntimeException("should not be");
    }

    protected void proceedNullException() {
    }

    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.AFTER_DELEGATE;        
    }

    @Override
    public boolean isSetNotNull() {
        return false;
    }
}
