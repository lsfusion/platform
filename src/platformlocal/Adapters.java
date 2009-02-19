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

    String getUpdate(String TableString,String SetString,String FromString,String WhereString);

    String getClassName();
    void createDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;
    Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    String startTransaction();
    String commitTransaction();
    String rollbackTransaction();

    String isNULL(String Expr1, String Expr2, boolean NotSafe);

    String getClustered();
    String getCommandEnd();

    String getNullValue(Type DBType);

    String getSessionTableName(String TableName);
    String getCreateSessionTable(String TableName,String DeclareString,String ConstraintString);

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    boolean isNullSafe();
    boolean isGreatest();

    boolean useFJ();

    int UpdateModel();

    boolean noAutoCommit();

    abstract String getStringType(int length);

    abstract String getNumericType(int length,int precision);

    abstract String getIntegerType();

    String getLongType();

    String getDoubleType();

    String getBitType();

    String getBitString(Boolean Value);

    String getSelect(String From,String Exprs,String Where,String OrderBy,String GroupBy, String Top);

    String getUnionOrder(String Union,String OrderBy, String Top);
}

abstract class DataAdapter implements SQLSyntax {

    String Server;
    String DataBase;

    protected DataAdapter(String iDataBase,String iServer) throws ClassNotFoundException {
        java.lang.Class.forName(getClassName());
        DataBase = iDataBase;
        Server = iServer; 
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

    public String getBitString(Boolean Value) {
        return (Value?"1":"0");
    }

    public int UpdateModel() {
        return 0;
    }

    public boolean noAutoCommit() {
        return false;
    }

    public String getNullValue(Type DBType) {
        return "NULL";
    }

    void Disconnect() throws SQLException {
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

    public String getCreateSessionTable(String TableName, String DeclareString, String ConstraintString) {
        return "CREATE TEMPORARY TABLE "+TableName+" ("+DeclareString+","+ConstraintString+")";
    }

    public String getSessionTableName(String TableName) {
        return TableName;
    }

    public boolean isGreatest() {
        return true;
    }

    public boolean useFJ() {
        return true;
    }

    static String clause(String Clause,String Data) {
        return (Data.length()==0?"":" "+Clause+" "+Data);
    }
    static String clause(String Clause,int Data) {
        return (Data==0?"":" "+Clause+" "+Data);
    }
}

class MySQLDataAdapter extends DataAdapter {

    MySQLDataAdapter(String iDataBase, String iServer) throws ClassNotFoundException {
        super(iDataBase, iServer);
    }

    public boolean allowViews() {
        return false;
    }

    public String getUpdate(String TableString, String SetString, String FromString, String WhereString) {
        return TableString + "," + FromString + SetString + WhereString;
    }

    public String getClassName() {
        return "com.mysql.jdbc.Driver";
    }

    public void createDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        Connection Connect = DriverManager.getConnection("jdbc:mysql://"+Server+":3306/"+DataBase);
        Connect.createStatement().execute("DROP DATABASE "+DataBase);
        Connect.createStatement().execute("CREATE DATABASE "+DataBase);
        Connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        Connection Connect = DriverManager.getConnection("jdbc:mysql://"+Server+":3306/"+DataBase);
        Connect.createStatement().execute("USE "+DataBase);

        return Connect;
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

    public String isNULL(String Expr1, String Expr2, boolean NotSafe) {
        return "IFNULL(" + Expr1 + "," + "Expr2" + ")";
    }

    public String getSelect(String From, String Exprs, String Where, String OrderBy, String GroupBy, String Top) {
        return "SELECT " + Exprs + " FROM " + From + clause("WHERE",Where) + clause("GROUP BY",GroupBy) + clause("ORDER BY",OrderBy) + clause("LIMIT",Top);
    }

    public String getUnionOrder(String Union, String OrderBy, String Top) {
        return Union + clause("ORDER BY",OrderBy) + clause("LIMIT",Top);
    }
}

class MSSQLDataAdapter extends DataAdapter {

    MSSQLDataAdapter(String iDataBase, String iServer) throws ClassNotFoundException {
        super(iDataBase, iServer);
    }

    @Override
    public String getLongType() {
        return "bigint";
    }

    public int UpdateModel() {
        return 1;
    }

    public boolean allowViews() {
        return true;
    }

    public String getUpdate(String TableString, String SetString, String FromString, String WhereString) {
        return TableString + SetString + " FROM " + FromString + WhereString;
    }

    public String getClassName() {
        return "net.sourceforge.jtds.jdbc.Driver";
    }

    public void createDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        Connection Connect = DriverManager.getConnection("jdbc:jtds:sqlserver://"+Server+":1433;namedPipe=true;User=sa;Password=11111");
        try {
        try {
            Connect.createStatement().execute("DROP DATABASE "+DataBase);
        } catch (Exception e) {

        }
        } catch(Exception e) {
        }
        Connect.createStatement().execute("CREATE DATABASE "+DataBase);
        Connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        Connection Connect = DriverManager.getConnection("jdbc:jtds:sqlserver://"+Server+":1433;namedPipe=true;User=sa;Password=11111");
        Connect.createStatement().execute("USE "+DataBase);

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

    public String isNULL(String Expr1, String Expr2, boolean NotSafe) {
        if(NotSafe)
            return "CASE WHEN "+Expr1+" IS NULL THEN "+Expr2+" ELSE "+Expr1+" END";
        else
            return "ISNULL("+Expr1+","+Expr2+")";
    }

    public String getCreateSessionTable(String TableName, String DeclareString, String ConstraintString) {
        return "CREATE TABLE #"+TableName+" ("+DeclareString+","+ConstraintString+")";
    }

    public String getSessionTableName(String TableName) {
        return "#"+TableName;
    }

    public boolean isNullSafe() {
        return false;
    }

    public boolean isGreatest() {
        return false;
    }

    public String getSelect(String From, String Exprs, String Where, String OrderBy, String GroupBy, String Top) {
        return "SELECT " + clause("TOP",Top) + Exprs + " FROM " + From + clause("WHERE",Where) + clause("GROUP BY",GroupBy) + clause("ORDER BY",OrderBy);
    }

    public String getUnionOrder(String Union, String OrderBy, String Top) {
        if(Top.length()==0)
            return Union + clause("ORDER BY",OrderBy);
        return "SELECT" + clause("TOP",Top) + " * FROM (" + Union + ")" + clause("ORDER BY",OrderBy);
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

    public String getUpdate(String TableString, String SetString, String FromString, String WhereString) {
        return TableString + SetString + " FROM " + FromString + WhereString;
    }

    public String getClassName() {
        return "org.postgresql.Driver";
    }

    public void createDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        Connection Connect = DriverManager.getConnection("jdbc:postgresql://"+Server+"/postgres?user=postgres&password=11111");
        try {
            Connect.createStatement().execute("DROP DATABASE "+DataBase);
        } catch (SQLException e) {
        }
        try {
            Connect.createStatement().execute("CREATE DATABASE "+DataBase);
        } catch (SQLException e) {
        }
        Connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        return DriverManager.getConnection("jdbc:postgresql://"+Server+"/"+DataBase+"?user=postgres&password=11111");
    }

    public String getCommandEnd() {
        return ";";
    }

    public String getClustered() {
        return "";
    }

    public String getNullValue(Type DBType) {
        String EmptyValue = DBType.getEmptyString();
        return "NULLIF(" + EmptyValue + "," + EmptyValue + ")";
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

    public String isNULL(String Expr1, String Expr2, boolean NotSafe) {
//        return "(CASE WHEN "+Expr1+" IS NULL THEN "+Expr2+" ELSE "+Expr1+" END)";
        return "COALESCE("+Expr1+","+Expr2+")";
    }

    public String getSelect(String From, String Exprs, String Where, String OrderBy, String GroupBy, String Top) {
        return "SELECT " + Exprs + " FROM " + From + clause("WHERE",Where) + clause("GROUP BY",GroupBy) + clause("ORDER BY",OrderBy) + clause("LIMIT",Top);
    }

    public String getUnionOrder(String Union, String OrderBy, String Top) {
        return Union + clause("ORDER BY",OrderBy) + clause("LIMIT",Top);
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

    public String getUpdate(String TableString, String SetString, String FromString, String WhereString) {
        // идет как Select Update
        return null;
    }

    public String getClassName() {
        return "oracle.jdbc.driver.OracleDriver";
    }

    public String getCreateSessionTable(String TableName, String DeclareString, String ConstraintString) {
        return "CREATE GLOBAL TEMPORARY TABLE "+TableName+" ("+DeclareString+","+ConstraintString+") ON COMMIT PRESERVE ROWS";
    }

    public void createDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        Connection Connect = DriverManager.getConnection("jdbc:oracle:thin:system/a11111@"+DataBase+":1521:orcl");
//        try {
//        Connect.createStatement().execute("ALTER DATABASE CLOSE");
//        Connect.createStatement().execute("DROP DATABASE");
//        } catch(Exception e) {
//        }
//        try {
//        Connect.createStatement().execute("CREATE DATABASE");
//        } catch(Exception e) {
//        }

        Connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        Connection Connect = DriverManager.getConnection("jdbc:oracle:thin:system/a11111@"+DataBase+":1521:orcl");
//        Connect.createStatement().execute("USE testplat");

        return Connect;
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

    public String isNULL(String Expr1, String Expr2, boolean NotSafe) {
        return "NVL("+Expr1+","+Expr2+")";
    }

    public String getTop(int Top, String SelectString, String OrderString, String WhereString) {
        if(Top!=0)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + "rownum<=" + Top;
        return SelectString + (WhereString.length()==0?"":" WHERE ") + WhereString + OrderString;
    }

    public String getSelect(String From, String Exprs, String Where, String OrderBy, String GroupBy, String Top) {
        if(Top.length()!=0)
            Where = (Where.length()==0?"":Where+" AND ") + "rownum<=" + Top;
        return "SELECT " + Exprs + " FROM " + From + clause("WHERE",Where) + clause("GROUP BY",GroupBy) + clause("ORDER BY",OrderBy);
    }

    public String getNullValue(Type DBType) {
        String EmptyValue = DBType.getEmptyString();
        return "NULLIF(" + EmptyValue + "," + EmptyValue + ")";
    }

    public String getUnionOrder(String Union, String OrderBy, String Top) {
        if(Top.length()==0)
            return Union + clause("ORDER BY",OrderBy);
        return "SELECT * FROM (" + Union + ") WHERE rownum<=" + Top + clause("ORDER BY",OrderBy);
    }
}
