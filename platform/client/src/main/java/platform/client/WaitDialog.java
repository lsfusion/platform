package platform.client;

import com.jhlabs.image.BlurFilter;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.effect.BufferedImageOpEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;
import platform.client.rmi.ConnectionLostManager;
import platform.client.rmi.ConnectionLostPainterUI;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;

public class WaitDialog {

    static JDialog dialog = new JDialog(Main.frame, true);
    static JLabel label = new JLabel("");
    static JLabel text = new JLabel("        Loading...");
    static boolean show = false;

    static {
        dialog.setLayout(new BorderLayout(10, 3));
        label.setIcon(new ImageIcon(WaitDialog.class.getResource("/platform/images/loader.gif")));
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize().getSize();
        dialog.setBounds((size.width - 150) / 2, (size.height - 20) / 2, 240, 40);
        dialog.setUndecorated(true);
        dialog.add(text, BorderLayout.NORTH);
        dialog.add(label, BorderLayout.CENTER);
    }

    public static void start() {
        if (show || ConnectionLostManager.layer == null) {
            return;
        }

        show = true;

        for (int i = 0; i < 20; i++) {
            try {
                Thread.sleep(50);
            } catch (Exception e) {
            }
            if (!show) return;
        }
        ConnectionLostManager.connectionLostUI.setLocked(true);
        dialog.setVisible(true);
    }


    public static void finish() {
        if (!show) {
            return;
        }
        show = false;
        dialog.setVisible(false);
        ConnectionLostManager.connectionLostUI.setLocked(false);
    }
}
