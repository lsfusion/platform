package platform.server.logics;

import platform.base.CollectionExtend;
import platform.base.Combinations;
import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.interop.RemoteLogicsInterface;
import platform.interop.exceptions.LoginException;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.server.data.*;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.Join;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.auth.AuthPolicy;
import platform.server.logics.auth.User;
import platform.server.logics.classes.ObjectClass;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.StringClass;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.constraints.Constraint;
import platform.server.logics.data.IDTable;
import platform.server.logics.data.TableFactory;
import platform.server.logics.data.ImplementTable;
import platform.server.logics.properties.*;
import platform.server.logics.properties.groups.AbstractGroup;
import platform.server.logics.properties.linear.*;
import platform.server.logics.session.DataChanges;
import platform.server.logics.session.DataSession;
import platform.server.logics.session.PropertyUpdateView;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.PropertyView;
import platform.server.view.navigator.ClassNavigatorForm;
import platform.server.view.navigator.NavigatorElement;
import platform.server.view.navigator.NavigatorForm;
import platform.server.view.navigator.RemoteNavigator;

import java.sql.SQLException;
import java.util.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.io.*;

import net.sf.jasperreports.engine.JRException;

public abstract class BusinessLogics<T extends BusinessLogics<T>> extends UnicastRemoteObject implements PropertyUpdateView, RemoteLogicsInterface {

    protected DataAdapter adapter;
    public static boolean activateCaches = true;

    public RemoteNavigatorInterface createNavigator(String login, String password) {

        User user = authPolicy.getUser(login, password);
        if (user == null) throw new LoginException();

        try {
            return new RemoteNavigator(adapter, this, user);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

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
        objectClass.addParent(RemoteClass.base);

        baseElement = new NavigatorElement<T>(0, "Base Group");
    }

    // по умолчанию с полным стартом
    public BusinessLogics(DataAdapter iAdapter) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super();

        adapter = iAdapter;

        initBase();

        initLogics();
        initImplements();
        initNavigators();

        initAuthentication();

        synchronizeDB();
    }

    public boolean toSave(Property property) {
        return property.isStored();
    }

    public Collection<Property> getNoUpdateProperties() {
        return new ArrayList<Property>();
    }

    static Set<Integer> wereSuspicious = new HashSet<Integer>();

