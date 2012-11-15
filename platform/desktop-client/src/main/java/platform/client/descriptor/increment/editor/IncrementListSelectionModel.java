package platform.client.descriptor.increment.editor;

import platform.base.BaseUtils;
import platform.base.context.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public abstract class IncrementListSelectionModel<S> extends AbstractListModel implements IncrementView {

    // по сути своего рода Increment Lazy
    public List<?> list;

    public S selected;

    public final Object object;
    public final String field;

    public void setSelectedItem(S anItem) {
        if (!BaseUtils.nullEquals(selected, anItem)) {
            BaseUtils.invokeSetter(object, field, anItem);
        }
    }

    public Object getSelectedItem() {
        return selected;
    }

    private class SelectIncrement implements IncrementView {

        public void update(Object updateObject, String updateField) {
            selected = (S) BaseUtils.invokeGetter(object, field);
            updateSelectionViews();
        }
    }

    public final SelectIncrement selectIncrement = new SelectIncrement();

    public abstract List<?> getList();

    //этот метод, как правило, нужно переопределять
    public void fillListDependencies() {
    }

    public int getSize() {
        return list.size();
    }

    public Object getElementAt(int index) {
        return list.get(index);
    }

    protected IncrementListSelectionModel(ApplicationContextProvider object, String field) {
        this.object = object;
        this.field = field;

        object.getContext().addDependency(object, field, this);
        fillListDependencies();

        object.getContext().addDependency(object, field, selectIncrement);
    }

    protected abstract void updateSelectionViews();

    public void update(Object updateObject, String updateField) {
        List<?> updateList = new ArrayList(getList());
        if(!updateList.equals(list)) {
            list = updateList;
            fireContentsChanged(this, -1, -1);
        }
   }
}
