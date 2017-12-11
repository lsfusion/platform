package lsfusion.server.data.sql;

import lsfusion.base.BaseUtils;
import oracle.jdbc.driver.OracleConnection;
import oracle.jdbc.driver.OracleDriver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

public class OracleDataAdapter extends DataAdapter {

    public OracleDataAdapter(String database, String server, String userID, String password, String instance) throws Exception {
        super(OracleSQLSyntax.instance, database, server, instance, userID, password, null, false);
    }

    private OracleConnection getPrelimAuthConnection()
            throws SQLException
    {
        Properties props = new Properties();
        props.put(OracleDriver.user_string, "sys");
        props.put(OracleDriver.password_string, "11111");
        props.put(OracleDriver.logon_as_internal_str, "sysdba");
        props.put(OracleDriver.prelim_auth_string, "true");

//        OracleDataSource ods = new OracleDataSource();
//        ods.setConnectionProperties(props);
//        ods.setURL("jdbc:oracle:thin:@localhost:1521");
//        return (OracleConnection)ods.getConnection();

        return (OracleConnection) DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521", props);
    }
    
    public void ensureDB(boolean cleanDB) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        Connection connect;
        if(cleanDB) {
            connect = DriverManager.getConnection("jdbc:oracle:thin:sys as sysdba/"+password+"@" + server + ":1521:"+instance);
            try {
                connect.createStatement().execute("DROP USER " + dataBase + " CASCADE");
//            connect.createStatement().execute("ALTER DATABASE CLOSE");
//            connect.createStatement().execute("ALTER SYSTEM ENABLE RESTRICTED SESSION");
//            connect.createStatement().execute("DROP DATABASE");
            } catch (Exception e) {
                e = e;
            }
            connect.close();
        }

//        OracleConnection prelcon = getPrelimAuthConnection();
//        prelcon.startup(OracleConnection.DatabaseStartupMode.NO_RESTRICTION);
//        prelcon.close();
//

//        OracleConnection connection = (OracleConnection)DriverManager.getConnection("jdbc:oracle:thin:"+dataBase+"/" + password + "@" + server + ":1521:" + instance);
//        ResultSet resultSet = connection.createStatement().executeQuery("SELECT short_name FROM uslugi");
//        while(resultSet.next()) {
//            System.out.println(resultSet.getString(1));
//        }

        connect = DriverManager.getConnection("jdbc:oracle:thin:sys as sysdba/"+password+"@"+server+":1521:"+instance);
        try {
            connect.createStatement().execute("CREATE USER " + dataBase + " IDENTIFIED BY 11111");
            connect.createStatement().execute("GRANT ALL PRIVILEGES TO " + dataBase);
//            connect.createStatement().execute("CREATE DATABASE");
        } catch(Exception e) {
            e = e;
        }
        connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        //        Connect.createStatement().execute("USE testplat");
        OracleConnection connection = (OracleConnection)DriverManager.getConnection("jdbc:oracle:thin:"+dataBase+"/" + password + "@" + server + ":1521:" + instance);
//        Statement statement = connection.createStatement();
//        statement.execute("ALTER SESSION SET current_schema=" + dataBase);
//        statement.close();
        return connection;        
    }
}
