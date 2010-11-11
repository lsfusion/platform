package platform.client.rmi;

import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;

import javax.swing.*;

public class ConnectionLostManager {

    private static boolean connectionLost = false;
    private static LockableUI connectionLostUI = new ConnectionLostPainterUI();
    private static boolean installed = false;

    public static void setConnectionLost(boolean lost) {
        if (connectionLost != lost) {
            connectionLost = lost;
            connectionLostUI.setLocked(lost);
        }
    }

    public static void install(JFrame frame) {
        if (!installed) {
            JXLayer layer = new JXLayer(frame.getContentPane());
            layer.setUI(connectionLostUI);
            frame.setContentPane(layer);
            installed = true;
        }
    }
}
