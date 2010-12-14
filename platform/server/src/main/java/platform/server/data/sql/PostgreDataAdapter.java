package platform.server.data.sql;

import platform.base.BaseUtils;
import platform.server.data.type.Type;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class PostgreDataAdapter extends DataAdapter {

    // Для debuga конструктор
    public PostgreDataAdapter() {
    }

    public PostgreDataAdapter(String dataBase, String server, String userID, String password) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(dataBase, server, userID, password);
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

        Connection connect = DriverManager.getConnection("jdbc:postgresql://" + server + "/postgres?user=" + userID + "&password=" + password);
/*        try {
            connect.createStatement().execute("DROP DATABASE "+ dataBase);
        } catch (SQLException e) {
        }*/
        try {
            connect.createStatement().execute("CREATE DATABASE " + dataBase + " WITH ENCODING='UTF8' ");
        } catch (SQLException e) {
        }
        connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        return DriverManager.getConnection("jdbc:postgresql://" + server + "/" + dataBase + "?user=" + userID + "&password=" + password);
    }

    public String getCommandEnd() {
        return ";";
    }

    public String getClustered() {
        return "";
    }

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    public boolean isNullSafe() {
        return false;
    }

    public String isNULL(String expr1, String expr2, boolean notSafe) {
//        return "(CASE WHEN "+Expr1+" IS NULL THEN "+Expr2+" ELSE "+Expr1+" END)";
        return "COALESCE(" + expr1 + "," + expr2 + ")";
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String top) {
        return "SELECT " + exprs + " FROM " + from + BaseUtils.clause("WHERE", where) + BaseUtils.clause("GROUP BY", groupBy) + BaseUtils.clause("ORDER BY", orderBy) + BaseUtils.clause("LIMIT", top);
    }

    public String getUnionOrder(String union, String orderBy, String top) {
        return union + BaseUtils.clause("ORDER BY", orderBy) + BaseUtils.clause("LIMIT", top);
    }

    public String getByteArrayType() {
        return "bytea";
    }

    @Override
    public String getOrderDirection(boolean descending) {
        return descending ? "DESC NULLS LAST" : "ASC NULLS FIRST";
    }

    @Override
    public boolean isBinaryString() {
        return true;
    }

    @Override
    public String getBinaryType(int length) {
//        return "bit(" + length * 8 + ")";
        return getStringType(length);
    }

    @Override
    public String getBinaryConcatenate() {
        return "||";
    }

    @Override
    public boolean useFJ() {
        return false;
    }

    @Override
    public boolean nullUnionTrouble() {
        return true;
    }

    @Override
    public boolean inlineTrouble() {
        return true;
    }

    @Override
    public String typeConvertSuffix(Type oldType, Type newType, String name) {
        return "USING " + name + "::" + newType.getDB(this);
    }

    @Override
    public String getInsensitiveLike() {
        return "ILIKE";
    }
}
