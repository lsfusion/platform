package lsfusion.server.data.sql;

import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.AbstractConnectionPool;
import lsfusion.server.data.TypePool;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.ExecutionContext;
import org.apache.log4j.Logger;
import org.springframework.util.PropertyPlaceholderHelper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Properties;

public abstract class DataAdapter extends AbstractConnectionPool implements SQLSyntax, TypePool {
    protected final static Logger logger = Logger.getLogger(DataAdapter.class);

    public String server;
    public String dataBase;
    public String userID;
    public String password;

    // для debuga
    protected DataAdapter() {
    }

    protected abstract void ensureDB(boolean cleanDB) throws Exception, SQLException, InstantiationException, IllegalAccessException;

    protected DataAdapter(String dataBase, String server, String userID, String password, boolean cleanDB) throws Exception, SQLException, IllegalAccessException, InstantiationException {

        Class.forName(getClassName());

        this.dataBase = dataBase;
        this.server = server;
        this.userID = userID;
        this.password = password;

        ensureDB(cleanDB);

        ensureConnection = startConnection();
        ensureConnection.setAutoCommit(true);
        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sqlaggr/getAnyNotNull.sc")));
        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sqlfun/jumpWorkdays.sc")));
        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sqlfun/completeBarcode.sc")));
        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sqlaggr/aggf.sc")));
        recursionString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sqlaggr/recursion.sc"));
        safeCastString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sqlaggr/safecast.sc"));
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

    public boolean isGreatest() {
        return true;
    }

    public boolean useFJ() {
        return true;
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
        return "CURRENT_TIMESTAMP";
    }

    public String typeConvertSuffix(Type oldType, Type newType, String name, TypeEnvironment typeEnv) {
        return "";
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
        return true;
    }

    public void setLogLevel(Connection connection, int level) {
        throw new UnsupportedOperationException();
    }

    public boolean orderTopTrouble() {
        throw new RuntimeException("unknown");
    }

    public String backupDB(ExecutionContext context, String dumpFileName, List<String> excludeTables) throws IOException, InterruptedException {
        return null;
    }

    public String getAnalyze(){
        return "";
    }

    public String getVacuumDB(){
        return "";
    }

    public void useDLL(){
        /*try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");

            Connection connect = DriverManager.getConnection("jdbc:jtds:sqlserver://localhost:1433;instance=SQLEXPRESS;User=sa;Password=11111");

            InputStream dllStream = Main.class.getResourceAsStream("SQLUtils.dll");
            String dllName = "SQLUtils";

            connect.createStatement().execute("USE test");

            connect.createStatement().execute("IF OBJECT_ID(N'Concatenate', N'AF') is not null DROP Aggregate Concatenate;");

            PreparedStatement statement = connect.prepareStatement("IF EXISTS (SELECT * FROM sys.assemblies WHERE [name] = ?) DROP ASSEMBLY SQLUtils;");
            statement.setString(1, dllName);
            statement.execute();
            statement.clearParameters();

            statement = connect.prepareStatement("CREATE ASSEMBLY [SQLUtils] \n" +
                    "FROM  ? "+
                    "WITH permission_set = Safe;");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (dllStream.read(buffer) != -1) out.write(buffer);

            statement.setBytes(1, out.toByteArray());
            statement.execute();
            statement.clearParameters();

            connect.createStatement().execute("CREATE AGGREGATE [dbo].[Concatenate](@input nvarchar(4000))\n" +
                    "RETURNS nvarchar(4000)\n" +
                    "EXTERNAL NAME [SQLUtils].[Concatenate];");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/
    }

    public static String genTypePostfix(ImList<Type> types) {
        String result = "";
        for(int i=0,size=types.size();i<size;i++)
            result = (result.length()==0?"":result + "_") + types.get(i).getSID();
        return result;
    }

    public static String genConcTypeName(ConcatenateType type) {
        return "T" + genTypePostfix(type.getTypes());
    }

    public static String genRecursionName(ImList<Type> types) {
        return "recursion_" + genTypePostfix(types);
    }

    public static String genNRowName(ImList<Type> types) {
        return "NROW" + types.size();
    }

    private final TypeEnvironment recTypes = new TypeEnvironment() {
        public void addNeedRecursion(ImList<Type> types) {
            throw new UnsupportedOperationException();
        }

        public void addNeedType(ConcatenateType types) {
            try {
                ensureConcType(types);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public void addNeedSafeCast(Type type) {
            throw new UnsupportedOperationException();
        }
    };

    protected Connection ensureConnection;

    protected void executeEnsure(String command) throws SQLException {
        Statement statement = ensureConnection.createStatement();
        try {
            statement.execute(command);
        } catch(SQLException e) {
            ServerLoggers.sqlSuppLog(e);
        } finally {
            statement.close();
        }
    }

    private MAddExclMap<ConcatenateType, Boolean> ensuredConcTypes = MapFact.mAddExclMap();

    protected String notNullRowString;

    public synchronized void ensureConcType(ConcatenateType concType) throws SQLException {

        Boolean ensured = ensuredConcTypes.get(concType);
        if(ensured != null)
            return;

        // ensuring types
        String declare = "";
        ImList<Type> types = concType.getTypes();
        for (int i=0,size=types.size();i<size;i++)
            declare = (declare.length() ==0 ? "" : declare + ",") + ConcatenateType.getFieldName(i) + " " + types.get(i).getDB(this, recTypes);

        String typeName = genConcTypeName(concType);
        executeEnsure("CREATE TYPE " + typeName + " AS (" + declare + ")");

        // создаем cast'ы всем concatenate типам
        for(int i=0,size=ensuredConcTypes.size();i<size;i++) {
            ConcatenateType ensuredType = ensuredConcTypes.getKey(i);
            if(concType.getCompatible(ensuredType)!=null) {
                String ensuredName = genConcTypeName(ensuredType);
                executeEnsure("DROP CAST IF EXISTS (" + typeName + " AS " + ensuredName + ")");
                executeEnsure("CREATE CAST (" + typeName + " AS " + ensuredName + ") WITH INOUT AS IMPLICIT"); // в обе стороны так как containsAll в DataClass по прежнему не направленный
                executeEnsure("DROP CAST IF EXISTS (" + ensuredName + " AS " + typeName + ")");
                executeEnsure("CREATE CAST (" + ensuredName + " AS " + typeName + ") WITH INOUT AS IMPLICIT");
            }
        }

        ensuredConcTypes.exclAdd(concType, true);
    }

    private static final PropertyPlaceholderHelper stringResolver = new PropertyPlaceholderHelper("${", "}", ":", true);

    protected String recursionString;
    protected String safeCastString;

    private LRUSVSMap<ImList<Type>, Boolean> ensuredRecursion = new LRUSVSMap<ImList<Type>, Boolean>(LRUUtil.G2);

    public synchronized void ensureRecursion(ImList<Type> types) throws SQLException {

        Boolean ensured = ensuredRecursion.get(types);
        if(ensured != null)
            return;

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

        ensuredRecursion.put(types, true);
    }

    public static String genSafeCastName(Type type) {
        return "scast_" + type.getSID();
    }

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
        throw new UnsupportedOperationException();
    }

    public String getRandomName() {
        return "random";
    }

    public boolean isTransactionCanceled(SQLException e) {
        throw new UnsupportedOperationException();
    }

    public boolean isConnectionClosed(SQLException e) {
        throw new UnsupportedOperationException();
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

    public String getConstraintName(String name) {
        return name;
    }

    public String getIndexName(String name) {
        return name;
    }

    public void ensureLogLevel(int logLevel) {
    }
}
