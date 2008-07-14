/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeMap;


class PropertyImplement<T> {
    
    PropertyImplement(Property iProperty) {
        Property = iProperty;
        Mapping = new HashMap<PropertyInterface,T>();
    }
    
    Property Property;
    Map<PropertyInterface,T> Mapping;
}

interface PropertyInterfaceImplement {

    public SourceExpr MapJoinSelect(JoinList Joins,Map<PropertyInterface,SourceExpr> JoinImplement, boolean Left,ChangesSession Session,int Value);
    public Class MapGetValueClass();
    public InterfaceClassSet MapGetClassSet(Class ReqValue);

    // для increment'ного обновления
    public boolean MapHasChanges(ChangesSession Session);
    
    abstract boolean MapFillAggregateList(List<AggregateProperty> ChangedProperties,Set<DataProperty> ChangedSet);

}
        

class PropertyInterface implements PropertyInterfaceImplement {
    //можно использовать JoinExps потому как все равну вернуться она не может потому как иначе она зациклится
    Class ValueClass;
    
    public SourceExpr MapJoinSelect(JoinList Joins,Map<PropertyInterface,SourceExpr> JoinImplement,boolean Left,ChangesSession Session,int Value) {
        return JoinImplement.get(this);
    }
    
    public Class MapGetValueClass() {
        return ValueClass;
    }

    public InterfaceClassSet MapGetClassSet(Class ReqValue) {
        InterfaceClassSet Result = new InterfaceClassSet();
        if(ReqValue!=null) {
            InterfaceClass ResultClass = new InterfaceClass();
            ResultClass.put(this,ReqValue);
            Result.add(ResultClass);
        }

        return Result;
    }
    
    public boolean MapHasChanges(ChangesSession Session) {
        return false;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean MapFillAggregateList(List<AggregateProperty> ChangedProperties,Set<DataProperty> ChangedSet) {
        return false;
    }
}


// само св-во также является имплементацией св-ва
class JoinList extends ArrayList<From> {
    JoinList() {
        CacheTables = new HashMap();
    }

    Map<Table,Map<Map<KeyField,SourceExpr>,FromTable>> CacheTables;

    // приходится перегружать hash'и а то ведет себя неадекватно
    @Override
    public boolean equals(Object o) {
        return this==o;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    Map<Map<KeyField,SourceExpr>,FromTable> GetCacheMaps(Table SourceTable) {
        // поищем что такая таблица уже есть в запросе причем с такими же полями и такими же Join'ами
        Map<Map<KeyField,SourceExpr>,FromTable> CacheTable = CacheTables.get(SourceTable);
        if(CacheTable==null) {
            CacheTable = new HashMap();
            CacheTables.put(SourceTable,CacheTable);
        }
        
        return CacheTable;
    }
}
abstract class Property<T extends PropertyInterface> {

    int ID=0;
    
    Property() {
        Interfaces = new ArrayList<T>();
        SelectCacheJoins = new HashMap<JoinList,Map<Map<PropertyInterface,SourceExpr>,SourceExpr>>();
    }
    
    // чтобы подчеркнуть что не направленный
    Collection<T> Interfaces;
    // кэшируем здесь а не в JoinList потому как быстрее
    // работает только для JOIN смотри ChangedJoinSelect
    Map<JoinList,Map<Map<PropertyInterface,SourceExpr>,SourceExpr>> SelectCacheJoins;
    
    // закэшируем чтобы тучу таблиц не создавать и быстрее работать
    public SourceExpr JoinSelect(JoinList Joins,Map<PropertyInterface,SourceExpr> JoinImplement,boolean Left) {

        Map<Map<PropertyInterface,SourceExpr>,SourceExpr> CacheJoins = SelectCacheJoins.get(Joins);
        if(CacheJoins==null) {
            CacheJoins = new HashMap<Map<PropertyInterface,SourceExpr>,SourceExpr>();
            SelectCacheJoins.put(Joins,CacheJoins);
        }
        
        // не будем проверять что все интерфейсы реализованы все равно null в map не попадет
        SourceExpr JoinExpr = CacheJoins.get(JoinImplement);
        if(JoinExpr==null) {
            JoinExpr = ProceedJoinSelect(Joins,JoinImplement,Left);
            CacheJoins.put(JoinImplement,JoinExpr);
        }

        return JoinExpr;
    }

    abstract SourceExpr ProceedJoinSelect(JoinList Joins,Map<PropertyInterface,SourceExpr> JoinImplement,boolean Left);
    
    // возвращает класс значения
    // если null то не подходит по интерфейсу
    abstract public Class GetValueClass();
    
    // возвращает то и только то мн-во интерфейсов которые заведомо дают этот интерфейс (GetValueClass >= ReqValue)
    // если null то когда в принципе дает значение
    abstract public InterfaceClassSet GetClassSet(Class ReqValue);
    
    abstract public String GetDBType();
    
    // для отладки
    String OutName = "";
    
    abstract boolean HasChanges(ChangesSession Session);

    // заполняет список, возвращает есть ли изменения
    abstract boolean FillAggregateList(List<AggregateProperty> ChangedProperties,Set<DataProperty> ChangedSet);
    
    SelectQuery GetOutSelect(DataAdapter Adapter) {
        JoinList Joins = new JoinList();
        Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
        SourceExpr ValueExpr = JoinSelect(Joins,JoinImplement,false);
        
        ListIterator<From> i = Joins.listIterator();
        SelectQuery Select = new SelectQuery(i.next());
        while(i.hasNext()) Select.From.Joins.add(i.next());
        Iterator<T> ii = Interfaces.iterator();
        int KeyNum = 0;
        while(ii.hasNext()) Select.Expressions.put("key"+(KeyNum++),JoinImplement.get(ii.next()));
        Select.Expressions.put("value",ValueExpr);
        
        return Select;
    }
    
