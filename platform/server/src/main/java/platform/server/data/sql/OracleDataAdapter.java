package platform.server.data.sql;

import platform.base.BaseUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;

class OracleDataAdapter extends DataAdapter {

    @Override
    public String getIntegerType() {
        return "NUMBER(5)";
    }
    @Override
    public int getIntegerSQL() {
        return Types.NUMERIC;
    }

    public int updateModel() {
        return 2;
    }

    public boolean noAutoCommit() {
        return true;
    }

    OracleDataAdapter(String iDataBase, String iServer, String iUserID, String iPassword) throws Exception, SQLException, InstantiationException, IllegalAccessException {
        super(iDataBase, iServer, iUserID, iPassword);
    }

    public boolean allowViews() {
        return true;
    }

    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        throw new RuntimeException("wrong update model");
    }

    public String getClassName() {
        return "oracle.jdbc.driver.OracleDriver";
    }

    public String getCreateSessionTable(String tableName, String declareString) {
        return "CREATE GLOBAL TEMPORARY TABLE "+ tableName +" ("+ declareString + ") ON COMMIT PRESERVE ROWS";
    }

    public void ensureDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

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

    public String isNULL(String exprs, boolean notSafe) {
        return "NVL("+ exprs +")";
    }

    public String getTop(int Top, String SelectString, String OrderString, String WhereString) {
        if(Top!=0)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + "rownum<=" + Top;
        return SelectString + (WhereString.length()==0?"":" WHERE ") + WhereString + OrderString;
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String having, String top) {
        if(top.length()!=0)
            where = (where.length()==0?"": where +" AND ") + "rownum<=" + top;
        return "SELECT " + exprs + " FROM " + from + BaseUtils.clause("WHERE", where) + BaseUtils.clause("GROUP BY", groupBy) + BaseUtils.clause("HAVING", having) + BaseUtils.clause("ORDER BY", orderBy);
    }

    public String getUnionOrder(String union, String orderBy, String top) {
        if(top.length()==0)
            return union + BaseUtils.clause("ORDER BY", orderBy);
        return "SELECT * FROM (" + union + ") WHERE rownum<=" + top + BaseUtils.clause("ORDER BY", orderBy);
    }
}
