package lsfusion.server.data.sql;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.server.classes.ByteArrayClass;
import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.data.Field;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.SessionTable;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.*;
import lsfusion.server.data.type.*;
import lsfusion.server.logics.BusinessLogics;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;

import static lsfusion.server.data.sql.MSSQLSQLSyntax.getOrderGroupAggName;
import static lsfusion.server.data.sql.MSSQLSQLSyntax.getTypeFuncName;

public class MSSQLDataAdapter extends DataAdapter {

    public MSSQLDataAdapter(String database, String server, String userID, String password, String instance) throws Exception {
        super(MSSQLSQLSyntax.instance, database, server, instance, userID, password, null, false);
    }

    private Connection getConnection() throws SQLException {
//        return DriverManager.getConnection("jdbc:jtds:sqlserver://"+ server + ";instance=" + instance + ";User=" + userID + ";Password=" + password);
        return DriverManager.getConnection("jdbc:sqlserver://" + server + ";instance=" + instance + ";user=" + userID + ";password=" + password);
    }

    public void ensureDB(boolean cleanDB) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        //namedPipe=true;
//        Connection connect = DriverManager.getConnection("jdbc:jtds:sqlserver://"+ server +":1433;User=" + userID + ";Password=" + password);
        Connection connect = getConnection();
        try {
//        try {
//            connect.createStatement().execute("DROP DATABASE "+ dataBase);
//        } catch (Exception e) {
//
//        }
            connect.createStatement().execute("CREATE DATABASE " + dataBase);
        } catch (Exception e) {
        }
//
//        try {
//            connect.createStatement().execute("ALTER DATABASE " + dataBase + " SET TRUSTWORTHY ON");
//        } catch(Exception e) {
//            e = e;
//        }
//

        try {
            connect.createStatement().execute("ALTER DATABASE " + dataBase + " SET ALLOW_SNAPSHOT_ISOLATION ON");
        } catch (Exception e) {
            e = e;
        }

        try {
            connect.createStatement().execute("ALTER DATABASE " + dataBase + " SET READ_COMMITTED_SNAPSHOT ON");
        } catch (Exception e) {
            e = e;
        }

        connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        //namedPipe=true;
        Connection connect = getConnection();
        connect.createStatement().execute("USE " + dataBase);

