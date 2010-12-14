package platform.server.data.sql;

import platform.server.data.type.Type;
import platform.server.data.ConnectionPool;
import platform.server.Settings;

import java.sql.SQLException;
import java.sql.Connection;
import java.util.*;
import java.lang.ref.WeakReference;

public abstract class DataAdapter implements SQLSyntax, ConnectionPool {

    public String server;
    public String dataBase;
    public String userID;
    public String password;

    // для debuga
    protected DataAdapter() {
    }

    abstract void ensureDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    protected DataAdapter(String dataBase, String server, String userID, String password) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {

        Class.forName(getClassName());

        this.dataBase = dataBase;
        this.server = server;
        this.userID = userID;
        this.password = password;

        ensureDB();

        if(!Settings.instance.isCommonUnique())
            common = startConnection();
    }

    private Connection common;

    public Connection getCommon(Object object) throws SQLException {
        if(Settings.instance.isCommonUnique())
            return getUnique(object);
        else
            return common;
    }

    public void returnCommon(Object object, Connection connection) throws SQLException {
        if(Settings.instance.isCommonUnique())
            returnUnique(object, connection);
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

    private Connection newConnection() throws SQLException {
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

    public Connection getUnique(Object object) throws SQLException {
        if(Settings.instance.isDisablePoolConnections())
            return newConnection();

        checkUsed();

        synchronized(lock) {
            Connection freeConnection = freeConnections.isEmpty() ? newConnection() : freeConnections.pop();

            usedConnections.put(freeConnection, new WeakReference<Object>(object));
            return freeConnection;
        }
    }

    public void returnUnique(Object object, Connection connection) throws SQLException {
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

    public String getStringType(int length) {
        return "char("+length+")";
    }

    public String getNumericType(int length,int precision) {
        return "numeric("+length+","+precision+")";
    }

    public String getIntegerType() {
        return "integer";
    }

    public String getDateType() {
        return "date";
    }

    public String getLongType() {
        return "long";
    }

    public String getDoubleType() {
        return "double precision";
    }

    public String getBitType() {
        return "integer";
    }

    public String getTextType() {
        return "text";
    }

    public boolean isBinaryString() {
        return false;
    }
    public String getBinaryType(int length) {
        return "binary(" + length + ")";
    }

    public String getByteArrayType() {
        return "longvarbinary";
    }

    public String getBitString(Boolean value) {
        return (value ?"1":"0");
    }

    public int updateModel() {
        return 0;
    }

    // по умолчанию
    public String getClustered() {
        return "CLUSTERED ";
    }

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    public boolean isNullSafe() {
        return true;
    }

    public String getCommandEnd() {
        return "";
    }

    public String getCreateSessionTable(String tableName, String declareString) {
        return "CREATE TEMPORARY TABLE "+ tableName +" ("+ declareString + ")";
    }

    public String getSessionTableName(String tableName) {
        return tableName;
    }

    public boolean isGreatest() {
        return true;
    }

    public boolean useFJ() {
        return true;
    }

    public String getDropSessionTable(String tableName) {
        return "DROP TABLE "+getSessionTableName(tableName);
    }

    public String getOrderDirection(boolean descending) {
        return descending?"DESC":"ASC";
    }

    public String getBinaryConcatenate() {
        return "+";
    }

    public boolean nullUnionTrouble() {
        return false;
    }

    public boolean inlineTrouble() {
        return false;
    }

    public String getHour() {
        return "EXTRACT(HOUR FROM CURRENT_TIME)";
    }

    public String getEpoch() {
        return "EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)";
    }

    public String typeConvertSuffix(Type oldType, Type newType, String name){
        return "";
    }
}
