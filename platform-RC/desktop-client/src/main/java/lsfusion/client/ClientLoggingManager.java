package lsfusion.client;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

public class ClientLoggingManager {
    private List<Handler> handlers = new ArrayList<>();

    private static ClientLoggingManager instance = new ClientLoggingManager();

    public static void turnOnTcpLogging() {
        on("D:\\client-sun.rmi.transport.tcp.log", "sun.rmi.transport.tcp", Level.ALL);
    }

    public static void turnOnRmiLogging(String baseDir) {
        on(baseDir + "client-sun.rmi.server.call.log", "sun.rmi.server.call", Level.ALL);
        on(baseDir + "client-sun.rmi.server.ref.log", "sun.rmi.server.ref", Level.ALL);
        on(baseDir + "client-sun.rmi.client.call.log", "sun.rmi.client.call", Level.ALL);
        on(baseDir + "client-sun.rmi.client.ref.log", "sun.rmi.client.ref", Level.ALL);
        on(baseDir + "client-sun.rmi.dgc.log", "sun.rmi.dgc", Level.ALL);
        on(baseDir + "client-sun.rmi.loader.log", "sun.rmi.loader", Level.ALL);
        on(baseDir + "client-sun.rmi.transport.misc.log", "sun.rmi.transport.misc", Level.ALL);
        on(baseDir + "client-sun.rmi.transport.tcp.log", "sun.rmi.transport.tcp", Level.ALL);
        on(baseDir + "client-sun.rmi.transport.proxy.log", "sun.rmi.transport.proxy", Level.ALL);
    }

    public static void turnOnRmiLogging() {
        turnOnRmiLogging("D:\\");
    }

    public static void on(String filePath, String loggerName, Level level) {
        try {
//            Handler fh = new FileHandler(filePath, false);
            Handler fh = new ConsoleHandler();
            fh.setLevel(level);
            fh.setFormatter(new SimpleFormatter());
            Logger.getLogger(loggerName).addHandler(fh);
            Logger.getLogger(loggerName).setLevel(level);
            instance.handlers.add(fh);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onFinest(String filePath, String loggerName) {
        on(filePath, loggerName, Level.FINEST);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        for (Handler h : handlers) {
            h.flush();
            h.close();
        }
    }
}
