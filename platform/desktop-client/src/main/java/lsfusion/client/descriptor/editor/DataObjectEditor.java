package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.DataObjectDescriptor;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import lsfusion.client.descriptor.increment.editor.IncrementTextEditor;
import lsfusion.client.logics.classes.ClientClass;

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
