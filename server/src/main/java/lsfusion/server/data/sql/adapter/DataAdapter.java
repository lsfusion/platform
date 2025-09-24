package lsfusion.server.data.sql.adapter;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.base.ResourceUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.file.IOUtils;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.sql.connection.AbstractConnectionPool;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLConsumer;
import lsfusion.server.data.sql.lambda.SQLRunnable;
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

    public static abstract class Server {
        public final String host;
        public Connection ensureConnection;
        private CpuTime lastCpuTime;
        private double load;

        public Server(String host) {
            this.host = host;
        }

        public double getLoad() {
            return load;
        }

        public void setLoad(double load) {
            this.load = load;
        }

        public CpuTime getLastCpuTime() {
            return lastCpuTime;
        }

        public void setLastCpuTime(CpuTime lastCpuTime) {
            this.lastCpuTime = lastCpuTime;
        }

        public abstract boolean isMaster();
    }

    public static class Master extends Server {
        public Master(String host) {
            super(host);
        }
        public boolean isMaster() {
            return true;
        }
    }
    public static class Slave extends Server {

        public final String id;

        public Slave(String host, String id) {
            super(host);

            this.id = id;
        }

        public boolean isMaster() {
            return false;
        }
    }

    public interface NeedServer {

        Server getServer(DataAdapter adapter, LogSequenceNumber lsn) throws SQLException;

        NeedServer BEST = DataAdapter::getBestServer;
    }

    public interface NeedExplicitServer extends NeedServer {

        Server getServer(DataAdapter adapter);

        boolean isMaster();

        static NeedExplicitServer EXPLICIT(Server server) {
            return new NeedExplicitServer() {
                @Override
                public Server getServer(DataAdapter adapter) {
                    return server;
                }

                @Override
                public boolean isMaster() {
                    return server.isMaster();
                }
            };
        }

        NeedExplicitServer MASTER = new NeedExplicitServer() {
            @Override
            public Server getServer(DataAdapter adapter) {
                return adapter.master;
            }

            @Override
            public boolean isMaster() {
                return true;
            }
        };

        @Override
        default Server getServer(DataAdapter adapter, LogSequenceNumber lsn) throws SQLException {
            return getServer(adapter);
        }
    }

    protected final Master master;

    public Master getMaster() {
        return master;
    }

    protected final List<Server> servers = Collections.synchronizedList(new ArrayList<>());

    public boolean serverAvailability(Server server) {
        return servers.contains(server);
    }

    public abstract int getNumberOfConnections(Server server) throws SQLException;

    public Slave findSlave(String id) {
        for (Server server : servers) {
            if (!server.isMaster()) {
                Slave slave = (Slave) server;
                if (slave.id.equals(id))
                    return slave;
            }
        }
        return null;
    }

    public String dataBase;
    public String user;
    public String password;
    public Long connectTimeout;

    public abstract void ensureDB(Server server, boolean cleanDB) throws Exception;

    public void initProctab(Server server){
    }

    protected DataAdapter(SQLSyntax syntax, String dataBase, String server, String user, String password, Long connectTimeout) throws Exception {

        Class.forName(syntax.getClassName());

        this.syntax = syntax;

        this.master = new Master(server);

        this.user = user;
        this.password = password;

        this.dataBase = dataBase;

        this.connectTimeout = connectTimeout;
    }

    public void startEnsureConnection(Server server) throws SQLException {
        Connection connection = startConnection(server);
        connection.setAutoCommit(true);
        server.ensureConnection = connection;
    }
    public void closeEnsureConnection(Server server) throws SQLException {
        closeConnection(server.ensureConnection, cn -> {});
    }

    protected boolean checkLSN(Server server, LogSequenceNumber lsn) throws SQLException {
        if(server.isMaster()) // if master
            return true;

        LogSequenceNumber connectionLSN = getSlaveLSN(server);

        if(connectionLSN == LogSequenceNumber.INVALID_LSN) // first copy / or not yet started wal sync
            return false;

        if(connectionLSN == DataAdapter.NO_SUBSCRIPTION)
            return false;

        assert lsn != null;
        return connectionLSN.compareTo(lsn) >= 0;
    }

    public void runOnAllServers(SQLConsumer<Server> run) throws SQLException, SQLHandledException {
        for(Server server : servers)
            run.accept(server);
    }
    public LogSequenceNumber ensureMaster(boolean cleanDB, SQLRunnable backwardCompatibility) throws Exception {
        ensureDB(master, cleanDB);

        startEnsureConnection(master);

        servers.add(master);

        backwardCompatibility.run();

        ensureSqlFuncs(master);

        return getMasterLSN();
    }
    public void addMaster(SQLConsumer<Server> synchronizeDB) throws Exception {
        synchronizeDB.accept(master);

        ensurePublication(master);
    }
    public void addSlave(Slave server, SQLConsumer<Server> synchronizeDB, SQLRunnable onFirstStart, LogSequenceNumber lsn) throws Exception {
        ensureDB(server, false);

        startEnsureConnection(server);

        ensureSqlFuncs(server);

        initProctab(server); //This runs on the slave in order to install the extension. Because extensions are not copied during replication.

        ensureCaches(server);

        if(lsn != null)
            waitForMasterLSN(lsn, server);

        synchronizeDB.accept(server);

        ensureAndEnableSubscription(server, onFirstStart);

        // adding in the end to be used only after
        servers.add(server);
    }
    public void removeSlave(DataAdapter.Slave slave) throws SQLException {
        for(Server server : servers) {
            if(server.host.equals(slave.host)) {
                servers.remove(server);

                disableSubscription(slave);

                closeEnsureConnection(slave);
                break;
            }
        }
    }
    public void waitAndDisable(Slave server, LogSequenceNumber lsn) throws Exception {
        try {
            startEnsureConnection(server);
        } catch (SQLException e) {
            ServerLoggers.sqlSuppLog(e);
            return;
        }

        waitForMasterLSN(lsn, server);

        disableSubscription(server);

        closeEnsureConnection(server);
    }

    protected abstract void ensurePublication(Master master) throws Exception;
    protected abstract void waitForMasterLSN(LogSequenceNumber masterLSN, Slave slave) throws SQLException;
    protected abstract void ensureAndEnableSubscription(Slave server, SQLRunnable onFirstStart) throws Exception;
    protected abstract void disableSubscription(Slave slave) throws SQLException;

    // getting least loaded server
    private Server getBestServer(LogSequenceNumber lsn) throws SQLException {
        if(servers.size() == 1)
            return master;

        Server bestServer = null;
        double bestLoad = 0;
        for (Server server : servers) {
            if (checkLSN(server, lsn)) {
                double load = getLoad(server);
                if (bestServer == null || load <= bestLoad) {
                    bestServer = server;
                    bestLoad = load;
                }
            }
        }

        return bestServer;
    }

    protected void destroyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    protected boolean needPoolConnection() {
        return servers.size() != 1;
    }

    public static List<String> getAllDBNames() {
        return new ArrayList<>(Arrays.asList(PostgreDataAdapter.DB_NAME, MySQLDataAdapter.DB_NAME));
    }

    private List<String> findSQLScripts() {
        List<String> otherDBNames = getAllDBNames();
        otherDBNames.remove(getDBName());
        return ResourceUtils.getResources(Pattern.compile("/sql/.*")).stream().filter(resource -> {
            if (resource.contains(".tsql"))
                return false;
            for (String otherDB : otherDBNames) {
                if (resource.contains("/" + otherDB + "/"))
                    return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    public abstract String getDBName();

    private void ensureSqlFuncs(Server server) throws SQLException {
        for (String command : findSQLScripts()) {
            String resourceCmd;
            boolean ignoreErrors;
            try {
                resourceCmd = readResource(command);
                ignoreErrors = BaseUtils.getFileName(command).endsWith("_opt");
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
            try {
                executeEnsureWithException(server, resourceCmd);
            } catch (SQLException e) {
                if(ignoreErrors)
                    disabledFunctions.add(BaseUtils.getFileNameAndExtension(command));
                else
                    throw e;
            }
        }
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

    protected void executeEnsure(Server server, String command) {
        try {
            executeEnsureWithException(server, command);
        } catch (SQLException e) {
            ServerLoggers.sqlSuppLog(e);
        }
    }
    protected void executeEnsure(Connection connect, String command) {
        try {
            try (Statement statement = connect.createStatement()) {
                statement.execute(command);
            }
        } catch (SQLException e) {
            ServerLoggers.sqlSuppLog(e);
        }
    }

    protected void executeEnsure(String command, Slave slave) {
        for(Server server : (slave == null ? servers : Collections.singletonList(slave))) {
            try {
                executeEnsureWithException(server, command);
            } catch (SQLException e) {
                ServerLoggers.sqlSuppLog(e);
            }
        }
    }

    public static String readResource(String path) throws IOException {
        InputStream resourceAsStream = BusinessLogics.class.getResourceAsStream(path);
        if(resourceAsStream == null)
            return null;
        return IOUtils.readStreamToString(resourceAsStream, ExternalUtils.resourceCharset.name());
    }

    public static Set<String> disabledFunctions = new HashSet<>();

    public static boolean hasTrgmExtension() {
        return !disabledFunctions.contains("trgm_opt.sql");
    }

    protected static void executeEnsureWithException(Server server, String command) throws SQLException {
        try (Statement statement = server.ensureConnection.createStatement()) {
            statement.execute(command);
        }
    }

    public void ensureConcType(ConcatenateType concType) throws SQLException {
        ensureConcType.run(concType);
    }

    protected void proceedEnsureConcType(ConcatenateType concType, Slave slave) throws SQLException {
        // ensuring types
        String declare = "";
        ImList<Type> types = concType.getTypes();
        for (int i=0,size=types.size();i<size;i++)
            declare = (declare.length() ==0 ? "" : declare + ",") + ConcatenateType.getFieldName(i) + " " + types.get(i).getDB(syntax, recTypes);

        String typeName = getConcTypeName(concType);
        executeEnsure("CREATE TYPE " + typeName + " AS (" + declare + ")", slave);

        // create casts for all concatenate types
        for(int i=0,size=ensureConcType.cache.size();i<size;i++) {
            ConcatenateType ensuredType = ensureConcType.cache.get(i);
            if(concType.getCompatible(ensuredType)!=null) {
                String ensuredName = getConcTypeName(ensuredType);
                executeEnsure("DROP CAST IF EXISTS (" + typeName + " AS " + ensuredName + ")", slave);
                //If call CREATE CAST with typeName equal to ensuredName, we will get an error.
                if (!typeName.equalsIgnoreCase(ensuredName))
                    executeEnsure("CREATE CAST (" + typeName + " AS " + ensuredName + ") WITH INOUT AS IMPLICIT", slave); // в обе стороны так как containsAll в DataClass по прежнему не направленный
                executeEnsure("DROP CAST IF EXISTS (" + ensuredName + " AS " + typeName + ")", slave);
                if (!typeName.equalsIgnoreCase(ensuredName))
                    executeEnsure("CREATE CAST (" + ensuredName + " AS " + typeName + ") WITH INOUT AS IMPLICIT", slave);
            }
        }
    }

    public static final PropertyPlaceholderHelper stringResolver = new PropertyPlaceholderHelper("${", "}", ":", true);

    public void ensureArrayClass(ArrayClass arrayClass) {
        throw new UnsupportedOperationException();
    }

    protected String recursionString;
    protected String safeCastString;
    protected String safeCastIntString;
    protected String safeCastStrString;

    private interface ProceedEnsure<T> {
        void proceed(T element, Slave slave) throws SQLException;
    }
    private static class Ensure<T> {
        private final MAddSet<T> cache = SetFact.mAddSet();
        private final ProceedEnsure<T> proceed;

        public Ensure(ProceedEnsure<T> proceed) {
            this.proceed = proceed;
        }

        public void run(T object) throws SQLException {
            synchronized (cache) {
                if (!cache.contains(object)) {
                    proceed.proceed(object, null);

                    cache.add(object);
                }
            }
        }
        public void run(Slave slave) throws SQLException {
            synchronized (cache) {
                for (int i = 0, size = cache.size(); i < size; i++) {
                    proceed.proceed(cache.get(i), slave);
                }
            }
        }
    }

    private final Ensure<Pair<Type, Integer>> ensureSafeCast = new Ensure<>(this::proceedEnsureSafeCast);
    private final Ensure<ConcatenateType> ensureConcType = new Ensure<>(this::proceedEnsureConcType);
    private final Ensure<Object> ensureRecursion = new Ensure<>(this::proceedEnsureRecursion);

    public void ensureCaches(Slave slave) throws SQLException {
        for(Ensure ensure : new Ensure[] {ensureSafeCast, ensureConcType, ensureRecursion})
            ensure.run(slave);
    }

    public void ensureSafeCast(Pair<Type, Integer> castType) throws SQLException {
        ensureSafeCast.run(castType);
    }

    private void proceedEnsureSafeCast(Pair<Type, Integer> castType, Slave slave) {
        // assert type.hasSafeCast;
        Properties properties = new Properties();
        properties.put("function.name", DefaultSQLSyntax.genSafeCastName(castType.first, castType.second));
        properties.put("param.type", castType.first.getDB(syntax, recTypes));
        properties.put("param.minvalue", castType.first.getInfiniteValue(true).toString());
        properties.put("param.maxvalue", castType.first.getInfiniteValue(false).toString());

        boolean isInt = castType.second == 0 && Settings.get().getSafeCastIntType() == 1;
        boolean isStr = castType.second == 1 && Settings.get().getSafeCastIntType() == 1;

        executeEnsure(stringResolver.replacePlaceholders(
                isInt ? safeCastIntString : isStr ? safeCastStrString : safeCastString, properties), slave);
    }

    @Override
    public void ensureRecursion(Object object) throws SQLException {
        ensureRecursion.run(object);
    }

    private void proceedEnsureRecursion(Object object, Slave slave) {
        ImList<Type> types = (ImList<Type>) object;

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

        executeEnsure(stringResolver.replacePlaceholders(recursionString, properties), slave);
    }

    public void ensureGroupAggOrder(Pair<GroupType, ImList<Type>> groupAggOrder) {
    }

    public void ensureTypeFunc(Pair<TypeFunc, Type> tf) {
    }

    protected String getPath() {
        throw new UnsupportedOperationException();
    }

    protected String getConcTypeName(ConcatenateType type) {
        return syntax.getConcTypeName(type);
    }
    
    public SQLSyntax getSyntax() {
        return syntax;
    }    
}
