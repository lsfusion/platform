package platform.server.session;

import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.Expr;
import platform.server.data.SQLSession;
import platform.server.data.PropertyField;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.StoredDataProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.DataProperty;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.Lazy;
import platform.server.caches.AbstractMapValues;
import platform.server.caches.hash.HashValues;
import platform.base.BaseUtils;

import java.util.*;
import java.sql.SQLException;

import net.jcip.annotations.Immutable;

@Immutable
public class SessionChanges extends AbstractMapValues<SessionChanges> {

    public final Map<CustomClass, AddClassTable> add;
    public final Map<CustomClass, RemoveClassTable> remove;
    public final Map<DataProperty, DataChangeTable> data;

    public boolean hasChanges() {
        return !add.isEmpty() || !remove.isEmpty() || !data.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SessionChanges && add.equals(((SessionChanges) o).add) && data.equals(((SessionChanges) o).data) && remove.equals(((SessionChanges) o).remove);
    }

    @Lazy
    public int hashValues(HashValues hashValues) {
        return (MapValuesIterable.hash(add,hashValues) * 31 + MapValuesIterable.hash(remove,hashValues)) * 31 + MapValuesIterable.hash(data,hashValues);
    }

    @Lazy
    public Set<ValueExpr> getValues() {
        Set<ValueExpr> result = new HashSet<ValueExpr>();
        MapValuesIterable.enumValues(result,add);
        MapValuesIterable.enumValues(result,remove);
        MapValuesIterable.enumValues(result,data);
        return result;
    }

    private SessionChanges() {
        add = new HashMap<CustomClass, AddClassTable>();
        remove = new HashMap<CustomClass, RemoveClassTable>();
        data = new HashMap<DataProperty, DataChangeTable>();
    }

    public final static SessionChanges EMPTY = new SessionChanges();

    private SessionChanges(SessionChanges changes, Map<ValueExpr,ValueExpr> mapValues) {
        add = MapValuesIterable.translate(changes.add, mapValues);
        remove = MapValuesIterable.translate(changes.remove, mapValues);
        data = MapValuesIterable.translate(changes.data, mapValues);
    }

    public SessionChanges translate(Map<ValueExpr,ValueExpr> mapValues) {
        return new SessionChanges(this, mapValues);
    }

    // конструктор копирования для 2 частных случаев не до конца чисто но ладно
    protected SessionChanges(SessionChanges changes) {
        this.add = new HashMap<CustomClass, AddClassTable>(changes.add);
        this.remove = new HashMap<CustomClass, RemoveClassTable>(changes.remove);
        this.data = new HashMap<DataProperty, DataChangeTable>(changes.data);
    }

    // обновляет классы, записывая в sql
    public SessionChanges(SessionChanges changes, Set<CustomClass> addClasses, Set<CustomClass> removeClasses, DataObject change, SQLSession session) throws SQLException {
        this(changes);

        for(CustomClass addClass : addClasses) {
            AddClassTable addTable = add.get(addClass);
            if(addTable==null) { // если нету таблицы создаем
                addTable = new AddClassTable(addClass.ID);
                session.createTemporaryTable(addTable);
            }
            add.put(addClass,addTable.insertRecord(session, Collections.singletonMap(addTable.object,change),new HashMap<PropertyField, ObjectValue>(), false));

            RemoveClassTable removeTable = remove.get(addClass);
            if(removeTable!=null) remove.put(addClass,removeTable.deleteRecords(session,Collections.singletonMap(removeTable.object,change))) ;
        }
        for(CustomClass removeClass : removeClasses) {
            RemoveClassTable removeTable = remove.get(removeClass);
            if(removeTable==null) { // если нету таблицы создаем
                removeTable = new RemoveClassTable(removeClass.ID);
                session.createTemporaryTable(removeTable);
            }
            remove.put(removeClass,removeTable.insertRecord(session,Collections.singletonMap(removeTable.object,change),new HashMap<PropertyField, ObjectValue>(), false));

            AddClassTable addTable = add.get(removeClass);
            if(addTable!=null) add.put(removeClass,addTable.deleteRecords(session,Collections.singletonMap(addTable.object,change)));
        }

        for(Map.Entry<DataProperty,DataChangeTable> dataChange : data.entrySet()) // удаляем существующие изменения
            dataChange.setValue(dataChange.getValue().dropChanges(session, dataChange.getKey(), change, removeClasses));
    }

