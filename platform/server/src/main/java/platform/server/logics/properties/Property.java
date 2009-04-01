package platform.server.logics.properties;

import platform.base.BaseUtils;
import platform.server.data.KeyField;
import platform.server.data.ModifyQuery;
import platform.server.data.PropertyField;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.LinearExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.logics.ObjectValue;
import platform.server.logics.BusinessLogics;
import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.*;
import platform.server.logics.data.TableFactory;
import platform.server.logics.data.MapKeysTable;
import platform.server.logics.properties.groups.AbstractNode;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

abstract public class Property<T extends PropertyInterface> extends AbstractNode implements PropertyClass<T> {

    public int ID=0;
    // символьный идентификатор, с таким именем создаются поля в базе и передаются в PropertyView
    public String sID;

    TableFactory tableFactory;

    // строится по сути "временный" Map PropertyInterface'ов на Objects'ы
    Map<T, KeyField> changeTableMap = null;
    // раз уж ChangeTableMap закэшировали то и ChangeTable тоже
    public IncrementChangeTable changeTable;

    // пока так из-за getType - объект может быть еще не проинициализирован
    public void fillChangeTable() {
        // заполняем привязку к таблицам изменений
        changeTable = tableFactory.getChangeTable(interfaces.size(), getType());
        changeTableMap = new HashMap<T, KeyField>();
        Iterator<KeyField> io = changeTable.objects.iterator();
        for(T propertyInterface : interfaces)
            changeTableMap.put(propertyInterface,io.next());
    }

    Property(String iSID,Collection<T> iInterfaces,TableFactory iTableFactory) {
        tableFactory = iTableFactory;
        sID = iSID;
        interfaces = iInterfaces;
    }


    public Collection<T> interfaces;

    // закэшируем чтобы быстрее работать
    // здесь как и в произвольных Left значит что могут быть null, не Left соответственно только не null
    // (пока в нашем случае просто можно убирать записи где точно null)
    public SourceExpr getSourceExpr(Map<T,? extends SourceExpr> joinImplement, InterfaceClassSet<T> joinClasses) {

        if(isStored()) {
            // если persistent читаем из таблицы
            // прогоним проверим все ли Implement'ировано
            return new Join<KeyField, PropertyField>(mapTable.table, BaseUtils.join(mapTable.mapKeys,joinImplement)).exprs.get(field);
        } else
            return ((AggregateProperty<T>)this).calculateSourceExpr(joinImplement, joinClasses);
    }

    public boolean isInInterface(InterfaceClassSet<T> ClassImplement) {
//        return true;
        for(InterfaceClass<T> InterfaceClass : ClassImplement)
            if(!getValueClass(InterfaceClass).isEmpty()) return true;
        return false;
    }

    // получает базовый класс чтобы определять
    public ClassSet getBaseClass() {
        ClassSet ResultClass = new ClassSet();
        for(InterfaceClass<T> InterfaceClass : getClassSet(ClassSet.universal))
            ResultClass.or(getValueClass(InterfaceClass));
        return ResultClass;
    }

    public InterfaceClassSet<T> getUniversalInterface() {
        InterfaceClass<T> Result = new InterfaceClass<T>();
        for(T Interface : interfaces)
            Result.put(Interface,ClassSet.universal);
        return new InterfaceClassSet<T>(Result);
    }

    public Type getType() {
        return getBaseClass().getType();
    }

    public String caption = "";

    public String toString() {
        return caption;
    }

    Map<InterfaceClass<T>,ClassSet> cacheValueClass = new HashMap<InterfaceClass<T>, ClassSet>();
    abstract ClassSet calculateValueClass(InterfaceClass<T> interfaceImplement);
    public ClassSet getValueClass(InterfaceClass<T> interfaceImplement) {
        if(!BusinessLogics.activateCaches) return calculateValueClass(interfaceImplement);
        ClassSet Result = cacheValueClass.get(interfaceImplement);
        if(Result==null) {
            Result = calculateValueClass(interfaceImplement);
            cacheValueClass.put(interfaceImplement,Result);
        }
        return Result;
    }

    Map<ClassSet, InterfaceClassSet<T>> cacheClassSet = new HashMap<ClassSet, InterfaceClassSet<T>>();
    abstract InterfaceClassSet<T> calculateClassSet(ClassSet reqValue);
    public InterfaceClassSet<T> getClassSet(ClassSet reqValue) {
        if(!BusinessLogics.activateCaches) return calculateClassSet(reqValue);
        InterfaceClassSet<T> Result = cacheClassSet.get(reqValue);
        if(Result==null) {
            Result = calculateClassSet(reqValue);
            cacheClassSet.put(reqValue,Result);
        }
        return Result;
    }

