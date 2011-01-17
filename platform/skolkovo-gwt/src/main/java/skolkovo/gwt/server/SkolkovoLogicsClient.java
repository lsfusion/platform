package skolkovo.gwt.server;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.interop.RemoteLoaderInterface;
import platform.interop.remote.ServerSocketFactory;
import skolkovo.api.remote.SkolkovoRemoteInterface;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;

public class SkolkovoLogicsClient {
    protected final static Logger logger = Logger.getLogger(SkolkovoLogicsClient.class);

    private static SkolkovoLogicsClient instance = new SkolkovoLogicsClient();
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "7652";

    private SkolkovoLogicsClient() {
    }

    public static SkolkovoLogicsClient getInstance() {
        return instance;
    }

    private RMISocketFactory socketFactory;
    private SkolkovoRemoteInterface logics;

    private void initRMISocketFactory() throws IOException {
        if (socketFactory == null) {
            socketFactory = RMISocketFactory.getSocketFactory();
            if (socketFactory == null) {
                socketFactory = RMISocketFactory.getDefaultSocketFactory();
            }

            socketFactory = new ServerSocketFactory();

            RMISocketFactory.setFailureHandler(new RMIFailureHandler() {

                public boolean failure(Exception ex) {
                    return true;
                }
            });

            RMISocketFactory.setSocketFactory(socketFactory);
        }
    }

    public SkolkovoRemoteInterface getLogics(String serverHost, String serverPort) {
        serverHost = BaseUtils.nvl(serverHost, DEFAULT_HOST);
        serverPort = BaseUtils.nvl(serverPort, DEFAULT_PORT);

        //пока вообще не кэшируем, чтобы не париться с обработкой дисконнекта.
//        if (logics == null) {
            try {
                initRMISocketFactory();
                RemoteLoaderInterface loader = (RemoteLoaderInterface) Naming.lookup("rmi://" + serverHost + ":" + serverPort + "/BusinessLogicsLoader");
                logics = (SkolkovoRemoteInterface) loader.getRemoteLogics();
            } catch (Exception e) {
                logger.error("Ошибка при получении объекта логики: ", e);
                throw new RuntimeException("Произошла ошибка при подлючении к серверу приложения.");
            }
//        }
        return logics;
    }
}
