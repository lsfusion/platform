package lsfusion.server.data;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.ExtInt;
import lsfusion.base.Provider;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.data.query.*;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.*;
import org.apache.commons.lang.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLQuery extends SQLCommand<ResultHandler<String, String>> {

    public SQLQuery(String command, ImMap<String, SQLQuery> subQueries, StaticExecuteEnvironment env, ImMap<String, ? extends Reader> keyReaders, ImMap<String, ? extends Reader> propertyReaders, boolean union, boolean recursionFunction) {
        super(command, subQueries, env);
        this.keyReaders = keyReaders;
        this.propertyReaders = propertyReaders;
        this.union = union;
        this.recursionFunction = recursionFunction;
    }

    public static ImMap<String, SQLQuery> translate(ImMap<String, SQLQuery> subQueries, final GetValue<String, String> translator) {
        return subQueries.mapValues(new GetValue<SQLQuery, SQLQuery>() {
            public SQLQuery getMapValue(SQLQuery value) {
                return value.translate(translator);
            }
        });
    }
    public SQLQuery translate(GetValue<String, String> translator) {
        return new SQLQuery(translator.getMapValue(command), translate(subQueries, translator), env, keyReaders, propertyReaders, union, recursionFunction);
    }

    final public ImMap<String, ? extends Reader> keyReaders;
    final public ImMap<String, ? extends Reader> propertyReaders;
    final public boolean union;
    final private boolean recursionFunction;

    @Override
    protected boolean isRecursionFunction() {
        return recursionFunction;
    }

    protected boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && keyReaders.equals(((SQLQuery) o).keyReaders) && propertyReaders.equals(((SQLQuery) o).propertyReaders) && union == (((SQLQuery) o).union) && recursionFunction == (((SQLQuery) o).recursionFunction);
    }

    @Override
    public int immutableHashCode() {
        return ((super.immutableHashCode() * 31 + keyReaders.hashCode()) * 31  + propertyReaders.hashCode()) * 31 + (union ? 1 : 0) + (recursionFunction ? 3 : 0);
    }

    private <K> boolean hasUnlimited(ImMap<K, ? extends Reader> keyReaders) {
        for(Reader reader : keyReaders.valueIt())
            if(reader.getCharLength().isUnlimited())
                return true;
        return false;
    }

    private <K, V> int calculateRowSize(ImMap<K, ? extends Reader> keyReaders, ImMap<V, ? extends Reader> propertyReaders, Provider<ImOrderMap<ImMap<K, Object>, ImMap<V, Object>>> mExecResult) {

        ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execResult = null;
        if(hasUnlimited(keyReaders) || hasUnlimited(propertyReaders)) // оптимизация
            execResult = mExecResult.get();

        return calculateRowSize(keyReaders, execResult == null ? null :  execResult.keyIt()) +
                calculateRowSize(propertyReaders, execResult == null ? null :  execResult.valueIt());
    }

    private <K> int calculateRowSize(ImMap<K, ? extends Reader> keyReaders, Iterable<ImMap<K, Object>> keys) {
        int rowSize = 0;
        for(int i=0, size = keyReaders.size();i<size;i++) {
            Reader reader = keyReaders.getValue(i);
            ExtInt length = reader.getCharLength();
            if(length.isUnlimited()) {
                K key = keyReaders.getKey(i);
                int proceededSize = 0; int total = 0;
                for(ImMap<K, Object> keyValue : keys) {
                    Object value = keyValue.get(key);
                    if(value != null)
                        proceededSize += reader.getSize(value);
                    total++;
                }
                rowSize += (proceededSize  / total);
            } else
                rowSize += length.getValue();
        }
        return rowSize;
    }


    private static long getMemoryLimit() {
        return Runtime.getRuntime().maxMemory() / Settings.get().getQueryRowCountOptDivider(); // 0.05
    }

    @Override
    public void execute(PreparedStatement statement, ResultHandler<String, String> handler, SQLSession session) throws SQLException {
        final ResultSet result = statement.executeQuery();

        SQLSyntax syntax = session.syntax;

        long pessLimit = Settings.get().getQueryRowCountPessLimit();// пессимистичная оценка, чтобы отсекать совсем маленькие
        long adjLimit = 0;
        long rowCount = 0;
        int rowSize = 0;
        boolean isNoQueryLimit = session.isNoQueryLimit();

        try {
            handler.start();

            while(result.next()) {
                ImValueMap<String, Object> mRowKeys = keyReaders.mapItValues(); // потому как exception есть
                for(int i=0,size=keyReaders.size();i<size;i++)
                    mRowKeys.mapValue(i, keyReaders.getValue(i).read(result, syntax, keyReaders.getKey(i)));
                ImValueMap<String, Object> mRowProperties = propertyReaders.mapItValues(); // потому как exception есть
                for(int i=0,size=propertyReaders.size();i<size;i++)
                    mRowProperties.mapValue(i, propertyReaders.getValue(i).read(result, syntax, propertyReaders.getKey(i)));
                handler.proceed(mRowKeys.immutableValue(), mRowProperties.immutableValue());

                if(!isNoQueryLimit && rowCount++ > pessLimit) {
                    if(adjLimit == 0) {
                        rowSize = calculateRowSize(keyReaders, propertyReaders, handler.getPrevResults());
                        adjLimit = BaseUtils.max(getMemoryLimit() / rowSize, pessLimit);

                        ServerLoggers.exinfoLog("LARGE QUERY LIMIT " + adjLimit + " SIZE " + rowSize + " " + statement.toString());
                    }
                    if(rowCount > adjLimit) {
                        while(result.next()) {
                            rowCount++;
                        }
                        Throwables.propagate(new SQLTooLargeQueryException(rowCount, adjLimit, rowSize));
                    }
                }
            }

            if(adjLimit > 0)
                ServerLoggers.exInfoLogger.info("LARGE QUERY ROWS COUNT " + rowCount);

            handler.finish();
        } finally {
            result.close();
        }
    }

    public String readSelect(SQLSession session, QueryEnvironment env, ImMap<String, ParseInterface> queryParams, DynamicExecuteEnvironment queryExecEnv) throws SQLException, SQLHandledException {
        // выведем на экран
        ReadAllResultHandler<String, String> handler = new ReadAllResultHandler<String, String>();
        session.executeSelect(this, env.getOpOwner(), queryParams, queryExecEnv, env.getTransactTimeout(), handler);
        ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> result = handler.terminate();

        String resultString = "";
        if(result.isEmpty())
            return resultString;

        String name = "";
        for(int i=0,size=keyReaders.size();i<size;i++)
            name += StringUtils.rightPad(keyReaders.getKey(i), keyReaders.getValue(i).getCharLength().getAprValue());
        for(int i=0,size=propertyReaders.size();i<size;i++)
            name += StringUtils.rightPad(propertyReaders.getKey(i), propertyReaders.getValue(i).getCharLength().getAprValue());
        resultString += name + '\n';

        for(int i=0,size=result.size();i<size;i++) {
            String rowName = "";

            ImMap<String, Object> keyMap = result.getKey(i);
            for(int j=0,sizeJ=keyMap.size();j<sizeJ;j++)
                rowName += StringUtils.rightPad(BaseUtils.nullToString(keyMap.getValue(j)), keyReaders.get(keyMap.getKey(j)).getCharLength().getAprValue());
            ImMap<String, Object> rowMap = result.getValue(i);
            for(int j=0,sizeJ=rowMap.size();j<sizeJ;j++)
                rowName += StringUtils.rightPad(BaseUtils.nullToString(rowMap.getValue(j)), propertyReaders.get(rowMap.getKey(j)).getCharLength().getAprValue());

            resultString += rowName + '\n';

            if (resultString.length() > 1000000) {
                resultString += "and more...\n";
                break;
            }
        }
        return resultString;
    }

    // оптимизация
    private static <K> boolean hasConcColumns(ImMap<K, ? extends Reader> colReaders) {
        for(int i=0,size= colReaders.size();i<size;i++)
            if(colReaders.getValue(i) instanceof ConcatenateType)
                return true;
        return false;
    }

    private static <K, V> boolean hasConc(ImMap<K, ? extends Reader> keyReaders, ImMap<V, ? extends Reader> propertyReaders) {
        return hasConcColumns(keyReaders) || hasConcColumns(propertyReaders);
    }

    private static ImMap<String, String> fixConcColumns(ImMap<String, ? extends Reader> colReaders, SQLSyntax syntax, TypeEnvironment env) {
        MExclMap<String, String> mReadColumns = MapFact.mExclMap();
        for(int i=0,size=colReaders.size();i<size;i++) {
            String keyName = colReaders.getKey(i);
            colReaders.getValue(i).readDeconc(keyName, keyName, mReadColumns, syntax, env);
        }
        return mReadColumns.immutable();
    }

    private static String fixConcSelect(String select, ImMap<String, ? extends Reader> keyReaders, ImMap<String, ? extends Reader> propertyReaders, SQLSyntax syntax, TypeEnvironment env) {
        return "SELECT " + SQLSession.stringExpr(fixConcColumns(keyReaders, syntax, env), fixConcColumns(propertyReaders, syntax, env)) + " FROM (" + select + ") s";
    }

    public SQLQuery fixConcSelect(SQLSyntax syntax) {
        if(syntax.hasDriverCompositeProblem() && hasConc(keyReaders, propertyReaders)) {
            MStaticExecuteEnvironment mEnv = StaticExecuteEnvironmentImpl.mEnv(env);
            return new SQLQuery(fixConcSelect(command, keyReaders, propertyReaders, syntax, mEnv), subQueries, mEnv.finish(), keyReaders, propertyReaders, union, false);
        }
        return this;
    }

    public String getInsertSelect(boolean orderPreserved, ImOrderSet<String> keyOrder, ImOrderSet<String> propertyOrder, ImOrderSet<PropertyField> fields, SQLSyntax syntax, MStaticExecuteEnvironment env) {
        boolean nullUnionTrouble = union && syntax.nullUnionTrouble();
        if (!orderPreserved || nullUnionTrouble) {
            final String alias = "ioalias";
            boolean casted = false;
            String exprs = keyOrder.toString(new GetValue<String, String>() {
                public String getMapValue(String value) {
                    return alias + "." + value;
                }
            }, ",");
            for (int i = 0, size = propertyOrder.size(); i < size; i++) { // последействие
                String propertyField = propertyOrder.get(i);
                String propertyExpr = alias + "." + propertyField;
                if (nullUnionTrouble && propertyReaders.get(propertyField) instanceof NullReader) { // если null, вставляем явный cast
                    casted = true;
                    propertyExpr = fields.get(i).type.getCast(propertyExpr, syntax, env);
                }
                exprs = (exprs.length() == 0 ? "" : exprs + ",") + propertyExpr;
            }
            if(!orderPreserved || casted)
                return "SELECT " + exprs + " FROM (" + command + ") " + alias;
        }

        return command;
    }

    public StaticExecuteEnvironment getEnv() {
        return env;
    }
}
