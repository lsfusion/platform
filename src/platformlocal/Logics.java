/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;

import java.util.*;
import static java.lang.Thread.sleep;

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
    PropertyField Class;
    
    ObjectTable() {
        super("objects");
        Key = new KeyField("object","integer");
        Keys.add(Key);
        Class = new PropertyField("class","integer");
        Properties.add(Class);
    };
    
    Integer GetClassID(DataSession Session,Integer idObject) throws SQLException {
        if(idObject==null) return null;

        JoinQuery<Object,String> Query = new JoinQuery<Object,String>();
        Join<KeyField,PropertyField> JoinTable = new Join<KeyField,PropertyField>(this,true);
        JoinTable.Joins.put(Key,new ValueSourceExpr(idObject));
        Query.add("classid",JoinTable.Exprs.get(Class));
        LinkedHashMap<Map<Object,Integer>,Map<String,Object>> Result = Query.executeSelect(Session);
        if(Result.size()>0)
            return (Integer)Result.values().iterator().next().get("classid");
        else
            return null;        
    }

    JoinQuery<KeyField,PropertyField> getClassJoin(Class ChangeClass) {

        Collection<Integer> SetID = new HashSet<Integer>();
        ChangeClass.FillSetID(SetID);

        JoinQuery<KeyField,PropertyField> ClassQuery = new JoinQuery<KeyField,PropertyField>(Keys);
        ClassQuery.add(new FieldSetValueWhere((new UniJoin<KeyField,PropertyField>(this,ClassQuery,true)).Exprs.get(Class),SetID));

        return ClassQuery;
    }
}

// таблица счетчика ID
class IDTable extends Table {
    KeyField Key;
    PropertyField Value;
    
    IDTable() {
        super("idtable");
        Key = new KeyField("id","integer");
        Keys.add(Key);
        
        Value = new PropertyField("value","integer");
        Properties.add(Value);
    }

    int ObjectID = 1;
    
    Integer GenerateID(DataSession Adapter) throws SQLException {
        // читаем
        JoinQuery<KeyField,PropertyField> Query = new JoinQuery<KeyField,PropertyField>(Keys);
        Join<KeyField,PropertyField> JoinTable = new Join<KeyField,PropertyField>(this,true);
        JoinTable.Joins.put(Key,Query.MapKeys.get(Key));
        Query.add(Value,JoinTable.Exprs.get(Value));

        Query.add(new FieldExprCompareWhere(Query.MapKeys.get(Key),ObjectID,0));

        Integer FreeID = (Integer)Query.executeSelect(Adapter).values().iterator().next().get(Value);

        // замещаем
        Map<KeyField,Integer> KeyFields = new HashMap();
        KeyFields.put(Key,ObjectID);
        Map<PropertyField,Object> PropFields = new HashMap();
        PropFields.put(Value,FreeID+1);
        Adapter.UpdateRecords(new ModifyQuery(this,new DumbSource<KeyField,PropertyField>(KeyFields,PropFields)));
        return FreeID+1;
    }
}

// таблица куда виды складывают свои объекты
class ViewTable extends SessionTable {
    ViewTable(Integer iObjects) {
        super("viewtable"+iObjects.toString());
        Objects = new ArrayList();
        for(Integer i=0;i<iObjects;i++) {
            KeyField ObjKeyField = new KeyField("object"+i,"integer");
            Objects.add(ObjKeyField);
            Keys.add(ObjKeyField);
        }
        
        View = new KeyField("viewid","integer");
        Keys.add(View);
    }
            
    List<KeyField> Objects;
    KeyField View;
    
    void DropViewID(DataSession Session,Integer ViewID) throws SQLException {
        Map<KeyField,Integer> ValueKeys = new HashMap();
        ValueKeys.put(View,ViewID);
        Session.deleteKeyRecords(this,ValueKeys);
    }
}

abstract class ChangeTable extends SessionTable {

//    KeyField Session;
    
    ChangeTable(String iName) {
        super(iName);

//        Session = new KeyField("session","integer");
//        Keys.add(Session);
    }
}

class ChangeObjectTable extends ChangeTable {

    Collection<KeyField> Objects;
    KeyField Property;
    PropertyField Value;
    
    ChangeObjectTable(String TablePrefix,Integer iObjects,Integer iDBType,List<String> DBTypes) {
        super(TablePrefix+"changetable"+iObjects+"t"+iDBType);

        Objects = new ArrayList();
        for(Integer i=0;i<iObjects;i++) {
            KeyField ObjKeyField = new KeyField("object"+i,"integer");
            Objects.add(ObjKeyField);
            Keys.add(ObjKeyField);
        }
        
        Property = new KeyField("property","integer");
        Keys.add(Property);
        
        Value = new PropertyField("value",DBTypes.get(iDBType));
        Properties.add(Value);
    }
}

