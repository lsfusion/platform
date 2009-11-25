package platform.server.session;

import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.data.*;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.sql.DataAdapter;
import platform.server.data.type.Type;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.table.IDTable;
import platform.server.logics.table.ImplementTable;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.DataPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.view.form.RemoteForm;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.classes.*;

import java.sql.SQLException;
import java.util.*;

public class DataSession extends SQLSession implements ChangesSession {

    public Map<RemoteForm,ViewDataChanges> incrementChanges = new HashMap<RemoteForm, ViewDataChanges>();

    public SessionChanges changes = new SessionChanges();

    public final BaseClass baseClass;
    public final CustomClass namedObject;
    public final DataProperty name;
    public final CustomClass transaction;
    public final DataProperty date;

    // для отладки
    public static boolean reCalculateAggr = false;

    public static Where getIsClassWhere(SessionChanges session, Expr expr, ValueClass isClass, WhereBuilder changedWheres) {
        Where isClassWhere = expr.isClass(isClass.getUpSet());
        if(isClass instanceof CustomClass && session!=null) {
            RemoveClassTable removeTable = session.remove.get((CustomClass)isClass);
            if(removeTable!=null) {
                Where removeWhere = removeTable.getJoinWhere(expr);
                isClassWhere = isClassWhere.and(removeWhere.not());
                if(changedWheres!=null) changedWheres.add(removeWhere);
            }
            AddClassTable addTable = session.add.get((CustomClass)isClass);
            if(addTable!=null) {
                Where addWhere = addTable.getJoinWhere(expr);
                isClassWhere = isClassWhere.or(addWhere);
                if(changedWheres!=null) changedWheres.add(addWhere);
            }
        }
        return isClassWhere;
    }

    public DataSession(DataAdapter adapter, BaseClass baseClass, CustomClass namedObject, DataProperty name, CustomClass transaction, DataProperty date) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        super(adapter);

