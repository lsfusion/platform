package platform.server.logics.property;

import platform.base.FunctionSet;
import platform.base.NotFunctionSet;
import platform.base.Pair;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.*;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ActionClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.type.Type;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.FormEntity;
import platform.server.logics.DataObject;
import platform.server.logics.property.actions.BaseEvent;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.logics.property.actions.SessionEnvEvent;
import platform.server.logics.property.actions.SystemEvent;
import platform.server.logics.property.actions.edit.GroupChangeActionProperty;
import platform.server.logics.property.actions.flow.ChangeFlowType;
import platform.server.logics.property.actions.flow.FlowResult;
import platform.server.session.ExecutionEnvironment;

import java.sql.SQLException;

public abstract class ActionProperty<P extends PropertyInterface> extends Property<P> {

    public ActionProperty(String sID, String caption, ImOrderSet<P> interfaces) {
        super(sID, caption, interfaces);
    }

    public final static AddValue<CalcProperty, Boolean> addValue = new SimpleAddValue<CalcProperty, Boolean>() {
        public Boolean addValue(CalcProperty key, Boolean prevValue, Boolean newValue) {
            return prevValue && newValue;
        }

        public boolean symmetric() {
            return true;
        }
    };

    // assert что возвращает только DataProperty и Set(IsClassProperty), Drop(IsClassProperty), IsClassProperty, для использования в лексикографике (calculateLinks)
    public ImMap<CalcProperty, Boolean> getChangeExtProps() {
        ActionPropertyMapImplement<?, P> compile = compile();
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
                mResult.add((IsClassProperty)((ChangedProperty)property).property);
            else {
                assert property instanceof DataProperty || property instanceof ObjectClassProperty;
                mResult.add(property);
            }
        }

