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
    Table table;
    Map<DataPropertyInterface,KeyField> mapFields;

    TableImplement() {
        this(null);
    }

    TableImplement(String iID) {
        ID = iID;
        childs = new HashSet<TableImplement>();
        parents = new HashSet<TableImplement>();
    }

    String ID = null;
    String getID() {

        if (ID != null) return ID;

        String result = "";
        for (DataPropertyInterface propint : this) {
            result += "_" + propint.interfaceClass.ID.toString();
        }
        return result;
    }

    // кэшированный граф
    Set<TableImplement> childs;
    Set<TableImplement> parents;

    // Operation на что сравниваем
    // 0 - не ToParent
    // 1 - ToParent
    // 2 - равно
    
    boolean recCompare(int operation,Collection<DataPropertyInterface> toCompare,ListIterator<DataPropertyInterface> iRec,Map<DataPropertyInterface,DataPropertyInterface> mapTo) {
        if(!iRec.hasNext()) return true;

        DataPropertyInterface proceedItem = iRec.next();
        for(DataPropertyInterface pairItem : toCompare) {
            if((operation ==1 && proceedItem.interfaceClass.isParent(pairItem.interfaceClass) || (operation ==0 && pairItem.interfaceClass.isParent(proceedItem.interfaceClass))) || (operation ==2 && pairItem.interfaceClass == proceedItem.interfaceClass)) {
                if(!mapTo.containsKey(pairItem)) {
                    // если parent - есть связь и нету ключа, гоним рекурсию дальше
                    mapTo.put(pairItem, proceedItem);
                    // если нашли карту выходим
                    if(recCompare(operation, toCompare,iRec, mapTo)) return true;
                    mapTo.remove(pairItem);
                }
            }
        }

        iRec.previous();
        return false;
    }
    // 0 никак не связаны, 1 - параметр снизу в дереве, 2 - параметр сверху в дереве, 3 - равно
    // также возвращает карту если 2
    int compare(Collection<DataPropertyInterface> toCompare,Map<KeyField,DataPropertyInterface> mapTo) {
        
        if(toCompare.size() != size()) return 0;

        // перебором и не будем страдать фигней
        // сначала что не 1 проверим
    
        HashMap<DataPropertyInterface,DataPropertyInterface> mapProceed = new HashMap<DataPropertyInterface, DataPropertyInterface>();
        
        ListIterator<DataPropertyInterface> iRec = (new ArrayList<DataPropertyInterface>(this)).listIterator();
        int relation = 0;
        if(recCompare(2,toCompare,iRec, mapProceed)) relation = 3;
        if(relation==0 && recCompare(0,toCompare,iRec, mapProceed)) relation = 2;
        if(relation>0) {
            if(mapTo !=null) {
                mapTo.clear();
                for(DataPropertyInterface dataInterface : toCompare)
                    mapTo.put(mapFields.get(mapProceed.get(dataInterface)),dataInterface);
            }
            
            return relation;
        }

        // MapProceed и так чистый и iRec также в начале
        if(recCompare(1,toCompare,iRec, mapProceed)) relation = 1;
        
        // !!!! должна заполнять MapTo только если уже нашла
        return relation;
    }

    void recIncludeIntoGraph(TableImplement includeItem,boolean toAdd,Set<TableImplement> checks) {
        
        if(checks.contains(this)) return;
        checks.add(this);
        
        Iterator<TableImplement> i = parents.iterator();
        while(i.hasNext()) {
            TableImplement item = i.next();
            Integer relation = item.compare(includeItem,null);
            if(relation==1) {
                // снизу в дереве
                // добавляем ее как промежуточную
                item.childs.add(includeItem);
                includeItem.parents.add(item);
                if(toAdd) {
                    item.childs.remove(this);
                    i.remove();
                }
            } else {
                // сверху в дереве или никак не связаны
                // передаем дальше
                if(relation!=3) item.recIncludeIntoGraph(includeItem,relation==2,checks);
                if(relation==2 || relation==3) toAdd = false;
            }
        }
        
        // если снизу добавляем Childs
        if(toAdd) {
            includeItem.childs.add(this);
            parents.add(includeItem);
        }
    }

    Table getTable(Collection<DataPropertyInterface> findItem,Map<KeyField,DataPropertyInterface> mapTo) {
        for(TableImplement item : parents) {
            int relation = item.compare(findItem,mapTo);
            if(relation==2 || relation==3)
                return item.getTable(findItem,mapTo);
        }
        
        return table;
    }
    
    void fillSet(Set<TableImplement> tableImplements) {
        if(!tableImplements.add(this)) return;
        for(TableImplement parent : parents) parent.fillSet(tableImplements);
    }

    void outClasses() {
        for(DataPropertyInterface propertyInterface : this)
            System.out.print(propertyInterface.interfaceClass.ID.toString()+" ");
    }
    void out() {
        //выводим себя
        System.out.print("NODE - ");
        outClasses();
        System.out.println("");
        
        for(TableImplement child : childs) {
            System.out.print("children - ");
            child.outClasses();
            System.out.println();
        }

        for(TableImplement parent : parents) {
            System.out.print("parents - ");
            parent.outClasses();
            System.out.println();
        }
        
        for(TableImplement parent : parents) parent.out();
    }
}

// таблица в которой лежат объекты
class ObjectTable extends Table {
    
    KeyField key;
    PropertyField objectClass;
    
    ObjectTable() {
        super("objects");
        key = new KeyField("object",Type.object);
        keys.add(key);
        objectClass = new PropertyField("class",Type.system);
        properties.add(objectClass);
    }
    
    Integer getClassID(DataSession session,Integer idObject) throws SQLException {
        if(idObject==null) return null;

        JoinQuery<Object,String> query = new JoinQuery<Object,String>(new ArrayList<Object>());
        Join<KeyField,PropertyField> joinTable = new Join<KeyField,PropertyField>(this);
        joinTable.joins.put(key,key.type.getExpr(idObject));
        query.and(joinTable.inJoin);
        query.properties.put("classid", joinTable.exprs.get(objectClass));
        LinkedHashMap<Map<Object,Integer>,Map<String,Object>> result = query.executeSelect(session);
        if(result.size()>0)
            return (Integer)result.values().iterator().next().get("classid");
        else
            return null;
    }

    JoinQuery<KeyField,PropertyField> getClassJoin(Class changeClass) {

        Collection<Integer> idSet = new ArrayList<Integer>();
        Collection<Class> classSet = new ArrayList<Class>();
        changeClass.fillChilds(classSet);
        for(Class childClass : classSet)
            idSet.add(childClass.ID);

        JoinQuery<KeyField,PropertyField> classQuery = new JoinQuery<KeyField,PropertyField>(keys);
        classQuery.and(new InListWhere((new Join<KeyField,PropertyField>(this,classQuery)).exprs.get(objectClass), idSet));

        return classQuery;
    }
}

// таблица счетчика sID
class IDTable extends Table {
    KeyField key;
    PropertyField value;

    IDTable() {
        super("idtable");
        key = new KeyField("id",Type.system);
        keys.add(key);

        value = new PropertyField("value",Type.system);
        properties.add(value);
    }

    public static int OBJECT = 1;
    public static int FORM = 2;

    static List<Integer> getCounters() {
        List<Integer> result = new ArrayList<Integer>();
        result.add(OBJECT);
        result.add(FORM);
        return result;
    }

    Integer generateID(DataSession dataSession, int idType) throws SQLException {

        if(BusinessLogics.autoFillDB) return BusinessLogics.autoIDCounter++;
        // читаем
        JoinQuery<KeyField,PropertyField> query = new JoinQuery<KeyField,PropertyField>(keys);
        Join<KeyField,PropertyField> joinTable = new Join<KeyField,PropertyField>(this);
        joinTable.joins.put(key,query.mapKeys.get(key));
        query.and(joinTable.inJoin);
        query.properties.put(value, joinTable.exprs.get(value));

        query.and(new CompareWhere(query.mapKeys.get(key),Type.object.getExpr(idType),CompareWhere.EQUALS));

        Integer freeID = (Integer) query.executeSelect(dataSession).values().iterator().next().get(value);

        // замещаем
        reserveID(dataSession, idType, freeID);
        return freeID+1;
    }

