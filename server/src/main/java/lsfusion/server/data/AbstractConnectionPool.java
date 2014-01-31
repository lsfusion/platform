package lsfusion.server.data;

import lsfusion.server.ServerLoggers;
import org.postgresql.PGConnection;
import lsfusion.base.MutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.server.Settings;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

public abstract class AbstractConnectionPool implements ConnectionPool {

    public abstract Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    private ExConnection common;

    public ExConnection getCommon(MutableObject object) throws SQLException {
        if(Settings.get().isCommonUnique())
            return getPrivate(object);
        else {
            synchronized(lock) {
                if(common==null)
                    common = newExConnection();
                return common;
            }
        }
    }

    public void returnCommon(MutableObject object, ExConnection connection) throws SQLException {
        if(Settings.get().isCommonUnique())
            returnPrivate(object, connection);
        else
            assert common==connection;
    }

    public boolean restoreCommon() throws SQLException {
        assert !Settings.get().isCommonUnique(); 
        synchronized(lock) {
            if(common.sql.isClosed()) { // мог восстановиться кем-то другим
                common.sql = newConnection();
                assert common.temporary.isEmpty();
                ServerLoggers.sqlHandLogger.info("RESTORED COMMON " + common.sql.isClosed());
                return !common.sql.isClosed();
            }
        }

        return true;
    }

    private final Object lock = new Object();
    private final Map<ExConnection, WeakReference<MutableObject>> usedConnections = MapFact.mAddRemoveMap(); // обычный map так как надо добавлять, remove'ить
    private final Stack<ExConnection> freeConnections = new Stack<ExConnection>();

    private void checkUsed() throws SQLException {
        synchronized(lock) {
            Iterator<Map.Entry<ExConnection,WeakReference<MutableObject>>> it = usedConnections.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<ExConnection, WeakReference<MutableObject>> usedEntry = it.next();
                if(usedEntry.getValue().get()==null) {
                    it.remove(); // можно было бы попробовать использовать повторно, но connection может быть "грязным" то есть с транзакцией или временными таблицами
                    usedEntry.getKey().close();
                }
            }
        }
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

    public Connection newConnection() throws SQLException {
        try {
            Connection newConnection = startConnection();
            ((PGConnection)newConnection).setPrepareThreshold(2);
            SQLSession.setACID(newConnection, false);
            return newConnection;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public ExConnection getPrivate(MutableObject object) throws SQLException {
        if(Settings.get().isDisablePoolConnections())
            return newExConnection();

        checkUsed();

        synchronized(lock) {
            ExConnection freeConnection = freeConnections.isEmpty() ? newExConnection() : freeConnections.pop();

            usedConnections.put(freeConnection, new WeakReference<MutableObject>(object));
            return freeConnection;
        }
    }

    public void returnPrivate(MutableObject object, ExConnection connection) throws SQLException {
        if(Settings.get().isDisablePoolConnections()) {
            connection.close();
            return;
        }

        synchronized(lock) {
            WeakReference<MutableObject> weakObject = usedConnections.remove(connection);
            assert weakObject.get() == object;
            if(!connection.sql.isClosed()) {
                assert connection.sql.getAutoCommit();
                addFreeConnection(connection);
            }
        }
    }

    public void restorePrivate(ExConnection connection) throws SQLException {
        connection.sql = newConnection();
        connection.temporary = new SQLTemporaryPool();
    }
}
