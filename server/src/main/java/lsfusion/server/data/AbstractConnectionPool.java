package lsfusion.server.data;

import lsfusion.server.ServerLoggers;
import lsfusion.base.MutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.server.Settings;
import lsfusion.server.data.sql.SQLSyntax;
import org.postgresql.PGConnection;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public abstract class AbstractConnectionPool implements ConnectionPool {

    public abstract Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

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
            rerun.close();
        }
    }

    public abstract class SyncNewConnectionReRun extends SyncReRun<Connection> {
        protected Connection rerun() throws SQLException {
            return newConnection();
        }

        protected void close(Connection rerun) throws SQLException {
            rerun.close();
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

    public ExConnection getCommon(MutableObject object) throws SQLException {
        if(Settings.get().isCommonUnique())
            return getPrivate(object);
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
                List<ExConnection> connectionsToClose = new ArrayList<ExConnection>();
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
                    connection.close();
            }
        }.execute();
    }

    private void addFreeConnection(ExConnection connection) throws SQLException {
        // assert что synchronized lock
        if(freeConnections.size() < Settings.get().getFreeConnections())
            freeConnections.push(connection);
        else
            connection.close();
    }

    public ExConnection newExConnection() throws SQLException {
        return new ExConnection(newConnection(), new SQLTemporaryPool());
    }

    protected void prepareConnection(Connection connection) {
    } 
    
    public Connection newConnection() throws SQLException {
        try {
            long l = System.currentTimeMillis();

            Connection newConnection = startConnection();
            prepareConnection(newConnection);
            SQLSession.setACID(newConnection, false, (SQLSyntax)this);

            ServerLoggers.sqlConnectionLogger.info("NEW CONNECTION : " + ((PGConnection)newConnection).getBackendPID() + ", Time : " + (System.currentTimeMillis() - l));

            return newConnection;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Connection newRestartConnection() throws SQLException {
        return newConnection();
    }

    public ExConnection getPrivate(final MutableObject object) throws SQLException {
        if(Settings.get().isDisablePoolConnections())
            return newExConnection();

        checkUsed();

        return new SyncNewExConnectionReRun() {
            public ExConnection execute(ExConnection rerun) throws SQLException {
                ExConnection freeConnection;
                if (freeConnections.isEmpty()) {
                    if(rerun != null)
                        freeConnection = rerun;
                    else
                        return null;
                } else
                    freeConnection = freeConnections.pop();

                usedConnections.put(freeConnection, new WeakReference<>(object));
                return freeConnection;
            }
        }.execute();
    }

    public void returnPrivate(final MutableObject object, final ExConnection connection) throws SQLException {
        if(Settings.get().isDisablePoolConnections()) {
            connection.close();
            return;
        }

        new SyncAfterRun<ExConnection>() {
            protected ExConnection executeAfter() throws SQLException {
                WeakReference<MutableObject> weakObject = usedConnections.remove(connection);
                assert weakObject.get() == object;
                if (!connection.sql.isClosed()) {
                    assert connection.sql.getAutoCommit();

                    // assert что synchronized lock
                    if (freeConnections.size() < Settings.get().getFreeConnections())
                        freeConnections.push(connection);
                    else
                        return connection;
                }
                return null;
            }

            protected void after(ExConnection run) throws SQLException {
                if(run != null)
                    run.close();
            }
        }.execute();
    }

    public void restorePrivate(ExConnection connection) throws SQLException {
        connection.sql = newConnection();
        connection.temporary = new SQLTemporaryPool();
    }
}
