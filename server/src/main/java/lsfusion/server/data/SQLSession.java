package lsfusion.server.data;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;
import lsfusion.base.col.lru.LRUCache;
import lsfusion.base.col.lru.MCacheMap;
import lsfusion.server.Message;
import lsfusion.server.ParamMessage;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.DataAdapter;
import lsfusion.server.data.sql.SQLExecute;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.*;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;

import java.lang.ref.WeakReference;
import java.sql.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.server.ServerLoggers.sqlLogger;
import static lsfusion.server.ServerLoggers.systemLogger;

public class SQLSession extends MutableObject {
    private static final Logger logger = ServerLoggers.sqlLogger;

    public SQLSyntax syntax;
    
    public <F extends Field> GetValue<String, F> getDeclare(final TypeEnvironment typeEnv) {
        return new GetValue<String, F>() {
            public String getMapValue(F value) {
                return value.getDeclare(syntax, typeEnv);
            }};
    }

    private final ConnectionPool connectionPool;
    public final TypePool typePool;

    public Connection getConnection() throws SQLException {
        return privateConnection !=null ? privateConnection : connectionPool.getCommon(this);
    }

    private void returnConnection(Connection connection) throws SQLException {
        if(privateConnection !=null)
            assert privateConnection == connection;
        else
            connectionPool.returnCommon(this, connection);
    }

    private Connection privateConnection = null;

    public final static String userParam = "adsadaweewuser";
    public final static String isServerRestartingParam = "sdfisserverrestartingpdfdf";
    public final static String computerParam = "fjruwidskldsor";
    public final static String isDebugParam = "dsiljdsiowee";
    public final static String isFullClientParam = "fdfdijir";

    public SQLSession(DataAdapter adapter) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        this(adapter, -1);
    }

    private final int isolationLevel;
    public SQLSession(DataAdapter adapter, int isolationLevel) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        syntax = adapter;
        connectionPool = adapter;
        typePool = adapter;

        this.isolationLevel = isolationLevel;
    }

    private void needPrivate() throws SQLException { // получает unique connection
        if(privateConnection ==null)
            privateConnection = connectionPool.getPrivate(this);
    }

    private void tryCommon() throws SQLException { // пытается вернуться к
        removeUnusedTemporaryTables();
        if(inTransaction == 0 && getSQLTemporaryPool().isEmpty(sessionTablesMap)) { // вернемся к commonConnection'у
            connectionPool.returnPrivate(this, privateConnection);
            privateConnection = null;
        }
    }

    private int inTransaction = 0; // счетчик для по сути распределенных транзакций

    public boolean isInTransaction() {
        return inTransaction > 0;
    }

    public static void setACID(Connection connection, boolean ACID) throws SQLException {
        connection.setAutoCommit(!ACID);
        connection.setReadOnly(!ACID);

        Statement statement = createSingleStatement(connection);
        try {
            statement.execute("SET SESSION synchronous_commit TO " + (ACID ? "DEFAULT" : "OFF"));
            statement.execute("SET SESSION commit_delay TO " + (ACID ? "DEFAULT" : "100000"));
        } catch (SQLException e) {
            logger.error(statement.toString());
            throw e;
        } finally {
            statement.close();
        }
    }

    private int prevIsolation;
    public void startTransaction() throws SQLException {
        if(Settings.get().isApplyVolatileStats())
            pushVolatileStats(null);

        needPrivate();

        if(inTransaction++ == 0) {
            if(isolationLevel > 0) {
                prevIsolation = privateConnection.getTransactionIsolation();
                privateConnection.setTransactionIsolation(isolationLevel);
            }
            setACID(privateConnection, true);
        }
    }

    private void endTransaction() throws SQLException {
        assert isInTransaction();
        if(--inTransaction == 0) {
            setACID(privateConnection, false);
            if(isolationLevel > 0)
                privateConnection.setTransactionIsolation(prevIsolation);
        }

        transactionTables.clear();

        tryCommon();

        if(Settings.get().isApplyVolatileStats())
            popVolatileStats(null);
    }

    public void rollbackTransaction() throws SQLException {
        if(inTransaction == 1) // транзакция заканчивается
            for(String transactionTable : transactionTables) {
//                dropTemporaryTableFromDB(transactionTable);

                sessionTablesMap.remove(transactionTable);
                getSQLTemporaryPool().removeTable(transactionTable);
            }

        privateConnection.rollback();
        endTransaction();
    }

    public void checkSessionTableMap(SessionTable table, Object owner) {
        assert sessionTablesMap.get(table.name).get() == owner;
    }

    public void commitTransaction() throws SQLException {
        privateConnection.commit();
        endTransaction();
    }

    // удостоверивается что таблица есть
    public void ensureTable(Table table) throws SQLException {
        Connection connection = getConnection();

        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(null, null, table.name, new String[]{"TABLE"});
        if (!tables.next()) {
            createTable(table.name, table.keys);
            for (PropertyField property : table.properties)
                addColumn(table.name, property);
        }

        returnConnection(connection);
    }

    public void addExtraIndices(String table, ImOrderSet<KeyField> keys) throws SQLException {
        ImOrderSet<String> keyStrings = keys.mapOrderSetValues(Field.<KeyField>nameGetter());
        for(int i=1;i<keys.size();i++)
            addIndex(table, keyStrings.subOrder(i, keys.size()).toOrderMap(true));
    }

    private String getConstraintName(String table) {
        return "PK_" + table;
    }

    private String getConstraintDeclare(String table, ImOrderSet<KeyField> keys) {
        String keyString = keys.toString(Field.<KeyField>nameGetter(), ",");
        return "CONSTRAINT " + getConstraintName(table) + " PRIMARY KEY " + syntax.getClustered() + " (" + keyString + ")";
    }

    public void createTable(String table, ImOrderSet<KeyField> keys) throws SQLException {
        ExecuteEnvironment env = new ExecuteEnvironment();

        if (keys.size() == 0)
            keys = SetFact.singletonOrder(KeyField.dumb);
        String createString = keys.toString(this.<KeyField>getDeclare(env), ",");
        createString = createString + "," + getConstraintDeclare(table, keys);

//        System.out.println("CREATE TABLE "+Table.Name+" ("+CreateString+")");
        executeDDL("CREATE TABLE " + table + " (" + createString + ")", env);
        addExtraIndices(table, keys);
    }

    public void renameTable(String oldTableName, String newTableName) throws SQLException {
        executeDDL("ALTER TABLE " + oldTableName + " RENAME TO " + newTableName);
    }

    public void dropTable(String table) throws SQLException {
        executeDDL("DROP TABLE " + table);
    }

    static String getIndexName(String table, ImOrderMap<String, Boolean> fields) {
        return table + "_" + fields.keys().toString("_") + "_idx";
    }

    private ImOrderMap<String, Boolean> getOrderFields(ImOrderSet<KeyField> keyFields, ImOrderSet<String> fields, boolean order) {
        ImOrderMap<String, Boolean> result = fields.toOrderMap(false);
        if(order)
            result = result.addOrderExcl(keyFields.mapOrderSetValues(Field.<KeyField>nameGetter()).toOrderMap(true));
        return result;
    }
    
    public void addIndex(String table, ImOrderSet<KeyField> keyFields, ImOrderSet<String> fields, boolean order) throws SQLException {
        addIndex(table, getOrderFields(keyFields, fields, order));
    }

    public void addIndex(String table, ImOrderMap<String, Boolean> fields) throws SQLException {
        String columns = fields.toString(new GetKeyValue<String, String, Boolean>() {
            public String getMapValue(String key, Boolean value) {
                return key + " ASC" + (value?"":" NULLS FIRST");
            }}, ",");

        executeDDL("CREATE INDEX " + getIndexName(table, fields) + " ON " + table + " (" + columns + ")");
    }

    public void dropIndex(String table, ImOrderSet<KeyField> keyFields, ImOrderSet<String> fields, boolean order) throws SQLException {
        dropIndex(table, getOrderFields(keyFields, fields, order));
    }
    
    public void dropIndex(String table, ImOrderMap<String, Boolean> fields) throws SQLException {
        executeDDL("DROP INDEX " + getIndexName(table, fields));
    }

