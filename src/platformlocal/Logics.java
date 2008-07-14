/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

class TableImplement extends ArrayList<DataPropertyInterface> {
    // заполняются пока автоматически
    Table Table;
    Map<DataPropertyInterface,KeyField> MapFields;

    TableImplement() {
        Childs = new HashSet<TableImplement>();
        Parents = new HashSet<TableImplement>();
    }
    
    // кэшированный граф
    Set<TableImplement> Childs;
    Set<TableImplement> Parents;

    boolean RecCompare(boolean ToParent,Collection<DataPropertyInterface> ToCompare,ListIterator<DataPropertyInterface> iRec,Map<DataPropertyInterface,DataPropertyInterface> MapTo) {
        if(!iRec.hasNext()) return true;

        DataPropertyInterface ProceedItem = iRec.next();
        Iterator<DataPropertyInterface> iToC = ToCompare.iterator();
        while(iToC.hasNext()) {
            DataPropertyInterface PairItem = iToC.next();
            if((ToParent && ProceedItem.Class.IsParent(PairItem.Class) || (!ToParent && PairItem.Class.IsParent(ProceedItem.Class)))) {
                if(!MapTo.containsKey(PairItem)) {
                // если parent - есть связь и нету ключа, гоним рекурсию дальше
                MapTo.put(PairItem, ProceedItem);
                // если нашли карту выходим
                if(RecCompare(ToParent,ToCompare,iRec,MapTo)) return true;
                MapTo.remove(PairItem);
                }
            }
        }

        iRec.previous();
        return false;
    }
    // 0 никак не связаны, 1 - параметр снизу в дереве, 2 - параметр сверху в дереве или равно
    // также возвращает карту если 2
    int Compare(Collection<DataPropertyInterface> ToCompare,Map<KeyField,DataPropertyInterface> MapTo) {
        
        if(ToCompare.size() != size()) return 0;

        // перебором и не будем страдать фигней
        // сначала что не 1 проверим
    
        HashMap<DataPropertyInterface,DataPropertyInterface> MapProceed = new HashMap<DataPropertyInterface,DataPropertyInterface>();
        
        ListIterator<DataPropertyInterface> iRec = (new ArrayList<DataPropertyInterface>(this)).listIterator();
        if(RecCompare(false,ToCompare,iRec,MapProceed)) {
            if(MapTo!=null) {
                MapTo.clear();
                Iterator<DataPropertyInterface> it = ToCompare.iterator();
                while(it.hasNext()) {
                    DataPropertyInterface DataInterface = it.next();
                    MapTo.put(MapFields.get(MapProceed.get(DataInterface)),DataInterface);
                }
            }
            
            return 2;
        }

        // MapProceed и так чистый и iRec также в начале
        if(RecCompare(true,ToCompare,iRec,MapProceed))
            return 1;
        
        // !!!! должна заполнять MapTo только если уже нашла
        return 0;
    }

    void RecIncludeIntoGraph(TableImplement IncludeItem,boolean ToAdd,Set<TableImplement> Checks) {
        
        if(Checks.contains(this)) return;
        Checks.add(this);
        
        Iterator<TableImplement> i = Parents.iterator();
        while(i.hasNext()) {
            TableImplement Item = i.next();
            Integer Relation = Item.Compare(IncludeItem,null);
            if(Relation==1) {
                // снизу в дереве
                // добавляем ее как промежуточную
                Item.Childs.add(IncludeItem);
                IncludeItem.Parents.add(Item);
                if(ToAdd) {
                    Item.Childs.remove(this);
                    i.remove();
                }
            } else {
                // сверху в дереве или никак не связаны
                // передаем дальше
                Item.RecIncludeIntoGraph(IncludeItem,Relation==2,Checks);
                if(Relation==2) ToAdd = false;
            }
        }
        
        // если снизу добавляем Childs
        if(ToAdd) {
            IncludeItem.Childs.add(this);
            Parents.add(IncludeItem);
        }
    }

    Table GetTable(Collection<DataPropertyInterface> FindItem,Map<KeyField,DataPropertyInterface> MapTo) {
        Iterator<TableImplement> i = Parents.iterator();
        while(i.hasNext()) {
            TableImplement Item = i.next();
            if(Item.Compare(FindItem,MapTo)==2) 
                return Item.GetTable(FindItem,MapTo);
        }
        
        return Table;
    }
    
    void FillSet(Set<TableImplement> TableImplements) {
        if(!TableImplements.add(this)) return;
        Iterator<TableImplement> i = Parents.iterator();
        while(i.hasNext()) i.next().FillSet(TableImplements);
    }