        return mResult.immutable();
    }
    // схема с аспектом сделана из-за того что getChangeProps для ChangeClassAction не инвариантен (меняется после компиляции), тоже самое и For с addObject'ом
    @IdentityLazy
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        MMap<CalcProperty, Boolean> result = MapFact.mMap(addValue);
        for(ActionProperty<?> dependAction : getDependActions())
            result.addAll(dependAction.getChangeExtProps());
        return result.immutable();
    }

    public ImMap<CalcProperty, Boolean> getUsedExtProps() {
        ActionPropertyMapImplement<?, P> compile = compile();
        if(compile!=null)
            return compile.property.getUsedExtProps();

        return aspectUsedExtProps();
    }

    @IdentityLazy
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
    
    public FunctionSet<CalcProperty> usedProps;
    public FunctionSet<CalcProperty> getDependsUsedProps() {
        if(usedProps==null)
            usedProps = CalcProperty.getDependsFromSet(getUsedProps());
        return usedProps;
    }

    @IdentityLazy
    public boolean hasFlow(ChangeFlowType type) {
        for(ActionProperty<?> dependAction : getDependActions())
            if(dependAction.hasFlow(type))
                return true;
        return false;
    }

    @IdentityLazy
    public ImSet<SessionCalcProperty> getSessionCalcDepends() {
        MSet<SessionCalcProperty> mResult = SetFact.mSet();
        for(CalcProperty property : getUsedProps())
            mResult.addAll(property.getSessionCalcDepends());
        return mResult.immutable();
    }

    public abstract ImSet<ActionProperty> getDependActions();

    public ImMap<P, ValueClass> getInterfaceClasses(boolean full) {
        return getWhereProperty().mapInterfaceClasses(full);
    }
    public ClassWhere<P> getClassWhere(boolean full) {
        return getWhereProperty().mapClassWhere(full);
    }

    public abstract CalcPropertyMapImplement<?, P> getWhereProperty();

    @Override
    protected ImCol<Pair<Property<?>, LinkType>> calculateLinks() {
        if(getEvents().isEmpty()) // вырежем Action'ы без Event'ов, они нигде не используются, а дают много компонент связности
            return SetFact.EMPTY();

        LinkType linkType = hasFlow(ChangeFlowType.NEWSESSION) ? LinkType.RECUSED : LinkType.USEDACTION;

        MCol<Pair<Property<?>, LinkType>> mResult = ListFact.mCol();
        ImMap<CalcProperty, Boolean> used = getUsedExtProps();
        for(int i=0,size=used.size();i<size;i++)
            mResult.add(new Pair<Property<?>, LinkType>(used.getKey(i), used.getValue(i) ? LinkType.RECUSED : LinkType.USEDACTION));
        mResult.add(new Pair<Property<?>, LinkType>(getWhereProperty().property, linkType));
        CalcProperty depend = getStrongUsed();
        if(depend!=null)
            mResult.add(new Pair<Property<?>, LinkType>(depend, LinkType.EVENTACTION));
        return mResult.immutableCol();
    }
    
    public CalcProperty strongUsed = null;
    public void setStrongUsed(CalcProperty property) { // чисто для лексикографики
        strongUsed = property;
    }
    public CalcProperty getStrongUsed() {
        return strongUsed;
    }

    public <V extends PropertyInterface> ActionPropertyMapImplement<P, V> getImplement(ImOrderSet<V> list) {
        return new ActionPropertyMapImplement<P, V>(this, getMapInterfaces(list));
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

    public Property showDep; // assert что не null когда events не isEmpty
    public boolean singleApply = false;
    public ImSet<CalcProperty> prevStart;
    public boolean resolve = false;
    
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

    public FlowResult execute(ExecutionContext<P> context) throws SQLException {
        for(ActionPropertyMapImplement<?, P> aspect : getBeforeAspects()) {
            FlowResult beforeResult = aspect.execute(context);
            if(beforeResult != FlowResult.FINISH)
                return beforeResult;
        }

        ActionPropertyMapImplement<?, P> compile = compile();
        if(compile!=null)
            return compile.execute(context);
        
        FlowResult result = aspectExecute(context);

        for(ActionPropertyMapImplement<?, P> aspect : getAfterAspects())
            aspect.execute(context);

        return result;
    }

    public void prereadCaches() {
        compile();
        getInterfaceClasses();
        getInterfaceClasses(true);
    }

    protected abstract FlowResult aspectExecute(ExecutionContext<P> context) throws SQLException;

    public ActionPropertyMapImplement<P, P> getImplement() {
        return new ActionPropertyMapImplement<P, P>(this, getIdentityInterfaces());
    }

    public void execute(ExecutionEnvironment env) throws SQLException {
        assert interfaces.size()==0;
        execute(MapFact.<P, DataObject>EMPTY(), env, null);
    }

    public void execute(ImMap<P, DataObject> keys, ExecutionEnvironment env, FormEnvironment<P> formEnv) throws SQLException {
        env.execute(this, keys, formEnv, null, null);
    }

    public ValueClass getValueClass() {
        return ActionClass.instance;
    }

    @Override
    public ActionPropertyMapImplement<?, P> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        return getImplement();
    }

    /**
     * возвращает тип для "простого" редактирования, когда этот action используется в качестве действия для редактирования </br>
     * assert, что тип будет DataClass, т.к. для остальных такое редактирование невозможно...
     */
    public Type getSimpleRequestInputType() {
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
        return new ActionPropertyClassImplement<P>(this, classes, mapping);
    }

    @IdentityInstanceLazy
    public ActionPropertyMapImplement<?, P> getGroupChange() {
        ActionPropertyMapImplement<P, P> changeImplement = getImplement();
        ImOrderSet<P> listInterfaces = getOrderInterfaces();

        GroupChangeActionProperty groupChangeActionProperty = new GroupChangeActionProperty("GCH" + getSID(), "sys", listInterfaces, changeImplement);
        return groupChangeActionProperty.getImplement(listInterfaces);
    }

    public ImSet<OldProperty> getSessionEventOldDepends() { // фильтрует те свойства которые нужны не на
        assert getSessionEnv(SystemEvent.SESSION)!=null;

        ImSet<OldProperty> result = getOldDepends();
        if(prevStart!=null)
            result = result.filterFn(new NotFunctionSet<OldProperty>(prevStart.mapSetValues(new GetValue<OldProperty, CalcProperty>() {
                public OldProperty getMapValue(CalcProperty value) {
                    return value.getOld();
                }})));
        return result;
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
}
