/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.util.*;

class ObjectValue {
    Integer idObject;
    Class Class;
    
    ObjectValue(Integer iObject,Class iClass) {idObject=iObject;Class=iClass;}
}

class PropertyImplement<P extends Property,T> {
    
    PropertyImplement(P iProperty) {
        Property = iProperty;
        Mapping = new HashMap<PropertyInterface,T>();
    }
    
    P Property;
    Map<PropertyInterface,T> Mapping;
}

interface PropertyInterfaceImplement {

    public SourceExpr mapSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull,ChangesSession Session,int Value);
    public Class MapGetValueClass(InterfaceClass ClassImplement);
    public InterfaceClassSet MapGetClassSet(Class ReqValue);

    // для increment'ного обновления
    public boolean MapHasChanges(ChangesSession Session);
    
    abstract boolean MapFillChangedList(List<ObjectProperty> ChangedProperties,ChangesSession Session);

}
        

class PropertyInterface implements PropertyInterfaceImplement {
    //можно использовать JoinExps потому как все равну вернуться она не может потому как иначе она зациклится
    
    public SourceExpr mapSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull,ChangesSession Session,int Value) {
        return JoinImplement.get(this);
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
    
    public boolean MapHasChanges(ChangesSession Session) {
        return false;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean MapFillChangedList(List<ObjectProperty> ChangedProperties,ChangesSession Session) {
        return false;
    }
}


abstract class Property<T extends PropertyInterface> {

    int ID=0;
    
    // чтобы подчеркнуть что не направленный
    Collection<T> Interfaces = new ArrayList();
    // кэшируем здесь а не в JoinList потому как быстрее
    // работает только для JOIN смотри ChangedJoinSelect
    Map<Map<PropertyInterface,SourceExpr>,SourceExpr> SelectCacheJoins = new HashMap();
    
    // закэшируем чтобы быстрее работать
    // здесь как и в произвольных Left значит что могут быть null, не Left соответственно только не null
    // (пока в нашем случае просто можно убирать записи где точно null)
    public SourceExpr getSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        SourceExpr CacheJoins = SelectCacheJoins.get(JoinImplement);

        // не будем проверять что все интерфейсы реализованы все равно null в map не попадет
        SourceExpr JoinExpr = SelectCacheJoins.get(JoinImplement);
        if(JoinExpr==null) {
            JoinExpr = proceedSourceExpr(JoinImplement,NotNull);
            SelectCacheJoins.put(JoinImplement,JoinExpr);
        }

        return JoinExpr;
    }

    abstract SourceExpr proceedSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull);

    // возвращает класс значения
    // если null то не подходит по интерфейсу
    abstract public Class GetValueClass(InterfaceClass ClassImplement);
    
    // возвращает то и только то мн-во интерфейсов которые заведомо дают этот интерфейс (GetValueClass >= ReqValue)
    // если null то когда в принципе дает значение
    abstract public InterfaceClassSet GetClassSet(Class ReqValue);
    
    abstract public String GetDBType();
    
    // для отладки
    String OutName = "";
    
    abstract boolean HasChanges(ChangesSession Session);

    // заполняет список, возвращает есть ли изменения
    abstract boolean FillChangedList(List<ObjectProperty> ChangedProperties,ChangesSession Session);
    
    JoinQuery<PropertyInterface,String> getOutSelect(DataAdapter Adapter,String Value) {
        JoinQuery<PropertyInterface,String> Query = new JoinQuery(Interfaces);
        Query.Properties.put(Value,getSourceExpr(Query.MapKeys,true));
        return Query;
    }
    
    void Out(DataAdapter Adapter) throws SQLException {
        System.out.println(OutName);
        getOutSelect(Adapter,"value").outSelect(Adapter);
    }
}


abstract class ObjectProperty<T extends PropertyInterface> extends Property<T> {

    ObjectProperty(TableFactory iTableFactory) {
        super();
        TableFactory = iTableFactory;
        SessionChanged = new HashMap();
    }
    TableFactory TableFactory;
    
    // для Increment'ного обновления (какой вид изменений есть\нужен)
    // 0 - =
    // 1 - +
    // 2 - new\prev
    Map<ChangesSession,Integer> SessionChanged;
    
    boolean HasChanges(ChangesSession Session) {
        return SessionChanged.containsKey(Session);
    }
    
    // для преобразования типов а то странно работает
    Integer GetChangeType(ChangesSession Session) {
        return SessionChanged.get(Session);
    }
    
    void SetChangeType(ChangesSession Session,int ChangeType) {
        // 0 и 0 = 0
        // 0 и 1 = 2
        // 1 и 1 = 1
        // 2 и x = 2
        Integer PrevType = GetChangeType(Session);
        if(PrevType!=null && !PrevType.equals(ChangeType)) ChangeType = 2;
        SessionChanged.put(Session,ChangeType);
    }
    
    // строится по сути "временный" Map PropertyInterface'ов на Objects'ы
    Map<PropertyInterface,KeyField> ChangeTableMap = null;
    // раз уж ChangeTableMap закэшировали то и ChangeTable тоже
    IncrementChangeTable ChangeTable;
    
    void FillChangeTable() {
        ChangeTable = TableFactory.GetChangeTable(Interfaces.size(),GetDBType());
        ChangeTableMap = new HashMap();
        Iterator<KeyField> io = ChangeTable.Objects.iterator();
        for(T Interface : Interfaces)
            ChangeTableMap.put(Interface,io.next());
    }

    // вычищает все из сессии
    void StartChangeTable(DataAdapter Adapter,ChangesSession Session) throws SQLException {

        Map<KeyField,Integer> ValueKeys = new HashMap();
        ValueKeys.put(ChangeTable.Session,Session.ID);
        ValueKeys.put(ChangeTable.Property,ID);
        Adapter.deleteKeyRecords(ChangeTable,ValueKeys);
    }
    
    // подготавливает Join примененных изменений
    Join<KeyField,PropertyField> getChangedValueJoin(Map<PropertyInterface,SourceExpr> JoinImplement,ChangesSession Session) {

        Join<KeyField,PropertyField> From = new Join<KeyField,PropertyField>(ChangeTable);
        From.Joins.put(ChangeTable.Session,new ValueSourceExpr(Session.ID));
        From.Joins.put(ChangeTable.Property,new ValueSourceExpr(ID));
        for(T Interface : Interfaces)
            From.Joins.put(ChangeTableMap.get(Interface),JoinImplement.get(Interface));

        return From;
    }

    // подготавливает JoinExpr
    SourceExpr getChangedValueExpr(Map<PropertyInterface,SourceExpr> JoinImplement,ChangesSession Session,PropertyField Value) {

        return new JoinExpr<KeyField,PropertyField>(getChangedValueJoin(JoinImplement,Session),Value,true);
    }