class DataChangeTable extends ChangeObjectTable {

    DataChangeTable(Integer iObjects,Integer iDBType,List<String> DBTypes) {
        super("data",iObjects,iDBType,DBTypes);
    }
}

class IncrementChangeTable extends ChangeObjectTable {

    PropertyField PrevValue;
    // системное поля, по сути для MaxGroupProperty
    PropertyField SysValue;
    
    IncrementChangeTable(Integer iObjects,Integer iDBType,List<String> DBTypes) {
        super("inc",iObjects,iDBType,DBTypes);

        PrevValue = new PropertyField("prevvalue",DBTypes.get(iDBType));
        Properties.add(PrevValue);

        SysValue = new PropertyField("sysvalue",DBTypes.get(iDBType));
        Properties.add(SysValue);
    }
}

// таблица изменений классов
// хранит добавляение\удаление классов
class ChangeClassTable extends ChangeTable {

    KeyField Class;
    KeyField Object;

    ChangeClassTable(String iTable) {
        super(iTable);

        Object = new KeyField("object","integer");
        Keys.add(Object);

        Class = new KeyField("class","integer");
        Keys.add(Class);
    }
    
    void ChangeClass(DataSession ChangeSession, Integer idObject, Collection<Class> Classes) throws SQLException {

        for(Class Change : Classes) {
            Map<KeyField,Integer> InsertKeys = new HashMap();
            InsertKeys.put(Object,idObject);
            InsertKeys.put(Class,Change.ID);
            ChangeSession.InsertRecord(this,InsertKeys,new HashMap());
        }
    }
    
    void DropSession(DataSession ChangeSession) throws SQLException {
        Map<KeyField,Integer> ValueKeys = new HashMap();
        ChangeSession.deleteKeyRecords(this,ValueKeys);
    }
    
    JoinQuery<KeyField,PropertyField> getClassJoin(DataSession ChangeSession,Class ChangeClass) {

        Collection<KeyField> ObjectKeys = new ArrayList();
        ObjectKeys.add(Object);
        JoinQuery<KeyField,PropertyField> ClassQuery = new JoinQuery<KeyField,PropertyField>(ObjectKeys);

        Join<KeyField,PropertyField> ClassJoin = new Join<KeyField,PropertyField>(this,true);
        ClassJoin.Joins.put(Object,ClassQuery.MapKeys.get(Object));
        ClassJoin.Joins.put(Class,new ValueSourceExpr(ChangeClass.ID));

        ClassQuery.add(ClassJoin.InJoin);

        return ClassQuery;
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

    void excludeJoin(JoinQuery<?,?> Query, DataSession Session,Class ChangeClass,SourceExpr Join) {
        Join<KeyField,PropertyField> ClassJoin = new Join<KeyField,PropertyField>(getClassJoin(Session,ChangeClass),false);
        ClassJoin.Joins.put(Object,Join);

        Query.add(new NotWhere(ClassJoin.InJoin));
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
    
    void FillDB(DataSession Session) throws SQLException {
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
                Node.Table.Keys.add(Field);
                Node.MapFields.put(Interface,Field);
            }
        }

        Session.CreateTable(ObjectTable);
        Session.CreateTable(IDTable);

        // закинем одну запись
        Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
        InsertKeys.put(IDTable.Key,IDTable.ObjectID);
        Map<PropertyField,Object> InsertProps = new HashMap<PropertyField,Object>();
        InsertProps.put(IDTable.Value,0);
        Session.InsertRecord(IDTable,InsertKeys,InsertProps);
    }

    // заполняет временные таблицы
    void fillSession(DataSession Session) throws SQLException {
        
        Session.CreateTemporaryTable(AddClassTable);
        Session.CreateTemporaryTable(RemoveClassTable);

        for(List<IncrementChangeTable> ListTables : ChangeTables)
            for(ChangeObjectTable ChangeTable : ListTables) Session.CreateTemporaryTable(ChangeTable);
        for(List<DataChangeTable> ListTables : DataChangeTables)
            for(ChangeObjectTable DataChangeTable : ListTables) Session.CreateTemporaryTable(DataChangeTable);

        for(ViewTable ViewTable : ViewTables) Session.CreateTemporaryTable(ViewTable);
    }

