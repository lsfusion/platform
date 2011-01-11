package platform.client;

import javax.swing.*;
import java.awt.*;

public class SplashScreen {
    private final static JFrame instance = new JFrame();

    static public void start(ImageIcon logo) {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        Container contentPane = instance.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(new JLabel(logo), BorderLayout.CENTER);
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
