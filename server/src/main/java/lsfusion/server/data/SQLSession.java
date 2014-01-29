package lsfusion.server.data;

import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSVSMap;
import lsfusion.server.data.query.*;
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
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.Message;
import lsfusion.server.ParamMessage;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.data.expr.KeyExpr;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.server.ServerLoggers.remoteLogger;
import static lsfusion.server.ServerLoggers.sqlLogger;
import static lsfusion.server.ServerLoggers.systemLogger;

public class SQLSession extends MutableObject {
    private static final Logger logger = ServerLoggers.sqlLogger;
    private static final Logger handLogger = ServerLoggers.sqlHandLogger;

    public SQLSyntax syntax;
    
    public <F extends Field> GetValue<String, F> getDeclare(final TypeEnvironment typeEnv) {
        return new GetValue<String, F>() {
            public String getMapValue(F value) {
                return value.getDeclare(syntax, typeEnv);
            }};
    }

    private final ConnectionPool connectionPool;
    public final TypePool typePool;

    public ExConnection getConnection() throws SQLException {
        temporaryTablesLock.lock();
        if (privateConnection != null) {
            ExConnection resultConnection = privateConnection;
            temporaryTablesLock.unlock();
            return resultConnection;
        } else {
            return connectionPool.getCommon(this);
        }
    }

    private void returnConnection(ExConnection connection) throws SQLException {
        if(privateConnection !=null)
            assert privateConnection == connection;
        else {
            connectionPool.returnCommon(this, connection);
            temporaryTablesLock.unlock();
        }
    }

    private ExConnection privateConnection = null;

    public boolean inconsistent = true; // для отладки

    public final static String userParam = "adsadaweewuser";
    public final static String isServerRestartingParam = "sdfisserverrestartingpdfdf";
    public final static String computerParam = "fjruwidskldsor";
    public final static String isDebugParam = "dsiljdsiowee";
    public final static String isFullClientParam = "fdfdijir";

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private ReentrantLock temporaryTablesLock = new ReentrantLock(true);

