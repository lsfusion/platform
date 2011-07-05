package platform.client.rmi;

import org.jdesktop.jxlayer.JXLayer;
import platform.client.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;

import static platform.client.ClientResourceBundle.getString;

public class ConnectionLostManager {

    private static boolean connectionLost = false;
    public static ConnectionLostPainterUI connectionLostUI = new ConnectionLostPainterUI();
    public static JXLayer layer;
    private static WeakReference<JFrame> frameRef;

    private static BlockDialog blockDialog;

    private static void setConnectionLost(boolean lost) {
        connectionLost = lost;
        connectionLostUI.lockAndPing(lost);
    }

    public static void forceDisconnect() {
        connectionLost(getString("rmi.connectionlost.because.because.of.another.client.with.your.login"), true);
    }

    public static void connectionLost(boolean fatal) {
        connectionLost(null, fatal);
    }

    public static void connectionLost(String message, boolean fatal) {
        JFrame currentFrame = getCurrentFrame();
        if (!connectionLost && currentFrame != null) {
            setConnectionLost(true);

            blockDialog = new BlockDialog(message, currentFrame, fatal);
            blockDialog.setVisible(true);
        }
    }

    public static void connectionRelived() {
        if (connectionLost && (blockDialog == null || !blockDialog.isFatal())) {
            setConnectionLost(false);
            clean();
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
            layer = new JXLayer(frame.getContentPane());
            layer.setUI(connectionLostUI);
            frame.setContentPane(layer);

            frameRef = new WeakReference<JFrame>(frame);
        }
    }

    private static JFrame getCurrentFrame() {
        return frameRef != null ? frameRef.get() : null;
    }

    public static class BlockDialog extends JDialog implements ActionListener {
        private JButton okBut;
        private final boolean fatal;

        public BlockDialog(String message, JFrame owner, boolean fatal) {
            super(owner, getString("rmi.connectionlost"), true);

            this.fatal = fatal;

            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            setLocationRelativeTo(owner);

            String messageText =
                    message != null
                            ? message
                            : fatal
                            ? "<html>"+ getString("rmi.connectionlost.on.communication.with.server")+" <br> "+ getString("rmi.connectionlost.try.to.restart.application.manually")+"</html>"
                            : "<html>"+ getString("rmi.connectionlost.with.server")+" <br> "+ getString("rmi.connectionlost.you.can.wait.or.restart")+"</html>";

            okBut = new JButton(getString("rmi.connectionlost.close.application"));
            okBut.addActionListener(this);

            JPanel messagePanel = new JPanel();
            messagePanel.add(new JLabel(messageText));

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(okBut);


            Container pane = getContentPane();
            pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

            pane.add(messagePanel);
            if (!fatal) {
                JProgressBar progressBar = new JProgressBar();
                progressBar.setIndeterminate(true);
                JPanel progressPanel = new JPanel();
                progressPanel.add(progressBar);
                pane.add(progressPanel);
            }

            pane.add(buttonPanel);

            pack();
            setResizable(false);
        }

        public void actionPerformed(ActionEvent e) {
            frameRef = null;
            clean();
            Main.restart();
        }

        public boolean isFatal() {
            return fatal;
        }
    }
}
