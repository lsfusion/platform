package platform.client;

import platform.interop.remote.ClientCallBackInterface;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.Queue;


public class PingThread extends Thread {
    int time;
    long sum;
    int counter;
    Queue<Long> queue = new LinkedList<Long>();
    long oldIn, oldOut;
    private ClientCallBackProcessor clientProcessor;
    private ClientCallBackInterface remoteClient;

    public PingThread(ClientCallBackInterface remoteClient, int time) {
        this.remoteClient = remoteClient;
        clientProcessor = new ClientCallBackProcessor(remoteClient);
        this.time = time;
    }

    public void run() {
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                counter++;
                long curTime = System.currentTimeMillis();
                if (remoteClient != null) {
                    clientProcessor.processMessages(remoteClient.pullMessages());
                }

                long pingTime = System.currentTimeMillis() - curTime;
                queue.add(pingTime);
                sum += pingTime;
                if (queue.size() > 10) {
                    sum -= queue.poll();
                }

                if (counter % 5 == 0) {
                    long newIn = Main.socketFactory.inSum;
                    long newOut = Main.socketFactory.outSum;

                    Main.frame.statusComponent
                            .setText(" Пинг:" + sum / queue.size() + " мс, отправлено: " + (newOut - oldOut) +
                                     "байт, получено: " + (newIn - oldIn) + " байт, всего отправлено: " + newOut + " байт, всего получено: " + newIn + " байт");
                    counter = 0;
                    oldIn = newIn;
                    oldOut = newOut;
                }
                Thread.sleep(time);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException ignored) {
        }
    }
}
