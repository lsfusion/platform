package platform.client;

import platform.interop.RemoteLogicsInterface;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.Queue;


public class PingThread extends Thread {
    private RemoteLogicsInterface remoteLogics;
    int time;
    long sum;
    int counter;
    Queue<Long> queue = new LinkedList<Long>();
    long oldIn, oldOut;

    public PingThread(RemoteLogicsInterface remoteLogics, int time) {
        this.remoteLogics = remoteLogics;
        this.time = time;
    }

    public void run() {
        try {
            while (true) {
                counter++;
                long curTime = System.currentTimeMillis();
                remoteLogics.ping();

                long pingTime = System.currentTimeMillis() - curTime;
                queue.add(pingTime);
                sum += pingTime;
                if (queue.size() > 10) {
                    sum -= queue.poll();
                }

                if (counter % 5 == 0) {
                    long newIn = Main.socketFactory.inSum;
                    long newOut = Main.socketFactory.outSum;

                    ((JLabel) Main.frame.statusComponent).setText(" Пинг:" + sum / queue.size() + " мс, отправлено: " + (newOut - oldOut) +
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
