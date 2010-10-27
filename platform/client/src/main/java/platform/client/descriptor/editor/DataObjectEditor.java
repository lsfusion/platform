package platform.client.descriptor.editor;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.DataObjectDescriptor;
import platform.client.descriptor.increment.editor.IncrementTextEditor;
import platform.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.logics.classes.ClientClass;

import javax.swing.*;
import java.util.List;
import java.util.Arrays;
import java.awt.*;

public class DataObjectEditor extends JPanel {

    public DataObjectEditor(final DataObjectDescriptor descriptor) {

        add(new TitledPanel("Значение", new IncrementTextEditor(descriptor, "object")));

        add(Box.createRigidArea(new Dimension(5, 5)));

        add(new TitledPanel("Тип", new JComboBox(new IncrementSingleListSelectionModel(descriptor, "typeClass") {
            public List<?> getList() {
                return Arrays.asList(ClientClass.getEnumTypeClasses());
            }
        })));
    }
}