    ValueClassSet<T> cacheValueClassSet = null;
    abstract ValueClassSet<T> calculateValueClassSet();
    public ValueClassSet<T> getValueClassSet() {
        if(!BusinessLogics.activateCaches) return calculateValueClassSet();
        if(cacheValueClassSet ==null) cacheValueClassSet = calculateValueClassSet();
        return cacheValueClassSet;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean fillChanges(List<Property> changedProperties, DataChanges changes, Collection<Property> noUpdate) {
        if(changedProperties.contains(this)) return true;
        if(noUpdate.contains(this)) return false;

        boolean changed = fillDependChanges(changedProperties, changes, noUpdate);
        if(changed)
            changedProperties.add(this);
        return changed;
    }
    protected abstract boolean fillDependChanges(List<Property> changedProperties, DataChanges changes, Collection<Property> noUpdate);

    JoinQuery<T,String> getOutSelect(String Value) {
        JoinQuery<T,String> Query = new JoinQuery<T,String>(interfaces);
        SourceExpr ValueExpr = getSourceExpr(Query.mapKeys,getClassSet(ClassSet.universal));
        Query.properties.put(Value, ValueExpr);
        Query.and(ValueExpr.getWhere());
        return Query;
    }

    public void out(DataSession Session) throws SQLException {
        System.out.println(caption);
        getOutSelect("value").outSelect(Session);
    }

    public boolean isObject() {
        // нужно также проверить
        for(InterfaceClass<T> InterfaceClass : getClassSet(ClassSet.universal))
            for(ClassSet Interface : InterfaceClass.values())
                if(Interface.intersect(ClassSet.getUp(RemoteClass.data)))
                    return false;

        return true;
    }

    void setChangeType(Map<Property, Integer> requiredTypes,int changeType) {
        // 0 и 0 = 0
        // 0 и 1 = 2
        // 1 и 1 = 1
        // 2 и x = 2

        // значит не изменилось (тогда не надо)
        if(!requiredTypes.containsKey(this)) return;

        Integer prevType = requiredTypes.get(this);
        if(prevType!=null && !prevType.equals(changeType)) changeType = 2;
        requiredTypes.put(this,changeType);
    }

    void outChangesTable(DataSession session) throws SQLException {
        JoinQuery<T, PropertyField> query = new JoinQuery<T, PropertyField>(interfaces);

        Join<KeyField, PropertyField> changeJoin = new Join<KeyField, PropertyField>(changeTable,query, changeTableMap);
        changeJoin.joins.put(changeTable.property,changeTable.property.type.getExpr(ID));
        query.and(changeJoin.inJoin);

        query.properties.put(changeTable.value, changeJoin.exprs.get(changeTable.value));
        query.properties.put(changeTable.prevValue, changeJoin.exprs.get(changeTable.prevValue));

        query.outSelect(session);
    }

    public PropertyField field;

    public MapKeysTable<T> mapTable; // именно здесь потому как не обязательно persistent
    public void markStored() {
        mapTable = tableFactory.getMapTable(getClassSet(ClassSet.universal).getCommonParent());

        field = new PropertyField(sID,getType());
        mapTable.table.properties.add(field);
    }
    public boolean isStored() {
        return field !=null && !(this instanceof AggregateProperty && tableFactory.reCalculateAggr); // для тестирования 2-е условие
    }

    // базовые методы - ничего не делать, его перегружают только Override и Data
    public ChangeValue getChangeProperty(DataSession Session, Map<T, ObjectValue> Keys, int Coeff, ChangePropertySecurityPolicy securityPolicy) throws SQLException { return null;}
    public void changeProperty(Map<T, ObjectValue> Keys, Object NewValue, boolean externalID, DataSession Session, ChangePropertySecurityPolicy securityPolicy) throws SQLException {}

    // заполняет требования к изменениям
    public abstract void fillRequiredChanges(Integer incrementType, Map<Property, Integer> requiredTypes);

    // для каскадного выполнения (запрос)
    public boolean XL = false;

    // получает запрос для инкрементных изменений
    public abstract Property.Change incrementChanges(DataSession session, int changeType) throws SQLException;

    // присоединяют объекты
    void joinChangeClass(ChangeClassTable Table,JoinQuery<DataPropertyInterface,?> Query, DataSession Session, DataPropertyInterface Interface) {
        Join<KeyField, PropertyField> ClassJoin = new Join<KeyField, PropertyField>(Table.getClassJoin(Session,Interface.interfaceClass));
        ClassJoin.joins.put(Table.object,Query.mapKeys.get(Interface));
        Query.and(ClassJoin.inJoin);
    }

    void joinObjects(JoinQuery<DataPropertyInterface,?> Query, DataPropertyInterface Interface) {
        Join<KeyField, PropertyField> ClassJoin = new Join<KeyField, PropertyField>(tableFactory.objectTable.getClassJoin(Interface.interfaceClass));
        ClassJoin.joins.put(tableFactory.objectTable.key,Query.mapKeys.get(Interface));
        Query.and(ClassJoin.inJoin);
    }

    // тип по умолчанию, если null заполнить кого ждем
    public abstract Integer getIncrementType(Collection<Property> changedProps, Set<Property> toWait);

    public class Change {
        int Type; // && 0 - =, 1 - +, 2 - и новое и предыдущее
        JoinQuery<T, PropertyField> source;
        public ValueClassSet<T> classes;

        Change(int iType, JoinQuery<T, PropertyField> iSource, ValueClassSet<T> iClasses) {
            Type = iType;
            source = iSource;
            classes = iClasses;
        }

        // подгоняет к Type'у
        public void correct(int RequiredType) {
            // проверим что вернули что вернули то что надо, "подчищаем" если не то
            // если вернул 2 запишем
            if(Type==2 || (Type!=RequiredType))
                RequiredType = 2;

            if(Type != RequiredType) {
                JoinQuery<T, PropertyField> NewQuery = new JoinQuery<T, PropertyField>(interfaces);
                Join<T, PropertyField> ChangeJoin = new Join<T, PropertyField>(source, NewQuery);
                SourceExpr NewExpr = ChangeJoin.exprs.get(changeTable.value);
                NewQuery.and(ChangeJoin.inJoin);
                // нужно LEFT JOIN'ить старые
                SourceExpr PrevExpr;
                // если пересекаются изменения со старым
                if(isInInterface(classes.getClassSet(ClassSet.universal)))
                    PrevExpr = getSourceExpr(NewQuery.mapKeys, classes.getClassSet(ClassSet.universal));
                else
                    PrevExpr = getType().getExpr(null);
                // по любому 2 нету надо докинуть
                NewQuery.properties.put(changeTable.prevValue, PrevExpr);
                if(Type==1) // есть 1, а надо по сути 0
                    NewExpr = new LinearExpr(NewExpr,PrevExpr,true);
                NewQuery.properties.put(changeTable.value, NewExpr);

                source = NewQuery;
                Type = RequiredType;
            }
        }

        public void out(DataSession session) throws SQLException {
            System.out.println(caption);
            source.outSelect(session);
            System.out.println(classes);
        }

        // сохраняет в инкрементную таблицу
        public void save(DataSession session) throws SQLException {

            Map<KeyField,Integer> valueKeys = new HashMap<KeyField,Integer>();
            valueKeys.put(changeTable.property,ID);
            session.deleteKeyRecords(changeTable,valueKeys);

            // откуда читать
            JoinQuery<T, PropertyField> readQuery = new JoinQuery<T, PropertyField>(interfaces);
            Join<KeyField, PropertyField> readJoin = new Join<KeyField, PropertyField>(changeTable,readQuery, changeTableMap);
            readJoin.joins.put(changeTable.property,changeTable.property.type.getExpr(ID));
            readQuery.and(readJoin.inJoin);

            JoinQuery<KeyField, PropertyField> writeQuery = new JoinQuery<KeyField, PropertyField>(changeTable.keys);
            Join<T, PropertyField> writeJoin = new Join<T, PropertyField>(source, changeTableMap,writeQuery);
            writeQuery.putKeyWhere(valueKeys);
            writeQuery.and(writeJoin.inJoin);

            writeQuery.properties.put(changeTable.value, writeJoin.exprs.get(changeTable.value));
            readQuery.properties.put(changeTable.value, readJoin.exprs.get(changeTable.value));
            if(Type==2) {
                writeQuery.properties.put(changeTable.prevValue, writeJoin.exprs.get(changeTable.prevValue));
                readQuery.properties.put(changeTable.prevValue, readJoin.exprs.get(changeTable.prevValue));
            }

//            if(caption.equals("Цена розн. (до)"))
//                System.out.println(caption);
            session.insertSelect(new ModifyQuery(changeTable,writeQuery));

            source = readQuery;
        }

        // сохраняет в базу
        public void apply(DataSession session) throws SQLException {

            JoinQuery<KeyField, PropertyField> modifyQuery = new JoinQuery<KeyField, PropertyField>(mapTable.table.keys);

            Join<T, PropertyField> update = new Join<T, PropertyField>(source,modifyQuery,mapTable.mapKeys);
            modifyQuery.and(update.inJoin);
            modifyQuery.properties.put(field, update.exprs.get(changeTable.value));
            session.modifyRecords(new ModifyQuery(mapTable.table,modifyQuery));
        }

        // для отладки, проверяет что у объектов заданные классы

        // связывает именно измененные записи из сессии
        // Value - что получать, 0 - новые значения, 1 - +(увеличение), 2 - старые значения
        public JoinExpr getExpr(Map<T,? extends SourceExpr> JoinImplement, int Value) {

            // теперь определимся что возвращать
            if(Value==2 && Type==2)
                return new Join<T, PropertyField>(source,JoinImplement).exprs.get(changeTable.prevValue);

            if(Value==Type || (Value==0 && Type==2))
                return new Join<T, PropertyField>(source,JoinImplement).exprs.get(changeTable.value);

            if(Value==1 && Type==2) {
                JoinQuery<T, PropertyField> DiffQuery = new JoinQuery<T, PropertyField>(interfaces);
                Join<T, PropertyField> ChangeJoin = new Join<T, PropertyField>(source,DiffQuery);
                DiffQuery.properties.put(changeTable.value, new LinearExpr(ChangeJoin.exprs.get(changeTable.value),
                                        ChangeJoin.exprs.get(changeTable.prevValue),false));
                DiffQuery.and(ChangeJoin.inJoin);
                return new Join<T, PropertyField>(DiffQuery,JoinImplement).exprs.get(changeTable.value);
            }

            throw new RuntimeException("Тип измененного значения не найден");
        }
    }
}
