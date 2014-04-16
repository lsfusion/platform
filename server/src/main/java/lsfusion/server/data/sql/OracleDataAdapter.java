package lsfusion.server.data.sql;

import lsfusion.base.BaseUtils;
import oracle.jdbc.driver.OracleConnection;
import oracle.jdbc.driver.OracleDriver;

import java.sql.*;
import java.util.Properties;

class OracleDataAdapter extends DataAdapter {

    @Override
    public String getIntegerType() {
        return "NUMBER(5)";
    }
    @Override
    public int getIntegerSQL() {
        return Types.NUMERIC;
    }

    public int updateModel() {
        return 2;
    }

    public String getByteArrayType() {
        return "long raw";
    }

    public String getTextType() {
        return "clob";
    }

    public String getMetaName(String name) {
        return name.toUpperCase();
    }

    public boolean noAutoCommit() {
        return true;
    }

    public OracleDataAdapter(String database, String server, String userID, String password) throws Exception, SQLException, InstantiationException, IllegalAccessException {
        super(database, server, userID, password, false);
    }

    public boolean allowViews() {
        return true;
    }

    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        throw new RuntimeException("wrong update model");
    }

    public String getClassName() {
        return "oracle.jdbc.driver.OracleDriver";
    }

    public String getCreateSessionTable(String tableName, String declareString) {
        return "CREATE GLOBAL TEMPORARY TABLE "+ tableName +" ("+ declareString + ") ON COMMIT PRESERVE ROWS";
    }

    private OracleConnection getPrelimAuthConnection()
            throws SQLException
    {
        Properties props = new Properties();
        props.put(OracleDriver.user_string, "sys");
        props.put(OracleDriver.password_string, "11111");
        props.put(OracleDriver.logon_as_internal_str, "sysdba");
        props.put(OracleDriver.prelim_auth_string, "true");

//        OracleDataSource ods = new OracleDataSource();
//        ods.setConnectionProperties(props);
//        ods.setURL("jdbc:oracle:thin:@localhost:1521");
//        return (OracleConnection)ods.getConnection();

        OracleConnection newConnection =
                (OracleConnection) DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521", props);
        return newConnection;
    }
    
    public void ensureDB(boolean cleanDB) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        Connection connect = DriverManager.getConnection("jdbc:oracle:thin:sys as sysdba/11111@localhost:1521:orcl");
        try {                                                                  
            connect.createStatement().execute("DROP USER " + dataBase + " CASCADE");
//            connect.createStatement().execute("ALTER DATABASE CLOSE");
//            connect.createStatement().execute("ALTER SYSTEM ENABLE RESTRICTED SESSION");
//            connect.createStatement().execute("DROP DATABASE");
         } catch(Exception e) {
            e = e;
        }
        connect.close();

//        OracleConnection prelcon = getPrelimAuthConnection();
//        prelcon.startup(OracleConnection.DatabaseStartupMode.NO_RESTRICTION);
//        prelcon.close();
//
        connect = DriverManager.getConnection("jdbc:oracle:thin:sys as sysdba/11111@localhost:1521:orcl");
        try {
            connect.createStatement().execute("CREATE USER " + dataBase + " IDENTIFIED BY 11111");
            connect.createStatement().execute("GRANT ALL PRIVILEGES TO " + dataBase);
//            connect.createStatement().execute("CREATE DATABASE");
        } catch(Exception e) {
            e = e;
        }
        connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        //        Connect.createStatement().execute("USE testplat");
        OracleConnection connection = (OracleConnection)DriverManager.getConnection("jdbc:oracle:thin:"+dataBase+"/11111@localhost:1521:orcl");
//        Statement statement = connection.createStatement();
//        statement.execute("ALTER SESSION SET current_schema=" + dataBase);
//        statement.close();
        return connection;        
    }

    public String getCommandEnd() {
        return "";
    }

    public String getClustered() {
        return "";
    }

    public String isNULL(String exprs, boolean notSafe) {
        return "NVL("+ exprs +")";
    }

    public String getTop(int Top, String SelectString, String OrderString, String WhereString) {
        if(Top!=0)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + "rownum<=" + Top;
        return SelectString + (WhereString.length()==0?"":" WHERE ") + WhereString + OrderString;
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String having, String top) {
        if(top.length()!=0)
            where = (where.length()==0?"": where +" AND ") + "rownum<=" + top;
        return "SELECT " + exprs + " FROM " + from + BaseUtils.clause("WHERE", where) + BaseUtils.clause("GROUP BY", groupBy) + BaseUtils.clause("HAVING", having) + BaseUtils.clause("ORDER BY", orderBy);
    }

    public String getUnionOrder(String union, String orderBy, String top) {
        if(top.length()==0)
            return union + BaseUtils.clause("ORDER BY", orderBy);
        return "SELECT * FROM (" + union + ") WHERE rownum<=" + top + BaseUtils.clause("ORDER BY", orderBy);
    }

    // потом надо будет смотреть
    
    @Override
    public boolean isDeadLock(SQLException e) {
        return false;
    }

    @Override
    public boolean isUpdateConflict(SQLException e) {
        return false;
    }

    @Override
    public boolean isTimeout(SQLException e) {
        return false;
    }

    @Override
    public boolean isTransactionCanceled(SQLException e) {
        return false;
    }

    @Override
    public boolean isConnectionClosed(SQLException e) {
        return false;
    }

    @Override
    public boolean hasJDBCTimeoutMultiThreadProblem() {
        return true;
    }

    @Override
    public String getFieldName(String name) {
        if(name.equals("struct"))
            return "aaaa";
        return BaseUtils.packWords(super.getFieldName(name), 30);
    }

    @Override
    public String getTableName(String tableName) {
        return BaseUtils.packWords(super.getTableName(tableName), 30);
    }

    @Override
    public String getConstraintName(String name) {
        return BaseUtils.packWords(super.getConstraintName(name), 30);
    }

    @Override
    public String getIndexName(String name) {
        return BaseUtils.packWords(super.getIndexName(name), 30);
    }
}
