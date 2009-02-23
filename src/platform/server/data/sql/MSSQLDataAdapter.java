package platform.server.data.sql;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;

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
