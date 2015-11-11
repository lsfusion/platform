package lsfusion.client.form;

import lsfusion.base.Provider;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;
import org.jdesktop.swingworker.SwingWorker;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class BusyDialogDisplayer extends TimerTask {
    public static int REPAINT_PERIOD = 1000;

    private Timer executionTimer;
    private Window drawingWindow;
    private BusyDialog busyDialog;
    private final Provider<String> serverMessageProvider;

    public BusyDialogDisplayer(Provider<String> serverMessageProvider) {
        this.serverMessageProvider = serverMessageProvider;
    }

    public void start() {
        drawingWindow = SwingUtils.getActiveVisibleWindow();
        if (drawingWindow == null) {
            drawingWindow = Main.frame;
        }

        busyDialog = new BusyDialog(drawingWindow, true);

        if (drawingWindow != null) {
            drawingWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            executionTimer = new Timer();
            executionTimer.schedule(this, 0, REPAINT_PERIOD);
        }
    }

    public void stop() {
        if (drawingWindow != null) {
            executionTimer.cancel();
            drawingWindow.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            drawingWindow.repaint();
            drawingWindow = null;
            busyDialog.dispose();
            busyDialog = null;
        }
    }

    public void show(Runnable r) {
        SwingWorker hideSwingWorker = null;
        try {
            hideSwingWorker = new HideSwingWorker(r);
            hideSwingWorker.execute();
            busyDialog.setVisible(true);
        } catch(Exception e) {
            System.out.println(e);
        }finally {
            if (hideSwingWorker != null)
                hideSwingWorker.cancel(false);
        }

    }

    public void hide() {
        if(busyDialog != null && busyDialog.isVisible())
            busyDialog.setVisible(false);
    }

    @Override
    public void run() {
        if (drawingWindow != null) {
            if(Main.configurationAccessAllowed)
                busyDialog.setStackMessage(serverMessageProvider.get());
        }
    }

    class HideSwingWorker extends SwingWorker {
        Runnable r;
        public HideSwingWorker(Runnable r) {
            this.r = r;
        }

        @Override
        protected Object doInBackground() throws Exception {
            r.run();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    hide();
                }
            });
            return null;
        }
    }
}