    void Out(DataAdapter Adapter) throws SQLException {
        Adapter.OutSelect(GetOutSelect(Adapter));
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
    ChangeTable ChangeTable;
    
    void FillChangeTable() {
        ChangeTable = TableFactory.GetChangeTable(Interfaces.size(),GetDBType());
        // если нету Map'a построим
        Iterator<T> i = Interfaces.iterator();
        if(ChangeTableMap==null) {
            ChangeTableMap = new HashMap();
            i = Interfaces.iterator();
            Iterator<KeyField> io = ChangeTable.Objects.iterator();
            while (i.hasNext()) ChangeTableMap.put(i.next(),io.next());
        }        
    }
    
    // получает таблицу и вычищает все из сессии
    void StartChangeTable(DataAdapter Adapter,ChangesSession Session) throws SQLException {
        FromTable DropSession = new FromTable(ChangeTable.Name);
        DropSession.Wheres.add(new FieldValueWhere(Session.ID,ChangeTable.Session.Name));
        DropSession.Wheres.add(new FieldValueWhere(ID,ChangeTable.Property.Name));
        Adapter.DeleteRecords(DropSession);
    }
    
    // получает UnionQuery с проставленными ключами
    UnionQuery GetChangeUnion(ChangesSession Session,int Operator) {
        // конечный результат, с ключами и выражением 
        UnionQuery ResultQuery = new UnionQuery(Operator);
        Iterator<T> im = Interfaces.iterator();
        while (im.hasNext()) 
            ResultQuery.Keys.add(ChangeTableMap.get(im.next()).Name);
        ResultQuery.Values.add(ChangeTable.Value.Name);
        ResultQuery.ValueKeys.put(ChangeTable.Session.Name,Session.ID);
        ResultQuery.ValueKeys.put(ChangeTable.Property.Name,ID);

        return ResultQuery;
    }
    
    // связывает именно измененные записи из сессии
    // Value - что получать, 0 - новые значения, 1 - +(увеличение), 2 - старые значения
    SourceExpr ChangedJoinSelect(JoinList Joins,Map<PropertyInterface,SourceExpr> JoinImplement,ChangesSession Session,int Value) {
        Map<KeyField,SourceExpr> MapJoins = new HashMap();
        Iterator<PropertyInterface> i = (Iterator<PropertyInterface>) Interfaces.iterator();
        while(i.hasNext()) {
            PropertyInterface Interface = i.next();
            MapJoins.put(ChangeTableMap.get(Interface),JoinImplement.get(Interface));
        }
        MapJoins.put(ChangeTable.Session,new ValueSourceExpr(Session.ID));
        MapJoins.put(ChangeTable.Property,new ValueSourceExpr(ID));
        
        Map<Map<KeyField,SourceExpr>,FromTable> CacheTable = Joins.GetCacheMaps(ChangeTable);
        FromTable SelectChanges = CacheTable.get(MapJoins);
        if(SelectChanges==null) {
            SelectChanges = new FromTable(ChangeTable.Name);
            SelectChanges.Wheres.add(new FieldWhere(new ValueSourceExpr(Session.ID),ChangeTable.Session.Name));
            SelectChanges.Wheres.add(new FieldWhere(new ValueSourceExpr(ID),ChangeTable.Property.Name));

            i = (Iterator<PropertyInterface>) Interfaces.iterator();
            while(i.hasNext()) {
                PropertyInterface Interface = i.next();
                KeyField JoinField = ChangeTableMap.get(Interface);

                // сюда мы маппимся 
                if(!JoinImplement.containsKey(Interface)) {
                    SourceExpr JoinExpr = new FieldSourceExpr(SelectChanges,JoinField.Name);
                    JoinImplement.put(Interface,JoinExpr);
                    MapJoins.put(JoinField,JoinExpr);
                } else 
                    SelectChanges.Wheres.add(new FieldWhere(JoinImplement.get(Interface),JoinField.Name));
            }
            
            CacheTable.put(MapJoins,SelectChanges);
            Joins.add(SelectChanges);
        }
        
        FieldSourceExpr NewValue = new FieldSourceExpr(SelectChanges,ChangeTable.Value.Name);
        FieldSourceExpr PrevValue = new FieldSourceExpr(SelectChanges,ChangeTable.PrevValue.Name);
        int ChangedType = GetChangeType(Session);
        // теперь определимся что возвращать
        if(Value==2 && ChangedType==2) {
            return PrevValue;
        }

        if(Value==ChangedType || (Value==0 && ChangedType==2))
            return NewValue;

        if(Value==1 && ChangedType==2) {
            ListSourceExpr Result = new ListSourceExpr(1);
            Result.AddOperand(NewValue,1);
            Result.AddOperand(PrevValue,-1);
        }

        Integer A = null;
        A.equals(1);

        return null;
    }

    void OutChangesTable(DataAdapter Adapter, ChangesSession Session) throws SQLException {
        SelectQuery SelectChanges = new SelectQuery(new FromTable(ChangeTable.Name));
        SelectChanges.From.Wheres.add(new FieldWhere(new ValueSourceExpr(Session.ID),ChangeTable.Session.Name));
        SelectChanges.From.Wheres.add(new FieldWhere(new ValueSourceExpr(ID),ChangeTable.Property.Name));
        
        Iterator<PropertyInterface> i = (Iterator<PropertyInterface>) Interfaces.iterator();
        while(i.hasNext()) {
            String Field = ChangeTableMap.get(i.next()).Name;
            SelectChanges.Expressions.put(Field,new FieldSourceExpr(SelectChanges.From,Field));
        }
        
        SelectChanges.Expressions.put(ChangeTable.Value.Name,new FieldSourceExpr(SelectChanges.From,ChangeTable.Value.Name));
        
        Adapter.OutSelect(SelectChanges);
    }

    // сохраняет изменения в таблицу
    void SaveChanges(DataAdapter Adapter,ChangesSession Session) throws SQLException {

        Integer ChangeType = SessionChanged.get(Session);
        if(ChangeType==null) return;
        
        Map<KeyField,T> MapKeys = new HashMap();
        Table SourceTable = GetTable(MapKeys);

        // сначала вкидываем Insert того чего нету в Source             
        SelectQuery Insert = new SelectQuery(new FromTable(ChangeTable.Name));
        Insert.From.Wheres.add(new FieldWhere(new ValueSourceExpr(Session.ID),ChangeTable.Session.Name));
        Insert.From.Wheres.add(new FieldWhere(new ValueSourceExpr(ID),ChangeTable.Property.Name));

        FromTable SelectSource = new FromTable(SourceTable.Name);
        Insert.From.Joins.add(SelectSource);
        SelectSource.JoinType = "LEFT";

        Iterator<KeyField> ifs = MapKeys.keySet().iterator();
        KeyField Key = null;
        while(ifs.hasNext()) {
            Key = ifs.next();
            String ChangeKey = ChangeTableMap.get(MapKeys.get(Key)).Name;
            FieldSourceExpr FieldExpr = new FieldSourceExpr(Insert.From,ChangeKey);
            SelectSource.Wheres.add(new FieldWhere(FieldExpr,Key.Name));
            
            Insert.Expressions.put(Key.Name,FieldExpr);
        }
        
        // сначала сделаем Insert, на RIGHT JOIN, IS NULL
        Insert.From.Wheres.add(new SourceIsNullWhere(new FieldSourceExpr(SelectSource,Key.Name)));
//        try {
        Adapter.InsertSelect(SourceTable,Insert);
//        } catch(Exception e) {
//            Adapter.InsertSelect(SourceTable,Insert);
//        }
        
        // затем Update на inner join
        SelectQuery Update = new SelectQuery(new FromTable(SourceTable.Name));

        FromTable SelectChanges = new FromTable(ChangeTable.Name);
        SelectChanges.Wheres.add(new FieldWhere(new ValueSourceExpr(Session.ID),ChangeTable.Session.Name));
        SelectChanges.Wheres.add(new FieldWhere(new ValueSourceExpr(ID),ChangeTable.Property.Name));
        Update.From.Joins.add(SelectChanges);

        ifs = MapKeys.keySet().iterator();
        while(ifs.hasNext()) {
            Key = ifs.next();
            SelectChanges.Wheres.add(new FieldWhere(new FieldSourceExpr(Update.From,Key.Name),ChangeTableMap.get(MapKeys.get(Key)).Name));
        }
        
        SourceExpr ChangeExpr = new FieldSourceExpr(SelectChanges,ChangeTable.Value.Name);
        if(ChangeType==1) { // +
            FormulaSourceExpr SumExpr = new FormulaSourceExpr("prm1+ISNULL(prm2,0)");
            SumExpr.Params.put("prm1",ChangeExpr);
            SumExpr.Params.put("prm2",new FieldSourceExpr(Update.From,Field.Name));
            ChangeExpr = SumExpr;
        }
        
        Update.Expressions.put(Field.Name,ChangeExpr);
        Adapter.UpdateRecords(Update);
    }

    
    Field Field;
    abstract Table GetTable(Map<KeyField,T> MapJoins);
    
    boolean IsPersistent() {
        return Field!=null;
    }
            
    // при текущей реализации проше предполагать что не имплементнутые Interface имеют null Select !!!!!
    SourceExpr ProceedJoinSelect(JoinList Joins,Map<PropertyInterface,SourceExpr> JoinImplement,boolean Left) {
        if(IsPersistent() && !(this instanceof AggregateProperty && TableFactory.ReCalculateAggr)) { // для тестирования 2-е условие
            Map<KeyField,T> MapJoins = new HashMap();
            Table SourceTable = GetTable(MapJoins);

            // прогоним проверим все ли Implement'ировано
            Map<KeyField,SourceExpr> MapFields = new HashMap<KeyField,SourceExpr>();
            Iterator<KeyField> it = SourceTable.KeyFields.iterator();
            while(it.hasNext()) {
                KeyField TableField = it.next();
                MapFields.put(TableField,JoinImplement.get(MapJoins.get(TableField)));
            }

            // поищем что такая таблица уже есть в запросе причем с такими же полями и такими же Join'ами
            Map<Map<KeyField,SourceExpr>,FromTable> CacheTable = Joins.GetCacheMaps(SourceTable);
            FromTable JoinTable = CacheTable.get(MapFields);
            if (JoinTable==null) {
                JoinTable = new FromTable(SourceTable.Name);
                it = SourceTable.KeyFields.iterator();
                while (it.hasNext())
                {
                    KeyField TableField = it.next();
                    PropertyInterface Interface = MapJoins.get(TableField);
                    SourceExpr JoinExpr = JoinImplement.get(Interface);
                    if(JoinExpr==null) {
                        JoinExpr = new FieldSourceExpr(JoinTable,TableField.Name);
                        JoinImplement.put(Interface,JoinExpr);
                        MapFields.put(TableField,JoinExpr);
                    } else 
                        JoinTable.Wheres.add(new FieldWhere(JoinExpr,TableField.Name));
                }

                CacheTable.put(MapFields, JoinTable);
                Joins.add(JoinTable);
                
                if(Left) JoinTable.JoinType = "LEFT";
            } 

            if(!Left) JoinTable.JoinType = "";

            return new FieldSourceExpr(JoinTable,Field.Name);
        } else {
            return ((AggregateProperty)this).CalculateJoinSelect(Joins,JoinImplement,Left);
        }
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
    }

    // при текущей реализации проше предполагать что не имплементнутые Interface имеют null Select !!!!!
    Table GetTable(Map<KeyField,DataPropertyInterface> MapJoins) {
        return TableFactory.GetTable(Interfaces,MapJoins);
    }
    
    public Class GetValueClass() {
        // пока так потом сделаем перегрузку по классам
        Iterator<DataPropertyInterface> i = Interfaces.iterator();
        while(i.hasNext()) {
            DataPropertyInterface DataInterface = i.next();
            
            // если не тот класс сразу зарубаем
            if(!DataInterface.ValueClass.IsParent(DataInterface.Class)) return null;
        }

        return Value;
    }
    
    public InterfaceClassSet GetClassSet(Class ReqValue) {
        if(ReqValue==null || Value.IsParent(ReqValue)) {
            InterfaceClassSet Result = new InterfaceClassSet();
            InterfaceClass ResultInterface = new InterfaceClass();
            Iterator<DataPropertyInterface> i = Interfaces.iterator();
            while(i.hasNext()) {
                DataPropertyInterface Interface = i.next();
                ResultInterface.put(Interface, Interface.Class);
            }
            Result.add(ResultInterface);

            return Result;
        } else
            return null;
    }

    public String GetDBType() {
        return Value.GetDBType();
    }

    void ChangeProperty(DataAdapter Adapter,Map<DataPropertyInterface,Integer> Keys,Object NewValue) throws SQLException {
        Map<KeyField,DataPropertyInterface> MapJoins = new HashMap<KeyField,DataPropertyInterface>();
        Table SourceTable = TableFactory.GetTable(Interfaces,MapJoins);
        Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
        Iterator<KeyField> i = SourceTable.KeyFields.iterator();
        while(i.hasNext()) {
            KeyField Key = i.next();
            InsertKeys.put(Key,Keys.get(MapJoins.get(Key)));
        }

        Map<Field,Object> InsertValues = new HashMap<Field,Object>();
        InsertValues.put(Field,NewValue);

        Adapter.UpdateInsertRecord(SourceTable,InsertKeys,InsertValues);
    }

    // пока оставим и старый метод
    void ChangeProperty(DataAdapter Adapter,Map<DataPropertyInterface,Integer> Keys,Object NewValue,ChangesSession Session) throws SQLException {
        // записываем в таблицу изменений
        Map<KeyField,Integer> InsertKeys = new HashMap();
        Iterator<DataPropertyInterface> i = Interfaces.iterator();
        while(i.hasNext()) {
            DataPropertyInterface Interface = i.next();
            InsertKeys.put(ChangeTableMap.get(Interface),Keys.get(Interface));
        }
        
        InsertKeys.put(ChangeTable.Property,ID);
        InsertKeys.put(ChangeTable.Session,Session.ID);

        Map<Field,Object> InsertValues = new HashMap();
        InsertValues.put(ChangeTable.Value,NewValue);

        Adapter.UpdateInsertRecord(ChangeTable,InsertKeys,InsertValues);

        // пометим изменение св-ва
        Session.Properties.add(this);
        SessionChanged.put(Session,0);
    }
    
    // заполняет список, возвращает есть ли изменения
    public boolean FillAggregateList(List<AggregateProperty> ChangedProperties,Set<DataProperty> ChangedSet) {
        return ChangedSet.contains(this);
    }
}

abstract class AggregateProperty<T extends PropertyInterface> extends ObjectProperty<T> {
    
