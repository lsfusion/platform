package platform.client.descriptor.editor;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.RegularFilterDescriptor;

import javax.swing.*;

public class RegularFilterEditor extends GroupElementEditor {

    public RegularFilterEditor(GroupObjectDescriptor groupObject, RegularFilterDescriptor regularFilter) {
        super(groupObject);

        add(new JLabel("Property : "+regularFilter));
    }
}
