package platform.server.logics.scripted;

import platform.server.auth.User;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class ScriptingBusinessLogics extends BusinessLogics<ScriptingBusinessLogics> {
    private final String name;
    private final List<String> scriptFilePaths;
    private final List<String> excludedScriptFilePaths;

    public ScriptingBusinessLogics(String name, DataAdapter iAdapter, int port, String paths) throws Exception {
        this(name, iAdapter, port, Arrays.asList(paths.split(";")), null);
    }

    public ScriptingBusinessLogics(String name, DataAdapter iAdapter, int port, String paths, String excludedPaths) throws Exception {
        this(name, iAdapter, port, Arrays.asList(paths.split(";")), Arrays.asList(excludedPaths.split(";")));
    }

    public ScriptingBusinessLogics(String name, DataAdapter adapter, int port, List<String> paths, List<String> excludedPaths) throws Exception {
        super(adapter, port);
        this.name = name;
        this.scriptFilePaths = paths;
        this.excludedScriptFilePaths = excludedPaths;
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        addModulesFromResource(scriptFilePaths, excludedScriptFilePaths);
    }

    //private String modifySlashes(String regexp) {
    //    return regexp.replace("/", "\\\\");
    //}

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
    }

    @Override
    public String getName() throws RemoteException {
        return name;
    }
}
