package platform.server.data.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class MySQLDataAdapter extends DataAdapter {

    MySQLDataAdapter(String iDataBase, String iServer) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
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

    public void ensureDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

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
