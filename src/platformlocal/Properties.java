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
    Integer idObject;
    Class Class;

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectValue that = (ObjectValue) o;

        if (!idObject.equals(that.idObject)) return false;

        return true;
    }

    public int hashCode() {
        return idObject.hashCode();
    }

    ObjectValue(Integer iObject,Class iClass) {idObject=iObject;Class=iClass;}
}

class PropertyImplement<T> {
    
    PropertyImplement(Property iProperty) {
        Property = iProperty;
        Mapping = new HashMap<PropertyInterface,T>();
    }
    
    Property Property;
    Map<PropertyInterface,T> Mapping;
}

interface PropertyInterfaceImplement {

    public SourceExpr mapSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull);
    public SourceExpr mapChangedExpr(Map<PropertyInterface, SourceExpr> JoinImplement, DataSession Session, int Value);
    public Class MapGetValueClass(InterfaceClass ClassImplement);
    public InterfaceClassSet MapGetClassSet(Class ReqValue);

    // для increment'ного обновления
    public boolean MapHasChanges(DataSession Session);

    abstract boolean mapFillChangedList(List<Property> ChangedProperties, DataChanges Changes);

    DumbImplementChange mapDumbValue(MapChangedRead Read, DumbInterfaceChange<?> DumbInterface);
}


class PropertyInterface implements PropertyInterfaceImplement {
    //можно использовать JoinExps потому как все равну вернуться она не может потому как иначе она зациклится

    public SourceExpr mapSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {
        return JoinImplement.get(this);
    }

    public SourceExpr mapChangedExpr(Map<PropertyInterface, SourceExpr> JoinImplement, DataSession Session, int Value) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Class MapGetValueClass(InterfaceClass ClassImplement) {
        return ClassImplement.get(this);
    }

    public InterfaceClassSet MapGetClassSet(Class ReqValue) {
        InterfaceClassSet Result = new InterfaceClassSet();
        InterfaceClass ResultClass = new InterfaceClass();
        if(ReqValue!=null)
           ResultClass.put(this,ReqValue);
        Result.add(ResultClass);


        return Result;
    }

    public boolean MapHasChanges(DataSession Session) {
        return false;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean mapFillChangedList(List<Property> ChangedProperties, DataChanges Changes) {
        return false;
    }

    public DumbImplementChange mapDumbValue(MapChangedRead Read, DumbInterfaceChange<?> DumbInterface) {
        return DumbInterface.get(this);
    }
}

class PropertyNode {

    PropertyGroup parent;
    PropertyGroup getParent() { return parent; }

}

class PropertyGroup extends PropertyNode{

    String caption;

    PropertyGroup(String icaption) {
        caption = icaption;
    }

    Collection<PropertyNode> properties = new ArrayList<PropertyNode>();
    void add(PropertyNode prop) {
        properties.add(prop);
        prop.parent = this;
    }

}

abstract class Property<T extends PropertyInterface> extends PropertyNode {

    int ID=0;

    TableFactory TableFactory;

    Property(TableFactory iTableFactory) {
        TableFactory = iTableFactory;
    }

    // чтобы подчеркнуть что не направленный
    Collection<T> Interfaces = new ArrayList();
    // кэшируем здесь а не в JoinList потому как быстрее
    // работает только для JOIN смотри ChangedJoinSelect
    Map<Map<PropertyInterface,SourceExpr>,SourceExpr> SelectCacheJoins = new HashMap();

    // закэшируем чтобы быстрее работать
    // здесь как и в произвольных Left значит что могут быть null, не Left соответственно только не null
    // (пока в нашем случае просто можно убирать записи где точно null)
    public SourceExpr getSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        // не будем проверять что все интерфейсы реализованы все равно null в map не попадет
        SourceExpr JoinExpr = SelectCacheJoins.get(JoinImplement);
        if(JoinExpr==null) {
            if(IsPersistent()) {
                // если persistent читаем из таблицы
                Map<KeyField,T> MapJoins = new HashMap();
                Table SourceTable = GetTable(MapJoins);

                // прогоним проверим все ли Implement'ировано
                Join<KeyField,PropertyField> SourceJoin = new Join<KeyField,PropertyField>(SourceTable, NotNull);
                for(KeyField Key : SourceTable.Keys)
                    SourceJoin.Joins.put(Key, JoinImplement.get(MapJoins.get(Key)));

                JoinExpr = SourceJoin.Exprs.get(Field);
            } else
                JoinExpr = ((AggregateProperty)this).calculateSourceExpr(JoinImplement, NotNull);

            SelectCacheJoins.put(JoinImplement,JoinExpr);
        }

        return JoinExpr;
    }

    // возвращает класс значения
    // если null то не подходит по интерфейсу
    abstract public Class GetValueClass(InterfaceClass ClassImplement);

    // возвращает то и только то мн-во интерфейсов которые заведомо дают этот интерфейс (GetValueClass >= ReqValue)
    // если null то когда в принципе дает значение
    abstract public InterfaceClassSet GetClassSet(Class ReqValue);

    // получает базовый класс чтобы определять
    Class getBaseClass() {
        InterfaceClassSet ClassSet = GetClassSet(null);
        if(ClassSet.size()>0)
            return GetValueClass(ClassSet.get(0));
        else
            return null;
    }

    public Type getType() {
        return getBaseClass().getType();
    }

    String caption = "";

    // заполняет список, возвращает есть ли изменения
    abstract boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes);

    JoinQuery<PropertyInterface,String> getOutSelect(String Value) {
        JoinQuery<PropertyInterface,String> Query = new JoinQuery(Interfaces);
        SourceExpr ValueExpr = getSourceExpr(Query.MapKeys, true);
        Query.add(Value, ValueExpr);
        Query.add(new SourceIsNullWhere(ValueExpr,true));
        return Query;
    }

    void Out(DataSession Session) throws SQLException {
        System.out.println(caption);
        getOutSelect("value").outSelect(Session);
    }

    boolean isObject() {
        // нужно также проверить
        for(InterfaceClass InterfaceClass : GetClassSet(null))
            for(Class Interface : InterfaceClass.values())
                if(!(Interface instanceof ObjectClass))
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
    Map<T,KeyField> ChangeTableMap = null;
    // раз уж ChangeTableMap закэшировали то и ChangeTable тоже
    IncrementChangeTable ChangeTable;

    void FillChangeTable() {
        ChangeTable = TableFactory.GetChangeTable(Interfaces.size(), getType());
        ChangeTableMap = new HashMap();
        Iterator<KeyField> io = ChangeTable.Objects.iterator();
        for(T Interface : Interfaces)
            ChangeTableMap.put(Interface,io.next());
    }

    void OutChangesTable(DataSession Session) throws SQLException {
        JoinQuery<T,PropertyField> Query = new JoinQuery<T,PropertyField>(Interfaces);

        Join<KeyField,PropertyField> ChangeJoin = new MapJoin<KeyField,PropertyField,T>(ChangeTable,Query,ChangeTableMap,true);
        ChangeJoin.Joins.put(ChangeTable.Property,new ValueSourceExpr(ID,ChangeTable.Property.Type));

        Query.add(ChangeTable.Value,ChangeJoin.Exprs.get(ChangeTable.Value));
        Query.add(ChangeTable.PrevValue,ChangeJoin.Exprs.get(ChangeTable.PrevValue));

        Query.outSelect(Session);
    }

    PropertyField Field;
    abstract Table GetTable(Map<KeyField,T> MapJoins);

    boolean IsPersistent() {
        return Field!=null && !(this instanceof AggregateProperty && TableFactory.ReCalculateAggr); // для тестирования 2-е условие
    }

    // базовые методы - ничего не делать, его перегружают только Override и Data
    boolean allowChangeProperty(Map<PropertyInterface, ObjectValue> Keys) { return false; }
    void ChangeProperty(Map<PropertyInterface, ObjectValue> Keys, Object NewValue, DataSession Session) throws SQLException {}

    // заполняет требования к изменениям
    abstract void fillRequiredChanges(Map<Property, Integer> RequiredTypes);

    // для каскадного выполнения (запрос)
    boolean XL = false;

    // получает запрос для инкрементных изменений
    abstract Change incrementChanges(DataSession Session, int ChangeType);

    // присоединяют объекты
    void joinChangeClass(ChangeClassTable Table,JoinQuery<? extends PropertyInterface,?> Query, DataSession Session,DataPropertyInterface Interface) {
        Join<KeyField,PropertyField> ClassJoin = new Join<KeyField,PropertyField>(Table.getClassJoin(Session,Interface.Class),true);
        ClassJoin.Joins.put(Table.Object,Query.MapKeys.get(Interface));
        Query.add(ClassJoin);
    }

    void joinObjects(JoinQuery<? extends PropertyInterface,?> Query,DataPropertyInterface Interface) {
        Join<KeyField,PropertyField> ClassJoin = new Join<KeyField,PropertyField>(TableFactory.ObjectTable.getClassJoin(Interface.Class),true);
        ClassJoin.Joins.put(TableFactory.ObjectTable.Key,Query.MapKeys.get(Interface));
        Query.add(ClassJoin);
    }

    // isRequired (InterfaceAddClasses) - обязателен ли на вход соответствующий список классов
    boolean isRequired(InterfaceAddClasses<? extends PropertyInterface> InterfaceClasses) {
        //      теоретически getClassSet на null, а затем проверить для каждого интерфейса нету ли этого класса в каждом из Interface'ов
        return false;
    }

    class Change {
        int Type; // && 0 - =, 1 - +, 2 - и новое и предыдущее
        Source<T,PropertyField> Source;
        DumbChange<T> Dumb;

        Change(int iType, Source<T, PropertyField> iSource, DumbChange iDumb) {
            Type = iType;
            Source = iSource;
            Dumb = iDumb;
        }

        // подгоняет к Type'у
        void correct(int RequiredType) {
            // проверим что вернули что вернули то что надо, "подчищаем" если не то
            // если вернул 2 запишем
            if(Type==2 || (Type!=RequiredType))
                RequiredType = 2;

            if(Type != RequiredType) {
                JoinQuery<T,PropertyField> NewQuery = new JoinQuery<T,PropertyField>(Interfaces);
                SourceExpr NewExpr = (new UniJoin<T,PropertyField>(Source,NewQuery,true)).Exprs.get(ChangeTable.Value);
                // нужно LEFT JOIN'ить старые
                SourceExpr PrevExpr = getSourceExpr((Map<PropertyInterface,SourceExpr>) NewQuery.MapKeys,false);
                // по любому 2 нету надо докинуть
                NewQuery.add(ChangeTable.PrevValue,PrevExpr);
                if(Type==1) {
                    // есть 1, а надо по сути 0
                    UnionSourceExpr SumExpr = new UnionSourceExpr(1);
                    SumExpr.Operands.put(NewExpr,1);
                    SumExpr.Operands.put(PrevExpr,1);
                    NewExpr = SumExpr;
                }
                NewQuery.add(ChangeTable.Value,NewExpr);

                Source = NewQuery;
                Type = RequiredType;
            }
        }

        // сохраняет в инкрементную таблицу
        void save(DataSession Session) throws SQLException {

            Map<KeyField,Integer> ValueKeys = new HashMap();
            ValueKeys.put(ChangeTable.Property,ID);
            Session.deleteKeyRecords(ChangeTable,ValueKeys);

            // откуда читать
            JoinQuery<T,PropertyField> ReadQuery = new JoinQuery<T,PropertyField>(Interfaces);
            Join<KeyField,PropertyField> ReadJoin = new MapJoin<KeyField,PropertyField,T>(ChangeTable,ReadQuery,ChangeTableMap,true);
            ReadJoin.Joins.put(ChangeTable.Property,new ValueSourceExpr(ID,ChangeTable.Property.Type));

            JoinQuery<KeyField,PropertyField> WriteQuery = new JoinQuery<KeyField,PropertyField>(ChangeTable.Keys);
            Join<T,PropertyField> WriteJoin = new MapJoin<T,PropertyField,KeyField>(Source,ChangeTableMap,WriteQuery,true);
            WriteQuery.putDumbJoin(ValueKeys);

            WriteQuery.add(ChangeTable.Value,WriteJoin.Exprs.get(ChangeTable.Value));
            ReadQuery.add(ChangeTable.Value,ReadJoin.Exprs.get(ChangeTable.Value));
            if(Type==2) {
                WriteQuery.add(ChangeTable.PrevValue,WriteJoin.Exprs.get(ChangeTable.PrevValue));
                ReadQuery.add(ChangeTable.PrevValue,ReadJoin.Exprs.get(ChangeTable.PrevValue));
            }

            Session.InsertSelect(new ModifyQuery(ChangeTable,WriteQuery));

            Source = ReadQuery;
        }

        // сохраняет в базу
        void apply(DataSession Session) throws SQLException {

            // если не изменились ничего не делаем
            Map<KeyField,T> MapKeys = new HashMap();
            Table SourceTable = GetTable(MapKeys);

            JoinQuery<KeyField,PropertyField> ModifyQuery = new JoinQuery<KeyField,PropertyField>(SourceTable.Keys);

            Join<T,PropertyField> Update = new Join<T,PropertyField>(Source,true);
            for(KeyField Key : SourceTable.Keys)
                Update.Joins.put(MapKeys.get(Key),ModifyQuery.MapKeys.get(Key));

            ModifyQuery.add(Field,Update.Exprs.get(ChangeTable.Value));
            Session.ModifyRecords(new ModifyQuery(SourceTable,ModifyQuery));
        }

        // подготавливает Join примененных изменений
        Join<? extends Object,PropertyField> getJoin(Map<PropertyInterface, SourceExpr> JoinImplement, boolean Inner) {
            return new Join<PropertyInterface,PropertyField>((Source<PropertyInterface,PropertyField>) Source,JoinImplement,Inner);
        }

        // связывает именно измененные записи из сессии
        // Value - что получать, 0 - новые значения, 1 - +(увеличение), 2 - старые значения
        SourceExpr getExpr(Map<PropertyInterface,SourceExpr> JoinImplement, int Value) {

            // теперь определимся что возвращать
            if(Value==2 && Type==2)
                return getJoin(JoinImplement,true).Exprs.get(ChangeTable.PrevValue);

            if(Value==Type || (Value==0 && Type==2))
                return getJoin(JoinImplement,true).Exprs.get(ChangeTable.Value);

            if(Value==1 && Type==2) {
                UnionSourceExpr Result = new UnionSourceExpr(1);
                Result.Operands.put(getJoin(JoinImplement,true).Exprs.get(ChangeTable.Value),1);
                Result.Operands.put(getJoin(JoinImplement,true).Exprs.get(ChangeTable.PrevValue),-1);
                return Result;
            }

            throw new RuntimeException();
        }
    }
}

