package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import platform.base.*;
import platform.interop.Compare;
import platform.interop.RemoteLogicsInterface;
import platform.interop.RemoteObject;
import platform.interop.exceptions.LoginException;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.server.auth.AuthPolicy;
import platform.server.auth.User;
import platform.server.data.*;
import platform.server.data.query.Query;
import platform.server.data.expr.Expr;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.table.IDTable;
import platform.server.logics.table.ImplementTable;
import platform.server.logics.table.TableFactory;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.derived.DistrGroupProperty;
import platform.server.logics.property.derived.CycleGroupProperty;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.linear.LP;
import platform.server.logics.linear.LC;
import platform.server.logics.control.Control;
import platform.server.session.DataSession;
import platform.server.data.SQLSession;
import platform.server.view.navigator.*;
import platform.server.classes.*;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.sql.SQLException;
import java.util.*;
import java.net.MalformedURLException;

import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;

public abstract class BusinessLogics<T extends BusinessLogics<T>> extends RemoteObject implements RemoteLogicsInterface {

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, FileNotFoundException, JRException, MalformedURLException {

        System.out.println("Server is starting...");

        XmlBeanFactory factory = new XmlBeanFactory(new FileSystemResource("conf/settings.xml"));

        BusinessLogics bl = (BusinessLogics)factory.getBean("businessLogics");
        
        LocateRegistry.createRegistry(bl.getExportPort()).rebind("BusinessLogics", bl);

        System.out.println("Server has successfully started");
    }

    public final static SQLSyntax debugSyntax = new PostgreDataAdapter();
    
    protected DataAdapter adapter;
    public SQLSyntax getAdapter() {
        return adapter;
    }
    
    public final static boolean activateCaches = true;

