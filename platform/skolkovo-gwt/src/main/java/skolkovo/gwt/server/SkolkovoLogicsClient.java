package skolkovo.gwt.server;

import platform.interop.RemoteLoaderInterface;
import platform.interop.remote.ServerSocketFactory;
import skolkovo.api.remote.SkolkovoRemoteInterface;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;

public class SkolkovoLogicsClient {
    private static SkolkovoLogicsClient instance = new SkolkovoLogicsClient();
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

    public SkolkovoRemoteInterface getLogics() {
        //пока вообще не кэшируем, чтобы не париться с обработкой дисконнекта.
//        if (logics == null) {
            try {
                initRMISocketFactory();
                RemoteLoaderInterface loader = (RemoteLoaderInterface) Naming.lookup("rmi://" + "localhost" + ":" + "7652" + "/BusinessLogicsLoader");
                logics = (SkolkovoRemoteInterface) loader.getRemoteLogics();
            } catch (Exception e) {
                e.printStackTrace();
            }
//        }
        return logics;
    }
}
