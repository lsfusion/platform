package lsfusion.server.data.sql;

import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.SFunctionSet;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.Log4jWriter;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.type.*;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.property.ExecutionContext;
import org.apache.commons.exec.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.postgresql.Driver;
import org.postgresql.PGConnection;
import org.postgresql.core.BaseConnection;
import org.postgresql.util.PSQLException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static lsfusion.base.BaseUtils.isRedundantString;
import static lsfusion.server.logics.ServerResourceBundle.getString;


public class PostgreDataAdapter extends DataAdapter {

    public final static SQLSyntax debugSyntax = new PostgreDataAdapter();

    public String binPath;
    public String dumpDir;

    // Для debuga конструктор
    public PostgreDataAdapter() {
    }

    public PostgreDataAdapter(String dataBase, String server, String userID, String password) throws Exception {
        this(dataBase, server, userID, password, false);
    }

    public PostgreDataAdapter(String dataBase, String server, String userID, String password, boolean cleanDB) throws Exception{
        this(dataBase, server, userID, password, null, null, null, cleanDB);
    }

    public PostgreDataAdapter(String dataBase, String server, String userID, String password, Long connectTimeout, String binPath, String dumpDir) throws Exception {
        this(dataBase, server, userID, password, connectTimeout, binPath, dumpDir, false);
    }

    public PostgreDataAdapter(String dataBase, String server, String userID, String password, Long connectTimeout, String binPath, String dumpDir, boolean cleanDB) throws Exception {
        super(dataBase, server, null, userID, password, connectTimeout, cleanDB);

        this.binPath = binPath;
        this.dumpDir = dumpDir;
    }

    public void ensureDB(boolean cleanDB) throws Exception, SQLException, InstantiationException, IllegalAccessException {

        Connection connect = null;
        while(connect == null) {
            try {
                connect = DriverManager.getConnection("jdbc:postgresql://" + server + "/postgres?user=" + userID + "&password=" + password);
            } catch (PSQLException e) {
                logger.error(e);
                Thread.sleep(connectTimeout);
            }
        }
        if (cleanDB) {
            try {
                connect.createStatement().execute("DROP DATABASE " + dataBase);
            } catch (SQLException e) {
                logger.error(ServerResourceBundle.getString("data.sql.error.creating.database"), e);
            }
        }

        try {
            // обязательно нужно создавать на основе template0, так как иначе у template1 может быть другая кодировка и ошибка
            connect.createStatement().execute("CREATE DATABASE " + dataBase + " WITH TEMPLATE template0 ENCODING='UTF8' ");
        } catch (SQLException e) {
            logger.info(ServerResourceBundle.getString("data.sql.error.creating.database"), e);
        }
        connect.close();
    }

    @Override
    protected void ensureSystemFuncs() throws IOException, SQLException {
        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/getAnyNotNull.sc")));
//        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/jumpWorkdays.sc")));
        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/completeBarcode.sc")));
        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/aggf.sc")));
        recursionString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/recursion.sc"));
        safeCastString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/safecast.sc"));
    }

    @Override
    protected String getPath() {
        return "/sql/postgres/";
    }

    @Override
    public String getLongType() {
        return "int8";
    }