    public SQLSession(DataAdapter adapter) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        syntax = adapter;
        connectionPool = adapter;
        typePool = adapter;
    }

    private void needPrivate() throws SQLException { // получает unique connection
        if(privateConnection ==null)
            privateConnection = connectionPool.getPrivate(this);
    }

    private void tryCommon() throws SQLException { // пытается вернуться к
        removeUnusedTemporaryTables(false);
        // в зависимости от политики или локальный пул (для сессии) или глобальный пул
        if(inTransaction == 0 && volatileStats.get() == 0 && sessionTablesMap.isEmpty()) { // вернемся к commonConnection'у
            connectionPool.returnPrivate(this, privateConnection);
            privateConnection = null;
        }
    }

    private int inTransaction = 0; // счетчик для по сути распределенных транзакций

    public boolean isInTransaction() {
        return inTransaction > 0;
    }
    
    public boolean lockIsInTransaction() {
        lockRead();
        
        boolean isInTransaction = isInTransaction(); 
        
        unlockRead();
        
        return isInTransaction;
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

    private void lockRead() {
        lock.readLock().lock();
    }

    private void unlockRead() {
        lock.readLock().unlock();
    }

    public void unlockTemporary() {
        temporaryTablesLock.unlock();
    }

    private Integer prevIsolation;
    public void startTransaction(int isolationLevel) throws SQLException, SQLHandledException {
        lock.writeLock().lock();
        try {
            if(Settings.get().isApplyVolatileStats())
                pushVolatileStats(null);
    
            if(inTransaction++ == 0) {
                needPrivate();
                if(isolationLevel > 0) {
                    prevIsolation = privateConnection.sql.getTransactionIsolation();
                    privateConnection.sql.setTransactionIsolation(isolationLevel);
                }
                setACID(privateConnection.sql, true);
            }
        } catch (SQLException e) {
            throw propagate(e, "START TRANSACTION");
        }
    }

    private void endTransaction() throws SQLException {
        assert isInTransaction();
        try {
            if(--inTransaction == 0) {
                setACID(privateConnection.sql, false);
                if(prevIsolation != null) {
                    privateConnection.sql.setTransactionIsolation(prevIsolation);
                    prevIsolation = null;
                }
            }
    
            transactionCounter = null;
            transactionTables.clear();
    
            if(Settings.get().isApplyVolatileStats())
                popVolatileStats(null);
        } finally {
            try { tryCommon(); } catch (Throwable t) {}

            lock.writeLock().unlock();
        }
    }

    public void rollbackTransaction() throws SQLException {
        if(inTransaction == 1) { // транзакция заканчивается
            if(transactionCounter!=null) {
                // в зависимости от политики или локальный пул (для сессии) или глобальный пул
                int transTablesCount = privateConnection.temporary.getCounter() - transactionCounter;
                assert transactionTables.size() == transTablesCount;
                for(int i=0;i<transTablesCount;i++) {
    //                dropTemporaryTableFromDB(transactionTable);
                    
                    String transactionTable = SQLTemporaryPool.getTableName(i+transactionCounter);
                    
                    assert transactionTables.contains(transactionTable);                        
                    sessionTablesMap.remove(transactionTable);
                    privateConnection.temporary.removeTable(transactionTable);
                }
                privateConnection.temporary.setCounter(transactionCounter);
            } else
                assert transactionTables.size() == 0;
            if(!(problemInTransaction == Problem.CLOSED)) 
                try { privateConnection.sql.rollback(); } catch (Throwable t) {}            
            problemInTransaction = null; 
        }

        try { endTransaction(); } catch (Throwable t) {}
    }

    public void checkSessionTableMap(SessionTable table, Object owner) {
        assert sessionTablesMap.get(table.name).get() == owner;
    }

    public void commitTransaction() throws SQLException {
        privateConnection.sql.commit();
        endTransaction();
    }

    // удостоверивается что таблица есть
    public void ensureTable(Table table) throws SQLException {
        lockRead();
        ExConnection connection = getConnection();

        try {
            DatabaseMetaData metaData = connection.sql.getMetaData();
            ResultSet tables = metaData.getTables(null, null, table.name, new String[]{"TABLE"});
            if (!tables.next()) {
                createTable(table.name, table.keys);
                for (PropertyField property : table.properties)
                    addColumn(table.name, property);
            }
        } finally {
            returnConnection(connection);

            unlockRead();
        }
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

    private final Map<String, WeakReference<Object>> sessionTablesMap = MapFact.mAddRemoveMap();
    private int sessionCounter = 0;

    public SessionTable createTemporaryTable(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count, FillTemporaryTable fill, Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> queryClasses, Object owner) throws SQLException, SQLHandledException {
        Result<Integer> actual = new Result<Integer>();
        return new SessionTable(getTemporaryTable(keys, properties, fill, count, actual, owner), keys, properties, queryClasses.first, queryClasses.second, actual.result).checkClasses(this, null);
    }

    private final Set<String> transactionTables = SetFact.mAddRemoveSet();
    private Integer transactionCounter = null;

    public String getTemporaryTable(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, FillTemporaryTable fill, Integer count, Result<Integer> actual, Object owner) throws SQLException, SQLHandledException {
        lockRead();
        temporaryTablesLock.lock();

        needPrivate();

        String table;
        try {
            removeUnusedTemporaryTables(false);

            Result<Boolean> isNew = new Result<Boolean>();
            // в зависимости от политики или локальный пул (для сессии) или глобальный пул
            table = privateConnection.temporary.getTable(this, keys, properties, count, sessionTablesMap, isNew, owner);
            try {
                privateConnection.temporary.fillData(this, fill, count, actual, table);
            } catch (Throwable t) {
                returnTemporaryTable(table, owner); // вернем таблицу, если не смогли ее заполнить
                throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
            } finally {
                if(isNew.result && isInTransaction()) { // пометим как transaction
                    if(transactionCounter==null)
                        transactionCounter = privateConnection.temporary.getCounter() - 1;                   
                    transactionTables.add(table);
                }
            }
        } finally {
            unlockRead();
        }

        return table;
    }

    private void removeUnusedTemporaryTables(boolean force) throws SQLException {
        for (Iterator<Map.Entry<String, WeakReference<Object>>> iterator = sessionTablesMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, WeakReference<Object>> entry = iterator.next();
            if (force || entry.getValue().get() == null) {
//                    dropTemporaryTableFromDB(entry.getKey());
                truncate(entry.getKey());
                iterator.remove();
            }
        }
    }

    public void returnTemporaryTable(SessionTable table, Object owner) throws SQLException {
        lockRead();
        temporaryTablesLock.lock();

        try {
            truncate(table.name);
            returnTemporaryTable(table.name, owner);
        } finally {
            temporaryTablesLock.unlock();
            unlockRead();
        }
    }
    
    public void returnTemporaryTable(String table, Object owner) throws SQLException {

        assert sessionTablesMap.containsKey(table);
        WeakReference<Object> removed = sessionTablesMap.remove(table);
        assert removed.get()==owner;

//            dropTemporaryTableFromDB(table.name);

        tryCommon();
    }
    
    public void rollReturnTemporaryTable(SessionTable table, Object owner) throws SQLException {
        lockRead();
        temporaryTablesLock.lock();

        needPrivate();

        try {
            // assertion построен на том что между началом транзакции ее rollback'ом, все созданные таблицы в явную drop'ся, соответственно может нарушится если скажем открыта форма и не close'ута, или просто new IntegrationService идет
            // в принципе он не настолько нужен, но для порядка пусть будет
            // придется убрать так как чистых использований уже достаточно много, например ClassChange.materialize, DataSession.addObjects, правда что сейчас с assertion'ами делать неясно
            assert !sessionTablesMap.containsKey(table.name); // вернул назад
            sessionTablesMap.put(table.name, new WeakReference<Object>(owner));

        } finally {
            temporaryTablesLock.unlock();
            unlockRead();
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
        String createString = SetFact.addExclSet(keys.getSet(), properties).toString(this.<Field>getDeclare(env), ",");
        createString = createString + "," + getConstraintDeclare(name, keys);
        executeDDL(syntax.getCreateSessionTable(name, createString), ExecuteEnvironment.NOREADONLY);
    }

    public void analyzeSessionTable(String table) throws SQLException {
        executeDDL("ANALYZE " + table, ExecuteEnvironment.NOREADONLY);
    }

    private int noReadOnly = 0;
    private final Object noReadOnlyLock = new Object();
    public void pushNoReadOnly(Connection connection) throws SQLException {
        synchronized (noReadOnlyLock) {
            if(inTransaction == 0 && noReadOnly++ == 0) {
                connection.setReadOnly(false);
            }
        }
    }
    public void popNoReadOnly(Connection connection) throws SQLException {
        synchronized (noReadOnlyLock) {
            if(inTransaction == 0 && --noReadOnly == 0) {
                connection.setReadOnly(true);
            }
        }
    }
    
    private AtomicInteger volatileStats = new AtomicInteger(0);
    
    public boolean isVolatileStats() {
        return volatileStats.get() > 0;
    }
    
    public void pushVolatileStats(Connection connection) throws SQLException {
        if(syntax.noDynamicSampling())
            if(volatileStats.getAndIncrement() == 0) {
                temporaryTablesLock.lock();
                try {
                    needPrivate();
                } finally {
                    {
                        temporaryTablesLock.unlock();
                    }
                }

                executeDDL("SET enable_nestloop=off");
            }
    }

    public void popVolatileStats(Connection connection) throws SQLException {
        if (syntax.noDynamicSampling())
            if (volatileStats.decrementAndGet() == 0) {
                if(problemInTransaction == null)
                    executeDDL("SET enable_nestloop=on");

                lockRead();
                temporaryTablesLock.lock();
                try {
                    tryCommon();
                } finally {
                    temporaryTablesLock.unlock();
                    unlockRead();
                }
            }
    }

    private AtomicInteger noHandled = new AtomicInteger(0);

    // если вообще нет обработки handled exception'ов
    public void pushNoHandled() {
        noHandled.getAndIncrement();
    }
    
    public boolean isNoHandled() {
        return noHandled.get() > 0;
    }
    
    public void popNoHandled() {
        noHandled.decrementAndGet();
    }

    private AtomicInteger noTransactTimeout = new AtomicInteger(0);

    // если вообще нет обработки handled exception'ов
    public void pushNoTransactTimeout() {
        noTransactTimeout.getAndIncrement();
    }

    public boolean isNoTransactTimeout() {
        return noTransactTimeout.get() > 0;
    }

    public void popNoTransactTimeout() {
        noTransactTimeout.decrementAndGet();
    }

    public void toggleVolatileStats() throws SQLException {
        if(volatileStats.get() == 0)
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

    public void toggleRemoteLoggerDebugMode() {

        if(remoteLogger.getLevel()== Level.INFO)
            remoteLogger.setLevel(Level.TRACE);
        else
            remoteLogger.setLevel(Level.INFO);
    }

    public void executeDDL(String DDL) throws SQLException {
        executeDDL(DDL, ExecuteEnvironment.EMPTY);
    }

    private void executeDDL(String DDL, ExecuteEnvironment env) throws SQLException {
        lockRead();

        ExConnection connection = getConnection();

        Statement statement = createSingleStatement(connection.sql);
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
            unlockRead();
        }
    }

    private int executeDML(SQLExecute execute) throws SQLException, SQLHandledException {
        return executeDML(execute.command, execute.params, execute.env, execute.queryExecEnv, execute.transactTimeout);
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
    // причины медленных запросов:
    // Postgres (но могут быть и другие)
    // 1. Неравномерная статистика:
    // а) разная статистика в зависимости от значений поля - например несколько огромных приходных инвойсов и много расходныех
    // б) большое количество NULL значений (скажем 0,999975) - например признак своей компании в множестве юрлиц, тогда становится очень большая дисперсия (то есть тогда либо не компания, и результат 0, или компания и результат большой 100к, при этом когда применяется selectivity он ессно round'ся и 0-100к превращается в 10, что неправильно в общем случае)  
    // Лечится только разнесением в разные таблицы / по разным классам (когда это возможно)
    // Postgres - иногда может быть большое время планирования, но пока проблема была локальная на других базах не повторялась
    private int executeExplain(PreparedStatement statement, boolean noAnalyze, boolean dml) throws SQLException {
        long l = System.currentTimeMillis();
        ResultSet result = statement.executeQuery();
        Integer rows = null;
        try {
            int i=0;
            String row = null;
            List<String> out = new ArrayList<String>();
            while(result.next()) {
                row = (String) result.getObject("QUERY PLAN");

                Pattern pt = Pattern.compile(" rows=((\\d)+) ");
                Matcher matcher = pt.matcher(row);
                int est=0;
                int act=-1;
                int m=0;
                while(matcher.find()) {
                    if(m==0)
                        est = Integer.valueOf(matcher.group(1));
                    if(m==1) { // 2-е соответствие
                        act = Integer.valueOf(matcher.group(1));
                        break;
                    }
                    m++;
                }

                if(!noAnalyze && dml && i++==1 && act>=0) // второй ряд (первый почему то всегда 0)
                    rows = act;

                Pattern tpt = Pattern.compile("actual time=(((\\d)+)[.]((\\d)+))[.][.](((\\d)+)[.]((\\d)+))");
                matcher = tpt.matcher(row);
                double rtime = 0.0; // never executed
                if(matcher.find()) {
                    rtime = Double.valueOf(matcher.group(6));
                }

                String mark = "";
                double diff = ((double)act)/((double)est);
                if(act > 500) {
                    if(diff > 4)
                        mark += "G";
                    else if(diff < 0.25)
                        mark += "L";
                    if(rtime > 1000.0)
                        mark += "T";
                }
                else if(rtime > 100.0)
                    mark += "t";
                out.add(BaseUtils.padr(mark, 2) + row);
            }
            
            if(row != null) {
                Pattern pt = Pattern.compile("Total runtime: (((\\d)+)[.]((\\d)+))");
                Matcher matcher = pt.matcher(row);
                double rtime = 0.0;
                if(matcher.find()) {
                    rtime = Double.valueOf(matcher.group(1));
                }
                if(rtime > 100.0) {
                    systemLogger.info(statement.toString());
                    for(String outRow : out)
                        systemLogger.info(outRow);
                } //else {
                  //  systemLogger.info(rtime);
                //}
            }
        } finally {
            result.close();
        }

        if(rows==null)
            return 0;
//        if(rows==0) // INSERT'ы и UPDATE'ы почему-то всегда 0 лепят (хотя не всегда почему-то)
//            return 100;
        return rows;
    }
    
    private static enum Problem {
        EXCEPTION, CLOSED
    }
    private Problem problemInTransaction = null;

    private SQLException propagate(SQLException e, PreparedStatement statement, boolean isTransactTimeout) throws SQLException, SQLHandledException {
        return propagate(e, statement != null ? statement.toString() : "PREPARING STATEMENT", isTransactTimeout);
    }

    private SQLException propagate(SQLException e, String message) throws SQLException, SQLHandledException {
        return propagate(e, message, false);
    }
    
    private SQLException propagate(SQLException e, String message, boolean isTransactTimeout) throws SQLException, SQLHandledException {
        boolean inTransaction = isInTransaction();
        if(inTransaction)
            problemInTransaction = Problem.EXCEPTION;

        SQLHandledException handled = null;
        boolean deadLock = false;
        if(syntax.isUpdateConflict(e) || (deadLock = syntax.isDeadLock(e)))
            handled = new SQLConflictException(!deadLock);
        
        if(syntax.isTimeout(e))
            handled = new SQLTimeoutException(isTransactTimeout);
        
        if(syntax.isConnectionClosed(e)) {
            handled = new SQLClosedException();
            problemInTransaction = Problem.CLOSED;
        }

        if(handled != null) {
            handLogger.info(message + (inTransaction ? " TRANSACTION" : "") + " " + handled.toString());
            throw handled;
        }
        
        logger.error(message); // duplicate keys валится при : неправильный вывод классов в таблицах (см. SessionTable.assertCheckClasses), неправильном неявном приведении типов (от широкого к узкому, DataClass.containsAll), проблемах с округлениями, недетерминированные ORDER функции (GROUP LAST и т.п.), нецелостной базой (значения классов в базе не правильные)
        throw e;
    }

    @Message("message.sql.execute")
    public int executeDML(@ParamMessage String command, ImMap<String, ParseInterface> paramObjects, ExecuteEnvironment env, QueryExecuteEnvironment queryExecEnv, int transactTimeout) throws SQLException, SQLHandledException { // public для аспекта
        lockRead();
        ExConnection connection = getConnection();

        boolean isTransactTimeout = false;
        int result = 0;
        long runTime = 0;
        Result<ReturnStatement> returnStatement = new Result<ReturnStatement>();
        PreparedStatement statement = null;
        try {
            statement = getStatement((explainAnalyzeMode && !explainNoAnalyze?"EXPLAIN (ANALYZE, VERBOSE, COSTS) ":"") + command, paramObjects, connection, syntax, env, returnStatement, env.isNoPrepare());

            env.before(this, connection, command);
            isTransactTimeout = queryExecEnv.beforeStatement(statement, this, transactTimeout);

            if(explainAnalyzeMode) {
                PreparedStatement explainStatement = statement;
                Result<ReturnStatement> returnExplain = null; long explainStarted = 0;
                if(explainNoAnalyze) {
                    returnExplain = new Result<ReturnStatement>();
                    explainStatement = getStatement("EXPLAIN (VERBOSE, COSTS)" + command, paramObjects, connection, syntax, env, returnExplain, env.isNoPrepare());
                    explainStarted = System.currentTimeMillis();
                }
//                systemLogger.info(explainStatement.toString());
                env.before(this, connection, command);
                result = executeExplain(explainStatement, explainNoAnalyze, true);
                env.after(this, connection, command);
                if(explainNoAnalyze)
                    returnExplain.result.proceed(explainStatement, System.currentTimeMillis() - explainStarted);
            }

            if(!(explainAnalyzeMode && !explainNoAnalyze)) {
                long started = System.currentTimeMillis();
                result = statement.executeUpdate();
                runTime = System.currentTimeMillis() - started;
            }
        } catch (SQLException e) {
            throw propagate(e, statement, isTransactTimeout);
        } finally {
            env.after(this, connection, command);

            if(statement!=null)
                returnStatement.result.proceed(statement, runTime);

            returnConnection(connection);
            unlockRead();
        }

        return result;
    }

    @Message("message.sql.execute")
    private int executeDML(@ParamMessage String command) throws SQLException {
        lockRead();
        ExConnection connection = getConnection();

        int result = 0;
        Statement statement = createSingleStatement(connection.sql);
        try {
            result = statement.executeUpdate(command);
        } catch (SQLException e) {
            logger.error(statement.toString());
            throw e;
        } finally {
            statement.close();

            returnConnection(connection);
            unlockRead();
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

    public boolean outStatement = false;
    
    @Message("message.sql.execute")
    public <K,V> ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> executeSelect(@ParamMessage String select, ExecuteEnvironment env, ImMap<String, ParseInterface> paramObjects, QueryExecuteEnvironment queryExecEnv, int transactTimeout, ImRevMap<K, String> keyNames, final ImMap<K, ? extends Reader> keyReaders, ImRevMap<V, String> propertyNames, ImMap<V, ? extends Reader> propertyReaders) throws SQLException, SQLHandledException {
        lockRead();

        ExConnection connection = getConnection();

        if(explainAnalyzeMode) {
//            systemLogger.info(select);
            Result<ReturnStatement> returnExplain = new Result<ReturnStatement>();
            PreparedStatement statement = getStatement("EXPLAIN (" + (explainNoAnalyze ? "VERBOSE, COSTS" : "ANALYZE") + ") " + select, paramObjects, connection, syntax, env, returnExplain, env.isNoPrepare());
            long started = System.currentTimeMillis();
            env.before(this, connection, select);
            executeExplain(statement, explainNoAnalyze, false);
            env.after(this, connection, select);
            returnExplain.result.proceed(statement, System.currentTimeMillis() - started);
        }

        // по хорошему надо бы внутрь pool'инга вставить, но это не такой большой overhead
        if(syntax.hasDriverCompositeProblem() && hasConc(keyReaders, propertyReaders))
            select = fixConcSelect(select, keyNames.crossJoin(keyReaders), propertyNames.crossJoin(propertyReaders), env);

        Result<ReturnStatement> returnStatement = new Result<ReturnStatement>();
        long runTime = 0;
        boolean isTransactTimeout = false;
        PreparedStatement statement = null;

        try {
            statement = getStatement(select, paramObjects, connection, syntax, env, returnStatement, env.isNoPrepare());
            
            if(outStatement)
                System.out.println(statement.toString());                    
                    
            MOrderExclMap<ImMap<K,Object>,ImMap<V,Object>> mExecResult = MapFact.mOrderExclMap();
            env.before(this, connection, select);
            isTransactTimeout = queryExecEnv.beforeStatement(statement, this, transactTimeout);

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

            return mExecResult.immutableOrder();
        } catch (SQLException e) {
            throw propagate(e, statement, isTransactTimeout);
        } finally { // suppress'м все exception'ы, чтобы вернулся исходный
            try { env.after(this, connection, select); } catch (Throwable t) {}

            try { returnStatement.result.proceed(statement, runTime); } catch (Throwable t) {}

            try { returnConnection(connection); } catch (Throwable t) {}

            unlockRead();
        }
    }

    public void insertBatchRecords(String table, ImOrderSet<KeyField> keys, ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows) throws SQLException {
        lockRead();

        ExConnection connection = getConnection();

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
        PreparedStatement statement = connection.sql.prepareStatement(command);

        try {
            for(int i=0,size=rows.size();i<size;i++) {
                ParamNum paramNum = new ParamNum();
                for(KeyField key : keys)
                    new TypeObject(rows.getKey(i).get(key), key).writeParam(statement, paramNum, syntax, env);
                for(PropertyField property : properties) {
                    ObjectValue propValue = rows.getValue(i).get(property);
                    if(propValue instanceof NullValue)
                        property.type.writeNullParam(statement, paramNum, syntax, env);
                    else
                        new TypeObject((DataObject) propValue, property).writeParam(statement, paramNum, syntax, env);
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

            unlockRead();

        }
    }

    private void insertParamRecord(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields) throws SQLException {
        String insertString = "";
        String valueString = "";

        int paramNum = 0;
        MExclMap<String, ParseInterface> params = MapFact.<String, ParseInterface>mExclMapMax(keyFields.size()+propFields.size());

        // пробежим по KeyFields'ам
        for (int i=0,size=keyFields.size();i<size;i++) {
            KeyField key = keyFields.getKey(i);
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + key.name;
            DataObject keyValue = keyFields.getValue(i);
            if (keyValue.isString(syntax))
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + keyValue.getString(syntax);
            else {
                String prm = "qxprm" + (paramNum++) + "nx";
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + prm;
                params.exclAdd(prm, new TypeObject(keyValue, key));
            }
        }

        for (int i=0,size=propFields.size();i<size;i++) {
            PropertyField property = propFields.getKey(i);
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + property.name;
            ObjectValue fieldValue = propFields.getValue(i);
            if (fieldValue.isString(syntax))
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + fieldValue.getString(syntax);
            else {
                String prm = "qxprm" + (paramNum++) + "nx";
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + prm;
                params.exclAdd(prm, new TypeObject((DataObject) fieldValue, property));
            }
        }

        if(insertString.length()==0) {
            assert valueString.length()==0;
            insertString = "dumb";
            valueString = "0";
        }

        try {
            executeDML("INSERT INTO " + table.getName(syntax) + " (" + insertString + ") VALUES (" + valueString + ")", params.immutable(), ExecuteEnvironment.EMPTY, QueryExecuteEnvironment.DEFAULT, 0);
        } catch (SQLHandledException e) {
            throw new UnsupportedOperationException(); // по идее ни deadlock'а, ни update conflict'а, ни timeout'а
        }
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

    public boolean isRecord(Table table, ImMap<KeyField, DataObject> keyFields) throws SQLException, SQLHandledException {

        // по сути пустое кол-во ключей
        return new Query<KeyField, String>(MapFact.<KeyField, KeyExpr>EMPTYREV(),
                table.join(DataObject.getMapExprs(keyFields)).getWhere()).execute(this).size() > 0;
    }

    public void ensureRecord(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields) throws SQLException, SQLHandledException {
        if (!isRecord(table, keyFields))
            insertRecord(table, keyFields, propFields);
    }

    public void updateRecords(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields) throws SQLException, SQLHandledException {
        updateRecords(table, false, keyFields, propFields);
    }

    public int updateRecordsCount(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields) throws SQLException, SQLHandledException {
        return updateRecords(table, true, keyFields, propFields);
    }

    private int updateRecords(Table table, boolean count, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields) throws SQLException, SQLHandledException {
        if(!propFields.isEmpty()) // есть запись нужно Update лупить
            return updateRecords(new ModifyQuery(table, new Query<KeyField, PropertyField>(table.getMapKeys(), Where.TRUE, keyFields, ObjectValue.getMapExprs(propFields))));
        if(count)
            return isRecord(table, keyFields) ? 1 : 0;
        return 0;

    }

    public boolean insertRecord(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, boolean update) throws SQLException, SQLHandledException {
        if(update && isRecord(table, keyFields)) {
            updateRecords(table, keyFields, propFields);
            return false;
        } else {
            insertRecord(table, keyFields, propFields);
            return true;
        }
    }

    public Object readRecord(Table table, ImMap<KeyField, DataObject> keyFields, PropertyField field) throws SQLException, SQLHandledException {
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


        lockRead();

        ExConnection connection = getConnection();

        Statement statement = createSingleStatement(connection.sql);
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

            unlockRead();

        }
    }

    public int deleteRecords(ModifyQuery modify) throws SQLException, SQLHandledException {
        if(modify.isEmpty()) // иначе exception кидает
            return 0;

        return executeDML(modify.getDelete(syntax));
    }

    public int updateRecords(ModifyQuery modify) throws SQLException, SQLHandledException {
        return executeDML(modify.getUpdate(syntax));
    }

    public int insertSelect(ModifyQuery modify) throws SQLException, SQLHandledException {
        return executeDML(modify.getInsertSelect(syntax));
    }
    public int insertSessionSelect(String name, IQuery<KeyField, PropertyField> query, QueryEnvironment env) throws SQLException, SQLHandledException {
//        query.outSelect(this, env);
        return executeDML(ModifyQuery.getInsertSelect(syntax.getSessionTableName(name), query, env, syntax));
    }

    public int insertLeftSelect(ModifyQuery modify, boolean updateProps, boolean insertOnlyNotNull) throws SQLException, SQLHandledException {
        return executeDML(modify.getInsertLeftKeys(syntax, updateProps, insertOnlyNotNull));
    }

    public int modifyRecords(ModifyQuery modify) throws SQLException, SQLHandledException {
        return modifyRecords(modify, new Result<Integer>());
    }

    public int modifyRecords(ModifyQuery modify, Result<Integer> proceeded) throws SQLException, SQLHandledException {
        return modifyRecords(modify, proceeded, false);
    }

    // сначала делает InsertSelect, затем UpdateRecords
    public int modifyRecords(ModifyQuery modify, Result<Integer> proceeded, boolean insertOnlyNotNull) throws SQLException, SQLHandledException {
        if (modify.isEmpty()) { // оптимизация
            proceeded.set(0);
            return 0;
        }

        int result = 0;
        if (modify.table.isSingle()) {// потому как запросом никак не сделаешь, просто вкинем одну пустую запись
            if (!isRecord(modify.table, MapFact.<KeyField, DataObject>EMPTY()))
                result = insertSelect(modify);
        } else
            result = insertLeftSelect(modify, false, insertOnlyNotNull);
        int updated = updateRecords(modify);
        proceeded.set(result + updated);
        return result;
    }

    public void close() throws SQLException {
        lock.writeLock().lock();
        temporaryTablesLock.lock();

        try {
            if(privateConnection !=null) {
                try {
                    removeUnusedTemporaryTables(true);
                } finally {
                    connectionPool.returnPrivate(this, privateConnection);
                    privateConnection = null;
                }
            }
        } finally {
            temporaryTablesLock.unlock();
            lock.writeLock().unlock();
        }
    }
    
    public boolean tryRestore() {
        lockRead();
        temporaryTablesLock.lock();
        try {
            if(privateConnection != null || Settings.get().isCommonUnique()) // вторая штука перестраховка, но такая опция все равно не используется
                return false;
            // повалился common
            assert sessionTablesMap.isEmpty();
            connectionPool.restoreCommon();
            return true;
        } catch(Exception e) {
            return false;
        } finally {
            temporaryTablesLock.unlock();
            unlockRead();
        }
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

    private static final LRUWSVSMap<SQLSession, ParseStatement, ParsedStatement> statementPool = new LRUWSVSMap<SQLSession, ParseStatement, ParsedStatement>(LRUUtil.G1);

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

    private PreparedStatement getStatement(String command, ImMap<String, ParseInterface> paramObjects, ExConnection connection, SQLSyntax syntax, ExecuteEnvironment env, Result<ReturnStatement> returnStatement, boolean noPrepare) throws SQLException {

        boolean poolPrepared = !noPrepare && !Settings.get().isDisablePoolPreparedStatements() && command.length() > Settings.get().getQueryPrepareLength();

        final ParseStatement parse = preparseStatement(command, poolPrepared, paramObjects, syntax);

        ParsedStatement parsed = null;
        if(poolPrepared)
            parsed = statementPool.get(this, parse);
        if(parsed==null) {
            parsed = parseStatement(parse, connection.sql, syntax);
            if(poolPrepared) {
                final ParsedStatement fParsed = parsed;
                returnStatement.set(new ReturnStatement() {
                    public void proceed(PreparedStatement statement, long runTime) throws SQLException {
                        if(runTime > Settings.get().getQueryPrepareRunTime())
                            statementPool.put(SQLSession.this, parse, fParsed);
                        else
                            statement.close();
                    }
                });
            } else
                returnStatement.set(closeStatement);
        } else
            returnStatement.set(keepStatement);

        try {
            ParamNum paramNum = new ParamNum();
            for (String param : parsed.preparedParams)
                paramObjects.get(param).writeParam(parsed.statement, paramNum, syntax, env);
            env.add(parsed.env);
        } catch (SQLException e) {
            returnStatement.result.proceed(parsed.statement, 0);
            throw e;
        }

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
