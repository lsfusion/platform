package lsfusion.server.data;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.query.StaticExecuteEnvironment;
import lsfusion.server.data.query.stat.Cost;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLDML extends SQLCommand<SQLDML.Handler> {

    public interface Handler {

        Handler VOID = new Handler() {
            public void proceed(Integer result) {
            }

            public void afterProceed() {
            }
        };

        void proceed(Integer result);

        void afterProceed();
    }

    public SQLDML(String command, Cost baseCost, ImMap<String, SQLQuery> subQueries, StaticExecuteEnvironment env, boolean recursionFunction) {
        super(command, baseCost, subQueries, env, recursionFunction);
    }

    public void execute(PreparedStatement statement, SQLDML.Handler handler, SQLSession session) throws SQLException {
        int result = statement.executeUpdate();
        handler.proceed(result);
    }

    @Override
    public void afterExecute(Handler handler) {
        handler.afterProceed();
    }

    @Override
    public boolean isDML() {
        return true;
    }
}