    void reserveID(DataSession session, int idType, Integer ID) throws SQLException {
        JoinQuery<KeyField,PropertyField> updateQuery = new JoinQuery<KeyField,PropertyField>(keys);
        updateQuery.putKeyWhere(Collections.singletonMap(key,idType));
        updateQuery.properties.put(value,value.type.getExpr(ID+1));
        session.UpdateRecords(new ModifyQuery(this,updateQuery));
    }
}

// таблица куда виды складывают свои объекты
class ViewTable extends SessionTable {
    ViewTable(Integer iObjects) {
        super("viewtable"+iObjects.toString());
        objects = new ArrayList<KeyField>();
        for(Integer i=0;i<iObjects;i++) {
            KeyField objKeyField = new KeyField("object"+i,Type.object);
            objects.add(objKeyField);
            keys.add(objKeyField);
        }

        view = new KeyField("viewid",Type.system);
        keys.add(view);
    }

    List<KeyField> objects;
    KeyField view;

    void dropViewID(DataSession session,Integer viewID) throws SQLException {
        Map<KeyField,Integer> valueKeys = new HashMap<KeyField, Integer>();
        valueKeys.put(view,viewID);
        session.deleteKeyRecords(this,valueKeys);
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

    Collection<KeyField> objects;
    KeyField property;
    PropertyField value;

    ChangeObjectTable(String tablePrefix,Integer iObjects,Type iDBType) {
        super(tablePrefix+"changetable"+iObjects+"t"+iDBType.ID);

        objects = new ArrayList<KeyField>();
        for(Integer i=0;i<iObjects;i++) {
            KeyField objKeyField = new KeyField("object"+i,Type.object);
            objects.add(objKeyField);
            keys.add(objKeyField);
        }

        property = new KeyField("property",Type.system);
        keys.add(property);

        value = new PropertyField("value",iDBType);
        properties.add(value);
    }
}

class DataChangeTable extends ChangeObjectTable {

    DataChangeTable(Integer iObjects,Type iDBType) {
        super("data",iObjects,iDBType);
    }
}

class IncrementChangeTable extends ChangeObjectTable {

    PropertyField prevValue;

    IncrementChangeTable(Integer iObjects,Type iDBType) {
        super("inc",iObjects,iDBType);

        prevValue = new PropertyField("prevvalue",iDBType);
        properties.add(prevValue);
    }
}

// таблица изменений классов
// хранит добавляение\удаление классов
class ChangeClassTable extends ChangeTable {

    KeyField objectClass;
    KeyField object;

    ChangeClassTable(String iTable) {
        super(iTable);

        object = new KeyField("object",Type.object);
        keys.add(object);

        objectClass = new KeyField("class",Type.system);
        keys.add(objectClass);
    }

    void changeClass(DataSession changeSession, Integer idObject, Collection<Class> Classes,boolean Drop) throws SQLException {

        for(Class change : Classes) {
            Map<KeyField,Integer> changeKeys = new HashMap<KeyField, Integer>();
            changeKeys.put(object,idObject);
            changeKeys.put(objectClass,change.ID);
            if(Drop) {
                if(!BusinessLogics.autoFillDB)
                    changeSession.deleteKeyRecords(this,changeKeys);
            } else
                changeSession.insertRecord(this,changeKeys,new HashMap<PropertyField, Object>());
        }
    }

    void dropSession(DataSession changeSession) throws SQLException {
        Map<KeyField,Integer> ValueKeys = new HashMap<KeyField, Integer>();
        changeSession.deleteKeyRecords(this,ValueKeys);
    }

    JoinQuery<KeyField,PropertyField> getClassJoin(DataSession changeSession,Class changeClass) {

        Collection<KeyField> objectKeys = new ArrayList<KeyField>();
        objectKeys.add(object);
        JoinQuery<KeyField,PropertyField> classQuery = new JoinQuery<KeyField,PropertyField>(objectKeys);

        Join<KeyField,PropertyField> classJoin = new Join<KeyField,PropertyField>(this);
        classJoin.joins.put(object,classQuery.mapKeys.get(object));
        classJoin.joins.put(objectClass,objectClass.type.getExpr(changeClass.ID));
        classQuery.and(classJoin.inJoin);

        return classQuery;
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

    void excludeJoin(JoinQuery<?,?> query, DataSession session,Class changeClass,SourceExpr join) {
        Join<KeyField,PropertyField> classJoin = new Join<KeyField,PropertyField>(getClassJoin(session,changeClass));
        classJoin.joins.put(object,join);
        query.and(classJoin.inJoin.not());
    }

}

class TableFactory extends TableImplement{

    ObjectTable objectTable;
    IDTable idTable;
    List<ViewTable> viewTables;
    List<Map<Type,DataChangeTable>> dataChangeTables = new ArrayList<Map<Type, DataChangeTable>>();
    List<Map<Type,IncrementChangeTable>> changeTables = new ArrayList<Map<Type, IncrementChangeTable>>();

    AddClassTable addClassTable;
    RemoveClassTable removeClassTable;

    // для отладки
    boolean reCalculateAggr = false;
    boolean crash = false;

    IncrementChangeTable getChangeTable(Integer objects, Type dbType) {
        Map<Type,IncrementChangeTable> objChangeTables = changeTables.get(objects-1);
        IncrementChangeTable table = objChangeTables.get(dbType);
        if(table==null) {
            table = new IncrementChangeTable(objects,dbType);
            objChangeTables.put(dbType,table);
        }
        return table;
    }

    DataChangeTable getDataChangeTable(Integer objects, Type dbType) {
        Map<Type,DataChangeTable> objChangeTables = dataChangeTables.get(objects-1);
        DataChangeTable table = objChangeTables.get(dbType);
        if(table==null) {
            table = new DataChangeTable(objects,dbType);
            objChangeTables.put(dbType,table);
        }
        return table;
    }

    int maxBeanObjects = 3;
    int maxInterface = 5;

    TableFactory() {
        objectTable = new ObjectTable();
        idTable = new IDTable();
        viewTables = new ArrayList<ViewTable>();

        addClassTable = new AddClassTable();
        removeClassTable = new RemoveClassTable();

        for(int i=1;i<= maxBeanObjects;i++)
            viewTables.add(new ViewTable(i));

        for(int i=0;i<maxInterface;i++)
            changeTables.add(new HashMap<Type, IncrementChangeTable>());

        for(int i=0;i<maxInterface;i++)
            dataChangeTables.add(new HashMap<Type, DataChangeTable>());
    }

    void includeIntoGraph(TableImplement IncludeItem) {
        Set<TableImplement> checks = new HashSet<TableImplement>();
        recIncludeIntoGraph(IncludeItem,true,checks);
    }

    void fillDB(DataSession session, boolean createTable) throws SQLException {
        Set<TableImplement> tableImplements = new HashSet<TableImplement>();
        fillSet(tableImplements);

        for(TableImplement node : tableImplements) {
            node.table = new Table("table"+node.getID());
            node.mapFields = new HashMap<DataPropertyInterface,KeyField>();
            Integer fieldNum = 0;
            for(DataPropertyInterface propertyInterface : node) {
                fieldNum++;
                KeyField field = new KeyField("key"+fieldNum.toString(),Type.object);
                node.table.keys.add(field);
                node.mapFields.put(propertyInterface,field);
            }
        }

        if (createTable) {

            session.createTable(objectTable);
            session.createTable(idTable);

            for (Integer idType : IDTable.getCounters()) {
                // закинем одну запись
                Map<KeyField,Integer> insertKeys = new HashMap<KeyField,Integer>();
                insertKeys.put(idTable.key, idType);
                Map<PropertyField,Object> insertProps = new HashMap<PropertyField,Object>();
                insertProps.put(idTable.value,0);
                session.insertRecord(idTable,insertKeys,insertProps);
            }
        }

    }

    // заполняет временные таблицы
    void fillSession(DataSession session) throws SQLException {

        session.createTemporaryTable(addClassTable);
        session.createTemporaryTable(removeClassTable);

        for(Map<Type, IncrementChangeTable> mapTables : changeTables)
            for(ChangeObjectTable changeTable : mapTables.values())
                session.createTemporaryTable(changeTable);
        for(Map<Type, DataChangeTable> mapTables : dataChangeTables)
            for(ChangeObjectTable dataChangeTable : mapTables.values())
                session.createTemporaryTable(dataChangeTable);

        for(ViewTable ViewTable : viewTables)
            session.createTemporaryTable(ViewTable);
    }

