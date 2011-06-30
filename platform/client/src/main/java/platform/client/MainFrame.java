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
    public JLabel statusComponent;
    public JComponent status;

    public MainFrame(final RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
        super();

        this.remoteNavigator = remoteNavigator;

        setIconImage(Main.getMainIcon().getImage());

        updateUser();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

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
        } catch (Exception e) {
            e.printStackTrace();
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize().getSize();
            setSize(size.width, size.height - 30);
        }

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {

                int confirmed = JOptionPane.showConfirmDialog(MainFrame.this,
                        ClientResourceBundle.getString("quit.do.you.really.want.to.quit"), ClientResourceBundle.getString("quit.confirmation"),
                        JOptionPane.YES_NO_OPTION);

                if (confirmed == JOptionPane.YES_OPTION) {

                    baseDir.mkdirs();

                    try {
                        FileWriter fileWr = new FileWriter(new File(baseDir, "dimension.txt"));
                        fileWr.write(getWidth() + " " + getHeight() + '\n');

                        fileWr.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        remoteNavigator.close();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    MainFrame.this.dispose();
                }
            }
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        status = new JPanel(new BorderLayout());
        statusComponent = new JLabel();
        status.add(statusComponent, BorderLayout.LINE_START);
    }

    public void updateUser() throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(remoteNavigator.getCurrentUserInfoByteArray()));
        setTitle(Main.getMainTitle() + " - " + inputStream.readObject());
    }

    public abstract void runReport(RemoteFormInterface remoteForm, boolean isModal) throws ClassNotFoundException, IOException;

    public abstract void runSingleGroupReport(RemoteFormInterface remoteForm, int groupId) throws IOException, ClassNotFoundException;

    public abstract void runSingleGroupXlsExport(RemoteFormInterface remoteForm, int groupId) throws IOException, ClassNotFoundException;

    public abstract void runForm(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException;
}