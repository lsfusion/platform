package platform.server.data.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLSyntax {

    final static String NULL = "NULL";

    boolean allowViews();

    String getUpdate(String tableString,String setString,String fromString,String whereString);

    String getClassName();
    Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    String isNULL(String expr1, String expr2, boolean notSafe);

    String getClustered();
    String getCommandEnd();

    String getSessionTableName(String tableName);
    String getCreateSessionTable(String tableName, String declareString);
    String getDropSessionTable(String tableName);

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    boolean isNullSafe();
    boolean isGreatest();

    boolean useFJ();

    int updateModel();

    abstract String getStringType(int length);

    abstract String getNumericType(int length,int precision);

    abstract String getIntegerType();

    String getLongType();

    String getDoubleType();

    String getBitType();

    String getBitString(Boolean value);

    String getTextType();

    String getBinaryType(int length);

    String getByteArrayType();

    String getSelect(String from,String exprs,String where,String orderBy,String groupBy, String top);

    String getUnionOrder(String union,String orderBy, String top);

    String getOrderDirection(boolean order);
}
