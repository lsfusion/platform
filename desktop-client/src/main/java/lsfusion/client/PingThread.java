package lsfusion.client;

import lsfusion.base.SystemUtils;
import lsfusion.client.form.RmiQueue;
import lsfusion.client.rmi.ConnectionLostManager;
import lsfusion.interop.remote.CallbackMessage;
import lsfusion.interop.remote.ClientCallBackInterface;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static lsfusion.client.ClientResourceBundle.getString;

public class PingThread extends Thread {

    private final static Logger logger = Logger.getLogger(Main.class);

    private final ClientCallBackInterface remoteClient;

    private final ClientCallBackProcessor clientProcessor;

    private final Queue<Long> queue = new LinkedList<>();

    private final int period;

    private long oldIn, oldOut;
    private long sum;
    private int counter;

    private Map<Long, List<Long>> pingInfoMap = new HashMap<>();
    private List<Long> globalPingList = new ArrayList<>();
    private List<Long> currentPingList = new ArrayList<>();
    private long lastPing;
    private long lastTimeFrom = System.currentTimeMillis();
    private int globalCounter;
    private int pingCounter;

    private AtomicBoolean abandoned = new AtomicBoolean();

    public PingThread(ClientCallBackInterface remoteClient) {
        this.period = StartupProperties.pullMessagesPeriod;
        this.remoteClient = remoteClient;
        clientProcessor = new ClientCallBackProcessor(remoteClient);
        setDaemon(true);
    }

    public void abandon() {
        abandoned.set(true);
    }

    public void run() {

        Integer computerId;
        try {
            computerId = Main.remoteLogics.getComputer(SystemUtils.getLocalHostName());
        } catch (RemoteException e) {
            computerId = null;
        }

        while (true) {
            if (abandoned.get() || ConnectionLostManager.isConnectionLost()) {
                return;
            }

            counter++;
            long curTime = System.currentTimeMillis();
            if (remoteClient != null) {
                if (!ConnectionLostManager.shouldBeBlocked()) {
                    //не спамим лишний раз, если отключены
                    try {
                        List<CallbackMessage> messages = RmiQueue.runRetryableRequest(new Callable<List<CallbackMessage>>() {
                            public List<CallbackMessage> call() throws Exception {
                                return remoteClient.pullMessages();
                            }
                        }, abandoned);
                        clientProcessor.processMessages(messages);
                    } catch (final Throwable t) {
                        //выкидываем ошибку в EDT, чтобы обработать общим механизмом и чтобы не убивать PingThread
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                if (abandoned.get()) {
                                    return;
                                }
                                throw new RuntimeException(t);
                            }
                        });
                    }
                }
            }

            long pingTime = System.currentTimeMillis() - curTime;
            queue.add(pingTime);
            sum += pingTime;
            if (queue.size() > 10) {
                sum -= queue.poll();
            }

            pingCounter++;
            currentPingList.add(pingTime);
            //every 30 sec
            if(pingCounter == 30) {
                globalCounter++;
                Collections.sort(currentPingList);
                globalPingList.addAll(currentPingList);
                long newPing = currentPingList.get(15); //medium
                long currentTime = System.currentTimeMillis();
                if (differs(lastPing, newPing)) {
                    Collections.sort(globalPingList);
                    long globalMedian = globalPingList.get(globalPingList.size() / 2);
                    if(differs(lastPing, globalMedian)) {
                        pingInfoMap.put(lastTimeFrom, Arrays.asList(currentTime, globalMedian));
                        globalPingList.clear();
                        lastTimeFrom = currentTime;
                        lastPing = globalMedian;
                    }
                } else {
                    pingInfoMap.put(lastTimeFrom, Arrays.asList(currentTime, newPing));
                }
                currentPingList.clear();
                pingCounter = 0;

                //every hour (30 sec * 120)
                if(globalCounter == 120) {
                    try {
                        lastTimeFrom = currentTime;
                        Main.remoteLogics.sendPingInfo(computerId, pingInfoMap);
                        pingInfoMap.clear();
                        globalCounter = 0;
                    } catch (RemoteException e) {
                        logger.error("Ping statistics saving failed: ", e);
                    }
                }
            }

            if (counter % 5 == 0) {
                long newIn = Main.getBytesReceived();
                long newOut = Main.getBytesSent();

                Main.setStatusText(getString("pingthread.statusMessage", sum / queue.size(), newOut - oldOut, newIn - oldIn, newOut, newIn));

                counter = 0;
                oldIn = newIn;
                oldOut = newOut;
            }

            try {
                Thread.sleep(period);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private boolean differs(long ping1, long ping2) {
        return (ping2 != 0 && (double) ping1 / ping2 < 0.5) || (ping1 != 0 && (double) ping2 / ping1 < 0.5);
    }
}
