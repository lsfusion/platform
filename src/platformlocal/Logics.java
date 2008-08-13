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
import java.util.Random;
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

    // Operation на что сравниваем
    // 0 - не ToParent
    // 1 - ToParent
    // 2 - равно
    
    boolean RecCompare(int Operation,Collection<DataPropertyInterface> ToCompare,ListIterator<DataPropertyInterface> iRec,Map<DataPropertyInterface,DataPropertyInterface> MapTo) {
        if(!iRec.hasNext()) return true;

        DataPropertyInterface ProceedItem = iRec.next();
        for(DataPropertyInterface PairItem : ToCompare) {
            if((Operation==1 && ProceedItem.Class.IsParent(PairItem.Class) || (Operation==0 && PairItem.Class.IsParent(ProceedItem.Class))) || (Operation==2 && PairItem.Class == ProceedItem.Class)) {
                if(!MapTo.containsKey(PairItem)) {
                    // если parent - есть связь и нету ключа, гоним рекурсию дальше
                    MapTo.put(PairItem, ProceedItem);
                    // если нашли карту выходим
                    if(RecCompare(Operation,ToCompare,iRec,MapTo)) return true;
                    MapTo.remove(PairItem);
                }
            }
        }

        iRec.previous();
        return false;
    }
    // 0 никак не связаны, 1 - параметр снизу в дереве, 2 - параметр сверху в дереве, 3 - равно
    // также возвращает карту если 2
    int Compare(Collection<DataPropertyInterface> ToCompare,Map<KeyField,DataPropertyInterface> MapTo) {
        
        if(ToCompare.size() != size()) return 0;

        // перебором и не будем страдать фигней
        // сначала что не 1 проверим
    
        HashMap<DataPropertyInterface,DataPropertyInterface> MapProceed = new HashMap();
        
        ListIterator<DataPropertyInterface> iRec = (new ArrayList<DataPropertyInterface>(this)).listIterator();
        int Relation = 0;
        if(RecCompare(2,ToCompare,iRec,MapProceed)) Relation = 3;
        if(Relation==0 && RecCompare(0,ToCompare,iRec,MapProceed)) Relation = 2;
        if(Relation>0) {
            if(MapTo!=null) {
                MapTo.clear();
                for(DataPropertyInterface DataInterface : ToCompare)
                    MapTo.put(MapFields.get(MapProceed.get(DataInterface)),DataInterface);
            }
            
            return Relation;
        }

        // MapProceed и так чистый и iRec также в начале
        if(RecCompare(1,ToCompare,iRec,MapProceed)) Relation = 1;
        
        // !!!! должна заполнять MapTo только если уже нашла
        return Relation;
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
                if(Relation!=3) Item.RecIncludeIntoGraph(IncludeItem,Relation==2,Checks);
                if(Relation==2 || Relation==3) ToAdd = false;
            }
        }
        
        // если снизу добавляем Childs
        if(ToAdd) {
            IncludeItem.Childs.add(this);
            Parents.add(IncludeItem);
        }
    }

    Table GetTable(Collection<DataPropertyInterface> FindItem,Map<KeyField,DataPropertyInterface> MapTo) {
        for(TableImplement Item : Parents) {
            int Relation = Item.Compare(FindItem,MapTo);
            if(Relation==2 || Relation==3)
                return Item.GetTable(FindItem,MapTo);
        }
        
        return Table;
    }
    
    void FillSet(Set<TableImplement> TableImplements) {
        if(!TableImplements.add(this)) return;
        for(TableImplement Parent : Parents) Parent.FillSet(TableImplements);
    }

    void OutClasses() {
        for(DataPropertyInterface Interface : this)
            System.out.print(Interface.Class.ID.toString()+" ");
    }
    void Out() {
        //выводим себя
        System.out.print("NODE - ");
        OutClasses();
        System.out.println("");
        
        for(TableImplement Child : Childs) {
            System.out.print("childs - ");
            Child.OutClasses();
            System.out.println();
        }

        for(TableImplement Parent : Parents) {
            System.out.print("parents - ");
            Parent.OutClasses();
            System.out.println();
        }
        
        for(TableImplement Parent : Parents) Parent.Out();
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
        if(idObject==null) return null;        
        SelectQuery Query = new SelectQuery(new FromTable(Name));
        Query.From.Wheres.add(new FieldValueWhere(idObject,Key.Name));
        Query.Expressions.put("classid",new FieldSourceExpr(Query.From,Class.Name));
        List<Map<String,Object>> Result = Adapter.ExecuteSelect(Query);
        if(Result.size()>0)
            return (Integer)Result.get(0).get("classid");
        else
            return null;        
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
        return FreeID+1;
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
    
    ChangeTable(String TablePrefix,Integer iObjects,Integer iDBType,List<String> DBTypes) {
        super(TablePrefix+"changetable"+iObjects+"t"+iDBType);

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

class DataChangeTable extends ChangeTable {

    DataChangeTable(Integer iObjects,Integer iDBType,List<String> DBTypes) {
        super("data",iObjects,iDBType,DBTypes);
    }
}

class IncrementChangeTable extends ChangeTable {

    Field PrevValue;
    // системное поля, по сути для MaxGroupProperty
    Field SysValue;
    
    IncrementChangeTable(Integer iObjects,Integer iDBType,List<String> DBTypes) {
        super("inc",iObjects,iDBType,DBTypes);

        PrevValue = new Field("prevvalue",DBTypes.get(iDBType));
        PropFields.add(PrevValue);

        SysValue = new Field("sysvalue",DBTypes.get(iDBType));
        PropFields.add(SysValue);
    }
}

// таблица изменений классов
// хранит добавляение\удаление классов
class ChangeClassTable extends Table {

    KeyField Session;
    KeyField Class;
    KeyField Object;

    ChangeClassTable(String iTable) {
        super(iTable);

        Session = new KeyField("session","integer");
        KeyFields.add(Session);
        
        Class = new KeyField("class","integer");
        KeyFields.add(Class);
        
        Object = new KeyField("object","integer");
        KeyFields.add(Object);
    }
    
    void ChangeClass(DataAdapter Adapter,ChangesSession ChangeSession,Integer idObject,Collection<Class> Classes) throws SQLException {

        for(Class Change : Classes) {
            Map<KeyField,Integer> InsertKeys = new HashMap();
            InsertKeys.put(Session,ChangeSession.ID);
            InsertKeys.put(Object,idObject);
            InsertKeys.put(Class,Change.ID);
            Adapter.InsertRecord(this,InsertKeys,new HashMap());
        }
    }
    
    void DropSession(DataAdapter Adapter,ChangesSession ChangeSession) throws SQLException {

        FromTable DropTable = new FromTable(Name);
        DropTable.Wheres.add(new FieldValueWhere(ChangeSession.ID,Session.Name));
        Adapter.DeleteRecords(DropTable);
    }
    
    FromTable ClassSelect(ChangesSession ChangeSession,Class ChangeClass) {
        FromTable JoinTable = new FromTable(Name);
        JoinTable.Wheres.add(new FieldValueWhere(ChangeSession.ID,Session.Name));
        JoinTable.Wheres.add(new FieldValueWhere(ChangeClass.ID,Class.Name));

        return JoinTable;
    }
}

class AddClassTable extends ChangeClassTable {

    AddClassTable() {
        super("addchange");
    }
}

class RemoveClassTable extends ChangeClassTable {

    RemoveClassTable() {
        super("removechange");
    }

    void ExcludeJoin(ChangesSession Session,JoinList Joins,Class Class,SourceExpr KeyExpr) {
        From RemoveSelect = ClassSelect(Session,Class);
        RemoveSelect.Wheres.add(new FieldWhere(KeyExpr,Object.Name));
        RemoveSelect.JoinType = "LEFT";
        Joins.add(RemoveSelect);
                        
        Joins.get(0).Wheres.add(new SourceIsNullWhere(new FieldSourceExpr(RemoveSelect,Object.Name),false));
    }

}

class TableFactory extends TableImplement{
    
    ObjectTable ObjectTable;
    IDTable IDTable;
    List<ViewTable> ViewTables;
    List<List<DataChangeTable>> DataChangeTables;
    List<List<IncrementChangeTable>> ChangeTables;
    
    AddClassTable AddClassTable;
    RemoveClassTable RemoveClassTable;
    
    // для отладки
    boolean ReCalculateAggr = false;
    boolean Crash = false;
    
    IncrementChangeTable GetChangeTable(Integer Objects, String DBType) {
        return ChangeTables.get(Objects-1).get(DBTypes.indexOf(DBType));
    }
    
    DataChangeTable GetDataChangeTable(Integer Objects, String DBType) {
        return DataChangeTables.get(Objects-1).get(DBTypes.indexOf(DBType));
    }

    List<String> DBTypes;

    int MaxBeanObjects = 3;
    int MaxInterface = 4;
    
    TableFactory() {
        ObjectTable = new ObjectTable();
        IDTable = new IDTable();
        ViewTables = new ArrayList();
        ChangeTables = new ArrayList();
        DataChangeTables = new ArrayList();
        
        AddClassTable = new AddClassTable();
        RemoveClassTable = new RemoveClassTable();
        
        for(int i=1;i<=MaxBeanObjects;i++)
            ViewTables.add(new ViewTable(i));

        DBTypes = new ArrayList();
        DBTypes.add("integer");
        DBTypes.add("char(50)");
        
        for(int i=1;i<=MaxInterface;i++) {
            List<IncrementChangeTable> ObjChangeTables = new ArrayList();
            ChangeTables.add(ObjChangeTables);
            for(int j=0;j<DBTypes.size();j++)
                ObjChangeTables.add(new IncrementChangeTable(i,j,DBTypes));
        }

        for(int i=1;i<=MaxInterface;i++) {
            List<DataChangeTable> ObjChangeTables = new ArrayList();
            DataChangeTables.add(ObjChangeTables);
            for(int j=0;j<DBTypes.size();j++)
                ObjChangeTables.add(new DataChangeTable(i,j,DBTypes));
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
        
        for(TableImplement Node : TableImplements) {
            TableNum++;
            Node.Table = new Table("table"+TableNum.toString());
            Node.MapFields = new HashMap<DataPropertyInterface,KeyField>();
            Integer FieldNum = 0;
            for(DataPropertyInterface Interface : Node) {
                FieldNum++;
                KeyField Field = new KeyField("key"+FieldNum.toString(),"integer");
                Node.Table.KeyFields.add(Field);
                Node.MapFields.put(Interface,Field);
            }
        }

        Adapter.CreateTable(AddClassTable);
        Adapter.CreateTable(RemoveClassTable);

        Adapter.CreateTable(ObjectTable);
        Adapter.CreateTable(IDTable);
        for(ViewTable ViewTable : ViewTables) Adapter.CreateTable(ViewTable);

        for(List<IncrementChangeTable> ListTables : ChangeTables)
            for(ChangeTable ChangeTable : ListTables) Adapter.CreateTable(ChangeTable);
        for(List<DataChangeTable> ListTables : DataChangeTables)
            for(ChangeTable DataChangeTable : ListTables) Adapter.CreateTable(DataChangeTable);

        // закинем одну запись
        Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
        InsertKeys.put(IDTable.Key, 0);
        Adapter.InsertRecord(IDTable,InsertKeys,new HashMap<Field,Object>());
    }
}

abstract class BusinessLogics<T extends BusinessLogics<T>> {
    
    BusinessLogics() {
        TableFactory = new TableFactory();
        Properties = new ArrayList();
        Persistents = new HashSet();
        Constraints = new HashMap();
        
        BaseClass = new ObjectClass(0, "Базовый класс");
        
        StringClass = new StringClass(1, "Строка");
//        StringClass.AddParent(BaseClass);
        IntegerClass = new QuantityClass(2, "Число");
        IntegerClass.AddParent(BaseClass);
        
        for(int i=0;i<TableFactory.MaxInterface;i++) {
            TableImplement Include = new TableImplement();
            for(int j=0;j<=i;j++)
                Include.add(new DataPropertyInterface(BaseClass));
            TableFactory.IncludeIntoGraph(Include);
        }         
        
        BaseGroup = new NavigatorGroup<T>(0, "Base Group");
        
        InitLogics();
        InitImplements();
        InitNavigators();
    }
    
    abstract void InitClasses();
    abstract void InitProperties();
    abstract void InitConstraints();
    
    // инициализируется логика
    void InitLogics() {
        InitClasses();
        InitProperties();
        InitConstraints();
    }
    
    abstract void InitPersistents();
    abstract void InitTables();
    abstract void InitIndexes();
    
    void InitImplements() {
        InitPersistents();
        InitTables();
        InitIndexes();
    }

    NavigatorGroup<T> BaseGroup;
    
    abstract void InitNavigators();
    
    void AddDataProperty(DataProperty Property) {
        Properties.add(Property);
    }

    // получает класс по ID объекта
    Class GetClass(ChangesSession Session,DataAdapter Adapter,Integer idObject) throws SQLException {
        // сначала получаем idClass
        if(Session!=null && Session.NewClasses.containsKey(idObject)) {
            List<Class> ChangeClasses = Session.NewClasses.get(idObject);
            return ChangeClasses.get(ChangeClasses.size()-1);
        }
        return BaseClass.FindClassID(TableFactory.ObjectTable.GetClassID(Adapter,idObject));
    }
    
    Integer AddObject(ChangesSession Session,DataAdapter Adapter,Class Class) throws SQLException {

        Integer FreeID = TableFactory.IDTable.GenerateID(Adapter);

        ChangeClass(Session,Adapter,FreeID,Class);
        
        return FreeID;
    }
    
    void ChangeClass(ChangesSession Session,DataAdapter Adapter,Integer idObject,Class Class) throws SQLException {
        
        // запишем объекты, которые надо будет сохранять
        Session.ChangeClass(idObject,Class);
    }
    
    void UpdateClassChanges(ChangesSession Session,DataAdapter Adapter) throws SQLException {

        // дропнем старую сессию
        TableFactory.AddClassTable.DropSession(Adapter,Session);
        TableFactory.RemoveClassTable.DropSession(Adapter,Session);
        Session.AddClasses.clear();
        Session.RemoveClasses.clear();
        
        for(Integer idObject : Session.NewClasses.keySet()) {
            // дальше нужно построить "разницу" какие классы ушли и записать в соответствующую таблицу
            
            Set<Class> TempRemoveClasses = new HashSet();
            
            ListIterator<Class> ic = Session.NewClasses.get(idObject).listIterator();
            Class NewClass = ic.next();
            if(NewClass==null) NewClass = BaseClass;
            while(ic.hasNext()) {
                Class NextClass = ic.next();
                if(NextClass==null) NextClass = BaseClass;
                NextClass.GetDiffSet(NewClass,null,TempRemoveClasses);
                NewClass = NextClass;
            }

            // узнаем старый класс
            Collection<Class> AddClasses = new ArrayList();
            Set<Class> RemoveClasses = new HashSet();
            NewClass.GetDiffSet(GetClass(null,Adapter,idObject),AddClasses,RemoveClasses);
            Session.AddClasses.addAll(AddClasses);
            
            // вырежем все из AddClasses
            TempRemoveClasses.removeAll(AddClasses);
            RemoveClasses.addAll(TempRemoveClasses);
            Session.RemoveClasses.addAll(RemoveClasses);

            // собсно в сессию надо записать все изм. объекты
            TableFactory.AddClassTable.ChangeClass(Adapter,Session,idObject,AddClasses);
            TableFactory.RemoveClassTable.ChangeClass(Adapter,Session,idObject,RemoveClasses);
        }
    }
    
    void SaveClassChanges(ChangesSession Session,DataAdapter Adapter) throws SQLException {
    
        for(Integer idObject : Session.NewClasses.keySet()) {
            Map<KeyField,Integer> InsertKeys = new HashMap();
            InsertKeys.put(TableFactory.ObjectTable.Key, idObject);
            Map<Field,Object> InsertProps = new HashMap();
            List<Class> ChangeClasses = Session.NewClasses.get(idObject);
            Class ChangeClass = ChangeClasses.get(ChangeClasses.size()-1);
            Object Value = null;
            if(ChangeClass!=null) Value = ChangeClass.ID;
            InsertProps.put(TableFactory.ObjectTable.Class,Value);
        
            Adapter.UpdateInsertRecord(TableFactory.ObjectTable,InsertKeys,InsertProps);
        }        
    }
    
    // счетчик сессий (пока так потом надо из базы или как-то по другому транзакционность сделать
    int SessionCounter = 0;
    ChangesSession CreateSession() {
        return new ChangesSession(SessionCounter++);
    }
    void DropSession(DataAdapter Adapter, ChangesSession Session) throws SQLException {
        // дропает сессию 
        // вычищает из всех св-в ссылки на нее появившиеся в процессе UpdateAggregations
        for(Property Property : Properties)
            if(Property instanceof ObjectProperty)
                ((ObjectProperty)Property).SessionChanged.remove(Session);

        // удаляет из всех таблиц свои данные        
        // AddClassTable, RemoveClassTable, ChangeTables
        FromTable Drop;
        Drop = new FromTable(TableFactory.AddClassTable.Name);
        Drop.Wheres.add(new FieldValueWhere(Session.ID,TableFactory.AddClassTable.Session.Name));
        Adapter.DeleteRecords(Drop);
        
        Drop = new FromTable(TableFactory.RemoveClassTable.Name);
        Drop.Wheres.add(new FieldValueWhere(Session.ID,TableFactory.RemoveClassTable.Session.Name));
        Adapter.DeleteRecords(Drop);

        for(List<IncrementChangeTable> TypeTables : TableFactory.ChangeTables)
            for(ChangeTable Table : TypeTables) {
                Drop = new FromTable(Table.Name);
                Drop.Wheres.add(new FieldValueWhere(Session.ID,Table.Session.Name));
                Adapter.DeleteRecords(Drop);
            }
        for(List<DataChangeTable> TypeTables : TableFactory.DataChangeTables)
            for(ChangeTable Table : TypeTables) {
                Drop = new FromTable(Table.Name);
                Drop.Wheres.add(new FieldValueWhere(Session.ID,Table.Session.Name));
                Adapter.DeleteRecords(Drop);
            }
    }

    Class BaseClass;
    Class StringClass;
    IntegralClass IntegerClass;
    
    TableFactory TableFactory;
    Collection<Property> Properties;
    
    Set<AggregateProperty> Persistents;
    Map<ObjectProperty,Constraint> Constraints;
    
        
    List<ObjectProperty> UpdateAggregations(DataAdapter Adapter,Collection<ObjectProperty> ToUpdateProperties, ChangesSession Session) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)

        // update'им изменения классов
        UpdateClassChanges(Session,Adapter);

        // нужно из графа зависимостей выделить направленный список object св-в (здесь из предположения что список запрашиваемых аггрегаций меньше общего во много раз)
        // все необходимые AggregateProperty - Apply (Persistents+Constraints), Bean (AggregateProperty в бине)
        // все необходимые DataProperty - Apply (все DataProperty), а для Bean'а (DataProperty в бине)
        // DataProperty (при изм. - новые зн.) ObjectProperty   (только в бине, в Apply не актуально - не факт при расчете себестоимости может иметь смысл)
        // DataProperty (при AddClasses - новые(если были)+ст.зн.) ObjectProperty
        // DataProperty (при RemoveClasses)
        List<ObjectProperty> UpdateList = new ArrayList();
        for(ObjectProperty Property : ToUpdateProperties) Property.FillChangedList(UpdateList,Session);

        // здесь бежим слева направо определяем изм. InterfaceClassSet (в DataProperty они первичны) - удаляем сразу те у кого null (правда это может убить всю ветку)
        // потом реализуем

        // пробежим вперед пометим свойства которые изменились, но неясно на что
        ListIterator<ObjectProperty> il = UpdateList.listIterator();
        ObjectProperty Property = null;
        while(il.hasNext()) {
            Property = il.next();
            Property.SessionChanged.put(Session,null);
        }
        // пробежим по которым надо поставим 0
        for(ObjectProperty UpdateProperty : ToUpdateProperties) UpdateProperty.SetChangeType(Session,0);

        // бежим по списку (в обратном порядке) заполняем требования, 
        while(Property!=null) {
            Property.FillRequiredChanges(Session);

            if(il.hasPrevious())
                Property = il.previous();
            else
                Property = null;
        }
        
        // запускаем IncrementChanges для этого списка
        for(ObjectProperty UpdateProperty : UpdateList) UpdateProperty.IncrementChanges(Adapter,Session);
        
        return UpdateList;
    }
    
    // проверяет Constraints
    boolean CheckConstraints(DataAdapter Adapter,ChangesSession Session) throws SQLException {

        for(ObjectProperty Property : Constraints.keySet())
            if(Property.HasChanges(Session) && !Constraints.get(Property).Check(Adapter,Session,Property))
                return false;

        return true;
    }
    
    boolean Apply(DataAdapter Adapter,ChangesSession Session) throws SQLException {
        // делается UpdateAggregations (для мн-ва persistent+constraints)
        Adapter.Execute("BEGIN TRANSACTION");
        
        // создадим общий список из Persistents + Constraints с AggregateProperty's + DataProperties
        Collection<ObjectProperty> UpdateList = new HashSet(Persistents);
        UpdateList.addAll(Constraints.keySet());
        for(Property Property : Properties)
            if(Property instanceof DataProperty) UpdateList.add((DataProperty)Property);

        List<ObjectProperty> ChangedList = UpdateAggregations(Adapter,UpdateList,Session);

        // проверим Constraints
        if(!CheckConstraints(Adapter,Session)) {
            Adapter.Execute("ROLLBACK");
            return false;
        }            
        
        // записываем Data, затем Persistents в таблицы из сессии
        SaveClassChanges(Session,Adapter);
        
        // сохранить св-ва которые Persistent, те что входят в Persistents и DataProperty
        for(ObjectProperty Property : ChangedList)
            if(Property instanceof DataProperty || Persistents.contains(Property))
                Property.SaveChanges(Adapter,Session);
        
        Adapter.Execute("COMMIT TRANSACTION");
        
        return true;        
    }

    void FillDB(DataAdapter Adapter) throws SQLException {
        // инициализируем таблицы
        TableFactory.FillDB(Adapter);

        // запишем ID'ки
        int IDPropNum = 0;
        for(Property Property : Properties)
            Property.ID = IDPropNum++;
        
        Set<DataProperty> DataProperties = new HashSet();
        Collection<AggregateProperty> AggrProperties = new ArrayList();
        Map<Table,Integer> Tables = new HashMap<Table,Integer>();
        // закинем в таблицы(создав там все что надо) св-ва
        for(Property Property : Properties) {
            // ChangeTable'ы заполним
            if(Property instanceof ObjectProperty)
                ((ObjectProperty)Property).FillChangeTable();

            if(Property instanceof DataProperty) {
                DataProperties.add((DataProperty)Property);
                ((DataProperty)Property).FillDataTable();
            }
            
            if(Property instanceof AggregateProperty)
                AggrProperties.add((AggregateProperty)Property);

            if(Property instanceof DataProperty || (Property instanceof AggregateProperty && Persistents.contains(Property))) {
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

        for(Table Table : Tables.keySet()) Adapter.CreateTable(Table);

        // построим в нужном порядке AggregateProperty и будем заполнять их
        List<AggregateProperty> UpdateList = new ArrayList();
        for(AggregateProperty Property : AggrProperties) Property.FillChangedList(UpdateList,null);
        Integer ViewNum = 0;
        for(AggregateProperty Property : UpdateList) {
            if(Property instanceof GroupProperty)
                ((GroupProperty)Property).FillDB(Adapter,ViewNum++);
        }
        
        // создадим dumb
        Table DumbTable = new Table("dumb");
        DumbTable.KeyFields.add(new KeyField("dumb","integer"));
        Adapter.CreateTable(DumbTable);
        Adapter.Execute("INSERT INTO dumb (dumb) VALUES (1)");
    }
    
    boolean CheckPersistent(DataAdapter Adapter) throws SQLException {
        for(AggregateProperty Property : Persistents) {
            if(!Property.CheckAggregation(Adapter,Property.OutName))
                return false;
//            Property.Out(Adapter);
        }
        
        return true;
    }

    Map<String,PropertyObjectImplement> FillSingleViews(ObjectImplement Object,RemoteForm Form,Set<String> Names) {
        
        Map<String,PropertyObjectImplement> Result = new HashMap();
        
        for(Property DrawProp : Properties) {
            if(DrawProp.Interfaces.size() == 1 && DrawProp instanceof ObjectProperty) {
                // проверим что дает хоть одно значение
                InterfaceClass InterfaceClass = new InterfaceClass();
                InterfaceClass.put(((Collection<PropertyInterface>)DrawProp.Interfaces).iterator().next(),Object.BaseClass);
                if(DrawProp.GetValueClass(InterfaceClass)!=null) {
                    PropertyObjectImplement PropertyImplement = new PropertyObjectImplement((ObjectProperty)DrawProp);
                    PropertyImplement.Mapping.put((PropertyInterface)DrawProp.Interfaces.iterator().next(),Object);
                    Form.Properties.add(new PropertyView(Form.IDShift(1),PropertyImplement,Object.GroupTo));
                    
                    if(Names!=null && Names.contains(DrawProp.OutName))
                        Result.put(DrawProp.OutName,PropertyImplement);
                }
            }
        }
        
        return Result;
    }
    
    // функционал по заполнению св-в по номерам, нужен для BL
    
    LDP AddDProp(Class Value,Class ...Params) {
        DataProperty Property = new DataProperty(TableFactory,Value);
        LDP ListProperty = new LDP(Property);
        for(Class Int : Params) {
            ListProperty.AddInterface(Int);
        }
        AddDataProperty(Property);
        return ListProperty;
    }
    
    void SetDefProp(LDP Data,LP Default,boolean OnChange) {
        DataProperty Property = ((DataProperty)Data.Property);
        Property.DefaultProperty = (ObjectProperty)Default.Property;
        for(int i=0;i<Data.ListInterfaces.size();i++)
            Property.DefaultMap.put((DataPropertyInterface)Data.ListInterfaces.get(i),Default.ListInterfaces.get(i));
        
        Property.OnDefaultChange = OnChange;
    }

    LDP AddVProp(Object Value,Class ValueClass,Class ...Params) {
        ValueProperty Property = new ValueProperty(TableFactory,ValueClass,Value);
        LDP ListProperty = new LDP(Property);
        for(Class Int : Params) {
            ListProperty.AddInterface(Int);
        }
        Properties.add(Property);
        return ListProperty;
    }

    LSFP AddSFProp(String Formula,boolean Filter,Integer Params) {

        StringFormulaProperty Property = null;
        if(Filter)
            Property = new FilterFormulaProperty(Formula);
        else
            Property = new StringFormulaProperty(Formula);
        LSFP ListProperty = new LSFP(Property,IntegerClass,Params);
        Properties.add(Property);
        return ListProperty;
    }

    LMFP AddMFProp(Integer Params) {
        MultiplyFormulaProperty Property = new MultiplyFormulaProperty();
        LMFP ListProperty = new LMFP(Property,IntegerClass,Params);
        Properties.add(Property);
        return ListProperty;
    }

    
    List<PropertyInterfaceImplement> ReadPropImpl(LP MainProp,Object ...Params) {
        List<PropertyInterfaceImplement> Result = new ArrayList<PropertyInterfaceImplement>();
        int WaitInterfaces = 0, MainInt = 0;
        PropertyMapImplement MapRead = null;
        LP PropRead = null;
        for(Object P : Params) {
            if(P instanceof Integer) {
                // число может быть как ссылкой на родной интерфейс так и 
                PropertyInterface PropInt = MainProp.ListInterfaces.get((Integer)P-1);
                if(WaitInterfaces==0) {
                    // родную берем 
                    Result.add(PropInt);
                } else {
                    // докидываем в маппинг
                    MapRead.Mapping.put(PropRead.ListInterfaces.get(PropRead.ListInterfaces.size()-WaitInterfaces), PropInt);
                    WaitInterfaces--;
                }
            } else {
               // имплементация, типа LP
               PropRead = (LP)P;
               MapRead = new PropertyMapImplement((ObjectProperty)PropRead.Property);
               WaitInterfaces = PropRead.ListInterfaces.size();
               Result.add(MapRead);
            }
        }
        
        return Result;
    }

    LRP AddRProp(LP MainProp, int IntNum, Object ...Params) {
        RelationProperty Property = new RelationProperty(TableFactory,MainProp.Property);
        LRP ListProperty = new LRP(Property,IntNum);
        int MainInt = 0;
        List<PropertyInterfaceImplement> PropImpl = ReadPropImpl(ListProperty,Params);
        for(PropertyInterfaceImplement Implement : PropImpl) {
            Property.Implements.Mapping.put(MainProp.ListInterfaces.get(MainInt),Implement);
            MainInt++;
        }
        Properties.add(Property);

        return ListProperty;
    }
    
    LGP AddGProp(LP GroupProp,boolean Sum,Object ...Params) {
        GroupProperty Property = null;
        if(Sum)
            Property = new SumGroupProperty(TableFactory,(ObjectProperty)GroupProp.Property);
        else
            Property = new MaxGroupProperty(TableFactory,(ObjectProperty)GroupProp.Property);
        LGP ListProperty = new LGP(Property,GroupProp);
        List<PropertyInterfaceImplement> PropImpl = ReadPropImpl(GroupProp,Params);
        for(PropertyInterfaceImplement Implement : PropImpl) ListProperty.AddInterface(Implement);
        Properties.add(Property);
        
        return ListProperty;
    }

    LRP AddLProp(int ListType, int IntNum, Object ...Params) {
        ListProperty Property = null;
        switch(ListType) {
            case 0:
                Property = new MaxListProperty(TableFactory);
                break;
            case 1:
                Property = new SumListProperty(TableFactory);
                break;
            case 2:
                Property = new OverrideListProperty(TableFactory);
                break;
        }
        
        LRP ListProperty = new LRP(Property,IntNum);

        for(int i=0;i<Params.length/(IntNum+2);i++) {
            Integer Offs = i*(IntNum+2);
            LP OpImplement = (LP)Params[Offs+1];
            PropertyMapImplement Operand = new PropertyMapImplement((ObjectProperty)OpImplement.Property);
            for(int j=0;j<IntNum;j++)
                Operand.Mapping.put(OpImplement.ListInterfaces.get(((Integer)Params[Offs+2+j])-1),ListProperty.ListInterfaces.get(j));
            Property.Operands.add(Operand);
            Property.Coeffs.put(Operand,(Integer)Params[Offs]);
        }
        Properties.add(Property);

        return ListProperty;
    }

    void RegGClass(LGP GroupProp,Object ...iParams) {
        int iInt=0;
        boolean bInt=true;
        for(Object i : iParams) {
            if(bInt) {
                iInt = (Integer)i-1;
                bInt = false;
            } else {
                ((GroupProperty)GroupProp.Property).ToClasses.put(GroupProp.GroupProperty.ListInterfaces.get(iInt),(Class)i);
                bInt = true;
            }
        }        
    }
    
    // генерирует белую БЛ
    void OpenTest(boolean Classes,boolean Properties,boolean Implements,boolean Persistent,boolean Changes)  throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException  {

        if(Classes) {
            Class Base = new ObjectClass(3, "Базовый объект");
            Base.AddParent(BaseClass);
            Class Article = new ObjectClass(4, "Товар");
            Article.AddParent(Base);
            Class Store = new ObjectClass(5, "Склад");
            Store.AddParent(Base);
            Class Document = new ObjectClass(6, "Документ");
            Document.AddParent(Base);
            Class PrihDocument = new ObjectClass(7, "Приходный документ");
            PrihDocument.AddParent(Document);
            Class RashDocument = new ObjectClass(8, "Расходный документ");
            RashDocument.AddParent(Document);
            Class ArticleGroup = new ObjectClass(9, "Группа товаров");
            ArticleGroup.AddParent(Base);
            Class Supplier = new ObjectClass(10, "Поставщик");
            Supplier.AddParent(Base);

            TableImplement Include;
            
            if(Implements) {
                Include = new TableImplement();
                Include.add(new DataPropertyInterface(Article));
                TableFactory.IncludeIntoGraph(Include);
                Include = new TableImplement();
                Include.add(new DataPropertyInterface(Store));
                TableFactory.IncludeIntoGraph(Include);
                Include = new TableImplement();
                Include.add(new DataPropertyInterface(ArticleGroup));
                TableFactory.IncludeIntoGraph(Include);
                Include = new TableImplement();
                Include.add(new DataPropertyInterface(Article));
                Include.add(new DataPropertyInterface(Document));
                TableFactory.IncludeIntoGraph(Include);
                Include = new TableImplement();
                Include.add(new DataPropertyInterface(Article));
                Include.add(new DataPropertyInterface(Store));
                TableFactory.IncludeIntoGraph(Include);
            }
            
            if(Properties) {
                LDP Name = AddDProp(StringClass,Base);
                LDP DocStore = AddDProp(Store,Document);
                LDP Quantity = AddDProp(IntegerClass,Document,Article);
                LDP PrihQuantity = AddDProp(IntegerClass,PrihDocument,Article);
                LDP RashQuantity = AddDProp(IntegerClass,RashDocument,Article);
                LDP ArtToGroup = AddDProp(ArticleGroup,Article);
                LDP DocDate = AddDProp(IntegerClass,Document);
                LDP ArtSupplier = AddDProp(Supplier,Article,Store);
                LDP PriceSupp = AddDProp(IntegerClass,Article,Supplier);
                LDP GrStQty = AddDProp(IntegerClass,ArticleGroup,Store);

                LSFP Dirihle = AddSFProp("prm1<prm2",true,2);
                LMFP Multiply = AddMFProp(2);

                Name.Property.OutName = "имя";
                DocStore.Property.OutName = "склад";
                Quantity.Property.OutName = "кол-во";
                PrihQuantity.Property.OutName = "кол-во прих.";
                RashQuantity.Property.OutName = "кол-во расх.";
                ArtToGroup.Property.OutName = "гр. тов";
                DocDate.Property.OutName = "дата док.";
                ArtSupplier.Property.OutName = "тек. пост.";
                PriceSupp.Property.OutName = "цена пост.";
                GrStQty.Property.OutName = "грт на скл.";

                LRP QtyGrSt = AddRProp(GrStQty,2,ArtToGroup,1,DocStore,2);
                QtyGrSt.Property.OutName = "тдок - кол-во гр. ск.";

                LRP OstPrice = AddRProp(PriceSupp,2,1,ArtSupplier,1,2);
                OstPrice.Property.OutName = "цена на складе";

                LRP StoreName = AddRProp(Name,1,DocStore,1);
                StoreName.Property.OutName = "имя склада";
                LRP ArtGroupName = AddRProp(Name,1,ArtToGroup,1);
                ArtGroupName.Property.OutName = "имя гр. тов.";

                LRP DDep = AddRProp(Dirihle,2,DocDate,1,DocDate,2);
                DDep.Property.OutName = "предш. док.";

                LRP QDep = AddRProp(Multiply,3,DDep,1,2,Quantity,1,3);
                QDep.Property.OutName = "изм. баланса";

                LGP GSum = AddGProp(QDep,true,2,3);
                GSum.Property.OutName = "остаток до операции";

                LGP GP = AddGProp(Quantity,true,DocStore,1,2);
                GP.Property.OutName = "сумм кол-во док. тов.";
                LGP GAP = AddGProp(GP,true,2);
                GAP.Property.OutName = "сумм кол-во тов.";
                LGP G2P = AddGProp(Quantity,true,DocStore,1,ArtToGroup,2);
                G2P.Property.OutName = "скл-гр. тов";

                LGP PrihArtStore = AddGProp(PrihQuantity,true,DocStore,1,2);
                PrihArtStore.Property.OutName = "приход по складу";

                LGP RashArtStore = AddGProp(RashQuantity,true,DocStore,1,2);
                RashArtStore.Property.OutName = "расход по складу";

                LRP OstArtStore = AddLProp(1,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);
                OstArtStore.Property.OutName = "остаток по складу";

                LGP OstArt = AddGProp(OstArtStore,true,2);
                OstArt.Property.OutName = "остаток по товару";

                LGP MaxPrih = AddGProp(PrihQuantity,false,DocStore,1,ArtToGroup,2);
                MaxPrih.Property.OutName = "макс. приход по гр. тов.";

                LRP MaxOpStore = AddLProp(0,2,1,PrihArtStore,1,2,1,RashArtStore,1,2);
                MaxOpStore.Property.OutName = "макс. операция";

                LGP SumMaxArt = AddGProp(MaxOpStore,true,2);
                SumMaxArt.Property.OutName = "сумма макс. операция";

                if(Persistent) {
    /*                Persistents.add((AggregateProperty)GP.Property);
                    Persistents.add((AggregateProperty)GAP.Property);
                    Persistents.add((AggregateProperty)G2P.Property);
                    Persistents.add((AggregateProperty)GSum.Property);
                    Persistents.add((AggregateProperty)OstArtStore.Property);
                    Persistents.add((AggregateProperty)OstArt.Property);
                    Persistents.add((AggregateProperty)MaxPrih.Property);
                    Persistents.add((AggregateProperty)MaxOpStore.Property);
                    Persistents.add((AggregateProperty)SumMaxArt.Property);
                    Persistents.add((AggregateProperty)OstPrice.Property);*/
                    Persistents.add((AggregateProperty)QtyGrSt.Property);
                }
                
                if(Changes) {
                    DataAdapter ad = new DataAdapter("");

                    FillDB(ad);

                    ChangesSession Session = CreateSession();

                    Integer i;
                    Integer[] Articles = new Integer[6];
                    for(i=0;i<Articles.length;i++) Articles[i] = AddObject(Session,ad,Article);

                    Integer[] Stores = new Integer[2];
                    for(i=0;i<Stores.length;i++) Stores[i] = AddObject(Session,ad,Store);

                    Integer[] PrihDocuments = new Integer[6];
                    for(i=0;i<PrihDocuments.length;i++) {
                        PrihDocuments[i] = AddObject(Session,ad,PrihDocument);
                        Name.ChangeProperty(Session,ad,"ПР ДОК "+i.toString(), PrihDocuments[i]);
                    }

                    Integer[] RashDocuments = new Integer[6];
                    for(i=0;i<RashDocuments.length;i++) {
                        RashDocuments[i] = AddObject(Session,ad,RashDocument);
                        Name.ChangeProperty(Session,ad,"РАСХ ДОК "+i.toString(), RashDocuments[i]);
                    }

                    Integer[] ArticleGroups = new Integer[2];
                    for(i=0;i<ArticleGroups.length;i++) ArticleGroups[i] = AddObject(Session,ad,ArticleGroup);

                    Name.ChangeProperty(Session,ad,"КОЛБАСА", Articles[0]);
                    Name.ChangeProperty(Session,ad,"ТВОРОГ", Articles[1]);
                    Name.ChangeProperty(Session,ad,"МОЛОКО", Articles[2]);
                    Name.ChangeProperty(Session,ad,"ОБУВЬ", Articles[3]);
                    Name.ChangeProperty(Session,ad,"ДЖЕМПЕР", Articles[4]);
                    Name.ChangeProperty(Session,ad,"МАЙКА", Articles[5]);

                    Name.ChangeProperty(Session,ad,"СКЛАД", Stores[0]);
                    Name.ChangeProperty(Session,ad,"ТЗАЛ", Stores[1]);

                    Name.ChangeProperty(Session,ad,"ПРОДУКТЫ", ArticleGroups[0]);
                    Name.ChangeProperty(Session,ad,"ОДЕЖДА", ArticleGroups[1]);

                    DocStore.ChangeProperty(Session,ad,Stores[0],PrihDocuments[0]);
                    DocStore.ChangeProperty(Session,ad,Stores[0],PrihDocuments[1]);
                    DocStore.ChangeProperty(Session,ad,Stores[1],PrihDocuments[2]);
                    DocStore.ChangeProperty(Session,ad,Stores[0],PrihDocuments[3]);
                    DocStore.ChangeProperty(Session,ad,Stores[1],PrihDocuments[4]);

                    DocStore.ChangeProperty(Session,ad,Stores[1],RashDocuments[0]);
                    DocStore.ChangeProperty(Session,ad,Stores[1],RashDocuments[1]);
                    DocStore.ChangeProperty(Session,ad,Stores[0],RashDocuments[2]);
                    DocStore.ChangeProperty(Session,ad,Stores[0],RashDocuments[3]);
                    DocStore.ChangeProperty(Session,ad,Stores[1],RashDocuments[4]);

            //        DocStore.ChangeProperty(ad,Stores[1],Documents[5]);

                    DocDate.ChangeProperty(Session,ad,1001,PrihDocuments[0]);
                    DocDate.ChangeProperty(Session,ad,1001,RashDocuments[0]);
                    DocDate.ChangeProperty(Session,ad,1008,PrihDocuments[1]);
                    DocDate.ChangeProperty(Session,ad,1009,RashDocuments[1]);
                    DocDate.ChangeProperty(Session,ad,1010,RashDocuments[2]);
                    DocDate.ChangeProperty(Session,ad,1011,RashDocuments[3]);
                    DocDate.ChangeProperty(Session,ad,1012,PrihDocuments[2]);
                    DocDate.ChangeProperty(Session,ad,1014,PrihDocuments[3]);
                    DocDate.ChangeProperty(Session,ad,1016,RashDocuments[4]);
                    DocDate.ChangeProperty(Session,ad,1018,PrihDocuments[4]);

                    ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[0]);
                    ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[1]);
                    ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[2]);
                    ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[1],Articles[3]);
                    ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[1],Articles[4]);

                    // Quantity
                    PrihQuantity.ChangeProperty(Session,ad,10,PrihDocuments[0],Articles[0]);
                    RashQuantity.ChangeProperty(Session,ad,5,RashDocuments[0],Articles[0]);
                    RashQuantity.ChangeProperty(Session,ad,3,RashDocuments[1],Articles[0]);

                    PrihQuantity.ChangeProperty(Session,ad,8,PrihDocuments[0],Articles[1]);
                    PrihQuantity.ChangeProperty(Session,ad,2,PrihDocuments[1],Articles[1]);
                    PrihQuantity.ChangeProperty(Session,ad,10,PrihDocuments[3],Articles[1]);
                    RashQuantity.ChangeProperty(Session,ad,14,RashDocuments[2],Articles[1]);

                    PrihQuantity.ChangeProperty(Session,ad,8,PrihDocuments[3],Articles[2]);
                    RashQuantity.ChangeProperty(Session,ad,2,RashDocuments[1],Articles[2]);
                    RashQuantity.ChangeProperty(Session,ad,10,RashDocuments[3],Articles[2]);
                    PrihQuantity.ChangeProperty(Session,ad,4,PrihDocuments[4],Articles[2]);

                    PrihQuantity.ChangeProperty(Session,ad,4,PrihDocuments[3],Articles[3]);

                    RashQuantity.ChangeProperty(Session,ad,4,RashDocuments[2],Articles[4]);
                    RashQuantity.ChangeProperty(Session,ad,4,RashDocuments[3],Articles[4]);

                    GrStQty.ChangeProperty(Session,ad,5,ArticleGroups[0],Stores[0]);
                    GrStQty.ChangeProperty(Session,ad,4,ArticleGroups[0],Stores[1]);
                    GrStQty.ChangeProperty(Session,ad,3,ArticleGroups[1],Stores[0]);

                    Apply(ad,Session);

                    Session = CreateSession();

                    DocStore.ChangeProperty(Session,ad,Stores[1],PrihDocuments[0]);
                    ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[1],Articles[0]);
                    
                    Apply(ad,Session);

//                    TableFactory.ReCalculateAggr = true;
//                    QtyGrSt.Property.Out(ad);
//                    TableFactory.ReCalculateAggr = false;
//                    QtyGrSt.Property.Out(ad);
                    CheckPersistent(ad);
                }
            }

        }
    }
    
    // случайным образом генерирует классы
    void RandomClasses(Random Randomizer) {
        int CustomClasses = Randomizer.nextInt(20);//
        List<Class> ObjClasses = new ArrayList();
        ObjClasses.add(BaseClass);
        for(int i=0;i<CustomClasses;i++) {
            Class Class = new ObjectClass(i+3, "Случайный класс");
            int Parents = Randomizer.nextInt(6) + 1;
            for(int j=0;j<Parents;j++) {
                Class.AddParent(ObjClasses.get(Randomizer.nextInt(ObjClasses.size())));
            }
            ObjClasses.add(Class);
        }
    }

    // случайным образом генерирует св-ва
    void RandomProperties(Random Randomizer) {
        
        List<Class> Classes = new ArrayList();
        BaseClass.FillClassList(Classes);
        
        List<Property> RandProps = new ArrayList();
        List<ObjectProperty> RandObjProps = new ArrayList();
        
        StringFormulaProperty Dirihle = new StringFormulaProperty("(CASE WHEN prm1<prm2 THEN 1 ELSE 0 END)");
        Dirihle.Interfaces.add(new StringFormulaPropertyInterface(BaseClass,"prm1"));
        Dirihle.Interfaces.add(new StringFormulaPropertyInterface(BaseClass,"prm2"));
        RandProps.add(Dirihle);

        MultiplyFormulaProperty Multiply = new MultiplyFormulaProperty();
        Multiply.Interfaces.add(new FormulaPropertyInterface(BaseClass));
        Multiply.Interfaces.add(new FormulaPropertyInterface(BaseClass));
        RandProps.add(Multiply);

        int DataPropCount = Randomizer.nextInt(15)+1;
        for(int i=0;i<DataPropCount;i++) {
            // DataProperty
            DataProperty DataProp = new DataProperty(TableFactory,Classes.get(Randomizer.nextInt(Classes.size())));
            // генерируем классы
            int IntCount = Randomizer.nextInt(TableFactory.MaxInterface)+1;
            for(int j=0;j<IntCount;j++)
                DataProp.Interfaces.add(new DataPropertyInterface(Classes.get(Randomizer.nextInt(Classes.size()))));

            RandProps.add(DataProp);
            RandObjProps.add(DataProp);
        }

        System.out.print("Создание аггрег. св-в ");
                
        int PropCount = Randomizer.nextInt(1000)+1;
        for(int i=0;i<PropCount;i++) {
//            int RandClass = Randomizer.nextInt(10);
//            int PropClass = (RandClass>7?0:(RandClass==8?1:2));
            int PropClass = Randomizer.nextInt(6);
//            int PropClass = 5;
            ObjectProperty GenProp = null;
            String ResType = "";
            if(PropClass==0) {
                // RelationProperty
                RelationProperty RelProp = new RelationProperty(TableFactory,RandProps.get(Randomizer.nextInt(RandProps.size())));
                
                // генерируем случайно кол-во интерфейсов
                List<PropertyInterface> RelPropInt = new ArrayList();
                int IntCount = Randomizer.nextInt(TableFactory.MaxInterface)+1;
                for(int j=0;j<IntCount;j++) {
                    PropertyInterface Interface = new PropertyInterface();
                    RelProp.Interfaces.add(Interface);
                    RelPropInt.add(Interface);
                }
                
                // чтобы 2 раза на одну и ту же ветку не натыкаться
                List<PropertyInterface> AvailRelInt = new ArrayList(RelPropInt);
                boolean Correct = true;
                
                for(PropertyInterface Interface : (Collection<PropertyInterface>)RelProp.Implements.Property.Interfaces) {
                    // генерируем случайно map'ы на эти интерфейсы
                    if(RelProp.Implements.Property instanceof ObjectProperty && Randomizer.nextBoolean()) {
                        if(AvailRelInt.size()==0) {
                            Correct = false;
                            break;
                        }
                        PropertyInterface MapInterface = AvailRelInt.get(Randomizer.nextInt(AvailRelInt.size()));
                        RelProp.Implements.Mapping.put(Interface,MapInterface);
                        AvailRelInt.remove(MapInterface);
                    } else {
                        // другое property пока сгенерим на 1
                        PropertyMapImplement ImpProp = new PropertyMapImplement(RandObjProps.get(Randomizer.nextInt(RandObjProps.size())));
                        if(ImpProp.Property.Interfaces.size()>RelPropInt.size()) {
                            Correct = false;
                            break;
                        }
                        
                        List<PropertyInterface> MapRelInt = new ArrayList(RelPropInt);
                        for(PropertyInterface ImpInterface : (Collection<PropertyInterface>)ImpProp.Property.Interfaces) {
                            PropertyInterface MapInterface = MapRelInt.get(Randomizer.nextInt(MapRelInt.size()));
                            ImpProp.Mapping.put(ImpInterface,MapInterface);
                            MapRelInt.remove(MapInterface);
                        }
                        RelProp.Implements.Mapping.put(Interface,ImpProp);
                    }
                }

                if(Correct) {
                    GenProp = RelProp;
                    ResType = "R";
                }
            }
            
            if(PropClass==1 || PropClass==2) {
                // группировочное
                ObjectProperty GroupProp = RandObjProps.get(Randomizer.nextInt(RandObjProps.size()));
                GroupProperty Property = null;
                if(PropClass==1) {
                    Property = new SumGroupProperty(TableFactory,GroupProp);
                    ResType = "SG";
                } else {
                    Property = new MaxGroupProperty(TableFactory,GroupProp);
                    ResType = "MG";
                }
                
                boolean Correct = true;                
                List<PropertyInterface> GroupInt = new ArrayList(GroupProp.Interfaces);
                int GroupCount = Randomizer.nextInt(TableFactory.MaxInterface)+1;
                for(int j=0;j<GroupCount;j++) {
                    PropertyInterfaceImplement Implement;
                    // генерируем случайно map'ы на эти интерфейсы
                    if(Randomizer.nextBoolean()) {
                        Implement = GroupInt.get(Randomizer.nextInt(GroupInt.size()));
                    } else {
                        // другое property пока сгенерим на 1
                        PropertyMapImplement ImpProp = new PropertyMapImplement(RandObjProps.get(Randomizer.nextInt(RandObjProps.size())));
                        if(ImpProp.Property.Interfaces.size()>GroupInt.size()) {
                            Correct = false;
                            break;
                        }

                        List<PropertyInterface> MapRelInt = new ArrayList(GroupInt);
                        for(PropertyInterface ImpInterface : (Collection<PropertyInterface>)ImpProp.Property.Interfaces) {
                            PropertyInterface MapInterface = MapRelInt.get(Randomizer.nextInt(MapRelInt.size()));
                            ImpProp.Mapping.put(ImpInterface,MapInterface);
                            MapRelInt.remove(MapInterface);
                        }
                        Implement = ImpProp;
                    }
                    
                    Property.Interfaces.add(new GroupPropertyInterface(Implement));
                }
                
                if(Correct)
                    GenProp = Property;
            }

            if(PropClass==3 || PropClass==4 || PropClass==5) {
                ListProperty Property = null;
                if(PropClass==3) {
                    Property = new SumListProperty(TableFactory);
                    ResType = "SL";
                } else {
                if(PropClass==4) {
                    Property = new MaxListProperty(TableFactory);
                    ResType = "ML";
                } else {
                    Property = new OverrideListProperty(TableFactory);
                    ResType = "OL";
                }
                }
                
                int OpIntCount = Randomizer.nextInt(TableFactory.MaxInterface)+1;
                for(int j=0;j<OpIntCount;j++)
                    Property.Interfaces.add(new PropertyInterface());
        
                boolean Correct = true;
                List<PropertyInterface> OpInt = new ArrayList(Property.Interfaces);
                int OpCount = Randomizer.nextInt(4)+1;
                for(int j=0;j<OpCount;j++) {
                    PropertyMapImplement Operand = new PropertyMapImplement(RandObjProps.get(Randomizer.nextInt(RandObjProps.size())));
                    if(Operand.Property.Interfaces.size()!=OpInt.size()) {
                        Correct = false;
                        break;
                    }

                    List<PropertyInterface> MapRelInt = new ArrayList(OpInt);
                    for(PropertyInterface ImpInterface : (Collection<PropertyInterface>)Operand.Property.Interfaces) {
                        PropertyInterface MapInterface = MapRelInt.get(Randomizer.nextInt(MapRelInt.size()));
                        Operand.Mapping.put(ImpInterface,MapInterface);
                        MapRelInt.remove(MapInterface);
                    }
                    Property.Operands.add(Operand);
                }
                
                if(Correct)
                    GenProp = Property;
            }
                       

            if(GenProp!=null) {
                // проверим что есть в интерфейсе и покрыты все ключи
                Iterator<InterfaceClass> ic = GenProp.GetClassSet(null).iterator();
                if(ic.hasNext() && ic.next().keySet().size()==GenProp.Interfaces.size()) {
                    System.out.print(ResType+"-");
                    RandProps.add(GenProp);
                    RandObjProps.add(GenProp);
                }
            }
        }
        
        Properties.addAll(RandProps);
        
        System.out.println();
    }
    
    // случайным образом генерирует имплементацию
    void RandomImplement(Random Randomizer) {
        List<Class> Classes = new ArrayList();
        BaseClass.FillClassList(Classes);

        // заполнение физ модели
        int ImplementCount = Randomizer.nextInt(8);
        for(int i=0;i<ImplementCount;i++) {
            TableImplement Include = new TableImplement();
            int ObjCount = Randomizer.nextInt(3)+1;
            for(int ioc=0;ioc<ObjCount;ioc++)
                Include.add(new DataPropertyInterface(Classes.get(Randomizer.nextInt(Classes.size()))));
            TableFactory.IncludeIntoGraph(Include);               
        }        
    }
    
    // случайным образом генерирует постоянные аггрегации
    void RandomPersistent(Random Randomizer) {
        
        // сначала список получим
        List<AggregateProperty> AggrProperties = new ArrayList();
        for(Property Property : Properties) {
            if(Property instanceof AggregateProperty)
                AggrProperties.add((AggregateProperty)Property);
        }
        
        int PersistentNum = Randomizer.nextInt(AggrProperties.size())+1;
        for(int i=0;i<PersistentNum;i++)
            Persistents.add(AggrProperties.get(Randomizer.nextInt(AggrProperties.size())));
    }
    
    void FullDBTest()  throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        
        // сгенерить классы
        // сгенерить св-ва
        // сгенерить физ. модель
        // сгенерить persistent аггрегации
