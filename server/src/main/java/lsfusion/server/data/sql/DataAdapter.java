package lsfusion.server.data.sql;

import lsfusion.base.IOUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.AbstractConnectionPool;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.SessionTable;
import lsfusion.server.data.TypePool;
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
import java.util.Set;

public abstract class DataAdapter extends AbstractConnectionPool implements TypePool {
    public final static SQLSyntax debugSyntax = PostgreSQLSyntax.instance;
    protected final static Logger logger = Logger.getLogger(DataAdapter.class);
    public final SQLSyntax syntax;

    public String server;
    public String instance;
    public String dataBase;
    public String userID;
    public String password;
    public Long connectTimeout;

    protected abstract void ensureDB(boolean cleanDB) throws Exception;

    protected DataAdapter(SQLSyntax syntax, String dataBase, String server, String instance, String userID, String password, Long connectTimeout, boolean cleanDB) throws Exception {

        Class.forName(syntax.getClassName());

        this.syntax = syntax;
        
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

    public String getBackupFilePath(String dumpFileName) throws IOException, InterruptedException {
        return null;
    }

    public String backupDB(ExecutionContext context, String dumpFileName, List<String> excludeTables) throws IOException, InterruptedException {
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

    protected Connection ensureConnection;

    protected void executeEnsure(String command) throws SQLException {
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

    public void ensureArrayClass(ArrayClass arrayClass) throws SQLException {
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

    public void ensureGroupAggOrder(Pair<GroupType, ImList<Type>> groupAggOrder) throws SQLException {
    }

    public void ensureTypeFunc(Pair<TypeFunc, Type> tf) throws SQLException {
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
