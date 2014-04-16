package lsfusion.server.data.sql;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.TableOwner;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.query.QueryExecuteEnvironment;
import lsfusion.server.data.type.ParseInterface;

public class SQLExecute {

    public final String command;
    public final ImMap<String, ParseInterface> params;
    public final ExecuteEnvironment env;
    public final QueryExecuteEnvironment queryExecEnv;
    public final int transactTimeout;
    public final OperationOwner owner;
    public final TableOwner tableOwner;

    public SQLExecute(String command, ImMap<String, ParseInterface> params, ExecuteEnvironment env, QueryExecuteEnvironment queryExecEnv, int transactTimeout, OperationOwner owner, TableOwner tableOwner) {
        this.command = command;
        this.params = params;
        this.env = env;
        this.queryExecEnv = queryExecEnv;
        this.transactTimeout = transactTimeout;
        this.owner = owner;
        this.tableOwner = tableOwner;
    }
}
