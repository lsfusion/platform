package platform.gwt.form.client.form.ui;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.GridEditableCell;
import platform.gwt.form.shared.view.grid.InternalEditEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class GGridPropertyTableMenuHandler {
    private GPropertyTable table;

    public GGridPropertyTableMenuHandler(GPropertyTable table) {
        this.table = table;
    }

    public void show(int x, int y, final GridEditableCell editCell, final Cell.Context editContext, final Element cellParent) {
        GPropertyDraw property = table.getSelectedProperty();
        if (property != null) {
            LinkedHashMap<String, String> contextMenuItems = property.getContextMenuItems();
            if (contextMenuItems != null && !contextMenuItems.isEmpty()) {
                final PopupPanel popup = new PopupPanel(true);

                final MenuBar menuBar = new MenuBar(true);
                for (final Map.Entry<String, String> item : contextMenuItems.entrySet()) {
                    final String action = item.getKey();
                    String caption = item.getValue();
                    MenuItem menuItem = new MenuItem(caption, new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            popup.hide();
                            table.onEditEvent(editCell, new InternalEditEvent(action), editContext, cellParent);
                        }
                    });

                    menuBar.addItem(menuItem);
                }

                popup.setPopupPosition(x, y);
                popup.setWidget(menuBar);
                popup.show();
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        menuBar.focus();
                    }
                });
            }
        }
    }
}
