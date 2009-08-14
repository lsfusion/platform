package platform.server.data.sql;

import platform.server.data.types.TypeObject;

import java.util.Map;

public class SQLExecute {

    public String command;
    public Map<String, TypeObject> params;

    public SQLExecute(String iCommand, Map<String, TypeObject> iParams) {
        command = iCommand;
        params = iParams;
    }
}
