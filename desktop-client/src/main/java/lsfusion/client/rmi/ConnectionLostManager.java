package lsfusion.client.rmi;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.SystemUtils;
import lsfusion.base.WeakIdentityHashSet;
import lsfusion.client.Main;
import lsfusion.client.MainFrame;
import lsfusion.client.StartupProperties;
import lsfusion.client.SwingUtils;
import lsfusion.client.form.RmiQueue;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.interop.RemoteLogicsInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static lsfusion.client.ClientResourceBundle.getString;

public class ConnectionLostManager {
    private static final AtomicLong failedRequests = new AtomicLong();
    private static final AtomicBoolean connectionLost = new AtomicBoolean(false);

    private static Timer timerWhenUnblocked;
    private static Timer timerWhenBlocked;

    private static MainFrame currentFrame;

    private static BlockDialog blockDialog;

    private static Pinger pinger;
    private static ExecutorService pingerExecutor = Executors.newCachedThreadPool(new DaemonThreadFactory("pinger"));

    public static void start(MainFrame frame) {
        SwingUtils.assertDispatchThread();

        assert frame != null;
        if (currentFrame != frame) {
            currentFrame = frame;
        }

        connectionLost.set(false);

        timerWhenUnblocked = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                blockIfHasFailed();
            }
        });
        timerWhenUnblocked.start();

        timerWhenBlocked = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (blockDialog != null) {
                    blockDialog.setFatal(isConnectionLost());
                    if (!shouldBeBlocked()) {
                        timerWhenBlocked.stop();

                        currentFrame.setLocked(false);

                        blockDialog.dispose();

                        blockDialog = null;
                    }
                }
            }
        });
    }

    public static void connectionLost() {
        connectionLost.set(true);
        RmiQueue.notifyEdtSyncBlocker();
    }

    public static void connectionBroke() {
        SwingUtils.assertDispatchThread();
        if (pinger == null) {
            pinger = new Pinger();
            pinger.execute();
        }
    }

    public static boolean isConnectionLost() {
        return connectionLost.get();
    }

    public static void forceDisconnect(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Main.restart();
            }
        });
    }

    public static void blockIfHasFailed() {
        SwingUtils.assertDispatchThread();

        if (shouldBeBlocked() && blockDialog == null) {
            currentFrame.setLocked(true);

            blockDialog = new BlockDialog(null, currentFrame, isConnectionLost(), true);
            blockDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    if (timerWhenBlocked != null) {
                        timerWhenBlocked.start();
                    }
                }
            });
            blockDialog.setVisible(true);
        }
    }

    public static void registerFailedRmiRequest() {
        //каждый зарегистрировавщийся поток, должне также проверять ConnectionLost, чтобы заркрыться при необходимости...
        failedRequests.incrementAndGet();
        RmiQueue.notifyEdtSyncBlocker();
    }

    public static void unregisterFailedRmiRequest() {
        failedRequests.decrementAndGet();
    }

    private static boolean hasFailedRequest() {
        return failedRequests.get() > 0;
    }

    private static WeakIdentityHashSet<RmiQueue> rmiQueues = new WeakIdentityHashSet<RmiQueue>();
    public static void registerRmiQueue(RmiQueue rmiQueue) {
        SwingUtils.assertDispatchThread();
        rmiQueues.add(rmiQueue);
    }

    public static boolean shouldBeBlocked() {
        return hasFailedRequest() || isConnectionLost();
    }

    public static void invalidate() {
        SwingUtils.assertDispatchThread();

        connectionLost();

        for (RmiQueue rmiQueue : rmiQueues) {
            rmiQueue.abandon();
        }

        if (pinger != null) {
            pinger.abandon();
            pinger = null;
        }

        rmiQueues.clear();

        failedRequests.set(0);

        if (timerWhenBlocked != null) {
            timerWhenBlocked.stop();
            timerWhenBlocked = null;
        }

        if (timerWhenUnblocked != null) {
            timerWhenUnblocked.stop();
            timerWhenUnblocked = null;
        }

        currentFrame = null;

        if (blockDialog != null) {
            blockDialog.dispose();
            blockDialog = null;
        }
    }

    public static class BlockDialog extends JDialog {
        private JButton btnExit;
        private JButton btnCancel;
        private JButton btnReconnect;

        private boolean fatal;

        private final String message;
        private final JLabel lbMessage;
        private final JPanel progressPanel;
        private final Container mainPane;

        public BlockDialog(String message, JFrame owner, boolean fatal, boolean showReconnect) {
            super(owner, getString("rmi.connectionlost"), true);

            this.message = message;

            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            setLocationRelativeTo(owner);

            btnExit = new JButton(getString("rmi.connectionlost.exit"));
            btnCancel = new JButton(getString("rmi.connectionlost.relogin"));
            btnReconnect = new JButton(getString("rmi.connectionlost.reconnect"));

            lbMessage = new JLabel();

            JPanel messagePanel = new JPanel();
            messagePanel.add(lbMessage);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(btnExit);

            if (showReconnect) {
                buttonPanel.add(btnReconnect);
            }

            buttonPanel.add(btnCancel);

            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);

            progressPanel = new JPanel();
            progressPanel.add(progressBar);

            mainPane = getContentPane();
            mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
            mainPane.add(messagePanel);
            mainPane.add(buttonPanel);

            this.fatal = !fatal;
            setFatal(fatal);

            pack();
            setResizable(false);

            initUIHandlers();

            setupDialogForDevMode();
        }

        private void setupDialogForDevMode() {
            //сразу уходим на реконнект, без ожидания возможного восстановления соединения...
            //полезно при частом перестарте сервера
            if (StartupProperties.autoReconnect) {
                addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowOpened(WindowEvent e) {
                        btnReconnect.doClick();
                    }
                });
            }

            //чтобы блокер-диалог не забирал фокус
            if (StartupProperties.preventBlockerActivation) {
                setFocusableWindowState(false);
            }
        }

        private void initUIHandlers() {
            btnExit.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Main.shutdown();
                }
            });
            btnCancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Main.restart();
                }
            });
            btnReconnect.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Main.reconnect();
                }
            });
        }

        public void setFatal(boolean fatal) {
            if (this.fatal != fatal) {
                String messageText =
                        message != null
                        ? message
                        : fatal
                          ? getString("rmi.connectionlost.fatal")
                          : getString("rmi.connectionlost.nonfatal");

                lbMessage.setText(messageText);

                this.fatal = fatal;

                if (fatal) {
                    mainPane.remove(progressPanel);
                } else {
                    mainPane.add(progressPanel, 1);
                }
                pack();
            }
        }

    }

    static class Pinger implements Runnable {
        private RemoteLogicsInterface remogeLogics = Main.remoteLogics;
        private AtomicBoolean abandoned = new AtomicBoolean();

        public void execute() {
            registerFailedRmiRequest();
            pingerExecutor.execute(this);
        }

        @Override
        public void run() {
            do {
                try {
                    remogeLogics.ping();
                    //пинг прошёл...
                    unregisterFailedRmiRequest();
                    break;
                } catch (Throwable t) {
                    if (abandoned.get()) {
                        //этот пингер больше неактуален
                        return;
                    }
                    if (ExceptionUtils.isFatalRemoteException(t)) {
                        ConnectionLostManager.connectionLost();
                        return;
                    }
                }

                SystemUtils.sleep(1000);
            } while (true);
        }

        public void abandon() {
            abandoned.set(true);
        }
    }
}
