package lsfusion.server.data.sql;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.base.lambda.Provider;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.query.exec.DynamicExecuteEnvironment;
import lsfusion.server.data.query.exec.MStaticExecuteEnvironment;
import lsfusion.server.data.query.exec.StaticExecuteEnvironment;
import lsfusion.server.data.query.exec.StaticExecuteEnvironmentImpl;
import lsfusion.server.data.query.exec.materialize.AdjustMaterializedExecuteEnvironment;
import lsfusion.server.data.query.exec.materialize.MaterializedQuery;
import lsfusion.server.data.query.exec.materialize.PureTime;
import lsfusion.server.data.query.exec.materialize.PureTimeInterface;
import lsfusion.server.data.query.result.ReadAllResultHandler;
import lsfusion.server.data.query.result.ResultHandler;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.exception.SQLTooLargeQueryException;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Cost;
import lsfusion.server.data.table.*;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.data.type.reader.NullReader;
import lsfusion.server.data.type.reader.Reader;
import lsfusion.server.logics.action.session.table.SessionTableUsage;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.commons.lang3.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

public class SQLQuery extends SQLCommand<ResultHandler<String, String>> {

    public SQLQuery(String command, Cost baseCost, boolean optAdjustLimit, ImMap<String, SQLQuery> subQueries, StaticExecuteEnvironment env, ImMap<String, ? extends Reader> keyReaders, ImMap<String, ? extends Reader> propertyReaders, boolean union, boolean recursionFunction) {
        super(command, baseCost, subQueries, env, recursionFunction);
        this.keyReaders = keyReaders;
        this.propertyReaders = propertyReaders;
        this.optAdjustLimit = optAdjustLimit;
        this.union = union;
    }

    final public boolean optAdjustLimit;
    public SQLQuery pessQuery; // не будем пока в конструктор добавлять, так как очень ограниченное использование

    public static ImMap<String, SQLQuery> translate(ImMap<String, SQLQuery> subQueries, final Function<String, String> translator) {
        return subQueries.mapValues(value -> value.translate(translator));
    }

    public SQLQuery translate(Function<String, String> translator) {
        SQLQuery result = new SQLQuery(translator.apply(command), baseCost, optAdjustLimit, translate(subQueries, translator), env, keyReaders, propertyReaders, union, recursionFunction);
        if(pessQuery != null)
            result.pessQuery = pessQuery.translate(translator);
        return result;
    }

    final public ImMap<String, ? extends Reader> keyReaders;
    final public ImMap<String, ? extends Reader> propertyReaders;
    final public boolean union;

    @Override
    protected boolean isRecursionFunction() {
        return recursionFunction;
    }

