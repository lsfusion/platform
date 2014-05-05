package lsfusion.server.data.sql;

import lsfusion.base.BaseUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.Log4jWriter;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.property.ExecutionContext;
import org.apache.commons.exec.*;
import org.postgresql.Driver;
import org.postgresql.PGConnection;
import org.postgresql.core.BaseConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        this(dataBase, server, userID, password, null, null, cleanDB);
    }

    public PostgreDataAdapter(String dataBase, String server, String userID, String password, String binPath, String dumpDir) throws Exception {
        this(dataBase, server, userID, password, binPath, dumpDir, false);
    }

    public PostgreDataAdapter(String dataBase, String server, String userID, String password, String binPath, String dumpDir, boolean cleanDB) throws Exception {
        super(dataBase, server, userID, password, cleanDB);

        this.binPath = binPath;
        this.dumpDir = dumpDir;
    }

    public void ensureDB(boolean cleanDB) throws Exception, SQLException, InstantiationException, IllegalAccessException {
        ensureLogLevel(1);

        Connection connect = DriverManager.getConnection("jdbc:postgresql://" + server + "/postgres?user=" + userID + "&password=" + password);
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
        return (descending ? "DESC" : "ASC") + (!notNull ? " NULLS " + (descending ? "LAST" : "FIRST") : "");
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
    public String typeConvertSuffix(Type oldType, Type newType, String name, TypeEnvironment typeEnv) {
        return "USING " + name + "::" + newType.getDB(this, typeEnv);
    }

    @Override
    public String getInsensitiveLike() {
        return "ILIKE";
    }

    public boolean supportGroupNumbers() {
        return true;
    }

    public boolean orderTopTrouble() {
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
}
