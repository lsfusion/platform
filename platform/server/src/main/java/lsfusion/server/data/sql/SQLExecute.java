package lsfusion.server.data.sql;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.type.ParseInterface;

public class SQLExecute {

    public String command;
    public ImMap<String, ParseInterface> params;
    public ExecuteEnvironment env;

    public SQLExecute(String command, ImMap<String, ParseInterface> params, ExecuteEnvironment env) {
        this.command = command;
        this.params = params;
        this.env = env;
    }
}