    // связывает именно измененные записи из сессии
    // Value - что получать, 0 - новые значения, 1 - +(увеличение), 2 - старые значения
    SourceExpr getChangedSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,ChangesSession Session,int Value) {

        int ChangedType = GetChangeType(Session);
        // теперь определимся что возвращать
        if(Value==2 && ChangedType==2)
            return getChangedValueExpr(JoinImplement,Session,ChangeTable.PrevValue);

        if(Value==ChangedType || (Value==0 && ChangedType==2))
            return getChangedValueExpr(JoinImplement,Session,ChangeTable.Value);

        if(Value==1 && ChangedType==2) {
            UnionSourceExpr Result = new UnionSourceExpr(1);
            Result.Operands.put(getChangedValueExpr(JoinImplement,Session,ChangeTable.Value),1);
            Result.Operands.put(getChangedValueExpr(JoinImplement,Session,ChangeTable.PrevValue),-1);
            return Result;
        }

        throw new RuntimeException();
    }

    // записывается в запрос с map'ом
    SourceExpr getUpdatedSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,ChangesSession Session,boolean NotNull) {

        if(Session!=null && HasChanges(Session)) {
            String Value = "joinvalue";

            UnionQuery<PropertyInterface,String> UnionQuery = new UnionQuery<PropertyInterface,String>((Collection<PropertyInterface>)Interfaces,3);

            JoinQuery<PropertyInterface,String> SourceQuery = UnionQuery.newJoinQuery(1);
            SourceQuery.Properties.put(Value,getSourceExpr(SourceQuery.MapKeys,true));

            JoinQuery<PropertyInterface,String> NewQuery = UnionQuery.newJoinQuery(1);
            NewQuery.Properties.put(Value,getChangedSourceExpr(NewQuery.MapKeys,Session,0));

            return new JoinExpr<PropertyInterface,String>(new Join<PropertyInterface,String>(UnionQuery,JoinImplement),Value,NotNull);
        } else
            return getSourceExpr(JoinImplement,NotNull);
    }
    
    void OutChangesTable(DataAdapter Adapter, ChangesSession Session) throws SQLException {
        JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);

        Join<KeyField,PropertyField> ChangeJoin = new MapJoin<KeyField,PropertyField,PropertyInterface>(ChangeTable,Query,ChangeTableMap);
        ChangeJoin.Joins.put(ChangeTable.Session,new ValueSourceExpr(Session.ID));
        ChangeJoin.Joins.put(ChangeTable.Property,new ValueSourceExpr(ID));

        Query.Properties.put(ChangeTable.Value,new JoinExpr<KeyField,PropertyField>(ChangeJoin,ChangeTable.Value,true));
        Query.Properties.put(ChangeTable.PrevValue,new JoinExpr<KeyField,PropertyField>(ChangeJoin,ChangeTable.PrevValue,true));

        Query.outSelect(Adapter);
    }

    // сохраняет изменения в таблицу
    void SaveChanges(DataAdapter Adapter,ChangesSession Session) throws SQLException {

        // если не изменились ничего не делаем
        if(!HasChanges(Session)) return;

        Map<KeyField,T> MapKeys = new HashMap();
        Table SourceTable = GetTable(MapKeys);

        JoinQuery<KeyField,PropertyField> ModifyQuery = new JoinQuery<KeyField,PropertyField>(SourceTable.Keys);

        Join<KeyField,PropertyField> Update = new Join<KeyField,PropertyField>(ChangeTable);
        for(KeyField Key : SourceTable.Keys)
            Update.Joins.put(ChangeTableMap.get(MapKeys.get(Key)),ModifyQuery.MapKeys.get(Key));
        Update.Joins.put(ChangeTable.Session,new ValueSourceExpr(Session.ID));
        Update.Joins.put(ChangeTable.Property,new ValueSourceExpr(ID));

        ModifyQuery.Properties.put(Field,new JoinExpr<KeyField,PropertyField>(Update,ChangeTable.Value,true));
        Adapter.ModifyRecords(new ModifyQuery(SourceTable,ModifyQuery));
    }

    
    PropertyField Field;
    abstract Table GetTable(Map<KeyField,T> MapJoins);
    
    boolean IsPersistent() {
        return Field!=null && !(this instanceof AggregateProperty && TableFactory.ReCalculateAggr); // для тестирования 2-е условие
    }
            
    // при текущей реализации проше предполагать что не имплементнутые Interface имеют null Select !!!!!
    SourceExpr proceedSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {
        if(IsPersistent()) {
            // если persistent читаем из таблицы
            Map<KeyField,T> MapJoins = new HashMap();
            Table SourceTable = GetTable(MapJoins);

            // прогоним проверим все ли Implement'ировано
            Join<KeyField,PropertyField> SourceJoin = new Join<KeyField,PropertyField>(SourceTable);
            for(KeyField Key : SourceTable.Keys)
                SourceJoin.Joins.put(Key,JoinImplement.get(MapJoins.get(Key)));

            return new JoinExpr<KeyField,PropertyField>(SourceJoin,Field,NotNull);
        } else {
            return ((AggregateProperty)this).calculateSourceExpr(JoinImplement,NotNull);
        }
    }

    // базовый метод - ничего не делать, его перегружают только Override и Data
    void ChangeProperty(DataAdapter Adapter,Map<PropertyInterface,ObjectValue> Keys,Object NewValue,ChangesSession Session) throws SQLException {}
    
    // заполняет требования к изменениям
    abstract void FillRequiredChanges(ChangesSession Session);

    // заполняет инкрементные изменения
    void IncrementChanges(DataAdapter Adapter, ChangesSession Session) throws SQLException {

        // удалим старые данные
        StartChangeTable(Adapter,Session);

        Query<PropertyInterface,PropertyField> ResultQuery = QueryIncrementChanged(Session);

        // проверим что вернули что вернули то что надо, "подчищаем" если не то
        int ChangeType = GetChangeType(Session);
        // если вернул 2 запишем
        if(QueryIncrementType==2 || (QueryIncrementType!=ChangeType)) {
            ChangeType = 2;
            SessionChanged.put(Session,2);
        }
                
        if(QueryIncrementType != ChangeType) {
            JoinQuery<PropertyInterface,PropertyField> NewQuery = new JoinQuery<PropertyInterface,PropertyField>((Collection<PropertyInterface>) Interfaces);
            SourceExpr NewExpr = new JoinExpr<PropertyInterface,PropertyField>(new UniJoin<PropertyInterface,PropertyField>(ResultQuery,NewQuery),ChangeTable.Value,true);
            // нужно LEFT JOIN'ить старые
            SourceExpr PrevExpr = getSourceExpr(NewQuery.MapKeys,false);
            // по любому 2 нету надо докинуть
            NewQuery.Properties.put(ChangeTable.PrevValue,PrevExpr);
            if(QueryIncrementType==1) {
                // есть 1, а надо по сути 0
                UnionSourceExpr SumExpr = new UnionSourceExpr(1);
                SumExpr.Operands.put(NewExpr,1);
                SumExpr.Operands.put(PrevExpr,1);
                NewExpr = SumExpr;
            }
            NewQuery.Properties.put(ChangeTable.Value,NewExpr);
            ResultQuery = NewQuery;
        }

//        System.out.println(OutName);
//        ResultQuery.outSelect(Adapter);
        Adapter.InsertSelect(modifyIncrementChanges(Session,ResultQuery,ChangeType));
    }

    // вынесем в отдельный метод потому как используется в 2 местах
    ModifyQuery modifyIncrementChanges(ChangesSession Session,Source<PropertyInterface,PropertyField> Query,int ChangeType) {

        JoinQuery<KeyField,PropertyField> WriteQuery = new JoinQuery<KeyField,PropertyField>(ChangeTable.Keys);
        Join<PropertyInterface,PropertyField> ResultJoin = new MapJoin<PropertyInterface,PropertyField,KeyField>(Query,ChangeTableMap,WriteQuery);
        WriteQuery.Properties.put(ChangeTable.Value,new JoinExpr<PropertyInterface,PropertyField>(ResultJoin,ChangeTable.Value,true));
        if(ChangeType>=2)
            WriteQuery.Properties.put(ChangeTable.PrevValue,new JoinExpr<PropertyInterface,PropertyField>(ResultJoin,ChangeTable.PrevValue,true));
        if(ChangeType>=3)
            WriteQuery.Properties.put(ChangeTable.SysValue,new JoinExpr<PropertyInterface,PropertyField>(ResultJoin,ChangeTable.SysValue,true));

        Map<KeyField,Integer> ValueKeys = new HashMap();
        ValueKeys.put(ChangeTable.Session,Session.ID);
        ValueKeys.put(ChangeTable.Property,ID);
        WriteQuery.putDumbJoin(ValueKeys);

        return new ModifyQuery(ChangeTable,WriteQuery);
    }

    // для возврата чтобы не плодить классы
    int QueryIncrementType;
    // получает запрос для инкрементных изменений
    abstract Query<PropertyInterface, PropertyField> QueryIncrementChanged(ChangesSession Session);

    // присоединяют объекты
    void joinChangeClass(ChangeClassTable Table,JoinQuery<PropertyInterface,?> Query,ChangesSession Session,DataPropertyInterface Interface) {
        Join<KeyField,PropertyField> ClassJoin = new Join<KeyField,PropertyField>(Table.getClassJoin(Session,Interface.Class));
        ClassJoin.Joins.put(Table.Object,Query.MapKeys.get(Interface));

        Query.Wheres.add(new JoinWhere(ClassJoin));
    }

    void joinObjects(JoinQuery<PropertyInterface,?> Query,DataPropertyInterface Interface) {
        Join<KeyField,PropertyField> ClassJoin = new Join<KeyField,PropertyField>(TableFactory.ObjectTable.getClassJoin(Interface.Class));
        ClassJoin.Joins.put(TableFactory.ObjectTable.Key,Query.MapKeys.get(Interface));

        Query.Wheres.add(new JoinWhere(ClassJoin));
    }
}
    
class DataPropertyInterface extends PropertyInterface {
    Class Class;
    
