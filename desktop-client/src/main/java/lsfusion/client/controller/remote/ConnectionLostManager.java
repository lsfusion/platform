package lsfusion.client.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.DaemonThreadFactory;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.heavy.weak.WeakIdentityHashSet;
import lsfusion.client.StartupProperties;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.exception.ClientExceptionManager;
import lsfusion.client.controller.MainController;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.base.exception.NonFatalRemoteClientException;
import lsfusion.interop.navigator.remote.ClientCallBackInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    private static boolean devMode;

    public static void start(MainFrame frame, ClientCallBackInterface clientCallBack, boolean devMode) {
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

                        blockDialog.hideDialog();

                        blockDialog = null;
                    }
                }
            }
        });

        ConnectionLostManager.devMode = devMode;
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
            blockDialog.showDialog();
        }
    }

    public static void registerFailedRmiRequest() {
        //каждый зарегистрировавщийся поток, должне также проверять ConnectionLost, чтобы заркрыться при необходимости...
        failedRequests.incrementAndGet();
        RmiQueue.notifyEdtSyncBlocker();
    }
    
    private static final ConcurrentHashMap<Long, List<NonFatalRemoteClientException>> failedNotFatalHandledRequests = new ConcurrentHashMap<>();
    public static void addFailedRmiRequest(RemoteException remote, long reqId) {
        List<NonFatalRemoteClientException> exceptions = failedNotFatalHandledRequests.get(reqId);
        if(exceptions == null) {
            exceptions = new ArrayList<>();
            failedNotFatalHandledRequests.put(reqId, exceptions);
        }
        NonFatalRemoteClientException e = new NonFatalRemoteClientException(ExceptionUtils.copyMessage(remote), reqId);
        ExceptionUtils.copyStackTraces(remote, e);
        exceptions.add(e);
    }
    
    public static void flushFailedNotFatalRequests(final boolean abandoned, long reqId) {
        final List<NonFatalRemoteClientException> flushExceptions = failedNotFatalHandledRequests.remove(reqId);
        if(flushExceptions != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Map<Pair<String, Long>, Collection<NonFatalRemoteClientException>> group;
                        group = BaseUtils.group(new BaseUtils.Group<Pair<String, Long>, NonFatalRemoteClientException>() {
                            public Pair<String, Long> group(NonFatalRemoteClientException key) {
                                return new Pair<>(ExceptionUtils.toString(key), key.reqId);
                            }
                        }, flushExceptions);

                    int count = 0;
                    for (Map.Entry<Pair<String, Long>, Collection<NonFatalRemoteClientException>> entry : group.entrySet()) {
                        if(count++ > 20) // по аналогии с вебом, так как большое количество ошибок открывает слишком много socket'ов (потом может просто надо будет batch обработку сделать) 
                            break;
                        Collection<NonFatalRemoteClientException> all = entry.getValue();
                        NonFatalRemoteClientException nonFatal = all.iterator().next();
                        nonFatal.count = all.size();
                        nonFatal.abandoned = abandoned;
                        ClientExceptionManager.reportThrowable(nonFatal);
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
            blockDialog.hideDialog();
            blockDialog = null;
        }
    }

    public static class BlockDialog extends JDialog {
        private Timer showButtonsTimer;
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

            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            btnExit = new JButton(getString("rmi.connectionlost.exit"));
            btnExit.setEnabled(false);
            btnCancel = new JButton(getString("rmi.connectionlost.relogin"));
            btnCancel.setEnabled(false);
            btnReconnect = new JButton(getString("rmi.connectionlost.reconnect"));
            btnReconnect.setEnabled(false);

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
            
            setLocationRelativeTo(owner);

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    MainController.shutdown();
                }
            });
        }

        public void showDialog() {
            showButtonsTimer = new Timer(5000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    btnExit.setEnabled(true);
                    btnCancel.setEnabled(true);
                    btnReconnect.setEnabled(true);
                }
            });
            showButtonsTimer.start();
            setVisible(true);
        }

        public void hideDialog() {
            showButtonsTimer.stop();
            dispose();
        }

        private void setupDialogForDevMode() {
            //чтобы блокер-диалог не забирал фокус
            if (StartupProperties.preventBlockerActivation) {
                setFocusableWindowState(false);
            }
        }

        private void initUIHandlers() {
            btnExit.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    MainController.shutdown();
                }
            });
            btnCancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    MainController.restart();
                }
            });
            btnReconnect.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    MainController.reconnect();
                }
            });
        }

        public void setFatal(boolean fatal) {
            if (this.fatal != fatal) {
                if(fatal && devMode) {
                    MainController.reconnect();
                } else {
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

    }

    static class Pinger implements Runnable {
        private AtomicBoolean abandoned = new AtomicBoolean();

        public void execute() {
            registerFailedRmiRequest();
            pingerExecutor.execute(this);
        }

        @Override
        public void run() {
            try {
                RmiQueue.runRetryableRequest(new Callable<Object>() {
                    public Object call() throws Exception {
                        if(MainController.remoteLogics != null)
                            MainController.remoteLogics.ping();
                        return true;
                    }
                }, abandoned, true);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }

        public void abandon() {
            abandoned.set(true);
        }
    }
}
