package lsfusion.server.data;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.data.query.StaticExecuteEnvironment;
import lsfusion.server.data.query.stat.ExecCost;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLDML extends SQLCommand<Result<Integer>> {

    public SQLDML(String command, ExecCost baseCost, ImMap<String, SQLQuery> subQueries, StaticExecuteEnvironment env) {
        super(command, baseCost, subQueries, env);
    }

    public void execute(PreparedStatement statement, Result<Integer> handler, SQLSession session) throws SQLException {
        int result = statement.executeUpdate();
        if(Settings.get().isSaleInvoiceDetailLog()) {
            String s = statement.toString();
            if(s.startsWith("UPDATE SaleLedger_saleLedger"))
                ServerLoggers.exinfoLog("UPDATESALELEDGER : " + s + " " + result);
        }
        handler.set(result);
    }
}
