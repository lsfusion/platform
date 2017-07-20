package lsfusion.gwt.form.client.form.ui.filter;

import com.google.gwt.user.client.ui.ListBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GFilterConditionListBox extends ListBox {
    List<Object> items = new ArrayList<>();

    public void add(Object item) {
        items.add(item);
        addItem(item == null ? null : item.toString());
    }

    public void add(Collection<?> items) {
        for (Object item : items) {
            add(item);
        }
    }

    public void add(Object...items) {
        for (Object item : items) {
            add(item);
        }
    }

    public void add(Object value, String caption) {
        items.add(value);
        addItem(caption);
    }

    public void setItems(Object[] newItems) {
        items.clear();
        clear();
        add(newItems);
    }

    public Object getObjectValue(int index) {
        return index != -1 ? items.get(index) : null;
    }

    public Object getSelectedItem() {
        return getObjectValue(getSelectedIndex());
    }

    public void setSelectedItem(Object item) {
        if (items.contains(item)) {
            setSelectedIndex(items.indexOf(item));
        }
    }
}
