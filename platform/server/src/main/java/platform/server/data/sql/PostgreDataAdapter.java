package platform.server.data.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgreDataAdapter extends DataAdapter {

    // Для debuga конструктор
    public PostgreDataAdapter() {
    }

    public PostgreDataAdapter(String iDataBase, String iServer, String iUserID, String iPassword) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(iDataBase, iServer, iUserID, iPassword);
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

    public void ensureDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        Connection connect = DriverManager.getConnection("jdbc:postgresql://"+ server +"/postgres?user=" + userID + "&password=" + password);
/*        try {
            connect.createStatement().execute("DROP DATABASE "+ dataBase);
        } catch (SQLException e) {
        }*/
        try {
            connect.createStatement().execute("CREATE DATABASE "+ dataBase + " WITH ENCODING='UTF8' ");
        } catch (SQLException e) {
        }
        connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        return DriverManager.getConnection("jdbc:postgresql://"+ server +"/"+ dataBase +"?user=" + userID + "&password=" + password);
    }

    public String getCommandEnd() {
        return ";";
    }

    public String getClustered() {
        return "";
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

    public String getByteArrayType() {
        return "bytea";
    }
}