class DataPropertyInterface extends PropertyInterface {
    Class Class;

    DataPropertyInterface(Class iClass) {
        Class = iClass;
    }
}


class DataProperty extends Property<DataPropertyInterface> {
    Class Value;

    DataProperty(TableFactory iTableFactory,Class iValue) {
        super(iTableFactory);
        Value = iValue;

        DefaultMap = new HashMap();
    }

    // при текущей реализации проше предполагать что не имплементнутые Interface имеют null Select !!!!!
    Table GetTable(Map<KeyField,DataPropertyInterface> MapJoins) {
        return TableFactory.GetTable(Interfaces,MapJoins);
    }

    public Class GetValueClass(InterfaceClass ClassImplement) {
        // пока так потом сделаем перегрузку по классам
        // если не тот класс сразу зарубаем
        for(DataPropertyInterface DataInterface : Interfaces)
            if(!ClassImplement.get(DataInterface).IsParent(DataInterface.Class))
                return null;

        return Value;
    }

    public InterfaceClassSet GetClassSet(Class ReqValue) {
        InterfaceClassSet Result = new InterfaceClassSet();

        if(ReqValue==null || Value.IsParent(ReqValue)) {
            InterfaceClass ResultInterface = new InterfaceClass();
            for(DataPropertyInterface Interface : Interfaces)
                ResultInterface.put(Interface, Interface.Class);
            Result.add(ResultInterface);
        }

        return Result;
    }

    // свойства для "ручных" изменений пользователем
    DataChangeTable DataTable;
    Map<KeyField,DataPropertyInterface> DataTableMap = null;

    void FillDataTable() {
        DataTable = TableFactory.GetDataChangeTable(Interfaces.size(), getType());
        // если нету Map'a построим
        DataTableMap = new HashMap();
        Iterator<KeyField> io = DataTable.Objects.iterator();
        for(DataPropertyInterface Interface : Interfaces)
            DataTableMap.put(io.next(),Interface);
    }

    void outDataChangesTable(DataSession Session) throws SQLException {
        DataTable.outSelect(Session);
    }

    @Override
    boolean allowChangeProperty(Map<PropertyInterface, ObjectValue> Keys) { return true; }

    @Override
    void ChangeProperty(Map<PropertyInterface, ObjectValue> Keys, Object NewValue, DataSession Session) throws SQLException {
        // записываем в таблицу изменений
        Map<KeyField,Integer> InsertKeys = new HashMap();
        for(KeyField Field : DataTableMap.keySet())
            InsertKeys.put(Field,Keys.get(DataTableMap.get(Field)).idObject);

        InsertKeys.put(DataTable.Property,ID);

        if(NewValue instanceof Integer && ((Integer)NewValue).equals(0))
            NewValue = null;

        if(NewValue instanceof String && ((String)NewValue).equals("0"))
            NewValue = null;

        Map<PropertyField,Object> InsertValues = new HashMap();
        InsertValues.put(DataTable.Value,NewValue);

        Session.UpdateInsertRecord(DataTable,InsertKeys,InsertValues);

        // пометим изменение св-ва
        Session.changeProperty(this);
    }

    // св-во по умолчанию (при AddClasses подставляется)
    Property DefaultProperty;
    // map интерфейсов на PropertyInterface
    Map<DataPropertyInterface,PropertyInterface> DefaultMap;
    // если нужно еще за изменениями следить и перебивать
    boolean OnDefaultChange;

    // заполняет список, возвращает есть ли изменения
    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes) {
        if(ChangedProperties.contains(this)) return true;
        // если null то значит полный список запрашивают
        if(Changes ==null) return true;

        boolean Changed = false;
        if(!Changed)
            if(Changes.Properties.contains(this)) Changed = true;

        if(!Changed)
            for(DataPropertyInterface Interface : Interfaces)
                if(Changes.RemoveClasses.contains(Interface.Class)) Changed = true;

        if(!Changed)
            if(Changes.RemoveClasses.contains(Value)) Changed = true;

        if(DefaultProperty!=null) {
            Changed = (DefaultProperty.fillChangedList(ChangedProperties, Changes) && OnDefaultChange) || Changed;

            if(!Changed)
                for(DataPropertyInterface Interface : Interfaces)
                    if(Changes.AddClasses.contains(Interface.Class)) Changed = true;
        }

        if(Changed) {
            ChangedProperties.add(this);
            return true;
        } else
            return false;
    }

    void fillRequiredChanges(Map<Property, Integer> RequiredTypes) {

        // если на изм. надо предыдущее изменение иначе просто на =
        // пока неясно после реализации QueryIncrementChanged станет яснее
        if(DefaultProperty!=null && RequiredTypes.containsKey(DefaultProperty))
            DefaultProperty.setChangeType(RequiredTypes,OnDefaultChange?2:0);
    }

    // заполним старыми значениями (LEFT JOIN'ом)
    Change incrementChanges(DataSession Session, int ChangeType) {

        // на 3 то есть слева/направо
        UnionQuery<DataPropertyInterface,PropertyField> ResultQuery = new UnionQuery<DataPropertyInterface,PropertyField>(Interfaces,3);

        // Default изменения (пока Add)
        if(DefaultProperty!=null) {
            if(!OnDefaultChange) {
                // бежим по всем добавленным интерфейсам
                for(DataPropertyInterface Interface : Interfaces)
                    if(Session.Changes.AddClasses.contains(Interface.Class)) {
                        JoinQuery<DataPropertyInterface,PropertyField> Query = new JoinQuery<DataPropertyInterface,PropertyField>(Interfaces);
                        Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
                        // "перекодируем" в базовый интерфейс
                        for(DataPropertyInterface DataInterface : Interfaces)
                            JoinImplement.put(DefaultMap.get(DataInterface),Query.MapKeys.get(DataInterface));

                        // вкидываем "новое" состояние DefaultProperty с Join'ое с AddClassTable
                        // если DefaultProperty требует на входе такой добавляемый интерфейс то можно чисто новое брать
                        joinChangeClass(TableFactory.AddClassTable,Query,Session,Interface);

                        Query.add(ChangeTable.Value,Session.getSourceExpr(DefaultProperty,JoinImplement,true));
                        ResultQuery.add(Query,1);
                    }
            } else {
                if(Session.PropertyChanges.containsKey(DefaultProperty)) {
                    JoinQuery<DataPropertyInterface,PropertyField> Query = new JoinQuery<DataPropertyInterface,PropertyField>(Interfaces);
                    Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
                    // "перекодируем" в базовый интерфейс
                    for(DataPropertyInterface DataInterface : Interfaces)
                        JoinImplement.put(DefaultMap.get(DataInterface),Query.MapKeys.get(DataInterface));

                    // по изменению св-ва
                    SourceExpr NewExpr = Session.PropertyChanges.get(DefaultProperty).getExpr(JoinImplement,0);
                    SourceExpr PrevExpr = Session.PropertyChanges.get(DefaultProperty).getExpr(JoinImplement,2);

                    Query.add(ChangeTable.Value,NewExpr);

                    NewExpr = new NullEmptySourceExpr(NewExpr);
                    PrevExpr = new NullEmptySourceExpr(PrevExpr);

                    // new, не равно prev
                    Query.add(new FieldExprCompareWhere(NewExpr,PrevExpr,FieldExprCompareWhere.NOT_EQUALS));
                    ResultQuery.add(Query,1);
                }
            }
        }

        JoinQuery<DataPropertyInterface,PropertyField> DataQuery = new JoinQuery<DataPropertyInterface,PropertyField>(Interfaces);

        // GetChangedFrom
        Join<KeyField,PropertyField> DataJoin = new MapJoin<KeyField,PropertyField,DataPropertyInterface>(DataTable,DataTableMap,DataQuery,true);
        DataJoin.Joins.put(DataTable.Property,new ValueSourceExpr(ID,DataTable.Property.Type));

        SourceExpr DataExpr = DataJoin.Exprs.get(DataTable.Value);
        DataQuery.add(ChangeTable.Value,DataExpr);

        for(DataPropertyInterface RemoveInterface : Interfaces) {
            if(Session.Changes.RemoveClasses.contains(RemoveInterface.Class)) {
                // те изменения которые были на удаляемые объекты исключаем
                TableFactory.RemoveClassTable.excludeJoin(DataQuery,Session,RemoveInterface.Class,DataQuery.MapKeys.get(RemoveInterface));

                // проверяем может кто удалился из интерфейса объекта
                JoinQuery<DataPropertyInterface,PropertyField> Query = new JoinQuery<DataPropertyInterface,PropertyField>(Interfaces);
                joinChangeClass(TableFactory.RemoveClassTable,Query,Session,RemoveInterface);
                // пока сделаем что наплевать на старое значение хотя конечно 2 раза может тоже не имеет смысл считать
                Query.add(ChangeTable.Value,new NullJoinSourceExpr(getSourceExpr((Map<PropertyInterface,SourceExpr>)(Map<? extends PropertyInterface,SourceExpr>)Query.MapKeys,true)));
                ResultQuery.add(Query,1);
            }
        }

        if(Session.Changes.RemoveClasses.contains(Value)) {
            // те изменения которые были на удаляемые объекты исключаем
            TableFactory.RemoveClassTable.excludeJoin(DataQuery,Session,Value,DataExpr);

            JoinQuery<DataPropertyInterface,PropertyField> Query = new JoinQuery<DataPropertyInterface,PropertyField>(Interfaces);
            Join<KeyField,PropertyField> RemoveJoin = new Join<KeyField,PropertyField>(TableFactory.RemoveClassTable.getClassJoin(Session,Value),true);
            RemoveJoin.Joins.put(TableFactory.RemoveClassTable.Object,getSourceExpr((Map<PropertyInterface,SourceExpr>)(Map<? extends PropertyInterface,SourceExpr>)Query.MapKeys,true));
            Query.add(RemoveJoin);
            Query.add(ChangeTable.Value,new ValueSourceExpr(null,ChangeTable.Value.Type));
            ResultQuery.add(Query,1);
        }

        // здесь именно в конце так как должна быть последней
        ResultQuery.add(DataQuery,1);

        return new Change(0,ResultQuery,new DumbChange<DataPropertyInterface>());
    }
}

abstract class AggregateProperty<T extends PropertyInterface> extends Property<T> {

    AggregateProperty(TableFactory iTableFactory) {super(iTableFactory);}

    Map<DataPropertyInterface,T> AggregateMap;

    // расчитывает выражение
    abstract SourceExpr calculateSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull);

    // сначала проверяет на persistence
    Table GetTable(Map<KeyField,T> MapJoins) {
        if(AggregateMap==null) {
            AggregateMap = new HashMap();
            if(GetClassSet(null).size()==0 || GetClassSet(null).get(0).containsValue(null))
                GetClassSet(null);
            InterfaceClass Parent = GetClassSet(null).GetCommonParent();
            for(T Interface : Interfaces) {
                AggregateMap.put(new DataPropertyInterface(Parent.get(Interface)),Interface);
            }
        }

        Map<KeyField,DataPropertyInterface> MapData = new HashMap();
        Table SourceTable = TableFactory.GetTable(AggregateMap.keySet(),MapData);
        // перекодирукм на MapJoins
        if(MapJoins!=null) {
            for(KeyField MapField : MapData.keySet())
                MapJoins.put(MapField,AggregateMap.get(MapData.get(MapField)));
        }

        return SourceTable;
    }

    Object dropZero(Object Value) {
        if(Value instanceof Integer && ((Integer)Value).equals(0)) return null;
        if(Value instanceof Long && ((Long)Value).intValue()==0) return null;
        return Value;
    }

    // проверяет аггрегацию для отладки
    boolean CheckAggregation(DataSession Session,String Caption) throws SQLException {
        JoinQuery<PropertyInterface, String> AggrSelect;
        AggrSelect = getOutSelect("value");
/*        if(caption.equals("остаток до операции") || caption.equals("OL 269")) {
            System.out.println("AGGR - "+caption);
            AggrSelect.outSelect(Session);
        }*/
        LinkedHashMap<Map<PropertyInterface,Integer>,Map<String,Object>> AggrResult = AggrSelect.executeSelect(Session);
        TableFactory.ReCalculateAggr = true;
        AggrSelect = getOutSelect("value");
/*        if(caption.equals("остаток до операции") || caption.equals("OL 269")) {
            System.out.println("RECALCULATE - "+caption);
            AggrSelect.outSelect(Session);
        }*/
        LinkedHashMap<Map<PropertyInterface,Integer>,Map<String,Object>> CalcResult = AggrSelect.executeSelect(Session);
        TableFactory.ReCalculateAggr = false;

        Iterator<Map.Entry<Map<PropertyInterface,Integer>,Map<String,Object>>> i = AggrResult.entrySet().iterator();
        while(i.hasNext()) {
            Map.Entry<Map<PropertyInterface,Integer>,Map<String,Object>> Row = i.next();
            Map<PropertyInterface, Integer> RowKey = Row.getKey();
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
            for(Map.Entry<Map<PropertyInterface,Integer>,Map<String,Object>> Row : AggrResult.entrySet())
                System.out.println(Row);
            System.out.println("----Calc-----");
            for(Map.Entry<Map<PropertyInterface,Integer>,Map<String,Object>> Row : CalcResult.entrySet())
                System.out.println(Row);
//
//            ((GroupProperty)this).outIncrementState(Session);
//            Session = Session;
        }

        return true;
    }

    void reCalculateAggregation(DataSession Session) throws SQLException {
        PropertyField WriteField = Field;
        Field = null;
        JoinQuery<PropertyInterface,PropertyField> ReCalculateQuery = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);
        ReCalculateQuery.add(WriteField,getSourceExpr(ReCalculateQuery.MapKeys,true));

        Map<KeyField,T> MapTable = new HashMap();
        Table AggrTable = GetTable(MapTable);

        JoinQuery<KeyField,PropertyField> WriteQuery = new JoinQuery<KeyField,PropertyField>(AggrTable.Keys);
        WriteQuery.add(WriteField,(new MapJoin<PropertyInterface,PropertyField,KeyField>(ReCalculateQuery,WriteQuery,(Map<KeyField,PropertyInterface>)MapTable,true).Exprs.get(WriteField)));
        Session.ModifyRecords(new ModifyQuery(AggrTable,WriteQuery));

        Field = WriteField;
    }
}

