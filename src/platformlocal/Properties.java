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

    public SourceExpr mapSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull, DataSession Session,int Value);
    public Class MapGetValueClass(InterfaceClass ClassImplement);
    public InterfaceClassSet MapGetClassSet(Class ReqValue);

    // для increment'ного обновления
    public boolean MapHasChanges(DataSession Session);
    
    abstract boolean MapFillChangedList(List<Property> ChangedProperties, DataSession Session);

    AddClasses mapClassSet(MapChangedRead Read, InterfaceAddClasses<?> InterfaceClasses);
}
        

class PropertyInterface implements PropertyInterfaceImplement {
    //можно использовать JoinExps потому как все равну вернуться она не может потому как иначе она зациклится
    
    public SourceExpr mapSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull, DataSession Session,int Value) {
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
    
    public boolean MapHasChanges(DataSession Session) {
        return false;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean MapFillChangedList(List<Property> ChangedProperties, DataSession Session) {
        return false;
    }

    public AddClasses mapClassSet(MapChangedRead Read, InterfaceAddClasses<?> InterfaceClasses) {
        return InterfaceClasses.get(this);
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

    public String getDBType() {
        return getBaseClass().GetDBType();
    }
    
    String caption = "";
    
    // заполняет список, возвращает есть ли изменения
    abstract boolean fillChangedList(List<Property> ChangedProperties, DataSession Session);
    
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

    // для Increment'ного обновления (какой вид изменений есть\нужен)
    // 0 - =
    // 1 - +
    // 2 - new\prev
    Map<DataSession,Integer> SessionChanged = new HashMap();

    boolean HasChanges(DataSession Session) {
        return SessionChanged.containsKey(Session);
    }

    // для преобразования типов а то странно работает
    Integer GetChangeType(DataSession Session) {
        return SessionChanged.get(Session);
    }

    void SetChangeType(DataSession Session,int ChangeType) {
        // 0 и 0 = 0
        // 0 и 1 = 2
        // 1 и 1 = 1
        // 2 и x = 2
        if(!SessionChanged.containsKey(Session))
            return;
            // throw new RuntimeException("Was Not Changed");
        Integer PrevType = GetChangeType(Session);
        if(PrevType!=null && !PrevType.equals(ChangeType)) ChangeType = 2;
        SessionChanged.put(Session,ChangeType);
    }

    // строится по сути "временный" Map PropertyInterface'ов на Objects'ы
    Map<PropertyInterface,KeyField> ChangeTableMap = null;
    // раз уж ChangeTableMap закэшировали то и ChangeTable тоже
    IncrementChangeTable ChangeTable;

    void FillChangeTable() {
        ChangeTable = TableFactory.GetChangeTable(Interfaces.size(), getDBType());
        ChangeTableMap = new HashMap();
        Iterator<KeyField> io = ChangeTable.Objects.iterator();
        for(T Interface : Interfaces)
            ChangeTableMap.put(Interface,io.next());
    }

    // вычищает все из сессии
    void StartChangeTable(DataSession Session) throws SQLException {

        Map<KeyField,Integer> ValueKeys = new HashMap();
        ValueKeys.put(ChangeTable.Property,ID);
        Session.deleteKeyRecords(ChangeTable,ValueKeys);
    }

    // подготавливает Join примененных изменений
    Join<? extends Object,PropertyField> getChangedValueJoin(Map<PropertyInterface, SourceExpr> JoinImplement, DataSession Session, boolean Inner) {

        if(IncrementSource!=null)
            return new Join<PropertyInterface,PropertyField>(IncrementSource,JoinImplement,Inner);
        else {
            Join<KeyField,PropertyField> From = new Join<KeyField,PropertyField>(ChangeTable,Inner);
            From.Joins.put(ChangeTable.Property,new ValueSourceExpr(ID));
            for(T Interface : Interfaces)
                From.Joins.put(ChangeTableMap.get(Interface),JoinImplement.get(Interface));

            return From;
        }
    }

    // подготавливает JoinExpr
    SourceExpr getChangedValueExpr(Map<PropertyInterface,SourceExpr> JoinImplement, DataSession Session,PropertyField Value) {

        return getChangedValueJoin(JoinImplement,Session, true).Exprs.get(Value);
    }

    // связывает именно измененные записи из сессии
    // Value - что получать, 0 - новые значения, 1 - +(увеличение), 2 - старые значения
    SourceExpr getChangedSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement, DataSession Session,int Value) {

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
    SourceExpr getUpdatedSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement, DataSession Session,boolean NotNull) {

        if(Session!=null && HasChanges(Session)) {
            String Value = "joinvalue";

            UnionQuery<PropertyInterface,String> UnionQuery = new UnionQuery<PropertyInterface,String>(Interfaces,3);

            JoinQuery<PropertyInterface,String> SourceQuery = new JoinQuery<PropertyInterface,String>(Interfaces);
            SourceQuery.add(Value,getSourceExpr(SourceQuery.MapKeys,true));
            UnionQuery.add(SourceQuery,1);

            JoinQuery<PropertyInterface,String> NewQuery = new JoinQuery<PropertyInterface,String>(Interfaces);
            NewQuery.add(Value,getChangedSourceExpr(NewQuery.MapKeys,Session,0));
            UnionQuery.add(NewQuery,1);

            return (new Join<PropertyInterface,String>(UnionQuery,JoinImplement,NotNull)).Exprs.get(Value);
        } else
            return getSourceExpr(JoinImplement,NotNull);
    }

    void OutChangesTable(DataSession Session) throws SQLException {
        JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);

        Join<KeyField,PropertyField> ChangeJoin = new MapJoin<KeyField,PropertyField,PropertyInterface>(ChangeTable,Query,ChangeTableMap,true);
        ChangeJoin.Joins.put(ChangeTable.Property,new ValueSourceExpr(ID));

        Query.add(ChangeTable.Value,ChangeJoin.Exprs.get(ChangeTable.Value));
        Query.add(ChangeTable.PrevValue,ChangeJoin.Exprs.get(ChangeTable.PrevValue));

        Query.outSelect(Session);
    }

    // сохраняет изменения в таблицу
    void SaveChanges(DataSession Session) throws SQLException {

        // если не изменились ничего не делаем
        if(!HasChanges(Session)) return;

        Map<KeyField,T> MapKeys = new HashMap();
        Table SourceTable = GetTable(MapKeys);

        JoinQuery<KeyField,PropertyField> ModifyQuery = new JoinQuery<KeyField,PropertyField>(SourceTable.Keys);

        Join<KeyField,PropertyField> Update = new Join<KeyField,PropertyField>(ChangeTable,true);
        for(KeyField Key : SourceTable.Keys)
            Update.Joins.put(ChangeTableMap.get(MapKeys.get(Key)),ModifyQuery.MapKeys.get(Key));
        Update.Joins.put(ChangeTable.Property,new ValueSourceExpr(ID));

        ModifyQuery.add(Field,Update.Exprs.get(ChangeTable.Value));
        Session.ModifyRecords(new ModifyQuery(SourceTable,ModifyQuery));
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
    abstract void FillRequiredChanges(DataSession Session);

    // для каскадного выполнения (запрос)
    Source<PropertyInterface,PropertyField> IncrementSource = null;
    boolean XL = false;

    // заполняет инкрементные изменения
    void IncrementChanges(DataSession Session) throws SQLException {

        // удалим старые данные
        StartChangeTable(Session);

        Source<? extends PropertyInterface,PropertyField> ResultQuery = QueryIncrementChanged(Session);

        // проверим что вернули что вернули то что надо, "подчищаем" если не то
        int ChangeType = GetChangeType(Session);
        // если вернул 2 запишем
        if(QueryIncrementType==2 || (QueryIncrementType!=ChangeType)) {
            ChangeType = 2;
            SessionChanged.put(Session,2);
        }

        if(QueryIncrementType != ChangeType) {
            JoinQuery<PropertyInterface,PropertyField> NewQuery = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);
            SourceExpr NewExpr = (new UniJoin<PropertyInterface,PropertyField>((Source<PropertyInterface,PropertyField>) ResultQuery,NewQuery,true)).Exprs.get(ChangeTable.Value);
            // нужно LEFT JOIN'ить старые
            SourceExpr PrevExpr = getSourceExpr(NewQuery.MapKeys,false);
            // по любому 2 нету надо докинуть
            NewQuery.add(ChangeTable.PrevValue,PrevExpr);
            if(QueryIncrementType==1) {
                // есть 1, а надо по сути 0
                UnionSourceExpr SumExpr = new UnionSourceExpr(1);
                SumExpr.Operands.put(NewExpr,1);
                SumExpr.Operands.put(PrevExpr,1);
                NewExpr = SumExpr;
            }
            NewQuery.add(ChangeTable.Value,NewExpr);
            ResultQuery = NewQuery;
        }

/*        if(!XL) {
            System.out.println("IncChanges CURR - "+caption);
            Out(Session);
            System.out.println("IncChanges - "+caption);
            ResultQuery.outSelect(Session);
        }*/
        if(!IsPersistent() && XL)
            IncrementSource = (Source<PropertyInterface,PropertyField>) ResultQuery;
        else {
//            try {
                Session.InsertSelect(modifyIncrementChanges(ResultQuery,ChangeType));
//            } catch(Exception e) {
//                IncrementSource = IncrementSource;
//            }
            IncrementSource = null;
        }
    }

    // вынесем в отдельный метод потому как используется в 2 местах
    ModifyQuery modifyIncrementChanges(Source<? extends PropertyInterface, PropertyField> Query, int ChangeType) {

        JoinQuery<KeyField,PropertyField> WriteQuery = new JoinQuery<KeyField,PropertyField>(ChangeTable.Keys);
        Join<PropertyInterface,PropertyField> ResultJoin = new MapJoin<PropertyInterface,PropertyField,KeyField>((Source<PropertyInterface,PropertyField>) Query,ChangeTableMap,WriteQuery,true);
        WriteQuery.add(ChangeTable.Value,ResultJoin.Exprs.get(ChangeTable.Value));
        if(ChangeType>=2)
            WriteQuery.add(ChangeTable.PrevValue,ResultJoin.Exprs.get(ChangeTable.PrevValue));
        if(ChangeType>=3)
            WriteQuery.add(ChangeTable.SysValue,ResultJoin.Exprs.get(ChangeTable.SysValue));

        Map<KeyField,Integer> ValueKeys = new HashMap();
        ValueKeys.put(ChangeTable.Property,ID);
        WriteQuery.putDumbJoin(ValueKeys);

        return new ModifyQuery(ChangeTable,WriteQuery);
    }

    // для возврата чтобы не плодить классы
    int QueryIncrementType;
    // получает запрос для инкрементных изменений
    abstract Source<? extends PropertyInterface, PropertyField> QueryIncrementChanged(DataSession Session);

    // присоединяют объекты
    void joinChangeClass(ChangeClassTable Table,JoinQuery<PropertyInterface,?> Query, DataSession Session,DataPropertyInterface Interface) {
        Join<KeyField,PropertyField> ClassJoin = new Join<KeyField,PropertyField>(Table.getClassJoin(Session,Interface.Class),true);
        ClassJoin.Joins.put(Table.Object,Query.MapKeys.get(Interface));
        Query.add(ClassJoin);
    }

    void joinObjects(JoinQuery<PropertyInterface,?> Query,DataPropertyInterface Interface) {
        Join<KeyField,PropertyField> ClassJoin = new Join<KeyField,PropertyField>(TableFactory.ObjectTable.getClassJoin(Interface.Class),true);
        ClassJoin.Joins.put(TableFactory.ObjectTable.Key,Query.MapKeys.get(Interface));
        Query.add(ClassJoin);
    }

    // isRequired (InterfaceAddClasses) - обязателен ли на вход соответствующий список классов
    boolean isRequired(InterfaceAddClasses<? extends PropertyInterface> InterfaceClasses) {
        //      теоретически getClassSet на null, а затем проверить для каждого интерфейса нету ли этого класса в каждом из Interface'ов
        return false;
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
    Map<KeyField,PropertyInterface> DataTableMap = null;

    void FillDataTable() {
        DataTable = TableFactory.GetDataChangeTable(Interfaces.size(), getDBType());
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
        Session.Properties.add(this);
    }

    // св-во по умолчанию (при AddClasses подставляется)
    Property DefaultProperty;
    // map интерфейсов на PropertyInterface
    Map<DataPropertyInterface,PropertyInterface> DefaultMap;
    // если нужно еще за изменениями следить и перебивать
    boolean OnDefaultChange;
    
    // заполняет список, возвращает есть ли изменения
    boolean fillChangedList(List<Property> ChangedProperties, DataSession Session) {
        if(ChangedProperties.contains(this)) return true;
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
            Changed = (DefaultProperty.fillChangedList(ChangedProperties, Session) && OnDefaultChange) || Changed;
            
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

    void FillRequiredChanges(DataSession Session) {

        // если на изм. надо предыдущее изменение иначе просто на =
        // пока неясно после реализации QueryIncrementChanged станет яснее
        if(DefaultProperty!=null && DefaultProperty.HasChanges(Session))
            DefaultProperty.SetChangeType(Session,OnDefaultChange?2:0);
    }

    // заполним старыми значениями (LEFT JOIN'ом)
    Source<? extends PropertyInterface, PropertyField> QueryIncrementChanged(DataSession Session) {

        // на 3 то есть слева/направо
        UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,3);

        // Default изменения (пока Add)
        if(DefaultProperty!=null) {
            if(!OnDefaultChange) {
                // бежим по всем добавленным интерфейсам
                for(DataPropertyInterface Interface : Interfaces) 
                    if(Session.AddClasses.contains(Interface.Class)) {
                        JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);
                        Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
                        // "перекодируем" в базовый интерфейс
                        for(DataPropertyInterface DataInterface : Interfaces)
                            JoinImplement.put(DefaultMap.get(DataInterface),Query.MapKeys.get(DataInterface));

                        // вкидываем "новое" состояние DefaultProperty с Join'ое с AddClassTable
                        // если DefaultProperty требует на входе такой добавляемый интерфейс то можно чисто новое брать
                        joinChangeClass(TableFactory.AddClassTable,Query,Session,Interface);

                        Query.add(ChangeTable.Value,DefaultProperty.getUpdatedSourceExpr(JoinImplement,Session,true));
                        ResultQuery.add(Query,1);
                    }
            } else {
                if(DefaultProperty.HasChanges(Session)) {
                    JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);
                    Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
                    // "перекодируем" в базовый интерфейс
                    for(DataPropertyInterface DataInterface : Interfaces)
                        JoinImplement.put(DefaultMap.get(DataInterface),Query.MapKeys.get(DataInterface));

                    // по изменению св-ва
                    SourceExpr NewExpr = DefaultProperty.getChangedSourceExpr(JoinImplement,Session,0);
                    SourceExpr PrevExpr = DefaultProperty.getChangedSourceExpr(JoinImplement,Session,2);

                    Query.add(ChangeTable.Value,NewExpr);

                    NewExpr = new NullEmptySourceExpr(NewExpr);
                    PrevExpr = new NullEmptySourceExpr(PrevExpr);
                    
                    // new, не равно prev
                    Query.add(new FieldExprCompareWhere(NewExpr,PrevExpr,5));
                    ResultQuery.add(Query,1);
                }
            }
        }

        JoinQuery<PropertyInterface,PropertyField> DataQuery = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);

        // GetChangedFrom
        Join<KeyField,PropertyField> DataJoin = new MapJoin<KeyField,PropertyField,PropertyInterface>(DataTable,DataTableMap,DataQuery,true);
        DataJoin.Joins.put(DataTable.Property,new ValueSourceExpr(ID));

        SourceExpr DataExpr = DataJoin.Exprs.get(DataTable.Value);
        DataQuery.add(ChangeTable.Value,DataExpr);

        for(DataPropertyInterface RemoveInterface : Interfaces) {
            if(Session.RemoveClasses.contains(RemoveInterface.Class)) {
                // те изменения которые были на удаляемые объекты исключаем
                TableFactory.RemoveClassTable.excludeJoin(DataQuery,Session,RemoveInterface.Class,DataQuery.MapKeys.get(RemoveInterface));

                // проверяем может кто удалился из интерфейса объекта
                JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);
                joinChangeClass(TableFactory.RemoveClassTable,Query,Session,RemoveInterface);
                // пока сделаем что наплевать на старое значение хотя конечно 2 раза может тоже не имеет смысл считать
                Query.add(ChangeTable.Value,new NullJoinSourceExpr(getSourceExpr(Query.MapKeys,true)));
                ResultQuery.add(Query,1);
            }
        }

        if(Session.RemoveClasses.contains(Value)) {
            // те изменения которые были на удаляемые объекты исключаем
            TableFactory.RemoveClassTable.excludeJoin(DataQuery,Session,Value,DataExpr);

            JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);
            Join<KeyField,PropertyField> RemoveJoin = new Join<KeyField,PropertyField>(TableFactory.RemoveClassTable.getClassJoin(Session,Value),true);
            RemoveJoin.Joins.put(TableFactory.RemoveClassTable.Object,getSourceExpr(Query.MapKeys,true));
            Query.add(RemoveJoin);
            Query.add(ChangeTable.Value,new NullSourceExpr(ChangeTable.Value.Type));
            ResultQuery.add(Query,1);
        }

        // здесь именно в конце так как должна быть последней
        ResultQuery.add(DataQuery,1);

        QueryIncrementType = 0;
        return ResultQuery;
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
    
    void FillRequiredChanges(DataSession Session) {
        // этому св-ву чужого не надо
    }

    boolean fillChangedList(List<Property> ChangedProperties, DataSession Session) {
        // если Value null то ничего не интересует
        if(Value==null) return false;
        if(ChangedProperties.contains(this)) return true;
        
        for(DataPropertyInterface ValueInterface : Interfaces)
            if(Session==null || Session.AddClasses.contains(ValueInterface.Class) || Session.RemoveClasses.contains(ValueInterface.Class)) {
                ChangedProperties.add(this);
                return true;
            }
        
        return false;
    }

    Source<? extends PropertyInterface, PropertyField> QueryIncrementChanged(DataSession Session) {

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
            JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface, PropertyField>(Interfaces);

            // RemoveClass + остальные из старой таблицы
            joinChangeClass(TableFactory.RemoveClassTable,Query,Session,ChangedInterface);
            for(DataPropertyInterface ValueInterface : Interfaces)
                if(ValueInterface!=ChangedInterface)
                    joinObjects(Query,ValueInterface);

            Query.add(ChangeTable.Value,new NullSourceExpr(ChangeTable.Value.Type));
            Query.add(ChangeTable.PrevValue,ValueSourceExpr.getExpr(Value,ValueClass.GetDBType()));

            ResultQuery.add(Query,1);
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

            JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface, PropertyField>(Interfaces);

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
            
            Query.add(ChangeTable.PrevValue,new NullSourceExpr(ChangeTable.PrevValue.Type));
            Query.add(ChangeTable.Value,ValueSourceExpr.getExpr(Value,ValueClass.GetDBType()));

            ResultQuery.add(Query,1);
        }
        
        QueryIncrementType = 2;
        
        return ResultQuery;
    }

    SourceExpr calculateSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull) {

        String ValueString = "value";

        Source<PropertyInterface,String> Source;
        // если null то возвращает EmptySource
        if(Value==null)
            Source = new EmptySource<PropertyInterface,String>(Interfaces,ValueString,ValueClass.GetDBType());
        else {
            JoinQuery<PropertyInterface,String> Query = new JoinQuery<PropertyInterface,String>(Interfaces);
                
            for(DataPropertyInterface ValueInterface : Interfaces)
                joinObjects(Query,ValueInterface);
            Query.add(ValueString,ValueSourceExpr.getExpr(Value,ValueClass.GetDBType()));
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
    public SourceExpr mapSourceExpr(Map<PropertyInterface,SourceExpr> JoinImplement,boolean NotNull, DataSession Session,int Value) {
        
        // соберем интерфейс по всем нижним интерфейсам
        Map<PropertyInterface,SourceExpr> MapImplement = new HashMap();
        for(PropertyInterface ImplementInterface : (Collection<PropertyInterface>)Property.Interfaces)
            MapImplement.put(ImplementInterface,JoinImplement.get(Mapping.get(ImplementInterface)));

        return (Session!=null?Property.getChangedSourceExpr(MapImplement,Session,Value):Property.getSourceExpr(MapImplement,NotNull));
    }

    public Join mapChangedJoin(Map<PropertyInterface, SourceExpr> JoinImplement, DataSession Session, boolean Inner) {

        // соберем интерфейс по всем нижним интерфейсам
        Map<PropertyInterface,SourceExpr> MapImplement = new HashMap();
        for(PropertyInterface ImplementInterface : (Collection<PropertyInterface>)Property.Interfaces)
            MapImplement.put(ImplementInterface,JoinImplement.get(Mapping.get(ImplementInterface)));

        return Property.getChangedValueJoin(MapImplement,Session, Inner);
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
        return Property.HasChanges(Session);
    }
    
    // заполняет список, возвращает есть ли изменения
    public boolean MapFillChangedList(List<Property> ChangedProperties, DataSession Session) {
        return Property.fillChangedList(ChangedProperties,Session);
    }

    public AddClasses mapClassSet(MapChangedRead Read, InterfaceAddClasses<?> InterfaceClasses) {
        if(Read.ImplementChanged.contains(this) && Read.ImplementType !=2)
            return Read.Session.PropertyAddValues.get(Property);
        else
            return new AddClasses();
    }

    // для OverrideList'а по сути
    void MapChangeProperty(Map<PropertyInterface, ObjectValue> Keys, Object NewValue, DataSession Session) throws SQLException {
        Map<PropertyInterface,ObjectValue> MapKeys = new HashMap();
        for(PropertyInterface ImplementInterface : (Collection<PropertyInterface>)Property.Interfaces)
            MapKeys.put(ImplementInterface,Keys.get(Mapping.get(ImplementInterface)));

        Property.ChangeProperty(MapKeys,NewValue,Session);
    }

    public InterfaceAddClasses mapAddClasses(DataSession Session) {
        InterfaceAddClasses<PropertyInterface> MapInterfaceClasses = new InterfaceAddClasses<PropertyInterface>();
        InterfaceAddClasses<PropertyInterface> PropertyInterfaceClasses = Session.PropertyAddClasses.get(Property);

        for(Map.Entry<PropertyInterface,PropertyInterface> ImplementInterface : Mapping.entrySet())
            MapInterfaceClasses.put(ImplementInterface.getValue(),PropertyInterfaceClasses.get(ImplementInterface.getKey()));

        return MapInterfaceClasses;
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

    List<PropertyInterface> GetChangedImplements(DataSession Session) {
        
        List<PropertyInterface> ChangedProperties = new ArrayList();
        for(PropertyInterface Interface : (Collection<PropertyInterface>)Implements.Property.Interfaces) {
            // должен вернуть null если нету изменений (или просто транслирует интерфейс) иначе возвращает AggregateProperty
            if(Implements.Mapping.get(Interface).MapHasChanges(Session)) 
                ChangedProperties.add(Interface);
        }
        
        return ChangedProperties;
    }
    
    void FillRequiredChanges(DataSession Session) {
        
        // если только основное - Property ->I - как было (если изменилось только 2 то его и вкинем), возвр. I
        // иначе (не (основное MultiplyProperty и 1)) - Property, Implements ->0 - как было, возвр. 0 - (на подчищение - если (1 или 2) то Left Join'им старые значения)
        // иначе (основное MultiplyProperty и 1) - Implements ->1 - как было (но с другим оператором), возвр. 1
        
        int ChangeType = GetChangeType(Session);
        
        Collection<PropertyInterface> ChangedProperties = GetChangedImplements(Session);
        if(ChangedProperties.size()==0) {
            Implements.Property.SetChangeType(Session,ChangeType);
        } else {
            int ReqType = (implementAllInterfaces() && ChangeType==0?0:2);

            if(Implements.Property.HasChanges(Session)) 
                Implements.Property.SetChangeType(Session,ReqType);

            for(PropertyInterface ImpInterface : (Collection<PropertyInterface>)Implements.Property.Interfaces) {
                PropertyInterfaceImplement Interface = Implements.Mapping.get(ImpInterface);
                if(Interface.MapHasChanges(Session)) // значит PropertyMapImplement на Property
                    (((PropertyMapImplement)Interface).Property).SetChangeType(Session,(Implements.Property instanceof MultiplyFormulaProperty && ChangeType==1?1:ReqType));
            }
        }
    }

    // инкрементные св-ва
    Source<? extends PropertyInterface, PropertyField> QueryIncrementChanged(DataSession Session) {
        
        // алгоритм такой - для всех map св-в (в которых были изменения) строим подмножества изм. св-в
        // далее реализации этих св-в "замещаем" (то есть при JOIN будем подставлять), с остальными св-вами делаем LEFT JOIN на IS NULL 
        // и JOIN'им с основным св-вом делая туда FULL JOIN новых значений
        // или UNION или большой FULL JOIN в нужном порядке (и не делать LEFT JOIN на IS NULL) и там сделать большой NVL
        
        List<PropertyInterface> ChangedProperties = GetChangedImplements(Session);

        QueryIncrementType = GetChangeType(Session);
        if(Implements.Property instanceof MultiplyFormulaProperty && QueryIncrementType==1)
            return getMapQuery(getChangeImplements(Session,1),ChangeTable.Value,new InterfaceAddClasses(),new AddClasses(),true);
        else {
            if(ChangedProperties.size()==0 && QueryIncrementType==1)
                return getMapQuery(getChangeMap(Session,1),ChangeTable.Value,new InterfaceAddClasses(),new AddClasses(), false);
            // если не все интерфейсы имплементируются св-вами надо запустить ветку с предыдущими значениями чтобы за null'ить
            if(QueryIncrementType==1 || (ChangedProperties.size()>0 && !implementAllInterfaces()))
                QueryIncrementType = 2;

            // конечный результат, с ключами и выражением
            UnionQuery<PropertyInterface,PropertyField> ResultQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,3); // по умолчанию на KEYNULL (но если Multiply то 1 на сумму)

            if(QueryIncrementType==2) {
                // все значения в PrevValue, а в Value - значение Null
                JoinQuery<JoinPropertyInterface, PropertyField> PrevSource = getMapQuery(getPreviousImplements(Session), ChangeTable.PrevValue, new InterfaceAddClasses(), new AddClasses(), false).getJoinQuery();
                PrevSource.add(ChangeTable.Value,new NullSourceExpr(Implements.Property.getDBType()));
                ResultQuery.add(PrevSource,1);
            }

            // все на Value - PrevValue не интересует, его как раз верхний подгоняет
            ResultQuery.add(getMapQuery(getChange(Session),ChangeTable.Value,new InterfaceAddClasses(),new AddClasses(),false),1);
            if(QueryIncrementType==2)
                ResultQuery.add(getMapQuery(getChangeMap(Session,2),ChangeTable.PrevValue,new InterfaceAddClasses(),new AddClasses(), false),1);

            return ResultQuery;
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

    InterfaceAddClasses<JoinPropertyInterface> getMapPropertyInterfaces(InterfaceAddClasses<JoinPropertyInterface> InterfaceClasses, InterfaceAddClasses<PropertyInterface> ImplementClasses) {
        return InterfaceClasses;
    }

    InterfaceAddClasses<PropertyInterface> getMapPropertyImplements(InterfaceAddClasses<JoinPropertyInterface> InterfaceClasses, InterfaceAddClasses<PropertyInterface> ImplementClasses) {
        return ImplementClasses;
    }

    void putImplementsToQuery(JoinQuery<JoinPropertyInterface, PropertyField> Query, PropertyField Value, MapRead Read, Map<PropertyInterface, SourceExpr> Implements) {
        Query.add(Value,Read.getMapExpr(getMapProperty(),Implements));
    }

    Map<PropertyField, String> getMapNullProps(PropertyField Value) {
        Map<PropertyField, String> NullProps = new HashMap();
        NullProps.put(Value,getDBType());
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

    void outIncrementState(DataSession Session) throws SQLException {

        System.out.println("THIS CURR : ");
        GroupProperty.Out(Session);
        System.out.println("THIS CHANGES : "+GroupProperty.GetChangeType(Session));
        GroupProperty.OutChangesTable(Session);

        System.out.println("GP CURR : ");
        GroupProperty.Out(Session);
        System.out.println("GP CHANGES : "+GroupProperty.GetChangeType(Session));
        GroupProperty.OutChangesTable(Session);

        for(GroupPropertyInterface Interface : Interfaces) {
            if(Interface.Implement instanceof PropertyMapImplement) {
                PropertyMapImplement PropertyImplement = (PropertyMapImplement)Interface.Implement;
                System.out.println("IG CURR : "+PropertyImplement);
                PropertyImplement.Property.Out(Session);
                System.out.println("IG CHANGES : "+PropertyImplement+" "+PropertyImplement.Property.GetChangeType(Session));
                PropertyImplement.Property.OutChangesTable(Session);
            }
        }
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

    InterfaceAddClasses<GroupPropertyInterface> getMapPropertyInterfaces(InterfaceAddClasses<PropertyInterface> InterfaceClasses, InterfaceAddClasses<GroupPropertyInterface> ImplementClasses) {
        return ImplementClasses;
    }

    InterfaceAddClasses<PropertyInterface> getMapPropertyImplements(InterfaceAddClasses<PropertyInterface> InterfaceClasses, InterfaceAddClasses<GroupPropertyInterface> ImplementClasses) {
        return InterfaceClasses;
    }

    void putImplementsToQuery(JoinQuery<PropertyInterface, Object> Query, Object Value, MapRead Read, Map<GroupPropertyInterface, SourceExpr> Implements) {
        Query.addAll(Implements);
        Query.add(Value,Read.getMapExpr(GroupProperty,Query.MapKeys));
    }

    Map<Object, String> getMapNullProps(Object Value) {
        Map<Object, String> NullProps = new HashMap();
        NullProps.put(Value,getDBType());
        InterfaceClass InterfaceClass = GetClassSet(null).get(0);
        for(Map.Entry<PropertyInterface,Class> Interface : InterfaceClass.entrySet())
            NullProps.put(Interface.getKey(),Interface.getValue().GetDBType());
        return NullProps;
    }

    Object getDefaultObject() {
        return "grfield";
    }

    Source<PropertyInterface, Object> getMapSourceQuery(Object Value) {
        return new GroupQuery<Object,PropertyInterface,Object>(Interfaces,getMapQuery(getDB(),Value),Value,Operator);
    }

    Source<PropertyInterface, PropertyField> getGroupQuery(Source<PropertyInterface,Object> MapSource,PropertyField Value) {
        return new GroupQuery<Object,PropertyInterface,PropertyField>(Interfaces, MapSource,Value,Operator);
    }

    Source<PropertyInterface, PropertyField> getGroupQuery(List<MapChangedRead> ReadList,PropertyField Value) {
        return new GroupQuery<Object,PropertyInterface,PropertyField>(Interfaces,getMapQuery(ReadList,Value,new InterfaceAddClasses(),new AddClasses(), false),Value,Operator);
    }
}

class SumGroupProperty extends GroupProperty {

    SumGroupProperty(TableFactory iTableFactory,Property iProperty) {super(iTableFactory,iProperty,1);}

    void FillRequiredChanges(DataSession Session) {
        // Group на ->1, Interface на ->2 - как было - возвр. 1 (на подчищение если (0 или 2) LEFT JOIN'им старые)
        if(GroupProperty.HasChanges(Session)) 
            GroupProperty.SetChangeType(Session,1);
        
        for(GroupPropertyInterface Interface : GetChangedProperties(Session))
            (((PropertyMapImplement)Interface.Implement).Property).SetChangeType(Session,2);
    }

    Source<? extends PropertyInterface, PropertyField> QueryIncrementChanged(DataSession Session) {
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

        ResultQuery.add(getGroupQuery(getChangeMap(Session,1),ChangeTable.Value),1);

        ResultQuery.add(getGroupQuery(getChangeImplements(Session,0),ChangeTable.Value),1);
        ResultQuery.add(getGroupQuery(getPreviousImplements(Session),ChangeTable.Value),-1);

/*        if(GroupProperty.HasChanges(Session))
            ResultQuery.add(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,0,true,true,false),1);

        if(ChangedProperties.size()>0) {
            ResultQuery.add(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,1,true,true,false),1);
            ResultQuery.add(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,1,false,false,false),-1);
        }*/

        QueryIncrementType = 1;

        return ResultQuery;
     }
}


class MaxGroupProperty extends GroupProperty {

    MaxGroupProperty(TableFactory iTableFactory,Property iProperty) {super(iTableFactory,iProperty,0);}

    void FillRequiredChanges(DataSession Session) {
        // Group на ->2, Interface на ->2 - как было - возвр. 2
        if(GroupProperty.HasChanges(Session)) 
            GroupProperty.SetChangeType(Session,2);
        
        for(GroupPropertyInterface Interface : GetChangedProperties(Session))
            ((PropertyMapImplement)Interface.Implement).Property.SetChangeType(Session,2);
    }

    // перегружаем метод, так как сразу пишем в таблицу поэтому ничего подчищать\проверять не надо
    @Override
    void IncrementChanges(DataSession Session) throws SQLException {
        
        List<GroupPropertyInterface> ChangedProperties = GetChangedProperties(Session);
        // ничего не изменилось вываливаемся
        if(ChangedProperties.size()==0 && !GroupProperty.HasChanges(Session)) return;

        StartChangeTable(Session);
/*
        if(caption.equals("MG 8"))
            caption = caption;
        System.out.println("IncChanges M 0 - "+caption);
        Out(Session);

        System.out.println("Group Property WAS - "+GroupProperty.caption);
        GroupProperty.Out(Session);
        System.out.println("Group Property Changed - "+GroupProperty.caption);
        GroupProperty.OutChangesTable(Session);
        outIncrementState(Session);
  */
        // нужно посчитать для группировок, MAX из ушедших (по старым значениям GroupProperty, Interface'ов) - аналогия 3 из Sum только еще основное св-во тоже задействуем
        // G/A(2) P(false) (без SS)(false) (без общ.)(false) =(null) MAX(=)
        // для группировок MAX из пришедших (по новым значениям все) - аналогия 2
        // G/A(2) =(true) (SS)(true) (без общ.)(false) =(null) MAX(=)
        // объединим кинув ушедшие (sys) и пришедшие (new)
        // расчитать старые для всех измененных (LEFT JOIN'им с старым View/persistent таблицей) JoinSelect(на true) (prev)

        // конечный результат, с ключами и выражением
        JoinQuery<PropertyInterface,PropertyField> ResultQuery = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);

        UnionQuery<PropertyInterface,PropertyField> ChangeQuery = new UnionQuery<PropertyInterface,PropertyField>(Interfaces,3);

        List<MapChangedRead> SysValueRead = getPreviousImplements(Session); SysValueRead.add(getPreviousMap(Session));
        ChangeQuery.add(getGroupQuery(SysValueRead,ChangeTable.SysValue),1);

/*        System.out.println("IncChanges F 1 - "+caption);
        for(MapChangedRead Read : getPreviousImplements(Session)) {
        try {
            if(getAddClasses(Read)!=null)
                getMapQuery(Read,ChangeTable.Value).outSelect(Session);
        } catch (SQLException e) {
        }
        }
        System.out.println("IncChanges F 2 - "+caption);
        try {
            if(getAddClasses(getPreviousMap(Session))!=null)
                getMapQuery(getPreviousMap(Session),ChangeTable.Value).outSelect(Session);
        } catch (SQLException e) {
        }
  */
        ChangeQuery.add(getGroupQuery(getChange(Session),ChangeTable.Value),1);
/*        System.out.println("IncChanges F 3 - "+caption);
        for(MapChangedRead Read : getChangeImplements(Session,0)) {
        try {
            if(getAddClasses(Read)!=null)
                getMapQuery(Read,ChangeTable.Value).outSelect(Session);
        } catch (SQLException e) {
        }
        }
        System.out.println("IncChanges F 4 - "+caption);
        for(MapChangedRead Read : getChange(Session,0,0)) {
        try {
            if(getAddClasses(Read)!=null)
                getMapQuery(Read,ChangeTable.Value).outSelect(Session);
        } catch (SQLException e) {
        }
        }
  */
/*        ChangeQuery.add(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,2,true,true,false),1);
ChangeQuery.add(IncrementQuery(Session,ChangeTable.SysValue,ChangedProperties,2,false,false,false),1);*/

        UniJoin<PropertyInterface,PropertyField> ChangeJoin = new UniJoin<PropertyInterface,PropertyField>(ChangeQuery,ResultQuery,true);

        SourceExpr NewValue = ChangeJoin.Exprs.get(ChangeTable.Value);
        SourceExpr OldValue = ChangeJoin.Exprs.get(ChangeTable.SysValue);
        SourceExpr PrevValue = getSourceExpr(ResultQuery.MapKeys,false);

        ResultQuery.add(ChangeTable.Value,NewValue);
        ResultQuery.add(ChangeTable.SysValue,OldValue);
        ResultQuery.add(ChangeTable.PrevValue,PrevValue);

        ValueSourceExpr MinValue = new ValueSourceExpr(-99999999);
        NewValue = new IsNullSourceExpr(NewValue,MinValue);
        OldValue = new IsNullSourceExpr(OldValue,MinValue);
        PrevValue = new IsNullSourceExpr(PrevValue,MinValue);
        
        // null ассоциируется с -бесконечностью
        // удаляем всех пришедших<=старых значений и ушедшие<старых значений
        // то есть пропускаем (пришедшие>старых значений) или (ушедшие=старых значений)
        ResultQuery.add(new FieldOPWhere(new FieldExprCompareWhere(NewValue,PrevValue,1),new FieldExprCompareWhere(OldValue,PrevValue,0),false));

//        System.out.println("IncChanges M 1 - "+caption);
//        ResultQuery.outSelect(Session);
        Session.InsertSelect(modifyIncrementChanges(ResultQuery,3));

        // для всех ушедших=старые значения (а они всегда <=) и пришедшие<старых значений обновляем по LEFT JOIN с запросом
		// MAX(по новым значениям, но весь запрос) просто берем запрос аналогии 2 (только взяв не только изменившиеся а все в том числе - пустое подмн-во)
                // G/A(2) =(true) (SS)(true) (с общ.)(true) =(null) MAX(=)

        // теоретически этот запрос нужно выполнять когда есть хоть одна запись но пока этого проверять не будем
        JoinQuery<PropertyInterface,PropertyField> UpdateQuery = new JoinQuery<PropertyInterface,PropertyField>(Interfaces);

        Join<Object,PropertyField> ChangesJoin = (Join<Object,PropertyField>)getChangedValueJoin(UpdateQuery.MapKeys,Session, true);
        NewValue = new IsNullSourceExpr(ChangesJoin.Exprs.get(ChangeTable.Value),MinValue);
        OldValue = new IsNullSourceExpr(ChangesJoin.Exprs.get(ChangeTable.SysValue),MinValue);
        PrevValue = new IsNullSourceExpr(ChangesJoin.Exprs.get(ChangeTable.PrevValue),MinValue);

        UpdateQuery.add(new FieldOPWhere(new FieldExprCompareWhere(NewValue,PrevValue,2),new FieldExprCompareWhere(OldValue,PrevValue,0),false));

        List<MapChangedRead> NewRead = new ArrayList(); NewRead.add(getPrevious(Session)); NewRead.addAll(getChange(Session));
        UpdateQuery.add(ChangeTable.Value,(new UniJoin<PropertyInterface,PropertyField>(getGroupQuery(NewRead,ChangeTable.Value),UpdateQuery,false)).Exprs.get(ChangeTable.Value));
//        UpdateQuery.add(ChangeTable.Value,(new UniJoin<PropertyInterface,PropertyField>(IncrementQuery(Session,ChangeTable.Value,ChangedProperties,2,true,true,true),UpdateQuery,false)).Exprs.get(ChangeTable.Value));

//        System.out.println("IncChanges M 2 - "+caption);
//        UpdateQuery.outSelect(Session);
        Session.UpdateRecords(modifyIncrementChanges(UpdateQuery,0));

//        System.out.println("IncChanges M 3 - "+caption);
//        ChangeTable.outSelect(Session);

//        if(caption.equals("MG 8"))
//            outIncrementState(Session);

/*        if(caption.equals("MG 125")) {
            System.out.println("IncChanges CURR - "+caption);
            Out(Session);
            System.out.println("IncChanges - "+caption);
            OutChangesTable(Session);
        }
  */      
        // помечаем изменение в сессии на 2 вручную
        SessionChanged.put(Session,2);
    }

    Source<? extends PropertyInterface, PropertyField> QueryIncrementChanged(DataSession Session) {
        // так как мы перегружаем IncrementChanges, этот метод вызываться не может
        return null;
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
            Query.add(ValueString,Operand.mapSourceExpr(Query.MapKeys,true,null,0));
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

    boolean fillChangedList(List<Property> ChangedProperties, DataSession Session) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = false;

        for(PropertyMapImplement Operand : Operands)
            Changed = Operand.MapFillChangedList(ChangedProperties,Session) || Changed;

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
    
    Query<PropertyInterface,PropertyField> IncrementQuery(DataSession Session,int ValueType) {
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
        } else
            il = (new SetBuilder<PropertyMapImplement>()).BuildSubSetList(ChangedProperties).listIterator();
            // здесь надо вырезать для оптимизации лишние

        // пропустим пустое подмн-во
        il.next();
        while(il.hasNext()) {
            List<PropertyMapImplement> ChangedProps = il.next();

            JoinQuery<PropertyInterface,PropertyField> Query = new JoinQuery<PropertyInterface, PropertyField>(Interfaces);

            UnionSourceExpr ResultExpr = new UnionSourceExpr(Operator);
            UnionSourceExpr PrevExpr = (ValueType==2?new UnionSourceExpr(Operator):null);

            for(PropertyMapImplement Operand : Operands) {
                if(!ChangedProps.contains(Operand) && SumList) continue;
                // здесь надо отрезать только те которые могут в принципе пересекаться по классам
                SourceExpr OperandExpr;
                SourceExpr PrevOperandExpr = null;
                if(ChangedProps.contains(Operand)) {
                    OperandExpr = Operand.mapSourceExpr(Query.MapKeys,true,Session,ValueType==1?1:0);
                    if(ValueType==2) PrevOperandExpr = Operand.mapSourceExpr(Query.MapKeys,true,Session,2);
                } else {
                    OperandExpr = Operand.mapSourceExpr(Query.MapKeys,false,null,0);
                    if(Operator==2 && ValueType==1) // если Override и 1 то нам нужно не само значение, а если не null то 0, иначе null (то есть не брать значение) {
                        OperandExpr = new CaseWhenSourceExpr(new SourceIsNullWhere(OperandExpr,false),new NullSourceExpr(OperandExpr.getDBType()),new ValueSourceExpr(0));
                    if(ValueType==2) PrevOperandExpr = Operand.mapSourceExpr(Query.MapKeys,false,null,2);
                }
                ResultExpr.Operands.put(OperandExpr,Coeffs.get(Operand));
                if(ValueType==2) PrevExpr.Operands.put(PrevOperandExpr,Coeffs.get(Operand));
            }

            Query.add(ChangeTable.Value,ResultExpr);
            if(ValueType==2) Query.add(ChangeTable.PrevValue,PrevExpr);

            ResultQuery.add(Query,1);
        }
        
        return ResultQuery;
    }
}


class SumUnionProperty extends UnionProperty {
    
    SumUnionProperty(TableFactory iTableFactory) {super(iTableFactory,1);}

    void FillRequiredChanges(DataSession Session) {
        // если pers или 1 - Operand на ->1 - IncrementQuery(1) возвр. 1 - (на подчищение - если (0 или 2) LEFT JOIN'им старые)
        // иначе (не pers и (0 или 2)) - Operand на ->I - IncrementQuery (I) возвр. I

        int ChangeType = GetChangeType(Session);
        for(PropertyMapImplement Operand : Operands) {
            if(Operand.MapHasChanges(Session)) 
                Operand.Property.SetChangeType(Session,IsPersistent() || ChangeType==1?1:ChangeType);
        }
    }

    Source<? extends PropertyInterface, PropertyField> QueryIncrementChanged(DataSession Session) {
        
        QueryIncrementType = (IsPersistent()?1:GetChangeType(Session));
        return IncrementQuery(Session,QueryIncrementType);
    }
    
    
}

class MaxUnionProperty extends UnionProperty {

    MaxUnionProperty(TableFactory iTableFactory) {super(iTableFactory,0);}
    
    void FillRequiredChanges(DataSession Session) {
        // если pers или 0 - Operand на ->0 - IncrementQuery(0), возвр. 0 - (на подчищение - если (1 или 2) LEFT JOIN'им старые)
        // иначе (не pers и (1 или 2)) - Operand на ->2 - IncrementQuery(2), возвр. 2

        int ChangeType = GetChangeType(Session);
        for(PropertyMapImplement Operand : Operands) {
            if(Operand.MapHasChanges(Session)) 
                Operand.Property.SetChangeType(Session,IsPersistent() || ChangeType==0?0:2);
        }
    }

    Source<? extends PropertyInterface, PropertyField> QueryIncrementChanged(DataSession Session) {

        QueryIncrementType = (IsPersistent() || GetChangeType(Session)==0?0:2);
        return IncrementQuery(Session,QueryIncrementType);
    }
}

class OverrideUnionProperty extends UnionProperty {
    
    OverrideUnionProperty(TableFactory iTableFactory) {super(iTableFactory,2);}
    
    void FillRequiredChanges(DataSession Session) {
        // Operand на ->I - IncrementQuery(I) возвр. I

        for(PropertyMapImplement Operand : Operands) {
            if(Operand.MapHasChanges(Session)) 
                Operand.Property.SetChangeType(Session,GetChangeType(Session));
        }
    }

    Source<? extends PropertyInterface, PropertyField> QueryIncrementChanged(DataSession Session) {

        QueryIncrementType = GetChangeType(Session);
        return IncrementQuery(Session,QueryIncrementType);
    }

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

    void FillRequiredChanges(DataSession Session) {
    }
    
    // не может быть изменений в принципе
    boolean fillChangedList(List<Property> ChangedProperties, DataSession Session) {
        return false;
    }

    Source<? extends PropertyInterface, PropertyField> QueryIncrementChanged(DataSession Session) {
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

class DataSession {

    Connection Connection;
    SQLSyntax Syntax;

    int ID = 0;
    public Map<Property,InterfaceAddClasses> PropertyAddClasses = new HashMap();
    public Map<Property,AddClasses> PropertyAddValues = new HashMap();

    DataSession(DataAdapter Adapter,int iID) throws SQLException{

        ID = iID;

        try {
            Connection = Adapter.startConnection();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        Syntax = Adapter;
    }

    Set<DataProperty> Properties = new HashSet();

    Set<Class> AddClasses = new HashSet();
    Set<Class> RemoveClasses = new HashSet();
    
    Map<Integer,List<Class>> NewClasses = new HashMap();

    void restart() {
        Properties.clear();
        AddClasses.clear();
        RemoveClasses.clear();
        NewClasses.clear();
    }

    void ChangeClass(Integer idObject,Class Class) {
        List<Class> ChangeClasses = NewClasses.get(idObject);
        if(ChangeClasses==null) {
            ChangeClasses = new ArrayList();
            NewClasses.put(idObject,ChangeClasses);
        }

        ChangeClasses.add(Class);
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

        try {
            Execute(Syntax.getCreateSessionTable(Table.Name,CreateString,"CONSTRAINT PK_S_" + ID +"_T_" + Table.Name + " PRIMARY KEY " + Syntax.getClustered() + " (" + KeyString + ")"));
        } catch(Exception e) {
        }
    }

    void Execute(String ExecuteString) throws SQLException {
        Statement Statement = Connection.createStatement();
//        System.out.println(ExecuteString+Syntax.getCommandEnd());
        try {
            Statement.execute(ExecuteString+Syntax.getCommandEnd());
//        } catch(SQLException e) {
//            e = e;
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
            ValueString = ValueString+","+(Value==null?"NULL":(Value instanceof String?"'"+(String)Value+"'":Value.toString()));
        }

        Execute("INSERT INTO "+Table.getSource(Syntax)+" ("+InsertString+") VALUES ("+ValueString+")");
    }

    void UpdateInsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<PropertyField,Object> PropFields) throws SQLException {

        // по сути пустое кол-во ключей
        JoinQuery<Object,String> IsRecQuery = new JoinQuery<Object,String>();

        Join<KeyField,PropertyField> TableJoin = new Join<KeyField,PropertyField>(Table,true);
        // сначала закинем KeyField'ы и прогоним Select
        for(KeyField Key : Table.Keys)
            TableJoin.Joins.put(Key,new ValueSourceExpr(KeyFields.get(Key)));
        IsRecQuery.add(TableJoin);

        if(IsRecQuery.executeSelect(this).size()>0) {
            // есть запись нужно Update лупить
            UpdateRecords(new ModifyQuery(Table,new DumbSource<KeyField,PropertyField>(KeyFields,PropFields)));
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
}

class MapRead {
    // делает разные вызовы Changed/Main
    SourceExpr getImplementExpr(PropertyInterfaceImplement Implement,Map<PropertyInterface,SourceExpr> JoinImplement) {
        return Implement.mapSourceExpr(JoinImplement,true,null,0);
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
            return Implement.mapSourceExpr(JoinImplement,true,Session,ImplementType);            
        else
            return super.getImplementExpr(Implement, JoinImplement);    //To change body of overridden methods use File | Settings | File Templates.
    }

    SourceExpr getMapExpr(Property MapProperty, Map<PropertyInterface, SourceExpr> JoinImplement) {
        if(MapChanged)
            return MapProperty.getChangedSourceExpr(JoinImplement,Session,MapType);    
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
    abstract InterfaceAddClasses<T> getMapPropertyInterfaces(InterfaceAddClasses<IN> InterfaceClasses,InterfaceAddClasses<IM> ImplementClasses);
    abstract InterfaceAddClasses<M> getMapPropertyImplements(InterfaceAddClasses<IN> InterfaceClasses,InterfaceAddClasses<IM> ImplementClasses);

    // ПРОВЕРКА\ЗАПОЛНЕНИЕ ADDCLASSES - а вот здесь много чего общего
    // возвращает InterfaceAddClasses<T>, null если Read не прошел проверку по AddClasses то есть заведомо Empty 
    InterfaceAddClasses<T> getAddClasses(MapChangedRead Read) {

        // бежим по всем изм. PropertyMapImplement, заполняем InterfaceAddClasses <InterfaceClass> интерфейсов на OR(+)
        // бежим по всем не изм. PropertyMapImplement, вызываем isRequired InterfaceAddClasses интерфейсов, если что-тоо не так вываливаемся

        // подготавливаем InterfaceAddClasses getMapImplements <ImplementClass>:
        //      для изм. PropertyMapImplement берем getChangedAddClasses !!!! если конечно на 0,1 идет иначе как и не изм.
        //      для не изм. PropertyMapImplement берем пустой ClassSet
        //      для Interface берем ClassSet из InterfaceAddClasses интерфейсов
        
        for(PropertyInterfaceImplement Implement : Read.ImplementChanged)
            if(!Implement.MapHasChanges(Read.Session)) return null;
        if(Read.MapChanged && !getMapProperty().HasChanges(Read.Session)) return null;

        InterfaceAddClasses<IN> InterfaceClasses = new InterfaceAddClasses<IN>();

        for(PropertyInterfaceImplement Implement : Read.ImplementChanged)
            if(Implement instanceof PropertyMapImplement)
                InterfaceClasses.or(((PropertyMapImplement)Implement).mapAddClasses(Read.Session));

        for(PropertyInterfaceImplement Implement : getMapImplements().values())
            if(Implement instanceof PropertyMapImplement && !Read.ImplementChanged.contains(Implement) && ((PropertyMapImplement)Implement).mapIsRequired(InterfaceClasses))
                return null;

        InterfaceAddClasses<IM> ImplementClasses = new InterfaceAddClasses<IM>();
        for(Map.Entry<IM,PropertyInterfaceImplement> Implement : getMapImplements().entrySet())
            ImplementClasses.put(Implement.getKey(),Implement.getValue().mapClassSet(Read,InterfaceClasses));

//        if(this instanceof JoinQuery) (M = ImplementClass, T = InterfaceClass)
                // на проверку InterfaceAddClasses <ImplementClass> имплементаций
                // на выход InterfaceAddClasses <InterfaceClass> интерфейсов
//        else (M = InterfaceClass, T = ImplementClass)
                // на проверку InterfaceAddClasses <InterfaceClass> интерфейсов
                // на выход InterfaceAddClasses <ImplementClass> имплементаций
        // если св-во не измененное и не идет добавление
        if(!Read.MapChanged && getMapProperty().isRequired(getMapPropertyImplements(InterfaceClasses, ImplementClasses))) return null;
        return getMapPropertyInterfaces(InterfaceClasses, ImplementClasses);
    }

    // возвращает добавленные классы значений 
    AddClasses getValueClasses(MapChangedRead Read) {
        // если не измененное getMapProperty или не предыдущее значение то возвращаем пустой AddClasses, иначе из сессии для основного св-ва 
        return (Read.MapChanged && Read.MapType!=2?Read.Session.PropertyAddValues.get(getMapProperty()):new AddClasses());
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
    abstract Map<OM,String> getMapNullProps(OM Value);

    // ВЫПОЛНЕНИЕ СПИСКА ИТЕРАЦИЙ

    Source<IN,OM> getMapQuery(List<MapChangedRead> ReadList, OM Value, InterfaceAddClasses<T> AddClasses, AddClasses ValueClasses, boolean Sum) {
        
        // делаем getQuery для всех итераций, после чего Query делаем Union на 3, InterfaceAddClasses на AND(*), Value на AND(*)
        UnionQuery<IN, OM> ListQuery = new UnionQuery<IN, OM>(getMapInterfaces(),Sum?1:3);
        for(MapChangedRead Read : ReadList) {
            InterfaceAddClasses<T> ReadAddClasses = getAddClasses(Read);
            if(ReadAddClasses!=null) {
                ValueClasses.and(getValueClasses(Read));
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
    boolean fillChangedList(List<Property> ChangedProperties, DataSession Session) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = getMapProperty().fillChangedList(ChangedProperties,Session);

        for(PropertyInterfaceImplement Implement : getMapImplements().values())
            Changed = Implement.MapFillChangedList(ChangedProperties,Session) || Changed;

        if(Changed)
            ChangedProperties.add(this);

        return Changed;
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