    // тестирующий конструктор
    public BusinessLogics(DataAdapter iAdapter,int testType,Integer seed,int iterations) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException {
        super();

        adapter = iAdapter;

        initBase();

        if(testType>=1) {
            initLogics();
            if(testType>=2)
                initImplements();
        }

        if(seed==null) {
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
        }

        System.out.println("Random seed - "+ seed);

        Random randomizer = new Random(seed);

        if(testType<1) {
            randomClasses(randomizer);
            randomProperties(randomizer);
        }

        if(testType<2) {
            randomImplement(randomizer);
            randomPersistent(randomizer);
        }

        // запустить ChangeDBTest
        try {
            changeDBTest(iterations,randomizer);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public AbstractGroup baseGroup = new AbstractGroup("Атрибуты");
    protected abstract void initGroups();
    protected abstract void initClasses();
    protected abstract void initProperties();
    protected abstract void initConstraints();

    // инициализируется логика
    void initLogics() {
        initGroups();
        initClasses();
        initProperties();
        initConstraints();
    }

    protected abstract void initPersistents();
    protected abstract void initTables();
    protected abstract void initIndexes();

    void initImplements() {
        initPersistents();
        initTables();
        initIndexes();
    }

    public NavigatorElement<T> baseElement;
    protected abstract void initNavigators() throws JRException, FileNotFoundException;

    public AuthPolicy authPolicy = new AuthPolicy();
    protected abstract void initAuthentication();

    String genSID() {
        return "prop" + properties.size();
    }

    protected void setPropOrder(Property prop, Property propRel, boolean before) {

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

    public Integer addObject(DataSession session, RemoteClass objectClass) throws SQLException {

        Integer freeID = tableFactory.idTable.generateID(session, IDTable.OBJECT);

        changeClass(session,freeID,objectClass);

        return freeID;
    }

    public void changeClass(DataSession session, Integer idObject, RemoteClass objectClass) throws SQLException {

        // запишем объекты, которые надо будет сохранять
        session.changeClass(idObject,objectClass);
    }

    // счетчик сессий (пока так потом надо из базы или как-то по другому транзакционность сделать
    int sessionCounter = 0;
    public DataSession createSession(DataAdapter adapter) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new DataSession(adapter, sessionCounter++, tableFactory,objectClass);
    }

    public ObjectClass objectClass;

    public TableFactory tableFactory;
    public List<Property> properties = new ArrayList<Property>();
    protected Set<AggregateProperty> persistents = new HashSet<AggregateProperty>();
    Map<Property, Constraint> constraints = new HashMap<Property, Constraint>();
    protected Set<List<? extends Property>> indexes = new HashSet<List<? extends Property>>();

    // проверяет Constraints
    String checkConstraints(DataSession session) throws SQLException {

        for(Property property : constraints.keySet())
            if(session.propertyChanges.containsKey(property)) {
                String constraintResult = constraints.get(property).check(session,property);
                if(constraintResult!=null) return constraintResult;
            }

        return null;
    }

    public Collection<Property> getStoredProperties() {
        Collection<Property> result = new ArrayList<Property>(persistents);
        for(Property property : properties)
            if(property instanceof DataProperty) result.add(property);
        return result;
    }

    public Collection<Property> getUpdateProperties() {
        return BaseUtils.join(getStoredProperties(),constraints.keySet());
    }

    public String apply(DataSession session) throws SQLException {
        // делается UpdateAggregations (для мн-ва persistent+constraints)
        session.startTransaction();

        List<Property> changedList = session.update(this,new HashSet<RemoteClass>());
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
            if(property.isStored())
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

    public void synchronizeDB() throws SQLException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        DataSession session = createSession(adapter);

        session.startTransaction();

        // запишем sID'ки
        int idPropNum = 0;
        for(Property property : properties) {
            property.ID = idPropNum++;
            property.fillChangeTable();
        }

        // инициализируем таблицы
        tableFactory.fillDB(session);
        
        // "старое" состояние базы
        DataInputStream inputDB = null;
        byte[] struct = (byte[]) session.readRecord(tableFactory.globalTable,new HashMap<KeyField, Integer>(),tableFactory.globalTable.struct);
        if(struct!=null) inputDB = new DataInputStream(new ByteArrayInputStream(struct));
/*        try {
            FileInputStream inputDBFile = new FileInputStream("prevstruct.str");
            byte[] readInput = new byte[inputDBFile.read()*255+inputDBFile.read()];
            inputDBFile.read(readInput);
            inputDB = new DataInputStream(new ByteArrayInputStream(readInput));
        } catch (FileNotFoundException e) { 
        }*/

        // новое состояние базы
        ByteArrayOutputStream outDBStruct = new ByteArrayOutputStream();
        DataOutputStream outDB = new DataOutputStream(outDBStruct);

        Collection<Property> storedProperties = new ArrayList<Property>(getStoredProperties());
        // привяжем к таблицам все свойства
        for(Property property : storedProperties)
            property.markStored();

        Map<String, ImplementTable> implementTables = tableFactory.getImplementTables();

        Map<ImplementTable,Set<List<String>>> mapIndexes = new HashMap<ImplementTable, Set<List<String>>>();
        for(ImplementTable table : implementTables.values())
            mapIndexes.put(table,new HashSet<List<String>>());

        // привяжем индексы
        for (List<? extends Property> index : indexes) {
            Iterator<? extends Property> i = index.iterator();
            if(!i.hasNext())
                throw new RuntimeException("Запрещено создавать пустые индексы");
            Property baseProperty = i.next();
            if(!baseProperty.isStored())
                throw new RuntimeException("Запрещено создавать индексы по не постоянным св-вам ("+baseProperty+")");
            ImplementTable indexTable = baseProperty.mapTable.table;
            
            List<String> tableIndex = new ArrayList<String>();
            tableIndex.add(baseProperty.field.name); 

            while (i.hasNext()) {
                Property property = i.next();
                if(!property.isStored())
                    throw new RuntimeException("Запрещено создавать индексы по не постоянным св-вам ("+baseProperty+")");
                if(indexTable.findProperty(property.field.name)==null)
                    throw new RuntimeException("Запрещено создавать индексы по св-вам ("+baseProperty+","+property+") в разных таблицах");
                tableIndex.add(property.field.name);
            }
            mapIndexes.get(indexTable).add(tableIndex);
        }

        // запишем новое состояние таблиц (чтобы потом изменять можно было бы)
        outDB.writeInt(mapIndexes.size());
        for(Map.Entry<ImplementTable,Set<List<String>>> mapIndex : mapIndexes.entrySet()) {
            mapIndex.getKey().serialize(outDB);
            outDB.writeInt(mapIndex.getValue().size());
            for(List<String> index : mapIndex.getValue()) {
                outDB.writeInt(index.size());
                for(String indexField : index)
                    outDB.writeUTF(indexField);
            }
        }

        // запишем новое состояние таблиц (чтобы потом изменять можно было бы)
        outDB.writeInt(storedProperties.size());
        for(Property<?> property : storedProperties) {
            outDB.writeUTF(property.sID);
            outDB.writeUTF(property.mapTable.table.name);
            for(Map.Entry<KeyField,? extends PropertyInterface> mapKey : property.mapTable.mapKeys.entrySet()) {
                outDB.writeInt(mapKey.getValue().ID);
                outDB.writeUTF(mapKey.getKey().name);
            }
        }

        // если не совпали sID или идентификаторы из базы удаляем сразу
        Map<String,Table> prevTables = new HashMap<String, Table>();
        for(int i=inputDB==null?0:inputDB.readInt();i>0;i--) {
            Table prevTable = new Table(inputDB);
            prevTables.put(prevTable.name,prevTable);

            for(int j=inputDB.readInt();j>0;j--) {
                List<String> index = new ArrayList<String>();
                for(int k=inputDB.readInt();k>0;k--)
                    index.add(inputDB.readUTF());
                ImplementTable implementTable = implementTables.get(prevTable.name);
                if(implementTable==null || !mapIndexes.get(implementTable).remove(index))
                    session.dropIndex(prevTable.name,index);
            }
        }

        // добавим таблицы которых не было
        for(ImplementTable table : implementTables.values())
            if(!prevTables.containsKey(table.name))
                session.createTable(table.name,table.keys);

        Set<ImplementTable> packTables = new HashSet<ImplementTable>();

        // бежим по свойствам
        int prevStoredNum = inputDB==null?0:inputDB.readInt();
        for(int i=0;i<prevStoredNum;i++) {
            String sID = inputDB.readUTF();
            Table prevTable = prevTables.get(inputDB.readUTF());
            Map<Integer,KeyField> mapKeys = new HashMap<Integer, KeyField>();
            for(int j=0;j<prevTable.keys.size();j++)
                mapKeys.put(inputDB.readInt(),prevTable.findKey(inputDB.readUTF()));

            boolean keep = false;
            for(Iterator<Property> is = storedProperties.iterator();is.hasNext();) {
                Property<?> property = is.next();
                if(property.sID.equals(sID)) {
                    Map<PropertyInterface, KeyField> foundInterfaces = new HashMap<PropertyInterface, KeyField>();
                    for(PropertyInterface propertyInterface : property.interfaces) {
                        KeyField mapKeyField = mapKeys.get(propertyInterface.ID);
                        if(mapKeyField!=null) foundInterfaces.put(propertyInterface, mapKeyField);
                    }
                    if(foundInterfaces.size()==mapKeys.size()) { // если все нашли
                        if(!(keep=property.mapTable.table.name.equals(prevTable.name))) { // если в другой таблице
                            session.addColumn(property.mapTable.table.name,property.field);
                            // делаем запрос на перенос
                            System.out.print("Идет перенос колонки "+property.field+" из таблицы "+prevTable.name+" в таблицу "+property.mapTable.table.name+"... ");
                            JoinQuery<KeyField,PropertyField> moveColumn = new JoinQuery<KeyField, PropertyField>(property.mapTable.table.keys);
                            JoinExpr<KeyField,PropertyField> moveExpr = new Join<KeyField,PropertyField>(prevTable,moveColumn,BaseUtils.join(property.mapTable.mapKeys,foundInterfaces)).exprs.get(prevTable.findProperty(sID));
                            moveColumn.properties.put(property.field, moveExpr);
                            moveColumn.and(moveExpr.getWhere());
                            session.modifyRecords(new ModifyQuery(property.mapTable.table,moveColumn));
                            System.out.println("Done");
                        } else // надо проверить что тип не изменился
                            if(!prevTable.findProperty(sID).type.equals(property.field.type))
                                session.modifyColumn(property.mapTable.table.name,property.field);
                        is.remove();
                    }
                    break;
                }
            }
            if(!keep) {
                session.dropColumn(prevTable.name,sID);
                ImplementTable table = implementTables.get(prevTable.name); // надо упаковать таблицу если удалили колонку 
                if(table!=null) packTables.add(table);
            }
        }

        Collection<AggregateProperty> recalculateProperties = new ArrayList<AggregateProperty>();
        for(Property property : storedProperties) { // добавляем оставшиеся
            session.addColumn(property.mapTable.table.name,property.field);
            if(property instanceof AggregateProperty)
                recalculateProperties.add((AggregateProperty)property);    
        }

        // удаляем таблицы старые
        for(String table : prevTables.keySet())
            if(!implementTables.containsKey(table))
                session.dropTable(table);

        // упакуем таблицы
        for(ImplementTable table : packTables)
            session.packTable(table);
        
        recalculateAggregations(session, recalculateProperties);

        // создадим индексы в базе
        for(Map.Entry<ImplementTable,Set<List<String>>> mapIndex : mapIndexes.entrySet())
            for(List<String> index : mapIndex.getValue())
                session.addIndex(mapIndex.getKey().name,index);

        session.updateInsertRecord(tableFactory.globalTable,new HashMap<KeyField, Integer>(),
                Collections.singletonMap(tableFactory.globalTable.struct,(Object)outDBStruct.toByteArray()));

        session.commitTransaction();
        
/*        byte[] outBytes = outDBStruct.toByteArray();
        FileOutputStream outFileStruct = new FileOutputStream("prevstruct.str");
        outFileStruct.write(outBytes.length/255);
        outFileStruct.write(outBytes.length%255);
        outFileStruct.write(outBytes);*/

        session.close();
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

    protected ObjectClass addObjectClass(String caption, RemoteClass... parents) {
        return addObjectClass(baseGroup, idShift(1), caption, parents);
    }

    ObjectClass addObjectClass(Integer iID, String caption, RemoteClass... parents) {
        return addObjectClass(baseGroup, iID, caption, parents);
    }

    ObjectClass addObjectClass(AbstractGroup group, Integer iID, String caption, RemoteClass... parents) {
        ObjectClass objectClass = new ObjectClass(iID, caption, parents);
        group.add(objectClass);
        return objectClass;
    }

    // без ID
    protected LDP addDProp(String caption, RemoteClass value, RemoteClass... params) {
        return addDProp((AbstractGroup)null, caption, value, params);
    }
    protected LDP addDProp(AbstractGroup group, String caption, RemoteClass value, RemoteClass... params) {
        return addDProp(group, genSID(), caption, value, params);
    }

    protected LDP addDProp(String sID, String caption, RemoteClass value, RemoteClass... params) {
        return addDProp(null, sID, caption, value, params);
    }
    protected LDP addDProp(AbstractGroup group, String sID, String caption, RemoteClass value, RemoteClass... params) {

        DataProperty property = new DataProperty(sID,params,tableFactory,value);
        property.sID = sID;
        property.caption = caption;
        properties.add(property);

        if (group != null)
            group.add(property);

        return new LDP(property);
    }

    protected void setDefProp(LDP data, LP defaultProperty,boolean onChange) {
        DataProperty property = ((DataProperty)data.property);
        property.defaultProperty = defaultProperty.property;
        for(int i=0;i<data.listInterfaces.size();i++)
            property.defaultMap.put((DataPropertyInterface)data.listInterfaces.get(i),defaultProperty.listInterfaces.get(i));

        property.onDefaultChange = onChange;
    }

    protected LCP addCProp(String caption, RemoteClass valueClass, Object value, RemoteClass... params) {
        return addCProp(genSID(), caption, valueClass, value, params);
    }
    protected LCP addCProp(String sID, String caption, RemoteClass valueClass, Object value, RemoteClass... params) {
        
        ClassProperty property = new ClassProperty(sID,params,tableFactory,valueClass,value);
        property.caption = caption;

        properties.add(property);
        
        return new LCP(property);
    }

    protected LSFP addSFProp(String formula, RemoteClass value,int paramCount) {

        StringFormulaProperty property = new StringFormulaProperty(genSID(),paramCount,tableFactory,value,formula);
        properties.add(property);
        return new LSFP(property,paramCount);
    }


    protected LCFP addCFProp(int compare) {
        CompareFormulaProperty property = new CompareFormulaProperty(genSID(),tableFactory,compare);
        LCFP listProperty = new LCFP(property);
        properties.add(property);
        return listProperty;
    }

    protected LNFP addNFProp() {
        NotNullFormulaProperty property = new NotNullFormulaProperty(genSID(),tableFactory);
        LNFP listProperty = new LNFP(property);
        properties.add(property);
        return listProperty;
    }

    protected LMFP addMFProp(RemoteClass value,int paramCount) {
        MultiplyFormulaProperty property = new MultiplyFormulaProperty(genSID(),tableFactory,value,paramCount);
        LMFP listProperty = new LMFP(property);
        properties.add(property);
        return listProperty;
    }

    protected LOFP addOFProp(int bitCount) {
        return addOFProp(genSID(),bitCount);
    }    
    protected LOFP addOFProp(String sID,int bitCount) {
        ObjectFormulaProperty property = new ObjectFormulaProperty(sID,bitCount,tableFactory,objectClass);
        properties.add(property);
        return new LOFP(property);
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

    protected LJP addJProp(String caption, LP mainProp, int intNum, Object... params) {
        return addJProp(null, caption, mainProp, intNum, params);
    }

    protected LJP addJProp(AbstractGroup group, String caption, LP mainProp, int intNum, Object... params) {
        return addJProp(group, genSID(), caption, mainProp, intNum, params);
    }

    protected LJP addJProp(AbstractGroup group, String sID, String caption, LP mainProp, int intNum, Object... params) {

        JoinProperty property = new JoinProperty(sID,intNum,tableFactory,mainProp.property);
        property.sID = sID;
        property.caption = caption;

        if (group != null)
            group.add(property);
        
        LJP listProperty = new LJP(property);
        int mainInt = 0;
        List<PropertyInterfaceImplement> propImpl = readPropImpl(listProperty,params);
        for(PropertyInterfaceImplement implement : propImpl) {
            property.implementations.mapping.put(mainProp.listInterfaces.get(mainInt),implement);
            mainInt++;
        }
        properties.add(property);

        return listProperty;
    }

    protected LGP addGProp(String caption, LP groupProp, boolean sum, Object... params) {
        return addGProp((AbstractGroup)null, caption, groupProp, sum, params);
    }
    protected LGP addGProp(AbstractGroup group, String caption, LP groupProp, boolean sum, Object... params) {
        return addGProp(group, genSID(), caption, groupProp, sum, params);
    }
    protected LGP addGProp(String sID, String caption, LP groupProp, boolean sum, Object... params) {
        return addGProp(null, sID, caption, groupProp, sum, params);
    }
    protected LGP addGProp(AbstractGroup group, String sID, String caption, LP groupProp, boolean sum, Object... params) {

        List<GroupPropertyInterface> interfaces = new ArrayList<GroupPropertyInterface>();
        List<PropertyInterfaceImplement> propImpl = readPropImpl(groupProp,params);
        for(PropertyInterfaceImplement implement : propImpl)
            interfaces.add(new GroupPropertyInterface(interfaces.size(),implement));

        GroupProperty property;
        if(sum)
            property = new SumGroupProperty(sID,interfaces,tableFactory,groupProp.property);
        else
            property = new MaxGroupProperty(sID,interfaces,tableFactory,groupProp.property);

        property.sID = sID;
        property.caption = caption;
        properties.add(property);

        if (group != null)
            group.add(property);

        return new LGP(property,interfaces);
    }

    protected LUP addUProp(String caption, Union unionType, int intNum, Object... params) {
        return addUProp((AbstractGroup)null, caption, unionType, intNum, params);
    }
    protected LUP addUProp(AbstractGroup group, String caption, Union unionType, int intNum, Object... params) {
        return addUProp(group, genSID(), caption, unionType, intNum, params);
    }

    protected LUP addUProp(String sID, String caption, Union unionType, int intNum, Object... params) {
        return addUProp(null, sID, caption, unionType, intNum, params);
    }
    protected LUP addUProp(AbstractGroup group, String sID, String caption, Union unionType, int intNum, Object... params) {

        UnionProperty property = null;
        switch(unionType) {
            case MAX:
                property = new MaxUnionProperty(sID,intNum,tableFactory);
                break;
            case SUM:
                property = new SumUnionProperty(sID,intNum,tableFactory);
                break;
            case OVERRIDE:
                property = new OverrideUnionProperty(sID,intNum,tableFactory);
                break;
        }
        property.sID = sID;
        property.caption = caption;

        LUP listProperty = new LUP(property);

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

    public void fillData() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    }

    // генерирует белую БЛ
    void openTest(DataAdapter adapter,boolean classes,boolean properties,boolean implement,boolean persistent,boolean changes) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException {

        if(classes) {
            initClasses();

            if(implement)
                initImplements();

            if(properties) {
                initProperties();

                if(persistent)
                    initPersistents();

                if(changes) {
                    synchronizeDB();
                    
                    fillData();
                }
            }

        }
    }

    // случайным образом генерирует классы
    void randomClasses(Random randomizer) {
        int customClasses = randomizer.nextInt(20);//
        List<RemoteClass> objClasses = new ArrayList<RemoteClass>();
        objClasses.add(objectClass);
        for(int i=0;i<customClasses;i++) {
            RemoteClass objectClass = new ObjectClass(i+10000, "Случайный класс"+i);
            int parents = randomizer.nextInt(2) + 1;
            for(int j=0;j<parents;j++)
                objectClass.addParent(objClasses.get(randomizer.nextInt(objClasses.size())));
            objClasses.add(objectClass);
        }
    }

    // случайным образом генерирует св-ва
    void randomProperties(Random randomizer) {

        List<RemoteClass> classes = new ArrayList<RemoteClass>();
        objectClass.fillChilds(classes);

        List<Property> randProps = new ArrayList<Property>();
        List<Property> randObjProps = new ArrayList<Property>();
        List<Property> randIntegralProps = new ArrayList<Property>();

        CompareFormulaProperty dirihle = new CompareFormulaProperty(genSID(), tableFactory, Compare.LESS);
        randProps.add(dirihle);

        MultiplyFormulaProperty multiply = new MultiplyFormulaProperty(genSID(), tableFactory, RemoteClass.integer,2);
        randProps.add(multiply);

        int dataPropCount = randomizer.nextInt(15)+1;
        for(int i=0;i<dataPropCount;i++) {
            int intCount = randomizer.nextInt(tableFactory.MAX_INTERFACE)+1;
            RemoteClass[] interfaceClasses = new RemoteClass[intCount];
            for(int j=0;j<intCount;j++)
                interfaceClasses[j] = classes.get(randomizer.nextInt(classes.size()));

            // DataProperty
            DataProperty dataProp = new DataProperty("prop"+i,interfaceClasses,tableFactory,(i%4==0? RemoteClass.integer :classes.get(randomizer.nextInt(classes.size()))));
            dataProp.caption = "Data Property " + i;
            // генерируем классы

            randProps.add(dataProp);
            randObjProps.add(dataProp);
            if(dataProp.getBaseClass().contains(RemoteClass.integral))
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
                // генерируем случайно кол-во интерфейсов

                int intCount = randomizer.nextInt(tableFactory.MAX_INTERFACE)+1;
                // JoinProperty
                JoinProperty relProp = new JoinProperty(genSID(),intCount,tableFactory,randProps.get(randomizer.nextInt(randProps.size())));
                List<PropertyInterface> relPropInt = new ArrayList<PropertyInterface>(relProp.interfaces);

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
                if(propClass ==1)
                    groupProp = randIntegralProps.get(randomizer.nextInt(randIntegralProps.size()));
                else
                    groupProp = randObjProps.get(randomizer.nextInt(randObjProps.size()));

                Collection<GroupPropertyInterface> interfaces = new ArrayList<GroupPropertyInterface>();

                boolean correct = true;
                List<PropertyInterface> groupInt = new ArrayList(groupProp.interfaces);
                int groupCount = randomizer.nextInt(tableFactory.MAX_INTERFACE)+1;
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

                    interfaces.add(new GroupPropertyInterface(j,implement));
                }

                if(correct) {
                    if(propClass ==1) {
                        genProp = new SumGroupProperty(genSID(),interfaces,tableFactory,groupProp);
                        resType = "SG";
                    } else {
                        genProp = new MaxGroupProperty(genSID(),interfaces,tableFactory,groupProp);
                        resType = "MG";
                    }
                }
            }

            if(propClass ==3 || propClass ==4 || propClass ==5) {
                UnionProperty property = null;
                List<Property> randValProps = randObjProps;
                int opIntCount = randomizer.nextInt(tableFactory.MAX_INTERFACE)+1;
                if(propClass ==3) {
                    randValProps = randIntegralProps;
                    property = new SumUnionProperty(genSID(),opIntCount,tableFactory);
                    resType = "SL";
                } else {
                if(propClass ==4) {
                    property = new MaxUnionProperty(genSID(),opIntCount,tableFactory);
                    resType = "ML";
                } else {
                    property = new OverrideUnionProperty(genSID(),opIntCount,tableFactory);
                    resType = "OL";
                }
                }

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
                    if(genProp.getBaseClass().contains(RemoteClass.integral))
                        randIntegralProps.add(genProp);
                }
            }
        }

        properties.addAll(randProps);

        System.out.println();
    }

    // случайным образом генерирует имплементацию
    void randomImplement(Random randomizer) {
        List<RemoteClass> classes = new ArrayList<RemoteClass>();
        objectClass.fillChilds(classes);

        // заполнение физ модели
        int implementCount = randomizer.nextInt(8);
        for(int i=0;i<implementCount;i++) {
            int objCount = randomizer.nextInt(3)+1;
            RemoteClass[] randomClasses = new RemoteClass[objCount]; 
            for(int ioc=0;ioc<objCount;ioc++)
                randomClasses[ioc] = classes.get(randomizer.nextInt(classes.size()));
            tableFactory.include(randomClasses);
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
    void changeDBTest(Integer maxIterations,Random randomizer) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        // сначала список получим
        List<DataProperty> dataProperties = new ArrayList<DataProperty>();
        for(Property property : properties) {
            if(property instanceof DataProperty)
                dataProperties.add((DataProperty)property);
        }

        DataSession session = createSession(adapter);

        List<RemoteClass> addClasses = new ArrayList<RemoteClass>();
        objectClass.fillChilds(addClasses);
        for(RemoteClass addClass : addClasses) {
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
            addClasses = new ArrayList<RemoteClass>();
            objectClass.fillChilds(addClasses);
            int objectAdd = randomizer.nextInt(5);
            for(int ia=0;ia<objectAdd;ia++) {
                RemoteClass addClass = addClasses.get(randomizer.nextInt(addClasses.size()));
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
                    Map<DataPropertyInterface, ObjectValue> keys = new HashMap<DataPropertyInterface, ObjectValue>();
                    for(DataPropertyInterface propertyInterface : changeProp.interfaces) {
                        RemoteClass randomClass = interfaceClasses.get(propertyInterface).getRandom(randomizer);
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

    public static List<Property> getChangedList(Collection<? extends Property> updateProps, DataChanges changes,Collection<Property> noUpdateProps) {
        List<Property> changedList = new ArrayList<Property>();
        for(Property property : updateProps)
            property.fillChangedList(changedList,changes,noUpdateProps);
        return changedList;
    }

    // флаг для оптимизации
    protected Map<DataProperty,Integer> autoQuantity(Integer quantity, LDP... properties) {
        Map<DataProperty,Integer> result = new HashMap<DataProperty,Integer>();
        for(LDP<?> property : properties)
            result.put(property.property,quantity);
        return result;
    }

    // полностью очищает базу
    protected void clean(DataSession session) throws SQLException {
        // удаляем все объекты
        session.deleteKeyRecords(tableFactory.objectTable, new HashMap<KeyField, Integer>());

        // удаляем все св-ва
        for(ImplementTable table : tableFactory.getImplementTables().values())
            session.deleteKeyRecords(table, new HashMap<KeyField, Integer>());
    }

    public static boolean autoFillDB = false;
    public static int autoIDCounter = 0;
    static int AutoSeed = 1400;
    public void autoFillDB(Map<RemoteClass, Integer> classQuantity, Map<DataProperty, Integer> propQuantity, Map<DataProperty, Set<DataPropertyInterface>> propNotNull) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        autoFillDB = true;
        DataSession session = createSession(adapter);

        clean(session);

        // сначала вырубим все аггрегации в конце пересчитаем
        Map<AggregateProperty, PropertyField> savePersistents = new HashMap<AggregateProperty, PropertyField>();
        for(AggregateProperty property : persistents) {
            savePersistents.put(property,property.field);
            property.field = null;
        }
        persistents.clear();

        // генерируем классы
        Map<Integer,String> objectNames = new HashMap<Integer, String>();
        Map<RemoteClass,List<Integer>> objects = new HashMap<RemoteClass, List<Integer>>();
        List<RemoteClass> classes = new ArrayList<RemoteClass>();
        objectClass.fillChilds(classes);

        for(RemoteClass fillClass : classes)
            objects.put(fillClass,new ArrayList<Integer>());

        for(RemoteClass fillClass : classes)
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

                        Map<DataPropertyInterface, ObjectValue> keys = new HashMap<DataPropertyInterface, ObjectValue>();
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

        for(Map.Entry<AggregateProperty,PropertyField> save : savePersistents.entrySet()) {
            save.getKey().field = save.getValue();
            persistents.add(save.getKey());
        }

        recalculateAggregations(session,savePersistents.keySet());

        session.commitTransaction();

        tableFactory.idTable.reserveID(session, IDTable.OBJECT, autoIDCounter);

        session.close();

        autoFillDB = false;
    }

    private void recalculateAggregations(DataSession session,Collection<AggregateProperty> recalculateProperties) throws SQLException {
        for(Property dependProperty : getChangedList(recalculateProperties,null,new HashSet<Property>()))
            if(dependProperty instanceof AggregateProperty) {
                AggregateProperty property = (AggregateProperty)dependProperty;
                if(recalculateProperties.contains(property)) {
                    System.out.print("Идет перерасчет аггрегированного св-ва ("+property+")... ");
                    property.recalculateAggregation(session);
                    System.out.println("Done");
                }
            }
    }

    public void createDefaultClassForms(RemoteClass cls, NavigatorElement parent) {

        NavigatorElement node = new ClassNavigatorForm(this, cls);
        parent.add(node);

        // Проверим, что такой формы еще не было
        boolean found = false;
        for (NavigatorElement relNode : cls.relevantElements)
            if (relNode.ID == node.ID) { found = true; break; }
        if (!found)
            cls.addRelevantElement(node);

        for (RemoteClass child : cls.childs) {
            createDefaultClassForms(child, node);
        }
    }

    // -------------------------------------- Старые интерфейсы --------------------------------------------------- //

    public Map<String, PropertyObjectImplement> fillSingleViews(ObjectImplement object, NavigatorForm form,Set<String> names) {

        Map<String, PropertyObjectImplement> result = new HashMap<String, PropertyObjectImplement>();

        for(Property drawProp : properties) {
            if(drawProp.interfaces.size() == 1) {
                // проверим что дает хоть одно значение
                InterfaceClass interfaceClass = new InterfaceClass();
                interfaceClass.put(((Collection<PropertyInterface>)drawProp.interfaces).iterator().next(), ClassSet.getUp(object.baseClass));
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

    public PropertyObjectImplement addPropertyView(NavigatorForm fbv, LP listProp, GroupObjectImplement gv, ObjectImplement... params) {
        PropertyObjectImplement propImpl = new PropertyObjectImplement(listProp.property);

        ListIterator<PropertyInterface> i = listProp.listInterfaces.listIterator();
        for(ObjectImplement object : params)
            propImpl.mapping.put(i.next(),object);
        fbv.propertyViews.add(new PropertyView(fbv.IDShift(1),propImpl,gv));
        return propImpl;
    }

}
