package paas.scripted;

import net.sf.jasperreports.engine.JRException;
import platform.server.auth.User;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.ScriptingLogicsModule;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ScriptedBusinessLogics extends BusinessLogics<ScriptedBusinessLogics> {
    private static int exportPort;
    private static List<String> moduleNames;
    private static List<String> scriptFilePaths;

    public ScriptedBusinessLogics(DataAdapter iAdapter, int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException {
        super(iAdapter, port);
    }

    @Override
    protected void createModules() {
        super.createModules();

        assert moduleNames.size() == scriptFilePaths.size();
        for (int i = 0; i < moduleNames.size(); ++i) {
            ScriptingLogicsModule scriptedLM = ScriptingLogicsModule.createFromFile(moduleNames.get(i), scriptFilePaths.get(i), LM, this);
            addLogicsModule(scriptedLM);
        }
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
    }

    public static ScriptedBusinessLogics createInstance(DataAdapter adapter, int port, String modules) throws ClassNotFoundException, IOException, JRException, SQLException, InstantiationException, IllegalAccessException {
        ScriptedBusinessLogics.exportPort = port;

        moduleNames = new ArrayList<String>();
        scriptFilePaths = new ArrayList<String>();

        for (String mod : modules.split(";")) {
            int ind = mod.indexOf(":");
            if (ind == -1) {
                throw new RuntimeException("Неправильный формат списка модулей.");
            }

            moduleNames.add(mod.substring(0, ind));
            scriptFilePaths.add(mod.substring(ind + 1));
        }

        return new ScriptedBusinessLogics(adapter, port);
    }
}
