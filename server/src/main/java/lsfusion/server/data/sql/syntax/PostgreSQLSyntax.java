package lsfusion.server.data.sql.syntax;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.query.exec.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.adapter.PostgreDataAdapter;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.FunctionType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.ArrayClass;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.Log4jWriter;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.jdbc.PgStatement;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;

import java.io.PrintWriter;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public class PostgreSQLSyntax extends DefaultSQLSyntax {
    
    public final static PostgreSQLSyntax instance = new PostgreSQLSyntax();

    private PostgreSQLSyntax() {
    }

    public static String genRecursionName(ImList<Type> types) {
        return "recursion_" + genTypePostfix(types);
    }

    public boolean allowViews() {
        return true;
    }

    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        return tableString + setString + " FROM " + fromString + whereString;
    }

    public String getClassName() {
        return "org.postgresql.Driver";
    }

    public String isNULL(String exprs, boolean notSafe) {
//        return "(CASE WHEN "+Expr1+" IS NULL THEN "+Expr2+" ELSE "+Expr1+" END)";
        return "COALESCE(" + exprs + ")";
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String having, String top, String offset, boolean distinct) {
        return "SELECT " + (distinct ? "DISTINCT " : "") + exprs + " FROM " + from + BaseUtils.clause("WHERE", where) + BaseUtils.clause("GROUP BY", groupBy) + BaseUtils.clause("HAVING", having) + BaseUtils.clause("ORDER BY", orderBy) + BaseUtils.clause("LIMIT", top) + BaseUtils.clause("OFFSET", offset);
    }

    public String getUnionOrder(String union, String orderBy, String top, String offset) {
        return union + BaseUtils.clause("ORDER BY", orderBy) + BaseUtils.clause("LIMIT", top) + BaseUtils.clause("OFFSET", offset);
    }

    @Override
    public String getLongType() {
        return "int8";
    }

    @Override
    public int getLongSQL() {
        return Types.BIGINT;
    }

    public String getCommandEnd() {
        return ";";
    }

    public String getClustered() {
        return "";
    }

    // у SQL сервера что-то гдючит ISNULL (а значит скорее всего и COALESCE) когда в подзапросе просто число указывается
    public boolean isNullSafe() {
        return false;
    }

    public String getByteArrayType() {
        return "bytea";
    }

    @Override
    public int getByteArraySQL() {
        return Types.VARBINARY;
    }

    @Override
    public String getOrderDirection(boolean descending, boolean notNull) {
        return (descending ? "DESC" : "ASC") + (!notNull ? " NULLS " + (descending ? "LAST" : "FIRST") : "");  // так как по умолчанию не nulls first
    }

    @Override
    public boolean hasDriverCompositeProblem() {
        return true;
    }

    @Override
    public int getCompositeSQL() {
        throw new RuntimeException("not supported");
    }

    @Override
    public boolean useFJ() {
        return false;
    }

    @Override
    public boolean orderUnion() {
        return true;
    }

    @Override
    public boolean nullUnionTrouble() {
        return true;
    }

    @Override
    public boolean inlineTrouble() {
        return true;
    }

    @Override
    public boolean inlineSelfJoinTrouble() {
        return true;
    }

    @Override
    public String getTypeChange(Type oldType, Type type, String name, MStaticExecuteEnvironment env) {
        String newType = type.getDB(this, env);
        return "TYPE " + newType + " USING " + name + "::" + newType;
    }

    @Override
    public String getPrefixSearchQuery(boolean exact) {
        int dbMajorVersion = ((PostgreDataAdapter) ThreadLocalContext.getDbManager().getAdapter()).getDbMajorVersion();
        return dbMajorVersion >= 11 ? super.getPrefixSearchQuery(exact) : "prefixSearchOld";
    }

    @Override
    public String getInsensitiveLike() {
        return "ILIKE";
    }

    public boolean supportGroupNumbers() {
        return true;
    }

    @Override
    public String getCancelActiveTaskQuery(Integer pid) {
        return String.format("SELECT pg_cancel_backend(%s)", pid);
    }

    @Override
    public String getAnalyze() {
        return "ANALYZE";
    }

    @Override
    public String getVacuumDB() {
        return "VACUUM FULL";
    }

    // every ordinary VACUUM finishes by recomputing the database-wide frozen xid, and that step reads all of pg_class. On an installation whose pg_class is bloated by exactly the temporary
    // table churn this vacuum is part of, that scan - not the table - is what the command costs, and it serializes. SKIP_DATABASE_STATS drops that step and nothing else (PostgreSQL 16+)
    // the version is the one of the server this statement runs on, not a global : a master and its slaves are separate connections, and an option a 16 accepts is a syntax error on a 15
    @Override
    public String getVacuumSessionTable(String table, int serverMajorVersion) {
        return serverMajorVersion >= 16 ? "VACUUM (SKIP_DATABASE_STATS) " + table : super.getVacuumSessionTable(table, serverMajorVersion);
    }

    @Override
    public String getBPTextType() {
        return "bpchar";
    }

    @Override
    public boolean noMaxImplicitCast() {
        return true;
    }

    @Override
    public boolean isDeadLock(SQLException e) {
        return "40P01".equals(e.getSQLState()); // null safe : the state can be missing on a driver level failure, and this runs from inside the error handling
    }

    @Override
    public boolean isUpdateConflict(SQLException e) {
        return "40001".equals(e.getSQLState()); // null safe : the state can be missing on a driver level failure, and this runs from inside the error handling
    }

    @Override
    public boolean isUniqueViolation(SQLException e) {
        return "23505".equals(e.getSQLState()); // null safe : the state can be missing on a driver level failure, and this runs from inside the error handling
    }

    @Override
    public boolean isTableDoesNotExist(SQLException e) {
        return "42P01".equals(e.getSQLState()); // null safe : the state can be missing on a driver level failure, and this runs from inside the error handling
    }

    @Override
    public boolean isTimeout(SQLException e) {
        return "57014".equals(e.getSQLState()); // null safe : the state can be missing on a driver level failure, and this runs from inside the error handling
    }

    @Override
    public String getRetryWithReason(SQLException e) {
        if(willHealViaReparse(e))
            return e.getMessage();
        return null;
    }

    private boolean willHealViaReparse(SQLException e) {
        // copy from QueryExecutorBase willHealViaReparse
        if (PSQLState.INVALID_SQL_STATEMENT_NAME.getState().equals(e.getSQLState())) {
            return true;
        }
        if (!PSQLState.NOT_IMPLEMENTED.getState().equals(e.getSQLState())) {
            return false;
        }

        if (!(e instanceof PSQLException)) {
            return false;
        }

        PSQLException pe = (PSQLException) e;

        ServerErrorMessage serverErrorMessage = pe.getServerErrorMessage();
        if (serverErrorMessage == null) {
            return false;
        }
        // "cached plan must not change result type"
        String routine = pe.getServerErrorMessage().getRoutine();
        return "RevalidateCachedQuery".equals(routine) // 9.2+
                || "RevalidateCachedPlan".equals(routine); // <= 9.1
    }

    @Override
    public boolean hasJDBCTimeoutMultiThreadProblem() {
        return true;
    }

    @Override
    public boolean isTransactionCanceled(SQLException e) {
        return "25P02".equals(e.getSQLState()); // null safe : the state can be missing on a driver level failure, and this runs from inside the error handling
    }

    @Override
    public boolean isConnectionClosed(SQLException e) {
        String sqlState = e.getSQLState();
        return "08003".equals(sqlState) || "08006".equals(sqlState); // null safe : the state can be missing on a driver level failure, and this runs from inside the error handling
    }

    @Override
    public boolean hasSelectivityProblem() {
        return true;
    }

    @Override
    public String getAdjustSelectivityPredicate() {
        return "localtimestamp<>localtimestamp";
    }

    @Override
    public String getStringConcatenate() {
        return "||";
    }

    @Override
    public String getArrayConcatenate(ArrayClass arrayClass, String prm1, String prm2, TypeEnvironment env) {
        return arrayClass.getCast("(" + prm1 + " || " + prm2 + ")", this, env);
    }

    @Override
    public boolean orderTopProblem() {
        return true;
    }

    @Override
    public String getAnyValueFunc() {
        return "ANYVALUE";
    }

    @Override
    public String getStringCFunc() {
        return "STRINGC";
    }

    @Override
    public String getLastFunc() {
        return "LAST";
    }

    @Override
    public String getMaxMin(boolean max, String expr1, String expr2, Type type, TypeEnvironment typeEnv) {
        return getMaxMin(max, ListFact.toList(expr1, expr2), type, typeEnv);
    }

    @Override
    public String getMaxMin(boolean max, ImList<String> exprs, Type type, TypeEnvironment typeEnv) {
        // GREATEST / LEAST are native, take any number of arguments, and skip NULLs exactly like the MAX / MIN
        // functions from aggf.sql. those take anyelement, so postgres requires every argument to have the *same*
        // actual type and never coerces them itself - which breaks whenever the SQL type of an operand is wider
        // than its declared class (SUM(int4) is bigint, SUM(int8) and extract(epoch ...) are numeric, ...)
        return (max?"GREATEST":"LEAST") + "(" + exprs.toString(",") + ")";
    }

    @Override
    public String getNotZero(String expr, Type type, TypeEnvironment typeEnv) {
        return "notZero(" + expr + ")";
    }

    @Override
    public SQLSyntaxType getSyntaxType() {
        return SQLSyntaxType.POSTGRES;
    }

    @Override
    public boolean supportsAnalyzeSessionTable() {
        return true;
    }

    @Override
    public boolean supportsDisableNestedLoop() {
        return true;
    }

    @Override
    public boolean supportsDeadLockPriority() {
        return false;
    }

    @Override
    public String getVolatileStats(boolean on) {
        return "SET enable_nestloop=" + (on ? "off" : "on");
    }

    @Override
    public String getChangeColumnType() {
        return " TYPE ";
    }

    @Override
    public boolean noDynamicSampling() {
        return true;
    }

    @Override
    public String getNotSafeConcatenateSource(ConcatenateType type, ImList<String> exprs, TypeEnvironment typeEnv) {
        return type.getCast("ROW(" + exprs.toString(",") + ")", this, typeEnv);
    }
    
    @Override
    public boolean isIndexNameLocal() {
        return false;
    }

    @Override
    public String getParamUsage(int num) {
        return "$" + num;
    }

    @Override
    public boolean noDynamicSQL() {
        return false;
    }

    @Override
    public boolean enabledCTE() {
        return true;
    }

    @Override
    public String getRecursion(ImList<FunctionType> types, String recName, String initialSelect, String stepSelect, String stepSmallSelect, int smallLimit, String fieldDeclare, String outerParams, TypeEnvironment typeEnv) {
        assert types.size() == types.filterList(element -> element instanceof Type).size();

        typeEnv.addNeedRecursion(types);
        String recursionName = genRecursionName(BaseUtils.immutableCast(types));
        return recursionName + "('" + recName + "'" +
                ",'(" + escapeSql(initialSelect) + ")'" + ",'(" + escapeSql(stepSelect) + ")'" +
                ",'(" + escapeSql(stepSmallSelect) + ")'" + "," + smallLimit +
                (outerParams.length() == 0 ? "" : "," + outerParams) + ") recursion (" + fieldDeclare + ")";
    }

    @Override
    public String wrapSubQueryRecursion(String string) {
        return escapeSql(string);
    }

    private String escapeSql(String sql) {
        return StringUtils.replace(sql, "'", "''");
    }

    @Override
    public String getArrayConstructor(String source, ArrayClass rowType, TypeEnvironment env) {
        return rowType.getCast("ARRAY[" + source + "]", this, env);
    }

    @Override
    public String getInArray(String element, String array) {
        return element + " = ANY(" + array + ")";
    }

    @Override
    public boolean doesNotTrimWhenCastToVarChar() {
        return false;
    }
    public boolean doesNotTrimWhenSumStrings() {
        return false;
    }

    public String getArrayType(ArrayClass arrayClass, TypeEnvironment typeEnv) {
        return arrayClass.getArrayType().getDB(this, typeEnv) + "[]";
    }

    @Override
    public boolean hasAggConcProblem() {
        return true;
    }

    @Override
    public boolean hasNotNullIndexProblem() {
        return true;
    }

    @Override
    public boolean hasNullWhereEstimateProblem() {
        return true;
    }

    @Override
    public boolean hasTransactionSavepointProblem() {
        return true;
    }

    @Override
    public String getAnalyzeSessionTable(String table) {

        String result = super.getAnalyzeSessionTable(table);
        int tempStatisticsTarget = Settings.get().getTempStatisticsTarget();
        if(tempStatisticsTarget > 0)
            result = "SET default_statistics_target=" +tempStatisticsTarget + ";" + result + ";SET default_statistics_target=DEFAULT";
        return result;
    }

    @Override
    public String getDeadlockPriority(Long priority) {
        assert false; // actually this approach doesn't work
        return "SET LOCAL deadlock_timeout to " + (priority != null ? ("'" + Math.round(BaseUtils.pow(2.0, priority) * 1000.0) + "ms'") : "DEFAULT");
    }

    @Override
    public boolean useFailedTimeInDeadlockPriority() {
        return true;
    }

    @Override
    public int getFloatingDivisionProblem() {
        return 16;
    }

    @Override
    public void setLogLevel(int level) {
        if (level != 0 && DriverManager.getLogWriter() == null)
        {
            DriverManager.setLogWriter(new PrintWriter(new Log4jWriter(ServerLoggers.jdbcLogger), false));
        }
    }

    @Override
    public void setACID(Statement statement, boolean acid) throws SQLException {
        statement.execute("SET SESSION synchronous_commit TO " + (acid ? "DEFAULT" : "OFF"));
        statement.execute("SET SESSION commit_delay TO " + (acid ? "DEFAULT" : "100000"));
    }

    @Override
    public void setQueryTimeout(Statement statement, long setTimeout) throws SQLException {
        ((PgStatement)statement).setQueryTimeoutMs(setTimeout);
    }
}
