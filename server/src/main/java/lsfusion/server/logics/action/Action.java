package lsfusion.server.logics.action;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.ListCaseAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.changed.ChangedProperty;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.action.session.changed.SessionProperty;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.event.*;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.interactive.action.input.SimpleRequestInput;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.instance.FormEnvironment;
import lsfusion.server.logics.form.interactive.property.GroupObjectProp;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.form.struct.action.ActionClassImplement;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.async.AsyncExec;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.AlgType;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.user.ClassDataProperty;
import lsfusion.server.logics.property.classes.user.ObjectClassProperty;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.*;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Action<P extends PropertyInterface> extends ActionOrProperty<P> {
    //просто для быстрого доступа
    private static final ActionDebugger debugger = ActionDebugger.getInstance();

    private boolean newDebugStack; // только для "top-level" action
    private ParamDebugInfo<P> paramInfo; // только для "top-level" action

    private ImSet<Pair<LP, List<ResolveClassSet>>> debugLocals;// только для list action
    
    public boolean hasDebugLocals() {
        return debugLocals != null && !debugLocals.isEmpty();
    }
    public void setDebugLocals(ImSet<Pair<LP, List<ResolveClassSet>>> debugLocals) {
        this.debugLocals = debugLocals;
    }

    public Action(LocalizedString caption, ImOrderSet<P> interfaces) {
        super(caption, interfaces);

        drawOptions.addProcessor(new DefaultProcessor() {
            @Override
            public void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity form) {
                if(entity.viewType == null)
                    entity.viewType = ClassViewType.PANEL;
            }

            @Override
            public void proceedDefaultDesign(PropertyDrawView propertyView) {
            }
        });
    }

    public final static AddValue<Property, Boolean> addValue = new SymmAddValue<Property, Boolean>() {
        public Boolean addValue(Property key, Boolean prevValue, Boolean newValue) {
            return prevValue && newValue;
        }
    };

    public void setDebugInfo(ActionDebugInfo debugInfo) {
        this.debugInfo = debugInfo;
    }
    
    @Override
    public ActionDebugInfo getDebugInfo() {
        return (ActionDebugInfo) debugInfo;
    }

    public void setNewDebugStack(boolean newDebugStack) {
        this.newDebugStack = newDebugStack;
    }

    public void setParamInfo(ParamDebugInfo<P> paramInfo) {
        this.paramInfo = paramInfo;
    }

    // assert что возвращает только DataProperty, ClassDataProperty, Set(IsClassProperty), Drop(IsClassProperty), Drop(ClassDataProperty), ObjectClassProperty, для использования в лексикографике (calculateLinks)
    public ImMap<Property, Boolean> getChangeExtProps() {
        ActionMapImplement<?, P> compile = callCompile(false);
        if(compile!=null)
            return compile.action.getChangeExtProps();

        return aspectChangeExtProps();
    }

    // убирает Set и Drop, так как с depends будет использоваться
    public ImSet<Property> getChangeProps() {
        ImMap<Property, Boolean> changeExtProps = getChangeExtProps();
        int size = changeExtProps.size();
        MSet<Property> mResult = SetFact.mSetMax(size);
        for(int i=0;i<size;i++) {
            Property property = changeExtProps.getKey(i);
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
    // true - if changed only in new session, false if changed in this session
    @IdentityStartLazy // только компиляция, построение лексикографики
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        MMap<Property, Boolean> result = MapFact.mMap(addValue);
        for(Action<?> dependAction : getDependActions())
            result.addAll(dependAction.getChangeExtProps());
        return result.immutable();
    }

    protected void markRecursions(ImSet<ListCaseAction> recursiveActions, Set<Action> marks) {
        if(!marks.add(this))
            return;
        for(Action action : getDependActions())
            action.markRecursions(recursiveActions, marks);
    }

    public ImMap<Property, Boolean> getUsedExtProps() {
        ActionMapImplement<?, P> compile = callCompile(false);
        if(compile!=null)
            return compile.action.getUsedExtProps();

        return aspectUsedExtProps();
    }

    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    protected ImMap<Property, Boolean> aspectUsedExtProps() {
        MMap<Property, Boolean> result = MapFact.mMap(addValue);
        for(Action<?> dependAction : getDependActions())
            result.addAll(dependAction.getUsedExtProps());
        return result.immutable();
    }

    public ImSet<Property> getUsedProps() {
        return getUsedExtProps().keys();
    }

    protected static ImMap<Property, Boolean> getChangeProps(Property... props) {
        MMap<Property, Boolean> result = MapFact.mMap(addValue);
        for(Property element : props)
            result.addAll(element.getChangeProps().toMap(false));
        return result.immutable();
    }
    protected static ImMap<Property, Boolean> getChangeProps(ImCol<Property> props) {
        MMap<Property, Boolean> result = MapFact.mMap(addValue);
        for(Property element : props)
            result.addAll(element.getChangeProps().toMap(false));
        return result.immutable();
    }
    protected static <T extends PropertyInterface> ImMap<Property, Boolean> getUsedProps(PropertyInterfaceImplement<T>... props) {
        return getUsedProps(SetFact.EMPTY(), props);
    }
    protected static <T extends PropertyInterface> ImMap<Property, Boolean> getUsedProps(ImCol<? extends PropertyInterfaceImplement<T>> col, PropertyInterfaceImplement<T>... props) {
        MSet<Property> mResult = SetFact.mSet();
        for(PropertyInterfaceImplement<T> element : col)
            element.mapFillDepends(mResult);
        for(PropertyInterfaceImplement<T> element : props)
            element.mapFillDepends(mResult);
        return mResult.immutable().toMap(false);
    }

    private FunctionSet<Property> usedProps;
    public FunctionSet<Property> getDependsUsedProps() {
        if(usedProps==null)
            usedProps = Property.getDependsFromSet(getUsedProps());
        return usedProps;
    }

    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.HASSESSIONUSAGES) {
            if(!getChangeProps().isEmpty())
                return true;
            if(Property.dependsSet(getUsedProps(), (SFunctionSet<Property>) Property::usesSession))
                return true;                
        }
        for(Action<?> dependAction : getDependActions())
            if(dependAction.hasFlow(type))
                return true;
        return false;
    }

    // пока просто ищем в конце APPLY и CHANGE'ы после APPLY
    // потом по хорошему надо будет в if then apply else cancel
    public boolean endsWithApplyAndNoChangesAfterBreaksBefore() {
        return false;
    }

    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    public ImSet<SessionProperty> getSessionCalcDepends(boolean events) {
        MSet<SessionProperty> mResult = SetFact.mSet();
        for(Property property : getUsedProps())
            mResult.addAll(property.getSessionCalcDepends(events));
        return mResult.immutable();
    }

    public ImSet<OldProperty> getParseOldDepends() {
        MSet<OldProperty> mResult = SetFact.mSet();
        for(Property property : getUsedProps())
            mResult.addAll(property.getParseOldDepends());
        return mResult.immutable();
    }

    public abstract ImSet<Action> getDependActions();
    
    @IdentityLazy
    private ImSet<Pair<String, Integer>> getInnerDebugActions() {
        ImSet<Pair<String, Integer>> result = getRecInnerDebugActions();
        if (debugInfo != null && debugInfo.needToCreateDelegate()) {
            result = result.merge(debugInfo.getDebuggerModuleLine());
        }
        return result;
    }

    protected ImSet<Pair<String, Integer>> getRecInnerDebugActions() {
        MSet<Pair<String, Integer>> result = SetFact.mSet();
        for (Action action : getDependActions()) {
            result.addAll(action.getInnerDebugActions());
        }
        return result.immutable();
    }

    @IdentityLazy
    public boolean uses(Property property) {
        return Property.depends(getUsedProps(), property) || hasFlow(ChangeFlowType.INTERACTIVEFORM);
    }

    @IdentityLazy
    public boolean changes(Property property) {
        return Property.depends(getChangeProps(), property) || hasFlow(ChangeFlowType.INTERACTIVEFORM);
    }

    @IdentityLazy
    private ImSet<Pair<String, Integer>> getChangePropsLocations() {
        MSet<Pair<String, Integer>> result = SetFact.mSet();
        for (Property property : getChangeProps()) {
            PropertyDebugInfo debugInfo = property.getDebugInfo();
            if (debugInfo != null && debugInfo.needToCreateDelegate()) {
                result.add(debugInfo.getDebuggerModuleLine());
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

    @IdentityInstanceLazy
    public PropertyMapImplement<?, P> getWhereProperty() {
        return getWhereProperty(false);
    }
    
    private static final Checker<ValueClass> checker = BaseUtils::hashEquals; 
            
    private PropertyMapImplement<?, P> calcClassWhereProperty() {
        ImMap<P, ValueClass> inferred = getExplicitCalcInterfaces(interfaces, getExplicitInterfaces(), this::calcWhereInterfaceClasses, "ACTION ", this, checker);
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

    @StackMessage("{logics.property.actions.flow.calc.where}")
    @ThisMessage
    public PropertyMapImplement<?, P> getWhereProperty(boolean recursive) {
        ActionWhereType actionWhere = AlgType.actionWhere;
        if(actionWhere != ActionWhereType.CALC && (!recursive || actionWhere == ActionWhereType.CLASS))
            return calcClassWhereProperty();
        else
            return calcWhereProperty();
    }
    
    public abstract PropertyMapImplement<?, P> calcWhereProperty();

    @Override
    protected ImCol<Pair<ActionOrProperty<?>, LinkType>> calculateLinks(boolean events) {
        if(getEvents().isEmpty()) // вырежем Action'ы без Event'ов, они нигде не используются, а дают много компонент связности
            return SetFact.EMPTY();

        MCol<Pair<ActionOrProperty<?>, LinkType>> mResult = ListFact.mCol();
        ImMap<Property, Boolean> used = getUsedExtProps();
        for(int i=0,size=used.size();i<size;i++) {
            Property<?> property = used.getKey(i);
            Boolean rec = used.getValue(i);

            // эвристика : усилим связи к session calc, предполагается 
            ImSet<SessionProperty> calcDepends = property.getSessionCalcDepends(events); // в том числе и для событий усилим, хотя может быть определенная избыточность,когда в SessionCalc - другой SessionCalc, но это очень редкие случаи
            for(int j=0,sizeJ=calcDepends.size();j<sizeJ;j++)
                mResult.add(new Pair<ActionOrProperty<?>, LinkType>(calcDepends.get(j), rec ? LinkType.RECEVENT : LinkType.EVENTACTION));

            mResult.add(new Pair<>(property, rec ? LinkType.RECUSED : LinkType.USEDACTION));
        }

//        раньше зачем-то было, но зачем непонятно
//        mResult.add(new Pair<Property<?>, LinkType>(getWhereProperty().property, hasFlow(ChangeFlowType.NEWSESSION) ? LinkType.RECUSED : LinkType.USEDACTION));

        ImSet<Property> depend = getStrongUsed();
        for(int i=0,size=depend.size();i<size;i++) {
            Property property = depend.get(i);
            mResult.add(new Pair<ActionOrProperty<?>, LinkType>(property, isRecursiveStrongUsed(property) ? LinkType.GOAFTERREC : LinkType.DEPEND));
        }
        return mResult.immutableCol();
    }

    private ImSet<Property> strongUsed = SetFact.EMPTY();
    public void addStrongUsed(ImSet<Property> properties) { // чисто для лексикографики
        strongUsed = strongUsed.merge(properties);
    }
    public ImSet<Property> getStrongUsed() {
        return strongUsed;
    }
    private ImSet<Property> recursiveStrongUsed = SetFact.EMPTY();
    public void checkRecursiveStrongUsed(Property property) {
        if(strongUsed.contains(property))
            recursiveStrongUsed = recursiveStrongUsed.merge(property);  
    }
    public boolean isRecursiveStrongUsed(Property property) { // при рекурсии ослабим связь, но не удалим, чтобы была в той же компоненте связности, но при этом не было цикла
        return recursiveStrongUsed.contains(property);
    }

    public <V extends PropertyInterface> ActionMapImplement<P, V> getImplement(ImOrderSet<V> list) {
        return new ActionMapImplement<>(this, getMapInterfaces(list));
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
    public void addBeforeAspect(ActionMapImplement<?, P> action) {
        ((MCol<ActionMapImplement<?, P>>)beforeAspects).add(action);
    }
    @LongMutable
    public ImCol<ActionMapImplement<?, P>> getBeforeAspects() {
        return (ImCol<ActionMapImplement<?,P>>)beforeAspects;
    }
    private Object afterAspects = ListFact.mCol();
    public void addAfterAspect(ActionMapImplement<?, P> action) {
        ((MCol<ActionMapImplement<?, P>>)afterAspects).add(action);
    }
    @LongMutable
    public ImCol<ActionMapImplement<?, P>> getAfterAspects() {
        return (ImCol<ActionMapImplement<?,P>>)afterAspects;
    }

    @Override
    protected void finalizeChanges() {
        beforeAspects = ((MCol<ActionMapImplement<?, P>>)beforeAspects).immutableCol();
        afterAspects = ((MCol<ActionMapImplement<?, P>>)afterAspects).immutableCol();
        events = ((MMap<BaseEvent, SessionEnvEvent>)events).immutable();
    }

    public final FlowResult execute(ExecutionContext<P> context) throws SQLException, SQLHandledException {
        assert interfaces.equals(context.getKeys().keys());
//        context.actionName = toString();
        if(newDebugStack) { // самым первым, так как paramInfo
            context = context.override();
            context.setNewDebugStack(true);
        }
        if(paramInfo != null) {
            context = context.override();
            context.setParamsToInterfaces(paramInfo.paramsToInterfaces);
            context.setParamsToFQN(paramInfo.paramsToClassFQN);
        }
        if(debugLocals != null) {
            context = context.override();
            context.setLocals(debugLocals);
        }
        if (debugInfo != null && debugger.isEnabled() && debugInfo.needToCreateDelegate()) {
            return debugger.delegate(this, context);
        } else {
            return executeImpl(context);
        }
    }

    public FlowResult executeImpl(ExecutionContext<P> context) throws SQLException, SQLHandledException {
        if(Thread.currentThread().isInterrupted() && !ThreadUtils.isFinallyMode(Thread.currentThread()))
            return FlowResult.THROWS;

        for(ActionMapImplement<?, P> aspect : getBeforeAspects()) {
            FlowResult beforeResult = aspect.execute(context);
            if(beforeResult != FlowResult.FINISH)
                return beforeResult;
        }

        ActionMapImplement<?, P> compile = callCompile(true);
        if (compile != null)
            return compile.execute(context);

        FlowResult result = aspectExecute(context);

        for(ActionMapImplement<?, P> aspect : getAfterAspects())
            aspect.execute(context);

        return result;
    }

    @Override
    public void prereadCaches() {
        super.prereadCaches();
        callCompile(true);
    }

    protected abstract FlowResult aspectExecute(ExecutionContext<P> context) throws SQLException, SQLHandledException;

    public ActionMapImplement<P, P> getImplement() {
        return new ActionMapImplement<>(this, getIdentityInterfaces());
    }

    public void execute(ExecutionEnvironment env, ExecutionStack stack) throws SQLException, SQLHandledException {
        assert interfaces.size()==0;
        execute(MapFact.<P, DataObject>EMPTY(), env, stack, null);
    }

    public void execute(ImMap<P, ? extends ObjectValue> keys, ExecutionEnvironment env, ExecutionStack stack, FormEnvironment<P> formEnv) throws SQLException, SQLHandledException {
        env.execute(this, keys, formEnv, null, stack);
    }

    @Override
    public ActionMapImplement<?, P> getDefaultEventAction(String eventActionSID, ImList<Property> viewProperties) {
        if(eventActionSID.equals(ServerResponse.EDIT_OBJECT))
            return null;
        return getImplement();
    }

    @IdentityInstanceLazy
    public SimpleRequestInput<P> getSimpleRequestInput(boolean optimistic) {
        return getSimpleRequestInput(optimistic, false);
    }
    // по сути protected (recursive usage)
    public SimpleRequestInput<P> getSimpleRequestInput(boolean optimistic, boolean inRequest) {
        return null;
    }

    public AsyncExec getAsyncExec() {
        return null;
    }

    // по аналогии с верхним, assert что !hasChildren
    public CustomClass getSimpleAdd() {
        return null;
    }

    private boolean isSimpleDelete;
    
    public void setSimpleDelete(boolean isSimpleDelete) {
        assert interfaces.size() == 1;
        this.isSimpleDelete = isSimpleDelete;
    }

    public P getSimpleDelete() {
        if(isSimpleDelete)
            return interfaces.single();
        return null;
    }

    protected ActionClassImplement<P> createClassImplement(ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> mapping) {
        return new ActionClassImplement<>(this, classes, mapping);
    }

    @IdentityStrongLazy // STRONG because of using in security policy
    public <G extends PropertyInterface> ActionObjectEntity<?> getGroupChange(GroupObjectEntity entity, ImRevMap<P, ObjectEntity> mapping) {
        ImSet<ObjectEntity> entityObjects = entity.getObjects();
        ImSet<ObjectEntity> notUsedEntityObjects = entityObjects.remove(mapping.valuesSet());
        if(!notUsedEntityObjects.isEmpty()) { // adding missing parameters to fulfil the assertion
            // it's a sort patchExtendParams (generating virtual parameters using list action for that)
            ImRevMap<PropertyInterface, P> context = interfaces.mapRevKeys((Supplier<PropertyInterface>) PropertyInterface::new);
            ImRevMap<PropertyInterface, ObjectEntity> notUsedInterfaces = notUsedEntityObjects.mapRevKeys((Supplier<PropertyInterface>) PropertyInterface::new);
            return PropertyFact.createListAction(context.keys().addExcl(notUsedInterfaces.keys()),
                    ListFact.singleton(new ActionMapImplement<>(this, context.reverse()))).
                        mapObjects(notUsedInterfaces.addRevExcl(context.join(mapping))).getGroupChange(entity);
        }

        return getGroupChange(entity.getProperty(GroupObjectProp.FILTER).mapPropertyImplement(mapping.reverse())).mapObjects(mapping);
    }
    public <G extends PropertyInterface> ActionMapImplement<?, P> getGroupChange(PropertyMapImplement<G, P> groupFilter) {

//        lm.addGroupObjectProp();
        MList<ActionMapImplement<?, P>> mList = ListFact.mList();

        // executing action for current object
        mList.add(getImplement()); 

        // executing action for other objects
        ImRevMap<P, PropertyInterface> context = interfaces.mapRevValues((Supplier<PropertyInterface>) PropertyInterface::new);
        ImRevMap<P, PropertyInterface> iterate = groupFilter.mapping.valuesSet().mapRevValues((Supplier<PropertyInterface>) PropertyInterface::new);        
        ImOrderSet<P> orderedSet = iterate.keys().toOrderSet();

        mList.add(PropertyFact.createPushRequestAction(interfaces, // PUSH REQUEST
                        PropertyFact.createForAction(context.valuesSet().addExcl(iterate.valuesSet()), context.valuesSet(), // FOR
                                PropertyFact.createAndNot(groupFilter.map(iterate), // group() AND NOT current objects
                                        PropertyFact.createCompareInterface(orderedSet.mapOrder(context), orderedSet.mapOrder(iterate), Compare.EQUALS)),
                                MapFact.EMPTYORDER(), false, 
                                getImplement().map(context.removeRev(iterate.keys()).addRevExcl(iterate)), // DO changeAction 
                      null, false, SetFact.EMPTY(), false).map(context.reverse())));
        
        return PropertyFact.createListAction(interfaces, mList.immutableList());
    }

    public <X extends PropertyInterface> ActionMapImplement<?, P> getDialogChangeWYS(InputListEntity<X, P> viewList, LP<?> targetProp) {
        // in group_change_wys it's gonna be tricky with recodeviewAction, if we want to optimize it to be executed with a single query
        BaseLogicsModule lm = getBaseLM();
        LP<?> inputWYSProp = lm.getRequestedValueProperty().getLCP(viewList.getDataClass());
        return getChangeWYS(viewList, inputWYSProp, viewList.getWYSObjectAction(lm, targetProp, inputWYSProp)); // finding object with that name
    }

    public <X extends PropertyInterface> ActionMapImplement<?, P> getNewChangeWYS(InputListEntity<X, P> viewList, LP<?> targetProp) {
        if(viewList != null) {
            Pair<Property<?>, ConcreteCustomClass> viewProperty = viewList.getViewProperty();
            if (viewProperty != null) {
                BaseLogicsModule lm = getBaseLM();
                return getChangeWYS(viewList, targetProp, Property.createJoinAction(lm.addNewEditAction(viewProperty.second, viewProperty.first), targetProp.getImplement()));
            }
        }
        return null;
    }

    public <X extends PropertyInterface> ActionMapImplement<?, P> getChangeWYS(InputListEntity<X, P> viewList, LP<?> inputWYSProp, ActionMapImplement<?, P> doAction) {
        // REQUEST
        //     inputViewAction
        // DO {
        //     doAction
        //     PUSH REQUEST eventAction
        // }
        return PropertyFact.createRequestAction(interfaces, viewList.getInputAction(getBaseLM(), inputWYSProp),
                    PropertyFact.createListAction(interfaces, ListFact.toList(doAction,
                                PropertyFact.createPushRequestAction(interfaces, getImplement()))), null);
    }

    @IdentityLazy
    public ImSet<OldProperty> getSessionEventOldDepends() { // assert что OldProperty, при этом у которых Scope соответствующий локальному событию
        // assert что OldProperty, при этом у которых Scope соответствующий локальному событию
        assert getSessionEnv(SystemEvent.SESSION) != null;
        return getOldDepends().filterFn(element -> element.scope == PrevScope.EVENT);
    }

    // optimization that uses the fact that event handlers should not have aftereffect
    @IdentityLazy
    public ImSet<SessionProperty> getGlobalEventSessionCalcDepends() {
        assert getSessionEnv(SystemEvent.APPLY) != null;
        // in theory cases like FOR f(a) OR FOR f(a) != PREV (f(a)) will be ignored, but so far it doesn't matter
        return getSessionCalcDepends(false).filterFn(element -> element instanceof ChangedProperty);
    }

    private ActionMapImplement<?, P> callCompile(boolean forExecution) {
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

    public ActionMapImplement<?, P> compile() {
        return null;
    }
    
    @FunctionalInterface
    public interface ActionReplacer {
        <P extends PropertyInterface> ActionMapImplement<?, P> replaceAction(Action<P> action);
    }
    public ActionMapImplement<?, P> replace(ActionReplacer replacer) {
        ActionMapImplement<?, P> replacedAction = replacer.replaceAction(this);
        if(replacedAction != null)
            return replacedAction;            
        return aspectReplace(replacer);
    }
    protected ActionMapImplement<?, P> aspectReplace(ActionReplacer replacer) {
        return null;
    }

    public ImList<ActionMapImplement<?, P>> getList() {
        return ListFact.singleton(getImplement());
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<P, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return false;
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> Property getPushWhere(ImRevMap<P, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        throw new RuntimeException("should not be");
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionMapImplement<?,T> pushFor(ImRevMap<P, T> mapping, ImSet<T> context, PropertyMapImplement<PW, T> where, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        throw new RuntimeException("should not be");
    }

    protected void proceedNullException() {
    }

    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.AFTER_DELEGATE;        
    }

    @Override
    public boolean isNotNull() {
        return false;
    }

    @IdentityStartLazy
    public ImList<Property> getSortedUsedProps() {
        return getUsedProps().sort(BusinessLogics.propComparator());
    }

    public boolean ignoreReadOnlyPolicy() {
        return !hasFlow(ChangeFlowType.READONLYCHANGE);
    }

    @Override
    public ApplyGlobalEvent getApplyEvent() {
        if (getSessionEnv(SystemEvent.APPLY)!=null) {
            if(event == null)
                event = new ApplyGlobalActionEvent(this);
            return event;
        }
        return null;
    }

    public ImMap<Property, Boolean> getRequestChangeExtProps(int count, Function<Integer, Type> type, Function<Integer, LP> targetProp) {
        return getBaseLM().getRequestChangeProps(count, type, targetProp).toMap(false);
    }
}
