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
import platform.base.BaseUtils;

import java.sql.*;
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
    }

    public void restart(boolean cancel) throws SQLException {

        if(cancel)
            for(DataChanges viewChanges : incrementChanges.values()) {
                viewChanges.properties.addAll(changes.properties);
                viewChanges.addClasses.addAll(changes.addClasses);
                viewChanges.removeClasses.addAll(changes.removeClasses);
            }

        tableFactory.clearSession(this);
        changes = new DataChanges();
        newClasses = new HashMap<Integer, RemoteClass>();
        baseClasses = new HashMap<Integer, RemoteClass>();

        propertyChanges = new HashMap<Property, Property.Change>();
        addChanges = new HashMap<RemoteClass, ClassSet>();
        removeChanges = new HashMap<RemoteClass, ClassSet>();
        dataChanges = new HashMap<DataProperty, ValueClassSet<DataPropertyInterface>>();
    }

    Map<Integer, RemoteClass> newClasses = new HashMap<Integer, RemoteClass>();
    // классы на момент выполнения
    public Map<Integer, RemoteClass> baseClasses = new HashMap<Integer, RemoteClass>();

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

        if(!newClasses.containsKey(idObject))
            baseClasses.put(idObject,PrevClass);
        newClasses.put(idObject,ToClass);

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
        for(Map.Entry<KeyField,DataPropertyInterface> Field : (Set<Map.Entry<KeyField,DataPropertyInterface>>)Property.dataChangeMap.entrySet()) {
            Integer idObject = (Integer) Keys.get(Field.getValue()).object;
            InsertKeys.put(Field.getKey(), idObject);
            InterfaceClass.put(Field.getValue(),getBaseClassSet(idObject));
        }

        InsertKeys.put(Property.dataChange.property,Property.ID);

        Map<PropertyField,Object> InsertValues = new HashMap<PropertyField,Object>();
        InsertValues.put(Property.dataChange.value,NewValue);

        ClassSet ValueClass = Property.getBaseClass();
        if(ValueClass.intersect(ClassSet.getUp(objectClass)))
            ValueClass = getBaseClassSet((Integer) NewValue);

        updateInsertRecord(Property.dataChange,InsertKeys,InsertValues);

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
        RemoteClass NewClass = newClasses.get(idObject);
        if(NewClass==null)
            NewClass = readClass(idObject);
        if(NewClass==null)
            NewClass = RemoteClass.base;
        return NewClass;
    }

    ClassSet getBaseClassSet(Integer idObject) throws SQLException {
        if(idObject==null) return new ClassSet();
        RemoteClass BaseClass = baseClasses.get(idObject);
        if(BaseClass==null)
            BaseClass = readClass(idObject);
        return new ClassSet(BaseClass);
    }

    // последний параметр
    public List<Property> update(PropertyUpdateView toUpdate,Collection<RemoteClass> updateClasses) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)

        DataChanges toUpdateChanges = incrementChanges.get(toUpdate);
        if(toUpdateChanges==null) toUpdateChanges = changes;

        Collection<Property> toUpdateProperties = toUpdate.getUpdateProperties();
        Collection<Property> noUpdateProperties = toUpdate.getNoUpdateProperties();
        // сначала читаем инкрементные св-ва которые изменились
        List<Property> incrementUpdateList = BusinessLogics.getChangedList(toUpdateProperties,toUpdateChanges,noUpdateProperties);
        List<Property> updateList = BusinessLogics.getChangedList(incrementUpdateList, changes,noUpdateProperties);

        Map<Property,Integer> requiredTypes = new HashMap<Property,Integer>();
        // пробежим вперед пометим свойства которые изменились, но неясно на что
        for(Property property : updateList)
            requiredTypes.put(property,toUpdateProperties.contains(property)?0:null);
        Map<Property, Integer> IncrementTypes = getIncrementTypes(updateList, requiredTypes);

        // запускаем IncrementChanges для этого списка
        for(Property property : updateList) {
            Property.Change change = property.incrementChanges(this,IncrementTypes.get(property));
            // подгоняем тип
            change.correct(requiredTypes.get(property));
            if(!(property instanceof MaxGroupProperty) && toUpdate.toSave(property))
                change.save(this);
            propertyChanges.put(property, change);
        }

        updateClasses.addAll(toUpdateChanges.addClasses);
        updateClasses.addAll(toUpdateChanges.removeClasses);

        // сбрасываем лог
        incrementChanges.put(toUpdate,new DataChanges());

        return incrementUpdateList;
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
            if(IncType==null || Property.isStored()) {
                ToWait = new HashSet<Property>();
                IncType = Property.getIncrementType(UpdateList, ToWait);
            }
            // если определившееся (точно 0 или 1) запустим Waiter'ов, соответственно вычистим
            if(IncType==null || (!Property.isStored() && !IncType.equals(2))) {
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

        for(Integer idObject : newClasses.keySet()) {
            Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
            InsertKeys.put(tableFactory.objectTable.key, idObject);

            Map<PropertyField,Object> InsertProps = new HashMap<PropertyField,Object>();
            RemoteClass ChangeClass = newClasses.get(idObject);
            InsertProps.put(tableFactory.objectTable.objectClass,ChangeClass!=null?ChangeClass.ID:null);

            updateInsertRecord(tableFactory.objectTable,InsertKeys,InsertProps);
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

    boolean inTransaction = false;

    public void startTransaction() throws SQLException {
        inTransaction = true;

        if(!syntax.noAutoCommit())
            execute(syntax.startTransaction());
    }

    public void rollbackTransaction() throws SQLException {
        execute(syntax.rollbackTransaction());

        inTransaction = false;
    }

    public void commitTransaction() throws SQLException {
        execute(syntax.commitTransaction());

        inTransaction = false;
    }

    // удостоверивается что таблица есть
    public void ensureTable(Table table) throws SQLException {

        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(null, null, table.name, new String[]{"TABLE"});
        if(!tables.next()) {
            createTable(table.name,table.keys);
            for(PropertyField property : table.properties)
                addColumn(table.name,property);
        }
    }

    public void createTable(String table,Collection<KeyField> keys) throws SQLException {
        System.out.print("Идет создание таблицы "+table+"... ");
        String createString = "";
        String keyString = "";
        for(KeyField key : keys) {
            createString = (createString.length()==0?"": createString +',') + key.getDeclare(syntax);
            keyString = (keyString.length()==0?"":keyString+',') + key.name;
        }
        if(createString.length()==0)
            createString = "dumb bit";
        else
            createString = createString + ",CONSTRAINT PK_" + table + " PRIMARY KEY " + syntax.getClustered() + " (" + keyString + ")";

//        System.out.println("CREATE TABLE "+Table.Name+" ("+CreateString+")");
        execute("CREATE TABLE "+ table +" ("+ createString +")");
        System.out.println(" Done");
    }
    public void dropTable(String table) throws SQLException {
        System.out.print("Идет удаление таблицы "+table+"... ");
        execute("DROP TABLE "+ table);
        System.out.println(" Done");
    }
    static String getIndexName(String table,Collection<String> fields) {
        String name = table + "_idx";
        for(String indexField : fields)
            name = name + "_" + indexField;
        return name;
    }
    public void addIndex(String table,Collection<String> fields) throws SQLException {
        System.out.print("Идет создание индекса "+getIndexName(table, fields)+"... ");
        String columns = "";
        for(String indexField : fields)
            columns = (columns.length()==0?"":columns+",") + indexField;

        execute("CREATE INDEX " + getIndexName(table, fields) + " ON " + table + " (" + columns + ")");
        System.out.println(" Done");
    }
    public void dropIndex(String table,Collection<String> fields) throws SQLException {
        System.out.print("Идет удаление индекса "+getIndexName(table, fields)+"... ");
        execute("DROP INDEX " + getIndexName(table, fields));
        System.out.println(" Done");
    }
    public void addColumn(String table,PropertyField field) throws SQLException {
        System.out.print("Идет добавление колонки "+table+"."+field.name+"... ");
        execute("ALTER TABLE " + table + " ADD COLUMN " + field.getDeclare(syntax));
        System.out.println(" Done");
    }
    public void dropColumn(String table,String field) throws SQLException {
        System.out.print("Идет удаление колонки "+table+"."+field+"... ");
        execute("ALTER TABLE " + table + " DROP COLUMN " + field);
        System.out.println(" Done");
    }
    public void modifyColumn(String table,PropertyField field) throws SQLException {
        System.out.print("Идет изменение типа колонки "+table+"."+field.name+"... ");
        execute("ALTER TABLE " + table + " ALTER COLUMN " + field.name + " TYPE " + field.type.getDB(syntax));
        System.out.println(" Done");
    }

    public void packTable(Table table) throws SQLException {
        System.out.print("Идет упаковка таблицы "+table+"... ");
        String dropWhere = "";
        for(PropertyField property : table.properties)
            dropWhere = (dropWhere.length()==0?"":dropWhere+" AND ") + property.name + " IS NULL";
        execute("DELETE FROM "+ table.getName(syntax) + (dropWhere.length()==0?"":" WHERE "+dropWhere));
        System.out.println(" Done");
    }

    private Set<SessionTable> temporaryTables = new HashSet<SessionTable>();
    public void useTemporaryTable(Table table) throws SQLException {
        if(table instanceof SessionTable && temporaryTables.add((SessionTable) table))
            createTemporaryTable((SessionTable) table);
    }
    private void createTemporaryTable(SessionTable table) throws SQLException {
        String createString = "";
        String keyString = "";
        for(KeyField key : table.keys) {
            createString = (createString.length()==0?"":createString+',') + key.getDeclare(syntax);
            keyString = (keyString.length()==0?"":keyString+',') + key.name;
        }
        for(PropertyField prop : table.properties)
            createString = createString+',' + prop.getDeclare(syntax);

//        try { execute("DROP TABLE "+table.name +" CASCADE CONSTRAINTS"); } catch (SQLException e) {  e.getErrorCode(); }

        execute(syntax.getCreateSessionTable(table.name,createString,"CONSTRAINT PK_S_" + ID +"_T_" + table.name + " PRIMARY KEY " + syntax.getClustered() + " (" + keyString + ")"));
    }

    public void execute(String executeString) throws SQLException {
        executeStatement(connection.prepareStatement(executeString));
    }

    public void executeStatement(PreparedStatement statement) throws SQLException {
//        System.out.println(statement);
        try {
            statement.execute();
        } catch(SQLException e) {
            System.out.println(statement.toString());
            throw e;
        } finally {
            statement.close();
        }

        ensureTransaction();
    }

    private void ensureTransaction() throws SQLException {
        if(!inTransaction && syntax.noAutoCommit()) {
            Statement statement = connection.createStatement();
            statement.execute(syntax.commitTransaction()+ syntax.getCommandEnd());
            statement.close();
        }
    }

    void insertParamRecord(Table table,Map<KeyField,Integer> keyFields,Map<PropertyField,Object> propFields) throws SQLException {
        String insertString = "";
        String valueString = "";

        // пробежим по KeyFields'ам
        for(KeyField key : table.keys) {
            insertString = (insertString.length()==0?"":insertString+',') + key.name;
            valueString = (valueString.length()==0?"":valueString+',') + keyFields.get(key);
        }

        int paramNum = 0;
        Map<String,TypedObject> params = new HashMap<String, TypedObject>();
        for(PropertyField prop : propFields.keySet()) {
            String prm = "qxprm" + (paramNum++) + "nx"; 
            insertString = (insertString.length()==0?"":insertString+',') + prop.name;
            valueString = (valueString.length()==0?"":valueString+',') + prm;
            params.put(prm,new TypedObject(propFields.get(prop),prop.type));
        }

        executeStatement(getStatement("INSERT INTO "+table.getName(syntax)+" ("+insertString+") VALUES ("+valueString+")", params));
    }

    public void insertRecord(Table table,Map<KeyField,Integer> keyFields,Map<PropertyField,Object> propFields) throws SQLException {
        useTemporaryTable(table);

        for(PropertyField prop : propFields.keySet())
            if(!prop.type.isString(propFields.get(prop))) {
                insertParamRecord(table, keyFields, propFields);
                return;
            }

        String insertString = "";
        String valueString = "";

        // пробежим по KeyFields'ам
        for(KeyField key : table.keys) {
            insertString = (insertString.length()==0?"":insertString+',') + key.name;
            valueString = (valueString.length()==0?"":valueString+',') + keyFields.get(key);
        }

        // пробежим по Fields'ам
        for(PropertyField prop : propFields.keySet()) {
            insertString = (insertString.length()==0?"":insertString+',') + prop.name;
            valueString = (valueString.length()==0?"":valueString+',') + TypedObject.getString(propFields.get(prop),prop.type, syntax);
        }

        execute("INSERT INTO "+table.getName(syntax)+" ("+insertString+") VALUES ("+valueString+")");
    }

    public boolean isRecord(Table table,Map<KeyField,Integer> keyFields) throws SQLException {

        // по сути пустое кол-во ключей
        JoinQuery<Object,String> isRecQuery = new JoinQuery<Object,String>(new ArrayList<Object>());

        Map<KeyField,ValueExpr> keyExprs = new HashMap<KeyField, ValueExpr>();
        for(KeyField Key : table.keys)
            keyExprs.put(Key,new ValueExpr(keyFields.get(Key),Key.type));

        // сначала закинем KeyField'ы и прогоним Select
        isRecQuery.and(new Join<KeyField, PropertyField>(table,keyExprs).inJoin);

        return isRecQuery.executeSelect(this).size()>0;
    }

    public void ensureRecord(Table table,Map<KeyField,Integer> keyFields,Map<PropertyField,Object> propFields) throws SQLException {
        if(!isRecord(table, keyFields))
            insertRecord(table,keyFields,propFields);
    }

    public void updateInsertRecord(Table table,Map<KeyField,Integer> keyFields,Map<PropertyField,Object> propFields) throws SQLException {
        useTemporaryTable(table);

        if(isRecord(table, keyFields)) {
            JoinQuery<KeyField, PropertyField> updateQuery = new JoinQuery<KeyField, PropertyField>(table.keys);
            updateQuery.putKeyWhere(keyFields);
            for(Map.Entry<PropertyField,Object> MapProp : propFields.entrySet())
                updateQuery.properties.put(MapProp.getKey(), MapProp.getKey().type.getExpr(MapProp.getValue()));

            // есть запись нужно Update лупить
            updateRecords(new ModifyQuery(table,updateQuery));
        } else
            // делаем Insert
            insertRecord(table,keyFields,propFields);
    }

    public Object readRecord(Table table,Map<KeyField,Integer> keyFields,PropertyField field) throws SQLException {
        // по сути пустое кол-во ключей
        JoinQuery<Object,String> getQuery = new JoinQuery<Object,String>(new ArrayList<Object>());

        Map<KeyField,ValueExpr> keyExprs = new HashMap<KeyField, ValueExpr>();
        for(KeyField Key : table.keys)
            keyExprs.put(Key,new ValueExpr(keyFields.get(Key),Key.type));

        // сначала закинем KeyField'ы и прогоним Select
        SourceExpr fieldExpr = new Join<KeyField, PropertyField>(table,keyExprs).exprs.get(field);
        getQuery.properties.put("result",fieldExpr);
        getQuery.and(fieldExpr.getWhere());
        LinkedHashMap<Map<Object, Integer>, Map<String, Object>> result = getQuery.executeSelect(this);
        if(result.size()>0)
            return result.values().iterator().next().get("result");
        else
            return null;
    }

    public void deleteKeyRecords(Table table,Map<KeyField,Integer> keys) throws SQLException {
        if(table instanceof SessionTable && !temporaryTables.contains((SessionTable)table)) return;

        String deleteWhere = "";
        for(Map.Entry<KeyField,Integer> deleteKey : keys.entrySet())
            deleteWhere = (deleteWhere.length()==0?"":deleteWhere+" AND ") + deleteKey.getKey().name + "=" + deleteKey.getValue();

        execute("DELETE FROM "+ table.getName(syntax)+(deleteWhere.length()==0?"":" WHERE "+deleteWhere));
    }

    public void updateRecords(ModifyQuery modify) throws SQLException {
        executeStatement(getStatement(modify.getUpdate(syntax).command, modify.getUpdate(syntax).params));
    }

    public void insertSelect(ModifyQuery modify) throws SQLException {
        useTemporaryTable(modify.table);
        executeStatement(getStatement(modify.getInsertSelect(syntax).command, modify.getInsertSelect(syntax).params));
    }

    // сначала делает InsertSelect, затем UpdateRecords
    public void modifyRecords(ModifyQuery modify) throws SQLException {
        useTemporaryTable(modify.table);
        executeStatement(getStatement(modify.getInsertLeftKeys(syntax).command, modify.getInsertLeftKeys(syntax).params));
        updateRecords(modify);
    }

    public void close() throws SQLException {
        connection.close();
    }

    public boolean hasChanges() {
        return changes.hasChanges();
    }

    public PreparedStatement getStatement(String command, Map<String, TypedObject> paramObjects) throws SQLException {

        char[][] params = new char[paramObjects.size()][];
        TypedObject[] values = new TypedObject[params.length];
        int paramNum = 0;
        for(Map.Entry<String,TypedObject> param : paramObjects.entrySet()) {
            params[paramNum] = param.getKey().toCharArray();
            values[paramNum++] = param.getValue();
        }

        // те которые isString сразу транслируем
        List<TypedObject> preparedParams = new ArrayList<TypedObject>();
        char[] toparse = command.toCharArray();
        String parsedString = "";
        char[] parsed = new char[toparse.length]; int num=0;
        for(int i=0;i<toparse.length;) {
            int charParsed = 0;
            for(int p=0;p<params.length;p++) {
                if(BaseUtils.startsWith(toparse,i,params[p])) { // нашли
                    if(values[p].isString()) { // если можно вручную пропарсить парсим
                        parsedString = parsedString + new String(parsed,0,num) + values[p].getString(syntax);
                        parsed = new char[toparse.length-i]; num = 0;
                    } else {
                        parsed[num++] = '?';
                        preparedParams.add(values[p]);
                    }
                    charParsed = params[p].length;
                    break;
                }
            }
            if(charParsed==0) {
                parsed[num++] = toparse[i];
                charParsed = 1;
            }
            i = i + charParsed;
        }
        parsedString = parsedString + new String(parsed,0,num);

        PreparedStatement statement = connection.prepareStatement(parsedString);
        paramNum = 1;
        for(TypedObject param : preparedParams)
            param.writeParam(statement,paramNum++);

        return statement;
    }
}
