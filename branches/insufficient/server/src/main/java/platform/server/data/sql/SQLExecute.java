package platform.server.data.sql;

import platform.server.data.type.TypeObject;
import platform.server.data.type.ParseInterface;

import java.util.Map;

public class SQLExecute {

    public String command;
    public Map<String, ParseInterface> params;

    public SQLExecute(String command, Map<String, ParseInterface> params) {
        this.command = command;
        this.params = params;
    }
}
