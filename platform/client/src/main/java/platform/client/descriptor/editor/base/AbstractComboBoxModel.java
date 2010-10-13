package platform.client.descriptor.editor.base;

import platform.base.BaseUtils;

import javax.swing.*;

public abstract class AbstractComboBoxModel extends AbstractListModel implements ComboBoxModel {
    Object selectedObject = null;

    public void setSelectedItem(Object anItem) {
        if (!BaseUtils.nullEquals(selectedObject, anItem)) {
            selectedObject = anItem;
            fireContentsChanged(this, -1, -1);
        }
    }

    public Object getSelectedItem() {
        return selectedObject;
    }
}
