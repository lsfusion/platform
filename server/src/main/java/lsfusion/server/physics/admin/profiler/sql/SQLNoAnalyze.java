package lsfusion.server.physics.admin.profiler.sql;

import lsfusion.server.data.sql.SQLCommand;
import lsfusion.server.data.sql.SQLDML;
import lsfusion.server.data.sql.SQLSession;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Runs EXPLAIN (VERBOSE, COSTS) before the actual query and stores the output in a list
// instead of logging it immediately.  The caller logs the stored lines if the subsequent
// query turns out to be slow or does not complete (hangs).
public class SQLNoAnalyze extends SQLAnalyze {

    private final List<String> lines = new ArrayList<>();
    private final List<String> compileLines = new ArrayList<>();

    public SQLNoAnalyze(SQLCommand command) {
        super(command, true); // noAnalyze=true → EXPLAIN (VERBOSE, COSTS)
    }

    @Override
    public void execute(PreparedStatement statement, SQLDML.Handler handler, SQLSession session) throws SQLException {
        handler.proceed(session.executeExplain(statement, true, isDML(), this::getFullText, lines, compileLines));
    }

    public List<String> getLines() {
        return lines;
    }

    public List<String> getCompileLines() {
        return compileLines;
    }
}
