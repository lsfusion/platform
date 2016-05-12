package lsfusion.server.data;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.server.Settings;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.MaterializedQuery;
import lsfusion.server.data.query.StaticExecuteEnvironment;
import lsfusion.server.data.query.stat.ExecCost;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;

// SQL команда внутри которой есть запрос
public abstract class SQLCommand<H> extends TwinImmutableObject<SQLCommand<H>> {

    protected final String command;
    public final ExecCost baseCost;
    public final ImMap<String, SQLQuery> subQueries;
    protected final StaticExecuteEnvironment env;

    protected final boolean recursionFunction; // subQueries все внутри '' идут

    @IdentityLazy
    public ExecCost getCost(ImMap<SQLQuery, Stat> materializedQueries) {
        ExecCost result = baseCost;
        for(SQLQuery subQuery : subQueries.valueIt()) {
            ExecCost subQueryCost;
            Stat matStat = materializedQueries.get(subQuery);
            if(matStat != null)
                subQueryCost = new ExecCost(matStat);
            else
                subQueryCost = subQuery.getCost(materializedQueries);
            result = result.or(subQueryCost);
        }
        return result;
    }

    public SQLCommand(String command, ExecCost baseCost, ImMap<String, SQLQuery> subQueries, StaticExecuteEnvironment env, boolean recursionFunction) {
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

        ImMap<String, ParsedString> parsedSubQueries = subQueries.mapValues(new GetValue<ParsedString, SQLQuery>() {
            public ParsedString getMapValue(SQLQuery value) {
                MaterializedQuery matQuery = materializedQueries.get(value);
                if(matQuery != null)
                    return new ParsedString(matQuery.getParsedString(syntax, envString, usedRecursion));

                ParsedParamString result = value.preparseStatement(parseParams, paramObjects, syntax, isVolatileStats, materializedQueries, usedRecursion).getString(syntax);
                if (recursionFunction)
                    result = result.wrapSubQueryRecursion(syntax);
                return result;

            }
        });

        ImFilterValueMap<String, ParsedString> mvSafeStrings = paramObjects.mapFilterValues();
        ImFilterValueMap<String, Type> mvNotSafeTypes = paramObjects.mapFilterValues();
        for(int i=0,size=paramObjects.size();i<size;i++) {
            ParseInterface parseInterface = paramObjects.getValue(i);
            if(parseInterface.isSafeString() && !(parseParams && parseInterface instanceof TypeObject)) {
                String string = parseInterface.getString(syntax, envString, usedRecursion);
                if(recursionFunction)
                    string = syntax.wrapSubQueryRecursion(string);
                mvSafeStrings.mapValue(i, new ParsedString(string));
            }
            if(!parseInterface.isSafeType())
                mvNotSafeTypes.mapValue(i, parseInterface.getType());
        }
        ImMap<String, ParsedString> safeStrings = mvSafeStrings.immutableValue().addExcl(parsedSubQueries);
        ImSet<String> params = paramObjects.keys().addExcl(parsedSubQueries.keys());
        return new PreParsedStatement(command, params, safeStrings, mvNotSafeTypes.immutableValue(), isVolatileStats, envString.toString(), env.getEnsureTypes());
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
}
