package lsfusion.client.descriptor.view;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;

public class PreviewDialog extends JDialog {
    private final FormDescriptor form;
    private final ClientNavigator navigator;

    public PreviewDialog(ClientNavigator iNavigator, FormDescriptor iForm) {
        super(null, Dialog.ModalityType.MODELESS);

        form = iForm;
        navigator = iNavigator;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout());
        try {
            RemoteFormInterface remoteForm = navigator.remoteNavigator.createPreviewForm(form.serialize());

            ClientFormController controller = new ClientFormController(remoteForm, navigator) {
                @Override
                public void hideForm() {
                    setVisible(false);
                    super.hideForm();
                }
            };
            controller.getComponent().setFocusTraversalPolicyProvider(true);

            add(controller.getComponent(), BorderLayout.CENTER);
        } catch (Exception e) {
            throw new RuntimeException(ClientResourceBundle.getString("descriptor.view.can.not.create.form"), e);
        }
    }
}
