package platform.client.descriptor.editor;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.SimplePropertyFilter;

import java.util.List;

public class ListGroupObjectEditor extends SimplePropertyFilter {

    public ListGroupObjectEditor(List<GroupObjectDescriptor> groupObjects) {
        super(groupObjects);
    }

    @Override
    int getPanelCount() {
        return 2;
    }

    @Override
    boolean hasDirectPanel() {
        return false;
    }

}