/*    public void addKeyColumns(String table, Map<KeyField, Object> fields, List<KeyField> keys) throws SQLException {
        if(fields.isEmpty())
            return;

        logger.info("Идет добавление ключа " + table + "." + fields + "... ");

        String constraintName = getConstraintName(table);
        String tableName = syntax.getSessionTableName(table);
        String addCommand = ""; String dropDefaultCommand = "";
        for(Map.Entry<KeyField, Object> field : fields.entrySet()) {
            addCommand = addCommand + "ADD COLUMN " + field.getKey().getDeclare(syntax) + " DEFAULT " + field.getKey().type.getString(field.getValue(), syntax) + ",";
            dropDefaultCommand = (dropDefaultCommand.length()==0?"":dropDefaultCommand + ",") + " ALTER COLUMN " + field.getKey().name + " DROP DEFAULT";
        }

        executeDerived("ALTER TABLE " + tableName + " " + addCommand + " ADD " + getConstraintDeclare(table, keys) +
                (keys.size()==fields.size()?"":", DROP CONSTRAINT " + constraintName));
        executeDerived("ALTER TABLE " + tableName + " " + dropDefaultCommand);

        logger.info(" Done");
    }

    public void addTemporaryColumn(String table, PropertyField field) throws SQLException {
        addColumn(table, field);
//        executeDerived("CREATE INDEX " + "idx_" + table + "_" + field.name + " ON " + table + " (" + field.name + ")"); //COLUMN
    }*/

    public void addColumn(String table, PropertyField field) throws SQLException {
        ExecuteEnvironment env = new ExecuteEnvironment();
        executeDDL("ALTER TABLE " + table + " ADD " + field.getDeclare(syntax, env), env); //COLUMN
    }

    public void dropColumn(String table, String field) throws SQLException {
        executeDDL("ALTER TABLE " + table + " DROP COLUMN " + field);
    }

    public void renameColumn(String table, String columnName, String newColumnName) throws SQLException {
        executeDDL("ALTER TABLE " + table + " RENAME " + columnName + " TO " + newColumnName);
    }

    public void modifyColumn(String table, Field field, Type oldType) throws SQLException {
        ExecuteEnvironment env = new ExecuteEnvironment();
        executeDDL("ALTER TABLE " + table + " ALTER COLUMN " + field.name + " TYPE " +
                field.type.getDB(syntax, env) + " " + syntax.typeConvertSuffix(oldType, field.type, field.name, env), env);
    }

    public void packTable(Table table) throws SQLException {
        String dropWhere = table.properties.toString(new GetValue<String, PropertyField>() {
            public String getMapValue(PropertyField value) {
                return value.name + " IS NULL";
            }}, " AND ");
        executeDML("DELETE FROM " + table.getName(syntax) + (dropWhere.length() == 0 ? "" : " WHERE " + dropWhere));
    }

    private SQLTemporaryPool temporaryPool = new SQLTemporaryPool();
    private SQLTemporaryPool getSQLTemporaryPool() { // в зависимости от политики или локальный пул (для сессии) или глобальный пул
        return temporaryPool;
    }

    private final Map<String, WeakReference<Object>> sessionTablesMap = MapFact.mAddRemoveMap();
    private int sessionCounter = 0;

    public SessionTable createTemporaryTable(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count, FillTemporaryTable fill, Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> queryClasses, Object owner) throws SQLException {
        Result<Integer> actual = new Result<Integer>();
        return new SessionTable(getTemporaryTable(keys, properties, fill, count, actual, owner), keys, properties, queryClasses.first, queryClasses.second, actual.result);
    }

    private final Set<String> transactionTables = SetFact.mAddRemoveSet();

    public String getTemporaryTable(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, FillTemporaryTable fill, Integer count, Result<Integer> actual, Object owner) throws SQLException {
        needPrivate();

        removeUnusedTemporaryTables();

        Result<Boolean> isNew = new Result<Boolean>();
        String table = getSQLTemporaryPool().getTable(this, keys, properties, fill, count, actual, sessionTablesMap, isNew);
        assert !sessionTablesMap.containsKey(table);
        sessionTablesMap.put(table, new WeakReference<Object>(owner));
        if(isNew.result && isInTransaction())
            transactionTables.add(table);

        return table;
    }

    private void removeUnusedTemporaryTables() throws SQLException {
        synchronized (sessionTablesMap) {
            for (Iterator<Map.Entry<String, WeakReference<Object>>> iterator = sessionTablesMap.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<String, WeakReference<Object>> entry = iterator.next();
                if (entry.getValue().get() == null) {
//                    dropTemporaryTableFromDB(entry.getKey());
                    iterator.remove();
                }
            }
        }
    }

    public void returnTemporaryTable(SessionTable table, Object owner) throws SQLException {
        synchronized (sessionTablesMap) {
            assert sessionTablesMap.containsKey(table.name);
            WeakReference<Object> removed = sessionTablesMap.remove(table.name);
            assert removed.get()==owner;

//            dropTemporaryTableFromDB(table.name);
        }

        tryCommon();
    }
    
    public void rollReturnTemporaryTable(SessionTable table, Object owner) throws SQLException {
        needPrivate();

        synchronized (sessionTablesMap) {
            // assertion построен на том что между началом транзакции ее rollback'ом, все созданные таблицы в явную drop'ся, соответственно может нарушится если скажем открыта форма и не close'ута, или просто new IntegrationService идет
            // в принципе он не настолько нужен, но для порядка пусть будет
            // придется убрать так как чистых использований уже достаточно много, например ClassChange.materialize, DataSession.addObjects, правда что сейчас с assertion'ами делать неясно
            assert !sessionTablesMap.containsKey(table.name); // вернул назад
            sessionTablesMap.put(table.name, new WeakReference<Object>(owner));
        }
    }

    // напрямую не используется, только через Pool

    private void dropTemporaryTableFromDB(String tableName) throws SQLException {
        executeDDL(syntax.getDropSessionTable(tableName), ExecuteEnvironment.NOREADONLY);
    }

    public void createTemporaryTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties) throws SQLException {
        ExecuteEnvironment env = new ExecuteEnvironment();

        if(keys.size()==0)
            keys = SetFact.singletonOrder(KeyField.dumb);
        String createString = SetFact.addExcl(keys.getSet(), properties).toString(this.<Field>getDeclare(env), ",");
        createString = createString + "," + getConstraintDeclare(name, keys);
        executeDDL(syntax.getCreateSessionTable(name, createString), ExecuteEnvironment.NOREADONLY);
    }

    public void analyzeSessionTable(String table) throws SQLException {
        executeDDL("ANALYZE " + table, ExecuteEnvironment.NOREADONLY);
    }

    public void pushNoReadOnly(Connection connection) throws SQLException {
        if(inTransaction==0)
            connection.setReadOnly(false);
    }
    public void popNoReadOnly(Connection connection) throws SQLException {
        if(inTransaction==0)
            connection.setReadOnly(true);
    }
    
    private int volatileStats = 0;
    public void pushVolatileStats(Connection connection) throws SQLException {
        if(syntax.noDynamicSampling())
            if(volatileStats++==0)
                executeDDL("SET enable_nestloop=off");
    }
    public void popVolatileStats(Connection connection) throws SQLException {
        if(syntax.noDynamicSampling())
            if(--volatileStats==0)
               executeDDL("SET enable_nestloop=on");
    }

    public void toggleVolatileStats() throws SQLException {
        if(volatileStats==0)
            pushVolatileStats(null);
        else
            popVolatileStats(null);
    }

    public void toggleSQLLoggerDebugMode() {

        if(sqlLogger.getLevel()== Level.INFO)
            sqlLogger.setLevel(Level.DEBUG);
        else
            sqlLogger.setLevel(Level.INFO);
    }

    public void executeDDL(String DDL) throws SQLException {
        executeDDL(DDL, ExecuteEnvironment.EMPTY);
    }

    private void executeDDL(String DDL, ExecuteEnvironment env) throws SQLException {
        Connection connection = getConnection();

        Statement statement = createSingleStatement(connection);
        try {
            env.before(this, connection, DDL);

            statement.execute(DDL);

        } catch (SQLException e) {
            logger.error(statement.toString());
            throw e;
        } finally {
            env.after(this, connection, DDL);

            statement.close();

            returnConnection(connection);
        }
    }

    private int executeDML(SQLExecute execute) throws SQLException {
        return executeDML(execute.command, execute.params, execute.env);
    }

    private boolean explainAnalyzeMode = false;
    public void toggleExplainAnalyzeMode() {
        explainAnalyzeMode = !explainAnalyzeMode;
    }
    public void toggleExplainMode() {
        explainAnalyzeMode = !explainAnalyzeMode;
        explainNoAnalyze = explainAnalyzeMode;
    }
    private boolean explainNoAnalyze = false;
    private int executeExplain(PreparedStatement statement, boolean noAnalyze) throws SQLException {
        long l = System.currentTimeMillis();
        ResultSet result = statement.executeQuery();
        Integer rows = null;
        try {
            int i=0;
            while(result.next()) {
                String row = (String) result.getObject("QUERY PLAN");
                if(!noAnalyze && i++==0) { // первый ряд
                    Pattern pt = Pattern.compile(" rows=((\\d)+) ");
                    Matcher matcher = pt.matcher(row);
                    int m=0;
                    while(matcher.find()) {
                        if(m++==1) { // 2-е соответствие
                            rows = Integer.valueOf(matcher.group(1));
                            break;
                        }
                    }
                }
                systemLogger.info(row);
            }
        } finally {
            result.close();
        }

        if(rows==null)
            return 0;
        return rows;
    }

    @Message("message.sql.execute")
    private int executeDML(@ParamMessage String command, ImMap<String, ParseInterface> paramObjects, ExecuteEnvironment env) throws SQLException {
        Connection connection = getConnection();

        Result<ReturnStatement> returnStatement = new Result<ReturnStatement>();
        PreparedStatement statement = getStatement((explainAnalyzeMode && !explainNoAnalyze?"EXPLAIN (ANALYZE) ":"") + command, paramObjects, connection, syntax, env, returnStatement, env.isNoPrepare());

        int result = 0;
        long runTime = 0;
        try {
            if(explainAnalyzeMode) {
                PreparedStatement explainStatement = statement;
                Result<ReturnStatement> returnExplain = null; long explainStarted = 0;
                if(explainNoAnalyze) {
                    returnExplain = new Result<ReturnStatement>();
                    explainStatement = getStatement("EXPLAIN (VERBOSE, COSTS)" + command, paramObjects, connection, syntax, env, returnExplain, env.isNoPrepare());
                    explainStarted = System.currentTimeMillis();
                }
                systemLogger.info(explainStatement.toString());
                env.before(this, connection, command);
                result = executeExplain(explainStatement, explainNoAnalyze);
                env.after(this, connection, command);
                if(explainNoAnalyze)
                    returnExplain.result.proceed(explainStatement, System.currentTimeMillis() - explainStarted);
            }

            env.before(this, connection, command);

            if(!(explainAnalyzeMode && !explainNoAnalyze)) {
                long started = System.currentTimeMillis();
                result = statement.executeUpdate();
                runTime = System.currentTimeMillis() - started;
            }
        } catch (SQLException e) {
            logger.error(statement.toString());
            throw e;
        } finally {
            env.after(this, connection, command);

            returnStatement.result.proceed(statement, runTime);

            returnConnection(connection);
        }

        return result;
    }

    @Message("message.sql.execute")
    private int executeDML(@ParamMessage String command) throws SQLException {
        Connection connection = getConnection();

        int result = 0;
        Statement statement = createSingleStatement(connection);
        try {
            result = statement.executeUpdate(command);
        } catch (SQLException e) {
            logger.error(statement.toString());
            throw e;
        } finally {
            statement.close();

            returnConnection(connection);
        }
        return result;
    }

    // оптимизация
    private static <K> boolean hasConcColumns(ImMap<K, ? extends Reader> colReaders) {
        for(int i=0,size= colReaders.size();i<size;i++)
            if(colReaders.getValue(i) instanceof ConcatenateType)
                return true;
        return false;
    }

    private static <K, V> boolean hasConc(ImMap<K, ? extends Reader> keyReaders, ImMap<V, ? extends Reader> propertyReaders) {
        return hasConcColumns(keyReaders) || hasConcColumns(propertyReaders);
    }

    private ImMap<String, String> fixConcColumns(ImMap<String, ? extends Reader> colReaders, TypeEnvironment env) {
        MExclMap<String, String> mReadColumns = MapFact.mExclMap();
        for(int i=0,size=colReaders.size();i<size;i++) {
            String keyName = colReaders.getKey(i);
            colReaders.getValue(i).readDeconc(keyName, keyName, mReadColumns, syntax, env);
        }
        return mReadColumns.immutable();
    }

    private String fixConcSelect(String select, ImMap<String, ? extends Reader> keyReaders, ImMap<String, ? extends Reader> propertyReaders, TypeEnvironment env) {
        return "SELECT " + SQLSession.stringExpr(fixConcColumns(keyReaders, env), fixConcColumns(propertyReaders, env)) + " FROM (" + select + ") s";
    }

    @Message("message.sql.execute")
    public <K,V> ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> executeSelect(@ParamMessage String select, ExecuteEnvironment env, ImMap<String, ParseInterface> paramObjects, ImRevMap<K, String> keyNames, final ImMap<K, ? extends Reader> keyReaders, ImRevMap<V, String> propertyNames, ImMap<V, ? extends Reader> propertyReaders) throws SQLException {
        Connection connection = getConnection();

        if(explainAnalyzeMode) {
            systemLogger.info(select);
            Result<ReturnStatement> returnExplain = new Result<ReturnStatement>();
            PreparedStatement statement = getStatement("EXPLAIN (" + (explainNoAnalyze ? "VERBOSE, COSTS" : "ANALYZE") + ") " + select, paramObjects, connection, syntax, env, returnExplain, env.isNoPrepare());
            long started = System.currentTimeMillis();
            env.before(this, connection, select);
            executeExplain(statement, explainNoAnalyze);
            env.after(this, connection, select);
            returnExplain.result.proceed(statement, System.currentTimeMillis() - started);
        }

        // по хорошему надо бы внутрь pool'инга вставить, но это не такой большой overhead
        if(syntax.hasDriverCompositeProblem() && hasConc(keyReaders, propertyReaders))
            select = fixConcSelect(select, keyNames.crossJoin(keyReaders), propertyNames.crossJoin(propertyReaders), env);

        Result<ReturnStatement> returnStatement = new Result<ReturnStatement>();
        PreparedStatement statement = getStatement(select, paramObjects, connection, syntax, env, returnStatement, env.isNoPrepare());
        MOrderExclMap<ImMap<K,Object>,ImMap<V,Object>> mExecResult = MapFact.mOrderExclMap();
        long runTime = 0;
        try {
            env.before(this, connection, select);

            long started = System.currentTimeMillis();
            final ResultSet result = statement.executeQuery();
            runTime = System.currentTimeMillis() - started;
            try {
                while(result.next()) {
                    ImValueMap<K, Object> rowKeys = keyNames.mapItValues(); // потому как exception есть
                    for(int i=0,size=keyNames.size();i<size;i++)
                        rowKeys.mapValue(i, keyReaders.get(keyNames.getKey(i)).read(result, syntax, keyNames.getValue(i)));
                    ImValueMap<V, Object> rowProperties = propertyNames.mapItValues(); // потому как exception есть
                    for(int i=0,size=propertyNames.size();i<size;i++)
                        rowProperties.mapValue(i, propertyReaders.get(propertyNames.getKey(i)).read(result, syntax, propertyNames.getValue(i)));
                    mExecResult.exclAdd(rowKeys.immutableValue(), rowProperties.immutableValue());
                }
            } finally {
                result.close();
            }
        } catch (SQLException e) {
            logger.error(statement.toString());
            throw e;
        } finally {
            env.after(this, connection, select);

            returnStatement.result.proceed(statement, runTime);

            returnConnection(connection);
        }

        return mExecResult.immutableOrder();
    }

    public void insertBatchRecords(String table, ImOrderSet<KeyField> keys, ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows) throws SQLException {
        Connection connection = getConnection();

        ImOrderSet<PropertyField> properties = rows.getValue(0).keys().toOrderSet();
        ImOrderSet<Field> fields = SetFact.addOrderExcl(keys, properties);

        final ExecuteEnvironment env = new ExecuteEnvironment();

        String insertString = fields.toString(Field.nameGetter(), ",");
        String valueString = fields.toString(new GetValue<String, Field>() {
            public String getMapValue(Field value) {
                return value.type.writeDeconc(syntax, env);
            }}, ",");

        if(insertString.length()==0) {
            assert valueString.length()==0;
            insertString = "dumb";
            valueString = "0";
        }

        String command = "INSERT INTO " + syntax.getSessionTableName(table) + " (" + insertString + ") VALUES (" + valueString + ")";
        PreparedStatement statement = connection.prepareStatement(command);

        try {
            for(int i=0,size=rows.size();i<size;i++) {
                ParamNum paramNum = new ParamNum();
                for(KeyField key : keys)
                    new TypeObject(rows.getKey(i).get(key)).writeParam(statement, paramNum, syntax, env);
                for(PropertyField property : properties) {
                    ObjectValue propValue = rows.getValue(i).get(property);
                    if(propValue instanceof NullValue)
                        property.type.writeNullParam(statement, paramNum, syntax, env);
                    else
                        new TypeObject((DataObject) propValue).writeParam(statement, paramNum, syntax, env);
                }
                statement.addBatch();
            }

            env.before(this, connection, command);

            statement.executeBatch();

        } catch (SQLException e) {
            logger.error(statement.toString());
            throw e;
        } finally {
            env.after(this, connection, command);

            statement.close();

            returnConnection(connection);
        }
    }

    private void insertParamRecord(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields) throws SQLException {
        String insertString = "";
        String valueString = "";

        int paramNum = 0;
        MExclMap<String, ParseInterface> params = MapFact.<String, ParseInterface>mExclMapMax(keyFields.size()+propFields.size());

        // пробежим по KeyFields'ам
        for (int i=0,size=keyFields.size();i<size;i++) {
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + keyFields.getKey(i);
            DataObject keyValue = keyFields.getValue(i);
            if (keyValue.isString(syntax))
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + keyValue.getString(syntax);
            else {
                String prm = "qxprm" + (paramNum++) + "nx";
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + prm;
                params.exclAdd(prm, new TypeObject(keyValue));
            }
        }

        for (int i=0,size=propFields.size();i<size;i++) {
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + propFields.getKey(i).name;
            ObjectValue fieldValue = propFields.getValue(i);
            if (fieldValue.isString(syntax))
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + fieldValue.getString(syntax);
            else {
                String prm = "qxprm" + (paramNum++) + "nx";
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + prm;
                params.exclAdd(prm, new TypeObject((DataObject) fieldValue));
            }
        }

        if(insertString.length()==0) {
            assert valueString.length()==0;
            insertString = "dumb";
            valueString = "0";
        }

        executeDML("INSERT INTO " + table.getName(syntax) + " (" + insertString + ") VALUES (" + valueString + ")", params.immutable(), ExecuteEnvironment.EMPTY);
    }

    public void insertRecord(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields) throws SQLException {

        boolean needParam = false;

        for (int i=0,size=keyFields.size();i<size;i++)
            if (!keyFields.getKey(i).type.isSafeString(keyFields.getValue(i))) {
                needParam = true;
            }

        for (int i=0,size=propFields.size();i<size;i++)
            if (!propFields.getKey(i).type.isSafeString(propFields.getValue(i))) {
                needParam = true;
            }

        if (needParam) {
            insertParamRecord(table, keyFields, propFields);
            return;
        }

        String insertString = "";
        String valueString = "";

        // пробежим по KeyFields'ам
        for (int i=0,size=keyFields.size();i<size;i++) { // нужно сохранить общий порядок, поэтому без toString
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + keyFields.getKey(i).name;
            valueString = (valueString.length() == 0 ? "" : valueString + ',') + keyFields.getValue(i).getString(syntax);
        }

        // пробежим по Fields'ам
        for (int i=0,size=propFields.size();i<size;i++) { // нужно сохранить общий порядок, поэтому без toString
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + propFields.getKey(i).name;
            valueString = (valueString.length() == 0 ? "" : valueString + ',') + propFields.getValue(i).getString(syntax);
        }

        if(insertString.length()==0) {
            assert valueString.length()==0;
            insertString = "dumb";
            valueString = "0";
        }

        executeDML("INSERT INTO " + table.getName(syntax) + " (" + insertString + ") VALUES (" + valueString + ")");
    }

    public boolean isRecord(Table table, ImMap<KeyField, DataObject> keyFields) throws SQLException {

        // по сути пустое кол-во ключей
        return new Query<KeyField, String>(MapFact.<KeyField, KeyExpr>EMPTYREV(),
                table.join(DataObject.getMapExprs(keyFields)).getWhere()).execute(this).size() > 0;
    }

    public void ensureRecord(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields) throws SQLException {
        if (!isRecord(table, keyFields))
            insertRecord(table, keyFields, propFields);
    }

    public void updateRecords(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields) throws SQLException {
        if(!propFields.isEmpty()) // есть запись нужно Update лупить
            updateRecords(new ModifyQuery(table, new Query<KeyField, PropertyField>(table.getMapKeys(), Where.TRUE, keyFields, ObjectValue.getMapExprs(propFields))));
    }

    public boolean insertRecord(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, boolean update) throws SQLException {
        if(update && isRecord(table, keyFields)) {
            updateRecords(table, keyFields, propFields);
            return false;
        } else {
            insertRecord(table, keyFields, propFields);
            return true;
        }
    }

    public Object readRecord(Table table, ImMap<KeyField, DataObject> keyFields, PropertyField field) throws SQLException {
        // по сути пустое кол-во ключей
        return new Query<KeyField, String>(MapFact.<KeyField, KeyExpr>EMPTYREV(),
                table.join(DataObject.getMapExprs(keyFields)).getExpr(field), "result", Where.TRUE).
                execute(this).singleValue().get("result");
    }

    public void truncate(String table) throws SQLException {
//        executeDML("TRUNCATE " + syntax.getSessionTableName(table));
        executeDML("DELETE FROM " + syntax.getSessionTableName(table));
    }

    public <X> int deleteKeyRecords(Table table, ImMap<KeyField, X> keys) throws SQLException {
        String deleteWhere = keys.toString(new GetKeyValue<String, KeyField, X>() {
            public String getMapValue(KeyField key, X value) {
                return key.name + "=" + value;
            }}, " AND ");

        return executeDML("DELETE FROM " + table.getName(syntax) + (deleteWhere.length() == 0 ? "" : " WHERE " + deleteWhere));
    }

    private static int readInt(Object value) {
        return ((Number)value).intValue();
    }

    private static Statement createSingleStatement(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        statement.setEscapeProcessing(false); // для preparedStatement'ов эту операцию не имеет смысл делать
        return statement;
    }

    private static String getCntDist(String name) {
        return "cnd_" + name + "_cnd";
    }

    private static String getCnt(String name) {
        return "cnt_" + name + "_cnt";
    }

    // в явную без query так как часто выполняется
    public void readSingleValues(SessionTable table, Result<ImMap<KeyField, Object>> keyValues, Result<ImMap<PropertyField, Object>> propValues) throws SQLException {
        ImSet<KeyField> tableKeys = table.getTableKeys();
        ExecuteEnvironment env = new ExecuteEnvironment();

        MExclMap<String, String> mReadKeys = MapFact.mExclMap();
        mReadKeys.exclAdd(getCnt(""), syntax.getCount("*"));
        if(tableKeys.size() > 1)
            for(KeyField field : tableKeys) {
                mReadKeys.exclAdd(getCntDist(field.name), syntax.getCountDistinct(field.name));
                field.type.readDeconc("ANYVALUE(" + field.name + ")", field.name, mReadKeys, syntax, env);
            }
        else 
            if(table.properties.isEmpty()) {
                keyValues.set(MapFact.<KeyField, Object>EMPTY());
                propValues.set(MapFact.<PropertyField, Object>EMPTY());
                return;
            }
        ImMap<String, String> readKeys = mReadKeys.immutable();

        MExclMap<String, String> mReadProps = MapFact.mExclMap();
        for(PropertyField field : table.properties) {
            mReadProps.exclAdd(getCntDist(field.name), syntax.getCountDistinct(field.name));
            mReadProps.exclAdd(getCnt(field.name), syntax.getCount(field.name));
            field.type.readDeconc("ANYVALUE(" + field.name + ")", field.name, mReadProps, syntax, env);
        }
        ImMap<String, String> readProps = mReadProps.immutable();

        String select = "SELECT " + SQLSession.stringExpr(readKeys, readProps) + " FROM " + syntax.getSessionTableName(table.name);

        Connection connection = getConnection();

        Statement statement = createSingleStatement(connection);
        try {
            ResultSet result = statement.executeQuery(select);
            try {
                boolean next = result.next();
                assert next;
                
                int totalCnt = readInt(result.getObject(getCnt("")));
                if(tableKeys.size() > 1) {
                    ImFilterValueMap<KeyField, Object> mKeyValues = tableKeys.mapFilterValues();
                    for(int i=0,size=tableKeys.size();i<size;i++) {
                        KeyField tableKey = tableKeys.get(i);
                        Integer cnt = readInt(result.getObject(getCntDist(tableKey.name)));
                        if(cnt == 1)
                            mKeyValues.mapValue(i, tableKey.type.read(result, syntax, tableKey.name));
                    }
                    keyValues.set(mKeyValues.immutableValue());
                } else
                    keyValues.set(MapFact.<KeyField, Object>EMPTY());

                ImFilterValueMap<PropertyField, Object> mvPropValues = table.properties.mapFilterValues();
                for(int i=0,size=table.properties.size();i<size;i++) {
                    PropertyField tableProperty = table.properties.get(i);
                    Integer cntDistinct = readInt(result.getObject(getCntDist(tableProperty.name)));
                    if(cntDistinct==0)
                        mvPropValues.mapValue(i, null);
                    if(cntDistinct==1 && totalCnt==readInt(result.getObject(getCnt(tableProperty.name))))
                        mvPropValues.mapValue(i, tableProperty.type.read(result, syntax, tableProperty.name));
                }
                propValues.set(mvPropValues.immutableValue());

                assert !result.next();
            } finally {
                result.close();
            }
        } catch (SQLException e) {
            logger.error(statement.toString());
            throw e;
        } finally {
            statement.close();

            returnConnection(connection);
        }
    }

    public int deleteRecords(ModifyQuery modify) throws SQLException {
        if(modify.isEmpty()) // иначе exception кидает
            return 0;

        return executeDML(modify.getDelete(syntax));
    }

    public int updateRecords(ModifyQuery modify) throws SQLException {
        return executeDML(modify.getUpdate(syntax));
    }

    public int insertSelect(ModifyQuery modify) throws SQLException {
        return executeDML(modify.getInsertSelect(syntax));
    }
    public int insertSessionSelect(String name, IQuery<KeyField, PropertyField> query, QueryEnvironment env) throws SQLException {
        return executeDML(ModifyQuery.getInsertSelect(syntax.getSessionTableName(name), query, env, syntax));
    }

    public int insertLeftSelect(ModifyQuery modify, boolean updateProps, boolean insertOnlyNotNull) throws SQLException {
        return executeDML(modify.getInsertLeftKeys(syntax, updateProps, insertOnlyNotNull));
    }

    public int modifyRecords(ModifyQuery modify) throws SQLException {
        return modifyRecords(modify, false);
    }

        // сначала делает InsertSelect, затем UpdateRecords
    public int modifyRecords(ModifyQuery modify, boolean insertOnlyNotNull) throws SQLException {
        if (modify.isEmpty()) // оптимизация
            return 0;

        int result = 0;
        if (modify.table.isSingle()) {// потому как запросом никак не сделаешь, просто вкинем одну пустую запись
            if (!isRecord(modify.table, MapFact.<KeyField, DataObject>EMPTY()))
                result = insertSelect(modify);
        } else
            result = insertLeftSelect(modify, false, insertOnlyNotNull);
        updateRecords(modify);
        return result;
    }

    public void close() throws SQLException {
        if(privateConnection !=null)
            privateConnection.close();
    }

    private static class ParsedStatement {
        public final PreparedStatement statement;
        public final ImList<String> preparedParams;
        public final ExecuteEnvironment env;

        private ParsedStatement(PreparedStatement statement, ImList<String> preparedParams, ExecuteEnvironment env) {
            this.statement = statement;
            this.preparedParams = preparedParams;
            this.env = env;
        }
    }

    private static ParsedStatement parseStatement(ParseStatement parse, Connection connection, SQLSyntax syntax) throws SQLException {
        ExecuteEnvironment env = new ExecuteEnvironment();

        char[][] paramArrays = new char[parse.params.size()][];
        String[] params = new String[parse.params.size()];
        String[] safeStrings = new String[paramArrays.length];
        Type[] notSafeTypes = new Type[paramArrays.length];
        int paramNum = 0;
        for (int i=0,size= parse.params.size();i<size;i++) {
            String param = parse.params.get(i);
            paramArrays[paramNum] = param.toCharArray();
            params[paramNum] = param;
            safeStrings[paramNum] = parse.safeStrings.get(param);
            notSafeTypes[paramNum++] = parse.notSafeTypes.get(param);
        }

        // те которые isString сразу транслируем
        MList<String> mPreparedParams = ListFact.mList();
        char[] toparse = parse.statement.toCharArray();
        String parsedString = "";
        char[] parsed = new char[toparse.length + paramArrays.length * 100];
        int num = 0;
        for (int i = 0; i < toparse.length;) {
            int charParsed = 0;
            for (int p = 0; p < paramArrays.length; p++) {
                if (BaseUtils.startsWith(toparse, i, paramArrays[p])) { // нашли
                    String valueString;

                    Type notSafeType = notSafeTypes[p];
                    if (safeStrings[p] !=null) // если можно вручную пропарсить парсим
                        valueString = safeStrings[p];
                    else {
                        if(notSafeType instanceof ConcatenateType)
                            valueString = notSafeType.writeDeconc(syntax, env);
                        else
                            valueString = "?";
                        mPreparedParams.add(params[p]);
                    }
                    if (notSafeType !=null)
                        valueString = notSafeType.getCast(valueString, syntax, env);

                    char[] valueArray = valueString.toCharArray();
                    if(num + valueArray.length >= parsed.length) {
                        parsedString = parsedString + new String(parsed, 0, num);
                        parsed = new char[BaseUtils.max(toparse.length - i + paramArrays.length * 100, valueArray.length + 100)];
                        num = 0;
                    }
                    System.arraycopy(valueArray, 0, parsed, num, valueArray.length);
                    num += valueArray.length;
                    charParsed = paramArrays[p].length;
                    assert charParsed!=0;
                    break;
                }
            }
            if (charParsed == 0) {
                if(num + 1 >= parsed.length) {
                    parsedString = parsedString + new String(parsed, 0, num);
                    parsed = new char[toparse.length - i + paramArrays.length * 100 + 1];
                    num = 0;
                }
                parsed[num++] = toparse[i];
                charParsed = 1;
            }
            i = i + charParsed;
        }
        parsedString = parsedString + new String(parsed, 0, num);

        return new ParsedStatement(connection.prepareStatement(parsedString), mPreparedParams.immutableList(), env);
    }

    private static class ParseStatement extends TwinImmutableObject {
        public final String statement;
        public final ImSet<String> params;
        public final ImMap<String, String> safeStrings;
        public final ImMap<String, Type> notSafeTypes;

        private ParseStatement(String statement, ImSet<String> params, ImMap<String, String> safeStrings, ImMap<String, Type> notSafeTypes) {
            this.statement = statement;
            this.params = params;
            this.safeStrings = safeStrings;
            this.notSafeTypes = notSafeTypes;
        }

        public boolean twins(TwinImmutableObject o) {
            return notSafeTypes.equals(((ParseStatement) o).notSafeTypes) && params.equals(((ParseStatement) o).params) && safeStrings.equals(((ParseStatement) o).safeStrings) && statement.equals(((ParseStatement) o).statement);
        }

        public int immutableHashCode() {
            return 31 * (31 * (31 * statement.hashCode() + params.hashCode()) + safeStrings.hashCode()) + notSafeTypes.hashCode();
        }
    }

    private MCacheMap<ParseStatement, ParsedStatement> statementPool = LRUCache.mBig();

    private static interface ReturnStatement {
        void proceed(PreparedStatement statement, long runTime) throws SQLException;
    }

    private final static ReturnStatement keepStatement = new ReturnStatement() {
        public void proceed(PreparedStatement statement, long runTime) throws SQLException {
        }};

    private final static ReturnStatement closeStatement = new ReturnStatement() {
        public void proceed(PreparedStatement statement, long runTime) throws SQLException {
            statement.close();
        }};

    public static class ParamNum {
        private int paramNum = 1;

        public int get() {
            return paramNum++;
        }
    }

    private PreparedStatement getStatement(String command, ImMap<String, ParseInterface> paramObjects, Connection connection, SQLSyntax syntax, ExecuteEnvironment env, Result<ReturnStatement> returnStatement, boolean noPrepare) throws SQLException {

        boolean poolPrepared = !noPrepare && !Settings.get().isDisablePoolPreparedStatements() && command.length() > Settings.get().getQueryPrepareLength();

        final ParseStatement parse = preparseStatement(command, poolPrepared, paramObjects, syntax);

        ParsedStatement parsed = null;
        if(poolPrepared)
            parsed = statementPool.get(parse);
        if(parsed==null) {
            parsed = parseStatement(parse, connection, syntax);
            if(poolPrepared) {
                final ParsedStatement fParsed = parsed;
                returnStatement.set(new ReturnStatement() {
                    public void proceed(PreparedStatement statement, long runTime) throws SQLException {
                        if(runTime > Settings.get().getQueryPrepareRunTime())
                            statementPool.exclAdd(parse, fParsed);
                        else
                            statement.close();
                    }
                });
            } else
                returnStatement.set(closeStatement);
        } else
            returnStatement.set(keepStatement);

        ParamNum paramNum = new ParamNum();
        for (String param : parsed.preparedParams)
            paramObjects.get(param).writeParam(parsed.statement, paramNum, syntax, env);
        env.add(parsed.env);

        return parsed.statement;
    }

    private ParseStatement preparseStatement(String command, boolean parseParams, ImMap<String, ParseInterface> paramObjects, SQLSyntax syntax) {
        ImFilterValueMap<String, String> mvSafeStrings = paramObjects.mapFilterValues();
        ImFilterValueMap<String, Type> mvNotSafeTypes = paramObjects.mapFilterValues();
        for(int i=0,size=paramObjects.size();i<size;i++) {
            ParseInterface parseInterface = paramObjects.getValue(i);
            if(parseInterface.isSafeString() && !(parseParams && parseInterface instanceof TypeObject))
                mvSafeStrings.mapValue(i, parseInterface.getString(syntax));
            if(!parseInterface.isSafeType())
                mvNotSafeTypes.mapValue(i, parseInterface.getType());
        }
        return new ParseStatement(command, paramObjects.keys(), mvSafeStrings.immutableValue(), mvNotSafeTypes.immutableValue());
    }

    private final static GetKeyValue<String, String, String> addFieldAliases = new GetKeyValue<String, String, String>() {
        public String getMapValue(String key, String value) {
            return value + " AS " + key;
        }};
    // вспомогательные методы

    public static String stringExpr(ImMap<String, String> keySelect, ImMap<String, String> propertySelect) {
        return stringExpr(keySelect.toOrderMap(), propertySelect.toOrderMap());
    }
    public static String stringExpr(ImOrderMap<String, String> keySelect, ImOrderMap<String, String> propertySelect) {
        
        String expressionString = keySelect.addOrderExcl(propertySelect).toString(addFieldAliases, ",");
        if (expressionString.length() == 0)
            expressionString = "0";
        return expressionString;
    }

/*    public static <T> OrderedMap<String, String> mapNames(Map<T, String> exprs, Map<T, String> names, Result<ImList<T>> order) {
        OrderedMap<String, String> result = new OrderedMap<String, String>();
        if (order.isEmpty())
            for (Map.Entry<T, String> name : names.entrySet()) {
                result.put(name.getValue(), exprs.get(name.getKey()));
                order.add(name.getKey());
            }
        else // для union all
            for (T expr : order)
                result.put(names.get(expr), exprs.get(expr));
        return result;
    }
  */
    public static <T> ImOrderMap<String, String> mapNames(ImMap<T, String> exprs, ImRevMap<T, String> names, Result<ImOrderSet<T>> order) {
        return MapFact.orderMap(exprs, names, order);
    }

    public static ImOrderMap<String, String> mapNames(ImMap<String, String> exprs, Result<ImOrderSet<String>> order) {
        return mapNames(exprs, exprs.keys().toRevMap(), order);
    }

}