//        OpenTest(false,false,false,false,false);

        DataAdapter Adapter = new DataAdapter("");
        OpenTest(true,true,false,false,false);
//        if(true) return;

        Random Randomizer = new Random();
//        Randomizer.setSeed(1000);

        while(true) {
//            RandomClasses(Randomizer);

//            RandomProperties(Randomizer);
            
            RandomImplement(Randomizer);

            RandomPersistent(Randomizer);
            
            FillDB(Adapter);

            // запустить ChangeDBTest
            ChangeDBTest(Adapter,20,Randomizer);

            Adapter.Disconnect();
        }
    }
    
    
    void ChangeDBTest(DataAdapter Adapter,Integer MaxIterations,Random Randomizer) throws SQLException {
        
        // сначала список получим
        List<DataProperty> DataProperties = new ArrayList();
        for(Property Property : Properties) {
            if(Property instanceof DataProperty)
                DataProperties.add((DataProperty)Property);
        }
        
//        Randomizer.setSeed(1);
        int Iterations = 2;
        while(Iterations<MaxIterations) {
            System.out.println(Iterations);

            ChangesSession Session = CreateSession();

            // будем также рандомно создавать объекты
            List<Class> AddClasses = new ArrayList();
            BaseClass.FillClassList(AddClasses);
            int ObjectAdd = Randomizer.nextInt(2)+1;
            for(int ia=0;ia<ObjectAdd;ia++) {
                Class AddClass = AddClasses.get(Randomizer.nextInt(AddClasses.size()));
                if(AddClass instanceof ObjectClass) {
                    AddObject(Session,Adapter,AddClass);
                }
            }

            int PropertiesChanged = Randomizer.nextInt(8)+1;
            for(int ip=0;ip<PropertiesChanged;ip++) {
                // берем случайные n св-в
                DataProperty ChangeProp = DataProperties.get(Randomizer.nextInt(DataProperties.size()));
                int NumChanges = Randomizer.nextInt(3)+1;
                for(int in=0;in<NumChanges;in++) {                    
/*                    // теперь определяем класс найденного объекта
                    Class ValueClass = null;
                    if(ChangeProp.Value instanceof ObjectClass)
                        ValueClass = BaseClass.FindClassID(ValueObject);
                    else
                        ValueClass = ChangeProp.Value;*/
                        
                    InterfaceClassSet InterfaceSet = ChangeProp.GetClassSet(null);
                    // определяем входные классы
                    InterfaceClass Classes = InterfaceSet.get(Randomizer.nextInt(InterfaceSet.size()));
                    // генерим рандомные объекты этих классов
                    Map<PropertyInterface,ObjectValue> Keys = new HashMap();
                    for(DataPropertyInterface Interface : ChangeProp.Interfaces)
                        Keys.put(Interface,new ObjectValue((Integer)Classes.get(Interface).GetRandomObject(Adapter,TableFactory,Randomizer,0),Classes.get(Interface)));
                    
                    Object ValueObject = null;
                    if(Randomizer.nextInt(10)<8)
                        ValueObject = ChangeProp.Value.GetRandomObject(Adapter,TableFactory,Randomizer,Iterations);
                    
                    ChangeProp.ChangeProperty(Adapter,Keys,ValueObject,Session);
                }
            }
            
/*            for(DataProperty Property : Session.Properties) {
                Property.OutChangesTable(Adapter, Session);
            }*/
                
                
            Apply(Adapter,Session);
            CheckPersistent(Adapter);
        }
    }
}


/*
 *             if(!TableFactory.Crash) {
                for(AggregateProperty Prop : Persistents)
                    if(Prop.OutName.equals("макс. операция")) {
                        System.out.println("-макс. операция--AFTER PERS ----");
                        Prop.Out(Adapter);
                        System.out.println("-макс. операция--AFTER RECA ----");
                        TableFactory.ReCalculateAggr = true;
                        Prop.Out(Adapter);
                        TableFactory.ReCalculateAggr = false;
                    }

                for(AggregateProperty Prop : Persistents)
                    if(Prop.OutName.equals("приход по складу")) {
                        System.out.println("-приход по складу--AFTER PERS ----");
                        Prop.Out(Adapter);
                        System.out.println("-приход по складу--AFTER RECA ----");
                        TableFactory.ReCalculateAggr = true;
                        Prop.Out(Adapter);
                        TableFactory.ReCalculateAggr = false;
                    }

                TableFactory.Crash = true;
*/