class ClassProperty extends AggregateProperty<DataPropertyInterface> {

    Class ValueClass;
    Object Value;

    ClassProperty(TableFactory iTableFactory, Class iValueClass, Object iValue) {
        super(iTableFactory);
        ValueClass = iValueClass;
        Value = iValue;
    }

    void fillRequiredChanges(Map<Property, Integer> RequiredTypes) {
        // этому св-ву чужого не надо
    }

    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes) {
        // если Value null то ничего не интересует
        if(Value==null) return false;
        if(ChangedProperties.contains(this)) return true;

        for(DataPropertyInterface ValueInterface : Interfaces)
            if(Changes ==null || Changes.AddClasses.contains(ValueInterface.Class) || Changes.RemoveClasses.contains(ValueInterface.Class)) {
                ChangedProperties.add(this);
                return true;
            }

        return false;
    }

    Change incrementChanges(DataSession Session, int ChangeType) {

        // работает на = и на + ему собсно пофигу, то есть сразу на 2

        // для любого изменения объекта на NEW можно определить PREV и соответственно
        // Set<Class> пришедшие, Set<Class> ушедшие
        // соответственно алгоритм бежим по всем интерфейсам делаем UnionQuery из SS изменений + старых объектов

        List<DataPropertyInterface> RemoveInterfaces = new ArrayList();
        for(DataPropertyInterface ValueInterface : Interfaces)
            if(Session.Changes.RemoveClasses.contains(ValueInterface.Class))
                RemoveInterfaces.add(ValueInterface);

        // конечный результат, с ключами и выражением
        UnionQuery<DataPropertyInterface,PropertyField> ResultQuery = new UnionQuery<DataPropertyInterface,PropertyField>(Interfaces,3);

        // для RemoveClass без SS все за Join'им (ValueClass пока трогать не будем (так как у значения пока не закладываем механизм изменений))
        for(DataPropertyInterface ChangedInterface : RemoveInterfaces) {
            JoinQuery<DataPropertyInterface,PropertyField> Query = new JoinQuery<DataPropertyInterface, PropertyField>(Interfaces);

            // RemoveClass + остальные из старой таблицы
            joinChangeClass(TableFactory.RemoveClassTable,Query,Session,ChangedInterface);
            for(DataPropertyInterface ValueInterface : Interfaces)
                if(ValueInterface!=ChangedInterface)
                    joinObjects(Query,ValueInterface);

            Query.add(ChangeTable.Value,new ValueSourceExpr(null,ChangeTable.Value.Type));
            Query.add(ChangeTable.PrevValue,new ValueSourceExpr(Value,ValueClass.getType()));

            ResultQuery.add(Query,1);
        }

        List<DataPropertyInterface> AddInterfaces = new ArrayList();
        for(DataPropertyInterface ValueInterface : Interfaces)
            if(Session.Changes.AddClasses.contains(ValueInterface.Class))
                AddInterfaces.add(ValueInterface);

        ListIterator<List<DataPropertyInterface>> il = (new SetBuilder<DataPropertyInterface>()).BuildSubSetList(AddInterfaces).listIterator();
        // пустое подмн-во не надо (как и в любой инкрементности)
        il.next();
        while(il.hasNext()) {
            List<DataPropertyInterface> ChangeProps = il.next();

            JoinQuery<DataPropertyInterface,PropertyField> Query = new JoinQuery<DataPropertyInterface, PropertyField>(Interfaces);

            for(DataPropertyInterface ValueInterface : Interfaces) {
                if(ChangeProps.contains(ValueInterface))
                    joinChangeClass(TableFactory.AddClassTable,Query,Session,ValueInterface);
                else {
                    joinObjects(Query,ValueInterface);

                    // здесь также надо проверить что не из RemoveClasses (то есть LEFT JOIN на null)
                    if(Session.Changes.RemoveClasses.contains(ValueInterface.Class))
                        TableFactory.RemoveClassTable.excludeJoin(Query,Session,ValueInterface.Class,Query.MapKeys.get(ValueInterface));
                }
            }

            Query.add(ChangeTable.PrevValue,new ValueSourceExpr(null,ChangeTable.PrevValue.Type));
            Query.add(ChangeTable.Value,new ValueSourceExpr(Value,ValueClass.getType()));

            ResultQuery.add(Query,1);
        }

        return new Change(2,ResultQuery,new DumbChange<DataPropertyInterface>());
    }

    SourceExpr calculateSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        String ValueString = "value";

        Source<PropertyInterface,String> Source;
        // если null то возвращает EmptySource
        if(Value==null)
            Source = new EmptySource<PropertyInterface,String>(Interfaces,ValueString,ValueClass.getType());
        else {
            JoinQuery<PropertyInterface,String> Query = new JoinQuery<PropertyInterface,String>(Interfaces);

            for(DataPropertyInterface ValueInterface : Interfaces)
                joinObjects(Query,ValueInterface);
            Query.add(ValueString,new ValueSourceExpr(Value,ValueClass.getType()));
            Source = Query;
        }

        return (new Join<PropertyInterface,String>(Source,JoinImplement,NotNull)).Exprs.get(ValueString);
    }

    public Class GetValueClass(InterfaceClass ClassImplement) {
        // аналогично DataProperty\только без перегрузки классов
        for(DataPropertyInterface ValueInterface : Interfaces)
            if(!ClassImplement.get(ValueInterface).IsParent(ValueInterface.Class))
                return null;

        return ValueClass;
    }

    public InterfaceClassSet GetClassSet(Class ReqValue) {
        // аналогично DataProperty\только без перегрузки классов
        InterfaceClassSet Result = new InterfaceClassSet();

        if(ReqValue==null || ValueClass.IsParent(ReqValue)) {
            InterfaceClass ResultInterface = new InterfaceClass();
            for(DataPropertyInterface ValueInterface : Interfaces)
                ResultInterface.put(ValueInterface, ValueInterface.Class);
            Result.add(ResultInterface);
        }

        return Result;
    }
}

class PropertyMapImplement extends PropertyImplement<PropertyInterface> implements PropertyInterfaceImplement {

    PropertyMapImplement(Property iProperty) {super(iProperty);}

    // NotNull только если сессии нету
    public SourceExpr mapSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {
        return Property.getSourceExpr(getMapImplement(JoinImplement),NotNull);
    }

    public SourceExpr mapChangedExpr(Map<PropertyInterface, SourceExpr> JoinImplement, DataSession Session, int Value) {
        return Session.PropertyChanges.get(Property).getExpr(getMapImplement(JoinImplement),Value);
    }

    private Map<PropertyInterface, SourceExpr> getMapImplement(Map<PropertyInterface, SourceExpr> JoinImplement) {
        Map<PropertyInterface,SourceExpr> MapImplement = new HashMap();
        for(PropertyInterface ImplementInterface : (Collection<PropertyInterface>)Property.Interfaces)
            MapImplement.put(ImplementInterface,JoinImplement.get(Mapping.get(ImplementInterface)));
        return MapImplement;
    }

    public Join mapChangedJoin(Map<PropertyInterface, SourceExpr> JoinImplement, DataSession Session, boolean Inner) {
        return Session.PropertyChanges.get(Property).getJoin(getMapImplement(JoinImplement),Inner);
    }

    public Class MapGetValueClass(InterfaceClass ClassImplement) {
        InterfaceClass MapImplement = new InterfaceClass();
        for(PropertyInterface ImplementInterface : (Collection<PropertyInterface>)Property.Interfaces)
            MapImplement.put(ImplementInterface,ClassImplement.get(Mapping.get(ImplementInterface)));

        return Property.GetValueClass(MapImplement);
    }

    public InterfaceClassSet MapGetClassSet(Class ReqValue) {
        InterfaceClassSet Result = new InterfaceClassSet();
        InterfaceClassSet PropertySet = Property.GetClassSet(ReqValue);
        // теперь надо мапнуть на базовые интерфейсы
        for(InterfaceClass ClassSet : PropertySet) {
            InterfaceClass MapClassSet = new InterfaceClass();
            Iterator<PropertyInterface> is = ClassSet.keySet().iterator();
            while(is.hasNext()) {
                PropertyInterface Interface = is.next();
                MapClassSet.put(Mapping.get(Interface),ClassSet.get(Interface));
            }
            Result.add(MapClassSet);
        }

        return Result;
    }

    public boolean MapHasChanges(DataSession Session) {
        return Session.PropertyChanges.containsKey(Property);
    }

    // заполняет список, возвращает есть ли изменения
    public boolean mapFillChangedList(List<Property> ChangedProperties, DataChanges Changes) {
        return Property.fillChangedList(ChangedProperties, Changes);
    }

    public DumbImplementChange mapDumbValue(MapChangedRead Read, DumbInterfaceChange<?> DumbInterface) {
        if(Read.ImplementChanged.contains(this) && Read.ImplementType !=2)
            return Read.Session.PropertyChanges.get(Property).Dumb.Value;
        else
            return new DumbImplementChange();
    }

    // для OverrideList'а по сути
    void MapChangeProperty(Map<PropertyInterface, ObjectValue> Keys, Object NewValue, DataSession Session) throws SQLException {
        Map<PropertyInterface,ObjectValue> MapKeys = new HashMap();
        for(PropertyInterface ImplementInterface : (Collection<PropertyInterface>)Property.Interfaces)
            MapKeys.put(ImplementInterface,Keys.get(Mapping.get(ImplementInterface)));

        Property.ChangeProperty(MapKeys,NewValue,Session);
    }

    DumbChange mapDumbChange(DataSession Session) {
        return new DumbChange<PropertyInterface>(mapDumbInterfaceChange(Session),Session.PropertyChanges.get(Property).Dumb.Value);
    }

    public DumbInterfaceChange mapDumbInterfaceChange(DataSession Session) {
        DumbInterfaceChange<?> PropertyDumbChange = Session.PropertyChanges.get(Property).Dumb.Interface;

        DumbInterfaceChange<PropertyInterface> MapDumbChange = new DumbInterfaceChange<PropertyInterface>();
        for(Map.Entry<PropertyInterface,PropertyInterface> ImplementInterface : Mapping.entrySet())
            MapDumbChange.put(ImplementInterface.getValue(),PropertyDumbChange.get(ImplementInterface.getKey()));

        return MapDumbChange;
    }

    public boolean mapIsRequired(InterfaceAddClasses<? extends PropertyInterface> InterfaceClasses) {
        InterfaceAddClasses<PropertyInterface> MapInterfaceClasses = new InterfaceAddClasses<PropertyInterface>();
        for(Map.Entry<PropertyInterface,PropertyInterface> ImplementInterface : Mapping.entrySet())
            MapInterfaceClasses.put(ImplementInterface.getKey(),InterfaceClasses.get(ImplementInterface.getValue()));

        return Property.isRequired(MapInterfaceClasses);
    }
}

// для четкости пусть будет
class JoinPropertyInterface extends PropertyInterface {
}

class JoinProperty extends MapProperty<JoinPropertyInterface,PropertyInterface,JoinPropertyInterface,PropertyInterface,PropertyField> {
    PropertyImplement<PropertyInterfaceImplement> Implements;

    JoinProperty(TableFactory iTableFactory, Property iProperty) {
        super(iTableFactory);
        Implements = new PropertyImplement(iProperty);
    }

    public Class GetValueClass(InterfaceClass ClassImplement) {

        InterfaceClass MapImplement = new InterfaceClass();
        for(PropertyInterface ImplementInterface : (Collection<PropertyInterface>)Implements.Property.Interfaces) {
            Class MapClass = Implements.Mapping.get(ImplementInterface).MapGetValueClass(ClassImplement);
            // если null то уже не подходит по интерфейсу
            if(MapClass==null) return null;

            MapImplement.put(ImplementInterface,MapClass);
        }

        return Implements.Property.GetValueClass(MapImplement);
    }

    public InterfaceClassSet GetClassSet(Class ReqValue) {
        InterfaceClassSet Result = new InterfaceClassSet();
        for(InterfaceClass ItClass : Implements.Property.GetClassSet(ReqValue)) {
            // все варианты даем на вход нижним и они And'ат, а потом все Or'ся
            InterfaceClassSet ItSet = null;
            for(PropertyInterface Interface : (Collection<PropertyInterface>)Implements.Property.Interfaces) {
                InterfaceClassSet RelSet = Implements.Mapping.get(Interface).MapGetClassSet(ItClass.get(Interface));
                ItSet = (ItSet==null?RelSet:ItSet.AndSet(RelSet));
            }

            Result.OrSet(ItSet);
        }

        return Result;
    }

    void fillRequiredChanges(Map<Property, Integer> RequiredTypes) {

        // если только основное - Property ->I - как было (если изменилось только 2 то его и вкинем), возвр. I
        // иначе (не (основное MultiplyProperty и 1)) - Property, Implements ->0 - как было, возвр. 0 - (на подчищение - если (1 или 2) то Left Join'им старые значения)
        // иначе (основное MultiplyProperty и 1) - Implements ->1 - как было (но с другим оператором), возвр. 1

        int ChangeType = RequiredTypes.get(this);

        if(!containsImplement(RequiredTypes.keySet())) {
            Implements.Property.setChangeType(RequiredTypes,ChangeType);
        } else {
            int ReqType = (implementAllInterfaces() && ChangeType==0?0:2);

            Implements.Property.setChangeType(RequiredTypes,ReqType);
            for(PropertyInterfaceImplement Interface : Implements.Mapping.values())
                if(Interface instanceof PropertyMapImplement)
                    (((PropertyMapImplement)Interface).Property).setChangeType(RequiredTypes,(Implements.Property instanceof MultiplyFormulaProperty && ChangeType==1?1:ReqType));
        }
    }

