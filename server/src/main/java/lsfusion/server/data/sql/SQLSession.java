package lsfusion.server.data.sql;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentIdentityWeakHashMap;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentWeakHashMap;
import lsfusion.base.col.heavy.weak.WeakLinkedHashSet;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSVSMap;
import lsfusion.base.lambda.E2Runnable;
import lsfusion.base.lambda.ERunnable;
import lsfusion.base.lambda.Provider;
import lsfusion.server.base.MutableClosedObject;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.base.controller.stack.ParamMessage;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.exec.*;
import lsfusion.server.data.query.exec.materialize.PureTime;
import lsfusion.server.data.query.exec.materialize.PureTimeInterface;
import lsfusion.server.data.query.modify.ModifyQuery;
import lsfusion.server.data.query.result.MapResultHandler;
import lsfusion.server.data.query.result.ReadAllResultHandler;
import lsfusion.server.data.query.result.ReadBatchResultHandler;
import lsfusion.server.data.query.result.ResultHandler;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.connection.ConnectionPool;
import lsfusion.server.data.sql.connection.ExConnection;
import lsfusion.server.data.sql.exception.SQLTimeoutException;
import lsfusion.server.data.sql.exception.*;
import lsfusion.server.data.sql.lambda.SQLRunnable;
import lsfusion.server.data.sql.statement.ParsedStatement;
import lsfusion.server.data.sql.statement.PreParsedStatement;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.sql.table.SQLTemporaryPool;
import lsfusion.server.data.stat.Cost;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.table.*;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeFunc;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.exec.TypePool;
import lsfusion.server.data.type.parse.AbstractParseInterface;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.data.type.parse.StringParseInterface;
import lsfusion.server.data.type.reader.PGObjectReader;
import lsfusion.server.data.type.reader.Reader;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.change.modifier.SessionModifier;
import lsfusion.server.logics.classes.data.ArrayClass;
import lsfusion.server.logics.classes.data.TSVectorClass;
import lsfusion.server.logics.classes.data.file.AJSONClass;
import lsfusion.server.logics.form.stat.struct.plain.JDBCTable;
import lsfusion.server.logics.navigator.controller.env.SQLSessionContextProvider;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.monitor.StatusMessage;
import lsfusion.server.physics.admin.monitor.ThreadDebugInfo;
import lsfusion.server.physics.admin.monitor.sql.SQLDebugInfo;
import lsfusion.server.physics.admin.monitor.sql.SQLThreadInfo;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.DBTable;
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
import java.util.Date;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.server.data.table.IndexOptions.defaultIndexOptions;
import static lsfusion.server.data.table.IndexType.*;
import static lsfusion.server.physics.admin.log.ServerLoggers.*;

public class SQLSession extends MutableClosedObject<OperationOwner> implements AutoCloseable {

    public static class ExecutingStatement {

        public final PreparedStatement statement;
        public Boolean forcedCancel;

        public ExecutingStatement(PreparedStatement statement) {
            this.statement = statement;
        }
    }
    private ExecutingStatement executingStatement;

    private static final Logger logger = ServerLoggers.sqlLogger;
    private static final Logger handLogger = ServerLoggers.sqlHandLogger;
    private static final Logger sqlConflictLogger = ServerLoggers.sqlConflictLogger;

    private static ConcurrentIdentityWeakHashMap<SQLSession, Integer> sqlSessionMap = MapFact.getGlobalConcurrentIdentityWeakHashMap();
    public static ConcurrentHashMap<Long, Long> threadAllocatedBytesAMap = MapFact.getGlobalConcurrentHashMap();
    public static ConcurrentHashMap<Long, Long> threadAllocatedBytesBMap = MapFact.getGlobalConcurrentHashMap();

    private Long startTransaction;
    private Map<String, Integer> attemptCountMap = new HashMap<>();
    public StatusMessage statusMessage;

    public static SQLSession getSQLSessionJava(Thread thread) {
        for(SQLSession sqlSession : sqlSessionMap.keySet()) {
            Thread activeThread = sqlSession.getActiveThread();
            if(activeThread != null && activeThread == thread)
                return sqlSession;
        }
        logger.error(String.format("Failed to interrupt process %s: no private connection found", thread));
        return null;
    }

    public static ExecutingStatement getExecutingStatementSQL(long processId) throws SQLException {
        for(SQLSession sqlSession : sqlSessionMap.keySet()) {
            ExecutingStatement executingStatement = sqlSession.executingStatement;
            if(executingStatement != null && ((PGConnection)executingStatement.statement.getConnection()).getBackendPID() == processId)
                return executingStatement;
        }
        return null;
    }

