package platform.server.session;

import platform.base.BaseUtils;
import platform.server.caches.AbstractMapValues;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.BaseClass;
import platform.server.data.PropertyField;
import platform.server.data.SQLSession;
import platform.server.data.KeyField;
import platform.server.data.Field;
import platform.server.data.query.Join;
import platform.server.data.type.ObjectType;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.DataProperty;

import java.sql.SQLException;
import java.util.*;

public class SessionChanges extends AbstractMapValues<SessionChanges> {

    public final Map<CustomClass, AddClassTable> add;
    public final Map<CustomClass, RemoveClassTable> remove;
    public final Map<DataProperty, DataChangeTable> data;

    public static class NewClassTable extends ChangeClassTable<NewClassTable> {

        public final PropertyField changeClass;

        public NewClassTable() {
            super("newchange");

            changeClass = new PropertyField("change", ObjectType.instance);
            properties.add(changeClass);
        }

        public NewClassTable(String name, KeyField object, PropertyField changeClass, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
            super(name, object, classes, propertyClasses, rows);

            this.changeClass = changeClass;
        }

        public NewClassTable createThis(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
            return new NewClassTable(name, object, changeClass, classes, propertyClasses, rows);
        }
    }
    public NewClassTable newClasses; //final на самом деле, но как в add и remove так как используется this() в явную это не задается

    public boolean hasChanges() {
        return !add.isEmpty() || !remove.isEmpty() || newClasses !=null || !data.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SessionChanges && add.equals(((SessionChanges) o).add) && data.equals(((SessionChanges) o).data) && remove.equals(((SessionChanges) o).remove) && BaseUtils.nullEquals(newClasses, ((SessionChanges) o).newClasses);
    }

    @IdentityLazy
    public int hashValues(HashValues hashValues) {
        return ((MapValuesIterable.hash(add,hashValues) * 31 + MapValuesIterable.hash(remove,hashValues)) * 31 + MapValuesIterable.hash(data,hashValues)) * 31 + (newClasses ==null?0: newClasses.hashValues(hashValues));
    }

    @IdentityLazy
    public Set<ValueExpr> getValues() {
        Set<ValueExpr> result = new HashSet<ValueExpr>();
        MapValuesIterable.enumValues(result,add);
        MapValuesIterable.enumValues(result,remove);
        MapValuesIterable.enumValues(result,data);
        if(newClasses !=null)
            result.addAll(newClasses.getValues());
        return result;
    }

    private SessionChanges() {
        add = new HashMap<CustomClass, AddClassTable>();
        remove = new HashMap<CustomClass, RemoveClassTable>();
        data = new HashMap<DataProperty, DataChangeTable>();

        newClasses = null;
    }

    public final static SessionChanges EMPTY = new SessionChanges();

    private SessionChanges(SessionChanges changes, MapValuesTranslate mapValues) {
        add = mapValues.translateValues(changes.add);
        remove = mapValues.translateValues(changes.remove);
        data = mapValues.translateValues(changes.data);

        newClasses = changes.newClasses == null ? null : changes.newClasses.translate(mapValues);
    }

    public SessionChanges translate(MapValuesTranslate mapValues) {
        return new SessionChanges(this, mapValues);
    }

    // конструктор копирования для 2 частных случаев не до конца чисто но ладно
    protected SessionChanges(SessionChanges changes) {
        this.add = new HashMap<CustomClass, AddClassTable>(changes.add);
        this.remove = new HashMap<CustomClass, RemoveClassTable>(changes.remove);
        this.data = new HashMap<DataProperty, DataChangeTable>(changes.data);

        newClasses = changes.newClasses;
    }

    // обновляет классы, записывая в sql
    public SessionChanges(SessionChanges changes, Set<CustomClass> addClasses, Set<CustomClass> removeClasses, ConcreteObjectClass toClass, DataObject change, SQLSession session) throws SQLException {
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

        if(newClasses==null) {
            newClasses = new NewClassTable();
            session.createTemporaryTable(newClasses);
        }
        newClasses = newClasses.insertRecord(session, Collections.singletonMap(newClasses.object, change), Collections.singletonMap(newClasses.changeClass, toClass.getClassObject()), true);
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
        if(newClasses!=null)
            session.dropTemporaryTable(newClasses);
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

    public Expr getIsClassExpr(Expr expr, BaseClass baseClass, WhereBuilder changedWheres) {
        Expr isClassExpr = expr.classExpr(baseClass);

        if(newClasses!=null) {
            Join<PropertyField> newJoin = newClasses.join(expr);
            Where newWhere = newJoin.getWhere();
            isClassExpr = newJoin.getExpr(newClasses.changeClass).ifElse(newWhere, isClassExpr);
            if(changedWheres!=null) changedWheres.add(newWhere);
        }

        return isClassExpr;
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

    public SessionChanges(SessionChanges changes, boolean classExpr) {
        this();

        newClasses = changes.newClasses; 
    }

    // "отбирает" изменения по свойству
    private SessionChanges(DataProperty property, DataChangeTable table) {
        this();

        data.put(property,table);
    }
    public SessionChanges getSessionChanges(DataProperty property) {
        DataChangeTable dataChange = data.get(property);
        if(dataChange!=null)
            return new SessionChanges(property, dataChange);
        else
            return SessionChanges.EMPTY;
    }

    // "отбирает" изменения по свойству
    private SessionChanges(Map<CustomClass, AddClassTable> add, Map<CustomClass, RemoveClassTable> remove, NewClassTable newClasses) {
        this();

        this.add.putAll(add);
        this.remove.putAll(remove);
        this.newClasses = newClasses; 
    }
    public SessionChanges getSessionChanges(Set<CustomClass> addClasses, Set<CustomClass> removeClasses) {
        return new SessionChanges(BaseUtils.filterKeys(add, addClasses), BaseUtils.filterKeys(remove, removeClasses), newClasses);
    }

    private SessionChanges(SessionChanges changes, SessionChanges merge) {
        add = BaseUtils.merge(changes.add, merge.add);
        remove = BaseUtils.merge(changes.remove, merge.remove);
        data = BaseUtils.merge(changes.data, merge.data);

        newClasses = BaseUtils.nvl(changes.newClasses, merge.newClasses);
    }
    public SessionChanges add(SessionChanges changes) {
        return new SessionChanges(this, changes);
    }
}
