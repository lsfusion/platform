package lsfusion.client.rmi;

import lsfusion.base.*;
import lsfusion.client.*;
import lsfusion.client.exceptions.ClientExceptionManager;
import lsfusion.client.form.RmiQueue;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.exceptions.NonFatalHandledRemoteException;
import lsfusion.interop.remote.ClientCallBackInterface;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
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

    private static PingThread pingThread;

    public static void start(MainFrame frame, ClientCallBackInterface clientCallBack) {
        SwingUtils.assertDispatchThread();

        assert frame != null;
        if (currentFrame != frame) {
            currentFrame = frame;
        }

        pingThread = new PingThread(clientCallBack);
        pingThread.setDaemon(true);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                pingThread.start();
            }
        });

        connectionLost.set(false);

        timerWhenUnblocked = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ClientExceptionManager.flushUnreportedThrowables();
                        
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
        if (pinger == null) { // запускаем в другом потоке, чтобы освободить edt, потом можно с PingThread'ом совместить
            // синхронизировать особо нет смысла, так как блокировка EDT все равно идет асинхронно, и клиентские действия все равно могут "проскочить"
            pinger = new Pinger();
            pinger.execute();
        }
    }

    public static boolean isConnectionLost() {
        return connectionLost.get();
    }

    public static void blockIfHasFailed() {
        SwingUtils.assertDispatchThread();

        if (shouldBeBlocked() && blockDialog == null && currentFrame != null) {
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
    
    private static final ConcurrentHashMap<Long, List<NonFatalHandledRemoteException>> failedNotFatalHandledRequests = new ConcurrentHashMap<>();
    public static void addFailedRmiRequest(RemoteException remote, long reqId) {
        List<NonFatalHandledRemoteException> exceptions = failedNotFatalHandledRequests.get(reqId);
        if(exceptions == null) {
            exceptions = new ArrayList<>();
            failedNotFatalHandledRequests.put(reqId, exceptions);
        }
        exceptions.add(new NonFatalHandledRemoteException(remote, reqId)); // !! важно создавать здесь чтобы релевантный stack trace был
    }
    
    public static void flushFailedNotFatalRequests(final boolean abandoned, long reqId) {
        final List<NonFatalHandledRemoteException> flushExceptions = failedNotFatalHandledRequests.remove(reqId);
        if(flushExceptions != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Map<Pair<String, Long>, Collection<NonFatalHandledRemoteException>> group;
                        group = BaseUtils.group(new BaseUtils.Group<Pair<String, Long>, NonFatalHandledRemoteException>() {
                            public Pair<String, Long> group(NonFatalHandledRemoteException key) {
                                return new Pair<>(key.getMessage() + ExceptionUtils.getStackTraceString(key), key.reqId);
                            }
                        }, flushExceptions);

                    for (Map.Entry<Pair<String, Long>, Collection<NonFatalHandledRemoteException>> entry : group.entrySet()) {
                        Collection<NonFatalHandledRemoteException> all = entry.getValue();
                        NonFatalHandledRemoteException nonFatal = all.iterator().next();
                        nonFatal.count = all.size();
                        nonFatal.abandoned = abandoned;
                        ClientExceptionManager.reportClientHandledRemoteThrowable(nonFatal);
                    }

                }
            });
        }
    }
    
    public static void unregisterFailedRmiRequest(boolean abandoned, long reqId) {
        if(!abandoned) // чтобы еще раз не decrement'ся
            failedRequests.decrementAndGet();
        
        flushFailedNotFatalRequests(abandoned, reqId);
    }

    private static boolean hasFailedRequest() {
        return failedRequests.get() > 0;
    }

    private static WeakIdentityHashSet<RmiQueue> rmiQueues = new WeakIdentityHashSet<>();
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

        if (pingThread != null) {
            pingThread.abandon();
            pingThread.interrupt();
            pingThread = null;
        }

        failedRequests.set(0); // важно abandon'ы сделать до обнуления failedRequests

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

            setFocusableWindowState(false);
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
        private RemoteLogicsInterface remoteLogics = Main.remoteLogics;
        private AtomicBoolean abandoned = new AtomicBoolean();

        public void execute() {
            registerFailedRmiRequest();
            pingerExecutor.execute(this);
        }

        @Override
        public void run() {
            RmiQueue.runRetryableRequest(new Callable<Object>() {
                public Object call() throws Exception {
                    remoteLogics.ping();
                    return true;
                }
            }, abandoned, true);
        }

        public void abandon() {
            abandoned.set(true);
        }
    }
}
