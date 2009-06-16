package platform.server.logics.properties;

import platform.base.BaseUtils;
import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.ModifyQuery;
import platform.server.data.PropertyField;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.classes.where.AndClassWhere;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.types.Type;
import platform.server.logics.DataObject;
import platform.server.logics.constraints.Constraint;
import platform.server.logics.data.MapKeysTable;
import platform.server.logics.data.TableFactory;
import platform.server.logics.properties.groups.AbstractNode;
import platform.server.session.*;
import platform.server.where.WhereBuilder;
import platform.server.caches.Lazy;

import java.sql.SQLException;
import java.util.*;

import net.jcip.annotations.Immutable;

@Immutable
abstract public class Property<T extends PropertyInterface> extends AbstractNode implements MapKeysInterface<T> {

    public int ID=0;
    // символьный идентификатор, с таким именем создаются поля в базе и передаются в PropertyView
    public String sID;

    Property(String iSID, Collection<T> iInterfaces) {
        sID = iSID;
        interfaces = iInterfaces;
    }

    public Collection<T> interfaces;

    public Map<T, KeyExpr> getMapKeys() {
        Map<T, KeyExpr> result = new HashMap<T, KeyExpr>();
        for(T propertyInterface : interfaces)
            result.put(propertyInterface,new KeyExpr(propertyInterface.toString()));
        return result;
    }

    public SourceExpr getSourceExpr(Map<T, ? extends SourceExpr> joinImplement) {
        return getSourceExpr(joinImplement,null,null, null, null);
    }

    // получает SourceExpr для сессии, заполняя если надо условия на изменения
    public SourceExpr getSourceExpr(Map<T, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps, WhereBuilder changedWhere) {
        if(session==null && isStored()) // если не изменилось и хранимое
            return mapTable.table.join(BaseUtils.join(BaseUtils.reverse(mapTable.mapKeys), joinImplement)).getExpr(field);

        if(session!=null) {
            if(!fillChanges(new ArrayList<Property>(),session,defaultProps,noUpdateProps))
                return getSourceExpr(joinImplement);

            IncrementChangeTable incrementTable = session.increment.get(this);
            if(incrementTable!=null) { // если уже все посчитано - просто возвращаем его
                Join<PropertyField> incrementJoin = incrementTable.join(BaseUtils.join(BaseUtils.reverse(BaseUtils.join(mapTable.mapKeys, incrementTable.mapKeys)), joinImplement));
                if(changedWhere!=null) changedWhere.add(incrementJoin.getWhere());
                return new CaseExpr(incrementJoin.getWhere(),incrementJoin.getExpr(incrementTable.changes.get(this)),getSourceExpr(joinImplement));
            }

            if(isStored() && this instanceof AggregateProperty) { // если хранимое и изменяется - то узнаем changed а в else подставляем stored
                WhereBuilder changedExprWhere = new WhereBuilder();
                SourceExpr changedExpr = calculateSourceExpr(joinImplement, session, defaultProps, noUpdateProps, changedExprWhere);
                if(changedWhere!=null) changedWhere.add(changedExprWhere.toWhere());
                return new CaseExpr(changedExprWhere.toWhere(),changedExpr,getSourceExpr(joinImplement));
            }
        }

        return calculateSourceExpr(joinImplement, session, defaultProps, noUpdateProps, changedWhere);
    }

    public SourceExpr calculateSourceExpr(Map<T, ? extends SourceExpr> joinImplement) {
        return calculateSourceExpr(joinImplement,null,null,null,null);
    }

    protected abstract SourceExpr calculateSourceExpr(Map<T, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps, WhereBuilder changedWhere);

    public boolean isInInterface(Map<T,ConcreteClass> interfaceClasses) { // вот тут все равно any или all вызывать
        return anyInInterface(new AndClassWhere<T>(interfaceClasses));
    }

    @Lazy
    public boolean anyInInterface(AndClassWhere<T> interfaceClasses) {
        return !getClassWhere().and(new ClassWhere<T>(interfaceClasses)).isFalse();
    }

    @Lazy
    public boolean allInInterface(AndClassWhere<T> interfaceClasses) {
        return new ClassWhere<T>(interfaceClasses).means(getClassWhere());
    }

