package platform.server.logics.property;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.caches.Lazy;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.SQLSession;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.where.classes.ClassWhere;
import platform.server.data.query.Query;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.table.MapKeysTable;
import platform.server.logics.table.TableFactory;
import platform.server.logics.property.group.AbstractNode;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;

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
        public SessionChanges newChanges() {
            return new SessionChanges();
        }
        public SessionChanges getSession() {
            return null;
        }
        public SessionChanges used(Property property, SessionChanges usedChanges) {
            return usedChanges;
        }
        public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
            return null;
        }
    };

    public Expr getExpr(Map<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, defaultModifier,null);
    }

    public <U extends TableChanges<U>> Expr getExpr(Map<T, ? extends Expr> joinImplement, TableModifier<U> modifier, WhereBuilder changedWhere) {

        WhereBuilder changedExprWhere = new WhereBuilder();
        Expr changedExpr = modifier.changed(this, joinImplement, changedExprWhere);

        if(changedExpr==null && isStored()) {
            if(!getUsedChanges(modifier).hasChanges()) // если нету изменений
                return mapTable.table.join(BaseUtils.join(BaseUtils.reverse(mapTable.mapKeys), joinImplement)).getExpr(field);
            if(usePreviousStored())
                changedExpr = calculateExpr(joinImplement, modifier, changedExprWhere);
        }

        if(changedExpr!=null) {
            if(changedWhere!=null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(), getExpr(joinImplement));
        } else
            return calculateExpr(joinImplement, modifier, changedWhere);
    }

    public Expr calculateExpr(Map<T, ? extends Expr> joinImplement) {
        return calculateExpr(joinImplement, defaultModifier, null);
    }

    protected abstract Expr calculateExpr(Map<T, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere);

    @Lazy
    public boolean anyInInterface(Map<T, ? extends AndClassSet> interfaceClasses) {
        return !getClassWhere().andCompatible(new ClassWhere<T>(interfaceClasses)).isFalse();
    }

    @Lazy
    public boolean allInInterface(Map<T,? extends AndClassSet> interfaceClasses) {
        return new ClassWhere<T>(interfaceClasses).meansCompatible(getClassWhere());
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
    <JV> Query<T,JV> getQuery(JV value) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return new Query<T,JV>(mapKeys, getExpr(mapKeys),value);
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
    public boolean checkChange = true;

    public DataChange getChangeProperty(DataSession session, Map<T, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        return null;
    }

    public PropertyChange getJoinChangeProperty(DataSession session, Map<T, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        return null;
    }

    public Object read(SQLSession session, Map<T, DataObject> keys, TableModifier<? extends TableChanges> modifier) throws SQLException {
        String readValue = "readvalue";
        Query<T,Object> readQuery = new Query<T, Object>(this);

        readQuery.putKeyWhere(keys);

        readQuery.properties.put(readValue, getExpr(readQuery.mapKeys,modifier,null));
        return BaseUtils.singleValue(readQuery.execute(session)).get(readValue);
    }

    public Expr getIncrementExpr(Map<KeyField, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        Map<T, ? extends Expr> joinKeys = BaseUtils.join(mapTable.mapKeys,joinImplement);
        WhereBuilder incrementWhere = new WhereBuilder();
        Expr incrementExpr = getExpr(joinKeys, modifier, incrementWhere);
        changedWhere.add(incrementWhere.toWhere().and(incrementExpr.getWhere().or(getExpr(joinKeys).getWhere()))); // если старые или новые изменились
        return incrementExpr;
    }

    // используется для оптимизации - если Stored то попытать использовать это значение
    protected abstract boolean usePreviousStored();

    @Lazy
    public Property getMaxChangeProperty(DataProperty change) {
        return new MaxChangeProperty(this,change);
    }
}
