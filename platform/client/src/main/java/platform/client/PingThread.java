package platform.client;

import org.apache.log4j.Logger;
import platform.interop.remote.ClientCallBackInterface;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.Queue;

import static platform.client.ClientResourceBundle.getString;
import static platform.client.StartupProperties.PLATFORM_CLIENT_PINGTIME;

public class PingThread extends Thread {
    private final static Logger logger = Logger.getLogger(PingThread.class);

    int updateTime;
    long sum;
    int counter;
    Queue<Long> queue = new LinkedList<Long>();
    long oldIn, oldOut;
    private ClientCallBackProcessor clientProcessor;
    private ClientCallBackInterface remoteClient;

    public PingThread(ClientCallBackInterface remoteClient) {
        this.updateTime = Integer.parseInt(System.getProperty(PLATFORM_CLIENT_PINGTIME, "1000"));
        this.remoteClient = remoteClient;
        clientProcessor = new ClientCallBackProcessor(remoteClient);
    }

    public void run() {
        while (true) {
            counter++;
            long curTime = System.currentTimeMillis();
            if (remoteClient != null) {
                try {
                    clientProcessor.processMessages(remoteClient.pullMessages());
                } catch (RemoteException e) {
                    e.printStackTrace();
                    logger.error("Error while pulling messages from server: ", e);
                    throw new RuntimeException(e);
                }
            }

            long pingTime = System.currentTimeMillis() - curTime;
            queue.add(pingTime);
            sum += pingTime;
            if (queue.size() > 10) {
                sum -= queue.poll();
            }

            if (counter % 5 == 0) {
                long newIn = Main.getBytesSent();
                long newOut = Main.getBytesReceived();

                Main.setStatusText(getString("pingthread.statusMessage", sum / queue.size(), newOut - oldOut, newIn - oldIn, newOut, newIn));

                counter = 0;
                oldIn = newIn;
                oldOut = newOut;
            }

            try {
                Thread.sleep(updateTime);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