    // инкрементные св-ва
    Change incrementChanges(DataSession Session, int ChangeType) {

        // алгоритм такой - для всех map св-в (в которых были изменения) строим подмножества изм. св-в
        // далее реализации этих св-в "замещаем" (то есть при JOIN будем подставлять), с остальными св-вами делаем LEFT JOIN на IS NULL
        // и JOIN'им с основным св-вом делая туда FULL JOIN новых значений
        // или UNION или большой FULL JOIN в нужном порядке (и не делать LEFT JOIN на IS NULL) и там сделать большой NVL

        DumbChange<JoinPropertyInterface> DumbChange = new DumbChange<JoinPropertyInterface>();
        int QueryIncrementType = ChangeType;
        if(Implements.Property instanceof MultiplyFormulaProperty && ChangeType==1)
            return new Change(1,getMapQuery(getChangeImplements(Session,1),ChangeTable.Value,DumbChange,true),DumbChange);
        else {
            boolean ChangedImplements = containsImplement(Session.PropertyChanges.keySet());
            if(!ChangedImplements && QueryIncrementType==1)
                return new Change(1,getMapQuery(getChangeMap(Session,1),ChangeTable.Value,DumbChange,false),DumbChange);
            // если не все интерфейсы имплементируются св-вами надо запустить ветку с предыдущими значениями чтобы за null'ить
            if(QueryIncrementType==1 || (ChangedImplements && !implementAllInterfaces()))
                QueryIncrementType = 2;

            // конечный результат, с ключами и выражением
            UnionQuery<JoinPropertyInterface,PropertyField> ResultQuery = new UnionQuery<JoinPropertyInterface,PropertyField>(Interfaces,3); // по умолчанию на KEYNULL (но если Multiply то 1 на сумму)

            if(QueryIncrementType==2) {
                // все значения в PrevValue, а в Value - значение Null
                JoinQuery<JoinPropertyInterface, PropertyField> PrevSource = getMapQuery(getPreviousImplements(Session),ChangeTable.PrevValue,DumbChange,false).getJoinQuery();
                PrevSource.add(ChangeTable.Value,new ValueSourceExpr(null,Implements.Property.getType()));
                ResultQuery.add(PrevSource,1);
            }

            // все на Value - PrevValue не интересует, его как раз верхний подгоняет
            ResultQuery.add(getMapQuery(getChange(Session),ChangeTable.Value,DumbChange,false),1);
            if(QueryIncrementType==2)
                ResultQuery.add(getMapQuery(getChangeMap(Session,2),ChangeTable.PrevValue,DumbChange,false),1);

            return new Change(QueryIncrementType,ResultQuery,DumbChange);
        }
    }

    Property<PropertyInterface> getMapProperty() {
        return Implements.Property;
    }

    Map<PropertyInterface, PropertyInterfaceImplement> getMapImplements() {
        return Implements.Mapping;
    }

    Collection<JoinPropertyInterface> getMapInterfaces() {
        return Interfaces;
    }

    DumbInterfaceChange<JoinPropertyInterface> getMapPropertyInterfaces(DumbInterfaceChange<JoinPropertyInterface> InterfaceDumb, DumbInterfaceChange<PropertyInterface> ImplementDumb) {
        return InterfaceDumb;
    }

    DumbInterfaceChange<PropertyInterface> getMapPropertyImplements(DumbInterfaceChange<JoinPropertyInterface> InterfaceDumb, DumbInterfaceChange<PropertyInterface> ImplementDumb) {
        return ImplementDumb;
    }

    void putImplementsToQuery(JoinQuery<JoinPropertyInterface, PropertyField> Query, PropertyField Value, MapRead Read, Map<PropertyInterface, SourceExpr> Implements) {
        Query.add(Value,Read.getMapExpr(getMapProperty(),Implements));
    }

    Map<PropertyField, Type> getMapNullProps(PropertyField Value) {
        Map<PropertyField, Type> NullProps = new HashMap();
        NullProps.put(Value, getType());
        return NullProps;
    }

    PropertyField getDefaultObject() {
        return ChangeTable.Value;
    }

    Source<PropertyInterface, PropertyField> getMapSourceQuery(PropertyField Value) {
        return (Source<PropertyInterface, PropertyField>)(Source<? extends PropertyInterface, PropertyField>)getMapQuery(getDB(),Value);
    }
}

class GroupPropertyInterface extends PropertyInterface {
    PropertyInterfaceImplement Implement;

    GroupPropertyInterface(PropertyInterfaceImplement iImplement) {Implement=iImplement;}
}

abstract class GroupProperty extends MapProperty<GroupPropertyInterface,PropertyInterface,PropertyInterface,GroupPropertyInterface,Object> {
    // каждый интерфейс должен имплементировать именно GetInterface GroupProperty

    // оператор
    int Operator;

    GroupProperty(TableFactory iTableFactory,Property iProperty,int iOperator) {
        super(iTableFactory);
        GroupProperty = iProperty;
        Operator = iOperator;
    }

    // группировочное св-во собсно должно быть не формулой
    Property GroupProperty;

/*
    // заполняются при заполнении базы
    Table ViewTable;
    PropertyField GroupField;
    Map<PropertyInterface,KeyField> GroupKeys = new HashMap();

    GroupQuery<Object,PropertyInterface,Object> getGroupQuery(Object PropertyObject) {
        JoinQuery<PropertyInterface,Object> Query = new JoinQuery<PropertyInterface,Object>(GroupProperty.Interfaces);
        for(GroupPropertyInterface ImplementInterface : Interfaces)
            Query.add(ImplementInterface,ImplementInterface.Implement.mapSourceExpr(Query.MapKeys,true,null,0));

        Query.add(PropertyObject,GroupProperty.getSourceExpr(Query.MapKeys,true));
        return new GroupQuery<Object,PropertyInterface,Object>(Interfaces,Query,PropertyObject,Operator);
    }

    void FillDB(DataSession Session,Integer ViewNum) throws SQLException {

        if(Session.Syntax.allowViews()) {
            // создадим View
            Map<Object,PropertyField> MapProperties = new HashMap();
            ViewTable = getGroupQuery("grfield").createView(Session,"view"+ViewNum,GroupKeys,MapProperties);
            GroupField = MapProperties.get("grfield");
        }
    }

    SourceExpr calculateSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        if(ViewTable!=null) {
            // по сути также как при persistent
            Join<KeyField,PropertyField> SourceJoin = new Join<KeyField,PropertyField>(ViewTable,NotNull);
            for(PropertyInterface Interface : Interfaces)
                SourceJoin.Joins.put(GroupKeys.get(Interface),JoinImplement.get(Interface));

            return SourceJoin.Exprs.get(GroupField);
        } else {
            return (new Join<PropertyInterface,Object>(getGroupQuery("grfield"),JoinImplement,NotNull)).Exprs.get("grfield");
        }

    }*/

    public Class GetValueClass(InterfaceClass ClassImplement) {

        InterfaceClassSet ClassSet = GetClassSet(null);

        // GetClassSet по идее ValueClass'ы проставил
        if(ClassSet.OrItem(ClassImplement))
            return GroupProperty.GetValueClass(GroupProperty.GetClassSet(null).iterator().next());
        else
            return null;
    }

    public InterfaceClassSet GetClassSet(Class ReqValue) {

        InterfaceClassSet Result = new InterfaceClassSet();

        // берем сначала все классы GroupProperty и интерфейсов, а затем гоним их через GetValueClass и то что получаем на выходе гоним наружу
        InterfaceClassSet GroupSet = GroupProperty.GetClassSet(ReqValue);

        for(GroupPropertyInterface Interface : Interfaces)
            GroupSet = GroupSet.AndSet(Interface.Implement.MapGetClassSet(null));

        for(InterfaceClass ClassSet : GroupSet) {
            InterfaceClass ResultSet = new InterfaceClass();

            for(GroupPropertyInterface GroupInterface : Interfaces)
                ResultSet.put(GroupInterface, GroupInterface.Implement.MapGetValueClass(ClassSet));

            Result.OrItem(ResultSet);
        }

        return Result;
    }
/*
    // заполняет список, возвращает есть ли изменения
    boolean fillChangedList(List<Property> ChangedProperties, DataSession Session) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = GroupProperty.fillChangedList(ChangedProperties,Session);

        for(GroupPropertyInterface Interface : Interfaces)
            Changed = Interface.Implement.MapFillChangedList(ChangedProperties,Session) || Changed;

        if(Changed)
            ChangedProperties.add(this);

        return Changed;
    }
  */
/*    // получает всевозможные инкрементные запросы для обеспечения IncrementChanges
    Query<PropertyInterface,PropertyField> IncrementQuery(DataSession Session,PropertyField Value,List<GroupPropertyInterface> ChangedProperties,int GroupSet,boolean ValueType,boolean InterfaceSubSet,boolean InterfaceEmptySet) {
        // ValueName куда значение класть
        // ChangedProperties чтобы по нескольку раз не считать
        // GroupSet -> 0(G) - новые, 1(A) - предыдущие, 2(G/A) - новые и предыдущие
        // ValueType -> true(=) - новые, false(P) - предыдущие
        // InterfaceSubSet -> true - включать все подмн-ва, false - одиночные подмн-ва
        // InterfaceEmptySet -> true - включать пустое подмн-во, false - не включать
        // P GroupValue -> чтобы знать на какое значение считать =(0)/+(1)/prev(2), Operand определяет SUM\MAX

        UnionQuery<PropertyInterface,Object> DataQuery = new UnionQuery<PropertyInterface,Object>(GroupProperty.Interfaces,3);

        for(int GroupOp=(GroupSet==1 || GroupSet==2?0:1);GroupOp<=((GroupSet==0 || GroupSet==2) && GroupProperty.HasChanges(Session)?1:0);GroupOp++) {
            // подмн-во Group
            ListIterator<List<GroupPropertyInterface>> il = null;
            if(InterfaceSubSet)
                il = (new SetBuilder<GroupPropertyInterface>()).BuildSubSetList(ChangedProperties).listIterator();
            else {
                List<List<GroupPropertyInterface>> ChangedList = new ArrayList();
                ChangedList.add(new ArrayList());
                for(GroupPropertyInterface Interface : ChangedProperties) {
                    List<GroupPropertyInterface> SingleList = new ArrayList();
                    SingleList.add(Interface);
                    ChangedList.add(SingleList);
                }
                il = ChangedList.listIterator();
            }

            // если не пустое и не идем по изменениям основного св-ва скипаем
            if(!(InterfaceEmptySet || GroupOp==1))
                il.next();

            while(il.hasNext()) {
                List<GroupPropertyInterface> ChangeProps = il.next();
                JoinQuery<PropertyInterface,Object> Query = new JoinQuery<PropertyInterface, Object>(GroupProperty.Interfaces);

                // значение
                Query.add(Value,(GroupOp==1?GroupProperty.getChangedSourceExpr(Query.MapKeys,Session,(ValueType?Operator:2)):GroupProperty.getSourceExpr(Query.MapKeys,true)));

                // значения интерфейсов
                for(GroupPropertyInterface Interface : Interfaces)
                    Query.add(Interface,Interface.Implement.mapSourceExpr(Query.MapKeys,true,(ChangeProps.contains(Interface)?Session:null),ValueType?0:2));

                DataQuery.add(Query,1);
            }
        }

        return new GroupQuery<Object,PropertyInterface,PropertyField>(Interfaces,DataQuery,Value,Operator);
    }
  */
    List<GroupPropertyInterface> GetChangedProperties(DataSession Session) {
        List<GroupPropertyInterface> ChangedProperties = new ArrayList();
        // должен вернуть null если нету изменений (или просто транслирует интерфейс) иначе возвращает AggregateProperty
        for(GroupPropertyInterface Interface : Interfaces)
            if(Interface.Implement.MapHasChanges(Session)) ChangedProperties.add(Interface);

        return ChangedProperties;
    }

    Property<PropertyInterface> getMapProperty() {
        return GroupProperty;
    }

    Map<GroupPropertyInterface, PropertyInterfaceImplement> getMapImplements() {
        Map<GroupPropertyInterface,PropertyInterfaceImplement> Result = new HashMap<GroupPropertyInterface,PropertyInterfaceImplement>();
        for(GroupPropertyInterface Interface : Interfaces)
            Result.put(Interface,Interface.Implement);
        return Result;
    }

    Collection<PropertyInterface> getMapInterfaces() {
        return GroupProperty.Interfaces;
    }

    DumbInterfaceChange<GroupPropertyInterface> getMapPropertyInterfaces(DumbInterfaceChange<PropertyInterface> InterfaceDumb, DumbInterfaceChange<GroupPropertyInterface> ImplementDumb) {
        return ImplementDumb;
    }

    DumbInterfaceChange<PropertyInterface> getMapPropertyImplements(DumbInterfaceChange<PropertyInterface> InterfaceDumb, DumbInterfaceChange<GroupPropertyInterface> ImplementDumb) {
        return InterfaceDumb;
    }

    void putImplementsToQuery(JoinQuery<PropertyInterface, Object> Query, Object Value, MapRead Read, Map<GroupPropertyInterface, SourceExpr> Implements) {
        Query.addAll(Implements);
        Query.add(Value,Read.getMapExpr(GroupProperty,Query.MapKeys));
    }

    Map<Object, Type> getMapNullProps(Object Value) {
        Map<Object, Type> NullProps = new HashMap();
        NullProps.put(Value, getType());
        InterfaceClass InterfaceClass = GetClassSet(null).get(0);
        for(Map.Entry<PropertyInterface,Class> Interface : InterfaceClass.entrySet())
            NullProps.put(Interface.getKey(),Interface.getValue().getType());
        return NullProps;
    }

