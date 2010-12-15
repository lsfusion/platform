package platform.server.data;

import platform.server.Settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.HashMap;
import java.util.Stack;
import java.util.Iterator;

public abstract class AbstractConnectionPool implements ConnectionPool {

    public abstract Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    private Connection common;

    public Connection getCommon(Object object) throws SQLException {
        if(Settings.instance.isCommonUnique())
            return getPrivate(object);
        else {
            synchronized(lock) {
                if(common==null)
                    common = newConnection();
                return common;
            }
        }
    }

    public void returnCommon(Object object, Connection connection) throws SQLException {
        if(Settings.instance.isCommonUnique())
            returnPrivate(object, connection);
        else
            assert common==connection;
    }

    private final Object lock = new Object();
    private final Map<Connection, WeakReference<Object>> usedConnections = new HashMap<Connection, WeakReference<Object>>();
    private final Stack<Connection> freeConnections = new Stack<Connection>();

    private void checkUsed() throws SQLException {
        synchronized(lock) {
            Iterator<Map.Entry<Connection,WeakReference<Object>>> it = usedConnections.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<Connection, WeakReference<Object>> usedEntry = it.next();
                if(usedEntry.getValue().get()==null) {
                    it.remove(); // можно было бы попробовать использовать повторно, но connection может быть "грязным" то есть с транзакцией или временными таблицами
                    usedEntry.getKey().close();
                }
            }
        }
    }

    private void addFreeConnection(Connection connection) throws SQLException {
        // assert что synchronized lock
        if(freeConnections.size() < Settings.instance.getFreeConnections())
            freeConnections.push(connection);
        else
            connection.close();
    }

    public Connection newConnection() throws SQLException {
        try {
            Connection newConnection = startConnection();
            newConnection.setAutoCommit(true);
            return newConnection;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getPrivate(Object object) throws SQLException {
        if(Settings.instance.isDisablePoolConnections())
            return newConnection();

        checkUsed();

        synchronized(lock) {
            Connection freeConnection = freeConnections.isEmpty() ? newConnection() : freeConnections.pop();

            usedConnections.put(freeConnection, new WeakReference<Object>(object));
            return freeConnection;
        }
    }

    public void returnPrivate(Object object, Connection connection) throws SQLException {
        if(Settings.instance.isDisablePoolConnections()) {
            connection.close();
            return;
        }

        synchronized(lock) {
            WeakReference<Object> weakObject = usedConnections.remove(connection);
            assert weakObject.get().equals(object);
            assert connection.getAutoCommit();
            addFreeConnection(connection);
        }
    }

}