    AggregateProperty(TableFactory iTableFactory) {super(iTableFactory);}

    // заполняет требования к изменениям
    abstract void FillRequiredChanges(ChangesSession Session);

    // заполняет инкрементные изменения
    void IncrementChanges(DataAdapter Adapter, ChangesSession Session) throws SQLException {

        StartChangeTable(Adapter,Session);
        
        Query ResultQuery = QueryIncrementChanged(Session);

        int ChangeType = GetChangeType(Session);
        // проверим что вернули что вернули то что надо
        if(ChangeType==2) {
            if(QueryIncrementType != ChangeType) {
                // нужно LEFT JOIN'ить старые
                SelectQuery NewQuery = new SelectQuery(new FromQuery(ResultQuery));
                
                JoinList Joins = new JoinList();
                Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
                Iterator<T> i = Interfaces.iterator();
                while(i.hasNext()) {
                    T Interface = i.next();
                    JoinImplement.put(Interface,new FieldSourceExpr(NewQuery.From,ChangeTableMap.get(Interface).Name));
                }
                
//                JoinSelect(,);
            }
        }
        
        ResultQuery.ValueKeys.put(ChangeTable.Session.Name,Session.ID);
        ResultQuery.ValueKeys.put(ChangeTable.Property.Name,ID);

        Adapter.InsertSelect(ChangeTable,ResultQuery);
        
        // помечаем изменение в сессии
        SessionChanged.put(Session,ResultValueType);
    }

    // для возврата чтобы не плодить классы
    Integer QueryIncrementType;
    // получает запрос для инкрементных изменений
    abstract Query QueryIncrementChanged(ChangesSession Session);

    Map<DataPropertyInterface,T> AggregateMap;
    
    // сначала проверяет на persistence
    Table GetTable(Map<KeyField,T> MapJoins) {
        if(AggregateMap==null) {
            AggregateMap = new HashMap();
            InterfaceClass Parent = GetClassSet(null).GetCommonParent();
            Iterator<T> i = Interfaces.iterator();
            while(i.hasNext()) {
                T Interface = i.next();
                AggregateMap.put(new DataPropertyInterface(Parent.get(Interface)),Interface);
            }
        }
        
        Map<KeyField,DataPropertyInterface> MapData = new HashMap();
        Table SourceTable = TableFactory.GetTable(AggregateMap.keySet(),MapData);
        // перекодирукм на MapJoins
        if(MapJoins!=null) {
            Iterator<KeyField> im = MapData.keySet().iterator();
            while(im.hasNext()) {
                KeyField MapField = im.next();
                MapJoins.put(MapField,AggregateMap.get(MapData.get(MapField)));
            }
        }
        
        return SourceTable;
    }
    
    // расчитывает JoinSelect
    abstract SourceExpr CalculateJoinSelect(JoinList Joins,Map<PropertyInterface,SourceExpr> JoinImplement,boolean Left);
    
    // проверяет аггрегацию для отладки
    void CheckAggregation(DataAdapter Adapter,String Caption) throws SQLException {
        System.out.println("----CheckAggregations "+Caption+"-----");

        SelectQuery AggrSelect;
        AggrSelect = GetOutSelect(Adapter);
        List<Map<String,Object>> AggrResult = Adapter.ExecuteSelect(AggrSelect);
        TableFactory.ReCalculateAggr = true;
        AggrSelect = GetOutSelect(Adapter);
        List<Map<String,Object>> CalcResult = Adapter.ExecuteSelect(AggrSelect);
        TableFactory.ReCalculateAggr = false;
        
        Iterator<Map<String,Object>> i = AggrResult.iterator();
        while(i.hasNext()) {
            Map<String,Object> Row = i.next();
            if(CalcResult.remove(Row)) 
                i.remove();
            else {
                Object Value =  Row.get("value");
                if(Value==null) 
                    i.remove();
                else {
                    if(Value instanceof Integer && ((Integer)Value).equals(0)) i.remove();
                    if(Value instanceof String && ((String)Value).trim().length()==0) i.remove();
                }
            }
        }
        // вычистим и отсюда 0
        i = CalcResult.iterator();
        while(i.hasNext()) {
            Object Value =  i.next().get("value");
            if(Value==null) 
                i.remove();
            else {
                if(Value instanceof Integer && ((Integer)Value).equals(0)) i.remove();
                if(Value instanceof String && ((String)Value).trim().length()==0) i.remove();
            }
        }

        if(CalcResult.size()>0 || AggrResult.size()>0) {
            System.out.println("----Aggr-----");
            i = AggrResult.iterator();
            while(i.hasNext()) 
                System.out.println(i.next());
            System.out.println("----Calc-----");
            i = CalcResult.iterator();
            while(i.hasNext()) 
                System.out.println(i.next());
        }
    }
}

class PropertyMapImplement extends PropertyImplement<PropertyInterface> implements PropertyInterfaceImplement {
    
    PropertyMapImplement(Property iProperty) {super(iProperty);}

    public SourceExpr MapJoinSelect(JoinList Joins,Map<PropertyInterface,SourceExpr> JoinImplement,boolean Left,ChangesSession Session,int Value) {
        
        // собираем null ссылки чтобы обновить свои JoinExprs
        Collection<PropertyInterface> NullInterfaces = new ArrayList<PropertyInterface>();
        // соберем интерфейс по всем нижним интерфейсам
        Map<PropertyInterface,SourceExpr> MapImplement = new HashMap();
        Iterator<PropertyInterface> i = Property.Interfaces.iterator();
        while (i.hasNext()) {
            PropertyInterface ImplementInterface = i.next();
            SourceExpr JoinExpr = JoinImplement.get(Mapping.get(ImplementInterface));
            if(JoinExpr==null) 
                NullInterfaces.add(ImplementInterface);
            else
                MapImplement.put(ImplementInterface,JoinExpr);
        }
        
        SourceExpr JoinSource = (Session!=null?((ObjectProperty)Property).ChangedJoinSelect(Joins,MapImplement,Session,Value):Property.JoinSelect(Joins,MapImplement,Left));
        
        // прогоним и проверим если кто-то изменил с null себе закинем JoinExprs
        Iterator<PropertyInterface> in = NullInterfaces.iterator();
        while (in.hasNext()) {
            PropertyInterface ImplementInterface = in.next();
            JoinImplement.put(Mapping.get(ImplementInterface),MapImplement.get(ImplementInterface));
        }
        
        return JoinSource;
    }
    
    public Class MapGetValueClass() {
        Iterator<PropertyInterface> i = Property.Interfaces.iterator();
        while (i.hasNext()) {
            PropertyInterface ImplementInterface = i.next();
            ImplementInterface.ValueClass = Mapping.get(ImplementInterface).ValueClass;
        }

        return Property.GetValueClass();
    }

