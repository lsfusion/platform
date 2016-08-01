package lsfusion.server.data;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSVSMap;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.DistinctKeys;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.*;
import lsfusion.server.data.query.stat.ExecCost;
import lsfusion.server.data.sql.DataAdapter;
import lsfusion.server.data.sql.SQLExecute;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.*;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.navigator.SQLSessionUserProvider;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.stack.ExecutionStackAspect;
import lsfusion.server.stack.ParamMessage;
import lsfusion.server.stack.StackMessage;
import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.log4j.Logger;
import org.postgresql.PGConnection;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.ref.WeakReference;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.server.ServerLoggers.explainLogger;

public class SQLSession extends MutableClosedObject<OperationOwner> {
    private PreparedStatement executingStatement;
    private static final Logger logger = ServerLoggers.sqlLogger;
    private static final Logger handLogger = ServerLoggers.sqlHandLogger;
    private static final Logger sqlConflictLogger = ServerLoggers.sqlConflictLogger;
    private WeakReference<Thread> activeThread;

    private static ConcurrentWeakHashMap<SQLSession, Integer> sqlSessionMap = MapFact.getGlobalConcurrentWeakHashMap();
    public static ConcurrentHashMap<Long, Long> threadAllocatedBytesAMap = MapFact.getGlobalConcurrentHashMap();
    public static ConcurrentHashMap<Long, Long> threadAllocatedBytesBMap = MapFact.getGlobalConcurrentHashMap();

    private Long startTransaction;
    private Map<String, Integer> attemptCountMap = new HashMap<>();
    public StatusMessage statusMessage;

    public static SQLSession getSQLSession(Integer sqlProcessId) {
        if(sqlProcessId != null) {
            for (SQLSession sqlSession : sqlSessionMap.keySet()) {
                ExConnection connection = sqlSession.getDebugConnection();
                if (connection != null && ((PGConnection) connection.sql).getBackendPID() == sqlProcessId)
                    return sqlSession;
            }
        }
        return null;
    }

    public static Integer getSQLProcessId(long processId) {
        for(SQLSession sqlSession : sqlSessionMap.keySet()) {
            ExConnection connection = sqlSession.getDebugConnection();
            if(connection != null) {
                Thread activeThread = sqlSession.getActiveThread();
                if(activeThread != null && activeThread.getId() == processId)
                    return ((PGConnection) connection.sql).getBackendPID();
            }
        }
        logger.error(String.format("Failed to interrupt process %s: no private connection found", processId));
        return null;
    }

    public static Map<Integer, SQLSession> getSQLSessionMap() {
        Map<Integer, SQLSession> sessionMap = new HashMap<>();
        for(SQLSession sqlSession : sqlSessionMap.keySet()) {
            ExConnection connection = sqlSession.getDebugConnection();
            if(connection != null)
                sessionMap.put(((PGConnection) connection.sql).getBackendPID(), sqlSession);
        }
        return sessionMap;
    }

    public static Map<Integer, List<Object>> getSQLThreadMap() {
        Map<Integer, List<Object>> sessionMap = new HashMap<>();
        for(SQLSession sqlSession : sqlSessionMap.keySet()) {
            ExConnection connection = sqlSession.getDebugConnection();
            if(connection != null)
                sessionMap.put(((PGConnection) connection.sql).getBackendPID(), Arrays.asList(sqlSession.getActiveThread(),
                        sqlSession.isInTransaction(), sqlSession.startTransaction, sqlSession.getAttemptCountMap(), sqlSession.statusMessage,
                        sqlSession.userProvider.getCurrentUser(), sqlSession.userProvider.getCurrentComputer(),
                        sqlSession.getExecutingStatement(), sqlSession.isDisabledNestLoop, sqlSession.getQueryTimeout()));
        }
        return sessionMap;
    }

    public static Long getThreadAllocatedBytes(Long currentAllocatedBytes, Long id) {
            Long a = threadAllocatedBytesAMap.get(id);
            return (currentAllocatedBytes == null ? 0 : currentAllocatedBytes) - (a == null ? 0 : a);
    }

    public static void updateThreadAllocatedBytesMap() {
        threadAllocatedBytesAMap = MapFact.getGlobalConcurrentHashMap(threadAllocatedBytesBMap);
        threadAllocatedBytesBMap.clear();
        ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
        if (tBean instanceof com.sun.management.ThreadMXBean) {
            long[] allThreadIds = tBean.getAllThreadIds();
            long[] threadAllocatedBytes = ((com.sun.management.ThreadMXBean) tBean).getThreadAllocatedBytes(allThreadIds);
            for (int i=0;i<allThreadIds.length;i++) {
                threadAllocatedBytesBMap.put(allThreadIds[i], threadAllocatedBytes[i]);
            }
        }
    }

    // [todo]: переопределен из-за того, что используется ConcurrentWeakHashMap (желательно какой-нибудь ConcurrentIdentityHashMap)
    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    public String getExecutingStatement() {
        return executingStatement == null ? null : executingStatement.toString();
    }

    public static void cancelExecutingStatement(SQLSession sqlSession, long processId, boolean interrupt) throws SQLException, SQLHandledException {
        if(sqlSession != null) {
            Integer sqlProcessId = SQLSession.getSQLProcessId(processId);
            if(sqlProcessId != null) {
                SQLSession cancelSession = SQLSession.getSQLSession(sqlProcessId);
                ServerLoggers.exinfoLog("SQL SESSION INTERRUPT " + cancelSession + " IN " + sqlSession);
                if (cancelSession != null)
                    cancelSession.setForcedCancel(interrupt);
                sqlSession.executeDDL(((DataAdapter) sqlSession.syntax).getCancelActiveTaskQuery(sqlProcessId));
            }
        }
    }

    public Integer getQueryTimeout() {
        try {
            return executingStatement == null ? null : executingStatement.getQueryTimeout();
        } catch (SQLException e) {
            return null;
        }
    }

    private interface SQLRunnable {
        void run() throws SQLException, SQLHandledException;
    }
    private static void runSuppressed(SQLRunnable run, Result<Throwable> firstException) {
        try {
            run.run();
        } catch (Throwable t) {
            if(t instanceof ThreadDeath)
                ServerLoggers.exinfoLog("UNEXPECTED THREAD DEATH");
            if(firstException.result == null)
                firstException.set(t);
            else
                ServerLoggers.sqlSuppLog(t);
        }
    }

    private static void finishExceptions(Result<Throwable> firstException) throws SQLException {
        if(firstException.result != null)
            throw ExceptionUtils.propagate(firstException.result, SQLException.class);
    }

    private void finishHandledExceptions(Result<Throwable> firstException) throws SQLException, SQLHandledException {
        if(firstException.result != null)
            throw ExceptionUtils.propagate(firstException.result, SQLException.class, SQLHandledException.class);
    }

    public SQLSyntax syntax;

    public SQLSessionUserProvider userProvider;

    public <F extends Field> GetValue<String, F> getDeclare(final TypeEnvironment typeEnv) {
        return getDeclare(syntax, typeEnv);
    }
    
    public static <F extends Field> GetValue<String, F> getDeclare(final SQLSyntax syntax, final TypeEnvironment typeEnv) {
        return new GetValue<String, F>() {
            public String getMapValue(F value) {
                return value.getDeclare(syntax, typeEnv);
            }};
    }

    private final ConnectionPool connectionPool;
    public final TypePool typePool;

    public ExConnection getConnection() throws SQLException {
        temporaryTablesLock.lock();
        ExConnection resultConnection = null;
        boolean useCommon = false;
        if (privateConnection != null) {
            explicitNeedPrivate++; // нужно чтобы никто не вернул connection, до returnConnection, по сути мы требуем private, если он уже есть

            needPrivate(); // на самом деле не обязательно вызывать (connection уже private), чисто для скобок needPrivate / tryCommon в returnConnection

            resultConnection = privateConnection;
            temporaryTablesLock.unlock();
        } else
            useCommon = true;

        try {
            if (useCommon)
                resultConnection = connectionPool.getCommon(this);
            resultConnection.checkClosed();
            resultConnection.updateLogLevel(syntax);
        } catch (Throwable t) {
            if(useCommon)
                temporaryTablesLock.unlock();
            throw Throwables.propagate(t);
        }
        return resultConnection;
    }

    public void returnConnection(ExConnection connection, OperationOwner owner) throws SQLException {
        if(privateConnection !=null) { // вернутся / изменится не может так как explicitNeedPrivate включен
            assert privateConnection == connection;
            // по сути lockTryCommon, но так как в getConnection lockNeedPrivate - нельзя использовать, оставим здесь этот код в явную
            temporaryTablesLock.lock();

            try {
                explicitNeedPrivate--;

                tryCommon(owner, false); // здесь не может быть true, так как в removeUnused идут DDL команды, которые тоже используют get/returnConnection, и получается бесконечный цикл
            } finally {
                temporaryTablesLock.unlock();
            }
        } else { // висит полный lock
            try {
                connectionPool.returnCommon(this, connection);
            } finally {
                temporaryTablesLock.unlock();
            }
        }
    }

    private ExConnection privateConnection = null;
    
    public ExConnection getDebugConnection() {
        return privateConnection;
    }

    public boolean inconsistent = false;

    public final static String userParam = "adsadaweewuser";
    public final static String isServerRestartingParam = "sdfisserverrestartingpdfdf";
    public final static String computerParam = "fjruwidskldsor";
    public final static String formParam = "yfifybdfnenfykfqyth";
    public final static String connectionParam = "ntreottgjlrkxtybt";
    public final static String isDebugParam = "dsiljdsiowee";
    public final static String isFullClientParam = "fdfdijir";

    public final static String limitParam = "sdsnjklirens";

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private ReentrantLock temporaryTablesLock = new ReentrantLock(true);
    private ReentrantReadWriteLock connectionLock = new ReentrantReadWriteLock(true);

