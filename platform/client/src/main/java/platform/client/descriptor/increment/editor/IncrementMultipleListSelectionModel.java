package platform.client.descriptor.increment.editor;

import platform.client.descriptor.increment.IncrementView;
import platform.client.descriptor.increment.editor.IncrementSelectionView;
import platform.client.descriptor.increment.editor.IncrementListSelectionModel;
import platform.base.WeakIdentityHashSet;

import java.util.Set;
import java.util.List;

public abstract class IncrementMultipleListSelectionModel extends IncrementListSelectionModel<List<?>> implements IncrementView {

    public IncrementMultipleListSelectionModel(Object object, String field) {
        super(object, field);
    }

    private WeakIdentityHashSet<IncrementSelectionView> selectionViews = new WeakIdentityHashSet<IncrementSelectionView>();

    public void addSelectionView(IncrementSelectionView selectionView) {
        selectionViews.add(selectionView);

        selectionView.updateSelection();
    }

    protected void updateSelectionViews() {
        if(selectionViews!=null) // чисто из-за особенностей инициализации 
            for(IncrementSelectionView selectionView : selectionViews)
                selectionView.updateSelection();
    }
}
