package lsfusion.client.descriptor.editor;

import lsfusion.client.descriptor.GroupObjectDescriptor;

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
