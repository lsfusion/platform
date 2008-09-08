/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

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

    String getTop(int Top,String SelectString);

    String getNullValue(String DBType);

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    boolean isNullSafe();
}

abstract class DataAdapter implements SQLSyntax {

    static DataAdapter getDefault() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        return new PostgreDataAdapter();
    }

    public String getNullValue(String DBType) {
        return "NULL";
    }

    public String getTop(int Top,String SelectString) {
        return (Top==0?"":"TOP "+Top+" ") + SelectString;
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

        Connection Connect = startConnection();
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

        Connection Connect = startConnection();
        Connect.createStatement().execute("DROP DATABASE testplat");
        Connect.createStatement().execute("CREATE DATABASE testplat");
        Connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        Connection Connect = DriverManager.getConnection("jdbc:jtds:sqlserver://server:1433;namedPipe=true;User=sa;Password=");
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

    public boolean isNullSafe() {
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

        Connection Connect = DriverManager.getConnection("jdbc:postgresql://server/postgres?user=postgres&password=11111");
        Connect.createStatement().execute("DROP DATABASE testplat");
        Connect.createStatement().execute("CREATE DATABASE testplat");
        Connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        return DriverManager.getConnection("jdbc:postgresql://server/testplat?user=postgres&password=11111");
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

    public String getTop(int Top,String SelectString) {
        return SelectString + (Top==0?"":" LIMIT "+Top);
    }

    public String isNULL(String Expr1, String Expr2, boolean NotSafe) {
//        return "(CASE WHEN "+Expr1+" IS NULL THEN "+Expr2+" ELSE "+Expr1+" END)";
        return "COALESCE("+Expr1+","+Expr2+")";
    }
}
