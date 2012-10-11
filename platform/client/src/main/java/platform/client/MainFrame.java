package platform.client;

import platform.interop.form.RemoteFormInterface;
import platform.interop.form.ReportGenerationData;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.rmi.RemoteException;
import java.util.Scanner;

import static platform.client.ClientResourceBundle.getString;

public abstract class MainFrame extends JFrame {

    public static interface FormCloseListener {
        public void formClosed();
    }

    protected File baseDir;
    public RemoteNavigatorInterface remoteNavigator;
    public JLabel statusComponent;
    public JComponent status;

    public MainFrame(final RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
        super();

        this.remoteNavigator = remoteNavigator;

        setIconImage(Main.getMainIcon().getImage());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        updateUser();

        statusComponent = new JLabel();
        status = new JPanel(new BorderLayout());
        status.add(statusComponent, BorderLayout.CENTER);

        loadLayout();

        initUIHandlers(remoteNavigator);
    }

    private void initUIHandlers(final RemoteNavigatorInterface remoteNavigator) {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                int confirmed = JOptionPane.showConfirmDialog(MainFrame.this,
                                                              getString("quit.do.you.really.want.to.quit"),
                                                              getString("quit.confirmation"),
                                                              JOptionPane.YES_NO_OPTION);
                if (confirmed == JOptionPane.YES_OPTION) {
                    try {
                        remoteNavigator.close();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    dispose();

                    Main.shutdown();
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
                //windowClosing не срабатывает, если просто вызван dispose,
                //поэтому сохраняем лэйаут в windowClosed
                saveLayout();
            }
        });
    }

    private void loadLayout() {
        try {
            baseDir = new File(System.getProperty("user.home"), ".fusion\\" + Main.remoteLogics.getName());
        } catch (RemoteException e) {
            //по умолчанию
            baseDir = new File(System.getProperty("user.home"), ".fusion");
        }

        try {
            File layoutFile = new File(baseDir, "dimension.txt");
            if (layoutFile.exists()) {
                Scanner in = new Scanner(new FileReader(layoutFile));
                int wWidth = in.nextInt();
                int wHeight = in.nextInt();
                setSize(wWidth, wHeight);
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize().getSize();
            setSize(size.width, size.height - 30);
        }
    }

    private void saveLayout() {
        baseDir.mkdirs();

        try {
            FileWriter out = new FileWriter(new File(baseDir, "dimension.txt"));

            out.write(getWidth() + " " + getHeight() + '\n');

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateUser() throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(remoteNavigator.getCurrentUserInfoByteArray()));
        setTitle(Main.getMainTitle() + " - " + inputStream.readObject());
    }

    public abstract void runReport(String reportSID, boolean isModal, ReportGenerationData generationData) throws IOException, ClassNotFoundException;

    public abstract void runForm(RemoteFormInterface remoteForm, FormCloseListener closeListener);
}