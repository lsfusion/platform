package platform.client.rmi;

import org.jdesktop.jxlayer.JXLayer;
import platform.client.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;

public class ConnectionLostManager {

    private static boolean connectionLost = false;
    private static ConnectionLostPainterUI connectionLostUI;

    private static WeakReference<JFrame> frameRef;

    private static BlockDialog blockDialog;

    private static void setConnectionLost(boolean lost) {
        connectionLost = lost;
        connectionLostUI.setLocked(lost);
    }

    public static void connectionLost(boolean fatal) {
        JFrame currentFrame = getCurrentFrame();
        if (!connectionLost && currentFrame != null) {
            setConnectionLost(true);

            blockDialog = new BlockDialog(currentFrame, fatal);
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
            JXLayer layer = new JXLayer(frame.getContentPane());
            layer.setUI(connectionLostUI = new ConnectionLostPainterUI());
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

        public BlockDialog(JFrame owner, boolean fatal) {
            super(owner, "Соедиение потеряно", true);

            this.fatal = fatal;

            setSize(270, 130);
            setResizable(false);
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            setLocationRelativeTo(owner);
            setLayout(new GridLayout(fatal ? 2 : 3, 1));

            String messageText = fatal
                ? "<html>Соединение с сервером потеряно, <br> попробуйте перелогииться.</html>"
                : "<html>Соединение с сервером потеряно, <br> вы можете перейти на форму логина <br> или подождать пока оно восстановиться.</html>";

            okBut = new JButton("Перейти к логину");
            okBut.addActionListener(this);

            JPanel messagePanel = new JPanel();
            messagePanel.add(new JLabel(messageText));

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(okBut);

            add(messagePanel);
            if (!fatal) {
                JProgressBar progressBar = new JProgressBar();
                progressBar.setIndeterminate(true);
                JPanel progressPanel = new JPanel();
                progressPanel.add(progressBar);
                add(progressPanel);
            }

            add(buttonPanel);
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