    void OutClasses() {
        Iterator<DataPropertyInterface> i = iterator();
        while(i.hasNext()) 
            System.out.print(i.next().Class.ID.toString()+" ");
    }
    void Out() {
        //выводим себя
        System.out.print("NODE - ");
        OutClasses();
        System.out.println("");
        
        Iterator<TableImplement> i = Childs.iterator();
        while(i.hasNext()) {
            System.out.print("childs - ");
            i.next().OutClasses();
            System.out.println();
        }

        i = Parents.iterator();
        while(i.hasNext()) {
            System.out.print("parents - ");
            i.next().OutClasses();
            System.out.println();
        }
        
        i = Parents.iterator();
        while(i.hasNext()) i.next().Out();
    }
}

// таблица в которой лежат объекты
class ObjectTable extends Table {
    
    KeyField Key;
    Field Class;
    
    ObjectTable() {
        super("objects");
        Key = new KeyField("object","integer");
        KeyFields.add(Key);
        Class = new Field("class","integer");
        PropFields.add(Class);
    };
    
    FromTable ClassSelect(Class ToSelect) {
        Collection<Integer> SetID = new HashSet<Integer>();
        ToSelect.FillSetID(SetID);
        
        FromTable ClassTable = new FromTable(Name);
        ClassTable.Wheres.add(new FieldSetValueWhere(SetID,Class.Name));
        
        return ClassTable;
    }
    
    FromTable ClassJoinSelect(Class ToSelect,SourceExpr JoinImplement) {
        FromTable JoinTable = ClassSelect(ToSelect);
        JoinTable.Wheres.add(new FieldWhere(JoinImplement,Key.Name));
        return JoinTable;
    }
    
    Integer GetClassID(DataAdapter Adapter,Integer idObject) throws SQLException {
        SelectQuery Query = new SelectQuery(new FromTable(Name));
        Query.From.Wheres.add(new FieldValueWhere(idObject,Key.Name));
        Query.Expressions.put("classid",new FieldSourceExpr(Query.From,Class.Name));
        return (Integer)Adapter.ExecuteSelect(Query).get(0).get("classid");
    }
  
}

// таблица счетчика ID
class IDTable extends Table {
    KeyField Key;
    
    IDTable() {
        super("idtable");
        Key = new KeyField("lastid","integer");
        KeyFields.add(Key);
    }
    
    Integer GenerateID(DataAdapter Adapter) throws SQLException { 
        FromTable From = new FromTable(Name);
        SelectQuery SelectID = new SelectQuery(From);
        // читаем
        SelectID.Expressions.put("lastid",new FieldSourceExpr(From,Key.Name));
        Integer FreeID = (Integer)Adapter.ExecuteSelect(SelectID).get(0).get("lastid");
        // замещаем
        SelectID.Expressions.put("lastid",new ValueSourceExpr(FreeID+1));
        Adapter.UpdateRecords(SelectID);
        return FreeID;
    }
}

// таблица куда виды складывают свои объекты
class ViewTable extends Table {
    ViewTable(Integer iObjects) {
        super("viewtable"+iObjects.toString());
        Objects = new ArrayList();
        for(Integer i=0;i<iObjects;i++) {
            KeyField ObjKeyField = new KeyField("object"+i,"integer");
            Objects.add(ObjKeyField);
            KeyFields.add(ObjKeyField);
        }
        
        View = new KeyField("viewid","integer");
        KeyFields.add(View);
    }
            
    List<KeyField> Objects;
    KeyField View;
    
    void DropViewID(DataAdapter Adapter,Integer ViewID) throws SQLException {
        FromTable Delete = new FromTable(Name);
        Delete.Wheres.add(new FieldValueWhere(ViewID,View.Name));
        Adapter.DeleteRecords(Delete);
    }
}

class ChangeTable extends Table {

    Collection<KeyField> Objects;
    KeyField Session;
    KeyField Property;
    Field Value;
    Field PrevValue;
    // системное поля, по сути для MaxGroupProperty
    Field SysValue;
    
    ChangeTable(Integer iObjects,Integer iDBType,List<String> DBTypes) {
        super("changetable"+iObjects+"t"+iDBType);

        Objects = new ArrayList();
        for(Integer i=0;i<iObjects;i++) {
            KeyField ObjKeyField = new KeyField("object"+i,"integer");
            Objects.add(ObjKeyField);
            KeyFields.add(ObjKeyField);
        }
        
        Session = new KeyField("session","integer");
        KeyFields.add(Session);
        
        Property = new KeyField("property","integer");
        KeyFields.add(Property);
        
        Value = new Field("value",DBTypes.get(iDBType));
        PropFields.add(Value);

        PrevValue = new Field("prevvalue",DBTypes.get(iDBType));
        PropFields.add(PrevValue);

        SysValue = new Field("sysvalue",DBTypes.get(iDBType));
        PropFields.add(SysValue);
    }
}