    DataPropertyInterface(Class iClass) {
        Class = iClass;
    }
}        


class DataProperty extends ObjectProperty<DataPropertyInterface> {
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

    public String GetDBType() {
        return Value.GetDBType();
    }

    // свойства для "ручных" изменений пользователем
    DataChangeTable DataTable;
    Map<KeyField,PropertyInterface> DataTableMap = null;

    void FillDataTable() {
        DataTable = TableFactory.GetDataChangeTable(Interfaces.size(),GetDBType());
        // если нету Map'a построим
        DataTableMap = new HashMap();
        Iterator<KeyField> io = DataTable.Objects.iterator();
        for(DataPropertyInterface Interface : Interfaces)
            DataTableMap.put(io.next(),Interface);
    }
    
    @Override
    void ChangeProperty(DataAdapter Adapter,Map<PropertyInterface,ObjectValue> Keys,Object NewValue,ChangesSession Session) throws SQLException {
        // записываем в таблицу изменений
        Map<KeyField,Integer> InsertKeys = new HashMap();
        for(KeyField Field : DataTableMap.keySet())
            InsertKeys.put(Field,Keys.get(DataTableMap.get(Field)).idObject);

        InsertKeys.put(DataTable.Property,ID);
        InsertKeys.put(DataTable.Session,Session.ID);
        
        if(NewValue instanceof Integer && ((Integer)NewValue).equals(0))
            NewValue = null;

        if(NewValue instanceof String && ((String)NewValue).equals("0"))
            NewValue = null;

        Map<PropertyField,Object> InsertValues = new HashMap();
        InsertValues.put(DataTable.Value,NewValue);

        Adapter.UpdateInsertRecord(DataTable,InsertKeys,InsertValues);

        // пометим изменение св-ва
        Session.Properties.add(this);
    }

    // св-во по умолчанию (при AddClasses подставляется)
    ObjectProperty DefaultProperty;
    // map интерфейсов на PropertyInterface
    Map<DataPropertyInterface,PropertyInterface> DefaultMap;
    // если нужно еще за изменениями следить и перебивать
    boolean OnDefaultChange;
    
    // заполняет список, возвращает есть ли изменения
    public boolean FillChangedList(List<ObjectProperty> ChangedProperties,ChangesSession Session) {
        // если null то значит полный список запрашивают 
        if(Session==null) return true;

        boolean Changed = false;
        if(!Changed)
            if(Session.Properties.contains(this)) Changed = true;

        if(!Changed)
            for(DataPropertyInterface Interface : Interfaces)
                if(Session.RemoveClasses.contains(Interface.Class)) Changed = true;

        if(!Changed)
            if(Session.RemoveClasses.contains(Value)) Changed = true;

        if(DefaultProperty!=null) {
            Changed = (DefaultProperty.FillChangedList(ChangedProperties, Session) && OnDefaultChange) || Changed;
            
            if(!Changed)
                for(DataPropertyInterface Interface : Interfaces)
                    if(Session.AddClasses.contains(Interface.Class)) Changed = true;
        }            
        
        if(Changed) {
            ChangedProperties.add(this);
            return true;
        } else        
            return false;
    }

    void FillRequiredChanges(ChangesSession Session) {

        // если на изм. надо предыдущее изменение иначе просто на =
        // пока неясно после реализации QueryIncrementChanged станет яснее
        if(DefaultProperty!=null)
            DefaultProperty.SetChangeType(Session,OnDefaultChange?2:0);
    }

    // заполним старыми значениями (LEFT JOIN'ом)
    Query<PropertyInterface, PropertyField> QueryIncrementChanged(ChangesSession Session) {

        // на 3 то есть слева/направо
        UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,3);

        // Default изменения (пока Add)
        if(DefaultProperty!=null) {
            if(!OnDefaultChange) {
                // бежим по всем добавленным интерфейсам
                for(DataPropertyInterface Interface : Interfaces) 
                    if(Session.AddClasses.contains(Interface.Class)) {
                        JoinQuery<PropertyInterface,PropertyField> Query = ResultQuery.newJoinQuery(1);
                        Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
                        // "перекодируем" в базовый интерфейс
                        for(DataPropertyInterface DataInterface : Interfaces)
                            JoinImplement.put(DefaultMap.get(DataInterface),Query.MapKeys.get(DataInterface));

                        // вкидываем "новое" состояние DefaultProperty с Join'ое с AddClassTable
                        // если DefaultProperty требует на входе такой добавляемый интерфейс то можно чисто новое брать
                        joinChangeClass(TableFactory.AddClassTable,Query,Session,Interface);

                        Query.Properties.put(ChangeTable.Value,DefaultProperty.getUpdatedSourceExpr(JoinImplement,Session,true));
                    }
            } else {
                if(DefaultProperty.HasChanges(Session)) {
                    JoinQuery<PropertyInterface,PropertyField> Query = ResultQuery.newJoinQuery(1);
                    Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
                    // "перекодируем" в базовый интерфейс
                    for(DataPropertyInterface DataInterface : Interfaces)
                        JoinImplement.put(DefaultMap.get(DataInterface),Query.MapKeys.get(DataInterface));

                    // по изменению св-ва
                    SourceExpr NewExpr = DefaultProperty.getChangedSourceExpr(JoinImplement,Session,0);
                    SourceExpr PrevExpr = DefaultProperty.getChangedSourceExpr(JoinImplement,Session,2);

                    Query.Properties.put(ChangeTable.Value,NewExpr);

                    NewExpr = new NullEmptySourceExpr(NewExpr);
                    PrevExpr = new NullEmptySourceExpr(PrevExpr);
                    
                    // new, не равно prev
                    Query.Wheres.add(new FieldExprCompareWhere(NewExpr,PrevExpr,5));
                }
            }
        }

        JoinQuery<PropertyInterface,PropertyField> DataQuery = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);

        // GetChangedFrom
        Join<KeyField,PropertyField> DataJoin = new MapJoin<KeyField,PropertyField,PropertyInterface>(DataTable,DataTableMap,DataQuery);
        DataJoin.Joins.put(DataTable.Session,new ValueSourceExpr(Session.ID));
        DataJoin.Joins.put(DataTable.Property,new ValueSourceExpr(ID));

        SourceExpr DataExpr = new JoinExpr<KeyField,PropertyField>(DataJoin,DataTable.Value,true);
        DataQuery.Properties.put(ChangeTable.Value,DataExpr);

        for(DataPropertyInterface RemoveInterface : Interfaces) {
            if(Session.RemoveClasses.contains(RemoveInterface.Class)) {
                // те изменения которые были на удаляемые объекты исключаем
                TableFactory.RemoveClassTable.excludeJoin(DataQuery,Session,RemoveInterface.Class,DataQuery.MapKeys.get(RemoveInterface));

                // проверяем может кто удалился из интерфейса объекта
                JoinQuery<PropertyInterface,PropertyField> Query = ResultQuery.newJoinQuery(1);

                joinChangeClass(TableFactory.RemoveClassTable,Query,Session,RemoveInterface);

                // пока сделаем что наплевать на старое значение хотя конечно 2 раза может тоже не имеет смысл считать
                Query.Properties.put(ChangeTable.Value,new NullSourceExpr(getSourceExpr(Query.MapKeys,true)));
            }
        }

        if(Session.RemoveClasses.contains(Value)) {
            // те изменения которые были на удаляемые объекты исключаем
            TableFactory.RemoveClassTable.excludeJoin(DataQuery,Session,Value,DataExpr);

            JoinQuery<PropertyInterface,PropertyField> Query = ResultQuery.newJoinQuery(1);

            Join<KeyField,PropertyField> RemoveJoin = new Join<KeyField,PropertyField>(TableFactory.RemoveClassTable.getClassJoin(Session,Value));
            RemoveJoin.Joins.put(TableFactory.RemoveClassTable.Object,getSourceExpr(Query.MapKeys,true));
            Query.Wheres.add(new JoinWhere(RemoveJoin));

            Query.Properties.put(ChangeTable.Value,new StaticNullSourceExpr(ChangeTable.Value.Type));
        }

        // здесь именно в конце так как должна быть последней
        ResultQuery.Unions.put(DataQuery,1);

        QueryIncrementType = 0;
        return ResultQuery;
    }
}

abstract class AggregateProperty<T extends PropertyInterface> extends ObjectProperty<T> {
    
    AggregateProperty(TableFactory iTableFactory) {super(iTableFactory);}