    // счетчик сессий (пока так потом надо из базы или как-то по другому транзакционность сделать
    void clearSession(DataSession Session) throws SQLException {

        Session.deleteKeyRecords(addClassTable,new HashMap<KeyField, Integer>());
        Session.deleteKeyRecords(removeClassTable,new HashMap<KeyField, Integer>());

        for(Map<Type, IncrementChangeTable> mapTables : changeTables)
            for(ChangeObjectTable changeTable : mapTables.values())
                Session.deleteKeyRecords(changeTable,new HashMap<KeyField, Integer>());
        for(Map<Type, DataChangeTable> mapTables : dataChangeTables)
            for(ChangeObjectTable dataChangeTable : mapTables.values())
                Session.deleteKeyRecords(dataChangeTable,new HashMap<KeyField, Integer>());
    }
}

abstract class BusinessLogics<T extends BusinessLogics<T>> implements PropertyUpdateView {

    // счетчик идентификаторов
    int idCount = 0;

    int idGet(int offs) {
        return idCount + offs;
    }

    int idShift(int offs) {
        idCount += offs;
        return idCount;
    }

    void initBase() {
        tableFactory = new TableFactory();

        objectClass = new ObjectClass(idShift(1), "Объект");
        objectClass.addParent(Class.base);

        for(int i=0;i< tableFactory.maxInterface;i++) {
            TableImplement include = new TableImplement();
            for(int j=0;j<=i;j++)
                include.add(new DataPropertyInterface(j,Class.base));
            tableFactory.includeIntoGraph(include);
        }

        baseElement = new NavigatorElement<T>(0, "Base Group");
    }

    // по умолчанию с полным стартом
    BusinessLogics() {
        initBase();

        initLogics();
        initImplements();
        initNavigators();

        initAuthentication();
    }

    public boolean toSave(Property property) {
        return property.isPersistent();
    }

    public Collection<Property> getNoUpdateProperties() {
        return new ArrayList<Property>();
    }

    static Set<Integer> wereSuspicious = new HashSet<Integer>();

    // тестирующий конструктор
    BusinessLogics(int testType) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        initBase();

        if(testType>=1) {
            initLogics();
            if(testType>=2)
                initImplements();
        }

        Integer seed;
        List<Integer> proceedSeeds = new ArrayList<Integer>();
        int[] suspicious = {888,1252,8773,9115,8700,9640,2940,4611,8038};
        if(testType>=0 || wereSuspicious.size()>=suspicious.length)
            seed = (new Random()).nextInt(10000);
        else {
            while(true) {
                seed = suspicious[(new Random()).nextInt(suspicious.length)];
                if(!wereSuspicious.contains(seed)) {
                    wereSuspicious.add(seed);
                    break;
                }
            }
        }

//        Seed = 4518;
//        Seed = 3936;
//        Seed = 8907;
//        Seed = 6646;
        if(Main.forceSeed !=null) seed = Main.forceSeed;
        System.out.println("Random seed - "+ seed);

        Random randomizer = new Random(seed);

        DataAdapter adapter = Main.getDefault();
        adapter.createDB();

        if(testType<1) {
            randomClasses(randomizer);
            randomProperties(randomizer);
        }

        if(testType<2) {
            randomImplement(randomizer);
            randomPersistent(randomizer);
        }

        DataSession session = createSession(adapter);
        fillDB(session, true);
        session.close();

