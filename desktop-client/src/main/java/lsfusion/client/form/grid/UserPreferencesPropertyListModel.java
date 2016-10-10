package lsfusion.client.form.grid;

import javax.swing.*;
import java.util.Arrays;

public class UserPreferencesPropertyListModel extends DefaultListModel<UserPreferencesPropertyListItem> {
    private boolean visible;

    public UserPreferencesPropertyListModel(boolean visible) {
        this.visible = visible;
    }
    
    @Override
    public UserPreferencesPropertyListItem getElementAt(int index) {
        return super.getElementAt(index);
    }

    @Override
    public UserPreferencesPropertyListItem get(int index) {
        return super.get(index);
    }

    @Override
    public UserPreferencesPropertyListItem[] toArray() {
        Object[] values = super.toArray();
        return Arrays.copyOf(values, values.length, UserPreferencesPropertyListItem[].class);
    }

    @Override
    public void insertElementAt(UserPreferencesPropertyListItem element, int index) {
        super.insertElementAt(element, index);
        added(element);
    }

    @Override
    public void add(int index, UserPreferencesPropertyListItem element) {
        super.add(index, element);
        added(element);
    }

    @Override
    public void addElement(UserPreferencesPropertyListItem element) {
        super.addElement(element);
        added(element);
    }
    
    private void added(UserPreferencesPropertyListItem element) {
        if (element != null) {
            element.setVisibleList(visible);
        }
    }
}