    public SQLSession(DataAdapter adapter, SQLSessionUserProvider userProvider) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        syntax = adapter;
        connectionPool = adapter;
        typePool = adapter;
        this.userProvider = userProvider;
        sqlSessionMap.put(this, 1);
    }

    private void needPrivate() throws SQLException { // получает unique connection
        assertLock();
        if(privateConnection ==null) {
            assert transactionTables.isEmpty();
            privateConnection = connectionPool.getPrivate(this);
//            sqlHandLogger.info("Obtaining backend PID: " + ((PGConnection) privateConnection.sql).getBackendPID());
//            System.out.println(this + " : NULL -> " + privateConnection + " " + " " + sessionTablesMap.keySet() + ExceptionUtils.getStackTrace());
        }
    }

    private void tryCommon(OperationOwner owner, boolean removeUnused) throws SQLException { // пытается вернуться к
        assertLock();

        if(removeUnused)
            removeUnusedTemporaryTables(false, owner);

        // в зависимости от политики или локальный пул (для сессии) или глобальный пул
        if(inTransaction == 0 && sessionTablesMap.isEmpty() && explicitNeedPrivate == 0) { // вернемся к commonConnection'у
            ServerLoggers.assertLog(privateConnection != null, "BRACES NEEDPRIVATE - TRYCOMMON SHOULD MATCH");
            connectionPool.returnPrivate(this, privateConnection);
//            System.out.println(this + " " + privateConnection + " -> NULL " + " " + sessionTablesMap.keySet() +  ExceptionUtils.getStackTrace());
//            sqlHandLogger.info("Returning backend PID: " + ((PGConnection) privateConnection.sql).getBackendPID());
            privateConnection = null;
        }
    }

    private void assertLock() {
        ServerLoggers.assertLog((temporaryTablesLock.isLocked() && lock.getReadLockCount() > 0) || lock.isWriteLocked(), "TEMPORARY TABLE SHOULD BY LOCKED");
    }

    private int explicitNeedPrivate;
    public void lockNeedPrivate() throws SQLException {
        temporaryTablesLock.lock();

        try {
            explicitNeedPrivate++;

            needPrivate();
        } finally {
            temporaryTablesLock.unlock();
        }
    }

    public void lockTryCommon(OperationOwner owner) throws SQLException {
        temporaryTablesLock.lock();
        try {
            explicitNeedPrivate--;

            tryCommon(owner, false);
        } finally {
            temporaryTablesLock.unlock();
        }
    }

    private int inTransaction = 0; // счетчик для по сути распределенных транзакций

    public boolean isInTransaction() {
        return inTransaction > 0;
    }

    public static void setACID(Connection connection, boolean ACID, SQLSyntax syntax) throws SQLException {
        connection.setAutoCommit(!ACID);
        connection.setReadOnly(!ACID);

        Statement statement = createSingleStatement(connection);
        try {
            syntax.setACID(statement, ACID);
        } catch (SQLException e) {
            logger.error(statement.toString());
            throw e;
        } finally {
            statement.close();
        }
    }

    private void lockRead(OperationOwner owner) {
        lockRead(owner, false);
    }

    private boolean tryLockRead(OperationOwner owner) {
        return lockRead(owner, true);
    }

    private boolean lockRead(OperationOwner owner, boolean tryLock) {
        checkClosed();

        if(tryLock) {
            boolean locked = lock.readLock().tryLock();
            if(!locked)
                return false;
        } else {
//            ServerLoggers.pausableLogStack("LOCKREAD TRY " + this);
            lock.readLock().lock();
        }
//        ServerLoggers.pausableLogStack("LOCKREAD GET " + this);

        try {
            prevActiveThread = setActiveThread();
            if(owner != OperationOwner.unknown)
                owner.checkThreadSafeAccess(writeOwner);
        } catch (Throwable t) {
            unlockRead();
            throw t;
        }

        return true;
    }

    @Override
    public String toString() {
        return "SQL@"+System.identityHashCode(this);
    }

    private void checkClosed() {
        ServerLoggers.assertLog(!isClosed(), "SQL SESSION IS ALREADY CLOSED " + this);
        if(isClosed())
            throw new RuntimeException("Sql session is already closed"); // иначе может начать работать в чужом connection'е
    }

    private void unlockRead() {
        unlockRead(false);
    }

    private void unlockRead(boolean dropActiveThread) {
        if(dropActiveThread)
            dropActiveThread(prevActiveThread);
        lock.readLock().unlock();
//        ServerLoggers.pausableLogStack("UNLOCKREAD " + this);
    }

    private WeakReference<Thread> prevActiveThread;
    private OperationOwner writeOwner;

    private void lockWrite(OperationOwner owner) {
        lockWrite(owner, false);
    }
    private boolean tryLockWrite(OperationOwner owner) {
        return lockWrite(owner, true);
    }
    private boolean lockWrite(OperationOwner owner, boolean tryLock) {
        checkClosed();

        if(tryLock) {
            boolean locked = lock.writeLock().tryLock();
            if(!locked)
                return false;
        } else {
//            ServerLoggers.pausableLogStack("LOCKWRITE TRY " + this);
            lock.writeLock().lock();
        }
//        ServerLoggers.pausableLogStack("LOCKWRITE GET " + this);

        prevActiveThread = setActiveThread();
        writeOwner = owner;

        return true;
    }

    private void unlockWrite() {
        unlockWrite(false);
    }

    private void unlockWrite(boolean dropActiveThread) {
        writeOwner = null;

        if(dropActiveThread)
            dropActiveThread(prevActiveThread);
        lock.writeLock().unlock();
//        ServerLoggers.pausableLogStack("UNLOCKWRITE " + this);
    }
    
    private Integer prevIsolation;
    private long transStartTime;
    public int getSecondsFromTransactStart() {
        assert isInTransaction();
        return (int) ((System.currentTimeMillis() - transStartTime)/1000);
    }

    public void startFakeTransaction(OperationOwner owner) throws SQLException, SQLHandledException {
        lockWrite(owner);

        explicitNeedPrivate++;

        needPrivate();

        privateConnection.sql.setReadOnly(false);
    }

    public void endFakeTransaction(OperationOwner owner) throws SQLException, SQLHandledException {
        try {
            privateConnection.sql.setReadOnly(false);

            explicitNeedPrivate--;

            tryCommon(owner, false);
        } finally {
            unlockWrite();
        }
    }

    public void startTransaction(int isolationLevel, OperationOwner owner) throws SQLException, SQLHandledException {
        startTransaction(isolationLevel, owner, new HashMap<String, Integer>(), false);
    }

    public void startTransaction(int isolationLevel, OperationOwner owner, Map<String, Integer> attemptCountMap, boolean useDeadLockPriority) throws SQLException, SQLHandledException {
        lockWrite(owner);
        startTransaction = System.currentTimeMillis();
        this.attemptCountMap = attemptCountMap;
        assert isInTransaction() || transactionTables.isEmpty();
        try {
            if(Settings.get().isApplyVolatileStats())
                pushVolatileStats(owner);
//            fifo.add("ST"  + getCurrentTimeStamp() + " " + this + " " + ExecutionStackAspect.getExStackTrace());
            if(inTransaction++ == 0) {
                transStartTime = System.currentTimeMillis();

                needPrivate();
                if(isolationLevel > 0) {
                    prevIsolation = privateConnection.sql.getTransactionIsolation();
                    privateConnection.sql.setTransactionIsolation(isolationLevel);
                }
                setACID(privateConnection.sql, true, syntax);

                this.useDeadLockPriority = useDeadLockPriority;
            }
        } catch (SQLException e) {
            throw ExceptionUtils.propagate(handle(e, "START TRANSACTION", privateConnection), SQLException.class, SQLHandledException.class);
        }
    }

    private void endTransaction(final OperationOwner owner) throws SQLException {
        Result<Throwable> firstException = new Result<>();

        assert isInTransaction();
        runSuppressed(new SQLRunnable() {
            public void run() throws SQLException {
                if(inTransaction == 1) {
                    if(useDeadLockPriority) {
                        if(deadLockPriority != null)
                            setDeadLockPriority(privateConnection, owner, null);
                        useDeadLockPriority = false;
                    }

                    setACID(privateConnection.sql, false, syntax);
                    if(prevIsolation != null) {
                        privateConnection.sql.setTransactionIsolation(prevIsolation);
                        prevIsolation = null;
                    }
                }

                transactionCounter = null;
                transactionTables.clear();
                endTransactionSessionTablesCount();

                if(Settings.get().isApplyVolatileStats())
                    popVolatileStats(owner);
            }}, firstException);

        runSuppressed(new SQLRunnable() {
                          public void run() throws SQLException {
                              inTransaction--;
                          }
                      }, firstException);

        runSuppressed(new SQLRunnable() {
            public void run() throws SQLException {
                tryCommon(owner, true);
            }}, firstException);

        startTransaction = null;
        attemptCountMap = new HashMap<>();
        unlockWrite();

        finishExceptions(firstException);
    }

    public void rollbackTransaction() throws SQLException {
        rollbackTransaction(OperationOwner.unknown);
    }
    public void rollbackTransaction(final OperationOwner owner) throws SQLException {
        Result<Throwable> firstException = new Result<>();

        if(inTransaction == 1) { // транзакция заканчивается
            runSuppressed(new SQLRunnable() {
                public void run() throws SQLException {
                    if(transactionCounter!=null) {
                        // в зависимости от политики или локальный пул (для сессии) или глобальный пул
                        int transTablesCount = privateConnection.temporary.getCounter() - transactionCounter;
                        if(transactionTables.size() != transTablesCount) {
                            ServerLoggers.assertLog(false, "CONSEQUENT TRANSACTION TABLES : COUNT " + transTablesCount + " " + transactionCounter + " " + transactionTables);
                        }
                        for(String transactionTable : transactionTables) {
                            //                dropTemporaryTableFromDB(transactionTable);

//                            String transactionTable = privateConnection.temporary.getTableName(i+transactionCounter);

//                            ServerLoggers.assertLog(transactionTables.contains(transactionTable), "CONSEQUENT TRANSACTION TABLES : HOLE");
//                            returnUsed(transactionTable, sessionTablesMap);
                            WeakReference<TableOwner> tableOwner = sessionTablesMap.remove(transactionTable);
//                            fifo.add("TRANSRET " + getCurrentTimeStamp() + " " + transactionTable + " " + privateConnection.temporary + " " + BaseUtils.nullToString(tableOwner) + " " + BaseUtils.nullToString(tableOwner == null ? null : tableOwner.get()) + " " + owner + " " + SQLSession.this + " " + ExecutionStackAspect.getExStackTrace());
//                            
//                            if(Settings.get().isEnableHacks())
//                                sessionTablesStackReturned.put(transactionTable, ExceptionUtils.getStackTrace());
//
                            lastReturnedStamp.remove(transactionTable);
                            privateConnection.temporary.removeTable(transactionTable);
                        }
                        privateConnection.temporary.setCounter(transactionCounter);
                    } else
                        ServerLoggers.assertLog(transactionTables.size() == 0, "CONSEQUENT TRANSACTION TABLES");

                    rollbackTransactionSessionTablesCount();
                }}, firstException);

            if(!(problemInTransaction == Problem.CLOSED))
                runSuppressed(new SQLRunnable() {
                    public void run() throws SQLException {
                        privateConnection.sql.rollback();
                    }}, firstException);
            problemInTransaction = null;
        }

//        fifo.add("RBACK"  + getCurrentTimeStamp() + " " + this + " " + ExecutionStackAspect.getExStackTrace());

        runSuppressed(new SQLRunnable() {
            public void run() throws SQLException {
                endTransaction(owner);
            }}, firstException);

        finishExceptions(firstException);
    }

    public void checkSessionTableMap(SessionTable table, Object owner) {
        assert sessionTablesMap.get(table.getName()).get() == owner;
    }

    public void commitTransaction() throws SQLException {
        commitTransaction(OperationOwner.unknown);
    }
    public void commitTransaction(OperationOwner owner) throws SQLException {
        if(inTransaction == 1)
            privateConnection.sql.commit();
//        fifo.add("CMT"  + getCurrentTimeStamp() + " " + this + " " + ExecutionStackAspect.getExStackTrace());

        endTransaction(owner);
    }

    // удостоверивается что таблица есть
    public void ensureTable(Table table, Logger logger) throws SQLException {
        lockRead(OperationOwner.unknown);
        ExConnection connection = getConnection();

        try {
//            DatabaseMetaData meta = connection.sql.getMetaData();
//            ResultSet res = meta.getTables(null, null, null,
//                    new String[] {"TABLE"});
//            while (res.next()) {
//                System.out.println(
//                        "   "+res.getString("TABLE_CAT")
//                                + ", "+res.getString("TABLE_SCHEM")
//                                + ", "+res.getString("TABLE_NAME")
//                                + ", "+res.getString("TABLE_TYPE")
//                                + ", "+res.getString("REMARKS"));
//            }
            DatabaseMetaData metaData = connection.sql.getMetaData();
            ResultSet tables = metaData.getTables(null, null, syntax.getMetaName(table.getName()), new String[]{"TABLE"});
            if (!tables.next()) {
                createTable(table, table.keys, logger);
                for (PropertyField property : table.properties)
                    addColumn(table, property);
            }
        } finally {
            try {
                returnConnection(connection, OperationOwner.unknown);
            } finally {
                unlockRead();
            }
        }
    }

    public void addExtraIndices(Table table, ImOrderSet<KeyField> keys, Logger logger) throws SQLException {
        for(int i=1;i<keys.size();i++)
            addIndex(table, BaseUtils.<ImOrderSet<Field>>immutableCast(keys).subOrder(i, keys.size()).toOrderMap(true), logger);
    }

    public void checkExtraIndices(SQLSession threadLocalSQL, Table table, ImOrderSet<KeyField> keys, Logger logger) throws SQLException {
        for(int i=1;i<keys.size();i++) {
            ImOrderMap<Field, Boolean> fields = BaseUtils.<ImOrderSet<Field>>immutableCast(keys).subOrder(i, keys.size()).toOrderMap(true);
            if (!threadLocalSQL.checkIndex(table, fields, false) && !threadLocalSQL.checkIndex(table, fields, true))
                addIndex(table, fields, logger);
        }
    }

    private String getConstraintName(String table) {
        return syntax.getConstraintName("PK_" + table);
    }

    private static String getConstraintDeclare(String table, ImOrderSet<KeyField> keys, SQLSyntax syntax) {
        String keyString = keys.toString(Field.<KeyField>nameGetter(syntax), ",");
        // "CONSTRAINT " + getConstraintName(table) + " "
        return "PRIMARY KEY " + syntax.getClustered() + " (" + keyString + ")";
    }

    public void createTable(Table table, ImOrderSet<KeyField> keys, Logger logger) throws SQLException {
        MStaticExecuteEnvironment env = StaticExecuteEnvironmentImpl.mEnv();

        if (keys.size() == 0)
            keys = SetFact.singletonOrder(KeyField.dumb);
        String createString = keys.toString(this.<KeyField>getDeclare(env), ",");
        createString = createString + "," + getConstraintDeclare(table.getName(), keys, syntax);

//        System.out.println("CREATE TABLE "+Table.Name+" ("+CreateString+")");
        executeDDL("CREATE TABLE " + table.getName(syntax) + " (" + createString + ")", env.finish());
        addExtraIndices(table, keys, logger);
    }

    public void renameTable(Table table, String newTableName) throws SQLException {
        executeDDL("ALTER TABLE " + table.getName(syntax) + " RENAME TO " + syntax.getTableName(newTableName));
    }

    public void dropTable(Table table) throws SQLException {
        executeDDL("DROP TABLE " + table.getName(syntax));
    }

    static String getIndexName(Table table, ImOrderMap<String, Boolean> fields, SQLSyntax syntax) {
        return syntax.getIndexName(fields.keyOrderSet().toString("_") + "_idx" + (syntax.isIndexNameLocal() ? "" : "_" + table.getName()));
    }

    static String getOldIndexName(Table table, ImOrderMap<String, Boolean> fields, SQLSyntax syntax) {
        return syntax.getIndexName((syntax.isIndexNameLocal() ? "" : table.getName() + "_") + fields.keyOrderSet().toString("_") + "_idx");
    }

    static String getIndexName(Table table, SQLSyntax syntax, ImOrderMap<Field, Boolean> fields) {
        return getIndexName(table, fields.mapOrderKeys(Field.nameGetter()), syntax);
    }

    private ImOrderMap<String, Boolean> getOrderFields(ImOrderSet<KeyField> keyFields, ImOrderSet<String> fields, boolean order) {
        ImOrderMap<String, Boolean> result = fields.toOrderMap(false);
        if(order)
            result = result.addOrderExcl(keyFields.mapOrderSetValues(Field.<KeyField>nameGetter()).toOrderMap(true));
        return result;
    }

    private ImOrderMap<Field, Boolean> getOrderFields(ImOrderSet<KeyField> keyFields, boolean order, ImOrderSet<Field> fields) {
        ImOrderMap<Field, Boolean> result = fields.toOrderMap(false);
        if(order)
            result = result.addOrderExcl(keyFields.toOrderMap(true));
        return result;
    }
    
    public void addIndex(Table table, ImOrderSet<KeyField> keyFields, ImOrderSet<Field> fields, boolean order, Logger logger) throws SQLException {
        addIndex(table, getOrderFields(keyFields, order, fields), logger);
    }

    public boolean checkIndex(Table table, ImOrderSet<KeyField> keyFields, ImOrderSet<Field> fields, boolean order) throws SQLException {
        return checkIndex(table, getOrderFields(keyFields, order, fields), false);
     }

    public boolean checkIndex(Table table, ImOrderMap<Field, Boolean> fields, boolean old) throws SQLException {
        //in Postgres 9.5 will be 'create index if not exists'
        boolean exists = true;
        try {
            executeDDL("SELECT 'public." + (old ? getOldIndexName(table, fields.mapOrderKeys(Field.nameGetter()), syntax) : getIndexName(table, syntax, fields)) + "'::regclass");
        } catch (SQLException e) {
            exists = false;
        }
        return exists;
    }

    public void addConstraint(Table table) throws SQLException {
        try {
            executeDDL("DO $$ BEGIN ALTER TABLE " + table.getName() + " ADD " + getConstraintDeclare(table.getName(), table.keys, syntax) +
            "; EXCEPTION WHEN others THEN /* ignore duplicates */ END; $$;");
        } catch(Exception e) {
            logger.error(e);
        }
    }

    public void addIndex(Table table, ImOrderMap<Field, Boolean> fields, Logger logger) throws SQLException {
        String columns = fields.toString(new GetKeyValue<String, Field, Boolean>() {
            public String getMapValue(Field key, Boolean value) {
                return key.getName(syntax) + " " + syntax.getOrderDirection(false, value);
            }}, ",");

        long start = System.currentTimeMillis();
        String nameIndex = getIndexName(table, syntax, fields);
        logger.info(String.format("Adding index started: %s", nameIndex));
        executeDDL("CREATE INDEX " + nameIndex + " ON " + table.getName(syntax) + " (" + columns + ")");
        logger.info(String.format("Adding index: %s, %sms", nameIndex, System.currentTimeMillis() - start));
    }

    public void dropIndex(Table table, ImOrderSet<KeyField> keyFields, ImOrderSet<String> fields, boolean order, boolean ifExists) throws SQLException {
        dropIndex(table, getOrderFields(keyFields, fields, order), ifExists);
    }

    public void dropIndex(Table table, ImOrderMap<String, Boolean> fields, boolean ifExists) throws SQLException {
        executeDDL("DROP INDEX " + (ifExists ? "IF EXISTS " : "" ) + getIndexName(table, fields, syntax) + (syntax.isIndexNameLocal() ? " ON " + table.getName(syntax) : ""));
    }

    public void renameIndex(Table table, ImOrderSet<KeyField> keyFields, ImOrderSet<String> fields, boolean order) throws SQLException {
        renameIndex(table, getOrderFields(keyFields, fields, order));
    }

    public void renameIndex(Table table, ImOrderSet<KeyField> keyFields, ImOrderSet<String> oldFields, ImOrderSet<String> newFields, boolean order, boolean ifExists) throws SQLException {
        renameIndex(table, getOrderFields(keyFields, oldFields, order), getOrderFields(keyFields, newFields, order), ifExists);
    }

    public void renameIndex(Table table, ImOrderMap<String, Boolean> fields) throws SQLException {
        executeDDL("ALTER INDEX " + getOldIndexName(table, fields, syntax) + " RENAME TO " + getIndexName(table, fields, syntax));
    }

    public void renameIndex(Table table, ImOrderMap<String, Boolean> oldFields, ImOrderMap<String, Boolean> newFields, boolean ifExists) throws SQLException {
        executeDDL("ALTER INDEX " + (ifExists ? "IF EXISTS " : "" ) + getIndexName(table, oldFields, syntax) + " RENAME TO " + getIndexName(table, newFields, syntax));
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

    public void addColumn(Table table, PropertyField field) throws SQLException {
        MStaticExecuteEnvironment env = StaticExecuteEnvironmentImpl.mEnv();
        executeDDL("ALTER TABLE " + table.getName(syntax) + " ADD " + field.getDeclare(syntax, env), env.finish()); //COLUMN
    }

    public void dropColumn(Table table, Field field) throws SQLException {
        innerDropColumn(table.getName(syntax), field.getName(syntax));
    }

    public void dropColumn(String table, String field) throws SQLException {
        innerDropColumn(syntax.getTableName(table), syntax.getFieldName(field));
    }

    private void innerDropColumn(String table, String field) throws SQLException {
        executeDDL("ALTER TABLE " + table + " DROP COLUMN " + field);
    }

    public void renameColumn(String table, String columnName, String newColumnName) throws SQLException {
        executeDDL(syntax.getRenameColumn(table, columnName, newColumnName));
    }

    public void modifyColumn(Table table, Field field, Type oldType) throws SQLException {
        MStaticExecuteEnvironment env = StaticExecuteEnvironmentImpl.mEnv();
        executeDDL("ALTER TABLE " + table + " ALTER COLUMN " + field.getName(syntax) + " " + syntax.getTypeChange(oldType, field.type, field.getName(syntax), env));
    }

    public void modifyColumns(Table table, Map<Field, Type> fieldTypeMap) throws SQLException {
        MStaticExecuteEnvironment env = StaticExecuteEnvironmentImpl.mEnv();
        String ddl = "";
        for(Map.Entry<Field, Type> entry : fieldTypeMap.entrySet()) {
            Field field = entry.getKey();
            Type oldType = entry.getValue();
            ddl += (ddl.isEmpty() ? "" : ",") + " ALTER COLUMN " + field.getName(syntax) + " " + syntax.getTypeChange(oldType, field.type, field.getName(syntax), env);
        }
        executeDDL("ALTER TABLE " + table + " " + ddl);
    }

    public void packTable(Table table, OperationOwner owner, TableOwner tableOwner) throws SQLException {
        checkTableOwner(table, tableOwner);
        
        String dropWhere = table.properties.toString(new GetValue<String, PropertyField>() {
            public String getMapValue(PropertyField value) {
                return value.getName(syntax) + " IS NULL";
            }}, " AND ");
        executeDML("DELETE FROM " + table.getName(syntax) + (dropWhere.length() == 0 ? "" : " WHERE " + dropWhere), owner, tableOwner, register(table, tableOwner, TableChange.DELETE));
    }

    private final Map<String, WeakReference<TableOwner>> sessionTablesMap = MapFact.mAddRemoveMap(); // все использования assertLock
    private final Map<String, Long> lastReturnedStamp = MapFact.mAddRemoveMap(); // все использования assertLock

    private int totalSessionTablesCount; // lockRead
    // по большому счету от sessionTablesCount можно отказаться (правда TRUNCATE не возвращает количество записей)
    private final Map<String, Integer> sessionTablesCount = MapFact.mAddRemoveMap(); // lockRead

    public int getSessionTablesCountAll(boolean lockedWrite) throws SQLException { // assertLock
        assert !lockedWrite || calcSessionTablesCountAll() == totalSessionTablesCount; // если не lockedwrite то может отличаться количество, так как может пройти изменение, но не делаться registerChange
        return totalSessionTablesCount;
    }

    private int calcSessionTablesCountAll() throws SQLException {
        int result = 0;
        for(Map.Entry<String, Integer> tableEntry : sessionTablesCount.entrySet()) {
            assert tableEntry.getValue().equals(getSessionCount(tableEntry.getKey(), OperationOwner.unknown));
            result += tableEntry.getValue();
        }
        return result;
    }

    //
//    public static void addUsed(String table, TableOwner owner, Map<String, WeakReference<TableOwner>> sessionTablesMap, Map<String, String> sessionTablesStackGot) {
//        fdfd
//    }
//
//    public static void returnUsed(String table, Map<String, WeakReference<TableOwner>> sessionTablesMap, Map<String, String> sessionTablesStackGot) {
//        fdfd
//    }
//
//    private final Map<String, String> sessionTablesStackGot = MapFact.mAddRemoveMap();
//    private final Map<String, String> sessionTablesStackReturned = MapFact.mAddRemoveMap();
//    
    // need to check classes after
    public SessionTable createTemporaryTable(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count, DistinctKeys<KeyField> distinctKeys, ImMap<PropertyField, PropStat> statProps, FillTemporaryTable fill, Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> queryClasses, TableOwner owner, OperationOwner opOwner) throws SQLException, SQLHandledException {
        Result<Integer> actual = new Result<>();
        return new SessionTable(getTemporaryTable(keys, properties, fill, count, actual, owner, opOwner), keys, properties, queryClasses.first, queryClasses.second, actual.result, distinctKeys, statProps).checkClasses(this, null, SessionTable.nonead, opOwner);
    }

    private final Set<String> transactionTables = SetFact.mAddRemoveSet();
    private Integer transactionCounter = null;
    private Integer transactionTotalSessionTablesCount = null;
    private final Map<String, Integer> transactionSessionTablesCount = MapFact.mAddRemoveMap();
    private void registerTransactionChange(String table) {
        if(isInTransaction()) {
            if (!transactionSessionTablesCount.containsKey(table))
                transactionSessionTablesCount.put(table, sessionTablesCount.get(table));
            if (transactionTotalSessionTablesCount == null)
                transactionTotalSessionTablesCount = totalSessionTablesCount;
        }
    }
    private void endTransactionSessionTablesCount() { // assert lockWrite
        transactionSessionTablesCount.clear();
        transactionTotalSessionTablesCount = null;
    }
    private void rollbackTransactionSessionTablesCount() { // assert lockWrite
        for(Map.Entry<String, Integer> tableCount : transactionSessionTablesCount.entrySet()) {
            if(tableCount.getValue() == null)
                sessionTablesCount.remove(tableCount.getKey());
            else
                sessionTablesCount.put(tableCount.getKey(), tableCount.getValue());
        }
        if(transactionTotalSessionTablesCount != null)
            totalSessionTablesCount = transactionTotalSessionTablesCount;
    }

    public static Buffer fifo = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(1000000));
    public static void outFifo() throws IOException {
        String filename = "e:\\out.txt";
        BufferedWriter outputWriter = new BufferedWriter(new FileWriter(filename));
        for (Object ff : fifo) {
            outputWriter.write(ff+"");
            outputWriter.newLine();
        }
        outputWriter.flush();
        outputWriter.close();
     }
    
    public String getTemporaryTable(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, FillTemporaryTable fill, Integer count, Result<Integer> actual, TableOwner owner, OperationOwner opOwner) throws SQLException, SQLHandledException {
        lockRead(opOwner);
        String table;
        try {
            temporaryTablesLock.lock();
            Result<Boolean> isNew = new Result<>();

            try {
                needPrivate();

                removeUnusedTemporaryTables(false, opOwner);

                // в зависимости от политики или локальный пул (для сессии) или глобальный пул
                table = privateConnection.temporary.getTable(this, keys, properties, count, sessionTablesMap, isNew, owner, opOwner); //, sessionTablesStackGot

                registerChange(table, owner, -1, TableChange.ADD);
                if(isNew.result && isInTransaction()) { // пометим как transaction
                    if(transactionCounter==null)
                        transactionCounter = privateConnection.temporary.getCounter() - 1;
                    transactionTables.add(table);
                }
            } finally {
                temporaryTablesLock.unlock();
            }
//            fifo.add("GET " + getCurrentTimeStamp() + " " + table + " " + privateConnection.temporary + " " + owner + " " + opOwner  + " " + this + " " + ExecutionStackAspect.getExStackTrace());
            try {
                privateConnection.temporary.fillData(this, fill, count, actual, table, opOwner);
            } catch (Throwable t) {
                returnTemporaryTable(table, owner, opOwner, t instanceof SQLTimeoutException || fill.canBeNotEmptyIfFailed()); // вернем таблицу, если не смогли ее заполнить, truncate при timeoutе потому как в остальных случаях и так должна быть пустая (строго говоря с timeout'ом это тоже перестраховка)
                try { ServerLoggers.assertLog(problemInTransaction != null || getSessionCount(table, opOwner) == 0, "TEMPORARY TABLE AFTER FILL NOT EMPTY"); } catch (Throwable i) { ServerLoggers.sqlSuppLog(i); }
                throw ExceptionUtils.propagate(t, SQLException.class, SQLHandledException.class);
            }
        } finally {
            unlockRead();
        }

        return table;
    }

    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");//dd/MM/yyyy
        Date now = new Date();
        return sdfDate.format(now);
    }
    
    private void removeUnusedTemporaryTables(boolean force, OperationOwner opOwner) throws SQLException {
        if(isInTransaction()) // потому как truncate сможет rollback'ся
            return;

        assertLock();
        for (Iterator<Map.Entry<String, WeakReference<TableOwner>>> iterator = sessionTablesMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, WeakReference<TableOwner>> entry = iterator.next();
            TableOwner tableOwner = entry.getValue().get();
            if (force || tableOwner == null) {
//                    dropTemporaryTableFromDB(entry.getKey());
//                fifo.add("RU " + getCurrentTimeStamp() + " " + force + " " + entry.getKey() + " " + privateConnection.temporary + " " + (tableOwner == null ? TableOwner.none : tableOwner) + " " + opOwner + " " + this + " " + ExecutionStackAspect.getExStackTrace());
                lastReturnedStamp.put(entry.getKey(), System.currentTimeMillis());
                truncateSession(entry.getKey(), opOwner, (tableOwner == null ? TableOwner.none : tableOwner));
                logger.info("REMOVE UNUSED TEMP TABLE : " + entry.getKey()); // потом надо будет больше инфы по owner'у добавить
                iterator.remove();
            }
        }
    }

    public void returnTemporaryTable(final SessionTable table, TableOwner owner, final OperationOwner opOwner) throws SQLException {
        lockedReturnTemporaryTable(table.getName(), owner, opOwner);
    }

    public void lockedReturnTemporaryTable(String name, TableOwner owner, OperationOwner opOwner) throws SQLException {
        lockRead(opOwner);

        try {
            returnTemporaryTable(name, owner, opOwner, true);
        } finally {
            unlockRead();
        }
    }

    @Override
    public OperationOwner getDefaultExplicitOwner() {
        return OperationOwner.unknown;
    }

    @Override
    public OperationOwner getFinalizeOwner() {
        return OperationOwner.unknown;
    }

    public void returnTemporaryTable(final String table, final TableOwner owner, final OperationOwner opOwner, boolean truncate) throws SQLException {
        temporaryTablesLock.lock();

        try {
            Result<Throwable> firstException = new Result<>();
//            fifo.add("RETURN " + getCurrentTimeStamp() + " " + truncate + " " + table + " " + privateConnection.temporary + " " + BaseUtils.nullToString(sessionTablesMap.get(table)) +  " " + owner + " " + opOwner  + " " + this + " " + ExecutionStackAspect.getExStackTrace());
            lastReturnedStamp.put(table, System.currentTimeMillis());
            if(truncate) {
                runSuppressed(new SQLRunnable() {
                    public void run() throws SQLException {
                        truncateSession(table, opOwner, owner);
                    }
                }, firstException);
                if(firstException.result != null) {
                    runSuppressed(new SQLRunnable() {
                        public void run() throws SQLException {
                            lastReturnedStamp.remove(table);
                            privateConnection.temporary.removeTable(table);
                        }}, firstException);
                    runSuppressed(new SQLRunnable() {
                        public void run() throws SQLException {
                            dropTemporaryTableFromDB(table);
                        }}, firstException);
                }
            }
    
            runSuppressed(new SQLRunnable() {
                public void run() throws SQLException {
                    assert sessionTablesMap.containsKey(table);
                    WeakReference<TableOwner> removed = sessionTablesMap.remove(table);
                    assert removed.get()==owner;
                }}, firstException);
    
            runSuppressed(new SQLRunnable() {
                public void run() throws SQLException {
                    tryCommon(opOwner, true);
                }}, firstException);

            finishExceptions(firstException);
        } finally { 
            temporaryTablesLock.unlock();
        }
    }
    
    public void rollReturnTemporaryTable(SessionTable table, TableOwner owner, OperationOwner opOwner) throws SQLException {
        lockRead(opOwner);
        try {
            temporaryTablesLock.lock();
            try {
                needPrivate();

                // assertion построен на том что между началом транзакции ее rollback'ом, все созданные таблицы в явную drop'ся, соответственно может нарушится если скажем открыта форма и не close'ута, или просто new IntegrationService идет
                // в принципе он не настолько нужен, но для порядка пусть будет
                // придется убрать так как чистых использований уже достаточно много, например ClassChange.materialize, DataSession.addObjects, правда что сейчас с assertion'ами делать неясно
                assert !sessionTablesMap.containsKey(table.getName()); // вернул назад
                WeakReference<TableOwner> value = new WeakReference<>(owner);
//                            fifo.add("RGET " + getCurrentTimeStamp() + " " + table + " " + privateConnection.temporary + " " + value + " " + owner + " " + opOwner  + " " + this + " " + ExecutionStackAspect.getExStackTrace());
                sessionTablesMap.put(table.getName(), value);

            } finally {
                temporaryTablesLock.unlock();
            }
        } finally {
            unlockRead();
        }
    }

    // напрямую не используется, только через Pool

    private void dropTemporaryTableFromDB(String tableName) throws SQLException {
        executeDDL(syntax.getDropSessionTable(tableName), StaticExecuteEnvironmentImpl.NOREADONLY);
    }

    public void createTemporaryTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, OperationOwner owner) throws SQLException {
        Pair<String, StaticExecuteEnvironment> result = getCreateDDL(name, keys, properties, syntax);
        executeDDL(result.first, result.second, owner);
    }

    private static Pair<String, StaticExecuteEnvironment> getCreateDDL(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, SQLSyntax syntax) {
        MStaticExecuteEnvironment mEnv = StaticExecuteEnvironmentImpl.mEnv();

        if(keys.size()==0)
            keys = SetFact.singletonOrder(KeyField.dumb);
        String createString = SetFact.addExclSet(keys.getSet(), properties).toString(SQLSession.<Field>getDeclare(syntax, mEnv), ",");
        createString = createString + "," + getConstraintDeclare(name, keys, syntax);
        mEnv.addNoReadOnly();
        String command = syntax.getCreateSessionTable(name, createString);
        StaticExecuteEnvironment env = mEnv.finish();
        return new Pair<>(command, env);
    }

    private static Pair<String, StaticExecuteEnvironment> getVacuumAnalyzeDDL(String table, SQLSyntax syntax) {
        return new Pair<>(syntax.getAnalyze(table), StaticExecuteEnvironmentImpl.NOREADONLY);
    }

    public void vacuumAnalyzeSessionTable(String table, OperationOwner owner) throws SQLException {
//        (isInTransaction()? "" :"VACUUM ") + по идее не надо так как TRUNCATE делается
        Pair<String, StaticExecuteEnvironment> ddl = getVacuumAnalyzeDDL(table, syntax);
        executeDDL(ddl.first, ddl.second, owner);
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

    private void assertConnectionLock() {
        ServerLoggers.assertLog(connectionLock.isWriteLocked(), "CONNECTION SHOULD BY LOCKED");
    }

    public boolean isDisabledNestLoop;
    public void setEnableNestLoop(ExConnection connection, OperationOwner owner, boolean on) throws SQLException {
        assertConnectionLock();
        assert syntax.supportsDisableNestedLoop();

        if(problemInTransaction != null) { // если возникла проблема в транзакции ругнется
            assert on;
            return;
        }

        isDisabledNestLoop = !on;
        Statement statement = createSingleStatement(connection.sql);
        try {
            statement.execute("SET enable_nestloop=" + (on ? "on" : "off"));
        } catch (SQLException e) {
            logger.error(statement.toString());
            throw e;
        } finally {
            statement.close();
        }
    }

    private boolean useDeadLockPriority;
    private Long deadLockPriority;
    public void setDeadLockPriority(ExConnection connection, OperationOwner owner, Long deadLockPriority) throws SQLException {
        assert isInTransaction();
        assert useDeadLockPriority;

        if(problemInTransaction != null) { // если возникла проблема в транзакции ругнется
            assert deadLockPriority == null;
            return;
        }

        this.deadLockPriority = deadLockPriority;
        Statement statement = createSingleStatement(connection.sql);
        try {
            statement.execute(syntax.getDeadlockPriority(deadLockPriority));
        } catch (SQLException e) {
            logger.error(statement.toString());
            throw e;
        } finally {
            statement.close();
        }
    }

    private ThreadLocal<Integer> noQueryLimit = new ThreadLocal<>();

    public void pushNoQueryLimit() {
        Integer prevValue = noQueryLimit.get();
        noQueryLimit.set(prevValue == null ? 1 : prevValue + 1);
    }

    public void popNoQueryLimit() {
        Integer prevValue = noQueryLimit.get();
        noQueryLimit.set(prevValue.equals(1) ? null : prevValue - 1);
    }

    public boolean isNoQueryLimit() {
        return noQueryLimit.get() != null;
    }

    private ThreadLocal<Integer> volatileStats = new ThreadLocal<>();

    public boolean isVolatileStats() {
        return volatileStats.get() != null;
    }

    public void pushVolatileStats(OperationOwner owner) throws SQLException {
        Integer prevValue = volatileStats.get();
        volatileStats.set(prevValue == null ? 1 : prevValue + 1);
    }

    public void popVolatileStats(OperationOwner opOwner) throws SQLException {
        Integer prevValue = volatileStats.get();
        volatileStats.set(prevValue.equals(1) ? null : prevValue - 1);
    }

    private ThreadLocal<Integer> noHandled = new ThreadLocal<>();

    // если вообще нет обработки handled exception'ов
    public void pushNoHandled() {
        Integer prevValue = noHandled.get();
        noHandled.set(prevValue == null ? 1 : prevValue + 1);
    }
    
    public boolean isNoHandled() {
        return noHandled.get() != null;
    }
    
    public void popNoHandled() {
        Integer prevValue = noHandled.get();
        noHandled.set(prevValue.equals(1) ? null : prevValue - 1);
    }

    private ThreadLocal<Integer> noTransactTimeout = new ThreadLocal<>();

    public void pushNoTransactTimeout() {
        Integer prevValue = noTransactTimeout.get();
        noTransactTimeout.set(prevValue == null ? 1 : prevValue + 1);
    }

    public boolean isNoTransactTimeout() {
        return noTransactTimeout.get() != null;
    }

    public void popNoTransactTimeout() {
        Integer prevValue = noTransactTimeout.get();
        noTransactTimeout.set(prevValue.equals(1) ? null : prevValue - 1);
    }

    private Boolean forcedCancel = null; //true: interrupt, false: cancel

    public void setForcedCancel(boolean interrupt) {
        forcedCancel = interrupt;
    }

    public Boolean isForcedCancel() {
        if(forcedCancel != null) {
            boolean interrupt = forcedCancel;
            forcedCancel = null;
            return interrupt;
        }
        return null;
    }

    public void executeDDL(String DDL) throws SQLException {
        executeDDL(DDL, StaticExecuteEnvironmentImpl.EMPTY);
    }

    private void executeDDL(String DDL, OperationOwner owner) throws SQLException {
        executeDDL(DDL, StaticExecuteEnvironmentImpl.EMPTY, owner);
    }

    public void executeDDL(String DDL, StaticExecuteEnvironment env) throws SQLException {
        executeDDL(DDL, env, OperationOwner.unknown);
    }
    
    private void executeDDL(String DDL, StaticExecuteEnvironment env, OperationOwner owner) throws SQLException {
        executeDDL(DDL, env, owner, RegisterChange.VOID);
    }
    private void executeDDL(String DDL, StaticExecuteEnvironment env, OperationOwner owner, RegisterChange change) throws SQLException {
        lockRead(owner);

        Statement statement = null;
        ExConnection connection = null;

        Result<Throwable> firstException = new Result<>();
        try {
            connection = getConnection();

            env.before(this, connection, DDL, owner);

            lockConnection(owner);

            statement = createSingleStatement(connection.sql);

            statement.execute(DDL);

        } catch (Throwable e) {
            logger.error((statement == null ? "PREPARING STATEMENT" : statement.toString()) + " " + e.getMessage());
            firstException.set(e);
        }
        
        afterStatementExecute(firstException, DDL, env, connection, statement, owner, change, -1);
    }

    private <OE, S extends DynamicExecEnvSnapshot<OE, S>> int executeDML(SQLExecute<OE, S> execute) throws SQLException, SQLHandledException {
        return executeDML(execute.command, execute.owner, execute.tableOwner, execute.params, execute.queryExecEnv, execute.outerEnv, execute.pureTime, execute.transactTimeout, execute.registerChange);
    }

    private static Map<Integer, Boolean> explainUserMode = MapFact.getGlobalConcurrentHashMap();
    private static Map<Integer, Boolean> explainNoAnalyzeUserMode = MapFact.getGlobalConcurrentHashMap();
    private static Map<Integer, Boolean> loggerDebugEnabled = MapFact.getGlobalConcurrentHashMap();
    private static Map<Integer, Boolean> userVolatileStats = MapFact.getGlobalConcurrentHashMap();

    public static void setExplainAnalyzeMode(Integer user, Boolean mode) {
        explainUserMode.put(user, mode != null && mode);
    }

    public static void setExplainMode(Integer user, Boolean mode) {
        explainNoAnalyzeUserMode.put(user, mode != null && mode);
    }

    public static void setLoggerDebugEnabled(Integer user, Boolean enabled) {
        loggerDebugEnabled.put(user, enabled != null && enabled);
    }
    
    public static void setVolatileStats(Integer user, Boolean enabled, OperationOwner owner) throws SQLException {
        userVolatileStats.put(user, enabled != null && enabled);
    }

    public boolean getVolatileStats() {
        Integer currentUser = userProvider.getCurrentUser();
        if(currentUser == null)
            return false;
        return getVolatileStats(currentUser);
    }

    public boolean explainAnalyze() {
        Integer currentUser = userProvider.getCurrentUser();
        if(currentUser == null)
            return false;
        Boolean eam = explainUserMode.get(currentUser);
        return eam != null && eam;
    }

    public boolean explainNoAnalyze() {
        Integer currentUser = userProvider.getCurrentUser();
        if(currentUser == null)
            return false;
        Boolean ea = explainNoAnalyzeUserMode.get(currentUser);
        return ea != null && ea;
    }

    public boolean isLoggerDebugEnabled() {
        Integer currentUser = userProvider.getCurrentUser();
        if(currentUser == null)
            return false;
        Boolean lde = loggerDebugEnabled.get(currentUser);
        return lde != null && lde;
    }
    
    public boolean getVolatileStats(Integer user) {
        Boolean vs = userVolatileStats.get(user);
        return vs != null && vs;
    }

    private static long safeValueOf(String s) {
        long result = Long.MAX_VALUE;
        try {
            result = Long.valueOf(s);
        } catch (NumberFormatException e) {
        }
        return result;
    }
    
    // причины медленных запросов:
    // Postgres (но могут быть и другие)
    // 1. Неравномерная статистика:
    // а) разная статистика в зависимости от значений поля - например несколько огромных приходных инвойсов и много расходныех
    // б) большое количество NULL значений (скажем 0,999975) - например признак своей компании в множестве юрлиц, тогда становится очень большая дисперсия (то есть тогда либо не компания, и результат 0, или компания и результат большой 100к, при этом когда применяется selectivity он ессно round'ся и 0-100к превращается в 10, что неправильно в общем случае)  
    // Лечится только разнесением в разные таблицы / по разным классам (когда это возможно)
    // Postgres - иногда может быть большое время планирования, но пока проблема была локальная на других базах не повторялась
    public int executeExplain(PreparedStatement statement, boolean noAnalyze, boolean dml) throws SQLException {
        long l = System.currentTimeMillis();
        long actualTime = System.currentTimeMillis() - l;
        Integer rows = null;
        try (ResultSet result = statement.executeQuery()) {
            int thr = Settings.get().getExplainThreshold();
            int i = 0;
            String row = null;
            String prevRow = null;
            List<String> out = new ArrayList<>();
            while (result.next()) {
                prevRow = row;
                row = (String) result.getObject("QUERY PLAN");

                Pattern pt = Pattern.compile(" rows=((\\d)+) ");
                Matcher matcher = pt.matcher(row);
                long est = 0;
                long act = -1;
                int m = 0;
                while (matcher.find()) {
                    if (m == 0)
                        est = safeValueOf(matcher.group(1));
                    if (m == 1) { // 2-е соответствие
                        act = safeValueOf(matcher.group(1));
                        break;
                    }
                    m++;
                }

                if (!noAnalyze && dml && (i == 1 || i == 2) && rows == null && act >= 0) // второй ряд (первый почему то всегда 0) или 3-й (так как 2-й может быть Buffers:)
                    rows = (int) act;
                i++;

                Pattern tpt = Pattern.compile("actual time=(((\\d)+)[.]((\\d)+))[.][.](((\\d)+)[.]((\\d)+))");
                matcher = tpt.matcher(row);
                double rtime = 0.0; // never executed
                if (matcher.find()) {
                    rtime = Double.valueOf(matcher.group(6));
                }

                String mark = "";
                double diff = ((double) act) / ((double) est);
                if (act > 500) {
                    if (diff > 4)
                        mark += "G";
                    else if (diff < 0.25)
                        mark += "L";
                    if (rtime > thr * 10)
                        mark += "T";
                } else if (rtime > thr)
                    mark += "t";
                out.add(BaseUtils.padr(mark, 2) + row);
            }

            if (row != null) {
                Double rtime = getTime("Total runtime: (((\\d)+)[.]((\\d)+))", row);
                Double ptime = null;
                if (rtime == null) { // >= 9.4
                    rtime = getTime("Execution time: (((\\d)+)[.]((\\d)+))", row);
                    if (prevRow != null)
                        ptime = getTime("Planning time: (((\\d)+)[.]((\\d)+))", prevRow);
                }
                double ttime = BaseUtils.nullAdd(rtime, ptime);
                if (noAnalyze || thr == 0 || ttime >= thr) {
                    explainLogger.info(statement.toString() + " disabled nested loop : " + isDisabledNestLoop + " actual time : " + actualTime);
                    explainLogger.info(ExecutionStackAspect.getStackString());
                    if (Settings.get().isExplainJavaStack())
                        explainLogger.info(ExceptionUtils.getStackTrace());
                    for (String outRow : out)
                        explainLogger.info(outRow);
                } //else {
                //  explainLogger.info(rtime);
                //}
            }
        }

        if(rows==null)
            return 0;
//        if(rows==0) // INSERT'ы и UPDATE'ы почему-то всегда 0 лепят (хотя не всегда почему-то)
//            return 100;
        return rows;
    }

    private Double getTime(String ptString, String ptRow) {
        Pattern pt = Pattern.compile(ptString);
        Matcher matcher = pt.matcher(ptRow);
        Double rtime = null;
        if(matcher.find()) {
            rtime = Double.valueOf(matcher.group(1));
        }
        return rtime;
    }

    private enum Problem {
        EXCEPTION, CLOSED
    }
    private Problem problemInTransaction = null;

    private Throwable handle(SQLException e, String message, ExConnection connection) {
        return handle(e, message, false, connection, true);
    }

    private Throwable handle(Throwable t, String message, boolean isTransactTimeout, ExConnection connection, boolean errorPrivate) {
        if(!(t instanceof SQLException))
            return t;
        
        SQLException e = (SQLException)t;            
        if(message == null)
            message = "PREPARING STATEMENT";
        
//        fifo.add("E"  + getCurrentTimeStamp() + " " + this + " " + e.getStackTrace());

        boolean inTransaction = isInTransaction();
        if(inTransaction && syntax.hasTransactionSavepointProblem())
            problemInTransaction = Problem.EXCEPTION;

        SQLHandledException handled = null;
        boolean deadLock = false;
        if(syntax.isUpdateConflict(e) || (deadLock = syntax.isDeadLock(e))) {
            handled = new SQLConflictException(!deadLock);
            sqlConflictLogger.info((inTransaction ? "TRANSACTION " : "") + " " + handled.toString() + message);
        }

        if(syntax.isUniqueViolation(e))
            handled = new SQLUniqueViolationException(false);

        if(syntax.isTimeout(e))
            handled = new SQLTimeoutException(isTransactTimeout, isForcedCancel());
        
        if(syntax.isConnectionClosed(e)) {
            handled = new SQLClosedException(connection.sql, inTransaction, e, errorPrivate);
            problemInTransaction = Problem.CLOSED;
        }

        if(handled != null) {
            handLogger.info((inTransaction ? "TRANSACTION " : "") + " " + handled.toString() + message);
            return handled;
        }
        
        logger.error(message + " " + e.getMessage()); // duplicate keys валится при : неправильный вывод классов в таблицах (см. SessionTable.assertCheckClasses), неправильном неявном приведении типов (от широкого к узкому, DataClass.containsAll), проблемах с округлениями, недетерминированные ORDER функции (GROUP LAST и т.п.), нецелостной базой (значения классов в базе не правильные)
        return e;
    }


    private void lockConnection(boolean needLock, OperationOwner owner) {
        if(syntax.hasJDBCTimeoutMultiThreadProblem() && owner != OperationOwner.debug) { // в debug'е нет смысла lock'ать
            if(needLock)
                connectionLock.writeLock().lock();
            else
                connectionLock.readLock().lock();
        }
    }
    private void unlockConnection(boolean needLock, OperationOwner owner) {
        if(syntax.hasJDBCTimeoutMultiThreadProblem() && owner != OperationOwner.debug) {
            if(needLock)
                connectionLock.writeLock().unlock();
            else
                connectionLock.readLock().unlock();
        }
    }
    // когда в принципе используются statement'ы, чтобы им случайно не повесился timeout
    private void lockConnection(OperationOwner owner) {
        lockConnection(false, owner);
    }
    private void unlockConnection(OperationOwner owner) {
        unlockConnection(false, owner);
    }

    private void afterStatementExecute(Result<Throwable> firstException, final String command, final StaticExecuteEnvironment env, final ExConnection connection, final Statement statement, final OperationOwner owner, final RegisterChange registerChange, final int rowsChange) throws SQLException {
        if(connection != null) {
            if (statement != null)
                runSuppressed(new SQLRunnable() {
                    public void run() throws SQLException {
                        statement.close();
                    }
                }, firstException);

            runSuppressed(new SQLRunnable() {
                public void run() throws SQLException {
                    unlockConnection(owner);
                }
            }, firstException);

            if(env != null)
                runSuppressed(new SQLRunnable() {
                    public void run() throws SQLException {
                        env.after(SQLSession.this, connection, command, owner);
                    }
                }, firstException);

            runSuppressed(new SQLRunnable() {
                public void run() throws SQLException {
                    returnConnection(connection, owner);
                }
            }, firstException);
        }

        runSuppressed(new SQLRunnable() {
            public void run() throws SQLException {
                if(registerChange != null)
                    registerChange.register(SQLSession.this, rowsChange);
            }
        }, firstException);

        unlockRead();

        finishExceptions(firstException);
    }

    // lockRead есть, плюс assert что синхронизировано в рамках одной таблицы
    private void registerChange(String table, TableOwner tableOwner, int rows, TableChange tableChange) {
        if(tableChange == TableChange.UPDATE)
            return;

        if(Settings.get().isDisableRegisterChanges())
            return;

        temporaryTablesLock.lock();
        try {
            registerTransactionChange(table);
            int delta;
            if(tableChange == TableChange.ADD) {
                assert rows == -1;
                delta = 0;
                sessionTablesCount.put(table, 0);
            } else
            if(tableChange == TableChange.REMOVE) {
                assert rows == -1;
                delta = -sessionTablesCount.remove(table);
            } else {
                delta = (tableChange == TableChange.INSERT ? rows : -rows);
                sessionTablesCount.put(table, sessionTablesCount.get(table) + delta);
            }
            totalSessionTablesCount += delta;
            assert privateConnection != null;
            if(privateConnection != null) {
                if (totalSessionTablesCount > privateConnection.maxTotalSessionTablesCount)
                    privateConnection.maxTotalSessionTablesCount = totalSessionTablesCount;
                privateConnection.lastTempTablesActivity = System.currentTimeMillis();
            }
        } finally {
            temporaryTablesLock.unlock();
        }
    }
    public static RegisterChange register(Table table, TableOwner tableOwner, TableChange tableChange) {
        if(table instanceof SessionTable)
            return register(table.name, tableOwner, tableChange);
        else
            return RegisterChange.VOID;
    }

    public static RegisterChange register(final String table, final TableOwner tableOwner, final TableChange tableChange) {
        return new RegisterChange() {
            public void register(SQLSession sql, int result) {
                sql.registerChange(table, tableOwner, result, tableChange);
            }
        };
    }

    @StackMessage("message.sql.execute")
    private int executeDML(@ParamMessage String command, OperationOwner owner, TableOwner tableOwner, RegisterChange registerChange) throws SQLException {
        lockRead(owner);
        Statement statement = null;
        ExConnection connection = null;
        int result = 0;

        Result<Throwable> firstException = new Result<>();
        try {
            connection = getConnection();

            lockConnection(owner);

            statement = createSingleStatement(connection.sql);

            result = statement.executeUpdate(command);
        } catch (Throwable e) {
            logger.error((statement == null ? "PREPARING STATEMENT" : statement.toString()) + " " + e.getMessage());
            firstException.set(e);
        }

        afterStatementExecute(firstException, command, null, connection, statement, owner, registerChange, result);

        return result;
    }

    public boolean outStatement = false;
    
    private static long getMemoryLimit() {
        return Runtime.getRuntime().maxMemory() / Settings.get().getQueryRowCountOptDivider(); // 0.05
    }
    
    public void debugExecute(String select) throws SQLException {
        ExConnection connection = getConnection();
        try (Statement statement = connection.sql.createStatement()) {
            statement.execute(select);
        }
    }

    // системные вызовы
    public <K,V> void executeSelect(String select, OperationOwner owner, StaticExecuteEnvironment env, ImRevMap<K, String> keyNames, final ImMap<String, ? extends Reader> keyReaders, ImRevMap<V, String> propertyNames, ImMap<String, ? extends Reader> propertyReaders, ResultHandler<K, V> handler) throws SQLException, SQLHandledException {
        executeSelect(select, owner, env, MapFact.<String, ParseInterface>EMPTY(), 0, keyNames, keyReaders, propertyNames, propertyReaders, false, handler);
    }
    public <K,V> ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> executeSelect(String select, OperationOwner owner, StaticExecuteEnvironment env, ImMap<String, ParseInterface> paramObjects, int transactTimeout, ImRevMap<K, String> keyNames, final ImMap<String, ? extends Reader> keyReaders, ImRevMap<V, String> propertyNames, ImMap<String, ? extends Reader> propertyReaders) throws SQLException, SQLHandledException {
        return executeSelect(select, owner, env, paramObjects, transactTimeout, keyNames, keyReaders, propertyNames, propertyReaders, false);
    }
    public <K,V> ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> executeSelect(String select, OperationOwner owner, StaticExecuteEnvironment env, ImMap<String, ParseInterface> paramObjects, int transactTimeout, ImRevMap<K, String> keyNames, final ImMap<String, ? extends Reader> keyReaders, ImRevMap<V, String> propertyNames, ImMap<String, ? extends Reader> propertyReaders, boolean disableNestLoop) throws SQLException, SQLHandledException {
        ReadAllResultHandler<K, V> result = new ReadAllResultHandler<>();
        executeSelect(select, owner, env, paramObjects, transactTimeout, keyNames, keyReaders, propertyNames, propertyReaders, disableNestLoop, result);
        return result.terminate();
    }
    public <K,V> void executeSelect(String select, OperationOwner owner, StaticExecuteEnvironment env, ImMap<String, ParseInterface> paramObjects, int transactTimeout, ImRevMap<K, String> keyNames, final ImMap<String, ? extends Reader> keyReaders, ImRevMap<V, String> propertyNames, ImMap<String, ? extends Reader> propertyReaders, boolean disableNestLoop, ResultHandler<K, V> handler) throws SQLException, SQLHandledException {
        executeSelect(new SQLQuery(select, ExecCost.MIN, MapFact.<String, SQLQuery>EMPTY(), env,keyReaders, propertyReaders, false, false), disableNestLoop ? DynamicExecuteEnvironment.DISABLENESTLOOP : DynamicExecuteEnvironment.DEFAULT, owner, paramObjects, transactTimeout, keyNames, propertyNames, handler);
    }

    public <K,V> void executeSelect(SQLQuery query, DynamicExecuteEnvironment queryExecEnv, OperationOwner owner, ImMap<String, ParseInterface> paramObjects, int transactTimeout, ImRevMap<K, String> keyNames, ImRevMap<V, String> propertyNames, ResultHandler<K, V> handler) throws SQLException, SQLHandledException {
        executeSelect(query, queryExecEnv, owner, paramObjects, transactTimeout, new MapResultHandler<>(handler, keyNames, propertyNames));
    }

    public <OE, S extends DynamicExecEnvSnapshot> void executeSelect(SQLQuery query, DynamicExecuteEnvironment queryExecEnv, OperationOwner owner, ImMap<String, ParseInterface> paramObjects, int transactTimeout, ResultHandler<String, String> handler) throws SQLException, SQLHandledException {
        executeCommand(query.fixConcSelect(syntax), queryExecEnv, owner, paramObjects, transactTimeout, handler, DynamicExecuteEnvironment.<OE, S>create(null), PureTime.VOID, true);
    }

    public <OE, S extends DynamicExecEnvSnapshot<OE, S>> int executeDML(@ParamMessage SQLDML command, OperationOwner owner, TableOwner tableOwner, ImMap<String, ParseInterface> paramObjects, DynamicExecuteEnvironment<OE, S> queryExecEnv, OE outerEnv, PureTimeInterface pureTime, int transactTimeout, final RegisterChange registerChange) throws SQLException, SQLHandledException { // public для аспекта
        final Result<Integer> count = new Result<>(0);
        SQLDML.Handler handler = new SQLDML.Handler() {
            public void proceed(Integer result) {
                count.set(result);
            }

            public void afterProceed() { // lockRead
                registerChange.register(SQLSession.this, count.result); // не в proceed чтобы lock'и внутри не ставить
            }
        };
        executeCommand(command, queryExecEnv, owner, paramObjects, transactTimeout, handler, DynamicExecuteEnvironment.<OE,S>create(outerEnv), pureTime, true);
        return count.result;
    }

    // можно было бы сделать аспектом, но во-первых вся логика before / after аспектная и реализована в явную, плюс непонятно как соотносить snapshot с mutable объектом (env)
    public <H, OE, S extends DynamicExecEnvSnapshot<OE, S>> void executeCommand(@ParamMessage final SQLCommand<H> command, final DynamicExecuteEnvironment<OE, S> queryExecEnv, final OperationOwner owner, ImMap<String, ParseInterface> paramObjects, int transactTimeout, H handler, DynamicExecEnvOuter<OE, S> outerEnv, PureTimeInterface pureTime, boolean setRepeatDate) throws SQLException, SQLHandledException {
        if(command.command.length() > Settings.get().getQueryLengthLimit())
            throw new SQLTooLongQueryException(command.command);

        S snapEnv = queryExecEnv.getSnapshot(command, transactTimeout, outerEnv);
        try {
            snapEnv.beforeOuter(command, this, paramObjects, owner, pureTime);

            SQLHandledException t;
            try {
                long timeStarted = System.currentTimeMillis();

                executeCommand(command, snapEnv, owner, paramObjects, handler);

                long runTime = System.currentTimeMillis() - timeStarted;
                pureTime.add(runTime); // тут теоретически вопрос с AnalyzeAspect'ом мог бы быть, но так как DML сразу выполняется не проблема
                queryExecEnv.succeeded(command, snapEnv, runTime, outerEnv);

                return;
            } catch (SQLClosedException e) {
                t = e;
                if (e.isInTransaction() || !tryRestore(owner, e.connection, e.isPrivate))
                    throw e;
            } catch (SQLHandledException e) {
                t = e;
                if (!e.repeatCommand())
                    throw e; // update conflict'ы, deadlock'и, transactTimeout'ы

                queryExecEnv.failed(command, snapEnv);

                if (problemInTransaction != null) { // транзакция все равно прервана
                    assert isInTransaction();
                    throw e;
                }
            }

            // повторяем
            try {
                synchronized (attemptCountMap) {
                    incAttemptCount(attemptCountMap, t.getDescription(false));
                }
                if(setRepeatDate)
                    startTransaction = Calendar.getInstance().getTime().getTime();
                executeCommand(command, queryExecEnv, owner, paramObjects, transactTimeout, handler, snapEnv, pureTime, false);
            } finally {
                if(setRepeatDate)
                    startTransaction = null;
                synchronized (attemptCountMap) {
                    decAttemptCount(attemptCountMap, t.getDescription(false));
                }
            }
        } finally {
            snapEnv.afterOuter(this, owner);
        }
    }

    public static Map<String, Integer> incAttemptCount(Map<String, Integer> attemptCountMap, String description) {
        Integer count = attemptCountMap.get(description);
        attemptCountMap.put(description, count == null ? 1 : (count + 1));
        return attemptCountMap;
    }

    public static Map<String, Integer> decAttemptCount(Map<String, Integer> attemptCountMap, String description) {
        Integer count = attemptCountMap.get(description);
        if (count != null) {
            if (count <= 1)
                attemptCountMap.remove(description);
            else
                attemptCountMap.put(description, count - 1);
        }
        return attemptCountMap;
    }

    public String getAttemptCountMap() {
        String result = "";
        if (attemptCountMap != null)
            for (Map.Entry<String, Integer> entry : attemptCountMap.entrySet()) {
                result += entry.getValue() + "(" + entry.getKey() + ") ";
            }
        return result.isEmpty() ? "0" : result.trim();
    }


    public static int getAttemptCountSum(Map<String, Integer> attemptCountMap) {
        int result = 0;
        for(Map.Entry<String, Integer> entry : attemptCountMap.entrySet()) {
            result += entry.getValue();
        }
        return result;
    }

    public void saveToDbForDebug(DynamicExecEnvSnapshot<?, ?> snapEnv, ImMap<String, ParseInterface> paramObjects) throws ClassNotFoundException, SQLException, SQLHandledException, InstantiationException, IllegalAccessException {
        ImSet<SessionTable> materializedTables = snapEnv.getMaterializedQueries().values().mapColSetValues(new GetValue<SessionTable, MaterializedQuery>() {
            public SessionTable getMapValue(MaterializedQuery value) {
                return new SessionTable(value.tableName, value.keyFields, value.propFields);
            }});
        ImSet<SessionTable> paramTables = paramObjects.values().filterCol(new SFunctionSet<ParseInterface>() {
            public boolean contains(ParseInterface element) {
                return element.getSessionTable() != null;
            }}).mapMergeSetValues(new GetValue<SessionTable, ParseInterface>() {
            public SessionTable getMapValue(ParseInterface value) {
                return value.getSessionTable();
            }});

        SessionTable.saveToDBForDebug(SetFact.addExclSet(materializedTables, paramTables), this);
    }

    // SQLAnalyzeAspect
    @StackMessage("message.sql.execute")
    public <H> void executeCommand(@ParamMessage final SQLCommand<H> command, final DynamicExecEnvSnapshot snapEnv, final OperationOwner owner, ImMap<String, ParseInterface> paramObjects, H handler) throws SQLException, SQLHandledException {
        lockRead(owner);

        long runTime = 0;
        final Result<ReturnStatement> returnStatement = new Result<>();
        PreparedStatement statement = null;
        ExConnection connection = null;

        final String string = command.getString();

        Result<Throwable> firstException = new Result<>();
        StaticExecuteEnvironment env = command.env;

        Savepoint savepoint = null;
        try {
            snapEnv.beforeConnection(this, owner);

            connection = getConnection();

            env.before(this, connection, string, owner);

            lockConnection(snapEnv.needConnectionLock(), owner);

            snapEnv.beforeStatement(this, connection, string, owner);

            if(useDeadLockPriority && command.isDML()) {
                assert isInTransaction();

                int secondsFromStart = getSecondsFromTransactStart();
                if(secondsFromStart > 0) {
                    long currentPriority = Math.round(Math.log(secondsFromStart) / Math.log(2.0));
                    if (deadLockPriority == null || deadLockPriority < currentPriority) // оптимизация
                        setDeadLockPriority(connection, owner, currentPriority); // предполагается, что deadLockPriority очистит endTransaction
                }
            }

            if(isInTransaction() && syntax.hasTransactionSavepointProblem()) {
                Integer count;
                if (Settings.get().isUseSavepointsForExceptions() && snapEnv.hasRepeatCommand() && (count = attemptCountMap.get(SQLTimeoutException.ADJUSTTRANSTIMEOUT)) != null && count >= 1)
                    savepoint = connection.sql.setSavepoint();
            }

            Result<Integer> length = new Result<>();
            statement = getStatement(command, paramObjects, connection, syntax, snapEnv, returnStatement, length);
            snapEnv.beforeExec(statement, this);

            long started = System.currentTimeMillis();

            try {
                try {
                    executingStatement = statement;
                    command.execute(statement, handler, this);
                } finally {
                    executingStatement = null;
                }
            } finally {
                runTime = System.currentTimeMillis() - started;

                connection.registerExecute(length.result, runTime);
            }
        } catch (Throwable t) { // по хорошему тоже надо через runSuppressed, но будут проблемы с final'ами
            t = handle(t, statement != null ? statement.toString() : "PREPARING STATEMENT", snapEnv.isTransactTimeout(), connection, privateConnection != null);
            firstException.set(t);

            if(savepoint != null && t instanceof SQLHandledException && ((SQLHandledException)t).repeatCommand()) {
                assert problemInTransaction == Problem.EXCEPTION;
                final ExConnection fConnection = connection; final Savepoint fSavepoint = savepoint;
                runSuppressed(new SQLRunnable() {
                    public void run() throws SQLException, SQLHandledException {
                        fConnection.sql.rollback(fSavepoint);
                        problemInTransaction = null;
                    }
                }, firstException);
                savepoint = null;
            }
        } finally {
            if(savepoint != null && problemInTransaction == null) { // если был exception в транзакции никакой release уже не сработает
                final ExConnection fConnection = connection; final Savepoint fSavepoint = savepoint;
                runSuppressed(new SQLRunnable() {
                    public void run() throws SQLException, SQLHandledException {
                        fConnection.sql.releaseSavepoint(fSavepoint);
                    }
                }, firstException);
            }
        }

        afterExStatementExecute(owner, env, snapEnv, connection, runTime, returnStatement, statement, string, command, handler, firstException);
    }

    private <H> void afterExStatementExecute(final OperationOwner owner, final StaticExecuteEnvironment env, final DynamicExecEnvSnapshot execInfo, final ExConnection connection, final long runTime, final Result<ReturnStatement> returnStatement, final PreparedStatement statement, final String string, final SQLCommand<H> command, final H handler, Result<Throwable> firstException) throws SQLException, SQLHandledException {
        if(connection != null) {
            runSuppressed(new SQLRunnable() {
                public void run() throws SQLException {
                    execInfo.afterStatement(SQLSession.this, connection, string, owner);
                }
            }, firstException);

            runSuppressed(new SQLRunnable() {
                public void run() throws SQLException {
                    env.after(SQLSession.this, connection, string, owner);
                }
            }, firstException);

            if (statement != null)
                runSuppressed(new SQLRunnable() {
                    public void run() throws SQLException {
                        returnStatement.result.proceed(statement, runTime);
                    }
                }, firstException);

            runSuppressed(new SQLRunnable() {
                public void run() throws SQLException {
                    unlockConnection(execInfo.needConnectionLock(), owner);
                }
            }, firstException);

            runSuppressed(new SQLRunnable() {
                public void run() throws SQLException {
                    returnConnection(connection, owner);
                }
            }, firstException);
        }

        runSuppressed(new SQLRunnable() {
            public void run() throws SQLException {
                execInfo.afterConnection(SQLSession.this, owner);
            }
        }, firstException);

        runSuppressed(new SQLRunnable() {
              @Override
              public void run() throws SQLException, SQLHandledException {
                  command.afterExecute(handler);
              }
           }
        , firstException);

        unlockRead();

        finishHandledExceptions(firstException);
    }

    private static final Parser<Object, Object> dataParser = new Parser<Object, Object>() {

        public ParseInterface getParse(Object key, Field field, SQLSyntax syntax) {
            return new TypeObject(key, field.type, syntax, true);
        }

        public ParseInterface getKeyParse(Object key, KeyField field, SQLSyntax syntax) {
            return getParse(key, field, syntax);
        }

        public ParseInterface getPropParse(Object prop, PropertyField field, SQLSyntax syntax) {
            if(prop == null)
                return new AbstractParseInterface.Null(field.type);
            else
                return getParse(prop, field, syntax);
        }
    };
            
    public void insertBatchRecords(GlobalTable table, ImMap<ImMap<KeyField, Object>, ImMap<PropertyField, Object>> rows, OperationOwner opOwner) throws SQLException {
        insertBatchRecords(table.getName(syntax), table.keys, rows, dataParser, opOwner, register(table, TableOwner.global, TableChange.INSERT));
    }

    private static final Parser<DataObject, ObjectValue> sessionParser = new Parser<DataObject, ObjectValue>() {
        public ParseInterface getKeyParse(DataObject key, KeyField field, SQLSyntax syntax) {
            return key.getParse(field, syntax);
        }

        public ParseInterface getPropParse(ObjectValue prop, PropertyField field, SQLSyntax syntax) {
            return prop.getParse(field, syntax);
        }
    };

    public void insertSessionBatchRecords(String table, ImOrderSet<KeyField> keys, ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows, OperationOwner opOwner, TableOwner tableOwner) throws SQLException {
        checkTableOwner(table, tableOwner);

        insertBatchRecords(syntax.getSessionTableName(table), keys, rows, sessionParser, opOwner, register(table, tableOwner, TableChange.INSERT));
    }
    
    private interface Parser<K, V> {
        ParseInterface getKeyParse(K key, KeyField field, SQLSyntax syntax);
        ParseInterface getPropParse(V prop, PropertyField field, SQLSyntax syntax);
    } 

    private <K, V> void insertBatchRecords(String table, ImOrderSet<KeyField> keys, ImMap<ImMap<KeyField, K>, ImMap<PropertyField, V>> rows, Parser<K, V> parser, OperationOwner opOwner, RegisterChange registerChange) throws SQLException {
        if(rows.isEmpty())
            return;

        ImOrderSet<PropertyField> properties = rows.getValue(0).keys().toOrderSet();

        Pair<String, StaticExecuteEnvironment> command = getInsertDML(table, keys, properties, syntax);

        PreparedStatement statement = null;
        ExConnection connection = null;

        lockRead(opOwner);

        Result<Throwable> firstException = new Result<>();
        int result = 0;
        try {
            connection = getConnection();

            command.second.before(this, connection, command.first, opOwner);

            lockConnection(opOwner);

            statement = connection.sql.prepareStatement(command.first);
            insertBatch(statement, keys, rows, parser, properties, syntax);

            result = rows.size();

        } catch (Throwable e) {
            while(e instanceof BatchUpdateException) {
                logger.error(statement == null ? "PREPARING STATEMENT" : statement.toString() + " BATCH UPDATE : " + e.getMessage());
                SQLException next = ((BatchUpdateException) e).getNextException();
                if(next == null)
                    break;
                else
                    e = next;
            }
            logger.error(statement == null ? "PREPARING STATEMENT" : statement.toString() + " " + e.getMessage());
            firstException.set(e);
        }
        
        afterStatementExecute(firstException, command.first, command.second, connection, statement, opOwner, registerChange, result);
    }

    private static Pair<String, StaticExecuteEnvironment> getInsertDML(String table, ImOrderSet<KeyField> keys, ImOrderSet<PropertyField> properties, final SQLSyntax syntax) {
        ImOrderSet<Field> fields = SetFact.addOrderExcl(keys, properties);

        final MStaticExecuteEnvironment mEnv = StaticExecuteEnvironmentImpl.mEnv();
        String insertString = fields.toString(Field.nameGetter(syntax), ",");
        String valueString = fields.toString(new GetValue<String, Field>() {
            public String getMapValue(Field value) {
                return value.type.writeDeconc(syntax, mEnv);
            }}, ",");

        if(insertString.length()==0) {
            assert valueString.length()==0;
            insertString = "dumb";
            valueString = "0";
        }

        return new Pair<>("INSERT INTO " + table + " (" + insertString + ") VALUES (" + valueString + ")", mEnv.finish());
    }

    private static <K, V> void insertBatch(PreparedStatement statement, ImOrderSet<KeyField> keys, ImMap<ImMap<KeyField, K>, ImMap<PropertyField, V>> rows, Parser<K, V> parser, ImOrderSet<PropertyField> properties, SQLSyntax syntax) throws SQLException {

        for(int i=0,size=rows.size();i<size;i++) {
            ParamNum paramNum = new ParamNum();
            ImMap<KeyField, K> rowKey = rows.getKey(i);
            for(KeyField key : keys) // чтобы сохранить порядок
                parser.getKeyParse(rowKey.get(key), key, syntax).writeParam(statement, paramNum, syntax);
            ImMap<PropertyField, V> rowValue = rows.getValue(i);
            for(PropertyField property : properties) // чтобы сохранить порядок
                parser.getPropParse(rowValue.get(property), property, syntax).writeParam(statement, paramNum, syntax);
            statement.addBatch();
        }

        statement.executeBatch();
    }

    private void insertParamRecord(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, OperationOwner owner, TableOwner tableOwner) throws SQLException {
        checkTableOwner(table, tableOwner);
        
        String insertString = "";
        String valueString = "";

        int paramNum = 0;
        MExclMap<String, ParseInterface> params = MapFact.<String, ParseInterface>mExclMapMax(keyFields.size()+propFields.size());

        // пробежим по KeyFields'ам
        for (int i=0,size=keyFields.size();i<size;i++) {
            KeyField key = keyFields.getKey(i);
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + key.getName(syntax);
            DataObject keyValue = keyFields.getValue(i);
            if (keyValue.isString(syntax))
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + keyValue.getString(syntax);
            else {
                String prm = "qxprm" + (paramNum++) + "nx";
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + prm;
                params.exclAdd(prm, new TypeObject(keyValue, key, syntax));
            }
        }

        for (int i=0,size=propFields.size();i<size;i++) {
            PropertyField property = propFields.getKey(i);
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + property.getName(syntax);
            ObjectValue fieldValue = propFields.getValue(i);
            if (fieldValue.isString(syntax))
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + fieldValue.getString(syntax);
            else {
                String prm = "qxprm" + (paramNum++) + "nx";
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + prm;
                params.exclAdd(prm, new TypeObject((DataObject) fieldValue, property, syntax));
            }
        }

        if(insertString.length()==0) {
            assert valueString.length()==0;
            insertString = "dumb";
            valueString = "0";
        }

        try {
            executeDML(new SQLDML("INSERT INTO " + table.getName(syntax) + " (" + insertString + ") VALUES (" + valueString + ")", ExecCost.MIN, MapFact.<String, SQLQuery>EMPTY(), StaticExecuteEnvironmentImpl.EMPTY, false), owner, tableOwner, params.immutable(), DynamicExecuteEnvironment.DEFAULT, null, PureTime.VOID, 0, register(table, tableOwner, TableChange.INSERT));
        } catch (SQLHandledException e) {
            throw new UnsupportedOperationException(); // по идее ни deadlock'а, ни update conflict'а, ни timeout'а
        }
    }

    public void insertRecord(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, TableOwner owner, OperationOwner opOwner) throws SQLException {
        checkTableOwner(table, owner);

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
            insertParamRecord(table, keyFields, propFields, opOwner, owner);
            return;
        }

        String insertString = "";
        String valueString = "";

        // пробежим по KeyFields'ам
        for (int i=0,size=keyFields.size();i<size;i++) { // нужно сохранить общий порядок, поэтому без toString
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + keyFields.getKey(i).getName(syntax);
            valueString = (valueString.length() == 0 ? "" : valueString + ',') + keyFields.getValue(i).getString(syntax);
        }

        // пробежим по Fields'ам
        for (int i=0,size=propFields.size();i<size;i++) { // нужно сохранить общий порядок, поэтому без toString
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + propFields.getKey(i).getName(syntax);
            valueString = (valueString.length() == 0 ? "" : valueString + ',') + propFields.getValue(i).getString(syntax);
        }

        if(insertString.length()==0) {
            assert valueString.length()==0;
            insertString = "dumb";
            valueString = "0";
        }

        executeDML("INSERT INTO " + table.getName(syntax) + " (" + insertString + ") VALUES (" + valueString + ")", opOwner, owner, register(table, owner, TableChange.INSERT));
    }

    public boolean isRecord(Table table, ImMap<KeyField, DataObject> keyFields, OperationOwner owner) throws SQLException, SQLHandledException {

        // по сути пустое кол-во ключей
        return new Query<KeyField, String>(MapFact.<KeyField, KeyExpr>EMPTYREV(),
                table.join(DataObject.getMapExprs(keyFields)).getWhere()).execute(this, owner).size() > 0;
    }

    public void ensureRecord(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, TableOwner tableOwner, OperationOwner owner) throws SQLException, SQLHandledException {
        if (!isRecord(table, keyFields, owner))
            insertRecord(table, keyFields, propFields, tableOwner, owner);
    }

    public void updateRecords(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, OperationOwner owner, TableOwner tableOwner) throws SQLException, SQLHandledException {
        updateRecords(table, false, keyFields, propFields, owner, tableOwner);
    }

    public int updateRecordsCount(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, OperationOwner owner, TableOwner tableOwner) throws SQLException, SQLHandledException {
        return updateRecords(table, true, keyFields, propFields, owner, tableOwner);
    }

    private int updateRecords(Table table, boolean count, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, OperationOwner owner, TableOwner tableOwner) throws SQLException, SQLHandledException {
        if(!propFields.isEmpty()) // есть запись нужно Update лупить
            return updateRecords(new ModifyQuery(table, new Query<>(table.getMapKeys(), Where.TRUE, keyFields, ObjectValue.getMapExprs(propFields)), owner, tableOwner));
        if(count)
            return isRecord(table, keyFields, owner) ? 1 : 0;
        return 0;

    }

    public boolean insertRecord(Table table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, boolean update, TableOwner tableOwner, OperationOwner owner) throws SQLException, SQLHandledException {
        if(update && isRecord(table, keyFields, owner)) {
            updateRecords(table, keyFields, propFields, owner, tableOwner);
            return false;
        } else {
            insertRecord(table, keyFields, propFields, tableOwner, owner);
            return true;
        }
    }

    public Object readRecord(Table table, ImMap<KeyField, DataObject> keyFields, PropertyField field, OperationOwner owner) throws SQLException, SQLHandledException {
        // по сути пустое кол-во ключей
        return new Query<>(MapFact.<KeyField, KeyExpr>EMPTYREV(),
                table.join(DataObject.getMapExprs(keyFields)).getExpr(field), "result", Where.TRUE).
                execute(this, owner).singleValue().get("result");
    }

    public void truncate(GlobalTable table, OperationOwner owner) throws SQLException {
        truncate(table.getName(syntax), owner, register(table, TableOwner.global, TableChange.REMOVE));
    }
    
    public void truncateSession(String table, OperationOwner owner, TableOwner tableOwner) throws SQLException {
        checkTableOwner(table, tableOwner);

        truncate(syntax.getSessionTableName(table), owner, register(table, tableOwner, TableChange.REMOVE));
    }

    public void truncate(String table, OperationOwner owner, RegisterChange registerChange) throws SQLException {
//        executeDML("TRUNCATE " + syntax.getSessionTableName(table));
        if(problemInTransaction == null) {
            executeDDL("TRUNCATE TABLE " + table, StaticExecuteEnvironmentImpl.NOREADONLY, owner, registerChange); // нельзя использовать из-за : в транзакции в режиме "только чтение" нельзя выполнить TRUNCATE TABLE
//            executeDML("DELETE FROM " + syntax.getSessionTableName(table), owner, tableOwner);
        }
    }

    public int getSessionCount(String table, OperationOwner opOwner) throws SQLException {
        return getCount(syntax.getSessionTableName(table), opOwner);
    }

    public int getCount(Table table, OperationOwner opOwner) throws SQLException {
        return getCount(table.getName(syntax), opOwner);
    }

    public int getCount(String table, OperationOwner opOwner) throws SQLException {
//        executeDML("TRUNCATE " + syntax.getSessionTableName(table));
        try {
            return (Integer)executeSelect("SELECT COUNT(*) AS cnt FROM " + table, opOwner, StaticExecuteEnvironmentImpl.EMPTY, MapFact.<String, ParseInterface>EMPTY(), 0, MapFact.singletonRev("cnt", "cnt"), MapFact.singleton("cnt", IntegerClass.instance), MapFact.<String, String>EMPTYREV(), MapFact.<String, Reader>EMPTY()).singleKey().singleValue();
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public <X> int deleteKeyRecords(Table table, ImMap<KeyField, X> keys, OperationOwner owner, TableOwner tableOwner) throws SQLException {
        checkTableOwner(table, tableOwner);
        
        String deleteWhere = keys.toString(new GetKeyValue<String, KeyField, X>() {
            public String getMapValue(KeyField key, X value) {
                return key.getName(syntax) + "=" + value;
            }}, " AND ");

        return executeDML("DELETE FROM " + table.getName(syntax) + (deleteWhere.length() == 0 ? "" : " WHERE " + deleteWhere), owner, tableOwner, register(table, tableOwner, TableChange.DELETE));
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
    public void readSingleValues(SessionTable table, Result<ImMap<KeyField, Object>> keyValues, Result<ImMap<PropertyField, Object>> propValues, Result<DistinctKeys<KeyField>> statKeys, Result<ImMap<PropertyField, PropStat>> statProps, OperationOwner opOwner) throws SQLException {
        ImSet<KeyField> tableKeys = table.getTableKeys();
        MStaticExecuteEnvironment env = StaticExecuteEnvironmentImpl.mEnv();

        MExclMap<String, String> mReadKeys = MapFact.mExclMap();
        mReadKeys.exclAdd(getCnt(""), syntax.getCount("*"));
        if(tableKeys.size() > 1)
            for(KeyField field : tableKeys) {
                String fieldName = field.getName(syntax);
                mReadKeys.exclAdd(getCntDist(field.getName()), syntax.getCountDistinct(fieldName));
                field.type.readDeconc(syntax.getAnyValueFunc() + "(" + fieldName + ")", field.getName(), mReadKeys, syntax, env);
            }
        else {
            statKeys.set(new DistinctKeys<>(tableKeys.isEmpty() ? MapFact.<KeyField, Stat>EMPTY() : MapFact.singleton(tableKeys.single(), new Stat(table.count))));
            if (table.properties.isEmpty()) {
                keyValues.set(MapFact.<KeyField, Object>EMPTY());
                propValues.set(MapFact.<PropertyField, Object>EMPTY());
                statProps.set(MapFact.<PropertyField, PropStat>EMPTY());
                return;
            }
        }
        ImMap<String, String> readKeys = mReadKeys.immutable();

        MExclMap<String, String> mReadProps = MapFact.mExclMap();
        for(PropertyField field : table.properties) {
            String fieldName = field.getName(syntax);
            mReadProps.exclAdd(getCntDist(field.getName()), syntax.getCountDistinct(fieldName));
            mReadProps.exclAdd(getCnt(field.getName()), syntax.getCount(fieldName));
            field.type.readDeconc(syntax.getAnyValueFunc() + "(" + fieldName + ")", field.getName(), mReadProps, syntax, env);
        }
        ImMap<String, String> readProps = mReadProps.immutable();

        String select = "SELECT " + SQLSession.stringExpr(readKeys, readProps) + " FROM " + table.getName(syntax);

        lockRead(opOwner);

        ExConnection connection = null;
        Statement statement = null;

        Result<Throwable> firstException = new Result<>();
        try {
            connection = getConnection();

            lockConnection(opOwner);

            statement = createSingleStatement(connection.sql);

            try (ResultSet result = statement.executeQuery(select)) {
                boolean next = result.next();
                assert next;

                int totalCnt = readInt(result.getObject(getCnt("")));
                if (tableKeys.size() > 1) {
                    ImFilterValueMap<KeyField, Object> mKeyValues = tableKeys.mapFilterValues();
                    ImFilterValueMap<KeyField, Stat> mStatKeys = tableKeys.mapFilterValues();
                    for (int i = 0, size = tableKeys.size(); i < size; i++) {
                        KeyField tableKey = tableKeys.get(i);
                        String fieldName = tableKey.getName();
                        int cnt = readInt(result.getObject(getCntDist(fieldName)));
                        if (cnt == 1)
                            mKeyValues.mapValue(i, tableKey.type.read(result, syntax, fieldName));
                        mStatKeys.mapValue(i, new Stat(cnt));
                    }
                    keyValues.set(mKeyValues.immutableValue());
                    statKeys.set(new DistinctKeys<>(mStatKeys.immutableValue()));
                } else
                    keyValues.set(MapFact.<KeyField, Object>EMPTY());

                ImFilterValueMap<PropertyField, Object> mvPropValues = table.properties.mapFilterValues();
                ImFilterValueMap<PropertyField, PropStat> mvStatProps = table.properties.mapFilterValues();
                for (int i = 0, size = table.properties.size(); i < size; i++) {
                    PropertyField tableProperty = table.properties.get(i);
                    String fieldName = tableProperty.getName();
                    int cntDistinct = readInt(result.getObject(getCntDist(fieldName)));
                    if (cntDistinct == 0)
                        mvPropValues.mapValue(i, null);
                    if (cntDistinct == 1 && totalCnt == readInt(result.getObject(getCnt(fieldName))))
                        mvPropValues.mapValue(i, tableProperty.type.read(result, syntax, fieldName));
                    mvStatProps.mapValue(i, new PropStat(new Stat(cntDistinct)));
                }
                propValues.set(mvPropValues.immutableValue());
                statProps.set(mvStatProps.immutableValue());

                assert !result.next();
            }
        } catch (Throwable e) {
            logger.error(statement == null ? "PREPARING STATEMENT" : statement.toString() + " " + e.getMessage());
            firstException.set(e);
        }

        afterStatementExecute(firstException, select, null, connection, statement, opOwner, null, -1);
    }
    
    private void checkTableOwner(String table, TableOwner owner) {
        WeakReference<TableOwner> wCurrentOwner = sessionTablesMap.get(table); // не синхронизирована ???
        TableOwner currentOwner;
        if(owner == TableOwner.debug)
            return;
        
        if(wCurrentOwner == null || (currentOwner = wCurrentOwner.get()) == null) {
            if(owner != TableOwner.none)
                ServerLoggers.assertLog(false, "UPDATED RETURNED TABLE : " + table + " " + owner);
        } else
            ServerLoggers.assertLog(currentOwner == owner, "UPDATED FOREIGN TABLE : " + table + " " + currentOwner + " " + owner);
    }
    private void checkTableOwner(Table table, TableOwner owner) {
        if(table instanceof SessionTable)
            checkTableOwner(table.getName(), owner);
        else
            ServerLoggers.assertLog(owner == TableOwner.global || owner == TableOwner.debug, "THERE SHOULD BE NO OWNER FOR GLOBAL TABLE " + table.getName() + " " + owner);
    }
    private void checkTableOwner(ModifyQuery modify) {
        checkTableOwner(modify.table, modify.owner);
    }

    public int deleteRecords(ModifyQuery modify) throws SQLException, SQLHandledException {
        checkTableOwner(modify);
        
        if(modify.isEmpty()) // иначе exception кидает
            return 0;

        return executeDML(modify.getDelete(syntax, userProvider));
    }

    public int updateRecords(ModifyQuery modify) throws SQLException, SQLHandledException {
        checkTableOwner(modify);
        return executeDML(modify.getUpdate(syntax, userProvider));
    }

    public int insertSelect(ModifyQuery modify) throws SQLException, SQLHandledException {
        checkTableOwner(modify);
        return executeDML(modify.getInsertSelect(syntax, userProvider));
    }
    public int insertSessionSelect(SQLExecute execute, final ERunnable out) throws SQLException, SQLHandledException {
        // out.run();

        try {
            return executeDML(execute);
        } catch(Throwable t) {
            Result<Throwable> firstException = new Result<>();
            firstException.set(t);

            if(!isInTransaction() && t instanceof SQLUniqueViolationException)
                runSuppressed(new SQLRunnable() {
                    public void run() throws SQLException, SQLHandledException {
                        try {
                            out.run();
                        } catch (Exception e) {
                            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
                        }
                    }
                }, firstException);

            finishHandledExceptions(firstException);
            throw new UnsupportedOperationException();
        }

    }
    public int insertSessionSelect(String name, final IQuery<KeyField, PropertyField> query, final QueryEnvironment env, final TableOwner owner) throws SQLException, SQLHandledException {
        checkTableOwner(name, owner);

        return insertSessionSelect(ModifyQuery.getInsertSelect(syntax.getSessionTableName(name), query, env, owner, syntax, userProvider, null, register(name, owner, TableChange.INSERT)), new ERunnable() {
            public void run() throws Exception {
                query.outSelect(SQLSession.this, env);
            }});
    }

    public int insertLeftSelect(ModifyQuery modify, boolean updateProps, boolean insertOnlyNotNull) throws SQLException, SQLHandledException {
        checkTableOwner(modify);
//        modify.getInsertLeftQuery(updateProps, insertOnlyNotNull).outSelect(this, modify.env);
        return executeDML(modify.getInsertLeftKeys(syntax, userProvider, updateProps, insertOnlyNotNull));
    }

    public int modifyRecords(ModifyQuery modify) throws SQLException, SQLHandledException {
        try {
            return modifyRecords(modify, new Result<Integer>());
        } catch (SQLUniqueViolationException e) {
            throw e.raceCondition();
        }
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
            if (!isRecord(modify.table, MapFact.<KeyField, DataObject>EMPTY(), modify.getOwner()))
                result = insertSelect(modify);
        } else
            result = insertLeftSelect(modify, false, insertOnlyNotNull);
        int updated = updateRecords(modify);
        proceeded.set(result + updated);
        return result;
    }
    
    @Override
    protected void onExplicitClose(OperationOwner owner, boolean syncedOnClient) throws SQLException {
        assert syncedOnClient; // предполагается что весь SQLSession синхронизирован

        lockWrite(owner);
        temporaryTablesLock.lock();

        try {
            sqlSessionMap.remove(this);

            if(privateConnection !=null) {
                try {
                    removeUnusedTemporaryTables(true, owner);
                } finally {
                    ServerLoggers.assertLog(sessionTablesMap.isEmpty(), "AT CLOSE USED TABLES SHOULD NOT EXIST " + this);
                    connectionPool.returnPrivate(this, privateConnection);
//                    System.out.println(this + " " + privateConnection + " -> NULL " + " " + sessionTablesMap.keySet() + ExceptionUtils.getStackTrace());
//                    sqlHandLogger.info("Returning backend PID: " + ((PGConnection) privateConnection.sql).getBackendPID());
                    privateConnection = null;
                }
            }
            ServerLoggers.exinfoLog("SQL SESSION CLOSE " + this);
        } finally {
            temporaryTablesLock.unlock();
            unlockWrite();
        }
    }
    
    public boolean tryRestore(OperationOwner opOwner, Connection connection, boolean isPrivate) {
        lockRead(opOwner);
        try {
            temporaryTablesLock.lock();
            try {
                if(isPrivate || Settings.get().isCommonUnique()) // вторая штука перестраховка, но такая опция все равно не используется
                    return false;
                // повалился common
                assert sessionTablesMap.isEmpty();
                return connectionPool.restoreCommon(connection);
            } catch(Throwable e) {
                return false;
            } finally {
                temporaryTablesLock.unlock();
            }
        } finally {
            unlockRead();
        }
    }

    private static final LRUWSVSMap<Connection, PreParsedStatement, ParsedStatement> statementPool = new LRUWSVSMap<>(LRUUtil.G1);

    public interface ReturnStatement {
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
    
    private void checkSessionTables(ImMap<String, ParseInterface> paramObjects) {
        for(ParseInterface paramObject : paramObjects.valueIt()) {
            SessionTable sessionTable = paramObject.getSessionTable();
            if(sessionTable != null)
                checkSessionTable(sessionTable);
        }
    }
    
    public void checkSessionTable(SessionTable table) {
        WeakReference<TableOwner> sessionTable = sessionTablesMap.get(table.getName());
        ServerLoggers.assertLog(sessionTable != null && sessionTable.get() != null, "USED RETURNED TABLE : " + table.getName());
    }

    private PreparedStatement getStatement(SQLCommand command, ImMap<String, ParseInterface> paramObjects, final ExConnection connection, SQLSyntax syntax, DynamicExecEnvSnapshot snapEnv, Result<ReturnStatement> returnStatement, Result<Integer> length) throws SQLException {

        StaticExecuteEnvironment env = command.env;
        boolean poolPrepared = !env.isNoPrepare() && !Settings.get().isDisablePoolPreparedStatements() && command.getString().length() > Settings.get().getQueryPrepareLength();

        checkSessionTables(paramObjects);

        ImMap<String, String> reparse;
        if(BusinessLogics.useReparse && (reparse = BusinessLogics.reparse.get()) != null) { // временный хак
            paramObjects = paramObjects.addExcl(reparse.mapValues(new GetValue<ParseInterface, String>() {
                public ParseInterface getMapValue(final String value) {
                    return new StringParseInterface() {
                        public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
                            return value;
                        }
                    };
                }
            }));
        }

        final PreParsedStatement parse = command.preparseStatement(poolPrepared, paramObjects, syntax, isVolatileStats(), snapEnv.getMaterializedQueries(), env.hasRecursion());

        ParsedStatement parsed = null;
        if(poolPrepared)
            parsed = statementPool.get(connection.sql, parse);
        if(parsed==null) {
            parsed = parse.parseStatement(connection.sql, syntax);
            if(poolPrepared) {
                final ParsedStatement fParsed = parsed;
                returnStatement.set(new ReturnStatement() {
                    public void proceed(PreparedStatement statement, long runTime) throws SQLException {
                        if(runTime > Settings.get().getQueryPrepareRunTime())
                            statementPool.put(connection.sql, parse, fParsed);
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
                paramObjects.get(param).writeParam(parsed.statement, paramNum, syntax);
        } catch (SQLException e) {
            returnStatement.result.proceed(parsed.statement, 0);
            throw e;
        }

        length.set(parsed.length);
        return parsed.statement;
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

    public Thread getActiveThread() {
        if(activeThread == null)
            return null;

        return activeThread.get();
    }

    private WeakReference<Thread> setActiveThread() {
        WeakReference<Thread> prev = activeThread;
        activeThread = new WeakReference<>(Thread.currentThread());
        return prev;
    }

    private void dropActiveThread(WeakReference<Thread> prev) {
        activeThread = prev;
    }

    private void runLockReadOperation(SQLRunnable run) throws SQLException, SQLHandledException {
        if (isClosed())
            return;

        boolean locked = tryLockRead(OperationOwner.unknown);
        if(!locked)
            return;

        try {
            locked = temporaryTablesLock.tryLock();
            if(!locked)
                return;

            try {

                if (isClosed())
                    return;

                if (privateConnection != null) {
                    ServerLoggers.assertLog(!isInTransaction(), "SHOULD NOT BE IN TRANSACTION"); // иначе lockRead не получился бы
                    run.run();
                }
            } finally {
                temporaryTablesLock.unlock();
            }
        } finally {
            unlockRead(true);
        }
    }

    private long getTimeStamp(String table) {
        Long timeStamp = lastReturnedStamp.get(table);
        if(timeStamp == null)
            return 0;
        return timeStamp;
    }

    public static void cleanTemporaryTables() throws SQLException, SQLHandledException {

        List<TableUsage> notUsedTables = new ArrayList<>();
        Result<Integer> recentlyUsedTables = new Result<>(0);
        long beforeTime = System.currentTimeMillis() - Settings.get().getTempTablesTimeThreshold() * 1000;

        Map<SQLSession, Integer> sessions = new IdentityHashMap<>(sqlSessionMap);

        for(SQLSession sqlSession : sessions.keySet()) {
            sqlSession.readNotUsedTables(notUsedTables, recentlyUsedTables, beforeTime);
        }

        int max = Settings.get().getTempTablesCountThreshold() * sessions.size();
        int tempTablesKeep = max - recentlyUsedTables.result;

        // вырезаем все таблицы кроме "последних" tempTablesKeep
        if(tempTablesKeep < 0)
            tempTablesKeep = 0;

        int size = notUsedTables.size();
        int removeFirst = size - tempTablesKeep;
        if(removeFirst < 0)
            removeFirst = 0;

        ServerLoggers.exInfoLogger.info("CLEAN TEMP TABLES : not used - " + size + ", drop - " + removeFirst + ", max  - " + max + ", used / recently used - " + recentlyUsedTables.result);

        if(removeFirst == 0)
            return;

        Collections.sort(notUsedTables); // least first

        for(int i=0;i<removeFirst;i++) {
            TableUsage usage = notUsedTables.get(i);
            usage.sql.cleanNotUsedTable(usage.table, usage.timeStamp);
        }
    }

    private static class TableUsage implements Comparable<TableUsage> {
        private final SQLSession sql;
        private final String table;
        private final long timeStamp;

        public TableUsage(SQLSession sql, String table, long timeStamp) {
            this.sql = sql;
            this.table = table;
            this.timeStamp = timeStamp;
        }

        @Override
        public int compareTo(TableUsage o) {
            return Long.compare(timeStamp, o.timeStamp);
        }
    }

    // так как нет обращений к базе, собираем таблицы группой, drop'аем таблицы по отдельности


    private void readNotUsedTables(final List<TableUsage> notUsedTables, final Result<Integer> recentlyUsedTables, final long beforeTime) throws SQLException, SQLHandledException {
        runLockReadOperation(new SQLRunnable() {
            public void run() throws SQLException, SQLHandledException {
                for(String table : privateConnection.temporary.getTables()) {
                    long timeStamp;
                    if (!sessionTablesMap.containsKey(table) && (timeStamp = getTimeStamp(table)) < beforeTime)
                        notUsedTables.add(new TableUsage(SQLSession.this, table, timeStamp));
                    else
                        recentlyUsedTables.set(recentlyUsedTables.result + 1);
                }
            }
        });
    }

    private void cleanNotUsedTable(final String table, final long timeStamp) throws SQLException, SQLHandledException {
        runLockReadOperation(new SQLRunnable() {
            public void run() throws SQLException, SQLHandledException {
                if(!sessionTablesMap.containsKey(table) && timeStamp == getTimeStamp(table)) { // double check, not used and the same time stamp
                    if(privateConnection.temporary.getTables().contains(table)) { // тут теоретически raceCondition'ов может быть очень много
                        lastReturnedStamp.remove(table);
                        privateConnection.temporary.removeTable(table);
                        dropTemporaryTableFromDB(table);
                    }
                }
            }
        });
    }

    public static void dumpScores() throws SQLException, SQLHandledException {
        Map<SQLSession, Integer> sessions = new IdentityHashMap<>(sqlSessionMap);

        List<ConnectionUsage> usedSQLConnections = new ArrayList<>();

        for(SQLSession session : sessions.keySet()) {
            session.readRestartConnection(usedSQLConnections, true);
        }

        Collections.sort(usedSQLConnections); // least first

        for(ConnectionUsage usedSQLConnection : usedSQLConnections) {
            ServerLoggers.exInfoLogger.info("DUMP - " + usedSQLConnection.description);
        }
    }

    public static void restartConnections(Result<Double> prevStart) throws SQLException, SQLHandledException {
        try {
            Double leftFromPrevStart = prevStart.result;

            Map<SQLSession, Integer> sessions = new IdentityHashMap<>(sqlSessionMap);

            double connectionsRestart = Settings.get().getPercentRestartConnections() * sessions.size() / 100;
            connectionsRestart += leftFromPrevStart;
            int removeFirst = (int) Math.round(connectionsRestart);
            leftFromPrevStart = connectionsRestart - removeFirst;

            prevStart.set(leftFromPrevStart);

            ServerLoggers.sqlConnectionLogger.info("Global restart connections : count - " + sessions.size() + ", remove first : " + removeFirst);

            if(removeFirst == 0)
                return;

            List<ConnectionUsage> usedSQLConnections = new ArrayList<>();

            for(SQLSession session : sessions.keySet()) {
                session.readRestartConnection(usedSQLConnections, false);
            }

            Collections.sort(usedSQLConnections); // least first

            Map<ConnectionPool, Connection> notUsedNewConnections = new IdentityHashMap<>();
            try {
                int succeeded = 0;
                for (ConnectionUsage usage : usedSQLConnections) {
                    if (usage.sql.restartConnection(usage.score, notUsedNewConnections))
                        succeeded++;
                    if (succeeded >= removeFirst)
                        break;
                }
            } finally {
                for (Connection notUsedConnection : notUsedNewConnections.values())
                    notUsedConnection.close();
            }
        } catch (Throwable t) { // временно так, чтобы не останавливался restartConnections никогда
            logger.error("GLOBAL RESTART CONNECTIONS ERROR", t);
        }
    }

    private static class ConnectionUsage implements Comparable<ConnectionUsage> {
        private final SQLSession sql;
        private final double score;
        private final String description;

        public ConnectionUsage(SQLSession sql, double score, String description) {
            this.sql = sql;
            this.score = score;
            this.description = description;
        }

        @Override
        public int compareTo(ConnectionUsage o) {
            return Double.compare(o.score, score);
        }
    }

    private void readRestartConnection(final List<ConnectionUsage> usedConnections, final boolean readDescription) throws SQLException, SQLHandledException {
        runLockReadOperation(new SQLRunnable() { // assertLock
            public void run() throws SQLException, SQLHandledException {
                Result<String> description = new Result<>(null);
                usedConnections.add(new ConnectionUsage(SQLSession.this, getScore(readDescription ? description : null, false), description.result));
            }
        });
    }

    private double getScore(Result<String> description, boolean lockedWrite) throws SQLException {
        Settings settings = Settings.get();

        int usedTablesSize = getSessionTablesCountAll(lockedWrite);

        long currentTime = System.currentTimeMillis();
        long timeStarted = (currentTime - privateConnection.timeStarted);
        long lastTempTablesActivity = (currentTime - privateConnection.lastTempTablesActivity);

        long maxUsedTables = privateConnection.maxTotalSessionTablesCount;

        double timeScore = privateConnection.timeScore;
        double lengthScore = privateConnection.lengthScore;

        double timeStartedAverageMax = settings.getTimeStartedAverageMaxCoeff() * settings.getPeriodRestartConnections() * 1000 * 100.0 / settings.getPercentRestartConnections();

        double usedAntiScore = Math.pow((double) usedTablesSize / (double) settings.getUsedTempRowsAverageMax(), settings.getUsedTempRowsDegree()) -
                Math.pow((double) lastTempTablesActivity / Settings.get().getLastTempTablesActivityAverageMax(), settings.getTimeStartedDegree()); // если не было активности долгое время перестартовываем

        double score = timeScore + lengthScore
                - BaseUtils.max(usedAntiScore, 0.0) +
                Math.pow((double)timeStarted/timeStartedAverageMax, settings.getTimeStartedDegree()) +
                Math.pow((double)maxUsedTables/(double)settings.getMaxUsedTempRowsAverageMax(), settings.getMaxUsedTempRowsDegree());

        if(description != null) {
            int backend = ((PGConnection)privateConnection.sql).getBackendPID();
            description.set("Backend : " + backend + ", Info : (" + userProvider.getLogInfo()+ "), SCORE : " + score + "(used temp table rows : " + usedTablesSize + ", time started : " + timeStarted + ", max used temp table rows : " + maxUsedTables + ", last temp tables activity : " + lastTempTablesActivity + ", time score : " + timeScore + ", length score : " + lengthScore + ")");
        }

        return score;
    }

    private boolean restartConnection(double score, Map<ConnectionPool, Connection> notUsedConnections) throws SQLException, SQLHandledException {
        if(isClosed())
            return false;

        OperationOwner owner = OperationOwner.unknown;

        Connection newConnection = notUsedConnections.remove(connectionPool);
        if(newConnection == null)
            newConnection = connectionPool.newRestartConnection(); // за пределами lockWrite чтобы не задерживать connection

        boolean noError = false;
        try {
            boolean locked = tryLockWrite(owner);
            try {
                Result<String> description = new Result<>();
                if(!locked || isClosed() || privateConnection == null || score > getScore(description, true)) { // double check - score упал
                    notUsedConnections.put(connectionPool, newConnection); // если не использовали возвращаем
                    noError = true;
                    return false;
                }

                long timeRestartStarted = System.currentTimeMillis();

                // сначала переносим временные таблицы
                try {
                    for (String table : sessionTablesMap.keySet()) {
                        SQLTemporaryPool.FieldStruct struct = privateConnection.temporary.getStruct(table);
                        uploadTableToConnection(table, struct, newConnection, OperationOwner.unknown);
                    }
                } catch (SQLException | SQLHandledException t) { // если проблема в SQL, пишем в лог / игнорируем
                    logger.error("RESTART CONNECTION ERROR : " + description.result, t);
                    return false;
                }

                // закрываем старое соединение
                privateConnection.sql.close();

                noError = true;

                // если все ок, подменяем connection
                privateConnection.sql = newConnection;
                privateConnection.timeScore = 0;
                privateConnection.lengthScore = 0;
                long currentTime = System.currentTimeMillis();
                privateConnection.timeStarted = currentTime;
                privateConnection.lastTempTablesActivity = currentTime;
                privateConnection.maxTotalSessionTablesCount = totalSessionTablesCount;

                // очищаем pool
                Set<String> tables = new HashSet<>(privateConnection.temporary.getTables());
                for(String table : tables)
                    if(!sessionTablesMap.containsKey(table)) { // not used
                        lastReturnedStamp.remove(table);
                        privateConnection.temporary.removeTable(table);
                    }

                int newBackend = ((PGConnection)newConnection).getBackendPID();
                ServerLoggers.sqlConnectionLogger.info("RESTART CONNECTION : Time : " + (System.currentTimeMillis() - timeRestartStarted) + ", New : " + newBackend + ", " + description.result);
            } finally {
                if(locked)
                    unlockWrite(true);
            }
        } finally {
            if(!noError)
                newConnection.close();
        }
        return true;
    }

    // чисто для миграции таблиц, небольшой copy paste, но из-за error-handling'а и locking'а делать рефакторинг себе дороже
    private void createTemporaryTable(Connection connection, String table, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, OperationOwner owner) throws SQLException {
        createTemporaryTable(connection, typePool, syntax, table, keys, properties, owner);
    }
    private static void createTemporaryTable(Connection connection, TypePool typePool, SQLSyntax syntax, String table, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, OperationOwner owner) throws SQLException {
        Pair<String, StaticExecuteEnvironment> result = getCreateDDL(table, keys, properties, syntax);
        executeDDL(result, connection, typePool, owner);
    }
    private void vacuumAnalyzeSessionTable(Connection connection, String table, OperationOwner owner) throws SQLException {
        vacuumAnalyzeSessionTable(table, connection, typePool, syntax, owner);
    }
    private static void vacuumAnalyzeSessionTable(String table, Connection connection, TypePool typePool, SQLSyntax syntax, OperationOwner owner) throws SQLException {
        Pair<String, StaticExecuteEnvironment> ddl = getVacuumAnalyzeDDL(table, syntax);
        executeDDL(ddl, connection, typePool, owner);
    }

    private static void executeDDL(Pair<String, StaticExecuteEnvironment> result, Connection connection, TypePool typePool, OperationOwner owner) throws SQLException {
        String command = result.first;
        StaticExecuteEnvironment env = result.second;

        Statement statement = null;

        Result<Throwable> firstException = new Result<>();
        try {
            env.before(connection, typePool, command, owner);

            statement = createSingleStatement(connection);

            statement.execute(command);

        } catch (Throwable e) {
            logger.error(statement == null ? "PREPARING STATEMENT" : statement.toString() + " " + e.getMessage());
            firstException.set(e);
        }

        afterStatementExecute(connection, typePool, owner, result, statement, firstException);
    }
    public void insertSessionBatchRecords(Connection connection, String table, ImMap<ImMap<KeyField, Object>, ImMap<PropertyField, Object>> rows, OperationOwner opOwner) throws SQLException {
        insertBatchRecords(connection, typePool, syntax, syntax.getSessionTableName(table), rows, dataParser, opOwner);
    }
    private static void insertBatchRecords(final Connection connection, final TypePool typePool, SQLSyntax syntax, String table, ImMap<ImMap<KeyField, Object>, ImMap<PropertyField, Object>> rows, Parser<Object, Object> parser, final OperationOwner opOwner) throws SQLException {
        if(rows.isEmpty())
            return;

        ImOrderSet<KeyField> keys = rows.getKey(0).keys().toOrderSet();
        ImOrderSet<PropertyField> properties = rows.getValue(0).keys().toOrderSet();

        final Pair<String, StaticExecuteEnvironment> dml = getInsertDML(table, keys, properties, syntax);

        PreparedStatement statement = null;

        Result<Throwable> firstException = new Result<>();
        try {
            dml.second.before(connection, typePool, dml.first, opOwner);

            statement = connection.prepareStatement(dml.first);
            insertBatch(statement, keys, rows, parser, properties, syntax);

        } catch (Throwable e) {
            logger.error(statement == null ? "PREPARING STATEMENT" : statement.toString() + " " + e.getMessage());
            firstException.set(e);
        }

        afterStatementExecute(connection, typePool, opOwner, dml, statement, firstException);
    }

    private static void afterStatementExecute(final Connection connection, final TypePool typePool, final OperationOwner opOwner, final Pair<String, StaticExecuteEnvironment> dml, final Statement statement, Result<Throwable> firstException) throws SQLException {
        if (statement != null) {
            runSuppressed(new SQLRunnable() {
                public void run() throws SQLException {
                    statement.close();
                }
            }, firstException);
        }

        runSuppressed(new SQLRunnable() {
            public void run() throws SQLException {
                dml.second.after(connection, typePool, dml.first, opOwner);
            }
        }, firstException);

        finishExceptions(firstException);
    }

    public void uploadTableToConnection(final String table, SQLTemporaryPool.FieldStruct fieldStruct, final Connection sqlTo, final OperationOwner owner) throws SQLException, SQLHandledException {
        uploadTableToConnection(table, fieldStruct.keys, fieldStruct.properties, sqlTo, owner);
    }

    private void uploadTableToConnection(final String table, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, final Connection sqlTo, final OperationOwner owner) throws SQLException, SQLHandledException {
        createTemporaryTable(sqlTo, table, keys, properties, owner);

        final Result<Integer> proceeded = new Result<>(0);
        ResultHandler<KeyField, PropertyField> reader = new ReadBatchResultHandler<KeyField, PropertyField>(10000) {
            public void start() {
            }

            public void proceedBatch(ImOrderMap<ImMap<KeyField, Object>, ImMap<PropertyField, Object>> batch) throws SQLException {
                insertSessionBatchRecords(sqlTo, table, batch.getMap(), owner);
                proceeded.set(proceeded.result + batch.size());
            }
        };

        readData(table, keys, properties, reader, owner);

        vacuumAnalyzeSessionTable(sqlTo, table, owner);
    }

    private void readData(String table, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ResultHandler<KeyField, PropertyField> reader, OperationOwner owner) throws SQLException, SQLHandledException {
        ImRevMap<KeyField, String> keyNames = keys.getSet().mapRevValues(Field.<KeyField>nameGetter());
        ImRevMap<PropertyField, String> propNames = properties.mapRevValues(Field.<PropertyField>nameGetter());

        ImRevMap<String, KeyField> revKeyNames = keyNames.reverse();
        ImRevMap<String, PropertyField> revPropNames = propNames.reverse();
        String select = "SELECT " + SQLSession.stringExpr(revKeyNames.mapValues(Field.<KeyField>nameGetter(syntax)),
                revPropNames.mapValues(Field.<PropertyField>nameGetter(syntax))) + " FROM " + syntax.getSessionTableName(table);

        executeSelect(select, owner, StaticExecuteEnvironmentImpl.EMPTY, keyNames, revKeyNames.mapValues(Field.<KeyField>fnTypeGetter()), propNames, revPropNames.mapValues(Field.<PropertyField>fnTypeGetter()), reader);
    }
}
