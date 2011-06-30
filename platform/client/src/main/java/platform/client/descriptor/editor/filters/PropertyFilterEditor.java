package platform.client.descriptor.editor.filters;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.PropertyObjectEditor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.filter.PropertyFilterDescriptor;

import javax.swing.*;

public class PropertyFilterEditor extends JPanel implements NodeEditor {

    public PropertyFilterEditor(GroupObjectDescriptor group, PropertyFilterDescriptor descriptor, final FormDescriptor form) {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(new TitledPanel(ClientResourceBundle.getString("descriptor.filter.property"), new PropertyObjectEditor(descriptor, "property", form, group)));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
