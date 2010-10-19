package platform.client.descriptor.increment.editor;

import platform.client.descriptor.increment.IncrementView;
import platform.client.descriptor.increment.IncrementDependency;
import platform.base.BaseUtils;

import javax.swing.*;
import java.util.List;

public abstract class IncrementListSelectionModel<S> extends AbstractListModel implements IncrementView {

    // по сути своего рода Increment Lazy
    public List<?> list;

    public S selected;

    public final Object object;
    public final String field;

    public void setSelectedItem(S anItem) {
        if(!BaseUtils.nullEquals(selected, anItem))
            BaseUtils.invokeSetter(object, field, anItem);
    }

    public Object getSelectedItem() {
        return selected;
    }

    private class SelectIncrement implements IncrementView {

        public void update(Object updateObject, String updateField) {
            S updateSelected = (S) BaseUtils.invokeGetter(object, field);
            if(!BaseUtils.nullEquals(updateSelected, selected)) {
                selected = updateSelected;

                updateSelectionViews();
            }
        }
    }
    public final SelectIncrement selectIncrement = new SelectIncrement();

    public abstract List<?> getList();

    // для Dependencies
    public abstract void fillListDependencies();

    public int getSize() {
        return list.size();
    }

    public Object getElementAt(int index) {
        return list.get(index);
    }

    protected IncrementListSelectionModel(Object object, String field) {
        this.object = object;
        this.field = field;

        IncrementDependency.add(object, field, this);
        fillListDependencies();
        
        IncrementDependency.add(object, field, selectIncrement);
    }

    protected abstract void updateSelectionViews();

    public void update(Object updateObject, String updateField) {
        List<?> updateList = getList();
        if(!updateList.equals(list)) {
            list = updateList;

            fireContentsChanged(this, -1, -1);
        }
   }
}
