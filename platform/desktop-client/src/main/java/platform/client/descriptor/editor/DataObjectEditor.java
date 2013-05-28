package platform.client.descriptor.editor;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.DataObjectDescriptor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import platform.client.descriptor.increment.editor.IncrementTextEditor;
import platform.client.logics.classes.ClientClass;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class DataObjectEditor extends JPanel {

    public DataObjectEditor(final DataObjectDescriptor descriptor) {

        add(new TitledPanel(ClientResourceBundle.getString("descriptor.editor.value"), new IncrementTextEditor(descriptor, "object")));

        add(Box.createRigidArea(new Dimension(5, 5)));

        add(new TitledPanel(ClientResourceBundle.getString("descriptor.editor.type"), new JComboBox(new IncrementSingleListSelectionModel(descriptor, "typeClass") {
            public List<?> getSingleList() {
                return Arrays.asList(ClientClass.getEnumTypeClasses());
            }
        })));
    }
}
