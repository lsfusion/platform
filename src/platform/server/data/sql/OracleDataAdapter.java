package platform.server.data.sql;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;

import platform.server.data.types.Type;

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
