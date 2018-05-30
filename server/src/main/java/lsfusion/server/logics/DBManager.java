package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMap;
import lsfusion.base.col.implementations.abs.ASet;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.lru.ALRUMap;
import lsfusion.interop.Compare;
import lsfusion.server.*;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.*;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.query.StaticExecuteEnvironmentImpl;
import lsfusion.server.data.sql.DataAdapter;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.navigator.*;
import lsfusion.server.integration.*;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.lifecycle.LogicsManager;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.mutables.NFLazy;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.table.IDTable;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.session.ClassChange;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.SessionCreator;
import lsfusion.server.stack.ParamMessage;
import lsfusion.server.stack.ProgressStackItem;
import lsfusion.server.stack.StackMessage;
import lsfusion.server.stack.StackProgress;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.apache.log4j.Logger;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.util.*;

import static java.util.Arrays.asList;
import static lsfusion.base.SystemUtils.getRevision;
import static lsfusion.server.context.ThreadLocalContext.localize;

public class DBManager extends LogicsManager implements InitializingBean {
    public static final Logger logger = Logger.getLogger(DBManager.class);
    public static final Logger startLogger = ServerLoggers.startLogger;
    public static final Logger serviceLogger = ServerLoggers.serviceLogger;

    private static Comparator<DBVersion> dbVersionComparator = new Comparator<DBVersion>() {
        @Override
        public int compare(DBVersion lhs, DBVersion rhs) {
            return lhs.compare(rhs);
        }
    };

    private TreeMap<DBVersion, List<SIDChange>> propertyCNChanges = new TreeMap<>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> actionCNChanges = new TreeMap<>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> propertyDrawNameChanges = new TreeMap<>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> storedPropertyCNChanges = new TreeMap<>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> classSIDChanges = new TreeMap<>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> tableSIDChanges = new TreeMap<>(dbVersionComparator);
    private TreeMap<DBVersion, List<SIDChange>> objectSIDChanges = new TreeMap<>(dbVersionComparator);

    private Map<String, String> finalPropertyDrawNameChanges = new HashMap<>();

    private DataAdapter adapter;

    private RestartManager restartManager;

    private BusinessLogics<?> businessLogics;

    private boolean ignoreMigration;
    private boolean migrationScriptWasRead = false;

    private boolean denyDropModules;

    private boolean denyDropTables;

    private String dbNamingPolicy;
    private Integer dbMaxIdLength;

    public boolean needExtraUpdateStats = false;

    private BaseLogicsModule<?> LM;

    private ReflectionLogicsModule reflectionLM;

    private long systemUserObject;
    private long systemComputer;

    private final ThreadLocal<SQLSession> threadLocalSql;

    private final Map<ImList<CalcPropertyObjectInterfaceImplement<String>>, Boolean> indexes = new HashMap<>();
    
    private String defaultUserLanguage;
    private String defaultUserCountry;
    private String defaultUserTimeZone;
    private Integer defaultTwoDigitYearStart;

    public DBManager() {
        super(DBMANAGER_ORDER);

        threadLocalSql = new ThreadLocal<>();
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


    public String getDbNamingPolicy() {
        return dbNamingPolicy;
    }

    public void setDbNamingPolicy(String dbNamingPolicy) {
        this.dbNamingPolicy = dbNamingPolicy;
    }

    public Integer getDbMaxIdLength() {
        return dbMaxIdLength;
    }

    public void setDbMaxIdLength(Integer dbMaxIdLength) {
        this.dbMaxIdLength = dbMaxIdLength;
    }

    public void updateStats(SQLSession sql) throws SQLException, SQLHandledException {
        businessLogics.updateStats(sql, false);
    }

    public SQLSyntax getSyntax() {
        return adapter.syntax;
    }
    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(adapter, "adapter must be specified");
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(restartManager, "restartManager must be specified");
    }

    public boolean sourceHashChanged;
    public String hashModules;
    @Override
    protected void onInit(LifecycleEvent event) {
        this.LM = businessLogics.LM;
        this.reflectionLM = businessLogics.reflectionLM;
        try {
            if(getSyntax().getSyntaxType() == SQLSyntaxType.MSSQL)
                Expr.useCasesCount = 5;

            startLogger.info("Synchronizing DB.");
            sourceHashChanged = synchronizeDB();
        } catch (Exception e) {
            throw new RuntimeException("Error synchronizing DB: ", e);
        }
    }