    Map<DataPropertyInterface,T> AggregateMap;
    
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
    
    // расчитывает JoinSelect
    abstract SourceExpr calculateSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull);
    
    // проверяет аггрегацию для отладки
    boolean CheckAggregation(DataAdapter Adapter,String Caption) throws SQLException {
        JoinQuery<PropertyInterface, String> AggrSelect;
        AggrSelect = getOutSelect(Adapter,"value");
        LinkedHashMap<Map<PropertyInterface,Integer>,Map<String,Object>> AggrResult = AggrSelect.executeSelect(Adapter);
        TableFactory.ReCalculateAggr = true;
        AggrSelect = getOutSelect(Adapter,"value");
        LinkedHashMap<Map<PropertyInterface,Integer>,Map<String,Object>> CalcResult = AggrSelect.executeSelect(Adapter);
        TableFactory.ReCalculateAggr = false;

        Iterator<Map<PropertyInterface,Integer>> i = AggrResult.keySet().iterator();
        while(i.hasNext()) {
            Map<PropertyInterface,Integer> Row = i.next();
            
            boolean ToRemove = false;
            for(Object Value : Row.values())
                if(Value==null) {
                    ToRemove = true;
                    break;
                }

            if(ToRemove || (CalcResult.remove(Row)!=null))
                i.remove();
            else {
                Object Value =  AggrResult.get(Row).get("value");
                if(Value instanceof Integer && ((Integer)Value).equals(0)) i.remove();
                if(Value instanceof String && ((String)Value).trim().length()==0) i.remove();
            }
        }
        // вычистим и отсюда 0
        i = CalcResult.keySet().iterator();
        while(i.hasNext()) {
            Map<PropertyInterface,Integer> Row = i.next();

            boolean ToRemove = false;
            for(Object Value : Row.values())
                if(Value==null) {
                    ToRemove = true;
                    break;
                }

            if(ToRemove)
                i.remove();
            else {
                Object Value = CalcResult.get(Row).get("value");
                if(Value instanceof Integer && ((Integer)Value).equals(0)) i.remove();
                if(Value instanceof String && ((String)Value).trim().length()==0) i.remove();
            }
        }

        if(CalcResult.size()>0 || AggrResult.size()>0) {
            System.out.println("----CheckAggregations "+Caption+"-----");
            System.out.println("----Aggr-----");
            for(Map.Entry<Map<PropertyInterface,Integer>,Map<String,Object>> Row : AggrResult.entrySet())
                System.out.println(Row);
            System.out.println("----Calc-----");
            for(Map.Entry<Map<PropertyInterface,Integer>,Map<String,Object>> Row : CalcResult.entrySet())
                System.out.println(Row);
            while(i.hasNext())
                System.out.println(i.next());
            
            return false;
        }
        
        return true;
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
    
    void FillRequiredChanges(ChangesSession Session) {
        // этому св-ву чужого не надо
    }

    boolean FillChangedList(List<ObjectProperty> ChangedProperties, ChangesSession Session) {
        if(ChangedProperties.contains(this)) return true;
        
        for(DataPropertyInterface ValueInterface : Interfaces)
            if(Session==null || Session.AddClasses.contains(ValueInterface.Class) || Session.RemoveClasses.contains(ValueInterface.Class)) {
                ChangedProperties.add(this);
                return true;
            }
        
        return false;
    }

    Query<PropertyInterface, PropertyField> QueryIncrementChanged(ChangesSession Session) {

        // работает на = и на + ему собсно пофигу, то есть сразу на 2
        
        // для любого изменения объекта на NEW можно определить PREV и соответственно 
        // Set<Class> пришедшие, Set<Class> ушедшие
        // соответственно алгоритм бежим по всем интерфейсам делаем UnionQuery из SS изменений + старых объектов
        
        List<DataPropertyInterface> RemoveInterfaces = new ArrayList();
        for(DataPropertyInterface ValueInterface : Interfaces)
            if(Session.RemoveClasses.contains(ValueInterface.Class))
                RemoveInterfaces.add(ValueInterface);

        // конечный результат, с ключами и выражением 
        UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,3);

        // для RemoveClass без SS все за Join'им (ValueClass пока трогать не будем (так как у значения пока не закладываем механизм изменений))
        for(DataPropertyInterface ChangedInterface : RemoveInterfaces) {
            JoinQuery<PropertyInterface,PropertyField> Query = ResultQuery.newJoinQuery(1);

            // RemoveClass + остальные из старой таблицы
            joinChangeClass(TableFactory.RemoveClassTable,Query,Session,ChangedInterface);
            for(DataPropertyInterface ValueInterface : Interfaces)
                if(ValueInterface!=ChangedInterface)
                    joinObjects(Query,ValueInterface);

            Query.Properties.put(ChangeTable.Value,new StaticNullSourceExpr(ChangeTable.Value.Type));
            Query.Properties.put(ChangeTable.PrevValue,(Value==null?new StaticNullSourceExpr(ValueClass.GetDBType()):new ValueSourceExpr(Value)));
        }

        List<DataPropertyInterface> AddInterfaces = new ArrayList();
        for(DataPropertyInterface ValueInterface : Interfaces)
            if(Session.AddClasses.contains(ValueInterface.Class))
                AddInterfaces.add(ValueInterface);

        ListIterator<List<DataPropertyInterface>> il = (new SetBuilder<DataPropertyInterface>()).BuildSubSetList(AddInterfaces).listIterator();
        // пустое подмн-во не надо (как и в любой инкрементности)
        il.next();
        while(il.hasNext()) {
            List<DataPropertyInterface> ChangeProps = il.next();

            JoinQuery<PropertyInterface,PropertyField> Query = ResultQuery.newJoinQuery(1);

            for(DataPropertyInterface ValueInterface : Interfaces) {
                if(ChangeProps.contains(ValueInterface))
                    joinChangeClass(TableFactory.AddClassTable,Query,Session,ValueInterface);
                else {
                    joinObjects(Query,ValueInterface);

                    // здесь также надо проверить что не из RemoveClasses (то есть LEFT JOIN на null)
                    if(Session.RemoveClasses.contains(ValueInterface.Class))
                        TableFactory.RemoveClassTable.excludeJoin(Query,Session,ValueInterface.Class,Query.MapKeys.get(ValueInterface));
                }
            }
            
            Query.Properties.put(ChangeTable.PrevValue,new StaticNullSourceExpr(ChangeTable.PrevValue.Type));
            Query.Properties.put(ChangeTable.Value,(Value==null?new StaticNullSourceExpr(ValueClass.GetDBType()):new ValueSourceExpr(Value)));
        }
        
        QueryIncrementType = 2;
        
        return ResultQuery;
    }

    SourceExpr calculateSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        String ValueString = "value";
        JoinQuery<PropertyInterface,String> Query = new JoinQuery(Interfaces);
                
        for(DataPropertyInterface ValueInterface : Interfaces)
            joinObjects(Query,ValueInterface);
        Query.Properties.put(ValueString,(Value==null?new StaticNullSourceExpr(ValueClass.GetDBType()):new ValueSourceExpr(Value)));

        return new JoinExpr<PropertyInterface,String>(new Join<PropertyInterface,String>(Query,JoinImplement),ValueString,NotNull);
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

    public String GetDBType() {
        if(Value==null || Value instanceof Integer)
            return "integer";

        if(Value instanceof String)
            return "char(50)";
        
        return null;
    }
}

class PropertyMapImplement extends PropertyImplement<ObjectProperty,PropertyInterface> implements PropertyInterfaceImplement {
    
    PropertyMapImplement(ObjectProperty iProperty) {super(iProperty);}

    // NotNull только если сессии нету
    public SourceExpr mapSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull,ChangesSession Session,int Value) {
        
        // соберем интерфейс по всем нижним интерфейсам
        Map<PropertyInterface,SourceExpr> MapImplement = new HashMap();
        for(PropertyInterface ImplementInterface : (Collection<PropertyInterface>)Property.Interfaces)
            MapImplement.put(ImplementInterface,JoinImplement.get(Mapping.get(ImplementInterface)));

        return (Session!=null?((ObjectProperty)Property).getChangedSourceExpr(MapImplement,Session,Value):Property.getSourceExpr(MapImplement,NotNull));
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
    
    public boolean MapHasChanges(ChangesSession Session) {
        return Property.HasChanges(Session);
    }
    
    // заполняет список, возвращает есть ли изменения
    public boolean MapFillChangedList(List<ObjectProperty> ChangedProperties,ChangesSession Session) {
        return Property.FillChangedList(ChangedProperties,Session);
    }
 
    // для OverrideList'а по сути
    void MapChangeProperty(DataAdapter Adapter,Map<PropertyInterface,ObjectValue> Keys,Object NewValue,ChangesSession Session) throws SQLException {
        Map<PropertyInterface,ObjectValue> MapKeys = new HashMap();
        for(PropertyInterface ImplementInterface : (Collection<PropertyInterface>)Property.Interfaces)
            MapKeys.put(ImplementInterface,Keys.get(Mapping.get(ImplementInterface)));

        Property.ChangeProperty(Adapter,MapKeys,NewValue,Session);
    }

}