    Object getDefaultObject() {
        return "grfield";
    }

    Source<PropertyInterface, Object> getMapSourceQuery(Object Value) {
        return new GroupQuery<Object,PropertyInterface,Object>(Interfaces,getMapQuery(getDB(),Value),Value,Operator);
    }

    Source<GroupPropertyInterface, PropertyField> getGroupQuery(List<MapChangedRead> ReadList,PropertyField Value) {
        return new GroupQuery<Object,GroupPropertyInterface,PropertyField>(Interfaces,getMapQuery(ReadList,Value,new DumbChange<GroupPropertyInterface>(),false),Value,Operator);
    }
}

class SumGroupProperty extends GroupProperty {

    SumGroupProperty(TableFactory iTableFactory,Property iProperty) {super(iTableFactory,iProperty,1);}

    void fillRequiredChanges(Map<Property, Integer> RequiredTypes) {
        // Group на ->1, Interface на ->2 - как было - возвр. 1 (на подчищение если (0 или 2) LEFT JOIN'им старые)
        GroupProperty.setChangeType(RequiredTypes,1);

        for(GroupPropertyInterface Interface : Interfaces)
            if(Interface.Implement instanceof PropertyMapImplement)
                (((PropertyMapImplement)Interface.Implement).Property).setChangeType(RequiredTypes,2);
    }

    Change incrementChanges(DataSession Session, int ChangeType) {
        // алгоритм пока такой :
        // 1. берем GROUPPROPERTY(изм на +) по аналогии с реляционными
        // G(0) =(true) SS(true) без общ.(false) 1 SUM(+)
        // 2. для новых св-в делаем GROUPPROPERTY(все) так же как и для реляционных св-в FULL JOIN'ы - JOIN'ов с "перегр." подмн-вами (единственный способ сразу несколько изменений "засечь") (и GROUP BY по ISNULL справо налево ключей)
        // A(1) =(true) SS(true) без обш.(false) 1 SUM(+)
        // 3. для старых св-в GROUPPROPERTY(все) FULL JOIN (JOIN "перегр." измененных с LEFT JOIN'ами старых) (без подмн-в) (и GROUP BY по ISNULL(обычных JOIN'ов,LEFT JOIN'a изм.))
        // A(1) P(false) без SS(false) без общ.(false) -1 SUM(+)
        // все UNION ALL и GROUP BY или же каждый GROUP BY а затем FULL JOIN на +

        // конечный результат, с ключами и выражением
        UnionQuery<GroupPropertyInterface,PropertyField> ResultQuery = new UnionQuery<GroupPropertyInterface,PropertyField>(Interfaces,1);

        // InterfaceSubSet ij<=2
        // InterfaceValue ij<=2
        // InterfaceEmptySet ij!=2
        // GroupSet (ij==1,0,1)

        ResultQuery.add(getGroupQuery(getChangeMap(Session,1),ChangeTable.Value),1);

        ResultQuery.add(getGroupQuery(getChangeImplements(Session,0),ChangeTable.Value),1);
        ResultQuery.add(getGroupQuery(getPreviousImplements(Session),ChangeTable.Value),-1);

/*        if(GroupProperty.HasChanges(Session))
            ResultQuery.add(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,0,true,true,false),1);

        if(ChangedProperties.size()>0) {
            ResultQuery.add(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,1,true,true,false),1);
            ResultQuery.add(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,1,false,false,false),-1);
        }*/

        return new Change(1,ResultQuery,new DumbChange<GroupPropertyInterface>());
     }
}


class MaxGroupProperty extends GroupProperty {

    MaxGroupProperty(TableFactory iTableFactory,Property iProperty) {super(iTableFactory,iProperty,0);}

    void fillRequiredChanges(Map<Property, Integer> RequiredTypes) {
        // Group на ->2, Interface на ->2 - как было - возвр. 2
        GroupProperty.setChangeType(RequiredTypes,2);

        for(GroupPropertyInterface Interface : Interfaces)
            if(Interface.Implement instanceof PropertyMapImplement)
                ((PropertyMapImplement)Interface.Implement).Property.setChangeType(RequiredTypes,2);
    }

    Change incrementChanges(DataSession Session, int ChangeType) {

        // делаем Full Join (на 3) :
        //      a) ушедшие (previmp и prevmap) = старые (sourceexpr) LJ (prev+change) (вообще и пришедшие <= старых)
        //      b) пришедшие (change) > старых (sourceexpr)

        PropertyField PrevMapValue = new PropertyField("drop",Type.Integer);

        UnionQuery<GroupPropertyInterface,PropertyField> ChangeQuery = new UnionQuery<GroupPropertyInterface,PropertyField>(Interfaces,3);

        List<MapChangedRead> PrevMapRead = getPreviousImplements(Session); PrevMapRead.add(getPreviousMap(Session));
        ChangeQuery.add(getGroupQuery(PrevMapRead,PrevMapValue),1);
        ChangeQuery.add(getGroupQuery(getChange(Session),ChangeTable.Value),1);

        // подозрительные на изменения ключи
        JoinQuery<GroupPropertyInterface,PropertyField> SuspiciousQuery = new JoinQuery<GroupPropertyInterface,PropertyField>(Interfaces);
        UniJoin<GroupPropertyInterface,PropertyField> ChangeJoin = new UniJoin<GroupPropertyInterface,PropertyField>(ChangeQuery,SuspiciousQuery,true);

        SourceExpr NewValue = ChangeJoin.Exprs.get(ChangeTable.Value);
        SourceExpr OldValue = ChangeJoin.Exprs.get(PrevMapValue);
        SourceExpr PrevValue = getSourceExpr((Map<PropertyInterface,SourceExpr>)(Map<? extends PropertyInterface,SourceExpr>) SuspiciousQuery.MapKeys,false);

        SuspiciousQuery.add(ChangeTable.Value,NewValue);
        SuspiciousQuery.add(PrevMapValue,OldValue);
        SuspiciousQuery.add(ChangeTable.PrevValue,PrevValue);

        SuspiciousQuery.add(new FieldOPWhere(
                new FieldExprCompareWhere(NewValue.getNullMinExpr(),PrevValue.getNullMinExpr(),FieldExprCompareWhere.GREATER),
                new FieldExprCompareWhere(OldValue.getNullMinExpr(),PrevValue.getNullMinExpr(),FieldExprCompareWhere.EQUALS),false));

        JoinQuery<GroupPropertyInterface,PropertyField> UpdateQuery = new JoinQuery<GroupPropertyInterface,PropertyField>(Interfaces);
        UniJoin<GroupPropertyInterface, PropertyField> ChangesJoin = new UniJoin<GroupPropertyInterface,PropertyField>(SuspiciousQuery,UpdateQuery,true);
        UpdateQuery.add(ChangeTable.PrevValue,ChangesJoin.Exprs.get(ChangeTable.PrevValue));
        List<MapChangedRead> NewRead = new ArrayList(); NewRead.add(getPrevious(Session)); NewRead.addAll(getChange(Session));
        UpdateQuery.add(ChangeTable.Value,(new UniJoin<GroupPropertyInterface,PropertyField>(getGroupQuery(NewRead,ChangeTable.Value),UpdateQuery,false)).Exprs.get(ChangeTable.Value));

        return new Change(2,UpdateQuery,new DumbChange<GroupPropertyInterface>());
    }
}

// КОМБИНАЦИИ (ЛИНЕЙНЫЕ,MAX,OVERRIDE) принимают null на входе, по сути как Relation но работают на Or\FULL JOIN
// соответственно мн-во св-в полностью должно отображаться на интерфейсы

abstract class UnionProperty extends AggregateProperty<PropertyInterface> {

    UnionProperty(TableFactory iTableFactory,int iOperator) {
        super(iTableFactory);
        Operands = new ArrayList();
        Operator = iOperator;
        Coeffs = new HashMap();
    }

    // имплементации св-в (полные)
    List<PropertyMapImplement> Operands;

    int Operator;
    // коэффициенты
    Map<PropertyMapImplement,Integer> Coeffs;

    SourceExpr calculateSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        String ValueString = "joinvalue";
        UnionQuery<PropertyInterface,String> ResultQuery = new UnionQuery<PropertyInterface,String>(Interfaces,Operator);
        for(PropertyMapImplement Operand : Operands) {
            JoinQuery<PropertyInterface,String> Query = new JoinQuery<PropertyInterface, String>(Interfaces);
            Query.add(ValueString,Operand.mapSourceExpr(Query.MapKeys,true));
            ResultQuery.add(Query,Coeffs.get(Operand));
        }

        return (new Join<PropertyInterface,String>(ResultQuery,JoinImplement,NotNull)).Exprs.get(ValueString);
    }

    public Class GetValueClass(InterfaceClass ClassImplement) {
        // в отличии от Relation только когда есть хоть одно св-во
        Class ResultClass = null;
        for(PropertyMapImplement Operand : Operands) {
            Class ValueClass = Operand.MapGetValueClass(ClassImplement);
            if(ValueClass!=null) {
                if(ResultClass==null)
                    ResultClass = ValueClass;
                else
                    ResultClass = ValueClass.CommonParent(ValueClass);
            }
        }

        return ResultClass;
    }

    public InterfaceClassSet GetClassSet(Class ReqValue) {
        // в отличии от Relation игнорируем null
        InterfaceClassSet Result = new InterfaceClassSet();
        for(PropertyMapImplement Operand : Operands)
            Result.OrSet(Operand.MapGetClassSet(ReqValue));

        return Result;
    }

    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = false;

        for(PropertyMapImplement Operand : Operands)
            Changed = Operand.mapFillChangedList(ChangedProperties, Changes) || Changed;

        if(Changed)
            ChangedProperties.add(this);

        return Changed;
    }

    List<PropertyMapImplement> GetChangedProperties(DataSession Session) {

        List<PropertyMapImplement> ChangedProperties = new ArrayList();
        for(PropertyMapImplement Operand : Operands)
            if(Operand.MapHasChanges(Session)) ChangedProperties.add(Operand);

        return ChangedProperties;
    }

    // определяет AddClasses подмн-ва и что все операнды пересекаются
    DumbChange<PropertyInterface> getDumbChange(DataSession Session,List<PropertyMapImplement> ChangedProps) {

        DumbChange<PropertyInterface> DumbChange = new DumbChange<PropertyInterface>();
        for(PropertyMapImplement Operand : ChangedProps) {
            if(!intersect(Session, Operand,ChangedProps)) return null;
            DumbChange.and(Operand.mapDumbChange(Session));
        }

        return DumbChange;
    }


    Change incrementChanges(DataSession Session, int ChangeType) {

        int QueryIncrementType = getIncrementType(ChangeType);
        //      	0                   1                           2
        //Max(0)	значение,SS,LJ      не может быть               значение,SS,LJ,prevv
        //Sum(1)	значение,SS,LJ      значение,без SS, без LJ     значение,SS,LJ,prevv
        //Override(2)	значение,SS,LJ      старое поле=null,SS, LJ     значение,SS,LJ,prevv

        DumbChange<PropertyInterface> DumbChange = new DumbChange<PropertyInterface>();

        // неструктурно как и все оптимизации

        if(Operator==1 && QueryIncrementType==1) {
            UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,1);

            for(PropertyMapImplement Operand : GetChangedProperties(Session)) {
                JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface, PropertyField>(Interfaces);
                Query.add(ChangeTable.Value,Operand.mapChangedExpr(Query.MapKeys, Session,1));
                ResultQuery.add(Query,Coeffs.get(Operand));

                DumbChange.and(Operand.mapDumbChange(Session));
            }

            return new Change(1,ResultQuery,DumbChange);
        } else {
            Source<PropertyInterface,PropertyField> ChangeQuery = getChange(Session,QueryIncrementType==1?1:0,ChangeTable.Value,DumbChange);
            if(QueryIncrementType==2) {
                UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,3);
                ResultQuery.add(ChangeQuery,1);
                ResultQuery.add(getChange(Session,2,ChangeTable.PrevValue,DumbChange),1);
                ChangeQuery = ResultQuery;
            }

            return new Change(QueryIncrementType,ChangeQuery,DumbChange);
        }
    }

    Source<PropertyInterface,PropertyField> getChange(DataSession Session, int MapType, PropertyField Value, DumbChange<PropertyInterface> ResultDumb) {

        UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,3);

        ListIterator<List<PropertyMapImplement>> il = (new SetBuilder<PropertyMapImplement>()).BuildSubSetList(GetChangedProperties(Session)).listIterator();
        // пропустим пустое подмн-во
        il.next();
        while(il.hasNext()) {
            List<PropertyMapImplement> ChangedProps = il.next();

            // проверим что все попарно пересекаются по классам, заодно строим InterfaceAddClasses<T> св-в
            DumbChange<PropertyInterface> DumbChange = getDumbChange(Session,ChangedProps);
            if(DumbChange==null) continue;
            ResultDumb.and(DumbChange);

            JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface, PropertyField>(Interfaces);
            UnionSourceExpr ResultExpr = new UnionSourceExpr(Operator);
            // именно в порядке операндов (для Overrid'а важно)
            for(PropertyMapImplement Operand : Operands) {
                if(ChangedProps.contains(Operand))
                    ResultExpr.Operands.put(Operand.mapChangedExpr(Query.MapKeys, Session,MapType),Coeffs.get(Operand));
                else {
                    // не измененное св-во - проверяем что не пересекается по классам, а также что не isRequired InterfaceAddClasses
                    if(intersect(Session, Operand,ChangedProps) && !Operand.mapIsRequired(DumbChange.Interface.getInteraceAddClasses())) {
                        SourceExpr OperandExpr = Operand.mapSourceExpr(Query.MapKeys,false);
                        if(Operator==2 && MapType==1) // если Override и 1 то нам нужно не само значение, а если не null то 0, иначе null (то есть не брать значение) {
                            OperandExpr = new CaseWhenSourceExpr(new SourceIsNullWhere(OperandExpr,false),new ValueSourceExpr(null,OperandExpr.getType()),new ValueSourceExpr(0,OperandExpr.getType()));
                        ResultExpr.Operands.put(OperandExpr,Coeffs.get(Operand));
                    }
                }
            }
            Query.add(Value,ResultExpr);
            ResultQuery.add(Query,1);
        }

        return ResultQuery;
    }

    boolean intersect(DataSession Session, PropertyMapImplement Operand, Collection<PropertyMapImplement> Operands) {
        for(PropertyMapImplement IntersectOperand : Operands) {
            if(Operand==IntersectOperand) return true;
            if(!intersect(Session, Operand,IntersectOperand)) return false;
        }
        return true;
    }

    // проверяет пересекаются по классам операнды или нет
    boolean intersect(DataSession Session, PropertyMapImplement Operand, PropertyMapImplement IntersectOperand) {
        return (Session.Changes.AddClasses.size() > 0 && Session.Changes.RemoveClasses.size() > 0) ||
               Operand.MapGetClassSet(null).AndSet(IntersectOperand.MapGetClassSet(null)).size() > 0;
//        return true;
    }

    void fillRequiredChanges(Map<Property, Integer> RequiredTypes) {

        for(PropertyMapImplement Operand : Operands)
            Operand.Property.setChangeType(RequiredTypes,getIncrementType(RequiredTypes.get(this)));
    }

    int getIncrementType(int RequiredType) {
        return RequiredType;
    }
}


