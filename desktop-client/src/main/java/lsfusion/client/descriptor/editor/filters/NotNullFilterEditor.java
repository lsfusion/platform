package lsfusion.client.descriptor.editor.filters;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.filter.NotNullFilterDescriptor;

public class NotNullFilterEditor extends PropertyFilterEditor {

    public NotNullFilterEditor(GroupObjectDescriptor group, NotNullFilterDescriptor descriptor, FormDescriptor form) {
        super(group, descriptor, form);
    }
}