class JoinProperty extends AggregateProperty<PropertyInterface> {
    PropertyImplement<Property,PropertyInterfaceImplement> Implements;
    
    JoinProperty(TableFactory iTableFactory, Property iProperty) {
        super(iTableFactory);
        Implements = new PropertyImplement(iProperty);
    }

    SourceExpr calculateSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {
        // для всех нижних делаем JoinSelect
        Map<PropertyInterface,SourceExpr> MapImplement = new HashMap();
        for(PropertyInterface ImplementInterface : (Collection<PropertyInterface>)Implements.Property.Interfaces)
            MapImplement.put(ImplementInterface, Implements.Mapping.get(ImplementInterface).mapSourceExpr(JoinImplement,NotNull,null,0));
        
        return Implements.Property.getSourceExpr(MapImplement,NotNull);
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

    List<PropertyInterface> GetChangedImplements(ChangesSession Session) {
        
        List<PropertyInterface> ChangedProperties = new ArrayList();
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Implements.Property.Interfaces) {
            // должен вернуть null если нету изменений (или просто транслирует интерфейс) иначе возвращает AggregateProperty
            if(Implements.Mapping.get(Interface).MapHasChanges(Session)) 
                ChangedProperties.add(Interface);
        }
        
        return ChangedProperties;
    }
    
    void FillRequiredChanges(ChangesSession Session) {
        
        // если только основное - Property ->I - как было (если изменилось только 2 то его и вкинем), возвр. I
        // иначе (не (основное MultiplyProperty и 1)) - Property, Implements ->0 - как было, возвр. 0 - (на подчищение - если (1 или 2) то Left Join'им старые значения)
        // иначе (основное MultiplyProperty и 1) - Implements ->1 - как было (но с другим оператором), возвр. 1
        
        int ChangeType = GetChangeType(Session);
        
        Collection<PropertyInterface> ChangedProperties = GetChangedImplements(Session);
        if(ChangedProperties.size()==0) {
            ((ObjectProperty)Implements.Property).SetChangeType(Session,ChangeType);
        } else {
            if(Implements.Property.HasChanges(Session)) 
                ((ObjectProperty)Implements.Property).SetChangeType(Session,0);

            for(PropertyInterface ImpInterface : (Collection<PropertyInterface>)Implements.Property.Interfaces) { 
                PropertyInterfaceImplement Interface = Implements.Mapping.get(ImpInterface);
                if(Interface.MapHasChanges(Session)) // значит PropertyMapImplement на ObjectProperty
                    ((ObjectProperty)(((PropertyMapImplement)Interface).Property)).SetChangeType(Session,(Implements.Property instanceof MultiplyFormulaProperty && ChangeType==1?1:0)) ;
            }
        }
    }
    
    // инкрементные св-ва
    Query<PropertyInterface, PropertyField> QueryIncrementChanged(ChangesSession Session) {
        
        // алгоритм такой - для всех map св-в (в которых были изменения) строим подмножества изм. св-в
        // далее реализации этих св-в "замещаем" (то есть при JOIN будем подставлять), с остальными св-вами делаем LEFT JOIN на IS NULL 
        // и JOIN'им с основным св-вом делая туда FULL JOIN новых значений
        // или UNION или большой FULL JOIN в нужном порядке (и не делать LEFT JOIN на IS NULL) и там сделать большой NVL
        
        List<PropertyInterface> ChangedProperties = GetChangedImplements(Session);

        QueryIncrementType = GetChangeType(Session);
        boolean MultiplyJoin = (Implements.Property instanceof MultiplyFormulaProperty && QueryIncrementType==1);
        if(ChangedProperties.size()!=0 && !MultiplyJoin)
           QueryIncrementType = 0;
        
        // конечный результат, с ключами и выражением
        UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,(MultiplyJoin?1:3)); // по умолчанию на KEYNULL (но если Multiply то 1 на сумму)

        // строим все подмножества св-в в лексикографическом порядке
        ListIterator<List<PropertyInterface>> il = (new SetBuilder<PropertyInterface>()).BuildSubSetList(ChangedProperties).listIterator();

        while(il.hasNext()) {
            List<PropertyInterface> ChangeProps = il.next();
            // будем докидывать FULL JOIN'ы в нужном порядке получая соотв. NVL
            // нужно за Join'ить со старыми значениями (исключить этот JOIN если пустое подмн-во !!! собсно в этом и заключается оптимизация инкрементности), затем с новыми (если она есть)
            for(int ij=(ChangeProps.size()==0?1:0);ij<(Implements.Property.HasChanges(Session)?2:1);ij++) {
                JoinQuery<PropertyInterface,PropertyField> Query = ResultQuery.newJoinQuery(1);
                // JoinImplement'ы основного св-ва
                Map<PropertyInterface,SourceExpr> MapJoinImplement = new HashMap();
                for(PropertyInterface ImplementInterface : (Collection<PropertyInterface>)Implements.Property.Interfaces)
                    MapJoinImplement.put(ImplementInterface,Implements.Mapping.get(ImplementInterface).mapSourceExpr(Query.MapKeys,true,(ChangeProps.contains(ImplementInterface)?Session:null),MultiplyJoin?1:0));
                
                SourceExpr ValueExpr = null;
                if(ij==0) 
                    ValueExpr = Implements.Property.getSourceExpr(MapJoinImplement,false);
                else {
                    ValueExpr = ((ObjectProperty)Implements.Property).getChangedSourceExpr(MapJoinImplement,Session,QueryIncrementType);
                    if(QueryIncrementType==2) {
                        // если и предыдущее надо, то закидываем предыдущее, а потом новое 
                        Query.Properties.put(ChangeTable.PrevValue,ValueExpr);
                        ValueExpr = ((ObjectProperty)Implements.Property).getChangedSourceExpr(MapJoinImplement,Session,0);
                    }
                }
                Query.Properties.put(ChangeTable.Value,ValueExpr);
            }
        }
        
        return ResultQuery;
    }

    public String GetDBType() {
        return Implements.Property.GetDBType();
    }
    
    // заполняет список, возвращает есть ли изменения
    public boolean FillChangedList(List<ObjectProperty> ChangedProperties,ChangesSession Session) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = Implements.Property.FillChangedList(ChangedProperties,Session);

        for(PropertyInterface Interface : (Collection<PropertyInterface>)Implements.Property.Interfaces)
            Changed = Implements.Mapping.get(Interface).MapFillChangedList(ChangedProperties,Session) || Changed;

        if(Changed)
            ChangedProperties.add(this);
        
        return Changed;
    }
}

class GroupPropertyInterface extends PropertyInterface {
    PropertyInterfaceImplement Implement;
    
    GroupPropertyInterface(PropertyInterfaceImplement iImplement) {Implement=iImplement;}
}

abstract class GroupProperty extends AggregateProperty<GroupPropertyInterface> {
    // каждый интерфейс должен имплементировать именно GetInterface GroupProperty

    // оператор
    int Operator;
    
    GroupProperty(TableFactory iTableFactory,ObjectProperty iProperty,int iOperator) {
        super(iTableFactory);
        GroupProperty = iProperty;
        ToClasses = new HashMap();
        GroupKeys = new HashMap();
        Operator = iOperator;
    }
    
    // группировочное св-во собсно должно быть не формулой
    ObjectProperty GroupProperty;
    
    // дополнительные условия на классы
    Map<PropertyInterface,Class> ToClasses;

    // заполняются при заполнении базы
    Table ViewTable;
    PropertyField GroupField;
    Map<PropertyInterface,KeyField> GroupKeys;

