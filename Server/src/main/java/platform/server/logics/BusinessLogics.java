package platform.server.logics;

import platform.base.CollectionExtend;
import platform.base.Combinations;
import platform.interop.Compare;
import platform.interop.RemoteLogicsInterface;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.Table;
import platform.server.data.Union;
import platform.server.data.sql.DataAdapter;
import platform.server.data.types.Type;
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
import platform.server.logics.data.TableImplement;
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
import java.io.FileNotFoundException;

import net.sf.jasperreports.engine.JRException;

public abstract class BusinessLogics<T extends BusinessLogics<T>> extends UnicastRemoteObject implements PropertyUpdateView, RemoteLogicsInterface {

    protected DataAdapter adapter;
    public static boolean activateCaches = true;

    abstract protected DataAdapter newAdapter() throws ClassNotFoundException;

    public RemoteNavigatorInterface createNavigator(String login,String password) throws RemoteException {
        User user = authPolicy.getUser(login, password);
        if (user == null) throw new RemoteException("login failed");

        return new RemoteNavigator(adapter,this,user);
    }

    public String result() {
        return "HI";
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

        for(int i=0;i< tableFactory.maxInterface;i++) {
            TableImplement include = new TableImplement();
            for(int j=0;j<=i;j++)
                include.add(new DataPropertyInterface(j, RemoteClass.base));
            tableFactory.includeIntoGraph(include);
        }

        baseElement = new NavigatorElement<T>(0, "Base Group");
    }

    protected boolean recreateDB = false;

    // по умолчанию с полным стартом
    public BusinessLogics() throws RemoteException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super();

        adapter = newAdapter();

        initBase();

        initLogics();
        initImplements();
        initNavigators();

        initAuthentication();

