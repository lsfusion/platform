package platform.server.logics.property;

import platform.base.FunctionSet;
import platform.base.Pair;
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
import platform.server.logics.property.actions.flow.FlowResult;
import platform.server.session.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.*;

public abstract class ActionProperty<P extends PropertyInterface> extends Property<P> {

    public ActionProperty(String sID, String caption, List<P> interfaces) {
        super(sID, caption, interfaces);
    }

    // assert что возвращает только DataProperty и IsClassProperty
    @IdentityLazy
    public Set<CalcProperty> getChangeProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        for(ActionProperty<?> dependAction : getDependActions())
            result.addAll(dependAction.getChangeProps());
        return result;
    }
    @IdentityLazy
    public Set<CalcProperty> getUsedProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        for(ActionProperty<?> dependAction : getDependActions())
            result.addAll(dependAction.getUsedProps());
        return result;
    }
    
    public final FunctionSet<CalcProperty> usedProps = new FunctionSet<CalcProperty>() {
        public boolean contains(CalcProperty element) {
            return CalcProperty.depends(getChangeProps(), element) || CalcProperty.depends(getUsedProps(), element);
        }
        public boolean isEmpty() {
            return false;
        }
        public boolean isFull() {
            return false;
        }
    };

    @IdentityLazy
    public boolean hasCancel() {
        boolean hasCancel = false;
        for(ActionProperty<?> dependAction : getDependActions())
            hasCancel = hasCancel || dependAction.hasCancel();
        return hasCancel;
    }
    @IdentityLazy
    public Set<SessionCalcProperty> getSessionCalcDepends() {
        Set<SessionCalcProperty> result = new HashSet<SessionCalcProperty>();
        for(CalcProperty property : getUsedProps())
            result.addAll(property.getSessionCalcDepends());
        for(CalcProperty property : getChangeProps())
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

        Collection<Pair<Property<?>, LinkType>> result = new ArrayList<Pair<Property<?>, LinkType>>();
        for(CalcProperty depend : getUsedProps())
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.USEDACTION));
        result.add(new Pair<Property<?>, LinkType>(getWhereProperty().property, LinkType.USEDACTION));
        for(CalcProperty depend : strongUsed)
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.EVENTACTION));
        return result;
    }
    
    public final Set<CalcProperty> strongUsed = new HashSet<CalcProperty>();

    // не сильно структурно поэтому вынесено в метод
    public <V> Map<P, V> getMapInterfaces(List<V> list) {
        int i=0;
        Map<P, V> result = new HashMap<P, V>();
        for(P propertyInterface : interfaces)
            result.put(propertyInterface, list.get(i++));
        return result;
    }
    
    public <V extends PropertyInterface> ActionPropertyMapImplement<P, V> getImplement(List<V> list) {
        return new ActionPropertyMapImplement<P, V>(this, getMapInterfaces(list));
    }

    public Set<BaseEvent> events = new HashSet<BaseEvent>();
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
}
