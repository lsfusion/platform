package lsfusion.server.data.sql.adapter;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.file.IOUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.syntax.PostgreSQLSyntax;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.ArrayClass;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.postgresql.util.PSQLException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static lsfusion.base.BaseUtils.isRedundantString;


public class PostgreDataAdapter extends DataAdapter {

    private String defaultBinPath;
    private String defaultDumpDir;
    private String binPath;
    private String dumpDir;

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
        super(PostgreSQLSyntax.instance, dataBase, server, null, userID, password, connectTimeout, cleanDB);

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

    public void ensureDB(boolean cleanDB) throws Exception {

        Connection connect = null;
        while(connect == null) {
            try {
                connect = DriverManager.getConnection("jdbc:postgresql://" + server + "/postgres?user=" + userID + "&password=" + password);
            } catch (PSQLException e) {
                ServerLoggers.startLogger.error(String.format("%s (host: %s, user: %s)", e.getMessage(), server, userID));
                logger.error("EnsureDB error: ", e);
                //08001 = connection refused (database is not started), 57P03 = the database system is starting up
                if (e.getSQLState() != null && (e.getSQLState().equals("08001") || e.getSQLState().equals("57P03"))) {
                    Thread.sleep(connectTimeout);
                } else throw e;
            }
        }
        if (cleanDB) {
            try {
                connect.createStatement().execute("DROP DATABASE " + dataBase);
            } catch (SQLException e) {
                ServerLoggers.sqlSuppLog(e);
            }
        }

        try {
            // обязательно нужно создавать на основе template0, так как иначе у template1 может быть другая кодировка и ошибка
            connect.createStatement().execute("CREATE DATABASE " + dataBase + " WITH TEMPLATE template0 ENCODING='UTF8' ");
        } catch (SQLException e) {
            ServerLoggers.sqlSuppLog(e);
        }

        try {
            connect.createStatement().execute("ALTER DATABASE " + dataBase + " SET TIMEZONE='" + TimeZone.getDefault().getID() + "'");
        } catch (SQLException e) {
            ServerLoggers.sqlSuppLog(e);
        }

        connect.close();
    }

    @Override
    protected void ensureSystemFuncs() throws IOException, SQLException {
        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/getAnyNotNull.sql")));
//        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/jumpWorkdays.sql")));
        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/completeBarcode.sql")));
        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/aggf.sql")));
        recursionString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/recursion.sql"));
        safeCastString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/safecast.sql"));
        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/postgres/cast.sql")));
    }

    @Override
    public void ensureLogLevel() {
        LogManager.getLogManager().getLogger("org.postgresql.Driver").setLevel(Level.OFF);
    }

    @Override
    protected String getPath() {
        return "/sql/postgres/";
    }

    public Connection startConnection() throws SQLException {
        long started = System.currentTimeMillis();
        try {
            return DriverManager.getConnection("jdbc:postgresql://" + server + "/" + dataBase.toLowerCase() + "?user=" + userID + "&password=" + password + "&connectTimeout=" + (int) (connectTimeout / 1000));
        } finally {
            long elapsed = System.currentTimeMillis() - started;
            if(elapsed > connectTimeout)
                logger.error("Too long getConnection : timeout - " + connectTimeout + ", actual time - " + elapsed);
        }
    }

    @Override
    public String getBackupFilePath(String dumpFileName) {
        return isRedundantString(dumpDir) ? null : new File(dumpDir, dumpFileName + ".backup").getPath();
    }

    @Override
    public String backupDB(ExecutionContext context, String dumpFileName, int threadCount, List<String> excludeTables) throws IOException {
        if (isRedundantString(dumpDir) || isRedundantString(binPath)) {
            context.delayUserInterfaction(new MessageClientAction(ThreadLocalContext.localize("{logics.backup.path.not.specified}"), ThreadLocalContext.localize("{logics.backup.error}")));
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

        return backupFilePath;
    }

    @Override
    public String customRestoreDB(String fileBackup, Set<String> tables) throws IOException {

        String tempDB = "db-temp" + Calendar.getInstance().getTime().getTime();
        String host, port;
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
                commandLine = new CommandLine(new File(binPath, "pg_restore"));
                commandLine.addArgument("--verbose");
                commandLine.addArgument("--host");
                commandLine.addArgument(host);
                commandLine.addArgument("--port");
                commandLine.addArgument(port);
                commandLine.addArgument("--username");
                commandLine.addArgument(userID);
                for (String table : tables) {
                    commandLine.addArgument("--table");
                    commandLine.addArgument(table.toLowerCase());
                }
                commandLine.addArgument("--dbname");
                commandLine.addArgument(tempDB);
                commandLine.addArgument(fileBackup);
                Executor executor = new DefaultExecutor();
                executor.setExitValue(0);

                executor.execute(commandLine);
                return tempDB;
            } catch (IOException e) {
                logger.error("Error while restoring the database : " + commandLine);
                return tempDB;
            }
        } else return null;
    }

    public boolean createDB(String host, String port, String dbName) throws IOException {
        CommandLine commandLine = new CommandLine(new File(binPath, "createdb"));
        commandLine.addArgument("--host");
        commandLine.addArgument(host);
        commandLine.addArgument("--port");
        commandLine.addArgument(port);
        commandLine.addArgument("--username");
        commandLine.addArgument(userID);
        commandLine.addArgument(dbName);
        Executor executor = new DefaultExecutor();
        //executor.setExitValue(0);
        try {
            executor.execute(commandLine);
            return true;
        } catch (IOException e) {
            logger.error("Error while creating temp database : " + commandLine);
            throw e;
        }
    }

    public void dropDB(String dbName) throws IOException {
        String host, port;
        if (server.contains(":")) {
            host = server.substring(0, server.lastIndexOf(':'));
            port = server.substring(server.lastIndexOf(':') + 1);
        } else {
            host = server;
            port = "5432";
        }

        CommandLine commandLine = new CommandLine(new File(binPath, "dropdb"));
        commandLine.addArgument("--host");
        commandLine.addArgument(host);
        commandLine.addArgument("--port");
        commandLine.addArgument(port);
        commandLine.addArgument("--username");
        commandLine.addArgument(userID);
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
        try(Connection connection = DriverManager.getConnection("jdbc:postgresql://" + server + "/" + dbName.toLowerCase() + "?user=" + userID + "&password=" + password)) {
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
                        Object key = result.getObject(i);
                        //TODO: убрать, когда все базы перейдут на LONG, до этого времени нельзя восстанавливать колонки с ключом INTEGER
                        rowKeys.add(key instanceof Integer ? new Long((Integer) result.getObject(i)) : key);
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
        } catch (IOException e) {
            logger.error("Error while killing process : " + commandLine);
        }
    }


    @Override
    protected void prepareConnection(Connection connection) {
//        ((PGConnection)connection).setPrepareThreshold(2);
        //((PGConnection)connection).setAutosave(AutoSave.NEVER); // enabled by default AutoSave used for fixing cached plan, however we can restart transaction by ourself so we do not need this overhead
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

}
