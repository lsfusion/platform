/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.util.*;

class ObjectValue {
    Integer object;
    Class objectClass;

    public boolean equals(Object o) {
        return this==o || o instanceof ObjectValue && object.equals(((ObjectValue) o).object);
    }

    public int hashCode() {
        return object.hashCode();
    }

    ObjectValue(Integer iObject,Class iClass) {
        object =iObject;
        objectClass =iClass;}
}

class PropertyImplement<T,P extends PropertyInterface> {

    PropertyImplement(PropertyImplement<T,P> iProperty) {
        property = iProperty.property;
        mapping = new HashMap<P,T>(iProperty.mapping);
    }

    PropertyImplement(Property<P> iProperty) {
        property = iProperty;
        mapping = new HashMap<P,T>();
    }

    Property<P> property;
    Map<P,T> mapping;

    public String toString() {
        return property.toString();
    }
}

interface PropertyInterfaceImplement<P extends PropertyInterface> {

    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> JoinImplement, InterfaceClassSet<P> JoinClasses);
    public ClassSet mapValueClass(InterfaceClass<P> ClassImplement);
    public InterfaceClassSet<P> mapClassSet(ClassSet ReqValue);


    abstract boolean mapFillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate);

    // для increment'ного обновления
    public boolean mapHasChanges(DataSession Session);
    public JoinExpr mapChangeExpr(DataSession Session, Map<P, ? extends SourceExpr> JoinImplement, int Value);
    ClassSet mapChangeValueClass(DataSession Session, InterfaceClass<P> ClassImplement);
    InterfaceClassSet<P> mapChangeClassSet(DataSession Session, ClassSet ReqValue);
}


class PropertyInterface<P extends PropertyInterface<P>> implements PropertyInterfaceImplement<P> {

    int ID = 0;
    PropertyInterface(int iID) {
        ID = iID;
    }

    public String toString() {
        return "I/"+ID;
    }

    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> JoinImplement, InterfaceClassSet<P> JoinClasses) {
        return JoinImplement.get(this);
    }

    public JoinExpr mapChangeExpr(DataSession Session, Map<P, ? extends SourceExpr> JoinImplement, int Value) {
        return null;
    }

    public ClassSet mapValueClass(InterfaceClass<P> ClassImplement) {
        return ClassImplement.get(this);
    }

    public InterfaceClassSet<P> mapClassSet(ClassSet ReqValue) {
        InterfaceClass<P> ResultClass = new InterfaceClass<P>();
        ResultClass.put((P) this,ReqValue);
        return new InterfaceClassSet<P>(ResultClass);
    }

    public boolean mapHasChanges(DataSession Session) {
        return false;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean mapFillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        return false;
    }

    public ClassSet mapChangeValueClass(DataSession Session, InterfaceClass<P> ClassImplement) {
        return mapValueClass(ClassImplement);
    }

    public InterfaceClassSet<P> mapChangeClassSet(DataSession Session, ClassSet ReqValue) {
        return mapClassSet(ReqValue);
    }
}

class AbstractNode {

    AbstractGroup parent;
    AbstractGroup getParent() { return parent; }
}

class AbstractGroup extends AbstractNode {

    String caption;

    AbstractGroup(String icaption) {
        caption = icaption;
    }

    Collection<AbstractNode> children = new ArrayList<AbstractNode>();
    void add(AbstractNode prop) {
        children.add(prop);
        prop.parent = this;
    }

    boolean hasChild(AbstractNode prop) {
        for (AbstractNode child : children) {
            if (child == prop) return true;
            if (child instanceof AbstractGroup && ((AbstractGroup)child).hasChild(prop)) return true;
        }
        return false;
    }

    List<Class> getClasses() {
        List<Class> result = new ArrayList();
        fillClasses(result);
        return result;
    }

    private void fillClasses(List<Class> classes) {
        for (AbstractNode child : children) {
            if (child instanceof AbstractGroup)
                ((AbstractGroup)child).fillClasses(classes);
            if (child instanceof Class)
                classes.add((Class)child);
        }
    }

    List<Property> getProperties() {
        List<Property> result = new ArrayList();
        fillProperties(result);
        return result;
    }

    private void fillProperties(List<Property> properties) {
        for (AbstractNode child : children) {
            if (child instanceof AbstractGroup)
                ((AbstractGroup)child).fillProperties(properties);
            if (child instanceof Property)
                properties.add((Property)child);
        }
    }

}

abstract class ChangeValue {
    Class Class;

    ChangeValue(Class iClass) {
        Class = iClass;
    }
}

class ChangeObjectValue extends ChangeValue {
    Object Value;

    ChangeObjectValue(Class iClass, Object iValue) {
        super(iClass);
        Value = iValue;
    }
}

class ChangeCoeffValue extends ChangeValue {
    Integer Coeff;

    ChangeCoeffValue(Class iClass, Integer iCoeff) {
        super(iClass);
        Coeff = iCoeff;
    }
}

abstract class Property<T extends PropertyInterface> extends AbstractNode implements PropertyClass<T> {

    int ID=0;
    // символьный идентификатор, с таким именем создаются поля в базе и передаются в PropertyView
    String sID;
    public String getSID() {
        if (sID != null) return sID; else return "prop" + ID;
    }

    TableFactory tableFactory;

    Property(TableFactory iTableFactory) {
        tableFactory = iTableFactory;
    }

    // чтобы подчеркнуть что не направленный
    Collection<T> interfaces = new ArrayList<T>();

    // закэшируем чтобы быстрее работать
    // здесь как и в произвольных Left значит что могут быть null, не Left соответственно только не null
    // (пока в нашем случае просто можно убирать записи где точно null)
    public SourceExpr getSourceExpr(Map<T,? extends SourceExpr> joinImplement,InterfaceClassSet<T> joinClasses) {

        if(IsPersistent()) {
            // если persistent читаем из таблицы
            Map<KeyField,T> mapJoins = new HashMap<KeyField,T>();
            Table sourceTable = getTable(mapJoins);

            // прогоним проверим все ли Implement'ировано
            return new Join<KeyField,PropertyField>(sourceTable,BaseUtils.join(mapJoins,joinImplement)).exprs.get(field);
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
    ClassSet getBaseClass() {
        ClassSet ResultClass = new ClassSet();
        for(InterfaceClass<T> InterfaceClass : getClassSet(ClassSet.universal))
            ResultClass.or(getValueClass(InterfaceClass));
        return ResultClass;
    }

    InterfaceClassSet<T> getUniversalInterface() {
        InterfaceClass<T> Result = new InterfaceClass<T>();
        for(T Interface : interfaces)
            Result.put(Interface,ClassSet.universal);
        return new InterfaceClassSet<T>(Result);
    }

    public Type getType() {
        return getBaseClass().getType();
    }

    String caption = "";

    public String toString() {
        return caption;
    }

    Map<InterfaceClass<T>,ClassSet> CacheValueClass = new HashMap<InterfaceClass<T>, ClassSet>();
    abstract ClassSet calculateValueClass(InterfaceClass<T> interfaceImplement);
    public ClassSet getValueClass(InterfaceClass<T> InterfaceImplement) {
        if(!Main.ActivateCaches) return calculateValueClass(InterfaceImplement);
        ClassSet Result = CacheValueClass.get(InterfaceImplement);
        if(Result==null) {
            Result = calculateValueClass(InterfaceImplement);
            CacheValueClass.put(InterfaceImplement,Result);
        }
        return Result;
    }

    Map<ClassSet,InterfaceClassSet<T>> CacheClassSet = new HashMap<ClassSet, InterfaceClassSet<T>>();
    abstract InterfaceClassSet<T> calculateClassSet(ClassSet reqValue);
    public InterfaceClassSet<T> getClassSet(ClassSet ReqValue) {
        if(!Main.ActivateCaches) return calculateClassSet(ReqValue);
        InterfaceClassSet<T> Result = CacheClassSet.get(ReqValue);
        if(Result==null) {
            Result = calculateClassSet(ReqValue);
            CacheClassSet.put(ReqValue,Result);
        }
        return Result;
    }

    ValueClassSet<T> CacheValueClassSet = null;
    abstract ValueClassSet<T> calculateValueClassSet();
    public ValueClassSet<T> getValueClassSet() {
        if(!Main.ActivateCaches) return calculateValueClassSet();
        if(CacheValueClassSet ==null) CacheValueClassSet = calculateValueClassSet();
        return CacheValueClassSet;
    }

    // заполняет список, возвращает есть ли изменения
    abstract boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate);

    JoinQuery<T,String> getOutSelect(String Value) {
        JoinQuery<T,String> Query = new JoinQuery<T,String>(interfaces);
        SourceExpr ValueExpr = getSourceExpr(Query.mapKeys,getClassSet(ClassSet.universal));
        Query.properties.put(Value, ValueExpr);
        Query.and(ValueExpr.getWhere());
        return Query;
    }

    void Out(DataSession Session) throws SQLException {
        System.out.println(caption);
        getOutSelect("value").outSelect(Session);
    }

    boolean isObject() {
        // нужно также проверить
        for(InterfaceClass<T> InterfaceClass : getClassSet(ClassSet.universal))
            for(ClassSet Interface : InterfaceClass.values())
                if(Interface.intersect(ClassSet.getUp(Class.data)))
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
    Map<T,KeyField> changeTableMap = null;
    // раз уж ChangeTableMap закэшировали то и ChangeTable тоже
    IncrementChangeTable changeTable;

    void FillChangeTable() {
        changeTable = tableFactory.GetChangeTable(interfaces.size(), getType());
        changeTableMap = new HashMap<T,KeyField>();
        Iterator<KeyField> io = changeTable.Objects.iterator();
        for(T Interface : interfaces)
            changeTableMap.put(Interface,io.next());
    }

    void OutChangesTable(DataSession Session) throws SQLException {
        JoinQuery<T,PropertyField> Query = new JoinQuery<T,PropertyField>(interfaces);

        Join<KeyField,PropertyField> ChangeJoin = new Join<KeyField,PropertyField>(changeTable,Query, changeTableMap);
        ChangeJoin.joins.put(changeTable.property,changeTable.property.type.getExpr(ID));
        Query.and(ChangeJoin.inJoin);

        Query.properties.put(changeTable.value, ChangeJoin.exprs.get(changeTable.value));
        Query.properties.put(changeTable.prevValue, ChangeJoin.exprs.get(changeTable.prevValue));

        Query.outSelect(Session);
    }

    PropertyField field;
    abstract Table getTable(Map<KeyField,T> MapJoins);

    boolean IsPersistent() {
        return field !=null && !(this instanceof AggregateProperty && tableFactory.reCalculateAggr); // для тестирования 2-е условие
    }

    // базовые методы - ничего не делать, его перегружают только Override и Data
    ChangeValue getChangeProperty(DataSession Session, Map<T, ObjectValue> Keys, int Coeff, ChangePropertySecurityPolicy securityPolicy) { return null;}
    void changeProperty(Map<T, ObjectValue> Keys, Object NewValue, boolean externalID, DataSession Session, ChangePropertySecurityPolicy securityPolicy) throws SQLException {}

    // заполняет требования к изменениям
    abstract void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes);

    // для каскадного выполнения (запрос)
    boolean XL = false;

    // получает запрос для инкрементных изменений
    abstract Change incrementChanges(DataSession session, int changeType) throws SQLException;

    // присоединяют объекты
    void joinChangeClass(ChangeClassTable Table,JoinQuery<DataPropertyInterface,?> Query, DataSession Session,DataPropertyInterface Interface) {
        Join<KeyField,PropertyField> ClassJoin = new Join<KeyField,PropertyField>(Table.getClassJoin(Session,Interface.interfaceClass));
        ClassJoin.joins.put(Table.object,Query.mapKeys.get(Interface));
        Query.and(ClassJoin.inJoin);
    }

    void joinObjects(JoinQuery<DataPropertyInterface,?> Query,DataPropertyInterface Interface) {
        Join<KeyField,PropertyField> ClassJoin = new Join<KeyField,PropertyField>(tableFactory.objectTable.getClassJoin(Interface.interfaceClass));
        ClassJoin.joins.put(tableFactory.objectTable.key,Query.mapKeys.get(Interface));
        Query.and(ClassJoin.inJoin);
    }

    // тип по умолчанию, если null заполнить кого ждем
    abstract Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait);

    class Change {
        int Type; // && 0 - =, 1 - +, 2 - и новое и предыдущее
        JoinQuery<T,PropertyField> source;
        ValueClassSet<T> classes;

        Change(int iType, JoinQuery<T, PropertyField> iSource, ValueClassSet<T> iClasses) {
            Type = iType;
            source = iSource;
            classes = iClasses;
        }

        // подгоняет к Type'у
        void correct(int RequiredType) {
            // проверим что вернули что вернули то что надо, "подчищаем" если не то
            // если вернул 2 запишем
            if(Type==2 || (Type!=RequiredType))
                RequiredType = 2;

            if(Type != RequiredType) {
                JoinQuery<T,PropertyField> NewQuery = new JoinQuery<T,PropertyField>(interfaces);
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

        void out(DataSession Session) throws SQLException {
            System.out.println(caption);
            source.outSelect(Session);
            System.out.println(classes);
        }

        // сохраняет в инкрементную таблицу
        void save(DataSession Session) throws SQLException {

            Map<KeyField,Integer> ValueKeys = new HashMap<KeyField,Integer>();
            ValueKeys.put(changeTable.property,ID);
            Session.deleteKeyRecords(changeTable,ValueKeys);

            // откуда читать
            JoinQuery<T,PropertyField> ReadQuery = new JoinQuery<T,PropertyField>(interfaces);
            Join<KeyField,PropertyField> ReadJoin = new Join<KeyField,PropertyField>(changeTable,ReadQuery, changeTableMap);
            ReadJoin.joins.put(changeTable.property,changeTable.property.type.getExpr(ID));
            ReadQuery.and(ReadJoin.inJoin);

            JoinQuery<KeyField,PropertyField> WriteQuery = new JoinQuery<KeyField,PropertyField>(changeTable.keys);
            Join<T,PropertyField> WriteJoin = new Join<T,PropertyField>(source, changeTableMap,WriteQuery);
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
            Session.InsertSelect(new ModifyQuery(changeTable,WriteQuery));

            source = ReadQuery;
        }

        // сохраняет в базу
        void apply(DataSession Session) throws SQLException {

            Map<KeyField,T> mapKeys = new HashMap<KeyField,T>();
            Table sourceTable = getTable(mapKeys);

            JoinQuery<KeyField,PropertyField> modifyQuery = new JoinQuery<KeyField,PropertyField>(sourceTable.keys);

            Join<T,PropertyField> update = new Join<T,PropertyField>(source,modifyQuery,mapKeys);
            modifyQuery.and(update.inJoin);
            modifyQuery.properties.put(field, update.exprs.get(changeTable.value));
            Session.modifyRecords(new ModifyQuery(sourceTable,modifyQuery));
        }

        // для отладки, проверяет что у объектов заданные классы

        // связывает именно измененные записи из сессии
        // Value - что получать, 0 - новые значения, 1 - +(увеличение), 2 - старые значения
        JoinExpr getExpr(Map<T,? extends SourceExpr> JoinImplement, int Value) {

            // теперь определимся что возвращать
            if(Value==2 && Type==2)
                return new Join<T,PropertyField>(source,JoinImplement).exprs.get(changeTable.prevValue);

            if(Value==Type || (Value==0 && Type==2))
                return new Join<T,PropertyField>(source,JoinImplement).exprs.get(changeTable.value);

            if(Value==1 && Type==2) {
                JoinQuery<T,PropertyField> DiffQuery = new JoinQuery<T,PropertyField>(interfaces);
                Join<T,PropertyField> ChangeJoin = new Join<T,PropertyField>(source,DiffQuery);
                DiffQuery.properties.put(changeTable.value, new LinearExpr(ChangeJoin.exprs.get(changeTable.value),
                                        ChangeJoin.exprs.get(changeTable.prevValue),false));
                DiffQuery.and(ChangeJoin.inJoin);
                return new Join<T,PropertyField>(DiffQuery,JoinImplement).exprs.get(changeTable.value);
            }

            throw new RuntimeException("Тип измененного значения не найден");
        }
    }
}

class DataPropertyInterface extends PropertyInterface<DataPropertyInterface> {
    Class interfaceClass;

