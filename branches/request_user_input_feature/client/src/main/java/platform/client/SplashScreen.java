package platform.client;

import javax.swing.*;
import java.awt.*;

public class SplashScreen extends JFrame {
    private final static SplashScreen instance = new SplashScreen();
    private JLabel lbLogo;

    private SplashScreen() {
        setAlwaysOnTop(true);
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        lbLogo = new JLabel();

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(lbLogo, BorderLayout.CENTER);
        contentPane.add(progressBar, BorderLayout.SOUTH);
    }

    private void setLogo(ImageIcon logo) {
        lbLogo.setIcon(logo);
        pack();
        setLocationRelativeTo(null);
    }

    static public void start(ImageIcon logo) {
        instance.setLogo(logo);
        instance.setVisible(true);
    }

    static public void close(){
        instance.setVisible(false);
        instance.dispose();
    }
}
