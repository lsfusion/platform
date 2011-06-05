package platform.server.data.sql;

import org.apache.log4j.Logger;
import platform.server.data.AbstractConnectionPool;
import platform.server.data.type.Type;

import java.sql.SQLException;
import java.sql.Types;

public abstract class DataAdapter extends AbstractConnectionPool implements SQLSyntax {
    protected final static Logger logger = Logger.getLogger(DataAdapter.class);

    public String server;
    public String dataBase;
    public String userID;
    public String password;

    // для debuga
    protected DataAdapter() {
    }

    abstract void ensureDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    protected DataAdapter(String dataBase, String server, String userID, String password) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {

        Class.forName(getClassName());

        this.dataBase = dataBase;
        this.server = server;
        this.userID = userID;
        this.password = password;

        ensureDB();
    }

    public String getStringType(int length) {
        return "char(" + length + ")";
    }
    public int getStringSQL() {
        return Types.CHAR;
    }

    public String getNumericType(int length, int precision) {
        return "numeric(" + length + "," + precision + ")";
    }
    public int getNumericSQL() {
        return Types.NUMERIC;
    }

    public String getIntegerType() {
        return "integer";
    }
    public int getIntegerSQL() {
        return Types.INTEGER;
    }

    public String getDateType() {
        return "date";
    }
    public int getDateSQL() {
        return Types.DATE;
    }

    public String getDateTimeType() {
        return "timestamp";
    }
    public int getDateTimeSQL() {
        return Types.TIMESTAMP;
    }

    public String getLongType() {
        return "long";
    }
    public int getLongSQL() {
        return Types.BIGINT;
    }

    public String getDoubleType() {
        return "double precision";
    }
    public int getDoubleSQL() {
        return Types.DOUBLE;
    }

    public String getBitType() {
        return "integer";
    }
    public int getBitSQL() {
        return Types.INTEGER;
    }

    public String getTextType() {
        return "text";
    }
    public int getTextSQL() {
        return Types.VARCHAR;
    }

    public boolean isBinaryString() {
        return false;
    }

    public String getBinaryType(int length) {
        return "binary(" + length + ")";
    }
    public int getBinarySQL() {
        return Types.BINARY;
    }

    public String getByteArrayType() {
        return "longvarbinary";
    }
    public int getByteArraySQL() {
        return Types.LONGVARBINARY;
    }

    public String getBitString(Boolean value) {
        return (value ? "1" : "0");
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
        return "CREATE TEMPORARY TABLE " + tableName + " (" + declareString + ")";
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

    public boolean orderUnion() {
        return false;
    }

    public String getDropSessionTable(String tableName) {
        return "DROP TABLE " + getSessionTableName(tableName);
    }

    public String getOrderDirection(boolean descending) {
        return descending ? "DESC" : "ASC";
    }

    public String getBinaryConcatenate() {
        return "+";
    }

    public boolean nullUnionTrouble() {
        return false;
    }

    public boolean inlineTrouble() {
        return false;
    }

    public String getHour() {
        return "EXTRACT(HOUR FROM CURRENT_TIME)";
    }

    public String getMinute() {
        return "EXTRACT(MINUTE FROM CURRENT_TIME)";
    }

    public String getEpoch() {
        return "EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)";
    }

    public String getDateTime() {
        return "CURRENT_TIMESTAMP";
    }

    public String typeConvertSuffix(Type oldType, Type newType, String name) {
        return "";
    }

    public String getInsensitiveLike() {
        return "LIKE";
    }
}
