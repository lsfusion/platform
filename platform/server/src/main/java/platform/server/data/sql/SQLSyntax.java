package platform.server.data.sql;

import platform.server.data.query.TypeEnvironment;
import platform.server.data.type.Type;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLSyntax {

    final static String NULL = "NULL";

    boolean allowViews();

    String getUpdate(String tableString,String setString,String fromString,String whereString);

    String getClassName();
    Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException;

    String isNULL(String exprs, boolean notSafe);

    String getClustered();
    String getCommandEnd();

    String getSessionTableName(String tableName);
    String getCreateSessionTable(String tableName, String declareString);
    String getDropSessionTable(String tableName);

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    boolean isNullSafe();
    boolean isGreatest();

    boolean useFJ();

    boolean orderUnion(); // распихивать order'ы внутрь union all'ов

    int updateModel();

    String getStringType(int length);
    int getStringSQL();

    String getVarStringType(int length);
    int getVarStringSQL();

    String getNumericType(int length,int precision);
    int getNumericSQL();

    String getIntegerType();
    int getIntegerSQL();

    String getDateType();
    int getDateSQL();

    String getDateTimeType();
    int getDateTimeSQL();

    String getTimeType();
    int getTimeSQL();

    String getLongType();
    int getLongSQL();

    String getDoubleType();
    int getDoubleSQL();

    String getBitType();
    int getBitSQL();

    String getBitString(Boolean value);

    String getTextType();
    int getTextSQL();

    boolean hasDriverCompositeProblem();
    int getCompositeSQL();

    String getByteArrayType();
    int getByteArraySQL();

    String getColorType();
    int getColorSQL();

    String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String having, String top);

    boolean nullUnionTrouble();
    String getUnionOrder(String union,String orderBy, String top);

    // проблема что inline'ся query и идут duplicate subplan'ы
    boolean inlineTrouble();

    String getOrderDirection(boolean descending, boolean notNull);

    String getHour();
    String getMinute();
    String getEpoch();
    String getDateTime();
    String typeConvertSuffix(Type oldType, Type newType, String name, TypeEnvironment typeEnv);

    String getInsensitiveLike();

    boolean supportGroupNumbers();

    String getCountDistinct(String field);
    String getCount(String field);

    boolean noDynamicSampling();

    boolean orderTopTrouble();
}
