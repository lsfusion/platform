package platform.client.descriptor.increment.editor;

import platform.base.BaseUtils;
import platform.client.descriptor.increment.editor.IncrementMultipleListSelectionModel;
import platform.client.descriptor.increment.editor.IncrementSelectionView;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

public class IncrementMultipleListEditor extends JList implements ListSelectionListener, IncrementSelectionView {

    private boolean updated = false;
    public void valueChanged(ListSelectionEvent ev) {
        if(!updated)
            incrementModel.setSelectedItem(BaseUtils.toList(getSelectedValues()));
    }

    IncrementMultipleListSelectionModel incrementModel;
    public IncrementMultipleListEditor(IncrementMultipleListSelectionModel incrementModel) {
        super(incrementModel);

        this.incrementModel = incrementModel;
        incrementModel.addSelectionView(this);

        addListSelectionListener(this);
    }

    public void updateSelection() {
        updated = true;

        clearSelection();

        for(Object selectObject : incrementModel.selected)
            setSelectedValue(selectObject, false);

        updated = false;
    }
}
