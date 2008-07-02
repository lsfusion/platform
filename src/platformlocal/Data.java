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

class DataAdapter {
    
    Connection Connection;

    void Connect(String ConnectionString) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        java.lang.Class.forName("net.sourceforge.jtds.jdbc.Driver"); 
        Connection = DriverManager.getConnection("jdbc:jtds:sqlserver://mycomp:1433;namedPipe=true;User=sa;Password=");

        try {
            Execute("DROP DATABASE TestPlat");
        } catch(Exception e) {            
        }
        
        Execute("CREATE DATABASE TestPlat");
        Execute("USE TestPlat");
    }
    
    void CreateTable(Table Table) throws SQLException {
        String CreateString = "";
        Iterator<KeyField> ik = Table.KeyFields.iterator();
        while(ik.hasNext()) 
            CreateString = (CreateString.length()==0?"":CreateString+',') + ik.next().GetDeclare();
        Iterator<Field> it = Table.PropFields.iterator();
        while(it.hasNext()) 
            CreateString = CreateString+',' + it.next().GetDeclare();

        Execute("CREATE TABLE "+Table.Name+" ("+CreateString+")");
    }
    
    void Execute(String ExecuteString) throws SQLException {
        Statement Statement = Connection.createStatement();
        Statement.execute(ExecuteString);                
    }
    
    void InsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<Field,Object> PropFields) throws SQLException {
        
        String InsertString = "";
        String ValueString = "";
        
        // пробежим по KeyFields'ам
        Iterator<KeyField> ik = Table.KeyFields.iterator();
        while(ik.hasNext()) {
            KeyField Key = ik.next();
            InsertString = (InsertString.length()==0?"":InsertString+',') + Key.Name;
            ValueString = (ValueString.length()==0?"":ValueString+',') + KeyFields.get(Key);
        }
        
        // пробежим по Fields'ам
        Iterator<Field> i = PropFields.keySet().iterator();
        while(i.hasNext()) {
            Field Prop = i.next();
            Object Value = PropFields.get(Prop);
            InsertString = InsertString+","+Prop.Name;
            ValueString = ValueString+","+(Value instanceof String?"'"+(String)Value+"'":Value.toString());
        }

        Execute("INSERT INTO "+Table.Name+" ("+InsertString+") VALUES ("+ValueString+")");
    }

    void UpdateInsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<Field,Object> PropFields) throws SQLException {

        SelectTable From = new SelectTable(Table.Name);
        SelectQuery Select = new SelectQuery(From);
        // сначала закинем KeyField'ы и прогоним Select
        Iterator<KeyField> ik = Table.KeyFields.iterator();
        while(ik.hasNext())  {
            KeyField Key = ik.next();
            Select.From.Wheres.add(new FieldValueWhere(KeyFields.get(Key),Key.Name));
        }
        
        Select.Expressions.put("isrec",new ValueSourceExpr(1));

        if(ExecuteSelect(Select).size()>0) {
            // есть запись нужно Update лупить
            Select.Expressions.clear();
            Iterator<Field> ip = PropFields.keySet().iterator();
            while(ip.hasNext()) {
                Field Prop = ip.next();
                Object PropValue = PropFields.get(Prop);
                if(PropValue!=null) Select.Expressions.put(Prop.Name,new ValueSourceExpr(PropValue));
            }
            UpdateRecords(Select);
        } else
            // делаем Insert
            InsertRecord(Table,KeyFields,PropFields);
                 
    }

    void UpdateRecords(SelectQuery Select) throws SQLException {
        Execute(Select.GetUpdate());
    }
    
    void DeleteRecords(SelectTable Select) throws SQLException {
        Execute(Select.GetDelete());
    }
    
    void InsertSelect(Table InsertTo,Query Select) throws SQLException {
        StringBuilder InsertString = new StringBuilder();
        String SelectString = Select.GetSelect(InsertString);
//        System.out.println("INSERT INTO "+InsertTo.Name+" ("+InsertString+") "+SelectString);
        Execute("INSERT INTO "+InsertTo.Name+" ("+InsertString+") "+SelectString);
    }

    List<Map<String,Object>> ExecuteSelect(SelectQuery Select) throws SQLException {
        List<Map<String,Object>> ExecResult = new ArrayList<Map<String,Object>>();
        Statement Statement = Connection.createStatement();
        String SelectString = Select.GetSelect(new StringBuilder());
        try {
            ResultSet Result = Statement.executeQuery(SelectString);
            try {
                while(Result.next()) {
                    Iterator<String> is = Select.Expressions.keySet().iterator();
                    Map<String,Object> RowMap = new HashMap<String,Object>();
                    while(is.hasNext()) {
                        String SelectExpr = is.next();
                        RowMap.put(SelectExpr,Result.getObject(SelectExpr));
                    }
                    ExecResult.add(RowMap);
                }
            } finally {
               Result.close();
            }
        } finally {
            Statement.close();
        }
        
        return ExecResult;
    }
    
    void OutSelect(SelectQuery Select) throws SQLException {
        // выведем на экран
        System.out.println(Select.GetSelect(new StringBuilder()));
        
        List<Map<String,Object>> Result = ExecuteSelect(Select);
        ListIterator<Map<String,Object>> ir = Result.listIterator();
        while(ir.hasNext()) {
            Map<String,Object> RowMap = ir.next();
            Iterator<String> is = Select.Expressions.keySet().iterator();
            while(is.hasNext()) {
                System.out.print(RowMap.get(is.next()));
                System.out.print(" ");
            }
            System.out.println("");
        }
    }
    
    void Disconnect() throws SQLException {
        Connection.close();
    }
}