    public RemoteNavigatorInterface createNavigator(String login, String password) {

        User user = authPolicy.getUser(login, password);
        if (user == null) throw new LoginException();

        try {
            return new RemoteNavigator(adapter, this, user, exportPort);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    // счетчик идентификаторов
    IDGenerator idGenerator = new DefaultIDGenerator();

    int idShift(int offs) {
        return idGenerator.idShift(offs);
    }

    protected AbstractCustomClass namedObject, transaction;

    protected LP groeq2;
    protected LP greater2, less2;
    protected LP between;
    protected LP object1, and1, andNot1;
    protected LP equals2,diff2;

    protected LP vtrue, vzero;

    public LP<?> name;
    protected LP<?> date;

    protected LP transactionLater;
    protected LP currentDate;
    protected LP currentHour;

    void initBase() {
        baseClass = new BaseClass(idShift(1), "Объект");

        namedObject = addAbstractClass("Объект с именем", baseClass);
        transaction = addAbstractClass("Транзакция", baseClass);

        tableFactory = new TableFactory();
        for(int i=0;i<TableFactory.MAX_INTERFACE;i++) { // заполним базовые таблицы
            CustomClass[] baseClasses = new CustomClass[i];
            for(int j=0;j<i;j++)
                baseClasses[j] = baseClass;
            tableFactory.include("base_"+i,baseClasses);
        }

        baseElement = new NavigatorElement<T>(0, "Base Group");

        // математические св-ва
        equals2 = addCFProp(Compare.EQUALS);
        object1 = addAFProp();
        and1 = addAFProp(false);
        andNot1 = addAFProp(true);
        groeq2 = addCFProp(Compare.GREATER_EQUALS);
        greater2 = addCFProp(Compare.GREATER);
        less2 = addCFProp(Compare.LESS);
        diff2 = addCFProp(Compare.NOT_EQUALS);
        between = addJProp("Между", and1, groeq2,1,2, groeq2,3,1);
        vtrue = addCProp("Истина",LogicalClass.instance,true);
        vzero = addCProp("0",DoubleClass.instance,0); 

        name = addDProp(baseGroup, "name", "Имя", StringClass.get(50), namedObject);
        date = addDProp(baseGroup, "date", "Дата", DateClass.instance, transaction);

        transactionLater = addSUProp("Транзакция позже", Union.OVERRIDE, addJProp("Дата позже", greater2, date, 1, date, 2),
                addJProp("", and1, addJProp("Дата=дата", equals2, date, 1, date, 2), 1, 2, addJProp("Код транзакции после", greater2, 1, 2), 1, 2));

        currentDate = addDProp(baseGroup, "currentDate", "Тек. дата", DateClass.instance);
        currentHour = addSFProp(adapter.getHour(), DoubleClass.instance, 0);
    }

    private Map<ValueClass,LP> is = new HashMap<ValueClass, LP>();
    // получает свойство is
    protected LP is(ValueClass valueClass) {
        LP isProp = is.get(valueClass);
        if(isProp==null) {
            isProp = addCProp(valueClass.toString() + "(пр.)", LogicalClass.instance, true, valueClass);
            is.put(valueClass,isProp);
        }
        return isProp;
    }
    private Map<ValueClass,LP> object = new HashMap<ValueClass, LP>();
    public LP object(ValueClass valueClass) {
        LP objectProp = object.get(valueClass);
        if(objectProp==null) {
            objectProp = addJProp(valueClass.toString(), and1, 1, is(valueClass), 1);
            object.put(valueClass,objectProp);
        }
        return objectProp;
    }

    protected LP and(boolean... nots) {
        return addAFProp(nots);
    }

    private Map<ValueClass,LP> split = new HashMap<ValueClass, LP>();
    public LP split(ValueClass valueClass) { // избыточно так как не может сама класс определить
        LP splitProp = split.get(valueClass);
        if(splitProp==null) {
            splitProp = addJProp("split " + valueClass.toString(), equals2, object(valueClass), 1, object(valueClass), 2);
            split.put(valueClass,splitProp);
        }
        return splitProp;
    }

    // по умолчанию с полным стартом
    public BusinessLogics(DataAdapter adapter,int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(exportPort);

        this.adapter = adapter;

        initBase();

        initLogics();
        initImplements();

        // сначала инициализируем stored потому как используется для определения интерфейса
        System.out.println("Initializing stored property...");
        initStored();

        assert checkProps();

        System.out.println("Initializing navigators...");

        baseElement.add(baseClass.getBaseClassForm(this));
        initNavigators();

        initAuthentication();

        synchronizeDB();

        // запишем текущую дату
        DataSession session = createSession(adapter);
        session.changeProperty((DataProperty)currentDate.property, new HashMap<ClassPropertyInterface, DataObject>(), new DataObject(DateConverter.dateToInt(new Date()), DateClass.instance), false);
        session.apply(this);
        session.close();
    }

    final static Set<Integer> wereSuspicious = new HashSet<Integer>();

    // тестирующий конструктор
    public BusinessLogics(DataAdapter iAdapter,int testType,Integer seed,int iterations) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException {
        super(1099);

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
//            randomClasses(randomizer);
//            randomProperties(randomizer);
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
    public AbstractGroup aggrGroup = new AbstractGroup("Аггрегированные атрибуты");
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
        return "control" + controls.size();
    }

    protected void setPropOrder(LP<?> prop, LP<?> propRel, boolean before) {
        setPropOrder(prop.property, propRel.property, before);
    }

    protected void setPropOrder(Property prop, Property propRel, boolean before) {

        int indProp = controls.indexOf(prop);
        int indPropRel = controls.indexOf(propRel);

        if (before) {
            if (indPropRel < indProp) {
                for (int i = indProp; i >= indPropRel + 1; i--)
                    controls.set(i, controls.get(i-1));
                controls.set(indPropRel, prop);
            }
        }
    }

    protected void addIndex(LP<?>... lps) {
        List<Property> index = new ArrayList<Property>();
        for(LP<?> lp : lps)
            index.add(lp.property);
        indexes.add(index);
    }

    // счетчик сессий (пока так потом надо из базы или как-то по другому транзакционность сделать
    public DataSession createSession(DataAdapter adapter) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new DataSession(adapter, baseClass, namedObject, name.property, transaction, date.property);
    }

    public BaseClass baseClass;

    public TableFactory tableFactory;
    public List<Control> controls = new ArrayList<Control>();
    public List<Property> getProperties() {
        List<Property> result = new ArrayList<Property>();
        for(Control control : controls)
            if(control instanceof Property)
                result.add((Property)control);
        return result;
    }
    protected Set<AggregateProperty> persistents = new HashSet<AggregateProperty>();
    protected Set<List<? extends Property>> indexes = new HashSet<List<? extends Property>>();

    // получает список св-в в порядке использования
    private void fillPropertyList(Property<?> property,LinkedHashSet<Property> set) {
        for(Property depend : property.getDepends())
            fillPropertyList(depend,set);
        set.add(property);
    }
    Iterable<Property> getPropertyList() {
        LinkedHashSet<Property> linkedSet = new LinkedHashSet<Property>();
        for(Property property : getProperties())
            fillPropertyList(property,linkedSet);
        return linkedSet;
    }
    private boolean depends(Property<?> property, Property check) {
        if(property.equals(check))
            return true;
        for(Property depend : property.getDepends())
            if(depends(depend,check))
                return true;
        return false;
    }


    public List<Property> getStoredProperties() {
        List<Property> result = new ArrayList<Property>();
        for(Property property : getPropertyList())
            if(persistents.contains(property) || property instanceof DataProperty)
                result.add(property);
        return result;
    }

    public <P extends PropertyInterface> Collection<MaxChangeProperty<?,P>> getChangeConstrainedProperties(Property<P> change) {
        Collection<MaxChangeProperty<?,P>> result = new ArrayList<MaxChangeProperty<?,P>>();
        for(Property<?> property : getPropertyList())
            if(property.isFalse && property.checkChange && depends(property,change))
                result.add(property.getMaxChangeProperty(change));
        return result;
    }

    public List<Property> getAppliedProperties() {
        List<Property> result = new ArrayList<Property>();
        for(Property property : getPropertyList())
            if(property.isStored() || property.isFalse)
                result.add(property);
        return result;
    }

    public void initStored() {
        // привяжем к таблицам все свойства
        for(Property property : getStoredProperties()) {
            System.out.println("Initializing stored - "+property+"...");
            property.markStored(tableFactory);
        }
    }

    public void synchronizeDB() throws SQLException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        DataSession session = createSession(adapter);

        session.startTransaction();

        // запишем sID'ки
        int idControlNum = 0;
        for(Control control : controls)
            control.ID = idControlNum++;

        // инициализируем таблицы
        tableFactory.fillDB(session);
        
        // "старое" состояние базы
        DataInputStream inputDB = null;
        byte[] struct = (byte[]) session.readRecord(GlobalTable.instance,new HashMap<KeyField, DataObject>(), GlobalTable.instance.struct);
        if(struct!=null) inputDB = new DataInputStream(new ByteArrayInputStream(struct));

        if(struct!=null && struct.length==0) { //чисто для бага JTDS
            session.rollbackTransaction();
            session.close();
            return;
        }
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

        Collection<Property> storedProperties = new ArrayList<Property>(getStoredProperties());
        // запишем новое состояние таблиц (чтобы потом изменять можно было бы)
        outDB.writeInt(storedProperties.size());
        for(Property<?> property : storedProperties) {
            outDB.writeUTF(property.sID);
            outDB.writeUTF(property.mapTable.table.name);
            for(Map.Entry<? extends PropertyInterface,KeyField> mapKey : property.mapTable.mapKeys.entrySet()) {
                outDB.writeInt(mapKey.getKey().ID);
                outDB.writeUTF(mapKey.getValue().name);
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
                    Map<KeyField,PropertyInterface> foundInterfaces = new HashMap<KeyField,PropertyInterface>();
                    for(PropertyInterface propertyInterface : property.interfaces) {
                        KeyField mapKeyField = mapKeys.get(propertyInterface.ID);
                        if(mapKeyField!=null) foundInterfaces.put(mapKeyField,propertyInterface);
                    }
                    if(foundInterfaces.size()==mapKeys.size()) { // если все нашли
                        if(!(keep=property.mapTable.table.name.equals(prevTable.name))) { // если в другой таблице
                            session.addColumn(property.mapTable.table.name,property.field);
                            // делаем запрос на перенос
                            System.out.print("Идет перенос колонки "+property.field+" из таблицы "+prevTable.name+" в таблицу "+property.mapTable.table.name+"... ");
                            Query<KeyField,PropertyField> moveColumn = new Query<KeyField, PropertyField>(property.mapTable.table);
                            Expr moveExpr = prevTable.joinAnd(BaseUtils.join(BaseUtils.join(foundInterfaces,((Property<PropertyInterface>)property).mapTable.mapKeys),moveColumn.mapKeys)).getExpr(prevTable.findProperty(sID));
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

        try {
            session.updateInsertRecord(GlobalTable.instance,new HashMap<KeyField, DataObject>(),
                Collections.singletonMap(GlobalTable.instance.struct,(ObjectValue)new DataObject((Object)outDBStruct.toByteArray(), ByteArrayClass.instance)));
        }
        catch(Exception e) {
            session.updateInsertRecord(GlobalTable.instance,new HashMap<KeyField, DataObject>(),
                Collections.singletonMap(GlobalTable.instance.struct,(ObjectValue)new DataObject((Object)new byte[0], ByteArrayClass.instance)));
        }

        session.commitTransaction();
        
/*        byte[] outBytes = outDBStruct.toByteArray();
        FileOutputStream outFileStruct = new FileOutputStream("prevstruct.str");
        outFileStruct.write(outBytes.length/255);
        outFileStruct.write(outBytes.length%255);
        outFileStruct.write(outBytes);*/

        session.close();
    }

    boolean checkPersistent(SQLSession session) throws SQLException {
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

    protected ConcreteCustomClass addConcreteClass(String caption, CustomClass... parents) {
        return addConcreteClass(baseGroup, idShift(1), caption, parents);
    }
    ConcreteCustomClass addConcreteClass(Integer iID, String caption, CustomClass... parents) {
        return addConcreteClass(baseGroup, iID, caption, parents);
    }
    ConcreteCustomClass addConcreteClass(AbstractGroup group, Integer iID, String caption, CustomClass... parents) {
        ConcreteCustomClass customClass = new ConcreteCustomClass(iID, caption, parents);
        group.add(customClass);
        return customClass;
    }

    protected AbstractCustomClass addAbstractClass(String caption, CustomClass... parents) {
        return addAbstractClass(baseGroup, idShift(1), caption, parents);
    }
    AbstractCustomClass addAbstractClass(Integer iID, String caption, CustomClass... parents) {
        return addAbstractClass(baseGroup, iID, caption, parents);
    }
    AbstractCustomClass addAbstractClass(AbstractGroup group, Integer iID, String caption, CustomClass... parents) {
        AbstractCustomClass customClass = new AbstractCustomClass(iID, caption, parents);
        group.add(customClass);
        return customClass;
    }

    // без ID
    protected LP addDProp(String caption, ValueClass value, ValueClass... params) {
        return addDProp((AbstractGroup)null, caption, value, params);
    }
    protected LP addDProp(AbstractGroup group, String caption, ValueClass value, ValueClass... params) {
        return addDProp(group, genSID(), caption, value, params);
    }

    protected LP addDProp(String sID, String caption, ValueClass value, ValueClass... params) {
        return addDProp(null, sID, caption, value, params);
    }
    protected LP addDProp(AbstractGroup group, String sID, String caption, ValueClass value, ValueClass... params) {
        return addControl(group,new LP<ClassPropertyInterface>(new DataProperty(sID,caption,params,value)));
    }

    protected LP addCProp(String caption, ConcreteValueClass valueClass, Object value, ValueClass... params) {
        return addCProp(genSID(), caption, valueClass, value, params);
    }
    protected LP addCProp(String sID, String caption, ConcreteValueClass valueClass, Object value, ValueClass... params) {
        return addCProp(null, sID, caption, valueClass, value, params);
    }

    protected LP addCProp(AbstractGroup group, String sID, String caption, ConcreteValueClass valueClass, Object value, ValueClass... params) {
        return addControl(group,new LP<ClassPropertyInterface>(new ClassProperty(sID,caption,params,valueClass,value)));
    }

    protected LP addSFProp(String formula, ConcreteValueClass value,int paramCount) {
        return addControl(null,new LP<StringFormulaProperty.Interface>(new StringFormulaProperty(genSID(),value,formula,paramCount)));
    }

    protected LP addCFProp(Compare compare) {
        return addControl(null,new LP<CompareFormulaProperty.Interface>(new CompareFormulaProperty(genSID(),compare)));
    }

    protected LP addMFProp(ConcreteValueClass value,int paramCount) {
        return addControl(null,new LP<StringFormulaProperty.Interface>(new MultiplyFormulaProperty(genSID(),value,paramCount)));
    }

    protected LP addAFProp(boolean... nots) {
        return addAFProp(genSID(),nots);
    }    
    protected LP addAFProp(String sID, boolean... nots) {
        return addControl(null,new LP<AndFormulaProperty.Interface>(new AndFormulaProperty(sID,nots)));
    }

    // Linear Implement
    private static abstract class LI {
        abstract <T extends PropertyInterface<T>> PropertyInterfaceImplement<T> map(List<T> interfaces);

        abstract Object[] write();

        abstract Object[] compare(LP compare, BusinessLogics BL,int intOff);
    }

    private static class LII extends LI {
        int intNum;

        private LII(int intNum) {
            this.intNum = intNum;
        }

        <T extends PropertyInterface<T>> PropertyInterfaceImplement<T> map(List<T> interfaces) {
            return interfaces.get(intNum-1);
        }

        Object[] write() {
            return new Object[]{intNum};
        }

        Object[] compare(LP compare, BusinessLogics BL,int intOff) {
            return new Object[]{compare,intNum,intNum+intOff};
        }
    }

    private static class LMI<P extends PropertyInterface> extends LI {
        LP<P> lp;
        int[] mapInt;

        private LMI(LP<P> lp) {
            this.lp = lp;
            this.mapInt = new int[lp.listInterfaces.size()];
        }

        <T extends PropertyInterface<T>> PropertyInterfaceImplement<T> map(List<T> interfaces) {
            PropertyMapImplement<P,T> mapRead = new PropertyMapImplement<P,T>(lp.property);
            for(int i=0;i<lp.listInterfaces.size();i++)
                mapRead.mapping.put(lp.listInterfaces.get(i),interfaces.get(mapInt[i]-1));
            return mapRead;
        }

        Object[] write() {
            Object[] result = new Object[mapInt.length+1];
            result[0] = lp;
            for(int i=0;i<mapInt.length;i++)
                result[i+1] = mapInt[i];
            return result;
        }

        Object[] compare(LP compare, BusinessLogics BL, int intOff) {
            int lmiLen = mapInt.length;
            Object[] common = new Object[lmiLen*2+1];
            Object[] shift = new Object[lmiLen+1]; shift[0] = lp;
            for(int j=1;j<=lmiLen;j++) {
                shift[j] = j + lmiLen;
                common[j] = mapInt[j-1];
                common[j+lmiLen] = mapInt[j-1] + intOff;
            }
            common[0] = BL.addJProp(compare,BaseUtils.add(BL.directLI(lp),shift));
            return common;
        }
    }

    public static Object[] getUParams(LP[] props,int exoff) {
        int intNum = props[0].listInterfaces.size();
        Object[] params = new Object[props.length*(1+intNum+exoff)];
        for(int i=0;i<props.length;i++) {
            if(exoff>0)
                params[i*(1+intNum+exoff)] = 1;
            params[i*(1+intNum+exoff)+exoff] = props[i];
            for(int j=1;j<=intNum;j++)
                params[i*(1+intNum+exoff)+exoff+j] = j;
        }
        return params;
    }

    protected Object[] directLI(LP prop) {
        return getUParams(new LP[]{prop},0);
    }

    // считывает "линейные" имплементации
    static List<LI> readLI(Object[] params) {
        List<LI> result = new ArrayList<LI>();
        for(int i=0;i<params.length;i++)
            if(params[i] instanceof Integer)
                result.add(new LII((Integer)params[i]));
            else {
                LMI impl = new LMI((LP) params[i]);
                for(int j=0;j<impl.mapInt.length;j++)
                    impl.mapInt[j] = (Integer)params[i+j+1];
                i += impl.mapInt.length;
                result.add(impl);
            }
        return result;
    }                               

    static Object[] writeLI(List<LI> linearImpl) {
        Object[][] objectLI = new Object[linearImpl.size()][];
        for(int i=0;i<linearImpl.size();i++)
            objectLI[i] = linearImpl.get(i).write();
        int size = 0;
        for(Object[] li : objectLI)
            size += li.length;
        Object[] result = new Object[size]; int i = 0;
        for(Object[] li : objectLI)
            for(Object param : li)
                result[i++] = param;
        return result; 
    }

    static <T extends PropertyInterface> List<PropertyInterfaceImplement<T>> mapLI(List<LI> linearImpl, List<T> interfaces) {
        List<PropertyInterfaceImplement<T>> result = new ArrayList<PropertyInterfaceImplement<T>>();
        for(LI impl : linearImpl)
            result.add(impl.map(interfaces));
        return result;
    }

    public static <T extends PropertyInterface> List<PropertyInterfaceImplement<T>> readImplements(List<T> listInterfaces,Object... params) {
        return mapLI(readLI(params),listInterfaces);
    }

    private <T extends LC<?,?>> T addControl(AbstractGroup group, T lp) {
        controls.add(lp.property);
        if(group!=null) group.add(lp.property);
        return lp;
    }

    protected LP addJProp(LP mainProp, Object... params) {
        return addJProp("sys", mainProp, params);
    }

    protected LP addJProp(String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup)null, caption, mainProp, params);
    }

    protected LP addJProp(String sID, String caption, LP mainProp, Object... params) {
        return addJProp(null, sID, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, String caption, LP mainProp, Object... params) {
        return addJProp(group, genSID(), caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, boolean implementChange, String caption, LP mainProp, Object... params) {
        return addJProp(group, implementChange, genSID(), caption, mainProp, params);
    }

    protected int getIntNum(Object[] params) {
        int intNum = 0;
        for(Object param : params)
            if(param instanceof Integer)
                intNum = BaseUtils.max(intNum,(Integer)param);
        return intNum;
    }

    public static <T extends PropertyInterface,P extends PropertyInterface> PropertyImplement<PropertyInterfaceImplement<P>,T> mapImplement(LP<T> property,List<PropertyInterfaceImplement<P>> propImpl) {
        int mainInt = 0;
        Map<T,PropertyInterfaceImplement<P>> mapping = new HashMap<T, PropertyInterfaceImplement<P>>();
        for(PropertyInterfaceImplement<P> implement : propImpl) {
            mapping.put(property.listInterfaces.get(mainInt),implement);
            mainInt++;
        }
        return new PropertyImplement<PropertyInterfaceImplement<P>,T>(property.property,mapping);
    }

    protected LP addJProp(AbstractGroup group, String sID, String caption, LP mainProp, Object... params) {
        return addJProp(group, false, sID, caption, mainProp, params);
    }

    protected LP addJProp(boolean implementChange, LP mainProp, Object... params) {
        return addJProp(null, implementChange, genSID(), "sys", mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, boolean implementChange, String sID, String caption, LP mainProp, Object... params) {

        JoinProperty<?> property = new JoinProperty(sID, caption, getIntNum(params), implementChange);

        LP listProperty = new LP<JoinProperty.Interface>(property);
        property.implement = mapImplement(mainProp,readImplements(listProperty.listInterfaces,params));

        return addControl(group, listProperty);
    }

    private <T extends PropertyInterface> LP addGProp(AbstractGroup group, String sID, String caption, LP<T> groupProp, boolean sum, Object... params) {

        GroupProperty<T> property;
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        if(sum)
            property = new SumGroupProperty<T>(sID, caption, listImplements, groupProp.property);
        else
            property = new MaxGroupProperty<T>(sID, caption, listImplements, groupProp.property);

        return mapLGProp(group,property,listImplements);
    }

    private <P extends PropertyInterface,L extends PropertyInterface> LP mapLProp(AbstractGroup group,PropertyMapImplement<L, P> implement, LP<P> property) {
        return addControl(group, new LP<L>(implement.property,BaseUtils.mapList(property.listInterfaces,BaseUtils.reverse(implement.mapping))));
    }

    private <P extends PropertyInterface,L extends PropertyInterface> LP mapLGProp(AbstractGroup group,PropertyImplement<PropertyInterfaceImplement<P>, L> implement, List<PropertyInterfaceImplement<P>> listImplements) {
        return addControl(group, new LP<L>(implement.property,BaseUtils.mapList(listImplements,BaseUtils.reverse(implement.mapping))));
    }

    private <P extends PropertyInterface> LP mapLGProp(AbstractGroup group, GroupProperty<P> property, List<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group,new PropertyImplement<PropertyInterfaceImplement<P>,GroupProperty.Interface<P>>(property,property.getMapInterfaces()),listImplements);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String caption, LP<P> sum, boolean percent, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, genSID(), caption, sum, percent, ascending, includeLast, partNum, params);
    }
    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String sID, String caption, LP<P> sum, boolean percent, boolean ascending, boolean includeLast, int partNum, Object... params) {
        List<LI> li = readLI(params);

        Collection<PropertyInterfaceImplement<P>> partitions = mapLI(li.subList(0,partNum),sum.listInterfaces);
        OrderedMap<PropertyInterfaceImplement<P>,Boolean> orders = new OrderedMap<PropertyInterfaceImplement<P>, Boolean>(mapLI(li.subList(partNum,li.size()),sum.listInterfaces), ascending);

        PropertyMapImplement<?, P> orderProperty;
        if(percent)
            orderProperty = DerivedProperty.createPOProp(sID, caption, sum.property, partitions, orders, includeLast);
        else
            orderProperty = DerivedProperty.createOProp(sID, caption, sum.property, partitions, orders, includeLast);

        return mapLProp(group, orderProperty, sum);
    }

    protected <R extends PropertyInterface,L extends PropertyInterface> LP addUGProp(AbstractGroup group, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        return addUGProp(group, genSID(), caption, ascending, restriction, ungroup, params);
    }
    protected <R extends PropertyInterface,L extends PropertyInterface> LP addUGProp(AbstractGroup group, String sID, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        List<LI> li = readLI(params);

        Map<L,PropertyInterfaceImplement<R>> groupImplement = new HashMap<L, PropertyInterfaceImplement<R>>();
        for(int i=0;i<ungroup.listInterfaces.size();i++)
            groupImplement.put(ungroup.listInterfaces.get(i),li.get(i).map(restriction.listInterfaces));
        OrderedMap<PropertyInterfaceImplement<R>,Boolean> orders = new OrderedMap<PropertyInterfaceImplement<R>, Boolean>(mapLI(li.subList(ungroup.listInterfaces.size(),li.size()),restriction.listInterfaces), ascending);
        return mapLProp(group, DerivedProperty.createUGProp(sID, caption, new PropertyImplement<PropertyInterfaceImplement<R>, L>(ungroup.property,groupImplement), orders, restriction.property),restriction);
    }
    
/*
    // свойство обратное группируещему - для этого задается ограничивающее свойство, результирующее св-во с группировочными, порядковое св-во
    protected LF addUGProp(AbstractGroup group, String caption, LF maxGroupProp, LF unGroupProp, Object... params) {
        List<LI> lParams = readLI(params);
        List<LI> lUnGroupParams = lParams.subList(0,unGroupProp.listInterfaces.size());
        List<LI> orderParams = lParams.subList(unGroupProp.listInterfaces.size(),lParams.size());

        int intNum = maxGroupProp.listInterfaces.size();

        // "двоим" интерфейсы, для результ. св-ва
        // ставим equals'ы на группировочные свойства (раздвоенные)
        List<Object[]> groupParams = new ArrayList<Object[]>();
        groupParams.add(directLI(maxGroupProp));
        for(LI li : lUnGroupParams)
            groupParams.add(li.compare(equals2, this, intNum));

        boolean[] andParams = new boolean[groupParams.size()-1];
        for(int i=0;i<andParams.length;i++)
            andParams[i] = false;
        LF groupPropSet = addJProp(addAFProp(andParams),BaseUtils.add(groupParams));

        for(int i=0;i<intNum;i++) { // докинем не достающие порядки
            boolean found = false;
            for(LI order : orderParams)
                if(order instanceof LII && ((LII)order).intNum==i+1) {
                    found = true;
                    break;
                }
            if(!found)
                orderParams.add(new LII(i+1));
        }

        // ставим на предшествие сначала order'а, потом всех интерфейсов
        LF[] orderProps = new LF[orderParams.size()];
        for(int i=0;i<orderParams.size();i++) {
            orderProps[i] = (addJProp(and1, BaseUtils.add(directLI(groupPropSet),orderParams.get(i).compare(greater2, this, intNum))));
            groupPropSet = addJProp(and1, BaseUtils.add(directLI(groupPropSet),orderParams.get(i).compare(equals2, this, intNum)));
        }
        LF groupPropPrev = addSUProp(Union.OVERRIDE, orderProps);

        // группируем суммируя по "задвоенным" св-вам maxGroup
        Object[] remainParams = new Object[intNum];
        for(int i=1;i<=intNum;i++)
            remainParams[i-1] = i+intNum;
        LF remainPrev = addSGProp(groupPropPrev, remainParams);

        // создадим группировочное св-во с маппом на общий интерфейс, нужно поубирать "дырки"
        

        // возвращаем MIN(unGroup-MU(prevGroup,0(maxGroup)),maxGroup) и не unGroup<=prevGroup
        LF zeroQuantity = addJProp(and1, BaseUtils.add(new Object[]{vzero},directLI(maxGroupProp)));
        LF zeroRemainPrev = addSUProp(Union.OVERRIDE , zeroQuantity, remainPrev);
        LF calc = addSFProp("prm3+prm1-prm2-GREATEST(prm3,prm1-prm2)",DoubleClass.instance,3);
        LF maxRestRemain = addJProp(calc, BaseUtils.add(BaseUtils.add(unGroupProp.write(),directLI(zeroRemainPrev)),directLI(maxGroupProp)));
        LF exceed = addJProp(groeq2, BaseUtils.add(directLI(remainPrev),unGroupProp.write()));
        return addJProp(group, caption, andNot1, BaseUtils.add(directLI(maxRestRemain),directLI(exceed)));
    }
  */
    protected LP addSGProp(LP groupProp, Object... params) {
        return addSGProp("sys", groupProp, params);
    }
    protected LP addSGProp(String caption, LP groupProp, Object... params) {
        return addSGProp((AbstractGroup)null, caption, groupProp, params);
    }
    protected LP addSGProp(AbstractGroup group, String caption, LP groupProp, Object... params) {
        return addSGProp(group, genSID(), caption, groupProp, params);
    }
    protected LP addSGProp(String sID, String caption, LP groupProp, Object... params) {
        return addSGProp(null, sID, caption, groupProp, params);
    }
    protected LP addSGProp(AbstractGroup group, String sID, String caption, LP groupProp, Object... params) {
        return addGProp(group, sID, caption, groupProp, true, params);
    }

    protected LP addMGProp(LP groupProp, Object... params) {
        return addMGProp(null, genSID(), "sys", groupProp, params);
    }
    protected LP addMGProp(AbstractGroup group, String sID, String caption, LP groupProp, Object... params) {
        return addMGProp(group,new String[]{sID},new String[]{caption}, 0, groupProp, params)[0];
    }
    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, String[] ids, String[] captions, int extra, LP<T> groupProp, Object... params) {
        return addMGProp(group, false, ids, captions, extra, groupProp, params);
    }
    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, boolean persist, String[] ids, String[] captions, int extra, LP<T> groupProp, Object... params) {
        LP[] result = new LP[extra+1];

        Collection<Property> suggestPersist = new ArrayList<Property>();

        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        List<PropertyInterfaceImplement<T>> groupImplements = listImplements.subList(extra, listImplements.size());
        List<PropertyImplement<PropertyInterfaceImplement<T>, ?>> mgProps = DerivedProperty.createMGProp(ids, captions, groupProp.property, baseClass,
                listImplements.subList(0,extra), new HashSet<PropertyInterfaceImplement<T>>(groupImplements), suggestPersist);

        if(persist)
            for(Property property : suggestPersist)
                persistents.add((AggregateProperty) addControl(null, new LP(property)).property);

        for(int i=0;i<mgProps.size();i++)
            result[i] = mapLGProp(group, mgProps.get(i), groupImplements);
        return result;

        /*
        List<LI> li = readLI(params);
        Object[] interfaces = writeLI(li.subList(extra,li.size())); // "вырежем" группировочные интерфейсы

        LF[] result = new LF[extra+1];
        int i = 0;
        do {
            result[i] = addGProp(group,ids[i],captions[i],groupProp,false,interfaces);
            if(i<extra) // если не последняя
                groupProp = addJProp(and1, BaseUtils.add(li.get(i).write(),directLI( // само свойство
                        addJProp(equals2, BaseUtils.add(directLI(groupProp),directLI( // только те кто дает предыдущий максимум
                        addJProp(result[i], interfaces))))))); // предыдущий максимум
        } while (i++<extra);
        return result;*/
    }

    protected <T extends PropertyInterface,P extends PropertyInterface> LP addCGProp(AbstractGroup group, String sID, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        CycleGroupProperty<T,P> property = new CycleGroupProperty<T,P>(sID, caption, listImplements, groupProp.property, dataProp.property);

        // нужно добавить ограничение на уникальность
        controls.add(property.getConstrainedProperty());

        return mapLGProp(group, property, listImplements);
    }

    protected <T extends PropertyInterface,P extends PropertyInterface> LP addDGProp(int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(null, "sys", orders, ascending, groupProp, params);
    }
    protected <T extends PropertyInterface,P extends PropertyInterface> LP addDGProp(AbstractGroup group, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(group, genSID(), caption, orders, ascending, groupProp, params);
    }
    protected <T extends PropertyInterface,P extends PropertyInterface> LP addDGProp(AbstractGroup group, String sID, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces,params);
        int intNum = listImplements.size();

        // читаем groupProp, implements его для группировки, restriction с map'ом и orders с map'ом на restriction
        List<PropertyInterfaceImplement<T>> groupImplements = listImplements.subList(0, intNum - orders - 1);
        DistrGroupProperty<T,P> property = new DistrGroupProperty<T,P>(sID, caption, groupImplements, groupProp.property,
                new OrderedMap<PropertyInterfaceImplement<T>, Boolean>(listImplements.subList(intNum-orders,intNum),ascending),
                (PropertyMapImplement<P,T>) listImplements.get(intNum-orders-1));

        // нужно добавить ограничение на уникальность
        return mapLGProp(group, property, groupImplements);
    }

