package platform.server.session;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.ModifyQuery;
import platform.server.data.PropertyField;
import platform.server.data.classes.*;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.sql.DataAdapter;
import platform.server.data.types.Type;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.data.IDTable;
import platform.server.logics.data.ImplementTable;
import platform.server.logics.properties.*;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;

import java.sql.SQLException;
import java.util.*;

public class DataSession extends SQLSession implements ChangesSession {

    public Map<PropertyUpdateView,ViewDataChanges> incrementChanges = new HashMap<PropertyUpdateView, ViewDataChanges>();

    public TableChanges changes = new TableChanges();

    public BaseClass baseClass;

    // для отладки
    public static boolean reCalculateAggr = false;

    public static Where getIsClassWhere(TableChanges session,SourceExpr expr, ValueClass isClass, WhereBuilder changedWheres) {
        Where isClassWhere = expr.getIsClassWhere(isClass.getUpSet());
        if(session!=null && isClass instanceof CustomClass) {
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

    public DataSession(DataAdapter adapter, BaseClass iCustomClass) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        super(adapter);

        baseClass = iCustomClass;
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

        changes.add = new HashMap<CustomClass, AddClassTable>();
        changes.remove = new HashMap<CustomClass, RemoveClassTable>();
        changes.data = new HashMap<DataProperty, DataChangeTable>();
    }

    public DataObject addObject(ConcreteCustomClass customClass) throws SQLException {
        
        DataObject object = new DataObject(IDTable.instance.generateID(this, IDTable.OBJECT),baseClass.unknown);

        // запишем объекты, которые надо будет сохранять
        changeClass(object, customClass);

        return object;
    }    

    Map<DataObject, ConcreteObjectClass> newClasses = new HashMap<DataObject, ConcreteObjectClass>();

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException {
        if(toClass==null) toClass = baseClass.unknown;

        Set<CustomClass> addClasses = new HashSet<CustomClass>();
        Set<CustomClass> removeClasses = new HashSet<CustomClass>();
        ConcreteObjectClass prevClass = getCurrentClass(change);
        toClass.getDiffSet(prevClass,addClasses,removeClasses);

        for(CustomClass addClass : addClasses) {
            AddClassTable addTable = changes.add.get(addClass);
            if(addTable==null) { // если нету таблицы создаем
                addTable = new AddClassTable(addClass.ID);
                createTemporaryTable(addTable);
            }
            changes.add.put(addClass,addTable.insertRecord(this,Collections.singletonMap(addTable.object,change),new HashMap<PropertyField, ObjectValue>(), false));

            if(!BusinessLogics.autoFillDB) {
                RemoveClassTable removeTable = changes.remove.get(addClass);
                if(removeTable!=null) deleteKeyRecords(removeTable,Collections.singletonMap(removeTable.object,(Integer)change.object));
            }
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
        
        newClasses.put(change,toClass);

        for(ViewDataChanges viewChanges : incrementChanges.values()) {
            viewChanges.addClasses.addAll(addClasses);
            viewChanges.removeClasses.addAll(removeClasses);
        }
    }

    public void changeProperty(DataProperty property, Map<DataPropertyInterface, DataObject> keys, Object newValue, boolean externalID) throws SQLException {
        changeProperty(property, keys, getObjectValue(newValue, property.getType()) ,externalID);
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

            JoinQuery<DataPropertyInterface,String> query = new JoinQuery<DataPropertyInterface, String>(extPropID);
            query.and(extPropID.getSourceExpr(query.mapKeys).compare(newValue.getExpr(),Compare.EQUALS));

            LinkedHashMap<Map<DataPropertyInterface, DataObject>, Map<String, ObjectValue>> result = query.executeSelectClasses(this, baseClass);

            if (result.size() == 0)
                newValue = new NullValue();
            else
                newValue = result.keySet().iterator().next().values().iterator().next();
        }

        changes.data.put(property,dataChange.insertRecord(this,BaseUtils.join(dataChange.mapKeys,keys),Collections.singletonMap(dataChange.value,newValue),true));

        for(ViewDataChanges viewChanges : incrementChanges.values())
            viewChanges.properties.add(property);
    }

    public ConcreteObjectClass getCurrentClass(DataObject value) throws SQLException {
        ConcreteObjectClass newClass;
        if((newClass = newClasses.get(value))==null)
            return (ConcreteObjectClass) value.objectClass;
        else
            return newClass;
    }

    public DataObject getDataObject(Object value, Type type) throws SQLException {
        ConcreteClass dataClass;
        if(type instanceof DataClass)
            dataClass = (DataClass)type;
        else {
            JoinQuery<Object,String> query = new JoinQuery<Object,String>(new HashMap<Object, KeyExpr>());
            Join<PropertyField> joinTable = baseClass.table.joinAnd(Collections.singletonMap(baseClass.table.key,new ValueExpr(value,baseClass.getConcrete())));
            query.and(joinTable.getWhere());
            query.properties.put("classid", joinTable.getExpr(baseClass.table.objectClass));
            LinkedHashMap<Map<Object, Object>, Map<String, Object>> result = query.executeSelect(this);
            if(result.size()==0)
                dataClass = baseClass.unknown;
            else {
                assert (result.size()==1);
                dataClass = baseClass.findConcreteClassID((Integer) result.values().iterator().next().get("classid"));
            }
        }
        return new DataObject(value,dataClass);
    }

    public ObjectValue getObjectValue(Object value, Type type) throws SQLException {
        if(value==null) return new NullValue();
        return getDataObject(value, type);
    }

    // узнает список изменений произошедших без него
    public List<Property> update(PropertyUpdateView toUpdate, Collection<CustomClass> updateClasses) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)

