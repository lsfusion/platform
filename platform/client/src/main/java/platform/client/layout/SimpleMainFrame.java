package platform.client.layout;

import net.sf.jasperreports.engine.JRException;
import platform.client.form.ClientForm;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.io.IOException;
import java.util.StringTokenizer;

public class SimpleMainFrame extends MainFrame {
    
    public SimpleMainFrame(RemoteNavigatorInterface remoteNavigator, String forms) throws ClassNotFoundException, IOException {
        super(remoteNavigator);

        ClientNavigator navigator = new ClientNavigator(remoteNavigator) {

            @Override
            public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException, JRException {
                // пока ничего не делаем, так как не должны вообще формы вызываться
            }
        };

        JTabbedPane mainPane = new JTabbedPane(JTabbedPane.BOTTOM);

        StringTokenizer st = new StringTokenizer(forms, ",");
        while (st.hasMoreTokens()) {
            Integer formID = Integer.parseInt(st.nextToken());
            ClientForm form = new ClientForm(navigator.remoteNavigator.createForm(formID, false), navigator);
            String caption = navigator.remoteNavigator.getCaption(formID); // надо будет переделать, чтобы не было лишнего вызова
            mainPane.addTab(caption, form);
        }

        setContentPane(mainPane);
    }

    @Override
    public void runReport(ClientNavigator clientNavigator, RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException, JRException {
        // надо здесь подумать, что вызывать
    }

    @Override
    public void runForm(ClientNavigator clientNavigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException, JRException {
        // надо здесь подумать, что вызывать
    }
}
