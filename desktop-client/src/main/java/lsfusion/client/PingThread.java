package lsfusion.client;

import lsfusion.base.SystemUtils;
import lsfusion.client.form.RmiQueue;
import lsfusion.client.rmi.ConnectionLostManager;
import lsfusion.interop.remote.ClientCallBackInterface;
import lsfusion.interop.remote.LifecycleMessage;
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
    private final int interval = 30;//calculate ping every 30 seconds
    private final int sendInterval = 120;//every hour (120 calculate intervals)

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
        clientProcessor = new ClientCallBackProcessor();
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
                        List<LifecycleMessage> messages = RmiQueue.runRetryableRequest(new Callable<List<LifecycleMessage>>() {
                            public List<LifecycleMessage> call() throws Exception {
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
            if(pingCounter == interval) {
                globalCounter++;
                Collections.sort(currentPingList);
                globalPingList.addAll(currentPingList);
                long newPing = currentPingList.get(interval / 2); //medium
                long totalMemory = Runtime.getRuntime().totalMemory() / 1048576;
                long freeMemory = Runtime.getRuntime().freeMemory() / 1048576;
                long usedMemory = totalMemory - freeMemory;

                List<Long> lastMemories = pingInfoMap.get(lastTimeFrom);
                Long minTotalMemory = lastMemories == null ? totalMemory : Math.min(totalMemory, lastMemories.get(2));
                Long maxTotalMemory = lastMemories == null ? totalMemory : Math.max(totalMemory, lastMemories.get(3));
                Long minUsedMemory = lastMemories == null ? usedMemory : Math.min(usedMemory, lastMemories.get(4));
                Long maxUsedMemory = lastMemories == null ? usedMemory : Math.max(usedMemory, lastMemories.get(5));

                long currentTime = System.currentTimeMillis();
                if (differs(lastPing, newPing)) {
                    Collections.sort(globalPingList);
                    long globalMedian = globalPingList.get(globalPingList.size() / 2);
                    if(differs(lastPing, globalMedian)) {
                        pingInfoMap.put(lastTimeFrom, Arrays.asList(currentTime, globalMedian, minTotalMemory, maxTotalMemory, minUsedMemory, maxUsedMemory));
                        globalPingList.clear();
                        lastTimeFrom = currentTime;
                        lastPing = globalMedian;
                    }
                } else {
                    pingInfoMap.put(lastTimeFrom, Arrays.asList(currentTime, newPing, minTotalMemory, maxTotalMemory, minUsedMemory, maxUsedMemory));
                }
                currentPingList.clear();
                pingCounter = 0;

                if(globalCounter == sendInterval) {
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
