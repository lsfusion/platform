package platform.server.logics.properties;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.caches.Lazy;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.CustomClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.types.Type;
import platform.server.logics.DataObject;
import platform.server.logics.constraints.Constraint;
import platform.server.logics.data.MapKeysTable;
import platform.server.logics.data.TableFactory;
import platform.server.logics.properties.groups.AbstractNode;
import platform.server.session.DataChanges;
import platform.server.session.DataSession;
import platform.server.session.MapChangeDataProperty;
import platform.server.session.TableChanges;
import platform.server.where.WhereBuilder;

import java.sql.SQLException;
import java.util.*;

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

    public static class DefaultChanges extends TableUsedChanges<DefaultChanges> {}
    private static TableDepends<DefaultChanges> defaultDepends = new TableDepends<DefaultChanges>(){
        public <P extends PropertyInterface> SourceExpr changed(Property<P> property, Map<P, ? extends SourceExpr> joinImplement, WhereBuilder changedWhere) {
            return null;
        }
        public DefaultChanges used(Property property, DefaultChanges usedChanges) {
            return usedChanges;
        }
        public DefaultChanges newChanges() {
            return new DefaultChanges();
        }
    };

    public SourceExpr getSourceExpr(Map<T, ? extends SourceExpr> joinImplement) {
        return getSourceExpr(joinImplement,null,defaultDepends,null);
    }

    public SourceExpr getSourceExpr(Map<T, ? extends SourceExpr> joinImplement, TableChanges session, TableDepends<? extends TableUsedChanges> depends, WhereBuilder changedWhere) {
        return getSourceExpr(joinImplement,session,new ArrayList<DataProperty>(),depends,changedWhere);
    }

    // получает SourceExpr для сессии, заполняя если надо условия на изменения
    protected <U extends TableUsedChanges<U>> SourceExpr getSourceExpr(Map<T, ? extends SourceExpr> joinImplement, TableChanges session, Collection<DataProperty> usedDefault, TableDepends<U> depends, WhereBuilder changedWhere) {
        if(session==null && isStored()) // если не изменилось и хранимое
            return mapTable.table.join(BaseUtils.join(BaseUtils.reverse(mapTable.mapKeys), joinImplement)).getExpr(field);

        if(session!=null) {
            U usedChanges = getUsedChanges(session, usedDefault, depends);

            WhereBuilder changedExprWhere = new WhereBuilder();
            SourceExpr changedExpr = null;
            if(!usedChanges.hasChanges())
                changedExpr = CaseExpr.NULL;
            if(depends!=null && changedExpr==null && usedChanges.usedDefault.isEmpty())
                changedExpr = depends.changed(this, joinImplement, changedExprWhere);
            if(changedExpr==null && isStored() && usePrevious()) // если хранимое и изменяется - то узнаем changed а в else подставляем stored
                changedExpr = calculateSourceExpr(joinImplement, session, usedDefault, depends, changedExprWhere);
            if(changedExpr!=null) {
                if(changedWhere!=null) changedWhere.add(changedExprWhere.toWhere());
                return changedExpr.ifElse(changedExprWhere.toWhere(),getSourceExpr(joinImplement));
            }
        }

        return calculateSourceExpr(joinImplement, session, usedDefault, depends, changedWhere);
    }

    public SourceExpr calculateSourceExpr(Map<T, ? extends SourceExpr> joinImplement) {
        return calculateSourceExpr(joinImplement,null,new ArrayList<DataProperty>(),defaultDepends, null);
    }

    protected abstract SourceExpr calculateSourceExpr(Map<T, ? extends SourceExpr> joinImplement, TableChanges session, Collection<DataProperty> usedDefault, TableDepends<? extends TableUsedChanges> depends, WhereBuilder changedWhere);

    @Lazy
    public boolean anyInInterface(Map<T, ? extends AndClassSet> interfaceClasses) {
        return !getClassWhere().and(new ClassWhere<T>(interfaceClasses)).isFalse();
    }

    @Lazy
    public boolean allInInterface(Map<T,? extends AndClassSet> interfaceClasses) {
        return new ClassWhere<T>(interfaceClasses).means(getClassWhere());
    }

    public <P extends PropertyInterface> boolean intersect(Property<P> property,Map<P,T> map) {
        return !getClassWhere().and(new ClassWhere<T>(property.getClassWhere(),map)).isFalse();
    }

    public boolean check() {
        return !getClassWhere().isFalse();
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

    // класс отражающий изменения влияющие на выражение
    public static abstract class UsedChanges<C extends DataChanges<C>,This extends UsedChanges<C,This>> {
        public abstract C newChanges();
        private final C changes = newChanges();
        public final Collection<DataProperty> usedDefault = new ArrayList<DataProperty>();

        public void add(This add, DataProperty exclude) {
            changes.add(add.changes);

            for(DataProperty property : add.usedDefault)
                if(!property.equals(exclude))
                    usedDefault.add(property);
        }

        public void dependsRemove(C dependChanges, ValueClass valueClass) {
            if(valueClass instanceof CustomClass)
                changes.dependsRemove(dependChanges,(CustomClass)valueClass);
        }
        public void dependsAdd(C dependChanges, ValueClass valueClass) {
            if(valueClass instanceof CustomClass)
                changes.dependsAdd(dependChanges,(CustomClass)valueClass);
        }
        public void dependsData(C dependChanges, DataProperty property) {
            changes.dependsData(dependChanges,property);
        }

        public boolean hasChanges() {
            return changes.hasChanges();
        }

        @Override
        public boolean equals(Object o) {
            return this==o || o instanceof UsedChanges && usedDefault.equals(((UsedChanges)o).usedDefault) && changes.equals(((UsedChanges)o).changes);
        }

        @Override
        public int hashCode() {
            return 31 * changes.hashCode() + usedDefault.hashCode();
        }
    }
    public interface Depends<C extends DataChanges<C>,U extends UsedChanges<C,U>> {
        U used(Property property,U usedChanges);
        U newChanges();
    }

    // возвращает от чего "зависят" изменения - с callback'ов
    abstract <C extends DataChanges<C>,U extends UsedChanges<C,U>> U calculateUsedChanges(C changes, Collection<DataProperty> usedDefault, Depends<C, U> depends);
    public <C extends DataChanges<C>,U extends UsedChanges<C,U>> U getUsedChanges(C changes, Collection<DataProperty> usedDefault, Depends<C,U> depends) {
        U result = calculateUsedChanges(changes, usedDefault, depends);
        if(result.hasChanges() && result.usedDefault.isEmpty())
            result = depends.used(this,result);
        return result;
    }

    public <C extends DataChanges<C>,U extends UsedChanges<C,U>> U getUsedChanges(C changes, Depends<C,U> depends) {
        return getUsedChanges(changes, new ArrayList<DataProperty>(), depends);
    }

    public static <C extends DataChanges<C>,U extends UsedChanges<C,U>> U getUsedChanges(Collection<Property> col, C changes, Collection<DataProperty> usedDefault, Depends<C, U> depends) {
        U result = depends.newChanges();
        for(Property<?> property : col)
            result.add(property.getUsedChanges(changes,usedDefault,depends),null);
        return result;
    }

    public static class TableUsedChanges<U extends TableUsedChanges<U>> extends UsedChanges<TableChanges,U> {
        public TableChanges newChanges() {
            return new TableChanges();
        }
    }
    public interface TableDepends<U extends TableUsedChanges<U>> extends Depends<TableChanges,U> {
        <P extends PropertyInterface> SourceExpr changed(Property<P> property, Map<P, ? extends SourceExpr> joinImplement, WhereBuilder changedWhere);
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

    public Object read(DataSession session, Map<T, DataObject> keys, TableDepends<? extends TableUsedChanges> depends) throws SQLException {
        String readValue = "readvalue";
        JoinQuery<T,Object> readQuery = new JoinQuery<T, Object>(this);

        readQuery.putKeyWhere(keys);

        readQuery.properties.put(readValue, getSourceExpr(readQuery.mapKeys,session.changes,depends,null));
        return BaseUtils.singleValue(readQuery.executeSelect(session)).get(readValue);
    }

/*    public void saveChanges(DataSession session,Map<DataProperty,DefaultData> defaultProps) throws SQLException {

        JoinQuery<KeyField, PropertyField> modifyQuery = new JoinQuery<KeyField, PropertyField>(mapTable.table);
        Map<T, ? extends SourceExpr> joinKeys = BaseUtils.join(mapTable.mapKeys, modifyQuery.mapKeys);

        WhereBuilder changedWhere = new WhereBuilder();
        SourceExpr changedExpr = getSourceExpr(joinKeys, session.changes, defaultProps, changedWhere, new ArrayList<Property>());
        modifyQuery.properties.put(field, changedExpr);
        // если changed и хоть один не null
        modifyQuery.and(changedWhere.toWhere());
        modifyQuery.and(changedExpr.getWhere().or(getSourceExpr(joinKeys).getWhere()));
        session.modifyRecords(new ModifyQuery(mapTable.table,modifyQuery));
    }*/

    public SourceExpr getIncrementExpr(Map<KeyField, ? extends SourceExpr> joinImplement, TableChanges session, TableDepends<? extends TableUsedChanges> depends, WhereBuilder changedWhere) {
        Map<T, ? extends SourceExpr> joinKeys = BaseUtils.join(mapTable.mapKeys,joinImplement);
        WhereBuilder incrementWhere = new WhereBuilder();
        SourceExpr incrementExpr = getSourceExpr(joinKeys, session, depends, incrementWhere);
        changedWhere.add(incrementWhere.toWhere().and(incrementExpr.getWhere().or(getSourceExpr(joinKeys).getWhere()))); // если старые или новые изменились
        return incrementExpr;
    }

    // выстраиваем все в порядок чтобы использовалось по очереди
    public abstract static class Order<O extends Property,C extends DataChanges<C>,U extends UsedChanges<C,U>> implements Depends<C,U>, Iterable<O> {
        
        private final Collection<O> properties;
        private final LinkedHashSet<O> order = new LinkedHashSet<O>();
        public Order(Collection<O> properties,C changes) {
            this.properties = properties;

            for(O property : properties)
                property.getUsedChanges(changes,this);
        }

        public U used(Property property, U usedChanges) {
            if(properties.contains(property)) // если верхний закидываем в order
                order.add((O) property);
            return usedChanges;
        }

        public Iterator<O> iterator() {
            return order.iterator();
        }
    }

    // используется для оптимизации - если Stored то попытать использовать это значение
    protected abstract boolean usePrevious();
}
