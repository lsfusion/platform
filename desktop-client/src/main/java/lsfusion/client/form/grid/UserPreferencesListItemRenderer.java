package lsfusion.client.form.grid;

import javax.swing.*;
import java.awt.*;

public class UserPreferencesListItemRenderer extends DefaultListCellRenderer {
    private boolean visibleList;

    public UserPreferencesListItemRenderer(boolean visibleList) {
        this.visibleList = visibleList;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        UserPreferencesPropertyListItem listItem = (UserPreferencesPropertyListItem) value;
        if (visibleList) {
            if (listItem.inGrid == null || !listItem.inGrid) {
                component.setForeground(Color.LIGHT_GRAY);
            }
        } else if (listItem.inGrid != null && !listItem.inGrid) { // справа не выделяем спрятанные колонки, т.к. пока никак не отличаем спрятанные настройкой от спрятанных через showIf
            component.setForeground(Color.LIGHT_GRAY);
        }
        return component;
    }
}