    public <P extends PropertyInterface> boolean intersect(Property<P> property,Map<P,T> map) {
        return !getClassWhere().and(property.getClassWhere().mapKeys(map)).isFalse();
    }

    @Lazy
    private ClassWhere<T> getClassWhere() {
        return getQuery("value").getClassWhere(new ArrayList<String>());
    }

    // получает базовый класс по сути нужен для определения класса фильтра
    public abstract ValueClass getValueClass();

    @Lazy
    public abstract Type getType();

    @Lazy
    public Type getInterfaceType(T propertyInterface) {
        return getQuery("value").getKeyType(propertyInterface);
    }

    public String caption = "";

    public String toString() {
        return caption;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean fillChanges(List<Property> changedProperties, DataChanges changes, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) {
        if(changedProperties.contains(this)) return true;
        if(noUpdateProps.contains(this)) return false;

        boolean changed = fillDependChanges(changedProperties, changes, defaultProps, noUpdateProps);
        if(changed)
            changedProperties.add(this);
        return changed;
    }
    protected abstract boolean fillDependChanges(List<Property> changedProperties, DataChanges changes, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps);

    public void fillTableChanges(TableChanges fill,TableChanges changes) {
        BaseUtils.putNotNull(this,changes.increment.get(this),fill.increment);
    }

    @Lazy
    <JV> JoinQuery<T,JV> getQuery(JV value) {
        JoinQuery<T,JV> query = new JoinQuery<T,JV>(this);
        SourceExpr valueExpr = getSourceExpr(query.mapKeys);
        query.properties.put(value, valueExpr);
        query.and(valueExpr.getWhere());
        return query;
    }

    public boolean isObject() {
        return true;
    }

    public PropertyField field;

    protected abstract Map<T, ValueClass> getMapClasses();
    protected abstract ClassWhere<Field> getClassWhere(PropertyField storedField);

    public boolean cached = false;

    public MapKeysTable<T> mapTable; // именно здесь потому как не обязательно persistent
    public void markStored(TableFactory tableFactory) {
        mapTable = tableFactory.getMapTable(getMapClasses());

        PropertyField storedField = new PropertyField(sID,getType());
        mapTable.table.propertyClasses.put(storedField, getClassWhere(storedField));
        mapTable.table.properties.add(storedField);

        // именно после так как высчитали, а то сама себя stored'ом считать будет
        field = storedField;

        assert !cached;
    }
    public boolean isStored() {
        return field !=null && (!DataSession.reCalculateAggr || this instanceof DataProperty); // для тестирования 2-е условие
    }

    public Constraint constraint;

    public MapChangeDataProperty<T> getChangeProperty(Map<T, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        return null;
    }

    public Object read(DataSession session, Map<T, DataObject> keys, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) throws SQLException {
        String readValue = "readvalue";
        JoinQuery<T,Object> readQuery = new JoinQuery<T, Object>(this);

        readQuery.putKeyWhere(keys);

        readQuery.properties.put(readValue, getSourceExpr(readQuery.mapKeys,session.changes,defaultProps, noUpdateProps, null));
        return readQuery.executeSelect(session).values().iterator().next().get(readValue);
    }

    public void saveChanges(DataSession session,Map<DataProperty,DefaultData> defaultProps) throws SQLException {

        JoinQuery<KeyField, PropertyField> modifyQuery = new JoinQuery<KeyField, PropertyField>(mapTable.table);
        Map<T, ? extends SourceExpr> joinKeys = BaseUtils.join(mapTable.mapKeys, modifyQuery.mapKeys);

        WhereBuilder changedWhere = new WhereBuilder();
        SourceExpr changedExpr = getSourceExpr(joinKeys, session.changes, defaultProps, new ArrayList<Property>(), changedWhere);
        modifyQuery.properties.put(field, changedExpr);
        // если changed и хоть один не null
        modifyQuery.and(changedWhere.toWhere());
        modifyQuery.and(changedExpr.getWhere().or(getSourceExpr(joinKeys).getWhere()));
        session.modifyRecords(new ModifyQuery(mapTable.table,modifyQuery));
    }
}