        // запустить ChangeDBTest
        try {
            changeDBTest(adapter,Main.iterations,randomizer);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    AbstractGroup baseGroup = new AbstractGroup("Атрибуты");
    abstract void initGroups();
    abstract void initClasses();
    abstract void initProperties();
    abstract void initConstraints();

    // инициализируется логика
    void initLogics() {
        initGroups();
        initClasses();
        initProperties();
        initConstraints();
    }

    abstract void initPersistents();
    abstract void initTables();
    abstract void initIndexes();

    void initImplements() {
        initPersistents();
        initTables();
        initIndexes();
    }

    NavigatorElement<T> baseElement;
    abstract void initNavigators();

    AuthPolicy authPolicy = new AuthPolicy();
    abstract void initAuthentication();

    void addDataProperty(DataProperty property) {
        properties.add(property);
    }

    void setPropOrder(Property prop, Property propRel, boolean before) {

        int indProp = properties.indexOf(prop);
        int indPropRel = properties.indexOf(propRel);

        if (before) {
            if (indPropRel < indProp) {
                for (int i = indProp; i >= indPropRel + 1; i--)
                    properties.set(i, properties.get(i-1));
                properties.set(indPropRel, prop);
            }
        }
    }

    Integer addObject(DataSession session, Class objectClass) throws SQLException {

        Integer freeID = tableFactory.idTable.generateID(session, IDTable.OBJECT);

        changeClass(session,freeID,objectClass);

        return freeID;
    }

    void changeClass(DataSession session, Integer idObject, Class objectClass) throws SQLException {

        // запишем объекты, которые надо будет сохранять
        session.changeClass(idObject,objectClass);
    }

    // счетчик сессий (пока так потом надо из базы или как-то по другому транзакционность сделать
    int sessionCounter = 0;
    DataSession createSession(DataAdapter adapter) throws SQLException {
        return new DataSession(adapter, sessionCounter++, tableFactory,objectClass);
    }

    ObjectClass objectClass;

    TableFactory tableFactory;
    List<Property> properties = new ArrayList<Property>();
    Set<AggregateProperty> persistents = new HashSet<AggregateProperty>();
    Map<Property,Constraint> constraints = new HashMap<Property, Constraint>();
    Set<List<? extends Property>> indexes = new HashSet<List<? extends Property>>();

    // проверяет Constraints
    String checkConstraints(DataSession session) throws SQLException {

        for(Property property : constraints.keySet())
            if(session.propertyChanges.containsKey(property)) {
                String constraintResult = constraints.get(property).check(session,property);
                if(constraintResult!=null) return constraintResult;
            }

        return null;
    }

    public Collection<Property> getUpdateProperties() {
        Collection<Property> updateList = new HashSet<Property>(persistents);
        updateList.addAll(constraints.keySet());
        for(Property property : properties)
            if(property instanceof DataProperty) updateList.add(property);
        return updateList;
    }

    String apply(DataSession session) throws SQLException {
        // делается UpdateAggregations (для мн-ва persistent+constraints)
        session.startTransaction();

        List<Property> changedList = session.update(this,new HashSet<Class>());
        session.incrementChanges.remove(this);

        // проверим Constraints
        String constraints = checkConstraints(session);
        if(constraints !=null) {
            // откатим транзакцию
            session.rollbackTransaction();
            return constraints;
        }

        session.saveClassChanges();

        // сохранить св-ва которые Persistent, те что входят в Persistents и DataProperty
        for(Property property : changedList)
            if(property instanceof DataProperty || persistents.contains(property))
                session.propertyChanges.get(property).apply(session);
/*
        System.out.println("All Changes");
        for(List<IncrementChangeTable> ListTables : TableFactory.ChangeTables)
           for(ChangeObjectTable ChangeTable : ListTables) ChangeTable.outSelect(Session);
  */
        session.commitTransaction();
        session.restart(false);

        return null;
    }

    void fillDB(DataSession session, boolean createTable) throws SQLException {

        // инициализируем таблицы
        tableFactory.fillDB(session, createTable);

        // запишем sID'ки
        int idPropNum = 0;
        for(Property property : properties)
            property.ID = idPropNum++;

        Set<DataProperty> dataProperties = new HashSet<DataProperty>();
        Collection<AggregateProperty> aggrProperties = new ArrayList<AggregateProperty>();
        Map<Table,Integer> tables = new HashMap<Table,Integer>();

        // закинем в таблицы(создав там все что надо) св-ва
        for(Property property : properties) {
            // ChangeTable'ы заполним
            property.fillChangeTable();

            if(property instanceof DataProperty) {
                dataProperties.add((DataProperty)property);
                ((DataProperty)property).fillDataTable();
            }

            if(property instanceof AggregateProperty)
                aggrProperties.add((AggregateProperty)property);

            if(property instanceof DataProperty || (property instanceof AggregateProperty && persistents.contains(property))) {
                Table table = property.getTable(null);

                Integer propNum = tables.get(table);
                if(propNum==null) propNum = 1;
                propNum = propNum + 1;
                tables.put(table, propNum);

                PropertyField propField = new PropertyField(property.getSID(),property.getType());
                table.properties.add(propField);
                property.field = propField;
            }
        }

        //закинем индексы
        for (List<? extends Property> index : indexes) {

            Table table = index.get(0).getTable(null);

            List<PropertyField> tableIndex = new ArrayList();
            for (Property property : index) {
                tableIndex.add(property.field);
            }

            table.Indexes.add(tableIndex);
        }

        if (createTable) {

            for(Table table : tables.keySet()) session.createTable(table);

    /*        // построим в нужном порядке AggregateProperty и будем заполнять их
            List<Property> UpdateList = new ArrayList();
            for(AggregateProperty Property : AggrProperties) Property.fillChangedList(UpdateList,null,new HashSet());
            Integer ViewNum = 0;
            for(Property Property : UpdateList) {
    //            if(Property instanceof GroupProperty)
    //                ((GroupProperty)Property).FillDB(Session,ViewNum++);
            }
      */
            // создадим dumb
            Table dumbTable = new Table("dumb");
            dumbTable.keys.add(new KeyField("dumb",Type.system));
            session.createTable(dumbTable);
            session.execute("INSERT INTO dumb (dumb) VALUES (1)");

            Table emptyTable = new Table("empty");
            emptyTable.keys.add(new KeyField("dumb",Type.system));
            session.createTable(emptyTable);
        }
    }

    boolean checkPersistent(DataSession session) throws SQLException {
//        System.out.println("checking persistent...");
        for(AggregateProperty property : persistents) {
//            System.out.println(Property.caption);
            if(!property.checkAggregation(session, property.caption)) // Property.caption.equals("Расх. со скл.")
                return false;
//            Property.Out(Adapter);
        }

        return true;
    }


    // функционал по заполнению св-в по номерам, нужен для BL

    ObjectClass addObjectClass(String caption, Class... parents) {
        return addObjectClass(baseGroup, idShift(1), caption, parents);
    }

    ObjectClass addObjectClass(Integer iID, String caption, Class... parents) {
        return addObjectClass(baseGroup, iID, caption, parents);
    }

    ObjectClass addObjectClass(AbstractGroup group, Integer iID, String caption, Class... parents) {
        ObjectClass objectClass = new ObjectClass(iID, caption, parents);
        group.add(objectClass);
        return objectClass;
    }

    LDP addDProp(String caption, Class value, Class... params) {
        return addDProp((AbstractGroup)null, caption, value, params);
    }
    LDP addDProp(String sID, String caption, Class value, Class... params) {
        return addDProp(null, sID, caption, value, params);
    }
    LDP addDProp(AbstractGroup group, String caption, Class value, Class... params) {
        return addDProp(group, null, caption, value, params);
    }
    LDP addDProp(AbstractGroup group, String sID, String caption, Class value, Class... params) {
        DataProperty property = new DataProperty(tableFactory,value);
        property.sID = sID;
        property.caption = caption;
        LDP listProperty = new LDP(property);
        for(Class interfaceClass : params)
            listProperty.AddInterface(interfaceClass);
        addDataProperty(property);

        if (group != null)
            group.add(property);

        return listProperty;
    }

    void setDefProp(LDP data,LP defaultProperty,boolean onChange) {
        DataProperty property = ((DataProperty)data.property);
        property.defaultProperty = defaultProperty.property;
        for(int i=0;i<data.listInterfaces.size();i++)
            property.defaultMap.put((DataPropertyInterface)data.listInterfaces.get(i),defaultProperty.listInterfaces.get(i));

        property.onDefaultChange = onChange;
    }

    LCP addCProp(String caption, Object value, Class valueClass, Class... params) {
        ClassProperty property = new ClassProperty(tableFactory,valueClass,value);
        property.caption = caption;
        LCP listProperty = new LCP(property);
        for(Class interfaceClass : params)
            listProperty.addInterface(interfaceClass);
        properties.add(property);
        return listProperty;
    }

    LSFP addSFProp(String formula,Class value,int paramCount) {

        StringFormulaProperty property = new StringFormulaProperty(tableFactory,value,formula);
        LSFP listProperty = new LSFP(property,paramCount);
        properties.add(property);
        return listProperty;
    }


    LCFP addCFProp(int compare) {
        CompareFormulaProperty property = new CompareFormulaProperty(tableFactory,compare);
        LCFP listProperty = new LCFP(property);
        properties.add(property);
        return listProperty;
    }

    LNFP addNFProp() {
        NotNullFormulaProperty property = new NotNullFormulaProperty(tableFactory);
        LNFP listProperty = new LNFP(property);
        properties.add(property);
        return listProperty;
    }

    LMFP addMFProp(Class value,int paramCount) {
        MultiplyFormulaProperty property = new MultiplyFormulaProperty(tableFactory,value,paramCount);
        LMFP listProperty = new LMFP(property);
        properties.add(property);
        return listProperty;
    }

    LOFP addOFProp(int bitCount) {
        ObjectFormulaProperty property = new ObjectFormulaProperty(tableFactory,objectClass);
        LOFP listProperty = new LOFP(property,bitCount);
        properties.add(property);
        return listProperty;
    }

    <T extends PropertyInterface> List<PropertyInterfaceImplement> readPropImpl(LP<T,Property<T>> mainProp,Object... params) {
        List<PropertyInterfaceImplement> result = new ArrayList<PropertyInterfaceImplement>();
        int waitInterfaces = 0, mainInt = 0;
        PropertyMapImplement mapRead = null;
        LP propRead = null;
        for(Object p : params) {
            if(p instanceof Integer) {
                // число может быть как ссылкой на родной интерфейс так и
                PropertyInterface propInt = mainProp.listInterfaces.get((Integer)p-1);
                if(waitInterfaces==0) {
                    // родную берем
                    result.add(propInt);
                } else {
                    // докидываем в маппинг
                    mapRead.mapping.put(propRead.listInterfaces.get(propRead.listInterfaces.size()-waitInterfaces), propInt);
                    waitInterfaces--;
                }
            } else {
               // имплементация, типа LP
               propRead = (LP)p;
               mapRead = new PropertyMapImplement(propRead.property);
               waitInterfaces = propRead.listInterfaces.size();
               result.add(mapRead);
            }
        }

        return result;
    }

    LJP addJProp(String caption, LP mainProp, int intNum, Object... params) {
        return addJProp(null, caption, mainProp, intNum, params);
    }

    LJP addJProp(AbstractGroup group, String caption, LP mainProp, int intNum, Object... params) {
        return addJProp(group, null, caption, mainProp, intNum, params);
    }

    LJP addJProp(AbstractGroup group, String sID, String caption, LP mainProp, int intNum, Object... params) {
        JoinProperty property = new JoinProperty(tableFactory,mainProp.property);
        property.sID = sID;
        property.caption = caption;
        LJP listProperty = new LJP(property,intNum);
        int mainInt = 0;
        List<PropertyInterfaceImplement> propImpl = readPropImpl(listProperty,params);
        for(PropertyInterfaceImplement implement : propImpl) {
            property.implementations.mapping.put(mainProp.listInterfaces.get(mainInt),implement);
            mainInt++;
        }
        properties.add(property);

        if (group != null)
            group.add(property);

        return listProperty;
    }

    LGP addGProp(String caption, LP groupProp, boolean sum, Object... params) {
        return addGProp(null, caption, groupProp, sum, params);
    }

    LGP addGProp(AbstractGroup group, String caption, LP groupProp, boolean sum, Object... params) {
        return addGProp(group, null, caption, groupProp, sum, params);
    }

    LGP addGProp(AbstractGroup group, String sID, String caption, LP groupProp, boolean sum, Object... params) {

        GroupProperty property;
        if(sum)
            property = new SumGroupProperty(tableFactory,groupProp.property);
        else
            property = new MaxGroupProperty(tableFactory,groupProp.property);

        property.sID = sID;
        property.caption = caption;

        LGP listProperty = new LGP(property,groupProp);
        List<PropertyInterfaceImplement> propImpl = readPropImpl(groupProp,params);
        for(PropertyInterfaceImplement implement : propImpl) listProperty.AddInterface(implement);

        properties.add(property);

        if (group != null)
            group.add(property);

        return listProperty;
    }

    LUP addUProp(String caption, Union unionType, int intNum, Object... params) {
        return addUProp((AbstractGroup)null, caption, unionType, intNum, params);
    }

    LUP addUProp(String sID, String caption, Union unionType, int intNum, Object... params) {
        return addUProp(null, sID, caption, unionType, intNum, params);
    }

    LUP addUProp(AbstractGroup group, String caption, Union unionType, int intNum, Object... params) {
        return addUProp(group, null, caption, unionType, intNum, params);
    }

    LUP addUProp(AbstractGroup group, String sID, String caption, Union unionType, int intNum, Object... params) {
        UnionProperty property = null;
        switch(unionType) {
            case MAX:
                property = new MaxUnionProperty(tableFactory);
                break;
            case SUM:
                property = new SumUnionProperty(tableFactory);
                break;
            case OVERRIDE:
                property = new OverrideUnionProperty(tableFactory);
                break;
        }
        property.sID = sID;
        property.caption = caption;

        LUP listProperty = new LUP(property,intNum);

        for(int i=0;i<params.length/(intNum+2);i++) {
            Integer offs = i*(intNum+2);
            LP opImplement = (LP)params[offs+1];
            PropertyMapImplement operand = new PropertyMapImplement(opImplement.property);
            for(int j=0;j<intNum;j++)
                operand.mapping.put(opImplement.listInterfaces.get(((Integer)params[offs+2+j])-1),listProperty.listInterfaces.get(j));
            property.operands.add(operand);
            property.coeffs.put(operand,(Integer)params[offs]);
        }
        properties.add(property);

        if (group != null)
            group.add(property);

        return listProperty;
    }

    void fillData(DataAdapter adapter) throws SQLException {
    }

    // генерирует белую БЛ
    void openTest(DataAdapter adapter,boolean classes,boolean properties,boolean implement,boolean persistent,boolean changes)  throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException  {

        if(classes) {
            initClasses();

            if(implement)
                initImplements();

            if(properties) {
                initProperties();

                if(persistent)
                    initPersistents();

                if(changes) {
                    DataSession session = createSession(adapter);
                    fillDB(session, true);
                    session.close();

                    fillData(adapter);
                }
            }

        }
    }

    // случайным образом генерирует классы
    void randomClasses(Random randomizer) {
        int customClasses = randomizer.nextInt(20);//
        List<Class> objClasses = new ArrayList<Class>();
        objClasses.add(objectClass);
        for(int i=0;i<customClasses;i++) {
            Class objectClass = new ObjectClass(i+10000, "Случайный класс"+i);
            int parents = randomizer.nextInt(2) + 1;
            for(int j=0;j<parents;j++)
                objectClass.addParent(objClasses.get(randomizer.nextInt(objClasses.size())));
            objClasses.add(objectClass);
        }
    }

    // случайным образом генерирует св-ва
    void randomProperties(Random randomizer) {

        List<Class> classes = new ArrayList<Class>();
        objectClass.fillChilds(classes);

        List<Property> randProps = new ArrayList<Property>();
        List<Property> randObjProps = new ArrayList<Property>();
        List<Property> randIntegralProps = new ArrayList<Property>();

        CompareFormulaProperty dirihle = new CompareFormulaProperty(tableFactory,CompareWhere.LESS);
        randProps.add(dirihle);

        MultiplyFormulaProperty Multiply = new MultiplyFormulaProperty(tableFactory,Class.integer,2);
        randProps.add(Multiply);

        int dataPropCount = randomizer.nextInt(15)+1;
        for(int i=0;i<dataPropCount;i++) {
            // DataProperty
            DataProperty dataProp = new DataProperty(tableFactory,(i%4==0?Class.integer :classes.get(randomizer.nextInt(classes.size()))));
            dataProp.caption = "Data Property " + i;
            // генерируем классы
            int intCount = randomizer.nextInt(tableFactory.maxInterface)+1;
            for(int j=0;j<intCount;j++)
                dataProp.interfaces.add(new DataPropertyInterface(j,classes.get(randomizer.nextInt(classes.size()))));

            randProps.add(dataProp);
            randObjProps.add(dataProp);
            if(dataProp.getBaseClass().contains(Class.integral))
                randIntegralProps.add(dataProp);
        }

        System.out.print("Создание аггрег. св-в ");

        int propCount = randomizer.nextInt(1000)+1; //
        for(int i=0;i<propCount;i++) {
//            int RandClass = Randomizer.nextInt(10);
//            int PropClass = (RandClass>7?0:(RandClass==8?1:2));
            int propClass = randomizer.nextInt(6);
//            int PropClass = 5;
            Property genProp = null;
            String resType = "";
            if(propClass ==0) {
                // JoinProperty
                JoinProperty relProp = new JoinProperty(tableFactory,randProps.get(randomizer.nextInt(randProps.size())));

                // генерируем случайно кол-во интерфейсов
                List<PropertyInterface> relPropInt = new ArrayList<PropertyInterface>();
                int intCount = randomizer.nextInt(tableFactory.maxInterface)+1;
                for(int j=0;j<intCount;j++) {
                    JoinPropertyInterface propertyInterface = new JoinPropertyInterface(j);
                    relProp.interfaces.add(propertyInterface);
                    relPropInt.add(propertyInterface);
                }

                // чтобы 2 раза на одну и ту же ветку не натыкаться
                List<PropertyInterface> availRelInt = new ArrayList(relPropInt);
                boolean correct = true;

                for(PropertyInterface propertyInterface : (Collection<PropertyInterface>)relProp.implementations.property.interfaces) {
                    // генерируем случайно map'ы на эти интерфейсы
                    if(!(relProp.implementations.property instanceof FormulaProperty) && randomizer.nextBoolean()) {
                        if(availRelInt.size()==0) {
                            correct = false;
                            break;
                        }
                        PropertyInterface mapInterface = availRelInt.get(randomizer.nextInt(availRelInt.size()));
                        relProp.implementations.mapping.put(propertyInterface,mapInterface);
                        availRelInt.remove(mapInterface);
                    } else {
                        // другое property пока сгенерим на 1
                        PropertyMapImplement impProp = new PropertyMapImplement(randObjProps.get(randomizer.nextInt(randObjProps.size())));
                        if(impProp.property.interfaces.size()>relPropInt.size()) {
                            correct = false;
                            break;
                        }

                        List<PropertyInterface> mapRelInt = new ArrayList(relPropInt);
                        for(PropertyInterface impInterface : (Collection<PropertyInterface>)impProp.property.interfaces) {
                            PropertyInterface mapInterface = mapRelInt.get(randomizer.nextInt(mapRelInt.size()));
                            impProp.mapping.put(impInterface,mapInterface);
                            mapRelInt.remove(mapInterface);
                        }
                        relProp.implementations.mapping.put(propertyInterface,impProp);
                    }
                }

                if(correct) {
                    genProp = relProp;
                    resType = "R";
                }
            }

            if(propClass ==1 || propClass ==2) {
                // группировочное
                Property groupProp;
                GroupProperty property = null;
                if(propClass ==1) {
                    groupProp = randIntegralProps.get(randomizer.nextInt(randIntegralProps.size()));
                    property = new SumGroupProperty(tableFactory,groupProp);
                    resType = "SG";
                } else {
                    groupProp = randObjProps.get(randomizer.nextInt(randObjProps.size()));
                    property = new MaxGroupProperty(tableFactory,groupProp);
                    resType = "MG";
                }

                boolean correct = true;
                List<PropertyInterface> groupInt = new ArrayList(groupProp.interfaces);
                int groupCount = randomizer.nextInt(tableFactory.maxInterface)+1;
                for(int j=0;j<groupCount;j++) {
                    PropertyInterfaceImplement implement;
                    // генерируем случайно map'ы на эти интерфейсы
                    if(randomizer.nextBoolean())
                        implement = groupInt.get(randomizer.nextInt(groupInt.size()));
                    else {
                        // другое property пока сгенерим на 1
                        PropertyMapImplement impProp = new PropertyMapImplement(randObjProps.get(randomizer.nextInt(randObjProps.size())));
                        if(impProp.property.interfaces.size()>groupInt.size()) {
                            correct = false;
                            break;
                        }

                        List<PropertyInterface> mapRelInt = new ArrayList(groupInt);
                        for(PropertyInterface impInterface : (Collection<PropertyInterface>)impProp.property.interfaces) {
                            PropertyInterface mapInterface = mapRelInt.get(randomizer.nextInt(mapRelInt.size()));
                            impProp.mapping.put(impInterface,mapInterface);
                            mapRelInt.remove(mapInterface);
                        }
                        implement = impProp;
                    }

                    property.interfaces.add(new GroupPropertyInterface(j,implement));
                }

                if(correct)
                    genProp = property;
            }

            if(propClass ==3 || propClass ==4 || propClass ==5) {
                UnionProperty property = null;
                List<Property> randValProps = randObjProps;
                if(propClass ==3) {
                    randValProps = randIntegralProps;
                    property = new SumUnionProperty(tableFactory);
                    resType = "SL";
                } else {
                if(propClass ==4) {
                    property = new MaxUnionProperty(tableFactory);
                    resType = "ML";
                } else {
                    property = new OverrideUnionProperty(tableFactory);
                    resType = "OL";
                }
                }

                int opIntCount = randomizer.nextInt(tableFactory.maxInterface)+1;
                for(int j=0;j<opIntCount;j++)
                    property.interfaces.add(new PropertyInterface(j));

                boolean correct = true;
                List<PropertyInterface> opInt = new ArrayList(property.interfaces);
                int opCount = randomizer.nextInt(4)+1;
                for(int j=0;j<opCount;j++) {
                    PropertyMapImplement operand = new PropertyMapImplement(randValProps.get(randomizer.nextInt(randValProps.size())));
                    if(operand.property.interfaces.size()!=opInt.size()) {
                        correct = false;
                        break;
                    }

                    List<PropertyInterface> mapRelInt = new ArrayList(opInt);
                    for(PropertyInterface impInterface : (Collection<PropertyInterface>)operand.property.interfaces) {
                        PropertyInterface mapInterface = mapRelInt.get(randomizer.nextInt(mapRelInt.size()));
                        operand.mapping.put(impInterface,mapInterface);
                        mapRelInt.remove(mapInterface);
                    }
                    property.operands.add(operand);
                }

                if(correct)
                    genProp = property;
            }


            if(genProp!=null && !genProp.getBaseClass().isEmpty()) {
                genProp.caption = resType + " " + i;
                // проверим что есть в интерфейсе и покрыты все ключи
                Iterator<InterfaceClass<?>> ic = genProp.getClassSet(ClassSet.universal).iterator();
                if(ic.hasNext() && ic.next().keySet().size()==genProp.interfaces.size()) {
                    System.out.print(resType+"-");
                    randProps.add(genProp);
                    randObjProps.add(genProp);
                    if(genProp.getBaseClass().contains(Class.integral))
                        randIntegralProps.add(genProp);
                }
            }
        }

        properties.addAll(randProps);

        System.out.println();
    }

    // случайным образом генерирует имплементацию
    void randomImplement(Random randomizer) {
        List<Class> classes = new ArrayList<Class>();
        objectClass.fillChilds(classes);

        // заполнение физ модели
        int implementCount = randomizer.nextInt(8);
        for(int i=0;i<implementCount;i++) {
            TableImplement include = new TableImplement();
            int objCount = randomizer.nextInt(3)+1;
            for(int ioc=0;ioc<objCount;ioc++)
                include.add(new DataPropertyInterface(ioc,classes.get(randomizer.nextInt(classes.size()))));
            tableFactory.includeIntoGraph(include);
        }
    }

    // случайным образом генерирует постоянные аггрегации
    void randomPersistent(Random Randomizer) {

        persistents.clear();

        // сначала список получим
        List<AggregateProperty> aggrProperties = new ArrayList<AggregateProperty>();
        for(Property property : properties) {
            if(property instanceof AggregateProperty && property.isObject())
                aggrProperties.add((AggregateProperty)property);
        }

        int persistentNum = Randomizer.nextInt(aggrProperties.size())+1;
        for(int i=0;i<persistentNum;i++)
            persistents.add(aggrProperties.get(Randomizer.nextInt(aggrProperties.size())));

//        for(AggregateProperty Property : AggrProperties)
//            if(Property.caption.equals("R 1"))
//            Persistents.add(Property);
     }

    static int changeDBIteration = 0;
    void changeDBTest(DataAdapter adapter,Integer maxIterations,Random randomizer) throws SQLException {

        // сначала список получим
        List<DataProperty> dataProperties = new ArrayList<DataProperty>();
        for(Property property : properties) {
            if(property instanceof DataProperty)
                dataProperties.add((DataProperty)property);
        }

        DataSession session = createSession(adapter);

        List<Class> addClasses = new ArrayList<Class>();
        objectClass.fillChilds(addClasses);
        for(Class addClass : addClasses) {
            if(addClass instanceof ObjectClass) {
                int objectAdd = randomizer.nextInt(10)+1;
                for(int ia=0;ia<objectAdd;ia++)
                    addObject(session, addClass);
            }
        }

        apply(session);

        long prevTime = System.currentTimeMillis();

//        Randomizer.setSeed(1);
        int iterations = 1;
        while(iterations<maxIterations) {

            long currentTime = System.currentTimeMillis();
            if(currentTime-prevTime>=40000)
                break;

            prevTime = currentTime;

            changeDBIteration = iterations;
            System.out.println("Iteration" + iterations++);

            // будем также рандомно создавать объекты
            addClasses = new ArrayList<Class>();
            objectClass.fillChilds(addClasses);
            int objectAdd = randomizer.nextInt(5);
            for(int ia=0;ia<objectAdd;ia++) {
                Class addClass = addClasses.get(randomizer.nextInt(addClasses.size()));
                if(addClass instanceof ObjectClass)
                    addObject(session, addClass);
            }

            int propertiesChanged = randomizer.nextInt(8)+1;
            for(int ip=0;ip<propertiesChanged;ip++) {
                // берем случайные n св-в
                DataProperty<?> changeProp = dataProperties.get(randomizer.nextInt(dataProperties.size()));
                int numChanges = randomizer.nextInt(3)+1;
                for(int in=0;in<numChanges;in++) {
/*                    // теперь определяем класс найденного объекта
                    Class valueClass = null;
                    if(ChangeProp.Value instanceof ObjectClass)
                        valueClass = objectClass.FindClassID(ValueObject);
                    else
                        valueClass = ChangeProp.Value;*/

                    // определяем входные классы
                    InterfaceClass<DataPropertyInterface> interfaceClasses = CollectionExtend.getRandom(changeProp.getClassSet(ClassSet.universal), randomizer);
                    // генерим рандомные объекты этих классов
                    Map<DataPropertyInterface,ObjectValue> keys = new HashMap<DataPropertyInterface, ObjectValue>();
                    for(DataPropertyInterface propertyInterface : changeProp.interfaces) {
                        Class randomClass = interfaceClasses.get(propertyInterface).getRandom(randomizer);
                        keys.put(propertyInterface,new ObjectValue((Integer) randomClass.getRandomObject(session, tableFactory, 0, randomizer),randomClass));
                    }

                    Object valueObject = null;
                    if(randomizer.nextInt(10)<8)
                        valueObject = changeProp.value.getRandomObject(session, tableFactory, iterations, randomizer);

                    changeProp.changeProperty(keys, valueObject, false, session, null);
                }
            }

/*            for(DataProperty Property : Session.propertyViews) {
                Property.OutChangesTable(Adapter, Session);
            }*/

            apply(session);
            checkPersistent(session);
        }

        session.close();
    }

    static List<Property> getChangedList(Collection<? extends Property> updateProps,DataChanges changes,Collection<Property> noUpdateProps) {
        List<Property> changedList = new ArrayList<Property>();
        for(Property property : updateProps)
            property.fillChangedList(changedList,changes,noUpdateProps);
        return changedList;
    }

    // флаг для оптимизации
    Map<DataProperty,Integer> autoQuantity(Integer quantity,LDP... properties) {
        Map<DataProperty,Integer> result = new HashMap<DataProperty,Integer>();
        for(LDP<?> property : properties)
            result.put(property.property,quantity);
        return result;
    }

    static boolean autoFillDB = false;
    static int autoIDCounter = 0;
    static int AutoSeed = 1400;
    void autoFillDB(DataAdapter adapter, Map<Class, Integer> classQuantity, Map<DataProperty, Integer> propQuantity, Map<DataProperty, Set<DataPropertyInterface>> propNotNull) throws SQLException {

        autoFillDB = true;
        DataSession session = createSession(adapter);

        // сначала вырубим все аггрегации в конце пересчитаем
        Map<AggregateProperty,PropertyField> savePersistents = new HashMap<AggregateProperty, PropertyField>();
        for(AggregateProperty property : persistents) {
            savePersistents.put(property,property.field);
            property.field = null;
        }
        persistents.clear();

        // генерируем классы
        Map<Integer,String> objectNames = new HashMap<Integer, String>();
        Map<Class,List<Integer>> objects = new HashMap<Class, List<Integer>>();
        List<Class> classes = new ArrayList<Class>();
        objectClass.fillChilds(classes);

        for(Class fillClass : classes)
            objects.put(fillClass,new ArrayList<Integer>());

        for(Class fillClass : classes)
            if(fillClass.childs.size()==0) {
                System.out.println(fillClass.caption);

                Integer quantity = classQuantity.get(fillClass);
                if(quantity==null) quantity = 1;

                List<Integer> listObjects = new ArrayList<Integer>();
                for(int i=0;i<quantity;i++) {
                    Integer idObject = addObject(session,fillClass);
                    listObjects.add(idObject);
                    objectNames.put(idObject,fillClass.caption+" "+(i+1));
                }

                Set<ObjectClass> parents = new HashSet<ObjectClass>();
                fillClass.fillParents(parents);

                for(ObjectClass objectClass : parents)
                    objects.get(objectClass).addAll(listObjects);
            }

        Random randomizer = new Random(AutoSeed);

        // бежим по св-вам
        for(Property abstractProperty : properties)
            if(abstractProperty instanceof DataProperty) {
                DataProperty<?> property = (DataProperty)abstractProperty;

                System.out.println(property.caption);

                Set<DataPropertyInterface> interfaceNotNull = propNotNull.get(property);
                if(interfaceNotNull==null) interfaceNotNull = new HashSet<DataPropertyInterface>();
                Integer quantity = propQuantity.get(property);
                if(quantity==null) {
                    quantity = 1;
                    for(DataPropertyInterface propertyInterface : property.interfaces)
                        if(!interfaceNotNull.contains(propertyInterface))
                            quantity = quantity * objects.get(propertyInterface.interfaceClass).size();

                    if(quantity > 1)
                        quantity = (int)(quantity * 0.5);
                }

                Map<DataPropertyInterface,Collection<Integer>> mapInterfaces = new HashMap<DataPropertyInterface, Collection<Integer>>();
                if(propNotNull.containsKey(property))
                    for(DataPropertyInterface propertyInterface : interfaceNotNull)
                        mapInterfaces.put(propertyInterface,objects.get(propertyInterface.interfaceClass));

                // сначала для всех PropNotNull генерируем все возможные Map<ы>
                for(Map<DataPropertyInterface,Integer> notNulls : new Combinations<DataPropertyInterface,Integer>(mapInterfaces)) { //
                    int randomInterfaces = 0;
                    while(randomInterfaces<quantity) {
                        Map<DataPropertyInterface,Integer> randomIteration = new HashMap<DataPropertyInterface, Integer>();
                        for(DataPropertyInterface propertyInterface : property.interfaces)
                            if(!notNulls.containsKey(propertyInterface)) {
                                List<Integer> listObjects = objects.get(propertyInterface.interfaceClass);
                                randomIteration.put(propertyInterface,listObjects.get(randomizer.nextInt(listObjects.size())));
                            }

                        Map<DataPropertyInterface,ObjectValue> keys = new HashMap<DataPropertyInterface, ObjectValue>();
                        randomIteration.putAll(notNulls);
                        for(Map.Entry<DataPropertyInterface,Integer> interfaceValue : randomIteration.entrySet())
                            keys.put(interfaceValue.getKey(),new ObjectValue(interfaceValue.getValue(),interfaceValue.getKey().interfaceClass));

                        Object valueObject = null;
                        if(property.value instanceof StringClass) {
                            String objectName = "";
                            for(DataPropertyInterface propertyInterface : property.interfaces)
                                objectName += objectNames.get(randomIteration.get(propertyInterface)) + " ";
                            valueObject = objectName;
                        } else
                            valueObject = property.value.getRandomObject(objects,randomizer,20);
                        property.changeProperty(keys,valueObject, false, session, null);
                        randomInterfaces++;
                    }
                }
            }

        System.out.println("Apply");
        apply(session);

        session.startTransaction();

        // восстановим persistence, пересчитая их
        for(Property dependProperty : getChangedList(savePersistents.keySet(),null,new HashSet<Property>()))
            if(dependProperty instanceof AggregateProperty && savePersistents.containsKey(dependProperty)) {
                AggregateProperty property = (AggregateProperty)dependProperty;

                System.out.println("Recalculate - "+property.caption);

                property.field = savePersistents.get(property);
                persistents.add(property);
                property.reCalculateAggregation(session);
            }

        session.commitTransaction();

        tableFactory.idTable.reserveID(session,IDTable.OBJECT, autoIDCounter);

        session.close();

        autoFillDB = false;
    }

    public void createDefaultClassForms(Class cls, NavigatorElement parent) {

        NavigatorElement node = new ClassNavigatorForm(this, cls);
        parent.add(node);

        // Проверим, что такой формы еще не было
        boolean found = false;
        for (NavigatorElement relNode : cls.relevantElements)
            if (relNode.ID == node.ID) { found = true; break; }
        if (!found)
            cls.addRelevantElement(node);

        for (Class child : cls.childs) {
            createDefaultClassForms(child, node);
        }
    }

    // -------------------------------------- Старые интерфейсы --------------------------------------------------- //

    Map<String,PropertyObjectImplement> fillSingleViews(ObjectImplement object,NavigatorForm form,Set<String> names) {

        Map<String,PropertyObjectImplement> result = new HashMap<String, PropertyObjectImplement>();

        for(Property drawProp : properties) {
            if(drawProp.interfaces.size() == 1) {
                // проверим что дает хоть одно значение
                InterfaceClass interfaceClass = new InterfaceClass();
                interfaceClass.put(((Collection<PropertyInterface>)drawProp.interfaces).iterator().next(),ClassSet.getUp(object.baseClass));
                if(!drawProp.getValueClass(interfaceClass).isEmpty()) {
                    PropertyObjectImplement propertyImplement = new PropertyObjectImplement(drawProp);
                    propertyImplement.mapping.put((PropertyInterface)drawProp.interfaces.iterator().next(),object);
                    form.propertyViews.add(new PropertyView(form.IDShift(1),propertyImplement,object.groupTo));

                    if(names!=null && names.contains(drawProp.caption))
                        result.put(drawProp.caption,propertyImplement);
                }
            }
        }

        return result;
    }

    PropertyObjectImplement addPropertyView(NavigatorForm fbv,LP listProp,GroupObjectImplement gv,ObjectImplement... params) {
        PropertyObjectImplement propImpl = new PropertyObjectImplement(listProp.property);

        ListIterator<PropertyInterface> i = listProp.listInterfaces.listIterator();
        for(ObjectImplement object : params)
            propImpl.mapping.put(i.next(),object);
        fbv.propertyViews.add(new PropertyView(fbv.IDShift(1),propImpl,gv));
        return propImpl;
    }

}

class ClassNavigatorForm extends NavigatorForm {

