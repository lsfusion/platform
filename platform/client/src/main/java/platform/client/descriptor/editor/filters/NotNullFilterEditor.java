package platform.client.descriptor.editor.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.filter.NotNullFilterDescriptor;
import platform.interop.serialization.RemoteDescriptorInterface;

public class NotNullFilterEditor extends PropertyFilterEditor {

    public NotNullFilterEditor(NotNullFilterDescriptor descriptor, FormDescriptor form, RemoteDescriptorInterface remote) {
        super(descriptor, form, remote);
    }
}
