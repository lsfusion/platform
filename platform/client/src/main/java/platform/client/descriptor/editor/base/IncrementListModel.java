package platform.client.descriptor.editor.base;

import platform.client.descriptor.increment.IncrementView;

import javax.swing.*;
import java.util.List;

public abstract class IncrementListModel extends AbstractListModel implements IncrementView {

    // по сути своего рода Increment Lazy
    public List<?> list;

    public abstract List<?> getList();

    // для Dependencies
    public abstract void fillListDependencies();

    public int getSize() {
        return list.size();
    }

    public Object getElementAt(int index) {
        return list.get(index);
    }

    protected IncrementListModel() {
        fillListDependencies();
    }

    public void update(Object updateObject, String updateField) {
        list = getList();

        fireContentsChanged(this, -1, -1);
   }
}
