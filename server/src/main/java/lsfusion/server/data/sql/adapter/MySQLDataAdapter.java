package lsfusion.server.data.sql.adapter;

import lsfusion.server.data.sql.syntax.MySQLSQLSyntax;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDataAdapter extends DataAdapter {

    MySQLDataAdapter(String iDataBase, String iServer, String iUserID, String iPassword) throws Exception {
        super(MySQLSQLSyntax.instance, iDataBase, iServer, null, iUserID, iPassword, null, false);
    }

    public void ensureDB(boolean cleanDB) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        Connection connect = DriverManager.getConnection("jdbc:mysql://" + server + ":3306/" + dataBase);
        connect.createStatement().execute("DROP DATABASE " + dataBase);
        connect.createStatement().execute("CREATE DATABASE " + dataBase);
        connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        Connection connect = DriverManager.getConnection("jdbc:mysql://" + server + ":3306/" + dataBase);
        connect.createStatement().execute("USE " + dataBase);

        return connect;
    }
}