    DataPropertyInterface(int iID,Class iClass) {
        super(iID);
        interfaceClass = iClass;
    }
}


class DataProperty<D extends PropertyInterface> extends Property<DataPropertyInterface> {
    Class Value;

    DataProperty(TableFactory iTableFactory,Class iValue) {
        super(iTableFactory);
        Value = iValue;

        defaultMap = new HashMap<DataPropertyInterface,D>();
    }

    // при текущей реализации проше предполагать что не имплементнутые Interface имеют null Select !!!!!
    Table getTable(Map<KeyField,DataPropertyInterface> MapJoins) {
        return tableFactory.getTable(interfaces,MapJoins);
    }

    public ClassSet calculateValueClass(InterfaceClass<DataPropertyInterface> ClassImplement) {
        // пока так потом сделаем перегрузку по классам
        // если не тот класс сразу зарубаем
       for(DataPropertyInterface DataInterface : interfaces)
            if(!ClassImplement.get(DataInterface).intersect(ClassSet.getUp(DataInterface.interfaceClass)))
                return new ClassSet();

        return ClassSet.getUp(Value);
    }

    public InterfaceClassSet<DataPropertyInterface> calculateClassSet(ClassSet reqValue) {
        if(reqValue.intersect(ClassSet.getUp(Value))) {
            InterfaceClass<DataPropertyInterface> ResultInterface = new InterfaceClass<DataPropertyInterface>();
            for(DataPropertyInterface Interface : interfaces)
                ResultInterface.put(Interface,ClassSet.getUp(Interface.interfaceClass));
            return new InterfaceClassSet<DataPropertyInterface>(ResultInterface);
        } else
            return new InterfaceClassSet<DataPropertyInterface>();
    }

    public ValueClassSet<DataPropertyInterface> calculateValueClassSet() {
        return new ValueClassSet<DataPropertyInterface>(ClassSet.getUp(Value),getClassSet(ClassSet.universal));
    }

    // свойства для "ручных" изменений пользователем
    DataChangeTable dataTable;
    Map<KeyField,DataPropertyInterface> dataTableMap = null;

    void FillDataTable() {
        dataTable = tableFactory.GetDataChangeTable(interfaces.size(), getType());
        // если нету Map'a построим
        dataTableMap = new HashMap<KeyField,DataPropertyInterface>();
        Iterator<KeyField> io = dataTable.Objects.iterator();
        for(DataPropertyInterface Interface : interfaces)
            dataTableMap.put(io.next(),Interface);
    }

    void outDataChangesTable(DataSession Session) throws SQLException {
        dataTable.outSelect(Session);
    }

    ChangeValue getChangeProperty(DataSession Session, Map<DataPropertyInterface, ObjectValue> Keys, int Coeff, ChangePropertySecurityPolicy securityPolicy) {

        if(!getValueClass(new InterfaceClass<DataPropertyInterface>(Keys)).isEmpty() && (securityPolicy == null || securityPolicy.checkPermission(this))) {
            if(Coeff==0 && Session!=null) {
                Object ReadValue = null;
                try {
                    ReadValue = Session.readProperty(this,Keys);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return new ChangeObjectValue(Value,ReadValue);
            } else
                return new ChangeCoeffValue(Value,Coeff);
        } else
            return null;
    }

    void changeProperty(Map<DataPropertyInterface, ObjectValue> Keys, Object NewValue, boolean externalID, DataSession Session, ChangePropertySecurityPolicy securityPolicy) throws SQLException {
        // записываем в таблицу изменений
        if (securityPolicy == null || securityPolicy.checkPermission(this))
            Session.changeProperty(this, Keys, NewValue, externalID);
    }

    // св-во по умолчанию (при ClassSet подставляется)
    Property<D> defaultProperty;
    // map интерфейсов на PropertyInterface
    Map<DataPropertyInterface,D> defaultMap;
    // если нужно еще за изменениями следить и перебивать
    boolean onDefaultChange;

    // заполняет список, возвращает есть ли изменения, последний параметр для рекурсий
    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        if(ChangedProperties.contains(this)) return true;
        if(NoUpdate.contains(this)) return false;
        // если null то значит полный список запрашивают
        if(Changes==null) return true;

        boolean Changed = Changes.properties.contains(this);

        if(!Changed)
            for(DataPropertyInterface Interface : interfaces)
                if(Changes.removeClasses.contains(Interface.interfaceClass)) Changed = true;

        if(!Changed)
            if(Changes.removeClasses.contains(Value)) Changed = true;

        if(defaultProperty !=null) {
            boolean DefaultChanged = defaultProperty.fillChangedList(ChangedProperties, Changes, NoUpdate);
            if(!Changed) {
                if(onDefaultChange)
                    Changed = DefaultChanged;
                else
                    for(DataPropertyInterface Interface : interfaces)
                        if(Changes.addClasses.contains(Interface.interfaceClass)) Changed = true;
            }
        }

        if(Changed) {
            ChangedProperties.add(this);
            return true;
        } else
            return false;
    }

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {

        // если на изм. надо предыдущее изменение иначе просто на =
        // пока неясно после реализации QueryIncrementChanged станет яснее
        if(defaultProperty !=null && RequiredTypes.containsKey(defaultProperty))
            defaultProperty.setChangeType(RequiredTypes, onDefaultChange ?2:0);
    }

    // заполним старыми значениями (LEFT JOIN'ом)
    Change incrementChanges(DataSession session, int changeType) {

        // на 3 то есть слева/направо
        ChangeQuery<DataPropertyInterface,PropertyField> resultQuery = new ChangeQuery<DataPropertyInterface,PropertyField>(interfaces);
        ValueClassSet<DataPropertyInterface> resultClass = new ValueClassSet<DataPropertyInterface>();

        // Default изменения (пока Add)
        if(defaultProperty !=null) {
            if(!onDefaultChange) {
                // бежим по всем добавленным интерфейсам
                for(DataPropertyInterface propertyInterface : interfaces)
                    if(session.changes.addClasses.contains(propertyInterface.interfaceClass)) {
                        JoinQuery<DataPropertyInterface,PropertyField> query = new JoinQuery<DataPropertyInterface,PropertyField>(interfaces);
                        Map<D,SourceExpr> joinImplement = new HashMap<D,SourceExpr>();
                        // "перекодируем" в базовый интерфейс
                        for(DataPropertyInterface dataInterface : interfaces)
                            joinImplement.put(defaultMap.get(dataInterface),query.mapKeys.get(dataInterface));

                        // вкидываем "новое" состояние DefaultProperty с Join'ое с AddClassTable
                        // если DefaultProperty требует на входе такой добавляемый интерфейс то можно чисто новое брать
                        joinChangeClass(tableFactory.addClassTable,query,session,propertyInterface);

                        ValueClassSet<D> defaultValueSet = session.getSourceClass(defaultProperty).and(new ChangeClass<D>(defaultMap.get(propertyInterface),session.addChanges.get(propertyInterface.interfaceClass)));
                        SourceExpr defaultExpr = session.getSourceExpr(defaultProperty, joinImplement, defaultValueSet.getClassSet(ClassSet.universal));
                        query.properties.put(changeTable.value, defaultExpr);
                        query.and(defaultExpr.getWhere());

                        resultQuery.add(query);
                        resultClass.or(defaultValueSet.mapBack(defaultMap));
                    }
            } else {
                if(session.propertyChanges.containsKey(defaultProperty)) {
                    Property<D>.Change defaultChange = session.getChange(defaultProperty);
                    JoinQuery<DataPropertyInterface,PropertyField> query = new JoinQuery<DataPropertyInterface,PropertyField>(interfaces);

                    Map<D,SourceExpr> joinImplement = new HashMap<D,SourceExpr>();
                    // "перекодируем" в базовый интерфейс
                    for(DataPropertyInterface dataInterface : interfaces)
                        joinImplement.put(defaultMap.get(dataInterface),query.mapKeys.get(dataInterface));

                    // по изменению св-ва
                    JoinExpr newExpr = defaultChange.getExpr(joinImplement,0);
                    query.properties.put(changeTable.value, newExpr);
                    // new, не равно prev
                    query.and(newExpr.from.inJoin);
                    query.and(new CompareWhere(newExpr,defaultChange.getExpr(joinImplement,2),CompareWhere.EQUALS).not());

                    resultQuery.add(query);
                    resultClass.or(defaultChange.classes.mapBack(defaultMap));
                }
            }
        }

        boolean dataChanged = session.changes.properties.contains(this);
        JoinQuery<DataPropertyInterface,PropertyField> dataQuery = null;
        SourceExpr dataExpr = null;
        if(dataChanged) {
            dataQuery = new JoinQuery<DataPropertyInterface,PropertyField>(interfaces);
            // GetChangedFrom
            Join<KeyField,PropertyField> dataJoin = new Join<KeyField,PropertyField>(dataTable, dataTableMap,dataQuery);
            dataJoin.joins.put(dataTable.property,dataTable.property.type.getExpr(ID));
            dataQuery.and(dataJoin.inJoin);

            dataExpr = dataJoin.exprs.get(dataTable.value);
            dataQuery.properties.put(changeTable.value, dataExpr);
            resultClass.or(session.dataChanges.get(this));
        }

        for(DataPropertyInterface removeInterface : interfaces) {
            if(session.changes.removeClasses.contains(removeInterface.interfaceClass)) {
                // те изменения которые были на удаляемые объекты исключаем
                if(dataChanged) tableFactory.removeClassTable.excludeJoin(dataQuery,session,removeInterface.interfaceClass,dataQuery.mapKeys.get(removeInterface));

                // проверяем может кто удалился из интерфейса объекта
                JoinQuery<DataPropertyInterface,PropertyField> query = new JoinQuery<DataPropertyInterface,PropertyField>(interfaces);
                joinChangeClass(tableFactory.removeClassTable,query,session,removeInterface);

                InterfaceClassSet<DataPropertyInterface> removeClassSet = getClassSet(ClassSet.universal).and(new InterfaceClass<DataPropertyInterface>(removeInterface,session.removeChanges.get(removeInterface.interfaceClass)));
                // пока сделаем что наплевать на старое значение хотя конечно 2 раза может тоже не имеет смысл считать
                query.properties.put(changeTable.value, changeTable.value.type.getExpr(null));
                query.and(getSourceExpr(query.mapKeys,removeClassSet).getWhere());

                resultQuery.add(query);
                resultClass.or(new ValueClassSet<DataPropertyInterface>(new ClassSet(),removeClassSet));
            }
        }

        if(session.changes.removeClasses.contains(Value)) {
            // те изменения которые были на удаляемые объекты исключаем
            if(dataChanged) tableFactory.removeClassTable.excludeJoin(dataQuery,session,Value,dataExpr);

            JoinQuery<DataPropertyInterface,PropertyField> query = new JoinQuery<DataPropertyInterface,PropertyField>(interfaces);
            Join<KeyField,PropertyField> removeJoin = new Join<KeyField,PropertyField>(tableFactory.removeClassTable.getClassJoin(session,Value));

            InterfaceClassSet<DataPropertyInterface> removeClassSet = getClassSet(session.removeChanges.get(Value));
            removeJoin.joins.put(tableFactory.removeClassTable.object,getSourceExpr(query.mapKeys,removeClassSet));
            query.and(removeJoin.inJoin);
            query.properties.put(changeTable.value, changeTable.value.type.getExpr(null));

            resultQuery.add(query);
            resultClass.or(new ValueClassSet<DataPropertyInterface>(new ClassSet(),removeClassSet));
        }

        // здесь именно в конце так как должна быть последней
        if(dataChanged)
            resultQuery.add(dataQuery);

        return new Change(0,resultQuery,resultClass);
    }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return 0;
    }
}

abstract class AggregateProperty<T extends PropertyInterface> extends Property<T> {

    AggregateProperty(TableFactory iTableFactory) {super(iTableFactory);}

    Map<DataPropertyInterface,T> aggregateMap;

    // расчитывает выражение
    abstract SourceExpr calculateSourceExpr(Map<T,? extends SourceExpr> joinImplement, InterfaceClassSet<T> joinClasses);

    // сначала проверяет на persistence
    Table getTable(Map<KeyField,T> MapJoins) {
        if(aggregateMap ==null) {
            aggregateMap = new HashMap<DataPropertyInterface,T>();
            Map<T,Class> parent = getClassSet(ClassSet.universal).getCommonParent();
            for(T anInterface : interfaces) {
                aggregateMap.put(new DataPropertyInterface(0,parent.get(anInterface)),anInterface);
            }
        }

        Map<KeyField,DataPropertyInterface> mapData = new HashMap<KeyField,DataPropertyInterface>();
        Table sourceTable = tableFactory.getTable(aggregateMap.keySet(),mapData);
        // перекодирукм на MapJoins
        if(MapJoins!=null) {
            for(KeyField MapField : mapData.keySet())
                MapJoins.put(MapField, aggregateMap.get(mapData.get(MapField)));
        }

        return sourceTable;
    }

    Object dropZero(Object Value) {
        if(Value instanceof Integer && Value.equals(0)) return null;
        if(Value instanceof Long && ((Long)Value).intValue()==0) return null;
        if(Value instanceof Double && ((Double)Value).intValue()==0) return null;
        if(Value instanceof Boolean && !((Boolean)Value)) return null;
        return Value;
    }

    // проверяет аггрегацию для отладки
    boolean CheckAggregation(DataSession Session,String Caption) throws SQLException {
        JoinQuery<T, String> AggrSelect;
        AggrSelect = getOutSelect("value");
/*        if(caption.equals("Дата 8") || caption.equals("Склад")) {
            System.out.println("AGGR - "+caption);
            AggrSelect.outSelect(Session);
        }*/
        LinkedHashMap<Map<T, Integer>, Map<String, Object>> AggrResult = AggrSelect.executeSelect(Session);
        tableFactory.reCalculateAggr = true;
        AggrSelect = getOutSelect("value");
/*        if(caption.equals("Дата 8") || caption.equals("Склад")) {
            System.out.println("RECALCULATE - "+caption);
            AggrSelect.outSelect(Session);
        }*/
//        if(BusinessLogics.ChangeDBIteration==13 && caption.equals("Посл. дата изм. парам.")) {
//            System.out.println("RECALCULATE - "+caption);
//            AggrSelect.outSelect(Session);
//        }

        LinkedHashMap<Map<T, Integer>, Map<String, Object>> CalcResult = AggrSelect.executeSelect(Session);
        tableFactory.reCalculateAggr = false;

        Iterator<Map.Entry<Map<T,Integer>,Map<String,Object>>> i = AggrResult.entrySet().iterator();
        while(i.hasNext()) {
            Map.Entry<Map<T,Integer>,Map<String,Object>> Row = i.next();
            Map<T, Integer> RowKey = Row.getKey();
            Object RowValue = dropZero(Row.getValue().get("value"));
            Map<String,Object> CalcRow = CalcResult.get(RowKey);
            Object CalcValue = (CalcRow==null?null:dropZero(CalcRow.get("value")));
            if(RowValue==CalcValue || (RowValue!=null && RowValue.equals(CalcValue))) {
                i.remove();
                CalcResult.remove(RowKey);
            }
        }
        // вычистим и отсюда 0
        i = CalcResult.entrySet().iterator();
        while(i.hasNext()) {
            if(dropZero(i.next().getValue().get("value"))==null)
                i.remove();
        }

        if(CalcResult.size()>0 || AggrResult.size()>0) {
            System.out.println("----CheckAggregations "+Caption+"-----");
            System.out.println("----Aggr-----");
            for(Map.Entry<Map<T,Integer>,Map<String,Object>> Row : AggrResult.entrySet())
                System.out.println(Row);
            System.out.println("----Calc-----");
            for(Map.Entry<Map<T,Integer>,Map<String,Object>> Row : CalcResult.entrySet())
                System.out.println(Row);
//
//            ((GroupProperty)this).outIncrementState(Session);
//            Session = Session;
        }

        return true;
    }