    public static Map<Integer, SQLThreadInfo> getSQLThreadMap() {
        ConcurrentWeakHashMap<Thread, SQLDebugInfo> sqlDebugInfoMap = SQLDebugInfo.getSqlDebugInfoMap();

        Map<Integer, SQLThreadInfo> sessionMap = new HashMap<>();
        for (SQLSession sqlSession : sqlSessionMap.keySet()) {
            Thread javaThread = sqlSession.getActiveThread();
            SQLDebugInfo sqlDebugInfo = sqlDebugInfoMap.get(javaThread);
            String debugInfo = sqlDebugInfo == null ? null : sqlDebugInfo.toString(sqlSession);

            ExConnection connection = sqlSession.getDebugConnection();
            if (connection != null) {
                sessionMap.put(((PGConnection) connection.sql).getBackendPID(), new SQLThreadInfo(javaThread,
                        sqlSession.threadDebugInfo, sqlSession.isInTransaction(), sqlSession.startTransaction, sqlSession.getAttemptCountMap(),
                        sqlSession.contextProvider.getThreadCurrentUser(), sqlSession.contextProvider.getThreadCurrentComputer(),
                        sqlSession.getExecutingStatement(), sqlSession.isDisabledNestLoop, sqlSession.getQueryTimeout(),
                        debugInfo, sqlSession.statusMessage));
            }
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
        Class threadMXBeanClass = ReflectionUtils.classForName("com.sun.management.ThreadMXBean");
        if (threadMXBeanClass != null && threadMXBeanClass.isInstance(tBean)) {
            long[] allThreadIds = tBean.getAllThreadIds();
            long[] threadAllocatedBytes = ReflectionUtils.getMethodValue(threadMXBeanClass, tBean, "getThreadAllocatedBytes", new Class[]{long[].class}, new Object[]{allThreadIds});
            for (int i=0;i<allThreadIds.length;i++) {
                threadAllocatedBytesBMap.put(allThreadIds[i], threadAllocatedBytes[i]);
            }
        }
    }

    public String getExecutingStatement() {
        return executingStatement == null ? null : executingStatement.statement.toString();
    }

    public static void cancelExecutingStatement(DBManager dbManager, Thread thread, boolean interrupt) throws SQLException, SQLHandledException {
        // having both thread and statement in executing statement is the only way to guarantee that we'll cancel the statement of the required processId
        SQLSession cancelSession = SQLSession.getSQLSessionJava(thread);
        if(cancelSession != null) {
            cancelSession.runSyncActiveThread(thread, () -> {
                ExecutingStatement executingStatement = cancelSession.executingStatement;
                if(executingStatement != null) {
                    ServerLoggers.exinfoLog("SQL SESSION INTERRUPT " + thread + " PROCESS " + thread.getId() + " SQL " + ((PGConnection) executingStatement.statement.getConnection()).getBackendPID() + " QUERY " + executingStatement.statement.toString());

                    // we're trying to cancel executing statement, because ddl task might stop some query in pooled connection that is already used by some other thread
                    executingStatement.forcedCancel = interrupt;
                    executingStatement.statement.cancel();
                }
            });

//                SQLSession sqlSession = dbManager.getStopSql();                
//                ExConnection connection = cancelSession.getDebugConnection();
//                if(connection != null)
//                    int sqlProcessID = ((PGConnection) connection.sql).getBackendPID();
//                sqlSession.executeDDL(sqlSession.syntax.getCancelActiveTaskQuery(sqlProcessID));
        }
    }

    public Integer getQueryTimeout() {
        try {
            return executingStatement == null ? null : executingStatement.statement.getQueryTimeout();
        } catch (SQLException e) {
            return null;
        }
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
                try {
                    ServerLoggers.sqlSuppLog(t);
                } catch (Throwable sl) { // suppress'им логирование, так как runSuppressed не оборачиваются в try finally а важно, что при Interrupt'е надо выполнить остальные                
                }
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

    public SQLSessionContextProvider contextProvider;

    public <F extends Field> Function<F, String> getDeclare(final TypeEnvironment typeEnv) {
        return getDeclare(syntax, typeEnv);
    }
    
    public static <F extends Field> Function<F, String> getDeclare(final SQLSyntax syntax, final TypeEnvironment typeEnv) {
        return value -> value.getDeclare(syntax, typeEnv);
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
                resultConnection = connectionPool.getCommon(this, contextProvider);
            resultConnection.checkClosed();
            resultConnection.updateLogLevel(syntax);
        } catch (Throwable t) {
            if(useCommon)
                temporaryTablesLock.unlock();
            throw Throwables.propagate(t);
        }
        debugConnection = resultConnection;
        return resultConnection;
    }

    public void returnConnection(ExConnection connection, OperationOwner owner) throws SQLException {
        debugConnection = null;
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
    
    private ExConnection debugConnection = null; // executing connection, usually private, but sometimes common
    public ExConnection getDebugConnection() {
        if(debugConnection != null)
            return debugConnection;
        return privateConnection;
    }

    public boolean inconsistent = false;

    public final static String userParam = "adsadaweewuser";
    public final static String authTokenParam = "vkljudfshldhfskjhdkjrraae";
    public final static String isServerRestartingParam = "sdfisserverrestartingpdfdf";
    public final static String computerParam = "fjruwidskldsor";
    public final static String formParam = "yfifybdfnenfykfqyth";
    public final static String connectionParam = "ntreottgjlrkxtybt";
    public final static String isDevParam = "dsiljdsiowee";
    public final static String isLightStartParam = "lsiljdsiowee";
    public final static String inTestModeParam = "gxRbJbuKPuF2ec88Fbio8RJFnxtlPNTgeasEhUX0";
    public final static String projectLSFDirParam = "XiSupdIyJMMwJSY2AyDDzEOHi6W6TLta658BJHG0";

    public final static String limitParam = "sdsnjklirens";

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private ReentrantLock temporaryTablesLock = new ReentrantLock(true);
    private ReentrantReadWriteLock connectionLock = new ReentrantReadWriteLock(true);

    public final ThreadDebugInfo threadDebugInfo;

    public SQLSession(DataAdapter adapter, SQLSessionContextProvider contextProvider) {
        syntax = adapter.syntax;
        connectionPool = adapter;
        typePool = adapter;
        this.contextProvider = contextProvider;
        sqlSessionMap.put(this, 1);
        Thread currentThread = Thread.currentThread();
        threadDebugInfo = new ThreadDebugInfo(currentThread.getName(),
                Settings.get().isStacktraceInSQLSession() ? ThreadUtils.getJavaStack(currentThread.getStackTrace()) : null);
    }

    private void needPrivate() throws SQLException { // получает unique connection
        assertLock();
        if(privateConnection ==null) {
            assert transactionTables.isEmpty();
            privateConnection = connectionPool.getPrivate(this, contextProvider);
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

    public boolean isWriteLockedByCurrentThread() {
        return lock.isWriteLockedByCurrentThread();
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
        if(ACID) // setting parameters before starting / after ending transaction to do not have transaction aborted exception 
            setEnvParams(connection, true, syntax);

        connection.setAutoCommit(!ACID);
        connection.setReadOnly(!ACID);

        if(!ACID)
            setEnvParams(connection, false, syntax);
    }

    public static void setEnvParams(Connection connection, boolean ACID, SQLSyntax syntax) throws SQLException {
        try (Statement statement = createSingleStatement(connection)) {
            syntax.setACID(statement, ACID);
        } catch (SQLException e) { // there can be permission denied, so we'll just suppress that exception
            sqlSuppLog(e);
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
            setActiveThread(tryLock);
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
        dropActiveThread(dropActiveThread);
        lock.readLock().unlock();
//        ServerLoggers.pausableLogStack("UNLOCKREAD " + this);
    }
    
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

        setActiveThread(tryLock);
        writeOwner = owner;

        return true;
    }

    private void unlockWrite() {
        unlockWrite(false);
    }

    private void unlockWrite(boolean dropActiveThread) {
        writeOwner = null;

        dropActiveThread(dropActiveThread);
        lock.writeLock().unlock();
//        ServerLoggers.pausableLogStack("UNLOCKWRITE " + this);
    }
    
    private Integer prevIsolation;
    private long transStartTime;
    public int getSecondsFromTransactStart() {
        assert isInTransaction();
        return (int) ((System.currentTimeMillis() - transStartTime)/1000);
    }

    public void startFakeTransaction(OperationOwner owner) throws SQLException {
        lockWrite(owner);

        explicitNeedPrivate++;

        needPrivate();

        privateConnection.sql.setReadOnly(false);
    }

    public void endFakeTransaction(OperationOwner owner) throws SQLException {
        try {
            privateConnection.sql.setReadOnly(false);

            explicitNeedPrivate--;

            tryCommon(owner, false);
        } finally {
            unlockWrite();
        }
    }

    public void startTransaction(int isolationLevel, OperationOwner owner) throws SQLException, SQLHandledException {
        startTransaction(isolationLevel, owner, new HashMap<>(), false, 0);
    }

    public void startTransaction(int isolationLevel, OperationOwner owner, Map<String, Integer> attemptCountMap, boolean useDeadLockPriority, long applyStartTime) throws SQLException, SQLHandledException {
        lockWrite(owner);
        startTransaction = System.currentTimeMillis();
        this.attemptCountMap = attemptCountMap;
        assert isInTransaction() || transactionTables.isEmpty();
        try {
            if(Settings.get().isApplyVolatileStats())
                pushVolatileStats(owner);
            if(isExplainTemporaryTablesEnabled())
                addFifo("ST");
            if(inTransaction++ == 0) {
                transStartTime = System.currentTimeMillis();

                needPrivate();
                if(isolationLevel > 0) {
                    prevIsolation = privateConnection.sql.getTransactionIsolation();
                    privateConnection.sql.setTransactionIsolation(isolationLevel);
                }
                setACID(privateConnection.sql, true, syntax);

                this.useDeadLockPriority = useDeadLockPriority;
                this.applyStartTime = applyStartTime;
            }
        } catch (SQLException e) {
            handleAndPropagate(e, "START TRANSACTION");
        }
    }

    public void handleAndPropagate(SQLException e, String message) throws SQLException, SQLHandledException {
        throw ExceptionUtils.propagate(handle(e, message, privateConnection), SQLException.class, SQLHandledException.class);
    }

    private void endTransaction(final OperationOwner owner, boolean rollback) throws SQLException {
        Result<Throwable> firstException = new Result<>();

        assert isInTransaction();
        runSuppressed(() -> {
            if(inTransaction == 1) {
                if(useDeadLockPriority) {
                    if(deadLockPriority != null)
                        setDeadLockPriority(privateConnection, owner, null);
                    useDeadLockPriority = false;
                }
                applyStartTime = 0;

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
        }, firstException);

        runSuppressed(() -> inTransaction--, firstException);

        runSuppressed(() -> tryCommon(owner, true), firstException);

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
            runSuppressed(() -> {
                if(transactionCounter!=null) {
                    // в зависимости от политики или локальный пул (для сессии) или глобальный пул
                    int transTablesCount = privateConnection.temporary.getCounter() - transactionCounter;
                    if(transactionTables.size() != transTablesCount) {
                        ServerLoggers.assertLog(false, "CONSEQUENT TRANSACTION TABLES : COUNT " + transTablesCount + " " + transactionCounter + " " + transactionTables);
                    }
                    for(String transactionTable : transactionTables) {
                        //                dropTemporaryTableFromDB(transactionTable);

//                            String transactionTable = privateConnection.temporary.getTableName(i+transactionCounter);

                        ServerLoggers.assertLog(transactionTables.contains(transactionTable), "CONSEQUENT TRANSACTION TABLES : HOLE");
//                            returnUsed(transactionTable, sessionTablesMap);
                        WeakReference<TableOwner> tableOwner = sessionTablesMap.remove(transactionTable);
                        if(isExplainTemporaryTablesEnabled())
                            addFifo("TRANSRET " + transactionTable + " " + privateConnection.temporary + " " + BaseUtils.nullToString(tableOwner) + " " + BaseUtils.nullToString(tableOwner == null ? null : tableOwner.get()) + " " + owner);
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
            }, firstException);

            if(!(problemInTransaction == Problem.CLOSED))
                runSuppressed(() -> privateConnection.sql.rollback(), firstException);
            problemInTransaction = null;
        }

        if(isExplainTemporaryTablesEnabled())
            addFifo("RBACK");

        runSuppressed(() -> endTransaction(owner, true), firstException);

        finishExceptions(firstException);
    }

    public void checkSessionTableMap(SessionTable table, Object owner) {
        assert sessionTablesMap.get(table.getName()).get() == owner;
    }

    public void commitTransaction() throws SQLException, SQLHandledException {
        commitTransaction(OperationOwner.unknown, () -> {});
    }
    public void commitTransaction(OperationOwner owner, SQLRunnable afterCommit) throws SQLException, SQLHandledException {
        if(inTransaction == 1) {
            try {
                privateConnection.sql.commit();
            } catch (SQLException e) {
                handleAndPropagate(e, "COMMIT TRANSACTION");
            }
        }

        afterCommit.run();

        if(isExplainTemporaryTablesEnabled())
            addFifo("CMT");

        endTransaction(owner, false);
    }

    // удостоверивается что таблица есть
    public void ensureTable(StoredTable table) throws SQLException {
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
                createTable(table, table.keys, false);
                for (PropertyField property : table.properties)
                    addColumn(table, property, false);
            }
        } finally {
            try {
                returnConnection(connection, OperationOwner.unknown);
            } finally {
                unlockRead();
            }
        }
    }

    public void addExtraIndexes(StoredTable table, ImOrderSet<KeyField> keys, boolean ifNotExists) throws SQLException {
        for(int i=1;i<keys.size();i++)
            addIndex(table, BaseUtils.<ImOrderSet<Field>>immutableCast(keys).subOrder(i, keys.size()).toOrderMap(true), defaultIndexOptions, ifNotExists);
    }

    public void checkExtraIndexes(SQLSession threadLocalSQL, StoredTable table, ImOrderSet<KeyField> keys) throws SQLException, SQLHandledException {
        for(int i=1;i<keys.size();i++) {
            ImOrderMap<Field, Boolean> fields = BaseUtils.<ImOrderSet<Field>>immutableCast(keys).subOrder(i, keys.size()).toOrderMap(true);
            threadLocalSQL.checkDefaultIndex(table, fields, defaultIndexOptions);
        }
    }

    private String getConstraintName(String table) {
        return syntax.getConstraintName("PK_" + table);
    }

    private static String getConstraintDeclare(String table, ImOrderSet<KeyField> keys, SQLSyntax syntax) {
        String keyString = keys.toString(Field.nameGetter(syntax), ",");
        // "CONSTRAINT " + getConstraintName(table) + " "
        return "PRIMARY KEY " + syntax.getClustered() + " (" + keyString + ")";
    }

    public static ImSet<ImOrderSet<Field>> getKeyIndexes(final ImOrderSet<KeyField> keys) {
        return SetFact.toOrderExclSet(keys.size(), i -> BaseUtils.<ImOrderSet<Field>>immutableCast(keys).subOrder(i, keys.size())).getSet();
    }

    public void createTable(StoredTable table, ImOrderSet<KeyField> keys, boolean ifNotExists) throws SQLException {
        MStaticExecuteEnvironment env = StaticExecuteEnvironmentImpl.mEnv();

        if (keys.size() == 0)
            keys = SetFact.singletonOrder(KeyField.dumb);
        String createString = keys.toString(this.getDeclare(env), ",");
        createString = createString + "," + getConstraintDeclare(table.getName(), keys, syntax);

//        System.out.println("CREATE TABLE "+Table.Name+" ("+CreateString+")");
        executeDDL("CREATE TABLE " + (ifNotExists ? "IF NOT EXISTS " : "")  + table.getName(syntax) + " (" + createString + ")", env.finish());
        addExtraIndexes(table, keys, ifNotExists);
    }

    public void renameTable(StoredTable table, String newTableName) throws SQLException {
        executeDDL("ALTER TABLE " + table.getName(syntax) + " RENAME TO " + syntax.getTableName(newTableName));
    }

    public void dropTable(StoredTable table) throws SQLException {
        executeDDL("DROP TABLE " + table.getName(syntax));
    }

    static String getIndexName(StoredTable table, String dbName, ImOrderMap<String, Boolean> fields, SQLSyntax syntax) {
        return getIndexName(table, fields, dbName, null, false, false, syntax);
    }

    public String getIndexName(StoredTable table, DBManager.IndexData<Field> index) {
        return getIndexName(table, syntax, index.options.dbName, getOrderFields(table.keys, index.options, SetFact.fromJavaOrderSet(index.fields)), index.options.type.suffix());
    }

    static String getIndexName(StoredTable table, SQLSyntax syntax, String dbName, ImOrderMap<Field, Boolean> fields, String suffix) {
        return getIndexName(table, syntax, dbName, fields, suffix, false, false);
    }

    static String getIndexName(StoredTable table, SQLSyntax syntax, String dbName, ImOrderMap<Field, Boolean> fields, String suffix, boolean suffixInTheEnd, boolean old) {
        return getIndexName(table, fields.mapOrderKeys(Field.nameGetter()), dbName, suffix, suffixInTheEnd, old, syntax);
    }

    static String getIndexName(StoredTable table, ImOrderMap<String, Boolean> fields, String dbName, String suffix, boolean suffixInTheEnd, boolean old, SQLSyntax syntax) {
        if(dbName != null)
            return dbName;
        if (suffixInTheEnd) { //new style
            return syntax.getIndexName(fields.keyOrderSet().toString("_") + "_idx" + (syntax.isIndexNameLocal() ? "" : "_" + table.getName())) + suffix;
        } else if (old) { //old style before 2015
            return syntax.getIndexName((syntax.isIndexNameLocal() ? "" : table.getName() + "_") + fields.keyOrderSet().toString("_") + "_idx");
        } else { //old style after 2015
            return syntax.getIndexName(fields.keyOrderSet().toString("_") + nvl(suffix, "") + "_idx" + (syntax.isIndexNameLocal() ? "" : "_" + table.getName()));
        }
    }

    private ImOrderMap<String, Boolean> getOrderFields(ImOrderSet<KeyField> keyFields, ImOrderSet<String> fields, IndexOptions indexOptions) {
        ImOrderMap<String, Boolean> result = fields.toOrderMap(false);
        if(indexOptions.order && indexOptions.type.isDefault())
            result = result.addOrderExcl(keyFields.mapOrderSetValues(Field.nameGetter()).toOrderMap(true));
        return result;
    }

    private ImOrderMap<Field, Boolean> getOrderFields(ImOrderSet<KeyField> keyFields, IndexOptions indexOptions, ImOrderSet<Field> fields) {
        ImOrderMap<Field, Boolean> result = fields.mapOrderValues((Field value) -> value instanceof KeyField);
        if(indexOptions.order && indexOptions.type.isDefault())
            result = result.addOrderExcl(keyFields.toOrderMap(true));
        return result;
    }

    public void checkIndex(StoredTable table, ImOrderSet<KeyField> keyFields, ImOrderSet<Field> fields, IndexOptions indexOptions) throws SQLException, SQLHandledException {
        ImOrderMap<Field, Boolean> fieldsMap = getOrderFields(keyFields, indexOptions, fields);
        if (indexOptions.type.isDefault()) {
            checkDefaultIndex(table, fieldsMap, indexOptions);
        } else if (indexOptions.type.isLike()) {
            checkLikeIndex(table, fieldsMap, indexOptions);
        } else if (indexOptions.type.isMatch()) {
            checkMatchIndex(table, fieldsMap, indexOptions);
        }
    }

    private void checkLikeIndex(StoredTable table, ImOrderMap<Field, Boolean> fields, IndexOptions indexOptions) throws SQLException, SQLHandledException {
        String newIndexName = getIndexName(table, syntax, indexOptions.dbName, fields, LIKE.suffix(), false, false);
        if (!checkIndex(newIndexName)) {
            //deprecated old index name with _like in the end  (can be removed after checkIndices)
            String oldIndexName = getIndexName(table, syntax, indexOptions.dbName, fields, LIKE.suffix(), true, false);
            if (checkIndex(oldIndexName)) {
                renameIndex(oldIndexName, newIndexName);
            } else {
                createLikeIndex(table, fields, indexOptions.dbName, getColumns(fields, indexOptions), logger, true);
            }
        }
    }

    private void checkMatchIndex(StoredTable table, ImOrderMap<Field, Boolean> fields, IndexOptions indexOptions) throws SQLException, SQLHandledException {
        String newIndexName = getIndexName(table, syntax, indexOptions.dbName, fields, MATCH.suffix(), false, false);
        if (!checkIndex(newIndexName)) {
            //deprecated old index name with _match in the end  (can be removed after checkIndices)
            String oldIndexName = getIndexName(table, syntax, indexOptions.dbName, fields, MATCH.suffix(), true, false);
            if (checkIndex(oldIndexName)) {
                renameIndex(oldIndexName, newIndexName);
            } else {
                createMatchIndex(table, fields, indexOptions.dbName, getColumns(fields, indexOptions), indexOptions, logger, true);
            }
        }
    }

    private void checkDefaultIndex(StoredTable table, ImOrderMap<Field, Boolean> fields, IndexOptions indexOptions) throws SQLException, SQLHandledException {
        String newIndexName = getIndexName(table, syntax, indexOptions.dbName, fields, null, false, false);
        if (!checkIndex(newIndexName)) {
            //deprecated old index name with idx in the end (before 2015)  (can be removed after checkIndices)
            String veryOldIndexName = getIndexName(table, syntax, indexOptions.dbName, fields, null, false, true);
            if (checkIndex(veryOldIndexName)) {
                renameIndex(veryOldIndexName, newIndexName);
            } else {
                createDefaultIndex(table, fields, indexOptions.dbName, getColumns(fields, indexOptions), logger, true);
            }
        }
    }

    private boolean checkIndex(String indexName) throws SQLException, SQLHandledException {
        //начиная с Postgres 9.5 можно заменить на 'create index if not exists', но непонятно, что тогда делать с логами, поэтому пока проверяем наличие индекса
        //https://dba.stackexchange.com/questions/35616/create-index-if-it-does-not-exist
        String command = "SELECT to_regclass('public." + indexName + "')";

        MExclSet<String> propertyNames = SetFact.mExclSet();
        propertyNames.exclAdd("to_regclass");
        propertyNames.immutable();

        MExclMap<String, Reader> propertyReaders = MapFact.mExclMap();
        propertyReaders.exclAdd("to_regclass", PGObjectReader.instance);
        propertyReaders.immutable();

        ImOrderMap rs = executeSelect(command, OperationOwner.unknown, StaticExecuteEnvironmentImpl.EMPTY, (ImMap<String, ParseInterface>) MapFact.mExclMap(),
                0, MapFact.EMPTYREV(), MapFact.EMPTY(), ((ImSet) propertyNames).toRevMap(), (ImMap) propertyReaders);

        boolean exists = false;
        for (Object rsValue : rs.values()) {
            if (((HMap) rsValue).get("to_regclass") != null)
                exists = true;
        }
        return exists;
    }

    public void addConstraint(StoredTable table) {
        try {
            if (!table.keys.isEmpty())
                executeDDL("DO $$ BEGIN ALTER TABLE " + table.getName() + " ADD " + getConstraintDeclare(table.getName(), table.keys, syntax) +
                        "; EXCEPTION WHEN others THEN /* ignore duplicates */ END; $$;");
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public void addIndex(StoredTable table, ImOrderSet<KeyField> keyFields, ImOrderSet<Field> fields, IndexOptions indexOptions, boolean ifNotExists) throws SQLException {
        addIndex(table, getOrderFields(keyFields, indexOptions, fields), indexOptions, ifNotExists);
    }

    public void addIndex(StoredTable table, ImOrderMap<Field, Boolean> fields) throws SQLException {
        addIndex(table, fields, defaultIndexOptions, false);
    }

    public void addIndex(StoredTable table, ImOrderMap<Field, Boolean> fields, IndexOptions indexOptions, boolean ifNotExists) throws SQLException {
        String columns = getColumns(fields, indexOptions);
        if (indexOptions.type.isDefault()) {
            createDefaultIndex(table, fields, indexOptions.dbName, columns, null, ifNotExists);
        } else if (indexOptions.type.isLike()) {
            checkIndexFieldTypes(fields);
            createLikeIndex(table, fields, indexOptions.dbName, columns, null, ifNotExists);
        } else if (indexOptions.type.isMatch()) {
            checkIndexFieldTypes(fields);
            if (fields.size() == 1 && fields.singleKey().type instanceof TSVectorClass) {
                createMatchIndexForTsVector(table, indexOptions.dbName, fields, columns, indexOptions, null, ifNotExists);
            } else {
                createMatchIndex(table, fields, indexOptions.dbName, columns, indexOptions, null, ifNotExists);
            }
        }
    }
    
    private void checkIndexFieldTypes(ImOrderMap<Field, Boolean> fields) {
        for(Field field : fields.keys()) {
            if(field.type instanceof AJSONClass) {
                throw new UnsupportedOperationException("Indexes for JSON / JSONTEXT properties are not supported");
            }
        }
    }

    private String getColumns(ImOrderMap<Field, Boolean> fields, IndexOptions indexOptions) {
        return fields.toString((key, value) -> {
            assert value || !(key instanceof KeyField);
            return key.getName(syntax) + (indexOptions.type.isDefault() ? (" " + syntax.getOrderDirection(false, value)) : "");
        }, ",");
    }

    private void createLikeIndex(StoredTable table, ImOrderMap<Field, Boolean> fields, String dbName, String columns, Logger logger, boolean ifNotExists) throws SQLException {
        if (DataAdapter.hasTrgmExtension()) {
            createIndex(table, getIndexName(table, syntax, dbName, fields, LIKE.suffix()),
                    " USING GIN (" + columns + " gin_trgm_ops)", logger, ifNotExists);
        }
    }

    private void createMatchIndex(StoredTable table, ImOrderMap<Field, Boolean> fields, String dbName, String columns, IndexOptions indexOptions, Logger logger, boolean ifNotExists) throws SQLException {
        if(DataAdapter.hasTrgmExtension()) {
            createIndex(table, getIndexName(table, syntax, dbName, fields, MATCH.suffix()),
                    " USING GIN (to_tsvector(" + (indexOptions.language != null ? ("'" + indexOptions.language + "', ") : "") + columns + "))", logger, ifNotExists);
        }
    }

    private void createMatchIndexForTsVector(StoredTable table, String dbName, ImOrderMap<Field, Boolean> fields, String columns, IndexOptions indexOptions, Logger logger, boolean ifNotExists) throws SQLException {
        createIndex(table, getIndexName(table, syntax, dbName, fields, MATCH.suffix()),
                " USING GIN (" + columns + ")", logger, ifNotExists);
    }

    private void createDefaultIndex(StoredTable table, ImOrderMap<Field, Boolean> fields, String dbName, String columns, Logger logger, boolean ifNotExists) throws SQLException {
        createIndex(table, getIndexName(table, syntax, dbName, fields, null), " (" + columns + ")", logger, ifNotExists);
    }

    private void createIndex(StoredTable table, String nameIndex, String columnsPostfix, Logger logger, boolean ifNotExists) throws SQLException {
        long start = System.currentTimeMillis();
        if (logger != null) {
            logger.info(String.format("Adding index started: %s", nameIndex));
        }
        executeDDL("CREATE INDEX " + (ifNotExists ? "IF NOT EXISTS " : "") + nameIndex + " ON " + table.getName(syntax) + columnsPostfix);
        if (logger != null) {
            logger.info(String.format("Adding index: %s, %sms", nameIndex, System.currentTimeMillis() - start));
        }
    }

    public void dropIndex(StoredTable table, ImOrderSet<KeyField> keyFields, ImOrderSet<String> fields, IndexOptions indexOptions, boolean ifExists) throws SQLException {
        if (indexOptions.type.isDefault()) {
            dropDefaultIndex(table, keyFields, fields, indexOptions, ifExists);
        } else if (indexOptions.type.isLike()) {
            dropLikeIndex(table, keyFields, fields, indexOptions);
        } else if (indexOptions.type.isMatch()) {
            dropMatchIndex(table, keyFields, fields, indexOptions);
        }
    }

    public void dropLikeIndex(StoredTable table, ImOrderSet<KeyField> keyFields, ImOrderSet<String> fields, IndexOptions indexOptions) throws SQLException {
        if (DataAdapter.hasTrgmExtension()) {
            ImOrderMap<String, Boolean> orderFields = getOrderFields(keyFields, fields, indexOptions);
            //ifExists true for both, because we don't know which of indexes exists
            dropIndex(table, getIndexName(table, orderFields, indexOptions.dbName, LIKE.suffix(), false, false, syntax), true);
            //deprecated old index name with _like in the end (can be removed after checkIndices)
            dropIndex(table, getIndexName(table, orderFields, indexOptions.dbName, LIKE.suffix(), true, false, syntax), true);
        }
    }

    public void dropMatchIndex(StoredTable table, ImOrderSet<KeyField> keyFields, ImOrderSet<String> fields, IndexOptions indexOptions) throws SQLException {
        if (DataAdapter.hasTrgmExtension()) {
            ImOrderMap<String, Boolean> orderFields = getOrderFields(keyFields, fields, indexOptions);
            //ifExists true for both, because we don't know which of indexes exists
            dropIndex(table, getIndexName(table, orderFields, indexOptions.dbName, MATCH.suffix(), false, false, syntax), true);
            //deprecated old index name with _match in the end  (can be removed after checkIndices)
            dropIndex(table, getIndexName(table, orderFields, indexOptions.dbName, MATCH.suffix(), true, false, syntax), true);
        }
    }

    public void dropDefaultIndex(StoredTable table, ImOrderSet<KeyField> keyFields, ImOrderSet<String> fields, IndexOptions indexOptions, boolean ifExists) throws SQLException {
        //ifExists true for both, because we don't know which of indexes exists
        ImOrderMap<String, Boolean> orderFields = getOrderFields(keyFields, fields, indexOptions);
        if (!ifExists)
            dropIndex(table, getIndexName(table, orderFields, indexOptions.dbName, null, false, true, syntax), true);
        //deprecated old index name with idx in the end (before 2015)  (can be removed after checkIndices)
        dropIndex(table, getIndexName(table, orderFields, indexOptions.dbName, null, false, false, syntax), true);
    }

    public void dropIndex(StoredTable table, String indexName, boolean ifExists) throws SQLException {
        executeDDL("DROP INDEX " + (ifExists ? "IF EXISTS " : "") + indexName + (syntax.isIndexNameLocal() ? " ON " + table.getName(syntax) : ""));
    }

    public void renameIndex(StoredTable table, ImOrderSet<KeyField> keyFields, ImOrderSet<String> oldFields, ImOrderSet<String> newFields, IndexOptions oldOptions, IndexOptions newOptions, boolean ifExists) throws SQLException {
        String oldIndexNameSuffix = (oldOptions.type == DEFAULT ? null : oldOptions.type.suffix());
        String newIndexNameSuffix = (newOptions.type == DEFAULT ? null : newOptions.type.suffix());
        String oldIndexName = getIndexName(table, getOrderFields(keyFields, oldFields, oldOptions), oldOptions.dbName, oldIndexNameSuffix, false, false, syntax);
        String newIndexName = getIndexName(table, getOrderFields(keyFields, newFields, newOptions), newOptions.dbName, newIndexNameSuffix, false, false, syntax);
        logger.info("Renaming index from " + oldIndexName + " to " + newIndexName);
        executeDDL("ALTER INDEX " + (ifExists ? "IF EXISTS " : "" ) + oldIndexName + " RENAME TO " + newIndexName);
    }

    public void renameIndex(String oldIndexName, String newIndexName) throws SQLException {
        logger.info("Renaming index from " + oldIndexName + " to " + newIndexName);
        executeDDL("ALTER INDEX " + oldIndexName + " RENAME TO " + newIndexName);
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

    public void addColumn(StoredTable table, PropertyField field, boolean ifNotExists) throws SQLException {
        MStaticExecuteEnvironment env = StaticExecuteEnvironmentImpl.mEnv();
        executeDDL("ALTER TABLE " + table.getName(syntax) + " ADD " + (ifNotExists ? "IF NOT EXISTS " : "") + field.getDeclare(syntax, env), env.finish()); //COLUMN
    }

    public void dropColumn(String table, String field, boolean ifExists) throws SQLException {
        innerDropColumn(syntax.getTableName(table), syntax.getFieldName(field), ifExists);
    }

    private void innerDropColumn(String table, String field, boolean ifExists) throws SQLException {
        executeDDL("ALTER TABLE " + table + " DROP COLUMN " + (ifExists ? "IF EXISTS " : "" ) + field);
    }

    public void dropColumns(String table, List<String> fields) throws SQLException {
        executeDDL("ALTER TABLE " + syntax.getTableName(table) + " " + fields.stream().map(
                field -> "DROP COLUMN IF EXISTS " + syntax.getFieldName(field)).collect(Collectors.joining(", ")));
    }

    public void renameColumn(String table, String columnName, String newColumnName) throws SQLException {
        executeDDL(syntax.getRenameColumn(table, columnName, newColumnName));
    }

    public void modifyColumn(StoredTable table, Field field, Type oldType) throws SQLException {
        MStaticExecuteEnvironment env = StaticExecuteEnvironmentImpl.mEnv();
        executeDDL("ALTER TABLE " + table + " ALTER COLUMN " + field.getName(syntax) + " " + syntax.getTypeChange(oldType, field.type, field.getName(syntax), env));
    }

    public void modifyColumns(StoredTable table, Map<Field, Type> fieldTypeMap) throws SQLException {
        MStaticExecuteEnvironment env = StaticExecuteEnvironmentImpl.mEnv();
        String ddl = "";
        for(Map.Entry<Field, Type> entry : fieldTypeMap.entrySet()) {
            Field field = entry.getKey();
            Type oldType = entry.getValue();
            ddl += (ddl.isEmpty() ? "" : ",") + " ALTER COLUMN " + field.getName(syntax) + " " + syntax.getTypeChange(oldType, field.type, field.getName(syntax), env);
        }
        executeDDL("ALTER TABLE " + table + " " + ddl);
    }

    public void packTable(StoredTable table, OperationOwner owner, TableOwner tableOwner) throws SQLException {
        checkTableOwner(table, tableOwner);
        
        String dropWhere = table.properties.toString(value -> value.getName(syntax) + " IS NULL", " AND ");
        executeDML("DELETE FROM " + table.getName(syntax) + (dropWhere.length() == 0 ? "" : " WHERE " + dropWhere), owner, tableOwner, register(table, tableOwner, TableChange.DELETE));
    }

    private final Map<String, WeakReference<TableOwner>> sessionTablesMap = MapFact.mAddRemoveMap(); // все использования assertLock
    public final Map<String, String> sessionDebugInfo = MapFact.mAddRemoveMap(); // все использования assertLock
    private final Map<String, Long> lastReturnedStamp = MapFact.mAddRemoveMap(); // все использования assertLock

    private int totalSessionTablesCount; // lockRead
    // по большому счету от sessionTablesCount можно отказаться (правда TRUNCATE не возвращает количество записей)
    private final Map<String, Integer> sessionTablesCount = MapFact.mAddRemoveMap(); // lockRead

    public int getSessionTablesCountAll(boolean lockedWrite) throws SQLException { // assertLock
        ServerLoggers.assertLog(!lockedWrite || (!Settings.get().isCheckSessionCount() || calcSessionTablesCountAll() == totalSessionTablesCount), "TABLE COUNT ON SQL AND APP SERVER SHOULD MATCH"); // если не lockedwrite то может отличаться количество, так как может пройти изменение, но не делаться registerChange
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
    public SessionTable createTemporaryTable(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count, ImMap<KeyField, Integer> distinctKeys, ImMap<PropertyField, PropStat> statProps, FillTemporaryTable fill, Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> queryClasses, TableOwner owner, OperationOwner opOwner) throws SQLException, SQLHandledException {
        Result<Integer> actual = new Result<>();
        String temporaryTable = getTemporaryTable(keys, properties, fill, count, actual, owner, opOwner);
        return new SessionTable(temporaryTable, keys, properties, queryClasses.first, queryClasses.second, actual.result, distinctKeys, statProps).checkClasses(this, null, SessionTable.nonead, opOwner);
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

    public static Buffer fifoTC = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(1000000));
    public static void outFifoTC() throws IOException {
        String filename = "e:\\outTC.txt";
        BufferedWriter outputWriter = new BufferedWriter(new FileWriter(filename));
        for (Object ff : fifoTC) {
            outputWriter.write(ff+"");
            outputWriter.newLine();
        }
        outputWriter.flush();
        outputWriter.close();
    }

    private void rollbackTransactionSessionTablesCount() { // assert lockWrite
        for(Map.Entry<String, Integer> tableCount : transactionSessionTablesCount.entrySet()) {
            if(tableCount.getValue() == null) {
                sessionTablesCount.remove(tableCount.getKey());
//                fifoTC.add("REMOVE " + getCurrentTimeStamp() + " " + tableCount.getKey() + " " + this + " " + ExecutionStackAspect.getExStackTrace());
            } else {
                sessionTablesCount.put(tableCount.getKey(), tableCount.getValue());
//                fifoTC.add("PUT " + getCurrentTimeStamp() + " " + tableCount.getKey() + " " + tableCount.getValue() + " " + this + " " + ExecutionStackAspect.getExStackTrace());
            }
        }
        if(transactionTotalSessionTablesCount != null)
            totalSessionTablesCount = transactionTotalSessionTablesCount;
    }

    public static Buffer fifo = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(10000));
    public void addFifo(String log) {
        fifo.add(log + " " + getCurrentTimeStamp() + " " + this + " " + ExecutionStackAspect.getExStackTrace());
    }
    public static void outFifo(String filename) throws IOException {
        // "e:\\out.txt";
        BufferedWriter outputWriter = new BufferedWriter(new FileWriter(filename));
        for (Object ff : fifo) {
            outputWriter.write(ff+"");
            outputWriter.newLine();
        }
        outputWriter.flush();
        outputWriter.close();
        fifo.clear();
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
                table = privateConnection.temporary.getTable(this, keys, properties, count, sessionTablesMap, sessionDebugInfo, isNew, owner, opOwner); //, sessionTablesStackGot

                registerChange(table, owner, -1, TableChange.ADD);
                if(isNew.result && isInTransaction()) { // пометим как transaction
                    if(transactionCounter==null)
                        transactionCounter = privateConnection.temporary.getCounter() - 1;
                    transactionTables.add(table);
                }
            } finally {
                temporaryTablesLock.unlock();
            }
            if(isExplainTemporaryTablesEnabled())
                addFifo("GET " + table + " " + privateConnection.temporary + " " + owner + " " + opOwner);
            try {
                privateConnection.temporary.fillData(this, fill, count, actual, table, opOwner);
            } catch (Throwable t) {
                returnTemporaryTable(table, owner, opOwner, true, null); // вернем таблицу, если не смогли ее заполнить, truncate при timeoutе потому как в остальных случаях и так должна быть пустая (строго говоря с timeout'ом это тоже перестраховка)
                // "truncate" changed from "t instanceof lsfusion.server.data.sql.exception.SQLTimeoutException || fill.canBeNotEmptyIfFailed()" to "true",
                // because if you run out of disk space for temporary tables, there is a large file lying on the disk, which blocks further work with temporary tables
                try { ServerLoggers.assertLog(problemInTransaction != null || (!Settings.get().isCheckSessionCount() || getSessionCount(table, opOwner) == 0), "TEMPORARY TABLE AFTER FILL NOT EMPTY"); } catch (Throwable i) { ServerLoggers.sqlSuppLog(i); }
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
                if(isExplainTemporaryTablesEnabled())
                    addFifo("RU " + force + " " + entry.getKey() + " " + privateConnection.temporary + " " + (tableOwner == null ? TableOwner.none : tableOwner) + " " + opOwner);
                lastReturnedStamp.put(entry.getKey(), System.currentTimeMillis());
                truncateSession(entry.getKey(), opOwner, null, (tableOwner == null ? TableOwner.none : tableOwner));
                logger.info("REMOVE UNUSED TEMP TABLE : " + entry.getKey() + ", DEBUG INFO : " + sessionDebugInfo.get(entry.getKey())); // потом надо будет больше инфы по owner'у добавить, в основном keysTable из-за pendingCleaners 
                iterator.remove();
            }
        }
    }

//    private final Map<String, String> lastOwner = new HashMap<String, String>();

    public void returnTemporaryTable(final SessionTable table, TableOwner owner, final OperationOwner opOwner, int count) throws SQLException {
        lockedReturnTemporaryTable(table.getName(), owner, opOwner, count);
    }

    public void lockedReturnTemporaryTable(String name, TableOwner owner, OperationOwner opOwner, int count) throws SQLException {
        lockRead(opOwner);

        try {
            returnTemporaryTable(name, owner, opOwner, true, count);
        } finally {
            unlockRead();
        }
    }

    @Override
    public OperationOwner getDefaultCloseOwner() {
        return OperationOwner.unknown;
    }

    public void returnTemporaryTable(final String table, final TableOwner owner, final OperationOwner opOwner, boolean truncate, Integer count) throws SQLException {
        temporaryTablesLock.lock();

        try {
            Result<Throwable> firstException = new Result<>();
            if(isExplainTemporaryTablesEnabled())
                addFifo("RETURN " + truncate + " " + table + " " + privateConnection.temporary + " " + BaseUtils.nullToString(sessionTablesMap.get(table)) +  " " + owner + " " + opOwner);
            lastReturnedStamp.put(table, System.currentTimeMillis());
            if(truncate) {
                runSuppressed(() -> truncateSession(table, opOwner, count, owner), firstException);
                if(firstException.result != null) {
                    runSuppressed(() -> {
                        lastReturnedStamp.remove(table);
                        privateConnection.temporary.removeTable(table);
                    }, firstException);
                    runSuppressed(() -> dropTemporaryTableFromDB(table), firstException);
                }
            }
    
            runSuppressed(() -> {
                assert sessionTablesMap.containsKey(table);
                WeakReference<TableOwner> removed = sessionTablesMap.remove(table);
                ServerLoggers.assertLog(removed == null || removed.get()==owner, "REMOVE OWNER SHOULD BE EQUAL TO GET OWNER");
            }, firstException);
    
            runSuppressed(() -> tryCommon(opOwner, true), firstException);

            finishExceptions(firstException);
        } finally { 
            temporaryTablesLock.unlock();
        }
    }
    
    public void rollReturnTemporaryTable(SessionTable table, TableOwner owner, OperationOwner opOwner, boolean assertNotExists) throws SQLException {
        lockRead(opOwner);
        try {
            temporaryTablesLock.lock();
            try {
                needPrivate();

                registerChange(table.getName(), owner, -1, TableChange.ADD);

                WeakReference<TableOwner> value = new WeakReference<>(owner);
                if(isExplainTemporaryTablesEnabled())
                    addFifo("RGET " + table + " " + privateConnection.temporary + " " + value + " " + owner + " " + opOwner);
                WeakReference<TableOwner> prevOwner = sessionTablesMap.put(table.getName(), value);
                sessionDebugInfo.put(table.getName(), owner.getDebugInfo());

                if(assertNotExists) {
                    // assertion построен на том что между началом транзакции ее rollback'ом, все созданные таблицы в явную drop'ся, соответственно может нарушится если скажем открыта форма и не close'ута, или просто new IntegrationService идет
                    // в принципе он не настолько нужен, но для порядка пусть будет
                    ServerLoggers.assertLog(prevOwner == null, "ROLLBACK TABLE SHOULD BE FREE");
                } else
                    ServerLoggers.assertLog(prevOwner == null || prevOwner.get() == owner, "ROLLBACK OWNERS SHOULD MATCH"); // вернул назад

            } finally {
                temporaryTablesLock.unlock();
            }
        } finally {
            unlockRead();
        }
    }

    // напрямую не используется, только через Pool

    private void dropTemporaryTableFromDB(String tableName) throws SQLException {
        Pair<String, StaticExecuteEnvironment> result = getDropDDL(tableName, syntax);
        executeDDL(result.first, result.second);
    }
    private static Pair<String, StaticExecuteEnvironment> getDropDDL(String name, SQLSyntax syntax) {
        return new Pair<>(syntax.getDropSessionTable(name), StaticExecuteEnvironmentImpl.NOREADONLY);
    }

    public static ImSet<ImOrderSet<Field>> getTemporaryIndexes(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties) {
        return SetFact.singleton(BaseUtils.immutableCast(keys));
    }

    public void createTemporaryTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, OperationOwner owner) throws SQLException {
        Pair<String, StaticExecuteEnvironment> result = getCreateDDL(name, keys, properties, syntax);
        executeDDL(result.first, result.second, owner);
    }

    private static Pair<String, StaticExecuteEnvironment> getCreateDDL(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, SQLSyntax syntax) {
        MStaticExecuteEnvironment mEnv = StaticExecuteEnvironmentImpl.mEnv();

        if(keys.size()==0)
            keys = SetFact.singletonOrder(KeyField.dumb);
        String createString = SetFact.addExclSet(keys.getSet(), properties).toString(SQLSession.getDeclare(syntax, mEnv), ",");
        createString = createString + "," + getConstraintDeclare(name, keys, syntax);
        mEnv.addNoReadOnly();
        String command = syntax.getCreateSessionTable(name, createString);
        StaticExecuteEnvironment env = mEnv.finish();
        return new Pair<>(command, env);
    }

    private static Pair<String, StaticExecuteEnvironment> getVacuumAnalyzeDDL(String table, SQLSyntax syntax) {
        return new Pair<>(syntax.getAnalyze(table), StaticExecuteEnvironmentImpl.NOREADONLY);
    }

    public void vacuumAnalyzeSessionTable(String table, OperationOwner owner) throws SQLException, SQLHandledException {
//        (isInTransaction()? "" :"VACUUM ") + по идее не надо так как TRUNCATE делается
        Pair<String, StaticExecuteEnvironment> ddl = getVacuumAnalyzeDDL(table, syntax);
        try {
            executeDDL(ddl.first, ddl.second, owner);
        } catch (SQLException throwable) {
            handleAndPropagate(throwable, "VACUUM ANALYZE");
        }
    }

    private int noReadOnly = 0;
    private final Object noReadOnlyLock = new Object();
    public void pushNoReadOnly() throws SQLException {
        lockRead(OperationOwner.unknown);

        try {
            lockNeedPrivate();

            pushNoReadOnly(getConnection().sql);
        } finally {
            unlockRead();
        }
    }
    public void pushNoReadOnly(Connection connection) throws SQLException {
        synchronized (noReadOnlyLock) {
            if(inTransaction == 0 && noReadOnly++ == 0) {
                connection.setReadOnly(false);
            }
        }
    }
    public void popNoReadOnly() throws SQLException {
        lockRead(OperationOwner.unknown);
        try {
            popNoReadOnly(getConnection().sql);
        } finally {
            lockTryCommon(OperationOwner.unknown);

            unlockRead();
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
    private long applyStartTime;
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

    public void pushVolatileStats(OperationOwner owner) {
        Integer prevValue = volatileStats.get();
        volatileStats.set(prevValue == null ? 1 : prevValue + 1);
    }

    public void popVolatileStats(OperationOwner opOwner) {
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
        SQLDebugInfo debugInfo = execute.debugInfo;
        SQLDebugInfo prevDebugInfo = SQLDebugInfo.pushStack(debugInfo);
        try {
            return executeDML(execute.command, execute.owner, execute.tableOwner, execute.params, execute.queryExecEnv, execute.outerEnv, execute.pureTime, execute.transactTimeout, execute.registerChange);
        } finally {
            SQLDebugInfo.popStack(debugInfo, prevDebugInfo);
        }
    }

    private static Map<Long, Boolean> explainUserMode = MapFact.getGlobalConcurrentHashMap();
    private static boolean explainNoAnalyze;
    private static Map<Long, Boolean> loggerDebugEnabled = MapFact.getGlobalConcurrentHashMap();
    private static Map<Long, Boolean> explainTemporaryTablesEnabled = MapFact.getGlobalConcurrentHashMap();
    private static Map<Long, Boolean> userVolatileStats = MapFact.getGlobalConcurrentHashMap();

    public static void setExplainAnalyzeMode(Long user, Boolean mode) {
        explainUserMode.put(user, mode != null && mode);
    }

    public static void setExplainNoAnalyze(boolean explainNoAnalyze) {
        SQLSession.explainNoAnalyze = explainNoAnalyze;
    }

    public static void setLoggerDebugEnabled(Long user, Boolean enabled) {
        loggerDebugEnabled.put(user, enabled != null && enabled);
    }
    
    public static void setExplainTemporaryTablesEnabled(Long user, Boolean enabled) {
        explainTemporaryTablesEnabled.put(user, enabled != null && enabled);
    }
    
    public static void setVolatileStats(Long user, Boolean enabled, OperationOwner owner) {
        userVolatileStats.put(user, enabled != null && enabled);
    }

    public boolean explainAnalyze() {
        Long currentUser = contextProvider.getCurrentUser();
        if(currentUser == null)
            return false;
        Boolean eam = explainUserMode.get(currentUser);
        return eam != null && eam;
    }

    public boolean explainNoAnalyze() {
        return explainNoAnalyze;
    }

    public boolean isLoggerDebugEnabled() {
        Long currentUser = contextProvider.getCurrentUser();
        if(currentUser == null)
            return false;
        Boolean lde = loggerDebugEnabled.get(currentUser);
        return lde != null && lde;
    }
    
    public boolean isExplainTemporaryTablesEnabled() {
        Long currentUser = contextProvider.getCurrentUser();
        if(currentUser == null)
            return false;
        Boolean ett = explainTemporaryTablesEnabled.get(currentUser);
        return ett != null && ett;
    }
    
    public boolean getVolatileStats(Long user) {
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
    public int executeExplain(PreparedStatement statement, boolean noAnalyze, boolean dml, Provider<String> fullText) throws SQLException {
        long l = System.currentTimeMillis();
        int minSpaces = Integer.MAX_VALUE;
        Integer rows = null;
        try (ResultSet result = statement.executeQuery()) {
            long actualTime = System.currentTimeMillis() - l;
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

                if (!noAnalyze && dml && i > 0 && act >= 0) { // первая запись почему-то всегда 0, минимальная табуляция (как правило первый или второго 
                    int sp=0; char ch;
                    for(;sp<row.length() && ((ch = row.charAt(sp))==' ' || ch=='\t');sp++);
                    if(sp < minSpaces && row.startsWith("->", sp)) {
                        rows = (int) act;
                        minSpaces = sp;
                    }
                }
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
                    String statementInfo = statement.toString() + " timeout : " + getQueryTimeout() + " actual time : " + actualTime +
                                    '\n' + "LSF stack:" + BaseUtils.tab('\n' + ExecutionStackAspect.getStackString());
                    statementInfo += '\n' + "Params debug info: " + BaseUtils.tab('\n' + SQLDebugInfo.getSqlDebugInfo(this));
                    if (Settings.get().isExplainJavaStack())
                        statementInfo += '\n' + "Java stack: " + BaseUtils.tab('\n' + ExceptionUtils.getStackTrace());
                    explainLogger.info(statementInfo);

                    for (String outRow : out)
                        explainLogger.info(outRow); // выводим время, чтобы видеть, что идет тот же запрос (когда очень большой запрос)

                    if(Settings.get().isExplainCompile())
                        SQLDebugInfo.outCompileDebugInfo("Full text : " + fullText.get() + '\n' + statementInfo);
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
        Pattern pt = Pattern.compile(ptString, Pattern.CASE_INSENSITIVE);
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
        return handle(e, message, false, connection, true, null);
    }

    private Throwable handle(Throwable t, String message, boolean isTransactTimeout, ExConnection connection, boolean errorPrivate, Boolean forcedCancel) {
        if(!(t instanceof SQLException))
            return t;
        
        SQLException e = (SQLException)t;            
        if(message == null)
            message = "PREPARING STATEMENT";
        
        if(isExplainTemporaryTablesEnabled())
            addFifo("E");

        boolean inTransaction = isInTransaction();
        if(inTransaction && syntax.hasTransactionSavepointProblem())
            problemInTransaction = Problem.EXCEPTION;

        SQLHandledException handled = null;
        boolean deadLock = false;
        if(syntax.isUpdateConflict(e) || (deadLock = syntax.isDeadLock(e))) {
            handled = new SQLConflictException(!deadLock);
            sqlConflictLogger.info((inTransaction ? "TRANSACTION " : "") + " " + handled.toString() + " " + message + (Settings.get().isLogConflictStack() ? ExecutionStackAspect.getExStackTrace() : ""));
        }

        // duplicate keys валится при :
        // ПОЛЬЗОВАТЕЛЬСКИЕ
        // неправильном неявном приведении типов (от широкого к узкому, DataClass.containsAll), проблемах с округлениями,
        //      в частности проблема с AS если есть GROUP BY f(a) широкий тип AS узкий тип, то тип выведется узкий, а в вычислении SQL округления не будет и при UNION / GROUP BY можно получить дублмкаты
        // недетерминированные ORDER функции (GROUP LAST и т.п.)
        // нецелостной базой (значения классов в базе не правильные)
        //      также при нарушении GROUP AGGR может возникать, так как GROUP AGGR тоже не детерминирован 
        // неправильный вывод классов в таблицах (см. SessionTable.assertCheckClasses),
        // !!! также при нарушении checkSessionCount (тестится fifo.add логом)
        //еще может быть ситуация при материализации подзапросов, если она выполняется не в транзакции (скорее всего в длинных запросах)
        //      что одни и те же ключи появляются в двух подзапросах и при объединении дублируются
        if(syntax.isUniqueViolation(e))
            handled = new SQLUniqueViolationException(false);

        if(syntax.isTableDoesNotExist(e))
            handLogger.info("TABLE DOES NOT EXIST " + sessionDebugInfo);

        String reason = syntax.getRetryWithReason(e);
        if(reason != null)
            handled = new SQLRetryException(reason);

        if(syntax.isTimeout(e))
            handled = new lsfusion.server.data.sql.exception.SQLTimeoutException(isTransactTimeout, forcedCancel);

        if(syntax.isConnectionClosed(e)) {
            handled = new SQLClosedException(connection.sql, inTransaction, e, errorPrivate);
            problemInTransaction = Problem.CLOSED;
        }

        if(handled != null) {
            handLogger.info((inTransaction ? "TRANSACTION " : "") + " " + handled + message + (handled instanceof SQLUniqueViolationException ? " " + ExceptionUtils.getStackTrace() : ""));
            return handled;
        }
        
        if(!suppressErrorLogging)
            logger.error(message + " " + e.getMessage());
        return e;
    }

    public boolean suppressErrorLogging;

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
                runSuppressed(statement::close, firstException);

            runSuppressed(() -> unlockConnection(owner), firstException);

            if(env != null)
                runSuppressed(() -> env.after(SQLSession.this, connection, command, owner), firstException);

            runSuppressed(() -> returnConnection(connection, owner), firstException);
        }

        runSuppressed(() -> {
            if(registerChange != null)
                registerChange.register(SQLSession.this, rowsChange);
        }, firstException);

        unlockRead();

        finishExceptions(firstException);
    }

    // lockRead есть, плюс assert что синхронизировано в рамках одной таблицы
    private void registerChange(String table, TableOwner tableOwner, int rows, TableChange tableChange) {
        if(tableChange == TableChange.UPDATE)
            return;

        if(tableOwner == TableOwner.debug)
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
//                fifoTC.add("ADD " + getCurrentTimeStamp() + " " + table + " " + 0 + " " + this + " " + ExecutionStackAspect.getExStackTrace());
            } else
            if(tableChange == TableChange.REMOVE) {
                assert rows == -1;
                Integer removeCount = sessionTablesCount.remove(table);
                if(removeCount == null) {
                    ServerLoggers.assertLog(false, "TABLE WAS REMOVED BEFORE"); // по идее всегда связано с транзакцией, когда таблица была получена в транзакции, но не возвращена по той или иной причине (например см. pendingCleaners и проверку createdInTransaction)
                    removeCount = 0;
                }
                delta = -removeCount;
//                fifoTC.add("REMOVE " + getCurrentTimeStamp() + " " + table + " " + this + " " + ExecutionStackAspect.getExStackTrace());
            } else {
                delta = (tableChange == TableChange.INSERT ? rows : -rows);
                sessionTablesCount.put(table, sessionTablesCount.get(table) + delta);
//                fifoTC.add("INSERT " + getCurrentTimeStamp() + " " + table + " ADD " + delta + " " + this + " " + ExecutionStackAspect.getExStackTrace());
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
    public static RegisterChange register(StoredTable table, TableOwner tableOwner, TableChange tableChange) {
        if(table instanceof SessionTable)
            return register(table.getName(), tableOwner, tableChange);
        else
            return RegisterChange.VOID;
    }

    public static RegisterChange register(final String table, final TableOwner tableOwner, final TableChange tableChange) {
        return (sql, result) -> sql.registerChange(table, tableOwner, result, tableChange);
    }

    @StackMessage("{message.sql.execute}")
    private int executeDML(@ParamMessage (profile = false) String command, OperationOwner owner, TableOwner tableOwner, RegisterChange registerChange) throws SQLException {
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
    public <K,V> void executeDML(String update) throws SQLException, SQLHandledException {
        executeDML(new SQLDML(update, Cost.MIN, MapFact.EMPTY(), StaticExecuteEnvironmentImpl.EMPTY, false), OperationOwner.unknown, TableOwner.global, MapFact.EMPTY(), DynamicExecuteEnvironment.DEFAULT, null, PureTime.VOID, 0, RegisterChange.VOID);
    }
    public <K,V> void executeSelect(String select, OperationOwner owner, StaticExecuteEnvironment env, ImRevMap<K, String> keyNames, final ImMap<String, ? extends Reader> keyReaders, ImRevMap<V, String> propertyNames, ImMap<String, ? extends Reader> propertyReaders, ResultHandler<K, V> handler) throws SQLException, SQLHandledException {
        executeSelect(select, owner, env, MapFact.EMPTY(), 0, keyNames, keyReaders, propertyNames, propertyReaders, false, handler);
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
        executeSelect(new SQLQuery(select, Cost.MIN, false, MapFact.EMPTY(), env,keyReaders, propertyReaders, false, false), disableNestLoop ? DynamicExecuteEnvironment.DISABLENESTLOOP : DynamicExecuteEnvironment.DEFAULT, owner, paramObjects, transactTimeout, keyNames, propertyNames, handler);
    }

    public <K,V> void executeSelect(SQLQuery query, DynamicExecuteEnvironment queryExecEnv, OperationOwner owner, ImMap<String, ParseInterface> paramObjects, int transactTimeout, ImRevMap<K, String> keyNames, ImRevMap<V, String> propertyNames, ResultHandler<K, V> handler) throws SQLException, SQLHandledException {
        executeSelect(query, queryExecEnv, null, owner, paramObjects, transactTimeout, new MapResultHandler<>(handler, keyNames, propertyNames));
    }

    public <OE, S extends DynamicExecEnvSnapshot> void executeSelect(SQLQuery query, DynamicExecuteEnvironment queryExecEnv, OE outerEnv, OperationOwner owner, ImMap<String, ParseInterface> paramObjects, int transactTimeout, ResultHandler<String, String> handler) throws SQLException, SQLHandledException {
        executeCommand(query.fixConcSelect(syntax), queryExecEnv, owner, paramObjects, transactTimeout, handler, DynamicExecuteEnvironment.<OE, S>create(outerEnv), PureTime.VOID, true);
    }

    public <OE, S extends DynamicExecEnvSnapshot<OE, S>> int executeDML(@ParamMessage (profile = false) SQLDML command, OperationOwner owner, TableOwner tableOwner, ImMap<String, ParseInterface> paramObjects, DynamicExecuteEnvironment<OE, S> queryExecEnv, OE outerEnv, PureTimeInterface pureTime, int transactTimeout, final RegisterChange registerChange) throws SQLException, SQLHandledException { // public для аспекта
        final Result<Integer> count = new Result<>(0);
        SQLDML.Handler handler = new SQLDML.Handler() {
            public void proceed(Integer result) {
                count.set(result);
            }

            public void afterProceed() { // lockRead
                registerChange.register(SQLSession.this, count.result); // не в proceed чтобы lock'и внутри не ставить
            }
        };
        executeCommand(command, queryExecEnv, owner, paramObjects, transactTimeout, handler, DynamicExecuteEnvironment.create(outerEnv), pureTime, true);
        return count.result;
    }

    // можно было бы сделать аспектом, но во-первых вся логика before / after аспектная и реализована в явную, плюс непонятно как соотносить snapshot с mutable объектом (env)
    public <H, OE, S extends DynamicExecEnvSnapshot<OE, S>> void executeCommand(@ParamMessage (profile = false) final SQLCommand<H> command, final DynamicExecuteEnvironment<OE, S> queryExecEnv, final OperationOwner owner, ImMap<String, ParseInterface> paramObjects, int transactTimeout, H handler, DynamicExecEnvOuter<OE, S> outerEnv, PureTimeInterface pureTime, boolean setRepeatDate) throws SQLException, SQLHandledException {
        if(command.getLength() > Settings.get().getQueryLengthLimit()) {
            String fullCommand = command.getFullText();
            int length = fullCommand.length();
            throw new SQLTooLongQueryException(length, fullCommand.substring(0, Math.min(10000, length)));
        }

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
        ImSet<SessionTable> materializedTables = snapEnv.getMaterializedQueries().values().mapColSetValues(value -> new SessionTable(value.tableName, value.keyFields, value.propFields));
        ImSet<SessionTable> paramTables = paramObjects.values().filterCol(element -> element.getSessionTable() != null).mapMergeSetValues(ParseInterface::getSessionTable);

        SessionTable.saveToDBForDebug(SetFact.addExclSet(materializedTables, paramTables), this);
    }

    // SQLAnalyzeAspect
    @StackMessage("{message.sql.execute}")
    public <H> void executeCommand(@ParamMessage (profile = false) final SQLCommand<H> command, final DynamicExecEnvSnapshot snapEnv, final OperationOwner owner, ImMap<String, ParseInterface> paramObjects, H handler) throws SQLException, SQLHandledException {
        lockRead(owner);

        long runTime = 0;
        final Result<ReturnStatement> returnStatement = new Result<>();
        PreparedStatement statement = null;
        ExConnection connection = null;

        final String string = command.getString();

        Result<Throwable> firstException = new Result<>();
        StaticExecuteEnvironment env = command.env;

        Savepoint savepoint = null;
        ExecutingStatement executingStatement = null;
        try {
            snapEnv.beforeConnection(this, owner);

            connection = getConnection();

            env.before(this, connection, string, owner);

            lockConnection(snapEnv.needConnectionLock(), owner);

            snapEnv.beforeStatement(this, connection, string, owner);

            if(useDeadLockPriority && command.isDML()) {
                assert isInTransaction();

                // время со старта транзакции + половину времени предыдущих попыток
                // 1-е чтобы у старых транзакций был больший приоритет, 2-е чтобы скажем в postgres deadlock timeout постепенно увеличивался, так как по его истечении больше проверок на deadlock не происходит, и жертвой будет опять таки старая транзнакция (этот же подход позволяет увеличивать у всех участников deadlock постепенно timeout пока одна из транзакций не пройдет)
                long millisScore = System.currentTimeMillis() - transStartTime;
                if(syntax.useFailedTimeInDeadlockPriority())
                    millisScore += (transStartTime - applyStartTime) / 2;

                long secondsScore = millisScore / 1000;
                if(secondsScore > 0) {
                    long currentPriority = Math.round(Math.log(secondsScore) / Math.log(2.0));
                    if (deadLockPriority == null || deadLockPriority < currentPriority) // оптимизация
                        setDeadLockPriority(connection, owner, currentPriority); // предполагается, что deadLockPriority очистит endTransaction
                }
            }

            if(snapEnv.isUseSavePoint())
                savepoint = connection.sql.setSavepoint();

            Result<Integer> length = new Result<>();
            statement = getStatement(command, paramObjects, connection, syntax, snapEnv, returnStatement, length);
            snapEnv.beforeExec(statement, this);

            long started = System.currentTimeMillis();
            executingStatement = new ExecutingStatement(statement);

            String checkStatementSubstring = Settings.get().getCheckStatementSubstring();
            if(checkStatementSubstring != null && !checkStatementSubstring.isEmpty() && statement.toString().contains(checkStatementSubstring)) {
                String checkExcludeStatementSubstring = Settings.get().getCheckExcludeStatementSubstring();
                if(checkExcludeStatementSubstring == null || !statement.toString().contains(checkExcludeStatementSubstring))
                    ServerLoggers.handledExLog("FOUND STATEMENT : " + statement);
            }

            try {
                try {
                    this.executingStatement = executingStatement;

                    if(Thread.interrupted()) // since interruptThread usually interrupts thread, so we won't event send it
                        throw new InterruptedException();

                    command.execute(statement, handler, this);
                } finally {
                    this.executingStatement = null;
                }
            } finally {
                runTime = System.currentTimeMillis() - started;

                connection.registerExecute(length.result, runTime);
            }
        } catch (Throwable t) { // по хорошему тоже надо через runSuppressed, но будут проблемы с final'ами
            t = handle(t, statement != null ? statement.toString() : "PREPARING STATEMENT", snapEnv.isTransactTimeout(), connection, privateConnection != null, executingStatement != null ? executingStatement.forcedCancel : null);
            firstException.set(t);

            if(savepoint != null && t instanceof SQLHandledException && ((SQLHandledException)t).repeatCommand()) {
                assert problemInTransaction == Problem.EXCEPTION;
                final ExConnection fConnection = connection; final Savepoint fSavepoint = savepoint;
                runSuppressed(() -> {
                    fConnection.sql.rollback(fSavepoint);
                    problemInTransaction = null;
                }, firstException);
                unregisterUseSavePoint();
                savepoint = null;
            }
        } finally {
            if(savepoint != null) {
                if(problemInTransaction == null) { // if there was an exception in transaction, releaseSavepoint will fail anyway
                    final ExConnection fConnection = connection;
                    final Savepoint fSavepoint = savepoint;
                    runSuppressed(() -> fConnection.sql.releaseSavepoint(fSavepoint), firstException);
                }
                unregisterUseSavePoint();
            }
        }

        afterExStatementExecute(owner, env, snapEnv, connection, runTime, returnStatement, statement, string, command, handler, firstException);
    }

    private <H> void afterExStatementExecute(final OperationOwner owner, final StaticExecuteEnvironment env, final DynamicExecEnvSnapshot execInfo, final ExConnection connection, final long runTime, final Result<ReturnStatement> returnStatement, final PreparedStatement statement, final String string, final SQLCommand<H> command, final H handler, Result<Throwable> firstException) throws SQLException, SQLHandledException {
        if(connection != null) {
            runSuppressed(() -> execInfo.afterStatement(SQLSession.this, connection, string, owner), firstException);

            runSuppressed(() -> env.after(SQLSession.this, connection, string, owner), firstException);

            if (statement != null)
                runSuppressed(() -> returnStatement.result.proceed(statement, runTime), firstException);

            runSuppressed(() -> unlockConnection(execInfo.needConnectionLock(), owner), firstException);

            runSuppressed(() -> returnConnection(connection, owner), firstException);
        }

        runSuppressed(() -> execInfo.afterConnection(SQLSession.this, owner), firstException);

        runSuppressed(() -> command.afterExecute(handler)
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
                return AbstractParseInterface.NULL(field.type);
            else
                return getParse(prop, field, syntax);
        }
    };
            
    public void insertBatchRecords(DBTable table, ImMap<ImMap<KeyField, Object>, ImMap<PropertyField, Object>> rows, OperationOwner opOwner) throws SQLException {
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
        String valueString = fields.toString(value -> value.type.writeDeconc(syntax, mEnv), ",");

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

    private void insertParamRecord(StoredTable table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, OperationOwner owner, TableOwner tableOwner) throws SQLException {
        checkTableOwner(table, tableOwner);
        
        String insertString = "";
        String valueString = "";

        int paramNum = 0;
        MExclMap<String, ParseInterface> params = MapFact.mExclMapMax(keyFields.size()+propFields.size());

        // пробежим по KeyFields'ам
        for (int i=0,size=keyFields.size();i<size;i++) {
            KeyField key = keyFields.getKey(i);
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + key.getName(syntax);
            DataObject keyValue = keyFields.getValue(i);
            if (keyValue.isSafeString(syntax))
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
            if (fieldValue.isSafeString(syntax))
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
            executeDML(new SQLDML("INSERT INTO " + table.getName(syntax) + " (" + insertString + ") VALUES (" + valueString + ")", Cost.MIN, MapFact.EMPTY(), StaticExecuteEnvironmentImpl.EMPTY, false), owner, tableOwner, params.immutable(), DynamicExecuteEnvironment.DEFAULT, null, PureTime.VOID, 0, register(table, tableOwner, TableChange.INSERT));
        } catch (SQLHandledException e) {
            throw new UnsupportedOperationException(); // по идее ни deadlock'а, ни update conflict'а, ни timeout'а
        }
    }

    public void insertRecord(StoredTable table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, TableOwner owner, OperationOwner opOwner) throws SQLException {
        checkTableOwner(table, owner);

        boolean needParam = false;

        for (int i=0,size=keyFields.size();i<size;i++)
            if (!keyFields.getValue(i).isSafeString(syntax)) {
                needParam = true;
            }

        for (int i=0,size=propFields.size();i<size;i++)
            if (!propFields.getValue(i).isSafeString(syntax)) {
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

    public boolean isRecord(StoredTable table, ImMap<KeyField, DataObject> keyFields, OperationOwner owner) throws SQLException, SQLHandledException {

        // по сути пустое кол-во ключей
        return new Query<KeyField, String>(MapFact.EMPTYREV(),
                table.join(DataObject.getMapExprs(keyFields)).getWhere()).execute(this, owner).size() > 0;
    }

    public void ensureRecord(StoredTable table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, TableOwner tableOwner, OperationOwner owner) throws SQLException, SQLHandledException {
        if (!isRecord(table, keyFields, owner))
            insertRecord(table, keyFields, propFields, tableOwner, owner);
    }

    public void updateRecords(StoredTable table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, OperationOwner owner, TableOwner tableOwner) throws SQLException, SQLHandledException {
        updateRecords(table, false, keyFields, propFields, owner, tableOwner);
    }

    public int updateRecordsCount(StoredTable table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, OperationOwner owner, TableOwner tableOwner) throws SQLException, SQLHandledException {
        return updateRecords(table, true, keyFields, propFields, owner, tableOwner);
    }

    private int updateRecords(StoredTable table, boolean count, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, OperationOwner owner, TableOwner tableOwner) throws SQLException, SQLHandledException {
        if(!propFields.isEmpty()) // есть запись нужно Update лупить
            return updateRecords(new ModifyQuery(table, new Query<>(table.getMapKeys(), Where.TRUE(), keyFields, ObjectValue.getMapExprs(propFields)), owner, tableOwner));
        if(count)
            return isRecord(table, keyFields, owner) ? 1 : 0;
        return 0;

    }

    public boolean insertRecord(StoredTable table, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, boolean update, TableOwner tableOwner, OperationOwner owner) throws SQLException, SQLHandledException {
        if(update && isRecord(table, keyFields, owner)) {
            updateRecords(table, keyFields, propFields, owner, tableOwner);
            return false;
        } else {
            insertRecord(table, keyFields, propFields, tableOwner, owner);
            return true;
        }
    }

    public Object readRecord(StoredTable table, ImMap<KeyField, DataObject> keyFields, PropertyField field, OperationOwner owner) throws SQLException, SQLHandledException {
        // по сути пустое кол-во ключей
        return new Query<>(MapFact.<KeyField, KeyExpr>EMPTYREV(),
                table.join(DataObject.getMapExprs(keyFields)).getExpr(field), "result", Where.TRUE()).
                execute(this, owner).singleValue().get("result");
    }

    public void truncate(DBTable table, OperationOwner owner) throws SQLException {
        truncate(table.getName(syntax), owner, TableOwner.global, false, register(table, TableOwner.global, TableChange.REMOVE));
    }
    
    public void truncateSession(String table, OperationOwner owner, Integer count, TableOwner tableOwner) throws SQLException {
        checkTableOwner(table, tableOwner);

//        lastOwner.put(table, tableOwner instanceof SessionTableUsage ? ((SessionTableUsage) tableOwner).stack + '\n' + ExceptionUtils.getStackTrace(): null);
        boolean useDeleteForm = count == null ? Settings.get().isDeleteFromInsteadOfTruncateForTempTablesUnknown() : count < Settings.get().getDeleteFromInsteadOfTruncateForTempTablesThreshold();
        truncate(syntax.getSessionTableName(table), owner, tableOwner, useDeleteForm, register(table, tableOwner,  useDeleteForm ? TableChange.DELETE : TableChange.REMOVE));
    }

    public void truncate(String table, OperationOwner owner, TableOwner tableOwner, boolean useDeleteFrom, RegisterChange registerChange) throws SQLException {
//        executeDML("TRUNCATE " + syntax.getSessionTableName(table));
        if(problemInTransaction == null) {
            String tableName = syntax.getSessionTableName(table);
            if(useDeleteFrom) {
                executeDML("DELETE FROM " + tableName, owner, tableOwner, registerChange);
                if(!isInTransaction())
                    executeDML(syntax.getVacuum(tableName), owner, tableOwner, registerChange);
            } else
                executeDDL("TRUNCATE TABLE " + tableName, StaticExecuteEnvironmentImpl.NOREADONLY, owner, registerChange); // нельзя использовать из-за : в транзакции в режиме "только чтение" нельзя выполнить TRUNCATE TABLE
        }
    }

    public int getSessionCount(String table, OperationOwner opOwner) throws SQLException {
        return getCount(syntax.getSessionTableName(table), opOwner);
    }

    public int getCount(StoredTable table, OperationOwner opOwner) throws SQLException {
        return getCount(table.getName(syntax), opOwner);
    }

    public int getCount(String table, OperationOwner opOwner) throws SQLException {
//        executeDML("TRUNCATE " + syntax.getSessionTableName(table));
        try {
            return (Integer)executeSelect("SELECT COUNT(*) AS cnt FROM " + table, opOwner, StaticExecuteEnvironmentImpl.EMPTY, MapFact.EMPTY(), 0, MapFact.singletonRev("cnt", "cnt"), MapFact.singleton("cnt", ValueExpr.COUNTCLASS), MapFact.<String, String>EMPTYREV(), MapFact.EMPTY()).singleKey().singleValue();
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public <X> int deleteKeyRecords(StoredTable table, ImMap<KeyField, X> keys, OperationOwner owner, TableOwner tableOwner) throws SQLException {
        checkTableOwner(table, tableOwner);
        
        String deleteWhere = keys.toString((key, value) -> key.getName(syntax) + "=" + value, " AND ");

        return executeDML("DELETE FROM " + table.getName(syntax) + (deleteWhere.length() == 0 ? "" : " WHERE " + deleteWhere), owner, tableOwner, register(table, tableOwner, TableChange.DELETE));
    }

    private static int readInt(Object value) {
        return ((Number)value).intValue();
    }

    public static Statement createSingleStatement(Connection connection) throws SQLException {
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
    public void readSingleValues(SessionTable table, Result<ImMap<KeyField, Object>> keyValues, Result<ImMap<PropertyField, Object>> propValues, Result<ImMap<KeyField, Integer>> statKeys, Result<ImMap<PropertyField, PropStat>> statProps, OperationOwner opOwner) throws SQLException {
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
            statKeys.set(tableKeys.isEmpty() ? MapFact.EMPTY() : MapFact.singleton(tableKeys.single(), table.count));
            if (table.properties.isEmpty()) {
                keyValues.set(MapFact.EMPTY());
                propValues.set(MapFact.EMPTY());
                statProps.set(MapFact.EMPTY());
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
                    ImFilterValueMap<KeyField, Integer> mStatKeys = tableKeys.mapFilterValues();
                    for (int i = 0, size = tableKeys.size(); i < size; i++) {
                        KeyField tableKey = tableKeys.get(i);
                        String fieldName = tableKey.getName();
                        int cnt = readInt(result.getObject(getCntDist(fieldName)));
                        if (cnt == 1)
                            mKeyValues.mapValue(i, tableKey.type.read(result, syntax, fieldName));
                        mStatKeys.mapValue(i, cnt);
                    }
                    keyValues.set(mKeyValues.immutableValue());
                    statKeys.set(mStatKeys.immutableValue());
                } else
                    keyValues.set(MapFact.EMPTY());

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
                ServerLoggers.assertLog(false, "UPDATED RETURNED TABLE : " + table + " " + owner + ", DEBUG INFO : " + sessionDebugInfo.get(table));
        } else {
            if(currentOwner != owner)
                ServerLoggers.assertLog(false, "UPDATED FOREIGN TABLE : " + table + " " + currentOwner + " " + owner + ", DEBUG INFO : " + sessionDebugInfo.get(table));
        }
    }
    private void checkTableOwner(StoredTable table, TableOwner owner) {
        if(table instanceof SessionTable)
            checkTableOwner(table.getName(), owner);
        else
            ServerLoggers.assertLog(owner == TableOwner.global || owner == TableOwner.debug, "THERE SHOULD BE NO OWNER FOR GLOBAL TABLE " + table + " " + owner);
    }
    private void checkTableOwner(ModifyQuery modify) {
        checkTableOwner(modify.table, modify.owner);
    }

    public int deleteRecords(ModifyQuery modify) throws SQLException, SQLHandledException {
        checkTableOwner(modify);
        
        if(modify.isEmpty()) // иначе exception кидает
            return 0;

        return executeDML(modify.getDelete(syntax, contextProvider));
    }

    public int updateRecords(ModifyQuery modify) throws SQLException, SQLHandledException {
        checkTableOwner(modify);
        return executeDML(modify.getUpdate(syntax, contextProvider));
    }

    public int insertSelect(ModifyQuery modify) throws SQLException, SQLHandledException {
        checkTableOwner(modify);
        return executeDML(modify.getInsertSelect(syntax, contextProvider));
    }
    public int insertSessionSelect(SQLExecute execute, final ERunnable out) throws SQLException, SQLHandledException {
        // out.run();

        try {
            return executeDML(execute);
        } catch(Throwable t) {
            Result<Throwable> firstException = new Result<>();
            firstException.set(t);

            if(!isInTransaction() && t instanceof SQLUniqueViolationException)
                runSuppressed(() -> {
                    try {
                        out.run();
                    } catch (Exception e) {
                        throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
                    }
                }, firstException);

            finishHandledExceptions(firstException);
            throw new UnsupportedOperationException();
        }

    }

    public int insertSessionSelect(String name, final IQuery<KeyField, PropertyField> query, final QueryEnvironment env, final TableOwner owner) throws SQLException, SQLHandledException {
        return insertSessionSelect(name, query, env, owner, MapFact.EMPTYORDER(), 0);
    }

    public int insertSessionSelect(String name, final IQuery<KeyField, PropertyField> query, final QueryEnvironment env, final TableOwner owner, ImOrderMap<PropertyField, Boolean> ordersTop, int selectTop) throws SQLException, SQLHandledException {
        checkTableOwner(name, owner);
        return insertSessionSelect(ModifyQuery.getInsertSelect(syntax.getSessionTableName(name), query, env, owner, syntax, contextProvider, null, register(name, owner, TableChange.INSERT), ordersTop, selectTop), () -> query.outSelect(SQLSession.this, env, true));
    }

    public int insertLeftSelect(ModifyQuery modify, boolean updateProps, boolean insertOnlyNotNull) throws SQLException, SQLHandledException {
        checkTableOwner(modify);
//        modify.getInsertLeftQuery(updateProps, insertOnlyNotNull).outSelect(this, modify.env);
        return executeDML(modify.getInsertLeftKeys(syntax, contextProvider, updateProps, insertOnlyNotNull));
    }

    public int modifyRecords(ModifyQuery modify) throws SQLException, SQLHandledException {
        try {
            return modifyRecords(modify, new Result<>());
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
        if (modify.table.isSingle()) {
            if (!isRecord(modify.table, MapFact.EMPTY(), modify.getOwner()))
                result = insertSelect(modify);
        } else
            result = insertLeftSelect(modify, false, insertOnlyNotNull);
        int updated = updateRecords(modify);
        proceeded.set(result + updated);
        return result;
    }
    
    @Override
    protected void onClose(OperationOwner owner) throws SQLException {
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

    private final static ReturnStatement keepStatement = (statement, runTime) -> {
    };

    private final static ReturnStatement closeStatement = (statement, runTime) -> statement.close();

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

    public void checkSessionTables(ImSet<Value> values) {
        for(Value paramObject : values)
            if(paramObject instanceof SessionTable) {
                checkSessionTable((SessionTable) paramObject);
            }
    }

    public void checkSessionTable(SessionTable table) {
        WeakReference<TableOwner> sessionTable = sessionTablesMap.get(table.getName());
        if(!(sessionTable != null && sessionTable.get() != null)) { // одна из возможных причин - DataSession.updateSessionNotChangedEvents
            ServerLoggers.assertLog(false, "USED RETURNED TABLE : " + table.getName() + ", DEBUG INFO : " + sessionDebugInfo.get(table.getName()), true);
            wasSessionTableAssertion.set(true);
        }
    }
    
    private final static ThreadLocal<Boolean> wasSessionTableAssertion = new ThreadLocal<>();
    public static void checkSessionTableAssertion(Modifier modifier) throws SQLException, SQLHandledException {
        if(wasSessionTableAssertion.get() != null) {
            outModifier("ASSERT", modifier);            
            wasSessionTableAssertion.remove();
        }
    }
    
    public static void outModifier(String info, Modifier modifier) throws SQLException, SQLHandledException {
        ServerLoggers.exInfoLogger.info(info + (modifier instanceof SessionModifier ? ((SessionModifier) modifier).out() : "\nDEFAULT") + "\npropertychanges : " + modifier.getPropertyChanges());
    }

    private PreparedStatement getStatement(SQLCommand command, ImMap<String, ParseInterface> paramObjects, final ExConnection connection, SQLSyntax syntax, DynamicExecEnvSnapshot snapEnv, Result<ReturnStatement> returnStatement, Result<Integer> length) throws SQLException {

        StaticExecuteEnvironment env = command.env;
        boolean poolPrepared = !env.isNoPrepare() && !Settings.get().isDisablePoolPreparedStatements() && command.getString().length() > Settings.get().getQueryPrepareLength();

        checkSessionTables(paramObjects);

        ImMap<String, String> reparse;
        if(BusinessLogics.useReparse && (reparse = BusinessLogics.reparse.get()) != null) { // временный хак
            paramObjects = paramObjects.addExcl(reparse.mapValues((String value) -> new StringParseInterface() {
                public String getString(SQLSyntax syntax1, StringBuilder envString, boolean usedRecursion) {
                    return value;
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
                returnStatement.set((statement, runTime) -> {
                    if(runTime > Settings.get().getQueryPrepareRunTime())
                        statementPool.put(connection.sql, parse, fParsed);
                    else
                        statement.close();
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

    private final static BiFunction<String, String, String> addFieldAliases = (key, value) -> value + " AS " + key;
    // вспомогательные методы

    public static String getSelect(SQLSyntax syntax, String fromSelect, ImMap<String, String> keySelect, ImMap<String, String> propertySelect, ImCol<String> whereSelect, int top) {
        return syntax.getSelect(fromSelect, stringExpr(keySelect, propertySelect), whereSelect.toString(" AND "), "", top > 0 ? String.valueOf(top) : "", false);
    }
    public static String getSelect(SQLSyntax syntax, String fromSelect, ImMap<String, String> keySelect, ImMap<String, String> propertySelect, ImCol<String> whereSelect) {
        return getSelect(syntax, fromSelect, keySelect.toOrderMap(), propertySelect.toOrderMap(), whereSelect);
    }
    public static String getSelect(SQLSyntax syntax, String fromSelect, ImOrderMap<String, String> keySelect, ImOrderMap<String, String> propertySelect, ImCol<String> whereSelect) {
        return syntax.getSelect(fromSelect, stringExpr(keySelect, propertySelect), whereSelect.toString(" AND "));
    }
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

    private final Object activeThreadLock = new Object();
    private WeakLinkedHashSet<Thread> activeThreads = new WeakLinkedHashSet<>(); // set для перестарта соединения и других async системных процессов
    private WeakReference<Thread> lastActiveSyncThread; // чтобы не терялся поток, так как activeThread не синхронизирован

    public Thread getActiveThread() {
        synchronized (activeThreadLock) {
            Thread activeThread = activeThreads.first();
            if(activeThread != null)
                return activeThread;

            return lastActiveSyncThread == null ? null : lastActiveSyncThread.get();
        }
    }

    private void setActiveThread(boolean async) {
        synchronized (activeThreadLock) {
            activeThreads.add(Thread.currentThread()); // не excl, потому как сначала lockWrite берет поток, а потом lockRead
        }
    }

    // the main usage - canceling sql statement of a particular thread
    // the problem is that statement.cancel can cancel the statement of another thread, since this method implementation is not synchronized for connection
    // so we have to guarantee that statement canceling is done for the specific thread
    private void runSyncActiveThread(Thread thread, SQLRunnable runnable) throws SQLException, SQLHandledException {
        synchronized (activeThreadLock) {
            if (activeThreads.contains(thread))
                runnable.run();
        }
    }

    private void dropActiveThread(boolean async) {
        synchronized (activeThreadLock) {
            Thread currentThread = Thread.currentThread();
            activeThreads.remove(currentThread);
            if(!async)
                lastActiveSyncThread = new WeakReference<>(currentThread);
        }
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
        runLockReadOperation(() -> {
            for(String table : privateConnection.temporary.getTables()) {
                long timeStamp;
                if (!sessionTablesMap.containsKey(table) && (timeStamp = getTimeStamp(table)) < beforeTime)
                    notUsedTables.add(new TableUsage(SQLSession.this, table, timeStamp));
                else
                    recentlyUsedTables.set(recentlyUsedTables.result + 1);
            }
        });
    }

    private void cleanNotUsedTable(final String table, final long timeStamp) throws SQLException, SQLHandledException {
        runLockReadOperation(() -> {
            if(!sessionTablesMap.containsKey(table) && timeStamp == getTimeStamp(table)) { // double check, not used and the same time stamp
                if(privateConnection.temporary.getTables().contains(table)) { // тут теоретически raceCondition'ов может быть очень много
                    lastReturnedStamp.remove(table);
                    privateConnection.temporary.removeTable(table);
                    dropTemporaryTableFromDB(table);
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

    public static void restartConnections(Result<Double> prevStart) {
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
                for (Map.Entry<ConnectionPool, Connection> notUsedConnection : notUsedNewConnections.entrySet())
                    notUsedConnection.getKey().closeRestartConnection(notUsedConnection.getValue());
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
        // assertLock
        runLockReadOperation(() -> {
            Result<String> description = new Result<>(null);
            usedConnections.add(new ConnectionUsage(SQLSession.this, getScore(readDescription ? description : null, false), description.result));
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
            description.set("Backend : " + backend + ", Info : (" + contextProvider.getLogInfo()+ "), SCORE : " + score + "(used temp table rows : " + usedTablesSize + ", time started : " + timeStarted + ", max used temp table rows : " + maxUsedTables + ", last temp tables activity : " + lastTempTablesActivity + ", time score : " + timeScore + ", length score : " + lengthScore + ")");
        }

        return score;
    }

    public boolean isRestarting;

    private boolean restartConnection(double score, Map<ConnectionPool, Connection> notUsedConnections) throws SQLException {
        if(isClosed())
            return false;

        Connection newConnection = notUsedConnections.remove(connectionPool);
        if(newConnection == null)
            newConnection = connectionPool.newRestartConnection(); // за пределами lockWrite чтобы не задерживать connection

        boolean noError = false;
        try {
            boolean locked = tryLockWrite(OperationOwner.unknown);
            try {
                if(locked)
                    isRestarting = true;
                    
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
                } catch (Throwable t) { // если проблема в SQL, пишем в лог / игнорируем
                    logger.error("RESTART CONNECTION ERROR : " + description.result, t);
                    return false;
                }

                // закрываем старое соединение
                connectionPool.closeRestartConnection(privateConnection.sql);

                noError = true;

                // если все ок, подменяем connection
                privateConnection.sql = newConnection;
                privateConnection.restartConnection(newConnection, contextProvider);
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
                if(locked) {
                    isRestarting = false;
                    unlockWrite(true);
                }
            }
        } finally {
            if(!noError)
                connectionPool.closeRestartConnection(newConnection);
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
    public static void dropTemporaryTableFromDB(Connection connection, SQLSyntax syntax, String table, OperationOwner owner) throws SQLException {
        Pair<String, StaticExecuteEnvironment> result = getDropDDL(table, syntax);
        executeDDL(result, connection, NOTYPES, owner);
    }
    private void vacuumAnalyzeSessionTable(Connection connection, String table, OperationOwner owner) throws SQLException {
        vacuumAnalyzeSessionTable(table, connection, typePool, syntax, owner);
    }
    private static void vacuumAnalyzeSessionTable(String table, Connection connection, TypePool typePool, SQLSyntax syntax, OperationOwner owner) throws SQLException {
        Pair<String, StaticExecuteEnvironment> ddl = getVacuumAnalyzeDDL(syntax.getSessionTableName(table), syntax);
        executeDDL(ddl, connection, typePool, owner);
    }

    private static void executeDDL(Pair<String, StaticExecuteEnvironment> result, Connection connection, TypePool typePool, OperationOwner owner) throws SQLException {
        String command = result.first;
        StaticExecuteEnvironment env = result.second;

        Statement statement = null;

        Result<Throwable> firstException = new Result<>();
        Object prevEnvState = null;
        try {
            prevEnvState = env.before(connection, typePool, command, owner);

            statement = createSingleStatement(connection);

            statement.execute(command);

        } catch (Throwable e) {
            logger.error(statement == null ? "PREPARING STATEMENT" : statement.toString() + " " + e.getMessage());
            firstException.set(e);
        }

        afterStatementExecute(connection, typePool, owner, result, statement, firstException, prevEnvState);
    }
    public void insertSessionBatchRecords(Connection connection, String table, ImMap<ImMap<KeyField, Object>, ImMap<PropertyField, Object>> rows, OperationOwner opOwner) throws SQLException {
        insertBatchRecords(connection, typePool, syntax, table, rows, opOwner);
    }
    private static void insertBatchRecords(final Connection connection, final TypePool typePool, SQLSyntax syntax, String table, ImMap<ImMap<KeyField, Object>, ImMap<PropertyField, Object>> rows, final OperationOwner opOwner) throws SQLException {
        if(rows.isEmpty())
            return;

        table = syntax.getSessionTableName(table);
        Parser<Object, Object> parser = dataParser;
                
        ImOrderSet<KeyField> keys = rows.getKey(0).keys().toOrderSet();
        ImOrderSet<PropertyField> properties = rows.getValue(0).keys().toOrderSet();

        final Pair<String, StaticExecuteEnvironment> dml = getInsertDML(table, keys, properties, syntax);

        PreparedStatement statement = null;

        Result<Throwable> firstException = new Result<>();
        Object prevEnvState = null;
        try {
            prevEnvState = dml.second.before(connection, typePool, dml.first, opOwner);

            statement = connection.prepareStatement(dml.first);
            insertBatch(statement, keys, rows, parser, properties, syntax);

        } catch (Throwable e) {
            logger.error(statement == null ? "PREPARING STATEMENT" : statement.toString() + " " + e.getMessage());
            firstException.set(e);
        }

        afterStatementExecute(connection, typePool, opOwner, dml, statement, firstException, prevEnvState);
    }

    private static void afterStatementExecute(final Connection connection, final TypePool typePool, final OperationOwner opOwner, final Pair<String, StaticExecuteEnvironment> dml, final Statement statement, Result<Throwable> firstException, final Object prevEnvState) throws SQLException {
        if (statement != null) {
            runSuppressed(statement::close, firstException);
        }

        runSuppressed(() -> dml.second.after(connection, typePool, dml.first, opOwner, prevEnvState), firstException);

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

    private static TypePool NOTYPES = new TypePool() {
        public void ensureRecursion(Object types) {
            throw new UnsupportedOperationException();
        }
        public void ensureConcType(ConcatenateType concType) {
            throw new UnsupportedOperationException();
        }
        public void ensureSafeCast(Pair<Type, Integer> type) {
            throw new UnsupportedOperationException();
        }
        public void ensureGroupAggOrder(Pair<GroupType, ImList<Type>> groupAggOrder) {
            throw new UnsupportedOperationException();
        }
        public void ensureTypeFunc(Pair<TypeFunc, Type> tf) {
            throw new UnsupportedOperationException();
        }
        public void ensureArrayClass(ArrayClass arrayClass) {
            throw new UnsupportedOperationException();
        }
    };
    public static void uploadTableToConnection(final String table, SQLSyntax syntax, final JDBCTable tableData, final Connection sqlTo, final OperationOwner owner) throws SQLException {
        final KeyField keyCount = new KeyField("row", ValueExpr.COUNTCLASS);
        final ImRevMap<String, PropertyField> properties = tableData.fields.getSet().mapRevValues((String value) -> new PropertyField(value, tableData.fieldTypes.get(value)));
        
        TypePool typePool = NOTYPES; 
        
        createTemporaryTable(sqlTo, typePool, syntax,  table, SetFact.singletonOrder(keyCount), properties.valuesSet(), owner);

        insertBatchRecords(sqlTo, typePool, syntax, table, tableData.set.toIndexedMap().mapKeyValues(value -> MapFact.singleton(keyCount, value), properties::crossJoin), owner);

        vacuumAnalyzeSessionTable(table, sqlTo, typePool, syntax, owner);
    }

    private void readData(String table, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ResultHandler<KeyField, PropertyField> reader, OperationOwner owner) throws SQLException, SQLHandledException {
        ImRevMap<KeyField, String> keyNames = keys.getSet().mapRevValues(Field.nameGetter());
        ImRevMap<PropertyField, String> propNames = properties.mapRevValues(Field.nameGetter());

        ImRevMap<String, KeyField> revKeyNames = keyNames.reverse();
        ImRevMap<String, PropertyField> revPropNames = propNames.reverse();
        String select = "SELECT " + SQLSession.stringExpr(revKeyNames.mapValues(Field.nameGetter(syntax)),
                revPropNames.mapValues(Field.nameGetter(syntax))) + " FROM " + syntax.getSessionTableName(table);

        executeSelect(select, owner, StaticExecuteEnvironmentImpl.EMPTY, keyNames, revKeyNames.mapValues(Field.fnTypeGetter()), propNames, revPropNames.mapValues(Field.fnTypeGetter()), reader);
    }

    @Override
    public void close() throws SQLException {
        explicitClose();
    }

    public Integer getTransactTimeouts() {
        return attemptCountMap.get(SQLTimeoutException.ADJUSTTRANSTIMEOUT);
    }

    public void registerNeedSavePoint() {
        connectionPool.registerNeedSavePoint();
    }

    public void unregisterNeedSavePoint() {
        connectionPool.unregisterNeedSavePoint();
    }

    public boolean registerUseSavePoint() {
        return connectionPool.registerUseSavePoint();
    }

    public void unregisterUseSavePoint() {
        connectionPool.unregisterUseSavePoint();
    }
}
