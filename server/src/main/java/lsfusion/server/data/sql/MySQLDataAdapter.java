package lsfusion.server.data.sql;

import lsfusion.base.BaseUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class MySQLDataAdapter extends DataAdapter {

    MySQLDataAdapter(String iDataBase, String iServer, String iUserID, String iPassword) throws Exception, SQLException, InstantiationException, IllegalAccessException {
        super(iDataBase, iServer, null, iUserID, iPassword, false);
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

    public void ensureDB(boolean cleanDB) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

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

    public String isNULL(String exprs, boolean notSafe) {
        return "IFNULL(" + exprs + ")";
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String having, String top) {
        return "SELECT " + exprs + " FROM " + from + BaseUtils.clause("WHERE", where) + BaseUtils.clause("GROUP BY", groupBy) + BaseUtils.clause("HAVING", having) + BaseUtils.clause("ORDER BY", orderBy) + BaseUtils.clause("LIMIT", top);
    }

    public String getUnionOrder(String union, String orderBy, String top) {
        return union + BaseUtils.clause("ORDER BY", orderBy) + BaseUtils.clause("LIMIT", top);
    }
}
