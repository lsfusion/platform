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
import java.util.ListIterator;
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

    public SourceExpr MapJoinSelect(JoinList Joins,boolean Changed);
    public Class MapGetValueClass();
    public InterfaceClassSet MapGetClassSet(Class ReqValue);

    // для increment'ного обновления
    public boolean MapHasChanges();

}
        

class PropertyInterface implements PropertyInterfaceImplement {
    //можно использовать JoinExps потому как все равну вернуться она не может потому как иначе она зациклится
    SourceExpr JoinImplement;
    Class ValueClass;
    
    public SourceExpr MapJoinSelect(JoinList Joins,boolean Changed) {
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
    
    public boolean MapHasChanges() {
        return false;
    }
}


// само св-во также является имплементацией св-ва
class JoinList extends ArrayList<Select> {
    JoinList() {
        CacheTables = new HashMap<Table,Map<Map<KeyField,SourceExpr>,SelectTable>>();
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

    Property() {
        Interfaces = new ArrayList<T>();
        SelectCacheJoins = new HashMap<JoinList,Map<Map<PropertyInterface,SourceExpr>,SourceExpr>>();
    }
    
    // чтобы подчеркнуть что не направленный
    Collection<T> Interfaces;
    // кэшируем здесь а не в JoinList потому как быстрее
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
    
    // для Increment'ного обновления
    Table TableChanges;
    Map<PropertyInterface,KeyField> KeyChanges;
    Field ValueChanges;

    // аналогичная схема должна быть для постоянных аггрегаций !!!
    // связывает именно измененные записи
    SourceExpr ChangedJoinSelect(JoinList Joins) {
        Select SelectChanges = new SelectTable(TableChanges.Name);
        Iterator<PropertyInterface> i = (Iterator<PropertyInterface>) Interfaces.iterator();
        while(i.hasNext()) {
            PropertyInterface Interface = i.next();
            String FieldName = KeyChanges.get(Interface).Name;
            // сюда мы маппимся 
            if(Interface.JoinImplement==null)
                Interface.JoinImplement = new FieldSourceExpr(SelectChanges,FieldName);
            else 
                SelectChanges.Wheres.add(new FieldWhere(Interface.JoinImplement,FieldName));
        }
        Joins.add(SelectChanges);
        
        return (new FieldSourceExpr(SelectChanges,ValueChanges.Name));
    }
    
    boolean HasChanges() {
        return TableChanges!=null;
    }
}

abstract class SourceProperty<T extends PropertyInterface> extends Property<T> {

    SourceProperty(TableFactory iTableFactory) {
        super();
        TableFactory = iTableFactory;
    }
    TableFactory TableFactory;
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
}

abstract class AggregateProperty<T extends PropertyInterface> extends Property<T> {
    
}

class PropertyMapImplement extends PropertyImplement<PropertyInterface> implements PropertyInterfaceImplement {
    
    PropertyMapImplement(Property iProperty) {super(iProperty);}