class Field {
    String Name;
    String Type;
    
    Field(String iName,String iType) {Name=iName;Type=iType;}
    
    String GetDeclare() {
        return Name + " " + Type;
    }
}

class KeyField extends Field {
    KeyField(String iName,String iType) {super(iName,iType);}
}
        
class Table {
    String Name;
    
    Table(String iName) {
        Name=iName;
        KeyFields = new ArrayList<KeyField>();
        PropFields = new ArrayList<Field>();
        MapFields = new HashMap<String,Field>();
    }
    
    Map<String,Field> MapFields;
    
    Collection<KeyField> KeyFields;
    Collection<Field> PropFields;
}

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
            if((ToParent && ProceedItem.Class.IsParent(PairItem.Class) || (!ToParent && PairItem.Class.IsParent(ProceedItem.Class))) && !MapTo.containsKey(PairItem)) {
                // если parent - есть связь и нету ключа, гоним рекурсию дальше
                MapTo.put(PairItem, ProceedItem);
                // если нашли карту выходим
                if(RecCompare(ToParent,ToCompare,iRec,MapTo)) return true;
                MapTo.remove(PairItem);
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
            if(Item.Compare(FindItem,MapTo)==2) return Item.GetTable(FindItem,MapTo);
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
    
    SelectTable ClassSelect(Class ToSelect) {
        Collection<Integer> SetID = new HashSet<Integer>();
        ToSelect.FillSetID(SetID);
        
        SelectTable ClassTable = new SelectTable(Name);
        ClassTable.Wheres.add(new FieldSetValueWhere(SetID,Class.Name));
        
        return ClassTable;
    }
    
    SelectTable ClassJoinSelect(Class ToSelect,SourceExpr JoinImplement) {
        SelectTable JoinTable = ClassSelect(ToSelect);
        JoinTable.Wheres.add(new FieldWhere(JoinImplement,Key.Name));
        return JoinTable;
    }
    
    Integer GetClassID(DataAdapter Adapter,Integer idObject) throws SQLException {
        SelectQuery Query = new SelectQuery(new SelectTable(Name));
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
        SelectTable From = new SelectTable(Name);
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
        SelectTable Delete = new SelectTable(Name);
        Delete.Wheres.add(new FieldValueWhere(ViewID,View.Name));
        Adapter.DeleteRecords(Delete);
    }
}

class ChangeTable extends Table {

    Collection<KeyField> Objects;
    KeyField Session;
    KeyField Property;
    Field Value;
    
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
    }
}

class TableFactory extends TableImplement{
    
    ObjectTable ObjectTable;
    IDTable IDTable;
    List<ViewTable> ViewTables;
    List<List<ChangeTable>> ChangeTables;
    
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
    Class IntegerClass;
    
    TableFactory TableFactory;
    Collection<Property> Properties;
    
    Set<AggregateProperty> PersistentProperties;
    
        
    void UpdateAggregations(DataAdapter Adapter,Collection<AggregateProperty> Properties, ChangesSession Session) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)
        
        // нужно из графа зависимостей выделить направленный список аггрегированных св-в для IncrementChanges 
        List<AggregateProperty> UpdateList = new ArrayList();
        Iterator<AggregateProperty> i = Properties.iterator();
        while(i.hasNext()) i.next().FillAggregateList(UpdateList,Session.Properties);

        // запускаем IncrementChanges для этого списка
        i = UpdateList.iterator();
        while(i.hasNext()) i.next().IncrementChanges(Adapter, Session);
    }
    
    // сохраняет из Changes в базу
    void SaveProperties(DataAdapter Adapter,Collection<? extends SourceProperty> Properties, ChangesSession Session) throws SQLException {
        Iterator<SourceProperty> i = (Iterator<SourceProperty>) Properties.iterator();
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
        
        Map<Table,Integer> Tables = new HashMap<Table,Integer>();
        // закинем в таблицы(создав там все что надо) св-ва
        Iterator<Property> i = Properties.iterator();
        while(i.hasNext()) {
            Property Property = i.next();
            if(Property instanceof DataProperty || (Property instanceof AggregateProperty && PersistentProperties.contains(Property))) {
                Table Table = ((SourceProperty)Property).GetTable(null);

                Integer PropNum = Tables.get(Table);
                if(PropNum==null) PropNum = 0;
                PropNum++;
                Tables.put(Table, PropNum+1);
            
                Field PropField = new Field("prop"+PropNum.toString(),Property.GetDBType());
                Table.PropFields.add(PropField);
                ((SourceProperty)Property).Field = PropField;
            }
        }
        
        Iterator<Table> it = Tables.keySet().iterator();
        while(it.hasNext()) Adapter.CreateTable(it.next());
        
        // создадим dumb
        Table DumbTable = new Table("dumb");
        DumbTable.KeyFields.add(new KeyField("dumb","integer"));
        Adapter.CreateTable(DumbTable);
        Adapter.Execute("INSERT INTO dumb (dumb) VALUES (1)");
    }
}