class TableFactory extends TableImplement{
    
    ObjectTable ObjectTable;
    IDTable IDTable;
    List<ViewTable> ViewTables;
    List<List<ChangeTable>> ChangeTables;
    
    // для отладки
    boolean ReCalculateAggr = false;
    
    ChangeTable GetChangeTable(Integer Objects, String DBType) {
        return ChangeTables.get(Objects-1).get(DBTypes.indexOf(DBType));
    }

    List<String> DBTypes;
    
    TableFactory() {
        ObjectTable = new ObjectTable();
        IDTable = new IDTable();
        ViewTables = new ArrayList();
        ChangeTables = new ArrayList();
        
        for(int i=1;i<5;i++)
            ViewTables.add(new ViewTable(i));

        DBTypes = new ArrayList();
        DBTypes.add("integer");
        DBTypes.add("char(50)");
        
        for(int i=1;i<5;i++) {
            List<ChangeTable> ObjChangeTables = new ArrayList();
            ChangeTables.add(ObjChangeTables);
            for(int j=0;j<DBTypes.size();j++)
                ObjChangeTables.add(new ChangeTable(i,j,DBTypes));
        }
    }

    void IncludeIntoGraph(TableImplement IncludeItem) {
        Set<TableImplement> Checks = new HashSet<TableImplement>();
        RecIncludeIntoGraph(IncludeItem,true,Checks);
    }
    
    void FillDB(DataAdapter Adapter) throws SQLException {
        Integer TableNum = 0;
        Set<TableImplement> TableImplements = new HashSet<TableImplement>();
        FillSet(TableImplements);
        
        Iterator<TableImplement> i = TableImplements.iterator();
        while(i.hasNext()) {
            TableImplement Node = i.next();
            TableNum++;
            Node.Table = new Table("table"+TableNum.toString());
            Node.MapFields = new HashMap<DataPropertyInterface,KeyField>();
            Integer FieldNum = 0;
            Iterator<DataPropertyInterface> it = Node.iterator();
            while(it.hasNext()) {
                FieldNum++;
                KeyField Field = new KeyField("key"+FieldNum.toString(),"integer");
                Node.Table.KeyFields.add(Field);
                Node.MapFields.put(it.next(),Field);
            }
        }
        
        Adapter.CreateTable(ObjectTable);
        Adapter.CreateTable(IDTable);
        Iterator<ViewTable> iv = ViewTables.iterator();
        while(iv.hasNext()) Adapter.CreateTable(iv.next());

        Iterator<List<ChangeTable>> ilc = ChangeTables.iterator();
        while(ilc.hasNext()) {
            Iterator<ChangeTable> ic = ilc.next().iterator();
            while(ic.hasNext()) Adapter.CreateTable(ic.next());
        }

        // закинем одну запись
        Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
        InsertKeys.put(IDTable.Key, 0);
        Adapter.InsertRecord(IDTable,InsertKeys,new HashMap<Field,Object>());
    }
}

class BusinessLogics {
    
    BusinessLogics() {
        BaseClass = new Class(0);
        TableFactory = new TableFactory();
        Properties = new ArrayList();
        PersistentProperties = new HashSet();
        
        BaseClass = new Class(0);
        
        StringClass = new StringClass(1);
        StringClass.AddParent(BaseClass);
        IntegerClass = new QuantityClass(2);
        IntegerClass.AddParent(BaseClass);
    }
    
    void AddDataProperty(DataProperty Property) {
        Properties.add(Property);
    }

    // получает класс по ID объекта
    Class GetClass(DataAdapter Adapter,Integer idObject) throws SQLException {
        // сначала получаем idClass
        return BaseClass.FindClassID(TableFactory.ObjectTable.GetClassID(Adapter,idObject));
    }

    Class BaseClass;
    Class StringClass;
    IntegralClass IntegerClass;
    
    TableFactory TableFactory;
    Collection<Property> Properties;
    
