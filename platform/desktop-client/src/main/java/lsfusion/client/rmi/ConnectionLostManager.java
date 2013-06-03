package lsfusion.client.rmi;

import org.jdesktop.jxlayer.JXLayer;
import lsfusion.client.Main;
import lsfusion.client.StartupProperties;
import lsfusion.client.exceptions.ClientExceptionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;

import static lsfusion.client.ClientResourceBundle.getString;

public class ConnectionLostManager {
    private static boolean connectionLost = false;
    private static ConnectionLostPainterUI connectionLostUI;
    private static JXLayer layer;
    private static WeakReference<JFrame> frameRef;

    private static BlockDialog blockDialog;

    private static void setConnectionLost(boolean lost) {
        connectionLost = lost;
        connectionLostUI.lockAndPing(lost);
    }

    public static void forceDisconnect(String message) {
        connectionLost(message, true, true);
    }

    public static void connectionLost(boolean fatal) {
        connectionLost(null, fatal, true);
    }

    public static void connectionLost(String message, boolean fatal, boolean showReconnect) {
        JFrame currentFrame = getCurrentFrame();
        if (!connectionLost && currentFrame != null) {
            setConnectionLost(true);

            blockDialog = new BlockDialog(message, currentFrame, fatal, showReconnect);
            blockDialog.setVisible(true);
        }
    }

    public static void connectionRelived() {
        if (connectionLost && (blockDialog == null || !blockDialog.isFatal())) {
            setConnectionLost(false);
            clean();

            //возможно, что восстановление соединения не починило rmi-ссылки, тогда поможет только reconnect...
            //пингуем сервер чтобы проверить это
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        Main.remoteLogics.ping();
                    } catch (RemoteException e) {
                        ClientExceptionManager.handle(e);
                    }
                }
            });
        }
    }

    private static void clean() {
        if (blockDialog != null) {
            blockDialog.setVisible(false);
            blockDialog.dispose();
            blockDialog = null;
        }
    }

    public static void install(JFrame frame) {
        JFrame currentFrame = getCurrentFrame();

        if (currentFrame == null) {
            connectionLostUI = new ConnectionLostPainterUI();
            layer = new JXLayer(frame.getContentPane());
            layer.setUI(connectionLostUI);
            frame.setContentPane(layer);

            frameRef = new WeakReference<JFrame>(frame);
        }
    }

    private static JFrame getCurrentFrame() {
        return frameRef != null ? frameRef.get() : null;
    }

    public static void invalidate() {
        if (connectionLostUI != null) {
            connectionLostUI.shutdown();
        }

        frameRef = null;
        layer = null;
        connectionLost = false;
        connectionLostUI = null;
        clean();
    }

    public static class BlockDialog extends JDialog {
        private JButton btnExit;
        private JButton btnCancel;
        private JButton btnReconnect;
        private final boolean fatal;

        public BlockDialog(String message, JFrame owner, boolean fatal, boolean showReconnect) {
            super(owner, getString("rmi.connectionlost"), true);

            this.fatal = fatal;

            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            setLocationRelativeTo(owner);

            String messageText =
                    message != null
                    ? message
                    : fatal
                      ? getString("rmi.connectionlost.fatal")
                      : getString("rmi.connectionlost.nonfatal");

            btnExit = new JButton(getString("rmi.connectionlost.exit"));
            btnCancel = new JButton(getString("rmi.connectionlost.cancel"));
            btnReconnect = new JButton(getString("rmi.connectionlost.reconnect"));

            Container contentPane = getContentPane();
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(btnExit);

            if (showReconnect) {
                buttonPanel.add(btnReconnect);
            }

            JPanel messagePanel = new JPanel();
            messagePanel.add(new JLabel(messageText));

            contentPane.add(messagePanel);
            if (!fatal) {
                buttonPanel.add(btnCancel);

                JProgressBar progressBar = new JProgressBar();
                progressBar.setIndeterminate(true);
                JPanel progressPanel = new JPanel();
                progressPanel.add(progressBar);
                contentPane.add(progressPanel);
            }

            contentPane.add(buttonPanel);

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

        public boolean isFatal() {
            return fatal;
        }
    }
}
