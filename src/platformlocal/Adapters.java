/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

interface SQLSyntax {

    boolean allowViews();

    String getUpdate(String tableString,String setString,String fromString,String whereString);

    String getClassName();
    void createDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;
    Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    String startTransaction();
    String commitTransaction();
    String rollbackTransaction();

    String isNULL(String expr1, String expr2, boolean notSafe);

    String getClustered();
    String getCommandEnd();

    String getNullValue(Type dbType);

    String getSessionTableName(String tableName);
    String getCreateSessionTable(String tableName,String declareString,String constraintString);

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    boolean isNullSafe();
    boolean isGreatest();

    boolean useFJ();

    int updateModel();

    boolean noAutoCommit();

    abstract String getStringType(int length);

    abstract String getNumericType(int length,int precision);

    abstract String getIntegerType();

    String getLongType();

    String getDoubleType();

    String getBitType();

    String getBitString(Boolean value);

    String getSelect(String from,String exprs,String where,String orderBy,String groupBy, String top);

    String getUnionOrder(String union,String orderBy, String top);
}

abstract class DataAdapter implements SQLSyntax {

    String server;
    String dataBase;

    protected DataAdapter(String iDataBase,String iServer) throws ClassNotFoundException {
        java.lang.Class.forName(getClassName());
        dataBase = iDataBase;
        server = iServer;
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

    public String getLongType() {
        return "long";
    }

    public String getDoubleType() {
        return "double precision";
    }

    public String getBitType() {
        return "integer";
    }

    public String getBitString(Boolean value) {
        return (value ?"1":"0");
    }

    public int updateModel() {
        return 0;
    }

    public boolean noAutoCommit() {
        return false;
    }

    public String getNullValue(Type dbType) {
        return "NULL";
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

    public String getCreateSessionTable(String tableName, String declareString, String constraintString) {
        return "CREATE TEMPORARY TABLE "+ tableName +" ("+ declareString +","+ constraintString +")";
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

    static String clause(String clause,String data) {
        return (data.length()==0?"":" "+ clause +" "+ data);
    }
    static String clause(String clause,int data) {
        return (data ==0?"":" "+ clause +" "+ data);
    }
}

class MySQLDataAdapter extends DataAdapter {

    MySQLDataAdapter(String iDataBase, String iServer) throws ClassNotFoundException {
        super(iDataBase, iServer);
    }

    public boolean allowViews() {
        return false;
    }

    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        return tableString + "," + fromString + setString + whereString;
    }

    public String getClassName() {
        return "com.mysql.jdbc.Driver";
    }

    public void createDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        Connection connect = DriverManager.getConnection("jdbc:mysql://"+ server +":3306/"+ dataBase);
        connect.createStatement().execute("DROP DATABASE "+ dataBase);
        connect.createStatement().execute("CREATE DATABASE "+ dataBase);
        connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        Connection connect = DriverManager.getConnection("jdbc:mysql://"+ server +":3306/"+ dataBase);
        connect.createStatement().execute("USE "+ dataBase);

        return connect;
    }

    public String startTransaction() {
        return "START TRANSACTION";
    }

    public String commitTransaction() {
        return "COMMIT";
    }

    public String rollbackTransaction() {
        return "ROLLBACK";
    }

    public String isNULL(String expr1, String expr2, boolean notSafe) {
        return "IFNULL(" + expr1 + "," + "Expr2" + ")";
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String top) {
        return "SELECT " + exprs + " FROM " + from + clause("WHERE", where) + clause("GROUP BY", groupBy) + clause("ORDER BY", orderBy) + clause("LIMIT", top);
    }

    public String getUnionOrder(String union, String orderBy, String top) {
        return union + clause("ORDER BY", orderBy) + clause("LIMIT", top);
    }
}

class MSSQLDataAdapter extends DataAdapter {

    MSSQLDataAdapter(String iDataBase, String iServer) throws ClassNotFoundException {
        super(iDataBase, iServer);
    }

    public String getLongType() {
        return "bigint";
    }

    public int updateModel() {
        return 1;
    }

    public boolean allowViews() {
        return true;
    }

    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        return tableString + setString + " FROM " + fromString + whereString;
    }

    public String getClassName() {
        return "net.sourceforge.jtds.jdbc.Driver";
    }

    public void createDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        Connection Connect = DriverManager.getConnection("jdbc:jtds:sqlserver://"+ server +":1433;namedPipe=true;User=sa;Password=11111");
        try {
        try {
            Connect.createStatement().execute("DROP DATABASE "+ dataBase);
        } catch (Exception e) {

        }
        } catch(Exception e) {
        }
        Connect.createStatement().execute("CREATE DATABASE "+ dataBase);
        Connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        Connection Connect = DriverManager.getConnection("jdbc:jtds:sqlserver://"+ server +":1433;namedPipe=true;User=sa;Password=11111");
        Connect.createStatement().execute("USE "+ dataBase);

        return Connect;
    }

    public String startTransaction() {
        return "BEGIN TRANSACTION";
    }

    public String commitTransaction() {
        return "COMMIT TRANSACTION";
    }

    public String rollbackTransaction() {
        return "ROLLBACK";
    }

    public String isNULL(String expr1, String expr2, boolean notSafe) {
        if(notSafe)
            return "CASE WHEN "+ expr1 +" IS NULL THEN "+ expr2 +" ELSE "+ expr1 +" END";
        else
            return "ISNULL("+ expr1 +","+ expr2 +")";
    }

    public String getCreateSessionTable(String tableName, String declareString, String constraintString) {
        return "CREATE TABLE #"+ tableName +" ("+ declareString +","+ constraintString +")";
    }

    public String getSessionTableName(String tableName) {
        return "#"+ tableName;
    }

    public boolean isNullSafe() {
        return false;
    }

    public boolean isGreatest() {
        return false;
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String top) {
        return "SELECT " + clause("TOP", top) + exprs + " FROM " + from + clause("WHERE", where) + clause("GROUP BY", groupBy) + clause("ORDER BY", orderBy);
    }

    public String getUnionOrder(String union, String orderBy, String top) {
        if(top.length()==0)
            return union + clause("ORDER BY", orderBy);
        return "SELECT" + clause("TOP", top) + " * FROM (" + union + ")" + clause("ORDER BY", orderBy);
    }
}