    Set<AggregateProperty> PersistentProperties;
    
        
    void UpdateAggregations(DataAdapter Adapter,Collection<AggregateProperty> Properties, ChangesSession Session) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)
        
        // нужно из графа зависимостей выделить направленный список аггрегированных св-в (здесь из предположения что список запрашиваемых аггрегаций меньше общего во много раз)
        List<AggregateProperty> UpdateList = new ArrayList();
        Iterator<AggregateProperty> i = Properties.iterator();
        while(i.hasNext()) i.next().FillAggregateList(UpdateList,Session.Properties);
        
        // здесь бежим слева направо определяем изм. InterfaceClassSet (в DataProperty они первичны) - удаляем сразу те у кого null (правда это может убить всю ветку)
        // потом реализуем

        // пробежим вперед пометим свойства которые изменились, но неясно на что
        ListIterator<AggregateProperty> il = UpdateList.listIterator();
        AggregateProperty Property = null;
        while(il.hasNext()) { 
            Property = il.next();
            Property.SessionChanged.put(Session,null);
        }
        // пробежим по которым надо поставим 0
        i = Properties.iterator();
        while(i.hasNext()) i.next().SetChangeType(Session,0);
        // прогоним DataProperty также им 0 поставим чтобы 1 не появлялись
        Iterator<DataProperty> id = Session.Properties.iterator();
        while(id.hasNext()) id.next().SetChangeType(Session,0);

        // бежим по списку (в обратном порядке) заполняем требования, 
        while(Property!=null) {
            Property.FillRequiredChanges(Session);

            if(il.hasPrevious())
                Property = il.previous();
            else
                Property = null;
        }
        
        // прогоним DataProperty предыдущие значения докинуть
        id = Session.Properties.iterator();
        while(id.hasNext()) id.next().UpdateIncrementChanges(Adapter,Session);
        
        // запускаем IncrementChanges для этого списка
        il = UpdateList.listIterator();
        while(il.hasNext()) 
            il.next().IncrementChanges(Adapter, Session);
        
        // дропнем изменения (пока, потом для FormBean'ов понадобится по другому)
        il = UpdateList.listIterator();
        while(il.hasNext())
            il.next().SessionChanged.remove(Session);
    }
    
    // сохраняет из Changes в базу
    void SaveProperties(DataAdapter Adapter,Collection<? extends ObjectProperty> Properties, ChangesSession Session) throws SQLException {
        Iterator<ObjectProperty> i = (Iterator<ObjectProperty>) Properties.iterator();
        while(i.hasNext()) i.next().SaveChanges(Adapter, Session);
    }
    
    boolean Apply(DataAdapter Adapter,ChangesSession Session) throws SQLException {
        // делается UpdateAggregations (для мн-ва persistent+constraints)
        UpdateAggregations(Adapter,PersistentProperties,Session);

        // записываем Data, затем PersistentProperties в таблицы из сессии
        SaveProperties(Adapter,PersistentProperties,Session);
        SaveProperties(Adapter,Session.Properties,Session);
        
        return true;
    }

    void FillDB(DataAdapter Adapter) throws SQLException {
        // инициализируем таблицы
        TableFactory.FillDB(Adapter);

        // запишем ID'ки
        int IDPropNum = 0;
        Iterator<Property> ip = Properties.iterator();
        while(ip.hasNext())
            ip.next().ID = IDPropNum++;
        
        Set<DataProperty> DataProperties = new HashSet();
        Collection<AggregateProperty> AggrProperties = new ArrayList();
        Map<Table,Integer> Tables = new HashMap<Table,Integer>();
        // закинем в таблицы(создав там все что надо) св-ва
        Iterator<Property> i = Properties.iterator();
        while(i.hasNext()) {
            Property Property = i.next();
            
            // ChangeTable'ы заполним
            if(Property instanceof ObjectProperty)
                ((ObjectProperty)Property).FillChangeTable();;

            if(Property instanceof DataProperty)
                DataProperties.add((DataProperty)Property);
            
            if(Property instanceof AggregateProperty)
                AggrProperties.add((AggregateProperty)Property);

            if(Property instanceof DataProperty || (Property instanceof AggregateProperty && PersistentProperties.contains(Property))) {
                Table Table = ((ObjectProperty)Property).GetTable(null);

                Integer PropNum = Tables.get(Table);
                if(PropNum==null) PropNum = 1;
                PropNum = PropNum + 1;
                Tables.put(Table, PropNum);

                Field PropField = new Field("prop"+PropNum.toString(),Property.GetDBType());
                Table.PropFields.add(PropField);
                ((ObjectProperty)Property).Field = PropField;
            }
        }

        Iterator<Table> it = Tables.keySet().iterator();
        while(it.hasNext()) Adapter.CreateTable(it.next());

        // построим в нужном порядке AggregateProperty и будем заполнять их
        List<AggregateProperty> UpdateList = new ArrayList();
        Iterator<AggregateProperty> ia = AggrProperties.iterator();
        while(ia.hasNext()) ia.next().FillAggregateList(UpdateList,DataProperties);
        ia = UpdateList.iterator();
        Integer ViewNum = 0;
        while(ia.hasNext()) {
            AggregateProperty Property = ia.next();
            if(Property instanceof GroupProperty)
                ((GroupProperty)Property).FillDB(Adapter,ViewNum++);
        }
        
        // создадим dumb
        Table DumbTable = new Table("dumb");
        DumbTable.KeyFields.add(new KeyField("dumb","integer"));
        Adapter.CreateTable(DumbTable);
        Adapter.Execute("INSERT INTO dumb (dumb) VALUES (1)");
    }
}