class SumUnionProperty extends UnionProperty {

    SumUnionProperty(TableFactory iTableFactory) {super(iTableFactory,1);}

    int getIncrementType(int RequiredType) {
        // если pers или 1 - Operand на ->1 - IncrementQuery(1) возвр. 1 - (на подчищение - если (0 или 2) LEFT JOIN'им старые)
        // иначе (не pers и (0 или 2)) - Operand на ->I - IncrementQuery (I) возвр. I

        return (IsPersistent()?1:RequiredType);
    }
}

class MaxUnionProperty extends UnionProperty {

    MaxUnionProperty(TableFactory iTableFactory) {super(iTableFactory,0);}

    int getIncrementType(int RequiredType) {
        return (IsPersistent() || RequiredType==0?0:2);
    }
}

class OverrideUnionProperty extends UnionProperty {

    OverrideUnionProperty(TableFactory iTableFactory) {super(iTableFactory,2);}

    private PropertyMapImplement getOperand(Map<PropertyInterface, ObjectValue> keys) {

        InterfaceClass changeClass = new InterfaceClass();
        for(PropertyInterface iface : Interfaces)
            changeClass.put(iface,keys.get(iface).Class);

        for(int i=Operands.size()-1;i>=0;i--) {
            PropertyMapImplement operand = Operands.get(i);
            if(operand.MapGetValueClass(changeClass)!=null) {
                return operand;
            }
        }

        return null;
    }

    @Override
    boolean allowChangeProperty(Map<PropertyInterface, ObjectValue> keys) {
        return getOperand(keys) != null;
    }

    @Override
    void ChangeProperty(Map<PropertyInterface, ObjectValue> keys, Object newValue, DataSession session) throws SQLException {

        PropertyMapImplement operand = getOperand(keys);
        if (operand != null)
            operand.MapChangeProperty(keys,newValue,session);
    }
}


// ФОРМУЛЫ

class FormulaPropertyInterface extends PropertyInterface {
    Class Class;

    FormulaPropertyInterface(Class iClass) {
        Class = iClass;
    }
}

// вообще Collection
abstract class FormulaProperty<T extends FormulaPropertyInterface> extends AggregateProperty<T> {

    FormulaProperty(TableFactory iTableFactory) {
        super(iTableFactory);
    }

    public Class GetValueClass(InterfaceClass Class) {

        Class Result = null;
        for(T Interface : Interfaces) {
            if(!Class.get(Interface).IsParent(Interface.Class)) return null;
            if(Result==null || !(Interface.Class instanceof BitClass))
                Result = Interface.Class;
        }

        return Result;
    }

    public InterfaceClassSet GetClassSet(Class ReqValue) {
        InterfaceClass ResultSet = new InterfaceClass();

        for(T Interface : Interfaces)
            ResultSet.put(Interface,Interface.Class);

        InterfaceClassSet Result = new InterfaceClassSet();
        if(ReqValue==null || GetValueClass(ResultSet).IsParent(ReqValue)) Result.add(ResultSet);
        return Result;
    }

    void fillRequiredChanges(Map<Property, Integer> RequiredTypes) {
    }

    // не может быть изменений в принципе
    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes) {
        return false;
    }

    Change incrementChanges(DataSession Session, int ChangeType) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

class StringFormulaPropertyInterface extends FormulaPropertyInterface {
    String Param;

    StringFormulaPropertyInterface(Class iClass,String iParam) {
        super(iClass);
        Param = iParam;
    }
}

class StringFormulaProperty extends FormulaProperty<StringFormulaPropertyInterface> {

    String Formula;

    StringFormulaProperty(TableFactory iTableFactory, String iFormula) {
        super(iTableFactory);
        Formula = iFormula;
    }

    SourceExpr calculateSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        FormulaSourceExpr Source = new FormulaSourceExpr(Formula);

        for(StringFormulaPropertyInterface Interface : Interfaces)
            Source.Params.put(Interface.Param,JoinImplement.get(Interface));

        return Source;
    }
}

class WhereStringFormulaProperty extends StringFormulaProperty {

    WhereStringFormulaProperty(TableFactory iTableFactory, String iFormula) {
        super(iTableFactory, iFormula);
    }

    public Class GetValueClass(InterfaceClass ValueClass) {

        for(StringFormulaPropertyInterface Interface : Interfaces)
            if(!ValueClass.get(Interface).IsParent(Interface.Class)) return null;

        return Class.bitClass;
    }

    SourceExpr calculateSourceExpr(Map<PropertyInterface, SourceExpr> JoinImplement, boolean NotNull) {

        FormulaSourceWhere Source = new FormulaSourceWhere(Formula);

        for(StringFormulaPropertyInterface Interface : Interfaces)
            Source.Params.put(Interface.Param,JoinImplement.get(Interface));

        return new FormulaWhereSourceExpr(Source,NotNull);
    }
}


class MultiplyFormulaProperty extends FormulaProperty<FormulaPropertyInterface> {

    MultiplyFormulaProperty(TableFactory iTableFactory) {
        super(iTableFactory);
    }

    SourceExpr calculateSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        MultiplySourceExpr Source = new MultiplySourceExpr();
        for(FormulaPropertyInterface Interface : Interfaces)
            Source.Operands.add(JoinImplement.get(Interface));

        return Source;
    }
}

class InterfaceClassSet extends ArrayList<InterfaceClass> {

    InterfaceClass GetCommonParent() {
        Iterator<InterfaceClass> i = iterator();
        InterfaceClass Result = i.next();
        while(i.hasNext()) Result.CommonParent(i.next());
        return Result;
    }

    void Out(Collection<PropertyInterface> ToDraw) {
        for(InterfaceClass InClass : this) {
            for(PropertyInterface Key : ToDraw)
                System.out.print(InClass.get(Key).ID.toString()+" ");
            System.out.println();
       }
   }

    // нужен интерфейс слияния и пересечения с InterfaceClass

    InterfaceClassSet AndSet(InterfaceClassSet Op) {
//        if(size()==0) return (InterfaceClassSet)Op.clone();
//        if(Op.size()==0) return (InterfaceClassSet)clone();
        InterfaceClassSet Result = new InterfaceClassSet();
        for(InterfaceClass IntClass : this)
            Result.OrSet(Op.AndItem(IntClass));
        return Result;
    }

    void OrSet(InterfaceClassSet Op) {
        for(InterfaceClass IntClass : Op) OrItem(IntClass);
    }

    InterfaceClassSet AndItem(InterfaceClass Op) {
        InterfaceClassSet Result = new InterfaceClassSet();
//        if(size()>0) {
            for(InterfaceClass IntClass : this) Result.OrSet(Op.And(IntClass));
//        } else
//            Result.add(Op);

        return Result;
    }

    boolean OrItem(InterfaceClass Op) {
        // бежим по всем, если выше какого-то класса, если ниже, то старый выкидываем
        Iterator<InterfaceClass> i = iterator();
        while(i.hasNext()) {
            InterfaceClass OrInterface = i.next();
            int OrResult = OrInterface.Or(Op);
            if(OrResult==1) return true;
            if(OrResult==2) i.remove();
        }

        add(Op);

        return false;
    }

    @Override public Object clone() {
        InterfaceClassSet CloneObject = new InterfaceClassSet();
        for(InterfaceClass IntClass : this) CloneObject.add(IntClass);
        return CloneObject;
    }
}


class InterfaceClass extends HashMap<PropertyInterface,Class> {

    @Override
    public Class put(PropertyInterface key, Class value) {
        if(value==null)
            throw new RuntimeException();
        return super.put(key, value);
    }

    InterfaceClassSet And(InterfaceClass AndOp) {
        InterfaceClassSet Result = new InterfaceClassSet();

        Map<Class[],PropertyInterface> JoinClasses = new HashMap<Class[],PropertyInterface>();

        Class Class;
        Class[] SingleArray;

        for(PropertyInterface Key : keySet()) {
            Class = get(Key);
            Class AndClass = AndOp.get(Key);

            if(AndClass!=null) {
                Class[] CommonClasses = (Class[])Class.CommonClassSet(AndClass).toArray(new Class[0]);
                // если не нашли ни одного общего класса, то выходим
                if(CommonClasses.length==0) return Result;
                JoinClasses.put(CommonClasses,Key);
            }
            else {
                SingleArray = new Class[1];
                SingleArray[0] = Class;
                JoinClasses.put(SingleArray,Key);
            }
        }

        for(PropertyInterface Key : AndOp.keySet()) {
            if(!containsKey(Key)) {
                SingleArray = new Class[1];
                SingleArray[0] = AndOp.get(Key);
                JoinClasses.put(SingleArray,Key);
            }
        }

        int ia;
        Class[][] ArrayClasses = (Class[][])JoinClasses.keySet().toArray(new Class[0][]);
        PropertyInterface[] ArrayInterfaces = new PropertyInterface[ArrayClasses.length];
        int[] IntIterators = new int[ArrayClasses.length];
        for(ia=0;ia<ArrayClasses.length;ia++) {
            ArrayInterfaces[ia] = JoinClasses.get(ArrayClasses[ia]);
            IntIterators[ia] = 0;
        }
        boolean Exit = false;
        while(!Exit) {
            // закидываем новые комбинации
            InterfaceClass ResultInterface = new InterfaceClass();
            for(ia=0;ia<ArrayClasses.length;ia++) ResultInterface.put(ArrayInterfaces[ia],ArrayClasses[ia][IntIterators[ia]]);
            Result.add(ResultInterface);

            // следующую итерацию
            while(ia<ArrayClasses.length && IntIterators[ia]==ArrayClasses[ia].length-1) {
                IntIterators[ia] = 0;
                ia++;
            }

            if(ia>=ArrayClasses.length) Exit=true;
        }

        return Result;
    }

    // 0 - не связаны, 1 - Op >= , 2 - Op <
    // известно что одной размерности
    int Or(InterfaceClass OrOp) {

        int ResultOr = -1;
        for(PropertyInterface Key : keySet()) {
            Class Class = get(Key);
            Class OrClass = OrOp.get(Key);

            if(Class!=OrClass) {
                // отличающийся
                if(ResultOr<2) {
                    if(OrClass.IsParent(Class))
                        ResultOr = 1;
                    else
                        if(ResultOr==1)
                            return 0;
                }

                if(ResultOr!=1)
                    if(Class.IsParent(OrClass))
                        ResultOr = 2;
                    else
                        return 0;
                }
            }

        if(ResultOr==-1) return 1;
        return ResultOr;
    }

    // известно что одной размерности
    void CommonParent(InterfaceClass Op) {
        for(PropertyInterface Key : keySet())
            put(Key,get(Key).CommonParent(Op.get(Key)));
    }
}

// изменения данных
class DataChanges {
    Set<DataProperty> Properties = new HashSet();

    Set<Class> AddClasses = new HashSet();
    Set<Class> RemoveClasses = new HashSet();

    DataChanges copy() {
        DataChanges CopyChanges = new DataChanges();
        CopyChanges.Properties.addAll(Properties);
        CopyChanges.AddClasses.addAll(AddClasses);
        CopyChanges.RemoveClasses.addAll(RemoveClasses);
        return CopyChanges;
    }

    public boolean hasChanges() {
        return !(Properties.isEmpty() && AddClasses.isEmpty() && RemoveClasses.isEmpty());        
    }
}

interface PropertyUpdateView {

    Collection<Property> getUpdateProperties();
}

class DataSession  {

    Connection Connection;
    SQLSyntax Syntax;

    DataChanges Changes = new DataChanges();
    Map<PropertyUpdateView,DataChanges> IncrementChanges = new HashMap();

    Map<Property, Property.Change> PropertyChanges = new HashMap();

    TableFactory TableFactory;
    ObjectClass ObjectClass;

    int ID = 0;

