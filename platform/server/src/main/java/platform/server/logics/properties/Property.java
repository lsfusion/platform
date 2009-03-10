package platform.server.logics.properties;

import platform.base.BaseUtils;
import platform.server.data.KeyField;
import platform.server.data.ModifyQuery;
import platform.server.data.PropertyField;
import platform.server.data.Table;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.LinearExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.logics.ObjectValue;
import platform.server.logics.BusinessLogics;
import platform.server.logics.auth.ChangePropertySecurityPolicy;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.*;
import platform.server.logics.data.TableFactory;
import platform.server.logics.properties.groups.AbstractNode;
import platform.server.logics.session.*;

import java.sql.SQLException;
import java.util.*;

abstract public class Property<T extends PropertyInterface> extends AbstractNode implements PropertyClass<T> {

    public int ID=0;
    // символьный идентификатор, с таким именем создаются поля в базе и передаются в PropertyView
    public String sID;
    public String getSID() {
        if (sID != null) return sID; else return "prop" + ID;
    }

    TableFactory tableFactory;

    Property(TableFactory iTableFactory) {
        tableFactory = iTableFactory;
    }

    // чтобы подчеркнуть что не направленный
    public Collection<T> interfaces = new ArrayList<T>();

    // закэшируем чтобы быстрее работать
    // здесь как и в произвольных Left значит что могут быть null, не Left соответственно только не null
    // (пока в нашем случае просто можно убирать записи где точно null)
    public SourceExpr getSourceExpr(Map<T,? extends SourceExpr> joinImplement, InterfaceClassSet<T> joinClasses) {

        if(isPersistent()) {
            // если persistent читаем из таблицы
            Map<KeyField,T> mapJoins = new HashMap<KeyField,T>();
            Table sourceTable = getTable(mapJoins);

            // прогоним проверим все ли Implement'ировано
            return new Join<KeyField, PropertyField>(sourceTable, BaseUtils.join(mapJoins,joinImplement)).exprs.get(field);
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

    Map<InterfaceClass<T>,ClassSet> CacheValueClass = new HashMap<InterfaceClass<T>, ClassSet>();
    abstract ClassSet calculateValueClass(InterfaceClass<T> interfaceImplement);
    public ClassSet getValueClass(InterfaceClass<T> interfaceImplement) {
        if(!BusinessLogics.activateCaches) return calculateValueClass(interfaceImplement);
        ClassSet Result = CacheValueClass.get(interfaceImplement);
        if(Result==null) {
            Result = calculateValueClass(interfaceImplement);
            CacheValueClass.put(interfaceImplement,Result);
        }
        return Result;
    }

    Map<ClassSet, InterfaceClassSet<T>> CacheClassSet = new HashMap<ClassSet, InterfaceClassSet<T>>();
    abstract InterfaceClassSet<T> calculateClassSet(ClassSet reqValue);
    public InterfaceClassSet<T> getClassSet(ClassSet reqValue) {
        if(!BusinessLogics.activateCaches) return calculateClassSet(reqValue);
        InterfaceClassSet<T> Result = CacheClassSet.get(reqValue);
        if(Result==null) {
            Result = calculateClassSet(reqValue);
            CacheClassSet.put(reqValue,Result);
        }
        return Result;
    }

    ValueClassSet<T> CacheValueClassSet = null;
    abstract ValueClassSet<T> calculateValueClassSet();
    public ValueClassSet<T> getValueClassSet() {
        if(!BusinessLogics.activateCaches) return calculateValueClassSet();
        if(CacheValueClassSet ==null) CacheValueClassSet = calculateValueClassSet();
        return CacheValueClassSet;
    }

    // заполняет список, возвращает есть ли изменения
    public abstract boolean fillChangedList(List<Property> changedProperties, DataChanges changes, Collection<Property> noUpdate);

    JoinQuery<T,String> getOutSelect(String Value) {
        JoinQuery<T,String> Query = new JoinQuery<T,String>(interfaces);
        SourceExpr ValueExpr = getSourceExpr(Query.mapKeys,getClassSet(ClassSet.universal));
        Query.properties.put(Value, ValueExpr);
        Query.and(ValueExpr.getWhere());
        return Query;
    }

    public void Out(DataSession Session) throws SQLException {
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

    void setChangeType(Map<Property, Integer> RequiredTypes,int ChangeType) {
        // 0 и 0 = 0
        // 0 и 1 = 2
        // 1 и 1 = 1
        // 2 и x = 2

        // значит не изменилось (тогда не надо)
        if(!RequiredTypes.containsKey(this)) return;

        Integer PrevType = RequiredTypes.get(this);
        if(PrevType!=null && !PrevType.equals(ChangeType)) ChangeType = 2;
        RequiredTypes.put(this,ChangeType);
    }

    // строится по сути "временный" Map PropertyInterface'ов на Objects'ы
    Map<T, KeyField> changeTableMap = null;
    // раз уж ChangeTableMap закэшировали то и ChangeTable тоже
    public IncrementChangeTable changeTable;

    public void fillChangeTable() {
        changeTable = tableFactory.getChangeTable(interfaces.size(), getType());
        changeTableMap = new HashMap<T, KeyField>();
        Iterator<KeyField> io = changeTable.objects.iterator();
        for(T anInterface : interfaces)
            changeTableMap.put(anInterface,io.next());
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
    public abstract Table getTable(Map<KeyField,T> MapJoins);

    public boolean isPersistent() {
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

        public void out(DataSession Session) throws SQLException {
            System.out.println(caption);
            source.outSelect(Session);
            System.out.println(classes);
        }

        // сохраняет в инкрементную таблицу
        public void save(DataSession Session) throws SQLException {

            Map<KeyField,Integer> ValueKeys = new HashMap<KeyField,Integer>();
            ValueKeys.put(changeTable.property,ID);
            Session.deleteKeyRecords(changeTable,ValueKeys);

            // откуда читать
            JoinQuery<T, PropertyField> ReadQuery = new JoinQuery<T, PropertyField>(interfaces);
            Join<KeyField, PropertyField> ReadJoin = new Join<KeyField, PropertyField>(changeTable,ReadQuery, changeTableMap);
            ReadJoin.joins.put(changeTable.property,changeTable.property.type.getExpr(ID));
            ReadQuery.and(ReadJoin.inJoin);

            JoinQuery<KeyField, PropertyField> WriteQuery = new JoinQuery<KeyField, PropertyField>(changeTable.keys);
            Join<T, PropertyField> WriteJoin = new Join<T, PropertyField>(source, changeTableMap,WriteQuery);
            WriteQuery.putKeyWhere(ValueKeys);
            WriteQuery.and(WriteJoin.inJoin);

            WriteQuery.properties.put(changeTable.value, WriteJoin.exprs.get(changeTable.value));
            ReadQuery.properties.put(changeTable.value, ReadJoin.exprs.get(changeTable.value));
            if(Type==2) {
                WriteQuery.properties.put(changeTable.prevValue, WriteJoin.exprs.get(changeTable.prevValue));
                ReadQuery.properties.put(changeTable.prevValue, ReadJoin.exprs.get(changeTable.prevValue));
            }

//            if(caption.equals("Цена розн. (до)"))
//                System.out.println(caption);
            Session.insertSelect(new ModifyQuery(changeTable,WriteQuery));

            source = ReadQuery;
        }

        // сохраняет в базу
        public void apply(DataSession session) throws SQLException {

            Map<KeyField,T> mapKeys = new HashMap<KeyField,T>();
            Table sourceTable = getTable(mapKeys);

            JoinQuery<KeyField, PropertyField> modifyQuery = new JoinQuery<KeyField, PropertyField>(sourceTable.keys);

            Join<T, PropertyField> update = new Join<T, PropertyField>(source,modifyQuery,mapKeys);
            modifyQuery.and(update.inJoin);
            modifyQuery.properties.put(field, update.exprs.get(changeTable.value));
            session.modifyRecords(new ModifyQuery(sourceTable,modifyQuery));
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
