package lsfusion.gwt.client.form.property.table.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.LinkedHashMap;
import java.util.Map;

public class GPropertyContextMenuPopup {
    public interface ItemSelectionListener {
        void onMenuItemSelected(String actionSID);
    }

    public static void show(GPropertyDraw property, int x, int y, final ItemSelectionListener selectionListener) {
        if (property == null) {
            return;
        }

        LinkedHashMap<String, String> contextMenuItems = property.getContextMenuItems();
        if (contextMenuItems == null || contextMenuItems.isEmpty()) {
            return;
        }

        final PopupPanel popup = new PopupPanel(true);

        final MenuBar menuBar = new MenuBar(true);
        for (final Map.Entry<String, String> item : contextMenuItems.entrySet()) {
            final String actionSID = item.getKey();
            String caption = item.getValue();
            MenuItem menuItem = new MenuItem(caption, new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    popup.hide();
                    selectionListener.onMenuItemSelected(actionSID);
                }
            });

            menuBar.addItem(menuItem);
        }

        popup.setWidget(menuBar);
        GwtClientUtils.showPopupInWindow(popup, x, y);
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                menuBar.focus();
            }
        });
    }
}
