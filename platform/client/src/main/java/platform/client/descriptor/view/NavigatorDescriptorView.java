package platform.client.descriptor.view;

import platform.client.descriptor.FormDescriptor;
import platform.client.logics.ClientForm;
import platform.client.navigator.AbstractNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class NavigatorDescriptorView extends JPanel {

    public NavigatorDescriptorView(final RemoteNavigatorInterface navigator) {

        setLayout(new BorderLayout());

        final FormDescriptorView formView = new FormDescriptorView();

        final AbstractNavigator navigatorView = new AbstractNavigator(navigator) {

            @Override
            public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {

                ClientForm richDesign = new ClientForm(new DataInputStream(new ByteArrayInputStream(navigator.getRichDesignByteArray(element.ID))));

                ClientSerializationPool pool = new ClientSerializationPool(richDesign);
                FormDescriptor formDescriptor = (FormDescriptor) pool.deserializeObject(
                        new DataInputStream(new ByteArrayInputStream(navigator.getFormEntityByteArray(element.ID))));

                formView.setModel(formDescriptor);
            }
        };

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigatorView, formView);
        splitPane.setResizeWeight(0.1);
        add(splitPane, BorderLayout.CENTER);
    }
}
