package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMap;
import lsfusion.base.col.implementations.abs.ASet;
import lsfusion.base.col.implementations.simple.EmptyOrderMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.Compare;
import lsfusion.server.*;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.*;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.sql.DataAdapter;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.navigator.*;
import lsfusion.server.integration.*;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.NFLazy;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.table.IDTable;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.SessionCreator;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.*;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.util.*;

import static java.util.Arrays.asList;
import static lsfusion.base.SystemUtils.getRevision;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class DBManager extends LifecycleAdapter implements InitializingBean {
    public static final Logger logger = Logger.getLogger(DBManager.class);
    public static final Logger systemLogger = ServerLoggers.systemLogger;

    private static Comparator<DBVersion> dbVersionComparator = new Comparator<DBVersion>() {
        @Override
        public int compare(DBVersion lhs, DBVersion rhs) {
            return lhs.compare(rhs);
        }
    };

    private TreeMap<DBVersion, List<SIDChange>> propertyCNChanges = new TreeMap<DBVersion, List<SIDChange>>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> propertySIDChanges = new TreeMap<DBVersion, List<SIDChange>>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> classSIDChanges = new TreeMap<DBVersion, List<SIDChange>>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> tableSIDChanges = new TreeMap<DBVersion, List<SIDChange>>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> objectSIDChanges = new TreeMap<DBVersion, List<SIDChange>>(dbVersionComparator);

    private DataAdapter adapter;

    private RestartManager restartManager;

    private BusinessLogics<?> businessLogics;

    private boolean ignoreMigration;

    private boolean denyDropModules;

    private boolean denyDropTables;
    
    private BaseLogicsModule<?> LM;

    private ReflectionLogicsModule reflectionLM;

    private int systemUserObject;
    private int systemComputer;

    private final ThreadLocal<SQLSession> threadLocalSql;

    private final Map<List<? extends CalcProperty>, Boolean> indexes = new HashMap<List<? extends CalcProperty>, Boolean>();

    public DBManager() {
        super(DBMANAGER_ORDER);

        threadLocalSql = new ThreadLocal<SQLSession>();
    }

    public String getDataBaseName() {
        return adapter.dataBase;
    }

    public DataAdapter getAdapter() {
        return adapter;
    }
    
    public void setAdapter(DataAdapter adapter) {
        this.adapter = adapter;
    }

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.businessLogics = businessLogics;
    }

    public void setRestartManager(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    public void setIgnoreMigration(boolean ignoreMigration) {
        this.ignoreMigration = ignoreMigration;
    }

    public void setDenyDropModules(boolean denyDropModules) {
        this.denyDropModules = denyDropModules;
    }

    public void setDenyDropTables(boolean denyDropTables) {
        this.denyDropTables = denyDropTables;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(adapter, "adapter must be specified");
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(restartManager, "restartManager must be specified");
    }

    public boolean sourceHashChanged; 
    @Override
    protected void onInit(LifecycleEvent event) {
        this.LM = businessLogics.LM;
        this.reflectionLM = businessLogics.reflectionLM;
        try {
            if(adapter.getSyntaxType() == SQLSyntaxType.MSSQL)
                Expr.useCasesCount = 5;
            
            systemLogger.info("Synchronizing DB.");
            sourceHashChanged = synchronizeDB();
        } catch (Exception e) {
            throw new RuntimeException("Error synchronizing DB: ", e);
        }
    }

    @IdentityStrongLazy // ресурсы потребляет
    private SQLSession getIDSql() throws SQLException { // подразумевает synchronized использование
        try {
            return createSQL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @IdentityStrongLazy
    private SQLSession getSystemSql() throws SQLException {
        try {
            return createSQL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SQLSession createSQL() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        return new SQLSession(adapter);
    }

    public SQLSession getThreadLocalSql() throws SQLException {
        SQLSession sqlSession = threadLocalSql.get();
        if(sqlSession == null) {
            try {
                sqlSession = createSQL();
                threadLocalSql.set(sqlSession);
            } catch (Throwable t) {
                throw ExceptionUtils.propagate(t, SQLException.class);
            }
        }
        return sqlSession;
    }

    public SQLSession closeThreadLocalSql() {
        SQLSession sql = threadLocalSql.get();
        if(sql!=null)
            try {
                sql.close(OperationOwner.unknown);
            } catch (SQLException e) {
                ServerLoggers.sqlSuppLog(e);
            } finally {
                threadLocalSql.set(null);
            }
        return sql;
    }

    public int generateID() throws RemoteException {
        try {
            return IDTable.instance.generateID(getIDSql(), IDTable.OBJECT);
        } catch (SQLException e) {
            throw new RuntimeException(getString("logics.info.error.reading.user.data"), e);
        }
    }

    public DataSession createSession() throws SQLException {
        return createSession((OperationOwner)null);
    }
    
    public DataSession createSession(OperationOwner upOwner) throws SQLException {
        return createSession(getThreadLocalSql(), upOwner);
    }

    public DataSession createSession(SQLSession sql) throws SQLException {
        return createSession(sql, null);
    }

    public DataSession createSession(SQLSession sql, OperationOwner upOwner) throws SQLException {
        return createSession(sql,
                new UserController() {
                    public void changeCurrentUser(DataObject user) {
                        throw new RuntimeException("not supported");
                    }

                    public DataObject getCurrentUser() {
                        return new DataObject(systemUserObject, businessLogics.authenticationLM.systemUser);
                    }
                },
                new ComputerController() {
                    public DataObject getCurrentComputer() {
                        return new DataObject(systemComputer, businessLogics.authenticationLM.computer);
                    }

                    public boolean isFullClient() {
                        return false;
                    }
                },
                new TimeoutController() {
                    public int getTransactionTimeout() {
                        return 0;
                    }
                },
                upOwner
        );
    }

    public DataSession createSession(SQLSession sql, UserController userController, ComputerController computerController, TimeoutController timeoutController, OperationOwner owner) throws SQLException {
        //todo: неплохо бы избавиться от зависимости на restartManager, а то она неестественна
        return new DataSession(sql, userController, computerController,
                timeoutController, new IsServerRestartingController() {
                                   public boolean isServerRestarting() {
                                       return restartManager.isPendingRestart();
                                   }
                               },
                               LM.baseClass, businessLogics.systemEventsLM.session, businessLogics.systemEventsLM.currentSession, getIDSql(), businessLogics.getSessionEvents(), owner);
    }


    public DataObject getServerComputerObject() {
        return new DataObject(getComputer(SystemUtils.getLocalHostName()), businessLogics.authenticationLM.computer);
    }

    public Integer getComputer(String strHostName) {
        try {
            DataSession session = createSession(getSystemSql());

            QueryBuilder<String, Object> q = new QueryBuilder<String, Object>(SetFact.singleton("key"));
            q.and(
                    businessLogics.authenticationLM.hostnameComputer.getExpr(
                            session.getModifier(), q.getMapExprs().get("key")
                    ).compare(new DataObject(strHostName), Compare.EQUALS)
            );

            Integer result;

            ImSet<ImMap<String, Object>> keys = q.execute(session).keys();
            if (keys.size() == 0) {
                DataObject addObject = session.addObject(businessLogics.authenticationLM.computer);
                businessLogics.authenticationLM.hostnameComputer.change(strHostName, session, addObject);

                result = (Integer) addObject.object;
                session.apply(businessLogics);
            } else {
                result = (Integer) keys.iterator().next().get("key");
            }

            session.close();
            logger.debug("Begin user session " + strHostName + " " + result);
            return result;
        } catch (Exception e) {
            logger.error("Error reading computer: ", e);
            throw new RuntimeException(e);
        }
    }

    private String getDroppedTablesString(OldDBStructure oldDBStructure, NewDBStructure newDBStructure) throws SQLException, SQLHandledException {
        String droppedTables = "";
        for (Table table : oldDBStructure.tables.keySet()) {
            if (newDBStructure.getTable(table.getName()) == null) {
                ImRevMap<KeyField, KeyExpr> mapKeys = table.getMapKeys();
                Expr expr = GroupExpr.create(MapFact.<KeyField, KeyExpr>EMPTY(), new ValueExpr(new DataObject(1)), table.join(mapKeys).getWhere(), GroupType.SUM, MapFact.<KeyField, Expr>EMPTY());
                ImOrderMap<ImMap<Object, Object>, ImMap<String, Object>> resultMap = new Query<Object, String>(MapFact.<Object, KeyExpr>EMPTYREV(), expr, "value").execute(createSession());
                if (!(resultMap instanceof EmptyOrderMap)) {
                    if (!droppedTables.equals("")) {
                        droppedTables += ", ";
                    }
                    droppedTables += table;
                }
            }
        }
        return droppedTables;
    }
    
    // Удаляем несуществующие индексы и убираем из newDBStructure не изменившиеся индексы
    // Делаем это до применения migration script, то есть не пытаемся сохранить все возможные индексы по максимуму
    private void checkIndices(SQLSession sql, OldDBStructure oldDBStructure, NewDBStructure newDBStructure) throws SQLException {
        for (Map.Entry<Table, Map<List<String>, Boolean>> oldTableIndices : oldDBStructure.tables.entrySet()) {
            Table oldTable = oldTableIndices.getKey();
            Table newTable = newDBStructure.getTable(oldTableIndices.getKey().getName());
            Map<List<Field>, Boolean> newTableIndices = null; Map<List<String>, Pair<Boolean, List<Field>>> newTableIndicesNames = null;
            if(newTable != null) {
                newTableIndices = newDBStructure.tables.get(newTable);
                newTableIndicesNames = new HashMap<List<String>, Pair<Boolean, List<Field>>>();
                for (Map.Entry<List<Field>, Boolean> entry : newTableIndices.entrySet()) {
                    List<String> names = new ArrayList<String>();
                    for (Field field : entry.getKey())
                        names.add(field.getName());
                    newTableIndicesNames.put(names, new Pair<Boolean, List<Field>>(entry.getValue(), entry.getKey()));
                }
            }

            for (Map.Entry<List<String>, Boolean> oldIndex : oldTableIndices.getValue().entrySet()) {
                List<String> oldIndexKeys = oldIndex.getKey();
                boolean oldOrder = oldIndex.getValue();
                boolean drop = (newTable == null); // ушла таблица
                if (!drop) {
                    Pair<Boolean, List<Field>> newOrder = newTableIndicesNames.get(oldIndexKeys);
                    if (newOrder != null && newOrder.first.equals(oldOrder)) {
                        newTableIndices.remove(newOrder.second); // не трогаем индекс
                    } else {
                        drop = true;
                    }
                }
                if (drop) {
                    // В структуре некоторых баз было ошибочное имя одного из индексов 
                    if (oldDBStructure.version >= 15 || !oldTable.getName().equals("Machinery_machineryPriceTransactionBarcode")) {
                        sql.dropIndex(oldTable, oldTable.keys, SetFact.fromJavaOrderSet(oldIndexKeys), oldOrder);
                    }
                }
            }
        }
    }
    
    private void checkUniqueDBName(NewDBStructure struct) {
        Map<Pair<String, String>, DBStoredProperty> sids = new HashMap<Pair<String, String>, DBStoredProperty>();
        for (DBStoredProperty property : struct.storedProperties) {
            Pair<String, String> key = new Pair<String, String>(property.getDBName(), property.getTable().getName());
            if (sids.containsKey(key)) {
                systemLogger.error(String.format("Equal sid '%s' in table '%s': %s and %s", key.first, key.second, sids.get(key).getCanonicalName(), property.getCanonicalName()));
            }
            sids.put(key, property);
         }
    }

    public void uploadToDB(SQLSession sql, boolean isolatedTransactions, final DataAdapter adapter) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, SQLHandledException {
        final OperationOwner owner = OperationOwner.unknown;
        final SQLSession sqlFrom = new SQLSession(adapter);

        sql.pushNoQueryLimit();
        try {
            ImSet<GlobalTable> tables = SetFact.addExcl(LM.tableFactory.getImplementTables(), IDTable.instance);
            final int size = tables.size();
            for (int i = 0; i < size; i++) {
                final GlobalTable implementTable = tables.get(i);
                final int fi = i;
                run(sql, isolatedTransactions, new RunService() {
                    @Override
                    public void run(SQLSession sql) throws SQLException, SQLHandledException {
                        uploadTableToDB(sql, implementTable, fi + "/" + size, sqlFrom, owner);
                    }
                });
            }
        } finally {
            sql.popNoQueryLimit();
        }
    }

    @Message("logics.upload.db")
    private void uploadTableToDB(SQLSession sql, final @ParamMessage GlobalTable implementTable, @ParamMessage String progress, final SQLSession sqlTo, final OperationOwner owner) throws SQLException, SQLHandledException {
        sqlTo.truncate(implementTable, owner);

        try {
            final Result<Integer> proceeded = new Result<Integer>(0);
            final int total = sql.getCount(implementTable, owner);
            ResultHandler<KeyField, PropertyField> reader = new ReadBatchResultHandler<KeyField, PropertyField>(10000) {
                public void start() {
                    ThreadLocalContext.pushActionMessage("Proceeded : " + proceeded.result + " of " + total);
                }

                public void proceedBatch(ImOrderMap<ImMap<KeyField, Object>, ImMap<PropertyField, Object>> batch) throws SQLException {
                    sqlTo.insertBatchRecords(implementTable, batch.getMap(), owner);
                    proceeded.set(proceeded.result + batch.size());
                    ThreadLocalContext.popActionMessage();
                    ThreadLocalContext.pushActionMessage("Proceeded : " + proceeded.result + " of " + total);
                }

                public void finish() throws SQLException {
                    ThreadLocalContext.popActionMessage();
                    super.finish();
                }
            };
            implementTable.readData(sql, LM.baseClass, owner, true, reader);
        } finally {
            ThreadLocalContext.popActionMessage();
        }
    }

    private boolean synchronizeDB() throws SQLException, IOException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        SQLSession sql = getThreadLocalSql();

        // инициализируем таблицы
        LM.tableFactory.fillDB(sql, LM.baseClass);

        // потом надо сделать соответствующий механизм для Formula
        ScriptingLogicsModule module = businessLogics.getModule("Country");
        if(module != null) {
            LCP<?> lp = module.findProperty("isDayOffCountryDate");

            Properties props = new Properties();
            props.put("dayoff.tablename", lp.property.mapTable.table.getName(sql.syntax));
            props.put("dayoff.fieldname", lp.property.getDBName());
            adapter.ensureScript("jumpWorkdays.sc", props);
        }

        SQLSyntax syntax = adapter;

        // "старое" состояние базы
        DataInputStream inputDB = null;
        StructTable structTable = StructTable.instance;
        byte[] struct = (byte[]) sql.readRecord(structTable, MapFact.<KeyField, DataObject>EMPTY(), structTable.struct, OperationOwner.unknown);
        if (struct != null)
            inputDB = new DataInputStream(new ByteArrayInputStream(struct));

        OldDBStructure oldDBStructure = new OldDBStructure(inputDB, sql);

        checkModules(oldDBStructure);

        String hashModules = SystemProperties.isDebug ? "NOHASH" : calculateHashModules();
        boolean sourceHashChanged = checkHashModulesChanged(oldDBStructure.hashModules, hashModules);

        runAlterationScript();

        Map<String, String> columnsToDrop = new HashMap<String, String>();

        try {
            sql.pushNoHandled();
            sql.startTransaction(DBManager.START_TIL, OperationOwner.unknown);

            // новое состояние базы
            ByteArrayOutputStream outDBStruct = new ByteArrayOutputStream();
            DataOutputStream outDB = new DataOutputStream(outDBStruct);

            DBVersion newDBVersion = getCurrentDBVersion(oldDBStructure.dbVersion);
            NewDBStructure newDBStructure = new NewDBStructure(newDBVersion);
            
            checkUniqueDBName(newDBStructure);
            // запишем новое состояние таблиц (чтобы потом изменять можно было бы)
            newDBStructure.write(outDB, hashModules);

            checkIndices(sql, oldDBStructure, newDBStructure);
            
            systemLogger.info("Applying migration script");
            if (oldDBStructure.version < 15) {
                convertDBStructureToVersion15(oldDBStructure, newDBStructure, sql);
            }
            alterateDBStructure(oldDBStructure, sql);

            // проверка, не удалятся ли старые таблицы
            if (denyDropTables) {
                String droppedTables = getDroppedTablesString(oldDBStructure, newDBStructure);
                if (!droppedTables.isEmpty()) {
                    throw new RuntimeException("Dropped tables: " + droppedTables);
                }
            }

            // добавим таблицы которых не было
            systemLogger.info("Creating tables");
            for (Table table : newDBStructure.tables.keySet()) {
                if (oldDBStructure.getTable(table.getName()) == null)
                    sql.createTable(table, table.keys);
            }

            // проверяем изменение структуры ключей
            for (Table table : newDBStructure.tables.keySet()) {
                Table oldTable = oldDBStructure.getTable(table.getName());
                if (oldTable != null) {
                    for (KeyField key : table.keys) {
                        KeyField oldKey = oldTable.findKey(key.getName());
                        if (!(key.type.equals(oldKey.type))) {
                            sql.modifyColumn(table, key, oldKey.type);
                            systemLogger.info("Changing type of key column " + key + " in table " + table + " from " + oldKey.type + " to " + key.type);
                        }
                    }
                }
            }

            List<AggregateProperty> recalculateProperties = new ArrayList<AggregateProperty>();

            MExclSet<Pair<String, String>> mDropColumns = SetFact.mExclSet(); // вообще pend'ить нужно только classDataProperty, но их тогда надо будет отличать

            // бежим по свойствам
            List<DBStoredProperty> restNewDBStored = new ArrayList<DBStoredProperty>(newDBStructure.storedProperties);
            for (DBStoredProperty oldProperty : oldDBStructure.storedProperties) {
                Table oldTable = oldDBStructure.getTable(oldProperty.tableName);

                boolean keep = false, moved = false;
                for (Iterator<DBStoredProperty> is = restNewDBStored.iterator(); is.hasNext(); ) {
                    DBStoredProperty newProperty = is.next();

                    if (newProperty.getDBName().equals(oldProperty.getDBName())) {
                        MExclMap<KeyField, PropertyInterface> mFoundInterfaces = MapFact.mExclMapMax(newProperty.property.interfaces.size());
                        for (PropertyInterface propertyInterface : newProperty.property.interfaces) {
                            KeyField mapKeyField = oldProperty.mapKeys.get(propertyInterface.ID);
                            if (mapKeyField != null)
                                mFoundInterfaces.exclAdd(mapKeyField, propertyInterface);
                        }
                        ImMap<KeyField, PropertyInterface> foundInterfaces = mFoundInterfaces.immutable();

                        if (foundInterfaces.size() == oldProperty.mapKeys.size()) { // если все нашли
                            ImplementTable newTable = newProperty.getTable();
                            if (!(keep = newProperty.tableName.equals(oldProperty.tableName))) { // если в другой таблице
                                sql.addColumn(newTable, newProperty.property.field);
                                // делаем запрос на перенос

                                systemLogger.info(getString("logics.info.property.is.transferred.from.table.to.table", newProperty.property.field, newProperty.property.caption, oldProperty.tableName, newProperty.tableName));
                                newProperty.property.mapTable.table.moveColumn(sql, newProperty.property.field, oldTable,
                                        foundInterfaces.join((ImMap<PropertyInterface, KeyField>) newProperty.property.mapTable.mapKeys), oldTable.findProperty(oldProperty.getDBName()));
                                systemLogger.info("Done");
                                moved = true;
                            } else { // надо проверить что тип не изменился
                                Type oldType = oldTable.findProperty(oldProperty.getDBName()).type;
                                if (oldDBStructure.version < 12 && newProperty.property.field.type instanceof ConcatenateType) { // вряд ли сможем конвертить пересоздадим
                                    sql.dropColumn(newTable, newProperty.property.field);
                                    sql.addColumn(newTable, newProperty.property.field);
                                    recalculateProperties.add((AggregateProperty) newProperty.property);
                                } else if (!oldType.equals(newProperty.property.field.type)) {
                                    systemLogger.info("Changing type of property column " + newProperty.property.field + " in table " + newProperty.tableName + " from " + oldType + " to " + newProperty.property.field.type);
                                    sql.modifyColumn(newTable, newProperty.property.field, oldType);
                                }
                            }
                            is.remove();
                        }
                        break;
                    }
                }
                if (!keep) {
                    if (oldProperty.isDataProperty && !moved) {
                        String newName = "_DELETED_" + oldProperty.getDBName();
                        Connection connection = sql.getConnection().sql;
                        Savepoint savepoint = connection.setSavepoint();
                        try {
                            savepoint = connection.setSavepoint();
                            sql.renameColumn(oldProperty.getTableName(syntax), oldProperty.getDBName(), newName);
                            columnsToDrop.put(newName, oldProperty.tableName);
                        } catch (PSQLException e) { // колонка с новым именем уже существует
                            connection.rollback(savepoint);
                            mDropColumns.exclAdd(new Pair<String, String>(oldTable.getName(syntax), oldProperty.getDBName()));
                        }
                    } else
                        mDropColumns.exclAdd(new Pair<String, String>(oldTable.getName(syntax), oldProperty.getDBName()));
                }
            }

            for (DBStoredProperty property : restNewDBStored) { // добавляем оставшиеся
                sql.addColumn(property.getTable(), property.property.field);
                if (struct != null && property.property instanceof AggregateProperty) // если все свойства "новые" то ничего перерасчитывать не надо
                    recalculateProperties.add((AggregateProperty) property.property);
            }

            // обработка изменений с классами
            MMap<String, ImMap<String, ImSet<Integer>>> mToCopy = MapFact.mMap(AMap.<String, String, Integer>addMergeMapSets()); // в какое свойство, из какого свойства - какой класс
            for (DBConcreteClass oldClass : oldDBStructure.concreteClasses) {
                for (DBConcreteClass newClass : newDBStructure.concreteClasses) {
                    if (oldClass.sID.equals(newClass.sID)) {
                        if (!(oldClass.sDataPropID.equals(newClass.sDataPropID))) // надо пометить перенос, и удаление
                            mToCopy.add(newClass.sDataPropID, MapFact.singleton(oldClass.sDataPropID, SetFact.singleton(oldClass.ID)));
                        break;
                    }
                }
            }
            ImMap<String, ImMap<String, ImSet<Integer>>> toCopy = mToCopy.immutable();
            for (int i = 0, size = toCopy.size(); i < size; i++) { // перенесем классы, которые сохранились но изменили поле
                DBStoredProperty classProp = newDBStructure.getProperty(toCopy.getKey(i));
                Table table = newDBStructure.getTable(classProp.tableName);

                QueryBuilder<KeyField, PropertyField> copyObjects = new QueryBuilder<KeyField, PropertyField>(table);
                Expr keyExpr = copyObjects.getMapExprs().singleValue();
                Where moveWhere = Where.FALSE;
                ImMap<String, ImSet<Integer>> copyFrom = toCopy.getValue(i);
                CaseExprInterface mExpr = Expr.newCases(true, copyFrom.size());
                MSet<String> mCopyFromTables = SetFact.mSetMax(copyFrom.size());
                for (int j = 0, sizeJ = copyFrom.size(); j < sizeJ; j++) {
                    DBStoredProperty oldClassProp = oldDBStructure.getProperty(copyFrom.getKey(j));
                    Table oldTable = oldDBStructure.getTable(oldClassProp.tableName);
                    mCopyFromTables.add(oldClassProp.tableName);

                    Expr oldExpr = oldTable.join(MapFact.singleton(oldTable.getTableKeys().single(), keyExpr)).getExpr(oldTable.findProperty(oldClassProp.getDBName()));
                    for (int prevID : copyFrom.getValue(j))
                        moveWhere = moveWhere.or(oldExpr.compare(new DataObject(prevID, LM.baseClass.objectClass), Compare.EQUALS));
                    mExpr.add(moveWhere, oldExpr);
                }
                copyObjects.addProperty(table.findProperty(classProp.getDBName()), mExpr.getFinal());
                copyObjects.and(moveWhere);

                systemLogger.info(getString("logics.info.objects.are.transferred.from.tables.to.table", classProp.tableName, mCopyFromTables.immutable().toString()));
                sql.modifyRecords(new ModifyQuery(table, copyObjects.getQuery(), OperationOwner.unknown, TableOwner.global));
            }
            ImMap<String, ImSet<Integer>> toClean = MapFact.mergeMaps(toCopy.values(), ASet.<String, Integer>addMergeSet());
            for (int i = 0, size = toClean.size(); i < size; i++) { // удалим оставшиеся классы
                DBStoredProperty classProp = oldDBStructure.getProperty(toClean.getKey(i));
                Table table = oldDBStructure.getTable(classProp.tableName);

                QueryBuilder<KeyField, PropertyField> dropClassObjects = new QueryBuilder<KeyField, PropertyField>(table);
                Where moveWhere = Where.FALSE;

                PropertyField oldField = table.findProperty(classProp.getDBName());
                Expr oldExpr = table.join(dropClassObjects.getMapExprs()).getExpr(oldField);
                for (int prevID : toClean.getValue(i))
                    moveWhere = moveWhere.or(oldExpr.compare(new DataObject(prevID, LM.baseClass.objectClass), Compare.EQUALS));
                dropClassObjects.addProperty(oldField, Expr.NULL);
                dropClassObjects.and(moveWhere);

                systemLogger.info(getString("logics.info.objects.are.removed.from.table", classProp.tableName));
                sql.updateRecords(new ModifyQuery(table, dropClassObjects.getQuery(), OperationOwner.unknown, TableOwner.global));
            }

            MSet<ImplementTable> mPackTables = SetFact.mSet();
            for (Pair<String, String> dropColumn : mDropColumns.immutable()) {
                systemLogger.info("Dropping column " + dropColumn.second + " from table " + dropColumn.first);
                sql.dropColumn(dropColumn.first, dropColumn.second);
                ImplementTable table = (ImplementTable) newDBStructure.getTable(dropColumn.first);
                if (table != null) mPackTables.add(table);
            }

            // удаляем таблицы старые
            for (Table table : oldDBStructure.tables.keySet()) {
                if (newDBStructure.getTable(table.getName()) == null) {
                    sql.dropTable(table);
                    systemLogger.info("Table " + table + " has been dropped");
                }
            }

            systemLogger.info("Packing tables");
            packTables(sql, mPackTables.immutable(), false); // упакуем таблицы

            systemLogger.info("Updating stats");
            updateStats(sql);  // пересчитаем статистику

            // создадим индексы в базе
            systemLogger.info("Adding indices");
            for (Map.Entry<Table, Map<List<Field>, Boolean>> mapIndex : newDBStructure.tables.entrySet())
                for (Map.Entry<List<Field>, Boolean> index : mapIndex.getValue().entrySet())
                    sql.addIndex(mapIndex.getKey(), mapIndex.getKey().keys, SetFact.fromJavaOrderSet(index.getKey()), index.getValue());

            systemLogger.info("Filling static objects ids");
            fillIDs(getChangesAfter(oldDBStructure.dbVersion, classSIDChanges), getChangesAfter(oldDBStructure.dbVersion, objectSIDChanges));

            for (DBConcreteClass newClass : newDBStructure.concreteClasses) {
                newClass.ID = newClass.customClass.ID;
            }

            newDBStructure.writeConcreteClasses(outDB);

            try {
                sql.insertRecord(structTable, MapFact.<KeyField, DataObject>EMPTY(), MapFact.singleton(StructTable.instance.struct, (ObjectValue) new DataObject((Object) outDBStruct.toByteArray(), ByteArrayClass.instance)), true, TableOwner.global, OperationOwner.unknown);
            } catch (Exception e) {
                ImMap<PropertyField, ObjectValue> propFields = MapFact.singleton(structTable.struct, (ObjectValue) new DataObject((Object) new byte[0], ByteArrayClass.instance));
                sql.insertRecord(structTable, MapFact.<KeyField, DataObject>EMPTY(), propFields, true, TableOwner.global, OperationOwner.unknown);
            }

            if (oldDBStructure.version < 14) {
                systemLogger.info("Recalculate class stats");
                DataSession session = createSession(OperationOwner.unknown);
                businessLogics.recalculateClassStat(session);
                session.apply(businessLogics);
            }

            systemLogger.info("Updating class stats");
            updateClassStat(sql);

            systemLogger.info("Recalculating aggregations");
            recalculateAggregations(sql, recalculateProperties, false); // перерасчитаем агрегации
            //        recalculateAggregations(sql, getAggregateStoredProperties());

            sql.commitTransaction();
        } catch (Throwable e) {
            sql.rollbackTransaction();
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        } finally {
            sql.popNoHandled();
        }

        DataSession session = createSession();
        for (String sid : columnsToDrop.keySet()) {
            DataObject object = session.addObject(reflectionLM.dropColumn);
            reflectionLM.sidDropColumn.change(sid, session, object);
            reflectionLM.sidTableDropColumn.change(columnsToDrop.get(sid), session, object);
            reflectionLM.timeDropColumn.change(new Timestamp(Calendar.getInstance().getTimeInMillis()), session, object);
            reflectionLM.revisionDropColumn.change(getRevision(), session, object);
        }
        if (oldDBStructure.version < 15) {
            convertReflectionPropertyTableToVersion15(session);
        }
        if (oldDBStructure.version < 16) {
            convertReflectionPropertyTableToVersion16(session);
        }
        session.apply(businessLogics);
        session.close();

        initSystemUser();
        
        return sourceHashChanged;
    }

    private String trimTo63(String id) {
        if (id.length() > 63) {
            id = id.substring(0, 63);
        }
        return id;
    }
        
    
    // Нужен пока для перехода на новую версию базы (version=15)
    private void convertDBStructureToVersion15(OldDBStructure oldDBStructure, NewDBStructure newDBStructure, SQLSession sql) throws SQLException {
        Map<String, String> propertySIDChanges = getChangesAfter(oldDBStructure.dbVersion, this.propertySIDChanges);
        SQLSyntax syntax = adapter;
        Map<String, String> convertSIDChanges = new HashMap<String, String>();

        // сначала проверим старые переименования
        for (Map.Entry<String, String> entry : propertySIDChanges.entrySet()) {
            boolean found = false;
            for (DBStoredProperty oldProperty : oldDBStructure.storedProperties) {
                if (entry.getKey().equals(oldProperty.getDBName())) {
                    convertSIDChanges.put(entry.getKey(), entry.getValue());
                    oldProperty.setDBName(entry.getValue());
                    found = true;
                    break;
                }
            }
            if (!found) {
                systemLogger.warn("Property " + entry.getKey() + " was not found for renaming to " + entry.getValue());
            }
        }

        Map<String, String> propertyCNChanges = getChangesAfter(oldDBStructure.dbVersion, this.propertyCNChanges);
        OldDBNamePolicy oldPolicy = new OldDBNamePolicy();
        for (DBStoredProperty oldProperty : oldDBStructure.storedProperties) {
            boolean found = false;
            for (DBStoredProperty newProperty : newDBStructure.storedProperties) {
                String newPropertyCN = newProperty.getCanonicalName();
                for (Map.Entry<String, String> change : propertyCNChanges.entrySet()) {
                    if (newPropertyCN.equals(change.getValue())) {
                        newPropertyCN = change.getKey();
                        break;
                    }
                }

                if (oldProperty.getDBName().equals(oldPolicy.transformToDBName(newPropertyCN))) {
                    String oldSID = oldProperty.getDBName();
                    String newSID = LM.getDBNamePolicy().transformToDBName(newPropertyCN);

                    for (Map.Entry<String, String> change : convertSIDChanges.entrySet()) {
                        if (oldProperty.getDBName().equals(change.getValue())) {
                            oldSID = change.getKey();
                            break;
                        }
                    }
                    convertSIDChanges.put(oldSID, newSID);
                    found = true;
                    oldSID = trimTo63(oldSID);
                    
                    if (!oldSID.equals(newSID)) {
                        systemLogger.info("Renaming column from " + oldSID + " to " + newSID + " in table " + oldProperty.tableName);
                        sql.renameColumn(oldProperty.getTableName(syntax), oldSID, newSID);
                    }
                    oldProperty.setCanonicalName(newPropertyCN);
                    break;
                }
            }
            if (!found) {
                systemLogger.warn("Property " + oldProperty.getDBName() + " was not converted to new database format!");
            }
        }

        for (DBConcreteClass cls : oldDBStructure.concreteClasses) {
            cls.sDataPropID = trimTo63(cls.sDataPropID);
        }
        
        for (Table table : oldDBStructure.tables.keySet()) {
            for (PropertyField field : table.properties) {
                if (convertSIDChanges.containsKey(field.getName())) {
                    field.setName(convertSIDChanges.get(field.getName()));
                }
            }
        }
    }

    private void convertReflectionPropertyTableToVersion15(DataSession session) {
        ImportField canonicalNamePropertyField = new ImportField(reflectionLM.propertyCanonicalNameValueClass);
        ImportField sidPropertyField = new ImportField(reflectionLM.propertySIDValueClass);

        ImportKey<?> keyProperty = new ImportKey(reflectionLM.property, reflectionLM.propertySID.getMapping(sidPropertyField));

        try {
            List<List<Object>> dataProperty = new ArrayList<List<Object>>();
            OldDBNamePolicy oldPolicy = new OldDBNamePolicy();
            for (LP<?, ?> lp : businessLogics.getNamedProperties()) {
                Property<?> property = lp.property;
                if (property.isNamed()) {
                    String oldSID = oldPolicy.transformToDBName(property.getCanonicalName());
                    dataProperty.add(asList((Object) oldSID, property.getCanonicalName()));
                }
            }

            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            properties.add(new ImportProperty(sidPropertyField, reflectionLM.dbNameProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(canonicalNamePropertyField, reflectionLM.canonicalNameProperty.getMapping(keyProperty)));

            List<ImportDelete> deletes = new ArrayList<ImportDelete>();
            deletes.add(new ImportDelete(keyProperty, LM.is(reflectionLM.property).getMapping(keyProperty), false));

            ImportTable table = new ImportTable(asList(sidPropertyField, canonicalNamePropertyField), dataProperty);

            IntegrationService service = new IntegrationService(session, table, asList(keyProperty), properties, deletes);
            service.synchronize(true, false);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
    
    private void convertReflectionPropertyTableToVersion16(DataSession session) {
        ImportField canonicalNameNavigatorElementField = new ImportField(reflectionLM.navigatorElementCanonicalNameClass);
        ImportField sidNavigatorElementField = new ImportField(reflectionLM.navigatorElementSIDClass);

        ImportKey<?> keyNavigatorElement = new ImportKey(reflectionLM.navigatorElement, reflectionLM.navigatorElementSID.getMapping(sidNavigatorElementField));

        try {
            List<List<Object>> data = new ArrayList<List<Object>>();
            
            for (NavigatorElement element : businessLogics.getNavigatorElements()) {
                if (element.isNamed()) {
                    String canonicalName = element.getCanonicalName();
                    String oldSID = canonicalName.replace('.', '_');
                    
                    data.add(asList((Object)oldSID, canonicalName));
                }
            }

            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            properties.add(new ImportProperty(sidNavigatorElementField, reflectionLM.sidNavigatorElement.getMapping(keyNavigatorElement)));
            properties.add(new ImportProperty(canonicalNameNavigatorElementField, reflectionLM.canonicalNameNavigatorElement.getMapping(keyNavigatorElement)));

            List<ImportDelete> deletes = new ArrayList<ImportDelete>();
            deletes.add(new ImportDelete(keyNavigatorElement, LM.is(reflectionLM.navigatorElement).getMapping(keyNavigatorElement), false));

            ImportTable table = new ImportTable(asList(sidNavigatorElementField, canonicalNameNavigatorElementField), data);

            IntegrationService service = new IntegrationService(session, table, asList(keyNavigatorElement), properties, deletes);
            service.synchronize(true, false);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
    
    private void fillIDs(Map<String, String> sIDChanges, Map<String, String> objectSIDChanges) throws SQLException, SQLHandledException {
        DataSession session = createSession(OperationOwner.unknown); // по сути вложенная транзакция

        LM.baseClass.fillIDs(session, LM.staticCaption, LM.staticName, sIDChanges, objectSIDChanges);

        session.apply(businessLogics);

        session.close();
    }

    private void updateClassStat(SQLSession session) throws SQLException, SQLHandledException {        

        Map<Integer, Integer> customObjectClassMap = new HashMap<Integer, Integer>();

        KeyExpr customObjectClassExpr = new KeyExpr("customObjectClass");
        ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object)"innerInvoice", customObjectClassExpr);

        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);
        query.addProperty("statCustomObjectClass", LM.statCustomObjectClass.getExpr(customObjectClassExpr));
        query.and(LM.statCustomObjectClass.getExpr(customObjectClassExpr).getWhere());

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session, OperationOwner.unknown);

        for (int i=0,size=result.size();i<size;i++) {
            Integer statCustomObjectClass = (Integer) result.getValue(i).get("statCustomObjectClass");
            Integer id = (Integer) result.getKey(i).getValue(0);
            if(id != null && statCustomObjectClass != null)
                customObjectClassMap.put(id, statCustomObjectClass);
        }

        for(CustomClass customClass : LM.baseClass.getAllClasses()) {
            if(customClass instanceof ConcreteCustomClass) {
                Integer stat = customObjectClassMap.get(customClass.ID);
                ((ConcreteCustomClass) customClass).stat = stat == null ? 1 : stat;
            }
        }
    }

    public String checkAggregations(SQLSession session) throws SQLException, SQLHandledException {
        List<AggregateProperty> checkProperties = businessLogics.getAggregateStoredProperties();

        final Result<Integer> proceeded = new Result<Integer>(0);
        int total = checkProperties.size();
        ThreadLocalContext.pushActionMessage("Proceeded : " + proceeded.result + " of " + total);
        try {
            String message = "";
            for (AggregateProperty property : checkProperties) {
                message += property.checkAggregation(session, LM.baseClass);

                proceeded.set(proceeded.result + 1);
                ThreadLocalContext.popActionMessage();
                ThreadLocalContext.pushActionMessage("Proceeded : " + proceeded.result + " of " + total);
            }
            return message;
        } finally {
            ThreadLocalContext.popActionMessage();
        }
    }

    public String checkAggregationTableColumn(SQLSession session, String propertyCanonicalName) throws SQLException, SQLHandledException {
        for (CalcProperty property : businessLogics.getAggregateStoredProperties())
            if (propertyCanonicalName.equals(property.getCanonicalName())) {
                return ((AggregateProperty) property).checkAggregation(session, LM.baseClass);
            }
        return null; 
    }

    public String recalculateAggregations(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        return recalculateAggregations(session, businessLogics.getAggregateStoredProperties(), isolatedTransaction);
    }

    public void ensureLogLevel() {
        adapter.ensureLogLevel(Settings.get().getLogLevelJDBC());
    }

    public static interface RunServiceData {
        void run(SessionCreator sql) throws SQLException, SQLHandledException;
    }

    public static void runData(SessionCreator creator, boolean runInTransaction, RunServiceData run) throws SQLException, SQLHandledException {
        if(runInTransaction) {
            ExecutionContext context = (ExecutionContext) creator;
            DataSession session = context.createSession();
            run.run(session);
            session.apply(context);            
            session.close();
        } else
            run.run(creator);
    }

    public static interface RunService {
        void run(SQLSession sql) throws SQLException, SQLHandledException;
    }

    public static void run(SQLSession session, boolean runInTransaction, RunService run) throws SQLException, SQLHandledException {
        run(session, runInTransaction, run, 0);
    }
    
    private static void run(SQLSession session, boolean runInTransaction, RunService run, int attempts) throws SQLException, SQLHandledException {
        if(runInTransaction) {
            session.startTransaction(RECALC_TIL, OperationOwner.unknown);
            try {
                run.run(session);
                session.commitTransaction();
            } catch (Throwable t) {
                session.rollbackTransaction();
                if(t instanceof SQLHandledException && ((SQLHandledException)t).repeatApply(session, OperationOwner.unknown, attempts)) { // update conflict или deadlock или timeout - пробуем еще раз
                    run(session, runInTransaction, run, attempts + 1);
                    return;
                }
                
                throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
            }
                
        } else
            run.run(session);
    }
    public String recalculateAggregations(SQLSession session, final List<AggregateProperty> recalculateProperties, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        final List<String> messageList = new ArrayList<String>();
        final Result<Integer> proceeded = new Result<Integer>(0);
        final int total = recalculateProperties.size();
        final long maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        ThreadLocalContext.pushActionMessage("Proceeded : " + proceeded.result + " of " + total);
        try {
            for (final AggregateProperty property : recalculateProperties)
                run(session, isolatedTransaction, new RunService() {
                    public void run(SQLSession sql) throws SQLException, SQLHandledException {
                        long start = System.currentTimeMillis();
                        property.recalculateAggregation(sql, LM.baseClass);

                        proceeded.set(proceeded.result + 1);
                        ThreadLocalContext.popActionMessage();
                        ThreadLocalContext.pushActionMessage("Proceeded : " + proceeded.result + " of " + total);
                        long time = System.currentTimeMillis() - start;
                        String message = String.format("Recalculate Aggregation: %s, %sms", property.getSID(), time);
                        systemLogger.info(message);
                        if(time > maxRecalculateTime)
                            messageList.add(message);
                    }
                });
        } finally {
            ThreadLocalContext.popActionMessage();
        }
        return businessLogics.formatMessageList(messageList);
    }
        
    public void recalculateAggregationTableColumn(SQLSession session, String propertyCanonicalName, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        for (AggregateProperty property : businessLogics.getAggregateStoredProperties())
            if (propertyCanonicalName.equals(property.getCanonicalName())) {
                runAggregationRecalculation(session, property, isolatedTransaction);
            }
    }
    
    private void runAggregationRecalculation(SQLSession session, final AggregateProperty aggregateProperty, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        run(session, isolatedTransaction, new RunService() {
            public void run(SQLSession sql) throws SQLException, SQLHandledException {
                aggregateProperty.recalculateAggregation(sql, LM.baseClass);
            }});    
    }

    public void recalculateAggregationWithDependenciesTableColumn(SQLSession session, String propertyCanonicalName, boolean isolatedTransaction, boolean dependents) throws SQLException, SQLHandledException {
        recalculateAggregationWithDependenciesTableColumn(session, businessLogics.findProperty(propertyCanonicalName).property, isolatedTransaction, new HashSet<CalcProperty>(), dependents);    
    }
    
    private void recalculateAggregationWithDependenciesTableColumn(SQLSession session, Property property, boolean isolatedTransaction, Set<CalcProperty> calculated, boolean dependents) throws SQLException, SQLHandledException {
        if (!dependents) {
            for (CalcProperty prop : (Iterable<CalcProperty>) ((CalcProperty) property).getDepends()) {
                if (prop != property && !calculated.contains(prop)) {
                    recalculateAggregationWithDependenciesTableColumn(session, prop, isolatedTransaction, calculated, false);
                }
            }            
        }
        
        if (property instanceof AggregateProperty && ((AggregateProperty) property).isStored()) {
            runAggregationRecalculation(session, (AggregateProperty) property, isolatedTransaction);
            calculated.add((AggregateProperty) property);
        }

        if (dependents) {
            for (AggregateProperty prop : businessLogics.getAggregateStoredProperties()) {
                if (prop != property && !calculated.contains(prop) && CalcProperty.depends(prop, (CalcProperty) property)) {
                    recalculateAggregationWithDependenciesTableColumn(session, prop, isolatedTransaction, calculated, true);
                }
            }
        }
    }

    private void checkModules(OldDBStructure dbStructure) {
        String droppedModules = "";
        for (String moduleName : dbStructure.modulesList)
            if (businessLogics.getSysModule(moduleName) == null) {
                systemLogger.info("Module " + moduleName + " has been dropped");
                droppedModules += moduleName + ", ";
            }
        if (denyDropModules && !droppedModules.isEmpty())
            throw new RuntimeException("Dropped modules: " + droppedModules.substring(0, droppedModules.length() - 2));
    }

    private String calculateHashModules() {
        try {
            String modulesString = "";
            for (LogicsModule module : businessLogics.getLogicModules()) {
                if (module instanceof ScriptingLogicsModule)
                    modulesString += new String(Hex.encodeHex(MessageDigest.getInstance("MD5").digest(((ScriptingLogicsModule) module).getCode().getBytes())));
            }

            return new String(Hex.encodeHex(MessageDigest.getInstance("MD5").digest(modulesString.getBytes())));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private boolean checkHashModulesChanged(String oldHash, String newHash) {
        return (oldHash == null || newHash == null) || !oldHash.equals(newHash);
    }


    private void runAlterationScript() {
        if (ignoreMigration) {
            //todo: добавить возможность задавать расположение для migration.script, чтобы можно было запускать разные логики из одного модуля
            return;
        }

        try {
            InputStream scriptStream = getClass().getResourceAsStream("/migration.script");
            if (scriptStream != null) {
                ANTLRInputStream stream = new ANTLRInputStream(scriptStream);
                AlterationScriptLexer lexer = new AlterationScriptLexer(stream);
                AlterationScriptParser parser = new AlterationScriptParser(new CommonTokenStream(lexer));

                parser.self = this;

                parser.script();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Не разбирается с индексами. Было решено, что сохранять индексы необязательно.
    private void alterateDBStructure(OldDBStructure oldData, SQLSession sql) throws SQLException {
        Map<String, String> propertyChanges = getChangesAfter(oldData.dbVersion, propertyCNChanges);
        Map<String, String> tableChanges = getChangesAfter(oldData.dbVersion, tableSIDChanges);
        Map<String, String> classChanges = getChangesAfter(oldData.dbVersion, classSIDChanges);

        Map<String, String> propertyDBNameChanges = new HashMap<String, String>();
        SQLSyntax syntax = adapter;

        for (Map.Entry<String, String> entry : propertyChanges.entrySet()) {
            boolean found = false;
            for (DBStoredProperty oldProperty : oldData.storedProperties) {
                if (entry.getKey().equals(oldProperty.getCanonicalName())) {
                    String newDBName = LM.getDBNamePolicy().transformToDBName(entry.getValue());
                    systemLogger.info("Renaming column from " + oldProperty.getDBName() + " to " + newDBName + " in table " + oldProperty.tableName);
                    sql.renameColumn(oldProperty.getTableName(syntax), oldProperty.getDBName(), newDBName);
                    propertyDBNameChanges.put(oldProperty.getDBName(), newDBName);
                    oldProperty.setCanonicalName(entry.getValue());
                    found = true;
                    break;
                }
            }
            if (!found) {
                systemLogger.warn("Property " + entry.getKey() + " was not found for renaming to " + entry.getValue());
            }
        }

        for (DBStoredProperty oldProperty : oldData.storedProperties) {
            if (tableChanges.containsKey(oldProperty.tableName)) {
                oldProperty.tableName = tableChanges.get(oldProperty.tableName);
            }
        }

        for (Table table : oldData.tables.keySet()) {
            for (PropertyField field : table.properties) {
                if (propertyDBNameChanges.containsKey(field.getName())) {
                    field.setName(propertyDBNameChanges.get(field.getName()));
                }
            }

            if (tableChanges.containsKey(table.getName())) {
                String newSID = tableChanges.get(table.getName());
                systemLogger.info("Renaming table from " + table + " to " + newSID);
                sql.renameTable(table, newSID);
                table.setName(newSID);
            }
        }

        for (DBConcreteClass oldClass : oldData.concreteClasses) {
            if(classChanges.containsKey(oldClass.sID)) {
                oldClass.sID = classChanges.get(oldClass.sID);
            }
        }
    }

    private DBVersion getCurrentDBVersion(DBVersion oldVersion) {
        DBVersion curVersion = oldVersion;
        if (!propertySIDChanges.isEmpty() && curVersion.compare(propertySIDChanges.lastKey()) < 0) {
            curVersion = propertySIDChanges.lastKey();
        }
        if (!propertyCNChanges.isEmpty() && curVersion.compare(propertyCNChanges.lastKey()) < 0) {
            curVersion = propertyCNChanges.lastKey();
        }
        if (!classSIDChanges.isEmpty() && curVersion.compare(classSIDChanges.lastKey()) < 0) {
            curVersion = classSIDChanges.lastKey();
        }
        if (!objectSIDChanges.isEmpty() && curVersion.compare(objectSIDChanges.lastKey()) < 0) {
            curVersion = objectSIDChanges.lastKey();
        }
        if (!tableSIDChanges.isEmpty() && curVersion.compare(tableSIDChanges.lastKey()) < 0) {
            curVersion = tableSIDChanges.lastKey();
        }
        return curVersion;
    }

    private void addSIDChange(TreeMap<DBVersion, List<SIDChange>> sidChanges, String version, String oldSID, String newSID) {
        DBVersion dbVersion = new DBVersion(version);
        if (!sidChanges.containsKey(dbVersion)) {
            sidChanges.put(dbVersion, new ArrayList<SIDChange>());
        }
        sidChanges.get(dbVersion).add(new SIDChange(oldSID, newSID));
    }

    public void addPropertyCNChange(String version, String oldName, List<String> oldClasses, String newName, List<String> newClasses) {
        String oldSignature = PropertyCanonicalNameUtils.createSignatureStr(oldClasses);
        String newSignature = oldSignature;
        if (newClasses != null) {
            newSignature = PropertyCanonicalNameUtils.createSignatureStr(newClasses);
        } 
        addSIDChange(propertyCNChanges, version, oldName + oldSignature, newName + newSignature);
    }   
    
    public void addPropertySIDChange(String version, String oldSID, String newSID) {
        addSIDChange(propertySIDChanges, version, transformUSID(oldSID), transformUSID(newSID));
    }

    public void addClassSIDChange(String version, String oldSID, String newSID) {
        addSIDChange(classSIDChanges, version, transformUSID(oldSID), transformUSID(newSID));
    }

    public void addTableSIDChange(String version, String oldSID, String newSID) {
        addSIDChange(tableSIDChanges, version, transformUSID(oldSID), transformUSID(newSID));
    }

    public void addObjectSIDChange(String version, String oldSID, String newSID) {
        addSIDChange(objectSIDChanges, version, transformObjectUSID(oldSID), transformObjectUSID(newSID));
    }
    
    private String transformUSID(String userSID) {
        return userSID.replaceFirst("\\.", "_");                            
    }
    
    private String transformObjectUSID(String userSID) {
        if (userSID.indexOf(".") != userSID.lastIndexOf(".")) {
            return transformUSID(userSID);
        }
        return userSID;
    }

    private Map<String, String> getChangesAfter(DBVersion versionAfter, TreeMap<DBVersion, List<SIDChange>> allChanges) {
        Map<String, String> resultChanges = new OrderedMap<String, String>();

        for (Map.Entry<DBVersion, List<SIDChange>> changesEntry : allChanges.entrySet()) {
            if (changesEntry.getKey().compare(versionAfter) > 0) {
                List<SIDChange> versionChanges = changesEntry.getValue();
                Map<String, String> versionChangesMap = new OrderedMap<String, String>();

                for (SIDChange change : versionChanges) {
                    if (versionChangesMap.containsKey(change.oldSID)) {
                        throw new RuntimeException(String.format("Renaming '%s' twice in version %s.", change.oldSID, changesEntry.getKey()));
                    }
                    versionChangesMap.put(change.oldSID, change.newSID);
                }

                // Если в текущей версии есть переименование a -> b, а в предыдущих версиях есть c -> a, то заменяем c -> a на c -> b
                for (Map.Entry<String, String> currentChanges : resultChanges.entrySet()) {
                    String renameTo = currentChanges.getValue();
                    if (versionChangesMap.containsKey(renameTo)) {
                        currentChanges.setValue(versionChangesMap.get(renameTo));
                        versionChangesMap.remove(renameTo);
                    }
                }

                // Добавляем оставшиеся (которые не получилось добавить к старым цепочкам) переименования из текущей версии в общий результат
                for (Map.Entry<String, String> change : versionChangesMap.entrySet()) {
                    if (resultChanges.containsKey(change.getKey())) {
                        throw new RuntimeException(String.format("Renaming '%s' twice", change.getKey()));
                    }
                    resultChanges.put(change.getKey(), change.getValue());
                }

                // Проверяем, чтобы не было нескольких переименований в одно и то же
                Set<String> renameToSIDs = new HashSet<String>();
                for (String renameTo : resultChanges.values()) {
                    if (renameToSIDs.contains(renameTo)) {
                        throw new RuntimeException(String.format("Renaming to '%s' twice.", renameTo));
                    }
                    renameToSIDs.add(renameTo);
                }
            }
        }
        return resultChanges;
    }

    @NFLazy
    public void addIndex(LCP<?>... lps) {
        List<CalcProperty> index = new ArrayList<CalcProperty>();
        for (LCP<?> lp : lps) {
            index.add((CalcProperty) lp.property);
        }
        indexes.put(index, lps[0].property.getType() instanceof DataClass);
    }

    public String backupDB(ExecutionContext context, String dumpFileName, List<String> excludeTables) throws IOException, InterruptedException {
        return adapter.backupDB(context, dumpFileName, excludeTables);
    }

    public void analyzeDB(SQLSession session) throws SQLException {
        session.executeDDL(adapter.getAnalyze());
    }

    public void vacuumDB(SQLSession session) throws SQLException {
        session.executeDDL(adapter.getVacuumDB());
    }

    public void packTables(SQLSession session, ImCol<ImplementTable> tables, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        for (final Table table : tables) {
            logger.debug(getString("logics.info.packing.table") + " (" + table + ")... ");
            run(session, isolatedTransaction, new RunService() {
                @Override
                public void run(SQLSession sql) throws SQLException, SQLHandledException {
                    sql.packTable(table, OperationOwner.unknown, TableOwner.global);
                }});
            logger.debug("Done");
        }
    }

    public static int START_TIL = -1;
    public static int DEBUG_TIL = -1;
    public static int RECALC_TIL = -1;
    public static int SESSION_TIL = -1;
    public static int ID_TIL = Connection.TRANSACTION_REPEATABLE_READ;

    public static boolean RECALC_REUPDATE = false;
    public static boolean PROPERTY_REUPDATE = false;

    public void dropColumn(String tableName, String columnName) throws SQLException, SQLHandledException {
        SQLSession sql = getThreadLocalSql();
        sql.startTransaction(DBManager.START_TIL, OperationOwner.unknown);
        try {
            sql.dropColumn(tableName, columnName);
            ImplementTable table = LM.tableFactory.getImplementTablesMap().get(tableName); // надо упаковать таблицу, если удалили колонку
            if (table != null)
                sql.packTable(table, OperationOwner.unknown, TableOwner.global);
            sql.commitTransaction();
        } catch(SQLException e) {
            sql.rollbackTransaction();
            throw e;
        }
    }

    private void updateStats(SQLSession sql) throws SQLException, SQLHandledException {
        updateStats(sql, true); // чтобы сами таблицы статистики получили статистику
        if (!SystemProperties.doNotCalculateStats)
            updateStats(sql, false);
    }

    private <T> ImMap<String, T> readStatsFromDB(SQLSession sql, LCP sIDProp, LCP statsProp, final LCP notNullProp) throws SQLException, SQLHandledException {
        QueryBuilder<String, String> query = new QueryBuilder<String, String>(SetFact.toSet("key"));
        Expr sidToObject = sIDProp.getExpr(query.getMapExprs().singleValue());
        query.and(sidToObject.getWhere());
        query.addProperty("property", statsProp.getExpr(sidToObject));
        if(notNullProp!=null)
            query.addProperty("notNull", notNullProp.getExpr(sidToObject));
        return query.execute(sql, OperationOwner.unknown).getMap().mapKeyValues(new GetValue<String, ImMap<String, Object>>() {
                                                                public String getMapValue(ImMap<String, Object> key) {
                                                                    return ((String) key.singleValue()).trim();
                                                                }}, new GetValue<T, ImMap<String, Object>>() {
                                                                public T getMapValue(ImMap<String, Object> value) {
                                                                    if(notNullProp!=null) {
                                                                        return (T)new Pair<Integer, Integer>((Integer)value.get("property"), (Integer)value.get("notNull"));
                                                                    } else
                                                                        return (T)value.singleValue();
                                                                }});
    }

    public void updateStats(SQLSession sql, boolean statDefault) throws SQLException, SQLHandledException {
        ImMap<String, Integer> tableStats;
        ImMap<String, Integer> keyStats;
        ImMap<String, Pair<Integer, Integer>> propStats;
        if(statDefault) {
            tableStats = MapFact.EMPTY();
            keyStats = MapFact.EMPTY();
            propStats = MapFact.EMPTY();
        } else {
            tableStats = readStatsFromDB(sql, reflectionLM.tableSID, reflectionLM.rowsTable, null);
            keyStats = readStatsFromDB(sql, reflectionLM.tableKeySID, reflectionLM.quantityTableKey, null);
            propStats = readStatsFromDB(sql, reflectionLM.tableColumnSID, reflectionLM.quantityTableColumn, reflectionLM.notNullQuantityTableColumn);
        }

        for (ImplementTable dataTable : LM.tableFactory.getImplementTables()) {
            dataTable.updateStat(tableStats, keyStats, propStats, statDefault);
        }
    }

    private void initSystemUser() {
        // считаем системного пользователя
        try {
            DataSession session = createSession();

            QueryBuilder<String, Object> query = new QueryBuilder<String, Object>(SetFact.singleton("key"));
            query.and(query.getMapExprs().singleValue().isClass(businessLogics.authenticationLM.systemUser));
            ImOrderSet<ImMap<String, Object>> rows = query.execute(session, MapFact.<Object, Boolean>EMPTYORDER(), 1).keyOrderSet();
            if (rows.size() == 0) { // если нету добавим
                systemUserObject = (Integer) session.addObject(businessLogics.authenticationLM.systemUser).object;
                session.apply(businessLogics);
            } else
                systemUserObject = (Integer) rows.single().get("key");

            query = new QueryBuilder<String, Object>(SetFact.singleton("key"));
            query.and(businessLogics.authenticationLM.hostnameComputer.getExpr(session.getModifier(), query.getMapExprs().singleValue()).compare(new DataObject("systemhost"), Compare.EQUALS));
            rows = query.execute(session, MapFact.<Object, Boolean>EMPTYORDER(), 1).keyOrderSet();
            if (rows.size() == 0) { // если нету добавим
                DataObject computerObject = session.addObject(businessLogics.authenticationLM.computer);
                systemComputer = (Integer) computerObject.object;
                businessLogics.authenticationLM.hostnameComputer.change("systemhost", session, computerObject);
                session.apply(businessLogics);
            } else
                systemComputer = (Integer) rows.single().get("key");

            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class DBStoredProperty {
        private String dbName;
        private String canonicalName;

        public Boolean isDataProperty;
        public String tableName;
        
        public String getTableName(SQLSyntax syntax) {
            return syntax.getTableName(tableName);
        }
        
        public ImMap<Integer, KeyField> mapKeys;
        public CalcProperty<?> property = null;
        public ImplementTable getTable() {
            return property.mapTable.table;
        }

        @Override
        public String toString() {
            return getDBName() + ' ' + tableName;
        }

        public DBStoredProperty(CalcProperty<?> property) {
            assert property.isNamed();
            this.setCanonicalName(property.getCanonicalName());
            this.isDataProperty = property instanceof DataProperty;
            this.tableName = property.mapTable.table.getName();
            mapKeys = ((CalcProperty<PropertyInterface>)property).mapTable.mapKeys.mapKeys(new GetValue<Integer, PropertyInterface>() {
                public Integer getMapValue(PropertyInterface value) {
                    return value.ID;
                }});
            this.property = property;
        }

        public DBStoredProperty(String canonicalName, String dbName, Boolean isDataProperty, String tableName, ImMap<Integer, KeyField> mapKeys) {
            this.setCanonicalName(canonicalName);
            if (canonicalName == null) {
                this.setDBName(dbName);
            }
            this.isDataProperty = isDataProperty;
            this.tableName = tableName;
            this.mapKeys = mapKeys;
        }

        public String getDBName() {
            return dbName;
        }

        public void setDBName(String dbName) {
            assert getCanonicalName() == null;
            this.dbName = dbName;
        }

        public String getCanonicalName() {
            return canonicalName;
        }

        public void setCanonicalName(String canonicalName) {
            this.canonicalName = canonicalName;
            if (canonicalName != null) {
                this.dbName = LM.getDBNamePolicy().transformToDBName(canonicalName);
            }
        }
    }

    private class DBConcreteClass {
        public String sID;
        public String sDataPropID; // в каком ClassDataProperty хранился

        @Override
        public String toString() {
            return sID + ' ' + sDataPropID;
        }

        public Integer ID = null; // только для старых
        public ConcreteCustomClass customClass = null; // только для новых

        private DBConcreteClass(String sID, String sDataPropID, Integer ID) {
            this.sID = sID;
            this.sDataPropID = sDataPropID;
            this.ID = ID;
        }

        private DBConcreteClass(ConcreteCustomClass customClass) {
            sID = customClass.getSID();
            sDataPropID = customClass.dataProperty.getDBName();

            this.customClass = customClass;
        }
    }

    private abstract class DBStructure<F> {
        public int version;
        public DBVersion dbVersion;
        public List<String> modulesList = new ArrayList<String>();
        public String hashModules;
        public Map<Table, Map<List<F>, Boolean>> tables = new HashMap<Table, Map<List<F>, Boolean>>();
        public List<DBStoredProperty> storedProperties = new ArrayList<DBStoredProperty>();
        public Set<DBConcreteClass> concreteClasses = new HashSet<DBConcreteClass>();

        public void writeConcreteClasses(DataOutputStream outDB) throws IOException { // отдельно от write, так как ID заполняются после fillIDs
            outDB.writeInt(concreteClasses.size());
            for (DBConcreteClass concreteClass : concreteClasses) {
                outDB.writeUTF(concreteClass.sID);
                outDB.writeUTF(concreteClass.sDataPropID);
                outDB.writeInt(concreteClass.ID);
            }
        }

        public Table getTable(String name) {
            for (Table table : tables.keySet()) {
                if (table.getName().equals(name)) {
                    return table;
                }
            }
            return null;
        }

        public DBStoredProperty getProperty(String name) {
            for (DBStoredProperty prop : storedProperties) {
                if (prop.getDBName().equals(name)) {
                    return prop;
                }
            }
            return null;
        }
    }

    private class NewDBStructure extends DBStructure<Field> {

        public NewDBStructure(DBVersion dbVersion) {
            version = 16;
            this.dbVersion = dbVersion;

            for (Table table : LM.tableFactory.getImplementTablesMap().valueIt()) {
                tables.put(table, new HashMap<List<Field>, Boolean>());
            }

            for (Map.Entry<List<? extends CalcProperty>, Boolean> index : indexes.entrySet()) {
                Iterator<? extends CalcProperty> i = index.getKey().iterator();
                if (!i.hasNext())
                    throw new RuntimeException(getString("logics.policy.forbidden.to.create.empty.indexes"));
                CalcProperty baseProperty = i.next();
                if (!baseProperty.isStored())
                    throw new RuntimeException(getString("logics.policy.forbidden.to.create.indexes.on.non.regular.properties") + " (" + baseProperty + ")");

                ImplementTable indexTable = baseProperty.mapTable.table;

                List<Field> tableIndex = new ArrayList<Field>();
                tableIndex.add(baseProperty.field);

                while (i.hasNext()) {
                    CalcProperty property = i.next();
                    if (!property.isStored())
                        throw new RuntimeException(getString("logics.policy.forbidden.to.create.indexes.on.non.regular.properties") + " (" + baseProperty + ")");
                    if (indexTable.findProperty(property.field.getName()) == null)
                        throw new RuntimeException(getString("logics.policy.forbidden.to.create.indexes.on.properties.in.different.tables", baseProperty, property));
                    tableIndex.add(property.field);
                }
                tables.get(indexTable).put(tableIndex, index.getValue());
            }

            for (CalcProperty<?> property : businessLogics.getStoredProperties()) {
                storedProperties.add(new DBStoredProperty(property));
                assert property.isNamed();
            }

            for (ConcreteCustomClass customClass : businessLogics.getConcreteCustomClasses()) {
                concreteClasses.add(new DBConcreteClass(customClass));
            }
        }

        public void write(DataOutputStream outDB, String hashModules) throws IOException {
            outDB.write('v' + version);  //для поддержки обратной совместимости
            if (version > 2) {
                outDB.writeUTF(dbVersion.toString());
            }

            //записываем список подключенных модулей
            outDB.writeInt(businessLogics.getLogicModules().size());
            for (LogicsModule logicsModule : businessLogics.getLogicModules())
                outDB.writeUTF(logicsModule.getName());

            //записываем хэш всех модулей
            outDB.writeUTF(hashModules);

            outDB.writeInt(tables.size());
            for (Map.Entry<Table, Map<List<Field>, Boolean>> tableIndices : tables.entrySet()) {
                tableIndices.getKey().serialize(outDB);
                outDB.writeInt(tableIndices.getValue().size());
                for (Map.Entry<List<Field>, Boolean> index : tableIndices.getValue().entrySet()) {
                    outDB.writeInt(index.getKey().size());
                    for (Field indexField : index.getKey()) {
                        outDB.writeUTF(indexField.getName());
                    }
                    outDB.writeBoolean(index.getValue());
                }
            }

            outDB.writeInt(storedProperties.size());
            for (DBStoredProperty property : storedProperties) {
                outDB.writeUTF(property.getCanonicalName());
                outDB.writeBoolean(property.isDataProperty);
                outDB.writeUTF(property.tableName);
                for (int i=0,size=property.mapKeys.size();i<size;i++) {
                    outDB.writeInt(property.mapKeys.getKey(i));
                    outDB.writeUTF(property.mapKeys.getValue(i).getName());
                }
            }
        }
    }

    private class OldDBStructure extends DBStructure<String> {

        public OldDBStructure(DataInputStream inputDB, SQLSession sql) throws IOException, SQLException, SQLHandledException {
            dbVersion = new DBVersion("0.0");
            if (inputDB == null) {
                version = -2;
            } else {
                version = inputDB.read() - 'v';
                if (version < 0) {
                    inputDB.reset();
                }

                if (version > 2) {
                    dbVersion = new DBVersion(inputDB.readUTF());
                }

                if (version > 9) {
                    int modulesCount = inputDB.readInt();
                    if (modulesCount > 0) {
                        for (int i = 0; i < modulesCount; i++)
                            modulesList.add(inputDB.readUTF());
                    }
                }
                if(version > 10) {
                    hashModules = inputDB.readUTF();
                }

                for (int i = inputDB.readInt(); i > 0; i--) {
                    SerializedTable prevTable = new SerializedTable(inputDB, LM.baseClass, version);
                    Map<List<String>, Boolean> indices = new HashMap<List<String>, Boolean>();
                    for (int j = inputDB.readInt(); j > 0; j--) {
                        List<String> index = new ArrayList<String>();
                        for (int k = inputDB.readInt(); k > 0; k--) {
                            index.add(inputDB.readUTF());
                        }
                        boolean prevOrdered = version >= 1 && inputDB.readBoolean();
                        indices.put(index, prevOrdered);
                    }
                    tables.put(prevTable, indices);
                }

                int prevStoredNum = inputDB.readInt();
                for (int i = 0; i < prevStoredNum; i++) {
                    String sID = null;
                    String canonicalName = null;
                    if (version >= 15) {
                        canonicalName = inputDB.readUTF();
                    } else {
                        sID = inputDB.readUTF();
                    }
                    boolean isDataProperty = true;
                    if (version >= 0) {
                        isDataProperty = inputDB.readBoolean();
                    }
                    String tableName = inputDB.readUTF();
                    Table prevTable = getTable(tableName);
                    MExclMap<Integer, KeyField> mMapKeys = MapFact.mExclMap(prevTable.getTableKeys().size());
                    for (int j = 0; j < prevTable.getTableKeys().size(); j++) {
                        mMapKeys.exclAdd(inputDB.readInt(), prevTable.findKey(inputDB.readUTF()));
                    }
                    storedProperties.add(new DBStoredProperty(canonicalName, sID, isDataProperty, tableName, mMapKeys.immutable()));
                }

                if(version > 4) {
                    int prevConcreteNum = inputDB.readInt();
                    for(int i = 0; i < prevConcreteNum; i++)
                        concreteClasses.add(new DBConcreteClass(inputDB.readUTF(), inputDB.readUTF(), inputDB.readInt()));
                } else {
                    // todo [dale]: Этот кусок сейчас нерабочий из-за создания StoredDataProperty без sid
                    assert false;
                    KeyField objectKey = new KeyField("object", ObjectType.instance);
                    PropertyField classField = new PropertyField("class", ObjectType.instance);
                    SerializedTable objectTable = new SerializedTable("objects", SetFact.singletonOrder(objectKey), SetFact.singleton(classField), LM.baseClass);
                    tables.put(objectTable, new HashMap<List<String>, Boolean>());
                    storedProperties.add(new DBStoredProperty(null, "class", false, "objects", MapFact.singleton(0, objectKey))); // в map'е все равно что будет

                    QueryBuilder<String, String> allClassesQuery = new QueryBuilder<String, String>(SetFact.singleton("key"));
                    Expr key = allClassesQuery.getMapExprs().singleValue();

                    BaseLogicsModule LM = ThreadLocalContext.getBusinessLogics().LM;

                    ImplementTable table = new ImplementTable("sidClass", LM.baseClass);

                    StoredDataProperty dataProperty = new StoredDataProperty("classSID", new ValueClass[] {LM.baseClass}, StringClass.get(250));
                    LCP classSID = new LCP<ClassPropertyInterface> (dataProperty);
                    dataProperty.markStored(LM.tableFactory, table);

                    Expr sidExpr = classSID.getExpr(Property.defaultModifier, key);

                    allClassesQuery.and(sidExpr.getWhere()); // вот тут придется напрямую из таблицы читать id'ки для классов, потому как isClass использовать очевидно нельзя
                    allClassesQuery.and(objectTable.join(MapFact.singleton(objectKey, key)).getExpr(classField).compare(new ValueExpr(Integer.MAX_VALUE - 5, LM.baseClass.objectClass), Compare.EQUALS));
                    allClassesQuery.addProperty("sid", sidExpr);
                    ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> qResult = allClassesQuery.execute(sql, OperationOwner.unknown);

                    for (int i = 0, size = qResult.size(); i < size; i++)
                        concreteClasses.add(new DBConcreteClass(
                                ((String) qResult.getValue(i).get("sid")).trim(), "class", (Integer) qResult.getKey(i).singleValue()));
                }
            }
        }
    }

    public static class SIDChange {
        public String oldSID;
        public String newSID;

        public SIDChange(String oldSID, String newSID) {
            this.oldSID = oldSID;
            this.newSID = newSID;
        }
    }

    public static class DBVersion {
        private List<Integer> version;

        public DBVersion(String version) {
            this.version = versionToList(version);
        }

        public static List<Integer> versionToList(String version) {
            String[] splittedArr = version.split("\\.");
            List<Integer> intVersion = new ArrayList<Integer>();
            for (String part : splittedArr) {
                intVersion.add(Integer.parseInt(part));
            }
            return intVersion;
        }

        public int compare(DBVersion rhs) {
            return compareVersions(version, rhs.version);
        }

        public static int compareVersions(List<Integer> lhs, List<Integer> rhs) {
            for (int i = 0; i < Math.max(lhs.size(), rhs.size()); i++) {
                int left = (i < lhs.size() ? lhs.get(i) : 0);
                int right = (i < rhs.size() ? rhs.get(i) : 0);
                if (left < right) return -1;
                if (left > right) return 1;
            }
            return 0;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < version.size(); i++) {
                if (i > 0) {
                    buf.append(".");
                }
                buf.append(version.get(i));
            }
            return buf.toString();
        }
    }
}