    protected boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && keyReaders.equals(((SQLQuery) o).keyReaders) && propertyReaders.equals(((SQLQuery) o).propertyReaders) && union == (((SQLQuery) o).union) && recursionFunction == (((SQLQuery) o).recursionFunction) && BaseUtils.nullEquals(pessQuery, ((SQLQuery) o).pessQuery);
    }

    @Override
    public int immutableHashCode() {
        return ((super.immutableHashCode() * 31 + keyReaders.hashCode()) * 31  + propertyReaders.hashCode()) * 31 + (union ? 1 : 0) + (recursionFunction ? 3 : 0) + (pessQuery!=null?pessQuery.hashCode():0);
    }

    public static <K> boolean hasUnlimited(ImMap<K, ? extends Reader> keyReaders) {
        for(Reader reader : keyReaders.valueIt())
            if(reader.getCharLength().isUnlimited())
                return true;
        return false;
    }

    public static <K> boolean hasTooLongKeys(ImMap<K, ? extends Reader> keyReaders) {
        for(Reader reader : keyReaders.valueIt())
            if(reader instanceof FileClass)
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
    public void afterExecute(ResultHandler<String, String> handler) {
    }

    @Override
    public void execute(PreparedStatement statement, ResultHandler<String, String> handler, SQLSession session) throws SQLException {

        SQLSyntax syntax = session.syntax;

        long pessLimit = Settings.get().getQueryRowCountPessLimit();// пессимистичная оценка, чтобы отсекать совсем маленькие
        long adjLimit = 0;
        long rowCount = 0;
        int rowSize = 0;
        boolean isNoQueryLimit = session.isNoQueryLimit();

        try (ResultSet result = statement.executeQuery()) {
            handler.start();

            while (result.next()) {
                ImValueMap<String, Object> mRowKeys = keyReaders.mapItValues(); // потому как exception есть
                for (int i = 0, size = keyReaders.size(); i < size; i++)
                    mRowKeys.mapValue(i, keyReaders.getValue(i).read(result, syntax, keyReaders.getKey(i)));
                ImValueMap<String, Object> mRowProperties = propertyReaders.mapItValues(); // потому как exception есть
                for (int i = 0, size = propertyReaders.size(); i < size; i++)
                    mRowProperties.mapValue(i, propertyReaders.getValue(i).read(result, syntax, propertyReaders.getKey(i)));
                handler.proceed(mRowKeys.immutableValue(), mRowProperties.immutableValue());

                if (!isNoQueryLimit && handler.hasQueryLimit() && rowCount++ > pessLimit) {
                    if (adjLimit == 0) {
                        rowSize = calculateRowSize(keyReaders, propertyReaders, handler.getPrevResults());
                        adjLimit = BaseUtils.max(getMemoryLimit() / rowSize, pessLimit);

                        ServerLoggers.exinfoLog("LARGE QUERY LIMIT " + adjLimit + " SIZE " + rowSize + " " + statement.toString());
                    }
                    if (rowCount > adjLimit) {
                        while (result.next()) {
                            rowCount++;
                        }
                        Throwables.propagate(new SQLTooLargeQueryException(rowCount, adjLimit, rowSize));
                    }
                }
            }

            if (adjLimit > 0)
                ServerLoggers.exInfoLogger.info("LARGE QUERY ROWS COUNT " + rowCount);

            handler.finish();
        }
    }

    public <K, V> void outSelect(ImMap<K, String> keys, ImMap<V, String> props, SQLSession session, DynamicExecuteEnvironment queryExecEnv, Object outerEnv, ImMap<String, ParseInterface> queryParams, int transactTimeout, boolean uniqueViolation, OperationOwner owner) throws SQLException, SQLHandledException {
        ServerLoggers.exinfoLog(this + " " + queryParams + '\n' + readSelect(keys, props, session, queryExecEnv, outerEnv, queryParams, transactTimeout, uniqueViolation, owner));
    }
    
    private static class ReadUniqueViolationResultHandler<K, V> implements ResultHandler<K, V> {
        public void start() {
        }
        
        private static final Object nullObject = new Object() {
            public String toString() {
                return "null";
            }
        };

        public void finish() throws SQLException {
        }

        private final MExclMap<ImMap<K, Object>, MMap<V, ImSet<Object>>> mExecResult = MapFact.mExclMap();

        public void proceed(ImMap<K, Object> rowKey, ImMap<V, Object> rowValue) throws SQLException {
            ImMap<V, ImSet<Object>> rowValueSet = rowValue.mapValues(value -> {
                return value == null ? SetFact.singleton(nullObject) : SetFact.singleton(value); // ImSet doesn't support nulls
            });

            MMap<V, ImSet<Object>> mValues = mExecResult.get(rowKey);
            if(mValues == null) {
                mValues = MapFact.mMap(rowValueSet, new SymmAddValue<V, ImSet<Object>>() {
                    public ImSet<Object> addValue(V key, ImSet<Object> prevValue, ImSet<Object> newValue) {
                        return prevValue.merge(newValue);
                    }
                });
                mExecResult.exclAdd(rowKey, mValues);
            } else
                mValues.addAll(rowValueSet);
        }

        public Provider<ImOrderMap<ImMap<K, Object>, ImMap<V, Object>>> getPrevResults() {
            throw new UnsupportedOperationException();
        }

        public boolean hasQueryLimit() {
            return false;
        }

        public ImMap<ImMap<K, Object>, ImMap<V, String>> terminate() {
            ImMap<ImMap<K, Object>, MMap<V, ImSet<Object>>> execResult = mExecResult.immutable();
            ImFilterValueMap<ImMap<K, Object>, ImMap<V, String>> result = execResult.mapFilterValues();
            for(int i=0,size=execResult.size();i<size;i++) {
                ImMap<V, ImSet<Object>> diffValues = execResult.getValue(i).immutable();
                final Result<Boolean> hasDifferentProps = new Result<>(false);
                ImMap<V, String> diffConcatValues = diffValues.mapItValues(value -> {
                    if (value.size() > 1)
                        hasDifferentProps.set(true);
                    return value.toString(",");
                });
                if(hasDifferentProps.result)
                    result.mapValue(i, diffConcatValues);
            }
            return result.immutableValue();
        }
    } 

    public <K, V> String readSelect(ImMap<K, String> keys, ImMap<V, String> props, SQLSession session, DynamicExecuteEnvironment queryExecEnv, Object outerEnv, ImMap<String, ParseInterface> queryParams, int transactTimeout, boolean uniqueViolation, OperationOwner owner) throws SQLException, SQLHandledException {
        ImMap<String, ? extends Reader> keyReaders = this.keyReaders;
        ImMap<String, ? extends Reader> propertyReaders = this.propertyReaders;

        ResultHandler<String, String> handler = uniqueViolation ? new ReadUniqueViolationResultHandler<String, String>() : new ReadAllResultHandler<String, String>();
        session.executeSelect(this, queryExecEnv, outerEnv, owner, queryParams, transactTimeout, handler);
        ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> result;
        if(uniqueViolation) {
            result = BaseUtils.immutableCast(((ReadUniqueViolationResultHandler<String, String>)handler).terminate().toOrderMap());
            propertyReaders = propertyReaders.keys().toMap(StringClass.text);
        } else
            result = ((ReadAllResultHandler<String, String>)handler).terminate();

        String resultString = "";
        if(result.isEmpty())
            return resultString;

        String name = "";
        for(int i=0,size=keys.size();i<size;i++)
            name += StringUtils.rightPad(keys.getKey(i).toString(), keyReaders.get(keys.getValue(i)).getCharLength().getAprValue()) + " ";
        for(int i=0,size=props.size();i<size;i++)
            name += StringUtils.rightPad(props.getKey(i).toString(), propertyReaders.get(props.getValue(i)).getCharLength().getAprValue()) + " ";
        resultString += name + '\n';

        for(int i=0,size=result.size();i<size;i++) {
            String rowName = "";

            ImMap<String, Object> keyMap = result.getKey(i);
            for(int j=0,sizeJ=keys.size();j<sizeJ;j++)
                rowName += StringUtils.rightPad(BaseUtils.nullToString(keyMap.get(keys.getValue(j))), keyReaders.get(keys.getValue(j)).getCharLength().getAprValue()) + " ";
            ImMap<String, Object> rowMap = result.getValue(i);
            for(int j=0,sizeJ=props.size();j<sizeJ;j++)
                rowName += StringUtils.rightPad(BaseUtils.nullToString(rowMap.get(props.getValue(j))), propertyReaders.get(props.getValue(j)).getCharLength().getAprValue()) + " ";

            resultString += rowName + '\n';

            if (resultString.length() > Settings.get().getOutSelectLengthThreshold()) {
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
            return new SQLQuery(fixConcSelect(command, keyReaders, propertyReaders, syntax, mEnv), baseCost, optAdjustLimit, subQueries, mEnv.finish(), keyReaders, propertyReaders, union, recursionFunction);
        }
        return this;
    }

    public SQLDML getInsertDML(String name, ImOrderSet<KeyField> keyFieldOrder, ImOrderSet<PropertyField> propertyFieldOrder, boolean orderPreserved, ImOrderSet<String> keySelectOrder, ImOrderSet<String> propertySelectOrder, SQLSyntax syntax) {
        String insertString = SetFact.addOrderExcl(keyFieldOrder, propertyFieldOrder).toString(Field.nameGetter(syntax), ",");

        MStaticExecuteEnvironment execEnv = StaticExecuteEnvironmentImpl.mEnv(this.env);
        return new SQLDML("INSERT INTO " + syntax.getSessionTableName(name) + " (" + (insertString.length() == 0 ? "dumb" : insertString) + ") " +
                getInsertSelect(orderPreserved, keySelectOrder, propertySelectOrder,
                        propertyFieldOrder, syntax, execEnv), baseCost, subQueries, execEnv.finish(), recursionFunction);
    }

    public String getInsertSelect(boolean orderPreserved, ImOrderSet<String> keyOrder, ImOrderSet<String> propertyOrder, ImOrderSet<PropertyField> fields, SQLSyntax syntax, MStaticExecuteEnvironment env) {
        boolean nullUnionTrouble = union && syntax.nullUnionTrouble();
        if (!orderPreserved || nullUnionTrouble) {
            final String alias = "ioalias";
            boolean casted = false;
            String exprs = keyOrder.toString(value -> alias + "." + value, ",");
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

    @StackMessage("{message.subquery.materialize}")
    @ThisMessage
    public MaterializedQuery materialize(final SQLSession session, final DynamicExecuteEnvironment subQueryExecEnv, final OperationOwner owner, final ImMap<SQLQuery, MaterializedQuery> materializedQueries, final ImMap<String, ParseInterface> queryParams, final int transactTimeout) throws SQLException, SQLHandledException {
        if(pessQuery != null && !Settings.get().isDisablePessQueries())
            return materializePessQuery(session, subQueryExecEnv, owner, materializedQueries, queryParams, transactTimeout);
        
        Result<Integer> actual = new Result<>();
        final MaterializedQuery.Owner tableOwner = new MaterializedQuery.Owner();

        final ImOrderSet<String> keys = keyReaders.keys().toOrderSet();
        final ImOrderSet<String> properties = propertyReaders.keys().toOrderSet();

        final ImOrderSet<KeyField> keyOrder = keys.mapOrder(SessionTableUsage.genKeys(keys,  NullReader.typeGetter(keyReaders)).reverse());
        final ImOrderSet<PropertyField> propOrder = properties.mapOrder(SessionTableUsage.genProps(properties, NullReader.typeGetter(propertyReaders)).reverse());

        final PureTime pureTime = new PureTime();
        String table = session.getTemporaryTable(keyOrder, propOrder.getSet(), new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException, SQLHandledException {
                SQLDML dml = getInsertDML(name, keyOrder, propOrder, false, keys, properties, session.syntax);
                SQLExecute execute = getExecute(dml, queryParams, subQueryExecEnv, materializedQueries, pureTime, transactTimeout, owner, tableOwner, SQLSession.register(name, tableOwner, TableChange.INSERT));
                return session.insertSessionSelect(execute, () -> outSelect(keyReaders.keys().toMap(), propertyReaders.keys().toMap(), session, subQueryExecEnv, materializedQueries, queryParams, transactTimeout, true, owner));
            }
        }, null, actual, tableOwner, owner);

        String mapFields = SQLSession.stringExpr(keys.mapSet(keyOrder.mapOrderSetValues(Field.nameGetter(session.syntax))),
                                                properties.mapSet(propOrder.mapOrderSetValues(Field.nameGetter(session.syntax))));
        return new MaterializedQuery(table, mapFields, SystemProperties.inDevMode ? keyOrder : null, SystemProperties.inDevMode ?  propOrder.getSet() : null, actual.result, pureTime.get(), tableOwner);
    }

    @StackMessage("{message.subquery.materialize.pess.mode}")
    public MaterializedQuery materializePessQuery(SQLSession session, DynamicExecuteEnvironment subQueryExecEnv, OperationOwner owner, ImMap<SQLQuery, MaterializedQuery> materializedQueries, ImMap<String, ParseInterface> queryParams, int transactTimeout) throws SQLException, SQLHandledException {
        return pessQuery.materialize(session, subQueryExecEnv, owner, materializedQueries, queryParams, transactTimeout);
    }

    private static SQLExecute getExecute(SQLDML dml, ImMap<String, ParseInterface> queryParams, DynamicExecuteEnvironment queryExecEnv, ImMap<SQLQuery, MaterializedQuery> materializedQueries, PureTimeInterface pureTime, int transactTimeout, OperationOwner owner, TableOwner tableOwner, RegisterChange registerChange) {
        if(queryExecEnv instanceof AdjustMaterializedExecuteEnvironment)
            return new SQLExecute<>(dml, queryParams, (AdjustMaterializedExecuteEnvironment) queryExecEnv, materializedQueries, pureTime, transactTimeout, owner, tableOwner, registerChange);
        return new SQLExecute(dml, queryParams, queryExecEnv, transactTimeout, owner, tableOwner, registerChange);
    }

    public static int countMatches(String command, String name, ImMap<String, SQLQuery> queries) {
        int result = StringUtils.countMatches(command, name);
        for(SQLQuery query : queries.valueIt()) {
            result += countMatches(query.command, name, query.subQueries);
        }
        return result;
    }

    public boolean isDML() {
        return false;
    }
}
