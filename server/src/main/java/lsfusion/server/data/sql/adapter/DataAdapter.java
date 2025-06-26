package lsfusion.server.data.sql.adapter;

import lsfusion.base.Pair;
import lsfusion.base.lambda.EConsumer;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.base.ResourceUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.file.IOUtils;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.sql.connection.AbstractConnectionPool;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLConsumer;
import lsfusion.server.data.sql.syntax.DefaultSQLSyntax;
import lsfusion.server.data.sql.syntax.PostgreSQLSyntax;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeFunc;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.exec.TypePool;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.ArrayClass;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;
import org.postgresql.replication.LogSequenceNumber;
import org.springframework.util.PropertyPlaceholderHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class DataAdapter extends AbstractConnectionPool implements TypePool {
    public final static SQLSyntax debugSyntax = PostgreSQLSyntax.instance;
    protected final static Logger logger = ServerLoggers.sqlLogger;
    public final SQLSyntax syntax;

    public String server; // master
    public String[] servers; // first is master, others - slaves

    public String dataBase;
    public String user;
    public String password;
    public Long connectTimeout;

    protected abstract void ensureDB(String server, boolean cleanDB, boolean master) throws Exception;

    protected DataAdapter(SQLSyntax syntax, String dataBase, String server, String user, String password, Long connectTimeout, boolean cleanDB) throws Exception {

        Class.forName(syntax.getClassName());

        this.syntax = syntax;

        servers = server.split(";");
        this.server = servers[0];

        this.user = user;
        this.password = password;

        this.dataBase = dataBase;

        this.connectTimeout = connectTimeout;
    }

    public LogSequenceNumber ensureDBConnection(boolean cleanDB) throws Exception {
        ensureConnections = new Connection[servers.length];
        String[] servers = this.servers;
        for (int i = 0; i < servers.length; i++) {
            String server = servers[i];
            ensureDB(server, cleanDB, i == 0);

            Connection connection = startConnection(server);
            connection.setAutoCommit(true);
            ensureConnections[i] = connection;
        }
        return getMasterLSN(ensureConnections[0]);
    }

    protected boolean checkLSN(Connection connection, LogSequenceNumber lsn) throws SQLException {
        LogSequenceNumber connectionLSN = getSlaveLSN(connection);

        if(connectionLSN == null) // master
            return true;

        if(connectionLSN == LogSequenceNumber.INVALID_LSN) // first copy / or not yet started wal sync
            return false;

        if(connectionLSN == DataAdapter.NO_SUBSCRIPTION)
            return false;

        assert lsn != null;
        return connectionLSN.compareTo(lsn) >= 0;
    }

    public int getServersCount() {
        return servers.length;
    }

    public abstract void runSync(LogSequenceNumber masterLSN, SQLConsumer<Integer> run) throws SQLException, SQLHandledException;

    // getting least loaded server
    private String getServer(Integer needServer, LogSequenceNumber lsn) throws SQLException {
        if(servers.length == 1)
            return server;

        if(needServer != null)
            return servers[needServer];

        String bestServer = null;
        double bestLoad = 0;
        for (int i = 0; i < ensureConnections.length; i++) {
            Connection ensureConnection = ensureConnections[i];
            if (checkLSN(ensureConnection, lsn)) {
                double load = getLoad(ensureConnection);
                if (bestServer == null || load <= bestLoad) {
                    bestServer = servers[i];
                    bestLoad = load;
                }
            }
        }

        return bestServer;
    }

    private final List<Connection> poolConnections = new ArrayList<>();

    @Override
    public Connection startConnection(Integer needServer, LogSequenceNumber lsn, Connection prevConnection) throws SQLException {
        String server = getServer(needServer, lsn); // get less loaded server

        if(prevConnection != null && isFromServer(prevConnection, server))
            return null;

        synchronized (poolConnections) {
            for (Connection poolConnection : poolConnections)
                if (isFromServer(poolConnection, server)) {
                    poolConnections.remove(poolConnection);
                    return poolConnection;
                }
        }

        return startConnection(server);
    }

    @Override
    public void stopConnection(Connection connection, EConsumer<Connection, SQLException> cleaner) throws SQLException {
        boolean canBePooled;
        synchronized (poolConnections) {
            canBePooled = poolConnections.size() < Settings.get().getFreeConnections();
        }
        if(canBePooled && cleaner != null && !connection.isClosed()) {
            cleaner.accept(connection);
            synchronized (poolConnections) {
                poolConnections.add(connection);
            }
        } else
            connection.close();
    }

    public static boolean isFromServer(Connection prevConnection, String server) {
        return getServer(prevConnection).equals(server);
    }

    protected static String getServer(Connection prevConnection) {
        return connectionInfo.get(prevConnection);
    }

    private final static Map<Connection, String> connectionInfo = Collections.synchronizedMap(new WeakHashMap<>());
    protected Connection startConnection(String server) throws SQLException {
        Connection connection = createConnection(server);
        connectionInfo.put(connection, server);
        return connection;
    }
    protected abstract Connection createConnection(String server) throws SQLException;

    public static List<String> getAllDBNames() {
        return new ArrayList<>(Arrays.asList(PostgreDataAdapter.DB_NAME, MySQLDataAdapter.DB_NAME));
    }

    private List<String> findSQLScripts() {
        List<String> allDBNames = getAllDBNames();
        allDBNames.remove(getDBName());
        return ResourceUtils.getResources(Pattern.compile("/sql/.*")).stream().filter(resource -> {
            if (resource.contains(".tsql"))
                return false;
            for (String key : allDBNames) {
                if (resource.contains("/" + key + "/"))
                    return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    public abstract String getDBName();

    public void ensureSqlFuncs() throws IOException, SQLException {
        executeEnsure(findSQLScripts());
    }

    public void ensureLogLevel() {
    }

    public boolean checkBackupParams(ExecutionContext context) {
        return false;
    }

    public String getBackupFilePath(String dumpFileName) {
        return null;
    }

    public String getBackupFileLogPath(String dumpFileName) {
        return null;
    }

    public void backupDB(ExecutionContext context, String dumpFileName, int threadCount, List<String> excludeTables) throws IOException {
    }

    public String customRestoreDB(String fileBackup, Set<String> tables, boolean isMultithread) throws IOException {
        return null;
    }

    public void dropDB(String dbName) throws IOException {
    }

    public List<List<List<Object>>> readCustomRestoredColumns(String dbName, String table, List<String> keys, List<String> columns) throws SQLException {
        return null;
    }

    public void killProcess(Integer processId) {
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
            ensureTableType(tableType);
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

        public void addNeedSafeCast(Type type, Integer sourceType) {
            throw new UnsupportedOperationException();
        }
    };

    protected void ensureTableType(SessionTable.TypeStruct tableType) {
        throw new UnsupportedOperationException();
    }

    protected Connection[] ensureConnections;

    protected void executeEnsure(Connection connect, String command) {
        try {
            executeEnsureWithException(connect, command);
        } catch (SQLException e) {
            ServerLoggers.sqlSuppLog(e);
        }
    }

    protected void executeEnsure(String command) {
        try {
            executeEnsureWithException(command);
        } catch (SQLException e) {
            ServerLoggers.sqlSuppLog(e);
        }
    }

    public static String readResource(String path) throws IOException {
        InputStream resourceAsStream = BusinessLogics.class.getResourceAsStream(path);
        if(resourceAsStream == null)
            return null;
        return IOUtils.readStreamToString(resourceAsStream, ExternalUtils.resourceCharset.name());
    }

    public static Set<String> disabledFunctions = new HashSet<>();
    protected void executeEnsure(List<String> functions) {
        functions.forEach(command -> {
            try {
                executeEnsureWithException(readResource(command));
            } catch (IOException | SQLException e) {
                String name = new File(command).getName();
                if (name.endsWith("_opt.sql")) {
                    disabledFunctions.add(name);
                } else {
                    throw new RuntimeException(command, e);
                }
            }
        });
    }

    public static boolean hasTrgmExtension() {
        return !disabledFunctions.contains("trgm_opt.sql");
    }

    protected void executeEnsureWithException(String command) throws SQLException {
        for(Connection ensureConnection : ensureConnections)
            executeEnsureWithException(ensureConnection, command);
    }

    protected static void executeEnsureWithException(Connection ensureConnection, String command) throws SQLException {
        try (Statement statement = ensureConnection.createStatement()) {
            statement.execute(command);
        }
    }

    protected MAddExclMap<ConcatenateType, Boolean> ensuredConcTypes = MapFact.mAddExclMap();

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

    public void ensureArrayClass(ArrayClass arrayClass) {
        throw new UnsupportedOperationException();
    }

    protected String safeCastString;
    protected String safeCastIntString;
    protected String safeCastStrString;

    private LRUSVSMap<Pair<Type, Integer>, Boolean> ensuredSafeCasts = new LRUSVSMap<>(LRUUtil.G2);

    public synchronized void ensureSafeCast(Pair<Type, Integer> castType) throws SQLException {
        Boolean ensured = ensuredSafeCasts.get(castType);
        if(ensured != null)
            return;

        // assert type.hasSafeCast;
        Properties properties = new Properties();
        properties.put("function.name", DefaultSQLSyntax.genSafeCastName(castType.first, castType.second));
        properties.put("param.type", castType.first.getDB(syntax, recTypes));
        properties.put("param.minvalue", castType.first.getInfiniteValue(true).toString());
        properties.put("param.maxvalue", castType.first.getInfiniteValue(false).toString());

        boolean isInt = castType.second == 0 && Settings.get().getSafeCastIntType() == 1;
        boolean isStr = castType.second == 1 && Settings.get().getSafeCastIntType() == 1;

        executeEnsure(stringResolver.replacePlaceholders(
                isInt ? safeCastIntString : isStr ? safeCastStrString : safeCastString, properties));

        ensuredSafeCasts.put(castType, true);
    }

    public void ensureGroupAggOrder(Pair<GroupType, ImList<Type>> groupAggOrder) {
    }

    public void ensureTypeFunc(Pair<TypeFunc, Type> tf) {
    }

    protected String getPath() {
        throw new UnsupportedOperationException();
    }
    public void ensureScript(String script, Properties props) throws SQLException, IOException {
        String scriptString = readResource(getPath() + script);
        executeEnsure(stringResolver.replacePlaceholders(scriptString, props));
    }

    protected String getConcTypeName(ConcatenateType type) {
        return syntax.getConcTypeName(type);
    }
    
    public SQLSyntax getSyntax() {
        return syntax;
    }    
}
