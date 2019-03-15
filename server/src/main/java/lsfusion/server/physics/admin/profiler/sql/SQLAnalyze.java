package lsfusion.server.physics.admin.profiler.sql;

import lsfusion.base.lambda.Provider;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.sql.SQLCommand;
import lsfusion.server.data.sql.SQLDML;
import lsfusion.server.data.sql.SQLSession;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLAnalyze extends SQLCommand<SQLDML.Handler> {

    private final boolean dml;
    public SQLAnalyze(SQLCommand command, boolean noAnalyze) {
        super("EXPLAIN (" + (noAnalyze ? "VERBOSE, COSTS" : "ANALYZE, VERBOSE, BUFFERS, COSTS") + ") " + command.command, command.baseCost, command.subQueries, command.env, command.recursionFunction);
        this.noAnalyze = noAnalyze;
        this.dml = command instanceof SQLDML; // в twins hashcode не включаем так как следует из самой команды
    }

    private final boolean noAnalyze;

    protected boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && noAnalyze == ((SQLAnalyze)o).noAnalyze;
    }

    public int immutableHashCode() {
        return super.immutableHashCode() * 31 + (noAnalyze ? 1 : 0);
    }

    public void execute(PreparedStatement statement, SQLDML.Handler handler, SQLSession session) throws SQLException {
        handler.proceed(session.executeExplain(statement, noAnalyze, dml, new Provider<String>() {
            public String get() {
                return getFullText();
            }
        }));
    }

    public void afterExecute(SQLDML.Handler handler) {
        handler.afterProceed();
    }

    @Override
    public boolean isDML() {
        return dml;
    }
}
