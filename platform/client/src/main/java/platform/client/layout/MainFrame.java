package platform.client.layout;

import net.sf.jasperreports.engine.JRException;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public abstract class MainFrame extends JFrame {

    public MainFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
        super();

        setIconImage(new ImageIcon(getClass().getResource("/platform/images/lsfusion.jpg")).getImage());

        drawCurrentUser(remoteNavigator);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(1024, 768);
    }

    public void drawCurrentUser(RemoteNavigatorInterface remoteNavigator) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(remoteNavigator.getCurrentUserInfoByteArray()));
        setTitle("LS Fusion - " + inputStream.readObject());
    }

    public abstract void runReport(ClientNavigator clientNavigator, RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException, JRException;
    public abstract void runForm(ClientNavigator clientNavigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException, JRException;
}