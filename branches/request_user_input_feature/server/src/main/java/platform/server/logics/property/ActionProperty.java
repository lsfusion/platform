package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.QuickSet;
import platform.interop.action.ClientAction;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

public abstract class ActionProperty extends Property<ClassPropertyInterface> {

    public static <K, V> Map<ClassPropertyInterface, V> cast(Map<K, V> map) {
        return BaseUtils.<Map<ClassPropertyInterface, V>>immutableCast(map);
    }
    
    public ActionProperty(String sID, ValueClass... classes) {
        this(sID, "sysAction", classes);
    }

    public ActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, IsClassProperty.getInterfaces(classes));
    }

    // assert что возвращает только DataProperty и IsClassProperty
    public abstract Set<CalcProperty> getChangeProps();
    public abstract Set<CalcProperty> getUsedProps();

    public boolean pendingEventExecute() {
        return getChangeProps().size()==0;
    }

    public PropertyChange<ClassPropertyInterface> getEventAction(Modifier modifier) {
        return getEventAction(modifier.getPropertyChanges());
    }

    public PropertyChange<ClassPropertyInterface> getEventAction(PropertyChanges changes) {
        return event.getChange(changes);
    }

    @IdentityLazy
    protected CalcPropertyImplement<?, ClassPropertyInterface> getInterfaceClassProperty() {
        return IsClassProperty.getProperty(interfaces);
    }

    protected QuickSet<CalcProperty> calculateUsedChanges(StructChanges propChanges, boolean cascade) {
        return getInterfaceClassProperty().property.getUsedChanges(propChanges, cascade);
    }

    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return ActionClass.TRUE.getExpr().and(getInterfaceClassProperty().mapExpr(joinImplement, propClasses, propChanges, changedWhere).getWhere());
    }

    @Override
    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        Collection<Pair<Property<?>, LinkType>> result = new ArrayList<Pair<Property<?>, LinkType>>();
        for(CalcProperty depend : getUsedProps())
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.USEDACTION));
        result.add(new Pair<Property<?>, LinkType>(getInterfaceClassProperty().property, LinkType.USEDACTION));
        for(CalcProperty depend : getEventDepends())
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.EVENTACTION));
        return result;
    }

    protected static ValueClass or(ValueClass v1, ValueClass v2) {
        if(v1==null)
            return v2;
        if(v2==null)
            return v1;
        return v1.getUpSet().getOr().or(v2.getUpSet().getOr()).getCommonClass();
    }

    public static <I extends PropertyInterface> ValueClass[] getClasses(List<I> mapInterfaces, Collection<? extends PropertyInterfaceImplement<I>> props) {
        ValueClass[] result = new ValueClass[mapInterfaces.size()];
        for(PropertyInterfaceImplement<I> prop : props) {
            Map<I, ValueClass> propClasses;
            if(prop instanceof CalcPropertyMapImplement)
                propClasses = ((CalcPropertyMapImplement<?, I>) prop).mapCommonInterfaces();
            else if(prop instanceof ActionPropertyMapImplement)
                propClasses = ((ActionPropertyMapImplement<I>) prop).mapCommonInterfaces();
            else
                propClasses = new HashMap<I, ValueClass>();

            for(int i=0;i<result.length;i++)
                result[i] = or(result[i], propClasses.get(mapInterfaces.get(i)));
        }
        return result;
    }

    // не сильно структурно поэтому вынесено в метод
    public <V> Map<ClassPropertyInterface, V> getMapInterfaces(List<V> list) {
        int i=0;
        Map<ClassPropertyInterface, V> result = new HashMap<ClassPropertyInterface, V>();
        for(ClassPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface, list.get(i++));
        return result;
    }
    
    public <V extends PropertyInterface> ActionPropertyMapImplement<V> getImplement(List<V> list) {
        return new ActionPropertyMapImplement<V>(this, getMapInterfaces(list));
    }

    public abstract void execute(ExecutionContext context) throws SQLException;

    public ActionPropertyMapImplement<ClassPropertyInterface> getImplement() {
        return new ActionPropertyMapImplement<ClassPropertyInterface>(this, getIdentityInterfaces());
    }

    public List<ClientAction> execute(Map<ClassPropertyInterface, DataObject> keys, ExecutionEnvironment env, FormEnvironment<ClassPropertyInterface> formEnv) throws SQLException {
        return getImplement().execute(keys, env, formEnv);
    }

    @Override
    public ClassWhere<Object> getClassValueWhere() {
        return new ClassWhere<Object>(BaseUtils.<Object, ValueClass>add(IsClassProperty.getMapClasses(interfaces), "value", ActionClass.instance), true);
    }

    public Event<?,ClassPropertyInterface> event = null;

    protected Set<CalcProperty> getEventDepends() {
        return event !=null ? event.getDepends(true) : new HashSet<CalcProperty>();
    }

    @Override
    protected <D extends PropertyInterface, W extends PropertyInterface> void addEvent(CalcPropertyInterfaceImplement<ClassPropertyInterface> valueImplement, CalcPropertyMapImplement<W, ClassPropertyInterface> whereImplement, int options) {
        // assert что valueImplement ActionClass
        event = new Event<D,ClassPropertyInterface>(this, valueImplement, whereImplement, options);
    }

    @Override
    public ActionPropertyMapImplement<ClassPropertyInterface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        return getImplement();
    }
}