    DataSession(DataAdapter Adapter,int iID,TableFactory iTableFactory,ObjectClass iObjectClass) throws SQLException{

        ID = iID;
        Syntax = Adapter;
        TableFactory = iTableFactory;
        ObjectClass = iObjectClass;

        try {
            Connection = Adapter.startConnection();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        TableFactory.fillSession(this);
    }

    void restart(boolean Cancel) throws SQLException {

        if(Cancel)
            for(DataChanges ViewChanges : IncrementChanges.values()) {
                ViewChanges.Properties.addAll(Changes.Properties);
                ViewChanges.AddClasses.addAll(Changes.AddClasses);
                ViewChanges.RemoveClasses.addAll(Changes.RemoveClasses);
            }

        TableFactory.clearSession(this);
        Changes = new DataChanges();
        NewClasses = new HashMap();

        PropertyChanges = new HashMap();
    }

    Map<Integer,Class> NewClasses = new HashMap();

    void changeClass(Integer idObject,Class Class) throws SQLException {
        if(Class==null) Class = ObjectClass;

        Set<Class> AddClasses = new HashSet();
        Set<Class> RemoveClasses = new HashSet();
        Class.GetDiffSet(getObjectClass(idObject),AddClasses,RemoveClasses);

        TableFactory.AddClassTable.changeClass(this,idObject,AddClasses,false);
        TableFactory.RemoveClassTable.changeClass(this,idObject,AddClasses,true);

        TableFactory.RemoveClassTable.changeClass(this,idObject,RemoveClasses,false);
        TableFactory.AddClassTable.changeClass(this,idObject,RemoveClasses,true);

        NewClasses.put(idObject,Class);

        Changes.AddClasses.addAll(AddClasses);
        Changes.RemoveClasses.addAll(RemoveClasses);

        for(DataChanges ViewChanges : IncrementChanges.values()) {
            ViewChanges.AddClasses.addAll(AddClasses);
            ViewChanges.RemoveClasses.addAll(RemoveClasses);
        }
    }

    void changeProperty(DataProperty Property) {
        Changes.Properties.add(Property);

        for(DataChanges ViewChanges : IncrementChanges.values())
            ViewChanges.Properties.add(Property);
    }

    Class readClass(Integer idObject) throws SQLException {
        return ObjectClass.FindClassID(TableFactory.ObjectTable.GetClassID(this,idObject));
    }

    Class getObjectClass(Integer idObject) throws SQLException {
        Class NewClass = NewClasses.get(idObject);
        if(NewClass==null)
            NewClass = readClass(idObject);
        if(NewClass==null)
            NewClass = ObjectClass;
        return NewClass;
    }

    // последний параметр
    List<Property> update(PropertyUpdateView ToUpdate,Collection<Class> UpdateClasses) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)

        DataChanges ToUpdateChanges = IncrementChanges.get(ToUpdate);
        if(ToUpdateChanges==null) ToUpdateChanges = Changes;

        Collection<Property> ToUpdateProperties = ToUpdate.getUpdateProperties();
        // сначала читаем инкрементные св-ва которые изменились
        List<Property> IncrementUpdateList = new ArrayList();
        for(Property Property : ToUpdateProperties) Property.fillChangedList(IncrementUpdateList,ToUpdateChanges);
        // затем всю ветку, для чтения
        List<Property> UpdateList = new ArrayList();
        for(Property Property : IncrementUpdateList) Property.fillChangedList(UpdateList,Changes);

        Map<Property,Integer> RequiredTypes = new HashMap();
        // пробежим вперед пометим свойства которые изменились, но неясно на что
        ListIterator<Property> il = UpdateList.listIterator();
        Property Property = null;
        while(il.hasNext()) {
            Property = il.next();
            RequiredTypes.put(Property,ToUpdateProperties.contains(Property)?0:null);

//            Session.PropertyAddClasses.put(Property,new InterfaceAddClasses());
//            Session.PropertyAddValues.put(Property,new AddClasses());
        }

        // бежим по списку (в обратном порядке) заполняем требования,
        while(Property!=null) {
            Property.fillRequiredChanges(RequiredTypes);
            Property = (il.hasPrevious()?il.previous():null);
        }

        // запускаем IncrementChanges для этого списка
        for(Property UpdateProperty : UpdateList) {
            Property.Change Change = UpdateProperty.incrementChanges(this, RequiredTypes.get(UpdateProperty));
            // подгоняем тип
            Change.correct(RequiredTypes.get(UpdateProperty));
            Change.save(this);
            PropertyChanges.put(UpdateProperty,Change);
        }

        UpdateClasses.addAll(ToUpdateChanges.AddClasses);
        UpdateClasses.addAll(ToUpdateChanges.RemoveClasses);

        // сбрасываем лог
        IncrementChanges.put(ToUpdate,new DataChanges());

        return IncrementUpdateList;
    }

    void saveClassChanges() throws SQLException {

        for(Integer idObject : NewClasses.keySet()) {
            Map<KeyField,Integer> InsertKeys = new HashMap();
            InsertKeys.put(TableFactory.ObjectTable.Key, idObject);

            Map<PropertyField,Object> InsertProps = new HashMap();
            Class ChangeClass = NewClasses.get(idObject);
            InsertProps.put(TableFactory.ObjectTable.Class,ChangeClass!=null?ChangeClass.ID:null);

            UpdateInsertRecord(TableFactory.ObjectTable,InsertKeys,InsertProps);
        }
    }

    // записывается в запрос с map'ом
    SourceExpr getSourceExpr(Property Property,Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        if(PropertyChanges.containsKey(Property)) {
            String Value = "joinvalue";

            UnionQuery<PropertyInterface,String> UnionQuery = new UnionQuery<PropertyInterface,String>(Property.Interfaces,3);

            JoinQuery<PropertyInterface,String> SourceQuery = new JoinQuery<PropertyInterface,String>(Property.Interfaces);
            SourceQuery.add(Value,Property.getSourceExpr(SourceQuery.MapKeys,true));
            UnionQuery.add(SourceQuery,1);

            JoinQuery<PropertyInterface,String> NewQuery = new JoinQuery<PropertyInterface,String>(Property.Interfaces);
            NewQuery.add(Value,PropertyChanges.get(Property).getExpr(NewQuery.MapKeys,0));
            UnionQuery.add(NewQuery,1);

            return (new Join<PropertyInterface,String>(UnionQuery,JoinImplement,NotNull)).Exprs.get(Value);
        } else
            return Property.getSourceExpr(JoinImplement,NotNull);
    }

    boolean InTransaction = false;

    void startTransaction() throws SQLException {
        InTransaction = true;

        if(!Syntax.noAutoCommit())
            Execute(Syntax.startTransaction());
    }

    void rollbackTransaction() throws SQLException {
        Execute(Syntax.rollbackTransaction());

        InTransaction = false;
    }

    void commitTransaction() throws SQLException {
        Execute(Syntax.commitTransaction());

        InTransaction = false;
    }

    void CreateTable(Table Table) throws SQLException {
        String CreateString = "";
        String KeyString = "";
        for(KeyField Key : Table.Keys) {
            CreateString = (CreateString.length()==0?"":CreateString+',') + Key.GetDeclare(Syntax);
            KeyString = (KeyString.length()==0?"":KeyString+',') + Key.Name;
        }
        for(PropertyField Prop : Table.Properties)
            CreateString = CreateString+',' + Prop.GetDeclare(Syntax);
        CreateString = CreateString + ",CONSTRAINT PK_" + Table.Name + " PRIMARY KEY " + Syntax.getClustered() + " (" + KeyString + ")";

        try {
            Execute("DROP TABLE "+Table.Name+" CASCADE CONSTRAINTS");
        } catch(Exception e) {
        }

//        System.out.println("CREATE TABLE "+Table.Name+" ("+CreateString+")");
        Execute("CREATE TABLE "+Table.Name+" ("+CreateString+")");

        int IndexNum = 1;
        for(List<PropertyField> Index : Table.Indexes) {
            String Columns = "";
            for(PropertyField IndexField : Index)
                Columns = (Columns.length()==0?"":Columns+",") + IndexField.Name;

            Execute("CREATE INDEX "+Table.Name+"_idx_"+(IndexNum++)+" ON "+Table.Name+" ("+Columns+")");
        }
    }

    void CreateTemporaryTable(SessionTable Table) throws SQLException {
        String CreateString = "";
        String KeyString = "";
        for(KeyField Key : Table.Keys) {
            CreateString = (CreateString.length()==0?"":CreateString+',') + Key.GetDeclare(Syntax);
            KeyString = (KeyString.length()==0?"":KeyString+',') + Key.Name;
        }
        for(PropertyField Prop : Table.Properties)
            CreateString = CreateString+',' + Prop.GetDeclare(Syntax);

        try {
            Execute("DROP TABLE "+Table.Name+" CASCADE CONSTRAINTS");
        } catch(Exception e) {
        }

        Execute(Syntax.getCreateSessionTable(Table.Name,CreateString,"CONSTRAINT PK_S_" + ID +"_T_" + Table.Name + " PRIMARY KEY " + Syntax.getClustered() + " (" + KeyString + ")"));
    }

    void Execute(String ExecuteString) throws SQLException {
        Statement Statement = Connection.createStatement();
//        System.out.println(ExecuteString+Syntax.getCommandEnd());
        try {
            Statement.execute(ExecuteString+Syntax.getCommandEnd());
//        } catch(SQLException e) {
//            if(!ExecuteString.startsWith("DROP") && !ExecuteString.startsWith("CREATE")) {
//            System.out.println(ExecuteString+Syntax.getCommandEnd());
//            e = e;
//            }
        } finally {
            Statement.close();
        }
        if(!InTransaction && Syntax.noAutoCommit())
            Statement.execute(Syntax.commitTransaction()+Syntax.getCommandEnd());

        try {
            Statement.close();
        } catch(Exception e) {

        }
    }

    void InsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<PropertyField,Object> PropFields) throws SQLException {

        String InsertString = "";
        String ValueString = "";

        // пробежим по KeyFields'ам
        for(KeyField Key : Table.Keys) {
            InsertString = (InsertString.length()==0?"":InsertString+',') + Key.Name;
            ValueString = (ValueString.length()==0?"":ValueString+',') + KeyFields.get(Key);
        }

        // пробежим по Fields'ам
        for(Field Prop : PropFields.keySet()) {
            Object Value = PropFields.get(Prop);
            InsertString = InsertString+","+Prop.Name;
            ValueString = ValueString+","+TypedObject.getString(PropFields.get(Prop),Prop.Type,Syntax);
        }

        Execute("INSERT INTO "+Table.getSource(Syntax)+" ("+InsertString+") VALUES ("+ValueString+")");
    }

    void UpdateInsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<PropertyField,Object> PropFields) throws SQLException {

        // по сути пустое кол-во ключей
        JoinQuery<Object,String> IsRecQuery = new JoinQuery<Object,String>();

        Join<KeyField,PropertyField> TableJoin = new Join<KeyField,PropertyField>(Table,true);
        // сначала закинем KeyField'ы и прогоним Select
        for(KeyField Key : Table.Keys)
            TableJoin.Joins.put(Key,new ValueSourceExpr(KeyFields.get(Key),Key.Type));
        IsRecQuery.add(TableJoin);

        if(IsRecQuery.executeSelect(this).size()>0) {
            Map<PropertyField,TypedObject> TypedPropFields = new HashMap();
            for(Map.Entry<PropertyField,Object> MapProp : PropFields.entrySet())
                TypedPropFields.put(MapProp.getKey(),new TypedObject(MapProp.getValue(),MapProp.getKey().Type));
            // есть запись нужно Update лупить
            UpdateRecords(new ModifyQuery(Table,new DumbSource<KeyField,PropertyField>(KeyFields,TypedPropFields)));
        } else
            // делаем Insert
            InsertRecord(Table,KeyFields,PropFields);
    }

    void deleteKeyRecords(Table Table,Map<KeyField,Integer> Keys) throws SQLException {
 //       Execute(Table.GetDelete());
        String DeleteWhere = "";
        for(Map.Entry<KeyField,Integer> DeleteKey : Keys.entrySet())
            DeleteWhere = (DeleteWhere.length()==0?"":DeleteWhere+" AND ") + DeleteKey.getKey().Name + "=" + DeleteKey.getValue();

        Execute("DELETE FROM "+Table.getSource(Syntax)+(DeleteWhere.length()==0?"":" WHERE "+DeleteWhere));
    }

    void UpdateRecords(ModifyQuery Modify) throws SQLException {
//        try {
            Execute(Modify.getUpdate(Syntax));
//        } catch(SQLException e) {
//            Execute(Modify.getUpdate(Syntax));
//        }
    }

    void InsertSelect(ModifyQuery Modify) throws SQLException {
        Execute(Modify.getInsertSelect(Syntax));
    }

    // сначала делает InsertSelect, затем UpdateRecords
    void ModifyRecords(ModifyQuery Modify) throws SQLException {
        Execute(Modify.getInsertLeftKeys(Syntax));
        Execute(Modify.getUpdate(Syntax));
    }

    void close() throws SQLException {
        Connection.close();
    }

    public boolean hasChanges() {
        return Changes.hasChanges();
    }
}

class AddClasses extends HashSet<Class> {

    void and(AddClasses Op) {

    }

    void or(AddClasses Op) {

    }

}

class InterfaceAddClasses<T extends PropertyInterface> extends HashMap<T,AddClasses> {

    void or(InterfaceAddClasses ToAdd) {
    }

    void and(InterfaceAddClasses ToAdd) {
    }

}

class MapRead {
    // делает разные вызовы Changed/Main
    SourceExpr getImplementExpr(PropertyInterfaceImplement Implement,Map<PropertyInterface,SourceExpr> JoinImplement) {
        return Implement.mapSourceExpr(JoinImplement,true);
    }

    SourceExpr getMapExpr(Property MapProperty,Map<PropertyInterface,SourceExpr> JoinImplement) {
        return MapProperty.getSourceExpr(JoinImplement,true);
    }

    Join exclude(PropertyInterfaceImplement Implement, Map<PropertyInterface, SourceExpr> JoinImplement) {
        return null;
    }
}

class MapChangedRead extends MapRead {

    MapChangedRead(DataSession iSession, boolean iMapChanged, int iMapType, int iImplementType, Collection<PropertyInterfaceImplement> iImplementChanged) {
        Session = iSession;
        MapChanged = iMapChanged;
        MapType = iMapType;
        ImplementType = iImplementType;
        ImplementChanged = iImplementChanged;
    }

    MapChangedRead(DataSession iSession, boolean iMapChanged, int iMapType, int iImplementType, PropertyInterfaceImplement iImplementChanged) {
        Session = iSession;
        MapChanged = iMapChanged;
        MapType = iMapType;
        ImplementType = iImplementType;
        ImplementChanged = new ArrayList();
        ImplementChanged.add(iImplementChanged);
    }

    DataSession Session;

    boolean MapChanged;
    int MapType;

    boolean ExcludeNotChanged = false;

    Collection<PropertyInterfaceImplement> ImplementChanged;
    int ImplementType;