    @IdentityStrongLazy // ресурсы потребляет
    private SQLSession getIDSql() { // подразумевает synchronized использование
        try {
            return createSQL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @IdentityStrongLazy
    private SQLSession getSystemSql() {
        try {
            return createSQL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @IdentityStrongLazy
    public SQLSession getStopSql() {
        try {
            return createSQL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long getSystemUserObject() {
        return systemUserObject;
    }

    private SQLSessionUserProvider userProvider = new SQLSessionUserProvider() {
        @Override
        public Long getCurrentUser() {
            return systemUserObject;
        }

        @Override
        public LogInfo getLogInfo() {
            return LogInfo.system;
        }

        @Override
        public Long getCurrentComputer() {
            return systemComputer;
        }
    };

    public SQLSession createSQL() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        return createSQL(userProvider);
    }

    public SQLSession createSQL(SQLSessionUserProvider environment) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        return new SQLSession(adapter, environment);
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
                sql.close();
            } catch (SQLException e) {
                ServerLoggers.sqlSuppLog(e);
            } finally {
                threadLocalSql.set(null);
            }
        return sql;
    }

    public long generateID() {
        try {
            return IDTable.instance.generateID(getIDSql(), IDTable.OBJECT);
        } catch (SQLException e) {
            throw new RuntimeException(localize("{logics.info.error.reading.user.data}"), e);
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
                    public boolean changeCurrentUser(DataObject user, ExecutionStack stack) {
                        throw new RuntimeException("not supported");
                    }

                    public ObjectValue getCurrentUser() {
                        return new DataObject(systemUserObject, businessLogics.authenticationLM.systemUser);
                    }

                    @Override
                    public Long getCurrentUserRole() {
                        return null;
                    }
                },
                new ComputerController() {
                    public ObjectValue getCurrentComputer() {
                        return new DataObject(systemComputer, businessLogics.authenticationLM.computer);
                    }

                    public boolean isFullClient() {
                        return false;
                    }
                },
                new FormController() {
                    @Override
                    public void changeCurrentForm(ObjectValue form) {
                        throw new RuntimeException("not supported");
                    }

                    @Override
                    public ObjectValue getCurrentForm() {
                        return NullValue.instance;
                    }
                },
                new ConnectionController() {
                    public void changeCurrentConnection(DataObject connection) {
                        throw new RuntimeException("not supported");
                    }

                    @Override
                    public ObjectValue getCurrentConnection() {
                        return NullValue.instance;
                    }
                },
                new TimeoutController() {
                    public int getTransactionTimeout() {
                        return 0;
                    }
                },
                new ChangesController() {
                    public void regChange(ImSet<CalcProperty> changes, DataSession session) {
                    }

                    public ImSet<CalcProperty> update(DataSession session, FormInstance form) {
                        return SetFact.EMPTY();
                    }

                    public void registerForm(FormInstance form) {
                    }

                    public void unregisterForm(FormInstance form) {
                    }
                }, new LocaleController() {
                    public Locale getLocale() {
                        return Locale.getDefault();
                    }
                }, upOwner
        );
    }

    public DataSession createSession(SQLSession sql, UserController userController, ComputerController computerController, FormController formController,
                                     ConnectionController connectionController, TimeoutController timeoutController, ChangesController changesController, LocaleController localeController, OperationOwner owner) throws SQLException {
        //todo: неплохо бы избавиться от зависимости на restartManager, а то она неестественна
        return new DataSession(sql, userController, computerController, formController, connectionController,
                timeoutController, changesController, localeController, new IsServerRestartingController() {
                                   public boolean isServerRestarting() {
                                       return restartManager.isPendingRestart();
                                   }
                               },
                               LM.baseClass, businessLogics.systemEventsLM.session, businessLogics.systemEventsLM.currentSession, getIDSql(), businessLogics.getSessionEvents(), owner);
    }


    public boolean isServer() {
        try {
            String localhostName = SystemUtils.getLocalHostName();
            return DBManager.HOSTNAME_COMPUTER == null || (localhostName != null && localhostName.equals(DBManager.HOSTNAME_COMPUTER));
        } catch (Exception e) {
            logger.error("Error reading computer: ", e);
            throw new RuntimeException(e);
        }
    }

    public DataObject getServerComputerObject(ExecutionStack stack) {
        return new DataObject(getComputer(SystemUtils.getLocalHostName(), stack), businessLogics.authenticationLM.computer);
    }

    public Long getComputer(String strHostName, ExecutionStack stack) {
        try {
            try (DataSession session = createSession(getSystemSql())) {

                QueryBuilder<String, Object> q = new QueryBuilder<>(SetFact.singleton("key"));
                q.and(
                        businessLogics.authenticationLM.hostnameComputer.getExpr(
                                session.getModifier(), q.getMapExprs().get("key")
                        ).compare(new DataObject(strHostName), Compare.EQUALS)
                );

                Long result;

                ImSet<ImMap<String, Object>> keys = q.execute(session).keys();
                if (keys.size() == 0) {
                    DataObject addObject = session.addObject(businessLogics.authenticationLM.computer);
                    businessLogics.authenticationLM.hostnameComputer.change(strHostName, session, addObject);

                    result = (Long) addObject.object;
                    session.apply(businessLogics, stack);
                } else {
                    result = (Long) keys.iterator().next().get("key");
                }

                logger.debug("Begin user session " + strHostName + " " + result);
                return result;
            }
        } catch (Exception e) {
            logger.error("Error reading computer: ", e);
            throw new RuntimeException(e);
        }
    }

    public ObjectValue getFormObject(String canonicalName, ExecutionStack stack) {
        return canonicalName == null ? NullValue.instance : new DataObject(getForm(canonicalName, stack), businessLogics.reflectionLM.form);
    }

    public Long getForm(String canonicalName, ExecutionStack stack) {
        try {
            try (DataSession session = createSession(getSystemSql())) {
                Long result = (Long) businessLogics.reflectionLM.formByCanonicalName.read(session, new DataObject(canonicalName));
                if (result == null) {
                    DataObject addObject = session.addObject(businessLogics.reflectionLM.form);
                    businessLogics.reflectionLM.formCanonicalName.change(canonicalName, session, addObject);
                    result = (Long) addObject.object;
                    session.apply(businessLogics, stack);
                }
                return result;
            }
        } catch (Exception e) {
            logger.error("Error reading form: ", e);
            throw new RuntimeException(e);
        }
    }

    private String getDroppedTablesString(SQLSession sql, OldDBStructure oldDBStructure, NewDBStructure newDBStructure) throws SQLException, SQLHandledException {
        String droppedTables = "";
        for (NamedTable table : oldDBStructure.tables.keySet()) {
            if (newDBStructure.getTable(table.getName()) == null) {
                ImRevMap<KeyField, KeyExpr> mapKeys = table.getMapKeys();
                Expr expr = GroupExpr.create(MapFact.<KeyField, KeyExpr>EMPTY(), ValueExpr.COUNT, table.join(mapKeys).getWhere(), GroupType.SUM, MapFact.<KeyField, Expr>EMPTY());
                Object result = Expr.readValue(sql, expr, OperationOwner.unknown); // таблица не пустая
                if (result != null) {
                    if (!droppedTables.equals("")) {
                        droppedTables += ", ";
                    }
                    droppedTables += table;
                }
            }
        }
        return droppedTables;
    }

    private static ImMap<String, ImRevMap<String, String>> getFieldToCanMap(DBStructure<?> dbStructure) {
        return SetFact.fromJavaOrderSet(dbStructure.storedProperties).getSet().group(new BaseUtils.Group<String, DBStoredProperty>() {
            public String group(DBStoredProperty value) {
                return value.tableName;
            }}).mapValues(new GetValue<ImRevMap<String, String>, ImSet<DBStoredProperty>>() {
            public ImRevMap<String, String> getMapValue(ImSet<DBStoredProperty> value) {
                return value.mapRevKeyValues(new GetValue<String, DBStoredProperty>() {
                    public String getMapValue(DBStoredProperty value) {
                        return value.getDBName();
                    }}, new GetValue<String, DBStoredProperty>() {
                    public String getMapValue(DBStoredProperty value) {
                        return value.getCanonicalName();
                    }});
            }});

    }

    // Удаляем несуществующие индексы и убираем из newDBStructure не изменившиеся индексы
    // Делаем это до применения migration script, то есть не пытаемся сохранить все возможные индексы по максимуму
    private void checkIndices(SQLSession sql, OldDBStructure oldDBStructure, NewDBStructure newDBStructure) throws SQLException {

        ImMap<String, String> propertyChanges = MapFact.fromJavaMap(getChangesAfter(oldDBStructure.dbVersion, storedPropertyCNChanges));

        ImMap<String, ImRevMap<String, String>> oldTableFieldToCan = MapFact.EMPTY(); ImMap<String, ImRevMap<String, String>> newTableFieldToCan = MapFact.EMPTY();
        if(!propertyChanges.isEmpty()) { // оптимизация
            oldTableFieldToCan = getFieldToCanMap(oldDBStructure);
            newTableFieldToCan = getFieldToCanMap(newDBStructure);
        }

        for (Map.Entry<NamedTable, Map<List<String>, Boolean>> oldTableIndices : oldDBStructure.tables.entrySet()) {
            NamedTable oldTable = oldTableIndices.getKey();
            NamedTable newTable = newDBStructure.getTable(oldTable.getName());
            Map<List<Field>, Boolean> newTableIndices = null; Map<List<String>, Pair<Boolean, List<Field>>> newTableIndicesNames = null; ImMap<String, String> fieldOldToNew = MapFact.EMPTY();
            if(newTable != null) {
                newTableIndices = newDBStructure.tables.get(newTable);
                newTableIndicesNames = new HashMap<>();
                for (Map.Entry<List<Field>, Boolean> entry : newTableIndices.entrySet()) {
                    List<String> names = new ArrayList<>();
                    for (Field field : entry.getKey())
                        names.add(field.getName());
                    newTableIndicesNames.put(names, new Pair<>(entry.getValue(), entry.getKey()));
                }

                // old field -> old cn -> new cn -> ne field
                if(!propertyChanges.isEmpty()) {
                    ImRevMap<String, String> oldFieldToCan = oldTableFieldToCan.get(oldTable.getName());
                    ImRevMap<String, String> newFieldToCan = newTableFieldToCan.get(newTable.getName());
                    if(oldFieldToCan != null && newFieldToCan != null) // так как таблицы могут быть пустыми
                        fieldOldToNew = oldFieldToCan.innerJoin(propertyChanges).innerCrossValues(newFieldToCan);
                }
            }

            for (Map.Entry<List<String>, Boolean> oldIndex : oldTableIndices.getValue().entrySet()) {
                List<String> oldIndexKeys = oldIndex.getKey();
                ImOrderSet<String> oldIndexKeysSet = SetFact.fromJavaOrderSet(oldIndexKeys);

                boolean replaced = BaseUtils.replaceListElements(oldIndexKeys, fieldOldToNew);

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
                    sql.dropIndex(oldTable, oldTable.keys, oldIndexKeysSet, oldOrder, Settings.get().isStartServerAnyWay());
                } else {
                    if(replaced) // assert что keys совпадают
                        sql.renameIndex(oldTable, oldTable.keys, oldIndexKeysSet, SetFact.fromJavaOrderSet(oldIndexKeys), oldOrder, Settings.get().isStartServerAnyWay());
                }
            }
        }
    }

    private void checkUniqueDBName(NewDBStructure struct) {
        Map<Pair<String, String>, DBStoredProperty> sids = new HashMap<>();
        for (DBStoredProperty property : struct.storedProperties) {
            Pair<String, String> key = new Pair<>(property.getDBName(), property.getTable().getName());
            if (sids.containsKey(key)) {
                startLogger.error(String.format("Equal sid '%s' in table '%s': %s and %s", key.first, key.second, sids.get(key).getCanonicalName(), property.getCanonicalName()));
            }
            sids.put(key, property);
         }
    }

    public void uploadToDB(SQLSession sql, boolean isolatedTransactions, final DataAdapter adapter) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, SQLHandledException {
        final OperationOwner owner = OperationOwner.unknown;
        final SQLSession sqlFrom = new SQLSession(adapter, userProvider);

        sql.pushNoQueryLimit();
        try {
            ImSet<DBTable> tables = SetFact.addExcl(LM.tableFactory.getImplementTables(), IDTable.instance);
            final int size = tables.size();
            for (int i = 0; i < size; i++) {
                final DBTable implementTable = tables.get(i);
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

    @StackMessage("{logics.upload.db}")
    private void uploadTableToDB(SQLSession sql, final @ParamMessage DBTable implementTable, @ParamMessage String progress, final SQLSession sqlTo, final OperationOwner owner) throws SQLException, SQLHandledException {
        sqlTo.truncate(implementTable, owner);

        final ProgressStackItemResult stackItem = new ProgressStackItemResult();
        try {
            final Result<Integer> proceeded = new Result<>(0);
            final int total = sql.getCount(implementTable, owner);
            ResultHandler<KeyField, PropertyField> reader = new ReadBatchResultHandler<KeyField, PropertyField>(10000) {
                public void start() {
                    stackItem.value = ThreadLocalContext.pushProgressMessage(localize("{logics.upload.db}"), proceeded.result, total);
                }

                public void proceedBatch(ImOrderMap<ImMap<KeyField, Object>, ImMap<PropertyField, Object>> batch) throws SQLException {
                    sqlTo.insertBatchRecords(implementTable, batch.getMap(), owner);
                    proceeded.set(proceeded.result + batch.size());
                    ThreadLocalContext.popActionMessage(stackItem.value);
                    stackItem.value = ThreadLocalContext.pushProgressMessage(localize("{logics.upload.db}"), proceeded.result, total);
                }

                public void finish() throws SQLException {
                    ThreadLocalContext.popActionMessage(stackItem.value);
                    super.finish();
                }
            };
            implementTable.readData(sql, LM.baseClass, owner, true, reader);
        } finally {
            ThreadLocalContext.popActionMessage(stackItem.value);
        }
    }

    private class ProgressStackItemResult {
        ProgressStackItem value;
    }

    private OldDBStructure getOldDBStructure(SQLSession sql) throws SQLException, SQLHandledException, IOException {
        DataInputStream inputDB = null;
        StructTable structTable = StructTable.instance;
        byte[] struct = (byte[]) sql.readRecord(structTable, MapFact.<KeyField, DataObject>EMPTY(), structTable.struct, OperationOwner.unknown);
        if (struct != null) {
            inputDB = new DataInputStream(new ByteArrayInputStream(struct));
        }
        return new OldDBStructure(inputDB);
    }

    public static boolean explicitMigrate = false;

    public boolean synchronizeDB() throws SQLException, IOException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        SQLSession sql = getThreadLocalSql();

        // инициализируем таблицы
        LM.tableFactory.fillDB(sql, LM.baseClass);

        // потом надо сделать соответствующий механизм для Formula
        ScriptingLogicsModule module = businessLogics.getModule("Country");
        if(module != null) {
            LCP<?> lp = module.findProperty("isDayOff[Country,DATE]");

            Properties props = new Properties();
            props.put("dayoff.tablename", lp.property.mapTable.table.getName(sql.syntax));
            props.put("dayoff.fieldname", lp.property.getDBName());
            adapter.ensureScript("jumpWorkdays.sc", props);
        }

        SQLSyntax syntax = getSyntax();

        // "старое" состояние базы
        OldDBStructure oldDBStructure = getOldDBStructure(sql);
        
        checkFormsTable(oldDBStructure);
        checkModules(oldDBStructure);

        // В этот момент в обычной ситуации migration script уже был обработан, вызов оставлен на всякий случай. Повторный вызов ничего не делает.
        runMigrationScript();

        Map<String, String> columnsToDrop = new HashMap<>();

        boolean noTransSyncDB = Settings.get().isNoTransSyncDB();

        try {
            sql.pushNoHandled();

            if(noTransSyncDB)
                sql.startFakeTransaction(OperationOwner.unknown);
            else
                sql.startTransaction(DBManager.START_TIL, OperationOwner.unknown);

            // новое состояние базы
            ByteArrayOutputStream outDBStruct = new ByteArrayOutputStream();
            DataOutputStream outDB = new DataOutputStream(outDBStruct);

            DBVersion newDBVersion = getCurrentDBVersion(oldDBStructure.dbVersion);
            NewDBStructure newDBStructure = new NewDBStructure(newDBVersion);

            if(newDBStructure.version >= 22 && oldDBStructure.version < 22 && oldDBStructure.version > 0) { // временно, для явной типизации
                if (SystemProperties.lightStart) {
                    throw new RuntimeException("YOU HAVE TO START SERVER WITH ISDEBUG : FALSE");
                }
                Settings.get().setStartServerAnyWay(true);
                explicitMigrate = true;
            }

            checkUniqueDBName(newDBStructure);
            // запишем новое состояние таблиц (чтобы потом изменять можно было бы)
            newDBStructure.write(outDB);

            startLogger.info("Checking indices");

            checkIndices(sql, oldDBStructure, newDBStructure);

            if (!oldDBStructure.isEmpty()) {
                startLogger.info("Applying migration script (" + oldDBStructure.dbVersion + " -> " + newDBStructure.dbVersion + ")");

                // применяем к oldDBStructure изменения из migration script, переименовываем таблицы и поля  
                alterDBStructure(oldDBStructure, newDBStructure, sql);
            }

            // проверка, не удалятся ли старые таблицы
            if (denyDropTables) {
                String droppedTables = getDroppedTablesString(sql, oldDBStructure, newDBStructure);
                if (!droppedTables.isEmpty()) {
                    throw new RuntimeException("Dropped tables: " + droppedTables);
                }
            }

            // добавим таблицы которых не было
            startLogger.info("Creating tables");
            for (NamedTable table : newDBStructure.tables.keySet()) {
                if (oldDBStructure.getTable(table.getName()) == null)
                    sql.createTable(table, table.keys, startLogger);
            }

            // проверяем изменение структуры ключей
            for (NamedTable table : newDBStructure.tables.keySet()) {
                NamedTable oldTable = oldDBStructure.getTable(table.getName());
                if (oldTable != null) {
                    for (KeyField key : table.keys) {
                        KeyField oldKey = oldTable.findKey(key.getName());
                        if (!(key.type.equals(oldKey.type))) {
                            sql.modifyColumn(table, key, oldKey.type);
                            startLogger.info("Changing type of key column " + key + " in table " + table + " from " + oldKey.type + " to " + key.type);
                        }
                    }
                }
            }

            List<CalcProperty> recalculateProperties = new ArrayList<>();

            MExclSet<Pair<String, String>> mDropColumns = SetFact.mExclSet(); // вообще pend'ить нужно только classDataProperty, но их тогда надо будет отличать

            // бежим по свойствам
            List<DBStoredProperty> restNewDBStored = new LinkedList<>(newDBStructure.storedProperties);
            List<CalcProperty> recalculateStatProperties = new ArrayList<>();
            Map<ImplementTable, Map<Field, Type>> alterTableMap = new HashMap<>();
            for (DBStoredProperty oldProperty : oldDBStructure.storedProperties) {
                NamedTable oldTable = oldDBStructure.getTable(oldProperty.tableName);

                boolean keep = false, moved = false;
                for (Iterator<DBStoredProperty> is = restNewDBStored.iterator(); is.hasNext(); ) {
                    DBStoredProperty newProperty = is.next();

                    if (newProperty.getCanonicalName().equals(oldProperty.getCanonicalName())) {
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

                                startLogger.info(localize(LocalizedString.createFormatted("{logics.info.property.is.transferred.from.table.to.table}", newProperty.property.field, newProperty.property.caption, oldProperty.tableName, newProperty.tableName)));
                                newProperty.property.mapTable.table.moveColumn(sql, newProperty.property.field, oldTable,
                                        foundInterfaces.join((ImMap<PropertyInterface, KeyField>) newProperty.property.mapTable.mapKeys), oldTable.findProperty(oldProperty.getDBName()));
                                startLogger.info("Done");
                                moved = true;
                                recalculateStatProperties.add(newProperty.property);
                            } else { // надо проверить что тип не изменился
                                Type oldType = oldTable.findProperty(oldProperty.getDBName()).type;
                                if (!oldType.equals(newProperty.property.field.type)) {
                                    startLogger.info("Prepare changing type of property column " + newProperty.property.field + " in table " + newProperty.tableName + " from " + oldType + " to " + newProperty.property.field.type);
                                    Map<Field, Type> fieldTypeMap = alterTableMap.get(newTable);
                                    if(fieldTypeMap == null)
                                        fieldTypeMap = new HashMap<>();
                                    fieldTypeMap.put(newProperty.property.field, oldType);
                                    alterTableMap.put(newTable, fieldTypeMap);
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
                        ExConnection exConnection = sql.getConnection();
                        Connection connection = exConnection.sql;
                        Savepoint savepoint = null;
                        try {
                            savepoint = connection.setSavepoint();
                            sql.renameColumn(oldProperty.getTableName(syntax), oldProperty.getDBName(), newName);
                            columnsToDrop.put(newName, oldProperty.tableName);
                        } catch (PSQLException e) { // колонка с новым именем уже существует
                            if(savepoint != null)
                                connection.rollback(savepoint);
                            mDropColumns.exclAdd(new Pair<>(oldTable.getName(syntax), oldProperty.getDBName()));
                        } finally {
                            sql.returnConnection(exConnection, OperationOwner.unknown);
                        }
                    } else
                        mDropColumns.exclAdd(new Pair<>(oldTable.getName(syntax), oldProperty.getDBName()));
                }
            }

            for (Map.Entry<ImplementTable, Map<Field, Type>> entry : alterTableMap.entrySet()) {
                startLogger.info("Changing type of property columns (" + entry.getValue().size() + ") in table " + entry.getKey().getName() + " started");
                sql.modifyColumns(entry.getKey(), entry.getValue());
                startLogger.info("Changing type of property columns (" + entry.getValue().size() + ") in table " + entry.getKey().getName() + " finished");
            }

            for (DBStoredProperty property : restNewDBStored) { // добавляем оставшиеся
                sql.addColumn(property.getTable(), property.property.field);
                if (oldDBStructure.version > 0) // если все свойства "новые" то ничего перерасчитывать не надо
                    recalculateProperties.add(property.property);
            }

            // обработка изменений с классами
            MMap<String, ImMap<String, ImSet<Long>>> mToCopy = MapFact.mMap(AMap.<String, String, Long>addMergeMapSets()); // в какое свойство, из какого свойства - какой класс
            for (DBConcreteClass oldClass : oldDBStructure.concreteClasses) {
                for (DBConcreteClass newClass : newDBStructure.concreteClasses) {
                    if (oldClass.sID.equals(newClass.sID)) {
                        if (!(oldClass.sDataPropID.equals(newClass.sDataPropID))) // надо пометить перенос, и удаление
                            mToCopy.add(newClass.sDataPropID, MapFact.singleton(oldClass.sDataPropID, SetFact.singleton(oldClass.ID)));
                        break;
                    }
                }
            }
            ImMap<String, ImMap<String, ImSet<Long>>> toCopy = mToCopy.immutable();
            for (int i = 0, size = toCopy.size(); i < size; i++) { // перенесем классы, которые сохранились но изменили поле
                DBStoredProperty classProp = newDBStructure.getProperty(toCopy.getKey(i));
                NamedTable table = newDBStructure.getTable(classProp.tableName);

                QueryBuilder<KeyField, PropertyField> copyObjects = new QueryBuilder<>(table);
                Expr keyExpr = copyObjects.getMapExprs().singleValue();
                Where moveWhere = Where.FALSE;
                ImMap<String, ImSet<Long>> copyFrom = toCopy.getValue(i);
                CaseExprInterface mExpr = Expr.newCases(true, copyFrom.size());
                MSet<String> mCopyFromTables = SetFact.mSetMax(copyFrom.size());
                for (int j = 0, sizeJ = copyFrom.size(); j < sizeJ; j++) {
                    DBStoredProperty oldClassProp = oldDBStructure.getProperty(copyFrom.getKey(j));
                    Table oldTable = oldDBStructure.getTable(oldClassProp.tableName);
                    mCopyFromTables.add(oldClassProp.tableName);

                    Expr oldExpr = oldTable.join(MapFact.singleton(oldTable.getTableKeys().single(), keyExpr)).getExpr(oldTable.findProperty(oldClassProp.getDBName()));
                    Where moveExprWhere = Where.FALSE;
                    for (long prevID : copyFrom.getValue(j))
                        moveExprWhere = moveExprWhere.or(oldExpr.compare(new DataObject(prevID, LM.baseClass.objectClass), Compare.EQUALS));
                    mExpr.add(moveExprWhere, oldExpr);
                    moveWhere = moveWhere.or(moveExprWhere);
                }
                copyObjects.addProperty(table.findProperty(classProp.getDBName()), mExpr.getFinal());
                copyObjects.and(moveWhere);

                startLogger.info(localize(LocalizedString.createFormatted("{logics.info.objects.are.transferred.from.tables.to.table}", classProp.tableName, mCopyFromTables.immutable().toString())));
                sql.modifyRecords(new ModifyQuery(table, copyObjects.getQuery(), OperationOwner.unknown, TableOwner.global));
            }
            ImMap<String, ImSet<Long>> toClean = MapFact.mergeMaps(toCopy.values(), ASet.<String, Long>addMergeSet());
            for (int i = 0, size = toClean.size(); i < size; i++) { // удалим оставшиеся классы
                DBStoredProperty classProp = oldDBStructure.getProperty(toClean.getKey(i));
                NamedTable table = oldDBStructure.getTable(classProp.tableName);

                QueryBuilder<KeyField, PropertyField> dropClassObjects = new QueryBuilder<>(table);
                Where moveWhere = Where.FALSE;

                PropertyField oldField = table.findProperty(classProp.getDBName());
                Expr oldExpr = table.join(dropClassObjects.getMapExprs()).getExpr(oldField);
                for (long prevID : toClean.getValue(i))
                    moveWhere = moveWhere.or(oldExpr.compare(new DataObject(prevID, LM.baseClass.objectClass), Compare.EQUALS));
                dropClassObjects.addProperty(oldField, Expr.NULL);
                dropClassObjects.and(moveWhere);

                startLogger.info(localize(LocalizedString.createFormatted("{logics.info.objects.are.removed.from.table}", classProp.tableName)));
                sql.updateRecords(new ModifyQuery(table, dropClassObjects.getQuery(), OperationOwner.unknown, TableOwner.global));
            }

            MSet<ImplementTable> mPackTables = SetFact.mSet();
            for (Pair<String, String> dropColumn : mDropColumns.immutable()) {
                startLogger.info("Dropping column " + dropColumn.second + " from table " + dropColumn.first);
                sql.dropColumn(dropColumn.first, dropColumn.second);
                ImplementTable table = (ImplementTable) newDBStructure.getTable(dropColumn.first);
                if (table != null) mPackTables.add(table);
            }

            // удаляем таблицы старые
            for (NamedTable table : oldDBStructure.tables.keySet()) {
                if (newDBStructure.getTable(table.getName()) == null) {
                    sql.dropTable(table);
                    startLogger.info("Table " + table + " has been dropped");
                }
            }

            startLogger.info("Packing tables");
            packTables(sql, mPackTables.immutable(), false); // упакуем таблицы

            // создадим индексы в базе
            startLogger.info("Adding indices");
            for (Map.Entry<NamedTable, Map<List<Field>, Boolean>> mapIndex : newDBStructure.tables.entrySet())
                for (Map.Entry<List<Field>, Boolean> index : mapIndex.getValue().entrySet()) {
                    NamedTable table = mapIndex.getKey();
                    sql.addIndex(table, table.keys, SetFact.fromJavaOrderSet(index.getKey()), index.getValue(), oldDBStructure.getTable(table.getName()) == null ? null : startLogger); // если таблица новая нет смысла логировать
                }

            startLogger.info("Filling static objects ids");
            if(!fillIDs(getChangesAfter(oldDBStructure.dbVersion, classSIDChanges), getChangesAfter(oldDBStructure.dbVersion, objectSIDChanges), oldDBStructure.version <= 25))
                throw new RuntimeException("Error while filling static objects ids");

            if (oldDBStructure.version < NavElementDBVersion && !oldDBStructure.isEmpty()) {
                modifyNavigatorElementsClasses(sql);
            }
            if (oldDBStructure.version < 29 && !oldDBStructure.isEmpty()) {
                modifyNavigatorFormClasses();
            }
            
            if (oldDBStructure.isEmpty()) {
                startLogger.info("Recalculate class stats");
                try(DataSession session = createSession(OperationOwner.unknown)) {
                    businessLogics.recalculateClassStats(session, false);
                    session.apply(businessLogics, getStack());
                }
            }

            startLogger.info("Updating stats");
            ImMap<String, Integer> tableStats = businessLogics.updateStats(sql, false);  // пересчитаем статистику

            for (DBConcreteClass newClass : newDBStructure.concreteClasses) {
                newClass.ID = newClass.customClass.ID;
            }

            startLogger.info("Migrating reflection properties and actions");
            if(!migrateReflectionProperties(oldDBStructure))
                throw new RuntimeException("Error while migrating reflection properties and actions");
            newDBStructure.writeConcreteClasses(outDB);

            try {
                sql.insertRecord(StructTable.instance, MapFact.<KeyField, DataObject>EMPTY(), MapFact.singleton(StructTable.instance.struct, (ObjectValue) new DataObject(outDBStruct.toByteArray(), ByteArrayClass.instance)), true, TableOwner.global, OperationOwner.unknown);
            } catch (Exception e) {
                ImMap<PropertyField, ObjectValue> propFields = MapFact.singleton(StructTable.instance.struct, (ObjectValue) new DataObject(new byte[0], ByteArrayClass.instance));
                sql.insertRecord(StructTable.instance, MapFact.<KeyField, DataObject>EMPTY(), propFields, true, TableOwner.global, OperationOwner.unknown);
            }

            startLogger.info("Recalculating aggregations");
            recalculateAggregations(getStack(), sql, recalculateProperties, false, startLogger); // перерасчитаем агрегации
            recalculateProperties.addAll(recalculateStatProperties);
            if(newDBStructure.version >= 28 && oldDBStructure.version < 28 && !oldDBStructure.isEmpty()) { // temporary for migration
                recalculateProperties = SetFact.fromJavaOrderSet(recalculateProperties).filterOrder(new SFunctionSet<CalcProperty>() {
                    public boolean contains(CalcProperty element) {
                        return !element.toString().contains("SystemEvents.Session");
                    }
                }).toJavaList();
            }
            updateAggregationStats(recalculateProperties, tableStats);
            
            if(oldDBStructure.version < NavElementDBVersion)
                DataSession.recalculateTableClasses(reflectionLM.navigatorElementTable, sql, LM.baseClass);

            if(newDBStructure.version >= 28 && oldDBStructure.version < 28 && !oldDBStructure.isEmpty()) {
                startLogger.info("Migrating properties to actions data started");
                if (!synchronizePropertyEntities(sql))
                    throw new RuntimeException("Error while migrating properties to actions");
                startLogger.info("Migrating properties to actions data ended");
            }

            if(!noTransSyncDB)
                sql.commitTransaction();

        } catch (Throwable e) {
            if(!noTransSyncDB)
                sql.rollbackTransaction();
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        } finally {
            if(noTransSyncDB)
                sql.endFakeTransaction(OperationOwner.unknown);

            sql.popNoHandled();
        }

        setDefaultUserLocalePreferences();
        
        try (DataSession session = createSession()) {
            for (String sid : columnsToDrop.keySet()) {
                DataObject object = session.addObject(reflectionLM.dropColumn);
                reflectionLM.sidDropColumn.change(sid, session, object);
                reflectionLM.sidTableDropColumn.change(columnsToDrop.get(sid), session, object);
                reflectionLM.timeDropColumn.change(new Timestamp(Calendar.getInstance().getTimeInMillis()), session, object);
                reflectionLM.revisionDropColumn.change(getRevision(), session, object);
            }
            session.apply(businessLogics, getStack());

            initSystemUser();

            String oldHashModules = (String) businessLogics.LM.findProperty("hashModules[]").read(session);
            hashModules = calculateHashModules();
            return checkHashModulesChanged(oldHashModules, hashModules);
        }
    }

    // temporary for migration
    public boolean synchronizePropertyEntities(SQLSession sql) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        if(!synchronizePropertyEntities(true, sql))
            return false;
        
        try {
            recalculateAndUpdateClassStat((CustomClass) reflectionLM.findClass("Action"));
            recalculateAndUpdateStat(reflectionLM.findTable("action"), null);

            try(DataSession session = createSession(sql)) {
                businessLogics.schedulerLM.copyAction.execute(session, getStack());
                businessLogics.securityLM.copyAccess.execute(session, getStack());
                if(!session.apply(businessLogics, getStack()))
                    return false;
                
                for(String tableName : new String[]{"action", "userRoleActionOrProperty", "userRoleAction", "userActionOrProperty", "userAction"}) {
                    recalculateAndUpdateStat((tableName.equals("action")?reflectionLM:businessLogics.securityLM).findTable(tableName), null);
                }
                
                ALRUMap.forceRemoveAllLRU(1.0); // чтобы synchronizeActionEntities отработал
            }
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
        return true;
    }
    private boolean needsToBeSynchronized(Property property) {
        return property.isNamed() && (property instanceof ActionProperty || !((CalcProperty)property).isEmpty(AlgType.syncType));
    }
    public boolean synchronizePropertyEntities(boolean actions, SQLSession sql) {

        startLogger.info("synchronize" + (actions ? "Action" : "Property") + "Entities collecting data started");
        ImportField canonicalNamePropertyField = new ImportField(reflectionLM.propertyCanonicalNameValueClass);
        ImportField dbNamePropertyField = new ImportField(reflectionLM.propertySIDValueClass);
        ImportField captionPropertyField = new ImportField(reflectionLM.propertyCaptionValueClass);
        ImportField loggablePropertyField = new ImportField(reflectionLM.propertyLoggableValueClass);
        ImportField storedPropertyField = new ImportField(reflectionLM.propertyStoredValueClass);
        ImportField isSetNotNullPropertyField = new ImportField(reflectionLM.propertyIsSetNotNullValueClass);
        ImportField returnPropertyField = new ImportField(reflectionLM.propertyClassValueClass);
        ImportField classPropertyField = new ImportField(reflectionLM.propertyClassValueClass);
        ImportField complexityPropertyField = new ImportField(LongClass.instance);
        ImportField tableSIDPropertyField = new ImportField(reflectionLM.propertyTableValueClass);
        ImportField annotationPropertyField = new ImportField(reflectionLM.propertyTableValueClass);
        ImportField statsPropertyField = new ImportField(ValueExpr.COUNTCLASS);

        ConcreteCustomClass customClass = actions ? reflectionLM.action : reflectionLM.property;
        LCP objectByName = actions ? reflectionLM.actionCanonicalName : reflectionLM.propertyCanonicalName;
        LCP nameByObject = actions ? reflectionLM.canonicalNameAction : reflectionLM.canonicalNameProperty;
        ImportKey<?> keyProperty = new ImportKey(customClass, objectByName.getMapping(canonicalNamePropertyField));

        try {
            List<List<Object>> dataProperty = new ArrayList<>();
            for (Property property : businessLogics.getOrderProperties()) {
                if (needsToBeSynchronized(property)) {
                    if((property instanceof ActionProperty) != actions)
                        continue;

                    String returnClass = "";
                    String classProperty = "";
                    String tableSID = "";
                    Long complexityProperty = null;

                    try {
                        classProperty = property.getClass().getSimpleName();

                        if(property instanceof CalcProperty) {
                            CalcProperty calcProperty = (CalcProperty)property;
                            complexityProperty = calcProperty.getComplexity();
                            if (calcProperty.mapTable != null) {
                                tableSID = calcProperty.mapTable.table.getName();
                            } else {
                                tableSID = "";
                            }
                        }

                        returnClass = property.getValueClass(ClassType.syncPolicy).getSID();
                    } catch (NullPointerException | ArrayIndexOutOfBoundsException ignored) {
                    }

                    dataProperty.add(asList(((ActionProperty)property).getCanonicalName(),(Object) property.getDBName(), property.caption.getSourceString(), property instanceof CalcProperty && ((CalcProperty)property).isLoggable() ? true : null,
                            property instanceof CalcProperty && ((CalcProperty) property).isStored() ? true : null,
                            property instanceof CalcProperty && ((CalcProperty) property).reflectionNotNull ? true : null,
                            returnClass, classProperty, complexityProperty, tableSID, property.annotation, (Settings.get().isDisableSyncStatProps() ? (Integer) Stat.DEFAULT.getCount() : businessLogics.getStatsProperty(property))));
                }
            }

            startLogger.info("synchronize" + (actions ? "Action" : "Property") + "Entities integration service started");
            List<ImportProperty<?>> properties = new ArrayList<>();
            properties.add(new ImportProperty(canonicalNamePropertyField, nameByObject.getMapping(keyProperty)));
            properties.add(new ImportProperty(dbNamePropertyField, reflectionLM.dbNameProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(captionPropertyField, reflectionLM.captionProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(loggablePropertyField, reflectionLM.loggableProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(storedPropertyField, reflectionLM.storedProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(isSetNotNullPropertyField, reflectionLM.isSetNotNullProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(returnPropertyField, reflectionLM.returnProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(classPropertyField, reflectionLM.classProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(complexityPropertyField, reflectionLM.complexityProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(tableSIDPropertyField, reflectionLM.tableSIDProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(annotationPropertyField, reflectionLM.annotationProperty.getMapping(keyProperty)));
            properties.add(new ImportProperty(statsPropertyField, reflectionLM.statsProperty.getMapping(keyProperty)));

            List<ImportDelete> deletes = new ArrayList<>();
            deletes.add(new ImportDelete(keyProperty, LM.is(customClass).getMapping(keyProperty), false));

            ImportTable table = new ImportTable(asList(canonicalNamePropertyField, dbNamePropertyField, captionPropertyField, loggablePropertyField,
                    storedPropertyField, isSetNotNullPropertyField, returnPropertyField,
                    classPropertyField, complexityPropertyField, tableSIDPropertyField, annotationPropertyField, statsPropertyField), dataProperty);

            try (DataSession session = createSession(sql)) {
                session.pushVolatileStats("RM_PE");
                IntegrationService service = new IntegrationService(session, table, Collections.singletonList(keyProperty), properties, deletes);
                service.synchronize(true, false);
                session.popVolatileStats();
                boolean result = session.apply(businessLogics, getStack());
                startLogger.info("synchronize" + (actions ? "Action" : "Property") + "Entities finished");
                return result;
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void modifyNavigatorElementsClasses(SQLSession session) {
        try {
            Map<String, Long> oldIds = getOldIds(session);
            for (NavigatorElement ne : businessLogics.getNavigatorElements()) {
                if (!(ne instanceof NavigatorAction)) {
                    CustomClass cls = getNavigatorFolderOrFormClass(ne);
                    String canonicalName = ne.getCanonicalName();
                    if (cls != null) {
                        Long oldId = oldIds.get(canonicalName);
                        modifyNavigatorElementClass(session, canonicalName, cls, oldId);
                    }
                }
            }
        } catch (SQLException | SQLHandledException e) {
            e.printStackTrace();
        }
    }

    private void modifyNavigatorFormClasses() throws SQLException, SQLHandledException {
        try(DataSession session = createSession(OperationOwner.unknown)) {
            KeyExpr keyExpr = new KeyExpr("key");
            session.changeClass(new ClassChange(keyExpr, keyExpr.isClass(businessLogics.reflectionLM.navigatorForm.getUpSet()), businessLogics.reflectionLM.navigatorAction));
            session.apply(businessLogics, getStack());
        }
    }

    private CustomClass getNavigatorFolderOrFormClass(NavigatorElement element) {
        if (element instanceof NavigatorFolder) {
            return businessLogics.findClass("Reflection.NavigatorFolder");
        } else if (element instanceof NavigatorForm) {
            return businessLogics.findClass("Reflection.NavigatorForm");
        }
        return null;
    }

    private void modifyNavigatorElementClass(SQLSession session, String canonicalName, CustomClass cls, Long oldId) throws SQLException, SQLHandledException {
        Long newId = modifyNETable(session, canonicalName, cls);        
        if (!isFolder(cls)) {
            if (newId != null && oldId != null) {
                modifyRoleTable(session, newId, oldId);
            } else {
                System.out.println("Error updating role table, canonicalName: " + canonicalName);
            }
            
        }
    }

    
    private boolean isFolder(CustomClass cls) {
        return cls.getCanonicalName().equals("Reflection.NavigatorFolder");
    }
    
    private Long modifyNETable(SQLSession session, String canonicalName, CustomClass cls) throws SQLException, SQLHandledException {
        SQLSyntax syntax = session.syntax;
        String tableName = syntax.getTableName("reflection_navigatorelement");
        String classFieldName = syntax.getFieldName("system__class_reflection_navigatorelement");
        String cnFieldName = syntax.getFieldName("reflection_canonicalname_navigatorelement");
        String query;

        Long newId = null;
        if (isFolder(cls)) {
            query = "UPDATE " + tableName + " SET " + classFieldName + "=" + cls.ID + " WHERE " + cnFieldName + "='" + canonicalName + "'";
        } else {
            newId = generateID();
            query = "UPDATE " + tableName + " SET " + classFieldName + "=" + cls.ID + ", " + "key0=" + newId + " WHERE " + cnFieldName + "='" + canonicalName + "'";
        }
        session.executeDML(query);
        return newId;
    }
    
    private Map<String, Long> getOldIds(SQLSession session) throws SQLException, SQLHandledException {
        SQLSyntax syntax = session.syntax;
        String tableName = syntax.getTableName("reflection_navigatorelement");
        String cnFieldName = syntax.getFieldName("reflection_canonicalname_navigatorelement");

        KeyField key = new KeyField("key0", LongClass.instance);
        PropertyField field = new PropertyField(cnFieldName, StringClass.getv(100));

        ImRevMap<KeyField, String> keyMap = MapFact.singletonRev(key, "key0");
        ImRevMap<PropertyField, String> propertyMap = MapFact.singletonRev(field, cnFieldName);

        ReadAllResultHandler<KeyField, PropertyField> mapResultHandler = new ReadAllResultHandler<>(); 
        
        String query = "SELECT key0, " + cnFieldName + " FROM " + tableName;
        session.executeSelect(query, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, keyMap, keyMap.reverse().mapValues(Field.<KeyField>fnTypeGetter()), 
                propertyMap, propertyMap.reverse().mapValues(Field.<PropertyField>fnTypeGetter()), mapResultHandler);
        
        ImOrderMap<ImMap<KeyField, Object>, ImMap<PropertyField, Object>> qresult = mapResultHandler.get();
        Map<String, Long> result = new HashMap<>();
        for (int i = 0; i < qresult.size(); ++i) {
            result.put((String)qresult.getValue(i).singleValue(), (Long) qresult.getKey(i).singleValue());
        }
        return result;
    }
    
    private void modifyRoleTable(SQLSession session, long id, long oldId) throws SQLException, SQLHandledException {
        SQLSyntax syntax = session.syntax;
        String roleTableName = syntax.getTableName("security_userrolenavigatorelement");
        String query = "UPDATE " + roleTableName + " SET key1=" + id + " WHERE key1 = " + oldId;
        session.executeDML(query);
    } 
    
    private void checkFormsTable(OldDBStructure dbStruct) {
        if (!dbStruct.isEmpty()) {
            Table table = dbStruct.getTable("Reflection_form");
            if (table == null) {
                throw new RuntimeException("Run 1.3.1 version first");
            }
        }
    }
    
    private void setDefaultUserLocalePreferences() throws SQLException, SQLHandledException {
        try (DataSession session = createSession()) {
            businessLogics.authenticationLM.defaultLanguage.change(defaultUserLanguage, session);
            businessLogics.authenticationLM.defaultCountry.change(defaultUserCountry, session);
            businessLogics.authenticationLM.defaultTimeZone.change(defaultUserTimeZone, session);
            businessLogics.authenticationLM.defaultTwoDigitYearStart.change(defaultTwoDigitYearStart, session);
            session.apply(businessLogics, getStack());
        } 
    }
    
    private void updateAggregationStats(List<CalcProperty> recalculateProperties, ImMap<String, Integer> tableStats) throws SQLException, SQLHandledException {
        Map<ImplementTable, List<CalcProperty>> calcPropertiesMap; // статистика для новых свойств
        if (Settings.get().isGroupByTables()) {
            calcPropertiesMap = new HashMap<>();
            for (CalcProperty property : recalculateProperties) {
                List<CalcProperty> entry = calcPropertiesMap.get(property.mapTable.table);
                if (entry == null)
                    entry = new ArrayList<>();
                entry.add(property);
                calcPropertiesMap.put(property.mapTable.table, entry);
            }
            for(Map.Entry<ImplementTable, List<CalcProperty>> entry : calcPropertiesMap.entrySet())
                recalculateAndUpdateStat(entry.getKey(), entry.getValue());
        }
    }

    public void recalculateAndUpdateClassStat(CustomClass customClass) throws SQLException, SQLHandledException {
        for(ObjectValueClassSet set : customClass.getUpObjectClassFields().valueIt())
            recalculateAndUpdateClassStat(set);        
    }
    public void recalculateAndUpdateClassStat(ObjectValueClassSet tableClasses) throws SQLException, SQLHandledException {
        ImMap<Long, Integer> classStats;
        try (DataSession session = createSession()) {
            classStats = businessLogics.recalculateClassStat(tableClasses, session, true);
        }
        for(ConcreteCustomClass tableClass : tableClasses.getSetConcreteChildren())
            tableClass.updateStat(classStats);
    }
    private void recalculateAndUpdateStat(ImplementTable table, List<CalcProperty> properties) throws SQLException, SQLHandledException {
        ImMap<PropertyField, String> fields = null;
        if(properties != null) {
            fields = SetFact.fromJavaOrderSet(properties).getSet().mapKeyValues(new GetValue<PropertyField, CalcProperty>() {
                public PropertyField getMapValue(CalcProperty value) {
                    return value.field;
                }
            }, new GetValue<String, CalcProperty>() {
                public String getMapValue(CalcProperty value) {
                    return value.getCanonicalName();
                }
            });
        }
        ImplementTable.CalcStat calculateStatResult;
        try (DataSession session = createSession()) {
            long start = System.currentTimeMillis();
            startLogger.info(String.format("Update Aggregation Stats started: %s", table));
            calculateStatResult = table.recalculateStat(reflectionLM, session, fields, false);
            calculateStatResult = table.recalculateStat(reflectionLM, session, fields, true);
            session.apply(businessLogics, getStack());
            long time = System.currentTimeMillis() - start;
            startLogger.info(String.format("Update Aggregation Stats: %s, %sms", table, time));
        }
        table.updateStat(MapFact.singleton(table.getName(), calculateStatResult.rows), calculateStatResult.keys, calculateStatResult.props, fields != null ? fields.keys() : null, false);
    }

    public void writeModulesHash() {
        try {
            startLogger.info("Writing hashModules " + hashModules);
            DataSession session = createSession();
            businessLogics.LM.findProperty("hashModules[]").change(hashModules, session);
            session.apply(businessLogics, getStack());
            startLogger.info("Writing hashModules finished successfully");
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private boolean migrateReflectionProperties(OldDBStructure oldDBStructure) {
        if(!migrateReflectionProperties(oldDBStructure, false))
            return false;
        return migrateReflectionProperties(oldDBStructure, true);
    }
    private boolean migrateReflectionProperties(OldDBStructure oldDBStructure, boolean actions) {
        DBVersion oldDBVersion = oldDBStructure.dbVersion;
        Map<String, String> nameChanges = getChangesAfter(oldDBVersion, actions ? actionCNChanges : propertyCNChanges);
        ImportField oldCanonicalNameField = new ImportField(reflectionLM.propertyCanonicalNameValueClass);
        ImportField newCanonicalNameField = new ImportField(reflectionLM.propertyCanonicalNameValueClass);

        ConcreteCustomClass customClass = actions ? reflectionLM.action : reflectionLM.property;
        LCP objectByName = actions ? reflectionLM.actionCanonicalName : reflectionLM.propertyCanonicalName;
        LCP nameByObject = actions ? reflectionLM.canonicalNameAction : reflectionLM.canonicalNameProperty;
        ImportKey<?> keyProperty = new ImportKey(customClass, objectByName.getMapping(oldCanonicalNameField));

        try {
            List<List<Object>> data = new ArrayList<>();
            for (String oldName : nameChanges.keySet()) {
                data.add(Arrays.<Object>asList(oldName, nameChanges.get(oldName)));
            }

            List<ImportProperty<?>> properties = new ArrayList<>();
            properties.add(new ImportProperty(newCanonicalNameField, nameByObject.getMapping(keyProperty)));

            ImportTable table = new ImportTable(asList(oldCanonicalNameField, newCanonicalNameField), data);

            try (DataSession session = createSession(OperationOwner.unknown)) { // создание сессии аналогично fillIDs
                IntegrationService service = new IntegrationService(session, table, Collections.singletonList(keyProperty), properties);
                service.synchronize(false, false);
                return session.apply(businessLogics, getStack());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private boolean fillIDs(Map<String, String> sIDChanges, Map<String, String> objectSIDChanges, boolean migrateObjectClassID) throws SQLException, SQLHandledException {
        try (DataSession session = createSession(OperationOwner.unknown)) { // по сути вложенная транзакция
            LM.baseClass.fillIDs(session, LM.staticCaption, LM.staticName, sIDChanges, objectSIDChanges, migrateObjectClassID);
            return session.apply(businessLogics, getStack());
        }
    }

    public String checkAggregations(SQLSession session) throws SQLException, SQLHandledException {
        List<CalcProperty> checkProperties = businessLogics.getAggregateStoredProperties(false);
        String message = "";
        for (int i = 0; i < checkProperties.size(); i++) {
            CalcProperty property = checkProperties.get(i);
            if(property instanceof AggregateProperty)
            message += ((AggregateProperty) property).checkAggregation(session, LM.baseClass, new ProgressBar(localize("{logics.info.checking.aggregated.property}"), i, checkProperties.size(), property.getSID()));
        }
        return message;
    }

//    public String checkStats(SQLSession session) throws SQLException, SQLHandledException {
//        ImOrderSet<Property> checkProperties = businessLogics.getPropertyList();
//        
//        double cnt = 0;
//        List<Double> sum = new ArrayList<Double>();
//        List<List<Double>> sumd = new ArrayList<List<Double>>();
//        for(int i=0;i<4;i++) {
//            sum.add(0.0);
//            sumd.add(new ArrayList<Double>());
//        }
//        
//        final Result<Integer> proceeded = new Result<Integer>(0);
//        int total = checkProperties.size();
//        ThreadLocalContext.pushActionMessage("Proceeded : " + proceeded.result + " of " + total);
//        try {
//            String message = "";
//            for (Property property : checkProperties) {
//                if(property instanceof AggregateProperty) {
//                    List<Double> diff = ((AggregateProperty) property).checkStats(session, LM.baseClass);
//                    if(diff != null) {
//                        for(int i=0;i<4;i++) {
//                            sum.set(i, sum.get(i) + diff.get(i));
//                            sumd.get(i).add(diff.get(i));
//                            cnt++;
//                        }
//                    }
//                }
//                
//                if(cnt % 100 == 0) {
//                    for(int i=0;i<4;i++) {
//                        double avg = (double) sum.get(i) / (double) cnt;
//                        double disp = 0;
//                        for (double diff : sumd.get(i)) {
//                            disp += ((double) diff - avg) * ((double) diff - avg);
//                        }
//                        System.out.println("I: " + i + "AVG : " + avg + " DISP : " + (disp) / cnt);
//                    }
//                }
//
//                proceeded.set(proceeded.result + 1);
//                ThreadLocalContext.popActionMessage();
//                ThreadLocalContext.pushActionMessage("Proceeded : " + proceeded.result + " of " + total);
//            }
//            return message;
//        } finally {
//            ThreadLocalContext.popActionMessage();
//        }
//    }
//
    public String checkAggregationTableColumn(SQLSession session, String propertyCanonicalName) throws SQLException, SQLHandledException {
        CalcProperty property = businessLogics.getAggregateStoredProperty(propertyCanonicalName);
        return property != null ? ((AggregateProperty) property).checkAggregation(session, LM.baseClass) : null;
    }

    public String recalculateAggregations(ExecutionStack stack, SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        return recalculateAggregations(stack, session, businessLogics.getAggregateStoredProperties(false), isolatedTransaction, serviceLogger);
    }

    public void ensureLogLevel() {
        getSyntax().setLogLevel(Settings.get().getLogLevelJDBC());
    }

    public interface RunServiceData {
        void run(SessionCreator sql) throws SQLException, SQLHandledException;
    }

    public static void runData(SessionCreator creator, boolean runInTransaction, RunServiceData run) throws SQLException, SQLHandledException {
        if(runInTransaction) {
            ExecutionContext context = (ExecutionContext) creator;
            try(DataSession session = context.createSession()) {
                run.run(session);
                session.apply(context);
            }
        } else
            run.run(creator);
    }

    public interface RunService {
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
                    //serviceLogger.error("Run error: ", t);
                    run(session, true, run, attempts + 1);
                    return;
                }

                throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
            }

        } else
            run.run(session);
    }

    public String recalculateAggregations(ExecutionStack stack, SQLSession session, final List<CalcProperty> recalculateProperties, boolean isolatedTransaction, Logger logger) throws SQLException, SQLHandledException {
        final List<String> messageList = new ArrayList<>();
        final int total = recalculateProperties.size();
        final long maxRecalculateTime = Settings.get().getMaxRecalculateTime();
        if(total > 0) {
            try (DataSession dataSession = createSession()) {
                for (int i = 0; i < recalculateProperties.size(); i++) {
                    CalcProperty property = recalculateProperties.get(i);
                    if(property instanceof AggregateProperty)
                        recalculateAggregation(dataSession, session, isolatedTransaction, new ProgressBar(localize("{logics.recalculation.aggregations}"), i, total), messageList, maxRecalculateTime, (AggregateProperty) property, logger);
                }
                dataSession.apply(businessLogics, stack);
            }
        }
        return businessLogics.formatMessageList(messageList);
    }

    @StackProgress
    private void recalculateAggregation(final DataSession dataSession, SQLSession session, boolean isolatedTransaction, @StackProgress final ProgressBar progressBar, final List<String> messageList, final long maxRecalculateTime, @ParamMessage final AggregateProperty property, final Logger logger) throws SQLException, SQLHandledException {
        run(session, isolatedTransaction, new RunService() {
            public void run(SQLSession sql) throws SQLException, SQLHandledException {
                long start = System.currentTimeMillis();
                logger.info(String.format("Recalculate Aggregation started: %s", property.getSID()));
                property.recalculateAggregation(businessLogics, dataSession, sql, LM.baseClass);

                long time = System.currentTimeMillis() - start;
                String message = String.format("Recalculate Aggregation: %s, %sms", property.getSID(), time);
                logger.info(message);
                if (time > maxRecalculateTime)
                    messageList.add(message);
            }
        });
    }

    public void recalculateTableClasses(SQLSession session, String tableName, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        for (ImplementTable table : businessLogics.LM.tableFactory.getImplementTables())
            if (tableName.equals(table.getName())) {
                runTableClassesRecalculation(session, table, isolatedTransaction);
            }
    }

    public String checkTableClasses(SQLSession session, String tableName, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        for (ImplementTable table : businessLogics.LM.tableFactory.getImplementTables())
            if (tableName.equals(table.getName())) {
                return DataSession.checkTableClasses(table, session, LM.baseClass, true);
            }
        return null;
    }

    private void runTableClassesRecalculation(SQLSession session, final ImplementTable implementTable, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        run(session, isolatedTransaction, new RunService() {
            public void run(SQLSession sql) throws SQLException, SQLHandledException {
                DataSession.recalculateTableClasses(implementTable, sql, LM.baseClass);
            }});
    }


    public void recalculateAggregationTableColumn(DataSession dataSession, SQLSession session, String propertyCanonicalName, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        AggregateProperty property = businessLogics.getAggregateStoredProperty(propertyCanonicalName);
        if (property != null)
            runAggregationRecalculation(dataSession, session, property, isolatedTransaction);
    }

    private void runAggregationRecalculation(final DataSession dataSession, SQLSession session, final AggregateProperty aggregateProperty, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        run(session, isolatedTransaction, new RunService() {
            public void run(SQLSession sql) throws SQLException, SQLHandledException {
                aggregateProperty.recalculateAggregation(businessLogics, dataSession, sql, LM.baseClass);
            }});
    }

    public void recalculateAggregationWithDependenciesTableColumn(SQLSession session, ExecutionStack stack, String propertyCanonicalName, boolean isolatedTransaction, boolean dependents) throws SQLException, SQLHandledException {
        try(DataSession dataSession = createSession()) {
            recalculateAggregationWithDependenciesTableColumn(dataSession, session, businessLogics.findProperty(propertyCanonicalName).property, isolatedTransaction, new HashSet<CalcProperty>(), dependents);
            dataSession.apply(businessLogics, stack);
        }
    }

    private void recalculateAggregationWithDependenciesTableColumn(DataSession dataSession, SQLSession session, CalcProperty<?> property, boolean isolatedTransaction, Set<CalcProperty> calculated, boolean dependents) throws SQLException, SQLHandledException {
        if (!dependents) {
            for (CalcProperty prop : property.getDepends()) {
                if (prop != property && !calculated.contains(prop)) {
                    recalculateAggregationWithDependenciesTableColumn(dataSession, session, prop, isolatedTransaction, calculated, false);
                }
            }
        }
        
        if (property instanceof AggregateProperty && property.isStored()) {
            runAggregationRecalculation(dataSession, session, (AggregateProperty) property, isolatedTransaction);
            calculated.add(property);
        }

        if (dependents) {
            for (CalcProperty prop : businessLogics.getAggregateStoredProperties(true)) {
                if (prop != property && !calculated.contains(prop) && CalcProperty.depends(prop, property)) {
                    boolean recalculate = reflectionLM.notRecalculateTableColumn.read(dataSession, reflectionLM.tableColumnSID.readClasses(dataSession, new DataObject(property.getDBName()))) == null;
                    if(recalculate)
                        recalculateAggregationWithDependenciesTableColumn(dataSession, session, prop, isolatedTransaction, calculated, true);
                }
            }
        }
    }

    public Set<String> getNotRecalculateStatsTableSet() {
        QueryBuilder<String, Object> query = new QueryBuilder<>(SetFact.singleton("key"));
        ImSet<String> notRecalculateStatsTableSet = SetFact.EMPTY();
        try (final DataSession dataSession = createSession()) {
            Expr expr = reflectionLM.notRecalculateStatsSID.getExpr(query.getMapExprs().singleValue());
            query.and(expr.getWhere());
            notRecalculateStatsTableSet = query.execute(dataSession).keys().mapSetValues(new GetValue<String, ImMap<String, Object>>() {
                @Override
                public String getMapValue(ImMap<String, Object> value) {
                    return (String) value.singleValue();
                }
            });

        } catch (SQLException | SQLHandledException e) {
            serviceLogger.info(e.getMessage());
        }
        return notRecalculateStatsTableSet.toJavaSet();
    }

    private void checkModules(OldDBStructure dbStructure) {
        String droppedModules = "";
        for (String moduleName : dbStructure.modulesList)
            if (businessLogics.getSysModule(moduleName) == null) {
                startLogger.info("Module " + moduleName + " has been dropped");
                droppedModules += moduleName + ", ";
            }
        if (denyDropModules && !droppedModules.isEmpty())
            throw new RuntimeException("Dropped modules: " + droppedModules.substring(0, droppedModules.length() - 2));
    }

    private String calculateHashModules() {
        List<Integer> moduleHashCodes = new ArrayList<>();
        for (LogicsModule module : businessLogics.getLogicModules()) {
            if (module instanceof ScriptingLogicsModule) {
                moduleHashCodes.add(((ScriptingLogicsModule) module).getCode().hashCode());
            }
        }
        moduleHashCodes.add((SystemProperties.lightStart ? "light" : "full").hashCode());
        return Integer.toHexString(moduleHashCodes.hashCode());
    }

    private boolean checkHashModulesChanged(String oldHash, String newHash) {
        startLogger.info(String.format("Comparing hashModules: old %s, new %s", oldHash, newHash));
        return (oldHash == null || newHash == null) || !oldHash.equals(newHash);
    }

    private synchronized void runMigrationScript() {
        if (ignoreMigration) {
            //todo: добавить возможность задавать расположение для migration.script, чтобы можно было запускать разные логики из одного модуля
            return;
        }

        if (!migrationScriptWasRead) {
            try {
                InputStream scriptStream = getClass().getResourceAsStream("/migration.script");
                if (scriptStream != null) {
                    ANTLRInputStream stream = new ANTLRInputStream(scriptStream);
                    MigrationScriptLexer lexer = new MigrationScriptLexer(stream);
                    MigrationScriptParser parser = new MigrationScriptParser(new CommonTokenStream(lexer));

                    parser.self = this;

                    parser.script();
                    migrationScriptWasRead = true;
                }
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
    }

    private void renameColumn(SQLSession sql, OldDBStructure oldData, DBStoredProperty oldProperty, String newName) throws SQLException {
        String oldName = oldProperty.getDBName();
        if (!oldName.equals(newName)) {
            startLogger.info("Renaming column from " + oldName + " to " + newName + " in table " + oldProperty.tableName);
            sql.renameColumn(oldProperty.getTableName(getSyntax()), oldName, newName);
            PropertyField field = oldData.getTable(oldProperty.tableName).findProperty(oldName);
            field.setName(newName);
        }
    }
    
    private void renameMigratingProperties(SQLSession sql, OldDBStructure oldData) throws SQLException {
        Map<String, String> propertyChanges = getChangesAfter(oldData.dbVersion, storedPropertyCNChanges);
        for (Map.Entry<String, String> entry : propertyChanges.entrySet()) {
            boolean found = false;
            String newDBName = LM.getDBNamePolicy().transformToDBName(entry.getValue());
            for (DBStoredProperty oldProperty : oldData.storedProperties) {
                if (entry.getKey().equals(oldProperty.getCanonicalName())) {
                    renameColumn(sql, oldData, oldProperty, newDBName);
                    oldProperty.setCanonicalName(entry.getValue());
                    found = true;
                    break;
                }
            }
            if (!found) {
                startLogger.warn("Property " + entry.getKey() + " was not found for renaming to " + entry.getValue());
            }
        }
    }
    
    private void renameMigratingTables(SQLSession sql, OldDBStructure oldData) throws SQLException {
        Map<String, String> tableChanges = getChangesAfter(oldData.dbVersion, tableSIDChanges);
        for (DBStoredProperty oldProperty : oldData.storedProperties) {
            if (tableChanges.containsKey(oldProperty.tableName)) {
                oldProperty.tableName = tableChanges.get(oldProperty.tableName);
            }
        }

        for (NamedTable table : oldData.tables.keySet()) {
            String tableName = table.getName();
            if (tableChanges.containsKey(tableName)) {
                String newSID = tableChanges.get(tableName);
                startLogger.info("Renaming table from " + table + " to " + newSID);
                sql.renameTable(table, newSID);
                table.setName(newSID);
            }
        }
    }
    
    private void renameMigratingClasses(OldDBStructure oldData) {
        Map<String, String> classChanges = getChangesAfter(oldData.dbVersion, classSIDChanges);
        for (DBConcreteClass oldClass : oldData.concreteClasses) {
            if(classChanges.containsKey(oldClass.sID)) {
                oldClass.sID = classChanges.get(oldClass.sID);
            }
        }
    }
    
    private void migrateClassProperties(SQLSession sql, OldDBStructure oldData, NewDBStructure newData) throws SQLException {
        // Изменим в старой структуре классовые свойства. Предполагаем, что в одной таблице может быть только одно классовое свойство. Переименовываем поля в таблицах
        Map<String, String> tableNewClassProps = new HashMap<>();
        for (DBConcreteClass cls : newData.concreteClasses) {
            DBStoredProperty classProp = newData.getProperty(cls.sDataPropID);
            assert classProp != null;
            String tableName = classProp.getTable().getName();
            if (tableNewClassProps.containsKey(tableName)) {
                assert cls.sDataPropID.equals(tableNewClassProps.get(tableName));
            } else {
                tableNewClassProps.put(tableName, cls.sDataPropID);
            }
        }
        
        Map<String, String> nameRenames = new HashMap<>();
        for (DBConcreteClass cls : oldData.concreteClasses) {
            if (!nameRenames.containsKey(cls.sDataPropID)) {
                DBStoredProperty oldClassProp = oldData.getProperty(cls.sDataPropID);
                assert oldClassProp != null;
                String tableName = oldClassProp.tableName;
                if (tableNewClassProps.containsKey(tableName)) {
                    String newName = tableNewClassProps.get(tableName);
                    nameRenames.put(cls.sDataPropID, newName);
                    String newDBName = LM.getDBNamePolicy().transformToDBName(newName);
                    renameColumn(sql, oldData, oldClassProp, newDBName);
                    oldClassProp.setCanonicalName(newName);
                    cls.sDataPropID = newName;
                }
            } else {
                cls.sDataPropID = nameRenames.get(cls.sDataPropID);
            }
        }
    } 
    
    private void migrateDBNames(SQLSession sql, OldDBStructure oldData, NewDBStructure newData) throws SQLException {
        Map<String, DBStoredProperty> newProperties = new HashMap<>();
        for (DBStoredProperty newProperty : newData.storedProperties) {
            newProperties.put(newProperty.getCanonicalName(), newProperty);
        }

        for (DBStoredProperty oldProperty : oldData.storedProperties) {
            DBStoredProperty newProperty;
            if ((newProperty = newProperties.get(oldProperty.getCanonicalName())) != null) {
                if (!newProperty.getDBName().equals(oldProperty.getDBName())) {
                    renameColumn(sql, oldData, oldProperty, newProperty.getDBName());
                    // переустанавливаем каноническое имя, чтобы получить новый dbName
                    oldProperty.setCanonicalName(oldProperty.getCanonicalName());
                }
            }
        }
    }
    
    // Временная реализация для переименования
    // issue #4672 Построение сигнатуры LOG-свойств
    // Проблема текущего способа: 
    // Каноническое имя log-свойств сейчас определяется не из сигнатуры базового свойства, а из getInterfaces, которые могут не совпадать с сигнатурой
    // В дальнейшем. когда сделаем нормальные канонические имена у log-свойств, можно перенести этот функционал в setUserLoggableProperties 
    // и добавлять изменения канонических имен log-свойств напрямую в storedPropertyCNChanges еще на том шаге
    private void addLogPropertiesToMigration(OldDBStructure oldData, DBVersion newDBVersion) {
        Map<String, String> changes = getChangesAfter(oldData.dbVersion, propertyCNChanges);
        Map<String, String> rChanges = BaseUtils.reverse(changes);
        
        Set<String> logProperties = new HashSet<>();
        
        for (LCP<?> lp : businessLogics.getNamedProperties()) {
            if (lp.property.getName().startsWith(PropertyCanonicalNameUtils.logPropPrefix)) {
                logProperties.add(lp.property.getCanonicalName());
            }
        }
        
        for (LCP<?> lp : businessLogics.getNamedProperties()) {
            if (lp.property.isFull(ClassType.useInsteadOfAssert.getCalc().getAlgInfo())) {
                String logPropCN = LogicsModule.getLogPropertyCN(lp, "System", businessLogics.systemEventsLM);
                if (logProperties.contains(logPropCN)) {
                    String propCN = lp.property.getCanonicalName();
                    if (rChanges.containsKey(propCN)) {
                        String oldPropCN = rChanges.get(propCN);
                        PropertyCanonicalNameParser parser = new PropertyCanonicalNameParser(businessLogics, oldPropCN);
                        String oldLogPropCN = LogicsModule.getLogPropertyCN("System", parser.getNamespace(), parser.getName(), 
                                LogicsModule.getSignatureForLogProperty(parser.getSignature(), businessLogics.systemEventsLM));
                        
                        if (!storedPropertyCNChanges.containsKey(newDBVersion)) {
                            storedPropertyCNChanges.put(newDBVersion, new ArrayList<SIDChange>());
                        }
                        storedPropertyCNChanges.get(newDBVersion).add(new SIDChange(oldLogPropCN, logPropCN));
                    }
                }
            }
        }
    }
    
    // Не разбирается с индексами. Было решено, что сохранять индексы необязательно.
    private void alterDBStructure(OldDBStructure oldData, NewDBStructure newData, SQLSession sql) throws SQLException, SQLHandledException {
        // Сохраним изменения имен свойств на форме для reflectionManager
        finalPropertyDrawNameChanges = getChangesAfter(oldData.dbVersion, propertyDrawNameChanges);
        
        // Обязательно до renameMigratingProperties, потому что в storedPropertyCNChanges добавляются изменения для log-свойств 
        addLogPropertiesToMigration(oldData, newData.dbVersion);
        
        // Изменяем в старой структуре свойства из скрипта миграции, переименовываем поля в таблицах
        renameMigratingProperties(sql, oldData);
        
        // Переименовываем таблицы из скрипта миграции, переустанавливаем ссылки на таблицы в свойствах
        renameMigratingTables(sql, oldData);

        // Переустановим имена классовым свойствам, если это необходимо. Также при необходимости переименуем поля в таблицах   
        // Иимена полей могут измениться при переименовании таблиц (так как в именах классовых свойств есть имя таблицы) либо при изменении dbNamePolicy
        migrateClassProperties(sql, oldData, newData);        
        
        // При изменении dbNamePolicy необходимо также переименовать поля
        migrateDBNames(sql, oldData, newData);
        
        // переименовываем классы из скрипта миграции
        renameMigratingClasses(oldData);
    }

    private DBVersion getCurrentDBVersion(DBVersion oldVersion) {
        DBVersion curVersion = oldVersion;
        if (!propertyCNChanges.isEmpty() && curVersion.compare(propertyCNChanges.lastKey()) < 0) {
            curVersion = propertyCNChanges.lastKey();
        }
        if (!actionCNChanges.isEmpty() && curVersion.compare(actionCNChanges.lastKey()) < 0) {
            curVersion = actionCNChanges.lastKey();
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
        if (!propertyDrawNameChanges.isEmpty() && curVersion.compare(propertyDrawNameChanges.lastKey()) < 0) {
            curVersion = propertyDrawNameChanges.lastKey();
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

    public void addPropertyCNChange(String version, String oldName, String oldSignature, String newName, String newSignature, boolean stored) {
        if (newSignature == null) {
            newSignature = oldSignature;
        } 
        addSIDChange(propertyCNChanges, version, oldName + oldSignature, newName + newSignature);
        if (stored) {
            addSIDChange(storedPropertyCNChanges, version, oldName + oldSignature, newName + newSignature);
        }
    }   
    public void addActionCNChange(String version, String oldName, String oldSignature, String newName, String newSignature) {
        if (newSignature == null) {
            newSignature = oldSignature;
        } 
        addSIDChange(actionCNChanges, version, oldName + oldSignature, newName + newSignature);
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
    
    public void addPropertyDrawSIDChange(String version, String oldName, String newName) {
        addSIDChange(propertyDrawNameChanges, version, oldName, newName);
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

    public Map<String, String> getPropertyDrawNamesChanges() {
        return finalPropertyDrawNameChanges;
    } 
    
    private Map<String, String> getChangesAfter(DBVersion versionAfter, TreeMap<DBVersion, List<SIDChange>> allChanges) {
        Map<String, String> resultChanges = new OrderedMap<>();

        for (Map.Entry<DBVersion, List<SIDChange>> changesEntry : allChanges.entrySet()) {
            if (changesEntry.getKey().compare(versionAfter) > 0) {
                List<SIDChange> versionChanges = changesEntry.getValue();
                Map<String, String> versionChangesMap = new OrderedMap<>();

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
                Set<String> renameToSIDs = new HashSet<>();
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
    public <Z extends PropertyInterface> void addIndex(ImList<CalcPropertyObjectInterfaceImplement<String>> index) {
        CalcPropertyRevImplement<Z, String> propertyImplement = (CalcPropertyRevImplement<Z, String>) findProperty(index);
        if(propertyImplement != null) {
            indexes.put(index, propertyImplement.property.getType() instanceof DataClass);
            propertyImplement.property.markIndexed(propertyImplement.mapping, index);
        }
    }

    private static CalcPropertyRevImplement<?, String> findProperty(ImList<CalcPropertyObjectInterfaceImplement<String>> index) {
        for (CalcPropertyObjectInterfaceImplement<String> lp : index) {
            if(lp instanceof CalcPropertyRevImplement) {
                return (CalcPropertyRevImplement<?, String>) lp;
            }
        }
        return null;
    }

    public String getBackupFilePath(String dumpFileName) throws IOException, InterruptedException {
        return adapter.getBackupFilePath(dumpFileName);
    }

    public String backupDB(ExecutionContext context, String dumpFileName, List<String> excludeTables) throws IOException, InterruptedException {
        return adapter.backupDB(context, dumpFileName, excludeTables);
    }

    public String customRestoreDB(String fileBackup, Set<String> tables) throws IOException {
        return adapter.customRestoreDB(fileBackup, tables);
    }

    public void dropDB(String dbName) throws IOException {
        adapter.dropDB(dbName);
    }

    public List<List<List<Object>>> readCustomRestoredColumns(String dbName, String table, List<String> keys, List<String> columns) throws SQLException {
        return adapter.readCustomRestoredColumns(dbName, table, keys, columns);
    }

    public void analyzeDB(SQLSession session) throws SQLException {
        session.executeDDL(getSyntax().getAnalyze());
    }

    public void vacuumDB(SQLSession session) throws SQLException {
        session.executeDDL(getSyntax().getVacuumDB());
    }

    public void packTables(SQLSession session, ImCol<ImplementTable> tables, boolean isolatedTransaction) throws SQLException, SQLHandledException {
        for (final ImplementTable table : tables) {
            logger.debug(localize("{logics.info.packing.table}") + " (" + table + ")... ");
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
    public static int SESSION_TIL = Connection.TRANSACTION_REPEATABLE_READ;
    public static int ID_TIL = Connection.TRANSACTION_REPEATABLE_READ;
    
    private static Stack<Integer> STACK_TIL = new Stack<>();
    
    public static void pushTIL(Integer TIL) {
        STACK_TIL.push(TIL);
    }
    
    public static Integer popTIL() {
        return STACK_TIL.isEmpty() ? null : STACK_TIL.pop();
    }
    
    public static Integer getCurrentTIL() {
        return STACK_TIL.isEmpty() ? SESSION_TIL : STACK_TIL.peek();
    }
    
    public static String HOSTNAME_COMPUTER;

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

    // может вызываться до инициализации DBManager
    private DBVersion getOldDBVersion(SQLSession sql) throws IOException, SQLException, SQLHandledException {
        DBVersion dbVersion = new DBVersion("0.0");
        
        DataInputStream inputDB;
        StructTable structTable = StructTable.instance;
        byte[] struct = (byte[]) sql.readRecord(structTable, MapFact.<KeyField, DataObject>EMPTY(), structTable.struct, OperationOwner.unknown);
        if (struct != null) {
            inputDB = new DataInputStream(new ByteArrayInputStream(struct));
            //noinspection ResultOfMethodCallIgnored
            inputDB.read();
            dbVersion = new DBVersion(inputDB.readUTF());
        }
        return dbVersion;
    }

    public Map<String, String> getPropertyCNChanges(SQLSession sql) {
        runMigrationScript();
        try {
            return getChangesAfter(getOldDBVersion(sql), propertyCNChanges);
        } catch (IOException | SQLException | SQLHandledException e) {
            Throwables.propagate(e);
        }
        return new HashMap<>();
    }
    
    private void initSystemUser() {
        // считаем системного пользователя
        try {
            try (DataSession session = createSession()) {

                QueryBuilder<String, Object> query = new QueryBuilder<>(SetFact.singleton("key"));
                query.and(query.getMapExprs().singleValue().isClass(businessLogics.authenticationLM.systemUser));
                ImOrderSet<ImMap<String, Object>> rows = query.execute(session, MapFact.<Object, Boolean>EMPTYORDER(), 1).keyOrderSet();
                if (rows.size() == 0) { // если нету добавим
                    systemUserObject = (Long) session.addObject(businessLogics.authenticationLM.systemUser).object;
                    session.apply(businessLogics, getStack());
                } else
                    systemUserObject = (Long) rows.single().get("key");

                query = new QueryBuilder<>(SetFact.singleton("key"));
                query.and(businessLogics.authenticationLM.hostnameComputer.getExpr(session.getModifier(), query.getMapExprs().singleValue()).compare(new DataObject("systemhost"), Compare.EQUALS));
                rows = query.execute(session, MapFact.<Object, Boolean>EMPTYORDER(), 1).keyOrderSet();
                if (rows.size() == 0) { // если нету добавим
                    DataObject computerObject = session.addObject(businessLogics.authenticationLM.computer);
                    systemComputer = (Long) computerObject.object;
                    businessLogics.authenticationLM.hostnameComputer.change("systemhost", session, computerObject);
                    session.apply(businessLogics, getStack());
                } else
                    systemComputer = (Long) rows.single().get("key");

            }
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
            this.setCanonicalName(canonicalName, dbName);
            this.isDataProperty = isDataProperty;
            this.tableName = tableName;
            this.mapKeys = mapKeys;
        }

        public String getDBName() {
            return dbName;
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

        public void setCanonicalName(String canonicalName, String dbName) {
            this.canonicalName = canonicalName;
            this.dbName = dbName;
        }
    }

    private class DBConcreteClass {
        public String sID;
        public String sDataPropID; // в каком ClassDataProperty хранился

        @Override
        public String toString() {
            return sID + ' ' + sDataPropID;
        }

        public Long ID = null; // только для старых
        public ConcreteCustomClass customClass = null; // только для новых

        private DBConcreteClass(String sID, String sDataPropID, Long ID) {
            this.sID = sID;
            this.sDataPropID = sDataPropID;
            this.ID = ID;
        }

        private DBConcreteClass(ConcreteCustomClass customClass) {
            sID = customClass.getSID();
            sDataPropID = customClass.dataProperty.getCanonicalName();

            this.customClass = customClass;
        }
    }

    private abstract class DBStructure<F> {
        public int version;
        public DBVersion dbVersion;
        public List<String> modulesList = new ArrayList<>();
        public Map<NamedTable, Map<List<F>, Boolean>> tables = new HashMap<>();
        public List<DBStoredProperty> storedProperties = new ArrayList<>();
        public Set<DBConcreteClass> concreteClasses = new HashSet<>();

        public void writeConcreteClasses(DataOutputStream outDB) throws IOException { // отдельно от write, так как ID заполняются после fillIDs
            outDB.writeInt(concreteClasses.size());
            for (DBConcreteClass concreteClass : concreteClasses) {
                outDB.writeUTF(concreteClass.sID);
                outDB.writeUTF(concreteClass.sDataPropID);
                outDB.writeLong(concreteClass.ID);
            }
        }

        public NamedTable getTable(String name) {
            for (NamedTable table : tables.keySet()) {
                if (table.getName().equals(name)) {
                    return table;
                }
            }
            return null;
        }

        public DBStoredProperty getProperty(String canonicalName) {
            for (DBStoredProperty prop : storedProperties) {
                if (prop.getCanonicalName().equals(canonicalName)) {
                    return prop;
                }
            }
            return null;
        }
    }

    public <P extends PropertyInterface> Map<NamedTable, Map<List<Field>, Boolean>> getIndicesMap() {
        Map<NamedTable, Map<List<Field>, Boolean>> res = new HashMap<>();
        for (ImplementTable table : LM.tableFactory.getImplementTablesMap().valueIt()) {
            res.put(table, new HashMap<List<Field>, Boolean>());
        }

        for (Map.Entry<ImList<CalcPropertyObjectInterfaceImplement<String>>, Boolean> index : indexes.entrySet()) {
            ImList<CalcPropertyObjectInterfaceImplement<String>> indexFields = index.getKey();

            if (indexFields.isEmpty()) {
                throw new RuntimeException(localize("{logics.policy.forbidden.to.create.empty.indexes}"));
            }

            CalcPropertyRevImplement<P, String> basePropertyImplement = (CalcPropertyRevImplement<P, String>) findProperty(indexFields);
            assert basePropertyImplement != null; // исходя из логики addIndex

            CalcProperty<P> baseProperty = basePropertyImplement.property;

            if (!baseProperty.isStored())
                throw new RuntimeException(localize("{logics.policy.forbidden.to.create.indexes.on.non.regular.properties}") + " (" + baseProperty + ")");

            ImplementTable baseIndexTable = baseProperty.mapTable.table;
            ImRevMap<String, KeyField> baseMapKeys = basePropertyImplement.mapping.crossJoin(baseProperty.mapTable.mapKeys);

            List<Field> tableIndex = new ArrayList<>();

            for (CalcPropertyObjectInterfaceImplement<String> indexField : indexFields) {
                Field field;
                if(indexField instanceof CalcPropertyRevImplement) {
                    CalcPropertyRevImplement<P, String> propertyImplement = (CalcPropertyRevImplement<P, String>)indexField;
                    CalcProperty<P> property = propertyImplement.property;

                    if (!property.isStored())
                        throw new RuntimeException(localize("{logics.policy.forbidden.to.create.indexes.on.non.regular.properties}") + " (" + property + ")");

                    ImplementTable indexTable = property.mapTable.table;
                    ImRevMap<String, KeyField> mapKeys = propertyImplement.mapping.crossJoin(property.mapTable.mapKeys);

                    if (!BaseUtils.hashEquals(baseIndexTable, indexTable))
                        throw new RuntimeException(localize(LocalizedString.createFormatted("{logics.policy.forbidden.to.create.indexes.on.properties.in.different.tables}", baseProperty, property)));
                    if (!BaseUtils.hashEquals(baseMapKeys, mapKeys))
                        throw new RuntimeException(localize(LocalizedString.createFormatted("{logics.policy.forbidden.to.create.indexes.on.properties.with.different.mappings}", baseProperty, property, baseMapKeys, mapKeys)));
                    field = property.field;
                } else {
                    field = baseMapKeys.get(((CalcPropertyObjectImplement<String>)indexField).object);
                }
                tableIndex.add(field);
            }
            res.get(baseIndexTable).put(tableIndex, index.getValue());
        }
        return res;
    }

    public static final int NavElementDBVersion = 27;
    
    private class NewDBStructure extends DBStructure<Field> {
        
        public <P extends PropertyInterface> NewDBStructure(DBVersion dbVersion) {
            version = 29;
            this.dbVersion = dbVersion;

            tables.putAll(getIndicesMap());

            for (CalcProperty<?> property : businessLogics.getStoredProperties()) {
                storedProperties.add(new DBStoredProperty(property));
                assert property.isNamed();
            }

            for (ConcreteCustomClass customClass : businessLogics.getConcreteCustomClasses()) {
                concreteClasses.add(new DBConcreteClass(customClass));
            }
        }

        public void write(DataOutputStream outDB) throws IOException {
            outDB.write('v' + version);  //для поддержки обратной совместимости
            outDB.writeUTF(dbVersion.toString());

            //записываем список подключенных модулей
            outDB.writeInt(businessLogics.getLogicModules().size());
            for (LogicsModule logicsModule : businessLogics.getLogicModules())
                outDB.writeUTF(logicsModule.getName());

            outDB.writeInt(tables.size());
            for (Map.Entry<NamedTable, Map<List<Field>, Boolean>> tableIndices : tables.entrySet()) {
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
                outDB.writeUTF(property.getDBName());
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

        public OldDBStructure(DataInputStream inputDB) throws IOException {
            dbVersion = new DBVersion("0.0");
            if (inputDB == null) {
                version = -2;
            } else {
                version = inputDB.read() - 'v';
                dbVersion = new DBVersion(inputDB.readUTF());

                int modulesCount = inputDB.readInt();
                if (modulesCount > 0) {
                    for (int i = 0; i < modulesCount; i++)
                        modulesList.add(inputDB.readUTF());
                }

                for (int i = inputDB.readInt(); i > 0; i--) {
                    SerializedTable prevTable = new SerializedTable(inputDB, LM.baseClass);
                    Map<List<String>, Boolean> indices = new HashMap<>();
                    for (int j = inputDB.readInt(); j > 0; j--) {
                        List<String> index = new ArrayList<>();
                        for (int k = inputDB.readInt(); k > 0; k--) {
                            index.add(inputDB.readUTF());
                        }
                        boolean prevOrdered = inputDB.readBoolean();
                        indices.put(index, prevOrdered);
                    }
                    tables.put(prevTable, indices);
                }

                int prevStoredNum = inputDB.readInt();
                for (int i = 0; i < prevStoredNum; i++) {
                    String canonicalName = inputDB.readUTF();
                    String sID = inputDB.readUTF();
                    boolean isDataProperty = inputDB.readBoolean();
                    
                    String tableName = inputDB.readUTF();
                    Table prevTable = getTable(tableName);
                    MExclMap<Integer, KeyField> mMapKeys = MapFact.mExclMap(prevTable.getTableKeys().size());
                    for (int j = 0; j < prevTable.getTableKeys().size(); j++) {
                        mMapKeys.exclAdd(inputDB.readInt(), prevTable.findKey(inputDB.readUTF()));
                    }
                    storedProperties.add(new DBStoredProperty(canonicalName, sID, isDataProperty, tableName, mMapKeys.immutable()));
                }

                int prevConcreteNum = inputDB.readInt();
                for(int i = 0; i < prevConcreteNum; i++)
                    concreteClasses.add(new DBConcreteClass(inputDB.readUTF(), inputDB.readUTF(), version <= 24 ? inputDB.readInt() : inputDB.readLong()));
            }
        }
        
        boolean isEmpty() {
            return version < 0;
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
            String[] splitArr = version.split("\\.");
            List<Integer> intVersion = new ArrayList<>();
            for (String part : splitArr) {
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

    public void setDefaultUserLanguage(String defaultUserLanguage) {
        this.defaultUserLanguage = defaultUserLanguage;
    }

    public void setDefaultUserCountry(String defaultUserCountry) {
        this.defaultUserCountry = defaultUserCountry;
    }

    public void setDefaultUserTimeZone(String defaultUserTimeZone) {
        this.defaultUserTimeZone = defaultUserTimeZone;
    }

    public void setDefaultTwoDigitYearStart(Integer defaultTwoDigitYearStart) {
        this.defaultTwoDigitYearStart = defaultTwoDigitYearStart;
    }
}
