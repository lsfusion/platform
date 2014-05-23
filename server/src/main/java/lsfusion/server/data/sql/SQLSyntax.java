package lsfusion.server.data.sql;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.CompileOrder;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.type.ClassReader;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Reader;
import lsfusion.server.data.type.Type;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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
    
    void setLogLevel(Connection connection, int level);

    boolean useFJ();

    boolean orderUnion(); // распихивать order'ы внутрь union all'ов

    int updateModel();

    String getBPTextType();
    int getBPTextSQL();

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

    boolean noMaxImplicitCast();

    boolean noDynamicSampling();

    boolean orderTopTrouble();
    
    boolean isDeadLock(SQLException e);

    boolean isUpdateConflict(SQLException e);

    boolean isUniqueViolation(SQLException e);

    boolean isTimeout(SQLException e);

    boolean isTransactionCanceled(SQLException e);

    boolean isConnectionClosed(SQLException e);
    
    String getRandomName();

    boolean hasJDBCTimeoutMultiThreadProblem();

    void setACID(Statement statement, boolean acid) throws SQLException;

    String getMetaName(String name);

    String getFieldName(String name);

    String getTableName(String tableName);

    String getGlobalTableName(String tableName);

    String getConstraintName(String name);

    String getIndexName(String name);
    
    boolean hasSelectivityProblem();

    String getAdjustSelectivityPredicate();

    String getStringConcatenate();
    String getArrayConcatenate();

    boolean supportGroupSingleValue();

    String getAnyValueFunc();
    String getLastFunc();
    String getMaxMin(boolean max, String expr1, String expr2);
    String getNotZero(String expr);

    boolean supportsAnalyzeSessionTable();
    String getAnalyzeSessionTable(String tableName);

    boolean supportsVolatileStats();
    String getVolatileStats(boolean on);

    String getChangeColumnType();

    String getStringCFunc();

    String getOrderGroupAgg(GroupType groupType, ImList<String> exprs, ImList<ClassReader> readers, ImOrderMap<String, CompileOrder> orders, TypeEnvironment typeEnv);

    String getNotSafeConcatenateSource(ConcatenateType type, ImList<String> exprs, TypeEnvironment typeEnv);

    boolean isIndexNameLocal();

    String getRenameColumn(String table, String columnName, String newColumnName);
}