    void reCalculateAggregation(DataSession session) throws SQLException {
        PropertyField writeField = field;
        field = null;

        Map<KeyField,T> mapTable = new HashMap<KeyField,T>();
        Table aggrTable = getTable(mapTable);

        JoinQuery<KeyField,PropertyField> writeQuery = new JoinQuery<KeyField,PropertyField>(aggrTable.keys);
        SourceExpr recalculateExpr = getSourceExpr(BaseUtils.join(BaseUtils.reverse(mapTable), writeQuery.mapKeys), getClassSet(ClassSet.universal));
        writeQuery.properties.put(writeField, recalculateExpr);
        writeQuery.and(new Join<KeyField, PropertyField>(aggrTable, writeQuery).inJoin.or(recalculateExpr.getWhere()));
        session.modifyRecords(new ModifyQuery(aggrTable,writeQuery));

        field = writeField;
    }

    List<PropertyMapImplement<PropertyInterface, T>> getImplements(Map<T, ObjectValue> Keys, ChangePropertySecurityPolicy securityPolicy) {
        return new ArrayList<PropertyMapImplement<PropertyInterface, T>>();
    }

    int getCoeff(PropertyMapImplement<?, T> Implement) { return 0; }

    PropertyMapImplement<?,T> getChangeImplement(Map<T, ObjectValue> Keys, ChangePropertySecurityPolicy securityPolicy) {
        List<PropertyMapImplement<PropertyInterface, T>> Implements = getImplements(Keys, securityPolicy);
        for(int i=Implements.size()-1;i>=0;i--)
            if(Implements.get(i).mapGetChangeProperty(null, Keys, 0, securityPolicy)!=null && (securityPolicy == null || securityPolicy.checkPermission(Implements.get(i).property)))
                return Implements.get(i);
        return null;
    }

    ChangeValue getChangeProperty(DataSession Session, Map<T, ObjectValue> Keys, int Coeff, ChangePropertySecurityPolicy securityPolicy) {
        PropertyMapImplement<?,T> Implement = getChangeImplement(Keys, securityPolicy);
        if(Implement==null) return null;
        return Implement.mapGetChangeProperty(Session,Keys,getCoeff(Implement)*Coeff, securityPolicy);
    }

    void changeProperty(Map<T, ObjectValue> Keys, Object NewValue, boolean externalID, DataSession Session, ChangePropertySecurityPolicy securityPolicy) throws SQLException {
        PropertyMapImplement<?,T> operand = getChangeImplement(Keys, securityPolicy);
        if (operand != null)
            operand.mapChangeProperty(Keys, NewValue, externalID, Session, securityPolicy);
    }

}

class ClassProperty extends AggregateProperty<DataPropertyInterface> {

    Class valueClass;
    Object value;

    ClassProperty(TableFactory iTableFactory, Class iValueClass, Object iValue) {
        super(iTableFactory);
        valueClass = iValueClass;
        value = iValue;
    }

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
        // этому св-ву чужого не надо
    }

    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        // если Value null то ничего не интересует
        if(value ==null) return false;
        if(ChangedProperties.contains(this)) return true;
        if(NoUpdate.contains(this)) return false;

        for(DataPropertyInterface ValueInterface : interfaces)
            if(Changes ==null || Changes.addClasses.contains(ValueInterface.interfaceClass) || Changes.removeClasses.contains(ValueInterface.interfaceClass)) {
                ChangedProperties.add(this);
                return true;
            }

        return false;
    }

    Change incrementChanges(DataSession session, int changeType) {

        // работает на = и на + ему собсно пофигу, то есть сразу на 2

        // для любого изменения объекта на NEW можно определить PREV и соответственно
        // Set<Class> пришедшие, Set<Class> ушедшие
        // соответственно алгоритм бежим по всем интерфейсам делаем UnionQuery из SS изменений + старых объектов

        // конечный результат, с ключами и выражением
        ChangeQuery<DataPropertyInterface,PropertyField> resultQuery = new ChangeQuery<DataPropertyInterface,PropertyField>(interfaces);
        ValueClassSet<DataPropertyInterface> resultClass = new ValueClassSet<DataPropertyInterface>();

        List<DataPropertyInterface> removeInterfaces = new ArrayList<DataPropertyInterface>();
        for(DataPropertyInterface valueInterface : interfaces)
            if(session.changes.removeClasses.contains(valueInterface.interfaceClass))
                removeInterfaces.add(valueInterface);

        // для RemoveClass без SS все за Join'им (valueClass пока трогать не будем (так как у значения пока не закладываем механизм изменений))
        for(DataPropertyInterface changedInterface : removeInterfaces) {
            JoinQuery<DataPropertyInterface,PropertyField> query = new JoinQuery<DataPropertyInterface, PropertyField>(interfaces);
            InterfaceClass<DataPropertyInterface> removeClass = new InterfaceClass<DataPropertyInterface>();

            // RemoveClass + остальные из старой таблицы
            joinChangeClass(tableFactory.removeClassTable,query, session,changedInterface);
            removeClass.put(changedInterface, session.removeChanges.get(changedInterface.interfaceClass));
            for(DataPropertyInterface valueInterface : interfaces)
                if(valueInterface!=changedInterface) {
                    joinObjects(query,valueInterface);
                    removeClass.put(valueInterface,ClassSet.getUp(valueInterface.interfaceClass));
                }

            query.properties.put(changeTable.value, changeTable.value.type.getExpr(null));
            query.properties.put(changeTable.prevValue, changeTable.prevValue.type.getExpr(value));

            resultQuery.add(query);
            resultClass.or(new ChangeClass<DataPropertyInterface>(removeClass,new ClassSet()));
        }

        List<DataPropertyInterface> addInterfaces = new ArrayList();
        for(DataPropertyInterface valueInterface : interfaces)
            if(session.changes.addClasses.contains(valueInterface.interfaceClass))
                addInterfaces.add(valueInterface);

        ListIterator<List<DataPropertyInterface>> il = SetBuilder.buildSubSetList(addInterfaces).listIterator();
        // пустое подмн-во не надо (как и в любой инкрементности)
        il.next();
        while(il.hasNext()) {
            List<DataPropertyInterface> changeProps = il.next();

            JoinQuery<DataPropertyInterface,PropertyField> query = new JoinQuery<DataPropertyInterface, PropertyField>(interfaces);
            InterfaceClass<DataPropertyInterface> addClass = new InterfaceClass<DataPropertyInterface>();

            for(DataPropertyInterface valueInterface : interfaces) {
                if(changeProps.contains(valueInterface)) {
                    joinChangeClass(tableFactory.addClassTable,query, session,valueInterface);
                    addClass.put(valueInterface, session.addChanges.get(valueInterface.interfaceClass));
                } else {
                    joinObjects(query,valueInterface);
                    addClass.put(valueInterface,ClassSet.getUp(valueInterface.interfaceClass));

                    // здесь также надо проверить что не из RemoveClasses (то есть LEFT JOIN на null)
                    if(session.changes.removeClasses.contains(valueInterface.interfaceClass))
                        tableFactory.removeClassTable.excludeJoin(query, session,valueInterface.interfaceClass,query.mapKeys.get(valueInterface));
                }
            }

            query.properties.put(changeTable.prevValue, changeTable.prevValue.type.getExpr(null));
            query.properties.put(changeTable.value, changeTable.value.type.getExpr(value));

            resultQuery.add(query);
            resultClass.or(new ChangeClass<DataPropertyInterface>(addClass,new ClassSet(valueClass)));
        }

        return new Change(2,resultQuery,resultClass);
    }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return 0;
    }


    SourceExpr calculateSourceExpr(Map<DataPropertyInterface, ? extends SourceExpr> joinImplement, InterfaceClassSet<DataPropertyInterface> joinClasses) {

        String valueString = "value";

        JoinQuery<DataPropertyInterface,String> query = new JoinQuery<DataPropertyInterface,String>(interfaces);

        if(value ==null) // если Value null закинем false по сути в запрос
            query.and(Where.FALSE);
        else
            for(DataPropertyInterface valueInterface : interfaces)
                joinObjects(query,valueInterface);
        query.properties.put(valueString, valueClass.getType().getExpr(value));

        return (new Join<DataPropertyInterface,String>(query, joinImplement)).exprs.get(valueString);
    }

    public ClassSet calculateValueClass(InterfaceClass<DataPropertyInterface> classImplement) {
        // аналогично DataProperty\только без перегрузки классов
        for(DataPropertyInterface valueInterface : interfaces)
            if(!classImplement.get(valueInterface).intersect(ClassSet.getUp(valueInterface.interfaceClass)))
                return new ClassSet();

        return new ClassSet(valueClass);
    }

    public InterfaceClassSet<DataPropertyInterface> calculateClassSet(ClassSet reqValue) {
        // аналогично DataProperty\только без перегрузки классов
        if(reqValue.contains(valueClass)) {
            InterfaceClass<DataPropertyInterface> resultInterface = new InterfaceClass<DataPropertyInterface>();
            for(DataPropertyInterface valueInterface : interfaces)
                resultInterface.put(valueInterface,ClassSet.getUp(valueInterface.interfaceClass));
            return new InterfaceClassSet<DataPropertyInterface>(resultInterface);
        } else
            return new InterfaceClassSet<DataPropertyInterface>();
    }

    public ValueClassSet<DataPropertyInterface> calculateValueClassSet() {
        return new ValueClassSet<DataPropertyInterface>(new ClassSet(valueClass),getClassSet(ClassSet.universal));
    }
}

class PropertyMapImplement<T extends PropertyInterface,P extends PropertyInterface> extends PropertyImplement<P,T> implements PropertyInterfaceImplement<P> {

    PropertyMapImplement(Property<T> iProperty) {super(iProperty);}

    // NotNull только если сессии нету
    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> JoinImplement, InterfaceClassSet<P> JoinClasses) {
        return property.getSourceExpr(getMapImplement(JoinImplement),JoinClasses.mapBack(mapping));
    }

    public JoinExpr mapChangeExpr(DataSession Session, Map<P, ? extends SourceExpr> JoinImplement, int Value) {
        return Session.getChange(property).getExpr(getMapImplement(JoinImplement),Value);
    }

    private <V> Map<T, V> getMapImplement(Map<P, V> JoinImplement) {
        Map<T,V> MapImplement = new HashMap<T,V>();
        for(T ImplementInterface : property.interfaces)
            MapImplement.put(ImplementInterface,JoinImplement.get(mapping.get(ImplementInterface)));
        return MapImplement;
    }

    public ClassSet mapValueClass(InterfaceClass<P> ClassImplement) {
        return property.getValueClass(ClassImplement.mapBack(mapping));
    }

    public boolean mapIsInInterface(InterfaceClassSet<P> ClassImplement) {
        return property.isInInterface(ClassImplement.mapBack(mapping));
    }

    public InterfaceClassSet<P> mapClassSet(ClassSet ReqValue) {
        return property.getClassSet(ReqValue).map(mapping);
    }

    public ValueClassSet<P> mapValueClassSet() {
        return property.getValueClassSet().map(mapping);
    }

    public boolean mapHasChanges(DataSession Session) {
        return Session.getChange(property)!=null;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean mapFillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        return property.fillChangedList(ChangedProperties, Changes, NoUpdate);
    }

    ChangeValue mapGetChangeProperty(DataSession Session, Map<P, ObjectValue> Keys, int Coeff, ChangePropertySecurityPolicy securityPolicy) {
        return property.getChangeProperty(Session,getMapImplement(Keys), Coeff, securityPolicy);
    }

    // для OverrideList'а по сути
    void mapChangeProperty(Map<P, ObjectValue> Keys, Object NewValue, boolean externalID, DataSession Session, ChangePropertySecurityPolicy securityPolicy) throws SQLException {
        property.changeProperty(getMapImplement(Keys), NewValue, externalID, Session, securityPolicy);
    }

    public ClassSet mapChangeValueClass(DataSession Session, InterfaceClass<P> ClassImplement) {
        return Session.getChange(property).classes.getValueClass(ClassImplement.mapBack(mapping));
    }

    public InterfaceClassSet<P> mapChangeClassSet(DataSession Session, ClassSet ReqValue) {
        return Session.getChange(property).classes.getClassSet(ReqValue).map(mapping);
    }

    public ValueClassSet<P> mapValueClassSet(DataSession Session) {
        return Session.getChange(property).classes.getValueClassSet().map(mapping);
    }
}

// для четкости пусть будет
class JoinPropertyInterface extends PropertyInterface<JoinPropertyInterface> {
    JoinPropertyInterface(int iID) {
        super(iID);
    }
}

class JoinProperty<T extends PropertyInterface> extends MapProperty<JoinPropertyInterface,T,JoinPropertyInterface,T,PropertyField> {
    PropertyImplement<PropertyInterfaceImplement<JoinPropertyInterface>,T> implementations;

    JoinProperty(TableFactory iTableFactory, Property<T> iProperty) {
        super(iTableFactory);
        implementations = new PropertyImplement<PropertyInterfaceImplement<JoinPropertyInterface>,T>(iProperty);
    }

