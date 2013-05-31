package platform.server.data.sql;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.query.CompiledQuery;
import platform.server.data.query.ExecuteEnvironment;
import platform.server.data.type.ParseInterface;

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