        return connect;
    }

    private final static String declareFieldsConc = "private ${field.type} _${field.name};";
    private final static String appendFieldsConc = "builder.Append(_${field.name});";
    private final static String appendSepConc = "builder.Append(\",\");";
    private final static String parseStringConc = "pt.${field.name} = xy[${field.number}];";
    private final static String parseConc = "pt.${field.name} = ${field.type}.Parse(xy[${field.number}]);";
    private final static String declarePropsConc = "public ${field.type} ${field.name} { get { return this._${field.name}; } set { _${field.name} = value; } }";
    private final static String assignFieldsConc = "_${field.name} = ${field.name};";
    private final static String compareFieldsConc = "cmp = ${field.name}.CompareTo(typeObj.${field.name});" + '\n' +
            "if(cmp != 0) return ${field.desc}cmp;";
    private final static String declarePrmsConc = "${field.type} ${field.name}";
    private final static String declareSqlPrmsConc = "@${field.name} ${field.sqltype}";
    private final static String serReadConc = "if(r.ReadBoolean())" + '\n' +
            "${field.name} = ${field.type}.Null;" + '\n' +
            "else" + '\n' +
            "${field.name} = new ${field.type}(${field.read});";
    private final static String serWriteConc = "if(${field.name}.IsNull)" + '\n' +
            "w.Write(true);" + '\n' +
            "else {" + '\n' +
            "w.Write(false);" + '\n' +
            "${field.write} }";

    private final static String passPrmsConc = "${field.name}";
    private String concTypeString;
    private String arrayClassString;

    private static class GroupAggParse {
        public final String code;
        public final String[] serFields;

        private GroupAggParse(String code, String[] serFields) {
            this.code = code;
            this.serFields = serFields;
        }
    }

    private ImMap<GroupType, GroupAggParse> groupAggOrderStrings;

    private ImMap<TypeFunc, String> typeFuncStrings;

    private String compilerExe;

    @Override
    protected void ensureSystemFuncs() throws IOException, SQLException {
        compilerExe = SystemUtils.getExePath("cc", "/sql/mssql/", BusinessLogics.class);
        concTypeString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/ConcType.cs"));
        arrayClassString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/ArrayClass.cs"));
        executeEnsure("EXEC sp_configure 'clr enabled', '1'");
        executeEnsure("RECONFIGURE");

//        ensureDTLFromResource("SQLUtils");
        groupAggOrderStrings = MapFact.toMap(
                GroupType.STRING_AGG, new GroupAggParse(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/StringAgg.cs")), new String[]{null, null, "ov"}),
                GroupType.LAST, new GroupAggParse(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/LastValue.cs")), new String[]{"lastValue", "lastOrder"}));
        typeFuncStrings = MapFact.toMap(
                TypeFunc.NOTZERO, IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/notzero.sc")),
                TypeFunc.MAX, IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/maxf.sc")),
                TypeFunc.MIN, IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/minf.sc")),
                TypeFunc.ANDEXPR, IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/andexpr.sc"))
        );

        String command = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/aggf.sc"));
        String separator = System.getProperty("line.separator");
        for (String single : command.split(separator + "GO" + separator))
            executeEnsure(single);

        ensureDTL(new Pair<>("LSFUtils", IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/Utils.cs"))), false, ListFact.<Type>EMPTY());

        ensureConcType(ConcatenateType.get(new Type[]{DateClass.instance, ObjectType.instance}));

        recursionString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/recursion.sc"));
        safeCastString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/safecast.sc"));
    }

    @Override
    protected String getPath() {
        return "/sql/mssql/";
    }

    public void useDLL() {
/*        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");

            Connection connect = getConnection();

            InputStream dllStream = BusinessLogics.class.getResourceAsStream("/sql/mssql/SQLUtils.dll");
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

    protected MAddExclMap<Pair<GroupType, ImList<Type>>, Boolean> ensuredGroupAggOrders = MapFact.mAddExclMap();

    public synchronized void ensureGroupAggOrder(Pair<GroupType, ImList<Type>> groupAggOrder) throws SQLException {
        Boolean ensured = ensuredGroupAggOrders.get(groupAggOrder);
        if (ensured != null)
            return;

        Properties properties = new Properties();
        GroupType groupType = groupAggOrder.first;
        ImList<Type> types = groupAggOrder.second;
        String aggName = getOrderGroupAggName(groupType, types);
        properties.put("aggr.name", aggName);

        GroupAggParse parse = groupAggOrderStrings.get(groupType);

        StringBuilder read = new StringBuilder();
        StringBuilder write = new StringBuilder();
        for (int i = 0, size = types.size(); i < size; i++) {
            Type type = types.get(i);
            String name;
            if (i == size - 1) {
                name = "order";
                properties.put("order.size", "" + type.getDotNetSize());
            } else
                name = "value" + (i == 0 ? "" : i);
            String serField = parse.serFields[i];

            String dotNetType = type.getDotNetType(syntax, recTypes);
            properties.put(name + ".type", dotNetType);
            properties.put(name + ".sqltype", getSQLType(type));
            appendSerialization(read, write, type, dotNetType, serField);
        }

        properties.put("ser.read", read.toString());
        properties.put("ser.write", write.toString());

        ensureDTL(new Pair<>(aggName, stringResolver.replacePlaceholders(parse.code, properties)), false, types);

        ensuredGroupAggOrders.exclAdd(groupAggOrder, true);
    }

    protected MAddExclMap<Pair<TypeFunc, Type>, Boolean> ensuredTypeFuncs = MapFact.mAddExclMap();

    public synchronized void ensureTypeFunc(Pair<TypeFunc, Type> tf) throws SQLException {
        Boolean ensured = ensuredTypeFuncs.get(tf);
        if (ensured != null)
            return;

        Properties properties = new Properties();
        properties.put("param.sqltype", tf.second.getDB(syntax, recTypes));
        String typeFuncName = getTypeFuncName(tf.first, tf.second);
        properties.put("fnc.name", typeFuncName);
        executeEnsure(stringResolver.replacePlaceholders(typeFuncStrings.get(tf.first), properties));

        ensuredTypeFuncs.exclAdd(tf, true);
    }

    private String getSQLType(Type type) {
        return type instanceof DateClass ? syntax.getDateTimeType() : type.getDB(syntax, recTypes);
    }

    private static void appendSerialization(StringBuilder read, StringBuilder write, Type type, String dotNetType, String serField) {
        if (serField != null) {
            String fieldRead, fieldWrite;
            if (type instanceof ConcatenateType) {
                fieldRead = serField + " = new " + dotNetType + "();" + '\n' + serField + ".Read(r);";
                fieldWrite = serField + ".Write(w);";
            } else {
                Properties fprops = new Properties();
                fprops.put("field.type", dotNetType);
                fprops.put("field.name", serField);
                fprops.put("field.read", type.getDotNetRead("r"));
                fprops.put("field.write", type.getDotNetWrite("w", serField + ".Value"));

                fieldRead = stringResolver.replacePlaceholders(serReadConc, fprops);
                fieldWrite = stringResolver.replacePlaceholders(serWriteConc, fprops);
            }

            if (read.length() > 0)
                read.append('\n');
            read.append(fieldRead);
            if (write.length() > 0)
                write.append('\n');
            write.append(fieldWrite);
        }
    }

    private ImList<Pair<String, String>> getTypesSources(ImList<Type> types, Set<String> refs) {
        MList<Pair<String, String>> mResult = ListFact.mList();
        for (Type type : types)
            if (type instanceof ConcatenateType) {
                ConcatenateType concType = (ConcatenateType) type;
                mResult.addAll(getTypesSources(concType.getTypes(), refs));
                mResult.add(split(getFullConcCode(concType), new HashSet<String>(), new ArrayList<String>(), new ArrayList<String>()));
            }
        return mResult.immutableList();
    }

    @Override
    protected void proceedEnsureConcType(ConcatenateType concType) throws SQLException {
        Pair<String, String> code = getFullConcCode(concType);
        ensureDTL(code, true, concType.getTypes());
    }

    private Pair<String, String> getFullConcCode(ConcatenateType concType) {
        // ensuring types
        String declareFields = "";
        String appendFields = "";
        String parseFields = "";
        String declareProps = "";
        String assignFields = "";
        String compareFields = "";
        String hashcodeFields = "";
        String declarePrms = "";
        String declareSqlPrms = "";
        String passPrms = "";
        String checkIsNull = "false";
        ImList<Type> types = concType.getTypes();
        boolean[] desc = concType.getDesc();
        StringBuilder read = new StringBuilder();
        StringBuilder write = new StringBuilder();
        for (int i = 0, size = types.size(); i < size; i++) {
            Properties properties = new Properties();
            Type type = types.get(i);
            String dotNetType = type.getDotNetType(syntax, recTypes);
            properties.put("field.type", dotNetType);
            String fieldName = ConcatenateType.getFieldName(i);
            properties.put("field.name", fieldName);
            properties.put("field.number", "" + i);
            properties.put("field.sqltype", getSQLType(type));
            properties.put("field.desc", desc[i] ? "-" : "");
            if (i == 0)
                checkIsNull = fieldName + ".IsNull";

            declareFields = (declareFields.length() == 0 ? "" : declareFields + '\n') + stringResolver.replacePlaceholders(declareFieldsConc, properties);
            appendFields = (appendFields.length() == 0 ? "" : appendFields + appendSepConc + '\n') + stringResolver.replacePlaceholders(appendFieldsConc, properties);
            parseFields = (parseFields.length() == 0 ? "" : parseFields + '\n') + stringResolver.replacePlaceholders(type instanceof StringClass ? parseStringConc : parseConc, properties);
            declareProps = (declareProps.length() == 0 ? "" : declareProps + '\n') + stringResolver.replacePlaceholders(declarePropsConc, properties);
            assignFields = (assignFields.length() == 0 ? "" : assignFields + '\n') + stringResolver.replacePlaceholders(assignFieldsConc, properties);
            compareFields = (compareFields.length() == 0 ? "" : compareFields + '\n') + stringResolver.replacePlaceholders(compareFieldsConc, properties);
            declarePrms = (declarePrms.length() == 0 ? "" : declarePrms + ',') + stringResolver.replacePlaceholders(declarePrmsConc, properties);
            passPrms = (passPrms.length() == 0 ? "" : passPrms + ',') + stringResolver.replacePlaceholders(passPrmsConc, properties);
            declareSqlPrms = (declareSqlPrms.length() == 0 ? "" : declareSqlPrms + ',') + stringResolver.replacePlaceholders(declareSqlPrmsConc, properties);

            String hashcodeField = fieldName + ".GetHashCode()";
            hashcodeFields = (hashcodeFields.length() == 0 ? hashcodeField : "( 31 * " + hashcodeFields + " + " + hashcodeField + ")");

            appendSerialization(read, write, type, dotNetType, "_" + fieldName);
        }

        Properties properties = new Properties();
        properties.put("declare.fields", declareFields);
        properties.put("check.isnull", checkIsNull);
        properties.put("append.fields", appendFields);
        properties.put("parse.fields", parseFields);
        properties.put("declare.props", declareProps);
        properties.put("assign.fields", assignFields);
        properties.put("compare.fields", compareFields);
        properties.put("declare.prms", declarePrms);
        properties.put("declare.sqlprms", declareSqlPrms);
        properties.put("pass.prms", passPrms);

        properties.put("hashcode.fields", hashcodeFields);

        properties.put("ser.read", read.toString());
        properties.put("ser.write", write.toString());

        String typeName = getConcTypeName(concType);
        properties.put("type.name", typeName);
        properties.put("maxbyte.size", "" + concType.getDotNetSize());

        return new Pair<>(typeName, stringResolver.replacePlaceholders(concTypeString, properties));
    }

    @Override
    public String getConcTypeName(ConcatenateType type) {
        return dataBase + "_" + DefaultSQLSyntax.genTypePostfix(type.getTypes(), type.getDesc());
    }

    //    public void ensureDTLFromResource(String resource) throws IOException, SQLException {
//        ensureDTL(resource, IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/" + resource + ".cs")), false);
//    }

    public void ensureDTL(Pair<String, String> source, boolean toTempDb, ImList<Type> types) throws SQLException {
        Set<String> refs = new HashSet<>();
        List<String> creates = new ArrayList<>();
        List<String> drops = new ArrayList<>();
        Pair<String, String> code = split(source, refs, creates, drops);

        ensureDTL(toTempDb, getTypesSources(types, refs).addList(code), refs, creates, drops);
    }

    private void ensureDTL(boolean toTempDb, ImList<Pair<String, String>> sources, Set<String> refs, List<String> creates, List<String> drops) throws SQLException {
        byte[] compiled;
        try {
            compiled = compileDLL(sources, refs);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        upload(sources.last().first, creates, drops, compiled, toTempDb);
    }

    private Pair<String, String> split(Pair<String, String> source, Set<String> refs, List<String> creates, List<String> drops) {
        StringBuilder sb = new StringBuilder();
        String separator = System.getProperty("line.separator");
        String[] split = source.second.split(separator);
        int i = 0;
        String line;
        while (!(line = split[i++].trim()).equals("SQL DROP"))
            if (!line.isEmpty())
                creates.add(line);
        while (!(line = split[i++].trim()).equals("REFS"))
            if (!line.isEmpty())
                drops.add(line);
        while (!(line = split[i++].trim()).equals("CODE"))
            if (!line.isEmpty())
                refs.add(line);
        for (; i < split.length; i++) {
            sb.append(split[i]);
            sb.append(separator);
        }
        return new Pair<>(source.first, sb.toString());
    }

    private synchronized void upload(String assemblyName, List<String> creates, List<String> drops, byte[] compiled, boolean toTempDb) throws SQLException {

        for (int i = 0; i < 2; i++) {
            if (i == 0 && !toTempDb)
                continue;

            Properties properties = new Properties();
            properties.put("assembly.name", assemblyName);

            executeEnsure("USE " + (i == 0 ? "tempdb" : dataBase));

            for (String drop : drops)
                executeEnsure(stringResolver.replacePlaceholders(drop, properties));

            executeEnsure("IF EXISTS (SELECT * FROM sys.assemblies WHERE [name] = '" + assemblyName + "') DROP ASSEMBLY " + assemblyName + ";");

            executeEnsureParams("CREATE ASSEMBLY [" + assemblyName + "] \n" +
                    "FROM  ? " +
                    "WITH permission_set = Safe;", ListFact.singleton(new TypeObject(compiled, ByteArrayClass.instance)));

            if (i == 0) {
                executeEnsure("DECLARE @prev varbinary(max);\n" +
                        "SET @prev = (SELECT content FROM " + dataBase + ".sys.assembly_files WHERE name = '" + assemblyName + "');\n" +
                        "ALTER ASSEMBLY [" + assemblyName + "] FROM @prev;");
            }

            for (String create : creates)
                executeEnsure(stringResolver.replacePlaceholders(create, properties));
        }
    }

    private void writeIntLE(DataOutputStream out, int v) throws IOException {
        out.write((v) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 24) & 0xFF);
    }

    private void writeString(DataOutputStream outIn, String string) throws IOException {
        byte[] bytes = string.getBytes();
        writeIntLE(outIn, bytes.length);
        outIn.write(bytes);
    }

    public byte[] compileDLL(ImList<Pair<String, String>> sources, Set<String> refs) throws IOException {

        CommandLine commandLine = new CommandLine(compilerExe);
        for (String ref : refs)
            commandLine.addArgument(ref);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream outIn = new DataOutputStream(byteStream);
        writeIntLE(outIn, sources.size());
        for (Pair<String, String> source : sources) {
            writeString(outIn, source.first);
            writeString(outIn, source.second);
        }

        Executor executor = new DefaultExecutor();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(out, new NullOutputStream(), new ByteArrayInputStream(byteStream.toByteArray())));
        executor.setExitValue(0);

        executor.execute(commandLine);

        return out.toByteArray();
    }

//    private Map<Recursion, String> mapNames = new HashMap<Recursion, String>();     
//    private String getRecursionName(Recursion recursion) {
//        String name = SystemUtils.generateID(recursion);
//        String prevName = mapNames.get(recursion);
//        if(!prevName.equals(name)) {
//            for(Map.Entry<Recursion, String> mapN : mapNames.entrySet()) {
//                if(mapN.getKey().equals(recursion)) {
//                    SystemUtils.generateID(mapN.getKey());
//                }
//                    
//            }
//        }
//        return name;
//    }
//

    private String recursionString;

    private MAddExclMap<SessionTable.TypeStruct, Boolean> ensuredTableTypes = MapFact.mAddExclMap();

    @Override
    protected void ensureTableType(SessionTable.TypeStruct tableType) throws SQLException {
        Boolean ensured = ensuredTableTypes.get(tableType);
        if (ensured != null)
            return;

        String typeName = syntax.getTableTypeName(tableType);
        ImOrderSet<Field> fields = SetFact.addOrderExcl(tableType.keys, tableType.properties);
        executeEnsure("CREATE TYPE " + typeName + " AS TABLE (" + fields.toString(SQLSession.getDeclare(syntax, recTypes), ",") + ")");

        ensuredTableTypes.exclAdd(tableType, true);
    }

    private static String getArrayAggName(String className) {
        return "AGG_" + className;
    }

    private MAddExclMap<ArrayClass, Boolean> ensuredArrayClasses = MapFact.mAddExclMap();

    private Pair<String, String> getFullArrayCode(ArrayClass arrayClass) {

        StringBuilder read = new StringBuilder();
        StringBuilder write = new StringBuilder();
        Properties properties = new Properties();

        Type type = arrayClass.getArrayType();
        String dotNetType = type.getDotNetType(syntax, recTypes);
        properties.put("value.type", dotNetType);
        properties.put("value.sqltype", getSQLType(type));
        appendSerialization(read, write, type, dotNetType, "vals[i]");

        properties.put("ser.read", read.toString());
        properties.put("ser.write", write.toString());

        String typeName = MSSQLSQLSyntax.getArrayClassName(arrayClass);
        properties.put("type.name", typeName);
        properties.put("aggr.name", getArrayAggName(typeName));

        return new Pair<>(typeName, stringResolver.replacePlaceholders(arrayClassString, properties));
    }

    @Override
    public void ensureArrayClass(ArrayClass arrayClass) throws SQLException {

        Boolean ensured = ensuredArrayClasses.get(arrayClass);
        if (ensured != null)
            return;

        Pair<String, String> code = getFullArrayCode(arrayClass);
        ensureDTL(code, false, ListFact.singleton(arrayClass.getArrayType())); // сам array тип в tempdb не нужен, так как не материализуется, при этом из-за кривости manager studio его приходится удалять програмно, поэтому пока отключим 

        ensuredArrayClasses.exclAdd(arrayClass, true);
    }

    private LRUSVSMap<Object, Boolean> ensuredRecursion = new LRUSVSMap<>(LRUUtil.G2);

    @Override
    public synchronized void ensureRecursion(Object object) throws SQLException {
        MSSQLSQLSyntax.Recursion recursion = (MSSQLSQLSyntax.Recursion) object;

        Boolean ensured = ensuredRecursion.get(object);
        if (ensured == null) {
            String declare = recursion.types.toString(new GetIndexValue<String, FunctionType>() {
                public String getMapValue(int i, FunctionType value) {
                    return syntax.getParamUsage(i + 1) + " " + value.getParamFunctionDB(syntax, recTypes);
                }
            }, ",");

            Properties properties = new Properties();
            properties.put("function.name", recursion.getName());
            properties.put("params.declare", declare);
            properties.put("result.declare", recursion.fieldDeclare);
            properties.put("rec.table", recursion.recName);
            properties.put("initial.select", recursion.initialSelect);
            String nextTable = "nt" + recursion.recName + "it";
            properties.put("rec.nexttable", nextTable);
            properties.put("step.select", recursion.stepSelect.replace(recursion.recName, "@" + recursion.recName));
            properties.put("step.nextselect", recursion.stepSelect.replace(recursion.recName, "@" + nextTable));

            executeEnsure(stringResolver.replacePlaceholders(recursionString, properties));

            ensuredRecursion.put(object, true);
        }

        // нужно к команде добавить заполнение переменных
    }
}