    InterfaceClassSet<JoinPropertyInterface> getMapClassSet(MapRead<JoinPropertyInterface> Read, InterfaceClass<T> InterfaceImplement) {
        InterfaceClassSet<JoinPropertyInterface> Result = getUniversalInterface();
        for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> MapInterface : implementations.mapping.entrySet())
           Result = Result.and(Read.getImplementClassSet(MapInterface.getValue(),InterfaceImplement.get(MapInterface.getKey())));
        return Result;
    }

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {

        // если только основное - Property ->I - как было (если изменилось только 2 то его и вкинем), возвр. I
        // иначе (не (основное MultiplyProperty и 1)) - Property, Implements ->0 - как было, возвр. 0 - (на подчищение - если (1 или 2) то Left Join'им старые значения)
        // иначе (основное MultiplyProperty и 1) - Implements ->1 - как было (но с другим оператором), возвр. 1

        if(!containsImplement(RequiredTypes.keySet())) {
            implementations.property.setChangeType(RequiredTypes,IncrementType);
        } else {
            int ReqType = (implementAllInterfaces() && IncrementType.equals(0)?0:2);

            implementations.property.setChangeType(RequiredTypes,ReqType);
            for(PropertyInterfaceImplement Interface : implementations.mapping.values())
                if(Interface instanceof PropertyMapImplement)
                    (((PropertyMapImplement)Interface).property).setChangeType(RequiredTypes,(implementations.property instanceof MultiplyFormulaProperty && IncrementType.equals(1)?1:ReqType));
        }
    }

    // инкрементные св-ва
    Change incrementChanges(DataSession session, int changeType) {

        // алгоритм такой - для всех map св-в (в которых были изменения) строим подмножества изм. св-в
        // далее реализации этих св-в "замещаем" (то есть при JOIN будем подставлять), с остальными св-вами делаем LEFT JOIN на IS NULL
        // и JOIN'им с основным св-вом делая туда FULL JOIN новых значений
        // или UNION или большой FULL JOIN в нужном порядке (и не делать LEFT JOIN на IS NULL) и там сделать большой NVL

        ChangeQuery<JoinPropertyInterface,PropertyField> resultQuery = new ChangeQuery<JoinPropertyInterface,PropertyField>(interfaces); // по умолчанию на KEYNULL (но если Multiply то 1 на сумму)
        ValueClassSet<JoinPropertyInterface> resultClass = new ValueClassSet<JoinPropertyInterface>();

        int QueryIncrementType = changeType;
        if(implementations.property instanceof MultiplyFormulaProperty && changeType ==1)
            resultQuery.add(getMapQuery(getChangeImplements(session,1), changeTable.value,resultClass,true));
        else {
            // если нужна 1 и изменились св-ва то придется просто 2-ку считать (хотя это потом можно поменять)
            if(QueryIncrementType==1 && containsImplement(session.propertyChanges.keySet()))
                QueryIncrementType = 2;

            // все на Value - PrevValue не интересует, его как раз верхний подгоняет
            resultQuery.add(getMapQuery(getChange(session,QueryIncrementType==1?1:0), changeTable.value,resultClass,false));
            if(QueryIncrementType==2) resultQuery.add(getMapQuery(getPreviousChange(session), changeTable.prevValue,new ValueClassSet<JoinPropertyInterface>(),false));
        }

        return new Change(QueryIncrementType,resultQuery,resultClass);
    }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        if(!containsImplement(ChangedProps)) {
            ToWait.add(implementations.property);
            return null;
        } else
        if(implementations.property instanceof MultiplyFormulaProperty)
            return 1;
        else
            return 0;
    }

    Property<T> getMapProperty() {
        return implementations.property;
    }

    Map<T, PropertyInterfaceImplement<JoinPropertyInterface>> getMapImplements() {
        return implementations.mapping;
    }

    Collection<JoinPropertyInterface> getMapInterfaces() {
        return interfaces;
    }

    // по сути для формулы выделяем
    ValueClassSet<JoinPropertyInterface> getReadValueClassSet(MapRead<JoinPropertyInterface> Read,InterfaceClassSet<T> MapClasses) {
        ValueClassSet<JoinPropertyInterface> Result = new ValueClassSet<JoinPropertyInterface>();
        
        if(implementations.property instanceof ObjectFormulaProperty) {
            ObjectFormulaProperty ObjectProperty = (ObjectFormulaProperty) implementations.property;
            // сначала кидаем на baseClass, bit'ы
            for(InterfaceClass<JoinPropertyInterface> ValueInterface : getMapClassSet(Read, (InterfaceClass<T>) ObjectProperty.getInterfaceClass(ClassSet.getUp(Class.base)))) {
                InterfaceClass<T> ImplementInterface = new InterfaceClass<T>();
                for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> MapInterface : implementations.mapping.entrySet()) // если null то уже не подходит по интерфейсу
                    ImplementInterface.put(MapInterface.getKey(), MapInterface.getValue().mapValueClass(ValueInterface));

                if(!ImplementInterface.isEmpty()) {
                    Result.or(new ChangeClass<JoinPropertyInterface>(ValueInterface, implementations.property.getValueClass(ImplementInterface)));
                    MapClasses.or(ImplementInterface);
                }
            }
        } else {
            for(ChangeClass<T> ImplementChange : Read.getMapChangeClass(implementations.property))
                for(InterfaceClass<T> ImplementInterface : ImplementChange.interfaceClasses) {
                    InterfaceClassSet<JoinPropertyInterface> ResultInterface = getMapClassSet(Read, ImplementInterface);
                    if(!ResultInterface.isEmpty()) {
                        Result.or(new ChangeClass<JoinPropertyInterface>(ResultInterface,ImplementChange.value));
                        MapClasses.or(ImplementInterface);
                    }
                }
        }

        return Result;
    }

    void fillChangedRead(UnionQuery<JoinPropertyInterface, PropertyField> listQuery, PropertyField value, MapChangedRead<JoinPropertyInterface> read, ValueClassSet<JoinPropertyInterface> readClasses) {
        // создается JoinQuery - на вход getMapInterfaces, Query.MapKeys - map интерфейсов
        InterfaceClassSet<T> MapClasses = new InterfaceClassSet<T>();
        ValueClassSet<JoinPropertyInterface> result = getReadValueClassSet(read, MapClasses);
        if(result.isEmpty()) return;

        JoinQuery<JoinPropertyInterface,PropertyField> query = new JoinQuery<JoinPropertyInterface,PropertyField>(interfaces);

        // далее создается для getMapImplements - map <ImplementClass,SourceExpr> имплементаций - по getExpr'ы (Changed,SourceExpr) с переданным map интерфейсов
        Map<T,SourceExpr> implementSources = new HashMap<T,SourceExpr>();
        Map<PropertyInterfaceImplement<JoinPropertyInterface>,SourceExpr> implementExprs = new HashMap<PropertyInterfaceImplement<JoinPropertyInterface>, SourceExpr>();
        for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> implement : implementations.mapping.entrySet()) {
            SourceExpr implementExpr = read.getImplementExpr(implement.getValue(), query.mapKeys, result.getClassSet(ClassSet.universal));
            implementSources.put(implement.getKey(), implementExpr);
            implementExprs.put(implement.getValue(), implementExpr);
        }
        read.fillMapExpr(query, value, implementations.property,implementSources,implementExprs,MapClasses);

        readClasses.or(result);
        listQuery.add(query);
    }

    public ClassSet calculateValueClass(InterfaceClass<JoinPropertyInterface> ClassImplement) {
        return getValueClassSet().getValueClass(ClassImplement);
    }

    public InterfaceClassSet<JoinPropertyInterface> calculateClassSet(ClassSet reqValue) {
        InterfaceClassSet<JoinPropertyInterface> Result = new InterfaceClassSet<JoinPropertyInterface>();
        for(InterfaceClass<T> ImplementClass : implementations.property.getClassSet(reqValue))
            Result.or(getMapClassSet(new MapRead<JoinPropertyInterface>(),ImplementClass));
        return Result;
    }

    public ValueClassSet<JoinPropertyInterface> calculateValueClassSet() {
        return getReadValueClassSet(DBRead,new InterfaceClassSet<T>());
    }

    Object JoinValue = "jvalue";
    SourceExpr calculateSourceExpr(Map<JoinPropertyInterface,? extends SourceExpr> joinImplement, InterfaceClassSet<JoinPropertyInterface> joinClasses) {

        // именно через Query потому как если не хватит ключей компилятор их подхватит здесь
        JoinQuery<JoinPropertyInterface,Object> Query = new JoinQuery<JoinPropertyInterface,Object>(interfaces);
        // считаем новые SourceExpr'ы и классы
        Map<T,SourceExpr> ImplementSources = new HashMap<T,SourceExpr>();
        for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> Implement : implementations.mapping.entrySet())
            ImplementSources.put(Implement.getKey(),Implement.getValue().mapSourceExpr(Query.mapKeys, joinClasses));

        InterfaceClassSet<T> ImplementClasses = new InterfaceClassSet<T>();
        for(InterfaceClass<JoinPropertyInterface> JoinClass : joinClasses) {
            InterfaceClass<T> ImplementClass = new InterfaceClass<T>();
            for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> Implement : implementations.mapping.entrySet())
                ImplementClass.put(Implement.getKey(),Implement.getValue().mapValueClass(JoinClass));
            ImplementClasses.or(ImplementClass);
        }

        Query.properties.put(JoinValue, implementations.property.getSourceExpr(ImplementSources, ImplementClasses));
        return (new Join<JoinPropertyInterface,Object>(Query, joinImplement)).exprs.get(JoinValue);
    }

    Map<PropertyField, Type> getMapNullProps(PropertyField Value) {
        Map<PropertyField, Type> NullProps = new HashMap<PropertyField, Type>();
        NullProps.put(Value, getType());
        return NullProps;
    }

    PropertyField getDefaultObject() {
        return changeTable.value;
    }

    List<PropertyMapImplement<PropertyInterface, JoinPropertyInterface>> getImplements(Map<JoinPropertyInterface, ObjectValue> Keys, ChangePropertySecurityPolicy securityPolicy) {
        List<PropertyMapImplement<PropertyInterface,JoinPropertyInterface>> Result = new ArrayList<PropertyMapImplement<PropertyInterface,JoinPropertyInterface>>();
        List<PropertyMapImplement<PropertyInterface,JoinPropertyInterface>> BitProps = new ArrayList<PropertyMapImplement<PropertyInterface,JoinPropertyInterface>>();
        for(PropertyInterfaceImplement<JoinPropertyInterface> Implement : implementations.mapping.values())
            if(Implement instanceof PropertyMapImplement) {
                PropertyMapImplement<PropertyInterface,JoinPropertyInterface> PropertyImplement = (PropertyMapImplement<PropertyInterface,JoinPropertyInterface>)Implement;
                // все Data вперед, биты назад, остальные попорядку
                if(PropertyImplement.property instanceof DataProperty)
                    Result.add(PropertyImplement);
                else {
                    ChangeValue ChangeValue = PropertyImplement.mapGetChangeProperty(null, Keys, 0, securityPolicy);
                    if(ChangeValue!=null && ChangeValue.Class instanceof BitClass)
                        BitProps.add(PropertyImplement);
                    else // в начало
                        Result.add(0,PropertyImplement);
                }
            }
        Result.addAll(0,BitProps);
        return Result;
    }
}

class GroupPropertyInterface<T extends PropertyInterface> extends PropertyInterface<GroupPropertyInterface<T>> {
    PropertyInterfaceImplement<T> implement;

    GroupPropertyInterface(int iID,PropertyInterfaceImplement<T> iImplement) {
        super(iID);
        implement =iImplement;
    }
}

abstract class GroupProperty<T extends PropertyInterface> extends MapProperty<GroupPropertyInterface<T>,T,T,GroupPropertyInterface<T>,Object> {
    // каждый интерфейс должен имплементировать именно GetInterface GroupProperty

    // оператор
    int Operator;

    GroupProperty(TableFactory iTableFactory,Property<T> iProperty,int iOperator) {
        super(iTableFactory);
        groupProperty = iProperty;
        Operator = iOperator;
    }

    // группировочное св-во собсно должно быть не формулой
    Property<T> groupProperty;

    InterfaceClassSet<T> getImplementSet(InterfaceClass<GroupPropertyInterface<T>> ClassImplement) {
        InterfaceClassSet<T> ValueClassSet = groupProperty.getUniversalInterface();
        for(Map.Entry<GroupPropertyInterface<T>,ClassSet> Class : ClassImplement.entrySet())
            ValueClassSet = ValueClassSet.and(Class.getKey().implement.mapClassSet(Class.getValue()));
        return ValueClassSet;
    }

    InterfaceClassSet<T> getMapClassSet(MapRead<T> Read, InterfaceClassSet<T> GroupClassSet, InterfaceClassSet<GroupPropertyInterface<T>> Result) {
        // сначала делаем and всех classSet'ов, а затем getValueClass
        for(GroupPropertyInterface<T> Interface : interfaces)
            GroupClassSet = GroupClassSet.and(Read.getImplementClassSet(Interface.implement,ClassSet.universal));
        for(InterfaceClass<T> GroupImplement : GroupClassSet) {
            InterfaceClass<GroupPropertyInterface<T>> ValueClass = new InterfaceClass<GroupPropertyInterface<T>>();
            for(GroupPropertyInterface<T> Interface : interfaces)
                ValueClass.put(Interface,Read.getImplementValueClass(Interface.implement,GroupImplement));
            Result.or(new InterfaceClassSet<GroupPropertyInterface<T>>(ValueClass));
        }

        return GroupClassSet;
    }

    List<GroupPropertyInterface> GetChangedProperties(DataSession Session) {
        List<GroupPropertyInterface> ChangedProperties = new ArrayList<GroupPropertyInterface>();
        // должен вернуть null если нету изменений (или просто транслирует интерфейс) иначе возвращает AggregateProperty
        for(GroupPropertyInterface Interface : interfaces)
            if(Interface.implement.mapHasChanges(Session)) ChangedProperties.add(Interface);

        return ChangedProperties;
    }

    Property<T> getMapProperty() {
        return groupProperty;
    }

    Map<GroupPropertyInterface<T>, PropertyInterfaceImplement<T>> getMapImplements() {
        Map<GroupPropertyInterface<T>,PropertyInterfaceImplement<T>> Result = new HashMap<GroupPropertyInterface<T>,PropertyInterfaceImplement<T>>();
        for(GroupPropertyInterface<T> Interface : interfaces)
            Result.put(Interface,Interface.implement);
        return Result;
    }

    Collection<T> getMapInterfaces() {
        return groupProperty.interfaces;
    }

    void fillChangedRead(UnionQuery<T, Object> listQuery, Object value, MapChangedRead<T> read, ValueClassSet<GroupPropertyInterface<T>> readClasses) {
        // создается JoinQuery - на вход getMapInterfaces, Query.MapKeys - map интерфейсов
        InterfaceClassSet<T> mapClasses = new InterfaceClassSet<T>();
        for(ChangeClass<T> change : read.getMapChangeClass(groupProperty)) {
            InterfaceClassSet<GroupPropertyInterface<T>> resultInterface = new InterfaceClassSet<GroupPropertyInterface<T>>();
            InterfaceClassSet<T> groupInterface = getMapClassSet(read, change.interfaceClasses, resultInterface);
            if(!groupInterface.isEmpty()) {
                readClasses.or(new ChangeClass<GroupPropertyInterface<T>>(resultInterface,change.value));
                mapClasses.or(groupInterface);
            }
        }
        if(mapClasses.isEmpty()) return;

        JoinQuery<T,Object> query = new JoinQuery<T,Object>(groupProperty.interfaces);
        Map<PropertyInterfaceImplement<T>,SourceExpr> implementExprs = new HashMap<PropertyInterfaceImplement<T>, SourceExpr>();
        for(GroupPropertyInterface<T> propertyInterface : interfaces) {
            SourceExpr implementExpr = read.getImplementExpr(propertyInterface.implement, query.mapKeys, mapClasses);
            query.properties.put(propertyInterface, implementExpr);
            implementExprs.put(propertyInterface.implement,implementExpr);
        }
        read.fillMapExpr(query, value, groupProperty, query.mapKeys,implementExprs,mapClasses);
        listQuery.add(query);
    }

    public ClassSet calculateValueClass(InterfaceClass<GroupPropertyInterface<T>> ClassImplement) {

        ClassSet Result = new ClassSet();
        for(InterfaceClass<T> GroupImplement : getImplementSet(ClassImplement))
            Result.or(groupProperty.getValueClass(GroupImplement));
        return Result;
    }

    public InterfaceClassSet<GroupPropertyInterface<T>> calculateClassSet(ClassSet reqValue) {
        InterfaceClassSet<GroupPropertyInterface<T>> Result = new InterfaceClassSet<GroupPropertyInterface<T>>();
        getMapClassSet(DBRead, groupProperty.getClassSet(reqValue),Result);
        return Result;
    }

    public ValueClassSet<GroupPropertyInterface<T>> calculateValueClassSet() {
        ValueClassSet<GroupPropertyInterface<T>> Result = new ValueClassSet<GroupPropertyInterface<T>>();
        for(ChangeClass<T> ImplementChange : groupProperty.getValueClassSet()) {
            InterfaceClassSet<GroupPropertyInterface<T>> ImplementInterface = new InterfaceClassSet<GroupPropertyInterface<T>>();
            getMapClassSet(DBRead,ImplementChange.interfaceClasses,ImplementInterface);
            Result.or(new ChangeClass<GroupPropertyInterface<T>>(ImplementInterface,ImplementChange.value));
        }
        return Result;
    }

    Object GroupValue = "grfield";

