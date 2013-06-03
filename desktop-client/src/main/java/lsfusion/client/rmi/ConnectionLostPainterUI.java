package lsfusion.client.rmi;

import com.jhlabs.image.BlurFilter;
import org.jdesktop.jxlayer.plaf.effect.BufferedImageOpEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;
import lsfusion.client.Main;
import lsfusion.interop.DaemonThreadFactory;

import java.rmi.RemoteException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static lsfusion.client.StartupProperties.LSFUSION_CLIENT_CONNECTION_LOST_PING_DELAY;

public class ConnectionLostPainterUI extends LockableUI implements Runnable {
    private ScheduledExecutorService executorService;

    public ConnectionLostPainterUI() {
        super(new BufferedImageOpEffect(new BlurFilter()));
    }

    public void lockAndPing(boolean isLocked) {
        setLocked(isLocked);
        if (isLocked) {
            executorService = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("-connection-lost-daemon-"));
            String pingDelay = System.getProperty(LSFUSION_CLIENT_CONNECTION_LOST_PING_DELAY, "3000");
            Integer delay = Integer.valueOf(pingDelay);
            executorService.scheduleWithFixedDelay(this, delay, delay, TimeUnit.MILLISECONDS);
        } else {
            executorService.shutdown();
        }
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    public void run() {
        try {
            if (Main.remoteLogics != null) {
                Main.remoteLogics.ping();
            }
        } catch (RemoteException ignored) {
        }
    }
}
