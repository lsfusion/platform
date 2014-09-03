package lsfusion.client;

import lsfusion.client.form.RmiQueue;
import lsfusion.client.rmi.ConnectionLostManager;
import lsfusion.interop.remote.CallbackMessage;
import lsfusion.interop.remote.ClientCallBackInterface;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static lsfusion.client.ClientResourceBundle.getString;

public class PingThread extends Thread {
    private final ClientCallBackInterface remoteClient;

    private final ClientCallBackProcessor clientProcessor;

    private final Queue<Long> queue = new LinkedList<Long>();

    private final int period;

    private long oldIn, oldOut;
    private long sum;
    private int counter;

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
        while (true) {
            if (abandoned.get() || ConnectionLostManager.isConnectionLost()) {
                return;
            }

            counter++;
            long curTime = System.currentTimeMillis();
            if (remoteClient != null) {
                if (!ConnectionLostManager.shouldBeBlocked()) {
                    //не спами лишний раз, если отключены
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
}
