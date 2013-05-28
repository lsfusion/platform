package platform.client.descriptor.editor.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.NotNullFilterDescriptor;

public class NotNullFilterEditor extends PropertyFilterEditor {

    public NotNullFilterEditor(GroupObjectDescriptor group, NotNullFilterDescriptor descriptor, FormDescriptor form) {
        super(group, descriptor, form);
    }
}
