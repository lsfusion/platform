package lsfusion.server.data.sql;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.*;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.CompileOrder;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.type.*;
import lsfusion.server.data.type.Reader;
import lsfusion.server.logics.BusinessLogics;
import org.apache.commons.exec.*;

import java.io.*;
import java.sql.*;
import java.util.*;

public class MSSQLDataAdapter extends DataAdapter {

    public MSSQLDataAdapter(String database, String server, String userID, String password, String instance) throws Exception, SQLException, InstantiationException, IllegalAccessException {
        super(database, server, instance, userID, password, false);
    }

    @Override
    public String getLongType() {
        return "bigint";
    }
    @Override
    public int getLongSQL() {
        return Types.BIGINT;
    }

    public int updateModel() {
        return 1;
    }

    public boolean allowViews() {
        return true;
    }

    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        return tableString + setString + " FROM " + fromString + whereString;
    }

    public String getClassName() {
        return "net.sourceforge.jtds.jdbc.Driver";
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:jtds:sqlserver://"+ server + ";instance=" + instance + ";User=" + userID + ";Password=" + password);
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
            connect.createStatement().execute("CREATE DATABASE "+ dataBase);
        } catch(Exception e) {
        }
//
//        try {
//            connect.createStatement().execute("ALTER DATABASE " + dataBase + " SET TRUSTWORTHY ON");
//        } catch(Exception e) {
//            e = e;
//        }
//
        connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        //namedPipe=true;
        Connection connect = getConnection();
        connect.createStatement().execute("USE "+ dataBase);

        return connect;
    }

    public String isNULL(String exprs, boolean notSafe) {
        return "COALESCE(" + exprs + ")";
/*        if(notSafe)
            return "CASE WHEN "+ expr1 +" IS NULL THEN "+ expr2 +" ELSE "+ expr1 +" END";
        else
            return "ISNULL("+ expr1 +","+ expr2 +")";*/
    }

    public String getCreateSessionTable(String tableName, String declareString) {
        return "CREATE TABLE #"+ tableName +" ("+ declareString + ")";
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
                                              "else"  + '\n' +
                                              "${field.name} = new ${field.type}(${field.read});";
    private final static String serWriteConc = "if(${field.name}.IsNull)" + '\n' +
            "w.Write(true);" + '\n' +
            "else {"  + '\n' +
            "w.Write(false);" + '\n' +
            "${field.write} }";
    
    private final static String passPrmsConc = "${field.name}";
    private String concTypeString;
    
    private static class GroupAggParse {
        public final String code;
        public final String[] serFields;

        private GroupAggParse(String code, String[] serFields) {
            this.code = code;
            this.serFields = serFields;
        }
    }
    private ImMap<GroupType, GroupAggParse> groupAggOrderStrings;

    private String compilerExe;
    @Override
    protected void ensureSystemFuncs() throws IOException, SQLException {
        compilerExe = SystemUtils.getExePath("cc", "/sql/mssql/", BusinessLogics.class);
        concTypeString = IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/ConcType.cs"));
        executeEnsure("EXEC sp_configure 'clr enabled', '1'");
        executeEnsure("RECONFIGURE");
        
//        ensureDTLFromResource("SQLUtils");
        groupAggOrderStrings = MapFact.toMap(
                GroupType.STRING_AGG, new GroupAggParse(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/StringAgg.cs")), new String[]{ null, null, "ov"}),
                GroupType.LAST, new GroupAggParse(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/LastValue.cs")), new String[]{ "lastValue", "lastOrder"}));

        executeEnsure(IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/aggf.sc")));

        ensuredGroupAggOrders = MapFact.mAddExclMap();
        ensureGroupAggOrder(new Pair<GroupType, ImList<Type>>(GroupType.LAST, ListFact.<Type>toList(StringClass.getv(40), IntegerClass.instance)));
    }

    @Override
    public boolean hasJDBCTimeoutMultiThreadProblem() {
        return false; // неизвестно пока
    }

    public String getSessionTableName(String tableName) {
        return "#"+ tableName;
    }

    public boolean isNullSafe() {
        return false;
    }

    public boolean isGreatest() {
        return false;
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String having, String top) {
        return "SELECT" + BaseUtils.clause("TOP", top) + " " + exprs + " FROM " + from + BaseUtils.clause("WHERE", where) + BaseUtils.clause("GROUP BY", groupBy) + BaseUtils.clause("HAVING", having) + BaseUtils.clause("ORDER BY", orderBy);
    }

    public String getUnionOrder(String union, String orderBy, String top) {
        if(top.length()==0)
            return union + BaseUtils.clause("ORDER BY", orderBy);
        return "SELECT" + BaseUtils.clause("TOP", top) + " * FROM (" + union + ") UALIAS" + BaseUtils.clause("ORDER BY", orderBy);
    }

    @Override
    public String getByteArrayType() {
        return "varbinary(max)";
    }
    @Override
    public int getByteArraySQL() {
        return Types.VARBINARY;
    }

    public String getCommandEnd() {
        return ";";
    }

    public String getFieldName(String fieldName) {
        return escapeID(fieldName);
    }

    public String getGlobalTableName(String tableName) {
        return escapeID(tableName);
    }

    private String escapeID(String ID) {
        return '"' + ID + '"';
    }

    @Override
    public boolean supportGroupSingleValue() {
        return false;
    }

    @Override
    public String getDateTimeType() {
        return "datetime";
    }

    @Override
    public String getAnyValueFunc() {
        return "MAX";
    }

    @Override
    public String getStringCFunc() {
        return "dbo.STRINGC";
    }

    @Override
    public String getLastFunc() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMaxMin(boolean max, String expr1, String expr2) {
        return "CASE WHEN " + expr1 + (max?">":"<") + expr2 + " THEN " + expr1 + " ELSE " + expr2 + " END";
    }

    @Override
    public String getNotZero(String expr) {
        return "CASE WHEN ABS(" + expr + ")>0.0005 THEN " + expr + " ELSE NULL END";
    }

    public void useDLL(){
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
    
    private static String getOrderGroupAggName(GroupType groupType, ImList<Type> types) {
        String fnc;
        switch (groupType) {
            case STRING_AGG:
                fnc = "STRING_AGG";
                break;
            case LAST:
                fnc = "LAST_VAL";
                break;
            default:
                throw new UnsupportedOperationException();
        }
            
        return fnc + "_" + genTypePostfix(types);
    }

    @Override
    public String getOrderGroupAgg(GroupType groupType, ImList<String> exprs, ImList<ClassReader> readers, ImOrderMap<String, CompileOrder> orders, TypeEnvironment typeEnv) {
        ImOrderMap<String, CompileOrder> filterOrders = orders.filterOrderValuesMap(new SFunctionSet<CompileOrder>() {
            public boolean contains(CompileOrder element) {
                return element.reader instanceof Type;
            }
        });
        ImOrderSet<String> sourceOrders = filterOrders.keyOrderSet();
        ImList<CompileOrder> compileOrders = filterOrders.valuesList();
        ImList<Type> orderTypes = BaseUtils.immutableCast(compileOrders.mapListValues(new GetValue<Reader, CompileOrder>() {
            public Reader getMapValue(CompileOrder value) {
                return value.reader;
            }}));
        boolean[] desc = new boolean[compileOrders.size()];
        for(int i=0,size=compileOrders.size();i<size;i++)
            desc[i] = compileOrders.get(i).desc;
        String orderSource;
        Type orderType;
        int size = orderTypes.size();
        if(size==0) {
            orderSource = "1";
            orderType = IntegerClass.instance;
        } else if(size==1 && !desc[0]) {
            orderSource = sourceOrders.single();
            orderType = orderTypes.single();
        } else {
            ConcatenateType concatenateType = ConcatenateType.get(orderTypes.toArray(new Type[size]), desc);
            orderSource = getNotSafeConcatenateSource(concatenateType, sourceOrders, typeEnv);
            orderType = concatenateType;
        }
        
        ImList<Type> fixedTypes;
        if(groupType == GroupType.STRING_AGG) { // будем считать что все implicit прокастится
            assert exprs.size() == 2;
            fixedTypes = ListFact.<Type>toList(StringClass.getv(ExtInt.UNLIMITED), StringClass.getv(ExtInt.UNLIMITED));
        } else {
            fixedTypes = readers.mapListValues(new GetValue<Type, ClassReader>() {
                public Type getMapValue(ClassReader value) {
                    if(value instanceof Type)
                        return (Type) value;
                    assert value instanceof NullReader;
                    return IntegerClass.instance;
                }
            });
        }
        fixedTypes = fixedTypes.addList(orderType);
        typeEnv.addNeedAggOrder(groupType, fixedTypes);

        return "dbo." + getOrderGroupAggName(groupType, fixedTypes) + "(" + exprs.toString(",") + "," + orderSource + ")";
    }

    protected MAddExclMap<Pair<GroupType, ImList<Type>>, Boolean> ensuredGroupAggOrders;// = MapFact.mAddExclMap();

    public void ensureGroupAggOrder(Pair<GroupType, ImList<Type>> groupAggOrder) throws SQLException {
        Boolean ensured = ensuredGroupAggOrders.get(groupAggOrder);
        if(ensured != null)
            return;

        Properties properties = new Properties();
        GroupType groupType = groupAggOrder.first;
        ImList<Type> types = groupAggOrder.second;
        String aggName = getOrderGroupAggName(groupType, types);
        properties.put("aggr.name", aggName);

        GroupAggParse parse = groupAggOrderStrings.get(groupType);

        StringBuilder read = new StringBuilder();
        StringBuilder write = new StringBuilder();
        for(int i=0,size=types.size();i<size;i++) {
            String name;
            if(i==size-1)
                name = "order";
            else
                name = "value" + (i==0?"":i);
            Type type = types.get(i);
            String serField = parse.serFields[i];

            String dotNetType = type.getDotNetType(this, recTypes);
            properties.put(name + ".type", dotNetType);
            properties.put(name + ".sqltype", getSQLType(type));
            appendSerialization(read, write, type, dotNetType, serField);
        }

        properties.put("ser.read", read.toString());
        properties.put("ser.write", write.toString());

        ensureDTL(new Pair<String, String>(aggName, stringResolver.replacePlaceholders(parse.code, properties)), false, types);

        ensuredGroupAggOrders.exclAdd(groupAggOrder, true);
    }

    private String getSQLType(Type type) {
        return type instanceof DateClass ? getDateTimeType() : type.getDB(this, recTypes);
    }

    private static void appendSerialization(StringBuilder read, StringBuilder write, Type type, String dotNetType, String serField) {
        if(serField !=null) {
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
            
            if(read.length() > 0)
                read.append('\n');
            read.append(fieldRead);
            if(write.length() > 0)
                write.append('\n');
            write.append(fieldWrite);
        }
    }

    @Override
    public String getNotSafeConcatenateSource(ConcatenateType type, ImList<String> exprs, TypeEnvironment typeEnv) {
        typeEnv.addNeedType(type);
        return "dbo." + genConcTypeName(type) + "(" + exprs.toString(",") + ")";
    }

    @Override
    public boolean isIndexNameLocal() {
        return true;
    }

    public ImList<Pair<String, String>> getTypesSources(ImList<Type> types, Set<String> refs) {
        MList<Pair<String, String>> mResult = ListFact.mList();        
        for(Type type : types)
            if(type instanceof ConcatenateType) {
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
        ImList<Type> types = concType.getTypes();
        boolean[] desc = concType.getDesc();
        StringBuilder read = new StringBuilder();
        StringBuilder write = new StringBuilder();
        for (int i=0,size=types.size();i<size;i++) {
            Properties properties = new Properties();
            Type type = types.get(i);
            String dotNetType = type.getDotNetType(this, recTypes);
            properties.put("field.type", dotNetType);
            String fieldName = ConcatenateType.getFieldName(i);
            properties.put("field.name", fieldName);
            properties.put("field.number", ""+i);
            properties.put("field.sqltype", getSQLType(type));
            properties.put("field.desc", desc[i] ? "-":"");
            
            declareFields =  (declareFields.length() == 0 ? "" : declareFields + '\n') + stringResolver.replacePlaceholders(declareFieldsConc, properties);
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

        String typeName = genConcTypeName(concType);
        properties.put("type.name", typeName);

        return new Pair<String, String>(typeName, stringResolver.replacePlaceholders(concTypeString, properties));
    }

//    public void ensureDTLFromResource(String resource) throws IOException, SQLException {
//        ensureDTL(resource, IOUtils.readStreamToString(BusinessLogics.class.getResourceAsStream("/sql/mssql/" + resource + ".cs")), false);
//    }
    
    public void ensureDTL(Pair<String, String> source, boolean toTempDb, ImList<Type> types) throws SQLException {
        Set<String> refs = new HashSet<String>();
        List<String> creates = new ArrayList<String>();
        List<String> drops = new ArrayList<String>();
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
        int i=0;
        String line;
        while(!(line = split[i++].trim()).equals("SQL DROP"))
            if(!line.isEmpty())
                creates.add(line);
        while(!(line = split[i++].trim()).equals("REFS"))
            if(!line.isEmpty())
                drops.add(line);
        while(!(line = split[i++].trim()).equals("CODE"))
            if(!line.isEmpty())
                refs.add(line);
        for(;i<split.length;i++) {
            sb.append(split[i]);
            sb.append(separator);
        }
        return new Pair<String, String>(source.first, sb.toString());
    }

    private synchronized void upload(String assemblyName, List<String> creates, List<String> drops, byte[] compiled, boolean toTempDb) throws SQLException {
        
        for(int i=0;i<2;i++) {
            if(i==0 && !toTempDb)
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

            for (String create : creates)
                executeEnsure(stringResolver.replacePlaceholders(create, properties));
        }
    }
    
    private void writeIntLE(DataOutputStream out, int v) throws IOException {
        out.write((v >>>  0) & 0xFF);
        out.write((v >>>  8) & 0xFF);
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
        for(Pair<String, String> source : sources) {
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

    @Override
    public boolean orderTopTrouble() { // будем считать базу умной
        return false;
    }

    public String getStringType(int length) {
        return "nchar(" + length + ")";
    }
    public int getStringSQL() {
        return Types.NCHAR;
    }

    @Override
    public String getVarStringType(int length) {
        return "nvarchar(" + length + ")";
    }
    @Override
    public int getVarStringSQL() {
        return Types.NVARCHAR;
    }

    @Override
    public String getRenameColumn(String table, String columnName, String newColumnName) {
        return "EXEC sp_rename '" + table + "." + columnName + "','" + newColumnName + "','COLUMN'";
    }
}
