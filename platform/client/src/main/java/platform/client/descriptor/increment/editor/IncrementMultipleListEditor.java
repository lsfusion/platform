package platform.client.descriptor.increment.editor;

import platform.base.BaseUtils;
import platform.client.descriptor.increment.editor.IncrementMultipleListSelectionModel;
import platform.client.descriptor.increment.editor.IncrementSelectionView;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

public class IncrementMultipleListEditor extends JList implements ListSelectionListener, IncrementSelectionView {

    public void valueChanged(ListSelectionEvent ev) {
        incrementModel.setSelectedItem(BaseUtils.toSet(getSelectedValues()));
    }

    IncrementMultipleListSelectionModel incrementModel;
    public IncrementMultipleListEditor(IncrementMultipleListSelectionModel incrementModel) {
        super(incrementModel);

        this.incrementModel = incrementModel;

        addListSelectionListener(this);
    }

    public void updateSelection() {
        clearSelection();

        for(Object selectObject : incrementModel.selected)
            setSelectedValue(selectObject, false);
    }
}
