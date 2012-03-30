package retail.equipment;

import org.apache.log4j.Logger;
import platform.interop.RemoteLoaderInterface;
import retail.api.remote.PriceTransaction;
import retail.api.remote.RetailRemoteInterface;
import retail.api.remote.ScalesInfo;

import java.net.MalformedURLException;
import java.rmi.*;
import java.text.MessageFormat;
import java.util.List;

public class EquipmentServer {
    
    private Thread thread;

    protected final static Logger logger = Logger.getLogger(EquipmentServer.class);

    public EquipmentServer(final String equServerID, final String serverUrl, final int millis) {

        thread = new Thread(new Runnable() {
            
            private RetailRemoteInterface remote = null;            
            
            @Override
            public void run() {

                while (true) {

                    try {
                        if (remote == null) {
                            RemoteLoaderInterface remoteLoader = null;
                            try {
                                remoteLoader = (RemoteLoaderInterface) Naming.lookup(MessageFormat.format("rmi://{0}/BusinessLogicsLoader", serverUrl));
                            } catch (ConnectException e) {
                                logger.error("Naming lookup error : " + e);
                            } catch (NoSuchObjectException e) {
                                logger.error("Naming lookup error : " + e);
                            } catch (RemoteException e) {
                                logger.error("Naming lookup error : " + e);
                            } catch (MalformedURLException e) {
                                logger.error("Naming lookup error : " + e);
                            } catch (NotBoundException e) {
                                logger.error("Naming lookup error : " + e);
                            }

                            if (remoteLoader != null) {
                                try {
                                    remote = (RetailRemoteInterface) remoteLoader.getRemoteLogics();
                                } catch (RemoteException e) {
                                    logger.error("Get remote logics error : " + e);
                                }
                            }
                        }

                        if (remote != null) {

                            //PriceTransaction transaction = remote.readNextPriceTransaction(equServerID);
                            //List<ScalesInfo> transactionList = remote.readScalesInfo(equServerID);
                            //transactionList.add(null);
                            logger.info("transaction complete");
                        }

                    } catch (Exception e) {
                        logger.error("Unhandled exception : " + e);
                        remote = null;
                    }

                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException e) {
                        logger.info("Thread has been interrupted : " + e);
                        break;
                    }
                }
            }
        });
        
        thread.start();
    }


    public void stop() {
        thread.interrupt();
    }
}