    public InterfaceClassSet MapGetClassSet(Class ReqValue) {
        InterfaceClassSet Result = new InterfaceClassSet();
        InterfaceClassSet PropertySet = Property.GetClassSet(ReqValue);
        // теперь надо мапнуть на базовые интерфейсы
        Iterator<InterfaceClass> i = PropertySet.iterator();
        while(i.hasNext()) {
            InterfaceClass ClassSet = i.next();
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
    public boolean MapFillAggregateList(List<AggregateProperty> ChangedProperties,Set<DataProperty> ChangedSet) {
        return Property.FillAggregateList(ChangedProperties,ChangedSet);
    }
}

class RelationProperty extends AggregateProperty<PropertyInterface> {
    PropertyImplement<PropertyInterfaceImplement> Implements;
    
    RelationProperty(TableFactory iTableFactory, Property iProperty) {
        super(iTableFactory);
        Implements = new PropertyImplement<PropertyInterfaceImplement>(iProperty);
    }

    SourceExpr CalculateJoinSelect(JoinList Joins,Map<PropertyInterface,SourceExpr> JoinImplement,boolean Left) {
        // для всех нижних делаем JoinSelect
        Iterator<PropertyInterface> im = Implements.Property.Interfaces.iterator();
        Map<PropertyInterface,SourceExpr> MapImplement = new HashMap();
        while (im.hasNext()) {
            PropertyInterface ImplementInterface = im.next();
            MapImplement.put(ImplementInterface, Implements.Mapping.get(ImplementInterface).MapJoinSelect(Joins,JoinImplement,Left,null,0));
        }

        return Implements.Property.JoinSelect(Joins,MapImplement,Left);
    }

    public Class GetValueClass() {
        // пока так потом сделаем перегрузку по классам
        Iterator<PropertyInterface> im = Implements.Property.Interfaces.iterator();
        while (im.hasNext())
        {
            PropertyInterface ImplementInterface = im.next();
            ImplementInterface.ValueClass = Implements.Mapping.get(ImplementInterface).MapGetValueClass();
            // если null то уже не подходит по интерфейсу
            if(ImplementInterface.ValueClass==null) return null; 
        }
        
        return Implements.Property.GetValueClass();
    }

    public InterfaceClassSet GetClassSet(Class ReqValue) {
        InterfaceClassSet Result = new InterfaceClassSet();
        InterfaceClassSet RelationSet = Implements.Property.GetClassSet(ReqValue);
        Iterator<InterfaceClass> i = RelationSet.iterator();
        while(i.hasNext()) {
            // все варианты даем на вход нижним и они And'ат, а потом все Or'ся            
            InterfaceClassSet ItSet = new InterfaceClassSet();
            InterfaceClass ItClass = i.next();
            Iterator<PropertyInterface> ip = Implements.Property.Interfaces.iterator();
            while(ip.hasNext()) {
                PropertyInterface Interface = ip.next();
                ItSet = ItSet.AndSet(Implements.Mapping.get(Interface).MapGetClassSet(ItClass.get(Interface)));
            }

            Result.OrSet(ItSet);
        }
        
        return Result;
    }

    List<PropertyInterface> GetChangedImplements(ChangesSession Session) {
        Iterator<PropertyInterface> im = Implements.Property.Interfaces.iterator();
        
        List<PropertyInterface> ChangedProperties = new ArrayList();
        while(im.hasNext()) {
            PropertyInterface Interface = im.next();
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

            Iterator<PropertyInterface> i = Implements.Property.Interfaces.iterator();
            while(i.hasNext()) {
                PropertyInterfaceImplement Interface = Implements.Mapping.get(i.next());
                if(Interface.MapHasChanges(Session)) // значит PropertyMapImplement на ObjectProperty
                    ((ObjectProperty)(((PropertyMapImplement)Interface).Property)).SetChangeType(Session,(Implements.Property instanceof MultiplyFormulaProperty && ChangeType==1?1:0)) ;
            }
        }
    }
    
    // инкрементные св-ва
    Query QueryIncrementChanged(ChangesSession Session) {
        
        // алгоритм такой - для всех map св-в (в которых были изменения) строим подмножества изм. св-в
        // далее реализации этих св-в "замещаем" (то есть при JOIN будем подставлять), с остальными св-вами делаем LEFT JOIN на IS NULL 
        // и JOIN'им с основным св-вом делая туда FULL JOIN новых значений
        // или UNION или большой FULL JOIN в нужном порядке (и не делать LEFT JOIN на IS NULL) и там сделать большой NVL
        
        List<PropertyInterface> ChangedProperties = GetChangedImplements(Session);

        if(ChangedProperties.size()==0)
            QueryIncrementType = ((ObjectProperty)Implements.Property).GetChangeType(Session);
        else
            QueryIncrementType = 0;

        // конечный результат, с ключами и выражением 
        UnionQuery ResultQuery = GetChangeUnion(Session,(Implements.Property instanceof MultiplyFormulaProperty?1:2)); // по умолчанию на ISNULL (но если Multiply то 1 на сумму)
        if(QueryIncrementType==2) ResultQuery.Values.add(ChangeTable.PrevValue.Name);

        // строим все подмножества св-в в лексикографическом порядке
        ListIterator<List<PropertyInterface>> il = (new SetBuilder<PropertyInterface>()).BuildSubSetList(ChangedProperties).listIterator();

        while(il.hasNext()) {
            List<PropertyInterface> ChangeProps = il.next();
            // будем докидывать FULL JOIN'ы в нужном порядке получая соотв. NVL
            // нужно за Join'ить со старыми значениями (исключить этот JOIN если пустое подмн-во !!! собсно в этом и заключается оптимизация инкрементности), затем с новыми (если она есть)
            for(int ij=(ChangeProps.size()==0?1:0);ij<(Implements.Property.HasChanges(Session)?2:1);ij++) {
                SelectQuery SubQuery = new SelectQuery(null);
                JoinList Joins = new JoinList();
                // JoinImplement'ы этого св-ва
                Map<PropertyInterface,SourceExpr> MapImplement = new HashMap();
                // JoinImplement'ы основного св-ва
                Map<PropertyInterface,SourceExpr> MapJoinImplement = new HashMap();

                Iterator<PropertyInterface> im = Implements.Property.Interfaces.iterator();
                while (im.hasNext()) {
                    PropertyInterface ImplementInterface = im.next();
                    MapJoinImplement.put(ImplementInterface,Implements.Mapping.get(ImplementInterface).MapJoinSelect(Joins,MapImplement,false,(ChangeProps.contains(ImplementInterface)?Session:null),0));
                }

                SourceExpr ValueExpr = null;
                if(ij==0) 
                    ValueExpr = Implements.Property.JoinSelect(Joins,MapJoinImplement,false);
                else {
                    ValueExpr = ((ObjectProperty)Implements.Property).ChangedJoinSelect(Joins,MapJoinImplement,Session,QueryIncrementType);
                    if(QueryIncrementType==2) {
                        // если и предыдущее надо, то закидываем предыдущее, а потом новое 
                        SubQuery.Expressions.put(ChangeTable.PrevValue.Name,ValueExpr);
                        ValueExpr = ((ObjectProperty)Implements.Property).ChangedJoinSelect(Joins,MapJoinImplement,Session,0);
                    }
                }
                SubQuery.Expressions.put(ChangeTable.Value.Name,ValueExpr);

                // закинем все ключи в запрос
                im = Interfaces.iterator();
                while (im.hasNext()) {
                    PropertyInterface Interface = im.next();
                    SubQuery.Expressions.put(ChangeTableMap.get(Interface).Name,MapImplement.get(Interface));
                }

                // закинем Join'ы как обычно
                ListIterator<From> is = Joins.listIterator();
                SubQuery.From = is.next();
                while(is.hasNext()) SubQuery.From.Joins.add(is.next());

                ResultQuery.Unions.add(SubQuery);
            }
        }
        
        return ResultQuery;
    }

    public String GetDBType() {
        return Implements.Property.GetDBType();
    }
    
    // заполняет список, возвращает есть ли изменения
    public boolean FillAggregateList(List<AggregateProperty> ChangedProperties,Set<DataProperty> ChangedSet) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = Implements.Property.FillAggregateList(ChangedProperties,ChangedSet);

        Iterator<PropertyInterface> i = Implements.Property.Interfaces.iterator();
        while (i.hasNext()) 
            Changed = Implements.Mapping.get(i.next()).MapFillAggregateList(ChangedProperties,ChangedSet) || Changed;

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
    String ViewName;
    String GroupField = "grfield";
    Map<GroupPropertyInterface,String> GroupKeys;
    
    void FillDB(DataAdapter Adapter,Integer ViewNum) throws SQLException {
        JoinList QueryJoins = new JoinList();
        GroupQuery SubQuery = new GroupQuery(null);

        Map<PropertyInterface,SourceExpr> GroupImplement = new HashMap();
        SubQuery.AggrExprs.put(GroupField,new GroupExpression(GroupProperty.JoinSelect(QueryJoins,GroupImplement,false),Operator));

        Integer KeyNum = 0;
        Iterator<GroupPropertyInterface> im = Interfaces.iterator();
        while (im.hasNext()) {
            GroupPropertyInterface ImplementInterface = im.next();
            SourceExpr JoinSource = ImplementInterface.Implement.MapJoinSelect(QueryJoins,GroupImplement,false,null,0);
            
            KeyNum++;
            String KeyField = "key"+KeyNum.toString();
            GroupKeys.put(ImplementInterface,KeyField);
            SubQuery.GroupBy.put(KeyField,JoinSource);
        }

        // закидываем From'ы
        ListIterator<From> is = QueryJoins.listIterator();
        SubQuery.From = is.next();
        while (is.hasNext())
            SubQuery.From.Joins.add(is.next());

        // для всех классов нужно еще докинуть Join и фильтры на класс
        Iterator<PropertyInterface> ic = ToClasses.keySet().iterator();
        while(ic.hasNext()) {
            PropertyInterface ClassInterface = ic.next();
            SubQuery.From.Joins.add(TableFactory.ObjectTable.ClassJoinSelect(ToClasses.get(ClassInterface),GroupImplement.get(ClassInterface)));
        }

        ViewName = "view"+ViewNum;
        Adapter.CreateView(ViewName,SubQuery);
    }
    
    SourceExpr CalculateJoinSelect(JoinList Joins,Map<PropertyInterface,SourceExpr> JoinImplement,boolean Left) {
        // создадим сразу Select
        FromTable QuerySelect = new FromTable(ViewName);
        if(Left) QuerySelect.JoinType = "LEFT";

        Iterator<GroupPropertyInterface> im = Interfaces.iterator();
        while (im.hasNext()) {
            GroupPropertyInterface ImplementInterface = im.next();
            String KeyField = GroupKeys.get(ImplementInterface);
            // здесь нужно или Join'ить или свой FieldSourceExpr подставлять
            if(!JoinImplement.containsKey(ImplementInterface)) {
                JoinImplement.put(ImplementInterface,new FieldSourceExpr(QuerySelect,KeyField));
            } else {
                QuerySelect.Wheres.add(new FieldWhere(JoinImplement.get(ImplementInterface),KeyField));
            }
        }

        Joins.add(QuerySelect);
        return new FieldSourceExpr(QuerySelect,GroupField);
    }
    
    public Class GetValueClass() {
        
        InterfaceClassSet ClassSet = GetClassSet(null);
        
        // проверим на соотвествие интерфейса
        InterfaceClass ValueSet = new InterfaceClass();
        Iterator<GroupPropertyInterface> im = Interfaces.iterator();
        while (im.hasNext()) {
            GroupPropertyInterface Interface = im.next();
            ValueSet.put(Interface,Interface.ValueClass);
        }
        
        // GetClassSet по идее ValueClass'ы проставил
        if(ClassSet.OrItem(ValueSet))
            return GroupProperty.GetValueClass();
        else
            return null;
    }

    public InterfaceClassSet GetClassSet(Class ReqValue) {

        InterfaceClassSet Result = new InterfaceClassSet();

        // берем сначала все классы GroupProperty и интерфейсов, а затем гоним их через GetValueClass и то что получаем на выходе гоним наружу
        InterfaceClassSet GroupSet = GroupProperty.GetClassSet(ReqValue);
        Iterator<GroupPropertyInterface> ig = Interfaces.iterator();
        while (ig.hasNext())
            GroupSet = GroupSet.AndSet(ig.next().Implement.MapGetClassSet(null));

        // для всех классов нужно еще докинуть ограничения на явную заданные классы
        InterfaceClass GroupClasses = new InterfaceClass();
        Iterator<PropertyInterface> ic = ToClasses.keySet().iterator();
        while(ic.hasNext()) {
            PropertyInterface ClassInterface = ic.next();
            GroupClasses.put(ClassInterface,ToClasses.get(ClassInterface));
        }
        GroupSet = GroupSet.AndItem(GroupClasses);
        
        Iterator<InterfaceClass> i = GroupSet.iterator();
        while(i.hasNext()) {
            InterfaceClass ResultSet = new InterfaceClass();
            InterfaceClass ClassSet = i.next();

            Iterator<PropertyInterface> im = GroupProperty.Interfaces.iterator();
            while (im.hasNext()) {
                PropertyInterface Interface = im.next();
                Interface.ValueClass = ClassSet.get(Interface);
            }
            
            ig = Interfaces.iterator();
            while (ig.hasNext()) {
                GroupPropertyInterface GroupInterface = ig.next();
                ResultSet.put(GroupInterface,GroupInterface.Implement.MapGetValueClass());
            }

            Result.OrItem(ResultSet);
        }

        return Result;
    }

    public String GetDBType() {
        return GroupProperty.GetDBType();
    }
    
    // заполняет список, возвращает есть ли изменения
    public boolean FillAggregateList(List<AggregateProperty> ChangedProperties,Set<DataProperty> ChangedSet) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = GroupProperty.FillAggregateList(ChangedProperties,ChangedSet);

        Iterator<GroupPropertyInterface> i = Interfaces.iterator();
        while (i.hasNext()) 
            Changed = i.next().Implement.MapFillAggregateList(ChangedProperties,ChangedSet) || Changed;

        if(Changed)
            ChangedProperties.add(this);
        
        return Changed;
    }
    
    // получает всевозможные инкрементные запросы для обеспечения IncrementChanges
    GroupQuery IncrementQuery(ChangesSession Session,String ValueName,List<GroupPropertyInterface> ChangedProperties,int GroupSet,boolean ValueType,boolean InterfaceSubSet,boolean InterfaceEmptySet) {
        // ValueName куда значение класть
        // ChangedProperties чтобы по нескольку раз не считать
        // GroupSet -> 0(G) - новые, 1(A) - предыдущие, 2(G/A) - новые и предыдущие
        // ValueType -> true(=) - новые, false(P) - предыдущие
        // InterfaceSubSet -> true - включать все подмн-ва, false - одиночные подмн-ва
        // InterfaceEmptySet -> true - включать пустое подмн-во, false - не включать
        // P GroupValue -> чтобы знать на какое значение считать =(0)/+(1)/prev(2), Operand определяет SUM\MAX

        UnionQuery DataQuery = new UnionQuery(2);
                    
        // заполняем ключи и значения для DataQuery
        int DataKeysNum = 1;
        Map<PropertyInterface,String> DataKeysMap = new HashMap();
        Iterator<PropertyInterface> in = GroupProperty.Interfaces.iterator();
        while(in.hasNext()) {
            String KeyField = "dkey" + DataKeysNum++;
            DataKeysMap.put(in.next(), KeyField);
            DataQuery.Keys.add(KeyField);
        }
        Iterator<GroupPropertyInterface> im = Interfaces.iterator();
        while (im.hasNext()) DataQuery.Values.add(ChangeTableMap.get(im.next()).Name);
        DataQuery.Values.add(ValueName);

        for(int GroupOp=(GroupSet==1 || GroupSet==2?0:1);GroupOp<=((GroupSet==0 || GroupSet==2) && GroupProperty.HasChanges(Session)?1:0);GroupOp++) {
            // подмн-во Group
            ListIterator<List<GroupPropertyInterface>> il = null;
            if(InterfaceSubSet)
                il = (new SetBuilder<GroupPropertyInterface>()).BuildSubSetList(ChangedProperties).listIterator();
            else {
                List<List<GroupPropertyInterface>> ChangedList = new ArrayList();
                im = ChangedProperties.iterator();
                ChangedList.add(new ArrayList());
                while(im.hasNext()) {
                    List<GroupPropertyInterface> SingleList = new ArrayList();
                    SingleList.add(im.next());
                    ChangedList.add(SingleList);
                }
                il = ChangedList.listIterator();
            }

            // если не пустое скипаем
            if(!(InterfaceEmptySet || GroupOp==1)) 
                il.next();

            while(il.hasNext()) {
                List<GroupPropertyInterface> ChangeProps = il.next();
                SelectQuery SubQuery = new SelectQuery(null);
                JoinList Joins = new JoinList();

                // обнуляем, закидываем GroupProperty,
                Map<PropertyInterface,SourceExpr> GroupImplement = new HashMap();
                
                // значение
                SubQuery.Expressions.put(ValueName,(GroupOp==1?GroupProperty.ChangedJoinSelect(Joins,GroupImplement,Session,(ValueType?Operator:2)):GroupProperty.JoinSelect(Joins,GroupImplement,false)));

                // значения интерфейсов
                im = Interfaces.iterator();
                while (im.hasNext()) {
                    GroupPropertyInterface Interface = im.next();
                    SubQuery.Expressions.put(ChangeTableMap.get(Interface).Name,Interface.Implement.MapJoinSelect(Joins,GroupImplement,false,(ChangeProps.contains(Interface)?Session:null),ValueType?0:2));
                }
                
                // значения ключей базовые
                in = GroupProperty.Interfaces.iterator();
                while (in.hasNext()) {
                    PropertyInterface Interface = in.next();
                    SubQuery.Expressions.put(DataKeysMap.get(Interface),GroupImplement.get(Interface));
                }

                // закинем Join'ы как обычно
                ListIterator<From> is = Joins.listIterator();
                SubQuery.From = is.next();
                while(is.hasNext()) SubQuery.From.Joins.add(is.next());

                DataQuery.Unions.add(SubQuery);
            }
        }
        
        FromQuery FromDataQuery = new FromQuery(DataQuery);
        GroupQuery GroupQuery = new GroupQuery(FromDataQuery);
        im = Interfaces.iterator();
        while (im.hasNext()) {
            String KeyField = ChangeTableMap.get(im.next()).Name;
            GroupQuery.GroupBy.put(KeyField,new FieldSourceExpr(FromDataQuery,KeyField));
        }
        GroupQuery.AggrExprs.put(ValueName,new GroupExpression(new FieldSourceExpr(FromDataQuery,ValueName),Operator));

        return GroupQuery;
    }

    List<GroupPropertyInterface> GetChangedProperties(ChangesSession Session) {
        Iterator<GroupPropertyInterface> im = Interfaces.iterator();
        List<GroupPropertyInterface> ChangedProperties = new ArrayList();
        while(im.hasNext()) {
            GroupPropertyInterface Interface = im.next();
            // должен вернуть null если нету изменений (или просто транслирует интерфейс) иначе возвращает AggregateProperty
            if(Interface.Implement.MapHasChanges(Session)) ChangedProperties.add(Interface);
        }
        
        return ChangedProperties;
    }
}

class SumGroupProperty extends GroupProperty {

