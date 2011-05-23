package platform.client.descriptor.editor;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.RegularFilterGroupDescriptor;

import javax.swing.*;

public class RegularFilterGroupEditor extends GroupElementEditor {

    public RegularFilterGroupEditor(GroupObjectDescriptor groupObject, RegularFilterGroupDescriptor regularGroup) {
        super(groupObject);

        addTab("Общее", new JLabel("Regular Filter Group : "+regularGroup));
    }
}
