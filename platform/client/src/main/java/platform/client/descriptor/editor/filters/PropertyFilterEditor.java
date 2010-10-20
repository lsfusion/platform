package platform.client.descriptor.editor.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.filter.PropertyFilterDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;
import java.util.List;

public class PropertyFilterEditor extends JPanel implements NodeEditor {

    public PropertyFilterEditor(PropertyFilterDescriptor descriptor, final FormDescriptor form, RemoteDescriptorInterface remote) {

        add(new TitledPanel("Реализация", new JComboBox(new IncrementSingleListSelectionModel(descriptor, "property") {
            public List<?> getList() {
                return form.getAllProperties();
            }
            public void fillListDependencies() {
                IncrementDependency.add(form, "groupObjects", this);
            }
        })));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
