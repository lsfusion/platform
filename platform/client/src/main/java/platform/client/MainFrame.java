package platform.client;

import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.rmi.RemoteException;
import java.util.Scanner;

public abstract class MainFrame extends JFrame {
    protected File baseDir;
    public RemoteNavigatorInterface remoteNavigator;
    public JComponent statusComponent;

    public MainFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
        super();

        this.remoteNavigator = remoteNavigator;

        setIconImage(new ImageIcon(getClass().getResource("/platform/images/lsfusion.jpg")).getImage());

        updateUser();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            baseDir = new File(System.getProperty("user.home"), ".fusion\\" + Main.remoteLogics.getName());
        } catch (RemoteException e) {
            //по умолчанию
            baseDir = new File(System.getProperty("user.home"), ".fusion");
        }

        try {
            Scanner in = new Scanner(new FileReader(new File(baseDir, "dimension.txt")));
            int wWidth = in.nextInt();
            int wHeight = in.nextInt();
            setSize(wWidth, wHeight);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize().getSize();
            setSize(size.width, size.height - 30);
        }

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                baseDir.mkdirs();

                try {
                    FileWriter fileWr = new FileWriter(new File(baseDir, "dimension.txt"));
                    fileWr.write(getWidth() + " " + getHeight() + '\n');

                    fileWr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        statusComponent = new JLabel(" ");
    }

    public void updateUser() throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(remoteNavigator.getCurrentUserInfoByteArray()));
        setTitle("LS Fusion - " + inputStream.readObject());
    }

    public abstract void runReport(RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException;
    public abstract void runForm(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException;
}