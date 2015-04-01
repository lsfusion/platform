package lsfusion.server.data.sql;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLCommand;
import lsfusion.server.data.SQLDML;
import lsfusion.server.data.TableOwner;
import lsfusion.server.data.query.StaticExecuteEnvironment;
import lsfusion.server.data.query.DynamicExecuteEnvironment;
import lsfusion.server.data.type.ParseInterface;

public class SQLExecute {

    public final SQLDML command;
    public final ImMap<String, ParseInterface> params;
    public final DynamicExecuteEnvironment queryExecEnv;
    public final int transactTimeout;
    public final OperationOwner owner;
    public final TableOwner tableOwner;

    public SQLExecute(SQLDML command, ImMap<String, ParseInterface> params, DynamicExecuteEnvironment queryExecEnv, int transactTimeout, OperationOwner owner, TableOwner tableOwner) {
        this.command = command;
        this.params = params;
        this.queryExecEnv = queryExecEnv;
        this.transactTimeout = transactTimeout;
        this.owner = owner;
        this.tableOwner = tableOwner;
    }
}
