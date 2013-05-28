package platform.client.descriptor.increment.editor;

import platform.base.WeakIdentityHashSet;
import platform.base.context.ApplicationContextProvider;
import platform.base.context.IncrementView;

import java.util.List;

public abstract class IncrementMultipleListSelectionModel extends IncrementListSelectionModel<List<?>> implements IncrementView {

    public IncrementMultipleListSelectionModel(ApplicationContextProvider object, String field) {
        super(object, field);
    }

    private WeakIdentityHashSet<IncrementSelectionView> selectionViews = new WeakIdentityHashSet<IncrementSelectionView>();

    public void addSelectionView(IncrementSelectionView selectionView) {
        selectionViews.add(selectionView);

        selectionView.updateSelection();
    }

    protected void updateSelectionViews() {
        // чисто из-за особенностей инициализации
        if (selectionViews != null) {
            for (IncrementSelectionView selectionView : selectionViews) {
                selectionView.updateSelection();
            }
        }
    }
}
