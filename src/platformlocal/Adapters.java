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

    String getTop(int Top, String SelectString, String OrderString, String WhereString);

    String getNullValue(String DBType);

    String getSessionTableName(String TableName);
    String getCreateSessionTable(String TableName,String DeclareString,String ConstraintString);

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    boolean isNullSafe();
    boolean isGreatest();

    String convertType(String Type);

    int UpdateModel();

    boolean noAutoCommit();
}

abstract class DataAdapter implements SQLSyntax {

    public String convertType(String Type) {
        return Type;
    }

    public int UpdateModel() {
        return 0;
    }

    public boolean noAutoCommit() {
        return false;
    }

    static DataAdapter getDefault() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        return new PostgreDataAdapter();
    }

    public String getNullValue(String DBType) {
        return "NULL";
    }

    public String getTop(int Top, String SelectString, String OrderString, String WhereString) {
        return (Top==0?"":"TOP "+Top+" ") + SelectString + (WhereString.length()==0?"":" WHERE ") + WhereString + OrderString;
    }

   DataAdapter() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

       java.lang.Class.forName(getClassName());
       createDB();
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
}

class MySQLDataAdapter extends DataAdapter {

    MySQLDataAdapter()  throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super();
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

        Connection Connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/TestPlat");
        Connect.createStatement().execute("DROP DATABASE testplat");
        Connect.createStatement().execute("CREATE DATABASE testplat");
        Connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        Connection Connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/TestPlat");
        Connect.createStatement().execute("USE testplat");

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
}

class MSSQLDataAdapter extends DataAdapter {

    MSSQLDataAdapter() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super();
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

        Connection Connect = DriverManager.getConnection("jdbc:jtds:sqlserver://mycomp:1433;namedPipe=true;User=sa;Password=");
        try {
        try {
            Connect.createStatement().execute("DROP DATABASE testplat");
        } catch (Exception e) {
            
        }
        } catch(Exception e) {            
        }
        Connect.createStatement().execute("CREATE DATABASE testplat");
        Connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        Connection Connect = DriverManager.getConnection("jdbc:jtds:sqlserver://mycomp:1433;namedPipe=true;User=sa;Password=");
        Connect.createStatement().execute("USE testplat");

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
}

class PostgreDataAdapter extends DataAdapter {

    PostgreDataAdapter() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super();
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

        Connection Connect = DriverManager.getConnection("jdbc:postgresql://localhost/postgres?user=postgres&password=11111");
        Connect.createStatement().execute("DROP DATABASE testplat");
        Connect.createStatement().execute("CREATE DATABASE testplat");
        Connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        return DriverManager.getConnection("jdbc:postgresql://localhost/testplat?user=postgres&password=11111");
    }

    public String getCommandEnd() {
        return ";";
    }

    public String getClustered() {
        return "";
    }

    public String getNullValue(String DBType) {
        String EmptyValue = (DBType.equals("integer")?"0":"''");
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

    public String getTop(int Top, String SelectString, String OrderString, String WhereString) {
        return SelectString + (WhereString.length()==0?"":" WHERE ") + WhereString + OrderString + (Top==0?"":" LIMIT "+Top);
    }

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    public boolean isNullSafe() {
        return false;
    }

    public String isNULL(String Expr1, String Expr2, boolean NotSafe) {
        return "(CASE WHEN "+Expr1+" IS NULL THEN "+Expr2+" ELSE "+Expr1+" END)";
//        return "COALESCE("+Expr1+","+Expr2+")";
    }
}


class OracleDataAdapter extends DataAdapter {

    public String convertType(String Type) {
        if(Type.equals("integer"))
            return "NUMBER(5)";

        return Type;
    }

    public int UpdateModel() {
        return 2;
    }

    public boolean noAutoCommit() {
        return true;
    }

    OracleDataAdapter() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super();
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

        Connection Connect = DriverManager.getConnection("jdbc:oracle:thin:system/a11111@server:1521:orcl");
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
        Connection Connect = DriverManager.getConnection("jdbc:oracle:thin:system/a11111@server:1521:orcl");
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
    
    public String getNullValue(String DBType) {
        String EmptyValue = (DBType.equals("integer")?"0":"''");
        return "NULLIF(" + EmptyValue + "," + EmptyValue + ")";
    }
}
