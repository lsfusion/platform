package lsfusion.server.data.sql;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.sql.statement.PreParsedStatement;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.query.compile.CompiledQuery;
import lsfusion.server.data.type.exec.EnsureTypeEnvironment;
import lsfusion.server.data.query.exec.materialize.MaterializedQuery;
import lsfusion.server.data.query.exec.StaticExecuteEnvironment;
import lsfusion.server.data.expr.join.stat.Cost;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;

// SQL команда внутри которой есть запрос
public abstract class SQLCommand<H> extends TwinImmutableObject<SQLCommand<H>> {

    public final String command;
    public final Cost baseCost;
    public final ImMap<String, SQLQuery> subQueries;
    public final StaticExecuteEnvironment env;

    public final boolean recursionFunction; // subQueries все внутри '' идут

    @IdentityLazy
    public Cost getCost(ImMap<SQLQuery, Stat> materializedQueries) {
        Cost result = baseCost;
        for(SQLQuery subQuery : subQueries.valueIt()) {
            Cost subQueryCost;
            Stat matStat = materializedQueries.get(subQuery);
            if(matStat != null)
                subQueryCost = new Cost(matStat);
            else
                subQueryCost = subQuery.getCost(materializedQueries);
            result = result.or(subQueryCost);
        }
        return result;
    }

    public SQLCommand(String command, Cost baseCost, ImMap<String, SQLQuery> subQueries, StaticExecuteEnvironment env, boolean recursionFunction) {
        this.command = command;
        this.baseCost = baseCost;
        this.subQueries = subQueries;
        this.env = env;
        this.recursionFunction = recursionFunction;
    }

    protected boolean isRecursionFunction() {
        return recursionFunction;
    }

    public PreParsedStatement preparseStatement(final boolean parseParams, final ImMap<String, ParseInterface> paramObjects, final SQLSyntax syntax, final boolean isVolatileStats, final ImMap<SQLQuery, MaterializedQuery> materializedQueries, final boolean usedRecursion) {
        final StringBuilder envString = new StringBuilder();
        final boolean recursionFunction = isRecursionFunction();

        final EnsureTypeEnvironment ensureTypes = env.getEnsureTypes();

        ImMap<String, ParsedString> parsedSubQueries = subQueries.mapValues(new GetValue<ParsedString, SQLQuery>() {
            public ParsedString getMapValue(SQLQuery value) {
                MaterializedQuery matQuery = materializedQueries.get(value);
                if(matQuery != null)
                    return new ParsedString(matQuery.getParsedString(syntax, envString, usedRecursion, ensureTypes));

                ParsedParamString result = value.preparseStatement(parseParams, paramObjects, syntax, isVolatileStats, materializedQueries, usedRecursion).getString(syntax);
                if (recursionFunction)
                    result = result.wrapSubQueryRecursion(syntax);
                return result;

            }
        });

        return preparseStatement(command, parseParams, paramObjects, syntax, isVolatileStats, usedRecursion, envString, recursionFunction, parsedSubQueries, ensureTypes);
    }

    public static PreParsedStatement preparseStatement(String command, ImMap<String, ParseInterface> paramObjects, SQLSyntax syntax) {
        return preparseStatement(command, false, paramObjects, syntax, false, false, new StringBuilder(), false, MapFact.<String, ParsedString>EMPTY(), null); // CONCATENATE'ов нет, поэтому ensureTypes null
    }
    
    private static PreParsedStatement preparseStatement(String command, boolean parseParams, ImMap<String, ParseInterface> paramObjects, SQLSyntax syntax, boolean isVolatileStats, boolean usedRecursion, StringBuilder envString, boolean recursionFunction, ImMap<String, ParsedString> parsedSubQueries, EnsureTypeEnvironment ensureTypes) {
        ImFilterValueMap<String, ParsedString> mvSafeStrings = paramObjects.mapFilterValues();
        ImFilterValueMap<String, Type> mvNotSafeTypes = paramObjects.mapFilterValues();
        for(int i=0,size=paramObjects.size();i<size;i++) {
            ParseInterface parseInterface = paramObjects.getValue(i);
            if(parseInterface.isSafeString() && !(parseParams && parseInterface instanceof TypeObject)) {
                String string = parseInterface.getString(syntax, envString, usedRecursion);
                if(recursionFunction && parseInterface.isAlwaysSafeString()) // ignoring noDynamicSQL, because of identity wrapSubQueryRecursion in that case 
                    string = syntax.wrapSubQueryRecursion(string); // outerparams should not be escaped, and all the others should 
                mvSafeStrings.mapValue(i, new ParsedString(string));
            }
            if(!parseInterface.isSafeType())
                mvNotSafeTypes.mapValue(i, parseInterface.getType());
        }
        ImMap<String, ParsedString> safeStrings = mvSafeStrings.immutableValue().addExcl(parsedSubQueries);
        ImSet<String> params = paramObjects.keys().addExcl(parsedSubQueries.keys());
        return new PreParsedStatement(command, params, safeStrings, mvNotSafeTypes.immutableValue(), isVolatileStats, envString.toString(), ensureTypes);
    }

    public String toString() {
        return command;
    }

    // assert'ся что все остальные данные (noAnalyze, mappинг) - внутри запроса
    protected boolean calcTwins(TwinImmutableObject o) {
        return command.equals(((SQLCommand<H>)o).command) && subQueries.equals(((SQLCommand<H>)o).subQueries);
    }

    public int immutableHashCode() {
        return command.hashCode() * 31 + subQueries.hashCode();
    }

    public String getString() {
        return command;
    }

    // session для настроек а не выполнения
    public abstract void execute(PreparedStatement statement, H handler, SQLSession session) throws SQLException;

    // когда lockRead остался, но все остальное уже выполнено
    public abstract void afterExecute(H handler);

    public boolean useVolatileStats() {
        return command.length() > Settings.get().getCommandLengthVolatileStats();
    }

    public int getLength() {
        int result = command.length();
        for(SQLQuery subQuery : subQueries.valueIt())
            result += subQuery.getLength();
        return result;
    }

    public String getFullText() {
        return CompiledQuery.translateParam(command, subQueries.mapValues(new GetValue<String, SQLQuery>() {
            public String getMapValue(SQLQuery value) {
                return value.getFullText();
            }
        }));
    }

    public abstract boolean isDML();
}
