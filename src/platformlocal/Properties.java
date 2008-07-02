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

    public SourceExpr MapJoinSelect(JoinList Joins,ChangesSession Session,Integer Value);
    public Class MapGetValueClass();
    public InterfaceClassSet MapGetClassSet(Class ReqValue);

    // для increment'ного обновления
    public boolean MapHasChanges(ChangesSession Session);
    
    abstract boolean MapFillAggregateList(List<AggregateProperty> ChangedProperties,Set<DataProperty> ChangedSet);

}
        

class PropertyInterface implements PropertyInterfaceImplement {
    //можно использовать JoinExps потому как все равну вернуться она не может потому как иначе она зациклится
    SourceExpr JoinImplement;
    Class ValueClass;
    
    public SourceExpr MapJoinSelect(JoinList Joins,ChangesSession Session,Integer Value) {
        return JoinImplement;
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
class JoinList extends ArrayList<Select> {
    JoinList() {
        CacheTables = new HashMap();
    }

    Map<Table,Map<Map<KeyField,SourceExpr>,SelectTable>> CacheTables;

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
    
}
abstract class Property<T extends PropertyInterface> {

    Integer ID=0;
    
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
    public SourceExpr JoinSelect(JoinList Joins) {

        Map<Map<PropertyInterface,SourceExpr>,SourceExpr> CacheJoins = SelectCacheJoins.get(Joins);
        if(CacheJoins==null) {
            CacheJoins = new HashMap<Map<PropertyInterface,SourceExpr>,SourceExpr>();
            SelectCacheJoins.put(Joins,CacheJoins);
        }
        
        Map<PropertyInterface,SourceExpr> InterfaceJoins = new HashMap<PropertyInterface,SourceExpr>();
        Iterator<T> i = Interfaces.iterator();
        while (i.hasNext()) {
            T Int = i.next();
            InterfaceJoins.put(Int,Int.JoinImplement);
        }

        SourceExpr JoinExpr = CacheJoins.get(InterfaceJoins);
        if(JoinExpr==null) {
            JoinExpr = ProceedJoinSelect(Joins);
            CacheJoins.put(InterfaceJoins,JoinExpr);
        }

        return JoinExpr;
    }

    abstract SourceExpr ProceedJoinSelect(JoinList Joins);
    
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
    
    // для оптимизации формул (реляционных св-в)
    boolean LinearIncrementChanges(ChangesSession Session) {
        return false;
    }
}

abstract class SourceProperty<T extends PropertyInterface> extends Property<T> {

    SourceProperty(TableFactory iTableFactory) {
        super();
        TableFactory = iTableFactory;
        SessionChanged = new HashMap();
    }
    TableFactory TableFactory;
    
    // для Increment'ного обновления (одновременно на что изменилось =,+)
    Map<ChangesSession,Integer> SessionChanged;
    
    boolean HasChanges(ChangesSession Session) {
        return SessionChanged.containsKey(Session);
    }
    
    Integer GetChangeType(ChangesSession Session) {
        return SessionChanged.get(Session);
    }

    // строится по сути "временный" Map PropertyInterface'ов на Objects'ы
    Map<PropertyInterface,KeyField> ChangeTableMap = null;
    
    ChangeTable GetChangeTable() {
        ChangeTable Table = TableFactory.GetChangeTable(Interfaces.size(),GetDBType());
        // если нету Map'a построим
        Iterator<T> i = Interfaces.iterator();
        if(ChangeTableMap==null) {
            ChangeTableMap = new HashMap();
            i = Interfaces.iterator();
            Iterator<KeyField> io = Table.Objects.iterator();
            while (i.hasNext()) ChangeTableMap.put(i.next(),io.next());
        }        
        return Table;
    }
    
    // получает таблицу и вычищает все из сессии
    ChangeTable StartChangeTable(DataAdapter Adapter,ChangesSession Session) throws SQLException {
        ChangeTable Table = GetChangeTable();
                
        SelectTable DropSession = new SelectTable(Table.Name);
        DropSession.Wheres.add(new FieldValueWhere(Session.ID,Table.Session.Name));
        DropSession.Wheres.add(new FieldValueWhere(ID,Table.Property.Name));
        Adapter.DeleteRecords(DropSession);
        
        return Table;
    }
    
    // получает UnionQuery с проставленными ключами
    UnionQuery GetChangeUnion(ChangeTable Table,ChangesSession Session) {
        // конечный результат, с ключами и выражением 
        UnionQuery ResultQuery = new UnionQuery();
        Iterator<T> im = Interfaces.iterator();
        while (im.hasNext()) 
            ResultQuery.Keys.add(ChangeTableMap.get(im.next()).Name);
        ResultQuery.Values.add(Table.Value.Name);
        ResultQuery.ValueKeys.put(Table.Session.Name,Session.ID);
        ResultQuery.ValueKeys.put(Table.Property.Name,ID);

        return ResultQuery;
    }
    
    // связывает именно измененные записи из сессии
    // Value - что получать, 0 - новые значения, 1 - +(увеличение), 2 - старые значения
    SourceExpr ChangedJoinSelect(JoinList Joins,ChangesSession Session,Integer Value) {
        ChangeTable Table = TableFactory.GetChangeTable(Interfaces.size(),GetDBType());
        Select SelectChanges = new SelectTable(Table.Name);
        SelectChanges.Wheres.add(new FieldWhere(new ValueSourceExpr(Session.ID),Table.Session.Name));
        SelectChanges.Wheres.add(new FieldWhere(new ValueSourceExpr(ID),Table.Property.Name));

        Iterator<PropertyInterface> i = (Iterator<PropertyInterface>) Interfaces.iterator();
        while(i.hasNext()) {
            PropertyInterface Interface = i.next();
            String FieldName = ChangeTableMap.get(Interface).Name;
            // сюда мы маппимся 
            if(Interface.JoinImplement==null)
                Interface.JoinImplement = new FieldSourceExpr(SelectChanges,FieldName);
            else 
                SelectChanges.Wheres.add(new FieldWhere(Interface.JoinImplement,FieldName));
        }
        Joins.add(SelectChanges);
        
        FieldSourceExpr NewValue = new FieldSourceExpr(SelectChanges,Table.Value.Name);
        Integer ChangedType = SessionChanged.get(Session);
        // теперь определимся что возвращать (если что надо то это и возвращаем)
        if(Value==ChangedType) 
            return NewValue;
        else {
            // нужно старое значение, LEFT JOIN'им его только надо как-то сделать 
            // для этого делаем отдельный JoinList (потому как не должен кэшироваться)
            JoinList LeftJoins = new JoinList();
            SourceExpr PrevValue = JoinSelect(LeftJoins);
            Iterator<Select> il = LeftJoins.iterator();
            while(il.hasNext()) {
                Select LeftSelect = il.next();
                LeftSelect.JoinType = "LEFT";
                Joins.add(LeftSelect);
            }

            if(Value==2)
                return PrevValue;
            else {
                FormulaSourceExpr Result = new FormulaSourceExpr("prm1"+(Value==0?"+":"-")+"ISNULL(prm2,0)");
                Result.Params.put("prm1",NewValue);
                Result.Params.put("prm2",PrevValue);

                return Result;
            }
        }       
    }

    void OutChangesTable(DataAdapter Adapter, ChangesSession Session) throws SQLException {
        ChangeTable Table = GetChangeTable();
        SelectQuery SelectChanges = new SelectQuery(new SelectTable(Table.Name));
        SelectChanges.From.Wheres.add(new FieldWhere(new ValueSourceExpr(Session.ID),Table.Session.Name));
        SelectChanges.From.Wheres.add(new FieldWhere(new ValueSourceExpr(ID),Table.Property.Name));
        
        Iterator<PropertyInterface> i = (Iterator<PropertyInterface>) Interfaces.iterator();
        while(i.hasNext()) {
            String Field = ChangeTableMap.get(i.next()).Name;
            SelectChanges.Expressions.put(Field,new FieldSourceExpr(SelectChanges.From,Field));
        }
        
        SelectChanges.Expressions.put(Table.Value.Name,new FieldSourceExpr(SelectChanges.From,Table.Value.Name));
        
        Adapter.OutSelect(SelectChanges);
    }
}

class DataPropertyInterface extends PropertyInterface {
    Class Class;
    
    DataPropertyInterface(Class iClass) {
        Class = iClass;
    }
}        


class DataProperty extends SourceProperty<DataPropertyInterface> {
    Class Value;
    Field Field;
    
    DataProperty(TableFactory iTableFactory,Class iValue) {
        super(iTableFactory);
        Value = iValue;
    }

    // при текущей реализации проше предполагать что не имплементнутые Interface имеют null Select !!!!!
    SourceExpr ProceedJoinSelect(JoinList Joins) {
        Map<KeyField,DataPropertyInterface> MapJoins = new HashMap<KeyField,DataPropertyInterface>();
        Table SourceTable = TableFactory.GetTable(Interfaces,MapJoins);

        // прогоним проверим все ли Implement'ировано
        Map<KeyField,SourceExpr> MapFields = new HashMap<KeyField,SourceExpr>();
        Iterator<KeyField> it = SourceTable.KeyFields.iterator();
        while(it.hasNext()) {
            KeyField TableField = it.next();
            MapFields.put(TableField,MapJoins.get(TableField).JoinImplement);
        }

        // поищем что такая таблица уже есть в запросе причем с такими же полями и такими же Join'ами
        Map<Map<KeyField,SourceExpr>,SelectTable> CacheTable = Joins.CacheTables.get(SourceTable);
        if(CacheTable==null) {
            CacheTable = new HashMap<Map<KeyField,SourceExpr>,SelectTable>();
            Joins.CacheTables.put(SourceTable,CacheTable);
        }
        
        SelectTable JoinTable = CacheTable.get(MapFields);
        if (JoinTable==null) {
            JoinTable = new SelectTable(SourceTable.Name);
            it = SourceTable.KeyFields.iterator();
            while (it.hasNext())
            {
                KeyField TableField = it.next();
                DataPropertyInterface DataInterface = MapJoins.get(TableField);
                if (DataInterface.JoinImplement==null) {
                    DataInterface.JoinImplement = new FieldSourceExpr(JoinTable,TableField.Name);
                    MapFields.put(TableField,DataInterface.JoinImplement);
                } else 
                    JoinTable.Wheres.add(new FieldWhere(DataInterface.JoinImplement,TableField.Name));
            }

            CacheTable.put(MapFields, JoinTable);
            Joins.add(JoinTable);
        }
        
        return new FieldSourceExpr(JoinTable,Field.Name);
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
        ChangeTable Table = GetChangeTable();
        Map<KeyField,Integer> InsertKeys = new HashMap();
        Iterator<DataPropertyInterface> i = Interfaces.iterator();
        while(i.hasNext()) {
            DataPropertyInterface Interface = i.next();
            InsertKeys.put(ChangeTableMap.get(Interface),Keys.get(Interface));
        }
        
        InsertKeys.put(Table.Property,ID);
        InsertKeys.put(Table.Session,Session.ID);

        Map<Field,Object> InsertValues = new HashMap();
        InsertValues.put(Table.Value,NewValue);

        Adapter.UpdateInsertRecord(Table,InsertKeys,InsertValues);

        // пометим изменение св-ва
        Session.Properties.add(this);
        SessionChanged.put(Session,0);
    }
    
    // заполняет список, возвращает есть ли изменения
    public boolean FillAggregateList(List<AggregateProperty> ChangedProperties,Set<DataProperty> ChangedSet) {
        return ChangedSet.contains(this);
    }
}

abstract class AggregateProperty<T extends PropertyInterface> extends SourceProperty<T> {
    
    AggregateProperty(TableFactory iTableFactory) {super(iTableFactory);}

    abstract void IncrementChanges(DataAdapter Adapter, ChangesSession Session) throws SQLException;
}

class PropertyMapImplement extends PropertyImplement<PropertyInterface> implements PropertyInterfaceImplement {
    
    PropertyMapImplement(Property iProperty) {super(iProperty);}

    public SourceExpr MapJoinSelect(JoinList Joins,ChangesSession Session,Integer Value) {
        
        // собираем null ссылки чтобы обновить свои JoinExprs
        Collection<PropertyInterface> NullInterfaces = new ArrayList<PropertyInterface>();
        // соберем интерфейс по всем нижним интерфейсам
        Iterator<PropertyInterface> i = Property.Interfaces.iterator();
        while (i.hasNext()) {
            PropertyInterface ImplementInterface = i.next();
            ImplementInterface.JoinImplement = Mapping.get(ImplementInterface).JoinImplement;
            if(ImplementInterface.JoinImplement==null) 
                NullInterfaces.add(ImplementInterface);
        }
        
        SourceExpr JoinSource = (Session!=null?((SourceProperty)Property).ChangedJoinSelect(Joins,Session,Value):Property.JoinSelect(Joins));
        
        // прогоним и проверим если кто-то изменил с null себе закинем JoinExprs
        Iterator<PropertyInterface> in = NullInterfaces.iterator();
        while (in.hasNext()) {
            PropertyInterface ImplementInterface = in.next();
            if(ImplementInterface.JoinImplement!=null) 
                Mapping.get(ImplementInterface).JoinImplement = ImplementInterface.JoinImplement;
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

    SourceExpr ProceedJoinSelect(JoinList Joins) {
        // для всех нижних делаем JoinSelect
        Iterator<PropertyInterface> im = Implements.Property.Interfaces.iterator();
        while (im.hasNext())
        {
            PropertyInterface ImplementInterface = im.next();
            ImplementInterface.JoinImplement = Implements.Mapping.get(ImplementInterface).MapJoinSelect(Joins,null,0);
        }

        return Implements.Property.JoinSelect(Joins);
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

    // инкрементные св-ва
    void IncrementChanges(DataAdapter Adapter, ChangesSession Session) throws SQLException {
        // алгоритм такой - для всех map св-в (в которых были изменения) строим подмножества изм. св-в
        // далее реализации этих св-в "замещаем" (то есть при JOIN будем подставлять), с остальными св-вами делаем LEFT JOIN на IS NULL 
        // и JOIN'им с основным св-вом делая туда FULL JOIN новых значений
        // или UNION или большой FULL JOIN в нужном порядке (и не делать LEFT JOIN на IS NULL) и там сделать большой NVL
        
        Iterator<PropertyInterface> im = Implements.Property.Interfaces.iterator();
        
        List<PropertyInterface> ChangedProperties = new ArrayList();
        while(im.hasNext()) {
            PropertyInterface Interface = im.next();
            // должен вернуть null если нету изменений (или просто транслирует интерфейс) иначе возвращает AggregateProperty
            if(Implements.Mapping.get(Interface).MapHasChanges(Session)) 
                ChangedProperties.add(Interface);
        }
        
        if(ChangedProperties.size()==0 && !Implements.Property.HasChanges(Session)) return;

        // удаляем старые 
        ChangeTable Table = StartChangeTable(Adapter,Session);
        // конечный результат, с ключами и выражением 
        UnionQuery ResultQuery = GetChangeUnion(Table,Session);

        Integer ResultValueType = null;
        Map<List<PropertyInterface>,Integer> SumCoeffs = null;
        ListIterator<List<PropertyInterface>> il = null;
        if(Implements.Property instanceof LinearFormulaProperty) {
            // если у нас основное LinearFormulaProperty (с коэффициентами), то пусть идет на + !!!! (всегда)
            //     как FULL JOIN (причем без SubSet'ов) и SumUnionQuery с коэффициентами из SumFormulaProperty (как в GroupProperty)
            ResultValueType = 1;
            SumCoeffs = new HashMap();
            List<List<PropertyInterface>> ChangedList = new ArrayList();
            im = ChangedProperties.iterator();
            while(im.hasNext()) {
                List<PropertyInterface> SingleList = new ArrayList();
                SingleList.add(im.next());
                ChangedList.add(SingleList);
                SumCoeffs.put(SingleList,((LinearFormulaPropertyInterface)SingleList.get(0)).Coeff);
            }
            il = ChangedList.listIterator();
        } else {
            // если ChangedProperties.size() - 0, то идет на то что было, иначе на = !!!! (всегда) (можно правда и на + давать по аналогии с GROUP, но это все равно медленнее работать будет)
            if(ChangedProperties.size()==0)
                ResultValueType = ((SourceProperty)Implements.Property).GetChangeType(Session);
            else
                ResultValueType = 0;
            
            // строим все подмножества св-в в лексикографическом порядке
            il = (new SetBuilder<PropertyInterface>()).BuildSubSetList(ChangedProperties).listIterator();
        }

        while(il.hasNext()) {
            List<PropertyInterface> ChangeProps = il.next();
            // будем докидывать FULL JOIN'ы в нужном порядке получая соотв. NVL
            // нужно за Join'ить со старыми значениями (исключить этот JOIN если пустое подмн-во !!! собсно в этом и заключается оптимизация инкрементности), затем с новыми (если она есть)
            for(int ij=(ChangeProps.size()==0?1:0);ij<(Implements.Property.HasChanges(Session)?2:1);ij++) {
                SelectQuery SubQuery = new SelectQuery(null);
                JoinList Joins = new JoinList();
                // скинем все JoinImplement'ы
                im = Interfaces.iterator();
                while (im.hasNext()) im.next().JoinImplement = null;

                im = Implements.Property.Interfaces.iterator();
                while (im.hasNext()) {
                    PropertyInterface ImplementInterface = im.next();
                    PropertyInterfaceImplement MapInterface = Implements.Mapping.get(ImplementInterface);
                    ImplementInterface.JoinImplement = MapInterface.MapJoinSelect(Joins,(ChangeProps.contains(ImplementInterface)?Session:null),0);
                }

                SourceExpr ValueExpr = (ij==0?Implements.Property.JoinSelect(Joins):((SourceProperty)Implements.Property).ChangedJoinSelect(Joins,Session,ResultValueType));
                SubQuery.Expressions.put(Table.Value.Name,ValueExpr);

                // закинем все ключи в запрос
                im = Interfaces.iterator();
                while (im.hasNext()) {
                    PropertyInterface Interface = im.next();
                    SubQuery.Expressions.put(ChangeTableMap.get(Interface).Name,Interface.JoinImplement);
                }

                // закинем Join'ы как обычно
                Iterator<Select> is = Joins.iterator();
                SubQuery.From = is.next();
                while(is.hasNext()) SubQuery.From.Joins.add(is.next());

                ResultQuery.Unions.add(SubQuery);
                if(SumCoeffs!=null) ResultQuery.SumCoeffs.put(SubQuery,SumCoeffs.get(ChangeProps));
            }
        }
        
        ResultQuery.ValueKeys.put(Table.Session.Name,Session.ID);
        ResultQuery.ValueKeys.put(Table.Property.Name,ID);

        Adapter.InsertSelect(Table,ResultQuery);
        
        // помечаем изменение в сессии
        SessionChanged.put(Session,ResultValueType);
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
            Changed = i.next().MapFillAggregateList(ChangedProperties,ChangedSet) || Changed;

        if(Changed)
            ChangedProperties.add(this);
        
        return Changed;
    }
}

class GroupPropertyInterface extends PropertyInterface {
    PropertyInterfaceImplement Implement;
    
    GroupPropertyInterface(PropertyInterfaceImplement iImplement) {Implement=iImplement;}
}

class GroupProperty extends AggregateProperty<GroupPropertyInterface> {
    // каждый интерфейс должен имплементировать именно GetInterface GroupProperty
    
    GroupProperty(TableFactory iTableFactory,SourceProperty iProperty) {
        super(iTableFactory);
        GroupProperty = iProperty;
        ToClasses = new HashMap<PropertyInterface,Class>();
    }
    
    // группировочное св-во собсно должно быть не формулой
    SourceProperty GroupProperty;
    
    // дополнительные условия на классы
    Map<PropertyInterface,Class> ToClasses;
    
    SourceExpr ProceedJoinSelect(JoinList Joins) {
        JoinList QueryJoins = new JoinList();
        // создадим сразу Select
        GroupQuery QuerySelect = new GroupQuery(null);

        // нужно создать пустые JoinImplement чтобы JoinSelect все заполнил в PropertyInterface GroupProperty
        Iterator<PropertyInterface> in = GroupProperty.Interfaces.iterator();
        while (in.hasNext()) in.next().JoinImplement = null;
        
        String GroupField = "grfield";
        QuerySelect.AggrExprs.put(GroupField,new GroupExpression(GroupProperty.JoinSelect(QueryJoins)));
        
        Integer KeyNum = 0;
        Iterator<GroupPropertyInterface> im = Interfaces.iterator();
        while (im.hasNext()) {
            GroupPropertyInterface ImplementInterface = im.next();
            SourceExpr JoinSource = ImplementInterface.Implement.MapJoinSelect(QueryJoins,null,0);
            
            KeyNum++;
            String KeyField = "key"+KeyNum.toString();
            QuerySelect.GroupBy.put(KeyField,JoinSource);
            // здесь нужно или Join'ить или свой FieldSourceExpr подставлять
            if(ImplementInterface.JoinImplement==null) {
                ImplementInterface.JoinImplement = new FieldSourceExpr(QuerySelect,KeyField);
            } else {
                QuerySelect.Wheres.add(new FieldWhere(ImplementInterface.JoinImplement,KeyField));
            }
        }

        // закидываем From'ы
        Iterator<Select> is = QueryJoins.iterator();
        QuerySelect.From = is.next();
        while (is.hasNext()) 
            QuerySelect.From.Joins.add(is.next());

        // для всех классов нужно еще докинуть Join и фильтры на класс
        Iterator<PropertyInterface> ic = ToClasses.keySet().iterator();
        while(ic.hasNext()) {
            PropertyInterface ClassInterface = ic.next();
            QuerySelect.From.Joins.add(TableFactory.ObjectTable.ClassJoinSelect(ToClasses.get(ClassInterface),ClassInterface.JoinImplement));
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

    void IncrementChanges(DataAdapter Adapter, ChangesSession Session) throws SQLException {
        // алгоритм пока такой :
        // 1. затем берем GROUPPROPERTY(изм на +) или по аналогии с реляционными или (JOIN cтарых св-в (не изм.) LEFT JOIN(измененных св-в) LEFT JOIN старых св-в (изм.))
        // + = SS 1
        // 2. для новых св-в делаем GROUPPROPERTY(все) так же как и для реляционных св-в FULL JOIN'ы - JOIN'ов с "перегр." подмн-вами (единственный способ сразу несколько изменений "засечь") (и GROUP BY по ISNULL справо налево ключей)
        // A = SS 1
        // 3. для старых св-в GROUPPROPERTY(все) FULL JOIN (JOIN "перегр." измененных с LEFT JOIN'ами старых) (без подмн-в) (и GROUP BY по ISNULL(обычных JOIN'ов,LEFT JOIN'a изм.))
        // A P (без SS) -1
        // все UNION ALL и GROUP BY или же каждый GROUP BY а затем FULL JOIN на +

        Iterator<PropertyInterface> in;
        Iterator<GroupPropertyInterface> im = Interfaces.iterator();
        
        List<GroupPropertyInterface> ChangedProperties = new ArrayList();
        while(im.hasNext()) {
            GroupPropertyInterface Interface = im.next();
            // должен вернуть null если нету изменений (или просто транслирует интерфейс) иначе возвращает AggregateProperty
            if(Interface.Implement.MapHasChanges(Session)) ChangedProperties.add(Interface);
        }
        
        // ничего не изменилось вываливаемся
        if(ChangedProperties.size()==0 && !GroupProperty.HasChanges(Session)) return;

        ChangeTable Table = StartChangeTable(Adapter,Session);
        // конечный результат, с ключами и выражением 
        UnionQuery ResultQuery = GetChangeUnion(Table,Session);

        for(int ij=(GroupProperty.HasChanges(Session)?1:2);ij<=(ChangedProperties.size()==0?1:3);ij++) {
            UnionQuery DataQuery = new UnionQuery();
            // заполняем ключи и значения
            int DataKeysNum = 1;
            Map<PropertyInterface,String> DataKeysMap = new HashMap();
            in = GroupProperty.Interfaces.iterator();
            while(in.hasNext()) {
                String Field = "dkey" + DataKeysNum++;
                DataKeysMap.put(in.next(), Field);
                DataQuery.Keys.add(Field);
            }
            im = Interfaces.iterator();
            while (im.hasNext()) DataQuery.Values.add(ChangeTableMap.get(im.next()).Name);
            DataQuery.Values.add(Table.Value.Name);
            
            boolean GroupChanged = (ij==1);
            Integer Coeff = 0;
            Integer GroupType = 0;
            ListIterator<List<GroupPropertyInterface>> il = null;
            // Subsets
            if(ij<=2) {
                il = (new SetBuilder<GroupPropertyInterface>()).BuildSubSetList(ChangedProperties).listIterator();
                GroupType = 0;
                Coeff = 1;
                // пустое при 2 не рассматриваем
                if(ij==2) il.next();
            }
            else {
                List<List<GroupPropertyInterface>> ChangedList = new ArrayList();
                im = ChangedProperties.iterator();
                while(im.hasNext()) {
                    List<GroupPropertyInterface> SingleList = new ArrayList();
                    SingleList.add(im.next());
                    ChangedList.add(SingleList);
                }
                il = ChangedList.listIterator();
                GroupType = 2;
                Coeff = -1;
            }
            
            while(il.hasNext()) {
                List<GroupPropertyInterface> ChangeProps = il.next();
                SelectQuery SubQuery = new SelectQuery(null);
                JoinList Joins = new JoinList();

                // обнуляем, закидываем GroupProperty,
                in = GroupProperty.Interfaces.iterator();
                while (in.hasNext()) in.next().JoinImplement = null;
                
                // значение
                SubQuery.Expressions.put(Table.Value.Name,(GroupChanged?GroupProperty.ChangedJoinSelect(Joins,Session,1):GroupProperty.JoinSelect(Joins)));

                // значения интерфейсов
                im = Interfaces.iterator();
                while (im.hasNext()) {
                    GroupPropertyInterface Interface = im.next();
                    SubQuery.Expressions.put(ChangeTableMap.get(Interface).Name,Interface.Implement.MapJoinSelect(Joins,(ChangeProps.contains(Interface)?Session:null),GroupType));
                }
                
                // значения ключей базовые
                in = GroupProperty.Interfaces.iterator();
                while (in.hasNext()) {
                    PropertyInterface Interface = in.next();
                    SubQuery.Expressions.put(DataKeysMap.get(Interface),Interface.JoinImplement);
                }

                // закинем Join'ы как обычно
                Iterator<Select> is = Joins.iterator();
                SubQuery.From = is.next();
                while(is.hasNext()) SubQuery.From.Joins.add(is.next());

                DataQuery.Unions.add(SubQuery);
            }
            
            GroupQuery GroupQuery = new GroupQuery(DataQuery);
            im = Interfaces.iterator();
            while (im.hasNext()) {
                String KeyField = ChangeTableMap.get(im.next()).Name;
                GroupQuery.GroupBy.put(KeyField,new FieldSourceExpr(DataQuery,KeyField));
            }
            GroupQuery.AggrExprs.put(Table.Value.Name,new GroupExpression(new FieldSourceExpr(DataQuery,Table.Value.Name)));

            ResultQuery.Unions.add(GroupQuery);
            ResultQuery.SumCoeffs.put(GroupQuery,Coeff);
        }

        Adapter.InsertSelect(Table,ResultQuery);
        // помечаем изменение в сессии
        SessionChanged.put(Session,1);
     }
}

// ФОРМУЛЫ

class FormulaPropertyInterface extends PropertyInterface {
    IntegralClass Class;
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
        if(Interface.Class.IsParent(ReqValue)) Result.add(ResultSet);
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
}

class StringFormulaProperty extends FormulaProperty<StringFormulaPropertyInterface> {

    String Formula;

    StringFormulaProperty(String iFormula) {
        super();
        Formula = iFormula;
    }
    
    SourceExpr ProceedJoinSelect(JoinList Joins) {
        FormulaSourceExpr Source = new FormulaSourceExpr(Formula);
                
        Iterator<StringFormulaPropertyInterface> it = Interfaces.iterator();
        while (it.hasNext()) {
            StringFormulaPropertyInterface Interface = it.next();
            Source.Params.put(Interface.Param,Interface.JoinImplement);
        }

        return Source;
    }
}

class LinearFormulaPropertyInterface extends FormulaPropertyInterface {
    Integer Coeff;
}

class LinearFormulaProperty extends FormulaProperty<LinearFormulaPropertyInterface> {

    SourceExpr ProceedJoinSelect(JoinList Joins) {

        FormulaSourceExpr Source = new FormulaSourceExpr("");
        int ParamNum=0;
        Iterator<LinearFormulaPropertyInterface> it = Interfaces.iterator();
        while (it.hasNext()) {
            LinearFormulaPropertyInterface Interface = it.next();
            String Param = "prm" + ParamNum++;
            Source.Formula = (Source.Formula.length()==0?"":Source.Formula+'+') + (Interface.Coeff==null?"":Interface.Coeff+"*") + Param;
            Source.Params.put(Param,Interface.JoinImplement);
        }

        return Source;
    }
}

class InterfaceClassSet extends ArrayList<InterfaceClass> {
    
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
}
    
class ChangesSession {
    
    ChangesSession(Integer iID) {
        ID = iID;
        Properties = new HashSet();
    }

    Integer ID;
    
    Set<DataProperty> Properties;
}