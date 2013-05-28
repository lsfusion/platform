package platform.server.data;

import org.postgresql.PGConnection;
import platform.base.MutableObject;
import platform.base.col.MapFact;
import platform.server.Settings;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

public abstract class AbstractConnectionPool implements ConnectionPool {

    public abstract Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    private Connection common;

    public Connection getCommon(MutableObject object) throws SQLException {
        if(Settings.get().isCommonUnique())
            return getPrivate(object);
        else {
            synchronized(lock) {
                if(common==null)
                    common = newConnection();
                return common;
            }
        }
    }

    public void returnCommon(MutableObject object, Connection connection) throws SQLException {
        if(Settings.get().isCommonUnique())
            returnPrivate(object, connection);
        else
            assert common==connection;
    }

    private final Object lock = new Object();
    private final Map<Connection, WeakReference<MutableObject>> usedConnections = MapFact.mAddRemoveMap(); // обычный map так как надо добавлять, remove'ить
    private final Stack<Connection> freeConnections = new Stack<Connection>();

    private void checkUsed() throws SQLException {
        synchronized(lock) {
            Iterator<Map.Entry<Connection,WeakReference<MutableObject>>> it = usedConnections.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<Connection, WeakReference<MutableObject>> usedEntry = it.next();
                if(usedEntry.getValue().get()==null) {
                    it.remove(); // можно было бы попробовать использовать повторно, но connection может быть "грязным" то есть с транзакцией или временными таблицами
                    usedEntry.getKey().close();
                }
            }
        }
    }

    private void addFreeConnection(Connection connection) throws SQLException {
        // assert что synchronized lock
        if(freeConnections.size() < Settings.get().getFreeConnections())
            freeConnections.push(connection);
        else
            connection.close();
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

    public Connection getPrivate(MutableObject object) throws SQLException {
        if(Settings.get().isDisablePoolConnections())
            return newConnection();

        checkUsed();

        synchronized(lock) {
            Connection freeConnection = freeConnections.isEmpty() ? newConnection() : freeConnections.pop();

            usedConnections.put(freeConnection, new WeakReference<MutableObject>(object));
            return freeConnection;
        }
    }

    public void returnPrivate(MutableObject object, Connection connection) throws SQLException {
        if(Settings.get().isDisablePoolConnections()) {
            connection.close();
            return;
        }

        synchronized(lock) {
            WeakReference<MutableObject> weakObject = usedConnections.remove(connection);
            assert weakObject.get() == object;
            assert connection.getAutoCommit();
            addFreeConnection(connection);
        }
    }

}