    GroupQuery<Object,PropertyInterface,Object> getGroupQuery(Object PropertyObject) {
        JoinQuery<PropertyInterface,Object> Query = new JoinQuery<PropertyInterface,Object>(GroupProperty.Interfaces);
        for(GroupPropertyInterface ImplementInterface : Interfaces)
            Query.Properties.put(ImplementInterface,ImplementInterface.Implement.mapSourceExpr(Query.MapKeys,true,null,0));

        Query.Properties.put(PropertyObject,GroupProperty.getSourceExpr(Query.MapKeys,true));
        return new GroupQuery<Object,PropertyInterface,Object>(Interfaces,Query,PropertyObject,Operator);
    }

    void FillDB(DataAdapter Adapter,Integer ViewNum) throws SQLException {

        if(Adapter.allowViews()) {
            // создадим View
            Map<Object,PropertyField> MapProperties = new HashMap();
            ViewTable = getGroupQuery("grfield").createView(Adapter,"view"+ViewNum,GroupKeys,MapProperties);
            GroupField = MapProperties.get("grfield");
        }
    }

    SourceExpr calculateSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        if(ViewTable!=null) {
            // по сути также как при persistent
            Join<KeyField,PropertyField> SourceJoin = new Join<KeyField,PropertyField>(ViewTable);
            for(PropertyInterface Interface : Interfaces)
                SourceJoin.Joins.put(GroupKeys.get(Interface),JoinImplement.get(Interface));

            return new JoinExpr<KeyField,PropertyField>(SourceJoin,GroupField,NotNull);
        } else {
            return new JoinExpr<PropertyInterface,Object>(new Join<PropertyInterface,Object>(getGroupQuery(GroupField),JoinImplement),GroupField,NotNull);
        }

    }
    
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
//        if(GroupSet.iterator().next().keySet().size()!=GroupProperty.Interfaces.size())
//            GroupSet = GroupProperty.GetClassSet(ReqValue);

        for(GroupPropertyInterface Interface : Interfaces)
            GroupSet = GroupSet.AndSet(Interface.Implement.MapGetClassSet(null));

        // для всех классов нужно еще докинуть ограничения на явную заданные классы
        InterfaceClass GroupClasses = new InterfaceClass();
        for(PropertyInterface ClassInterface : ToClasses.keySet())
            GroupClasses.put(ClassInterface,ToClasses.get(ClassInterface));
        GroupSet = GroupSet.AndItem(GroupClasses);
        
        for(InterfaceClass ClassSet : GroupSet) {
            InterfaceClass ResultSet = new InterfaceClass();

            for(GroupPropertyInterface GroupInterface : Interfaces) {
                Class VC = GroupInterface.Implement.MapGetValueClass(ClassSet);
                ResultSet.put(GroupInterface,VC);
                if(VC==null)
                    return GetClassSet(ReqValue);
            }
            
            Result.OrItem(ResultSet);
        }

        return Result;
    }

    public String GetDBType() {
        return GroupProperty.GetDBType();
    }
    
    // заполняет список, возвращает есть ли изменения
    public boolean FillChangedList(List<ObjectProperty> ChangedProperties,ChangesSession Session) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = GroupProperty.FillChangedList(ChangedProperties,Session);

        for(GroupPropertyInterface Interface : Interfaces)
            Changed = Interface.Implement.MapFillChangedList(ChangedProperties,Session) || Changed;

        if(Changed)
            ChangedProperties.add(this);
        
        return Changed;
    }
    
    // получает всевозможные инкрементные запросы для обеспечения IncrementChanges
    Query<PropertyInterface,PropertyField> IncrementQuery(ChangesSession Session,PropertyField Value,List<GroupPropertyInterface> ChangedProperties,int GroupSet,boolean ValueType,boolean InterfaceSubSet,boolean InterfaceEmptySet) {
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

            // если не пустое скипаем
            if(!(InterfaceEmptySet || GroupOp==1)) 
                il.next();

            while(il.hasNext()) {
                List<GroupPropertyInterface> ChangeProps = il.next();
                JoinQuery<PropertyInterface,Object> Query = DataQuery.newJoinQuery(1);

                // значение
                Query.Properties.put(Value,(GroupOp==1?GroupProperty.getChangedSourceExpr(Query.MapKeys,Session,(ValueType?Operator:2)):GroupProperty.getSourceExpr(Query.MapKeys,true)));

                // значения интерфейсов
                for(GroupPropertyInterface Interface : Interfaces)
                    Query.Properties.put(Interface,Interface.Implement.mapSourceExpr(Query.MapKeys,true,(ChangeProps.contains(Interface)?Session:null),ValueType?0:2));
            }
        }
        
        return new GroupQuery<Object,PropertyInterface,PropertyField>(Interfaces,DataQuery,Value,Operator);
    }

    List<GroupPropertyInterface> GetChangedProperties(ChangesSession Session) {
        List<GroupPropertyInterface> ChangedProperties = new ArrayList();
        // должен вернуть null если нету изменений (или просто транслирует интерфейс) иначе возвращает AggregateProperty
        for(GroupPropertyInterface Interface : Interfaces)
            if(Interface.Implement.MapHasChanges(Session)) ChangedProperties.add(Interface);
        
        return ChangedProperties;
    }
}

class SumGroupProperty extends GroupProperty {

    SumGroupProperty(TableFactory iTableFactory,ObjectProperty iProperty) {super(iTableFactory,iProperty,1);}

    void FillRequiredChanges(ChangesSession Session) {
        // Group на ->1, Interface на ->2 - как было - возвр. 1 (на подчищение если (0 или 2) LEFT JOIN'им старые)
        if(GroupProperty.HasChanges(Session)) 
            GroupProperty.SetChangeType(Session,1);
        
        for(GroupPropertyInterface Interface : GetChangedProperties(Session))
            ((ObjectProperty)((PropertyMapImplement)Interface.Implement).Property).SetChangeType(Session,2);
    }

    Query<PropertyInterface, PropertyField> QueryIncrementChanged(ChangesSession Session) {
        // алгоритм пока такой :
        // 1. берем GROUPPROPERTY(изм на +) по аналогии с реляционными
        // G(0) =(true) SS(true) без общ.(false) 1 SUM(+)
        // 2. для новых св-в делаем GROUPPROPERTY(все) так же как и для реляционных св-в FULL JOIN'ы - JOIN'ов с "перегр." подмн-вами (единственный способ сразу несколько изменений "засечь") (и GROUP BY по ISNULL справо налево ключей)
        // A(1) =(true) SS(true) без обш.(false) 1 SUM(+)
        // 3. для старых св-в GROUPPROPERTY(все) FULL JOIN (JOIN "перегр." измененных с LEFT JOIN'ами старых) (без подмн-в) (и GROUP BY по ISNULL(обычных JOIN'ов,LEFT JOIN'a изм.))
        // A(1) P(false) без SS(false) без общ.(false) -1 SUM(+)
        // все UNION ALL и GROUP BY или же каждый GROUP BY а затем FULL JOIN на +

        List<GroupPropertyInterface> ChangedProperties = GetChangedProperties(Session);
        // конечный результат, с ключами и выражением 
        UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,1);

        // InterfaceSubSet ij<=2
        // InterfaceValue ij<=2
        // InterfaceEmptySet ij!=2
        // GroupSet (ij==1,0,1)
        
        if(GroupProperty.HasChanges(Session))
            ResultQuery.Unions.put(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,0,true,true,false),1);

        if(ChangedProperties.size()>0) {
            ResultQuery.Unions.put(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,1,true,true,false),1);
            ResultQuery.Unions.put(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,1,false,false,false),-1);
        }

        QueryIncrementType = 1;

        return ResultQuery;
     }
}


// КОМБИНАЦИИ (ЛИНЕЙНЫЕ,MAX,OVERRIDE) принимают null на входе, по сути как Relation но работают на Or\FULL JOIN
// соответственно мн-во св-в полностью должно отображаться на интерфейсы

class MaxGroupProperty extends GroupProperty {

    MaxGroupProperty(TableFactory iTableFactory,ObjectProperty iProperty) {super(iTableFactory,iProperty,0);}

    void FillRequiredChanges(ChangesSession Session) {
        // Group на ->2, Interface на ->2 - как было - возвр. 2
        if(GroupProperty.HasChanges(Session)) 
            GroupProperty.SetChangeType(Session,2);
        
        for(GroupPropertyInterface Interface : GetChangedProperties(Session))
            ((ObjectProperty)((PropertyMapImplement)Interface.Implement).Property).SetChangeType(Session,2);
    }