        if (recreateDB) {
            adapter.createDB();

            DataSession session = createSession(adapter);
            fillDB(session, true);
            session.close();

            fillData(adapter);
        } else
            fillDB(null, false);
    }

    public boolean toSave(Property property) {
        return property.isPersistent();
    }

    public Collection<Property> getNoUpdateProperties() {
        return new ArrayList<Property>();
    }

    static Set<Integer> wereSuspicious = new HashSet<Integer>();

    // тестирующий конструктор
    public BusinessLogics(int testType,Integer seed,int iterations) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, RemoteException {
        super();

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
            changeDBTest(adapter,iterations,randomizer);
        } catch(Exception e) {
            e.printStackTrace();
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

    void addDataProperty(DataProperty property) {
        properties.add(property);
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

    public Collection<Property> getUpdateProperties() {
        Collection<Property> updateList = new HashSet<Property>(persistents);
        updateList.addAll(constraints.keySet());
        for(Property property : properties)
            if(property instanceof DataProperty) updateList.add(property);
        return updateList;
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

    public void fillDB(DataSession session, boolean createTable) throws SQLException {

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
            dumbTable.keys.add(new KeyField("dumb", Type.system));
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

    protected LDP addDProp(String caption, RemoteClass value, RemoteClass... params) {
        return addDProp((AbstractGroup)null, caption, value, params);
    }
    protected LDP addDProp(String sID, String caption, RemoteClass value, RemoteClass... params) {
        return addDProp(null, sID, caption, value, params);
    }
    protected LDP addDProp(AbstractGroup group, String caption, RemoteClass value, RemoteClass... params) {
        return addDProp(group, null, caption, value, params);
    }
    protected LDP addDProp(AbstractGroup group, String sID, String caption, RemoteClass value, RemoteClass... params) {
        DataProperty property = new DataProperty(tableFactory,value);
        property.sID = sID;
        property.caption = caption;
        LDP listProperty = new LDP(property);
        for(RemoteClass interfaceClass : params)
            listProperty.AddInterface(interfaceClass);
        addDataProperty(property);

        if (group != null)
            group.add(property);

        return listProperty;
    }

    protected void setDefProp(LDP data, LP defaultProperty,boolean onChange) {
        DataProperty property = ((DataProperty)data.property);
        property.defaultProperty = defaultProperty.property;
        for(int i=0;i<data.listInterfaces.size();i++)
            property.defaultMap.put((DataPropertyInterface)data.listInterfaces.get(i),defaultProperty.listInterfaces.get(i));

        property.onDefaultChange = onChange;
    }

    protected LCP addCProp(String caption, Object value, RemoteClass valueClass, RemoteClass... params) {
        ClassProperty property = new ClassProperty(tableFactory,valueClass,value);
        property.caption = caption;
        LCP listProperty = new LCP(property);
        for(RemoteClass interfaceClass : params)
            listProperty.addInterface(interfaceClass);
        properties.add(property);
        return listProperty;
    }

    protected LSFP addSFProp(String formula, RemoteClass value,int paramCount) {

        StringFormulaProperty property = new StringFormulaProperty(tableFactory,value,formula);
        LSFP listProperty = new LSFP(property,paramCount);
        properties.add(property);
        return listProperty;
    }


    protected LCFP addCFProp(int compare) {
        CompareFormulaProperty property = new CompareFormulaProperty(tableFactory,compare);
        LCFP listProperty = new LCFP(property);
        properties.add(property);
        return listProperty;
    }

    protected LNFP addNFProp() {
        NotNullFormulaProperty property = new NotNullFormulaProperty(tableFactory);
        LNFP listProperty = new LNFP(property);
        properties.add(property);
        return listProperty;
    }

    protected LMFP addMFProp(RemoteClass value,int paramCount) {
        MultiplyFormulaProperty property = new MultiplyFormulaProperty(tableFactory,value,paramCount);
        LMFP listProperty = new LMFP(property);
        properties.add(property);
        return listProperty;
    }

    protected LOFP addOFProp(int bitCount) {
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

    protected LJP addJProp(String caption, LP mainProp, int intNum, Object... params) {
        return addJProp(null, caption, mainProp, intNum, params);
    }

    protected LJP addJProp(AbstractGroup group, String caption, LP mainProp, int intNum, Object... params) {
        return addJProp(group, null, caption, mainProp, intNum, params);
    }

    protected LJP addJProp(AbstractGroup group, String sID, String caption, LP mainProp, int intNum, Object... params) {
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

    protected LGP addGProp(String caption, LP groupProp, boolean sum, Object... params) {
        return addGProp(null, caption, groupProp, sum, params);
    }

    protected LGP addGProp(AbstractGroup group, String caption, LP groupProp, boolean sum, Object... params) {
        return addGProp(group, null, caption, groupProp, sum, params);
    }

    protected LGP addGProp(AbstractGroup group, String sID, String caption, LP groupProp, boolean sum, Object... params) {

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

    protected LUP addUProp(String caption, Union unionType, int intNum, Object... params) {
        return addUProp((AbstractGroup)null, caption, unionType, intNum, params);
    }

    protected LUP addUProp(String sID, String caption, Union unionType, int intNum, Object... params) {
        return addUProp(null, sID, caption, unionType, intNum, params);
    }

    protected LUP addUProp(AbstractGroup group, String caption, Union unionType, int intNum, Object... params) {
        return addUProp(group, null, caption, unionType, intNum, params);
    }

    protected LUP addUProp(AbstractGroup group, String sID, String caption, Union unionType, int intNum, Object... params) {
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

    public void fillData(DataAdapter adapter) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
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

        CompareFormulaProperty dirihle = new CompareFormulaProperty(tableFactory, Compare.LESS);
        randProps.add(dirihle);

        MultiplyFormulaProperty Multiply = new MultiplyFormulaProperty(tableFactory, RemoteClass.integer,2);
        randProps.add(Multiply);

        int dataPropCount = randomizer.nextInt(15)+1;
        for(int i=0;i<dataPropCount;i++) {
            // DataProperty
            DataProperty dataProp = new DataProperty(tableFactory,(i%4==0? RemoteClass.integer :classes.get(randomizer.nextInt(classes.size()))));
            dataProp.caption = "Data Property " + i;
            // генерируем классы
            int intCount = randomizer.nextInt(tableFactory.maxInterface)+1;
            for(int j=0;j<intCount;j++)
                dataProp.interfaces.add(new DataPropertyInterface(j,classes.get(randomizer.nextInt(classes.size()))));

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
    void changeDBTest(DataAdapter adapter,Integer maxIterations,Random randomizer) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

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

    public static boolean autoFillDB = false;
    public static int autoIDCounter = 0;
    static int AutoSeed = 1400;
    public void autoFillDB(DataAdapter adapter, Map<RemoteClass, Integer> classQuantity, Map<DataProperty, Integer> propQuantity, Map<DataProperty, Set<DataPropertyInterface>> propNotNull) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        autoFillDB = true;
        DataSession session = createSession(adapter);

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

        tableFactory.idTable.reserveID(session, IDTable.OBJECT, autoIDCounter);

        session.close();

        autoFillDB = false;
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