    // делает разные вызовы Changed/Main
    SourceExpr getImplementExpr(PropertyInterfaceImplement Implement, Map<PropertyInterface, SourceExpr> JoinImplement) {
        if(ImplementChanged.contains(Implement))
            return Implement.mapChangedExpr(JoinImplement, Session,ImplementType);
        else
            return super.getImplementExpr(Implement, JoinImplement);    //To change body of overridden methods use File | Settings | File Templates.
    }

    SourceExpr getMapExpr(Property MapProperty, Map<PropertyInterface, SourceExpr> JoinImplement) {
        if(MapChanged)
            return Session.PropertyChanges.get(MapProperty).getExpr(JoinImplement,MapType);
        else
            return MapProperty.getSourceExpr(JoinImplement,MapType!=1);
    }

    Join exclude(PropertyInterfaceImplement Implement, Map<PropertyInterface, SourceExpr> JoinImplement) {
        if(!ExcludeNotChanged || ImplementChanged.contains(Implement) || !Implement.MapHasChanges(Session)) return null;

        return ((PropertyMapImplement)Implement).mapChangedJoin(JoinImplement, Session, false);
    }

}

// св-ва которые связывают другие св-ва друг с другом
// InterfaceClass = T - Join, M - Group
// ImplementClass = M - Join, T - Group
// ObjectMapClass = PropertyField - Join, Object - Group
abstract class MapProperty<T extends PropertyInterface,M extends PropertyInterface,IN extends PropertyInterface,IM extends PropertyInterface,OM> extends AggregateProperty<T> {

    MapProperty(TableFactory iTableFactory) {
        super(iTableFactory);
    }

    // получает св-во для Map'а
    // Join - return Implements.Property
    // Group - return GroupProperty
    abstract Property<M> getMapProperty();

    // получает список имплементаций
    // Join - return Implements.Mapping
    // Group бежит по GroupPropertyInterface и возвращает сформированный Map
    abstract Map<IM,PropertyInterfaceImplement> getMapImplements();

    // получает список интерфейсов
    // Join - return Interfaces
    // Group - return GroupProperty.Interfaces
    abstract Collection<IN> getMapInterfaces();

    // транслирует\выбирает нужный класс - трансляторы для выбора
    // для Join - возвращает интерфейсы
    // для Group - наоборот
    abstract DumbInterfaceChange<T> getMapPropertyInterfaces(DumbInterfaceChange<IN> InterfaceDumb, DumbInterfaceChange<IM> ImplementDumb);
    abstract DumbInterfaceChange<M> getMapPropertyImplements(DumbInterfaceChange<IN> InterfaceDumb, DumbInterfaceChange<IM> ImplementDumb);

    // ПРОВЕРКА\ЗАПОЛНЕНИЕ ADDCLASSES - а вот здесь много чего общего
    // возвращает InterfaceAddClasses<T>, null если Read не прошел проверку по AddClasses то есть заведомо Empty
    DumbChange<T> getDumbChange(MapChangedRead Read) {

        // бежим по всем изм. PropertyMapImplement, заполняем InterfaceAddClasses <InterfaceClass> интерфейсов на OR(+)
        // бежим по всем не изм. PropertyMapImplement, вызываем isRequired InterfaceAddClasses интерфейсов, если что-тоо не так вываливаемся

        // подготавливаем InterfaceAddClasses getMapImplements <ImplementClass>:
        //      для изм. PropertyMapImplement берем getChangedAddClasses !!!! если конечно на 0,1 идет иначе как и не изм.
        //      для не изм. PropertyMapImplement берем пустой ClassSet
        //      для Interface берем ClassSet из InterfaceAddClasses интерфейсов

        for(PropertyInterfaceImplement Implement : Read.ImplementChanged)
            if(!Implement.MapHasChanges(Read.Session)) return null;
        if(Read.MapChanged && !Read.Session.PropertyChanges.containsKey(getMapProperty())) return null;

        DumbInterfaceChange<IN> InterfaceDumb = new DumbInterfaceChange<IN>();

        for(PropertyInterfaceImplement Implement : Read.ImplementChanged)
            if(Implement instanceof PropertyMapImplement)
                InterfaceDumb.or(((PropertyMapImplement)Implement).mapDumbInterfaceChange(Read.Session));

        for(PropertyInterfaceImplement Implement : getMapImplements().values())
            if(Implement instanceof PropertyMapImplement && !Read.ImplementChanged.contains(Implement) && ((PropertyMapImplement)Implement).mapIsRequired(InterfaceDumb.getInteraceAddClasses()))
                return null;

        DumbInterfaceChange<IM> ImplementDumb = new DumbInterfaceChange<IM>();
        for(Map.Entry<IM,PropertyInterfaceImplement> Implement : getMapImplements().entrySet())
            ImplementDumb.put(Implement.getKey(),Implement.getValue().mapDumbValue(Read,InterfaceDumb));

//        if(this instanceof JoinQuery) (M = ImplementClass, T = InterfaceClass)
                // на проверку InterfaceAddClasses <ImplementClass> имплементаций
                // на выход InterfaceAddClasses <InterfaceClass> интерфейсов
//        else (M = InterfaceClass, T = ImplementClass)
                // на проверку InterfaceAddClasses <InterfaceClass> интерфейсов
                // на выход InterfaceAddClasses <ImplementClass> имплементаций
        // если св-во не измененное и не идет добавление
        if(!Read.MapChanged && getMapProperty().isRequired(getMapPropertyImplements(InterfaceDumb, ImplementDumb).getInteraceAddClasses())) return null;
        return new DumbChange<T>(getMapPropertyInterfaces(InterfaceDumb, ImplementDumb),Read.MapChanged && Read.MapType!=2?Read.Session.PropertyChanges.get(getMapProperty()).Dumb.Value:new DumbImplementChange());
    }

    // "сохраняет" имплементации в запрос
    // Join - закидываем в Value getExpr'ы (Changed,SourceExpr) map'a импллементаций
    // Group - закидываем в запрос map имплементаций
    //          закидываем в Value getMapExpr'ы (Changed,SourceExpr) map'а интерфейсов (Query.MapKeys)
    abstract void putImplementsToQuery(JoinQuery<IN,OM> Query,OM Value,MapRead Read,Map<IM,SourceExpr> Implements);

    // ВЫПОЛНЕНИЕ ИТЕРАЦИИ
    JoinQuery<IN,OM> getMapQuery(MapRead Read,OM Value) {

        // создается JoinQuery - на вход getMapInterfaces, Query.MapKeys - map интерфейсов
        JoinQuery<IN,OM> Query = new JoinQuery<IN,OM>(getMapInterfaces());

        // далее создается для getMapImplements - map <ImplementClass,SourceExpr> имплементаций - по getExpr'ы (Changed,SourceExpr) с переданным map интерфейсов
        Map<IM,SourceExpr> ImplementSources = new HashMap();
        for(Map.Entry<IM,PropertyInterfaceImplement> Implement : getMapImplements().entrySet()) {
            ImplementSources.put(Implement.getKey(),Read.getImplementExpr(Implement.getValue(),(Map<PropertyInterface,SourceExpr>) Query.MapKeys));
            Join ExcludeJoin = Read.exclude(Implement.getValue(), (Map<PropertyInterface, SourceExpr>) Query.MapKeys);
            if(ExcludeJoin!=null) Query.add(new NotWhere(ExcludeJoin.InJoin));
        }

        putImplementsToQuery(Query,Value,Read,ImplementSources);
        return Query;
    }

    // получает св-ва для запроса
    abstract Map<OM, Type> getMapNullProps(OM Value);

    // ВЫПОЛНЕНИЕ СПИСКА ИТЕРАЦИЙ

    Source<IN,OM> getMapQuery(List<MapChangedRead> ReadList, OM Value, DumbChange<T> DumbChange, boolean Sum) {

        // делаем getQuery для всех итераций, после чего Query делаем Union на 3, InterfaceAddClasses на AND(*), Value на AND(*)
        UnionQuery<IN, OM> ListQuery = new UnionQuery<IN, OM>(getMapInterfaces(),Sum?1:3);
        for(MapChangedRead Read : ReadList) {
            DumbChange<T> ReadDumbChange = getDumbChange(Read);
            if(ReadDumbChange!=null) {
                DumbChange.and(ReadDumbChange);
                ListQuery.add(getMapQuery(Read,Value),1);
            } else
                // иначе EmptySource, соответственно в ValueClasses ничего не пишем
                ListQuery.add(new EmptySource<IN,OM>(getMapInterfaces(),getMapNullProps(Value)),1);
        }

        return ListQuery;
    }

    // get* получают списки итераций чтобы потом отправить их на выполнение:

    MapChangedRead getImplementSet(DataSession Session,List<PropertyInterfaceImplement> SubSet,int ImplementType) {
        // если не все интерфейсы то на Inner с exclude'ом, иначе на Left
        if(!implementAllInterfaces() && ImplementType==0) {
            MapChangedRead Read = new MapChangedRead(Session, false, 0, 0, SubSet);
            Read.ExcludeNotChanged = true;
            return Read;
        } else
            return new MapChangedRead(Session, false, 1, ImplementType, SubSet);
    }

    // новое состояние
    List<MapChangedRead> getChange(DataSession Session) {
        List<MapChangedRead> ChangedList = new ArrayList();
        for(List<PropertyInterfaceImplement> SubSet : (new SetBuilder<PropertyInterfaceImplement>()).BuildSubSetList(getMapImplements().values())) {
            if(SubSet.size()>0)
                ChangedList.add(getImplementSet(Session, SubSet, 0));
            ChangedList.add(new MapChangedRead(Session,true,0,0,SubSet));
        }
        return ChangedList;
    }

    // новое состояние с измененным основным значением
    // J - C (0,1) - SS+ (0)
    List<MapChangedRead> getChangeMap(DataSession Session, int MapType) {
        List<MapChangedRead> ChangedList = new ArrayList();
        for(List<PropertyInterfaceImplement> SubSet : (new SetBuilder<PropertyInterfaceImplement>()).BuildSubSetList(getMapImplements().values()))
            ChangedList.add(new MapChangedRead(Session,true,MapType,0,SubSet));
        return ChangedList;
    }
    // новое значение для имплементаций, здесь если не все имплементации придется извращаться и exclude'ать все не измененные выражения
    // LJ - P - SS (0,1)
    List<MapChangedRead> getChangeImplements(DataSession Session,int ImplementType) {
        List<MapChangedRead> ChangedList = new ArrayList();
        for(List<PropertyInterfaceImplement> SubSet : (new SetBuilder<PropertyInterfaceImplement>()).BuildSubSetList(getMapImplements().values()))
            if(SubSet.size()>0)
                ChangedList.add(getImplementSet(Session, SubSet, ImplementType));

        return ChangedList;
    }
    // предыдущие значения по измененным объектам
    // J - P - L(2)
    List<MapChangedRead> getPreviousImplements(DataSession Session) {
        List<MapChangedRead> ChangedList = new ArrayList();
        for(PropertyInterfaceImplement Implement : getMapImplements().values())
            ChangedList.add(new MapChangedRead(Session,false,0,2,Implement));
        return ChangedList;
    }
    // предыдущие значения по измененному основному (для MaxGroup'а надо)
    // J - C(2) - P
    MapChangedRead getPreviousMap(DataSession Session) {
        return new MapChangedRead(Session,true,2,0,new ArrayList());
    }
    // чтобы можно было бы использовать в одном списке
    MapChangedRead getPrevious(DataSession Session) {
        return new MapChangedRead(Session,false,0,0,new ArrayList());
    }
    // значение из базы (можно и LJ)
    // J - P - P
    MapRead getDB() {
        return new MapRead();
    }

    // получает источник для данных
    abstract OM getDefaultObject();
    abstract Source<PropertyInterface,OM> getMapSourceQuery(OM Value);

    SourceExpr calculateSourceExpr(Map<PropertyInterface, SourceExpr> JoinImplement, boolean NotNull) {
        OM Value = getDefaultObject();
        return (new Join<PropertyInterface,OM>(getMapSourceQuery(Value),JoinImplement,NotNull)).Exprs.get(Value);
    }

    // заполняет список, возвращает есть ли изменения
    boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = getMapProperty().fillChangedList(ChangedProperties, Changes);

        for(PropertyInterfaceImplement Implement : getMapImplements().values())
            Changed = Implement.mapFillChangedList(ChangedProperties, Changes) || Changed;

        if(Changed)
            ChangedProperties.add(this);

        return Changed;
    }

    boolean containsImplement(Set<Property> Properties) {
        for(PropertyInterfaceImplement Implement : getMapImplements().values())
            if(Implement instanceof PropertyMapImplement && Properties.contains(((PropertyMapImplement)Implement).Property))
                return true;
        return false;
    }

    //
    boolean implementAllInterfaces() {
        Set<PropertyInterface> ImplementInterfaces = new HashSet();
        for(PropertyInterfaceImplement InterfaceImplement : getMapImplements().values()) {
            if(InterfaceImplement instanceof PropertyMapImplement)
                ImplementInterfaces.addAll(((PropertyMapImplement)InterfaceImplement).Mapping.values());
        }

        return ImplementInterfaces.size()==getMapInterfaces().size();
    }

}

// хранит заранее известные ограничения на изменения
class DumbImplementChange {
    AddClasses AddClasses;
    Integer Object;
}

class DumbInterfaceChange<T extends PropertyInterface> extends HashMap<T,DumbImplementChange> {

    InterfaceAddClasses<T> getInteraceAddClasses() {
        return new InterfaceAddClasses<T>();
    }

    void and(DumbInterfaceChange<T> ToAdd) {
    }

    void or(DumbInterfaceChange<T> ToAdd) {
    }

}

class DumbChange<T extends PropertyInterface> {

    DumbChange() {
        Interface = new DumbInterfaceChange<T>();
        Value = new DumbImplementChange();
    }

    DumbChange(DumbInterfaceChange<T> iInterface, DumbImplementChange iValue) {
        Interface = iInterface;
        Value = iValue;
    }

    DumbInterfaceChange<T> Interface;
    DumbImplementChange Value;

    void and(DumbChange<T> ToAdd) {
    }

    void or(DumbChange<T> ToAdd) {
    }
}
