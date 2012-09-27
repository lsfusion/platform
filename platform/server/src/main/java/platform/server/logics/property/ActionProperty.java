package platform.server.logics.property;

import platform.base.*;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ActionClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.type.Type;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.property.actions.BaseEvent;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.logics.property.actions.edit.GroupChangeActionProperty;
import platform.server.logics.property.actions.flow.ChangeFlowType;
import platform.server.logics.property.actions.flow.FlowResult;
import platform.server.session.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.*;

public abstract class ActionProperty<P extends PropertyInterface> extends Property<P> {

    public ActionProperty(String sID, String caption, List<P> interfaces) {
        super(sID, caption, interfaces);
    }

    // assert что возвращает только DataProperty и Set(IsClassProperty), Drop(IsClassProperty), IsClassProperty, для использования в лексикографике (calculateLinks)
    public QuickSet<CalcProperty> getChangeExtProps() {
        ActionPropertyMapImplement<?, P> compile = compile();
        if(compile!=null)
            return compile.property.getChangeExtProps();

        return aspectChangeExtProps();
    }

    // убирает Set и Drop, так как с depends будет использоваться
    public QuickSet<CalcProperty> getChangeProps() {
        QuickSet<CalcProperty> result = new QuickSet<CalcProperty>();
        for(CalcProperty property : getChangeExtProps())
            if(property instanceof ChangedProperty)
                result.add((IsClassProperty)((ChangedProperty)property).property);
            else {
                assert property instanceof DataProperty || property instanceof ObjectClassProperty;
                result.add(property);
            }

        return result;
    }
    // схема с аспектом сделана из-за того что getChangeProps для ChangeClassAction не инвариантен (меняется после компиляции), тоже самое и For с addObject'ом
    @IdentityLazy
    protected QuickSet<CalcProperty> aspectChangeExtProps() {
        QuickSet<CalcProperty> result = new QuickSet<CalcProperty>();
        for(ActionProperty<?> dependAction : getDependActions())
            result.addAll(dependAction.getChangeExtProps());
        return result;
    }
    @IdentityLazy
    public Set<CalcProperty> getUsedProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        for(ActionProperty<?> dependAction : getDependActions())
            result.addAll(dependAction.getUsedProps());
        return result;
    }

    protected static QuickSet<CalcProperty> getChangeProps(CalcProperty... props) {
        QuickSet<CalcProperty> result = new QuickSet<CalcProperty>();
        for(CalcProperty element : props)
            result.addAll(element.getChangeProps());
        return result;
    }
    protected static <T extends PropertyInterface> Set<CalcProperty> getUsedProps(CalcPropertyInterfaceImplement<T>... props) {
        return getUsedProps(new ArrayList<CalcPropertyInterfaceImplement<T>>(), props);
    }
    protected static <T extends PropertyInterface> Set<CalcProperty> getUsedProps(Collection<? extends CalcPropertyInterfaceImplement<T>> col, CalcPropertyInterfaceImplement<T>... props) {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        for(CalcPropertyInterfaceImplement<T> element : col)
            element.mapFillDepends(result);
        for(CalcPropertyInterfaceImplement<T> element : props)
            element.mapFillDepends(result);
        return result;
    }
    
    public final FunctionSet<CalcProperty> usedProps = new FunctionSet<CalcProperty>() {
        public boolean contains(CalcProperty element) {
            return CalcProperty.depends(getUsedProps(), element);
        }
        public boolean isEmpty() {
            return false;
        }
        public boolean isFull() {
            return false;
        }
    };

    @IdentityLazy
    public boolean hasFlow(ChangeFlowType type) {
        for(ActionProperty<?> dependAction : getDependActions())
            if(dependAction.hasFlow(type))
                return true;
        return false;
    }

    @IdentityLazy
    public Set<SessionCalcProperty> getSessionCalcDepends() {
        Set<SessionCalcProperty> result = new HashSet<SessionCalcProperty>();
        for(CalcProperty property : getUsedProps())
            result.addAll(property.getSessionCalcDepends());
        return result;
    }

    public abstract Set<ActionProperty> getDependActions();

    public Map<P, ValueClass> getInterfaceClasses(boolean full) {
        return getWhereProperty().mapInterfaceClasses(full);
    }
    public ClassWhere<P> getClassWhere(boolean full) {
        return getWhereProperty().mapClassWhere(full);
    }

    public abstract CalcPropertyMapImplement<?, P> getWhereProperty();

    @Override
    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        if(events.isEmpty()) // вырежем Action'ы без Event'ов, они нигде не используются, а дают много компонент связности
            return new ArrayList<Pair<Property<?>, LinkType>>();

        LinkType linkType = hasFlow(ChangeFlowType.NEWSESSION) ? LinkType.RECUSED : LinkType.USEDACTION;

        Collection<Pair<Property<?>, LinkType>> result = new ArrayList<Pair<Property<?>, LinkType>>();
        for(CalcProperty depend : getUsedProps())
            result.add(new Pair<Property<?>, LinkType>(depend, linkType));
        result.add(new Pair<Property<?>, LinkType>(getWhereProperty().property, linkType));
        for(CalcProperty depend : strongUsed)
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.EVENTACTION));
        return result;
    }
    
    public final Set<CalcProperty> strongUsed = new HashSet<CalcProperty>();

    public <V extends PropertyInterface> ActionPropertyMapImplement<P, V> getImplement(List<V> list) {
        return new ActionPropertyMapImplement<P, V>(this, getMapInterfaces(list));
    }

    public <V extends PropertyInterface> ActionPropertyMapImplement<P, V> getImplement(V... list) {
        return getImplement(BaseUtils.toList(list));
    }

    public Set<BaseEvent> events = new HashSet<BaseEvent>();
    public Property showDep; // assert что не null когда events не isEmpty
    public boolean singleApply = false;
    public boolean resolve = false;
    public Collection<ActionPropertyMapImplement<?, P>> beforeAspects = new ArrayList<ActionPropertyMapImplement<?, P>>();
    public Collection<ActionPropertyMapImplement<?, P>> afterAspects = new ArrayList<ActionPropertyMapImplement<?, P>>();

    public FlowResult execute(ExecutionContext<P> context) throws SQLException {
        for(ActionPropertyMapImplement<?, P> aspect : beforeAspects) {
            FlowResult beforeResult = aspect.execute(context);
            if(beforeResult != FlowResult.FINISH)
                return beforeResult;
        }

        ActionPropertyMapImplement<?, P> compile = compile();
        if(compile!=null)
            return compile.execute(context);
        
        FlowResult result = aspectExecute(context);

        for(ActionPropertyMapImplement<?, P> aspect : afterAspects)
            aspect.execute(context);

        return result;
    }

    protected abstract FlowResult aspectExecute(ExecutionContext<P> context) throws SQLException;

    public ActionPropertyMapImplement<P, P> getImplement() {
        return new ActionPropertyMapImplement<P, P>(this, getIdentityInterfaces());
    }

    public void execute(ExecutionEnvironment env) throws SQLException {
        assert interfaces.size()==0;
        execute(new HashMap<P, DataObject>(), env, null);
    }

    public void execute(Map<P, DataObject> keys, ExecutionEnvironment env, FormEnvironment<P> formEnv) throws SQLException {
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

    protected ActionPropertyClassImplement<P> createClassImplement(List<ValueClassWrapper> classes, List<P> mapping) {
        return new ActionPropertyClassImplement<P>(this, classes, mapping);
    }

    @IdentityLazy
    public ActionPropertyMapImplement<?, P> getGroupChange() {
        ActionPropertyMapImplement<P, P> changeImplement = getImplement();
        ArrayList<P> listInterfaces = new ArrayList<P>(interfaces);

        GroupChangeActionProperty groupChangeActionProperty = new GroupChangeActionProperty("GCH" + getSID(), "sys", listInterfaces, changeImplement);
        return groupChangeActionProperty.getImplement(listInterfaces);
    }

    public ActionPropertyMapImplement<?, P> compile() {
       return null;
    }

    public List<ActionPropertyMapImplement<?, P>> getList() {
        return Collections.<ActionPropertyMapImplement<?, P>>singletonList(getImplement());
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(Map<P, T> mapping, Collection<T> context, boolean ordersNotNull) {
        return false;
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> CalcProperty getPushWhere(Map<P, T> mapping, Collection<T> context, boolean ordersNotNull) {
        throw new RuntimeException("should not be");
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?,T> pushFor(Map<P, T> mapping, Collection<T> context, CalcPropertyMapImplement<PW, T> where, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        throw new RuntimeException("should not be");
    }
}
