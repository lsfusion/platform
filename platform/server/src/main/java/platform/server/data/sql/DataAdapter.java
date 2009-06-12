package platform.server.data.sql;

import platform.server.data.types.Type;

import java.sql.SQLException;

public abstract class DataAdapter implements SQLSyntax {

    String server;
    String dataBase;

    // для debuga
    protected DataAdapter() {
    }

    abstract void ensureDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    protected DataAdapter(String iDataBase,String iServer) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        Class.forName(getClassName());
        dataBase = iDataBase;
        server = iServer;

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

    public String getByteArrayType() {
        return "longvarbinary";
    }

    public String getBitString(Boolean value) {
        return (value ?"1":"0");
    }

    public int updateModel() {
        return 0;
    }

    public boolean noAutoCommit() {
        return false;
    }

    public String getNullValue(Type dbType) {
        return "NULL";
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

    public String getCreateSessionTable(String tableName, String declareString, String constraintString) {
        return "CREATE TEMPORARY TABLE "+ tableName +" ("+ declareString +","+ constraintString +")";   
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

    static String clause(String clause,String data) {
        return (data.length()==0?"":" "+ clause +" "+ data);
    }
    static String clause(String clause,int data) {
        return (data ==0?"":" "+ clause +" "+ data);
    }

    public String getDropSessionTable(String tableName) {
        return "DROP TABLE "+getSessionTableName(tableName);
    }
}
