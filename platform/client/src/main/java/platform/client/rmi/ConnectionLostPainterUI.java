package platform.client.rmi;

import com.jhlabs.image.BlurFilter;
import org.jdesktop.jxlayer.plaf.effect.BufferedImageOpEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;
import platform.client.DaemonThreadFactory;
import platform.client.Main;
import platform.client.PropertyConstants;

import javax.swing.*;
import java.awt.*;
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
    public void paint(Graphics graphics, JComponent l) {
        super.paint(graphics, l);

        if (isLocked()) {
            Graphics2D g2 = (Graphics2D)graphics;
            String msg = "Соединение потеряно. Ждите либо попробуйте перезапустить приложение.";
            g2.setFont(new Font(g2.getFont().getName(), Font.BOLD, 24));
            l.setForeground(Color.RED);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (l.getWidth()-fm.stringWidth(msg))/2, (l.getHeight()-fm.getHeight())/2);
        }
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