    protected LP addUProp(AbstractGroup group, String sID, String caption, Union unionType, Object... params) {

        int intNum = ((LP)params[unionType==Union.SUM?1:0]).listInterfaces.size();

        UnionProperty property = null;
        int extra = 0;
        switch(unionType) {
            case MAX:
                property = new MaxUnionProperty(sID,caption,intNum);
                break;
            case SUM:
                property = new SumUnionProperty(sID,caption,intNum);
                extra = 1;
                break;
            case OVERRIDE:
                property = new OverrideUnionProperty(sID,caption,intNum);
                break;
        }

        LP listProperty = new LP<UnionProperty.Interface>(property);

        for(int i=0;i<params.length/(intNum+1+extra);i++) {
            Integer offs = i*(intNum+1+extra);
            LP<?> opImplement = (LP)params[offs+extra];
            PropertyMapImplement operand = new PropertyMapImplement(opImplement.property);
            for(int j=0;j<intNum;j++)
                operand.mapping.put(opImplement.listInterfaces.get(((Integer)params[offs+1+extra+j])-1),listProperty.listInterfaces.get(j));

            switch(unionType) {
                case MAX:
                    ((MaxUnionProperty)property).operands.add(operand);
                    break;
                case SUM:
                    ((SumUnionProperty)property).operands.put(operand,(Integer)params[offs]);
                    break;
                case OVERRIDE:
                    ((OverrideUnionProperty)property).operands.add(operand);
                    break;
            }
        }

        return addControl(group, listProperty);
    }

