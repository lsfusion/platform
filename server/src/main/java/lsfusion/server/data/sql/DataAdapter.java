package lsfusion.server.data.sql;

import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.AbstractConnectionPool;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.SessionTable;
import lsfusion.server.data.TypePool;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.*;
import lsfusion.server.data.type.*;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.ExecutionContext;
import org.apache.log4j.Logger;
import org.springframework.util.PropertyPlaceholderHelper;

import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Properties;

public abstract class DataAdapter extends AbstractConnectionPool implements SQLSyntax, TypePool {
    protected final static Logger logger = Logger.getLogger(DataAdapter.class);

    public String server;
    public String instance;
    public String dataBase;
    public String userID;
    public String password;
    public Long connectTimeout;

    // для debuga
    protected DataAdapter() {
    }

    protected abstract void ensureDB(boolean cleanDB) throws Exception, SQLException, InstantiationException, IllegalAccessException;

    protected DataAdapter(String dataBase, String server, String instance, String userID, String password, Long connectTimeout, boolean cleanDB) throws Exception, SQLException, IllegalAccessException, InstantiationException {

        Class.forName(getClassName());

        this.dataBase = dataBase;
        this.server = server;
        this.userID = userID;
        this.password = password;
        this.connectTimeout = connectTimeout;
        this.instance = instance;

        ensureDB(cleanDB);

        ensureConnection = startConnection();
        ensureConnection.setAutoCommit(true);
        ensureSystemFuncs();
    }

    protected void ensureSystemFuncs() throws IOException, SQLException {
        throw new UnsupportedOperationException();        
    }

    public String getBPTextType() {
        throw new UnsupportedOperationException();
    }

    public int getBPTextSQL() {
        throw new UnsupportedOperationException();
    }

    public String getStringType(int length) {
        return "char(" + length + ")";
    }
    public int getStringSQL() {
        return Types.CHAR;
    }

