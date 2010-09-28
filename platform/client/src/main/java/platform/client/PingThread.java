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
    long totalSum, curSum;

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
                    ((JLabel) Main.frame.statusComponent).setText(" Ping:" + sum / queue.size() + " ms, Current: " + curSum + " b, Total: " + totalSum + " b");
                    counter %= 5;
                    curSum = 0;
                }
                Thread.sleep(time);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);

        }
    }

    public void incrementBytes(int cnt){
        totalSum += cnt;
        curSum += cnt;
    }
}
