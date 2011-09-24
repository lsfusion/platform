package paas;

import org.apache.commons.exec.*;
import org.apache.log4j.Logger;
import paas.scripted.ScriptedBusinessLogics;
import platform.interop.remote.ApplicationManager;

import java.io.IOException;
import java.net.Socket;
import java.rmi.Naming;
import java.util.List;

public class BLLogicsManager {
    protected final static Logger logger = Logger.getLogger(BLLogicsManager.class);

    private static final String javaExe = System.getProperty("java.home") + "/bin/java";

    public BLLogicsManager() {
    }

    protected String getStatus(int port) {
        try {
            ApplicationManager remoteManager = (ApplicationManager) Naming.lookup("rmi://localhost:" + port + "/AppManager");
            return "started";
        } catch (Exception e) {
            return isPortAvailable(port) ? "stopped" : "busyPort";
        }
    }

    public boolean isPortAvailable(int port) {
        Socket socket = null;
        try {
            socket = new Socket("localhost", port);
        } catch (Exception e) {
            // Getting exception means the port is not used by other applications
            return true;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioe) {
                    // Do nothing
                }
            }
        }

        return false;
    }

    public void stopApplication(Integer port) {
        try {
            ApplicationManager remoteManager = (ApplicationManager) Naming.lookup("rmi://localhost:" + port + "/AppManager");
            remoteManager.stop();
        } catch (Exception e) {
            logger.warn("Ошибка при попытке остановить приложение: ", e);
        }
    }

    public void executeScriptedBL(int port, String dbName, List<String> moduleNames, List<String> scriptFilePaths) throws IOException, InterruptedException {
        CommandLine commandLine = new CommandLine(javaExe);
        commandLine.addArgument("-Dlsf.settings.path=conf/scripted/settings.xml");
        commandLine.addArgument("-Dpaas.scripted.port=" + port);
        commandLine.addArgument("-Dpaas.scripted.db.name=" + dbName);
        commandLine.addArgument("-Dpaas.scripted.modules=" + toParameters(moduleNames, scriptFilePaths));
        commandLine.addArgument("-Djava.rmi.server.hostname=" + "92.243.72.138");

        commandLine.addArgument("-cp");
        commandLine.addArgument(System.getProperty("java.class.path"));
        commandLine.addArgument(ScriptedBusinessLogics.class.getName());

        Executor executor = new DefaultExecutor();
//        executor.setStreamHandler(new PumpStreamHandler(new NullOutpuStream(), new NullOutpuStream()));
        executor.setStreamHandler(new PumpStreamHandler());
        executor.setExitValue(1);

        executor.execute(commandLine, new DefaultExecuteResultHandler());

//        resultHandler.waitFor();
    }

    private String toParameters(List<String> moduleNames, List<String> scriptFilePaths) {
        assert moduleNames.size() == scriptFilePaths.size();
        StringBuilder result = new StringBuilder(moduleNames.size() * 30);
        for (int i = 0; i < moduleNames.size(); ++i) {
            if (result.length() != 0) {
                result.append(";");
            }
            result.append(moduleNames.get(i)).append(":").append(scriptFilePaths.get(i));
        }

        return result.toString();
    }

    private class PrintResultHandler extends DefaultExecuteResultHandler {
        private ExecuteWatchdog watchdog;

        public PrintResultHandler(ExecuteWatchdog watchdog) {
            this.watchdog = watchdog;
        }

        public PrintResultHandler(int exitValue) {
            super.onProcessComplete(exitValue);
        }

        public void onProcessComplete(int exitValue) {
            super.onProcessComplete(exitValue);
            System.out.println("[resultHandler] The document was successfully printed ...");
        }

        public void onProcessFailed(ExecuteException e) {
            super.onProcessFailed(e);
            if (watchdog != null && watchdog.killedProcess()) {
                System.err.println("[resultHandler] The print process timed out");
            } else {
                System.err.println("[resultHandler] The print process failed to do : " + e.getMessage());
            }
        }
    }
}
