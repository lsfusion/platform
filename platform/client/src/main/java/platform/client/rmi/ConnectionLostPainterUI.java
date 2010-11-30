package platform.client.rmi;

import com.jhlabs.image.BlurFilter;
import org.jdesktop.jxlayer.plaf.effect.BufferedImageOpEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;
import platform.client.Main;
import platform.client.PropertyConstants;
import platform.interop.DaemonThreadFactory;

import java.rmi.RemoteException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConnectionLostPainterUI extends LockableUI implements Runnable {
    private ScheduledExecutorService executorService;

    public ConnectionLostPainterUI() {
        super(new BufferedImageOpEffect(new BlurFilter()));
    }

    @Override
    public void setLocked(boolean isLocked) {
        super.setLocked(isLocked);
        if (isLocked) {
            executorService = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
            String pingDelay = System.getProperty(PropertyConstants.PLATFORM_CLIENT_CONNECTION_LOST_PING_DELAY, "3000");
            Integer delay = Integer.valueOf(pingDelay);
            executorService.scheduleWithFixedDelay(this, delay, delay, TimeUnit.MILLISECONDS);
        } else {
            executorService.shutdown();
        }
    }

    public void run() {
        try {
            Main.remoteLogics.ping();
        } catch (RemoteException ex) {
        }
    }
}
