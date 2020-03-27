package lsfusion.client.form.property.table.view;

import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientPropertyContextMenuPopup extends JPopupMenu {
    public interface ItemSelectionListener {
        void onMenuItemSelected(String actionSID);
    }

    public ClientPropertyContextMenuPopup() {
    }

    public void show(ClientPropertyDraw property, Component owner, Point point, final ItemSelectionListener selectionListener) {
        if (property == null) {
            return;
        }
        LinkedHashMap<String, String> contextMenuItems = property.getContextMenuItems();
        if (contextMenuItems == null || contextMenuItems.isEmpty()) {
            return;
        }

        removeAll();

        for (Map.Entry<String, String> e : contextMenuItems.entrySet()) {
            final String action = e.getKey();
            final String caption = e.getValue();

            JMenuItem item = new JMenuItem(caption, null);
            item.setOpaque(false);
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectionListener.onMenuItemSelected(action);
                }
            });
            add(item);
        }

        show(owner, point.x, point.y);
    }
}