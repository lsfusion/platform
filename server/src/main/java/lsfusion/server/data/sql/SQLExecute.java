package lsfusion.server.data.sql;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.query.QueryExecuteEnvironment;
import lsfusion.server.data.type.ParseInterface;

public class SQLExecute {

    public String command;
    public ImMap<String, ParseInterface> params;
    public ExecuteEnvironment env;
    public QueryExecuteEnvironment queryExecEnv;
    public int transactTimeout;

    public SQLExecute(String command, ImMap<String, ParseInterface> params, ExecuteEnvironment env, QueryExecuteEnvironment queryExecEnv, int transactTimeout) {
        this.command = command;
        this.params = params;
        this.env = env;
        this.queryExecEnv = queryExecEnv;
        this.transactTimeout = transactTimeout;
    }
}
