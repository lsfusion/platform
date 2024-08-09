package lsfusion.server.data.sql.syntax;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.query.exec.MStaticExecuteEnvironment;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.FunctionType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.EnsureTypeEnvironment;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.ArrayClass;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public interface SQLSyntax {

    String NULL = "NULL";

    boolean allowViews();

    String getCancelActiveTaskQuery(Integer pid);

    String getUpdate(String tableString,String setString,String fromString,String whereString);

    String getClassName();

    String isNULL(String exprs, boolean notSafe);

    String getClustered();
    String getCommandEnd();
    
    String getSessionTableName(String tableName);
    String getQueryName(String tableName, SessionTable.TypeStruct type, StringBuilder envString, boolean usedRecursion, EnsureTypeEnvironment typeEnv);
    String getCreateSessionTable(String tableName, String declareString);
    String getDropSessionTable(String tableName);

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    boolean isNullSafe();
    boolean isGreatest();
    
    void setLogLevel(int level);

    boolean useFJ();

    boolean orderUnion(); // распихивать order'ы внутрь union all'ов

    int updateModel();

    String getBPTextType();
    int getBPTextSQL();

    String getStringType(int length);
    int getStringSQL();

    String getVarStringType(int length);
    int getVarStringSQL();

    String getNumericType(ExtInt precision, ExtInt scale);
    int getNumericSQL();

    String getIntegerType();
    int getIntegerSQL();

    String getDateType();
    int getDateSQL();

    String getDateTimeType(ExtInt millisLength);
    int getDateTimeSQL();

    String getZDateTimeType(ExtInt millisLength);
    int getZDateTimeSQL();

    String getTimeType(ExtInt millisLength);
    int getTimeSQL();

    int getIntervalSQL();

    String getLongType();
    int getLongSQL();

    String getDoubleType();
    int getDoubleSQL();

    int getBitSQL();

    String getBitString(Boolean value);

    String getTextType();
    int getTextSQL();

    boolean hasDriverCompositeProblem();
    int getCompositeSQL();

    String getByteArrayType();
    int getByteArraySQL();

    int getColorSQL();

    String getJSON();

    String getJSONText();

    String getTSVector();

    String getTSQuery();

    default String getSelect(String from, String exprs, String where) {
        return getSelect(from, exprs, where, "", "", false);
    }
    default String getSelect(String from, String exprs, String where, String orderBy, String top, boolean distinct) {
        return getSelect(from, exprs, where, orderBy, "", "", top, distinct);
    }
    String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String having, String top, boolean distinct);

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

    String getPrefixSearchQuery();

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
    
    boolean isTableDoesNotExist(SQLException e);
    
    String getRetryWithReason(SQLException e);

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

    boolean supportsDeadLockPriority();

    String getChangeColumnType();

    String getStringCFunc();

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

    String getArrayType(ArrayClass arrayClass, TypeEnvironment typeEnv);

    String getInArray(String element, String array);

    boolean hasGroupByConstantProblem();

    SQLSyntaxType getSyntaxType();

    String getSafeCastNameFnc(Type type, Integer sourceType);

    Date fixDate(Date value);

    Timestamp fixDateTime(Timestamp value);

    boolean hasAggConcProblem();

    String getConcTypeName(ConcatenateType type);

    String getIIF(String ifWhere, String trueExpr, String falseExpr);

    String getAndExpr(String where, String expr, Type type, TypeEnvironment typeEnv);

    boolean doesNotTrimWhenCastToVarChar();
    boolean doesNotTrimWhenSumStrings();

    String getTypeChange(Type oldType, Type type, String name, MStaticExecuteEnvironment env);

    boolean hasNotNullIndexProblem(); // проблема если идет join по a.f=b и есть индекс по f но там много NULL, субд не догадывается использовать этот индекс для фильтрации NOT NULL

    boolean hasNullWhereEstimateProblem(); // проблема при A LEFT JOIN B WHERE B.f IS NULL, где в A очень много записей в B очень мало, СУБД сначала join'ит их, а потом применяет selectivity f

    boolean hasTransactionSavepointProblem();

    String getDeadlockPriority(Long priority);
    boolean useFailedTimeInDeadlockPriority();

    String getAnalyze(String table);
    String getVacuum(String table);
    
    String getAnalyze();    
    String getVacuumDB();

    // проблема, когда округление деления division в СУБД зависит от scale'а параметров + логически округление типов не всегда совпадает с СУБД (скажем в CASE \ UNION сервер приложений считает что будет максимальный тип, и может округлить до него материализацией, а сервер СУБД этого не делает и берет скажем последний тип), тогда если материализуется один из под
    // соответственно тут два варианта: a) либо гарантировать что округление в СУБД всегда совпадает с округлением в логике сервера приложений (то есть Case\IF, Union'ы придется cast'ить)
    // b) в местах где округление может играть роль : деление вставлять явные cast'ы параметров (cast'ы значений и так есть)
    // возвращает минимальное округление которое может делать СУБД (для оптимизации) : -1 если нет проблемы
    int getFloatingDivisionProblem();

    void setQueryTimeout(Statement statement, long setTimeout) throws SQLException;
}
