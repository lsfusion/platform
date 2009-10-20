package platform.server.logics.properties;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.caches.Lazy;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.logics.DataObject;
import platform.server.logics.data.MapKeysTable;
import platform.server.logics.data.TableFactory;
import platform.server.logics.properties.groups.AbstractNode;
import platform.server.session.*;
import platform.server.where.WhereBuilder;

import java.sql.SQLException;
import java.util.*;

@Immutable
abstract public class Property<T extends PropertyInterface> extends AbstractNode implements MapKeysInterface<T> {

    public int ID=0;
    // символьный идентификатор, с таким именем создаются поля в базе и передаются в PropertyView
    public final String sID;

    public final String caption;

    public String toString() {
        return caption;
    }
    
    Property(String sID, String caption, Collection<T> interfaces) {
        this.sID = sID;
        this.caption = caption;
        this.interfaces = interfaces;
    }

    protected void fillDepends(Set<Property> depends) {
    }

    public Set<Property> getDepends() {
        Set<Property> depends = new HashSet<Property>();
        fillDepends(depends);
        return depends;
    }

    public Collection<T> interfaces;

    public Map<T, KeyExpr> getMapKeys() {
        Map<T, KeyExpr> result = new HashMap<T, KeyExpr>();
        for(T propertyInterface : interfaces)
            result.put(propertyInterface,new KeyExpr(propertyInterface.toString()));
        return result;
    }

    protected static TableModifier<SessionChanges> defaultModifier = new TableModifier<SessionChanges>(){
        public <P extends PropertyInterface> SourceExpr changed(Property<P> property, Map<P, ? extends SourceExpr> joinImplement, WhereBuilder changedWhere) {
            return null;
        }
        public SessionChanges used(Property property, SessionChanges usedChanges) {
            return usedChanges;
        }
        public SessionChanges newChanges() {
            return new SessionChanges();
        }
        public SessionChanges getSession() {
            return null;
        }
    };

    public SourceExpr getSourceExpr(Map<T, ? extends SourceExpr> joinImplement) {
        return getSourceExpr(joinImplement, defaultModifier,null);
    }

    public <U extends TableChanges<U>> SourceExpr getSourceExpr(Map<T, ? extends SourceExpr> joinImplement, TableModifier<U> modifier, WhereBuilder changedWhere) {

        WhereBuilder changedExprWhere = new WhereBuilder();
        SourceExpr changedExpr = modifier.changed(this, joinImplement, changedExprWhere);

        if(changedExpr==null && isStored()) {
            if(!getUsedChanges(modifier).hasChanges()) // если нету изменений
                return mapTable.table.join(BaseUtils.join(BaseUtils.reverse(mapTable.mapKeys), joinImplement)).getExpr(field);
            if(usePreviousStored())
                changedExpr = calculateSourceExpr(joinImplement, modifier, changedExprWhere);
        }

        if(changedExpr!=null) {
            if(changedWhere!=null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(),getSourceExpr(joinImplement));
        } else
            return calculateSourceExpr(joinImplement, modifier, changedWhere);
    }

    public SourceExpr calculateSourceExpr(Map<T, ? extends SourceExpr> joinImplement) {
        return calculateSourceExpr(joinImplement, defaultModifier, null);
    }

    protected abstract SourceExpr calculateSourceExpr(Map<T, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere);

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

    // возвращает от чего "зависят" изменения - с callback'ов
    abstract <U extends DataChanges<U>> U calculateUsedChanges(Modifier<U> modifier);
    public <U extends DataChanges<U>> U getUsedChanges(Modifier<U> modifier) {
        return modifier.used(this, calculateUsedChanges(modifier));
    }

    public static <U extends DataChanges<U>> U getUsedChanges(Collection<Property> col, Modifier<U> modifier) {
        U result = modifier.newChanges();
        for(Property<?> property : col)
            result.add(property.getUsedChanges(modifier));
        return result;
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

    public boolean isFalse = false;

    public MapChangeDataProperty<T> getChangeProperty(Map<T, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        return null;
    }

    public Object read(SQLSession session, Map<T, DataObject> keys, TableModifier<? extends TableChanges> modifier) throws SQLException {
        String readValue = "readvalue";
        JoinQuery<T,Object> readQuery = new JoinQuery<T, Object>(this);

        readQuery.putKeyWhere(keys);

        readQuery.properties.put(readValue, getSourceExpr(readQuery.mapKeys,modifier,null));
        return BaseUtils.singleValue(readQuery.executeSelect(session)).get(readValue);
    }

    public SourceExpr getIncrementExpr(Map<KeyField, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        Map<T, ? extends SourceExpr> joinKeys = BaseUtils.join(mapTable.mapKeys,joinImplement);
        WhereBuilder incrementWhere = new WhereBuilder();
        SourceExpr incrementExpr = getSourceExpr(joinKeys, modifier, incrementWhere);
        changedWhere.add(incrementWhere.toWhere().and(incrementExpr.getWhere().or(getSourceExpr(joinKeys).getWhere()))); // если старые или новые изменились
        return incrementExpr;
    }

    // используется для оптимизации - если Stored то попытать использовать это значение
    protected abstract boolean usePreviousStored();

    @Lazy
    public Property getMaxChangeProperty(DataProperty change) {
        return new MaxChangeProperty(this,change);
    }
}