    // перегружаем метод, так как сразу пишем в таблицу поэтому ничего подчищать\проверять не надо
    @Override
    void IncrementChanges(DataAdapter Adapter, ChangesSession Session) throws SQLException {
        
        List<GroupPropertyInterface> ChangedProperties = GetChangedProperties(Session);
        // ничего не изменилось вываливаемся
        if(ChangedProperties.size()==0 && !GroupProperty.HasChanges(Session)) return;

        // нужно посчитать для группировок, MAX из ушедших (по старым значениям GroupProperty, Interface'ов) - аналогия 3 из Sum только еще основное св-во тоже задействуем
        // G/A(2) P(false) (без SS)(false) (без общ.)(false) =(null) MAX(=)
        // для группировок MAX из пришедших (по новым значениям все) - аналогия 2
        // G/A(2) =(true) (SS)(true) (без общ.)(false) =(null) MAX(=)
        // объединим кинув ушедшие (sys) и пришедшие (new)
        // расчитать старые для всех измененных (LEFT JOIN'им с старым View/persistent таблицей) JoinSelect(на true) (prev)

        StartChangeTable(Adapter,Session);
        // конечный результат, с ключами и выражением
        JoinQuery<PropertyInterface,PropertyField> ResultQuery = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);

        UnionQuery<PropertyInterface,PropertyField> ChangeQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,3);
        ChangeQuery.Unions.put(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,2,true,true,false),1);
        ChangeQuery.Unions.put(IncrementQuery(Session,ChangeTable.SysValue,ChangedProperties,2,false,false,false),1);

        UniJoin<PropertyInterface,PropertyField> ChangeJoin = new UniJoin<PropertyInterface,PropertyField>(ChangeQuery,ResultQuery);

        SourceExpr NewValue = new JoinExpr<PropertyInterface,PropertyField>(ChangeJoin,ChangeTable.Value,true);
        SourceExpr OldValue = new JoinExpr<PropertyInterface,PropertyField>(ChangeJoin,ChangeTable.SysValue,true);
        SourceExpr PrevValue = getSourceExpr(ResultQuery.MapKeys,false);

        ResultQuery.Properties.put(ChangeTable.Value,NewValue);
        ResultQuery.Properties.put(ChangeTable.SysValue,OldValue);
        ResultQuery.Properties.put(ChangeTable.PrevValue,PrevValue);

        ValueSourceExpr MinValue = new ValueSourceExpr(-99999999);
        NewValue = new IsNullSourceExpr(NewValue,MinValue);
        OldValue = new IsNullSourceExpr(OldValue,MinValue);
        PrevValue = new IsNullSourceExpr(PrevValue,MinValue);
        
        // null ассоциируется с -бесконечностью
        // удаляем всех пришедших<=старых значений и ушедшие<старых значений
        // то есть пропускаем (пришедшие>старых значений) или (ушедшие=старых значений)
        ResultQuery.Wheres.add(new FieldOPWhere(new FieldExprCompareWhere(NewValue,PrevValue,1),new FieldExprCompareWhere(OldValue,PrevValue,0),false));

//        Adapter.OutSelect(ResultQuery);
        Adapter.InsertSelect(modifyIncrementChanges(Session,ResultQuery,3));

        // для всех ушедших=старые значения (а они всегда <=) и пришедшие<старых значений обновляем по LEFT JOIN с запросом
		// MAX(по новым значениям, но весь запрос) просто берем запрос аналогии 2 (только взяв не только изменившиеся а все в том числе - пустое подмн-во)
                // G/A(2) =(true) (SS)(true) (с общ.)(true) =(null) MAX(=)

        // теоретически этот запрос нужно выполнять когда есть хоть одна запись но пока этого проверять не будем
        JoinQuery<PropertyInterface,PropertyField> UpdateQuery = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);

        Join<KeyField,PropertyField> ChangesJoin = getChangedValueJoin(UpdateQuery.MapKeys,Session);
        NewValue = new IsNullSourceExpr(new JoinExpr<KeyField,PropertyField>(ChangesJoin,ChangeTable.Value,true),MinValue);
        OldValue = new IsNullSourceExpr(new JoinExpr<KeyField,PropertyField>(ChangesJoin,ChangeTable.SysValue,true),MinValue);
        PrevValue = new IsNullSourceExpr(new JoinExpr<KeyField,PropertyField>(ChangesJoin,ChangeTable.PrevValue,true),MinValue);

        UpdateQuery.Wheres.add(new FieldOPWhere(new FieldExprCompareWhere(NewValue,PrevValue,2),new FieldExprCompareWhere(OldValue,PrevValue,0),false));

        UpdateQuery.Properties.put(ChangeTable.Value,new JoinExpr<PropertyInterface,PropertyField>(
                new UniJoin<PropertyInterface,PropertyField>(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,2,true,true,true),UpdateQuery),ChangeTable.Value,false));

        Adapter.UpdateRecords(modifyIncrementChanges(Session,UpdateQuery,0));

        // помечаем изменение в сессии на 2 вручную
        SessionChanged.put(Session,2);
    }

    Query<PropertyInterface, PropertyField> QueryIncrementChanged(ChangesSession Session) {
        // так как мы перегружаем IncrementChanges, этот метод вызываться не может
        return null;
    }
}
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
            JoinQuery<PropertyInterface,String> Query = ResultQuery.newJoinQuery(Coeffs.get(Operand));
            Query.Properties.put(ValueString,Operand.mapSourceExpr(Query.MapKeys,true,null,0));
        }

        return new JoinExpr<PropertyInterface,String>(new Join<PropertyInterface,String>(ResultQuery,JoinImplement),ValueString,NotNull);
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

    public String GetDBType() {
        return Operands.iterator().next().Property.GetDBType();        
    }

    boolean FillChangedList(List<ObjectProperty> ChangedProperties, ChangesSession Session) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = false;

        for(PropertyMapImplement Operand : Operands)
            Changed = Operand.MapFillChangedList(ChangedProperties,Session) || Changed;

        if(Changed)
            ChangedProperties.add(this);
        
        return Changed;
    }

    List<PropertyMapImplement> GetChangedProperties(ChangesSession Session) {
        
        List<PropertyMapImplement> ChangedProperties = new ArrayList();
        for(PropertyMapImplement Operand : Operands)
            if(Operand.MapHasChanges(Session)) ChangedProperties.add(Operand);
        
        return ChangedProperties;
    }
    
    Query<PropertyInterface,PropertyField> IncrementQuery(ChangesSession Session,int ValueType) {
        //      	0                   1                           2
        //Max(0)	значение,SS,LJ      не может быть               значение,SS,LJ,prevv
        //Sum(1)	значение,SS,LJ      значение,без SS, без LJ     значение,SS,LJ,prevv
        //Override(2)	значение,SS,LJ      старое поле=null,SS, LJ     значение,SS,LJ,prevv

        // неструктурно как и все оптимизации
        
        List<PropertyMapImplement> ChangedProperties = GetChangedProperties(Session);
        
        boolean SumList = (Operator==1 && ValueType==1);
        
        // конечный результат, с ключами и выражением 
        UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,SumList?1:3);

        ListIterator<List<PropertyMapImplement>> il = null;
        if(SumList) {
            // Sum, 1 - без SS
            List<List<PropertyMapImplement>> ChangedList = new ArrayList();
            ChangedList.add(new ArrayList());
            for(PropertyMapImplement Operand : ChangedProperties) {
                List<PropertyMapImplement> SingleList = new ArrayList();
                SingleList.add(Operand);
                ChangedList.add(SingleList);
            }
            il = ChangedList.listIterator();
        } else {
            il = (new SetBuilder<PropertyMapImplement>()).BuildSubSetList(ChangedProperties).listIterator();
            // здесь надо вырезать для оптимизации лишние
        }
        
        // пропустим пустое подмн-во
        il.next();
        while(il.hasNext()) {
            List<PropertyMapImplement> ChangedProps = il.next();

            JoinQuery<PropertyInterface,PropertyField> Query = ResultQuery.newJoinQuery(1);

            UnionSourceExpr ResultExpr = new UnionSourceExpr(Operator);
            UnionSourceExpr PrevExpr = (ValueType==2?new UnionSourceExpr(Operator):null);

            // так как делается LEFT JOIN важен порядок сначала закидываем ChangedProps ChangedJoinSelect
            // сначала бежим по Join'ам, затем по LEFT JOIN, но ResultExpr именно в нужном порядке
            Map<PropertyMapImplement,SourceExpr> ResultValues = new HashMap();
            Map<PropertyMapImplement,SourceExpr> PrevValues = new HashMap();
            
            for(PropertyMapImplement Operand : ChangedProps) {
                ResultValues.put(Operand,Operand.mapSourceExpr(Query.MapKeys,true,Session,ValueType==1?1:0));
                if(ValueType==2) PrevValues.put(Operand,Operand.mapSourceExpr(Query.MapKeys,true,Session,2));
            }
            
            if(!SumList) {
                for(PropertyMapImplement Operand : Operands) {
                    // здесь надо отрезать только те которые могут в принципе пересекаться по классам
                    if(!ChangedProps.contains(Operand)) {
                        SourceExpr OperandExpr = Operand.mapSourceExpr(Query.MapKeys,false,null,0);
                        if(Operator==2 && ValueType==1) { // если Override и 1 то нам нужно не само значение, а если не null то 0, иначе null (то есть не брать значение) {
                            List<SourceExpr> NullValues = new ArrayList();
                            NullValues.add(OperandExpr);
                            OperandExpr = new NullValueSourceExpr(NullValues,new StaticNullSourceExpr(OperandExpr.getDBType()),new ValueSourceExpr(0));
                        }
                        ResultValues.put(Operand,OperandExpr);
                        if(ValueType==2) PrevValues.put(Operand,Operand.mapSourceExpr(Query.MapKeys,false,null,2));
                    }
                }
            }

            // так хитро бежим потому как важен порядок
            for(PropertyMapImplement Operand : Operands)
                if(ResultValues.containsKey(Operand)) {
                    ResultExpr.Operands.put(ResultValues.get(Operand),Coeffs.get(Operand));
                    if(ValueType==2) PrevExpr.Operands.put(PrevValues.get(Operand),Coeffs.get(Operand));
                } 

            Query.Properties.put(ChangeTable.Value,ResultExpr);
            if(ValueType==2) Query.Properties.put(ChangeTable.PrevValue,PrevExpr);
        }
        
        return ResultQuery;
    }
}


