package lsfusion.server.data.sql.connection;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.lambda.EConsumer;
import lsfusion.base.mutability.MutableObject;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.sql.table.SQLTemporaryPool;
import lsfusion.server.logics.navigator.controller.env.SQLSessionContextProvider;
import lsfusion.server.logics.navigator.controller.env.SQLSessionLSNProvider;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.postgresql.PGConnection;
import org.postgresql.replication.LogSequenceNumber;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractConnectionPool implements ConnectionPool {

    public static final LogSequenceNumber NO_SUBSCRIPTION = LogSequenceNumber.valueOf(1);
    protected abstract LogSequenceNumber getSlaveLSN(Connection connection) throws SQLException;

    protected abstract Connection startConnection(Integer needServer, LogSequenceNumber lsn, Connection prevConnection) throws SQLException;
    protected abstract void stopConnection(Connection connection, EConsumer<Connection, SQLException> cleaner) throws SQLException;
    public abstract SQLSyntax getSyntax();

    protected abstract boolean checkLSN(Connection connection, LogSequenceNumber lsn) throws SQLException;

    private final Map<ExConnection, WeakReference<MutableObject>> usedConnections = MapFact.mAddRemoveMap(); // обычный map так как надо добавлять, remove'ить
    private final List<ExConnection> freeConnections = new ArrayList<>();

    private void checkUsed() throws SQLException {
        List<ExConnection> connectionsToClose;
        synchronized (usedConnections) {
            connectionsToClose = new ArrayList<>();
            Iterator<Map.Entry<ExConnection, WeakReference<MutableObject>>> it = usedConnections.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ExConnection, WeakReference<MutableObject>> usedEntry = it.next();
                if (usedEntry.getValue().get() == null) {
                    it.remove();
                    connectionsToClose.add(usedEntry.getKey());
                }
            }
        }

        for(ExConnection connection : connectionsToClose)
            closeExConnection(connection, false); // we could try reusing it, but the connection may be “dirty,” i.e., with a transaction or uncommitted temporary tables
    }

    public ExConnection newExConnection(SQLSessionLSNProvider lsn) throws SQLException {
        return new ExConnection(newConnection(null, lsn, null), new SQLTemporaryPool());
    }
    
    public void closeExConnection(ExConnection connection, boolean clean) throws SQLException {
        closeConnection(connection.sql, clean ? SQLSession.getCleaner(connection.temporary.getTables(), getSyntax()) : null);
    }

    private AtomicInteger connectionsCount = new AtomicInteger();

    private Connection safeStartConnection(Integer needServer, LogSequenceNumber lsn, Connection prevConnection, int count) throws SQLException {
        try {
            return startConnection(needServer, lsn, prevConnection);
        } catch (SQLException e) {
            if(count < Settings.get().getNewConnectionAttempts()) {
                ServerLoggers.sqlSuppLog(e);
                return safeStartConnection(needServer, lsn, prevConnection, count + 1);
            }

            throw e;
        }
    }
    public Connection newConnection(Integer needServer, SQLSessionLSNProvider lsnProvider, Connection prevConnection) throws SQLException {
        long l = System.currentTimeMillis();

        Connection newConnection = safeStartConnection(needServer, lsnProvider.getLSN(), prevConnection, 0);
        if(newConnection == null)
            return null;

        SQLSession.setACID(newConnection, false, getSyntax());

        logConnection("NEW", l, connectionsCount.incrementAndGet(), ((PGConnection)newConnection).getBackendPID());

        return newConnection;
    }

    public void closeConnection(Connection connection, EConsumer<Connection, SQLException> cleaner) throws SQLException {
        long l = System.currentTimeMillis();

        int backendPID = ((PGConnection) connection).getBackendPID();

        stopConnection(connection, cleaner);

        logConnection("CLOSE", l, connectionsCount.getAndDecrement(), backendPID);
    }

    private static void logConnection(String type, long l, long cc, int backendPID) {
        String message = type + " CONNECTION : " + backendPID + (l > 0 ? ", Time : " + (System.currentTimeMillis() - l) : "") + ", Current connections count : " + cc;
        ServerLoggers.sqlConnectionLog(message);
    }

    @Override
    public Connection newRestartConnection(SQLSessionLSNProvider lsn) throws SQLException {
        return newConnection(null, lsn, null);
    }

    @Override
    public void closeRestartConnection(Connection connection, EConsumer<Connection, SQLException> cleaner) throws SQLException {
        closeConnection(connection, cleaner);
    }

    public ExConnection getConnection(final MutableObject object, SQLSessionLSNProvider lsn, SQLSessionContextProvider contextProvider) throws SQLException {
        ExConnection connection = null;
        boolean disablePoolConnections = Settings.get().isDisablePoolConnections();
        if (!disablePoolConnections) {
            checkUsed();

            synchronized (freeConnections) {
                for (int i = 0, size = freeConnections.size(); i < size; i++) {
                    connection = freeConnections.get(i);
                    if (checkLSN(connection.sql, lsn.getLSN())) {
                        freeConnections.remove(i);
                        logConnection("NEW CONNECTION FROM CACHE (size : " + freeConnections.size() + ")", -1, connectionsCount.get(), ((PGConnection) connection.sql).getBackendPID());
                        break;
                    } else
                        connection = null;
                }
            }
        }

        if(connection == null)
            connection = newExConnection(lsn);

        if (!disablePoolConnections) {
            synchronized (usedConnections) {
                usedConnections.put(connection, new WeakReference<>(object));
            }
        }
        connection.updateContext(false, contextProvider);
        return connection;
    }

    @Override
    public Connection getBalanceConnection(Integer needServer, SQLSessionLSNProvider lsn, Connection prevConnection) throws SQLException {
        return newConnection(needServer, lsn, prevConnection);
    }

    @Override
    public void returnBalanceConnection(Connection connection, EConsumer<Connection, SQLException> cleaner) throws SQLException {
        closeConnection(connection, cleaner);
    }

    public void returnConnection(final MutableObject object, final ExConnection connection) throws SQLException {
        boolean close = true;
        boolean clean = true;
        if (!connection.sql.isClosed()) {
            if(!Settings.get().isDisablePoolConnections()) {
                synchronized (usedConnections) {
                    WeakReference<MutableObject> weakObject = usedConnections.remove(connection);
                    assert weakObject.get() == object;
                    assert connection.sql.getAutoCommit();
                }

                synchronized (freeConnections) {
                    // assert что synchronized lock
                    if (freeConnections.size() < Settings.get().getFreeConnections()) {
                        freeConnections.add(connection);
                        logConnection("CLOSE CONNECTION TO CACHE (size : " + freeConnections.size() + ")", -1, connectionsCount.get(), ((PGConnection) connection.sql).getBackendPID());
                        close = false;
                    }
                }
            }
        } else
            clean = false;

        if(close)
            closeExConnection(connection, clean);
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