    // счетчик сессий (пока так потом надо из базы или как-то по другому транзакционность сделать
    void clearSession(DataSession Session) throws SQLException {

        Session.deleteKeyRecords(AddClassTable,new HashMap());
        Session.deleteKeyRecords(RemoveClassTable,new HashMap());

        for(List<IncrementChangeTable> TypeTables : ChangeTables)
            for(ChangeObjectTable Table : TypeTables)
                Session.deleteKeyRecords(Table,new HashMap());
        for(List<DataChangeTable> TypeTables : DataChangeTables)
            for(ChangeObjectTable Table : TypeTables)
                Session.deleteKeyRecords(Table,new HashMap());
    }
}

abstract class BusinessLogics<T extends BusinessLogics<T>> {
    
    void initBase() {
        TableFactory = new TableFactory();

        objectClass = new ObjectClass(0, "Базовый класс");
        integralClass = new IntegralClass(2, "Число");
        stringClass = new StringClass(1, "Строка");
        quantityClass = new QuantityClass(2, "Кол-во");
        quantityClass.AddParent(integralClass);
        dateClass = new DateClass(3, "Дата");
        dateClass.AddParent(integralClass);
        bitClass = new BitClass(4, "Бит");
        bitClass.AddParent(integralClass);

        for(int i=0;i<TableFactory.MaxInterface;i++) {
            TableImplement Include = new TableImplement();
            for(int j=0;j<=i;j++)
                Include.add(new DataPropertyInterface(objectClass));
            TableFactory.IncludeIntoGraph(Include);
        }         
        
        baseElement = new NavigatorElement<T>(0, "Base Group");
    }

    // по умолчанию с полным стартом
    BusinessLogics() {
        initBase();

        InitLogics();
        InitImplements();
        InitNavigators();
    }

    static int LastSuspicious = 0;

    // тестирующий конструктор
    BusinessLogics(int TestType) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        initBase();

        if(TestType>=1) {
            InitLogics();
            if(TestType>=2)
                InitImplements();
        }

        Integer Seed;
        List<Integer> ProceedSeeds = new ArrayList();
        int[] Suspicious = {3949,387,7651,6445,1359,8760};
        if(TestType>=0 || LastSuspicious>=Suspicious.length)
            Seed = (new Random()).nextInt(10000);
        else
            Seed = Suspicious[LastSuspicious++];        

        System.out.println("Random seed - "+Seed);

        Random Randomizer = new Random(Seed);

        DataAdapter Adapter = DataAdapter.getDefault();

        if(TestType<1) {
            RandomClasses(Randomizer);
            RandomProperties(Randomizer);
        }

        if(TestType<2) {
            RandomImplement(Randomizer);
            RandomPersistent(Randomizer);
        }

        DataSession Session = createSession(Adapter);
        FillDB(Session);
        Session.close();

        // запустить ChangeDBTest
        try {
            ChangeDBTest(Adapter,20,Randomizer);
        } catch(Exception e) {
            e.printStackTrace();
        }
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

    NavigatorElement<T> baseElement;
    
    abstract void InitNavigators();
    
    void AddDataProperty(DataProperty Property) {
        Properties.add(Property);
    }

    // получает класс по ID объекта
    Class getObjectClass(DataSession Session, Integer idObject) throws SQLException {
        // сначала получаем idClass
        if(Session.NewClasses.containsKey(idObject)) {
            List<Class> ChangeClasses = Session.NewClasses.get(idObject);
            return ChangeClasses.get(ChangeClasses.size()-1);
        } else
            return readClass(Session,idObject);
    }

    Class readClass(DataSession Session, Integer idObject) throws SQLException {
        return objectClass.FindClassID(TableFactory.ObjectTable.GetClassID(Session,idObject));
    }
    
    Integer AddObject(DataSession Session, Class Class) throws SQLException {

        Integer FreeID = TableFactory.IDTable.GenerateID(Session);

        ChangeClass(Session,FreeID,Class);
        
        return FreeID;
    }
    
    void ChangeClass(DataSession Session, Integer idObject, Class Class) throws SQLException {
        
        // запишем объекты, которые надо будет сохранять
        Session.ChangeClass(idObject,Class);
    }
    