class SumUnionProperty extends UnionProperty {
    
    SumUnionProperty(TableFactory iTableFactory) {super(iTableFactory,1);}

    void FillRequiredChanges(ChangesSession Session) {
        // если pers или 1 - Operand на ->1 - IncrementQuery(1) возвр. 1 - (на подчищение - если (0 или 2) LEFT JOIN'им старые)
        // иначе (не pers и (0 или 2)) - Operand на ->I - IncrementQuery (I) возвр. I

        int ChangeType = GetChangeType(Session);
        for(PropertyMapImplement Operand : Operands) {
            if(Operand.MapHasChanges(Session)) 
                ((ObjectProperty)(Operand.Property)).SetChangeType(Session,IsPersistent() || ChangeType==1?1:ChangeType);
        }
    }

    Query<PropertyInterface, PropertyField> QueryIncrementChanged(ChangesSession Session) {
        
        QueryIncrementType = (IsPersistent()?1:GetChangeType(Session));
        return IncrementQuery(Session,QueryIncrementType);
    }
    
    
}

class MaxUnionProperty extends UnionProperty {

    MaxUnionProperty(TableFactory iTableFactory) {super(iTableFactory,0);}
    
    void FillRequiredChanges(ChangesSession Session) {
        // если pers или 0 - Operand на ->0 - IncrementQuery(0), возвр. 0 - (на подчищение - если (1 или 2) LEFT JOIN'им старые)
        // иначе (не pers и (1 или 2)) - Operand на ->2 - IncrementQuery(2), возвр. 2

        int ChangeType = GetChangeType(Session);
        for(PropertyMapImplement Operand : Operands) {
            if(Operand.MapHasChanges(Session)) 
                ((ObjectProperty)(Operand.Property)).SetChangeType(Session,IsPersistent() || ChangeType==0?0:2);
        }
    }

    Query<PropertyInterface, PropertyField> QueryIncrementChanged(ChangesSession Session) {

        QueryIncrementType = (IsPersistent() || GetChangeType(Session)==0?0:2);
        return IncrementQuery(Session,QueryIncrementType);
    }
}

class OverrideUnionProperty extends UnionProperty {
    
    OverrideUnionProperty(TableFactory iTableFactory) {super(iTableFactory,2);}
    
    void FillRequiredChanges(ChangesSession Session) {
        // Operand на ->I - IncrementQuery(I) возвр. I

        for(PropertyMapImplement Operand : Operands) {
            if(Operand.MapHasChanges(Session)) 
                ((ObjectProperty)(Operand.Property)).SetChangeType(Session,GetChangeType(Session));
        }
    }

    Query<PropertyInterface, PropertyField> QueryIncrementChanged(ChangesSession Session) {

        QueryIncrementType = GetChangeType(Session);
        return IncrementQuery(Session,QueryIncrementType);
    }
    
    @Override
    void ChangeProperty(DataAdapter Adapter,Map<PropertyInterface,ObjectValue> Keys,Object NewValue,ChangesSession Session) throws SQLException {
        // нужно бежать в обратном порядке, там где появится первый 
        InterfaceClass ChangeClass = new InterfaceClass();
        for(PropertyInterface Interface : Interfaces)
            ChangeClass.put(Interface,Keys.get(Interface).Class);
        
        for(int i=Operands.size()-1;i>=0;i--) {
            PropertyMapImplement Operand = Operands.get(i);
            if(Operand.MapGetValueClass(ChangeClass)!=null)
                Operand.MapChangeProperty(Adapter,Keys,NewValue,Session);
        }
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
abstract class FormulaProperty<T extends FormulaPropertyInterface> extends Property<T> {
 
    public Class GetValueClass(InterfaceClass Class) {
        for(T Interface : Interfaces)
            if(!Class.get(Interface).IsParent(Interface.Class)) return null;
        
        return Interfaces.iterator().next().Class;
    }

    public InterfaceClassSet GetClassSet(Class ReqValue) {
        InterfaceClass ResultSet = new InterfaceClass();

        for(T Interface : Interfaces)
            ResultSet.put(Interface,Interface.Class);

        InterfaceClassSet Result = new InterfaceClassSet();
        if(ReqValue==null || Interfaces.iterator().next().Class.IsParent(ReqValue)) Result.add(ResultSet);
        return Result;
    }

    public String GetDBType() {
        return Interfaces.iterator().next().Class.GetDBType();
    }

    boolean HasChanges(ChangesSession Session) {
        return false;
    }
    
    // заполняет список, возвращает есть ли изменения
    public boolean FillChangedList(List<ObjectProperty> ChangedProperties,ChangesSession Session) {
        return false;
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
    boolean Filter;

    StringFormulaProperty(String iFormula,boolean iFilter) {
        super();
        Formula = iFormula;
        Filter = iFilter;
    }

    SourceExpr proceedSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        FormulaSourceExpr Source = new FormulaSourceExpr(Formula);

        for(StringFormulaPropertyInterface Interface : Interfaces)
            Source.Params.put(Interface.Param,JoinImplement.get(Interface));

        if(Filter)
            return new FormulaWhereSourceExpr(Source,NotNull);
        else
            return Source;
    }
}


class MultiplyFormulaProperty extends FormulaProperty<FormulaPropertyInterface> {

    SourceExpr proceedSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        FormulaSourceExpr Source = new FormulaSourceExpr("");
        int ParamNum=0;
        for(FormulaPropertyInterface Interface : Interfaces) {
            String Param = "prm" + ParamNum++;
            Source.Formula = (Source.Formula.length()==0?"":Source.Formula+'*') + Param;
            Source.Params.put(Param,JoinImplement.get(Interface));
        }

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

class ChangesSession {
    
    ChangesSession(Integer iID) {
        ID = iID;
        Properties = new HashSet();
        
        AddClasses = new HashSet();
        RemoveClasses = new HashSet();
        
        NewClasses = new HashMap();
    }

    Integer ID;
    
    Set<DataProperty> Properties;
    
    Set<Class> AddClasses;
    Set<Class> RemoveClasses;
    
    Map<Integer,List<Class>> NewClasses;
    
    void ChangeClass(Integer idObject,Class Class) {
        List<Class> ChangeClasses = NewClasses.get(idObject);
        if(ChangeClasses==null) {
            ChangeClasses = new ArrayList();
            NewClasses.put(idObject,ChangeClasses);
        }
        
        ChangeClasses.add(Class);
    }
}