    // объединение классовое (непересекающихся) свойств
    protected LP addCUProp(LP... props) {
        return addCUProp("sys", props);
    }
    protected LP addCUProp(String caption, LP... props) {
        return addCUProp((AbstractGroup)null, caption, props);
    }
    protected LP addCUProp(AbstractGroup group, String caption, LP... props) {
        return addCUProp(group, genSID(), caption, props);
    }
    protected LP addCUProp(String sID, String caption, LP... props) {
        return addCUProp(null, sID, caption, props);
    }

    Collection<LP[]> checkCUProps = new ArrayList<LP[]>();
    // объединяет разные по классам св-ва
    protected LP addCUProp(AbstractGroup group, String sID, String caption, LP... props) {
        assert checkCUProps.add(props);
        return addUProp(group,sID,caption,Union.OVERRIDE, getUParams(props, 0));
    }

    // разница
    protected LP addDUProp(LP prop1, LP prop2) {
        return addDUProp("sys", prop1, prop2);
    }
    protected LP addDUProp(String caption, LP prop1, LP prop2) {
        return addDUProp((AbstractGroup)null, caption, prop1, prop2);
    }
    protected LP addDUProp(AbstractGroup group, String caption, LP prop1, LP prop2) {
        return addDUProp(group, genSID(), caption, prop1, prop2);
    }
    protected LP addDUProp(String sID, String caption, LP prop1, LP prop2) {
        return addDUProp(null, sID, caption, prop1, prop2);
    }
    protected LP addDUProp(AbstractGroup group, String sID, String caption, LP prop1, LP prop2) {
        int intNum = prop1.listInterfaces.size();
        Object[] params = new Object[2*(2+intNum)];
        params[0] = 1; params[1] = prop1;
        for(int i=0;i<intNum;i++)
            params[2+i] = i+1;
        params[2+intNum] = -1; params[3+intNum] = prop2;
        for(int i=0;i<intNum;i++)
            params[4+intNum+i] = i+1;
        return addUProp(group,sID,caption,Union.SUM, params);
    }