    SourceExpr calculateSourceExpr(Map<GroupPropertyInterface<T>,? extends SourceExpr> joinImplement, InterfaceClassSet<GroupPropertyInterface<T>> joinClasses) {

        InterfaceClassSet<T> ImplementClasses = new InterfaceClassSet<T>();
        for(InterfaceClass<GroupPropertyInterface<T>> JoinClass : joinClasses)
            ImplementClasses.or(getImplementSet(JoinClass));

        JoinQuery<T,Object> Query = new JoinQuery<T,Object>(groupProperty.interfaces);
        for(GroupPropertyInterface<T> Interface : interfaces)
            Query.properties.put(Interface, Interface.implement.mapSourceExpr(Query.mapKeys,ImplementClasses));
        Query.properties.put(GroupValue, groupProperty.getSourceExpr(Query.mapKeys, ImplementClasses));

        return (new Join<GroupPropertyInterface<T>,Object>(new GroupQuery<Object,GroupPropertyInterface<T>,Object,T>(interfaces,Query,GroupValue,Operator), joinImplement)).exprs.get(GroupValue);
    }

    Map<Object, Type> getMapNullProps(Object Value) {
        Map<Object, Type> NullProps = new HashMap<Object,Type>();
        NullProps.put(Value, getType());
        InterfaceClass<GroupPropertyInterface<T>> InterfaceClass = getClassSet(ClassSet.universal).iterator().next();
        for(Map.Entry<GroupPropertyInterface<T>,ClassSet> Interface : InterfaceClass.entrySet())
            NullProps.put(Interface.getKey(),Interface.getValue().getType());
        return NullProps;
    }

    Source<GroupPropertyInterface<T>, PropertyField> getGroupQuery(List<MapChangedRead<T>> ReadList, PropertyField Value, ValueClassSet<GroupPropertyInterface<T>> ResultClass) {
        return new GroupQuery<Object,GroupPropertyInterface<T>,PropertyField,T>(interfaces,getMapQuery(ReadList,Value,ResultClass,false),Value,Operator);
    }
}

class SumGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    SumGroupProperty(TableFactory iTableFactory,Property<T> iProperty) {super(iTableFactory,iProperty,1);}

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
        // Group на ->1, Interface на ->2 - как было - возвр. 1 (на подчищение если (0 или 2) LEFT JOIN'им старые)
        groupProperty.setChangeType(RequiredTypes,1);

        for(GroupPropertyInterface<T> Interface : interfaces)
            if(Interface.implement instanceof PropertyMapImplement)
                (((PropertyMapImplement)Interface.implement).property).setChangeType(RequiredTypes,2);
    }

    Change incrementChanges(DataSession session, int changeType) {

        // конечный результат, с ключами и выражением
        OperationQuery<GroupPropertyInterface<T>,PropertyField> resultQuery = new OperationQuery<GroupPropertyInterface<T>,PropertyField>(interfaces,Union.SUM);
        ValueClassSet<GroupPropertyInterface<T>> resultClass = new ValueClassSet<GroupPropertyInterface<T>>();

        resultQuery.add(getGroupQuery(getChangeMap(session,1), changeTable.value,resultClass),1);
        resultQuery.add(getGroupQuery(getChangeImplements(session,0), changeTable.value,resultClass),1);
        resultQuery.add(getGroupQuery(getPreviousImplements(session), changeTable.value,resultClass),-1);

        return new Change(1,resultQuery,resultClass);
     }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return 1;
    }
}


class MaxGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    MaxGroupProperty(TableFactory iTableFactory,Property<T> iProperty) {super(iTableFactory,iProperty,0);}

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
        // Group на ->2, Interface на ->2 - как было - возвр. 2
        groupProperty.setChangeType(RequiredTypes,2);

        for(GroupPropertyInterface<T> Interface : interfaces)
            if(Interface.implement instanceof PropertyMapImplement)
                ((PropertyMapImplement)Interface.implement).property.setChangeType(RequiredTypes,2);
    }

    Change incrementChanges(DataSession session, int changeType) throws SQLException {

        // делаем Full Join (на 3) :
        //      a) ушедшие (previmp и prevmap) = старые (sourceexpr) LJ (prev+change) (вообще и пришедшие <= старых)
        //      b) пришедшие (change) > старых (sourceexpr)

        ValueClassSet<GroupPropertyInterface<T>> resultClass = new ValueClassSet<GroupPropertyInterface<T>>();

        PropertyField prevMapValue = new PropertyField("drop",Type.integer);

        ChangeQuery<GroupPropertyInterface<T>,PropertyField> changeQuery = new ChangeQuery<GroupPropertyInterface<T>,PropertyField>(interfaces);

        changeQuery.add(getGroupQuery(getPreviousChange(session),prevMapValue,resultClass));
        changeQuery.add(getGroupQuery(getChange(session,0), changeTable.value,resultClass));

        // подозрительные на изменения ключи
        JoinQuery<GroupPropertyInterface<T>,PropertyField> suspiciousQuery = new JoinQuery<GroupPropertyInterface<T>,PropertyField>(interfaces);
        Join<GroupPropertyInterface<T>,PropertyField> changeJoin = new Join<GroupPropertyInterface<T>,PropertyField>(changeQuery,suspiciousQuery);

        JoinExpr newValue = changeJoin.exprs.get(changeTable.value);
        JoinExpr OldValue = changeJoin.exprs.get(prevMapValue);
        SourceExpr prevValue = getSourceExpr(suspiciousQuery.mapKeys,resultClass.getClassSet(ClassSet.universal));

        suspiciousQuery.properties.put(changeTable.value, newValue);
        suspiciousQuery.properties.put(changeTable.prevValue, prevValue);

        suspiciousQuery.and(newValue.getWhere().and(prevValue.getWhere().not()).or(
                new CompareWhere(prevValue,newValue,CompareWhere.LESS)).or(new CompareWhere(prevValue,OldValue,CompareWhere.EQUALS)));


        // сохраняем
        Change incrementChange = new Change(2,suspiciousQuery,resultClass);
        incrementChange.save(session);

        JoinQuery<GroupPropertyInterface<T>,PropertyField> reReadQuery = new JoinQuery<GroupPropertyInterface<T>, PropertyField>(interfaces);
        Join<GroupPropertyInterface<T>,PropertyField> sourceJoin = new Join<GroupPropertyInterface<T>,PropertyField>(incrementChange.source, reReadQuery);

        newValue = sourceJoin.exprs.get(changeTable.value);
        // новое null и InJoin или ноаое меньше старого 
        reReadQuery.and(sourceJoin.inJoin.and(newValue.getWhere().not()).or(new CompareWhere(newValue,sourceJoin.exprs.get(changeTable.prevValue),CompareWhere.LESS)));

        if(!(reReadQuery.executeSelect(session,new LinkedHashMap<PropertyField,Boolean>(),1).size() == 0)) {
            // если кол-во > 0 перечитываем, делаем LJ GQ с протолкнутым ReReadQuery
            JoinQuery<KeyField,PropertyField> updateQuery = new JoinQuery<KeyField,PropertyField>(changeTable.keys);
            updateQuery.putKeyWhere(Collections.singletonMap(changeTable.property,ID));
            // сначала на LJ чтобы заNULL'ить максимумы
            updateQuery.and(new Join<GroupPropertyInterface<T>,PropertyField>(reReadQuery, changeTableMap, updateQuery).inJoin);
            // затем новые значения
            ValueClassSet<GroupPropertyInterface<T>> newClass = new ValueClassSet<GroupPropertyInterface<T>>();
            List<MapChangedRead<T>> newRead = new ArrayList<MapChangedRead<T>>(); newRead.add(getPrevious(session)); newRead.addAll(getChange(session,0));
            updateQuery.properties.put(changeTable.value, (new Join<GroupPropertyInterface<T>,PropertyField>(getGroupQuery(newRead, changeTable.value,newClass), changeTableMap, updateQuery)).exprs.get(changeTable.value));

//            Main.Session = Session;
//            new ModifyQuery(ChangeTable,UpdateQuery).outSelect(Session);
//            Main.Session = null;
            resultClass.or(resultClass.and(newClass));
            session.UpdateRecords(new ModifyQuery(changeTable, updateQuery));
        }
        return incrementChange;
    }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return 2;
    }
}

// КОМБИНАЦИИ (ЛИНЕЙНЫЕ,MAX,OVERRIDE) принимают null на входе, по сути как Relation но работают на Or\FULL JOIN
// соответственно мн-во св-в полностью должно отображаться на интерфейсы

abstract class UnionProperty extends AggregateProperty<PropertyInterface> {

    UnionProperty(TableFactory iTableFactory,Union iOperator) {
        super(iTableFactory);
        operator = iOperator;
    }

    // имплементации св-в (полные)
    List<PropertyMapImplement<PropertyInterface,PropertyInterface>> operands = new ArrayList<PropertyMapImplement<PropertyInterface, PropertyInterface>>();

    Union operator;
    // коэффициенты
    Map<PropertyMapImplement<PropertyInterface,PropertyInterface>,Integer> coeffs = new HashMap<PropertyMapImplement<PropertyInterface, PropertyInterface>, Integer>();

    SourceExpr calculateSourceExpr(Map<PropertyInterface,? extends SourceExpr> joinImplement, InterfaceClassSet<PropertyInterface> joinClasses) {

        String valueString = "joinvalue";
        OperationQuery<PropertyInterface,String> resultQuery = new OperationQuery<PropertyInterface,String>(interfaces, operator);
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> operand : operands) {
            if(operand.mapIsInInterface(joinClasses)) {
                JoinQuery<PropertyInterface,String> query = new JoinQuery<PropertyInterface, String>(interfaces);
                query.properties.put(valueString, operand.mapSourceExpr(query.mapKeys, joinClasses));
                resultQuery.add(query, coeffs.get(operand));
            }
        }

        return (new Join<PropertyInterface,String>(resultQuery, joinImplement)).exprs.get(valueString);
    }

    public ClassSet calculateValueClass(InterfaceClass<PropertyInterface> ClassImplement) {
        // в отличии от Relation только когда есть хоть одно св-во
        ClassSet ResultClass = new ClassSet();
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : operands)
            ResultClass.or(Operand.mapValueClass(ClassImplement));
        return ResultClass;
    }

    public InterfaceClassSet<PropertyInterface> calculateClassSet(ClassSet reqValue) {
        // в отличии от Relation игнорируем null
        InterfaceClassSet<PropertyInterface> Result = new InterfaceClassSet<PropertyInterface>();
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : operands)
            Result.or(Operand.mapClassSet(reqValue));
        return Result;
    }

    public ValueClassSet<PropertyInterface> calculateValueClassSet() {
        ValueClassSet<PropertyInterface> Result = new ValueClassSet<PropertyInterface>();
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : operands)
            Result.or(Operand.mapValueClassSet());
        return Result;
    }

    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        if(ChangedProperties.contains(this)) return true;
        if(NoUpdate.contains(this)) return false;

        boolean Changed = false;

        for(PropertyMapImplement Operand : operands)
            Changed = Operand.mapFillChangedList(ChangedProperties, Changes, NoUpdate) || Changed;

        if(Changed)
            ChangedProperties.add(this);

        return Changed;
    }

    List<PropertyMapImplement<PropertyInterface,PropertyInterface>> getChangedProperties(DataSession Session) {

        List<PropertyMapImplement<PropertyInterface,PropertyInterface>> ChangedProperties = new ArrayList<PropertyMapImplement<PropertyInterface,PropertyInterface>>();
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : operands)
            if(Operand.mapHasChanges(Session)) ChangedProperties.add(Operand);

        return ChangedProperties;
    }

    // определяет ClassSet подмн-ва и что все операнды пересекаются
    ValueClassSet<PropertyInterface> getChangeClassSet(DataSession Session,List<PropertyMapImplement<PropertyInterface,PropertyInterface>> ChangedProps) {

        ValueClassSet<PropertyInterface> Result = new ValueClassSet<PropertyInterface>(new ClassSet(),getUniversalInterface());
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : ChangedProps)// {
//            if(!intersect(Session, Operand,ChangedProps)) return null;
            Result = Result.and(Operand.mapValueClassSet(Session));
//        }

        return Result;
    }


    Change incrementChanges(DataSession session, int changeType) {

        if(getUnionType()==0 && changeType ==1) changeType = 2;

        //      	0                   1                           2
        //Max(0)	значение,SS,LJ      не может быть               значение,SS,LJ,prevv
        //Sum(1)	значение,SS,LJ      значение,без SS, без LJ     значение,SS,LJ,prevv
        //Override(2)	значение,SS,LJ      старое поле=null,SS, LJ     значение,SS,LJ,prevv

        ValueClassSet<PropertyInterface> resultClass = new ValueClassSet<PropertyInterface>();

        // неструктурно как и все оптимизации
        if(operator == Union.SUM && changeType ==1) {
            OperationQuery<PropertyInterface,PropertyField> resultQuery = new OperationQuery<PropertyInterface,PropertyField>(interfaces,Union.SUM);

            for(PropertyMapImplement<PropertyInterface,PropertyInterface> operand : getChangedProperties(session)) {
                JoinQuery<PropertyInterface,PropertyField> query = new JoinQuery<PropertyInterface, PropertyField>(interfaces);
                JoinExpr changeExpr = operand.mapChangeExpr(session, query.mapKeys, 1);
                query.properties.put(changeTable.value, changeExpr);
                query.and(changeExpr.from.inJoin);
                resultQuery.add(query, coeffs.get(operand));

                resultClass.or(operand.mapValueClassSet(session));
            }

            return new Change(1,resultQuery, resultClass);
        } else {
            ChangeQuery<PropertyInterface,PropertyField> resultQuery = new ChangeQuery<PropertyInterface,PropertyField>(interfaces);
            resultQuery.add(getChange(session, changeType ==1?1:0, changeTable.value,resultClass));
            if(changeType ==2) resultQuery.add(getChange(session,2, changeTable.prevValue,resultClass));

            return new Change(changeType,resultQuery,resultClass);
        }
    }

    Source<PropertyInterface,PropertyField> getChange(DataSession session, int mapType, PropertyField value, ValueClassSet<PropertyInterface> resultClass) {

        ChangeQuery<PropertyInterface,PropertyField> resultQuery = new ChangeQuery<PropertyInterface,PropertyField>(interfaces);

        ListIterator<List<PropertyMapImplement<PropertyInterface,PropertyInterface>>> il = SetBuilder.buildSubSetList(getChangedProperties(session)).listIterator();
        // пропустим пустое подмн-во
        il.next();
        while(il.hasNext()) {
            List<PropertyMapImplement<PropertyInterface, PropertyInterface>> changedProps = il.next();

            // проверим что все попарно пересекаются по классам, заодно строим InterfaceClassSet<T> св-в
            ValueClassSet<PropertyInterface> changeClass = getChangeClassSet(session,changedProps);
            if(changeClass.isEmpty()) continue;

            JoinQuery<PropertyInterface,PropertyField> query = new JoinQuery<PropertyInterface, PropertyField>(interfaces);
            List<SourceExpr> resultOperands = new ArrayList<SourceExpr>();
            // именно в порядке операндов (для Overrid'а важно)
            for(PropertyMapImplement<PropertyInterface, PropertyInterface> operand : operands)
                if(changedProps.contains(operand)) {
                    JoinExpr changedExpr = operand.mapChangeExpr(session, query.mapKeys, mapType);
                    resultOperands.add(new LinearExpr(changedExpr, coeffs.get(operand)));
                    query.and(changedExpr.from.inJoin);
                } else { // AND'им как если Join результат
                    ValueClassSet<PropertyInterface> leftClass = changeClass.and(operand.mapValueClassSet());
                    if(!leftClass.isEmpty()) {
                        resultOperands.add(new LinearExpr(operand.mapSourceExpr(query.mapKeys, leftClass.getClassSet(ClassSet.universal)), coeffs.get(operand)));
                        // значит может изменится Value на другое значение
                        changeClass.or(leftClass);
                    }
                }

            query.properties.put(value, OperationQuery.getExpr(resultOperands, operator));

            resultQuery.add(query);
            resultClass.or(changeClass);
        }
        if(resultQuery.where.isFalse())
            resultQuery.properties.put(value, getType().getExpr(null));

        return resultQuery;
    }

    boolean intersect(DataSession Session, PropertyMapImplement<PropertyInterface,PropertyInterface> Operand, Collection<PropertyMapImplement<PropertyInterface,PropertyInterface>> Operands) {
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> IntersectOperand : Operands) {
            if(Operand==IntersectOperand) return true;
            if(!intersect(Session, Operand,IntersectOperand)) return false;
        }
        return true;
    }

    // проверяет пересекаются по классам операнды или нет
    boolean intersect(DataSession Session, PropertyMapImplement<PropertyInterface,PropertyInterface> Operand, PropertyMapImplement<PropertyInterface,PropertyInterface> IntersectOperand) {
        return (Session.changes.addClasses.size() > 0 && Session.changes.removeClasses.size() > 0) ||
               !Operand.mapClassSet(ClassSet.universal).and(IntersectOperand.mapClassSet(ClassSet.universal)).isEmpty();
//        return true;
    }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return getUnionType();
    }

    Integer getUnionType() {
        return 0;
    }

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
        if(getUnionType()==0 && IncrementType.equals(1)) IncrementType = 2;

        for(PropertyMapImplement Operand : operands)
            Operand.property.setChangeType(RequiredTypes,IncrementType);
    }

    List<PropertyMapImplement<PropertyInterface, PropertyInterface>> getImplements(Map<PropertyInterface, ObjectValue> Keys, ChangePropertySecurityPolicy securityPolicy) {
        return operands;
    }

    int getCoeff(PropertyMapImplement<?, PropertyInterface> Implement) {
        return coeffs.get(Implement);
    }
}


