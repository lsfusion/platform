package lsfusion.client.descriptor.editor.filters;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.editor.PropertyObjectEditor;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.filter.PropertyFilterDescriptor;

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
