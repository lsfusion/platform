package platform.client;

import javax.swing.*;
import java.awt.*;

public class SplashScreen {
    private final static JFrame instance = new JFrame();

    static public void start() {
        instance.setSize(300, 273);
        Container content = instance.getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel label1 = new JLabel();
        label1.setIcon(new ImageIcon(SplashScreen.class.getResource("/platform/images/lsfusion.jpg")));
        label1.setText("");
        content.add(label1);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        //progressBar.setMaximumSize(new Dimension(250, 20));
        
        content.add(progressBar);
        instance.setLocationRelativeTo(null);
        instance.setAlwaysOnTop(true);
        instance.setUndecorated(true);
        instance.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        instance.setVisible(true);
    }

    static public void close(){
        instance.dispose();
    }
}