class SumUnionProperty extends UnionProperty {

    SumUnionProperty(TableFactory iTableFactory) {super(iTableFactory,Union.SUM);}

    Integer getUnionType() {
        return 1;
    }
}

class MaxUnionProperty extends UnionProperty {

    MaxUnionProperty(TableFactory iTableFactory) {super(iTableFactory,Union.MAX);}

}

class OverrideUnionProperty extends UnionProperty {

    OverrideUnionProperty(TableFactory iTableFactory) {super(iTableFactory,Union.OVERRIDE);}
}


// ФОРМУЛЫ

class FormulaPropertyInterface<P extends FormulaPropertyInterface<P>> extends PropertyInterface<P> {
//    Class Class;

    FormulaPropertyInterface(int iID) {
        super(iID);
//        Class = iClass;
    }
}

abstract class FormulaProperty<T extends FormulaPropertyInterface> extends AggregateProperty<T> {

    protected FormulaProperty(TableFactory iTableFactory) {
        super(iTableFactory);
    }

    void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
    }

    // не может быть изменений в принципе
    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        return false;
    }

    Change incrementChanges(DataSession session, int changeType) {
        return null;
    }

    Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return null;
    }

}

// вообще Collection
abstract class ValueFormulaProperty<T extends FormulaPropertyInterface> extends FormulaProperty<T> {

    Class Value;

    ValueFormulaProperty(TableFactory iTableFactory,Class iValue) {
        super(iTableFactory);
        Value = iValue;
    }

    public ClassSet calculateValueClass(InterfaceClass<T> ClassImplement) {
        if(ClassImplement.hasEmpty()) return new ClassSet();
        return ClassSet.getUp(Value);
    }

    public InterfaceClassSet<T> calculateClassSet(ClassSet reqValue) {

        if(reqValue.intersect(ClassSet.getUp(Value)))
            return getOperandInterface();
        else
            return new InterfaceClassSet<T>();
    }

    public ValueClassSet<T> calculateValueClassSet() {
        return new ValueClassSet<T>(ClassSet.getUp(Value),getOperandInterface());
    }

    abstract Class getOperandClass();

    InterfaceClassSet<T> getOperandInterface() {
        InterfaceClass<T> Result = new InterfaceClass<T>();
        for(T Interface : interfaces)
            Result.put(Interface,ClassSet.getUp(getOperandClass()));
        return new InterfaceClassSet<T>(Result);
    }

}

class StringFormulaPropertyInterface extends FormulaPropertyInterface<StringFormulaPropertyInterface> {

    StringFormulaPropertyInterface(int iID) {
        super(iID);
    }
}

class StringFormulaProperty extends ValueFormulaProperty<StringFormulaPropertyInterface> {

    String Formula;

    StringFormulaProperty(TableFactory iTableFactory, Class iValue, String iFormula) {
        super(iTableFactory,iValue);
        Formula = iFormula;
    }

    SourceExpr calculateSourceExpr(Map<StringFormulaPropertyInterface,? extends SourceExpr> joinImplement, InterfaceClassSet<StringFormulaPropertyInterface> joinClasses) {

        Map<String,SourceExpr> Params = new HashMap<String, SourceExpr>();
        for(StringFormulaPropertyInterface Interface : interfaces)
            Params.put("prm"+(Interface.ID+1), joinImplement.get(Interface));

        return new FormulaExpr(Formula,Value.getType(),Params);
    }

    Class getOperandClass() {
        return Class.data;
    }
}

abstract class WhereFormulaProperty extends ValueFormulaProperty<FormulaPropertyInterface> {

    WhereFormulaProperty(TableFactory iTableFactory) {
        super(iTableFactory, Class.bit);
    }

    Class getOperandClass() {
        return Class.base;
    }

    SourceExpr calculateSourceExpr(Map<FormulaPropertyInterface, ? extends SourceExpr> joinImplement, InterfaceClassSet<FormulaPropertyInterface> joinClasses) {
        return new CaseExpr(getWhere(joinImplement),Type.bit.getExpr(true));
    }

    abstract Where getWhere(Map<FormulaPropertyInterface, ? extends SourceExpr> JoinImplement);
}

class CompareFormulaProperty extends WhereFormulaProperty {

    int Compare;
    FormulaPropertyInterface Operator1;
    FormulaPropertyInterface Operator2;

    CompareFormulaProperty(TableFactory iTableFactory, int iCompare) {
        super(iTableFactory);
        Compare = iCompare;
        Operator1 = new FormulaPropertyInterface(0);
        Operator2 = new FormulaPropertyInterface(1);
        interfaces.add(Operator1);
        interfaces.add(Operator2);
    }

    Where getWhere(Map<FormulaPropertyInterface, ? extends SourceExpr> JoinImplement) {
        return new CompareWhere(JoinImplement.get(Operator1),JoinImplement.get(Operator2),Compare);
    }
}

class NotNullFormulaProperty extends WhereFormulaProperty {

    FormulaPropertyInterface Property;

    NotNullFormulaProperty(TableFactory iTableFactory) {
        super(iTableFactory);
        Property = new FormulaPropertyInterface(0);
        interfaces.add(Property);
    }

    Where getWhere(Map<FormulaPropertyInterface, ? extends SourceExpr> JoinImplement) {
        return JoinImplement.get(Property).getWhere();
    }
}


class MultiplyFormulaProperty extends StringFormulaProperty {

    MultiplyFormulaProperty(TableFactory iTableFactory,Class iValue,int Params) {
        super(iTableFactory,iValue,"");
        for(int i=0;i<Params;i++) {
            interfaces.add(new StringFormulaPropertyInterface(i));
            Formula = (Formula.length()==0?"":Formula+"*") + "prm"+(i+1);
        }
    }

    Class getOperandClass() {
        return Class.integral;
    }
}

// выбирает объект по битам
class ObjectFormulaProperty extends FormulaProperty<FormulaPropertyInterface> {

    FormulaPropertyInterface ObjectInterface;
    ClassSet ObjectClass;

    ObjectFormulaProperty(TableFactory iTableFactory, ObjectClass iObjectClass) {
        super(iTableFactory);
        ObjectClass = ClassSet.getUp(iObjectClass);
        ObjectInterface = new FormulaPropertyInterface(0);
        interfaces.add(ObjectInterface);
    }

    SourceExpr calculateSourceExpr(Map<FormulaPropertyInterface,? extends SourceExpr> joinImplement, InterfaceClassSet<FormulaPropertyInterface> joinClasses) {
        Where where = Where.TRUE;
        for(FormulaPropertyInterface Interface : interfaces)
            if(Interface!=ObjectInterface)
                where = where.and(joinImplement.get(Interface).getWhere());

        return new CaseExpr(where, joinImplement.get(ObjectInterface));
    }

    ClassSet calculateValueClass(InterfaceClass<FormulaPropertyInterface> interfaceImplement) {
        return interfaceImplement.get(ObjectInterface);
    }

    InterfaceClassSet<FormulaPropertyInterface> calculateClassSet(ClassSet reqValue) {
        if(!reqValue.isEmpty())
            return new InterfaceClassSet<FormulaPropertyInterface>(getInterfaceClass(reqValue));
        else
            return new InterfaceClassSet<FormulaPropertyInterface>();
    }

    InterfaceClass<FormulaPropertyInterface> getInterfaceClass(ClassSet ReqValue) {
        InterfaceClass<FormulaPropertyInterface> Result = new InterfaceClass<FormulaPropertyInterface>();
        for(FormulaPropertyInterface Interface : interfaces)
            Result.put(Interface,Interface==ObjectInterface?ReqValue:new ClassSet(Class.bit));
        return Result;
    }

    ValueClassSet<FormulaPropertyInterface> calculateValueClassSet() {
        throw new RuntimeException("у этого св-ва этот метод слишком сложный, поэтому надо решать верхними частными случаям");
    }

}

// изменения данных
class DataChanges {
    Set<DataProperty> properties = new HashSet<DataProperty>();

    Set<Class> addClasses = new HashSet<Class>();
    Set<Class> removeClasses = new HashSet<Class>();

    DataChanges copy() {
        DataChanges CopyChanges = new DataChanges();
        CopyChanges.properties.addAll(properties);
        CopyChanges.addClasses.addAll(addClasses);
        CopyChanges.removeClasses.addAll(removeClasses);
        return CopyChanges;
    }

    public boolean hasChanges() {
        return !(properties.isEmpty() && addClasses.isEmpty() && removeClasses.isEmpty());
    }
}

interface PropertyUpdateView {

    Collection<Property> getUpdateProperties();

    Collection<Property> getNoUpdateProperties();
    boolean toSave(Property Property);
}

class DataSession  {

    Connection connection;
    SQLSyntax syntax;

    DataChanges changes = new DataChanges();
    Map<PropertyUpdateView,DataChanges> IncrementChanges = new HashMap<PropertyUpdateView,DataChanges>();

    Map<Property, Property.Change> propertyChanges = new HashMap<Property, Property.Change>();
    <P extends PropertyInterface> Property<P>.Change getChange(Property<P> Property) {
        return propertyChanges.get(Property);
    }

    Map<Class,ClassSet> addChanges = new HashMap<Class, ClassSet>();
    Map<Class,ClassSet> removeChanges = new HashMap<Class, ClassSet>();
    Map<DataProperty, ValueClassSet<DataPropertyInterface>> dataChanges = new HashMap<DataProperty, ValueClassSet<DataPropertyInterface>>();

    TableFactory TableFactory;
    ObjectClass ObjectClass;

    int ID = 0;

    DataSession(DataAdapter Adapter,int iID,TableFactory iTableFactory,ObjectClass iObjectClass) throws SQLException{

        ID = iID;
        syntax = Adapter;
        TableFactory = iTableFactory;
        ObjectClass = iObjectClass;

        try {
            connection = Adapter.startConnection();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        TableFactory.fillSession(this);
    }

    void restart(boolean Cancel) throws SQLException {

        if(Cancel)
            for(DataChanges ViewChanges : IncrementChanges.values()) {
                ViewChanges.properties.addAll(changes.properties);
                ViewChanges.addClasses.addAll(changes.addClasses);
                ViewChanges.removeClasses.addAll(changes.removeClasses);
            }

        TableFactory.clearSession(this);
        changes = new DataChanges();
        NewClasses = new HashMap<Integer,Class>();
        BaseClasses = new HashMap<Integer,Class>();

        propertyChanges = new HashMap<Property, Property.Change>();
        addChanges = new HashMap<Class, ClassSet>();
        removeChanges = new HashMap<Class, ClassSet>();
        dataChanges = new HashMap<DataProperty, ValueClassSet<DataPropertyInterface>>();
    }

    Map<Integer,Class> NewClasses = new HashMap<Integer,Class>();
    // классы на момент выполнения
    Map<Integer,Class> BaseClasses = new HashMap<Integer,Class>();

    private void putClassChanges(Set<Class> Changes,Class PrevClass,Map<Class,ClassSet> To) {
        for(Class Change : Changes) {
            ClassSet PrevChange = To.get(Change);
            if(PrevChange==null) PrevChange = new ClassSet();
            PrevChange.or(new ClassSet(PrevClass));
            To.put(Change,PrevChange);
        }
    }

    void changeClass(Integer idObject,Class ToClass) throws SQLException {
        if(ToClass==null) ToClass = Class.base;

        Set<Class> AddClasses = new HashSet<Class>();
        Set<Class> RemoveClasses = new HashSet<Class>();
        Class PrevClass = getObjectClass(idObject);
        ToClass.GetDiffSet(PrevClass,AddClasses,RemoveClasses);

        putClassChanges(AddClasses,PrevClass, addChanges);
        TableFactory.addClassTable.changeClass(this,idObject,AddClasses,false);
        TableFactory.removeClassTable.changeClass(this,idObject,AddClasses,true);

        putClassChanges(RemoveClasses,PrevClass, removeChanges);
        TableFactory.removeClassTable.changeClass(this,idObject,RemoveClasses,false);
        TableFactory.addClassTable.changeClass(this,idObject,RemoveClasses,true);

        if(!NewClasses.containsKey(idObject))
            BaseClasses.put(idObject,PrevClass);
        NewClasses.put(idObject,ToClass);

        changes.addClasses.addAll(AddClasses);
        changes.removeClasses.addAll(RemoveClasses);

        for(DataChanges ViewChanges : IncrementChanges.values()) {
            ViewChanges.addClasses.addAll(AddClasses);
            ViewChanges.removeClasses.addAll(RemoveClasses);
        }
    }

    <T extends PropertyInterface> Object readProperty(Property<T> Property,Map<T,ObjectValue> Keys) throws SQLException {
        String ReadValue = "readvalue";
        JoinQuery<T,Object> ReadQuery = new JoinQuery<T, Object>(Property.interfaces);

        Map<T,Integer> KeyValues = new HashMap<T,Integer>();
        for(Map.Entry<T,ObjectValue> MapKey : Keys.entrySet())
            KeyValues.put(MapKey.getKey(), (Integer) MapKey.getValue().object);
        ReadQuery.putKeyWhere(KeyValues);

        ReadQuery.properties.put(ReadValue, getSourceExpr(Property,ReadQuery.mapKeys,new InterfaceClassSet<T>(new InterfaceClass<T>(Keys))));
        return ReadQuery.executeSelect(this).values().iterator().next().get(ReadValue);
    }

    void changeProperty(DataProperty Property, Map<DataPropertyInterface, ObjectValue> Keys, Object NewValue, boolean externalID) throws SQLException {

        // если изменяем по внешнему коду, но сначала надо найти внутренний код, а затем менять
        if (externalID && NewValue!=null) {

            DataProperty<?> extPropID = Property.Value.getExternalID();

            JoinQuery<DataPropertyInterface,String> query = new JoinQuery<DataPropertyInterface, String>(extPropID.interfaces);
            query.where = query.where.and(new CompareWhere(extPropID.getSourceExpr(query.mapKeys,extPropID.getClassSet(ClassSet.universal)),Property.getType().getExpr(NewValue),CompareWhere.EQUALS));

            LinkedHashMap<Map<DataPropertyInterface,Integer>,Map<String,Object>> result = query.executeSelect(this);

            if (result.size() == 0) return;

            NewValue = result.keySet().iterator().next().values().iterator().next();
        }

        // запишем в таблицу
        // также заодно новые классы считаем
        Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
        InterfaceClass<DataPropertyInterface> InterfaceClass = new InterfaceClass<DataPropertyInterface>();
        for(Map.Entry<KeyField,DataPropertyInterface> Field : (Set<Map.Entry<KeyField,DataPropertyInterface>>)Property.dataTableMap.entrySet()) {
            Integer idObject = (Integer) Keys.get(Field.getValue()).object;
            InsertKeys.put(Field.getKey(), idObject);
            InterfaceClass.put(Field.getValue(),getBaseClassSet(idObject));
        }

        InsertKeys.put(Property.dataTable.property,Property.ID);

        Map<PropertyField,Object> InsertValues = new HashMap<PropertyField,Object>();
        InsertValues.put(Property.dataTable.value,NewValue);

        ClassSet ValueClass = Property.getBaseClass();
        if(ValueClass.intersect(ClassSet.getUp(ObjectClass)))
            ValueClass = getBaseClassSet((Integer) NewValue);

        UpdateInsertRecord(Property.dataTable,InsertKeys,InsertValues);

        // пометим изменения
        changes.properties.add(Property);

        ValueClassSet<DataPropertyInterface> DataChange = dataChanges.get(Property);
        if(DataChange==null) DataChange = new ValueClassSet<DataPropertyInterface>();
        DataChange.or(new ChangeClass<DataPropertyInterface>(new InterfaceClassSet<DataPropertyInterface>(InterfaceClass),ValueClass));
        dataChanges.put(Property,DataChange);

        for(DataChanges ViewChanges : IncrementChanges.values())
            ViewChanges.properties.add(Property);
    }

    Class readClass(Integer idObject) throws SQLException {
        if(BusinessLogics.AutoFillDB) return null;

        return ObjectClass.findClassID(TableFactory.objectTable.GetClassID(this,idObject));
    }

    Class getObjectClass(Integer idObject) throws SQLException {
        Class NewClass = NewClasses.get(idObject);
        if(NewClass==null)
            NewClass = readClass(idObject);
        if(NewClass==null)
            NewClass = Class.base;
        return NewClass;
    }

    ClassSet getBaseClassSet(Integer idObject) throws SQLException {
        if(idObject==null) return new ClassSet();
        Class BaseClass = BaseClasses.get(idObject);
        if(BaseClass==null)
            BaseClass = readClass(idObject);
        return new ClassSet(BaseClass);
    }

    // последний параметр
    List<Property> update(PropertyUpdateView ToUpdate,Collection<Class> UpdateClasses) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)

