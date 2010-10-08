package platform.client.descriptor.editor;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.FilterDescriptor;

import javax.swing.*;

public class FixedFilterEditor extends GroupElementEditor {

    public FixedFilterEditor(GroupObjectDescriptor groupObject, FilterDescriptor fixedFilter) {
        super(groupObject);
        
        add(new JLabel("Fixed Filter : "+fixedFilter));
    }
}
