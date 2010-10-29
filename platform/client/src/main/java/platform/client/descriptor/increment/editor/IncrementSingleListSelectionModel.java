package platform.client.descriptor.increment.editor;

import javax.swing.*;

public abstract class IncrementSingleListSelectionModel extends IncrementListSelectionModel<Object> implements ComboBoxModel {

    protected void updateSelectionViews() {
        fireContentsChanged(this, -1, -1);
    }

    protected IncrementSingleListSelectionModel(Object object, String field) {
        super(object, field);
    }
}
