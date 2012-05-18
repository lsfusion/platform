package platform.server.logics.property;

import platform.base.Pair;
import platform.base.QuickSet;
import platform.interop.action.ClientAction;
import platform.server.classes.*;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.logics.property.actions.flow.FlowResult;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

public abstract class ActionProperty<P extends PropertyInterface> extends Property<P> {

    public ActionProperty(String sID, String caption, List<P> interfaces) {
        super(sID, caption, interfaces);
    }

    // assert что возвращает только DataProperty и IsClassProperty
    public abstract Set<CalcProperty> getChangeProps();
    public abstract Set<CalcProperty> getUsedProps();

    public boolean pendingEventExecute() {
        return getChangeProps().size()==0;
    }

    public PropertyChange<P> getEventAction(Modifier modifier) {
        return getEventAction(modifier.getPropertyChanges());
    }

    public PropertyChange<P> getEventAction(PropertyChanges changes) {
        return event.getChange(changes);
    }

    public Map<P, ValueClass> getInterfaceClasses() {
        return getWhereProperty().mapInterfaceClasses();
    }
    public ClassWhere<P> getClassWhere(boolean full) {
        return getWhereProperty().mapClassWhere(full);
    }

    protected QuickSet<CalcProperty> calculateUsedChanges(StructChanges propChanges, boolean cascade) {
        return getWhereProperty().property.getUsedChanges(propChanges, cascade);
    }

    protected Expr calculateExpr(Map<P, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ActionClass.TRUE.getExpr().and(getWhereProperty().mapExpr(joinImplement, propClasses, propChanges, changedWhere).getWhere());
    }

    public abstract CalcPropertyMapImplement<?, P> getWhereProperty();

    @Override
    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
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

    public List<ClientAction> execute(Map<P, DataObject> keys, ExecutionEnvironment env, FormEnvironment<P> formEnv) throws SQLException {
        return env.execute(this, keys, formEnv, null);
    }

    public ValueClass getValueClass() {
        return ActionClass.instance;
    }

    public Event<?,P> event = null;

    protected Set<CalcProperty> getEventDepends() {
        return event !=null ? event.getDepends(true) : new HashSet<CalcProperty>();
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

        event = new Event<D,P>(this, DerivedProperty.<P>createStatic(true, ActionClass.instance), whereImplement, options);
    }

}