    // обновляет первичное свойство, записывая в sql
    public SessionChanges(SessionChanges changes, DataProperty property, Map<ClassPropertyInterface, DataObject> keys, ObjectValue newValue, SQLSession session) throws SQLException {
        this(changes); // конструктор копирования не очень правильно для immutable, но здесь вполне элегантно смотрится
        
        DataChangeTable dataChange = data.get(property);
        if(dataChange == null) { // создадим таблицу, если не было
            dataChange = new DataChangeTable(property);
            session.createTemporaryTable(dataChange);
        }
        data.put(property, dataChange.insertRecord(session, BaseUtils.join(dataChange.mapKeys, keys), Collections.singletonMap(dataChange.value, newValue), true));
    }

    public void dropTables(SQLSession session) throws SQLException {
        for(AddClassTable addTable : add.values())
            session.dropTemporaryTable(addTable);
        for(RemoveClassTable removeTable : remove.values())
            session.dropTemporaryTable(removeTable);
        for(DataChangeTable dataTable : data.values())
            session.dropTemporaryTable(dataTable);
    }

    public Where getIsClassWhere(Expr expr, ValueClass isClass, WhereBuilder changedWheres) {
        Where isClassWhere = expr.isClass(isClass.getUpSet());
        if(isClass instanceof CustomClass) {
            RemoveClassTable removeTable = remove.get((CustomClass)isClass);
            if(removeTable!=null) {
                Where removeWhere = removeTable.getJoinWhere(expr);
                isClassWhere = isClassWhere.and(removeWhere.not());
                if(changedWheres!=null) changedWheres.add(removeWhere);
            }

            AddClassTable addTable = add.get((CustomClass)isClass);
            if(addTable!=null) {
                Where addWhere = addTable.getJoinWhere(expr);
                isClassWhere = isClassWhere.or(addWhere);
                if(changedWheres!=null) changedWheres.add(addWhere);
            }
        }
        return isClassWhere;
    }

    public Where getRemoveWhere(ValueClass valueClass, Expr expr) {
        RemoveClassTable removeTable;
        if(valueClass instanceof CustomClass && ((removeTable = remove.get((CustomClass)valueClass))!=null))
            return removeTable.getJoinWhere(expr);
        else
            return Where.FALSE;
    }

    // "отбирает" изменения по переданным классам
    public SessionChanges(SessionChanges changes, Collection<ValueClass> valueClasses, boolean onlyRemove) {
        this();
        
        RemoveClassTable removeTable;
        for(ValueClass valueClass : valueClasses)
            if(valueClass instanceof CustomClass && ((removeTable = changes.remove.get((CustomClass)valueClass))!=null))
                remove.put((CustomClass) valueClass,removeTable);

        AddClassTable addTable;
        if(!onlyRemove)
            for(ValueClass valueClass : valueClasses)
                if(valueClass instanceof CustomClass && ((addTable = changes.add.get((CustomClass)valueClass))!=null))
                    add.put((CustomClass) valueClass,addTable);
    }

    private SessionChanges(DataProperty property, DataChangeTable table) {
        this();

        data.put(property,table);
    }
    // "отбирает" изменения по свойству
    public SessionChanges getSessionChanges(DataProperty property) {
        DataChangeTable dataChange = data.get(property);
        if(dataChange!=null)
            return new SessionChanges(property, dataChange);
        else
            return SessionChanges.EMPTY;
    }

    private SessionChanges(SessionChanges changes, SessionChanges merge) {
        add = BaseUtils.merge(changes.add, merge.add);
        remove = BaseUtils.merge(changes.remove, merge.remove);
        data = BaseUtils.merge(changes.data, merge.data);
    }
    public SessionChanges add(SessionChanges changes) {
        return new SessionChanges(this, changes);
    }
}