    ClassNavigatorForm(BusinessLogics BL, Class cls) {
        super(cls.ID + 2134232, cls.caption);

        ObjectImplement object = new ObjectImplement(IDShift(1),cls);
        object.caption = cls.caption;

        GroupObjectImplement groupObject = new GroupObjectImplement(IDShift(1));

        groupObject.addObject(object);
        addGroup(groupObject);

        addPropertyView(BL.properties, BL.baseGroup, true, object);
    }
}

class LP<T extends PropertyInterface,P extends Property<T>> {

    LP(P iProperty) {
        this(iProperty, new ArrayList<T>());
    }

    LP(P iProperty, List<T> iListInterfaces) {
        property =iProperty;
        listInterfaces = iListInterfaces;
    }

    P property;
    List<T> listInterfaces;
}

class LCP extends LP<DataPropertyInterface,ClassProperty> {

    LCP(ClassProperty iProperty) {super(iProperty);}

    void addInterface(Class inClass) {
        DataPropertyInterface propertyInterface = new DataPropertyInterface(listInterfaces.size(),inClass);
        listInterfaces.add(propertyInterface);
        property.interfaces.add(propertyInterface);
    }
}

class LDP<D extends PropertyInterface> extends LP<DataPropertyInterface,DataProperty<D>> {