        DataChanges ToUpdateChanges = IncrementChanges.get(ToUpdate);
        if(ToUpdateChanges==null) ToUpdateChanges = changes;

        Collection<Property> ToUpdateProperties = ToUpdate.getUpdateProperties();
        Collection<Property> NoUpdateProperties = ToUpdate.getNoUpdateProperties();
        // сначала читаем инкрементные св-ва которые изменились
        List<Property> IncrementUpdateList = BusinessLogics.getChangedList(ToUpdateProperties,ToUpdateChanges,NoUpdateProperties);
        List<Property> UpdateList = BusinessLogics.getChangedList(IncrementUpdateList, changes,NoUpdateProperties);

        Map<Property,Integer> RequiredTypes = new HashMap<Property,Integer>();
        // пробежим вперед пометим свойства которые изменились, но неясно на что
        for(Property Property : UpdateList)
            RequiredTypes.put(Property,ToUpdateProperties.contains(Property)?0:null);
        Map<Property, Integer> IncrementTypes = getIncrementTypes(UpdateList, RequiredTypes);

        // запускаем IncrementChanges для этого списка
        for(Property Property : UpdateList) {
//            System.out.println(Property.caption);
//            if(Property.caption.equals("Остаток (расх.)"))
//                Property = Property;
            Property.Change Change = Property.incrementChanges(this,IncrementTypes.get(Property));
            // подгоняем тип
            Change.correct(RequiredTypes.get(Property));
//            System.out.println("inctype"+Property.caption+" "+IncrementTypes.get(Property));
//            Main.Session = this;
//            Change.out(this);
//            Main.Session = null;
            if(!(Property instanceof MaxGroupProperty) && ToUpdate.toSave(Property))
                Change.save(this);
//            System.out.println(Property.caption + " incComplexity : " + Change.Source.getComplexity());
//            Change.out(this);
/*            System.out.println(Property.caption+" - CHANGES");
            Property.OutChangesTable(this);
            System.out.println(Property.caption+" - CURRENT");
            Property.Out(this);
            Change.checkClasses(this);*/
            propertyChanges.put(Property,Change);
        }

        UpdateClasses.addAll(ToUpdateChanges.addClasses);
        UpdateClasses.addAll(ToUpdateChanges.removeClasses);

        // сбрасываем лог
        IncrementChanges.put(ToUpdate,new DataChanges());

        return IncrementUpdateList;
    }

    // определяет на что считаться 0,1,2
    private Map<Property, Integer> getIncrementTypes(List<Property> UpdateList, Map<Property, Integer> RequiredTypes) {
        // бежим по списку (в обратном порядке) заполняем требования,
        Collections.reverse(UpdateList);
        // на какие значения читаться Persistent'ам
        Map<Property,Integer> IncrementTypes = new HashMap<Property,Integer>();
        // Waiter'ы св-ва которые ждут определившехся на выполнение св-в : не persistent и не 2
        Set<Property> ToWait = null;
        Map<Property,Set<Property>> Waiters = new HashMap<Property, Set<Property>>();
        for(Property Property : UpdateList) {
            Integer IncType = RequiredTypes.get(Property);
            // сначала проверим на Persistent и на "альтруистические" св-ва
            if(IncType==null || Property.IsPersistent()) {
                ToWait = new HashSet<Property>();
                IncType = Property.getIncrementType(UpdateList, ToWait);
            }
            // если определившееся (точно 0 или 1) запустим Waiter'ов, соответственно вычистим
            if(IncType==null || (!Property.IsPersistent() && !IncType.equals(2))) {
                for(Iterator<Map.Entry<Property,Set<Property>>> ie = Waiters.entrySet().iterator();ie.hasNext();) {
                    Map.Entry<Property,Set<Property>> Wait = ie.next();
                    if(Wait.getValue().contains(Property))
                        if(IncType==null) // докидываем еще Waiter'ов
                            Wait.getValue().addAll(ToWait);
                        else { // нашли нужный тип, remove'ся
                            fillChanges(Wait.getKey(), IncType, RequiredTypes, IncrementTypes);
                            ie.remove();
                        }
                }
            }
            if(IncType!=null)
                fillChanges(Property, IncType, RequiredTypes, IncrementTypes);
            else // св-во не знает пока чего хочет
                Waiters.put(Property, ToWait);
        }
        Collections.reverse(UpdateList);
        // еше могут остаться Waiter'ы, тогда возьмем первую не 2, иначе возьмем 0 (все чтобы еще LJ минимизировать)
        for(Property Property : UpdateList) {
            Integer IncType = IncrementTypes.get(Property);
            if(IncType==null) {
                for(Property WaitProperty : Waiters.get(Property)) {
                    Integer WaitType = IncrementTypes.get(WaitProperty);
                    if(!WaitType.equals(2)) IncType = WaitType;
                }
                if(IncType==null) IncType = 0;
                fillChanges(Property, IncType, RequiredTypes, IncrementTypes);
            }
        }
        return IncrementTypes;
    }

    private void fillChanges(Property Property, Integer incrementType, Map<Property, Integer> requiredTypes, Map<Property, Integer> incrementTypes) {
        incrementTypes.put(Property, incrementType);
        Property.fillRequiredChanges(incrementType, requiredTypes);
    }

    void saveClassChanges() throws SQLException {

        for(Integer idObject : NewClasses.keySet()) {
            Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
            InsertKeys.put(TableFactory.objectTable.key, idObject);

            Map<PropertyField,Object> InsertProps = new HashMap<PropertyField,Object>();
            Class ChangeClass = NewClasses.get(idObject);
            InsertProps.put(TableFactory.objectTable.objectClass,ChangeClass!=null?ChangeClass.ID:null);

            UpdateInsertRecord(TableFactory.objectTable,InsertKeys,InsertProps);
        }
    }

    <P extends PropertyInterface> ValueClassSet<P> getSourceClass(Property<P> Property) {
        ValueClassSet<P> Result = Property.getValueClassSet();
        Property<P>.Change Change = getChange(Property);
        if(Change!=null) {
            Result = new ValueClassSet<P>(Result);
            Result.or(Change.classes);
        }
        return Result;
    }

    // записывается в запрос с map'ом
    <P extends PropertyInterface> SourceExpr getSourceExpr(Property<P> property, Map<P,? extends SourceExpr> joinImplement, InterfaceClassSet<P> joinClasses) {

        boolean inInterface = property.isInInterface(joinClasses);
        Property<P>.Change change = getChange(property);
        if(change!=null && !change.classes.getClassSet(ClassSet.universal).and(joinClasses).isEmpty()) {
            String value = "joinvalue";

            ChangeQuery<P,String> unionQuery = new ChangeQuery<P,String>(property.interfaces);

            if(inInterface) {
                JoinQuery<P,String> sourceQuery = new JoinQuery<P,String>(property.interfaces);
                SourceExpr valueExpr = property.getSourceExpr(sourceQuery.mapKeys, joinClasses);
                sourceQuery.properties.put(value, valueExpr);
                sourceQuery.and(valueExpr.getWhere());
                unionQuery.add(sourceQuery);
            }

            JoinQuery<P,String> newQuery = new JoinQuery<P,String>(property.interfaces);
            JoinExpr changeExpr = getChange(property).getExpr(newQuery.mapKeys, 0);
            newQuery.properties.put(value, changeExpr);
            newQuery.and(changeExpr.from.inJoin);
            unionQuery.add(newQuery);

            return (new Join<P,String>(unionQuery, joinImplement)).exprs.get(value);
        } else
        if(inInterface)
            return property.getSourceExpr(joinImplement, joinClasses);
        else
            return property.getType().getExpr(null);
    }

    boolean InTransaction = false;

    void startTransaction() throws SQLException {
        InTransaction = true;

        if(!syntax.noAutoCommit())
            execute(syntax.startTransaction());
    }

    void rollbackTransaction() throws SQLException {
        execute(syntax.rollbackTransaction());

        InTransaction = false;
    }

    void commitTransaction() throws SQLException {
        execute(syntax.commitTransaction());

        InTransaction = false;
    }

    void CreateTable(Table Table) throws SQLException {
        String CreateString = "";
        String KeyString = "";
        for(KeyField Key : Table.keys) {
            CreateString = (CreateString.length()==0?"":CreateString+',') + Key.GetDeclare(syntax);
            KeyString = (KeyString.length()==0?"":KeyString+',') + Key.Name;
        }
        for(PropertyField Prop : Table.Properties)
            CreateString = CreateString+',' + Prop.GetDeclare(syntax);
        CreateString = CreateString + ",CONSTRAINT PK_" + Table.Name + " PRIMARY KEY " + syntax.getClustered() + " (" + KeyString + ")";

        try {
            execute("DROP TABLE "+Table.Name+" CASCADE CONSTRAINTS");
        } catch (SQLException e) {
            e.getErrorCode();
        }

//        System.out.println("CREATE TABLE "+Table.Name+" ("+CreateString+")");
        execute("CREATE TABLE "+Table.Name+" ("+CreateString+")");

        int IndexNum = 1;
        for(List<PropertyField> Index : Table.Indexes) {
            String Columns = "";
            for(PropertyField IndexField : Index)
                Columns = (Columns.length()==0?"":Columns+",") + IndexField.Name;

            execute("CREATE INDEX "+Table.Name+"_idx_"+(IndexNum++)+" ON "+Table.Name+" ("+Columns+")");
        }
    }

    void createTemporaryTable(SessionTable Table) throws SQLException {
        String CreateString = "";
        String KeyString = "";
        for(KeyField Key : Table.keys) {
            CreateString = (CreateString.length()==0?"":CreateString+',') + Key.GetDeclare(syntax);
            KeyString = (KeyString.length()==0?"":KeyString+',') + Key.Name;
        }
        for(PropertyField Prop : Table.Properties)
            CreateString = CreateString+',' + Prop.GetDeclare(syntax);

        try {
            execute("DROP TABLE "+Table.Name+" CASCADE CONSTRAINTS");
        } catch (SQLException e) {
            e.getErrorCode();
        }

        try {
            execute(syntax.getCreateSessionTable(Table.Name,CreateString,"CONSTRAINT PK_S_" + ID +"_T_" + Table.Name + " PRIMARY KEY " + syntax.getClustered() + " (" + KeyString + ")"));
        } catch (SQLException e) {
            e.getErrorCode();
        }
    }

    void execute(String ExecuteString) throws SQLException {
        Statement Statement = connection.createStatement();
//        System.out.println(ExecuteString+syntax.getCommandEnd());
        try {
            Statement.execute(ExecuteString+ syntax.getCommandEnd());
//        } catch(SQLException e) {
//            if(!ExecuteString.startsWith("DROP") && !ExecuteString.startsWith("CREATE")) {
//            System.out.println(ExecuteString+Syntax.getCommandEnd());
//            e = e;
//           }
        } finally {
            Statement.close();
        }
        if(!InTransaction && syntax.noAutoCommit())
            Statement.execute(syntax.commitTransaction()+ syntax.getCommandEnd());

        try {
            Statement.close();
        } catch (SQLException e) {
            e.getErrorCode();
        }
    }

    void insertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<PropertyField,Object> PropFields) throws SQLException {

        String InsertString = "";
        String ValueString = "";

        // пробежим по KeyFields'ам
        for(KeyField Key : Table.keys) {
            InsertString = (InsertString.length()==0?"":InsertString+',') + Key.Name;
            ValueString = (ValueString.length()==0?"":ValueString+',') + KeyFields.get(Key);
        }

        // пробежим по Fields'ам
        for(PropertyField Prop : PropFields.keySet()) {
            InsertString = InsertString+","+Prop.Name;
            ValueString = ValueString+","+TypedObject.getString(PropFields.get(Prop),Prop.type, syntax);
        }

        execute("INSERT INTO "+Table.getName(syntax)+" ("+InsertString+") VALUES ("+ValueString+")");
    }

    void UpdateInsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<PropertyField,Object> PropFields) throws SQLException {

        // по сути пустое кол-во ключей
        JoinQuery<Object,String> IsRecQuery = new JoinQuery<Object,String>(new ArrayList<Object>());

        Map<KeyField,ValueExpr> KeyExprs = new HashMap<KeyField,ValueExpr>();
        for(KeyField Key : Table.keys)
            KeyExprs.put(Key,new ValueExpr(KeyFields.get(Key),Key.type));

        // сначала закинем KeyField'ы и прогоним Select
        IsRecQuery.and(new Join<KeyField,PropertyField>(Table,KeyExprs).inJoin);

        if(IsRecQuery.executeSelect(this).size()>0) {
            JoinQuery<KeyField,PropertyField> UpdateQuery = new JoinQuery<KeyField, PropertyField>(Table.keys);
            UpdateQuery.putKeyWhere(KeyFields);
            for(Map.Entry<PropertyField,Object> MapProp : PropFields.entrySet())
                UpdateQuery.properties.put(MapProp.getKey(), MapProp.getKey().type.getExpr(MapProp.getValue()));

            // есть запись нужно Update лупить
            UpdateRecords(new ModifyQuery(Table,UpdateQuery));
        } else
            // делаем Insert
            insertRecord(Table,KeyFields,PropFields);
    }

    void deleteKeyRecords(Table Table,Map<KeyField,Integer> Keys) throws SQLException {
 //       Execute(Table.GetDelete());
        String DeleteWhere = "";
        for(Map.Entry<KeyField,Integer> DeleteKey : Keys.entrySet())
            DeleteWhere = (DeleteWhere.length()==0?"":DeleteWhere+" AND ") + DeleteKey.getKey().Name + "=" + DeleteKey.getValue();

        execute("DELETE FROM "+Table.getName(syntax)+(DeleteWhere.length()==0?"":" WHERE "+DeleteWhere));
    }

    void UpdateRecords(ModifyQuery Modify) throws SQLException {
//        try {
            execute(Modify.getUpdate(syntax));
//        } catch(SQLException e) {
//            Execute(Modify.getUpdate(Syntax));
//        }
    }

    void InsertSelect(ModifyQuery Modify) throws SQLException {
        execute(Modify.getInsertSelect(syntax));
    }

    // сначала делает InsertSelect, затем UpdateRecords
    void modifyRecords(ModifyQuery modify) throws SQLException {
        execute(modify.getInsertLeftKeys(syntax));
        execute(modify.getUpdate(syntax));
    }

    void close() throws SQLException {
        connection.close();
    }

    public boolean hasChanges() {
        return changes.hasChanges();
    }

    int CursorCounter = 0;
    public String getCursorName() {
        return "cursor"+(CursorCounter++);
    }
}