    @Override
    public int getLongSQL() {
        return Types.BIGINT;
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

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        return DriverManager.getConnection("jdbc:postgresql://" + server + "/" + dataBase + "?user=" + userID + "&password=" + password);
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

    public String isNULL(String exprs, boolean notSafe) {
//        return "(CASE WHEN "+Expr1+" IS NULL THEN "+Expr2+" ELSE "+Expr1+" END)";
        return "COALESCE(" + exprs + ")";
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String having, String top) {
        return "SELECT " + exprs + " FROM " + from + BaseUtils.clause("WHERE", where) + BaseUtils.clause("GROUP BY", groupBy) + BaseUtils.clause("HAVING", having) + BaseUtils.clause("ORDER BY", orderBy) + BaseUtils.clause("LIMIT", top);
    }

    public String getUnionOrder(String union, String orderBy, String top) {
        return union + BaseUtils.clause("ORDER BY", orderBy) + BaseUtils.clause("LIMIT", top);
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
    public String getTypeChange(Type oldType, Type type, String name, ExecuteEnvironment env) {
        String newType = type.getDB(this, env);
        return "TYPE " + newType + " USING " + name + "::" + newType;
    }

    @Override
    public String getInsensitiveLike() {
        return "ILIKE";
    }

    public boolean supportGroupNumbers() {
        return true;
    }

    @Override
    public String backupDB(ExecutionContext context, String dumpFileName, List<String> excludeTables) throws IOException, InterruptedException {
        if (isRedundantString(dumpDir) || isRedundantString(binPath)) {
            context.delayUserInterfaction(new MessageClientAction(getString("logics.backup.path.not.specified"), getString("logics.backup.error")));
            return null;
        }

        String host, port;
        if (server.contains(":")) {
            host = server.substring(0, server.lastIndexOf(':'));
            port = server.substring(server.lastIndexOf(':') + 1);
        } else {
            host = server;
            port = "5432";
        }

        new File(dumpDir).mkdirs();

        String backupFilePath = new File(dumpDir, dumpFileName + ".backup").getPath();
        String backupLogFilePath = backupFilePath + ".log";

        CommandLine commandLine = new CommandLine(new File(binPath, "pg_dump"));
        commandLine.addArgument("-h");
        commandLine.addArgument(host);
        commandLine.addArgument("-p");
        commandLine.addArgument(port);
        commandLine.addArgument("-U");
        commandLine.addArgument(userID);

        for(String excludeTable : excludeTables) {
            commandLine.addArgument("-T");
            commandLine.addArgument(excludeTable.toLowerCase());
        }
        
        commandLine.addArgument("-F");
        commandLine.addArgument("custom");
        commandLine.addArgument("-b");
        commandLine.addArgument("-v");
        commandLine.addArgument("-f");
        commandLine.addArgument(backupFilePath);
        commandLine.addArgument(dataBase);


        FileOutputStream logStream = new FileOutputStream(backupLogFilePath);
        try {
            Map<String, String> env = new HashMap<String, String>(System.getenv());
            env.put("LC_MESSAGES", "en_EN");
            env.put("PGPASSWORD", password);

            Executor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(logStream));
            executor.setExitValue(0);

            int result;
            
            try {
                result = executor.execute(commandLine, env);
            } catch (ExecuteException e) {
                logger.error("Error while dumping the database : " + commandLine);
                throw e;
            } catch (IOException e) {
                logger.error("Error while dumping the database : " + commandLine);
                throw e;
            }

            if (result != 0) {
                throw new IOException("Error executing pg_dump - process returned code: " + result);
            }
        } finally {
            logStream.close();
        }

        return backupFilePath;
    }

    @Override
    public void killProcess(Integer processId) {
        if (isRedundantString(binPath)) {
            logger.error("Error while killing process : no bin path");
            return;
        }

        CommandLine commandLine = new CommandLine(new File(binPath, "pg_ctl"));
        commandLine.addArgument("kill");
        commandLine.addArgument("TERM");
        commandLine.addArgument(String.valueOf(processId));

        Executor executor = new DefaultExecutor();
        executor.setExitValue(0);

        try {
            int result = executor.execute(commandLine);
            if (result != 0) {
                logger.error("Error while killing process : " + result);
            }
        } catch (ExecuteException e) {
            logger.error("Error while killing process : " + commandLine);
        } catch (IOException e) {
            logger.error("Error while killing process : " + commandLine);
        }
    }


    @Override
    public String getCancelActiveTaskQuery(Integer pid) {
        return String.format("SELECT pg_cancel_backend(%s)", pid);
    }
    
    @Override
    public String getAnalyze() {
        return "VACUUM ANALYZE";
    }

    @Override
    public String getVacuumDB() {
        return "VACUUM FULL";
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
        return e.getSQLState().equals("40P01");
    }

    @Override
    public boolean isUpdateConflict(SQLException e) {
        return e.getSQLState().equals("40001");
    }

    @Override
    public boolean isUniqueViolation(SQLException e) {
        return e.getSQLState().equals("23505");
    }

    @Override
    public boolean isTimeout(SQLException e) {
        return e.getSQLState().equals("57014");
    }

    @Override
    protected void prepareConnection(Connection connection) {
        ((PGConnection)connection).setPrepareThreshold(2);
    }

    @Override
    public boolean hasJDBCTimeoutMultiThreadProblem() {
        return true;
    }

    @Override
    public boolean isTransactionCanceled(SQLException e) {
        return e.getSQLState().equals("25P02");
    }

    @Override
    public boolean isConnectionClosed(SQLException e) {
        String sqlState = e.getSQLState();
        return sqlState.equals("08003") || sqlState.equals("08006");
    }

    @Override
    public void setLogLevel(Connection connection, int level) {
        ensureLogLevel(level);
        ((BaseConnection)connection).getLogger().setLogLevel(level);
    }

    @Override
    public void ensureLogLevel(int logLevel) {
        if (logLevel != 0 && DriverManager.getLogWriter() == null)
        {
            DriverManager.setLogWriter(new PrintWriter(new Log4jWriter(ServerLoggers.jdbcLogger), false));
        }
        Driver.setLogLevel(logLevel);
    }

    @Override
    public boolean hasSelectivityProblem() {
        return true;
    }

    @Override
    public String getAdjustSelectivityPredicate() {
        return "current_timestamp<>current_timestamp";
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
    public String getArrayAgg(String s, ClassReader classReader, TypeEnvironment typeEnv) {
        return "AGGAR_SETADD(" + s + ")";
    }

    @Override
    public boolean orderTopTrouble() {
        return true;
    }

    @Override
    public void setACID(Statement statement, boolean acid) throws SQLException {
        statement.execute("SET SESSION synchronous_commit TO " + (acid ? "DEFAULT" : "OFF"));
        statement.execute("SET SESSION commit_delay TO " + (acid ? "DEFAULT" : "100000"));
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
        return (max?"MAX":"MIN") + "(" + expr1 + "," + expr2 + ")";
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
    public String getAnalyzeSessionTable(String tableName) {
        return "ANALYZE " + getSessionTableName(tableName);
    }

    @Override
    public boolean supportsVolatileStats() {
        return true;
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
    protected void proceedEnsureConcType(ConcatenateType concType) throws SQLException {
        // ensuring types
        String declare = "";
        ImList<Type> types = concType.getTypes();
        for (int i=0,size=types.size();i<size;i++)
            declare = (declare.length() ==0 ? "" : declare + ",") + ConcatenateType.getFieldName(i) + " " + types.get(i).getDB(this, recTypes);

        String typeName = getConcTypeName(concType);
        executeEnsure("CREATE TYPE " + typeName + " AS (" + declare + ")");

        // создаем cast'ы всем concatenate типам
        for(int i=0,size=ensuredConcTypes.size();i<size;i++) {
            ConcatenateType ensuredType = ensuredConcTypes.getKey(i);
            if(concType.getCompatible(ensuredType)!=null) {
                String ensuredName = getConcTypeName(ensuredType);
                executeEnsure("DROP CAST IF EXISTS (" + typeName + " AS " + ensuredName + ")");
                executeEnsure("CREATE CAST (" + typeName + " AS " + ensuredName + ") WITH INOUT AS IMPLICIT"); // в обе стороны так как containsAll в DataClass по прежнему не направленный
                executeEnsure("DROP CAST IF EXISTS (" + ensuredName + " AS " + typeName + ")");
                executeEnsure("CREATE CAST (" + ensuredName + " AS " + typeName + ") WITH INOUT AS IMPLICIT");
            }
        }
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
    public String getRecursion(ImList<FunctionType> types, String recName, String initialSelect, String stepSelect, String fieldDeclare, String outerParams, TypeEnvironment typeEnv) {
        assert types.size() == types.filterList(new SFunctionSet<FunctionType>() {
            public boolean contains(FunctionType element) {
                return element instanceof Type;
            }}).size();
        
        typeEnv.addNeedRecursion(types);
        String recursionName = genRecursionName(BaseUtils.<ImList<Type>>immutableCast(types));
        return recursionName + "('" + recName +"'," +
                "'(" + StringEscapeUtils.escapeSql(initialSelect)+")','(" +StringEscapeUtils.escapeSql(stepSelect)+")'"+(outerParams.length() == 0 ? "" : "," + outerParams)+") recursion ("
                + fieldDeclare + ")";
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
        return true;
    }

    public String getArrayType(ArrayClass arrayClass, TypeEnvironment typeEnv) {
        return arrayClass.getArrayType().getDB(this, typeEnv) + "[]";
    }

    protected String recursionString;

    public static String genRecursionName(ImList<Type> types) {
        return "recursion_" + genTypePostfix(types);
    }

    public void ensureArrayClass(ArrayClass arrayClass) {
    }

    private LRUSVSMap<Object, Boolean> ensuredRecursion = new LRUSVSMap<Object, Boolean>(LRUUtil.G2);

    @Override
    public synchronized void ensureRecursion(Object object) throws SQLException {

        Boolean ensured = ensuredRecursion.get(object);
        if(ensured != null)
            return;

        ImList<Type> types = (ImList<Type>)object;

        String declare = "";
        String using = "";
        for (int i=0,size=types.size();i<size;i++) {
            String paramName = "p" + i;
            Type type = types.get(i);
            declare = declare + ", " + paramName + " " + type.getDB(this, recTypes);
            using = (using.length() == 0 ? "USING " : using + ",") + paramName;
        }

        Properties properties = new Properties();
        properties.put("function.name", genRecursionName(types));
        properties.put("params.declare", declare);
        properties.put("params.usage", using);

        executeEnsure(stringResolver.replacePlaceholders(recursionString, properties));

        ensuredRecursion.put(object, true);
    }

    @Override
    public boolean hasAggConcProblem() {
        return true;
    }

    @Override
    public boolean hasNotNullIndexProblem() {
        return true;
    }
}
