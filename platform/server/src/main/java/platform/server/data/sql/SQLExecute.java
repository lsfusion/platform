package platform.server.data.sql;

import platform.server.data.query.ExecuteEnvironment;
import platform.server.data.type.TypeObject;
import platform.server.data.type.ParseInterface;

import java.util.Map;

public class SQLExecute {

    public String command;
    public Map<String, ParseInterface> params;
    public ExecuteEnvironment env;

    public SQLExecute(String command, Map<String, ParseInterface> params, ExecuteEnvironment env) {
        this.command = command;
        this.params = params;
        this.env = env;
    }
}
