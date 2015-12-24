package lsfusion.client.form;

import lsfusion.base.InterruptibleProvider;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BusyDialogDisplayer extends TimerTask {
    public static int REPAINT_PERIOD = 1000;

    private Timer executionTimer;
    private Window drawingWindow;
    private BlurWindow blurWindow;
    private BusyDialog busyDialog;
    private final InterruptibleProvider<List<Object>> serverMessageProvider;

    public BusyDialogDisplayer(InterruptibleProvider<List<Object>> serverMessageProvider) {
        this.serverMessageProvider = serverMessageProvider;
    }

    public void start() {
        drawingWindow = SwingUtils.getActiveVisibleWindow();
        if (drawingWindow == null) {
            drawingWindow = Main.frame;
        }

        blurWindow = new BlurWindow(drawingWindow);
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
            blurWindow.dispose();
            blurWindow = null;
            busyDialog.dispose();
            busyDialog = null;
        }
    }

    public void show(Runnable r) {
        SwingWorker hideSwingWorker = null;
        try {
            hideSwingWorker = new HideSwingWorker(r);
            hideSwingWorker.execute();
            blurWindow.setVisible(true);
            busyDialog.setVisible(true);
        } finally {
            if (hideSwingWorker != null)
                hideSwingWorker.cancel(false);
        }

    }

    public void hide() {
        if(busyDialog != null && busyDialog.isVisible()) {
            blurWindow.setVisible(false);
            busyDialog.setVisible(false);
        }
    }

    @Override
    public void run() {
        if (drawingWindow != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (busyDialog != null) {
                            busyDialog.updateBusyDialog(serverMessageProvider.get());
                            Boolean needInterrupt = busyDialog.needInterrupt();
                            if (needInterrupt != null)
                                serverMessageProvider.interrupt(needInterrupt);
                        }
                    } catch (Throwable e) {
                        stop();
                        RmiQueue.busyRunning = false;
                    }
                }
            });
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