class PostgreDataAdapter extends DataAdapter {

    PostgreDataAdapter(String iDataBase, String iServer) throws ClassNotFoundException {
        super(iDataBase, iServer);
    }

    public String getLongType() {
        return "int8";
    }

    public boolean allowViews() {
        return true;
    }

    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        return tableString + setString + " FROM " + fromString + whereString;
    }

    public String getClassName() {
        return "org.postgresql.Driver";
    }

    public void createDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        Connection connect = DriverManager.getConnection("jdbc:postgresql://"+ server +"/postgres?user=postgres&password=11111");
        try {
            connect.createStatement().execute("DROP DATABASE "+ dataBase);
        } catch (SQLException e) {
        }
        try {
            connect.createStatement().execute("CREATE DATABASE "+ dataBase);
        } catch (SQLException e) {
        }
        connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        return DriverManager.getConnection("jdbc:postgresql://"+ server +"/"+ dataBase +"?user=postgres&password=11111");
    }

    public String getCommandEnd() {
        return ";";
    }

    public String getClustered() {
        return "";
    }

    public String getNullValue(Type dbType) {
        String emptyValue = dbType.getEmptyString();
        return "NULLIF(" + emptyValue + "," + emptyValue + ")";
    }

    public String startTransaction() {
        return "BEGIN TRANSACTION";
    }

    public String commitTransaction() {
        return "COMMIT TRANSACTION";
    }

    public String rollbackTransaction() {
        return "ROLLBACK";
    }

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    public boolean isNullSafe() {
        return false;
    }

    public String isNULL(String expr1, String expr2, boolean notSafe) {
//        return "(CASE WHEN "+Expr1+" IS NULL THEN "+Expr2+" ELSE "+Expr1+" END)";
        return "COALESCE("+ expr1 +","+ expr2 +")";
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String top) {
        return "SELECT " + exprs + " FROM " + from + clause("WHERE", where) + clause("GROUP BY", groupBy) + clause("ORDER BY", orderBy) + clause("LIMIT", top);
    }

    public String getUnionOrder(String union, String orderBy, String top) {
        return union + clause("ORDER BY", orderBy) + clause("LIMIT", top);
    }
}


class OracleDataAdapter extends DataAdapter {

    public String getIntegerType() {
        return "NUMBER(5)";
    }

    public int UpdateModel() {
        return 2;
    }

    public boolean noAutoCommit() {
        return true;
    }

    OracleDataAdapter(String iDataBase, String iServer) throws ClassNotFoundException {
        super(iDataBase, iServer);
    }

    public boolean allowViews() {
        return true;
    }

    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        // идет как Select Update
        return null;
    }

    public String getClassName() {
        return "oracle.jdbc.driver.OracleDriver";
    }

    public String getCreateSessionTable(String tableName, String declareString, String constraintString) {
        return "CREATE GLOBAL TEMPORARY TABLE "+ tableName +" ("+ declareString +","+ constraintString +") ON COMMIT PRESERVE ROWS";
    }

    public void createDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        Connection connect = DriverManager.getConnection("jdbc:oracle:thin:system/a11111@"+ dataBase +":1521:orcl");
//        try {
//        Connect.createStatement().execute("ALTER DATABASE CLOSE");
//        Connect.createStatement().execute("DROP DATABASE");
//        } catch(Exception e) {
//        }
//        try {
//        Connect.createStatement().execute("CREATE DATABASE");
//        } catch(Exception e) {
//        }

        connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        //        Connect.createStatement().execute("USE testplat");
        return DriverManager.getConnection("jdbc:oracle:thin:system/a11111@"+ dataBase +":1521:orcl");
    }

    public String getCommandEnd() {
        return "";
    }

    public String getClustered() {
        return "";
    }

    public String startTransaction() {
        return null;
    }

    public String commitTransaction() {
        return "COMMIT";
    }

    public String rollbackTransaction() {
        return "ROLLBACK";
    }

    public String isNULL(String expr1, String expr2, boolean notSafe) {
        return "NVL("+ expr1 +","+ expr2 +")";
    }

    public String getTop(int Top, String SelectString, String OrderString, String WhereString) {
        if(Top!=0)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + "rownum<=" + Top;
        return SelectString + (WhereString.length()==0?"":" WHERE ") + WhereString + OrderString;
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String top) {
        if(top.length()!=0)
            where = (where.length()==0?"": where +" AND ") + "rownum<=" + top;
        return "SELECT " + exprs + " FROM " + from + clause("WHERE", where) + clause("GROUP BY", groupBy) + clause("ORDER BY", orderBy);
    }

    public String getNullValue(Type dbType) {
        String EmptyValue = dbType.getEmptyString();
        return "NULLIF(" + EmptyValue + "," + EmptyValue + ")";
    }

    public String getUnionOrder(String union, String orderBy, String top) {
        if(top.length()==0)
            return union + clause("ORDER BY", orderBy);
        return "SELECT * FROM (" + union + ") WHERE rownum<=" + top + clause("ORDER BY", orderBy);
    }
}