    LDP(DataProperty<D> iProperty) {super(iProperty);}

    void AddInterface(Class inClass) {
        DataPropertyInterface propertyInterface = new DataPropertyInterface(listInterfaces.size(),inClass);
        listInterfaces.add(propertyInterface);
        property.interfaces.add(propertyInterface);
    }

    void ChangeProperty(DataSession session, Object value, Integer... iParams) throws SQLException {
        Map<DataPropertyInterface,ObjectValue> keys = new HashMap<DataPropertyInterface,ObjectValue>();
        Integer intNum = 0;
        for(int i : iParams) {
            DataPropertyInterface propertyInterface = listInterfaces.get(intNum);
            keys.put(propertyInterface,new ObjectValue(i,propertyInterface.interfaceClass));
            intNum++;
        }

        property.changeProperty(keys, value, false, session, null);
    }

    void putNotNulls(Map<DataProperty,Set<DataPropertyInterface>> propNotNulls,Integer... iParams) {
        Set<DataPropertyInterface> interfaceNotNulls = new HashSet<DataPropertyInterface>();
        for(Integer iInterface : iParams)
            interfaceNotNulls.add(listInterfaces.get(iInterface));

        propNotNulls.put(property,interfaceNotNulls);
    }
}

class LSFP extends LP<StringFormulaPropertyInterface,StringFormulaProperty> {