    SumGroupProperty(TableFactory iTableFactory,ObjectProperty iProperty) {super(iTableFactory,iProperty,1);}

    void FillRequiredChanges(ChangesSession Session) {
        // Group на ->1, Interface на ->2 - как было - возвр. 1 (на подчищение если (0 или 2) LEFT JOIN'им старые)
        if(GroupProperty.HasChanges(Session)) 
            GroupProperty.SetChangeType(Session,1);
        
        Iterator<GroupPropertyInterface> i = GetChangedProperties(Session).iterator();
        while(i.hasNext())
            ((ObjectProperty)((PropertyMapImplement)i.next().Implement).Property).SetChangeType(Session,2);
    }

    Query QueryIncrementChanged(ChangesSession Session) {
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
        UnionQuery ResultQuery = GetChangeUnion(Session,1);

        // InterfaceSubSet ij<=2
        // InterfaceValue ij<=2
        // InterfaceEmptySet ij!=2
        // GroupSet (ij==1,0,1)
        
        GroupQuery GroupQuery = null;
        if(GroupProperty.HasChanges(Session)) {
            GroupQuery = IncrementQuery(Session,ChangeTable.Value.Name,ChangedProperties,0,true,true,false);
            ResultQuery.Unions.add(GroupQuery);
            ResultQuery.Coeffs.put(GroupQuery,1);
        }

        if(ChangedProperties.size()>0) {
            GroupQuery = IncrementQuery(Session,ChangeTable.Value.Name,ChangedProperties,1,true,true,false);
            ResultQuery.Unions.add(GroupQuery);
            ResultQuery.Coeffs.put(GroupQuery,1);
            
            GroupQuery = IncrementQuery(Session,ChangeTable.Value.Name,ChangedProperties,1,false,false,false);
            ResultQuery.Unions.add(GroupQuery);
            ResultQuery.Coeffs.put(GroupQuery,-1);
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
            GroupProperty.SetChangeType(Session,1);
        
        Iterator<GroupPropertyInterface> i = GetChangedProperties(Session).iterator();
        while(i.hasNext())
            ((ObjectProperty)((PropertyMapImplement)i.next().Implement).Property).SetChangeType(Session,2);
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
        UnionQuery ChangeQuery = new UnionQuery(2);
        FromQuery FromChangeQuery = new FromQuery(ChangeQuery);
        SelectQuery ResultQuery = new SelectQuery(FromChangeQuery);

        // про Left Join'им старое значение и сразу запишем
        Iterator<GroupPropertyInterface> i = Interfaces.iterator();
        Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
        while(i.hasNext()) {
            GroupPropertyInterface Interface = i.next();
            String KeyName = ChangeTableMap.get(Interface).Name;
            ChangeQuery.Keys.add(KeyName);
            SourceExpr KeyExpr = new FieldSourceExpr(FromChangeQuery,KeyName);
            JoinImplement.put(Interface,KeyExpr);
            ResultQuery.Expressions.put(KeyName,KeyExpr);
        }
        
        ChangeQuery.Values.add(ChangeTable.SysValue.Name);
        ChangeQuery.Unions.add(IncrementQuery(Session,ChangeTable.SysValue.Name,ChangedProperties,2,false,false,false));
        
//        Adapter.OutSelect(IncrementQuery(Session,Table.SysValue.Name,ChangedProperties,2,false,false,false));
                
        ChangeQuery.Values.add(ChangeTable.Value.Name);
        ChangeQuery.Unions.add(IncrementQuery(Session,ChangeTable.Value.Name,ChangedProperties,2,true,true,false));

        // протранслируем все дальше
        JoinList JoinPrev = new JoinList();
        SourceExpr PrevValue = JoinSelect(JoinPrev,JoinImplement,true);
        ListIterator<From> ij = JoinPrev.listIterator();
        while(ij.hasNext()) FromChangeQuery.Joins.add(ij.next());
        
        ResultQuery.Expressions.put(ChangeTable.Session.Name,new ValueSourceExpr(Session.ID));
        ResultQuery.Expressions.put(ChangeTable.Property.Name,new ValueSourceExpr(ID));

        SourceExpr NewValue = new FieldSourceExpr(FromChangeQuery,ChangeTable.Value.Name);
        SourceExpr OldValue = new FieldSourceExpr(FromChangeQuery,ChangeTable.SysValue.Name);
                
        ResultQuery.Expressions.put(ChangeTable.Value.Name,NewValue);
        ResultQuery.Expressions.put(ChangeTable.SysValue.Name,OldValue);
        ResultQuery.Expressions.put(ChangeTable.PrevValue.Name,PrevValue);

        ValueSourceExpr MinValue = new ValueSourceExpr(-99999999);
        NewValue = new IsNullSourceExpr(NewValue,MinValue);
        OldValue = new IsNullSourceExpr(OldValue,MinValue);
        PrevValue = new IsNullSourceExpr(PrevValue,MinValue);
        
        // null ассоциируется с -бесконечностью
        // удаляем всех пришедших<=старых значений и ушедшие<старых значений
        // то есть пропускаем (пришедшие>старых значений) или (ушедшие=старых значений)
        FromChangeQuery.Wheres.add(new FieldOPWhere(new FieldExprCompareWhere(NewValue,PrevValue,1),new FieldExprCompareWhere(OldValue,PrevValue,0),false));

//        Adapter.OutSelect(ResultQuery);
        Adapter.InsertSelect(ChangeTable,ResultQuery);
        
        // для всех ушедших=старые значения (а они всегда <=) и пришедшие<старых значений обновляем по LEFT JOIN с запросом
		// MAX(по новым значениям, но весь запрос) просто берем запрос аналогии 2 (только взяв не только изменившиеся а все в том числе - пустое подмн-во)
                // G/A(2) =(true) (SS)(true) (с общ.)(true) =(null) MAX(=)
        FromTable FromChanges = new FromTable(ChangeTable.Name);
        FromChanges.Wheres.add(new FieldWhere(new ValueSourceExpr(Session.ID),ChangeTable.Session.Name));
        FromChanges.Wheres.add(new FieldWhere(new ValueSourceExpr(ID),ChangeTable.Property.Name));
        
        NewValue = new IsNullSourceExpr(new FieldSourceExpr(FromChanges,ChangeTable.Value.Name),MinValue);
        OldValue = new IsNullSourceExpr(new FieldSourceExpr(FromChanges,ChangeTable.SysValue.Name),MinValue);
        PrevValue = new IsNullSourceExpr(new FieldSourceExpr(FromChanges,ChangeTable.PrevValue.Name),MinValue);

        FromChanges.Wheres.add(new FieldOPWhere(new FieldExprCompareWhere(NewValue,PrevValue,2),new FieldExprCompareWhere(OldValue,PrevValue,0),false));
        
        // теоретически этот запрос нужно выполнять когда нету ни одной записи но пока этого проверять не будем
        SelectQuery UpdateQuery = new SelectQuery(FromChanges);
        GroupQuery NewQuery = IncrementQuery(Session,ChangeTable.Value.Name,ChangedProperties,2,true,true,true);
        FromQuery FromNewQuery = new FromQuery(NewQuery);
        FromNewQuery.JoinType = "LEFT";
        i = Interfaces.iterator();
        while(i.hasNext()) {
            GroupPropertyInterface Interface = i.next();
            String KeyName = ChangeTableMap.get(Interface).Name;
            FromNewQuery.Wheres.add(new FieldWhere(new FieldSourceExpr(FromChanges,KeyName),KeyName));
        }
        FromChanges.Joins.add(FromNewQuery);
        UpdateQuery.Expressions.put(ChangeTable.Value.Name,new FieldSourceExpr(FromNewQuery,ChangeTable.Value.Name));

 //       Adapter.OutSelect(UpdateQuery);
 //       System.out.println(UpdateQuery.GetUpdate());
        Adapter.UpdateRecords(UpdateQuery);
//        OutChangesTable(Adapter, Session);
        // помечаем изменение в сессии на =
        SessionChanged.put(Session,0);
    }

    Query QueryIncrementChanged(ChangesSession Session) {
        // так как мы перегружаем IncrementChanges, этот метод вызываться не может
        return null;
    }
}
abstract class ListProperty extends AggregateProperty<PropertyInterface> {

