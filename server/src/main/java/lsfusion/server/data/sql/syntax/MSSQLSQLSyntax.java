package lsfusion.server.data.sql.syntax;

import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.interop.form.remote.serialization.BinarySerializable;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.expr.formula.SumFormulaImpl;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.compile.CompileOrder;
import lsfusion.server.data.query.exec.MStaticExecuteEnvironment;
import lsfusion.server.data.table.Field;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.FunctionType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeFunc;
import lsfusion.server.data.type.exec.EnsureTypeEnvironment;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.reader.ClassReader;
import lsfusion.server.data.type.reader.NullReader;
import lsfusion.server.logics.classes.data.ArrayClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.physics.admin.Settings;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MSSQLSQLSyntax extends DefaultSQLSyntax {

    public final static MSSQLSQLSyntax instance = new MSSQLSQLSyntax();

    private MSSQLSQLSyntax() {
    }

    public static String getArrayClassName(ArrayClass arrayClass) {
        return arrayClass.getSID();
    }

    public boolean allowViews() {
        return true;
    }

    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        return tableString + setString + " FROM " + fromString + whereString;
    }

    // в jtds драйвере есть непонятный баг, когда при многопоточной генерации чеков теряются временные таблицы 
    public String getClassName() {
//        return "net.sourceforge.jtds.jdbc.Driver";
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    public String isNULL(String exprs, boolean notSafe) {
        return "COALESCE(" + exprs + ")";
/*        if(notSafe)
        return "CASE WHEN "+ expr1 +" IS NULL THEN "+ expr2 +" ELSE "+ expr1 +" END";
    else
        return "ISNULL("+ expr1 +","+ expr2 +")";*/
    }

    public String getCreateSessionTable(String tableName, String declareString) {
        return "CREATE TABLE " + getSessionTableName(tableName) + " (" + declareString + ")";
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
    public boolean hasDriverCompositeProblem() {
        return true;
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

    @Override
    public String getSafeCastNameFnc(Type type, boolean isInt) {
        return "dbo." + super.getSafeCastNameFnc(type, isInt);
    }

    @Override
    public boolean hasJDBCTimeoutMultiThreadProblem() {
        return false; // неизвестно пока
    }

    public String getSessionTableName(String tableName) {
        return "#" + tableName;
    }

    public boolean isNullSafe() {
        return false;
    }

    public boolean isGreatest() {
        return false;
    }

    @Override
    public String getCancelActiveTaskQuery(Integer pid) {
        return String.format("KILL %s", pid);
    }

    @Override
    public String getAnalyze() {
        return "execute sp_updatestats 'resample'";
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
        return "datetime"; // return "datetime2"; правильнее было бы datetime2, но нет mapping'а на .net'ский тип 
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
    public String getMaxMin(boolean max, String expr1, String expr2, Type type, TypeEnvironment typeEnv) {
        if(!Settings.get().isUseMSSQLFuncWrapper())
            return "CASE WHEN " + expr1 + (max?">":"<") + expr2 + " THEN " + expr1 + " ELSE COALESCE(" + expr2 + "," + expr1 + ") END";

        TypeFunc typeFunc = max ? TypeFunc.MAX : TypeFunc.MIN;
        typeEnv.addNeedTypeFunc(typeFunc, type);
        return "dbo." + getTypeFuncName(typeFunc, type) + "(" + expr1 + "," + expr2 + ")";
    }

    @Override
    public String getNotZero(String expr, Type type, TypeEnvironment typeEnv) {
        if(!Settings.get().isUseMSSQLFuncWrapper())
            return "(CASE WHEN ABS(" + expr + ")>0.0005 THEN " + expr + " ELSE NULL END)";

        typeEnv.addNeedTypeFunc(TypeFunc.NOTZERO, type);
        return "dbo." + getTypeFuncName(TypeFunc.NOTZERO, type) + "(" + expr + ")";
    }

    @Override
    public String getAndExpr(String where, String expr, Type type, TypeEnvironment typeEnv) {
        if(!Settings.get().isUseMSSQLFuncWrapper())
            return super.getAndExpr(where, expr, type, typeEnv);

        typeEnv.addNeedTypeFunc(TypeFunc.ANDEXPR, type);
        return "dbo." + getTypeFuncName(TypeFunc.ANDEXPR, type) + "(CASE WHEN " + where + " THEN 1 ELSE 0 END," + expr + ")";
    }

    @Override
    public SQLSyntaxType getSyntaxType() {
        return SQLSyntaxType.MSSQL;
    }

    @Override
    public String getOrderGroupAgg(GroupType groupType, Type resultType, ImList<String> exprs, final ImList<ClassReader> readers, ImOrderMap<String, CompileOrder> orders, TypeEnvironment typeEnv) {
        ImOrderMap<String, CompileOrder> filterOrders = orders.filterOrderValuesMap(element -> element.reader instanceof Type);
        ImOrderSet<String> sourceOrders = filterOrders.keyOrderSet();
        ImList<CompileOrder> compileOrders = filterOrders.valuesList();
        ImList<Type> orderTypes = BaseUtils.immutableCast(compileOrders.mapListValues((CompileOrder value) -> value.reader));
        boolean[] desc = new boolean[compileOrders.size()];
        for(int i=0,size=compileOrders.size();i<size;i++)
            desc[i] = compileOrders.get(i).desc;
        String orderSource;
        Type orderType;
        int size = orderTypes.size();
        if(size==0) {
            orderSource = "1";
            orderType = ValueExpr.COUNTCLASS;
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
            StringClass textClass = StringClass.getv(ExtInt.UNLIMITED);
            fixedTypes = ListFact.toList(textClass, textClass);
            exprs = SumFormulaImpl.castToVarStrings(exprs, readers, resultType, this, typeEnv);
        } else {
            fixedTypes = readers.mapListValues((ClassReader value) -> {
                if(value instanceof Type)
                    return (Type) value;
                assert value instanceof NullReader;
                return NullReader.typeInstance;
            });
        }
        fixedTypes = fixedTypes.addList(orderType);
        typeEnv.addNeedAggOrder(groupType, fixedTypes);

        return "dbo." + getOrderGroupAggName(groupType, fixedTypes) + "(" + exprs.toString(",") + "," + orderSource + ")";
    }

    @Override
    public String getTypeChange(Type oldType, Type type, String name, MStaticExecuteEnvironment env) {
        return type.getDB(this, env);
    }

    public static String getOrderGroupAggName(GroupType groupType, ImList<Type> types) {
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

    public static String getTypeFuncName(TypeFunc typeFunc, Type type) {
        String prefix;
        switch (typeFunc) {
            case NOTZERO:
                prefix = "notZero";
                break;
            case MAX:
                prefix = "MAXF";
                break;
            case MIN:
                prefix = "MINF";
                break;
            case ANDEXPR:
                prefix = "and";
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return prefix + "_" + type.getSID();
    }

    @Override
    public String getNotSafeConcatenateSource(ConcatenateType type, ImList<String> exprs, TypeEnvironment typeEnv) {
        typeEnv.addNeedType(type);
        return "dbo." + getConcTypeName(type) + "(" + exprs.toString(",") + ")";
    }

    @Override
    public boolean isIndexNameLocal() {
        return true;
    }

    @Override
    public String getParamUsage(int num) {
        return "@prm" + num;
    }

    @Override
    public String getIIF(String ifWhere, String trueExpr, String falseExpr) {
        return "IIF(" + ifWhere + "," + trueExpr + "," + falseExpr + ")"; // есть ограничение на 10 вложенных case'ов
    }

    @Override
    public boolean orderTopProblem() { // будем считать базу умной
        return false;
    }

    public String getStringType(int length) {
        return "nchar(" + length + ")";
    }
    public int getStringSQL() {
        return Types.CHAR; // Types.NCHAR jtds не знает
    }

    @Override
    public String getVarStringType(int length) {
        return "nvarchar(" + length + ")";
    }
    @Override
    public int getVarStringSQL() {
        return Types.VARCHAR;  // Types.NVARCHAR jtds не знает
    }

    @Override
    public String getTextType() {
        return "nvarchar(max)";
    }

    @Override
    public int getTextSQL() {
        return Types.VARCHAR;  // Types.NVARCHAR jtds не знает
    }

    @Override
    public String getRenameColumn(String table, String columnName, String newColumnName) {
        return "EXEC sp_rename '" + table + "." + columnName + "','" + newColumnName + "','COLUMN'";
    }

    public static class Recursion implements BinarySerializable {
        public final ImList<FunctionType> types;
        public final String recName;
        public final String initialSelect;
        public final String stepSelect;
        public final String fieldDeclare;

        private Recursion(ImList<FunctionType> types, String recName, String initialSelect, String stepSelect, String fieldDeclare) {
            this.types = types;
            this.recName = recName;
            this.initialSelect = initialSelect;
            this.stepSelect = stepSelect;
            this.fieldDeclare = fieldDeclare;
        }

        public String getName() {
            return SystemUtils.generateID(this);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Recursion && fieldDeclare.equals(((Recursion) o).fieldDeclare) && initialSelect.equals(((Recursion) o).initialSelect) && recName.equals(((Recursion) o).recName) && stepSelect.equals(((Recursion) o).stepSelect) && types.equals(((Recursion) o).types);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * (31 * (31 * types.hashCode() + recName.hashCode()) + initialSelect.hashCode()) + stepSelect.hashCode()) + fieldDeclare.hashCode();
        }

        @Override
        public void write(DataOutputStream out) throws IOException {
            SystemUtils.write(out, types);
            out.writeUTF(recName);
            out.writeUTF(initialSelect);
            out.writeUTF(stepSelect);
            out.writeUTF(fieldDeclare);
        }
    }

    @Override
    public String getRecursion(ImList<FunctionType> types, String recName, String initialSelect, String stepSelect, String stepSmallSelect, int smallLimit, String fieldDeclare, String outerParams, TypeEnvironment typeEnv) {
        Recursion recursion = new Recursion(types, recName, initialSelect, stepSelect, fieldDeclare);
        typeEnv.addNeedRecursion(recursion);
        return "dbo." + recursion.getName() + "(" + outerParams+ ")";
    }

    @IdentityStrongLazy
    public String getTableTypeName(SessionTable.TypeStruct tableType) {
        return SystemUtils.generateID(tableType);
    }

    @Override
    public boolean noDynamicSQL() {
        return true;
    }

    @Override
    public boolean enabledCTE() {
        return false;
    }

    @Override
    public String getArrayConcatenate(ArrayClass arrayClass, String prm1, String prm2, TypeEnvironment env) {
        env.addNeedArrayClass(arrayClass);

        return prm1 + "." + "\"Add\"(" + prm2 + ")";
    }

    @Override
    public String getArrayAgg(String s, ClassReader classReader, TypeEnvironment env) {
        ArrayClass arrayClass = (ArrayClass) classReader;
        env.addNeedArrayClass(arrayClass);

        return "dbo.AGG_" + getArrayClassName(arrayClass) + "(" + s + ")";
    }

    @Override
    public String getArrayConstructor(String source, ArrayClass rowType, TypeEnvironment env) {
        env.addNeedArrayClass(rowType);

        return "dbo." + getArrayClassName(rowType) + "(" + source + ")";
    }

    @Override
    public String getArrayType(ArrayClass arrayClass, TypeEnvironment env) {
        env.addNeedArrayClass(arrayClass);
        return getArrayClassName(arrayClass);
    }

    @Override
    public String getInArray(String element, String array) {
        return array + ".\"Contains\"(" + element + ")!=0";
    }

    @Override
    public boolean doesNotTrimWhenCastToVarChar() {
        return true;
    }
    @Override
    public boolean doesNotTrimWhenSumStrings() {
        return true;
    }

    @Override
    public boolean hasGroupByConstantProblem() {
        return true;
    }

    @Override
    public boolean isUpdateConflict(SQLException e) {
        return e.getSQLState().equals("S0005") || e.getSQLState().equals("S0001") || e.getSQLState().equals("S0002");
    }

    @Override
    public boolean isDeadLock(SQLException e) {
        return e.getSQLState().equals("40001");
    }

    @Override
    public boolean isUniqueViolation(SQLException e) {
        return e.getSQLState().equals("23000");
    }

    @Override
    public String getDateTimeMillis() {
        return "DATEADD(millisecond, DATEDIFF(millisecond, 0, GETDATE()), 0)";
    }

    @Override
    public String getDateTime() {
        return "DATEADD(second, DATEDIFF(second, 0, GETDATE()), 0)";
    } 
    
    @Override
    public String getRandom() {
        return "dbo.RandFromInt(dbo.currentTransID())";
    }

    private static Date minDate;
    private static Timestamp minTimestamp;
    static {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(1753, Calendar.JANUARY, 1);
        minDate = new Date(cal.getTimeInMillis());
        minTimestamp = new Timestamp(cal.getTimeInMillis());
    }

    @Override
    public Date fixDate(Date value) {
        if(value.compareTo(minDate) < 0)
            return minDate;
        return value;
    }

    @Override
    public Timestamp fixDateTime(Timestamp value) {
        if(value.compareTo(minTimestamp) < 0)
            return minTimestamp;
        return value;
    }

    @Override
    public boolean supportsNoCount() {
        return true;
    }

    @Override
    public String getDeadlockPriority(Long priority) {
        return "SET DEADLOCK_PRIORITY " + (priority != null ? BaseUtils.min(priority, 10) : "NORMAL");
    }

    // треш конечно, нужно отличать только использованные в рекурсиях таблицы, но пока так грубо
    @Override
    public String getQueryName(String tableName, SessionTable.TypeStruct struct, StringBuilder envString, boolean usedRecursion, EnsureTypeEnvironment typeEnv) {
        String table = super.getQueryName(tableName, struct, envString, usedRecursion, typeEnv);
        if(usedRecursion) {
            ImOrderSet<Field> fields = struct.getFields();
            String varTable = "@" + tableName;
            envString.append("SET NOCOUNT ON" + '\n');
            envString.append("DECLARE " + varTable + " AS " + struct.getDB(this, typeEnv) + '\n');
            String fst = fields.toString(Field.nameGetter(this), ",");
            envString.append("INSERT INTO " + varTable + " (" + fst + ") SELECT " + fst + " FROM " + table + '\n');
            envString.append("SET NOCOUNT OFF" + '\n');
            return varTable;
        }
        return table;
    }
}