    void UpdateClassChanges(DataSession Session) throws SQLException {

        // дропнем старую сессию
        TableFactory.AddClassTable.DropSession(Session);
        TableFactory.RemoveClassTable.DropSession(Session);
        Session.AddClasses.clear();
        Session.RemoveClasses.clear();
        
        for(Integer idObject : Session.NewClasses.keySet()) {
            // дальше нужно построить "разницу" какие классы ушли и записать в соответствующую таблицу
            
            Set<Class> TempRemoveClasses = new HashSet();
            
            ListIterator<Class> ic = Session.NewClasses.get(idObject).listIterator();
            Class NewClass = ic.next();
            if(NewClass==null) NewClass = objectClass;
            while(ic.hasNext()) {
                Class NextClass = ic.next();
                if(NextClass==null) NextClass = objectClass;
                NextClass.GetDiffSet(NewClass,null,TempRemoveClasses);
                NewClass = NextClass;
            }

            // узнаем старый класс
            Collection<Class> AddClasses = new ArrayList();
            Set<Class> RemoveClasses = new HashSet();
            NewClass.GetDiffSet(readClass(Session,idObject),AddClasses,RemoveClasses);
            Session.AddClasses.addAll(AddClasses);
            
            // вырежем все из AddClasses
            TempRemoveClasses.removeAll(AddClasses);
            RemoveClasses.addAll(TempRemoveClasses);
            Session.RemoveClasses.addAll(RemoveClasses);

            // собсно в сессию надо записать все изм. объекты
            TableFactory.AddClassTable.ChangeClass(Session,idObject,AddClasses);
            TableFactory.RemoveClassTable.ChangeClass(Session,idObject,RemoveClasses);
        }
    }
    
    void SaveClassChanges(DataSession Session) throws SQLException {
    
        for(Integer idObject : Session.NewClasses.keySet()) {
            Map<KeyField,Integer> InsertKeys = new HashMap();
            InsertKeys.put(TableFactory.ObjectTable.Key, idObject);
            Map<PropertyField,Object> InsertProps = new HashMap();
            List<Class> ChangeClasses = Session.NewClasses.get(idObject);
            Class ChangeClass = ChangeClasses.get(ChangeClasses.size()-1);
            Object Value = null;
            if(ChangeClass!=null) Value = ChangeClass.ID;
            InsertProps.put(TableFactory.ObjectTable.Class,Value);
        
            Session.UpdateInsertRecord(TableFactory.ObjectTable,InsertKeys,InsertProps);
        }        
    }

    // счетчик сессий (пока так потом надо из базы или как-то по другому транзакционность сделать
    void restartSession(DataSession Session) throws SQLException {
        // дропает сессию
        // вычищает из всех св-в ссылки на нее появившиеся в процессе UpdateAggregations
        setSessionChanged(Session,new HashMap());

        TableFactory.clearSession(Session);
        Session.restart();
    }

    int SessionCounter = 0;
    DataSession createSession(DataAdapter Adapter) throws SQLException {
        // дропает сессию
        // вычищает из всех св-в ссылки на нее появившиеся в процессе UpdateAggregations
        DataSession Session = new DataSession(Adapter,SessionCounter++);
        TableFactory.fillSession(Session);

        return Session;
    }

    IntegralClass integralClass;
    ObjectClass objectClass;
    StringClass stringClass;
    QuantityClass quantityClass;
    DateClass dateClass;
    BitClass bitClass;

    TableFactory TableFactory;
    List<Property> Properties = new ArrayList();
    Set<AggregateProperty> Persistents = new HashSet();
    Map<ObjectProperty,Constraint> Constraints = new HashMap();