    ListProperty(TableFactory iTableFactory,int iOperator) {
        super(iTableFactory);
        Operands = new ArrayList();
        Operator = iOperator;
        Coeffs = new HashMap();
    }

    // имплементации св-в (полные)
    Collection<PropertyMapImplement> Operands;
    
    int Operator;
    // коэффициенты
    Map<PropertyMapImplement,Integer> Coeffs;

    SourceExpr CalculateJoinSelect(JoinList Joins, Map<PropertyInterface, SourceExpr> JoinImplement,boolean Left) {
        
        // собсно с этим методом и должна быть связана основная оптимизация
        // если JoinImplement все заданы то если есть все ключи (кол-во совпадает с размерностью) запускается JoinSelect с Left
        // иначе если не хватает ключей (тогда Left по-любому false)
        // пока сделаем просто если все операнды идут в одну таблицу с одним map'ом запускаем ее на обычный JoinSelect
        // иначе сделаем отдельные UnionQuery GetOutSelect'ов а затем их за Join'им

        // неструктурно потому как оптимизация
        // сначала если не совпадают интерфейсы попробуем достать может все идут в одну таблицу
        boolean IsCommon = true;
        if(!Left && JoinImplement.size()!=Interfaces.size()) {
            Table CommonTable = null;
            Map<KeyField,PropertyInterface> CommonJoins = null;
            Iterator<PropertyMapImplement> i = Operands.iterator();
            while(IsCommon && i.hasNext()) {
                PropertyMapImplement Operand = i.next();

                ObjectProperty OpProperty = (ObjectProperty)Operand.Property;
                if(OpProperty.Field!=null) {
                    Map<KeyField,PropertyInterface> MapJoins = new HashMap();
                    Table SourceTable = OpProperty.GetTable(MapJoins);
                    // дальше придется Map'нуть на базовый интерфейс 
                    Map<KeyField,PropertyInterface> BaseJoins = new HashMap();
                    Iterator<KeyField> ik = MapJoins.keySet().iterator();
                    while(ik.hasNext()) {
                        KeyField Key = ik.next();
                        BaseJoins.put(Key,Operand.Mapping.get(MapJoins.get(Key)));
                    }
                
                    if(CommonTable==null) {
                        CommonTable = SourceTable;
                        CommonJoins = BaseJoins;
                    } else 
                        IsCommon = (CommonTable==SourceTable && CommonJoins.equals(BaseJoins));
                } else
                    IsCommon = false;
            }
        }

        if(IsCommon) {
            ListSourceExpr ResultExpr = new ListSourceExpr(Operator);
            Iterator<PropertyMapImplement> i = Operands.iterator();
            while(i.hasNext()) {
                PropertyMapImplement Operand = i.next();
                ResultExpr.AddOperand(Operand.MapJoinSelect(Joins,JoinImplement,Left,null,0),Coeffs.get(Operand));
            }
            
            return ResultExpr;
        } else {
            // заполним названия полей в которые будем Select'ать
            String Value = "joinvalue";
            Map<PropertyInterface,String> MapFields = new HashMap();
            Iterator<PropertyInterface> ii = Interfaces.iterator();
            Integer KeyNum = 1;
            UnionQuery ResultQuery = new UnionQuery(Operator);
            while(ii.hasNext()) {
                String KeyField = "key"+(KeyNum++);
                MapFields.put(ii.next(),KeyField);
                ResultQuery.Keys.add(KeyField);
            }
            ResultQuery.Values.add(Value);

            Iterator<PropertyMapImplement> i = Operands.iterator();
            while(i.hasNext()) {
                PropertyMapImplement Operand = i.next();
                SelectQuery SubQuery = new SelectQuery(null);
                JoinList OpJoins = new JoinList();
                Map<PropertyInterface,SourceExpr> OpImplement = new HashMap();                
                SubQuery.Expressions.put(Value,Operand.MapJoinSelect(OpJoins,OpImplement,false,null,0));
                // запишем все ключи
                ii = OpImplement.keySet().iterator();
                while(ii.hasNext()) {
                    PropertyInterface Interface = ii.next();
                    SubQuery.Expressions.put(MapFields.get(Interface),OpImplement.get(Interface));
                }
                
                // From'ы как обычно закидываем
                ListIterator<From> is = OpJoins.listIterator();
                SubQuery.From = is.next();
                while(is.hasNext()) SubQuery.From.Joins.add(is.next());
                
                ResultQuery.Unions.add(SubQuery);
                ResultQuery.Coeffs.put(SubQuery,Coeffs.get(Operand));
            }
            
            FromQuery FromResultQuery = new FromQuery(ResultQuery);
            Joins.add(FromResultQuery);
            
            // за Join'им результат
            ii = Interfaces.iterator();
            while(ii.hasNext()) {
                PropertyInterface ResultInterface = ii.next();
                String KeyField = MapFields.get(ResultInterface);
                if(!JoinImplement.containsKey(ResultInterface)) {
                    JoinImplement.put(ResultInterface,new FieldSourceExpr(FromResultQuery,KeyField));
                } else {
                    FromResultQuery.Wheres.add(new FieldWhere(JoinImplement.get(ResultInterface),KeyField));
                }                
            }
            
            return (new FieldSourceExpr(FromResultQuery,Value));
        }
    }

