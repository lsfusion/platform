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
            if(s.startsWith("DELETE FROM SaleLedger_saleLedger"))
                ServerLoggers.exinfoLog("DELETESALELEDGER : " + s + " " + result);
            if(s.startsWith("INSERT INTO SaleLedger_saleLedger"))
                ServerLoggers.exinfoLog("INSERTINTOSALELEDGER : " + s + " " + result);
            if(s.startsWith("UPDATE Sale_invoiceDetail"))
                ServerLoggers.exinfoLog("UPDATESALEINVOICEDETAIL : " + s + " " + result);
            if(s.startsWith("DELETE FROM Sale_invoiceDetail"))
                ServerLoggers.exinfoLog("DELETESALEINVOICEDETAIL : " + s + " " + result);
            if(s.startsWith("INSERT INTO Sale_invoiceDetail"))
                ServerLoggers.exinfoLog("INSERTINTOSALEINVOICEDETAIL : " + s + " " + result);
        }
        handler.set(result);
    }
}