    public SourceExpr MapJoinSelect(JoinList Joins,boolean Changed) {
        
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
        
        SourceExpr JoinSource = (Changed?Property.ChangedJoinSelect(Joins):Property.JoinSelect(Joins));
        
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
    
    public boolean MapHasChanges() {
        return Property.HasChanges();
    }
}

class RelationProperty extends SourceProperty<PropertyInterface> {
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
            ImplementInterface.JoinImplement = Implements.Mapping.get(ImplementInterface).MapJoinSelect(Joins,false);
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
    void IncrementChanges() {
        // алгоритм такой - для всех map св-в (в которых были изменения) строим подмножества изм. св-в
        // далее реализации этих св-в "замещаем" (то есть при JOIN будем подставлять), с остальными св-вами делаем LEFT JOIN на IS NULL 
        // и JOIN'им с основным св-вом делая туда FULL JOIN новых значений
        // или UNION или большой FULL JOIN в нужном порядке (и не делать LEFT JOIN на IS NULL) и там сделать большой NVL

        Iterator<PropertyInterface> im = Implements.Property.Interfaces.iterator();
        
        List<PropertyInterface> ChangedProperties = new ArrayList();
        while(im.hasNext()) {
            PropertyInterface Interface = im.next();
            // должен вернуть null если нету изменений (или просто транслирует интерфейс) иначе возвращает AggregateProperty
            if(Implements.Mapping.get(Interface).MapHasChanges()) 
                ChangedProperties.add(Interface);
        }
        
        // конечный результат, с ключами и выражением 
        SelectQuery ResultQuery = null;
        Map<PropertyInterface,SelectExpression> ResultKeys = null;
        SourceExpr ResultValue = null;

        // строим все подмножества св-в в лексикографическом порядке
        ListIterator<List<PropertyInterface>> il = (new SetBuilder<PropertyInterface>()).BuildSubSetList(ChangedProperties).listIterator();
        while(il.hasNext()) {
            List<PropertyInterface> ChangeProps = il.next();
            // будем докидывать FULL JOIN'ы в нужном порядке получая соотв. NVL
            // нужно за Join'ить со старыми значениями (исключить этот JOIN если пустое подмн-во !!! собсно в этом и заключается оптимизация инкрементности), затем с новыми (если она есть)
            for(int ij=(ChangeProps.size()==0?1:0);ij<(Implements.Property.HasChanges()?2:1);ij++) {
                SelectQuery SubQuery = new SelectQuery(null);

                JoinList Joins = new JoinList();

                // скинем все JoinImplement'ы
                im = Interfaces.iterator();
                while (im.hasNext()) im.next().JoinImplement = null;

                im = Implements.Property.Interfaces.iterator();
                while (im.hasNext()) {
                    PropertyInterface ImplementInterface = im.next();
                    PropertyInterfaceImplement MapInterface = Implements.Mapping.get(ImplementInterface);
                    if(ChangeProps.contains(ImplementInterface))
                        ImplementInterface.JoinImplement = MapInterface.MapJoinSelect(Joins,ChangeProps.contains(ImplementInterface));
                }

                if(ij==0)
                    Implements.Property.JoinSelect(Joins);
                else
                    Implements.Property.ChangedJoinSelect(Joins);

                // закинем все в запрос
                int Keys = 1;
                im = Interfaces.iterator();
                while (im.hasNext()) {
                    SubQuery.Expressions.add(new SelectExpression(im.next().JoinImplement,"key"+Keys));
                    // сразу же заJoin'им SubQuery если он не первый
                    if(ResultQuery!=null) {
                        SubQuery.Joins.add(SubQuery);
                    }
                }
            }
        }
    }

    public String GetDBType() {
        return Implements.Property.GetDBType();
    }
}
class GroupPropertyInterface extends PropertyInterface {
    PropertyInterfaceImplement Implement;
    
    GroupPropertyInterface(PropertyInterfaceImplement iImplement) {Implement=iImplement;}
}

class GroupProperty extends SourceProperty<GroupPropertyInterface> {
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
        List<SourceExpr> QueryInterfaceJoins = new ArrayList<SourceExpr>();
        Iterator<PropertyInterface> in = GroupProperty.Interfaces.iterator();
        while (in.hasNext())
            in.next().JoinImplement = null;
        
        String GroupField = "grfield";
        QuerySelect.AggrExprs.add(new GroupExpression(GroupProperty.JoinSelect(QueryJoins),GroupField));
        
        Integer KeyNum = 0;
        Iterator<GroupPropertyInterface> im = Interfaces.iterator();
        while (im.hasNext()) {
            GroupPropertyInterface ImplementInterface = im.next();
            SourceExpr JoinSource = ImplementInterface.Implement.MapJoinSelect(QueryJoins,false);
            
            KeyNum++;
            String KeyField = "key"+KeyNum.toString();
            QuerySelect.GroupBy.add(new SelectExpression(JoinSource,KeyField));
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
}
class FormulaPropertyInterface extends PropertyInterface {
    IntegralClass Class;
    String Param;
}


// вообще Collection 
class FormulaProperty extends Property<FormulaPropertyInterface> {
    String Formula;
    
    FormulaProperty(String iFormula) {
        super();
        Formula = iFormula;
    }
    
    SourceExpr ProceedJoinSelect(JoinList Joins) {
        FormulaSourceExpr Source = new FormulaSourceExpr(Formula);
                
        Iterator<FormulaPropertyInterface> it = Interfaces.iterator();
        while (it.hasNext()) {
            FormulaPropertyInterface Interface = it.next();
            Source.Params.put(Interface.Param,Interface.JoinImplement);
        }
        
        return Source;
    }

    public Class GetValueClass() {
        Iterator<FormulaPropertyInterface> i = Interfaces.iterator();
        FormulaPropertyInterface Interface = null;
        while (i.hasNext()) {
            Interface = i.next();
            if(!Interface.ValueClass.IsParent(Interface.Class)) return null;
        }
        
        return Interface.Class;
    }

    public InterfaceClassSet GetClassSet(Class ReqValue) {
        InterfaceClass ResultSet = new InterfaceClass();

        Iterator<FormulaPropertyInterface> i = Interfaces.iterator();
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
        Iterator<FormulaPropertyInterface> i = Interfaces.iterator();
        return i.next().Class.GetDBType();
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
    
    