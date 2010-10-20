package platform.client.descriptor.editor.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.filter.IsClassFilterDescriptor;
import platform.interop.serialization.RemoteDescriptorInterface;

public class IsClassFilterEditor extends PropertyFilterEditor {

    public IsClassFilterEditor(IsClassFilterDescriptor descriptor, FormDescriptor form, RemoteDescriptorInterface remote) {
        super(descriptor, form, remote);
    }
}
