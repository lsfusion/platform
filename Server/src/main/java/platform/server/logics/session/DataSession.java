package platform.server.logics.session;

import platform.interop.Compare;
import platform.server.data.*;
import platform.server.data.query.ChangeQuery;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.BusinessLogics;
import platform.server.logics.ObjectValue;
import platform.server.logics.classes.ObjectClass;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.*;
import platform.server.logics.data.TableFactory;
import platform.server.logics.properties.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class DataSession  {

    public Connection connection;
    public SQLSyntax syntax;

    public DataChanges changes = new DataChanges();
    public Map<PropertyUpdateView,DataChanges> incrementChanges = new HashMap<PropertyUpdateView, DataChanges>();

    public Map<Property, Property.Change> propertyChanges = new HashMap<Property, Property.Change>();
    public <P extends PropertyInterface> Property<P>.Change getChange(Property<P> Property) {
        return propertyChanges.get(Property);
    }

    public Map<RemoteClass,ClassSet> addChanges = new HashMap<RemoteClass, ClassSet>();
    public Map<RemoteClass, ClassSet> removeChanges = new HashMap<RemoteClass, ClassSet>();
    public Map<DataProperty, ValueClassSet<DataPropertyInterface>> dataChanges = new HashMap<DataProperty, ValueClassSet<DataPropertyInterface>>();

    TableFactory tableFactory;
    ObjectClass objectClass;

    int ID = 0;

    public DataSession(DataAdapter Adapter,int iID,TableFactory iTableFactory, ObjectClass iObjectClass) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        ID = iID;
        syntax = Adapter;
        tableFactory = iTableFactory;
        objectClass = iObjectClass;

        connection = Adapter.startConnection();
        tableFactory.fillSession(this);
    }

    public void restart(boolean Cancel) throws SQLException {

        if(Cancel)
            for(DataChanges ViewChanges : incrementChanges.values()) {
                ViewChanges.properties.addAll(changes.properties);
                ViewChanges.addClasses.addAll(changes.addClasses);
                ViewChanges.removeClasses.addAll(changes.removeClasses);
            }

        tableFactory.clearSession(this);
        changes = new DataChanges();
        NewClasses = new HashMap<Integer, RemoteClass>();
        BaseClasses = new HashMap<Integer, RemoteClass>();

        propertyChanges = new HashMap<Property, Property.Change>();
        addChanges = new HashMap<RemoteClass, ClassSet>();
        removeChanges = new HashMap<RemoteClass, ClassSet>();
        dataChanges = new HashMap<DataProperty, ValueClassSet<DataPropertyInterface>>();
    }

    Map<Integer, RemoteClass> NewClasses = new HashMap<Integer, RemoteClass>();
    // классы на момент выполнения
    public Map<Integer, RemoteClass> BaseClasses = new HashMap<Integer, RemoteClass>();

    private void putClassChanges(Set<RemoteClass> Changes, RemoteClass PrevClass,Map<RemoteClass,ClassSet> To) {
        for(RemoteClass Change : Changes) {
            ClassSet PrevChange = To.get(Change);
            if(PrevChange==null) PrevChange = new ClassSet();
            PrevChange.or(new ClassSet(PrevClass));
            To.put(Change,PrevChange);
        }
    }

    public void changeClass(Integer idObject, RemoteClass ToClass) throws SQLException {
        if(ToClass==null) ToClass = RemoteClass.base;

        Set<RemoteClass> AddClasses = new HashSet<RemoteClass>();
        Set<RemoteClass> RemoveClasses = new HashSet<RemoteClass>();
        RemoteClass PrevClass = getObjectClass(idObject);
        ToClass.getDiffSet(PrevClass,AddClasses,RemoveClasses);

        putClassChanges(AddClasses,PrevClass, addChanges);
        tableFactory.addClassTable.changeClass(this,idObject,AddClasses,false);
        tableFactory.removeClassTable.changeClass(this,idObject,AddClasses,true);

        putClassChanges(RemoveClasses,PrevClass, removeChanges);
        tableFactory.removeClassTable.changeClass(this,idObject,RemoveClasses,false);
        tableFactory.addClassTable.changeClass(this,idObject,RemoveClasses,true);

        if(!NewClasses.containsKey(idObject))
            BaseClasses.put(idObject,PrevClass);
        NewClasses.put(idObject,ToClass);

        changes.addClasses.addAll(AddClasses);
        changes.removeClasses.addAll(RemoveClasses);

        for(DataChanges ViewChanges : incrementChanges.values()) {
            ViewChanges.addClasses.addAll(AddClasses);
            ViewChanges.removeClasses.addAll(RemoveClasses);
        }
    }

    public <T extends PropertyInterface> Object readProperty(Property<T> Property,Map<T, ObjectValue> Keys) throws SQLException {
        String ReadValue = "readvalue";
        JoinQuery<T,Object> ReadQuery = new JoinQuery<T, Object>(Property.interfaces);

        Map<T,Integer> KeyValues = new HashMap<T,Integer>();
        for(Map.Entry<T,ObjectValue> MapKey : Keys.entrySet())
            KeyValues.put(MapKey.getKey(), (Integer) MapKey.getValue().object);
        ReadQuery.putKeyWhere(KeyValues);

        ReadQuery.properties.put(ReadValue, getSourceExpr(Property,ReadQuery.mapKeys,new InterfaceClassSet<T>(new InterfaceClass<T>(Keys))));
        return ReadQuery.executeSelect(this).values().iterator().next().get(ReadValue);
    }

    public void changeProperty(DataProperty Property, Map<DataPropertyInterface, ObjectValue> Keys, Object NewValue, boolean externalID) throws SQLException {

        // если изменяем по внешнему коду, но сначала надо найти внутренний код, а затем менять
        if (externalID && NewValue!=null) {

            DataProperty<?> extPropID = Property.value.getExternalID();

            JoinQuery<DataPropertyInterface,String> query = new JoinQuery<DataPropertyInterface, String>(extPropID.interfaces);
            query.where = query.where.and(new CompareWhere(extPropID.getSourceExpr(query.mapKeys,extPropID.getClassSet(ClassSet.universal)),Property.getType().getExpr(NewValue), Compare.EQUALS));

            LinkedHashMap<Map<DataPropertyInterface,Integer>,Map<String,Object>> result = query.executeSelect(this);

            if (result.size() == 0) return;

            NewValue = result.keySet().iterator().next().values().iterator().next();
        }

        // запишем в таблицу
        // также заодно новые классы считаем
        Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
        InterfaceClass<DataPropertyInterface> InterfaceClass = new InterfaceClass<DataPropertyInterface>();
        for(Map.Entry<KeyField,DataPropertyInterface> Field : (Set<Map.Entry<KeyField,DataPropertyInterface>>)Property.dataTableMap.entrySet()) {
            Integer idObject = (Integer) Keys.get(Field.getValue()).object;
            InsertKeys.put(Field.getKey(), idObject);
            InterfaceClass.put(Field.getValue(),getBaseClassSet(idObject));
        }

        InsertKeys.put(Property.dataTable.property,Property.ID);

        Map<PropertyField,Object> InsertValues = new HashMap<PropertyField,Object>();
        InsertValues.put(Property.dataTable.value,NewValue);

        ClassSet ValueClass = Property.getBaseClass();
        if(ValueClass.intersect(ClassSet.getUp(objectClass)))
            ValueClass = getBaseClassSet((Integer) NewValue);

        UpdateInsertRecord(Property.dataTable,InsertKeys,InsertValues);

        // пометим изменения
        changes.properties.add(Property);

        ValueClassSet<DataPropertyInterface> DataChange = dataChanges.get(Property);
        if(DataChange==null) DataChange = new ValueClassSet<DataPropertyInterface>();
        DataChange.or(new ChangeClass<DataPropertyInterface>(new InterfaceClassSet<DataPropertyInterface>(InterfaceClass),ValueClass));
        dataChanges.put(Property,DataChange);

        for(DataChanges ViewChanges : incrementChanges.values())
            ViewChanges.properties.add(Property);
    }

    RemoteClass readClass(Integer idObject) throws SQLException {
        if(BusinessLogics.autoFillDB) return null;

        return objectClass.findClassID(tableFactory.objectTable.getClassID(this,idObject));
    }

    public RemoteClass getObjectClass(Integer idObject) throws SQLException {
        RemoteClass NewClass = NewClasses.get(idObject);
        if(NewClass==null)
            NewClass = readClass(idObject);
        if(NewClass==null)
            NewClass = RemoteClass.base;
        return NewClass;
    }

    ClassSet getBaseClassSet(Integer idObject) throws SQLException {
        if(idObject==null) return new ClassSet();
        RemoteClass BaseClass = BaseClasses.get(idObject);
        if(BaseClass==null)
            BaseClass = readClass(idObject);
        return new ClassSet(BaseClass);
    }

    // последний параметр
    public List<Property> update(PropertyUpdateView ToUpdate,Collection<RemoteClass> UpdateClasses) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)

        DataChanges ToUpdateChanges = incrementChanges.get(ToUpdate);
        if(ToUpdateChanges==null) ToUpdateChanges = changes;

        Collection<Property> ToUpdateProperties = ToUpdate.getUpdateProperties();
        Collection<Property> NoUpdateProperties = ToUpdate.getNoUpdateProperties();
        // сначала читаем инкрементные св-ва которые изменились
        List<Property> IncrementUpdateList = BusinessLogics.getChangedList(ToUpdateProperties,ToUpdateChanges,NoUpdateProperties);
        List<Property> UpdateList = BusinessLogics.getChangedList(IncrementUpdateList, changes,NoUpdateProperties);

        Map<Property,Integer> RequiredTypes = new HashMap<Property,Integer>();
        // пробежим вперед пометим свойства которые изменились, но неясно на что
        for(Property Property : UpdateList)
            RequiredTypes.put(Property,ToUpdateProperties.contains(Property)?0:null);
        Map<Property, Integer> IncrementTypes = getIncrementTypes(UpdateList, RequiredTypes);

        // запускаем IncrementChanges для этого списка
        for(Property Property : UpdateList) {
//            System.out.println(Property.caption);
//            if(Property.caption.equals("Всего с НДС"))
//                Property = Property;
            Property.Change Change = Property.incrementChanges(this,IncrementTypes.get(Property));
            // подгоняем тип
            Change.correct(RequiredTypes.get(Property));
//            System.out.println("inctype"+Property.caption+" "+IncrementTypes.get(Property));
//            Main.Session = this;
//            Change.out(this);
//            Main.Session = null;
            if(!(Property instanceof MaxGroupProperty) && ToUpdate.toSave(Property))
                Change.save(this);
//            Property.Out(this);            
//            System.out.println(Property.caption + " incComplexity : " + Change.Source.getComplexity());
//            Change.out(this);
/*            System.out.println(Property.caption+" - CHANGES");
            Property.OutChangesTable(this);
            System.out.println(Property.caption+" - CURRENT");
            Property.Out(this);
            Change.checkClasses(this);*/
            propertyChanges.put(Property,Change);
        }

        UpdateClasses.addAll(ToUpdateChanges.addClasses);
        UpdateClasses.addAll(ToUpdateChanges.removeClasses);

        // сбрасываем лог
        incrementChanges.put(ToUpdate,new DataChanges());

        return IncrementUpdateList;
    }

    // определяет на что считаться 0,1,2
    private Map<Property, Integer> getIncrementTypes(List<Property> UpdateList, Map<Property, Integer> RequiredTypes) {
        // бежим по списку (в обратном порядке) заполняем требования,
        Collections.reverse(UpdateList);
        // на какие значения читаться Persistent'ам
        Map<Property,Integer> IncrementTypes = new HashMap<Property,Integer>();
        // Waiter'ы св-ва которые ждут определившехся на выполнение св-в : не persistent и не 2
        Set<Property> ToWait = null;
        Map<Property,Set<Property>> Waiters = new HashMap<Property, Set<Property>>();
        for(Property Property : UpdateList) {
            Integer IncType = RequiredTypes.get(Property);
            // сначала проверим на Persistent и на "альтруистические" св-ва
            if(IncType==null || Property.isPersistent()) {
                ToWait = new HashSet<Property>();
                IncType = Property.getIncrementType(UpdateList, ToWait);
            }
            // если определившееся (точно 0 или 1) запустим Waiter'ов, соответственно вычистим
            if(IncType==null || (!Property.isPersistent() && !IncType.equals(2))) {
                for(Iterator<Map.Entry<Property,Set<Property>>> ie = Waiters.entrySet().iterator();ie.hasNext();) {
                    Map.Entry<Property,Set<Property>> Wait = ie.next();
                    if(Wait.getValue().contains(Property))
                        if(IncType==null) // докидываем еще Waiter'ов
                            Wait.getValue().addAll(ToWait);
                        else { // нашли нужный тип, remove'ся
                            fillChanges(Wait.getKey(), IncType, RequiredTypes, IncrementTypes);
                            ie.remove();
                        }
                }
            }
            if(IncType!=null)
                fillChanges(Property, IncType, RequiredTypes, IncrementTypes);
            else // св-во не знает пока чего хочет
                Waiters.put(Property, ToWait);
        }
        Collections.reverse(UpdateList);
        // еше могут остаться Waiter'ы, тогда возьмем первую не 2, иначе возьмем 0 (все чтобы еще LJ минимизировать)
        for(Property Property : UpdateList) {
            Integer IncType = IncrementTypes.get(Property);
            if(IncType==null) {
                for(Property WaitProperty : Waiters.get(Property)) {
                    Integer WaitType = IncrementTypes.get(WaitProperty);
                    if(!WaitType.equals(2)) IncType = WaitType;
                }
                if(IncType==null) IncType = 0;
                fillChanges(Property, IncType, RequiredTypes, IncrementTypes);
            }
        }
        return IncrementTypes;
    }

    private void fillChanges(Property Property, Integer incrementType, Map<Property, Integer> requiredTypes, Map<Property, Integer> incrementTypes) {
        incrementTypes.put(Property, incrementType);
        Property.fillRequiredChanges(incrementType, requiredTypes);
    }

    public void saveClassChanges() throws SQLException {

        for(Integer idObject : NewClasses.keySet()) {
            Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
            InsertKeys.put(tableFactory.objectTable.key, idObject);

            Map<PropertyField,Object> InsertProps = new HashMap<PropertyField,Object>();
            RemoteClass ChangeClass = NewClasses.get(idObject);
            InsertProps.put(tableFactory.objectTable.objectClass,ChangeClass!=null?ChangeClass.ID:null);

            UpdateInsertRecord(tableFactory.objectTable,InsertKeys,InsertProps);
        }
    }

    public <P extends PropertyInterface> ValueClassSet<P> getSourceClass(Property<P> Property) {
        ValueClassSet<P> Result = Property.getValueClassSet();
        Property<P>.Change Change = getChange(Property);
        if(Change!=null) {
            Result = new ValueClassSet<P>(Result);
            Result.or(Change.classes);
        }
        return Result;
    }

    // записывается в запрос с map'ом
    public <P extends PropertyInterface> SourceExpr getSourceExpr(Property<P> property, Map<P,? extends SourceExpr> joinImplement, InterfaceClassSet<P> joinClasses) {

        boolean inInterface = property.isInInterface(joinClasses);
        Property<P>.Change change = getChange(property);
        if(change!=null && !change.classes.getClassSet(ClassSet.universal).and(joinClasses).isEmpty()) {
            String value = "joinvalue";

            ChangeQuery<P,String> unionQuery = new ChangeQuery<P,String>(property.interfaces);

            if(inInterface) {
                JoinQuery<P,String> sourceQuery = new JoinQuery<P,String>(property.interfaces);
                SourceExpr valueExpr = property.getSourceExpr(sourceQuery.mapKeys, joinClasses);
                sourceQuery.properties.put(value, valueExpr);
                sourceQuery.and(valueExpr.getWhere());
                unionQuery.add(sourceQuery);
            }

            JoinQuery<P,String> newQuery = new JoinQuery<P,String>(property.interfaces);
            JoinExpr changeExpr = getChange(property).getExpr(newQuery.mapKeys, 0);
            newQuery.properties.put(value, changeExpr);
            newQuery.and(changeExpr.from.inJoin);
            unionQuery.add(newQuery);

            return (new Join<P,String>(unionQuery, joinImplement)).exprs.get(value);
        } else
        if(inInterface)
            return property.getSourceExpr(joinImplement, joinClasses);
        else
            return property.getType().getExpr(null);
    }

    boolean InTransaction = false;

    public void startTransaction() throws SQLException {
        InTransaction = true;

        if(!syntax.noAutoCommit())
            execute(syntax.startTransaction());
    }

    public void rollbackTransaction() throws SQLException {
        execute(syntax.rollbackTransaction());

        InTransaction = false;
    }

    public void commitTransaction() throws SQLException {
        execute(syntax.commitTransaction());

        InTransaction = false;
    }

    public void createTable(Table Table) throws SQLException {
        String CreateString = "";
        String KeyString = "";
        for(KeyField Key : Table.keys) {
            CreateString = (CreateString.length()==0?"":CreateString+',') + Key.GetDeclare(syntax);
            KeyString = (KeyString.length()==0?"":KeyString+',') + Key.Name;
        }
        for(PropertyField Prop : Table.properties)
            CreateString = CreateString+',' + Prop.GetDeclare(syntax);
        CreateString = CreateString + ",CONSTRAINT PK_" + Table.Name + " PRIMARY KEY " + syntax.getClustered() + " (" + KeyString + ")";

        try {
            execute("DROP TABLE "+Table.Name+" CASCADE CONSTRAINTS");
        } catch (SQLException e) {
            e.getErrorCode();
        }

//        System.out.println("CREATE TABLE "+Table.Name+" ("+CreateString+")");
        execute("CREATE TABLE "+Table.Name+" ("+CreateString+")");

        int IndexNum = 1;
        for(List<PropertyField> Index : Table.Indexes) {
            String Columns = "";
            for(PropertyField IndexField : Index)
                Columns = (Columns.length()==0?"":Columns+",") + IndexField.Name;

            execute("CREATE INDEX "+Table.Name+"_idx_"+(IndexNum++)+" ON "+Table.Name+" ("+Columns+")");
        }
    }

    public void createTemporaryTable(SessionTable Table) throws SQLException {
        String CreateString = "";
        String KeyString = "";
        for(KeyField Key : Table.keys) {
            CreateString = (CreateString.length()==0?"":CreateString+',') + Key.GetDeclare(syntax);
            KeyString = (KeyString.length()==0?"":KeyString+',') + Key.Name;
        }
        for(PropertyField Prop : Table.properties)
            CreateString = CreateString+',' + Prop.GetDeclare(syntax);

        try {
            execute("DROP TABLE "+Table.Name+" CASCADE CONSTRAINTS");
        } catch (SQLException e) {
            e.getErrorCode();
        }

        execute(syntax.getCreateSessionTable(Table.Name,CreateString,"CONSTRAINT PK_S_" + ID +"_T_" + Table.Name + " PRIMARY KEY " + syntax.getClustered() + " (" + KeyString + ")"));
    }

    public void execute(String executeString) throws SQLException {
        Statement statement = connection.createStatement();
//        System.out.println(ExecuteString+syntax.getCommandEnd());
        try {
            statement.execute(executeString + syntax.getCommandEnd());
//        } catch(SQLException e) {
//            if(!ExecuteString.startsWith("DROP") && !ExecuteString.startsWith("CREATE")) {
//            System.out.println(ExecuteString+Syntax.getCommandEnd());
//            e = e;
//           }
        } finally {
            statement.close();
        }
        if(!InTransaction && syntax.noAutoCommit())
            statement.execute(syntax.commitTransaction()+ syntax.getCommandEnd());

        try {
            statement.close();
        } catch (SQLException e) {
            e.getErrorCode();
        }
    }

    public void insertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<PropertyField,Object> PropFields) throws SQLException {

        String InsertString = "";
        String ValueString = "";

        // пробежим по KeyFields'ам
        for(KeyField Key : Table.keys) {
            InsertString = (InsertString.length()==0?"":InsertString+',') + Key.Name;
            ValueString = (ValueString.length()==0?"":ValueString+',') + KeyFields.get(Key);
        }

        // пробежим по Fields'ам
        for(PropertyField Prop : PropFields.keySet()) {
            InsertString = InsertString+","+Prop.Name;
            ValueString = ValueString+","+ TypedObject.getString(PropFields.get(Prop),Prop.type, syntax);
        }

        execute("INSERT INTO "+Table.getName(syntax)+" ("+InsertString+") VALUES ("+ValueString+")");
    }

    void UpdateInsertRecord(Table Table,Map<KeyField,Integer> KeyFields,Map<PropertyField,Object> PropFields) throws SQLException {

        // по сути пустое кол-во ключей
        JoinQuery<Object,String> IsRecQuery = new JoinQuery<Object,String>(new ArrayList<Object>());

        Map<KeyField,ValueExpr> KeyExprs = new HashMap<KeyField, ValueExpr>();
        for(KeyField Key : Table.keys)
            KeyExprs.put(Key,new ValueExpr(KeyFields.get(Key),Key.type));

        // сначала закинем KeyField'ы и прогоним Select
        IsRecQuery.and(new Join<KeyField, PropertyField>(Table,KeyExprs).inJoin);

        if(IsRecQuery.executeSelect(this).size()>0) {
            JoinQuery<KeyField, PropertyField> UpdateQuery = new JoinQuery<KeyField, PropertyField>(Table.keys);
            UpdateQuery.putKeyWhere(KeyFields);
            for(Map.Entry<PropertyField,Object> MapProp : PropFields.entrySet())
                UpdateQuery.properties.put(MapProp.getKey(), MapProp.getKey().type.getExpr(MapProp.getValue()));

            // есть запись нужно Update лупить
            UpdateRecords(new ModifyQuery(Table,UpdateQuery));
        } else
            // делаем Insert
            insertRecord(Table,KeyFields,PropFields);
    }

    public void deleteKeyRecords(Table Table,Map<KeyField,Integer> Keys) throws SQLException {
 //       Execute(Table.GetDelete());
        String DeleteWhere = "";
        for(Map.Entry<KeyField,Integer> DeleteKey : Keys.entrySet())
            DeleteWhere = (DeleteWhere.length()==0?"":DeleteWhere+" AND ") + DeleteKey.getKey().Name + "=" + DeleteKey.getValue();

        execute("DELETE FROM "+Table.getName(syntax)+(DeleteWhere.length()==0?"":" WHERE "+DeleteWhere));
    }

    public void UpdateRecords(ModifyQuery Modify) throws SQLException {
//        try {
            execute(Modify.getUpdate(syntax));
//        } catch(SQLException e) {
//            Execute(Modify.getUpdate(Syntax));
//        }
    }

    public void InsertSelect(ModifyQuery Modify) throws SQLException {
        execute(Modify.getInsertSelect(syntax));
    }

    // сначала делает InsertSelect, затем UpdateRecords
    public void modifyRecords(ModifyQuery modify) throws SQLException {
        execute(modify.getInsertLeftKeys(syntax));
        execute(modify.getUpdate(syntax));
    }

    public void close() throws SQLException {
        connection.close();
    }

    public boolean hasChanges() {
        return changes.hasChanges();
    }

    int CursorCounter = 0;
    public String getCursorName() {
        return "cursor"+(CursorCounter++);
    }
}
