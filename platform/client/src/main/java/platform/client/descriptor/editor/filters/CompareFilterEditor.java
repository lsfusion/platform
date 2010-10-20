package platform.client.descriptor.editor.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.filter.CompareFilterDescriptor;
import platform.interop.serialization.RemoteDescriptorInterface;

public class CompareFilterEditor extends PropertyFilterEditor {

    public CompareFilterEditor(CompareFilterDescriptor descriptor, FormDescriptor form, RemoteDescriptorInterface remote) {
        super(descriptor, form, remote);
    }
}
