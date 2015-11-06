package lsfusion.server.data.sql;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.SessionTable;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.CompileOrder;
import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.query.StaticExecuteEnvironment;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.type.*;

import java.sql.*;

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
    String getQueryName(String tableName, SessionTable.TypeStruct type, StringBuilder envString, boolean usedRecursion);
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
    boolean inlineSelfJoinTrouble();

    String getOrderDirection(boolean descending, boolean notNull);

    String getHour();
    String getMinute();
    String getEpoch();
    String getDateTime();

    String getInsensitiveLike();

    boolean supportGroupNumbers();

    String getCountDistinct(String field);
    String getCount(String field);

    boolean noMaxImplicitCast();

    boolean noDynamicSampling();

    boolean orderTopProblem();
    
    boolean isDeadLock(SQLException e);

    boolean isUpdateConflict(SQLException e);

    boolean isUniqueViolation(SQLException e);

    boolean isTimeout(SQLException e);

    boolean isTransactionCanceled(SQLException e);

    boolean isConnectionClosed(SQLException e);
    
    String getRandom();

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

    boolean supportGroupSingleValue();

    String getAnyValueFunc();
    String getLastFunc();
    String getMaxMin(boolean max, String expr1, String expr2, Type type, TypeEnvironment typeEnv);
    String getNotZero(String expr, Type type, TypeEnvironment typeEnv);

    boolean supportsAnalyzeSessionTable();
    String getAnalyzeSessionTable(String tableName);

    boolean supportsDisableNestedLoop();
    boolean supportsNoCount();
    String getVolatileStats(boolean on);

    String getChangeColumnType();

    String getStringCFunc();

    String getOrderGroupAgg(GroupType groupType, ImList<String> exprs, ImList<ClassReader> readers, ImOrderMap<String, CompileOrder> orders, TypeEnvironment typeEnv);

    String getNotSafeConcatenateSource(ConcatenateType type, ImList<String> exprs, TypeEnvironment typeEnv);

    boolean isIndexNameLocal();

    String getRenameColumn(String table, String columnName, String newColumnName);

    String getParamUsage(int num);

    String getRecursion(ImList<FunctionType> types, String recName, String initialSelect, String stepSelect, String stepSmallSelect, int smallLimit, String fieldDeclare, String outerParams, TypeEnvironment typeEnv);
    String wrapSubQueryRecursion(String string);

    String getTableTypeName(SessionTable.TypeStruct tableType);

    boolean noDynamicSQL();

    boolean enabledCTE();

    String getArrayConstructor(String source, ArrayClass rowType, TypeEnvironment env);

    String getArrayConcatenate(ArrayClass arrayClass, String prm1, String prm2, TypeEnvironment env);

    String getArrayAgg(String s, ClassReader classReader, TypeEnvironment typeEnv);

    String getArrayType(ArrayClass arrayClass, TypeEnvironment typeEnv);

    String getInArray(String element, String array);

    boolean hasGroupByConstantProblem();

    SQLSyntaxType getSyntaxType();

    String getSafeCastNameFnc(Type type);

    Date fixDate(Date value);

    Timestamp fixDateTime(Timestamp value);

    boolean hasAggConcProblem();

    String getConcTypeName(ConcatenateType type);

    String getIIF(String ifWhere, String trueExpr, String falseExpr);

    String getAndExpr(String where, String expr, Type type, TypeEnvironment typeEnv);

    boolean doesNotTrimWhenCastToVarChar();

    String getTypeChange(Type oldType, Type type, String name, MStaticExecuteEnvironment env);

    boolean hasNotNullIndexProblem(); // проблема если идет join по a.f=b и есть индекс по f но там много NULL, субд не догадывается использовать этот индекс для фильтрации NOT NULL

    boolean hasNullWhereEstimateProblem(); // проблема при A LEFT JOIN B WHERE B.f IS NULL, где в A очень много записей в B очень мало, СУБД сначала join'ит их, а потом применяет selectivity f

    boolean hasTransactionSavepointProblem();
}
