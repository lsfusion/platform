package platform.server.data.sql;

import platform.server.data.type.Type;

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

    String getStringType(int length);

    String getNumericType(int length,int precision);

    String getIntegerType();

    String getDateType();

    String getLongType();

    String getDoubleType();

    String getBitType();

    String getBitString(Boolean value);

    String getTextType();

    String getBinaryConcatenate();
    boolean isBinaryString();
    String getBinaryType(int length);

    String getByteArrayType();

    String getSelect(String from,String exprs,String where,String orderBy,String groupBy, String top);

    boolean nullUnionTrouble();
    String getUnionOrder(String union,String orderBy, String top);

    // проблема что inline'ся query и идут duplicate subplan'ы
    boolean inlineTrouble();

    String getOrderDirection(boolean descending);

    String getHour();
    String getEpoch();
    String typeConvertSuffix(Type oldType, Type newType, String name);

    String getInsensitiveLike();
}
