package lsfusion.server.data.sql.adapter;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.file.IOUtils;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.connection.AbstractConnectionPool;
import lsfusion.server.data.sql.syntax.DefaultSQLSyntax;
import lsfusion.server.data.sql.syntax.PostgreSQLSyntax;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeFunc;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.exec.TypePool;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.ArrayClass;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;
import org.springframework.util.PropertyPlaceholderHelper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public abstract class DataAdapter extends AbstractConnectionPool implements TypePool {
    public final static SQLSyntax debugSyntax = PostgreSQLSyntax.instance;
    protected final static Logger logger = ServerLoggers.sqlLogger;
    public final SQLSyntax syntax;

    public String server;
    public Integer port;
    public String instance;
    public String dataBase;
    public String userID;
    public String password;
    public Long connectTimeout;

    protected abstract void ensureDB(boolean cleanDB) throws Exception;

    protected DataAdapter(SQLSyntax syntax, String dataBase, String server, Integer port, String instance, String userID, String password, Long connectTimeout, boolean cleanDB) throws Exception {

        Class.forName(syntax.getClassName());

        this.syntax = syntax;
        
        this.dataBase = dataBase;
        this.port = port;
        this.server = server;
        this.userID = userID;
        this.password = password;
        this.connectTimeout = connectTimeout;
        this.instance = instance;
    }

    public void ensure(boolean cleanDB) throws Exception {
        ensureDB(cleanDB);

        ensureConnection = startConnection();
        ensureConnection.setAutoCommit(true);
        ensureSystemFuncs();
    }

    protected void ensureSystemFuncs() throws IOException, SQLException {
        throw new UnsupportedOperationException();        
    }

    public void ensureLogLevel() {
    }

    public String getBackupFilePath(String dumpFileName) {
        return null;
    }

    public String backupDB(ExecutionContext context, String dumpFileName, int threadCount, List<String> excludeTables) throws IOException {
        return null;
    }

    public String customRestoreDB(String fileBackup, Set<String> tables) throws IOException {
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

        public void addNeedSafeCast(Type type) {
            throw new UnsupportedOperationException();
        }
    };

    protected void ensureTableType(SessionTable.TypeStruct tableType) {
        throw new UnsupportedOperationException();
    }

    protected Connection ensureConnection;

    protected void executeEnsure(String command) {
        //        statement.setQueryTimeout(1);
        try (Statement statement = ensureConnection.createStatement()) {
            statement.execute(command);
        } catch (SQLException e) {
            ServerLoggers.sqlSuppLog(e);
        }
    }

    protected void executeEnsureParams(String command, ImList<TypeObject> params) throws SQLException {
        PreparedStatement statement = ensureConnection.prepareStatement(command);
//        statement.setQueryTimeout(1);
        SQLSession.ParamNum paramNum = new SQLSession.ParamNum();
        for(TypeObject param : params)
            param.writeParam(statement, paramNum, syntax);
        try {
            statement.execute();
        } catch(SQLException e) {
            ServerLoggers.sqlSuppLog(e);
        } finally {
            statement.close();
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

    private LRUSVSMap<Type, Boolean> ensuredSafeCasts = new LRUSVSMap<>(LRUUtil.G2);

    public synchronized void ensureSafeCast(Type type) throws SQLException {
        Boolean ensured = ensuredSafeCasts.get(type);
        if(ensured != null)
            return;

        // assert type.hasSafeCast;
        Properties properties = new Properties();
        properties.put("function.name", DefaultSQLSyntax.genSafeCastName(type));
        properties.put("param.type", type.getDB(syntax, recTypes));
        properties.put("param.minvalue", type.getInfiniteValue(true).toString());
        properties.put("param.maxvalue", type.getInfiniteValue(false).toString());

        executeEnsure(stringResolver.replacePlaceholders(safeCastString, properties));

        ensuredSafeCasts.put(type, true);
    }

    public void ensureGroupAggOrder(Pair<GroupType, ImList<Type>> groupAggOrder) {
    }

    public void ensureTypeFunc(Pair<TypeFunc, Type> tf) {
    }

    protected String getPath() {
        throw new UnsupportedOperationException();
    }
    public void ensureScript(String script, Properties props) throws SQLException, IOException {
        String scriptString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream(getPath() + script));
        executeEnsure(stringResolver.replacePlaceholders(scriptString, props));
    }

    protected String getConcTypeName(ConcatenateType type) {
        return syntax.getConcTypeName(type);
    }
    
    public SQLSyntax getSyntax() {
        return syntax;
    }    
}
