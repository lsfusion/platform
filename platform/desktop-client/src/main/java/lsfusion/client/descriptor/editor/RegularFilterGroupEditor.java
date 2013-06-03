package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.filter.RegularFilterGroupDescriptor;

import javax.swing.*;

public class RegularFilterGroupEditor extends GroupElementEditor {

    public RegularFilterGroupEditor(GroupObjectDescriptor groupObject, RegularFilterGroupDescriptor regularGroup) {
        super(groupObject);

        addTab(ClientResourceBundle.getString("descriptor.editor.common"), new JLabel("Regular Filter Group : "+regularGroup));
    }
}