    public Class GetValueClass() {
        // в отличии от Relation только когда есть хоть одно св-во
        Iterator<PropertyMapImplement> i = Operands.iterator();
        while(i.hasNext()) {
            Class ValueClass = i.next().MapGetValueClass();
            if(ValueClass!=null) return ValueClass;                
        }

        return null;
    }

    public InterfaceClassSet GetClassSet(Class ReqValue) {
        // в отличии от Relation делаем Or а не And
        InterfaceClassSet Result = new InterfaceClassSet();
        Iterator<PropertyMapImplement> i = Operands.iterator();
        while(i.hasNext())
            Result.OrSet(i.next().MapGetClassSet(ReqValue));

        return Result;
    }

    public String GetDBType() {
        return Operands.iterator().next().Property.GetDBType();        
    }

    boolean FillAggregateList(List<AggregateProperty> ChangedProperties, Set<DataProperty> ChangedSet) {
        if(ChangedProperties.contains(this)) return true;

        boolean Changed = false;

        Iterator<PropertyMapImplement> i = Operands.iterator();
        while (i.hasNext()) 
            Changed = i.next().MapFillAggregateList(ChangedProperties,ChangedSet) || Changed;

        if(Changed)
            ChangedProperties.add(this);
        
        return Changed;
    }

    List<PropertyMapImplement> GetChangedProperties(ChangesSession Session) {
        
        List<PropertyMapImplement> ChangedProperties = new ArrayList();
        Iterator<PropertyMapImplement> i = Operands.iterator();
        while(i.hasNext()) {
            PropertyMapImplement Operand = i.next();
            if(Operand.MapHasChanges(Session)) ChangedProperties.add(Operand);
        }
        
        return ChangedProperties;
    }
    
    Query IncrementQuery(ChangesSession Session,int ValueType) {
        //      	0                   1                           2
        //Max(0)	значение,SS,LJ      не может быть               значение,SS,LJ,prevv
        //Sum(1)	значение,SS,LJ      значение,без SS, без LJ     значение,SS,LJ,prevv
        //Override(2)	значение,SS,LJ      старое поле=null,SS, LJ     значение,SS,LJ,prevv

        // неструктурно как и все оптимизации
        
        List<PropertyMapImplement> ChangedProperties = GetChangedProperties(Session);
        
        boolean SumList = (Operator==1 && ValueType==1);
        
        // конечный результат, с ключами и выражением 
        UnionQuery ResultQuery = GetChangeUnion(Session,SumList?1:2);
        if(ValueType==2) ResultQuery.Values.add(ChangeTable.PrevValue.Name);

        ListIterator<List<PropertyMapImplement>> il = null;
        if(SumList) {
            // Sum, 1 - без SS
            List<List<PropertyMapImplement>> ChangedList = new ArrayList();
            Iterator<PropertyMapImplement> im = ChangedProperties.iterator();
            ChangedList.add(new ArrayList());
            while(im.hasNext()) {
                List<PropertyMapImplement> SingleList = new ArrayList();
                SingleList.add(im.next());
                ChangedList.add(SingleList);
            }
            il = ChangedList.listIterator();
        } else {
            il = (new SetBuilder<PropertyMapImplement>()).BuildSubSetList(ChangedProperties).listIterator();
            // здесь надо вырезать все
        }
        
        // пропустим пустое подмн-во
        il.next();
        while(il.hasNext()) {
            List<PropertyMapImplement> ChangedProps = il.next();
            
            JoinList SetJoins = new JoinList();
            ListSourceExpr ResultExpr = new ListSourceExpr(Operator);
            ListSourceExpr PrevExpr = (ValueType==2?new ListSourceExpr(Operator):null);

            Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
            // так как делается LEFT JOIN важен порядок сначала закидываем ChangedProps ChangedJoinSelect
            Iterator<PropertyMapImplement> i = ChangedProps.iterator();
            while(i.hasNext()) {
                PropertyMapImplement Operand = i.next();
                ResultExpr.AddOperand(Operand.MapJoinSelect(SetJoins,JoinImplement,false,Session,0),Coeffs.get(Operand));
                if(ValueType==2) PrevExpr.AddOperand(Operand.MapJoinSelect(SetJoins,JoinImplement,false,Session,2),Coeffs.get(Operand));
            }

            if(!SumList) {
                // здесь надо отрезать только те которые могут в принципе пересекаться по классам
                i = Operands.iterator();
                while(i.hasNext()) {
                    PropertyMapImplement Operand = i.next();
                    if(!ChangedProps.contains(Operand)) {
                        SourceExpr OperandExpr = Operand.MapJoinSelect(SetJoins,JoinImplement,true,null,0);
                        if(Operator==2 && ValueType==1) // если Override и 1 то нам нужно не само значение, а если не null то 0, иначе null (то есть не брать значение)
                            OperandExpr = new NullZeroSourceExpr(OperandExpr);

                        ResultExpr.AddOperand(OperandExpr,Coeffs.get(Operand));
                        if(ValueType==2) PrevExpr.AddOperand(Operand.MapJoinSelect(SetJoins,JoinImplement,true,null,2),Coeffs.get(Operand));
                    }
                }
            }

            ListIterator<From> ij = SetJoins.listIterator();
            SelectQuery SubQuery = new SelectQuery(ij.next());
            while(ij.hasNext()) SubQuery.From.Joins.add(ij.next());

            // закинем ключи значения
            Iterator<PropertyInterface> ii = Interfaces.iterator();
            while(ii.hasNext()) {
                PropertyInterface Interface = ii.next();
                SubQuery.Expressions.put(ChangeTableMap.get(Interface).Name,JoinImplement.get(Interface));
            }
            SubQuery.Expressions.put(ChangeTable.Value.Name,ResultExpr);
            if(ValueType==2) SubQuery.Expressions.put(ChangeTable.PrevValue.Name,PrevExpr);

            ResultQuery.Unions.add(SubQuery);
        }
        
        return ResultQuery;
    }
}


class SumListProperty extends ListProperty {
    
