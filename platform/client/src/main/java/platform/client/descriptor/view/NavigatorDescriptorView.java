package platform.client.descriptor.view;

import platform.client.Main;
import platform.client.descriptor.FormDescriptor;
import platform.client.logics.ClientForm;
import platform.client.navigator.AbstractNavigator;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.client.serialization.ClientSerializationPool;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class NavigatorDescriptorView extends JPanel {

    public NavigatorDescriptorView(final ClientNavigator navigator) {

        setLayout(new BorderLayout());

        final FormDescriptorView formView = new FormDescriptorView(navigator, Main.remoteLogics);

        final AbstractNavigator navigatorView = new AbstractNavigator(navigator.remoteNavigator) {

            @Override
            public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
                ClientForm richDesign = new ClientSerializationPool().deserializeObject(
                        new DataInputStream(
                                new ByteArrayInputStream(
                                        navigator.remoteNavigator.getRichDesignByteArray(element.ID))));

                ClientSerializationPool pool = new ClientSerializationPool(richDesign);
                FormDescriptor formDescriptor = pool.deserializeObject(
                        new DataInputStream(
                                new ByteArrayInputStream(
                                        navigator.remoteNavigator.getFormEntityByteArray(element.ID))));

                formView.setForm(formDescriptor);
            }
        };

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigatorView, formView);
        splitPane.setResizeWeight(0.1);
        add(splitPane, BorderLayout.CENTER);
    }
}
