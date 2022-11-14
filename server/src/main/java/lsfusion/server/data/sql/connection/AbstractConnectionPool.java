package lsfusion.server.data.sql.connection;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.mutability.MutableObject;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.sql.table.SQLTemporaryPool;
import lsfusion.server.logics.navigator.controller.env.SQLSessionContextProvider;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.postgresql.PGConnection;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractConnectionPool implements ConnectionPool {

    public abstract Connection startConnection() throws SQLException;
    public abstract SQLSyntax getSyntax();

    private ExConnection common;

    private abstract class SyncRun<T> {
    }

    private abstract class SyncReRun<T> extends SyncRun<T> {
        public T execute() throws SQLException {
            T result;
            synchronized (lock) {
                result = execute(null);
            }
            if(result == null) {
                T rerun = rerun();
                try {
                    synchronized (lock) {
                        result = execute(rerun);
                    }
                } finally {
                    if(rerun != result) // если не был использован результат, закрываем его
                        close(rerun);
                }
            }
            return result;
        }

        protected abstract T execute(T rerun) throws SQLException;

        protected abstract T rerun() throws SQLException;

        protected abstract void close(T rerun) throws SQLException;
    }

    public abstract class SyncNewExConnectionReRun extends SyncReRun<ExConnection> {
        protected ExConnection rerun() throws SQLException {
            return newExConnection();
        }

        protected void close(ExConnection rerun) throws SQLException {
            closeExConnection(rerun);
        }
    }

    public abstract class SyncNewConnectionReRun extends SyncReRun<Connection> {
        protected Connection rerun() throws SQLException {
            return newConnection();
        }

        protected void close(Connection rerun) throws SQLException {
            closeConnection(rerun);
        }
    }

    private abstract class SyncAfterRun<T> extends SyncRun<Object> {

        public void execute() throws SQLException {
            T after;
            synchronized (lock) {
                after = executeAfter();
            }
            after(after);
        }

        protected abstract T executeAfter() throws SQLException;

        protected abstract void after(T run) throws SQLException;
    }

    public ExConnection getCommon(MutableObject object, SQLSessionContextProvider contextProvider) throws SQLException {
        if(Settings.get().isCommonUnique())
            return getPrivate(object, contextProvider);
        else
            return new SyncNewExConnectionReRun() {
                public ExConnection execute(ExConnection rerun) {
                    if(common==null) {
                        if(rerun != null)
                            common = rerun;
                        else
                            return null;
                    }
                    return common;
                }
            }.execute();
    }

    public void returnCommon(MutableObject object, ExConnection connection) throws SQLException {
        if(Settings.get().isCommonUnique())
            returnPrivate(object, connection);
        else
            assert common==connection;
    }

    public boolean restoreCommon(final Connection connection) throws SQLException {
        assert !Settings.get().isCommonUnique();
        return !new SyncNewConnectionReRun() {
            public Connection execute(Connection rerun) throws SQLException {
                if(common.sql == connection) { // мог восстановиться кем-то другим
                    assert common.temporary.isEmpty();
                    if(rerun != null)
                        common.sql = rerun;
                    else
                        return null;
                    ServerLoggers.handledLog("RESTORED COMMON " + common.sql.isClosed());
                } else
                    ServerLoggers.handledLog("SOMEBODY RESTORED COMMON " + common.sql + " CONNECTION " + connection + " " + common.sql.isClosed() + " " + connection.isClosed());
                return common.sql;
            }

            public Connection rerun() throws SQLException {
                return newConnection();
            }
        }.execute().isClosed();
    }

    private final Object lock = new Object();
    private final Map<ExConnection, WeakReference<MutableObject>> usedConnections = MapFact.mAddRemoveMap(); // обычный map так как надо добавлять, remove'ить
    private final Stack<ExConnection> freeConnections = new Stack<>();

    private void checkUsed() throws SQLException {
        new SyncAfterRun<List<ExConnection>>() {
            protected List<ExConnection> executeAfter() {
                List<ExConnection> connectionsToClose = new ArrayList<>();
                Iterator<Map.Entry<ExConnection,WeakReference<MutableObject>>> it = usedConnections.entrySet().iterator();
                while(it.hasNext()) {
                    Map.Entry<ExConnection, WeakReference<MutableObject>> usedEntry = it.next();
                    if(usedEntry.getValue().get()==null) {
                        it.remove(); // можно было бы попробовать использовать повторно, но connection может быть "грязным" то есть с транзакцией или неочмщенными временными таблицами
                        connectionsToClose.add(usedEntry.getKey());
                    }
                }
                return connectionsToClose;
            }

            protected void after(List<ExConnection> run) throws SQLException {
                for(ExConnection connection : run)
                    closeExConnection(connection);
            }
        }.execute();
    }

    private void addFreeConnection(ExConnection connection) throws SQLException {
        // assert что synchronized lock
        if(freeConnections.size() < Settings.get().getFreeConnections())
            freeConnections.push(connection);
        else
            closeExConnection(connection);
    }

    public ExConnection newExConnection() throws SQLException {
        return new ExConnection(newConnection(), new SQLTemporaryPool());
    }
    
    public void closeExConnection(ExConnection connection) throws SQLException {
        closeConnection(connection.sql);
    }

    protected void prepareConnection(Connection connection) {
    }

    private AtomicInteger connectionsCount = new AtomicInteger();

    public Connection newConnection() throws SQLException {
        long l = System.currentTimeMillis();

        Connection newConnection = startConnection();
        prepareConnection(newConnection);
        SQLSession.setACID(newConnection, false, getSyntax());

        logConnection("NEW", l, connectionsCount.incrementAndGet(), ((PGConnection)newConnection).getBackendPID());

        return newConnection;
    }

    public void closeConnection(Connection connection) throws SQLException {
        long l = System.currentTimeMillis();

        int backendPID = ((PGConnection) connection).getBackendPID();

        connection.close();

        logConnection("CLOSE", l, connectionsCount.getAndDecrement(), backendPID);
    }

    private static void logConnection(String type, long l, long cc, int backendPID) {
        String message = type + " CONNECTION : " + backendPID + (l > 0 ? ", Time : " + (System.currentTimeMillis() - l) : "") + ", Current connections count : " + cc;
        ServerLoggers.sqlConnectionLog(message);
    }

    @Override
    public Connection newRestartConnection() throws SQLException {
        return newConnection();
    }

    @Override
    public void closeRestartConnection(Connection connection) throws SQLException {
        closeConnection(connection);
    }

    public ExConnection getPrivate(final MutableObject object, SQLSessionContextProvider contextProvider) throws SQLException {
        ExConnection connection;
        if(Settings.get().isDisablePoolConnections()) {
            connection = newExConnection();
        } else {
            checkUsed();

            connection = new SyncNewExConnectionReRun() {
                public ExConnection execute(ExConnection rerun) {
                    ExConnection freeConnection;
                    if (rerun != null && freeConnections.size() < Settings.get().getFreeConnections()) {
                        freeConnection = rerun;
                    } else {
                        if (freeConnections.isEmpty())
                            return null;
                        freeConnection = freeConnections.pop();
                        logConnection("NEW CONNECTION FROM CACHE (size : " + freeConnections.size() + ")", -1, connectionsCount.get(), ((PGConnection) freeConnection.sql).getBackendPID());
                    }

                    usedConnections.put(freeConnection, new WeakReference<>(object));
                    return freeConnection;
                }
            }.execute();
        }
        connection.updateContext(contextProvider);
        return connection;
    }

    public void returnPrivate(final MutableObject object, final ExConnection connection) throws SQLException {
        if(Settings.get().isDisablePoolConnections()) {
            closeExConnection(connection);
            return;
        }

        new SyncAfterRun<ExConnection>() {
            protected ExConnection executeAfter() throws SQLException {
                WeakReference<MutableObject> weakObject = usedConnections.remove(connection);
                assert weakObject.get() == object;
                if (!connection.sql.isClosed()) {
                    assert connection.sql.getAutoCommit();

                    // assert что synchronized lock
                    if (freeConnections.size() < Settings.get().getFreeConnections()) {
                        freeConnections.push(connection);
                        logConnection("CLOSE CONNECTION TO CACHE (size : " + freeConnections.size() + ")", -1, connectionsCount.get(), ((PGConnection) connection.sql).getBackendPID());
                    } else
                        return connection;
                }
                return null;
            }

            protected void after(ExConnection run) throws SQLException {
                if(run != null)
                    closeExConnection(run);
            }
        }.execute();
    }

    private AtomicInteger neededSavePoints = new AtomicInteger();
    private AtomicInteger usedSavePoints = new AtomicInteger();
    
    private Integer useSavePointsThreshold;
    private double useSavePointsThresholdMultiplier = 1.0;

    @Override
    public void registerNeedSavePoint() {
        neededSavePoints.getAndDecrement();
    }

    @Override
    public void unregisterNeedSavePoint() {
        neededSavePoints.decrementAndGet();
    }

    private int getUseSafeThreshold() {
        if(useSavePointsThreshold == null)
            useSavePointsThreshold = Settings.get().getUseSavePointsThreshold();
        
        return (int) (useSavePointsThreshold * useSavePointsThresholdMultiplier);
    }
    
    @Override
    public boolean registerUseSavePoint() {
        int usedCount = usedSavePoints.getAndIncrement();
        boolean useSavePoint = usedCount < getUseSafeThreshold();
        if(useSavePoint)
            ServerLoggers.sqlConnectionLogger.info("CREATE SAVEPOINT, CURRENT : " + usedCount);
        return useSavePoint;        
    }

    @Override
    public void unregisterUseSavePoint() {
        int usedCount = usedSavePoints.decrementAndGet();
        ServerLoggers.sqlConnectionLogger.info("DROP SAVEPOINT, CURRENT : " + usedCount);
    }
    
    private int updateCount;
    private int sufficientCount;
    
    public void updateSavePointsInfo(Result<Long> prevResult) {
        
        updateCount++;        
        if(neededSavePoints.get() <= getUseSafeThreshold())
            sufficientCount++;

        long currentTime = System.currentTimeMillis();
        if(prevResult.result == null)
            prevResult.set(currentTime);
        
        long resultPeriod = Settings.get().getUpdateSavePointsResultPeriod();                

        if((currentTime - prevResult.result) / 1000 > resultPeriod) {
            prevResult.set(null);
            
            // the same as in initLRUTuner
            double averageRate = 0.7;
            double safeInterval = 0.1;

            long upThreshold = (long) ((averageRate + safeInterval) * updateCount);
            long downThreshold = (long) ((averageRate - safeInterval) * updateCount);
            
            if(sufficientCount > upThreshold) {
                useSavePointsThresholdMultiplier = BaseUtils.max(useSavePointsThresholdMultiplier / Settings.get().getUpdateSavePointsCoeff(), Settings.get().getUpdateSavePointsMinMultiplier());
                ServerLoggers.sqlConnectionLogger.info("DEC SAVEPOINT MULTI " + useSavePointsThresholdMultiplier);
            }
            if(sufficientCount < downThreshold) {
                useSavePointsThresholdMultiplier = BaseUtils.min(useSavePointsThresholdMultiplier * Settings.get().getUpdateSavePointsCoeff(), Settings.get().getUpdateSavePointsMaxMultiplier());
                ServerLoggers.sqlConnectionLogger.info("INC SAVEPOINT MULTI " + useSavePointsThresholdMultiplier);
            }                
            
            updateCount = 0;
            sufficientCount = 0;
        }
    }
}
