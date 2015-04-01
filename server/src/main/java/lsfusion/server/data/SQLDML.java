package lsfusion.server.data;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.query.StaticExecuteEnvironment;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLDML extends SQLCommand<Result<Integer>> {

    public SQLDML(String command, ImMap<String, SQLQuery> subQueries, StaticExecuteEnvironment env) {
        super(command, subQueries, env);
    }

    public void execute(PreparedStatement statement, Result<Integer> handler, SQLSession session) throws SQLException {
        handler.set(statement.executeUpdate());
    }
}