        DataChanges toUpdateChanges = incrementChanges.get(toUpdate);
        if(toUpdateChanges==null) toUpdateChanges = changes;
        incrementChanges.put(toUpdate,new ViewDataChanges());

        updateClasses.addAll(toUpdateChanges.getAddClasses());
        updateClasses.addAll(toUpdateChanges.getRemoveClasses());

        return BusinessLogics.getChangedList(toUpdate.getUpdateProperties(),toUpdateChanges, toUpdate.getNoUpdateProperties(),toUpdate.getDefaultProperties());
    }

    private IncrementChangeTable readIncrementChanges(Collection<Property> properties,Map<DataProperty, DefaultData> defaultProps) throws SQLException {
        // создаем таблицу
        IncrementChangeTable changeTable = new IncrementChangeTable(properties);

        // подготавливаем запрос
        JoinQuery<KeyField,PropertyField> changesQuery = new JoinQuery<KeyField, PropertyField>(changeTable);
        Where groupWhere = Where.FALSE;
        for(Map.Entry<Property,PropertyField> change : changeTable.changes.entrySet()) {
            WhereBuilder changedWhere = new WhereBuilder();
            changesQuery.properties.put(change.getValue(),change.getKey().getSourceExpr(
                    BaseUtils.join(BaseUtils.join(change.getKey().mapTable.mapKeys, changeTable.mapKeys), changesQuery.mapKeys),
                    changes, defaultProps, new ArrayList<Property>(), changedWhere));
            groupWhere = groupWhere.or(changedWhere.toWhere());
        }
        changesQuery.and(groupWhere);

        // подготовили - теперь надо сохранить в курсор и записать классы
        createTemporaryTable(changeTable);
        insertSelect(new ModifyQuery(changeTable,changesQuery));
        changeTable.classes = changesQuery.getClassWhere(new ArrayList<PropertyField>());
        for(PropertyField field : changeTable.changes.values())
            changeTable.propertyClasses.put(field,changesQuery.<Field>getClassWhere(Collections.singleton(field)));

        return changeTable;
    }

    public String apply(BusinessLogics<?> BL) throws SQLException {
        // делается UpdateAggregations (для мн-ва persistent+constraints)
        startTransaction();

        Collection<IncrementChangeTable> temporary = new ArrayList<IncrementChangeTable>();

        List<Property> changedList = BusinessLogics.getChangedList(BaseUtils.join(BL.getStoredProperties(),BL.getConstrainedProperties()),changes,new ArrayList<Property>(),BL.defaultProps);

        try {
            // сохранить св-ва которые Persistent, те что входят в Persistents и DataProperty
            for(Property property : changedList) {
                if(property.constraint!=null) {
                    String constraintResult = property.constraint.check(this,property,BL.defaultProps,new ArrayList<Property>());
                    if(constraintResult!=null) {
                        // откатим транзакцию
                        rollbackTransaction();
                        return constraintResult;
                    }
                }

                if(property.isStored()) { // сохраним изменения в таблицы
                    IncrementChangeTable changeTable = readIncrementChanges(Collections.singleton(property), BL.defaultProps);
                    changes.increment.put(property, changeTable);
                    temporary.add(changeTable);
                }
            }

            // записываем в базу
            for(Collection<Property> groupTable : BaseUtils.group(new BaseUtils.Group<ImplementTable,Property>(){public ImplementTable group(Property key) {return key.mapTable.table;}},
                    changes.increment.keySet()).values()) {
                IncrementChangeTable changeTable;
                if(groupTable.size()==1) // временно так - если одна берем старую иначе группой
                    changeTable = changes.increment.get(groupTable.iterator().next());
                else {
                    changeTable = readIncrementChanges(groupTable,BL.defaultProps);
                    temporary.add(changeTable);
                }

                JoinQuery<KeyField, PropertyField> modifyQuery = new JoinQuery<KeyField, PropertyField>(changeTable.table);
                Join<PropertyField> join = changeTable.join(BaseUtils.join(BaseUtils.reverse(changeTable.mapKeys), modifyQuery.mapKeys));
                for(Map.Entry<Property,PropertyField> change : changeTable.changes.entrySet())
                    modifyQuery.properties.put(change.getKey().field,join.getExpr(change.getValue()));
                modifyQuery.and(join.getWhere());
                modifyRecords(new ModifyQuery(changeTable.table, modifyQuery));
            }

            for(Map.Entry<DataObject,ConcreteObjectClass> newClass : newClasses.entrySet())
                newClass.getValue().saveClassChanges(this,newClass.getKey());

            commitTransaction();
            restart(false);

        } finally { // удаляем changeTables (на каждое св-во будет одна таблица)
            for(IncrementChangeTable addTable : temporary)
                dropTemporaryTable(addTable);
            changes.increment = new HashMap<Property, IncrementChangeTable>();
        }

        return null;
    }

    private final Map<Integer,Integer> sessionIDs = new HashMap<Integer, Integer>();
    public int generateSessionID(int ID) {
        Integer idCounter;
        synchronized(sessionIDs) {
            idCounter = sessionIDs.get(ID);
            if(idCounter==null) idCounter = 0;
            sessionIDs.put(ID,idCounter+1);
        }
        return ID << 4 + idCounter;
    }
}
