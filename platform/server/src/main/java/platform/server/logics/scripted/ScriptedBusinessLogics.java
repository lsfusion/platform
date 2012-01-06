package platform.server.logics.scripted;

import platform.server.auth.User;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class ScriptedBusinessLogics extends BusinessLogics<ScriptedBusinessLogics> {
    private final String name;
    private final List<String> moduleNames;
    private final List<String> scriptFilePaths;

    public ScriptedBusinessLogics(String name, DataAdapter iAdapter, int port, String names, String paths) throws Exception {
        this(name, iAdapter, port, Arrays.asList(names.split(";")), Arrays.asList(paths.split(";")));
    }

    public ScriptedBusinessLogics(String name, DataAdapter adapter, int port, List<String> names, List<String> paths) throws Exception {
        super(adapter, port);
        this.name = name;
        this.moduleNames = names;
        this.scriptFilePaths = paths;

        if (moduleNames.size() != scriptFilePaths.size()) {
            throw new RuntimeException("Количество имён модулей не равно количеству путей к файлам модулей.");
        }
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();

        for (int i = 0; i < moduleNames.size(); ++i) {
            ScriptingLogicsModule scriptedLM = ScriptingLogicsModule.createFromFile(scriptFilePaths.get(i), LM, this);
            addLogicsModule(scriptedLM);
        }
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
    }

    @Override
    public String getName() throws RemoteException {
        return name;
    }
}
