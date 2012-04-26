package retail.equipment;

import org.apache.log4j.Logger;
import platform.interop.RemoteLoaderInterface;
import retail.api.remote.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.rmi.*;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;

public class EquipmentServer {

    private Thread thread;

    protected final static Logger logger = Logger.getLogger(EquipmentServer.class);
    Map<String, Object> handlerMap = new HashMap<String, Object>();
    EquipmentServerSettings equipmentServerSettings;

    public EquipmentServer(final String equServerID, final String serverUrl) {

        thread = new Thread(new Runnable() {

            private RetailRemoteInterface remote = null;

            @Override
            public void run() {

                int millis = 10000;
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
                                    equipmentServerSettings = remote.readEquipmentServerDelay(equServerID);
                                    if (equipmentServerSettings.delay != null)
                                        millis = equipmentServerSettings.delay;
                                } catch (RemoteException e) {
                                    logger.error("Get remote logics error : " + e);
                                }
                            }
                        }

                        if (remote != null) {

                            processTransactionInfo(remote, equServerID);
                            sendSalesInfo(remote, equServerID);
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
                        Object clsHandler = getHandler(entry.getValue().get(0).handlerModel.trim());
                        transaction.sendTransaction(clsHandler, entry.getValue());
                    } catch (Exception e) {
                        remote.errorTransactionReport(transaction.id, e);
                        return;
                    }
                }
            }
            remote.succeedTransaction(transaction.id);
        }
    }

    private void sendSalesInfo(RetailRemoteInterface remote, String equServerID) throws SQLException, IOException {
        List<CashRegisterInfo> cashRegisterInfoList = remote.readCashRegisterInfo(equServerID);

        Map<String, List<MachineryInfo>> handlerModelMap = new HashMap<String, List<MachineryInfo>>();
        for (CashRegisterInfo cashRegister : cashRegisterInfoList) {
            if (!handlerModelMap.containsKey(cashRegister.nameModel))
                handlerModelMap.put(cashRegister.nameModel, new ArrayList());
            handlerModelMap.get(cashRegister.nameModel).add(cashRegister);
        }

        for (Map.Entry<String, List<MachineryInfo>> entry : handlerModelMap.entrySet()) {
            if (entry.getKey() != null) {

                try {
                    Object clsHandler = getHandler(entry.getValue().get(0).handlerModel.trim());
                    SalesBatch salesBatch = ((CashRegisterHandler) clsHandler).readSalesInfo(cashRegisterInfoList);
                    String result = remote.sendSalesInfo(salesBatch.salesInfoList, equServerID);
                    if (result != null)
                        remote.errorEquipmentServerReport(equServerID, new Throwable(result));
                    else
                        ((CashRegisterHandler) clsHandler).finishReadingSalesInfo(salesBatch);
                } catch (Exception e) {
                    remote.errorEquipmentServerReport(equServerID, e.fillInStackTrace());
                    return;
                }
            }
        }
    }

    private Object getHandler(String handlerModel) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
        Object clsHandler;
        if (handlerMap.containsKey(handlerModel))
            clsHandler = handlerMap.get(handlerModel);
        else {
            if (handlerModel.split("\\$").length == 1) {
                Class cls = Class.forName(handlerModel);
                clsHandler = cls.newInstance();
            } else {
                Class outerClass = Class.forName(handlerModel.split("\\$")[0]);
                Class innerClass = Class.forName(handlerModel);
                clsHandler = innerClass.getDeclaredConstructors()[0].newInstance(outerClass.newInstance());
            }
            handlerMap.put(handlerModel, clsHandler);
        }
        return clsHandler;
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