    @Override
    public String getVarStringType(int length) {
        return "varchar(" + length + ")";
    }
    @Override
    public int getVarStringSQL() {
        return Types.VARCHAR;
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

    public String getTimeType() {
        return "time";
    }
    public int getTimeSQL() {
        return Types.TIME;
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

    public boolean hasDriverCompositeProblem() {
        return false;
    }

    public int getCompositeSQL() {
        return Types.BINARY;
    }

    public String getByteArrayType() {
        return "longvarbinary";
    }
    public int getByteArraySQL() {
        return Types.LONGVARBINARY;
    }

    public String getColorType() {
        return "integer";
    }

    public int getColorSQL() {
        return Types.INTEGER;
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

    public String getQueryName(String tableName, SessionTable.TypeStruct type, StringBuilder envString, boolean usedRecursion) {
        return getSessionTableName(tableName);
    }

    public boolean isGreatest() {
        return true;
    }

    public boolean useFJ() {
        return false;
    }

    public boolean orderUnion() {
        return false;
    }

    public String getDropSessionTable(String tableName) {
        return "DROP TABLE " + getSessionTableName(tableName);
    }

    public String getOrderDirection(boolean descending, boolean notNull) {
        return descending ? "DESC" : "ASC";
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
        return "DATE_TRUNC('second', CURRENT_TIMESTAMP)";
    }

    public String getTypeChange(Type oldType, Type type, String name, MStaticExecuteEnvironment env) {
        throw new UnsupportedOperationException();
    }

    public String getInsensitiveLike() {
        return "LIKE";
    }

    public boolean supportGroupNumbers() {
        return false;
    }

    public String getCountDistinct(String field) {
        return "COUNT(DISTINCT " + field + ")";
    }
    public String getCount(String field) {
        return "COUNT(" + field + ")";
    }

    public boolean noMaxImplicitCast() {
        return false;
    }

    public boolean noDynamicSampling() {
        return false;
    }

    public void setLogLevel(Connection connection, int level) {
    }

    public boolean orderTopProblem() {
        throw new RuntimeException("unknown");
    }

    public String getCancelActiveTaskQuery(Integer pid) {
        return "";
    }
    
    public String backupDB(ExecutionContext context, String dumpFileName, List<String> excludeTables) throws IOException, InterruptedException {
        return null;
    }

    public void killProcess(Integer processId) {
    }

    public String getAnalyze(){
        return "";
    }

    public String getVacuumDB(){
        return "";
    }

    public static String genTypePostfix(ImList<Type> types) {
        return genTypePostfix(types, new boolean[types.size()]);
    }

    public static String genTypePostfix(ImList<Type> types, boolean[] desc) {
        String result = "";
        for(int i=0,size=types.size();i<size;i++)
            result = (result.length()==0?"":result + "_") + types.get(i).getSID() + (desc[i]?"_D":"");
        return result;
    }

    public String getConcTypeName(ConcatenateType type) {
        return "T" + genTypePostfix(type.getTypes(), type.getDesc());
    }

    public String getIIF(String ifWhere, String trueExpr, String falseExpr) {
        return "CASE WHEN " + ifWhere + " THEN " + trueExpr + " ELSE " + falseExpr + " END";
    }

    public String getAndExpr(String where, String expr, Type type, TypeEnvironment typeEnv) {
        return getIIF(where, expr, SQLSyntax.NULL);
    }

    public static String genNRowName(ImList<Type> types) {
        return "NROW" + types.size();
    }

    protected final TypeEnvironment recTypes = new TypeEnvironment() {
        public void addNeedRecursion(Object types) {
            throw new UnsupportedOperationException();
        }

        public void addNeedType(ConcatenateType types) {
            try {
                ensureConcType(types);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public void addNeedTableType(SessionTable.TypeStruct tableType) {
            try {
                ensureTableType(tableType);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public void addNeedAggOrder(GroupType groupType, ImList<Type> types) {
            throw new UnsupportedOperationException();
        }

        public void addNeedTypeFunc(TypeFunc typeFunc, Type type) {
            throw new UnsupportedOperationException();
        }

        public void addNeedArrayClass(ArrayClass tableType) {
            throw new UnsupportedOperationException();
        }

        public void addNeedSafeCast(Type type) {
            throw new UnsupportedOperationException();
        }
    };

    protected void ensureTableType(SessionTable.TypeStruct tableType) throws SQLException {
        throw new UnsupportedOperationException();
    }
    
    public String getTableTypeName(SessionTable.TypeStruct tableType) {
        throw new UnsupportedOperationException();
    }

    public boolean noDynamicSQL() {
        throw new UnsupportedOperationException();
    }

    public boolean enabledCTE() {
        throw new UnsupportedOperationException();
    }

    protected Connection ensureConnection;

    protected void executeEnsure(String command) throws SQLException {
        Statement statement = ensureConnection.createStatement();
//        statement.setQueryTimeout(1);
        try {
            statement.execute(command);
        } catch(SQLException e) {
            ServerLoggers.sqlSuppLog(e);
        } finally {
            statement.close();
        }
    }

    protected void executeEnsureParams(String command, ImList<TypeObject> params) throws SQLException {
        PreparedStatement statement = ensureConnection.prepareStatement(command);
//        statement.setQueryTimeout(1);
        SQLSession.ParamNum paramNum = new SQLSession.ParamNum();
        for(TypeObject param : params)
            param.writeParam(statement, paramNum, this);
        try {
            statement.execute();
        } catch(SQLException e) {
            ServerLoggers.sqlSuppLog(e);
        } finally {
            statement.close();
        }
    }

    protected MAddExclMap<ConcatenateType, Boolean> ensuredConcTypes = MapFact.mAddExclMap();

    protected String notNullRowString;

    protected void proceedEnsureConcType(ConcatenateType concType) throws SQLException {
        throw new UnsupportedOperationException();
    }
    
    public synchronized void ensureConcType(ConcatenateType concType) throws SQLException {

        Boolean ensured = ensuredConcTypes.get(concType);
        if(ensured != null)
            return;

        proceedEnsureConcType(concType);

        ensuredConcTypes.exclAdd(concType, true);
    }

    public static final PropertyPlaceholderHelper stringResolver = new PropertyPlaceholderHelper("${", "}", ":", true);

    public synchronized void ensureRecursion(Object ot) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void ensureArrayClass(ArrayClass arrayClass) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public static String genSafeCastName(Type type) {
        return "scast_" + type.getSID();
    }

    protected String safeCastString;

    private LRUSVSMap<Type, Boolean> ensuredSafeCasts = new LRUSVSMap<Type, Boolean>(LRUUtil.G2);

    public synchronized void ensureSafeCast(Type type) throws SQLException {
        Boolean ensured = ensuredSafeCasts.get(type);
        if(ensured != null)
            return;

        Properties properties = new Properties();
        properties.put("function.name", genSafeCastName(type));
        properties.put("param.type", type.getDB(this, recTypes));
        properties.put("param.minvalue", type.getInfiniteValue(true).toString());
        properties.put("param.maxvalue", type.getInfiniteValue(false).toString());

        executeEnsure(stringResolver.replacePlaceholders(safeCastString, properties));

        ensuredSafeCasts.put(type, true);
    }
    
    public String getSafeCastNameFnc(Type type) {
        return genSafeCastName(type);
    }

    public void ensureGroupAggOrder(Pair<GroupType, ImList<Type>> groupAggOrder) throws SQLException {
    }

    public void ensureTypeFunc(Pair<TypeFunc, Type> tf) throws SQLException {
    }

    public boolean isDeadLock(SQLException e) {
        throw new UnsupportedOperationException();
    }

    public boolean isUpdateConflict(SQLException e) {
        throw new UnsupportedOperationException();
    }

    public boolean isUniqueViolation(SQLException e) {
        throw new UnsupportedOperationException();
    }

    public boolean isTimeout(SQLException e) {
        return false;
    }

    public String getRandom() {
        return "random()";
    }

    public boolean isTransactionCanceled(SQLException e) {
        throw new UnsupportedOperationException();
    }

    public boolean isConnectionClosed(SQLException e) {
        return false;
    }

    public boolean hasJDBCTimeoutMultiThreadProblem() {
        throw new UnsupportedOperationException();
    }

    public void setACID(Statement statement, boolean acid) throws SQLException {
    }

    public String getMetaName(String name) {
        return name;
    }

    public String getFieldName(String name) {
        return name;
    }

    public String getTableName(String name) {
        return name;
    }

    public String getGlobalTableName(String name) {
        return name;
    }

    public String getConstraintName(String name) {
        return name;
    }

    public String getIndexName(String name) {
        return name;
    }

    public void ensureLogLevel(int logLevel) {
    }

    public boolean hasSelectivityProblem() {
        return false;
    }

    public String getAdjustSelectivityPredicate() {
        throw new UnsupportedOperationException();
    }

    public String getStringConcatenate() {
        return "+";
    }

    public String getArrayConcatenate(ArrayClass arrayClass, String prm1, String prm2, TypeEnvironment env) {
        throw new UnsupportedOperationException();
    }

    public String getArrayAgg(String s, ClassReader classReader, TypeEnvironment typeEnv) {
        throw new UnsupportedOperationException();
    }

    public boolean supportGroupSingleValue() {
        return true;
    }
    
    protected SQLSyntax getSyntax() {
        return this;
    }

    public String getAnyValueFunc() {
        throw new UnsupportedOperationException();
    }

    public String getStringCFunc() {
        throw new UnsupportedOperationException();
    }

    public String getLastFunc() {
        throw new UnsupportedOperationException();
    }

    public String getOrderGroupAgg(GroupType groupType, ImList<String> exprs, ImList<ClassReader> readers, ImOrderMap<String, CompileOrder> orders, TypeEnvironment typeEnv) {
        String orderClause = BaseUtils.clause("ORDER BY", Query.stringOrder(orders, this));

        String fnc;
        switch (groupType) {
            case STRING_AGG:
                fnc = "STRING_AGG";
                break;
            case LAST:
                fnc = getLastFunc();
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return fnc + "(" + exprs.toString(",") + orderClause + ")";
    }

    public String getNotSafeConcatenateSource(ConcatenateType type, ImList<String> exprs, TypeEnvironment typeEnv) {
        throw new UnsupportedOperationException();
    }

    public boolean isIndexNameLocal() {
        throw new UnsupportedOperationException();
    }

    public String getParamUsage(int num) {
        throw new UnsupportedOperationException();
    }

    public String getRecursion(ImList<FunctionType> types, String recName, String initialSelect, String stepSelect, String fieldDeclare, String outerParams, TypeEnvironment typeEnv) {
        throw new UnsupportedOperationException();
    }

    public String getArrayConstructor(String source, ArrayClass rowType, TypeEnvironment env) {
        throw new UnsupportedOperationException();
    }

    public String getArrayType(ArrayClass arrayClass, TypeEnvironment typeEnv) {
        throw new UnsupportedOperationException();
    }

    public String getInArray(String element, String array) {
        throw new UnsupportedOperationException();
    }

    public boolean doesNotTrimWhenCastToVarChar() {
        throw new UnsupportedOperationException();
    }

    public boolean hasGroupByConstantProblem() {
        return false;
    }

    public String getRenameColumn(String table, String columnName, String newColumnName) {
        return "ALTER TABLE " + table + " RENAME " + columnName + " TO " + newColumnName;
    }

    public String getMaxMin(boolean max, String expr1, String expr2, Type type, TypeEnvironment typeEnv) {
        throw new UnsupportedOperationException();
    }

    // должно быть синхронизировано с StaticClass.isZero
    public String getNotZero(String expr, Type type, TypeEnvironment typeEnv) {
        throw new UnsupportedOperationException();
    }

    public boolean supportsAnalyzeSessionTable() {
        return false;
    }

    public String getAnalyzeSessionTable(String tableName) {
        throw new UnsupportedOperationException();
    }

    public boolean supportsDisableNestedLoop() {
        return false;
    }

    public boolean supportsNoCount() {
        return false;
    }

    public String getVolatileStats(boolean on) {
        throw new UnsupportedOperationException();
    }

    public String getChangeColumnType() {
        return "";
    }

    public SQLSyntaxType getSyntaxType() {
        throw new UnsupportedOperationException();
    }

    public Date fixDate(Date value) {
        return value;
    }

    public Timestamp fixDateTime(Timestamp value) {
        return value;
    }

    public boolean hasAggConcProblem() {
        return false;
    }

    public boolean hasNotNullIndexProblem() { // проблема если идет join по a.f=b и есть индекс по f но там много NULL, субд не догадывается использовать этот индекс для фильтрации NOT NULL
        return false;
    }

    protected String getPath() {
        throw new UnsupportedOperationException();
    }
    public void ensureScript(String script, Properties props) throws SQLException, IOException {
        String scriptString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream(getPath() + script));
        executeEnsure(stringResolver.replacePlaceholders(scriptString, props));
    }
}
