package platform.server.logics.property;

import platform.base.Pair;
import platform.interop.action.ClientAction;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.property.actions.ActionEvent;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.logics.property.actions.edit.GroupChangeActionProperty;
import platform.server.logics.property.actions.flow.FlowResult;
import platform.server.session.*;

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
    @IdentityLazy
    public boolean hasCancel() {
        boolean hasCancel = false;
        for(ActionProperty<?> dependAction : getDependActions())
            hasCancel = hasCancel || dependAction.hasCancel();
        return hasCancel;
    }

    public abstract Set<ActionProperty> getDependActions();

    public boolean pendingEventExecute() {
        return getChangeProps().size()==0 && !hasCancel();
    }

    public PropertySet<P> getEventAction(Modifier modifier) {
        return getEventAction(modifier.getPropertyChanges());
    }

    public PropertySet<P> getEventAction(PropertyChanges changes) {
        return event.getChange(changes);
    }

    public Map<P, ValueClass> getInterfaceClasses(boolean full) {
        return getWhereProperty().mapInterfaceClasses(full);
    }
    public ClassWhere<P> getClassWhere(boolean full) {
        return getWhereProperty().mapClassWhere(full);
    }

    public abstract CalcPropertyMapImplement<?, P> getWhereProperty();

    @Override
    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        if(event==null) // вырежем Action'ы без Event'ов, они нигде не используются, а дают много компонент связности
            return new ArrayList<Pair<Property<?>, LinkType>>();

        Collection<Pair<Property<?>, LinkType>> result = new ArrayList<Pair<Property<?>, LinkType>>();
        for(CalcProperty depend : getUsedProps())
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.USEDACTION));
        result.add(new Pair<Property<?>, LinkType>(getWhereProperty().property, LinkType.USEDACTION));
        for(CalcProperty depend : getEventDepends())
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.EVENTACTION));
        return result;
    }

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

    public abstract FlowResult execute(ExecutionContext<P> context) throws SQLException;

    public ActionPropertyMapImplement<P, P> getImplement() {
        return new ActionPropertyMapImplement<P, P>(this, getIdentityInterfaces());
    }

    public void execute(Map<P, DataObject> keys, ExecutionEnvironment env, FormEnvironment<P> formEnv) throws SQLException {
        env.execute(this, keys, formEnv, null);
    }

    public ValueClass getValueClass() {
        return ActionClass.instance;
    }

    public ActionEvent<P> event = null;

    protected Set<CalcProperty> getEventDepends() {
        return event !=null ? event.getDepends() : new HashSet<CalcProperty>();
    }

    @Override
    public ActionPropertyMapImplement<?, P> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        return getImplement();
    }

    protected ActionPropertyClassImplement<P> createClassImplement(List<ValueClassWrapper> classes, List<P> mapping) {
        return new ActionPropertyClassImplement<P>(this, classes, mapping);
    }

    public <D extends PropertyInterface> void setEventAction(CalcPropertyMapImplement<?, P> whereImplement, int options) {
        if(!((CalcProperty)whereImplement.property).noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET);

        event = new ActionEvent<P>(this, whereImplement, options);
    }

    @IdentityLazy
    public ActionPropertyMapImplement<?, P> getGroupChange() {
        ActionPropertyMapImplement<P, P> changeImplement = getImplement();
        ArrayList<P> listInterfaces = new ArrayList<P>(interfaces);

        GroupChangeActionProperty groupChangeActionProperty = new GroupChangeActionProperty("GCH" + getSID(), "sys", listInterfaces, changeImplement);
        return groupChangeActionProperty.getImplement(listInterfaces);
    }
}