    // последний параметр
    List<ObjectProperty> UpdateAggregations(Collection<ObjectProperty> ToUpdateProperties, DataSession Session) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)

        // update'им изменения классов
        UpdateClassChanges(Session);

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
        for(ObjectProperty UpdateProperty : UpdateList) UpdateProperty.IncrementChanges(Session);
        
        return UpdateList;
    }
    
    // проверяет Constraints
    String CheckConstraints(DataSession Session) throws SQLException {

        for(ObjectProperty Property : Constraints.keySet())
            if(Property.HasChanges(Session)) {
                String ConstraintResult = Constraints.get(Property).Check(Session,Property);
                if(ConstraintResult!=null) return ConstraintResult;
            }

        return null;
    }

    Map<ObjectProperty,Integer> getSessionChanged(DataSession Session) {

        Map<ObjectProperty,Integer> Result = new HashMap();
        for(Property Property : Properties)
            if(Property instanceof ObjectProperty)
                Result.put((ObjectProperty)Property,(Integer)((ObjectProperty)Property).SessionChanged.get(Session));

        return Result;
    }

    void setSessionChanged(DataSession Session,Map<ObjectProperty,Integer> SessionChanged) {

        for(Property Property : Properties)
            if(Property instanceof ObjectProperty) {
                Integer ChangeType = SessionChanged.get(Property);
                if(ChangeType==null)
                    ((ObjectProperty)Property).SessionChanged.remove(Session);
                else
                    ((ObjectProperty)Property).SessionChanged.put(Session,ChangeType);
            }
    }
    
    String Apply(DataSession Session) throws SQLException {
        // делается UpdateAggregations (для мн-ва persistent+constraints)
        Session.startTransaction();

        // сохраним все SessionChanged для транзакции в памяти
        Map<ObjectProperty,Integer> TransactChanged = getSessionChanged(Session);
        
        // создадим общий список из Persistents + Constraints с AggregateProperty's + DataProperties
        Collection<ObjectProperty> UpdateList = new HashSet(Persistents);
        UpdateList.addAll(Constraints.keySet());
        for(Property Property : Properties)
            if(Property instanceof DataProperty) UpdateList.add((DataProperty)Property);

        List<ObjectProperty> ChangedList = UpdateAggregations(UpdateList,Session);

        // проверим Constraints
        String Constraints = CheckConstraints(Session);
        if(Constraints!=null) {
            // откатим транзакцию
            setSessionChanged(Session,TransactChanged);
            Session.rollbackTransaction();
            return Constraints;
        }            
        
        // записываем Data, затем Persistents в таблицы из сессии
        SaveClassChanges(Session);
        
        // сохранить св-ва которые Persistent, те что входят в Persistents и DataProperty
        for(ObjectProperty Property : ChangedList)
            if(Property instanceof DataProperty || Persistents.contains(Property))
                Property.SaveChanges(Session);
        
        Session.commitTransaction();
        restartSession(Session);
        
        return null;        
    }

    void FillDB(DataSession Session) throws SQLException {
        // инициализируем таблицы
        TableFactory.FillDB(Session);

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

                PropertyField PropField = new PropertyField("prop"+PropNum.toString(),Property.GetDBType());
                Table.Properties.add(PropField);
                ((ObjectProperty)Property).Field = PropField;
            }
        }

        for(Table Table : Tables.keySet()) Session.CreateTable(Table);

        // построим в нужном порядке AggregateProperty и будем заполнять их
        List<ObjectProperty> UpdateList = new ArrayList();
        for(AggregateProperty Property : AggrProperties) Property.FillChangedList(UpdateList,null);
        Integer ViewNum = 0;
        for(ObjectProperty Property : UpdateList) {
            if(Property instanceof GroupProperty)
                ((GroupProperty)Property).FillDB(Session,ViewNum++);
        }
        
        // создадим dumb
        Table DumbTable = new Table("dumb");
        DumbTable.Keys.add(new KeyField("dumb","integer"));
        Session.CreateTable(DumbTable);
        Session.Execute("INSERT INTO dumb (dumb) VALUES (1)");
    }
    
    boolean CheckPersistent(DataSession Session) throws SQLException {
        for(AggregateProperty Property : Persistents) {
            if(!Property.CheckAggregation(Session,Property.caption))
                return false;
//            Property.Out(Adapter);
        }
        
        return true;
    }

    Map<String,PropertyObjectImplement> FillSingleViews(ObjectImplement Object,NavigatorForm Form,Set<String> Names) {
        
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
                    
                    if(Names!=null && Names.contains(DrawProp.caption))
                        Result.put(DrawProp.caption,PropertyImplement);
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

    LDP AddCProp(Object Value,Class ValueClass,Class ...Params) {
        ClassProperty Property = new ClassProperty(TableFactory,ValueClass,Value);
        LDP ListProperty = new LDP(Property);
        for(Class Int : Params) {
            ListProperty.AddInterface(Int);
        }
        Properties.add(Property);
        return ListProperty;
    }

    LSFP AddSFProp(String Formula,Integer Params) {

        StringFormulaProperty Property = new StringFormulaProperty(Formula);
        LSFP ListProperty = new LSFP(Property, integralClass,Params);
        Properties.add(Property);
        return ListProperty;
    }

    LSFP AddWSFProp(String Formula,Integer Params) {

        WhereStringFormulaProperty Property = new WhereStringFormulaProperty(Formula);
        LSFP ListProperty = new LSFP(Property, integralClass,Params);
        Properties.add(Property);
        return ListProperty;
    }

    LMFP AddMFProp(Integer Params) {
        MultiplyFormulaProperty Property = new MultiplyFormulaProperty();
        LMFP ListProperty = new LMFP(Property, integralClass,Params);
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

    LJP AddJProp(LP MainProp, int IntNum, Object ...Params) {
        JoinProperty Property = new JoinProperty(TableFactory,MainProp.Property);
        LJP ListProperty = new LJP(Property,IntNum);
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

    LJP AddUProp(int UnionType, int IntNum, Object ...Params) {
        UnionProperty Property = null;
        switch(UnionType) {
            case 0:
                Property = new MaxUnionProperty(TableFactory);
                break;
            case 1:
                Property = new SumUnionProperty(TableFactory);
                break;
            case 2:
                Property = new OverrideUnionProperty(TableFactory);
                break;
        }
        
        LJP ListProperty = new LJP(Property,IntNum);

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

    void fillData(DataAdapter Adapter) throws SQLException {
    }

    // генерирует белую БЛ
    void OpenTest(DataAdapter Adapter,boolean Classes,boolean Properties,boolean Implements,boolean Persistent,boolean Changes)  throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException  {

        if(Classes) {
            InitClasses();

            if(Implements)
                InitImplements();
                
            if(Properties) {
                InitProperties();

                if(Persistent)
                    InitPersistents();

                if(Changes) {
                    DataSession Session = createSession(Adapter);
                    FillDB(Session);
                    Session.close();

                    fillData(Adapter);
                }
            }

        }
    }
    
    // случайным образом генерирует классы
    void RandomClasses(Random Randomizer) {
        int CustomClasses = Randomizer.nextInt(20);//
        List<Class> ObjClasses = new ArrayList();
        ObjClasses.add(objectClass);
        for(int i=0;i<CustomClasses;i++) {
            Class Class = new ObjectClass(i+99993, "Случайный класс"+i);
            int Parents = Randomizer.nextInt(2) + 1;
            for(int j=0;j<Parents;j++) {
                Class.AddParent(ObjClasses.get(Randomizer.nextInt(ObjClasses.size())));
            }
            ObjClasses.add(Class);
        }
    }

    // случайным образом генерирует св-ва
    void RandomProperties(Random Randomizer) {
        
        List<Class> Classes = new ArrayList();
        objectClass.FillClassList(Classes);
        
        List<Property> RandProps = new ArrayList();
        List<ObjectProperty> RandObjProps = new ArrayList();
        
        StringFormulaProperty Dirihle = new WhereStringFormulaProperty("prm1<prm2");
        Dirihle.Interfaces.add(new StringFormulaPropertyInterface(integralClass,"prm1"));
        Dirihle.Interfaces.add(new StringFormulaPropertyInterface(integralClass,"prm2"));
        RandProps.add(Dirihle);

        MultiplyFormulaProperty Multiply = new MultiplyFormulaProperty();
        Multiply.Interfaces.add(new FormulaPropertyInterface(integralClass));
        Multiply.Interfaces.add(new FormulaPropertyInterface(integralClass));
        RandProps.add(Multiply);

        int DataPropCount = Randomizer.nextInt(15)+1;
        for(int i=0;i<DataPropCount;i++) {
            // DataProperty
            DataProperty DataProp = new DataProperty(TableFactory,Classes.get(Randomizer.nextInt(Classes.size())));
            DataProp.caption = "Data Property " + i;
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
                // JoinProperty
                JoinProperty RelProp = new JoinProperty(TableFactory,RandProps.get(Randomizer.nextInt(RandProps.size())));
                
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
                UnionProperty Property = null;
                if(PropClass==3) {
                    Property = new SumUnionProperty(TableFactory);
                    ResType = "SL";
                } else {
                if(PropClass==4) {
                    Property = new MaxUnionProperty(TableFactory);
                    ResType = "ML";
                } else {
                    Property = new OverrideUnionProperty(TableFactory);
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
                GenProp.caption = ResType + " " + i;
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
        objectClass.FillClassList(Classes);

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

        Persistents.clear();

        // сначала список получим
        List<AggregateProperty> AggrProperties = new ArrayList();
        for(Property Property : Properties) {
            if(Property instanceof AggregateProperty && Property.isObject())
                AggrProperties.add((AggregateProperty)Property);
        }
        
        int PersistentNum = Randomizer.nextInt(AggrProperties.size())+1;
        for(int i=0;i<PersistentNum;i++)
            Persistents.add(AggrProperties.get(Randomizer.nextInt(AggrProperties.size())));

//        for(AggregateProperty Property : AggrProperties)
//            if(Property.caption.equals("R 1"))
//            Persistents.add(Property);        
     }

    static int ChangeDBIteration = 0;
    void ChangeDBTest(DataAdapter Adapter,Integer MaxIterations,Random Randomizer) throws SQLException {
        
        // сначала список получим
        List<DataProperty> DataProperties = new ArrayList();
        for(Property Property : Properties) {
            if(Property instanceof DataProperty)
                DataProperties.add((DataProperty)Property);
        }

        DataSession Session = createSession(Adapter);

        List<Class> AddClasses = new ArrayList();
        objectClass.FillClassList(AddClasses);
        for(Class AddClass : AddClasses) {
            if(AddClass instanceof ObjectClass) {
                int ObjectAdd = Randomizer.nextInt(10)+1;
                for(int ia=0;ia<ObjectAdd;ia++)
                    AddObject(Session, AddClass);
            }
        }

        Apply(Session);

        long PrevTime = System.currentTimeMillis();
        
//        Randomizer.setSeed(1);
        int Iterations = 1;
        while(Iterations<MaxIterations) {

            long CurrentTime = System.currentTimeMillis();
            if(CurrentTime-PrevTime>=4000)
                break;

            PrevTime = CurrentTime;

            ChangeDBIteration = Iterations;
            System.out.println("Iteration" + Iterations++);

            // будем также рандомно создавать объекты
            AddClasses = new ArrayList();
            objectClass.FillClassList(AddClasses);
            int ObjectAdd = Randomizer.nextInt(5);
            for(int ia=0;ia<ObjectAdd;ia++) {
                Class AddClass = AddClasses.get(Randomizer.nextInt(AddClasses.size()));
                if(AddClass instanceof ObjectClass)
                    AddObject(Session, AddClass);
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
                        ValueClass = objectClass.FindClassID(ValueObject);
                    else
                        ValueClass = ChangeProp.Value;*/
                        
                    InterfaceClassSet InterfaceSet = ChangeProp.GetClassSet(null);
                    // определяем входные классы
                    InterfaceClass Classes = InterfaceSet.get(Randomizer.nextInt(InterfaceSet.size()));
                    // генерим рандомные объекты этих классов
                    Map<PropertyInterface,ObjectValue> Keys = new HashMap();
                    for(DataPropertyInterface Interface : ChangeProp.Interfaces)
                        Keys.put(Interface,new ObjectValue((Integer)Classes.get(Interface).GetRandomObject(Session,TableFactory,Randomizer,0),Classes.get(Interface)));
                    
                    Object ValueObject = null;
                    if(Randomizer.nextInt(10)<8)
                        ValueObject = ChangeProp.Value.GetRandomObject(Session,TableFactory,Randomizer,Iterations);
                    
                    ChangeProp.ChangeProperty(Keys,ValueObject,Session);
                }
            }
            
/*            for(DataProperty Property : Session.Properties) {
                Property.OutChangesTable(Adapter, Session);
            }*/
                
            Apply(Session);
            CheckPersistent(Session);
        }

        Session.close();
    }

    void autoFillDB(DataAdapter Adapter, Map<Class, Integer> ClassQuantity, Map<DataProperty, Integer> PropQuantity, Map<DataProperty, Set<DataPropertyInterface>> PropNotNull) throws SQLException {

        DataSession Session = createSession(Adapter);

        // сначала вырубим все аггрегации в конце пересчитаем
        Map<AggregateProperty,PropertyField> SavePersistents = new HashMap();
        for(AggregateProperty Property : Persistents) {
            SavePersistents.put(Property,Property.Field);
            Property.Field = null;
        }
        Persistents.clear();

        // генерируем классы
        Map<Integer,String> ObjectNames = new HashMap();
        Map<Class,List<Integer>> Objects = new HashMap();
        List<Class> Classes = new ArrayList();
        objectClass.FillClassList(Classes);

        for(Class FillClass : Classes)
            Objects.put(FillClass,new ArrayList());

        for(Class FillClass : Classes)
            if(FillClass.Childs.size()==0) {
                Integer Quantity = ClassQuantity.get(FillClass);
                if(Quantity==null) Quantity = 1;

                List<Integer> ListObjects = new ArrayList();
                for(int i=0;i<Quantity;i++) {
                    Integer idObject = AddObject(Session,FillClass);
                    ListObjects.add(idObject);
                    ObjectNames.put(idObject,FillClass.caption+" "+(i+1));
                }

                Set<Class> Parents = new HashSet();
                FillClass.fillParents(Parents);

                for(Class Class : Parents)
                    Objects.get(Class).addAll(ListObjects);
            }

        Random Randomizer = new Random(1000);

        // бежим по св-вам
        for(Property AbstractProperty : Properties)
            if(AbstractProperty instanceof DataProperty) {
                DataProperty Property = (DataProperty)AbstractProperty;

                System.out.println(Property.caption);

                Set<DataPropertyInterface> InterfaceNotNull = PropNotNull.get(Property);
                if(InterfaceNotNull==null) InterfaceNotNull = new HashSet(); 
                Integer Quantity = PropQuantity.get(Property);
                if(Quantity==null) {
                    Quantity = 1;
                    for(DataPropertyInterface Interface : Property.Interfaces)
                        if(!InterfaceNotNull.contains(Interface))
                            Quantity = Quantity * Objects.get(Interface.Class).size();

                    if(Quantity > 1)
                        Quantity = (int)(Quantity * 0.5);
                }

                Map<DataPropertyInterface,Collection<Integer>> MapInterfaces = new HashMap();
                if(PropNotNull.containsKey(Property))
                    for(DataPropertyInterface Interface : InterfaceNotNull)
                        MapInterfaces.put(Interface,Objects.get(Interface.Class));

                // сначала для всех PropNotNull генерируем все возможные Map<ы>
                for(Map<DataPropertyInterface,Integer> NotNulls : (new MapBuilder<DataPropertyInterface,Integer>()).buildCombination(MapInterfaces)) {
                    Set<Map<DataPropertyInterface,Integer>> RandomInterfaces = new HashSet();
                    while(RandomInterfaces.size()<Quantity) {
                        Map<DataPropertyInterface,Integer> RandomIteration = new HashMap();
                        for(DataPropertyInterface Interface : Property.Interfaces)
                            if(!NotNulls.containsKey(Interface)) {
                                List<Integer> ListObjects = Objects.get(Interface.Class);
                                RandomIteration.put(Interface,ListObjects.get(Randomizer.nextInt(ListObjects.size())));
                            }
                        RandomInterfaces.add(RandomIteration);
                    }

                    for(Map<DataPropertyInterface,Integer> RandomIteration : RandomInterfaces) {
                        Map<PropertyInterface,ObjectValue> Keys = new HashMap();
                        RandomIteration.putAll(NotNulls);
                        for(Map.Entry<DataPropertyInterface,Integer> InterfaceValue : RandomIteration.entrySet())
                            Keys.put(InterfaceValue.getKey(),new ObjectValue(InterfaceValue.getValue(),InterfaceValue.getKey().Class));

                        Object ValueObject = null;
                        if(Property.Value instanceof StringClass) {
                            String ObjectName = "";
                            for(DataPropertyInterface Interface : Property.Interfaces)
                                ObjectName += ObjectNames.get(RandomIteration.get(Interface)) + " ";
                            ValueObject = ObjectName;
                        } else
                            ValueObject = Property.Value.getRandomObject(Objects,Randomizer,20);
                        Property.ChangeProperty(Keys,ValueObject,Session);
                    }
                }
            }

        System.out.println("Apply");
        Apply(Session);

        Session.startTransaction();

        List<ObjectProperty> DependList = new ArrayList();
        for(AggregateProperty Property : SavePersistents.keySet()) Property.FillChangedList(DependList,null);
        // восстановим persistence, пересчитая их
        for(ObjectProperty ObjectProperty : DependList)
            if(ObjectProperty instanceof AggregateProperty && SavePersistents.containsKey(ObjectProperty)) {
                AggregateProperty Property = (AggregateProperty)ObjectProperty;

                System.out.println("Recalculate - "+Property.caption);
                
                Property.Field = SavePersistents.get(Property);
                Persistents.add(Property);
                Property.reCalculateAggregation(Session);
            }

        Session.commitTransaction();
        
        Session.close();
    }

    public void createDefaultClassForms(Class cls, NavigatorElement parent) {

        NavigatorElement node = new ClassNavigatorForm(this, cls);
        parent.addChild(node);
        cls.addRelevantElement(node);

        for (Class child : cls.Childs) {
            createDefaultClassForms(child, node);
        }
    }
}

class ClassNavigatorForm extends NavigatorForm {

    ClassNavigatorForm(BusinessLogics BL, Class cls) {
        super(cls.ID + 2134232, cls.caption);

        ObjectImplement object = new ObjectImplement(IDShift(1),cls);
        object.caption = cls.caption;

        GroupObjectImplement groupObject = new GroupObjectImplement();

        groupObject.addObject(object);
        addGroup(groupObject);
        groupObject.GID = 4;

        BL.FillSingleViews(object,this,null);

    }
}

