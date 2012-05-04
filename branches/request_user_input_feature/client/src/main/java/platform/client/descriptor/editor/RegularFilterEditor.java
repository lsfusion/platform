package platform.client.descriptor.editor;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.filter.RegularFilterDescriptor;
import platform.client.descriptor.increment.editor.IncrementTextEditor;

import javax.swing.*;

public class RegularFilterEditor extends GroupElementEditor {

    public RegularFilterEditor(GroupObjectDescriptor groupObject, RegularFilterDescriptor regularFilter, FormDescriptor form) {
        super(groupObject);

        addTab(ClientResourceBundle.getString("descriptor.editor.common"), new TitledPanel(ClientResourceBundle.getString("descriptor.editor.name"), new IncrementTextEditor(regularFilter, "caption")));
    }
}
