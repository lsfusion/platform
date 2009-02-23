package platform.server.data.sql;

import java.sql.SQLException;
import java.sql.Connection;

import platform.server.data.types.Type;

public interface SQLSyntax {

    boolean allowViews();

    String getUpdate(String tableString,String setString,String fromString,String whereString);

    String getClassName();
    void createDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;
    Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    String startTransaction();
    String commitTransaction();
    String rollbackTransaction();

    String isNULL(String expr1, String expr2, boolean notSafe);

    String getClustered();
    String getCommandEnd();

    String getNullValue(Type dbType);

    String getSessionTableName(String tableName);
    String getCreateSessionTable(String tableName,String declareString,String constraintString);

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    boolean isNullSafe();
    boolean isGreatest();

    boolean useFJ();

    int updateModel();

    boolean noAutoCommit();

    abstract String getStringType(int length);

    abstract String getNumericType(int length,int precision);

    abstract String getIntegerType();

    String getLongType();

    String getDoubleType();

    String getBitType();

    String getBitString(Boolean value);

    String getSelect(String from,String exprs,String where,String orderBy,String groupBy, String top);

    String getUnionOrder(String union,String orderBy, String top);
}