    // объединение пересекающихся свойств
    protected LP addSUProp(Union unionType, LP... props) {
        return addSUProp("sys", unionType, props);
    }
    protected LP addSUProp(String caption, Union unionType, LP... props) {
        return addSUProp((AbstractGroup)null, caption, unionType, props);
    }
    protected LP addSUProp(AbstractGroup group, String caption, Union unionType, LP... props) {
        return addSUProp(group, genSID(), caption, unionType, props);
    }
    protected LP addSUProp(String sID, String caption, Union unionType, LP... props) {
        return addSUProp(null, sID, caption, unionType, props);
    }
    Collection<LP[]> checkSUProps = new ArrayList<LP[]>();
    // объединяет разные по классам св-ва
    protected LP addSUProp(AbstractGroup group, String sID, String caption, Union unionType, LP... props) {
        assert checkSUProps.add(props);
        return addUProp(group,sID,caption,unionType,getUParams(props,(unionType==Union.SUM?1:0)));
    }

    protected LP[] addMUProp(AbstractGroup group, String[] ids, String[] captions, int extra, LP... props) {
        int propNum = props.length/(1+extra);
        LP[] maxProps = Arrays.copyOfRange(props,0,propNum);
        
        LP[] result = new LP[extra+1];
        int i = 0;
        do {
            result[i] = addUProp(group,ids[i],captions[i],Union.MAX,getUParams(maxProps,0));
            if(i<extra) { // если не последняя
                for(int j=0;j<propNum;j++)
                    maxProps[j] = addJProp(and1, BaseUtils.add(directLI(props[(i+1)*propNum+j]),directLI( // само свойство
                    addJProp(equals2, BaseUtils.add(directLI(maxProps[j]),directLI(result[i])))))); // только те кто дает предыдущий максимум
            }
        } while (i++<extra);
        return result;
    }

