package platform.client;

import javax.swing.*;
import java.awt.*;

public class SplashScreen {
    private final static JFrame instance = new JFrame();

    static public void start(byte[] imageData) {
        ImageIcon splash = imageData != null ? new ImageIcon(imageData) : null;
        if (splash == null || splash.getImage() == null) {
            splash = new ImageIcon(SplashScreen.class.getResource("/platform/images/lsfusion.jpg"));
        }

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        Container contentPane = instance.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(new JLabel(splash), BorderLayout.CENTER);
        contentPane.add(progressBar, BorderLayout.SOUTH);

        instance.setAlwaysOnTop(true);
        instance.setUndecorated(true);
        instance.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        instance.pack();
        instance.setLocationRelativeTo(null);
        instance.setVisible(true);
    }

    static public void close(){
        instance.dispose();
    }
}