    LSFP(StringFormulaProperty iProperty,int paramCount) {
        super(iProperty);
        for(int i=0;i<paramCount;i++) {
            StringFormulaPropertyInterface propertyInterface = new StringFormulaPropertyInterface(listInterfaces.size());
            listInterfaces.add(propertyInterface);
            property.interfaces.add(propertyInterface);
        }
    }
}

class LCFP extends LP<FormulaPropertyInterface,CompareFormulaProperty> {

    LCFP(CompareFormulaProperty iProperty) {
        super(iProperty);
        listInterfaces.add(property.operator1);
        listInterfaces.add(property.operator2);
    }
}

class LNFP extends LP<FormulaPropertyInterface,NotNullFormulaProperty> {

    LNFP(NotNullFormulaProperty iProperty) {
        super(iProperty);
        listInterfaces.add(property.property);
    }
}

class LMFP extends LP<StringFormulaPropertyInterface,MultiplyFormulaProperty> {

    LMFP(MultiplyFormulaProperty iProperty) {
        super(iProperty);
        listInterfaces.addAll(property.interfaces);
    }
}

class LOFP extends LP<FormulaPropertyInterface,ObjectFormulaProperty> {

    LOFP(ObjectFormulaProperty iProperty,int bitCount) {
        super(iProperty);
        listInterfaces.add(property.objectInterface);
        for(int i=0;i<bitCount;i++) {
            FormulaPropertyInterface propertyInterface = new FormulaPropertyInterface(listInterfaces.size());
            listInterfaces.add(propertyInterface);
            property.interfaces.add(propertyInterface);
        }
    }
}

class LJP<T extends PropertyInterface> extends LP<JoinPropertyInterface,JoinProperty<T>> {