    protected void addConstraint(LP<?> lp, boolean checkChange) {
        lp.property.setConstraint(checkChange);
    }

    private boolean intersect(LP[] props) {
        for(int i=0;i<props.length;i++)
            for(int j=i+1;j<props.length;j++)
                if(((LP<?>) props[i]).intersect((LP<?>)props[j]))
                    return true;
        return false;
    }

    public final static boolean checkClasses = false;
    private boolean checkProps() {
        if(checkClasses)
            for(Property prop : getProperties()) {
                System.out.println("Checking property : "+prop+"...");
                assert prop.check();
            }
        for(LP[] props : checkCUProps) {
            System.out.println("Checking class properties : "+props+"...");
            assert !intersect(props);
        }
        for(LP[] props : checkSUProps) {
            System.out.println("Checking union properties : "+props+"...");
            assert intersect(props);
        }
        return true;
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

/*    // случайным образом генерирует классы
    void randomClasses(Random randomizer) {
        int customClasses = randomizer.nextInt(20);//
        List<CustomClass> objClasses = new ArrayList<CustomClass>();
        objClasses.add(customClass);
        for(int i=0;i<customClasses;i++) {
            CustomClass customClass = new CustomClass(i+10000, "Случайный класс"+i);
            int parents = randomizer.nextInt(2) + 1;
            for(int j=0;j<parents;j++)
                customClass.addParent(objClasses.get(randomizer.nextInt(objClasses.size())));
            objClasses.add(customClass);
        }
    }

    // случайным образом генерирует св-ва
    void randomProperties(Random randomizer) {

        List<CustomClass> classes = new ArrayList<CustomClass>();
        customClass.fillChilds(classes);

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
            CustomClass[] interfaceClasses = new CustomClass[intCount];
            for(int j=0;j<intCount;j++)
                interfaceClasses[j] = classes.get(randomizer.nextInt(classes.size()));

            // DataProperty
            DataProperty dataProp = new DataProperty("prop"+i,interfaceClasses,tableFactory,(i%4==0? RemoteClass.integer :classes.get(randomizer.nextInt(classes.size()))));
            dataProp.caption = "Data Property " + i;
            // генерируем классы

            randProps.add(dataProp);
            randObjProps.add(dataProp);
            if(dataProp.getBaseClass().getCommonClass() instanceof IntegralClass)
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

                for(PropertyInterface propertyInterface : (Collection<PropertyInterface>)relProp.implement.property.interfaces) {
                    // генерируем случайно map'ы на эти интерфейсы
                    if(!(relProp.implement.property instanceof FormulaProperty) && randomizer.nextBoolean()) {
                        if(availRelInt.size()==0) {
                            correct = false;
                            break;
                        }
                        PropertyInterface mapInterface = availRelInt.get(randomizer.nextInt(availRelInt.size()));
                        relProp.implement.mapping.put(propertyInterface,mapInterface);
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
                        relProp.implement.mapping.put(propertyInterface,impProp);
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

                Collection<Function> interfaces = new ArrayList<Function>();

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

                    interfaces.add(new Function(j,implement));
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
                    property.operands.put(operand,1);
                }

                if(correct)
                    genProp = property;
            }


            if(genProp!=null && genProp.isInInterface(new HashMap()) && genProp.hasAllKeys()) {
                genProp.caption = resType + " " + i;
                // проверим что есть в интерфейсе и покрыты все ключи
                System.out.print(resType+"-");
                randProps.add(genProp);
                randObjProps.add(genProp);
                if(genProp.getBaseClass().getCommonClass() instanceof IntegralClass)
                    randIntegralProps.add(genProp);
            }
        }

        property.addAll(randProps);

        System.out.println();
    }
   */
    // случайным образом генерирует имплементацию
    void randomImplement(Random randomizer) {
        List<CustomClass> classes = new ArrayList<CustomClass>(baseClass.getChilds());

        // заполнение физ модели
        int implementCount = randomizer.nextInt(8);
        for(int i=0;i<implementCount;i++) {
            int objCount = randomizer.nextInt(3)+1;
            CustomClass[] randomClasses = new CustomClass[objCount]; 
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
        for(Property property : getProperties()) {
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
        for(Property property : getProperties())
            if(property instanceof DataProperty)
                dataProperties.add((DataProperty)property);

        DataSession session = createSession(adapter);

        List<ConcreteCustomClass> addClasses = new ArrayList<ConcreteCustomClass>();
        baseClass.fillConcreteChilds(addClasses);
        for(ConcreteCustomClass addClass : addClasses) {
            int objectAdd = randomizer.nextInt(10)+1;
            for(int ia=0;ia<objectAdd;ia++)
                session.addObject(addClass,null);
        }

        session.apply(this);

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
            addClasses = new ArrayList<ConcreteCustomClass>();
            baseClass.fillConcreteChilds(addClasses);
            int objectAdd = randomizer.nextInt(5);
            for(int ia=0;ia<objectAdd;ia++)
                session.addObject(addClasses.get(randomizer.nextInt(addClasses.size())),null);

            int propertiesChanged = randomizer.nextInt(8)+1;
            for(int ip=0;ip<propertiesChanged;ip++) {
                // берем случайные n св-в
                DataProperty changeProp = dataProperties.get(randomizer.nextInt(dataProperties.size()));
                int numChanges = randomizer.nextInt(3)+1;
                for(int in=0;in<numChanges;in++) {
/*                    // теперь определяем класс найденного объекта
                    Class valueClass = null;
                    if(ChangeProp.Value instanceof CustomClass)
                        valueClass = customClass.FindClassID(ValueObject);
                    else
                        valueClass = ChangeProp.Value;*/

                    // генерим рандомные объекты этих классов
                    Map<ClassPropertyInterface, DataObject> keys = new HashMap<ClassPropertyInterface, DataObject>();
                    for(ClassPropertyInterface propertyInterface : changeProp.interfaces)
                        keys.put(propertyInterface,propertyInterface.interfaceClass.getRandomObject(session, randomizer));

                    ObjectValue valueObject;
                    if(randomizer.nextInt(10)<8)
                        valueObject = changeProp.value.getRandomObject(session, randomizer);
                    else
                        valueObject = NullValue.instance;

                    session.changeProperty(changeProp, keys, valueObject, false);
                }
            }

/*            for(DataProperty Property : Session.propertyViews) {
                Property.OutChangesTable(Adapter, Session);
            }*/

            session.apply(this);
            checkPersistent(session);
        }

        session.close();
    }

    // полностью очищает базу
    protected void clean(SQLSession session) throws SQLException {
        // удаляем все объекты
        session.deleteKeyRecords(baseClass.table, new HashMap<KeyField, Integer>());

        // удаляем все св-ва
        for(ImplementTable table : tableFactory.getImplementTables().values())
            session.deleteKeyRecords(table, new HashMap<KeyField, Object>());
    }

    public static boolean autoFillDB = false;
    public static int autoIDCounter = 0;
    static int autoSeed = 1400;
    public void autoFillDB(Map<ConcreteCustomClass, Integer> classQuantity, Map<DataProperty, Integer> propQuantity, Map<DataProperty, Set<ClassPropertyInterface>> propNotNull) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        System.out.print("Идет заполнение базы данных...");

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

        // берем все конкретные классы
        Map<DataObject,String> objectNames = new HashMap<DataObject, String>();
        List<ConcreteCustomClass> concreteClasses = new ArrayList<ConcreteCustomClass>();
        baseClass.fillConcreteChilds(concreteClasses);

        // генерируем списки ИД объектов по классам
        List<CustomClass> classes = new ArrayList<CustomClass>(baseClass.getChilds());
        Map<CustomClass,List<DataObject>> objects = new HashMap<CustomClass, List<DataObject>>();
        for(CustomClass fillClass : classes)
            objects.put(fillClass,new ArrayList<DataObject>());

        for(ConcreteCustomClass fillClass : concreteClasses)
            if(fillClass.children.size()==0) {
                System.out.println("Класс : "+fillClass.caption);

                Integer quantity = classQuantity.get(fillClass);
                if(quantity==null) quantity = 1;

                List<DataObject> listObjects = new ArrayList<DataObject>();
                for(int i=0;i<quantity;i++) {
                    DataObject idObject = session.addObject(fillClass,null);
                    listObjects.add(idObject);
                    objectNames.put(idObject,fillClass.caption+" "+(i+1));
                }

                Set<CustomClass> parents = new HashSet<CustomClass>();
                fillClass.fillParents(parents);

                for(CustomClass customClass : parents)
                    objects.get(customClass).addAll(listObjects);
            }

        Random randomizer = new Random(autoSeed);

        // бежим по св-вам
        for(Property abstractProperty : getProperties())
            if(abstractProperty instanceof DataProperty) {
                DataProperty property = (DataProperty)abstractProperty;

                System.out.println("Свойство : "+property.caption);

                Set<ClassPropertyInterface> interfaceNotNull = propNotNull.get(property);
                if(interfaceNotNull==null) interfaceNotNull = new HashSet<ClassPropertyInterface>();
                Integer quantity = propQuantity.get(property);
                if(quantity==null) {
                    quantity = 1;
                    for(ClassPropertyInterface propertyInterface : property.interfaces)
                        if(!interfaceNotNull.contains(propertyInterface))
                            quantity = quantity * propertyInterface.interfaceClass.getRandomList(objects).size();

                    if(quantity > 1)
                        quantity = (int)(quantity * 0.5);
                }

                Map<ClassPropertyInterface,List<DataObject>> mapInterfaces = new HashMap<ClassPropertyInterface, List<DataObject>>();
                if(propNotNull.containsKey(property))
                    for(ClassPropertyInterface propertyInterface : interfaceNotNull)
                        mapInterfaces.put(propertyInterface,propertyInterface.interfaceClass.getRandomList(objects));

                // сначала для всех PropNotNull генерируем все возможные Map<ы>
                for(Map<ClassPropertyInterface,DataObject> notNulls : new Combinations<ClassPropertyInterface,DataObject>(mapInterfaces)) { //
                    int randomInterfaces = 0;
                    while(randomInterfaces<quantity) {
                        Map<ClassPropertyInterface,DataObject> randomIteration = new HashMap<ClassPropertyInterface,DataObject>(notNulls);
                        for(ClassPropertyInterface propertyInterface : property.interfaces)
                            if(!notNulls.containsKey(propertyInterface))
                                randomIteration.put(propertyInterface,BaseUtils.getRandom(propertyInterface.interfaceClass.getRandomList(objects),randomizer));

                        DataObject valueObject = null;
                        if(property.value instanceof StringClass) {
                            String objectName = "";
                            for(ClassPropertyInterface propertyInterface : property.interfaces)
                                objectName += objectNames.get(randomIteration.get(propertyInterface)) + " ";
                            valueObject = new DataObject(objectName,StringClass.get(50));
                        } else
                            valueObject = BaseUtils.getRandom(property.value.getRandomList(objects),randomizer);
                        session.changeProperty(property,randomIteration,valueObject,false);
                        randomInterfaces++;
                    }
                }
            }

        System.out.println("Применение изменений...");
        session.apply(this);

        session.startTransaction();

        for(Map.Entry<AggregateProperty,PropertyField> save : savePersistents.entrySet()) {
            save.getKey().field = save.getValue();
            persistents.add(save.getKey());
        }

        recalculateAggregations(session,savePersistents.keySet());

        session.commitTransaction();

        IDTable.instance.reserveID(session, IDTable.OBJECT, autoIDCounter);

        session.close();

        System.out.println("База данных была успешно заполнена");

        autoFillDB = false;
    }

    private void recalculateAggregations(SQLSession session,Collection<AggregateProperty> recalculateProperties) throws SQLException {

        for(Property property : getPropertyList())
            if(property instanceof AggregateProperty) {
                AggregateProperty dependProperty = (AggregateProperty)property;
                if(recalculateProperties.contains(dependProperty)) {
                    System.out.print("Идет перерасчет аггрегированного св-ва ("+dependProperty+")... ");
                    dependProperty.recalculateAggregation(session);
                    System.out.println("Done");
                }
            }
    }
}
