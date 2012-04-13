package retail.equipment;

import org.apache.log4j.Logger;
import platform.interop.RemoteLoaderInterface;
import retail.api.remote.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.rmi.*;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;

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

                            processTransactionInfo(remote, equServerID);
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


    private void processTransactionInfo(RetailRemoteInterface remote, String equServerID) throws SQLException, RemoteException, FileNotFoundException, UnsupportedEncodingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        List<TransactionInfo> transactionInfoList = remote.readTransactionInfo(equServerID);
        Collections.sort(transactionInfoList, COMPARATOR);
        for (TransactionInfo<MachineryInfo> transaction : transactionInfoList) {

            Map<String, List<MachineryInfo>> handlerModelMap = new HashMap<String, List<MachineryInfo>>();
            for (MachineryInfo machinery : transaction.machineryInfoList) {
                if (!handlerModelMap.containsKey(machinery.handlerModel))
                    handlerModelMap.put(machinery.handlerModel, new ArrayList());
                handlerModelMap.get(machinery.handlerModel).add(machinery);
            }

            for (Map.Entry<String, List<MachineryInfo>> entry : handlerModelMap.entrySet()) {
                if (entry.getKey() != null) {

                    try {
                        Object clsHandler;
                        if (entry.getKey().trim().split("\\$").length == 1) {
                            Class cls = Class.forName(entry.getKey().trim());
                            clsHandler = cls.newInstance();
                        } else {
                            Class outerClass = Class.forName(entry.getKey().trim().split("\\$")[0]);
                            Class innerClass = Class.forName(entry.getKey().trim());
                            clsHandler = innerClass.getDeclaredConstructors()[0].newInstance(outerClass.newInstance());
                        }
                        transaction.sendTransaction(clsHandler, entry.getValue());
                    } catch (Exception e) {
                        remote.errorReport(transaction.id, e);
                        return;
                    }
                }
            }

            remote.succeedTransaction(transaction.id);
        }

    }

    private static Comparator<TransactionInfo> COMPARATOR = new Comparator<TransactionInfo>() {
        public int compare(TransactionInfo o1, TransactionInfo o2) {
            return o1.dateTimeCode.compareTo(o2.dateTimeCode);
        }
    };


    public void stop() {
        thread.interrupt();
    }
}