        this.baseClass = baseClass;
        this.namedObject = namedObject;
        this.name = name;
        this.transaction = transaction;
        this.date = date;
    }

    public void restart(boolean cancel) throws SQLException {

        if(cancel)
            for(ViewDataChanges viewChanges : incrementChanges.values()) {
                viewChanges.properties.addAll(changes.data.keySet());
                viewChanges.addClasses.addAll(changes.add.keySet());
                viewChanges.removeClasses.addAll(changes.remove.keySet());
            }

        newClasses = new HashMap<DataObject, ConcreteObjectClass>();

        for(AddClassTable addTable : changes.add.values())
            dropTemporaryTable(addTable);
        for(RemoveClassTable removeTable : changes.remove.values())
            dropTemporaryTable(removeTable);
        for(DataChangeTable dataTable : changes.data.values())
            dropTemporaryTable(dataTable);

        changes = new SessionChanges();
    }

    public DataObject addObject(ConcreteCustomClass customClass) throws SQLException {

        DataObject object = new DataObject(IDTable.instance.generateID(this, IDTable.OBJECT),baseClass.unknown);

        // запишем объекты, которые надо будет сохранять
        changeClass(object, customClass);
        
        if(customClass.isChild(namedObject))
            changeProperty(name,Collections.singletonMap(BaseUtils.single(name.interfaces),object),
                    new DataObject(customClass.caption+" "+object.object,(StringClass)name.value),false);

        if(customClass.isChild(transaction))
            changeProperty(date,Collections.singletonMap(BaseUtils.single(date.interfaces),object),
                    new DataObject(DateConverter.dateToInt(new Date()),(DateClass)date.value),false);

        return object;
    }    

    Map<DataObject, ConcreteObjectClass> newClasses = new HashMap<DataObject, ConcreteObjectClass>();

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException {
        if(toClass==null) toClass = baseClass.unknown;

        Set<CustomClass> addClasses = new HashSet<CustomClass>();
        Set<CustomClass> removeClasses = new HashSet<CustomClass>();
        ConcreteObjectClass prevClass = (ConcreteObjectClass) getCurrentClass(change);
        toClass.getDiffSet(prevClass,addClasses,removeClasses);

        for(CustomClass addClass : addClasses) {
            AddClassTable addTable = changes.add.get(addClass);
            if(addTable==null) { // если нету таблицы создаем
                addTable = new AddClassTable(addClass.ID);
                createTemporaryTable(addTable);
            }
            changes.add.put(addClass,addTable.insertRecord(this,Collections.singletonMap(addTable.object,change),new HashMap<PropertyField, ObjectValue>(), false));

            if(BusinessLogics.autoFillDB) continue;

            RemoveClassTable removeTable = changes.remove.get(addClass);
            if(removeTable!=null) deleteKeyRecords(removeTable,Collections.singletonMap(removeTable.object,(Integer)change.object));
        }
        for(CustomClass removeClass : removeClasses) {
            RemoveClassTable removeTable = changes.remove.get(removeClass);
            if(removeTable==null) { // если нету таблицы создаем
                removeTable = new RemoveClassTable(removeClass.ID);
                createTemporaryTable(removeTable);
            }
            changes.remove.put(removeClass,removeTable.insertRecord(this,Collections.singletonMap(removeTable.object,change),new HashMap<PropertyField, ObjectValue>(), false));

            AddClassTable addTable = changes.add.get(removeClass);
            if(addTable!=null) deleteKeyRecords(addTable,Collections.singletonMap(addTable.object,(Integer)change.object));
        }

        for(Map.Entry<DataProperty,DataChangeTable> dataChange : changes.data.entrySet())
            dataChange.getValue().dropChanges(this, dataChange.getKey(), change, removeClasses);

        newClasses.put(change,toClass);

        for(ViewDataChanges viewChanges : incrementChanges.values()) {
            viewChanges.addClasses.addAll(addClasses);
            viewChanges.removeClasses.addAll(removeClasses);
        }
    }

    public void changeProperty(DataProperty property, Map<DataPropertyInterface, DataObject> keys, ObjectValue newValue, boolean externalID) throws SQLException {
        DataChangeTable dataChange = changes.data.get(property);
        if(dataChange == null) { // создадим таблицу, если не было
            dataChange = new DataChangeTable(property);
            createTemporaryTable(dataChange);
        }
        
        // если изменяем по внешнему коду, но сначала надо найти внутренний код, а затем менять
        if (externalID) {
            DataProperty extPropID = property.value.getExternalID();

            Query<DataPropertyInterface,String> query = new Query<DataPropertyInterface, String>(extPropID);
            query.and(extPropID.getExpr(query.mapKeys).compare((DataObject) newValue,Compare.EQUALS));

            OrderedMap<Map<DataPropertyInterface, DataObject>, Map<String, ObjectValue>> result = query.executeClasses(this, baseClass);

            if (result.size() == 0)
                newValue = NullValue.instance;
            else
                newValue = BaseUtils.singleValue(result.singleKey());
        }

        changes.data.put(property,dataChange.insertRecord(this,BaseUtils.join(dataChange.mapKeys,keys),Collections.singletonMap(dataChange.value,newValue),true));

        for(ViewDataChanges viewChanges : incrementChanges.values())
            viewChanges.properties.add(property);
    }

    public ConcreteClass getCurrentClass(DataObject value) {
        ConcreteClass newClass;
        if((newClass = newClasses.get(value))==null)
            return value.objectClass;
        else
            return newClass;
    }

    public <T> Map<T, ConcreteClass> getCurrentClasses(Map<T, DataObject> map) {
        Map<T, ConcreteClass> result = new HashMap<T, ConcreteClass>();
        for(Map.Entry<T,DataObject> entry : map.entrySet())
            result.put(entry.getKey(),getCurrentClass(entry.getValue()));
        return result;
    }

    public DataObject getDataObject(Object value, Type type) throws SQLException {
        ConcreteClass dataClass;
        if(type instanceof DataClass)
            dataClass = (DataClass)type;
        else {
            Query<Object,String> query = new Query<Object,String>(new HashMap<Object, KeyExpr>());
            Join<PropertyField> joinTable = baseClass.table.joinAnd(Collections.singletonMap(baseClass.table.key,new ValueExpr(value,baseClass.getConcrete())));
            query.and(joinTable.getWhere());
            query.properties.put("classid", joinTable.getExpr(baseClass.table.objectClass));
            OrderedMap<Map<Object, Object>, Map<String, Object>> result = query.execute(this);
            if(result.size()==0)
                dataClass = baseClass.unknown;
            else {
                assert (result.size()==1);
                dataClass = baseClass.findConcreteClassID((Integer) result.singleValue().get("classid"));
            }
        }
        return new DataObject(value,dataClass);
    }

    public ObjectValue getObjectValue(Object value, Type type) throws SQLException {
        if(value==null)
            return NullValue.instance;
        else
            return getDataObject(value, type);
    }

    // узнает список изменений произошедших без него
    public Collection<Property> update(RemoteForm<?> form, Collection<CustomClass> updateClasses) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)

        Collection<Property> result = new ArrayList<Property>();

        ViewDataChanges incrementChange = incrementChanges.get(form);
        if(incrementChange==null)
            incrementChange = new ViewDataChanges(changes);
        incrementChanges.put(form,new ViewDataChanges());

        updateClasses.addAll(incrementChange.addClasses);
        updateClasses.addAll(incrementChange.removeClasses);

        ViewModifier formUpdate = form.update(incrementChange);
        for(Property<?> property : form.getUpdateProperties())
            if(property.getUsedChanges(formUpdate).hasChanges())
                result.add(property);
        return result;
    }

    private static class Increment extends TableModifier<Increment.UsedChanges> {

        Map<Property, IncrementChangeTable> tables = new HashMap<Property, IncrementChangeTable>(); 

        public final DataSession session;

        public SessionChanges getSession() {
            return session.changes;
        }

        private Increment(DataSession session) {
            this.session = session;
        }

        static class UsedChanges extends TableChanges<UsedChanges> {
            final Map<Property, IncrementChangeTable> increment = new HashMap<Property, IncrementChangeTable>();

            @Override
            public boolean hasChanges() {
                return super.hasChanges() || !increment.isEmpty();
            }

            @Override
            public void add(UsedChanges add) {
                super.add(add);
                increment.putAll(add.increment);
            }

            @Override
            public boolean equals(Object o) {
                return this==o || o instanceof UsedChanges && increment.equals(((UsedChanges)o).increment) && super.equals(o);
            }

            @Override
            public int hashCode() {
                return 31 * super.hashCode() + increment.hashCode();
            }
        }

        public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
            IncrementChangeTable incrementTable = tables.get(property);
            if(incrementTable!=null) { // если уже все посчитано - просто возвращаем его
                Join<PropertyField> incrementJoin = incrementTable.join(BaseUtils.join(BaseUtils.reverse(BaseUtils.join(property.mapTable.mapKeys, incrementTable.mapKeys)), joinImplement));
                changedWhere.add(incrementJoin.getWhere());
                return incrementJoin.getExpr(incrementTable.changes.get(property));
            } else
                return null;
        }

        public UsedChanges used(Property property, UsedChanges usedChanges) {
            IncrementChangeTable incrementTable = tables.get(property);
            if(incrementTable!=null) {
                usedChanges = new UsedChanges();
                usedChanges.increment.put(property,incrementTable);
            }
            return usedChanges;
        }

        public UsedChanges newChanges() {
            return new UsedChanges();
        }

        public IncrementChangeTable read(Collection<Property> properties) throws SQLException {
            // создаем таблицу
            IncrementChangeTable changeTable = new IncrementChangeTable(properties);

            // подготавливаем запрос
            Query<KeyField,PropertyField> changesQuery = new Query<KeyField, PropertyField>(changeTable);
            WhereBuilder changedWhere = new WhereBuilder();
            for(Map.Entry<Property,PropertyField> change : changeTable.changes.entrySet())
                changesQuery.properties.put(change.getValue(),
                        change.getKey().getIncrementExpr(BaseUtils.join(changeTable.mapKeys, changesQuery.mapKeys), this, changedWhere));
            changesQuery.and(changedWhere.toWhere());

            // подготовили - теперь надо сохранить в курсор и записать классы
            session.createTemporaryTable(changeTable);
            session.insertSelect(new ModifyQuery(changeTable,changesQuery));
            changeTable.classes = changesQuery.getClassWhere(new ArrayList<PropertyField>());
            for(PropertyField field : changeTable.changes.values())
                changeTable.propertyClasses.put(field,changesQuery.<Field>getClassWhere(Collections.singleton(field)));

            for(Property property : properties)
                tables.put(property,changeTable);

            return changeTable;
        }

        private Expr getNameExpr(Expr expr) {
            return session.name.getExpr(Collections.singletonMap(BaseUtils.single(session.name.interfaces),expr),this,null);
        }

        public <T extends PropertyInterface> String check(Property<T> property) throws SQLException {
            if(property.isFalse) {
                Query<T,String> changed = new Query<T,String>(property);

                WhereBuilder changedWhere = new WhereBuilder();
                Expr valueExpr = property.getExpr(changed.mapKeys,this,changedWhere);
                changed.and(valueExpr.getWhere());
                changed.and(changedWhere.toWhere()); // только на измененные смотрим

                // сюда надо name'ы вставить
                for(T propertyInterface : property.interfaces)
                   changed.properties.put("int"+propertyInterface.ID,getNameExpr(changed.mapKeys.get(propertyInterface)));

                OrderedMap<Map<T, Object>, Map<String, Object>> result = changed.execute(session);
                if(result.size()>0) {
                    String resultString = property.toString() + '\n';
                    for(Map.Entry<Map<T,Object>,Map<String,Object>> row : result.entrySet()) {
                        String objects = "";
                        for(T propertyInterface : property.interfaces)
                            objects = (objects.length()==0?"":objects+", ") + row.getKey().get(propertyInterface)+" "+BaseUtils.nullString((String) row.getValue().get("int"+propertyInterface.ID)).trim();
                        resultString += "    " + objects + '\n';
                    }

                    return resultString;
                }
            }

            return null;
        }
    }

    public String apply(final BusinessLogics<?> BL) throws SQLException {
        // делается UpdateAggregations (для мн-ва persistent+constraints)
        startTransaction();

        Increment increment = new Increment(this);
        Collection<IncrementChangeTable> temporary = new ArrayList<IncrementChangeTable>();

        // сохранить св-ва которые Persistent, те что входят в Persistents и DataProperty
        for(Property<?> property : BL.getAppliedProperties()) {
            if(property.getUsedChanges(increment).hasChanges()) {
                String constraintResult = increment.check(property);
                if(constraintResult!=null) {
                    // откатим транзакцию
                    rollbackTransaction();
                    return constraintResult;
                }

                if(property.isStored()) // сохраним изменения в таблицы
                    temporary.add(increment.read(Collections.<Property>singleton(property)));
            }
        }

        // записываем в базу
        for(Collection<Property> groupTable : BaseUtils.group(new BaseUtils.Group<ImplementTable,Property>(){public ImplementTable group(Property key) {return key.mapTable.table;}},
                increment.tables.keySet()).values()) {
            IncrementChangeTable changeTable;
            if(groupTable.size()==1) // временно так - если одна берем старую иначе группой
                changeTable = increment.tables.get(groupTable.iterator().next());
            else {
                changeTable = increment.read(groupTable);
                temporary.add(changeTable);
            }

            Query<KeyField, PropertyField> modifyQuery = new Query<KeyField, PropertyField>(changeTable.table);
            Join<PropertyField> join = changeTable.join(BaseUtils.join(BaseUtils.reverse(changeTable.mapKeys), modifyQuery.mapKeys));
            for(Map.Entry<Property,PropertyField> change : changeTable.changes.entrySet())
                modifyQuery.properties.put(change.getKey().field,join.getExpr(change.getValue()));
            modifyQuery.and(join.getWhere());
            modifyRecords(new ModifyQuery(changeTable.table, modifyQuery));
        }

        for(Map.Entry<DataObject,ConcreteObjectClass> newClass : newClasses.entrySet())
            newClass.getValue().saveClassChanges(this,newClass.getKey());

        commitTransaction();

        for(IncrementChangeTable addTable : temporary)
            dropTemporaryTable(addTable);

        restart(false);

        return null;
    }

    private final Map<Integer,Integer> viewIDs = new HashMap<Integer, Integer>();
    public int generateViewID(int ID) {
        Integer idCounter;
        synchronized(viewIDs) {
            idCounter = viewIDs.get(ID);
            if(idCounter==null) idCounter = 0;
            viewIDs.put(ID,idCounter+1);
        }
        return (ID << 6) + idCounter;
    }
}
