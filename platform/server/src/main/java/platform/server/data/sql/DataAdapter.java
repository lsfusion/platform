package platform.server.data.sql;

import java.sql.SQLException;

public abstract class DataAdapter implements SQLSyntax {

    String server;
    String dataBase;
    String userID;
    String password;

    // для debuga
    protected DataAdapter() {
    }

    abstract void ensureDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    protected DataAdapter(String iDataBase, String iServer, String iUserID, String iPassword) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {

        Class.forName(getClassName());

        dataBase = iDataBase;
        server = iServer;
        userID = iUserID;
        password = iPassword;

        ensureDB();
    }

    public String getStringType(int length) {
        return "char("+length+")";
    }

    public String getNumericType(int length,int precision) {
        return "numeric("+length+","+precision+")";
    }

    public String getIntegerType() {
        return "integer";
    }

    public String getLongType() {
        return "long";
    }

    public String getDoubleType() {
        return "double precision";
    }

    public String getBitType() {
        return "integer";
    }

    public String getTextType() {
        return "text";
    }

    public boolean isBinaryString() {
        return false;
    }
    public String getBinaryType(int length) {
        return "binary(" + length + ")";
    }

    public String getByteArrayType() {
        return "longvarbinary";
    }

    public String getBitString(Boolean value) {
        return (value ?"1":"0");
    }

    public int updateModel() {
        return 0;
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

    public String getCreateSessionTable(String tableName, String declareString) {
        return "CREATE TEMPORARY TABLE "+ tableName +" ("+ declareString + ")";
    }

    public String getSessionTableName(String tableName) {
        return tableName;
    }

    public boolean isGreatest() {
        return true;
    }

    public boolean useFJ() {
        return true;
    }

    public String getDropSessionTable(String tableName) {
        return "DROP TABLE "+getSessionTableName(tableName);
    }

    public String getOrderDirection(boolean descending) {
        return descending?"DESC":"ASC";
    }

    public String getBinaryConcatenate() {
        return "+";
    }

    public boolean nullUnionTrouble() {
        return false;
    }

    public String getHour() {
        return "EXTRACT(HOUR FROM CURRENT_TIME)";
    }
}
