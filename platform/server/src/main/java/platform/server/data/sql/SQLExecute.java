package platform.server.data.sql;

import platform.server.data.TypedObject;

import java.util.Map;

public class SQLExecute {

    public String command;
    public Map<String, TypedObject> params;

    public SQLExecute(String iCommand, Map<String, TypedObject> iParams) {
        command = iCommand;
        params = iParams;
    }
}
