package platform.client.descriptor.increment.editor;

import platform.base.BaseUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class IncrementMultipleListEditor extends JList implements ListSelectionListener, IncrementSelectionView {
    private boolean updated = false;

    IncrementMultipleListSelectionModel incrementModel;

    public IncrementMultipleListEditor(IncrementMultipleListSelectionModel incrementModel) {
        super(incrementModel);

        this.incrementModel = incrementModel;
        incrementModel.addSelectionView(this);

        addListSelectionListener(this);
    }

    public void valueChanged(ListSelectionEvent ev) {
        if (!updated) {
            incrementModel.setSelectedItem(BaseUtils.toList(getSelectedValues()));
        }
    }

    public void updateSelection() {
        updated = true;

        ListModel dm = getModel();

        int n = 0;
        int indices[] = new int[incrementModel.selected.size()];
        for (int i = 0; i < dm.getSize(); ++i) {
            Object element = dm.getElementAt(i);
            for (Object selectObject : incrementModel.selected) {
                if (selectObject != null && selectObject.equals(element)) {
                    indices[n++] = i;
                    break;
                }
            }
        }

        setSelectedIndices(indices);

        updated = false;
    }
}