    LJP(JoinProperty<T> iProperty,int objects) {
        super(iProperty);
        for(int i=0;i<objects;i++) {
            JoinPropertyInterface propertyInterface = new JoinPropertyInterface(i);
            listInterfaces.add(propertyInterface);
            property.interfaces.add(propertyInterface);
        }
    }
}

class LUP extends LP<PropertyInterface,UnionProperty> {

    LUP(UnionProperty iProperty,int objects) {
        super(iProperty);
        for(int i=0;i<objects;i++) {
            JoinPropertyInterface propertyInterface = new JoinPropertyInterface(i);
            listInterfaces.add(propertyInterface);
            property.interfaces.add(propertyInterface);
        }
    }
}

class LGP<T extends PropertyInterface> extends LP<GroupPropertyInterface<T>,GroupProperty<T>> {

    LP<T,?> groupProperty;
    LGP(GroupProperty<T> iProperty,LP<T,?> iGroupProperty) {
        super(iProperty);
        groupProperty = iGroupProperty;
    }

    void AddInterface(PropertyInterfaceImplement<T> implement) {
        GroupPropertyInterface<T> propertyInterface = new GroupPropertyInterface<T>(listInterfaces.size(),implement);
        listInterfaces.add(propertyInterface);
        property.interfaces.add(propertyInterface);
    }
}

/*
    List<PropertyView> GetPropViews(RemoteForm fbv, Property prop) {

        List<PropertyView> result = new ArrayList();

        for (PropertyView propview : fbv.propertyViews)
            if (propview.View.Property == prop) result.add(propview);

        return result;
    }

    // "СЂРёСЃСѓРµС‚" РєР»Р°СЃСЃ, СЃРѕ РІСЃРµРјРё СЃРІ-РІР°РјРё
    void DisplayClasses(DataAdapter Adapter, DataPropertyInterface[] ToDraw) throws SQLException {

        Map<DataPropertyInterface,SourceExpr> JoinSources = new HashMap<DataPropertyInterface,SourceExpr>();
        SelectQuery SimpleSelect = new SelectQuery(null);
        FromTable PrevSelect = null;
        for(int ic=0;ic<ToDraw.length;ic++) {
            FromTable Select = TableFactory.ObjectTable.ClassSelect(ToDraw[ic].Class);
            Select.JoinType = "FULL";
            if(PrevSelect==null)
                SimpleSelect.From = Select;
            else
                PrevSelect.Joins.add(Select);

            PrevSelect = Select;
            JoinSources.put(ToDraw[ic],new FieldSourceExpr(Select,TableFactory.ObjectTable.Key.Name));
        }

        JoinList Joins=new JoinList();

        Integer SelFields = 0;
        Iterator<Property> i = propertyViews.iterator();
        while(i.hasNext()) {
            Property Prop = i.next();

            MapBuilder<PropertyInterface,DataPropertyInterface> mb= new MapBuilder<PropertyInterface,DataPropertyInterface>();
            List<Map<PropertyInterface,DataPropertyInterface>> Maps = mb.BuildMap((PropertyInterface[])Prop.Interfaces.toArray(new PropertyInterface[0]), ToDraw);
            // РїРѕРїСЂРѕР±СѓРµРј РІСЃРµ РІР°СЂРёР°РЅС‚С‹ РѕС‚РѕР±СЂР°Р¶РµРЅРёСЏ
            Iterator<Map<PropertyInterface,DataPropertyInterface>> im = Maps.iterator();
            while(im.hasNext()) {
                Map<PropertyInterface,DataPropertyInterface> Impl = im.next();
                Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();

                ClassInterface ClassImplement = new ClassInterface();
                Iterator<PropertyInterface> ip = Prop.Interfaces.iterator();
                while(ip.hasNext()) {
                    PropertyInterface Interface = ip.next();
                    DataPropertyInterface MapInterface = Impl.get(Interface);
                    ClassImplement.put(Interface,MapInterface.Class);
                    JoinImplement.put(Interface,JoinSources.get(MapInterface));
                }

                if(Prop.GetValueClass(ClassImplement)!=null) {
                    // С‚Рѕ РµСЃС‚СЊ Р°РєС‚СѓР°Р»СЊРЅРѕРµ СЃРІ-РІРѕ
                    SimpleSelect.Expressions.put("test"+(SelFields++).toString(),Prop.JoinSelect(Joins,JoinImplement,false));
                }
            }
        }

        Iterator<From> ij = Joins.iterator();
        while(ij.hasNext()) {
            From Join = ij.next();
            Join.JoinType = "LEFT";
            PrevSelect.Joins.add(Join);
        }

        Adapter.OutSelect(SimpleSelect);
    }
*/
