package lsfusion.server.data.sql.adapter;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLConsumer;
import lsfusion.server.data.sql.syntax.PostgreSQLSyntax;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.ArrayClass;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.table.DumbTable;
import lsfusion.server.physics.exec.db.table.StructTable;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.postgresql.replication.LogSequenceNumber;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static lsfusion.base.BaseUtils.isRedundantString;
import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;
import static lsfusion.server.physics.admin.log.ServerLoggers.startLogError;


public class PostgreDataAdapter extends DataAdapter {

    private String defaultBinPath;
    private String defaultDumpDir;
    private String binPath;
    private String dumpDir;
    protected static final String DB_NAME = "postgres";

    protected static final String DB_SUBSRIPTION = "lsfusion_sub";

    private int dbMajorVersion;

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
        super(PostgreSQLSyntax.instance, dataBase, server, userID, password, connectTimeout, cleanDB);

        this.defaultBinPath = binPath;
        this.defaultDumpDir = dumpDir;
        this.binPath = binPath;
        this.dumpDir = dumpDir;
    }

    public void setBinPath(String binPath) {
        this.binPath = binPath != null ? binPath : defaultBinPath;
    }

    public void setDumpDir(String dumpDir) {
        this.dumpDir = dumpDir != null ? dumpDir : defaultDumpDir;
    }

    private Connection getConnection(String server, String dataBase) throws SQLException {
        Connection connection;
        try {
            connection = getConnection(server, dataBase, false); //  + "&loggerLevel=TRACE&loggerFile=pgjdbc.log"
        } catch (PSQLException e) {
            String sqlState = e.getSQLState();
            if (sqlState != null && sqlState.equals("3D000")) //3D000 database from properties does not exist
                connection = getConnection(server, DB_NAME); // try to connect to default db
            else
                throw e;
        }

        return connection;
    }

    private Connection getConnection(String server, String dataBase, boolean useConnectTimeout) throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://" + server + "/" + dataBase.toLowerCase() + "?user=" + user + "&password=" + password + (useConnectTimeout ? "&connectTimeout=" + (int) (connectTimeout / 1000) : ""));
    }
    private String getLibpqConnectionString(String server, String dataBase) {
        String host = server;
        String port = null;
        if (server.contains(":")) {
            String[] parts = server.split(":", 2);
            host = parts[0];
            port = parts[1];
        }

        return "host=" + host + (port != null ? " port=" + port : "") + " dbname=" + dataBase.toLowerCase() + " user=" + user + " password=" + password;
    }

    public void ensureDB(String server, boolean cleanDB, boolean master) throws Exception {

        Connection connect = null;
        while(connect == null) {
            try {
                connect = getConnection(server, dataBase);
                dbMajorVersion = connect.getMetaData().getDatabaseMajorVersion();
            } catch (PSQLException e) {
                startLogError(String.format("%s (host: %s, user: %s)", e.getMessage(), server, user));
                logger.error("EnsureDB error: ", e);
                //08001 = connection refused (database is not started), 57P03 = the database system is starting up
                String sqlState = e.getSQLState();
                if (sqlState != null && (sqlState.equals("08001") || sqlState.equals("57P03"))) {
                    Thread.sleep(connectTimeout);
                } else throw e;
            }
        }
        if (cleanDB)
            executeEnsure(connect, "DROP DATABASE " + dataBase);

        // обязательно нужно создавать на основе template0, так как иначе у template1 может быть другая кодировка и ошибка
        executeEnsure(connect, "CREATE DATABASE " + dataBase + " WITH TEMPLATE template0 ENCODING='UTF8' ");

        executeEnsure(connect, "ALTER DATABASE " + dataBase + " SET TIMEZONE='" + TimeZone.getDefault().getID() + "'");

        connect.close();
    }

    public void runSync(LogSequenceNumber masterLSN, SQLConsumer<Integer> run) throws SQLException, SQLHandledException {

        // if doing async start / stop subscription should be done, but then we need to check / forbid next db structure change (so won't happen slave -> z (skip y), master -> y -> some changes -> z)

        int serversCount = getServersCount();

        for(int server = serversCount - 1; server >= 0; server--) {
            Connection ensureConnection = ensureConnections[server];
            if(server != 0) { // // waiting slave to reach master if there is a subscription
                while(true) {
                    LogSequenceNumber connectionLSN = readSlaveLSN(ensureConnection);
                    if(connectionLSN != LogSequenceNumber.INVALID_LSN) { // first copy / or not yet started wal sync
                        if (connectionLSN == DataAdapter.NO_SUBSCRIPTION) {
                            executeEnsure(ensureConnections[0], "SELECT pg_drop_replication_slot('" + getSlotName(server) + "');\n");
                            break;
                        }
                        if(connectionLSN.compareTo(masterLSN) >= 0 && readSlaveReady(ensureConnection))
                            break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw Throwables.propagate(e);
                    }
                }
            }

            run.accept(server);
        }

        if(serversCount > 1) {
            // checking publications / subscriptions
            for (int server = 0; server < serversCount; server++) {
                Connection ensureConnection = ensureConnections[server];

                if (server == 0) {
                    executeEnsure(ensureConnection, "CREATE PUBLICATION " + DB_SUBSRIPTION + "\n" +
                            "  FOR ALL TABLES\n" +
                            "  WITH (\n" +
                            "    publish = 'insert, update, delete, truncate'\n" +
                            "  );");
                } else {
                    try {
                        executeEnsureWithException(ensureConnection, "CREATE SUBSCRIPTION " + DB_SUBSRIPTION + "\n" +
                                "                CONNECTION '" + getLibpqConnectionString(this.server, dataBase) + "'\n" +
                                "                PUBLICATION " + DB_SUBSRIPTION + " WITH (slot_name = '" + getSlotName(server) + "', enabled = false);");
                        // we can't do it in transaction, but we just hope that workers won't be fa
                        // we need to truncate struct and dumb tables not to get unique violation
                        ensureConnection.setAutoCommit(false);
                        try {
                            executeEnsureWithException(ensureConnection, "TRUNCATE TABLE " + StructTable.instance.getName(syntax) + ";\n");
                            executeEnsureWithException(ensureConnection, "TRUNCATE TABLE " + DumbTable.instance.getName(syntax) + ";\n");
                            executeEnsureWithException(ensureConnection, "ALTER SUBSCRIPTION " + DB_SUBSRIPTION + " ENABLE\n;");
                        } finally {
                            ensureConnection.setAutoCommit(true);
                        }
                        while(!readSlaveReady(ensureConnection)) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                throw Throwables.propagate(e);
                            }
                        }
                    } catch (SQLException e) {
                        if (PSQLState.PROTOCOL_VIOLATION.getState().equals(e.getSQLState()) && e.getMessage().contains("wal_level >= logical")) {
                            Connection masterEnsureConnection = ensureConnections[0];
                            executeEnsure(masterEnsureConnection, "ALTER SYSTEM SET wal_level = logical;");
                            executeEnsure(masterEnsureConnection, "ALTER SYSTEM SET max_replication_slots = 30;"); // the problem that default is 10 and the max_sync_workers_per_subscription is 10 (and there are), so we'll inevitably get errors with default 10 on the first copy_data
                            ServerLoggers.startLog("Publisher wal_level is < logical. ALTER SYSTEM SET wal_level = logical has already been applied; "
                                    + "please restart the database server and only after that you can use the slave.");
                        } else {
                            ServerLoggers.sqlSuppLog(e);
                        }
                    }
                    // we're assuming that copy_data is done once, so after that slave is ready
                    // however after disable / enable while also can be needed (?)
                    slavesReady.put(servers[server], true);
                }
            }
        }
    }

    private static String getSlotName(int server) {
        return DB_SUBSRIPTION + server;
    }

    public double getLoad(Connection connection) {
        return Math.random();
        // for master add coeff, for slave including lag
    }

    public LogSequenceNumber getMasterLSN(Connection connection) throws SQLException {
        if(!isFromServer(connection, server)) // if not master
            return null;

        String sql = "SELECT pg_current_wal_lsn() AS last_apply_lsn";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return LogSequenceNumber.valueOf(rs.getString("last_apply_lsn"));
            } else {
                return LogSequenceNumber.INVALID_LSN;
            }
        }
    }

    private final ConcurrentHashMap<String, Boolean> slavesReady = MapFact.getGlobalConcurrentHashMap();

    public LogSequenceNumber getSlaveLSN(Connection connection) throws SQLException {
        if(isFromServer(connection, server)) // if master
            return null;

        // there is a subscription but not yet ready
        Boolean slaveReady = slavesReady.get(getServer(connection));
        if(slaveReady == null || !slaveReady)
            return LogSequenceNumber.INVALID_LSN;

        return readSlaveLSN(connection);
    }

    public LogSequenceNumber readSlaveLSN(Connection connection) throws SQLException {
        String sql =
                "SELECT latest_end_lsn " +
                        "FROM pg_stat_subscription " +
                        "WHERE subname = '" + DB_SUBSRIPTION + "'";
        try (Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return LogSequenceNumber.valueOf(BaseUtils.nvl(rs.getString("latest_end_lsn"),"0"));
            } else {
                return DataAdapter.NO_SUBSCRIPTION;
            }
        }
    }

    public boolean readSlaveReady(Connection connection) throws SQLException {
        if(isFromServer(connection, server)) // if master
            return true;

        String sql =
                "SELECT bool_and(srsubstate IN ('r','s')) AS all_tables_synced" +
                        " FROM pg_subscription_rel" +
                        " WHERE srsubid = (SELECT oid FROM pg_subscription WHERE subname = '" + DB_SUBSRIPTION + "');";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getBoolean("all_tables_synced");
            } else {
                return true;
            }
        }
    }

    public int getDbMajorVersion() {
        return dbMajorVersion;
    }

    @Override
    public void ensureSqlFuncs() throws IOException, SQLException {
        super.ensureSqlFuncs();

        recursionString = readResource("/sql/postgres/recursion.tsql");
        safeCastString = readResource("/sql/postgres/safecast.tsql");
        safeCastIntString = readResource("/sql/postgres/safecastint.tsql");
        safeCastStrString = readResource("/sql/postgres/safecaststr.tsql");
    }

    @Override
    public void ensureLogLevel() {
        LogManager.getLogManager().getLogger("org.postgresql.Driver").setLevel(Level.OFF);
    }

    @Override
    protected String getPath() {
        return "/sql/postgres/";
    }

    public Connection createConnection(String server) throws SQLException {
        long started = System.currentTimeMillis();
        try {
            return getConnection(server, dataBase, true);
        } finally {
            long elapsed = System.currentTimeMillis() - started;
            if(elapsed > connectTimeout)
                logger.error("Too long getConnection : timeout - " + connectTimeout + ", actual time - " + elapsed);
        }
    }

    @Override
    public boolean checkBackupParams(ExecutionContext context) {
        if (!dirExists(dumpDir, true)) {
            context.messageError(localize("{backup.dump.directory.path.not.specified}"));
            return false;
        }
        return true;
    }

    @Override
    public String getBackupFilePath(String dumpFileName) {
        return new File(dumpDir, dumpFileName + ".backup").getPath();
    }

    @Override
    public String getBackupFileLogPath(String dumpFileName) {
        return getBackupFilePath(dumpFileName) + ".log";
    }

    protected String getMaintenanceServer() {
        return server;
    }

    @Override
    public void backupDB(ExecutionContext context, String dumpFileName, int threadCount, List<String> excludeTables) throws IOException {
        String host, port;
        String server = getMaintenanceServer();
        if (server.contains(":")) {
            host = server.substring(0, server.lastIndexOf(':'));
            port = server.substring(server.lastIndexOf(':') + 1);
        } else {
            host = server;
            port = "5432";
        }

        String backupFilePath = getBackupFilePath(dumpFileName);
        String backupLogFilePath = getBackupFileLogPath(dumpFileName);

        CommandLine commandLine = getBinPathCommandLine("pg_dump");
        commandLine.addArgument("-h");
        commandLine.addArgument(host);
        commandLine.addArgument("-p");
        commandLine.addArgument(port);
        commandLine.addArgument("-U");
        commandLine.addArgument(user);

        for(String excludeTable : excludeTables) {
            commandLine.addArgument("--exclude-table-data="+excludeTable.toLowerCase());
        }
        
        commandLine.addArgument("-F");
        if(threadCount > 1) {
            commandLine.addArgument("directory");
            commandLine.addArgument("-j");
            commandLine.addArgument(String.valueOf(threadCount));
        } else {
            commandLine.addArgument("custom");
        }
        commandLine.addArgument("-b");
        commandLine.addArgument("-v");
        commandLine.addArgument("-f");
        commandLine.addArgument(backupFilePath);
        commandLine.addArgument(dataBase);


        try (FileOutputStream logStream = new FileOutputStream(backupLogFilePath)) {
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("LC_MESSAGES", "en_EN");
            env.put("PGPASSWORD", password);

            Executor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(logStream));
            executor.setExitValue(0);

            int result;

            try {
                result = executor.execute(commandLine, env);
            } catch (IOException e) {
                logger.error("Error while dumping the database : " + commandLine);
                throw e;
            }

            if (result != 0) {
                throw new IOException("Error executing pg_dump - process returned code: " + result);
            }
        }
    }

    private boolean dirExists(String dir, boolean mkdirs) {
        if (isRedundantString(dir))
            return false;
        File f = new File(dir);
        return f.exists() || (mkdirs && f.mkdirs());
    }

    @Override
    public String customRestoreDB(String fileBackup, Set<String> tables, boolean isMultithread) throws IOException {

        String tempDB = "db-temp" + Calendar.getInstance().getTime().getTime();
        String host, port;
        String server = getMaintenanceServer();
        if (server.contains(":")) {
            host = server.substring(0, server.lastIndexOf(':'));
            port = server.substring(server.lastIndexOf(':') + 1);
        } else {
            host = server;
            port = "5432";
        }

        if(createDB(host, port, tempDB)) {
            CommandLine commandLine = null;
            try {
                commandLine = getBinPathCommandLine("pg_restore");
                commandLine.addArgument("--verbose");
                commandLine.addArgument("--host");
                commandLine.addArgument(host);
                commandLine.addArgument("--port");
                commandLine.addArgument(port);
                commandLine.addArgument("--username");
                commandLine.addArgument(user);
                for (String table : tables) {
                    commandLine.addArgument("--table");
                    commandLine.addArgument(table.toLowerCase());
                }
                commandLine.addArgument("--dbname");
                commandLine.addArgument(tempDB);
                commandLine.addArgument(fileBackup);
                if(isMultithread) {
                    commandLine.addArgument("--format=d");
                }

                Map<String, String> env = new HashMap<>(System.getenv());
                env.put("PGPASSWORD", password);

                Executor executor = new DefaultExecutor();
                executor.setExitValue(0);

                executor.execute(commandLine, env);
                return tempDB;
            } catch (IOException e) {
                logger.error("Error while restoring the database : " + commandLine, e);
                return tempDB;
            }
        } else return null;
    }

    public boolean createDB(String host, String port, String dbName) throws IOException {
        CommandLine commandLine = getBinPathCommandLine("createdb");
        commandLine.addArgument("--host");
        commandLine.addArgument(host);
        commandLine.addArgument("--port");
        commandLine.addArgument(port);
        commandLine.addArgument("--username");
        commandLine.addArgument(user);
        commandLine.addArgument(dbName);
        Executor executor = new DefaultExecutor();
        //executor.setExitValue(0);
        try {
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("PGPASSWORD", password);

            executor.execute(commandLine, env);
            return true;
        } catch (IOException e) {
            logger.error("Error while creating temp database : " + commandLine);
            throw e;
        }
    }

    public void dropDB(String dbName) throws IOException {
        String host, port;
        String server = getMaintenanceServer();
        if (server.contains(":")) {
            host = server.substring(0, server.lastIndexOf(':'));
            port = server.substring(server.lastIndexOf(':') + 1);
        } else {
            host = server;
            port = "5432";
        }

        CommandLine commandLine = getBinPathCommandLine("dropdb");
        commandLine.addArgument("--host");
        commandLine.addArgument(host);
        commandLine.addArgument("--port");
        commandLine.addArgument(port);
        commandLine.addArgument("--username");
        commandLine.addArgument(user);
        commandLine.addArgument("--if-exists");
        commandLine.addArgument(dbName);
        Executor executor = new DefaultExecutor();
        try {
            executor.execute(commandLine);
        } catch (IOException e) {
            logger.error("Error while creating temp database : " + commandLine);
            throw e;
        }
    }

    @Override
    public List<List<List<Object>>> readCustomRestoredColumns(String dbName, String table, List<String> keys, List<String> columns) throws SQLException {
        List<List<Object>> dataKeys = new ArrayList<>();
        List<List<Object>> dataColumns = new ArrayList<>();
        try(Connection connection = getConnection(getMaintenanceServer(), dbName.toLowerCase(), false)) {
            try (Statement statement = connection.createStatement()) {
                String column = "";
                for(String k : keys)
                    column += k + ",";
                for (String c : columns)
                    column += c + ",";
                column = column.isEmpty() ? "*" : column.substring(0, column.length() - 1);
                ResultSet result = statement.executeQuery(String.format("SELECT %s FROM %s", column, table));
                while (result.next()) {
                    List<Object> rowKeys = new ArrayList<>();
                    List<Object> rowColumns = new ArrayList<>();
                    for (int i = 1; i <= keys.size(); i++) {
                        rowKeys.add(result.getObject(i));
                    }
                    for (int i = keys.size() + 1; i <= keys.size() + columns.size(); i++)
                        rowColumns.add(result.getObject(i));
                    dataKeys.add(rowKeys);
                    dataColumns.add(rowColumns);
                }
            }
        }
        return Arrays.asList(dataKeys, dataColumns);
    }

    @Override
    public void killProcess(Integer processId) {
        CommandLine commandLine = getBinPathCommandLine("pg_ctl");
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
        } catch (IOException e) {
            logger.error("Error while killing process : " + commandLine);
        }
    }

    private CommandLine getBinPathCommandLine(String command) {
       return dirExists(binPath, false) ? new CommandLine(new File(binPath, command)) : new CommandLine(command);
    }

    @Override
    protected void proceedEnsureConcType(ConcatenateType concType) throws SQLException {
        // ensuring types
        String declare = "";
        ImList<Type> types = concType.getTypes();
        for (int i=0,size=types.size();i<size;i++)
            declare = (declare.length() ==0 ? "" : declare + ",") + ConcatenateType.getFieldName(i) + " " + types.get(i).getDB(syntax, recTypes);

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

    private String recursionString;

    public void ensureArrayClass(ArrayClass arrayClass) {
    }

    private LRUSVSMap<Object, Boolean> ensuredRecursion = new LRUSVSMap<>(LRUUtil.G2);

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
            declare = declare + ", " + paramName + " " + type.getDB(syntax, recTypes);
            using = (using.length() == 0 ? "USING " : using + ",") + paramName;
        }

        Properties properties = new Properties();
        properties.put("function.name", PostgreSQLSyntax.genRecursionName(types));
        properties.put("params.declare", declare);
        properties.put("params.usage", using);

        executeEnsure(stringResolver.replacePlaceholders(recursionString, properties));

        ensuredRecursion.put(object, true);
    }

    @Override
    public String getDBName() {
        return DB_NAME;
    }
}