class MapRead<P extends PropertyInterface> {
    // делает разные вызовы Changed/Main
    SourceExpr getImplementExpr(PropertyInterfaceImplement<P> Implement, Map<P, ? extends SourceExpr> JoinImplement, InterfaceClassSet<P> JoinClasses) {
        return Implement.mapSourceExpr(JoinImplement, JoinClasses);
    }

    <M extends PropertyInterface,OM> void fillMapExpr(JoinQuery<P, OM> query, OM value, Property<M> mapProperty, Map<M, ? extends SourceExpr> joinImplement, Map<PropertyInterfaceImplement<P>, SourceExpr> implementExprs, InterfaceClassSet<M> joinClasses) {
        SourceExpr ValueExpr = mapProperty.getSourceExpr(joinImplement, joinClasses);
        query.properties.put(value, ValueExpr);
        query.and(ValueExpr.getWhere());
    }

    // разные классы считывает

    ClassSet getImplementValueClass(PropertyInterfaceImplement<P> Implement,InterfaceClass<P> ClassImplement) {
        return Implement.mapValueClass(ClassImplement);
    }

    InterfaceClassSet<P> getImplementClassSet(PropertyInterfaceImplement<P> Implement, ClassSet ReqValue) {
        return Implement.mapClassSet(ReqValue);
    }

    <M extends PropertyInterface> ValueClassSet<M> getMapChangeClass(Property<M> MapProperty) {
        return MapProperty.getValueClassSet();
    }
}

class MapChangedRead<P extends PropertyInterface> extends MapRead<P> {

    MapChangedRead(DataSession iSession, boolean iMapChanged, int iMapType, int iImplementType, Collection<PropertyInterfaceImplement<P>> iImplementChanged) {
        session = iSession;
        mapChanged = iMapChanged;
        mapType = iMapType;
        ImplementType = iImplementType;
        ImplementChanged = iImplementChanged;
    }

    MapChangedRead(DataSession iSession, boolean iMapChanged, int iMapType, int iImplementType, PropertyInterfaceImplement<P> iImplementChanged) {
        this(iSession,iMapChanged,iMapType,iImplementType,Collections.singleton(iImplementChanged));
    }

    DataSession session;

    boolean mapChanged;
    // 0 - J P
    // 1 - просто NULL
    // 2 - NULL JOIN P (то есть Join'им но null'им)
    int mapType;

    Collection<PropertyInterfaceImplement<P>> ImplementChanged;
    int ImplementType;

    // проверяет изменились ли вообще то что запрашивается
    <M extends PropertyInterface> boolean check(Property<M> MapProperty) {
        for(PropertyInterfaceImplement<P> Implement : ImplementChanged)
            if(!Implement.mapHasChanges(session)) return false;
        return !(mapChanged && !session.propertyChanges.containsKey(MapProperty));
    }

    // делает разные вызовы Changed/Main
    SourceExpr getImplementExpr(PropertyInterfaceImplement<P> Implement, Map<P, ? extends SourceExpr> JoinImplement, InterfaceClassSet<P> JoinClasses) {
        if(ImplementChanged.contains(Implement))
            return Implement.mapChangeExpr(session, JoinImplement, ImplementType);
        else
            return super.getImplementExpr(Implement, JoinImplement, JoinClasses);    //To change body of overridden methods use File | Settings | File Templates.
    }

    // ImplementExprs специально для MapType==1
    <M extends PropertyInterface,OM> void fillMapExpr(JoinQuery<P, OM> query, OM value, Property<M> mapProperty, Map<M, ? extends SourceExpr> joinImplement, Map<PropertyInterfaceImplement<P>, SourceExpr> implementExprs, InterfaceClassSet<M> joinClasses) {

        // закинем всем условия на Implement'ы (Join'у нужно только для !MapChanged и MapType==1, но переживет)
        for(Map.Entry<PropertyInterfaceImplement<P>,SourceExpr> ImplementExpr : implementExprs.entrySet())
            if(ImplementChanged.contains(ImplementExpr.getKey())) // нужно закинуть не Changed'ы на notNull, а Changed'ы на InJoin
                query.and(((JoinExpr)ImplementExpr.getValue()).from.inJoin);
            else
                query.and(ImplementExpr.getValue().getWhere());

        if(mapChanged) {
            JoinExpr mapExpr = session.getChange(mapProperty).getExpr(joinImplement, mapType ==3?0: mapType);
            query.and(mapExpr.from.inJoin);
            SourceExpr expr = (mapType ==3?mapExpr.getType().getExpr(null):mapExpr);
            query.properties.put(value, expr);
        } else {
            if(mapType ==1) // просто null кидаем
                query.properties.put(value, mapProperty.getType().getExpr(null));
            else {
                SourceExpr mapExpr = mapProperty.getSourceExpr(joinImplement, joinClasses);
                query.and(mapExpr.getWhere());
                SourceExpr expr = (mapType ==2?mapExpr.getType().getExpr(null):mapExpr);
                query.properties.put(value, expr);
            }
        }
    }

    ClassSet getImplementValueClass(PropertyInterfaceImplement<P> Implement, InterfaceClass<P> ClassImplement) {
        if(ImplementChanged.contains(Implement) && ImplementType!=2) {
            return Implement.mapChangeValueClass(session, ClassImplement);
        } else
            return super.getImplementValueClass(Implement, ClassImplement);    //To change body of overridden methods use File | Settings | File Templates.
    }

    InterfaceClassSet<P> getImplementClassSet(PropertyInterfaceImplement<P> Implement, ClassSet ReqValue) {
        if(ImplementChanged.contains(Implement)) {
            if(ImplementType==2) // если ImplementType=2 то And'им базовый класс с новыми
                return Implement.mapClassSet(ClassSet.universal).and(Implement.mapChangeClassSet(session, ClassSet.universal));
            else
                return Implement.mapChangeClassSet(session, ReqValue);
        } else
            return super.getImplementClassSet(Implement, ReqValue);    //To change body of overridden methods use File | Settings | File Templates.
    }

    <M extends PropertyInterface> ValueClassSet<M> getMapChangeClass(Property<M> MapProperty) {
        if(mapChanged) {
            ValueClassSet<M> MapChange = session.getChange(MapProperty).classes;
            if(mapType >=2) // если старые затираем возвращаем ссылку на nullClass
                return new ValueClassSet<M>(new ClassSet(), MapChange.getClassSet(ClassSet.universal));
            else
                return MapChange;
        } else {
            if(mapType ==2) // если 2 то NullClass
                return new ValueClassSet<M>(new ClassSet(), MapProperty.getClassSet(ClassSet.universal));
            else
            if(mapType ==1)
                return new ValueClassSet<M>(new ClassSet(),MapProperty.getUniversalInterface());
            else
                return super.getMapChangeClass(MapProperty);
        }
    }
}

// св-ва которые связывают другие св-ва друг с другом
// ClassInterface = T - Join, M - Group
// ImplementClass = M - Join, T - Group
// ObjectMapClass = PropertyField - Join, Object - Group
abstract class MapProperty<T extends PropertyInterface,M extends PropertyInterface,IN extends PropertyInterface,IM extends PropertyInterface,OM> extends AggregateProperty<T> {

    MapProperty(TableFactory iTableFactory) {
        super(iTableFactory);
        DBRead = new MapRead<IN>();
    }

    // получает св-во для Map'а
    // Join - return Implements.Property
    // Group - return GroupProperty
    abstract Property<M> getMapProperty();

    // получает список имплементаций
    // Join - return Implements.Mapping
    // Group бежит по GroupPropertyInterface и возвращает сформированный Map
    abstract Map<IM,PropertyInterfaceImplement<IN>> getMapImplements();

    // получает список интерфейсов
    // Join - return Interfaces
    // Group - return GroupProperty.Interfaces
    abstract Collection<IN> getMapInterfaces();

    abstract void fillChangedRead(UnionQuery<IN, OM> listQuery, OM value, MapChangedRead<IN> read, ValueClassSet<T> readClasses);

    // получает св-ва для запроса
    abstract Map<OM, Type> getMapNullProps(OM Value);

    // ВЫПОЛНЕНИЕ СПИСКА ИТЕРАЦИЙ

    JoinQuery<IN, OM> getMapQuery(List<MapChangedRead<IN>> readList, OM value, ValueClassSet<T> readClass, boolean sum) {

        // делаем getQuery для всех итераций, после чего Query делаем Union на 3, InterfaceClassSet на AND(*), Value на AND(*)
        UnionQuery<IN, OM> ListQuery = sum?new OperationQuery<IN, OM>(getMapInterfaces(),Union.SUM):new ChangeQuery<IN, OM>(getMapInterfaces());
        for(MapChangedRead<IN> Read : readList)
            if(Read.check(getMapProperty()))
                fillChangedRead(ListQuery, value, Read, readClass);
        if(ListQuery.where.isFalse())
            for(Map.Entry<OM,Type> nullProp : getMapNullProps(value).entrySet())
                ListQuery.properties.put(nullProp.getKey(), nullProp.getValue().getExpr(null));

        return ListQuery;
    }

    // get* получают списки итераций чтобы потом отправить их на выполнение:

    List<MapChangedRead<IN>> getImplementSet(DataSession Session, List<PropertyInterfaceImplement<IN>> SubSet, int ImplementType, boolean NotNull) {
        List<MapChangedRead<IN>> Result = new ArrayList<MapChangedRead<IN>>();
        if(ImplementType==2) // DEBUG
            throw new RuntimeException("по идее не должно быть");
        if(!(NotNull && SubSet.size()==1)) { // сначала "зануляем" ( пропускаем NotNull только одной размерности, теоретически можно доказать(
            if(implementAllInterfaces()) // просто без Join'a
                Result.add(new MapChangedRead<IN>(Session, false, 1, ImplementType, SubSet));
            else {
                Result.add(new MapChangedRead<IN>(Session, true, 3, 2, SubSet));
                Result.add(new MapChangedRead<IN>(Session, false, 2, 2, SubSet));
            }
        }
        // затем Join'им
        Result.add(new MapChangedRead<IN>(Session, false, 0, ImplementType, SubSet));

        return Result;
    }

    // новое состояние
    List<MapChangedRead<IN>> getChange(DataSession Session,int MapType) {
        List<MapChangedRead<IN>> ChangedList = new ArrayList<MapChangedRead<IN>>();
        for(List<PropertyInterfaceImplement<IN>> SubSet : SetBuilder.buildSubSetList(getMapImplements().values())) {
            if(SubSet.size()>0)
                ChangedList.addAll(getImplementSet(Session, SubSet, 0, false));
            ChangedList.add(new MapChangedRead<IN>(Session,true,MapType,0,SubSet));
        }
        return ChangedList;
    }

    // новое состояние с измененным основным значением
    // J - C (0,1) - SS+ (0)
    List<MapChangedRead<IN>> getChangeMap(DataSession Session, int MapType) {
        List<MapChangedRead<IN>> ChangedList = new ArrayList<MapChangedRead<IN>>();
        for(List<PropertyInterfaceImplement<IN>> SubSet : SetBuilder.buildSubSetList(getMapImplements().values()))
            ChangedList.add(new MapChangedRead<IN>(Session,true,MapType,0,SubSet));
        return ChangedList;
    }
    // новое значение для имплементаций, здесь если не все имплементации придется извращаться и exclude'ать все не измененные выражения
    // LJ - P - SS (0,1)
    List<MapChangedRead<IN>> getChangeImplements(DataSession Session,int ImplementType) {
        List<MapChangedRead<IN>> ChangedList = new ArrayList<MapChangedRead<IN>>();
        for(List<PropertyInterfaceImplement<IN>> SubSet : SetBuilder.buildSubSetList(getMapImplements().values()))
            if(SubSet.size()>0)
                ChangedList.addAll(getImplementSet(Session, SubSet, ImplementType, true));

        return ChangedList;
    }
    // предыдущие значения по измененным объектам
    // J - P - L(2)
    List<MapChangedRead<IN>> getPreviousImplements(DataSession Session) {
        List<MapChangedRead<IN>> ChangedList = new ArrayList<MapChangedRead<IN>>();
        for(PropertyInterfaceImplement<IN> Implement : getMapImplements().values())
            ChangedList.add(new MapChangedRead<IN>(Session,false,0,2,Implement));
        return ChangedList;
    }
    // предыдущие значения
    List<MapChangedRead<IN>> getPreviousChange(DataSession Session) {
        List<MapChangedRead<IN>> ChangedList = getPreviousImplements(Session);
        ChangedList.add(new MapChangedRead<IN>(Session,true,2,0,new ArrayList<PropertyInterfaceImplement<IN>>()));
        return ChangedList;
    }
    // чтобы можно было бы использовать в одном списке
    MapChangedRead<IN> getPrevious(DataSession Session) {
        return new MapChangedRead<IN>(Session,false,0,0,new ArrayList<PropertyInterfaceImplement<IN>>());
    }
    // значение из базы (можно и LJ)
    // J - P - P
    MapRead<IN> DBRead;

    // получает источник для данных
/*    abstract OM getDefaultObject();
    abstract Source<T,OM> getMapSourceQuery(OM Value);

    SourceExpr calculateSourceExpr(Map<T, SourceExpr> JoinImplement, InterfaceClassSet<T> JoinClasses, boolean NotNull) {
        OM Value = getDefaultObject();
        return (new Join<T,OM>(getMapSourceQuery(Value),JoinImplement,NotNull)).Exprs.get(Value);
    }*/

    // заполняет список, возвращает есть ли изменения
    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        if(ChangedProperties.contains(this)) return true;
        if(NoUpdate.contains(this)) return false;

        boolean Changed = getMapProperty().fillChangedList(ChangedProperties, Changes, NoUpdate);

        for(PropertyInterfaceImplement Implement : getMapImplements().values())
            Changed = Implement.mapFillChangedList(ChangedProperties, Changes, NoUpdate) || Changed;

        if(Changed)
            ChangedProperties.add(this);

        return Changed;
    }

    boolean containsImplement(Collection<Property> Properties) {
        for(PropertyInterfaceImplement Implement : getMapImplements().values())
            if(Implement instanceof PropertyMapImplement && Properties.contains(((PropertyMapImplement)Implement).property))
                return true;
        return false;
    }

    boolean implementAllInterfaces() {

        if(getMapProperty() instanceof WhereFormulaProperty) return false;

        Set<PropertyInterface> ImplementInterfaces = new HashSet<PropertyInterface>();
        for(PropertyInterfaceImplement<IN> InterfaceImplement : getMapImplements().values()) {
            if(InterfaceImplement instanceof PropertyMapImplement)
                ImplementInterfaces.addAll(((PropertyMapImplement<?,IN>)InterfaceImplement).mapping.values());
        }

        return ImplementInterfaces.size()==getMapInterfaces().size();
    }
}