    SumListProperty(TableFactory iTableFactory) {super(iTableFactory,1);}

    void FillRequiredChanges(ChangesSession Session) {
        // если pers или 1 - Operand на ->1 - IncrementQuery(1) возвр. 1 - (на подчищение - если (0 или 2) LEFT JOIN'им старые)
        // иначе (не pers и (0 или 2)) - Operand на ->I - IncrementQuery (I) возвр. I

        int ChangeType = GetChangeType(Session);
        Iterator<PropertyMapImplement> i = Operands.iterator();
        while(i.hasNext()) {
            PropertyMapImplement Operand = i.next();
            if(Operand.MapHasChanges(Session)) 
                ((ObjectProperty)(Operand.Property)).SetChangeType(Session,IsPersistent() || ChangeType==1?1:ChangeType);
        }
    }

    Query QueryIncrementChanged(ChangesSession Session) {
        
        QueryIncrementType = (IsPersistent()?1:GetChangeType(Session));
        return IncrementQuery(Session,QueryIncrementType);
    }
    
    
}

class MaxListProperty extends ListProperty {

    MaxListProperty(TableFactory iTableFactory) {super(iTableFactory,0);}
    
    void FillRequiredChanges(ChangesSession Session) {
        // если pers или 0 - Operand на ->0 - IncrementQuery(0), возвр. 0 - (на подчищение - если (1 или 2) LEFT JOIN'им старые)
        // иначе (не pers и (1 или 2)) - Operand на ->2 - IncrementQuery(2), возвр. 2

        int ChangeType = GetChangeType(Session);
        Iterator<PropertyMapImplement> i = Operands.iterator();
        while(i.hasNext()) {
            PropertyMapImplement Operand = i.next();
            if(Operand.MapHasChanges(Session)) 
                ((ObjectProperty)(Operand.Property)).SetChangeType(Session,IsPersistent() || ChangeType==0?0:2);
        }
    }

    Query QueryIncrementChanged(ChangesSession Session) {

        QueryIncrementType = (IsPersistent() || GetChangeType(Session)==0?0:2);
        return IncrementQuery(Session,QueryIncrementType);
    }
}

class OverrideListProperty extends ListProperty {
    
    OverrideListProperty(TableFactory iTableFactory) {super(iTableFactory,2);}
    
    void FillRequiredChanges(ChangesSession Session) {
        // Operand на ->I - IncrementQuery(I) возвр. I

        Iterator<PropertyMapImplement> i = Operands.iterator();
        while(i.hasNext()) {
            PropertyMapImplement Operand = i.next();
            if(Operand.MapHasChanges(Session)) 
                ((ObjectProperty)(Operand.Property)).SetChangeType(Session,GetChangeType(Session));
        }
    }

    Query QueryIncrementChanged(ChangesSession Session) {

        QueryIncrementType = GetChangeType(Session);
        return IncrementQuery(Session,QueryIncrementType);
    }
}

// ФОРМУЛЫ

class FormulaPropertyInterface extends PropertyInterface {
    IntegralClass Class;
    
    FormulaPropertyInterface(IntegralClass iClass) {
        Class = iClass;
    }
}

// вообще Collection 
abstract class FormulaProperty<T extends FormulaPropertyInterface> extends Property<T> {
 
    public Class GetValueClass() {
        Iterator<FormulaPropertyInterface> i = (Iterator<FormulaPropertyInterface>) Interfaces.iterator();
        FormulaPropertyInterface Interface = null;
        while (i.hasNext()) {
            Interface = i.next();
            if(!Interface.ValueClass.IsParent(Interface.Class)) return null;
        }
        
        return Interface.Class;
    }

    public InterfaceClassSet GetClassSet(Class ReqValue) {
        InterfaceClass ResultSet = new InterfaceClass();

        Iterator<FormulaPropertyInterface> i = (Iterator<FormulaPropertyInterface>) Interfaces.iterator();
        FormulaPropertyInterface Interface = null;
        while (i.hasNext()) {
            Interface = i.next();
            ResultSet.put(Interface,Interface.Class);
        }

        InterfaceClassSet Result = new InterfaceClassSet();
        if(ReqValue==null || Interface.Class.IsParent(ReqValue)) Result.add(ResultSet);
        return Result;
    }

    public String GetDBType() {
        Iterator<FormulaPropertyInterface> i = (Iterator<FormulaPropertyInterface>) Interfaces.iterator();
        return i.next().Class.GetDBType();
    }

    boolean HasChanges(ChangesSession Session) {
        return false;
    }
    
    // заполняет список, возвращает есть ли изменения
    public boolean FillAggregateList(List<AggregateProperty> ChangedProperties,Set<DataProperty> ChangedSet) {
        return false;
    }
}

class StringFormulaPropertyInterface extends FormulaPropertyInterface {
    String Param;
    
    StringFormulaPropertyInterface(IntegralClass iClass,String iParam) {
        super(iClass);
        Param = iParam;
    }
}

class StringFormulaProperty extends FormulaProperty<StringFormulaPropertyInterface> {

    String Formula;

    StringFormulaProperty(String iFormula) {
        super();
        Formula = iFormula;
    }
    
    SourceExpr ProceedJoinSelect(JoinList Joins,Map<PropertyInterface,SourceExpr> JoinImplement,boolean Left) {
        FormulaSourceExpr Source = new FormulaSourceExpr(Formula);
                
        Iterator<StringFormulaPropertyInterface> it = Interfaces.iterator();
        while (it.hasNext()) {
            StringFormulaPropertyInterface Interface = it.next();
            Source.Params.put(Interface.Param,JoinImplement.get(Interface));
        }

        return Source;
    }
}


class MultiplyFormulaProperty extends FormulaProperty<FormulaPropertyInterface> {

    SourceExpr ProceedJoinSelect(JoinList Joins, Map<PropertyInterface, SourceExpr> JoinImplement, boolean Left) {

        FormulaSourceExpr Source = new FormulaSourceExpr("");
        int ParamNum=0;
        Iterator<FormulaPropertyInterface> it = Interfaces.iterator();
        while (it.hasNext()) {
            FormulaPropertyInterface Interface = it.next();
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
        Iterator<InterfaceClass> i = iterator();
        while(i.hasNext()) {
            InterfaceClass InClass = i.next();
            Iterator<PropertyInterface> id = ToDraw.iterator();
            while(id.hasNext()) {
                PropertyInterface Key = id.next();
                System.out.print(InClass.get(Key).ID.toString()+" ");
            }
            System.out.println();
       }
   }
    
    // нужен интерфейс слияния и пересечения с InterfaceClass

    InterfaceClassSet AndSet(InterfaceClassSet Op) {
        if(size()==0) return (InterfaceClassSet)Op.clone();
        if(Op.size()==0) return (InterfaceClassSet)clone();
        Iterator<InterfaceClass> i = iterator();
        InterfaceClassSet Result = new InterfaceClassSet();
        while(i.hasNext()) Result.OrSet(Op.AndItem(i.next()));
        return Result;
    }

    void OrSet(InterfaceClassSet Op) {
        Iterator<InterfaceClass> i = Op.iterator();
        while(i.hasNext()) OrItem(i.next());
    }

    InterfaceClassSet AndItem(InterfaceClass Op) {
        InterfaceClassSet Result = new InterfaceClassSet();
        if(size()>0) {
            Iterator<InterfaceClass> i = iterator();
            while(i.hasNext()) Result.OrSet(Op.And(i.next()));
        } else 
            Result.add(Op);        
        
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
        Iterator<InterfaceClass> i = iterator();
        while(i.hasNext()) CloneObject.add(i.next());
        return CloneObject;
    }
}

class InterfaceClass extends HashMap<PropertyInterface,Class> {
        
    InterfaceClassSet And(InterfaceClass AndOp) {
        Iterator<PropertyInterface> i = keySet().iterator();

        InterfaceClassSet Result = new InterfaceClassSet();

        Map<Class[],PropertyInterface> JoinClasses = new HashMap<Class[],PropertyInterface>();
                
        PropertyInterface Key;
        Class Class;
        Class[] SingleArray;
        while(i.hasNext()) {
            Key = i.next();
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

        Iterator<PropertyInterface> io = AndOp.keySet().iterator();
        while(io.hasNext()) {
            Key = io.next();
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
        Iterator<PropertyInterface> i = keySet().iterator();
        
        int ResultOr = -1;
        while(i.hasNext()) {
            PropertyInterface Key = i.next();
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
        Iterator<PropertyInterface> i = keySet().iterator();
        while(i.hasNext()) {
            PropertyInterface Key = i.next();
            put(Key,get(Key).CommonParent(Op.get(Key)));
        }
    }            
}
    
class ChangesSession {
    
    ChangesSession(Integer iID) {
        ID = iID;
        Properties = new HashSet();
    }

    Integer ID;
    
    Set<DataProperty> Properties;
}