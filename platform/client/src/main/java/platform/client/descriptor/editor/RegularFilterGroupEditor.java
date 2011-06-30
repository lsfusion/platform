package platform.client.descriptor.editor;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.RegularFilterGroupDescriptor;

import javax.swing.*;

public class RegularFilterGroupEditor extends GroupElementEditor {

    public RegularFilterGroupEditor(GroupObjectDescriptor groupObject, RegularFilterGroupDescriptor regularGroup) {
        super(groupObject);

        addTab(ClientResourceBundle.getString("descriptor.editor.common"), new JLabel("Regular Filter Group : "+regularGroup));
    }
}
