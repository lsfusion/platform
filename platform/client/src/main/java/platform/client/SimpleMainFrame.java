package platform.client;

import platform.client.form.ClientForm;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.StringTokenizer;

public class SimpleMainFrame extends MainFrame {
    
    public SimpleMainFrame(RemoteNavigatorInterface remoteNavigator, String forms) throws ClassNotFoundException, IOException {
        super(remoteNavigator);

        ClientNavigator navigator = new ClientNavigator(remoteNavigator) {

            @Override
            public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
                // пока ничего не делаем, так как не должны вообще формы вызываться
            }
        };

        JTabbedPane mainPane = new JTabbedPane(JTabbedPane.BOTTOM);

        StringTokenizer st = new StringTokenizer(forms, ",");
        while (st.hasMoreTokens()) {
            Integer formID = Integer.parseInt(st.nextToken());
            ClientForm form = new ClientForm(navigator.remoteNavigator.createForm(formID, false), navigator);
            form.getComponent().setFocusTraversalPolicyProvider(true);
//            form.getComponent().setFocusTraversalPolicy(new DefaultFocusTraversalPolicy()); // ставим другую TraversalPolicy, чтобы работало быстрее на слабых компьютерах
            String caption = navigator.remoteNavigator.getCaption(formID); // надо будет переделать, чтобы не было лишнего вызова
            mainPane.addTab(caption, form.getComponent());
        }

        setContentPane(mainPane);
    }

    @Override
    public void runReport(ClientNavigator clientNavigator, RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException {
        // надо здесь подумать, что вызывать
    }

    @Override
    public void runForm(ClientNavigator clientNavigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        // надо здесь подумать, что вызывать
    }
}
