package platform.client;

import platform.client.form.ClientFormController;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
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

        Container cont = getContentPane();
        final JTabbedPane mainPane = new JTabbedPane(JTabbedPane.BOTTOM);

        StringTokenizer st = new StringTokenizer(forms, ",");
        while (st.hasMoreTokens()) {
            String formSID = st.nextToken();
            final ClientFormController form = new ClientFormController(remoteNavigator.createForm(formSID, false), null);
            form.getComponent().setFocusTraversalPolicyProvider(true);
            mainPane.addTab(form.getFullCaption(), form.getComponent());

            KeyStroke keyStroke = form.getKeyStroke();
            if (keyStroke != null) {
                mainPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, formSID);
                mainPane.getActionMap().put(formSID, new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        mainPane.setSelectedComponent(form.getComponent());
                    }
                });
            }
        }
        cont.setLayout(new BorderLayout());
        cont.add(mainPane, BorderLayout.CENTER);
        cont.add(statusComponent, BorderLayout.SOUTH);
        mainPane.setFocusable(false);
    }

    @Override
    public void runReport(RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException {
        // надо здесь подумать, что вызывать
    }

    @Override
    public void runForm(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        // надо здесь подумать, что вызывать
    }
}
